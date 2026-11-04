package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import kotlin.math.log10

/**
 * Synthetic, test-only comparison of the previously captured grid HEIC files.
 * Neither the files nor the production writer's requested dimensions are modified.
 * Bitmap rowBytes describes the final raster, not an intermediate codec stride.
 */
@RunWith(AndroidJUnit4::class)
class NativeHeicPixelLayoutTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val narrowFile = "g1-grid-heic-66x68-off.heic"
    private val wideFile = "g1-grid-heic-96x68-off.heic"

    @Test fun compareActualDecoderPixelPositions() {
        val report = JSONObject()
            .put("schema", 1).put("kind", "HEIC_PIXEL_LAYOUT_DIAGNOSTIC")
            .put("api", Build.VERSION.SDK_INT).put("status", "INCOMPLETE")
        try {
            if (Build.VERSION.SDK_INT < 29) {
                report.put("status", "UNSUPPORTED_API")
                return
            }
            val narrow = File(context.filesDir, narrowFile)
            val wide = File(context.filesDir, wideFile)
            if (!narrow.isFile || !wide.isFile) {
                report.put("status", "UNAVAILABLE").put("reason", "SOURCE_OUTPUT_UNAVAILABLE")
                return
            }
            assertTrue("Unexpectedly large synthetic narrow HEIC", narrow.length() in 16..2_097_152)
            assertTrue("Unexpectedly large synthetic wide HEIC", wide.length() in 16..2_097_152)
            val narrowBytes = narrow.readBytes()
            val wideBytes = wide.readBytes()
            val count = (0 until minOf(narrowBytes.size, wideBytes.size))
                .count { narrowBytes[it] != wideBytes[it] } +
                kotlin.math.abs(narrowBytes.size - wideBytes.size)
            val firstDifference = (0 until minOf(narrowBytes.size, wideBytes.size))
                .firstOrNull { narrowBytes[it] != wideBytes[it] } ?: -1
            val narrowWidthOffset = ispeWidthOffset(narrowBytes)
            val wideWidthOffset = ispeWidthOffset(wideBytes)
            report.put("files", JSONObject()
                .put("narrow", fileSummary(narrowBytes))
                .put("wide", fileSummary(wideBytes))
                .put("difference_count", count)
                .put("first_difference_offset", firstDifference)
                .put("one_ispe_width_byte_difference", narrowBytes.size == wideBytes.size &&
                    count == 1 && narrowWidthOffset != null && wideWidthOffset != null &&
                    narrowWidthOffset == wideWidthOffset &&
                    firstDifference in narrowWidthOffset..(narrowWidthOffset + 3) &&
                    readWidth(narrowBytes, narrowWidthOffset) == 66 &&
                    readWidth(wideBytes, wideWidthOffset) == 96))

            val rows = JSONArray()
            report.put("decoders", rows)
            val bitmapFactory = decodePair("BITMAP_FACTORY", narrow, wide) { file ->
                try { BitmapFactory.decodeFile(file.absolutePath) }
                catch (_: RuntimeException) { null }
            }
            val imageDecoder = decodePair("IMAGE_DECODER", narrow, wide) { file ->
                // API 28+ framework references stay in a separate helper: API 26 JUnit-safe.
                Api28GridDecoder.decode(file)
            }
            rows.put(bitmapFactory.json).put(imageDecoder.json)
            report.put("cross_decoder", compareDecoders(bitmapFactory, imageDecoder))
            report.put("status", "DIAGNOSTIC_COMPLETE")
        } finally {
            File(context.filesDir, "g1-heic-pixel-layout.json").writeText(report.toString(2))
        }
    }

    private fun fileSummary(bytes: ByteArray): JSONObject = JSONObject()
        .put("bytes", bytes.size).put("sha256", sha256(bytes))

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    // The full-box flags precede the four-byte width in an ispe property.
    private fun ispeWidthOffset(bytes: ByteArray): Int? {
        for (i in 0 until bytes.size - 16) {
            if (bytes[i] == 'i'.code.toByte() && bytes[i + 1] == 's'.code.toByte() &&
                bytes[i + 2] == 'p'.code.toByte() && bytes[i + 3] == 'e'.code.toByte()) {
                return i + 8
            }
        }
        return null
    }

    private fun readWidth(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 255) shl 24) or
        ((bytes[offset + 1].toInt() and 255) shl 16) or
        ((bytes[offset + 2].toInt() and 255) shl 8) or
        (bytes[offset + 3].toInt() and 255)

    private data class Raster(val width: Int, val height: Int, val pixels: IntArray)
    private data class DecoderResult(val json: JSONObject, val narrow: Raster?, val wide: Raster?)

    private fun describe(bitmap: Bitmap): JSONObject {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val rgb = ByteArray(pixels.size * 3)
        var k = 0
        for (pixel in pixels) {
            rgb[k++] = (pixel ushr 16).toByte()
            rgb[k++] = (pixel ushr 8).toByte()
            rgb[k++] = pixel.toByte()
        }
        return JSONObject()
            .put("width", bitmap.width).put("height", bitmap.height)
            .put("row_bytes", bitmap.rowBytes)
            .put("config", bitmap.config?.name ?: "UNKNOWN")
            .put("decoded_srgb", bitmap.colorSpace?.isSrgb == true)
            .put("rgb_sha256", sha256(rgb))
    }

    private fun raster(bitmap: Bitmap): Raster = IntArray(bitmap.width * bitmap.height).let { pixels ->
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        Raster(bitmap.width, bitmap.height, pixels)
    }

    private fun decodePair(label: String, narrowFile: File, wideFile: File,
                           decode: (File) -> Bitmap?): DecoderResult {
        val entry = JSONObject().put("decoder", label).put("status", "UNAVAILABLE")
        val n = decode(narrowFile)
        val w = decode(wideFile)
        try {
            if (n == null || w == null) {
                return DecoderResult(entry.put("reason", "PIXEL_DECODE_FAILED"), null, null)
            }
            entry.put("narrow", describe(n)).put("wide", describe(w))
            if (n.width != 66 || n.height != 68 || w.width != 96 || w.height != 68) {
                return DecoderResult(entry.put("reason", "UNEXPECTED_VISIBLE_DIMENSIONS"), null, null)
            }
            val a = raster(n)
            val b = raster(w)
            entry.put("status", "MEASURED")
            entry.put("comparison", compare(a, b))
            return DecoderResult(entry, a, b)
        } finally {
            n?.recycle()
            w?.recycle()
        }
    }

    private fun squaredError(a: Int, b: Int): Long {
        val dr = ((a ushr 16) and 255) - ((b ushr 16) and 255)
        val dg = ((a ushr 8) and 255) - ((b ushr 8) and 255)
        val db = (a and 255) - (b and 255)
        return (dr * dr + dg * dg + db * db).toLong()
    }

    private fun psnr(squared: Long, pixels: Int): Double =
        if (squared == 0L) 99.0 else 10.0 * log10(65025.0 * 3.0 * pixels / squared.toDouble())

    /** Test both Android decoders against each other at the same visible coordinates. */
    private fun compareDecoders(a: DecoderResult, b: DecoderResult): JSONObject {
        val result = JSONObject().put("status", "NOT_COMPARABLE")
        val an = a.narrow ?: return result
        val aw = a.wide ?: return result
        val bn = b.narrow ?: return result
        val bw = b.wide ?: return result
        if (an.width != bn.width || an.height != bn.height ||
            aw.width != bw.width || aw.height != bw.height) return result
        fun comparison(first: Raster, second: Raster): JSONObject {
            var squared = 0L
            var exact = 0
            for (i in first.pixels.indices) {
                val p = first.pixels[i]
                val q = second.pixels[i]
                if ((p and 0xffffff) == (q and 0xffffff)) exact++
                squared += squaredError(p, q)
            }
            return JSONObject().put("exact_rgb_pixels", exact)
                .put("pixels", first.pixels.size)
                .put("psnr_db", psnr(squared, first.pixels.size))
        }
        return result.put("status", "MEASURED")
            .put("narrow", comparison(an, bn))
            .put("wide", comparison(aw, bw))
    }

    /** Compare an ordinary x-offset crop with a tightly packed 66-pixel row stride. */
    private fun compare(narrow: Raster, wide: Raster): JSONObject {
        val w = narrow.width
        val h = narrow.height
        val count = w * h
        var originSquared = 0L
        var packedSquared = 0L
        var exact = 0
        val rowSums = JSONArray()
        for (y in 0 until h) {
            var rowAbs = 0L
            for (x in 0 until w) {
                val narrowPixel = narrow.pixels[y * w + x]
                val widePixel = wide.pixels[y * wide.width + x]
                if ((narrowPixel and 0x00ffffff) == (widePixel and 0x00ffffff)) exact++
                originSquared += squaredError(narrowPixel, widePixel)
                packedSquared += squaredError(narrowPixel, wide.pixels[y * w + x])
                rowAbs += kotlin.math.abs(((narrowPixel ushr 16) and 255) - ((widePixel ushr 16) and 255))
                rowAbs += kotlin.math.abs(((narrowPixel ushr 8) and 255) - ((widePixel ushr 8) and 255))
                rowAbs += kotlin.math.abs((narrowPixel and 255) - (widePixel and 255))
            }
            rowSums.put(rowAbs.toDouble() / (w * 3))
        }
        var bestOffset = 0
        var bestError = Long.MAX_VALUE
        for (offset in 0..(wide.width - w)) {
            var error = 0L
            for (y in 0 until h) for (x in 0 until w) {
                error += squaredError(narrow.pixels[y * w + x],
                    wide.pixels[y * wide.width + offset + x])
            }
            if (error < bestError) {
                bestError = error
                bestOffset = offset
            }
        }
        return JSONObject()
            .put("pixels", count)
            .put("origin_psnr_db", psnr(originSquared, count))
            .put("best_crop_x", bestOffset)
            .put("best_crop_psnr_db", psnr(bestError, count))
            .put("packed_row_psnr_db", psnr(packedSquared, count))
            .put("origin_exact_rgb_pixels", exact)
            .put("origin_row_mean_abs_rgb", rowSums)
    }
}
