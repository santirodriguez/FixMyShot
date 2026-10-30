package org.cajapersonal.fixmyshot.diagnostics

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Looper
import android.util.Base64
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.codec.NativeStillEncoder
import org.cajapersonal.fixmyshot.core.CodecRoute
import org.cajapersonal.fixmyshot.core.EncodeRequest
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.core.PixelSize
import org.json.JSONObject
import java.io.File
import java.io.InterruptedIOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/** A single explicitly requested synthetic AVIF; never reads drafts, picks or user photos. */
internal data class AvifByteSample(
    val reason: String? = null,
    val encodedBytes: Int = 0,
    val encodedSha256: String? = null,
    val encodedBase64: String? = null,
    val cancelled: Boolean = false,
    val identity: NativeCheckReport = NativeCheckReport(),
) {
    val ready: Boolean get() = reason == null && !cancelled &&
        encodedBytes > 0 && encodedSha256 != null && encodedBase64 != null

    fun withAppIdentity(context: Context, stop: AtomicBoolean): AvifByteSample =
        copy(identity = identity.withAppIdentity(context, stop))

    fun toJson(): String {
        fun clean(s: String) = s.filter { it.code in 32..126 }.take(64)
        return JSONObject().put("schema", 1).put("kind", "SYNTHETIC_AVIF_BYTES_V1")
            .put("acceptance", "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION")
            .put("format", "AVIF").put("source_width", 65).put("source_height", 67)
            .put("api", identity.api).put("manufacturer", clean(identity.manufacturer))
            .put("model", clean(identity.model)).put("app_version", clean(identity.appVersion))
            .put("version_code", identity.versionCode)
            .put("base_apk_sha256", identity.baseApkSha256 ?: JSONObject.NULL)
            .put("cancelled", cancelled)
            .put("reason", reason ?: JSONObject.NULL)
            .put("encoded_bytes", encodedBytes)
            .put("encoded_sha256", encodedSha256 ?: JSONObject.NULL)
            .put("encoded_base64", encodedBase64 ?: JSONObject.NULL)
            .toString(2)
    }
}

/** Opt-in 65x67 AVIF sample: bounded private candidate, no gallery or provider writes. */
internal class AvifByteSampler(
    private val directory: File,
    private val heap: Long,
    private val encode: (Bitmap, EncodeRequest, AtomicBoolean) -> EncodeResult =
        { image, request, stop ->
            NativeStillEncoder(directory, heap).encode(
                image, request, stop, timeoutMillis = 8_000, gridEnabled = true)
        },
) {
    companion object {
        const val MAX_SAMPLE_BYTES = 8192
        private val SIZE = PixelSize(65, 67)
    }

    fun run(stop: AtomicBoolean): AvifByteSample {
        check(Looper.myLooper() != Looper.getMainLooper())
        if (stop.get()) return AvifByteSample(reason = "CANCELLED", cancelled = true)
        if (Build.VERSION.SDK_INT < 34) return AvifByteSample(reason = "UNSUPPORTED_API")
        var source: Bitmap? = null
        var owned: File? = null
        try {
            source = NativeCodecDiagnostic.fixture(SIZE, stop)
            val request = EncodeRequest(SIZE, OutputFormat.AVIF, false)
            when (val result = encode(source, request, stop)) {
                is EncodeResult.Failed -> return AvifByteSample(reason = result.reason.name)
                is EncodeResult.Encoded -> {
                    val file = result.file
                    val canonical = file.canonicalFile
                    if (canonical.parentFile != directory.canonicalFile ||
                        canonical != File(directory.canonicalFile, file.name) ||
                        !file.name.startsWith("native-still-") || file.extension != "avif")
                        return AvifByteSample(reason = "UNOWNED_CANDIDATE")
                    owned = file
                    if (result.route != CodecRoute.NATIVE_AVIF)
                        return AvifByteSample(reason = "WRONG_CODEC_ROUTE")
                    if (stop.get()) throw InterruptedIOException()
                    val length = file.length()
                    if (length !in 16L..MAX_SAMPLE_BYTES.toLong())
                        return AvifByteSample(reason = "SIZE_LIMIT")
                    val bytes = file.readBytes()
                    if (stop.get()) throw InterruptedIOException()
                    if (bytes.size.toLong() != length || !isAvif(bytes))
                        return AvifByteSample(reason = "INVALID_OUTPUT")
                    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                        .joinToString("") { "%02x".format(it.toInt() and 255) }
                    return AvifByteSample(encodedBytes = bytes.size, encodedSha256 = digest,
                        encodedBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
            }
        } catch (_: InterruptedIOException) {
            stop.set(true)
            return AvifByteSample(reason = "CANCELLED", cancelled = true)
        } catch (_: OutOfMemoryError) {
            return AvifByteSample(reason = "RESOURCE_LIMIT")
        } catch (_: Exception) {
            return AvifByteSample(reason = "DIAGNOSTIC_FAILED")
        } finally {
            owned?.delete()
            source?.recycle()
        }
    }

    private fun isAvif(bytes: ByteArray): Boolean {
        if (bytes.size < 20 || String(bytes, 4, 4, Charsets.US_ASCII) != "ftyp")
            return false
        val size = ((bytes[0].toLong() and 255) shl 24) or
            ((bytes[1].toLong() and 255) shl 16) or
            ((bytes[2].toLong() and 255) shl 8) or (bytes[3].toLong() and 255)
        if (size < 20 || size > minOf(bytes.size, 4096)) return false
        for (offset in 8 until size.toInt() - 3 step 4) {
            if (offset == 12) continue // minor version, not a brand
            if (String(bytes, offset, 4, Charsets.US_ASCII) == "avif") return true
        }
        return false
    }
}
