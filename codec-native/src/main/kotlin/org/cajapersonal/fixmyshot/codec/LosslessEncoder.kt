package org.cajapersonal.fixmyshot.codec

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Looper
import android.os.ParcelFileDescriptor
import org.cajapersonal.fixmyshot.core.*
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicBoolean

sealed interface EncodeResult {
    /** An app-private candidate, not a published user export. Caller owns deletion. */
    data class Encoded(val file: File, val route: CodecRoute, val elapsedNanos: Long) : EncodeResult
    data class Failed(val reason: FailureCode) : EncodeResult
}

/** Serialized, blocking codec boundary. Never call on the UI thread. */
class LosslessEncoder(private val privateDirectory: File, private val processHeapLimitBytes: Long) {
    @Synchronized
    fun encode(bitmap: Bitmap, request: EncodeRequest, cancelled: AtomicBoolean = AtomicBoolean(false), timeoutMillis: Long = 30_000): EncodeResult {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Encoding must run off the UI thread" }
        require(timeoutMillis in 1..120_000)
        // This boundary receives pixels only; the source-aware private pipeline owns preservation.
        if (request.metadataPolicy != MetadataPolicy.REMOVE_IDENTIFYING) {
            return EncodeResult.Failed(FailureCode.METADATA_POLICY_UNAVAILABLE)
        }
        val eligibility = CodecCapabilities.assess(Build.VERSION.SDK_INT, request)
        if (eligibility is CodecEligibility.Unavailable) return EncodeResult.Failed(eligibility.reason)
        val route = (eligibility as CodecEligibility.ProbeRequired).route
        if (bitmap.isRecycled || bitmap.width != request.size.width || bitmap.height != request.size.height ||
            bitmap.config != Bitmap.Config.ARGB_8888 || bitmap.hasAlpha() != request.hasAlpha) {
            return EncodeResult.Failed(FailureCode.INVALID_INPUT)
        }
        if (bitmap.colorSpace?.isSrgb != true) return EncodeResult.Failed(FailureCode.COLOR_CONVERSION_REQUIRED)
        if (!CodecMemoryBudget.permits(request, processHeapLimitBytes)) return EncodeResult.Failed(FailureCode.RESOURCE_LIMIT)
        if (cancelled.get()) return EncodeResult.Failed(FailureCode.CANCELLED)
        val started = System.nanoTime()
        val deadline = started + timeoutMillis * 1_000_000
        var output: File? = null
        var retained = false
        return try {
            if (!privateDirectory.isDirectory && !privateDirectory.mkdirs()) throw IOException("Private output directory unavailable")
            val file = File.createTempFile("codec-", ".${request.format.extension}", privateDirectory)
            output = file
            if (route == CodecRoute.LIBWEBP) {
                NativeWebp.ensureLoaded()
                val code = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE).use {
                    NativeWebp.encode(bitmap, it.fd, cancelled, timeoutMillis, bitmap.isPremultiplied)
                }
                if (code != 0) return EncodeResult.Failed(when (code) {
                    2 -> FailureCode.CANCELLED
                    3 -> FailureCode.TIMEOUT
                    4 -> FailureCode.IO_ERROR
                    5 -> FailureCode.RESOURCE_LIMIT
                    else -> FailureCode.ENCODE_FAILED
                })
            } else {
                FileOutputStream(file).use { stream ->
                    val bounded = object : FilterOutputStream(stream) {
                        private fun checkStopped() {
                            if (cancelled.get() || System.nanoTime() >= deadline) throw InterruptedIOException("Encoding stopped")
                        }
                        override fun write(value: Int) { checkStopped(); out.write(value) }
                        override fun write(bytes: ByteArray, offset: Int, length: Int) { checkStopped(); out.write(bytes, offset, length) }
                    }
                    val format = when (route) {
                        CodecRoute.ANDROID_PNG -> Bitmap.CompressFormat.PNG
                        CodecRoute.ANDROID_WEBP_LOSSLESS -> {
                            if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSLESS
                            else return EncodeResult.Failed(FailureCode.UNSUPPORTED_API)
                        }
                        else -> return EncodeResult.Failed(FailureCode.NOT_INTEGRATED)
                    }
                    if (!bitmap.compress(format, 100, bounded)) return EncodeResult.Failed(when {
                        cancelled.get() -> FailureCode.CANCELLED
                        System.nanoTime() >= deadline -> FailureCode.TIMEOUT
                        else -> FailureCode.ENCODE_FAILED
                    })
                    stream.fd.sync()
                }
            }
            if (cancelled.get()) return EncodeResult.Failed(FailureCode.CANCELLED)
            if (System.nanoTime() >= deadline) return EncodeResult.Failed(FailureCode.TIMEOUT)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (file.length() == 0L || bounds.outWidth != request.size.width || bounds.outHeight != request.size.height ||
                bounds.outMimeType != request.format.mimeType) return EncodeResult.Failed(FailureCode.INVALID_OUTPUT)
            retained = true
            EncodeResult.Encoded(file, route, System.nanoTime() - started)
        } catch (_: InterruptedIOException) {
            EncodeResult.Failed(if (cancelled.get()) FailureCode.CANCELLED else FailureCode.TIMEOUT)
        } catch (_: UnsatisfiedLinkError) {
            EncodeResult.Failed(FailureCode.NATIVE_UNAVAILABLE)
        } catch (_: IOException) {
            EncodeResult.Failed(FailureCode.IO_ERROR)
        } catch (_: IllegalArgumentException) {
            EncodeResult.Failed(FailureCode.ENCODE_FAILED)
        } finally {
            if (!retained) output?.delete()
        }
    }
}
