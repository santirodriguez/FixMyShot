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

/** One explicitly requested native synthetic sample; never reads drafts, picks or user photos. */
internal data class AvifByteSample(
    val reason: String? = null,
    val encodedBytes: Int = 0,
    val encodedSha256: String? = null,
    val encodedBase64: String? = null,
    val cancelled: Boolean = false,
    val identity: NativeCheckReport = NativeCheckReport(),
    val format: OutputFormat = OutputFormat.AVIF,
) {
    init { require(format in setOf(OutputFormat.AVIF, OutputFormat.HEIC)) }

    val ready: Boolean get() = reason == null && !cancelled &&
        encodedBytes > 0 && encodedSha256 != null && encodedBase64 != null

    fun withAppIdentity(context: Context, stop: AtomicBoolean): AvifByteSample =
        copy(identity = identity.withAppIdentity(context, stop))

    fun toJson(): String {
        fun clean(s: String) = s.filter { it.code in 32..126 }.take(64)
        return JSONObject().put("schema", 1).put("kind", "SYNTHETIC_${format.name}_BYTES_V1")
            .put("acceptance", "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION")
            .put("format", format.name).put("source_width", 65).put("source_height", 67)
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

/** Shared opt-in 65x67 sampler; AVIF remains the default for the existing diagnostic. */
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

    fun run(stop: AtomicBoolean, format: OutputFormat = OutputFormat.AVIF): AvifByteSample {
        require(format in setOf(OutputFormat.AVIF, OutputFormat.HEIC))
        check(Looper.myLooper() != Looper.getMainLooper())
        if (stop.get()) return AvifByteSample(format = format, reason = "CANCELLED", cancelled = true)
        if (Build.VERSION.SDK_INT < (if (format == OutputFormat.HEIC) 29 else 34)) return AvifByteSample(format = format, reason = "UNSUPPORTED_API")
        var source: Bitmap? = null
        var owned: File? = null
        try {
            source = NativeCodecDiagnostic.fixture(SIZE, stop)
            val request = EncodeRequest(SIZE, format, false)
            when (val result = encode(source, request, stop)) {
                is EncodeResult.Failed -> return AvifByteSample(format = format, reason = result.reason.name)
                is EncodeResult.Encoded -> {
                    val file = result.file
                    val canonical = file.canonicalFile
                    if (canonical.parentFile != directory.canonicalFile ||
                        canonical != File(directory.canonicalFile, file.name) ||
                        !file.name.startsWith("native-still-") || file.extension != format.extension)
                        return AvifByteSample(format = format, reason = "UNOWNED_CANDIDATE")
                    owned = file
                    if (result.route != (if (format == OutputFormat.HEIC) CodecRoute.NATIVE_HEIC else CodecRoute.NATIVE_AVIF))
                        return AvifByteSample(format = format, reason = "WRONG_CODEC_ROUTE")
                    if (stop.get()) throw InterruptedIOException()
                    val length = file.length()
                    if (length !in 16L..MAX_SAMPLE_BYTES.toLong())
                        return AvifByteSample(format = format, reason = "SIZE_LIMIT")
                    val bytes = file.readBytes()
                    if (stop.get()) throw InterruptedIOException()
                    if (bytes.size.toLong() != length || !isExpectedContainer(bytes, format))
                        return AvifByteSample(format = format, reason = "INVALID_OUTPUT")
                    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                        .joinToString("") { "%02x".format(it.toInt() and 255) }
                    return AvifByteSample(format = format, encodedBytes = bytes.size, encodedSha256 = digest,
                        encodedBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
            }
        } catch (_: InterruptedIOException) {
            stop.set(true)
            return AvifByteSample(format = format, reason = "CANCELLED", cancelled = true)
        } catch (_: OutOfMemoryError) {
            return AvifByteSample(format = format, reason = "RESOURCE_LIMIT")
        } catch (_: Exception) {
            return AvifByteSample(format = format, reason = "DIAGNOSTIC_FAILED")
        } finally {
            owned?.delete()
            source?.recycle()
        }
    }

    private fun isExpectedContainer(bytes: ByteArray, format: OutputFormat): Boolean {
        if (bytes.size < 20 || String(bytes, 4, 4, Charsets.US_ASCII) != "ftyp")
            return false
        val size = ((bytes[0].toLong() and 255) shl 24) or
            ((bytes[1].toLong() and 255) shl 16) or
            ((bytes[2].toLong() and 255) shl 8) or (bytes[3].toLong() and 255)
        if (size < 20 || size > minOf(bytes.size, 4096) || size % 4 != 0L) return false
        val brands = mutableSetOf<String>()
        for (offset in 8 until size.toInt() - 3 step 4) {
            if (offset == 12) continue // minor version, not a brand
            brands.add(String(bytes, offset, 4, Charsets.US_ASCII))
        }
        return if (format == OutputFormat.AVIF) "avif" in brands else
            "avif" !in brands && brands.any { it in setOf("heic", "heix", "hevc", "hevx", "heim", "heis") }
    }
}
