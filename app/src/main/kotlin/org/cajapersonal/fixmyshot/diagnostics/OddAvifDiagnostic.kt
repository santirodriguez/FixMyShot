package org.cajapersonal.fixmyshot.diagnostics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Looper
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.codec.NativeStillEncoder
import org.cajapersonal.fixmyshot.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InterruptedIOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/** Opt-in synthetic geometry comparison, never export qualification or a user-image path. */
internal data class OddAvifMode(val width: Int, val height: Int, val grid: Boolean)
internal data class OddAvifSample(
    val mode: OddAvifMode, val reason: String? = null, val bytes: Long = 0,
    val digest: String? = null, val bitmap: NativeCheckMetrics? = null,
    val image: NativeCheckMetrics? = null,
)
internal data class OddAvifReport(
    val samples: List<OddAvifSample> = emptyList(), val cancelled: Boolean = false,
    val failed: Boolean = false, val identity: NativeCheckReport = NativeCheckReport(),
) {
    fun withAppIdentity(context: Context, cancel: AtomicBoolean) =
        copy(identity = identity.withAppIdentity(context, cancel))

    fun toJson(): String {
        fun metric(m: NativeCheckMetrics?): Any = m?.let {
            JSONObject().put("exact_pixels", it.exactPixels)
                .put("psnr_db", it.psnrDb ?: JSONObject.NULL)
                .put("patch_median_channel_error_max", it.patchError)
        } ?: JSONObject.NULL
        fun safe(s: String) = s.filter { it.code in 32..126 }.take(64)
        return JSONObject().put("schema", 1).put("kind", "ODD_AVIF_NUMERIC_DIAGNOSTIC_V1")
            .put("acceptance", "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION")
            .put("api", identity.api).put("manufacturer", safe(identity.manufacturer))
            .put("model", safe(identity.model)).put("app_version", safe(identity.appVersion))
            .put("version_code", identity.versionCode)
            .put("base_apk_sha256", identity.baseApkSha256 ?: JSONObject.NULL)
            .put("cancelled", cancelled).put("failed", failed)
            .put("source_width", 65).put("source_height", 67)
            .put("samples", JSONArray(samples.map { s ->
                JSONObject().put("width", s.mode.width).put("height", s.mode.height)
                    .put("grid_requested", s.mode.grid)
                    .put("reason", s.reason ?: JSONObject.NULL)
                    .put("encoded_bytes", s.bytes).put("encoded_sha256", s.digest ?: JSONObject.NULL)
                    .put("bitmap_factory", metric(s.bitmap)).put("image_decoder", metric(s.image))
            })).toString(2)
    }
}

internal class OddAvifDiagnostic(
    private val directory: File, private val heap: Long,
    private val encode: (Bitmap, EncodeRequest, AtomicBoolean, Boolean) -> EncodeResult =
        { image, request, cancel, grid -> NativeStillEncoder(directory, heap)
            .encode(image, request, cancel, timeoutMillis = 8_000, gridEnabled = grid) },
) {
    companion object {
        val MODES = listOf(OddAvifMode(65, 67, true), OddAvifMode(65, 67, false),
            OddAvifMode(66, 68, false), OddAvifMode(66, 68, true),
            OddAvifMode(96, 68, false))
        private const val MAX_BYTES = 8_192L
    }

    /** Padding replicates edge pixels; it does not pretend to preserve the output canvas. */
    fun padded(source: Bitmap, mode: OddAvifMode, cancel: AtomicBoolean): Bitmap {
        require(source.width == 65 && source.height == 67 && mode in MODES)
        val result = Bitmap.createBitmap(mode.width, mode.height, Bitmap.Config.ARGB_8888)
        try {
            val row = IntArray(mode.width)
            for (y in 0 until mode.height) {
                if (cancel.get()) throw InterruptedIOException()
                for (x in row.indices) row[x] = source.getPixel(minOf(64, x), minOf(66, y))
                result.setPixels(row, 0, mode.width, 0, y, mode.width, 1)
            }
            result.setHasAlpha(false)
            return result
        } catch (error: Throwable) { result.recycle(); throw error }
    }

    fun run(cancel: AtomicBoolean, onProgress: (Int) -> Unit = {}): OddAvifReport {
        check(Looper.myLooper() != Looper.getMainLooper())
        if (cancel.get()) return OddAvifReport(cancelled = true)
        if (Build.VERSION.SDK_INT < 34)
            return OddAvifReport(MODES.map { OddAvifSample(it, "UNSUPPORTED_API") })
        val samples = mutableListOf<OddAvifSample>()
        var source: Bitmap? = null
        var failed = false
        try {
            source = NativeCodecDiagnostic.fixture(PixelSize(65, 67), cancel)
            for (mode in MODES) {
                if (cancel.get()) break
                val sample = try {
                    val bitmap = padded(source, mode, cancel)
                    try { attempt(source, bitmap, mode, cancel) }
                    finally { bitmap.recycle() }
                } catch (_: InterruptedIOException) {
                    cancel.set(true); OddAvifSample(mode, "CANCELLED")
                } catch (_: OutOfMemoryError) { OddAvifSample(mode, "RESOURCE_LIMIT") }
                catch (_: Exception) { OddAvifSample(mode, "DIAGNOSTIC_FAILED") }
                samples += sample
                onProgress(samples.size)
            }
        } catch (_: InterruptedIOException) { cancel.set(true) }
        catch (_: OutOfMemoryError) { failed = true }
        catch (_: Exception) { failed = true }
        finally { source?.recycle() }
        return OddAvifReport(samples, cancel.get(), failed)
    }

    private fun attempt(source: Bitmap, bitmap: Bitmap, mode: OddAvifMode,
                        cancel: AtomicBoolean): OddAvifSample {
        var owned: File? = null
        try {
            val request = EncodeRequest(PixelSize(mode.width, mode.height), OutputFormat.AVIF, false)
            when (val result = encode(bitmap, request, cancel, mode.grid)) {
                is EncodeResult.Failed -> return OddAvifSample(mode, result.reason.name)
                is EncodeResult.Encoded -> {
                    val file = result.file
                    val canonical = file.canonicalFile
                    if (canonical.parentFile != directory.canonicalFile ||
                        canonical != File(directory.canonicalFile, file.name) ||
                        !file.name.startsWith("native-still-") || file.extension != "avif")
                        return OddAvifSample(mode, "UNOWNED_CANDIDATE")
                    owned = file
                    if (result.route != CodecRoute.NATIVE_AVIF)
                        return OddAvifSample(mode, "WRONG_CODEC_ROUTE")
                    if (cancel.get()) throw InterruptedIOException()
                    if (file.length() !in 16L..MAX_BYTES) return OddAvifSample(mode, "SIZE_LIMIT")
                    val bytes = file.readBytes()
                    if (bytes.size.toLong() != file.length() || bytes.copyOfRange(4, 8)
                        .toString(Charsets.US_ASCII) != "ftyp")
                        return OddAvifSample(mode, "INVALID_OUTPUT")
                    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                        .joinToString("") { "%02x".format(it.toInt() and 255) }
                    val bf = measure(source, mode, cancel) {
                        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
                        })
                    }
                    val id = measure(source, mode, cancel) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                        }
                    }
                    if (cancel.get()) throw InterruptedIOException()
                    return OddAvifSample(mode,
                        if (bf == null && id == null) "PLATFORM_DECODE_UNAVAILABLE" else null,
                        bytes.size.toLong(), digest, bf, id)
                }
            }
        } catch (_: InterruptedIOException) {
            cancel.set(true); return OddAvifSample(mode, "CANCELLED")
        } catch (_: OutOfMemoryError) { return OddAvifSample(mode, "RESOURCE_LIMIT") }
        catch (_: Exception) { return OddAvifSample(mode, "DIAGNOSTIC_FAILED") }
        finally { owned?.delete() }
    }

    private fun measure(source: Bitmap, mode: OddAvifMode, cancel: AtomicBoolean,
                        decode: () -> Bitmap?): NativeCheckMetrics? {
        val decoded = try { decode() } catch (_: Exception) { null } ?: return null
        try {
            if (decoded.width != mode.width || decoded.height != mode.height) return null
            val roi = Bitmap.createBitmap(decoded, 0, 0, 65, 67)
            try {
                val normal = if (roi.config == Bitmap.Config.ARGB_8888) roi
                    else roi.copy(Bitmap.Config.ARGB_8888, false) ?: return null
                try { return NativeCodecDiagnostic.compare(source, normal, cancel) }
                finally { if (normal !== roi) normal.recycle() }
            } finally { roi.recycle() }
        } finally { decoded.recycle() }
    }
}
