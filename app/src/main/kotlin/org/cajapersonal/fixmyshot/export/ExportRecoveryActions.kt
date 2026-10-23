package org.cajapersonal.fixmyshot.export

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException

/** Deliberate actions on recorded outputs. No provider mutation or automatic promotion. */
internal class ExportRecoveryActions(
    private val jobs: File,
    private val verify: (OutputRecord) -> OutputRecoveryEvidence,
) {
    private fun folder(token: String): File {
        require(token.matches(Regex("[0-9a-f]{32}")))
        return File(jobs, token).also {
            require(it.isDirectory && it.canonicalFile == File(jobs.canonicalFile, token) &&
                it.canonicalFile.parentFile == jobs.canonicalFile)
        }
    }

    private fun record(folder: File, batch: BatchCheckpoint, id: String): Pair<Int, OutputRecord>? {
        val index = batch.items.indexOfFirst { it.itemId == id }
        if (index < 0) return null
        val item = batch.items[index]
        val output = OutputJournal(File(folder, "item-$index.json")).read() ?: return null
        if (output.itemId != item.itemId || output.uri == null ||
            (item.uri != null && item.uri != output.uri) || output.stage in setOf(
                OutputStage.RESERVED, OutputStage.FAILED, OutputStage.CANCELLED) ||
            verify(output) != OutputRecoveryEvidence.MATCHED) return null
        return index to output
    }

    /** ACTION_VIEW receives only the verified recorded URI with a temporary read grant. */
    fun open(token: String, id: String): Intent? = try {
        val folder = folder(token)
        val batch = BatchExportJournal(File(folder, "batch.json")).read()
        if (batch == null || batch.token != token) null else {
            val output = record(folder, batch, id)?.second
            output?.let {
                val uri = Uri.parse(it.uri)
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, it.mime).apply {
                    clipData = ClipData.newRawUri("FixMyShot", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    } catch (_: Exception) { null }

    /** Caller must obtain the user's explicit confirmation; reverify under the export lock. */
    fun confirmPublished(token: String, id: String): Boolean = try {
        val folder = folder(token)
        RandomAccessFile(File(folder, ".active"), "rw").channel.use { channel ->
            val lock = try { channel.tryLock() } catch (_: OverlappingFileLockException) { null }
            if (lock == null) false else try {
                val journal = BatchExportJournal(File(folder, "batch.json"))
                val batch = journal.read()
                val checked = batch?.takeIf { it.token == token }?.let { record(folder, it, id) }
                if (batch == null || checked == null) false else {
                    val (index, output) = checked
                    // Record item acceptance first. A crash still leaves the batch reviewable.
                    OutputJournal(File(folder, "item-$index.json")).write(output.copy(stage = OutputStage.PUBLISHED))
                    val items = batch.items.toMutableList()
                    items[index] = items[index].copy(phase = BatchItemPhase.PUBLISHED,
                        uri = output.uri, reason = null)
                    journal.write(batch.copy(items = items))
                    true
                }
            } finally { lock.release() }
        }
    } catch (_: Exception) { false }
}
