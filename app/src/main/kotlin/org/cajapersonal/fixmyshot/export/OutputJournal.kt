package org.cajapersonal.fixmyshot.export

import android.net.Uri
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Versioned, app-private single-output checkpoint; no original URI or source metadata. */
internal enum class OutputStage {
    RESERVED, PENDING, VISIBLE_WRITING, VALIDATED, PUBLISHED, FAILED, CANCELLED, INDETERMINATE
}

internal data class OutputRecord(
    val token: String,
    val itemId: String,
    val filename: String,
    val mime: String,
    val digest: String,
    val stage: OutputStage,
    val uri: String? = null,
) {
    init {
        require(token.matches(Regex("[0-9a-f]{32}")))
        require(itemId.isNotBlank() && digest.matches(Regex("[0-9a-f]{64}")))
        require(filename.matches(Regex("FixMyShot_[0-9]{8}T[0-9]{6}Z_[0-9a-f]{8}\\.(png|webp)")))
        require(mime in setOf("image/png", "image/webp"))
        if (uri != null) require(Uri.parse(uri).scheme == "content")
        require(stage in setOf(OutputStage.RESERVED, OutputStage.FAILED, OutputStage.CANCELLED) ||
            uri != null) { "A pending/published output must carry its URI" }
    }
}

internal class OutputJournal(private val file: File) {
    private val atomic get() = AtomicFile(file)
    @Synchronized fun read(): OutputRecord? {
        if (!file.exists() && !File(file.path + ".bak").exists()) return null
        try {
            val data = atomic.openRead().use { it.readBytes() }
            if (data.size > 16_384) throw IOException("Journal too large")
            val json = JSONObject(data.toString(Charsets.UTF_8))
            if (json.getInt("schema") != 1) throw IOException("Unknown journal schema")
            return OutputRecord(
                token = json.getString("token"), itemId = json.getString("item"),
                filename = json.getString("name"), mime = json.getString("mime"),
                digest = json.getString("digest"),
                stage = OutputStage.valueOf(json.getString("stage")),
                uri = json.optString("uri").takeIf { it.isNotEmpty() },
            )
        } catch (error: Exception) {
            throw IOException("Export journal is unreadable; do not retry automatically", error)
        }
    }

    @Synchronized fun write(record: OutputRecord) {
        val json = JSONObject()
            .put("schema", 1)
            .put("token", record.token)
            .put("item", record.itemId)
            .put("name", record.filename)
            .put("mime", record.mime)
            .put("digest", record.digest)
            .put("stage", record.stage.name)
        record.uri?.let { json.put("uri", it) }
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        if (!file.parentFile.isDirectory && !file.parentFile.mkdirs()) throw IOException("Journal dir unavailable")
        val handle = atomic.startWrite()
        try {
            handle.write(bytes)
            atomic.finishWrite(handle)
        } catch (error: Exception) {
            atomic.failWrite(handle)
            throw IOException("Atomic journal update failed", error)
        }
    }

    /** Caller may clear only after the output has been reconciled or explicitly discarded. */
    @Synchronized fun clear() {
        atomic.delete()
    }
}
