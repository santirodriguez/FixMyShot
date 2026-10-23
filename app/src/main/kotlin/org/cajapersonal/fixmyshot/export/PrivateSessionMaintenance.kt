package org.cajapersonal.fixmyshot.export

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException

/** Only private working copies are removed. Provider output is never touched here. */
internal class PrivateSessionMaintenance(
    private val cache: File,
    private val jobs: File,
    private val acknowledged: (String) -> Boolean,
) {
    private val idPattern = Regex("[0-9a-f]{32}")

    /** An unreadable unreviewed job conservatively prevents source cleanup. */
    private fun protectedSources(current: Set<String>): Set<String>? {
        return try {
        val folders = if (jobs.exists()) jobs.listFiles() ?: return null else emptyArray()
        if (folders.size > 512) return null
        val retained = current.toMutableSet()
        for (folder in folders) {
            if (!folder.isDirectory || !idPattern.matches(folder.name) ||
                folder.canonicalFile != File(jobs.canonicalFile, folder.name) ||
                folder.canonicalFile.parentFile != jobs.canonicalFile) return null
            if (acknowledged(folder.name)) continue
            val snapshot = BatchExportJournal(File(folder, "batch.json")).read() ?: return null
            if (snapshot.token != folder.name) return null
            retained += snapshot.items.map { it.itemId }
        }
        retained
        } catch (_: Exception) { null }
    }

    fun release(ids: Set<String>, current: Set<String> = emptySet()): Boolean {
        val retained = protectedSources(current) ?: return false
        val root = File(cache, "review-sources")
        var success = true
        for (id in ids - retained) {
            if (!idPattern.matches(id)) continue
            val file = File(root, "$id.source")
            if (file.exists()) {
                if (!ownedFile(root, file) || !file.delete()) success = false
            }
        }
        return success
    }

    /** Remove the reviewed job's original URI/settings journal, retaining its output receipts. */
    fun releaseReviewedDraft(token: String, current: Set<String>): Boolean {
        if (!idPattern.matches(token) || !acknowledged(token)) return false
        val folder = File(jobs, token)
        return try {
            if (!folder.isDirectory || folder.canonicalFile != File(jobs.canonicalFile, token) ||
                folder.canonicalFile.parentFile != jobs.canonicalFile) return false
            RandomAccessFile(File(folder, ".active"), "rw").channel.use { channel ->
                val lock = try { channel.tryLock() } catch (_: OverlappingFileLockException) { null }
                if (lock == null) false else try {
                    val draftFile = File(folder, "draft.json")
                    val draft = ExportDraftJournal(draftFile).read()
                    val released = release(draft?.images?.map { it.id }?.toSet().orEmpty(), current)
                    if (released) android.util.AtomicFile(draftFile).delete()
                    released
                } finally { lock.release() }
            }
        } catch (_: Exception) { false }
    }

    /** Startup only, before import/export workers begin; active drafts and unresolved jobs win. */
    fun pruneExpired(current: Set<String>, now: Long = System.currentTimeMillis()) {
        val retained = protectedSources(current) ?: return
        // Finish interrupted private cleanup after a durable acknowledgement.
        jobs.listFiles()?.forEach { folder ->
            if (idPattern.matches(folder.name) && acknowledged(folder.name)) releaseReviewedDraft(folder.name, current)
        }
        val sources = File(cache, "review-sources")
        sources.listFiles()?.takeIf { it.size <= 4096 }?.forEach { file ->
            val id = file.name.removeSuffix(".source")
            if (file.name == "$id.source" && idPattern.matches(id) && id !in retained &&
                now - file.lastModified() > 24L * 60 * 60 * 1000 && ownedFile(sources, file)) file.delete()
        }
        // Stale candidates have no role in recovery; frozen sources own any later retry.
        val candidates = File(cache, "batch-candidates")
        candidates.listFiles()?.takeIf { it.size <= 4096 }?.forEach { file ->
            if (now - file.lastModified() > 24L * 60 * 60 * 1000 && ownedFile(candidates, file)) file.delete()
        }
    }

    private fun ownedFile(root: File, file: File): Boolean = file.isFile &&
        root.canonicalFile == File(cache.canonicalFile, root.name) &&
        file.canonicalFile == File(root.canonicalFile, file.name) && file.canonicalFile.parentFile == root.canonicalFile
}
