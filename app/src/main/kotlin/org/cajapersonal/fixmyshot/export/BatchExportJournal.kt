package org.cajapersonal.fixmyshot.export

import android.net.Uri
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** A bounded, app-private batch receipt. It never triggers an automatic retry. */
internal enum class BatchItemPhase {
    QUEUED, ENCODING, SAVING, PUBLISHED, FAILED, CANCELLED, NEEDS_REVIEW
}

internal data class BatchItemCheckpoint(
    val itemId: String,
    val sourceSha256: String,
    val phase: BatchItemPhase,
    val uri: String? = null,
    val reason: String? = null,
) {
    init {
        require(itemId.isNotBlank() && itemId.length <= 128)
        require(sourceSha256.matches(Regex("[0-9a-f]{64}")))
        if (uri != null) {
            require(uri.length <= 2048 && Uri.parse(uri).scheme == "content")
        }
        if (phase == BatchItemPhase.PUBLISHED) require(uri != null)
        if (reason != null) require(reason.matches(Regex("[A-Z_]{1,48}")))
    }
}

internal data class BatchCheckpoint(
    val token: String,
    val sessionRevision: Long,
    val items: List<BatchItemCheckpoint>,
) {
    init {
        require(token.matches(Regex("[0-9a-f]{32}")))
        require(sessionRevision >= 0L && items.size in 1..100)
        require(items.map { it.itemId }.distinct().size == items.size)
    }
}

/**
 * Persistent job snapshot and independent single-item output checkpoints live
 * under the same private directory. A stale snapshot is for reconciliation,
 * not permission to silently re-export already published user images.
 */
internal class BatchExportJournal(private val file: File) {
    private val atomic get() = AtomicFile(file)

    @Synchronized
    fun read(): BatchCheckpoint? {
        if (!file.exists() && !File(file.path + ".bak").exists()) return null
        try {
            val bytes = atomic.openRead().use { input ->
                val content = readJournalBytes(input, 65_536)
                if (content.size > 65_536) throw IOException("Batch snapshot exceeds limit")
                content
            }
            val data = JSONObject(bytes.toString(Charsets.UTF_8))
            if (data.getInt("schema") != 1) throw IOException("Unknown batch snapshot schema")
            val entries = data.getJSONArray("items")
            if (entries.length() !in 1..100) throw IOException("Invalid batch item count")
            val items = (0 until entries.length()).map { index ->
                val item = entries.getJSONObject(index)
                BatchItemCheckpoint(
                    itemId = item.getString("id"),
                    sourceSha256 = item.getString("hash"),
                    phase = BatchItemPhase.valueOf(item.getString("phase")),
                    uri = item.optString("uri").takeIf { it.isNotEmpty() },
                    reason = item.optString("reason").takeIf { it.isNotEmpty() },
                )
            }
            return BatchCheckpoint(
                token = data.getString("token"),
                sessionRevision = data.getLong("revision"),
                items = items,
            )
        } catch (error: Exception) {
            throw IOException("Unreadable batch snapshot; manual reconciliation required", error)
        }
    }

    @Synchronized
    fun write(snapshot: BatchCheckpoint) {
        val rows = JSONArray()
        snapshot.items.forEach { item ->
            val row = JSONObject()
                .put("id", item.itemId)
                .put("hash", item.sourceSha256)
                .put("phase", item.phase.name)
            item.uri?.let { row.put("uri", it) }
            item.reason?.let { row.put("reason", it) }
            rows.put(row)
        }
        val bytes = JSONObject()
            .put("schema", 1)
            .put("token", snapshot.token)
            .put("revision", snapshot.sessionRevision)
            .put("items", rows)
            .toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > 65_536) throw IOException("Batch snapshot exceeds limit")
        val parent = file.parentFile ?: throw IOException("Missing batch directory")
        if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Batch directory unavailable")
        val handle = atomic.startWrite()
        try {
            handle.write(bytes)
            atomic.finishWrite(handle)
        } catch (error: Exception) {
            atomic.failWrite(handle)
            throw IOException("Atomic batch checkpoint failed", error)
        }
    }
}

