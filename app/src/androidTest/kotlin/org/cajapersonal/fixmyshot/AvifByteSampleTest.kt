package org.cajapersonal.fixmyshot

import android.os.Build
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.diagnostics.AvifByteSampler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Checks one synthetic source without using the user's imported images. */
@RunWith(AndroidJUnit4::class)
class AvifByteSampleTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private fun newDir() = File(app.cacheDir, "avif-sample-" + UUID.randomUUID().toString())
    private val avif = byteArrayOf(0, 0, 0, 20, 102, 116, 121, 112,
        97, 118, 105, 102, 0, 0, 0, 0, 109, 105, 102, 49)
    private fun fake(dir: File, route: CodecRoute, data: ByteArray): EncodeResult.Encoded {
        dir.mkdirs()
        val f = File.createTempFile("native-still-", ".avif", dir)
        f.writeBytes(data)
        return EncodeResult.Encoded(f, route, 1L)
    }

    @Test fun oneSampleRoundTripAndPrivateCleanup() {
        val dir = newDir()
        var calls = 0
        try {
            val subject = AvifByteSampler(dir, Runtime.getRuntime().maxMemory()) { bitmap, request, _ ->
                calls++
                assertEquals(65, bitmap.width); assertEquals(67, bitmap.height)
                assertEquals(OutputFormat.AVIF, request.format)
                fake(dir, CodecRoute.NATIVE_AVIF, avif)
            }
            val result = subject.run(AtomicBoolean(false))
            if (Build.VERSION.SDK_INT >= 34) {
                assertEquals(1, calls); assertTrue(result.ready)
                assertArrayEquals(avif, Base64.decode(result.encodedBase64, Base64.DEFAULT))
                assertEquals("SYNTHETIC_AVIF_BYTES_V1",
                    JSONObject(result.toJson()).getString("kind"))
                assertFalse(result.toJson().contains("content://"))
                assertTrue(dir.listFiles().isNullOrEmpty())
            } else { assertEquals(0, calls); assertEquals("UNSUPPORTED_API", result.reason) }
        } finally { dir.deleteRecursively() }
    }

    @Test fun rejectsWrongRouteAndForgedAvifWithCleanup() {
        for (badRoute in listOf(true, false)) {
            val dir = newDir()
            try {
                val subject = AvifByteSampler(dir, Runtime.getRuntime().maxMemory()) { _, _, _ ->
                    fake(dir, if (badRoute) CodecRoute.NATIVE_HEIC else CodecRoute.NATIVE_AVIF,
                        if (badRoute) avif else ByteArray(20))
                }
                val result = subject.run(AtomicBoolean(false))
                if (Build.VERSION.SDK_INT >= 34) {
                    assertEquals(if (badRoute) "WRONG_CODEC_ROUTE" else "INVALID_OUTPUT", result.reason)
                    assertFalse(result.ready); assertTrue(dir.listFiles().isNullOrEmpty())
                }
            } finally { dir.deleteRecursively() }
        }
    }

    @Test fun cancellationDoesNotExposeSampleOrLeakFile() {
        val dir = newDir(); val stop = AtomicBoolean(false)
        var calls = 0
        try {
            val subject = AvifByteSampler(dir, Runtime.getRuntime().maxMemory()) { _, _, cancelled ->
                calls++; val file = fake(dir, CodecRoute.NATIVE_AVIF, avif)
                cancelled.set(true); file
            }
            val result = subject.run(stop)
            if (Build.VERSION.SDK_INT >= 34) {
                assertEquals(1, calls); assertTrue(result.cancelled); assertFalse(result.ready)
                assertTrue(dir.listFiles().isNullOrEmpty())
            } else assertEquals(0, calls)
        } finally { dir.deleteRecursively() }
    }
}
