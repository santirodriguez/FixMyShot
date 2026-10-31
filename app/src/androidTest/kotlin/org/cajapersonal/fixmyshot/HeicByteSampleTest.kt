package org.cajapersonal.fixmyshot

import android.os.Build
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.diagnostics.AvifByteSampler
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Fake container tests check capture boundaries, never claim native HEIC fidelity. */
@RunWith(AndroidJUnit4::class)
class HeicByteSampleTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()
    private val heic = byteArrayOf(0,0,0,20,102,116,121,112,104,101,105,99,0,0,0,0,109,105,102,49)
    private fun directory() = File(app.cacheDir, "heic-sample-" + UUID.randomUUID())
    private fun candidate(dir: File, bytes: ByteArray = heic,
        route: CodecRoute = CodecRoute.NATIVE_HEIC): EncodeResult.Encoded {
        dir.mkdirs()
        val file = File.createTempFile("native-still-", ".heic", dir)
        file.writeBytes(bytes)
        return EncodeResult.Encoded(file, route, 1)
    }

    @Test fun oneSyntheticHeicCaptureHasTypedReceiptAndCleansPrivateFile() {
        val dir = directory(); var calls = 0
        try {
            val sample = AvifByteSampler(dir, heap) { bitmap, request, _ ->
                calls++
                assertEquals(65, bitmap.width); assertEquals(67, bitmap.height)
                assertEquals(OutputFormat.HEIC, request.format)
                candidate(dir)
            }.run(AtomicBoolean(false), OutputFormat.HEIC)
            if (Build.VERSION.SDK_INT >= 29) {
                assertEquals(1, calls); assertTrue(sample.ready)
                assertArrayEquals(heic, Base64.decode(sample.encodedBase64, Base64.DEFAULT))
                val report = JSONObject(sample.toJson())
                assertEquals("SYNTHETIC_HEIC_BYTES_V1", report.getString("kind"))
                assertEquals("HEIC", report.getString("format"))
                assertFalse(sample.toJson().contains("content://"))
                assertTrue(dir.listFiles().isNullOrEmpty())
            } else { assertEquals(0, calls); assertEquals("UNSUPPORTED_API", sample.reason) }
        } finally { dir.deleteRecursively() }
    }

    @Test fun wrongRouteAndNonHeicAreRejectedAndCleaned() {
        for (wrongRoute in listOf(true, false)) {
            val dir = directory()
            try {
                val sample = AvifByteSampler(dir, heap) { _, _, _ ->
                    candidate(dir, if (wrongRoute) heic else ByteArray(20),
                        if (wrongRoute) CodecRoute.NATIVE_AVIF else CodecRoute.NATIVE_HEIC)
                }.run(AtomicBoolean(false), OutputFormat.HEIC)
                if (Build.VERSION.SDK_INT >= 29) {
                    assertEquals(if (wrongRoute) "WRONG_CODEC_ROUTE" else "INVALID_OUTPUT", sample.reason)
                    assertFalse(sample.ready); assertTrue(dir.listFiles().isNullOrEmpty())
                }
            } finally { dir.deleteRecursively() }
        }
    }

    @Test fun cancellationKeepsBytesOutOfReportAndDeletesOwnedFile() {
        val dir=directory();val stop=AtomicBoolean(false)
        try {
            val sample=AvifByteSampler(dir,heap) { _,_,cancel ->
                val result=candidate(dir);cancel.set(true);result
            }.run(stop,OutputFormat.HEIC)
            if(Build.VERSION.SDK_INT>=29) {
                assertTrue(sample.cancelled);assertFalse(sample.ready)
                assertNull(sample.encodedBase64);assertTrue(dir.listFiles().isNullOrEmpty())
            }
        } finally { dir.deleteRecursively() }
    }

    @Test fun sizeLimitDeletesOnlyOwnedCandidate() {
        val dir=directory();val outside=directory()
        try {
            val large=AvifByteSampler(dir,heap) { _,_,_ -> candidate(dir,ByteArray(8193)) }
                .run(AtomicBoolean(false),OutputFormat.HEIC)
            if(Build.VERSION.SDK_INT>=29) {
                assertEquals("SIZE_LIMIT",large.reason);assertTrue(dir.listFiles().isNullOrEmpty())
                val result=candidate(outside)
                val unowned=AvifByteSampler(dir,heap) { _,_,_ -> result }
                    .run(AtomicBoolean(false),OutputFormat.HEIC)
                assertEquals("UNOWNED_CANDIDATE",unowned.reason)
                assertTrue(result.file.exists());assertNull(unowned.encodedBase64)
            }
        } finally {dir.deleteRecursively();outside.deleteRecursively()}
    }
}
