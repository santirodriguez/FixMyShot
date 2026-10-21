package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.heifwriter.AvifWriter
import androidx.heifwriter.HeifWriter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeoutException
import kotlin.math.log10
import kotlin.math.min

/**
 * Grid/alignment diagnostic only; this does not expose a product encoding route.
 * Padded diagnostic rasters do NOT qualify as exports at the original 65x67 dimensions.
 */
@RunWith(AndroidJUnit4::class)
class NativeGridDiagnosticTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private data class Geometry(
        val width: Int, val height: Int,
        val sourceWidth: Int, val sourceHeight: Int,
        val grid: Boolean,
    ) {
        val padded: Boolean get() = width != sourceWidth || height != sourceHeight
        val suffix: String get() = "${width}x${height}-${if (grid) "on" else "off"}"
    }

    // Existing native probe covers 64x64 and 65x67 with default grid enabled.
    private val geometries = listOf(
        Geometry(64, 64, 64, 64, false),
        Geometry(65, 67, 65, 67, false),
        Geometry(66, 68, 65, 67, false),
        Geometry(66, 68, 65, 67, true),
        // Compare decoder stride/block geometry at 32- and 64-pixel-aligned widths.
        Geometry(96, 68, 65, 67, false),
        Geometry(128, 96, 65, 67, false),
    )

    @Test fun gridAndPaddingAreDiagnosedWithoutChangingTheExportContract() {
        val report = JSONObject().put("schema", 1).put("status", "INCOMPLETE")
            .put("api", Build.VERSION.SDK_INT).put("kind", "GRID_AND_PADDING_DIAGNOSTIC")
            .put("samples", JSONArray())
        val samples = report.getJSONArray("samples")
        try {
            for (geometry in geometries) {
                val bitmap = createPixels(geometry)
                try {
                    for (format in listOf("HEIC", "AVIF")) {
                        samples.put(encodeOne(bitmap, geometry, format))
                    }
                } finally { bitmap.recycle() }
            }
            assertEquals(geometries.size * 2, samples.length())
            report.put("status", "DIAGNOSTIC_COMPLETE")
        } finally {
            File(context.filesDir, "g1-grid-diagnostic.json").writeText(report.toString(2))
        }
    }

    private fun pixel(x: Int, y: Int): Int = when {
        y < 12 -> Color.rgb(230, 230, 230)
        x < 16 -> Color.rgb(12, 12, 12)
        else -> Color.rgb((x * 17) % 256, (y * 11) % 256, (x * 7 + y * 3) % 256)
    }

    private fun createPixels(g: Geometry): Bitmap {
        val bitmap = Bitmap.createBitmap(g.width, g.height, Bitmap.Config.ARGB_8888)
        val row = IntArray(g.width)
        for (y in 0 until g.height) {
            for (x in 0 until g.width) {
                row[x] = pixel(min(x, g.sourceWidth - 1), min(y, g.sourceHeight - 1))
            }
            bitmap.setPixels(row, 0, g.width, 0, y, g.width, 1)
        }
        bitmap.setHasAlpha(false)
        return bitmap
    }

    private fun encodeOne(bitmap: Bitmap, g: Geometry, format: String): JSONObject {
        val extension = if (format == "HEIC") "heic" else "avif"
        val name = "g1-grid-$extension-${g.suffix}.$extension"
        val sample = JSONObject().put("format", format)
            .put("width", g.width).put("height", g.height)
            .put("source_width", g.sourceWidth).put("source_height", g.sourceHeight)
            .put("padded", g.padded).put("grid", g.grid)
            .put("color_profile_inspection", "NOT_PERFORMED")
        val minApi = if (format == "HEIC") 29 else 34
        if (Build.VERSION.SDK_INT < minApi) {
            return sample.put("status", "UNAVAILABLE").put("reason", "UNSUPPORTED_API")
        }
        var file: File? = null
        return try {
            val started = System.nanoTime()
            val candidate = File.createTempFile("grid-diagnostic-", ".$extension", context.cacheDir)
            file = candidate
            // Per-case cap is independent of the existing production encoder's timeout.
            if (format == "HEIC") {
                HeifWriter.Builder(candidate.absolutePath, g.width, g.height, HeifWriter.INPUT_MODE_BITMAP)
                    .setMaxImages(1).setQuality(100).setGridEnabled(g.grid).build().use { writer ->
                        writer.start(); writer.addBitmap(bitmap); writer.stop(15_000)
                    }
            } else {
                AvifWriter.Builder(candidate.absolutePath, g.width, g.height, AvifWriter.INPUT_MODE_BITMAP)
                    .setMaxImages(1).setQuality(100).setGridEnabled(g.grid).build().use { writer ->
                        writer.start(); writer.addBitmap(bitmap); writer.stop(15_000)
                    }
            }
            if (!hasExpectedBrand(candidate, format)) {
                return sample.put("status", "UNAVAILABLE").put("reason", "INVALID_OUTPUT")
            }
            val decoded = decode(candidate) ?: return sample.put("status", "UNAVAILABLE")
                .put("reason", "INVALID_OUTPUT")
            try {
                if (decoded.width != g.width || decoded.height != g.height) {
                    return sample.put("status", "UNAVAILABLE").put("reason", "INVALID_OUTPUT")
                }
                // Metrics are diagnostic on the unpadded source ROI; color tags remain unverified.
                val stats = measureColor(decoded, g)
                val destination = File(context.filesDir, name)
                candidate.copyTo(destination, overwrite = true)
                sample.put("status", "ENCODED_AND_REOPENED")
                    .put("bytes", candidate.length())
                    .put("elapsed_ns", System.nanoTime() - started)
                    .put("decoded_width", decoded.width).put("decoded_height", decoded.height)
                    .put("decoded_srgb", decoded.colorSpace?.isSrgb == true)
                    .put("psnr_rgb_db", stats.first)
                    .put("median_abs_rgb", JSONArray(stats.second.toList()))
            } finally { decoded.recycle() }
            sample
        } catch (_: TimeoutException) {
            sample.put("status", "UNAVAILABLE").put("reason", "TIMEOUT")
        } catch (_: IOException) {
            sample.put("status", "UNAVAILABLE").put("reason", "IO_ERROR")
        } catch (_: IllegalArgumentException) {
            sample.put("status", "UNAVAILABLE").put("reason", "ENCODE_FAILED")
        } catch (_: IllegalStateException) {
            sample.put("status", "UNAVAILABLE").put("reason", "ENCODE_FAILED")
        } catch (_: UnsupportedOperationException) {
            sample.put("status", "UNAVAILABLE").put("reason", "NATIVE_UNAVAILABLE")
        } finally { file?.delete() }
    }

    private fun hasExpectedBrand(file: File, format: String): Boolean = try {
        RandomAccessFile(file, "r").use { input ->
            if (input.length() < 16) return@use false
            val boxSize = input.readInt().toLong() and 0xffffffffL
            val type = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
            val major = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
            boxSize >= 16 && boxSize <= input.length() && type == "ftyp" &&
                (if (format == "HEIC") major in setOf("heic", "heix", "hevc", "mif1")
                 else major in setOf("avif", "avis"))
        }
    } catch (_: IOException) { false }

    private fun decode(file: File): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap != null) return bitmap
        // Never reference newer framework decoder signatures in this JUnit4 test class.
        // On API 26, JUnit scans synthetic methods before it can apply runtime API guards.
        return if (Build.VERSION.SDK_INT >= 28) Api28GridDecoder.decode(file) else null
    }

    private fun measureColor(decoded: Bitmap, g: Geometry): Pair<Double, IntArray> {
        val counts = g.sourceWidth * g.sourceHeight
        val errors = Array(3) { IntArray(counts) }
        var squared = 0.0
        var offset = 0
        for (y in 0 until g.sourceHeight) for (x in 0 until g.sourceWidth) {
            val expected = pixel(x, y)
            val actual = decoded.getPixel(x, y)
            val from = intArrayOf(Color.red(expected), Color.green(expected), Color.blue(expected))
            val to = intArrayOf(Color.red(actual), Color.green(actual), Color.blue(actual))
            for (ch in 0..2) {
                val delta = from[ch] - to[ch]
                errors[ch][offset] = kotlin.math.abs(delta)
                squared += delta.toDouble() * delta
            }
            offset++
        }
        val mse = squared / (counts * 3.0)
        val psnr = if (mse == 0.0) 99.0 else 10.0 * log10(65025.0 / mse)
        return psnr to IntArray(3) { c -> errors[c].apply { sort() }[counts / 2] }
    }
}
