package org.cajapersonal.fixmyshot.diagnostics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import java.io.IOException
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10

internal data class NativeCheckSample(
    val format: OutputFormat, val size: PixelSize, val failure: FailureCode? = null,
    val bytes: Long = 0, val elapsedNanos: Long = 0, val metrics: NativeCheckMetrics? = null,
)

internal data class NativeCheckMetrics(val exactPixels: Boolean, val psnrDb: Double?, val patchError: Int) {
    val meetsNumericTargets: Boolean get() = (exactPixels || (psnrDb ?: 0.0) >= 38.0) && patchError <= 2
}

/** Closed report fields only: no photo references, paths, codec exceptions or device identifiers. */
internal data class NativeCheckReport(
    val samples: List<NativeCheckSample> = emptyList(), val cancelled: Boolean = false,
    val failed: Boolean = false, val api: Int = Build.VERSION.SDK_INT,
    val manufacturer: String = Build.MANUFACTURER, val model: String = Build.MODEL,
    val appVersion: String = "", val versionCode: Long = 0, val baseApkSha256: String? = null,
) {
    /** Bind to installed executable bytes without embedding a private installation path. */
    fun withAppIdentity(context: Context, cancelled: AtomicBoolean): NativeCheckReport {
        check(Looper.myLooper() != Looper.getMainLooper())
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val named = copy(appVersion = info.versionName.orEmpty(), versionCode =
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong())
        if (cancelled.get()) return named
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            File(context.applicationInfo.sourceDir).inputStream().use { input ->
                val block = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    if (cancelled.get()) return named
                    val count = input.read(block)
                    if (count < 0) break
                    total += count
                    if (total > 256L * 1024 * 1024) return named
                    digest.update(block, 0, count)
                }
            }
            named.copy(baseApkSha256 = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) })
        } catch (_: Exception) { named }
    }

    fun toJson(): String {
        fun bounded(value: String) = value.filter { it.code in 32..126 }.take(64)
        return JSONObject().put("schema", 1).put("kind", "SYNTHETIC_NATIVE_CHECK_V1")
            .put("acceptance", "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION")
            .put("measurement", "ONE_ADAPTER_ATTEMPT_PER_CASE__NOT_P95_OR_PEAK_MEMORY")
            .put("api", api).put("manufacturer", bounded(manufacturer)).put("model", bounded(model))
            .put("app_version", bounded(appVersion)).put("version_code", versionCode)
            .put("base_apk_sha256", baseApkSha256 ?: JSONObject.NULL)
            .put("cancelled", cancelled).put("failed", failed)
            .put("samples", JSONArray(samples.map { sample ->
                JSONObject().put("format", sample.format.name).put("width", sample.size.width)
                    .put("height", sample.size.height)
                    .put("status", if (sample.failure != null) "UNAVAILABLE" else "ENCODED_AND_REOPENED")
                    .put("reason", sample.failure?.name ?: JSONObject.NULL)
                    .put("bytes", sample.bytes).put("adapter_elapsed_ns", sample.elapsedNanos)
                    .apply { sample.metrics?.let {
                        put("exact_pixels", it.exactPixels).put("psnr_db", it.psnrDb ?: JSONObject.NULL)
                        put("patch_median_channel_error_max", it.patchError)
                        put("meets_numeric_targets", it.meetsNumericTargets)
                    } }
            })).toString(2)
    }
}

/** Explicit foreground operation; never reads originals, changes format policy or writes a provider. */
internal class NativeCodecDiagnostic(
    private val directory: File,
    private val heap: Long,
    private val sizes: List<PixelSize> = SIZES,
    private val encode: (Bitmap, EncodeRequest, AtomicBoolean) -> EncodeResult = { image, request, cancel ->
        NativeStillEncoder(directory, heap).encode(image, request, cancel, timeoutMillis = 8_000)
    },
) {
    companion object {
        val SIZES = listOf(PixelSize(64, 64), PixelSize(65, 67), PixelSize(1344, 2992))
        val FORMATS = listOf(OutputFormat.HEIC, OutputFormat.AVIF)
        val COLORS = intArrayOf(Color.rgb(64, 64, 64), Color.rgb(128, 128, 128),
            Color.rgb(192, 192, 192), Color.rgb(192, 64, 64), Color.rgb(64, 192, 64), Color.rgb(64, 64, 192))
        private val active = AtomicBoolean(false)

        fun fixture(size: PixelSize, cancelled: AtomicBoolean): Bitmap {
            require(size in SIZES)
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            try {
                bitmap.setHasAlpha(false)
                val row = IntArray(size.width)
                for (y in 0 until size.height) {
                    if (cancelled.get()) throw InterruptedIOException()
                    for (x in row.indices) row[x] = when {
                        y < size.height / 2 -> COLORS[(x * COLORS.size / size.width).coerceAtMost(5)]
                        y < size.height * 3 / 4 -> if (y % 16 == 0 || x % 32 == 0) Color.WHITE else Color.rgb(18, 18, 18)
                        x % 32 in 0..1 && y % 16 in 0..10 -> Color.rgb(20, 100, 220)
                        else -> Color.WHITE
                    }
                    bitmap.setPixels(row, 0, size.width, 0, y, size.width, 1)
                }
                return bitmap
            } catch (error: Throwable) { bitmap.recycle(); throw error }
        }

        fun compare(source: Bitmap, decoded: Bitmap, cancelled: AtomicBoolean): NativeCheckMetrics {
            require(source.width == decoded.width && source.height == decoded.height)
            require(source.config == Bitmap.Config.ARGB_8888 && decoded.config == Bitmap.Config.ARGB_8888 &&
                source.colorSpace?.isSrgb == true && decoded.colorSpace?.isSrgb == true)
            val original = IntArray(source.width)
            val actual = IntArray(source.width)
            var squares = 0L
            for (y in 0 until source.height) {
                if (cancelled.get()) throw InterruptedIOException()
                source.getPixels(original, 0, source.width, 0, y, source.width, 1)
                decoded.getPixels(actual, 0, source.width, 0, y, source.width, 1)
                for (x in original.indices) {
                    if (Color.alpha(actual[x]) != 255) throw IOException("Nonopaque native output")
                    var shift = 0
                    while (shift <= 16) {
                        val delta = ((original[x] ushr shift) and 255) - ((actual[x] ushr shift) and 255)
                        squares += delta.toLong() * delta
                        shift += 8
                    }
                }
            }
            var patchError = 0
            for (index in COLORS.indices) {
                val cx = (source.width * (2 * index + 1) / (2 * COLORS.size)).coerceIn(1, source.width - 2)
                val cy = source.height / 4
                for (shift in listOf(0, 8, 16)) {
                    val values = (cy - 1..cy + 1).flatMap { y -> (cx - 1..cx + 1).map { x ->
                        (decoded.getPixel(x, y) ushr shift) and 255
                    } }.sorted()
                    patchError = maxOf(patchError, abs(values[4] - ((COLORS[index] ushr shift) and 255)))
                }
            }
            val mse = squares.toDouble() / (source.width.toLong() * source.height * 3)
            return NativeCheckMetrics(squares == 0L, if (squares == 0L) null else 10 * log10(255.0 * 255 / mse), patchError)
        }
    }

    init { require(sizes.isNotEmpty() && sizes.distinct().size == sizes.size && sizes.all { it in SIZES }) }

    fun run(cancelled: AtomicBoolean, progress: (List<NativeCheckSample>) -> Unit = {}): NativeCheckReport {
        check(Looper.myLooper() != Looper.getMainLooper())
        if (!active.compareAndSet(false, true)) return NativeCheckReport(failed = true)
        val samples = mutableListOf<NativeCheckSample>()
        var failed = false
        try {
            for (size in sizes) {
                if (cancelled.get()) break
                var bitmap: Bitmap? = null
                try {
                    for (format in FORMATS) {
                        if (cancelled.get()) break
                        val request = EncodeRequest(size, format, false)
                        val eligibility = CodecCapabilities.assess(Build.VERSION.SDK_INT, request)
                        val refusal = when {
                            eligibility is CodecEligibility.Unavailable -> eligibility.reason
                            !CodecMemoryBudget.permits(request, heap) -> FailureCode.RESOURCE_LIMIT
                            else -> null
                        }
                        val sample = if (refusal != null) NativeCheckSample(format, size, refusal) else {
                            val source = bitmap ?: fixture(size, cancelled).also { bitmap = it }
                            attempt(source, request, cancelled)
                        }
                        samples += sample
                        progress(samples.toList())
                    }
                } finally { bitmap?.recycle() }
            }
        } catch (_: InterruptedIOException) { cancelled.set(true) }
        catch (_: OutOfMemoryError) { failed = true }
        catch (_: Exception) { failed = true }
        finally { active.set(false) }
        return NativeCheckReport(samples.toList(), cancelled.get(), failed)
    }

    private fun attempt(source: Bitmap, request: EncodeRequest, cancelled: AtomicBoolean): NativeCheckSample {
        var owned: File? = null
        return try {
            when (val result = encode(source, request, cancelled)) {
                is EncodeResult.Failed -> NativeCheckSample(request.format, request.size, result.reason)
                is EncodeResult.Encoded -> {
                    val file = result.file
                    if (file.canonicalFile.parentFile != directory.canonicalFile ||
                        file.canonicalFile != File(directory.canonicalFile, file.name) || !file.name.startsWith("native-still-")) {
                        return NativeCheckSample(request.format, request.size, FailureCode.INVALID_OUTPUT)
                    }
                    owned = file
                    val route = if (request.format == OutputFormat.HEIC) CodecRoute.NATIVE_HEIC else CodecRoute.NATIVE_AVIF
                    if (cancelled.get()) throw InterruptedIOException()
                    if (result.route != route || file.length() !in 1L..128L * 1024 * 1024 || !container(file, request.format)) {
                        return NativeCheckSample(request.format, request.size, FailureCode.INVALID_OUTPUT)
                    }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, bounds)
                    val mimeValid = if (request.format == OutputFormat.HEIC)
                        bounds.outMimeType in setOf("image/heic", "image/heif") else bounds.outMimeType == "image/avif"
                    if (bounds.outWidth != request.size.width || bounds.outHeight != request.size.height || !mimeValid) {
                        return NativeCheckSample(request.format, request.size, FailureCode.INVALID_OUTPUT)
                    }
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
                    }
                    val decoded = BitmapFactory.decodeFile(file.path, options) ?: if (Build.VERSION.SDK_INT >= 28) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                        }
                    } else null
                    if (decoded == null) return NativeCheckSample(request.format, request.size, FailureCode.INVALID_OUTPUT)
                    try {
                        if (decoded.width != source.width || decoded.height != source.height ||
                            decoded.config != Bitmap.Config.ARGB_8888 || decoded.colorSpace?.isSrgb != true) {
                            return NativeCheckSample(request.format, request.size, FailureCode.INVALID_OUTPUT)
                        }
                        NativeCheckSample(request.format, request.size, bytes = file.length(),
                            elapsedNanos = result.elapsedNanos, metrics = compare(source, decoded, cancelled))
                    } finally { decoded.recycle() }
                }
            }
        } catch (_: InterruptedIOException) {
            cancelled.set(true)
            NativeCheckSample(request.format, request.size, FailureCode.CANCELLED)
        } catch (_: OutOfMemoryError) {
            NativeCheckSample(request.format, request.size, FailureCode.RESOURCE_LIMIT)
        } catch (_: Exception) {
            NativeCheckSample(request.format, request.size, FailureCode.INVALID_OUTPUT)
        } finally { owned?.delete() }
    }

    /** Read only the bounded ftyp brand set independently of the adapter's extension. */
    private fun container(file: File, format: OutputFormat): Boolean = RandomAccessFile(file, "r").use { input ->
        if (input.length() < 16) return@use false
        val length = input.readInt().toLong() and 0xffffffffL
        fun tag() = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
        if (tag() != "ftyp" || length !in 16L..minOf(4096L, input.length()) || (length - 16) % 4 != 0L) return@use false
        val brands = mutableSetOf(tag())
        input.readInt()
        while (input.filePointer < length) brands += tag()
        if (format == OutputFormat.AVIF) "avif" in brands
        else brands.any { it in setOf("heic", "heix", "hevc", "hevx", "heim", "heis") }
    }
}
