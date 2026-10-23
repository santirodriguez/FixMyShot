package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.cajapersonal.fixmyshot.core.EncodeRequest
import org.cajapersonal.fixmyshot.core.OutputFormat
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicBoolean

/** Checks the generated container and its *native* decoded color space, without coercing to sRGB. */
internal object PrivateCandidateVerifier {
    private const val MAX_FILE_BYTES = 128L * 1024L * 1024L

    fun accepts(file: File, request: EncodeRequest, opaqueWebpProven: Boolean = false): Boolean {
        if (opaqueWebpProven && (request.format != OutputFormat.WEBP_LOSSLESS || !request.hasAlpha)) return false
        if (!file.isFile || file.length() !in 1L..MAX_FILE_BYTES) return false
        return try {
            val goodContainer = RandomAccessFile(file, "r").use { input ->
                when (request.format) {
                    OutputFormat.PNG -> PrivatePngStructure.accepts(input, request)
                    OutputFormat.WEBP_LOSSLESS -> PrivateWebpStructure.accepts(input, request)
                    else -> false
                }
            }
            if (!goodContainer) return false
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth != request.size.width || bounds.outHeight != request.size.height ||
                bounds.outMimeType != request.format.mimeType) return false
            var sample = 1
            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 512) sample *= 2
            val reopened = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }) ?: return false
            try {
                reopened.width > 0 && reopened.height > 0 && reopened.colorSpace?.isSrgb == true &&
                    (reopened.hasAlpha() == request.hasAlpha || (opaqueWebpProven && !reopened.hasAlpha()))
            } finally { reopened.recycle() }
        } catch (_: IOException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    /** Only the already-rendered full canvas can prove unused alpha. No preview/sampling proof. */
    fun isFullyOpaque(bitmap: Bitmap, cancelled: AtomicBoolean = AtomicBoolean(false)): Boolean {
        require(!bitmap.isRecycled && bitmap.config == Bitmap.Config.ARGB_8888)
        if (!bitmap.hasAlpha()) return true
        val buffer = IntArray(4096)
        val tileWidth = minOf(bitmap.width, buffer.size)
        val tileHeight = maxOf(1, buffer.size / tileWidth)
        val deadline = System.nanoTime() + 30_000_000_000L
        for (y in 0 until bitmap.height step tileHeight) {
            for (x in 0 until bitmap.width step tileWidth) {
                if (cancelled.get() || System.nanoTime() >= deadline) throw InterruptedIOException("Alpha inspection stopped")
                val width = minOf(tileWidth, bitmap.width - x)
                val height = minOf(tileHeight, bitmap.height - y)
                bitmap.getPixels(buffer, 0, width, x, y, width, height)
                for (i in 0 until width * height) if (buffer[i] ushr 24 != 255) return false
            }
        }
        return true
    }
}
