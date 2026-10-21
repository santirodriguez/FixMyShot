package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.codec.NativeStillEncoder
import org.cajapersonal.fixmyshot.core.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Probe actual device encode/reopen using tiny, odd-dimension, opaque synthetic pixels. */
@RunWith(AndroidJUnit4::class)
class NativeCodecProbeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun nativeWriterAvailabilityMustBeEarned() {
        val width = 65
        val height = 67
        val directory = File(context.cacheDir, "native-probe-${System.nanoTime()}")
        val report = JSONObject().put("schema", 1).put("status", "INCOMPLETE")
            .put("api", Build.VERSION.SDK_INT)
            .put("size", JSONArray(listOf(width, height)))
        val samples = JSONArray()
        report.put("samples", samples)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(false)
        try {
            val row = IntArray(width)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    row[x] = when {
                        y < 12 -> Color.rgb(230, 230, 230)
                        x < 16 -> Color.rgb(12, 12, 12)
                        else -> Color.rgb((x * 17) % 256, (y * 11) % 256, (x * 7 + y * 3) % 256)
                    }
                }
                bitmap.setPixels(row, 0, width, 0, y, width, 1)
            }
            val adapter = NativeStillEncoder(directory, Runtime.getRuntime().maxMemory())
            for (format in listOf(OutputFormat.HEIC, OutputFormat.AVIF)) {
                val request = EncodeRequest(PixelSize(width, height), format, false)
                val expected = if (format == OutputFormat.HEIC) CodecRoute.NATIVE_HEIC else CodecRoute.NATIVE_AVIF
                val sample = JSONObject().put("format", format.name).put("route", expected.name)
                when (val encoded = adapter.encode(bitmap, request, timeoutMillis = 40_000)) {
                    is EncodeResult.Encoded -> {
                        assertEquals(expected, encoded.route)
                        val decoded = android.graphics.BitmapFactory.decodeFile(encoded.file.absolutePath)
                            ?: if (Build.VERSION.SDK_INT >= 28) {
                                try {
                                    android.graphics.ImageDecoder.decodeBitmap(
                                        android.graphics.ImageDecoder.createSource(encoded.file),
                                    )
                                } catch (_: java.io.IOException) { null }
                            } else null
                        val reopened = checkNotNull(decoded) {
                            "Native sample cannot be independently decoded: ${format.name}"
                        }
                        try {
                            assertEquals(width, reopened.width)
                            assertEquals(height, reopened.height)
                        } finally { reopened.recycle() }
                        val destination = File(context.filesDir, "g1-native-fixture.${format.extension}")
                        try {
                            encoded.file.copyTo(destination, overwrite = true)
                            sample.put("status", "ENCODED_AND_REOPENED")
                                .put("bytes", encoded.file.length())
                                .put("elapsed_ns", encoded.elapsedNanos)
                        } finally {
                            assertTrue("Native private candidate cleanup failed", encoded.file.delete())
                        }
                    }
                    is EncodeResult.Failed -> {
                        assertNotEquals("A selected native route must not be a dead stub",
                            FailureCode.NOT_INTEGRATED, encoded.reason)
                        if (Build.VERSION.SDK_INT < (if (format == OutputFormat.HEIC) 29 else 34)) {
                            assertEquals(FailureCode.UNSUPPORTED_API, encoded.reason)
                        }
                        sample.put("status", "UNAVAILABLE").put("reason", encoded.reason.name)
                        assertFalse(File(context.filesDir, "g1-native-fixture.${format.extension}").exists())
                    }
                }
                assertEquals(0, directory.listFiles()?.size ?: 0)
                samples.put(sample)
            }
            report.put("status", "PROBE_COMPLETE")
        } finally {
            bitmap.recycle()
            directory.deleteRecursively()
            File(context.filesDir, "g1-native-probe.json").writeText(report.toString(2))
        }
    }
}
