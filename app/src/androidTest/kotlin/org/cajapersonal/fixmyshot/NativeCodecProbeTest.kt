package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.media.MediaCodecList
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
import java.io.IOException

/** Independent device probes for even and odd single-still dimensions. */
@RunWith(AndroidJUnit4::class)
class NativeCodecProbeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val cases = listOf(PixelSize(64, 64), PixelSize(65, 67))

    /** Codec-list advertisement is diagnostic metadata, not proof an encoder can write a still. */
    private fun codecInventory(): JSONObject {
        val types = listOf("video/hevc", "video/av01", "image/vnd.android.heic", "image/avif")
        val codecs = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        val inventory = JSONObject()
        for (mime in types) {
            val matching = codecs.filter { info -> info.supportedTypes.any { it.equals(mime, ignoreCase = true) } }
            fun names(encoders: Boolean): JSONArray = JSONArray(matching.filter { it.isEncoder == encoders }
                .map { it.name }.distinct().sorted().take(32))
            inventory.put(mime, JSONObject().put("encoders", names(true)).put("decoders", names(false)))
        }
        return inventory
    }

    @Test fun nativeWriterAvailabilityMustBeEarned() {
        val directory = File(context.cacheDir, "native-probe-${System.nanoTime()}")
        val report = JSONObject().put("schema", 2).put("status", "INCOMPLETE")
            .put("api", Build.VERSION.SDK_INT)
            .put("sizes", JSONArray(cases.map { JSONArray(listOf(it.width, it.height)) }))
        val samples = JSONArray()
        report.put("samples", samples)
        try {
            report.put("codec_inventory", codecInventory())
            val adapter = NativeStillEncoder(directory, Runtime.getRuntime().maxMemory())
            for (size in cases) {
                val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                bitmap.setHasAlpha(false)
                try {
                    val row = IntArray(size.width)
                    for (y in 0 until size.height) {
                        for (x in 0 until size.width) {
                            row[x] = when {
                                y < 12 -> Color.rgb(230, 230, 230)
                                x < 16 -> Color.rgb(12, 12, 12)
                                else -> Color.rgb((x * 17) % 256, (y * 11) % 256, (x * 7 + y * 3) % 256)
                            }
                        }
                        bitmap.setPixels(row, 0, size.width, 0, y, size.width, 1)
                    }
                    for (format in listOf(OutputFormat.HEIC, OutputFormat.AVIF)) {
                        val request = EncodeRequest(size, format, false)
                        val route = if (format == OutputFormat.HEIC) CodecRoute.NATIVE_HEIC else CodecRoute.NATIVE_AVIF
                        val sample = JSONObject().put("format", format.name).put("route", route.name)
                            .put("width", size.width).put("height", size.height)
                        val name = "g1-native-${size.width}x${size.height}.${format.extension}"
                        when (val result = adapter.encode(bitmap, request, timeoutMillis = 40_000)) {
                            is EncodeResult.Encoded -> {
                                assertEquals(route, result.route)
                                val decoded = BitmapFactory.decodeFile(result.file.absolutePath)
                                    ?: if (Build.VERSION.SDK_INT >= 28) {
                                        try { ImageDecoder.decodeBitmap(ImageDecoder.createSource(result.file)) }
                                        catch (_: IOException) { null }
                                    } else null
                                val reopened = checkNotNull(decoded) { "Native sample cannot be reopened: $name" }
                                try {
                                    assertEquals(size.width, reopened.width)
                                    assertEquals(size.height, reopened.height)
                                } finally { reopened.recycle() }
                                val destination = File(context.filesDir, name)
                                try {
                                    result.file.copyTo(destination, overwrite = true)
                                    sample.put("status", "ENCODED_AND_REOPENED")
                                        .put("bytes", result.file.length())
                                        .put("elapsed_ns", result.elapsedNanos)
                                } finally {
                                    assertTrue("Native private candidate cleanup failed", result.file.delete())
                                }
                            }
                            is EncodeResult.Failed -> {
                                assertNotEquals("Native route must not remain a stub", FailureCode.NOT_INTEGRATED, result.reason)
                                if (Build.VERSION.SDK_INT < (if (format == OutputFormat.HEIC) 29 else 34)) {
                                    assertEquals(FailureCode.UNSUPPORTED_API, result.reason)
                                }
                                sample.put("status", "UNAVAILABLE").put("reason", result.reason.name)
                                assertFalse(File(context.filesDir, name).exists())
                            }
                        }
                        assertEquals(0, directory.listFiles()?.size ?: 0)
                        samples.put(sample)
                    }
                } finally { bitmap.recycle() }
            }
            report.put("status", "PROBE_COMPLETE")
        } finally {
            directory.deleteRecursively()
            File(context.filesDir, "g1-native-probe.json").writeText(report.toString(2))
        }
    }
}
