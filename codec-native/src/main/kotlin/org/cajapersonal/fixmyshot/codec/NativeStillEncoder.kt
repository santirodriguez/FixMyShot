package org.cajapersonal.fixmyshot.codec

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Looper
import androidx.heifwriter.AvifWriter
import androidx.heifwriter.HeifWriter
import org.cajapersonal.fixmyshot.core.*
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Device-dependent, single-still HEIC/AVIF feasibility adapter.
 * Maximum quality is lossy; API eligibility never promises device support.
 * Input pixels must already be oriented, opaque, SDR and sRGB.
 */
class NativeStillEncoder(private val privateDirectory: File, private val processHeapLimitBytes: Long) {
    @Synchronized
    fun encode(
        bitmap: Bitmap,
        request: EncodeRequest,
        cancelled: AtomicBoolean = AtomicBoolean(false),
        timeoutMillis: Long = 30_000,
        gridEnabled: Boolean = true,
    ): EncodeResult {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Encoding must run off the UI thread" }
        require(timeoutMillis in 1..120_000L)
        val eligibility = CodecCapabilities.assess(Build.VERSION.SDK_INT, request)
        if (eligibility is CodecEligibility.Unavailable) return EncodeResult.Failed(eligibility.reason)
        val route = (eligibility as CodecEligibility.ProbeRequired).route
        if (route != CodecRoute.NATIVE_HEIC && route != CodecRoute.NATIVE_AVIF) {
            return EncodeResult.Failed(FailureCode.INVALID_INPUT)
        }
        if (bitmap.isRecycled || bitmap.width != request.size.width || bitmap.height != request.size.height ||
            bitmap.config != Bitmap.Config.ARGB_8888 || bitmap.hasAlpha() != request.hasAlpha) {
            return EncodeResult.Failed(FailureCode.INVALID_INPUT)
        }
        if (bitmap.colorSpace?.isSrgb != true) return EncodeResult.Failed(FailureCode.COLOR_CONVERSION_REQUIRED)
        if (!CodecMemoryBudget.permits(request, processHeapLimitBytes)) return EncodeResult.Failed(FailureCode.RESOURCE_LIMIT)
        if (cancelled.get()) return EncodeResult.Failed(FailureCode.CANCELLED)

        val started = System.nanoTime()
        val deadline = started + timeoutMillis * 1_000_000L
        var candidate: File? = null
        var retained = false
        return try {
            if (!privateDirectory.isDirectory && !privateDirectory.mkdirs()) throw IOException("Private output unavailable")
            val file = File.createTempFile("native-still-", ".${request.format.extension}", privateDirectory)
            candidate = file
            if (cancelled.get()) return EncodeResult.Failed(FailureCode.CANCELLED)
            val remainingMillis = (deadline - System.nanoTime()) / 1_000_000L
            if (remainingMillis <= 0) return EncodeResult.Failed(FailureCode.TIMEOUT)
            when (route) {
                CodecRoute.NATIVE_HEIC -> {
                    HeifWriter.Builder(file.absolutePath, request.size.width, request.size.height, HeifWriter.INPUT_MODE_BITMAP)
                        .setMaxImages(1).setQuality(100).setGridEnabled(gridEnabled).build().use { writer ->
                            writer.start()
                            writer.addBitmap(bitmap)
                            writer.stop(remainingMillis)
                        }
                }
                CodecRoute.NATIVE_AVIF -> {
                    AvifWriter.Builder(file.absolutePath, request.size.width, request.size.height, AvifWriter.INPUT_MODE_BITMAP)
                        .setMaxImages(1).setQuality(100).setGridEnabled(gridEnabled).build().use { writer ->
                            writer.start()
                            writer.addBitmap(bitmap)
                            writer.stop(remainingMillis)
                        }
                }
                else -> return EncodeResult.Failed(FailureCode.NOT_INTEGRATED)
            }
            if (cancelled.get()) return EncodeResult.Failed(FailureCode.CANCELLED)
            if (System.nanoTime() >= deadline) return EncodeResult.Failed(FailureCode.TIMEOUT)
            if (!isActualStillContainer(file, route)) return EncodeResult.Failed(FailureCode.INVALID_OUTPUT)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val mimeValid = when (route) {
                CodecRoute.NATIVE_HEIC -> bounds.outMimeType in setOf("image/heic", "image/heif")
                CodecRoute.NATIVE_AVIF -> bounds.outMimeType == "image/avif"
                else -> false
            }
            if (file.length() == 0L || bounds.outWidth != request.size.width ||
                bounds.outHeight != request.size.height || !mimeValid) {
                return EncodeResult.Failed(FailureCode.INVALID_OUTPUT)
            }
            // Header/dimension parsing may succeed even if the device cannot decode actual pixels.
            // Use a bounded sampled decode for large images; full-size pixel fidelity is a separate gate.
            var sampleSize = 1
            while (maxOf(request.size.width, request.size.height) / sampleSize > 512 &&
                sampleSize < 1_048_576) {
                sampleSize *= 2
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }) ?: if (Build.VERSION.SDK_INT >= 28) {
                PlatformStillDecoder.decode(file, sampleSize)
            } else null
            if (decoded == null) return EncodeResult.Failed(FailureCode.INVALID_OUTPUT)
            try {
                if (decoded.width <= 0 || decoded.height <= 0) {
                    return EncodeResult.Failed(FailureCode.INVALID_OUTPUT)
                }
            } finally {
                decoded.recycle()
            }
            if (System.nanoTime() >= deadline) return EncodeResult.Failed(FailureCode.TIMEOUT)
            retained = true
            EncodeResult.Encoded(file, route, System.nanoTime() - started)
        } catch (_: TimeoutException) {
            EncodeResult.Failed(FailureCode.TIMEOUT)
        } catch (_: IOException) {
            EncodeResult.Failed(FailureCode.IO_ERROR)
        } catch (_: IllegalStateException) {
            EncodeResult.Failed(FailureCode.ENCODE_FAILED)
        } catch (_: IllegalArgumentException) {
            EncodeResult.Failed(FailureCode.ENCODE_FAILED)
        } catch (_: UnsupportedOperationException) {
            EncodeResult.Failed(FailureCode.NATIVE_UNAVAILABLE)
        } finally {
            if (!retained) candidate?.delete()
        }
    }

    /** Check the actual encoded ISO BMFF brand, not the requested extension. */
    private fun isActualStillContainer(file: File, route: CodecRoute): Boolean =
        RandomAccessFile(file, "r").use { input ->
            if (input.length() < 16) return@use false
            val boxLength = input.readInt().toLong() and 0xffffffffL
            val fourcc = ByteArray(4)
            input.readFully(fourcc)
            if (fourcc.toString(Charsets.US_ASCII) != "ftyp" ||
                boxLength < 16 || boxLength > minOf(4096L, input.length()) ||
                (boxLength - 16) % 4 != 0L) return@use false
            val brands = mutableSetOf<String>()
            input.readFully(fourcc); brands.add(fourcc.toString(Charsets.US_ASCII))
            input.readInt() // minor version
            while (input.filePointer + 4 <= boxLength) {
                input.readFully(fourcc)
                brands.add(fourcc.toString(Charsets.US_ASCII))
            }
            when (route) {
                CodecRoute.NATIVE_HEIC -> brands.any { it in setOf("heic", "heix", "hevc", "hevx", "heim", "heis") }
                CodecRoute.NATIVE_AVIF -> "avif" in brands
                else -> false
            }
        }
}
