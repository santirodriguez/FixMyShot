package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.diagnostics.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Synthetic-only privacy, geometry and cleanup tests; not native codec certification. */
@RunWith(AndroidJUnit4::class)
class OddAvifDiagnosticTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private fun directory() = File(app.cacheDir, "avif-geometry-test-${UUID.randomUUID()}")

    @Test fun paddingReplicatesEdgesWithoutChangingSource() {
        val cancel = AtomicBoolean(false)
        val source = NativeCodecDiagnostic.fixture(PixelSize(65, 67), cancel)
        val directory = directory()
        try {
            val checker = OddAvifDiagnostic(directory, Runtime.getRuntime().maxMemory())
            for (mode in OddAvifDiagnostic.MODES) {
                val image = checker.padded(source, mode, cancel)
                try {
                    assertEquals(mode.width, image.width)
                    assertEquals(mode.height, image.height)
                    assertEquals(source.getPixel(64, 66), image.getPixel(mode.width - 1, mode.height - 1))
                    assertEquals(source.getPixel(32, 40), image.getPixel(32, 40))
                } finally { image.recycle() }
            }
            assertEquals(65, source.width); assertEquals(67, source.height)
            assertFalse(directory.exists())
        } finally { source.recycle() }
    }

    @Test fun disguisedPngIsRejectedAndOwnedCandidateRemoved() {
        val directory = directory()
        var calls = 0
        try {
            val checker = OddAvifDiagnostic(directory, Runtime.getRuntime().maxMemory()) {
                bitmap, _, _, _ ->
                calls++
                directory.mkdirs()
                val candidate = File.createTempFile("native-still-", ".avif", directory)
                candidate.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                EncodeResult.Encoded(candidate, if (calls == 1) CodecRoute.NATIVE_HEIC else CodecRoute.NATIVE_AVIF, 1L)
            }
            val report = checker.run(AtomicBoolean(false))
            if (Build.VERSION.SDK_INT >= 34) {
                assertEquals(5, calls)
                assertEquals(5, report.samples.size)
                assertEquals("WRONG_CODEC_ROUTE", report.samples.first().reason)
                assertTrue(report.samples.drop(1).all { it.reason == "INVALID_OUTPUT" })
                assertTrue(directory.listFiles().isNullOrEmpty())
            } else {
                assertEquals(0, calls)
                assertTrue(report.samples.all { it.reason == "UNSUPPORTED_API" })
            }
            val json = checker.run(AtomicBoolean(true)).toJson()
            val parsed = JSONObject(json)
            assertEquals("ODD_AVIF_NUMERIC_DIAGNOSTIC_V1", parsed.getString("kind"))
            assertEquals("DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION", parsed.getString("acceptance"))
            assertFalse(json.contains(directory.path)); assertFalse(json.contains("content://"))
        } finally { directory.deleteRecursively() }
    }

    @Test fun cancellationStopsAfterFirstCaseWithoutRetry() {
        val directory = directory()
        val cancel = AtomicBoolean(false)
        var called = 0
        try {
            val checker = OddAvifDiagnostic(directory, Runtime.getRuntime().maxMemory()) {
                _, _, _, _ -> called++; EncodeResult.Failed(FailureCode.ENCODE_FAILED)
            }
            val report = checker.run(cancel) { cancel.set(true) }
            if (Build.VERSION.SDK_INT >= 34) {
                assertEquals(1, called)
                assertEquals(1, report.samples.size)
                assertTrue(report.cancelled)
            } else {
                assertEquals(0, called)
                assertEquals(5, report.samples.size)
            }
            assertFalse(directory.exists())
        } finally { directory.deleteRecursively() }
    }
}
