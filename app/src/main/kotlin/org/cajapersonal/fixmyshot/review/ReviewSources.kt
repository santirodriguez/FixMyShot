package org.cajapersonal.fixmyshot.review

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.StatFs
import android.graphics.ColorSpace
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import org.cajapersonal.fixmyshot.core.DraftImage
import org.cajapersonal.fixmyshot.core.PixelSize
import org.cajapersonal.fixmyshot.core.SourceColor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import org.cajapersonal.fixmyshot.core.PrivateOutputRecipe
import org.cajapersonal.fixmyshot.core.MetadataPolicy
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

/** All provider I/O is performed on a dedicated worker; no user path or URI is logged. */
internal class ReviewSources(private val context: Context,
    private val availableBytes: (File) -> Long? = { directory ->
        try { StatFs(directory.absolutePath).availableBytes } catch (_: RuntimeException) { null }
    }) {
    private val directory: File get() = File(context.cacheDir, "review-sources")

    enum class Failure { UNREADABLE, UNSUPPORTED, TOO_LARGE, LOW_STORAGE, COLOR_UNSUPPORTED, MALFORMED, ANIMATED, HIGH_PRECISION, HDR_UNSUPPORTED }
    sealed interface ImportResult {
        data class Added(val image: DraftImage) : ImportResult
        data class Rejected(val reason: Failure) : ImportResult
    }

    private val maxInputBytes = 100L * 1024 * 1024
    private val maxStagingBytes = 1024L * 1024 * 1024
    private val maxSourcePixels = 64_000_000L
    private val storageHeadroom = 16L * 1024 * 1024
    private val storageCheckInterval = 1024L * 1024
    private val validId = Regex("[0-9a-f]{32}")

    /** Advisory only: another writer can consume space after any check; actual writes still fail typed. */
    private fun hasStagingSpace(): Boolean {
        val available = availableBytes(directory) ?: return true
        return available < 0 || available >= storageHeadroom + storageCheckInterval
    }

    /** Reject untrusted external paths, oversized providers and unsupported decoded formats. */
    fun stage(uri: Uri): ImportResult {
        if (uri.scheme != "content") return ImportResult.Rejected(Failure.UNSUPPORTED)
        var privateCopy: File? = null
        var accepted = false
        try {
            if (!directory.isDirectory && !directory.mkdirs()) return ImportResult.Rejected(Failure.UNREADABLE)
            val total = directory.listFiles()?.filter { it.isFile && it.name.endsWith(".source") }
                ?.sumOf { it.length() } ?: 0L
            if (total >= maxStagingBytes) return ImportResult.Rejected(Failure.TOO_LARGE)
            if (!hasStagingSpace()) return ImportResult.Rejected(Failure.LOW_STORAGE)
            val id = UUID.randomUUID().toString().replace("-", "")
            val target = File(directory, "$id.source")
            privateCopy = target
            val digest = MessageDigest.getInstance("SHA-256")
            val input = context.contentResolver.openInputStream(uri) ?: return ImportResult.Rejected(Failure.UNREADABLE)
            var count = 0L
            var nextStorageCheck = storageCheckInterval
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
                        if (count >= nextStorageCheck) {
                            if (!hasStagingSpace()) return ImportResult.Rejected(Failure.LOW_STORAGE)
                            nextStorageCheck = count + storageCheckInterval
                        }
                        digest.update(block, 0, size)
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
            val color = try { SourceAppearance.inspect(target, bounds) }
                catch (error: SourceAppearance.Rejected) { return ImportResult.Rejected(error.reason) }
                catch (_: IOException) { return ImportResult.Rejected(Failure.MALFORMED) }
            if (bounds.outMimeType == "image/png" && hasUnsupportedPngFeatures(target)) {
                return ImportResult.Rejected(Failure.UNSUPPORTED)
            }
            if (bounds.outMimeType == "image/webp" && isAnimatedWebp(target)) {
                return ImportResult.Rejected(Failure.UNSUPPORTED)
            }
            // Android's BitmapFactory leaves JPEG orientation to the caller.
            val orientation = readOrientation(target, bounds.outMimeType)
            if (orientation !in 1..8) return ImportResult.Rejected(Failure.UNSUPPORTED)
            val rotated = orientation in setOf(5, 6, 7, 8)
            val size = if (rotated) PixelSize(height, width) else PixelSize(width, height)
            val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val result = ImportResult.Added(DraftImage(id, uri.toString(), size,
                sourceHasAlpha = bounds.outMimeType != "image/jpeg", contentSha256 = hash, sourceColor = color))
            accepted = true
            return result
        } catch (_: IOException) {
            return ImportResult.Rejected(Failure.UNREADABLE)
        } catch (_: SecurityException) {
            return ImportResult.Rejected(Failure.UNREADABLE)
        } catch (_: IllegalArgumentException) {
            return ImportResult.Rejected(Failure.UNSUPPORTED)
        } catch (_: RuntimeException) {
            // Provider/runtime failures remain typed; never leak exception text or strand the queue.
            return ImportResult.Rejected(Failure.UNREADABLE)
        } finally {
            if (!accepted) try { privateCopy?.delete() } catch (_: SecurityException) { /* Keep failure typed. */ }
        }
    }

    /** Only privately staged bytes matching the frozen digest may enter output preparation. */
    fun matchesFingerprint(id: String, expectedSha256: String): Boolean {
        if (!validId.matches(id) || !Regex("[0-9a-f]{64}").matches(expectedSha256)) return false
        val file = File(directory, "$id.source")
        if (!file.isFile || file.length() !in 1L..maxInputBytes) return false
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val block = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(block)
                    if (read < 0) break
                    if (read > 0) digest.update(block, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } == expectedSha256
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    /** Full pixels, not a sampled UI preview; callers must preflight memory first. */
    fun decodeOriginal(id: String, expectedOriented: PixelSize, expectedColor: SourceColor = SourceColor.SRGB): Bitmap {
        val original = decodeNormalized(id, null, expectedColor)
        if (original.width != expectedOriented.width || original.height != expectedOriented.height) {
            original.recycle()
            throw IOException("Staged source dimensions changed")
        }
        return original
    }

    /** Caller must drop references to old UI bitmaps without recycling visible snapshots. */
    fun decodeSample(id: String, maxLongSide: Int = 1500): Bitmap =
        decodeNormalized(id, maxLongSide)

    private fun decodeNormalized(id: String, maxLongSide: Int?, expectedColor: SourceColor? = null): Bitmap {
        require(validId.matches(id) && (maxLongSide == null || maxLongSide in 64..1600))
        val file = File(directory, "$id.source")
        if (!file.isFile) throw IOException("Staged image is unavailable")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > maxSourcePixels) throw IOException("Invalid source bounds")
        val color = SourceAppearance.inspect(file, bounds)
        if (expectedColor != null && color != expectedColor) throw IOException("Staged source appearance changed")
        if ((bounds.outMimeType == "image/png" && hasUnsupportedPngFeatures(file)) ||
            (bounds.outMimeType == "image/webp" && isAnimatedWebp(file))) throw IOException("Unsupported static source")
        var sample = 1
        if (maxLongSide != null) {
            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maxLongSide) sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
        }
        val original = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IOException("Unable to decode selected image")
        try {
            if (original.config != Bitmap.Config.ARGB_8888 ||
                original.colorSpace?.isSrgb != true ||
                (Build.VERSION.SDK_INT >= 34 && original.hasGainmap())) {
                throw IOException("Unsupported HDR, color space or decoder output")
            }
            val orientation = readOrientation(file, bounds.outMimeType)
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

    /** The stream constructor avoids API26 filename-based limitations for .source files. */
    private fun readOrientation(file: File, mime: String?): Int {
        if (mime !in setOf("image/jpeg", "image/png", "image/webp")) return ExifInterface.ORIENTATION_NORMAL
        return FileInputStream(file).use { input ->
            val orientation = ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            // AndroidX can synthesize UNDEFINED for a container with no orientation tag.
            // Unspecified orientation is identity; other out-of-range values stay invalid.
            if (orientation == ExifInterface.ORIENTATION_UNDEFINED) ExifInterface.ORIENTATION_NORMAL else orientation
        }
    }

    fun sourceForMetadata(id: String): File {
        require(validId.matches(id))
        return File(directory, "$id.source").also { require(it.isFile) }
    }

    /** Only unchanged pixels/appearance can bypass the renderer. Candidate validation stays external. */
    fun copyPayload(recipe: PrivateOutputRecipe, target: File, cancelled: AtomicBoolean): Boolean {
        if (!validId.matches(recipe.sourceId) || recipe.crop != null || recipe.sourceColor != SourceColor.SRGB ||
            recipe.request.metadataPolicy != MetadataPolicy.REMOVE_IDENTIFYING ||
            recipe.canvas != recipe.orientedSource || recipe.placement.left != 0 || recipe.placement.top != 0 ||
            recipe.placement.width != recipe.canvas.width || recipe.placement.height != recipe.canvas.height) return false
        val file = File(directory, "${recipe.sourceId}.source")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if ((bounds.outMimeType == "image/png" && hasUnsupportedPngFeatures(file)) ||
            (bounds.outMimeType == "image/webp" && isAnimatedWebp(file))) return false
        if (bounds.outMimeType != recipe.request.format.mimeType ||
            bounds.outWidth != recipe.canvas.width || bounds.outHeight != recipe.canvas.height ||
            bounds.outColorSpace?.isSrgb != true || bounds.outConfig == Bitmap.Config.RGBA_F16 ||
            readOrientation(file, bounds.outMimeType) != ExifInterface.ORIENTATION_NORMAL) return false
        return PayloadPreservingCopy.write(file, target, recipe.request, cancelled)
    }

    /** Do not flatten APNG or silently clip explicitly signalled HDR to the static SDR path. */
    private fun hasUnsupportedPngFeatures(file: File): Boolean = RandomAccessFile(file, "r").use { input ->
        if (input.length() < 8 || input.readLong() != 0x89504e470d0a1a0aUL.toLong()) return@use true
        var count = 0
        while (input.filePointer < input.length()) {
            if (++count > 4096 || input.length() - input.filePointer < 12) return@use true
            val length = input.readInt().toLong() and 0xffffffffL
            val name = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
            if (length > input.length() - input.filePointer - 4) return@use true
            if (name in setOf("acTL", "fcTL", "fdAT", "cICP", "cLLI", "mDCV")) return@use true
            if ((name == "eXIf" && length > 256 * 1024) || (name == "iCCP" && length > 64 * 1024)) return@use true
            input.seek(input.filePointer + length + 4)
            if (name == "IEND") return@use input.filePointer != input.length()
        }
        true
    }

    fun discard(id: String) {
        if (validId.matches(id)) File(directory, "$id.source").delete()
    }

    fun discardAll(ids: List<String>) = ids.forEach(::discard)

    private fun isAnimatedWebp(file: File): Boolean {
        RandomAccessFile(file, "r").use { input ->
            fun tag() = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
            if (input.length() < 12 || tag() != "RIFF") return true
            val total = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            if (total != input.length() - 8 || tag() != "WEBP") return true
            var count = 0
            while (input.filePointer < input.length()) {
                if (++count > 4096 || input.length() - input.filePointer < 8) return true
                val kind = tag()
                val length = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
                val end = input.filePointer + length + (length and 1L)
                if (end > input.length() || kind in setOf("ANIM", "ANMF")) return true
                if ((kind == "EXIF" && length > 256 * 1024) || (kind == "ICCP" && length > 64 * 1024)) return true
                if (kind == "VP8X" && (length != 10L || input.readUnsignedByte() and 2 != 0)) return true
                input.seek(end)
            }
            return false
        }
    }
}

