package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.cajapersonal.fixmyshot.core.EncodeRequest
import org.cajapersonal.fixmyshot.core.OutputFormat
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/** Checks the generated container and its *native* decoded color space, without coercing to sRGB. */
internal object PrivateCandidateVerifier {
    private const val MAX_FILE_BYTES = 128L * 1024L * 1024L

    fun accepts(file: File, request: EncodeRequest): Boolean {
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
                    reopened.hasAlpha() == request.hasAlpha
            } finally { reopened.recycle() }
        } catch (_: IOException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
