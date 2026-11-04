package org.cajapersonal.fixmyshot

import android.media.MediaCodec
import android.media.MediaFormat
import org.json.JSONObject

/**
 * Inspects the real codec owned by a HEIC writer, for synthetic Android tests only.
 * Private AndroidX field changes yield UNAVAILABLE, never a guessed codec identity.
 * A pre-stop format poll is not proof of the writer's private muxer callback format.
 */
internal object HeicCodecTrace {
    private fun field(owner: Any, name: String): Any? {
        var type: Class<*>? = owner.javaClass
        while (type != null) {
            try {
                val declared = type.getDeclaredField(name)
                declared.isAccessible = true
                return declared.get(owner)
            } catch (_: NoSuchFieldException) {
                type = type.superclass
            }
        }
        return null
    }

    private fun size(format: MediaFormat, key: String): Int? =
        if (format.containsKey(key)) format.getInteger(key) else null

    private fun unavailable(): JSONObject = JSONObject()
        .put("status", "UNAVAILABLE").put("reason", "REFLECTION_UNAVAILABLE")

    fun capture(writer: Any): JSONObject {
        return try {
            val encoder = field(writer, "mEncoder") ?: return unavailable()
            val codec = field(encoder, "mEncoder") as? MediaCodec ?: return unavailable()
            val configured = field(encoder, "mCodecFormat") as? MediaFormat ?: return unavailable()
            val mime = configured.getString(MediaFormat.KEY_MIME) ?: return unavailable()
            val width = size(configured, MediaFormat.KEY_WIDTH) ?: return unavailable()
            val height = size(configured, MediaFormat.KEY_HEIGHT) ?: return unavailable()
            val originalWidth = field(encoder, "mWidth") as? Int ?: return unavailable()
            val originalHeight = field(encoder, "mHeight") as? Int ?: return unavailable()
            val grid = field(encoder, "mUseGrid") as? Boolean ?: return unavailable()
            JSONObject()
                .put("status", "CAPTURED")
                .put("source", "ANDROIDX_TEST_REFLECTION")
                .put("codec_name", codec.name)
                .put("configured_mime", mime)
                .put("configured_width", width)
                .put("configured_height", height)
                .put("requested_width", originalWidth)
                .put("requested_height", originalHeight)
                .put("effective_grid", grid)
                .put("pre_stop_output_status", "NOT_READY")
        } catch (_: ReflectiveOperationException) {
            unavailable()
        } catch (_: RuntimeException) {
            unavailable()
        }
    }

    fun pollBeforeStop(writer: Any, trace: JSONObject) {
        if (trace.optString("status") != "CAPTURED") return
        try {
            val encoder = field(writer, "mEncoder") ?: return
            val codec = field(encoder, "mEncoder") as? MediaCodec ?: return
            val output = codec.outputFormat
            val mime = output.getString(MediaFormat.KEY_MIME) ?: return
            val width = size(output, MediaFormat.KEY_WIDTH) ?: return
            val height = size(output, MediaFormat.KEY_HEIGHT) ?: return
            trace.put("pre_stop_output_status", "AVAILABLE")
                .put("pre_stop_output_mime", mime)
                .put("pre_stop_output_width", width)
                .put("pre_stop_output_height", height)
        } catch (_: ReflectiveOperationException) {
            // The callback's format may be inaccessible through the writer.
        } catch (_: RuntimeException) {
            // The codec might not have published its output format yet.
        }
    }
}
