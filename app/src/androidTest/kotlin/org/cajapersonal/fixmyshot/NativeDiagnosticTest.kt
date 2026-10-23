package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.codec.NativeStillEncoder
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.diagnostics.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10

/** Diagnostic policy/metrics and owned-candidate boundaries, not native codec acceptance. */
@RunWith(AndroidJUnit4::class)
class NativeDiagnosticTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()
    private val size = NativeCodecDiagnostic.SIZES.first()
    private fun directory() = File(app.cacheDir, "native-check-test-${UUID.randomUUID()}")

    @Test fun numericOracleSeparatesExactPixelsSinglePixelErrorAndWrongColor() {
        val source = NativeCodecDiagnostic.fixture(size, AtomicBoolean(false))
        val decoded = source.copy(Bitmap.Config.ARGB_8888, true)!!
        try {
            val exact = NativeCodecDiagnostic.compare(source, decoded, AtomicBoolean(false))
            assertTrue(exact.exactPixels); assertNull(exact.psnrDb)
            assertEquals(0, exact.patchError); assertTrue(exact.meetsNumericTargets)
            assertEquals(Color.WHITE, source.getPixel(0, size.height - 1))
            decoded.setPixel(0, size.height - 1, Color.BLACK)
            val single = NativeCodecDiagnostic.compare(source, decoded, AtomicBoolean(false))
            assertFalse(single.exactPixels)
            assertEquals(10.0 * log10((size.width * size.height).toDouble()), single.psnrDb!!, 0.000001)
            assertEquals(0, single.patchError); assertFalse(single.meetsNumericTargets)
            decoded.eraseColor(Color.BLUE)
            val wrong = NativeCodecDiagnostic.compare(source, decoded, AtomicBoolean(false))
            assertTrue(wrong.patchError > 2); assertFalse(wrong.meetsNumericTargets)
            assertThrows(java.io.InterruptedIOException::class.java) {
                NativeCodecDiagnostic.compare(source, decoded, AtomicBoolean(true))
            }
        } finally { source.recycle(); decoded.recycle() }
    }

    @Test fun typedReportExcludesPathsExceptionsAndNonNativeInputCannotCrashAdapter() {
        val directory = directory()
        try {
            val report = NativeCodecDiagnostic(directory, heap, listOf(size)) { _, _, _ ->
                throw IOException("content://synthetic/private ${directory.path}")
            }.run(AtomicBoolean(false))
            assertEquals(2, report.samples.size)
            assertTrue(report.samples.all { it.failure != null && it.metrics == null })
            val text = report.toJson()
            assertFalse(text.contains(directory.path)); assertFalse(text.contains("content://"))
            assertFalse(text.contains("IOException"))
            val json = JSONObject(text)
            assertEquals("DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION", json.getString("acceptance"))
            assertEquals(setOf("schema", "kind", "acceptance", "measurement", "api", "manufacturer", "model",
                "app_version", "version_code", "base_apk_sha256", "cancelled", "failed", "samples"), json.keys().asSequence().toSet())
            val bitmap = NativeCodecDiagnostic.fixture(size, AtomicBoolean(false))
            try {
                assertEquals(EncodeResult.Failed(FailureCode.INVALID_INPUT),
                    NativeStillEncoder(directory, heap).encode(bitmap, EncodeRequest(size, OutputFormat.PNG, false)))
            } finally { bitmap.recycle() }
            assertEquals(0, directory.listFiles()?.size ?: 0)
        } finally { directory.deleteRecursively() }
    }

    @Test fun cancellationStopsRemainingCasesAndDisguisedNativeOutputIsDeleted() {
        val directory = directory()
        var calls = 0
        val cancel = AtomicBoolean(false)
        try {
            val runner = NativeCodecDiagnostic(directory, heap, listOf(size)) { bitmap, request, _ ->
                calls++
                directory.mkdirs()
                val file = File.createTempFile("native-still-", ".${request.format.extension}", directory)
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                EncodeResult.Encoded(file, CodecRoute.NATIVE_HEIC, 1L)
            }
            val report = runner.run(cancel) { cancel.set(true) }
            assertTrue(report.cancelled); assertEquals(1, report.samples.size)
            if (Build.VERSION.SDK_INT >= 29) {
                assertEquals(1, calls)
                assertEquals(FailureCode.INVALID_OUTPUT, report.samples.single().failure)
            } else {
                assertEquals(0, calls)
                assertEquals(FailureCode.UNSUPPORTED_API, report.samples.single().failure)
            }
            assertEquals(0, directory.listFiles()?.size ?: 0)
        } finally { directory.deleteRecursively() }
    }

    @Test fun cancelledOrLowBudgetChecksDoNotEncodeAndIdentityUsesOnlyOwnApk() {
        val directory = directory()
        var calls = 0
        try {
            val runner = NativeCodecDiagnostic(directory, 1, listOf(size)) { _, _, _ ->
                calls++; throw AssertionError("Refused check must not reach encoder")
            }
            val cancelled = runner.run(AtomicBoolean(true))
            assertTrue(cancelled.cancelled); assertTrue(cancelled.samples.isEmpty())
            val report = runner.run(AtomicBoolean(false))
            assertEquals(2, report.samples.size)
            assertTrue(report.samples.all { it.failure in setOf(FailureCode.RESOURCE_LIMIT, FailureCode.UNSUPPORTED_API) })
            assertEquals(0, calls); assertFalse(directory.exists())
            val bound = report.withAppIdentity(app, AtomicBoolean(false))
            assertTrue(bound.appVersion.isNotBlank()); assertTrue(bound.versionCode > 0)
            assertTrue(bound.baseApkSha256?.matches(Regex("[0-9a-f]{64}")) == true)
            assertFalse(bound.toJson().contains(app.applicationInfo.sourceDir))
        } finally { directory.deleteRecursively() }
    }
}
