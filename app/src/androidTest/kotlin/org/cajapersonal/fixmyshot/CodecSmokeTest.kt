package org.cajapersonal.fixmyshot

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Debug
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.CRC32

/** Synthetic bootstrap evidence; physical-device acceptance is deliberately separate. */
@RunWith(AndroidJUnit4::class)
class CodecSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun encoder(directory: File) = LosslessEncoder(directory, Runtime.getRuntime().maxMemory())

    @Test fun launcherStarts() {
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        assertFalse(activity.isFinishing)
        instrumentation.runOnMainSync { activity.finish() }
    }

    @Test fun syntheticPngAndWebpRoundTrips() {
        val directory = File(context.cacheDir, "g1-${System.nanoTime()}")
        val results = JSONArray()
        val report = JSONObject().put("schema", 1).put("status", "INCOMPLETE")
            .put("api", Build.VERSION.SDK_INT).put("device", Build.MODEL)
            .put("abis", JSONArray(Build.SUPPORTED_ABIS.toList()))
            .put("memory_measurement", "before/after snapshots, NOT peak")
            .put("timing", "first and repeat fixture runs, NOT process-cold benchmarks")
            .put("samples", results)
        try {
            val codec = encoder(directory)
            repeat(20) { fixture ->
                val width = if (fixture == 0) 1344 else 127 + fixture * 7
                val height = if (fixture == 0) 2992 else 241 + fixture * 11
                val source = makeFixture(width, height, fixture)
                try {
                    for (format in listOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
                        repeat(2) { iteration ->
                            val request = EncodeRequest(PixelSize(width, height), format, source.hasAlpha())
                            val beforePss = Debug.getPss()
                            val result = codec.encode(source, request, timeoutMillis = 120_000)
                            assertTrue("$fixture/$format: $result", result is EncodeResult.Encoded)
                            result as EncodeResult.Encoded
                            try {
                                val chunks = inspectContainer(result.file, format, source.hasAlpha())
                                val decoded = checkNotNull(BitmapFactory.decodeFile(result.file.absolutePath)) { "Independent reopen failed" }
                                try {
                                    assertEquals(width, decoded.width)
                                    assertEquals(height, decoded.height)
                                    assertTrue("Expected decoded sRGB", decoded.colorSpace?.isSrgb == true)
                                    val expected = IntArray(width)
                                    val actual = IntArray(width)
                                    for (y in 0 until height) {
                                        source.getPixels(expected, 0, width, 0, y, width, 1)
                                        decoded.getPixels(actual, 0, width, 0, y, width, 1)
                                        for (x in 0 until width) {
                                            if (Color.alpha(expected[x]) == 0) assertEquals(0, Color.alpha(actual[x]))
                                            else assertEquals("$fixture/$format at $x,$y", expected[x], actual[x])
                                        }
                                    }
                                    results.put(JSONObject().put("fixture", fixture).put("width", width)
                                        .put("height", height).put("alpha", source.hasAlpha())
                                        .put("format", format.name).put("route", result.route.name)
                                        .put("iteration", iteration).put("elapsed_ns", result.elapsedNanos)
                                        .put("bytes", result.file.length()).put("chunks", JSONArray(chunks))
                                        .put("pss_before_kib", beforePss).put("pss_after_kib", Debug.getPss())
                                        .put("native_heap_after_bytes", Debug.getNativeHeapAllocatedSize())
                                        .put("pixels", "PASS"))
                                } finally { decoded.recycle() }
                            } finally { assertTrue("Candidate cleanup failed", result.file.delete()) }
                        }
                    }
                } finally { source.recycle() }
            }
            report.put("status", "PASS_SYNTHETIC_SMOKE_ONLY")
        } finally {
            File(context.filesDir, "g1-smoke.json").writeText(report.toString(2))
            directory.deleteRecursively()
        }
    }

    @Test fun cancellationAndRejectionLeaveNoCandidate() {
        val directory = File(context.cacheDir, "cancel-${System.nanoTime()}")
        val source = makeFixture(128, 128, 1)
        try {
            val codec = encoder(directory)
            val request = EncodeRequest(PixelSize(128, 128))
            assertEquals(EncodeResult.Failed(FailureCode.CANCELLED), codec.encode(source, request, AtomicBoolean(true)))
            assertEquals(EncodeResult.Failed(FailureCode.METADATA_POLICY_UNAVAILABLE),
                codec.encode(source, request.copy(metadataPolicy = MetadataPolicy.PRESERVE_ALLOWLIST)))
            assertEquals(EncodeResult.Failed(FailureCode.INVALID_INPUT),
                codec.encode(source, request.copy(size = PixelSize(129, 128))))
            assertEquals(0, directory.listFiles()?.size ?: 0)
        } finally { source.recycle(); directory.deleteRecursively() }
    }

    private fun makeFixture(width: Int, height: Int, fixture: Int): Bitmap {
        val alpha = fixture != 0 && fixture % 5 == 0
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(alpha)
        val colors = intArrayOf(Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.CYAN)
        val row = IntArray(width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                row[x] = when {
                    alpha && x < 8 -> Color.TRANSPARENT
                    y % 37 == 0 || x % 41 == 0 -> colors[(x + y + fixture) % colors.size]
                    (y / 24 + fixture) % 2 == 0 -> Color.rgb(18, 18, 18)
                    else -> Color.rgb(240, 240, 240)
                }
            }
            bitmap.setPixels(row, 0, width, 0, y, width, 1)
        }
        return bitmap
    }

    private fun inspectContainer(file: File, format: OutputFormat, alpha: Boolean): List<String> =
        RandomAccessFile(file, "r").use { input ->
            val chunks = mutableListOf<String>()
            if (format == OutputFormat.PNG) {
                assertEquals(0x89504e470d0a1a0aUL.toLong(), input.readLong())
                while (input.filePointer < input.length()) {
                    val length = input.readInt().toLong() and 0xffffffffL
                    assertTrue(length <= input.length() - input.filePointer - 8)
                    val type = ByteArray(4).also { input.readFully(it) }
                    val name = type.toString(Charsets.US_ASCII)
                    chunks.add(name)
                    assertFalse(name in setOf("eXIf", "tEXt", "zTXt", "iTXt"))
                    val crc = CRC32().apply { update(type) }
                    var left = length
                    val buffer = ByteArray(8192)
                    if (name == "IHDR") {
                        assertEquals(13L, length)
                        val header = ByteArray(13).also { input.readFully(it) }
                        crc.update(header)
                        assertEquals(8, header[8].toInt())
                        assertEquals(if (alpha) 6 else 2, header[9].toInt())
                        left = 0
                    }
                    while (left > 0) {
                        val count = minOf(left, buffer.size.toLong()).toInt()
                        input.readFully(buffer, 0, count); crc.update(buffer, 0, count); left -= count
                    }
                    assertEquals(crc.value, input.readInt().toLong() and 0xffffffffL)
                }
                assertEquals("IHDR", chunks.first()); assertEquals("IEND", chunks.last())
            } else {
                fun fourcc() = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
                assertEquals("RIFF", fourcc())
                val riffSize = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
                assertEquals(input.length() - 8, riffSize)
                assertEquals("WEBP", fourcc())
                while (input.filePointer < input.length()) {
                    val name = fourcc(); chunks.add(name)
                    val length = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
                    assertTrue(length <= input.length() - input.filePointer)
                    assertFalse(name in setOf("EXIF", "XMP ", "ANIM", "ANMF", "VP8 "))
                    val end = input.filePointer + length + (length and 1L)
                    assertTrue(end <= input.length())
                    if (name == "VP8L") { assertTrue(length > 0); assertEquals(0x2f, input.readUnsignedByte()) }
                    input.seek(end)
                }
                assertTrue("Missing actual lossless bitstream", "VP8L" in chunks)
            }
            chunks
        }
}
