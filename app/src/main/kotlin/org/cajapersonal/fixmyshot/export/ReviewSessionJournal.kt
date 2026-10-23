package org.cajapersonal.fixmyshot.export

import android.util.AtomicFile
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.PreviousDraft
import org.json.JSONObject
import java.io.File
import java.io.IOException

internal data class ReviewSessionSnapshot(val draft: DraftSession, val selectedId: String?, val savedAt: Long) {
    fun expired(now: Long): Boolean = now >= savedAt && now - savedAt > 24L * 60 * 60 * 1000
}

/** Current editable session only, including one undo snapshot. Never an export/retry instruction. */
internal class ReviewSessionJournal(private val file: File) {
    companion object {
        const val LIMIT = 4 * 1024 * 1024
        // AtomicFile needs caller serialization, including across reader instances in this process.
        private val lock = Any()
    }
    private val atomic get() = AtomicFile(file)

    private fun safeFiles() {
        for (suffix in listOf("", ".bak", ".new")) {
            val candidate = File(file.path + suffix)
            if (candidate.canonicalFile != File(file.parentFile.canonicalFile, candidate.name)) {
                throw IOException("Session journal path unavailable")
            }
        }
    }

    private fun validate(snapshot: ReviewSessionSnapshot) {
        require(snapshot.savedAt >= 0 && snapshot.draft.workingSourceIds.isNotEmpty())
        require(snapshot.selectedId == null || snapshot.draft.images.any { it.id == snapshot.selectedId })
        val images = snapshot.draft.images + snapshot.draft.undoSnapshot?.images.orEmpty()
        require(images.size <= 200)
        for (image in images) {
            require(image.id.matches(Regex("[0-9a-f]{32}")) && image.contentSha256 != null)
        }
        images.groupBy { it.id }.values.forEach { versions ->
            require(versions.map { listOf(it.sourceToken, it.contentSha256, it.orientedSize, it.sourceHasAlpha) }
                .distinct().size == 1)
        }
    }

    fun write(snapshot: ReviewSessionSnapshot) = synchronized(lock) { writeLocked(snapshot) }

    private fun writeLocked(snapshot: ReviewSessionSnapshot) {
        safeFiles()
        if (snapshot.draft.workingSourceIds.isEmpty()) { clear(); return }
        validate(snapshot)
        val json = JSONObject().put("schema", 1).put("savedAt", snapshot.savedAt)
            .put("current", ExportDraftJournal.encodeDraft(snapshot.draft, allowEmpty = true))
        snapshot.selectedId?.let { json.put("selected", it) }
        snapshot.draft.undoSnapshot?.let {
            json.put("undo", ExportDraftJournal.encodeDraft(DraftSession(it.destination, it.batch, it.images), allowEmpty = true))
        }
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > LIMIT) throw IOException("Session journal exceeds limit")
        if (!file.parentFile.isDirectory && !file.parentFile.mkdirs()) throw IOException("Session directory unavailable")
        val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) }
        catch (error: Exception) { atomic.failWrite(output); throw IOException("Session checkpoint failed", error) }
    }

    fun read(): ReviewSessionSnapshot? = synchronized(lock) { readLocked() }

    private fun readLocked(): ReviewSessionSnapshot? {
        safeFiles()
        if (!file.exists() && !File(file.path + ".bak").exists()) return null
        try {
            val json = JSONObject(atomic.openRead().use { readJournalBytes(it, LIMIT) }.toString(Charsets.UTF_8))
            require(json.getInt("schema") == 1)
            val current = ExportDraftJournal.decodeDraft(json.getJSONObject("current"), allowEmpty = true)
            val previous = if (json.has("undo")) ExportDraftJournal.decodeDraft(json.getJSONObject("undo"), allowEmpty = true)
                .let { PreviousDraft(it.destination, it.batch, it.images) } else null
            val draft = DraftSession(current.destination, current.batch, current.images, current.revision, previous)
            return ReviewSessionSnapshot(draft, if (json.has("selected")) json.getString("selected") else null,
                json.getLong("savedAt")).also(::validate)
        } catch (error: Exception) { throw IOException("Saved review session unavailable", error) }
    }

    fun clear() = synchronized(lock) { clearLocked() }

    /** Finishing one export must not clear a different, newer saved working session. */
    fun clearMatching(draft: DraftSession): Boolean = synchronized(lock) {
        val saved = readLocked()
        if (saved != null && saved.draft != draft) false else { clearLocked(); true }
    }

    private fun clearLocked() {
        safeFiles()
        atomic.delete()
        if (listOf("", ".bak", ".new").any { File(file.path + it).exists() }) {
            throw IOException("Saved review session could not be discarded")
        }
    }
}
