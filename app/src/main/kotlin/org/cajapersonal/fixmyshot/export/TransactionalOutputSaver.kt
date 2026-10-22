package org.cajapersonal.fixmyshot.export

import android.net.Uri
import android.os.Looper
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.core.PrivateOutputRecipe
import org.cajapersonal.fixmyshot.review.PrivateCandidateVerifier
import org.cajapersonal.fixmyshot.review.PrivateExecutionResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal enum class SaveFailure {
    UNSUPPORTED_FORMAT, INVALID_CANDIDATE, PREVIOUS_JOB_UNRESOLVED,
    CANCELLED, PROVIDER_ERROR, JOURNAL_ERROR
}

internal sealed interface SaveResult {
    data class Published(val uri: Uri, val name: String) : SaveResult
    data class Failed(val reason: SaveFailure) : SaveResult
    /** A known owned output might already exist; require explicit reconciliation before retry. */
    data class Indeterminate(val uri: Uri?, val name: String) : SaveResult
}

/**
 * Single-item prototype of the G4 commit boundary. Runs on a worker and never alters originals.
 * No UI path invokes this yet. Real API29+ operations are supplied by MediaStore29Port.
 */
internal class TransactionalOutputSaver(
    private val privateCacheRoot: File,
    private val port: OwnedOutputPort,
    private val journal: OutputJournal,
) {
    @Synchronized
    fun save(
        recipe: PrivateOutputRecipe,
        ready: PrivateExecutionResult.Ready,
        cancelled: AtomicBoolean = AtomicBoolean(false),
    ): SaveResult {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Saving must not block the UI thread" }
        if (cancelled.get()) return SaveResult.Failed(SaveFailure.CANCELLED)
        if (recipe.request.format !in setOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
            return SaveResult.Failed(SaveFailure.UNSUPPORTED_FORMAT)
        }
        val source = ready.file
        val insideCache = try {
            source.canonicalPath.startsWith(privateCacheRoot.canonicalPath + File.separator)
        } catch (_: IOException) { false }
        if (!insideCache || source.length() !in 1L..128L * 1024 * 1024 ||
            ready.sourceSha256 != recipe.sourceSha256 || ready.output != recipe.canvas ||
            ready.codecRoute != recipe.codecRoute ||
            !PrivateCandidateVerifier.accepts(source, recipe.request)) {
            return SaveResult.Failed(SaveFailure.INVALID_CANDIDATE)
        }
        val digest = try { digestOf(FileInputStream(source)) } catch (_: IOException) {
            return SaveResult.Failed(SaveFailure.INVALID_CANDIDATE)
        }
        val token = UUID.randomUUID().toString().replace("-", "")
        val stamp = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val name = "FixMyShot_${stamp}_${token.take(8)}.${recipe.request.format.extension}"
        val reservation = OutputRecord(token, recipe.sourceId, name, recipe.request.format.mimeType,
            digest, OutputStage.RESERVED)
        try {
            if (journal.read() != null) return SaveResult.Failed(SaveFailure.PREVIOUS_JOB_UNRESOLVED)
            journal.write(reservation)
        } catch (_: IOException) {
            return SaveResult.Failed(SaveFailure.JOURNAL_ERROR)
        }

        var uri: Uri? = null
        var published = false
        var attemptedPublish = false
        var failure = SaveFailure.PROVIDER_ERROR
        try {
            uri = port.create(name, recipe.request.format.mimeType) ?: throw IOException("Insert failed")
            journal.write(reservation.copy(stage = OutputStage.PENDING, uri = uri.toString()))
            val output = port.openWrite(uri) ?: throw IOException("Output unavailable")
            output.use { to ->
                FileInputStream(source).use { from ->
                    val bytes = ByteArray(16 * 1024)
                    var transferred = 0L
                    val deadline = System.nanoTime() + 120_000_000_000L
                    while (true) {
                        if (cancelled.get()) throw InterruptedIOException("Cancelled")
                        if (System.nanoTime() >= deadline) throw IOException("Copy timeout")
                        val count = from.read(bytes)
                        if (count < 0) break
                        to.write(bytes, 0, count)
                        transferred += count
                    }
                    if (transferred != source.length()) throw IOException("Source changed during copy")
                }
                to.flush()
                if (to is FileOutputStream) to.fd.sync()
            }
            if (cancelled.get()) throw InterruptedIOException("Cancelled")
            val reopened = port.openRead(uri) ?: throw IOException("Cannot validate pending output")
            val actual = digestOf(reopened)
            if (actual != digest) throw IOException("Transferred candidate differs")
            journal.write(reservation.copy(stage = OutputStage.VALIDATED, uri = uri.toString()))
            if (cancelled.get()) throw InterruptedIOException("Cancelled")
            attemptedPublish = true
            if (!port.publish(uri)) throw IOException("Publication status indeterminate")
            published = true
            journal.write(reservation.copy(stage = OutputStage.PUBLISHED, uri = uri.toString()))
            return SaveResult.Published(uri, name)
        } catch (_: InterruptedIOException) {
            failure = SaveFailure.CANCELLED
        } catch (_: IOException) {
            failure = SaveFailure.PROVIDER_ERROR
        } catch (_: SecurityException) {
            failure = SaveFailure.PROVIDER_ERROR
        } catch (_: IllegalArgumentException) {
            failure = SaveFailure.PROVIDER_ERROR
        }

        // Once the provider was asked to publish, a missing confirmation is ambiguous.
        // Never automatically delete an output that might already be visible.
        if (attemptedPublish || published) {
            recordBestEffort(reservation.copy(stage = OutputStage.INDETERMINATE, uri = uri?.toString()))
            return SaveResult.Indeterminate(uri, name)
        }
        val removed = uri == null || try { port.delete(uri) } catch (_: Exception) { false }
        val updated = reservation.copy(
            stage = if (removed) {
                if (failure == SaveFailure.CANCELLED) OutputStage.CANCELLED else OutputStage.FAILED
            } else OutputStage.INDETERMINATE,
            uri = if (removed) null else uri?.toString(),
        )
        if (!recordBestEffort(updated)) return SaveResult.Indeterminate(uri, name)
        return if (removed) SaveResult.Failed(failure) else SaveResult.Indeterminate(uri, name)
    }

    /** Read-only restart checkpoint: no invisible retry, deletion or publication. */
    fun lastCheckpoint(): OutputRecord? = journal.read()

    private fun recordBestEffort(record: OutputRecord): Boolean = try {
        journal.write(record)
        true
    } catch (_: IOException) { false }

    private fun digestOf(stream: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        stream.use { from ->
            val block = ByteArray(16 * 1024)
            var size = 0L
            while (true) {
                val count = from.read(block)
                if (count < 0) break
                size += count
                if (size > 128L * 1024 * 1024) throw IOException("Candidate exceeds bounded size")
                digest.update(block, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
