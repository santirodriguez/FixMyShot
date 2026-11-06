package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.review.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in API35 16 KiB emulator validation; writes only synthetic app-private evidence. */
@RunWith(AndroidJUnit4::class)
class SixteenKPrivacyTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val evidence get() = File(app.filesDir, "ci-16k-privacy").apply { mkdirs() }

    @Test fun nativeLibwebpJniEncodesLosslessOn16kPages() {
        assertEquals(16384L, Os.sysconf(OsConstants._SC_PAGE_SIZE))
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(false)
        bitmap.eraseColor(Color.BLUE)
        bitmap.setPixel(3, 5, Color.RED)
        val destination = File(evidence, "native-webp-16k.webp")
        try {
            // NativeWebp is intentionally internal to :codec-native. Reflection is
            // confined to instrumentation so no production API/ABI is widened.
            val klass = Class.forName("org.cajapersonal.fixmyshot.codec.NativeWebp")
            val instance = klass.getField("INSTANCE").get(null)
            klass.getMethod("ensureLoaded").invoke(instance)
            val encode = klass.getMethod("encode", Bitmap::class.java, Integer.TYPE,
                AtomicBoolean::class.java, java.lang.Long.TYPE, java.lang.Boolean.TYPE)
            ParcelFileDescriptor.open(destination, ParcelFileDescriptor.MODE_CREATE or
                ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE).use { fd ->
                assertEquals(0, (encode.invoke(instance, bitmap, fd.fd,
                    AtomicBoolean(false), 120_000L, bitmap.isPremultiplied) as Number).toInt())
            }
            val reopened = checkNotNull(BitmapFactory.decodeFile(destination.absolutePath))
            try {
                assertEquals(64, reopened.width)
                assertEquals(64, reopened.height)
                assertEquals(Color.BLUE, reopened.getPixel(0, 0))
                assertEquals(Color.RED, reopened.getPixel(3, 5))
            } finally { reopened.recycle() }
        } finally { bitmap.recycle() }
    }

    @Test fun emitFourSyntheticPrivacyOutputsForIndependentHostInspection() {
        assertEquals(16384L, Os.sysconf(OsConstants._SC_PAGE_SIZE))
        // Explicitly synthetic EXIF/GPS/XMP, never camera or user media.
        val source = File(app.cacheDir, "ci-synthetic-${UUID.randomUUID()}.source")
        val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setHasAlpha(false)
            bitmap.eraseColor(Color.BLUE)
            bitmap.setPixel(3, 4, Color.RED)
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
        val temp = File(app.cacheDir, "ci-16k-privacy-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            ExifInterface(source).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, "6")
                setAttribute(ExifInterface.TAG_MAKE, "Synthetic camera")
                setAttribute(ExifInterface.TAG_ARTIST, "Synthetic private author")
                setAttribute(ExifInterface.TAG_MAKER_NOTE, "PRIVATE-MAKER")
                setAttribute(ExifInterface.TAG_USER_COMMENT, "PRIVATE-COMMENT")
                setAttribute(ExifInterface.TAG_IMAGE_UNIQUE_ID, "PRIVATE-ID")
                setAttribute(ExifInterface.TAG_XMP, "<private>PRIVATE-XMP</private>")
                setLatLong(12.5, -34.25)
                saveAttributes()
            }
            val fingerprint = MessageDigest.getInstance("SHA-256").digest(source.readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            val image = DraftImage(source.name.removeSuffix(".source"), "ci-synthetic",
                PixelSize(48, 64), false, contentSha256 = fingerprint)
            for (format in listOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
                for (policy in listOf(MetadataPolicy.REMOVE_IDENTIFYING, MetadataPolicy.PRESERVE_ALLOWLIST)) {
                    val session = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
                        batch = EditSettings(background = BackgroundChoice.White,
                            format = format, metadata = policy)).addImages(listOf(image))
                    val recipe = ExportPreparation.inspect(session, android.os.Build.VERSION.SDK_INT,
                        Runtime.getRuntime().maxMemory()).candidates.single()
                    val result = PrivateOutputExecutor(ReviewSources(app),
                        LosslessEncoder(temp, Runtime.getRuntime().maxMemory()),
                        Runtime.getRuntime().maxMemory(), temp).execute(session, recipe)
                    assertTrue("Synthetic export failed: $result", result is PrivateExecutionResult.Ready)
                    result as PrivateExecutionResult.Ready
                    assertTrue(PrivateCandidateVerifier.accepts(result.file, recipe.request))
                    val choice = if (policy == MetadataPolicy.REMOVE_IDENTIFYING) "default" else "preserve"
                    val target = File(evidence, "privacy-$choice.${format.extension}")
                    result.file.copyTo(target, overwrite = true)
                    assertTrue(target.isFile && target.length() > 0)
                }
            }
        } finally {
            source.delete()
            temp.deleteRecursively()
        }
    }
}
