package org.cajapersonal.fixmyshot.review

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import org.cajapersonal.fixmyshot.core.DraftImage
import org.cajapersonal.fixmyshot.core.PixelSize
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/** All provider I/O is performed on a dedicated worker; no user path or URI is logged. */
internal class ReviewSources(private val context: Context) {
    private val directory: File get() = File(context.cacheDir, "review-sources")

    enum class Failure { UNREADABLE, UNSUPPORTED, TOO_LARGE, COLOR_UNSUPPORTED }
    sealed interface ImportResult {
        data class Added(val image: DraftImage) : ImportResult
        data class Rejected(val reason: Failure) : ImportResult
    }

    private val maxInputBytes = 100L * 1024 * 1024
    private val maxStagingBytes = 1024L * 1024 * 1024
    private val maxSourcePixels = 64_000_000L
    private val validId = Regex("[0-9a-f]{32}")

    /** Reject untrusted external paths, oversized providers and unsupported decoded formats. */
    fun stage(uri: Uri): ImportResult {
        if (uri.scheme != "content") return ImportResult.Rejected(Failure.UNSUPPORTED)
        if (!directory.isDirectory && !directory.mkdirs()) return ImportResult.Rejected(Failure.UNREADABLE)
        val total = directory.listFiles()?.filter { it.isFile && it.name.endsWith(".source") }
            ?.sumOf { it.length() } ?: 0L
        if (total >= maxStagingBytes) return ImportResult.Rejected(Failure.TOO_LARGE)
        val id = UUID.randomUUID().toString().replace("-", "")
        val target = File(directory, "$id.source")
        var accepted = false
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return ImportResult.Rejected(Failure.UNREADABLE)
            var count = 0L
            input.use { from ->
                FileOutputStream(target).use { to ->
                    val block = ByteArray(16 * 1024)
                    while (true) {
                        val size = from.read(block)
                        if (size < 0) break
                        count += size.toLong()
                        if (count > maxInputBytes || total + count > maxStagingBytes) {
                            return ImportResult.Rejected(Failure.TOO_LARGE)
                        }
                        to.write(block, 0, size)
                    }
                    to.fd.sync()
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            val width = bounds.outWidth
            val height = bounds.outHeight
            if (width <= 0 || height <= 0 || bounds.outMimeType !in setOf("image/png", "image/jpeg", "image/webp")) {
                return ImportResult.Rejected(Failure.UNSUPPORTED)
            }
            if (width.toLong() * height.toLong() > maxSourcePixels) return ImportResult.Rejected(Failure.TOO_LARGE)
            if (bounds.outColorSpace != null && bounds.outColorSpace?.isSrgb != true) return ImportResult.Rejected(Failure.COLOR_UNSUPPORTED)
            if (bounds.outConfig == Bitmap.Config.RGBA_F16) return ImportResult.Rejected(Failure.COLOR_UNSUPPORTED)
            if (bounds.outMimeType == "image/webp" && isAnimatedWebp(target)) {
                return ImportResult.Rejected(Failure.UNSUPPORTED)
            }
            // Android's BitmapFactory leaves JPEG orientation to the caller.
            val orientation = if (bounds.outMimeType == "image/jpeg") {
                ExifInterface(target.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                )
            } else ExifInterface.ORIENTATION_NORMAL
            if (orientation !in 1..8) return ImportResult.Rejected(Failure.UNSUPPORTED)
            val rotated = orientation in setOf(5, 6, 7, 8)
            val size = if (rotated) PixelSize(height, width) else PixelSize(width, height)
            accepted = true
            return ImportResult.Added(DraftImage(id, uri.toString(), size, sourceHasAlpha = bounds.outMimeType != "image/jpeg"))
        } catch (_: IOException) {
            return ImportResult.Rejected(Failure.UNREADABLE)
        } catch (_: SecurityException) {
            return ImportResult.Rejected(Failure.UNREADABLE)
        } catch (_: IllegalArgumentException) {
            return ImportResult.Rejected(Failure.UNSUPPORTED)
        } finally {
            if (!accepted) target.delete()
        }
    }

    /** Caller must close returned Bitmaps by dropping references once no longer displayed. */
    fun decodeSample(id: String, maxLongSide: Int = 1500): Bitmap {
        require(validId.matches(id) && maxLongSide in 64..1600)
        val file = File(directory, "$id.source")
        if (!file.isFile) throw IOException("Staged image is unavailable")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > maxSourcePixels) throw IOException("Invalid source bounds")
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maxLongSide) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
        }
        val original = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IOException("Unable to decode selected image")
        try {
            if (original.colorSpace?.isSrgb != true ||
                (Build.VERSION.SDK_INT >= 34 && original.hasGainmap())) throw IOException("Unsupported HDR or color space")
            val orientation = if (bounds.outMimeType == "image/jpeg") ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                else ExifInterface.ORIENTATION_NORMAL
            if (orientation == ExifInterface.ORIENTATION_NORMAL) return original
            val transform = Matrix().apply {
                when (orientation) {
                    2 -> postScale(-1f, 1f)
                    3 -> postRotate(180f)
                    4 -> postScale(1f, -1f)
                    5 -> { postScale(1f, -1f); postRotate(90f) }
                    6 -> postRotate(90f)
                    7 -> { postScale(-1f, 1f); postRotate(90f) }
                    8 -> postRotate(270f)
                    else -> throw IOException("Unsupported source orientation")
                }
            }
            val oriented = Bitmap.createBitmap(original, 0, 0, original.width, original.height, transform, false)
            if (oriented !== original) original.recycle()
            return oriented
        } catch (error: Exception) {
            if (!original.isRecycled) original.recycle()
            throw error
        }
    }

    fun discard(id: String) {
        if (validId.matches(id)) File(directory, "$id.source").delete()
    }

    fun discardAll(ids: List<String>) = ids.forEach(::discard)

    fun pruneExpired() {
        val now = System.currentTimeMillis()
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".source") && now - file.lastModified() > 24L * 60 * 60 * 1000) {
                file.delete()
            }
        }
    }

    private fun isAnimatedWebp(file: File): Boolean {
        FileInputStream(file).use { input ->
            val header = ByteArray(21)
            if (input.read(header) != header.size) return false
            val webp = String(header, 8, 4, Charsets.US_ASCII) == "WEBP"
            val vp8x = String(header, 12, 4, Charsets.US_ASCII) == "VP8X"
            return webp && vp8x && (header[20].toInt() and 0x02) != 0
        }
    }
}
