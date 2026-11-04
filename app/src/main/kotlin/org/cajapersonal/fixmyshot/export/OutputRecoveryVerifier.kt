package org.cajapersonal.fixmyshot.export

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest

/** Read-only provider evidence. Only MATCHED can support an existing PUBLISHED receipt. */
internal enum class OutputRecoveryEvidence {
    MATCHED, MISSING, MODIFIED, PENDING, UNVERIFIABLE
}

/**
 * Checks bytes still reachable through a previously recorded app-owned content URI.
 * Never creates, publishes, rewrites or deletes provider files. Budgets cap restart I/O.
 */
internal class OutputRecoveryVerifier(
    private val resolver: ContentResolver,
    private val api: Int = Build.VERSION.SDK_INT,
) {
    private val maxItemBytes = 128L * 1024 * 1024
    private val maxScanBytes = 256L * 1024 * 1024
    private var scannedBytes = 0L

    @Synchronized
    fun verify(record: OutputRecord): OutputRecoveryEvidence {
        val uri = record.uri?.let(Uri::parse) ?: return OutputRecoveryEvidence.UNVERIFIABLE
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            return OutputRecoveryEvidence.UNVERIFIABLE
        }
        try {
            // MediaStore can contain a complete but still hidden IS_PENDING item.
            if (api >= 29 && uri.authority == MediaStore.AUTHORITY) {
                val state = resolver.query(uri, arrayOf(MediaStore.MediaColumns.IS_PENDING),
                    null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return OutputRecoveryEvidence.MISSING
                    if (cursor.isNull(0)) return OutputRecoveryEvidence.UNVERIFIABLE
                    cursor.getInt(0)
                } ?: return OutputRecoveryEvidence.UNVERIFIABLE
                if (state != 0) return OutputRecoveryEvidence.PENDING
            }

            val deadline = System.nanoTime() + 30_000_000_000L
            val digest = MessageDigest.getInstance("SHA-256")
            var bytes = 0L
            val source = resolver.openInputStream(uri) ?: return OutputRecoveryEvidence.MISSING
            source.use { stream ->
                val block = ByteArray(16 * 1024)
                while (true) {
                    if (System.nanoTime() >= deadline) return OutputRecoveryEvidence.UNVERIFIABLE
                    val count = stream.read(block)
                    if (count < 0) break
                    if (count == 0 || bytes > maxItemBytes - count ||
                        scannedBytes > maxScanBytes - count) {
                        return OutputRecoveryEvidence.UNVERIFIABLE
                    }
                    digest.update(block, 0, count)
                    bytes += count
                    scannedBytes += count
                }
            }
            val actual = digest.digest().joinToString("") {
                "%02x".format(it.toInt() and 255)
            }
            return if (bytes > 0 && actual == record.digest)
                OutputRecoveryEvidence.MATCHED else OutputRecoveryEvidence.MODIFIED
        } catch (_: FileNotFoundException) {
            return OutputRecoveryEvidence.MISSING
        } catch (_: IOException) {
            return OutputRecoveryEvidence.UNVERIFIABLE
        } catch (_: SecurityException) {
            return OutputRecoveryEvidence.UNVERIFIABLE
        } catch (_: IllegalArgumentException) {
            return OutputRecoveryEvidence.UNVERIFIABLE
        } catch (_: IllegalStateException) {
            return OutputRecoveryEvidence.UNVERIFIABLE
        }
    }
}
