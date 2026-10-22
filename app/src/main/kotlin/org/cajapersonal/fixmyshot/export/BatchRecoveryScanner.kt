package org.cajapersonal.fixmyshot.export

import java.io.File
import java.io.IOException

internal data class RecoveryItem(val token: String?, val snapshot: BatchCheckpoint?, val pendingCount: Int)

internal class BatchRecoveryScanner(
    private val jobs: File,
    private val acknowledged: (String) -> Boolean = { false },
) {
    fun inspect(): RecoveryItem? {
        if (!jobs.exists()) return null
        val folders = jobs.listFiles() ?: return RecoveryItem(null, null, 1)
        if (folders.size > 512) return RecoveryItem(null, null, folders.size)
        if (folders.any {
                !it.isDirectory || !it.name.matches(Regex("[0-9a-f]{32}")) ||
                    it.canonicalFile.parentFile != jobs.canonicalFile
            }) return RecoveryItem(null, null, folders.size)
        val pending = folders.filterNot { acknowledged(it.name) }.sortedBy { it.name }
        val folder = pending.firstOrNull() ?: return null
        val snapshot = try {
            BatchExportJournal(File(folder, "batch.json")).read()
        } catch (_: IOException) { null }
        return RecoveryItem(folder.name, snapshot?.takeIf { it.token == folder.name }, pending.size)
    }
}
