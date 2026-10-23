package org.cajapersonal.fixmyshot.export

import android.util.AtomicFile
import org.cajapersonal.fixmyshot.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Frozen export settings and private staged-source references, never image bytes or raw EXIF. */
internal class ExportDraftJournal(private val file: File) {
    companion object {
        private const val LIMIT = 2 * 1024 * 1024
        fun same(a: DraftSession, b: DraftSession): Boolean = a.destination == b.destination &&
            a.batch == b.batch && a.images == b.images && a.revision == b.revision

        fun encodeDraft(draft: DraftSession, allowEmpty: Boolean = false): JSONObject {
            require(draft.images.size in (if (allowEmpty) 0..100 else 1..100))
            val destination = when (val value = draft.destination) {
                is DestinationChoice.Preset -> JSONObject().put("preset", value.id)
                is DestinationChoice.Custom -> JSONObject().put("p", value.ratio.p).put("q", value.ratio.q)
            }
            val items = JSONArray()
            draft.images.forEach { image ->
                require(image.id.length <= 128 && image.sourceToken.length <= 16_384)
                items.put(JSONObject().put("id", image.id).put("source", image.sourceToken)
                    .put("width", image.orientedSize.width).put("height", image.orientedSize.height)
                    .put("alpha", image.sourceHasAlpha).put("revision", image.revision).put("color", image.sourceColor.name)
                    .put("hash", requireNotNull(image.contentSha256)).put("overrides", overrides(image.overrides)))
            }
            return JSONObject().put("schema", 1).put("catalog", DestinationCatalog.VERSION)
                .put("revision", draft.revision).put("destination", destination)
                .put("batch", settings(draft.batch)).put("images", items)
        }

        fun decodeDraft(json: JSONObject, allowEmpty: Boolean = false): DraftSession {
            require(json.getInt("schema") == 1 && json.getInt("catalog") == DestinationCatalog.VERSION)
            val destination = json.getJSONObject("destination").let {
                if (it.has("preset")) DestinationChoice.Preset(it.getString("preset"))
                else DestinationChoice.Custom.of(it.getInt("p"), it.getInt("q"))
            }
            val rows = json.getJSONArray("images")
            require(rows.length() in (if (allowEmpty) 0..100 else 1..100))
            val images = (0 until rows.length()).map { index ->
                val item = rows.getJSONObject(index)
                val id = item.getString("id"); val source = item.getString("source")
                require(id.length <= 128 && source.length <= 16_384)
                DraftImage(id, source, PixelSize(item.getInt("width"), item.getInt("height")),
                    item.getBoolean("alpha"), item.getLong("revision"),
                    readOverrides(item.getJSONObject("overrides")), item.getString("hash"),
                    if (item.has("color")) SourceColor.valueOf(item.getString("color")) else SourceColor.SRGB)
            }
            return DraftSession(destination, readSettings(json.getJSONObject("batch")), images, json.getLong("revision"))
        }

        private fun crop(value: CropFocus) = JSONObject().put("x", value.x).put("y", value.y).put("zoom", value.zoom)
        private fun readCrop(value: JSONObject) = CropFocus(value.getDouble("x"), value.getDouble("y"), value.getDouble("zoom"))
        private fun background(value: BackgroundChoice): JSONObject = when (value) {
            BackgroundChoice.Automatic -> JSONObject().put("kind", "automatic")
            BackgroundChoice.White -> JSONObject().put("kind", "white")
            BackgroundChoice.Black -> JSONObject().put("kind", "black")
            BackgroundChoice.Transparent -> JSONObject().put("kind", "transparent")
            is BackgroundChoice.CustomRgb -> JSONObject().put("kind", "rgb").put("rgb", value.rgb)
        }
        private fun readBackground(value: JSONObject): BackgroundChoice = when (value.getString("kind")) {
            "automatic" -> BackgroundChoice.Automatic
            "white" -> BackgroundChoice.White
            "black" -> BackgroundChoice.Black
            "transparent" -> BackgroundChoice.Transparent
            "rgb" -> BackgroundChoice.CustomRgb(value.getInt("rgb"))
            else -> throw IOException("Unknown background")
        }
        private fun size(value: OutputSizeChoice) = when (value) {
            OutputSizeChoice.PreservePixels -> JSONObject().put("kind", "native")
            is OutputSizeChoice.Exact -> JSONObject().put("kind", "exact")
                .put("width", value.canvas.width).put("height", value.canvas.height).put("enlarge", value.allowEnlargement)
        }
        private fun readSize(value: JSONObject): OutputSizeChoice = when (value.getString("kind")) {
            "native" -> OutputSizeChoice.PreservePixels
            "exact" -> OutputSizeChoice.Exact(PixelSize(value.getInt("width"), value.getInt("height")), value.getBoolean("enlarge"))
            else -> throw IOException("Unknown output size")
        }
        private fun settings(value: EditSettings) = JSONObject().put("fit", value.fit.name)
            .put("crop", crop(value.crop)).put("background", background(value.background))
            .put("format", value.format.name).put("size", size(value.size)).put("metadata", value.metadata.name)
        private fun readSettings(value: JSONObject) = EditSettings(FitMode.valueOf(value.getString("fit")),
            readCrop(value.getJSONObject("crop")), readBackground(value.getJSONObject("background")),
            OutputFormat.valueOf(value.getString("format")), readSize(value.getJSONObject("size")),
            MetadataPolicy.valueOf(value.getString("metadata")))
    
        private fun overrides(value: ItemOverrides): JSONObject {
            val result = JSONObject()
            fun <T> put(key: String, field: ItemValue<T>, encode: (T) -> Any) {
                if (field is ItemValue.Custom) result.put(key, encode(field.value))
            }
            put("fit", value.fit) { it.name }; put("crop", value.crop, ::crop)
            put("background", value.background, ::background); put("format", value.format) { it.name }
            put("size", value.size, ::size); put("metadata", value.metadata) { it.name }
            return result
        }
        private fun readOverrides(value: JSONObject): ItemOverrides {
            fun <T> field(key: String, decode: () -> T): ItemValue<T> =
                if (value.has(key)) ItemValue.Custom(decode()) else ItemValue.Inherit
            return ItemOverrides(
                field("fit") { FitMode.valueOf(value.getString("fit")) },
                field("crop") { readCrop(value.getJSONObject("crop")) },
                field("background") { readBackground(value.getJSONObject("background")) },
                field("format") { OutputFormat.valueOf(value.getString("format")) },
                field("size") { readSize(value.getJSONObject("size")) },
                field("metadata") { MetadataPolicy.valueOf(value.getString("metadata")) })
        }
    }
    private val atomic get() = AtomicFile(file)

    @Synchronized fun write(draft: DraftSession, planFingerprint: String? = null) {
        val document = encodeDraft(draft)
        if (planFingerprint != null) {
            require(planFingerprint.matches(Regex("[0-9a-f]{64}")))
            document.put("plan", planFingerprint)
        }
        val bytes = document.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > LIMIT) throw IOException("Frozen draft exceeds limit")
        if (!file.parentFile.isDirectory && !file.parentFile.mkdirs()) throw IOException("Draft directory unavailable")
        val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) }
        catch (error: Exception) { atomic.failWrite(output); throw IOException("Draft checkpoint failed", error) }
    }

    @Synchronized fun read(): DraftSession? {
        if (!file.exists() && !File(file.path + ".bak").exists()) return null
        try {
            val json = JSONObject(atomic.openRead().use { readJournalBytes(it, LIMIT) }.toString(Charsets.UTF_8))
            return decodeDraft(json)
        } catch (error: Exception) { throw IOException("Frozen draft unavailable; review before exporting", error) }
    }

    @Synchronized fun matchesPlan(recipes: List<PrivateOutputRecipe>): Boolean {
        if (recipes.isEmpty()) return false
        return try {
            val json = JSONObject(atomic.openRead().use { readJournalBytes(it, LIMIT) }.toString(Charsets.UTF_8))
            json.getInt("schema") == 1 && json.getInt("catalog") == DestinationCatalog.VERSION &&
                json.optString("plan") == FrozenPlanFingerprint.of(recipes)
        } catch (_: Exception) { false }
    }

}
