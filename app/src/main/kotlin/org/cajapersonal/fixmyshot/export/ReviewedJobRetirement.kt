package org.cajapersonal.fixmyshot.export

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException

/** Retire reviewed, fully published private receipts. Never reads or changes provider output. */
internal class ReviewedJobRetirement(
    private val jobs: File,
    private val acknowledged: (String) -> Boolean,
    private val forgetAcknowledged: (String) -> Boolean,
) {
    companion object {
        private val lock = Any()
        private val tokenPattern = Regex("[0-9a-f]{32}")
        private val journalNames = (listOf("batch.json") + (0 until 100).map { "item-$it.json" })
            .flatMap { name -> listOf(name, "$name.bak", "$name.new") }.toSet() + ".active"
    }

    private val retired get() = File(jobs.parentFile, "${jobs.name}-retired")

    private fun safeRoot(root: File): Boolean = root.parentFile != null &&
        root.canonicalFile == File(root.parentFile.canonicalFile, root.name) &&
        (!root.exists() || root.isDirectory)

    private fun safeFolder(root: File, folder: File): Boolean = safeRoot(root) &&
        folder.isDirectory && tokenPattern.matches(folder.name) &&
        folder.canonicalFile == File(root.canonicalFile, folder.name)

    private fun files(folder: File): List<File>? {
        val children = folder.listFiles()?.toList() ?: return null
        if (children.size > 304 || children.any { file ->
                file.name !in journalNames || !file.isFile ||
                    file.canonicalFile != File(folder.canonicalFile, file.name)
            }) return null
        return children
    }

    /** Rename the whole directory before deleting any receipt, so a crash cannot expose half a job. */
    fun retire(token: String): Boolean = synchronized(lock) {
        try {
            if (!tokenPattern.matches(token) || !acknowledged(token) || !safeRoot(jobs) || !safeRoot(retired)) {
                return@synchronized false
            }
            val folder = File(jobs, token)
            val destination = File(retired, token)
            if (!folder.exists()) return@synchronized destination.exists() && drain(destination)
            if (!safeFolder(jobs, folder) || files(folder) == null || destination.exists()) return@synchronized false
            RandomAccessFile(File(folder, ".active"), "rw").channel.use { channel ->
                val held = try { channel.tryLock() } catch (_: OverlappingFileLockException) { null }
                    ?: return@synchronized false
                try {
                    if (!acknowledged(token) || !safeFolder(jobs, folder) || files(folder) == null) {
                        return@synchronized false
                    }
                    val batch = BatchExportJournal(File(folder, "batch.json")).read()
                        ?: return@synchronized false
                    if (batch.token != token || batch.items.any { it.phase != BatchItemPhase.PUBLISHED }) {
                        return@synchronized false
                    }
                    val allowed = (listOf("batch.json") + batch.items.indices.map { "item-$it.json" })
                        .flatMap { name -> listOf(name, "$name.bak", "$name.new") }.toSet() + ".active"
                    if (files(folder)?.any { it.name !in allowed } != false) return@synchronized false
                    for ((index, item) in batch.items.withIndex()) {
                        val record = OutputJournal(File(folder, "item-$index.json")).read()
                        if (record == null || record.stage != OutputStage.PUBLISHED ||
                            record.itemId != item.itemId || record.uri != item.uri) return@synchronized false
                    }
                    if (!retired.isDirectory && !retired.mkdir()) return@synchronized false
                    if (!safeRoot(retired) || !folder.renameTo(destination)) return@synchronized false
                } finally { held.release() }
            }
            // Close the original stable lock inode before removing its retired private copy.
            drain(destination)
        } catch (_: Exception) { false }
    }

    private fun drain(folder: File): Boolean {
        if (!safeFolder(retired, folder) || File(jobs, folder.name).exists()) return false
        val children = files(folder) ?: return false
        if (children.isNotEmpty() && !acknowledged(folder.name)) return false
        for (file in children) if (!file.delete()) return false
        // An empty retired directory preserves a retry point if committing preference cleanup fails.
        if (!forgetAcknowledged(folder.name)) return false
        return folder.delete()
    }

    /** Retry only bounded, directly owned remnants of an earlier completed rename. */
    fun prune() = synchronized(lock) {
        try {
            if (!safeRoot(jobs) || !safeRoot(retired) || !retired.exists()) return@synchronized
            val folders = retired.listFiles()?.takeIf { it.size <= 512 } ?: return@synchronized
            folders.forEach { folder ->
                try { drain(folder) } catch (_: Exception) { /* Leave private evidence for a later start. */ }
            }
        } catch (_: Exception) { /* Cleanup never authorizes a provider operation. */ }
    }
}
