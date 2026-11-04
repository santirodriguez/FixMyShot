package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.review.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MetadataPolicyTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()
    private val forbidden = listOf(ExifInterface.TAG_MAKER_NOTE, ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_BODY_SERIAL_NUMBER, ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_IMAGE_UNIQUE_ID, ExifInterface.TAG_XMP)

    private fun fixture(jpeg: Boolean = false): File {
        val file = File(File(app.cacheDir, "review-sources").apply { mkdirs() },
            UUID.randomUUID().toString().replace("-", "") + ".source")
        val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setHasAlpha(false); bitmap.eraseColor(Color.BLUE); bitmap.setPixel(3, 4, Color.RED)
            file.outputStream().use { assertTrue(bitmap.compress(
                if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, "6")
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2020:01:02 03:04:05")
            setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, "2020:01:02 03:04:06")
            setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, "+03:00")
            setAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED, "+03:00")
            setAttribute(ExifInterface.TAG_MAKE, "Synthetic camera")
            setAttribute(ExifInterface.TAG_MODEL, "Fixture model")
            setAttribute(ExifInterface.TAG_LENS_MAKE, "Synthetic lens")
            setAttribute(ExifInterface.TAG_LENS_MODEL, "Fixture lens")
            setAttribute(ExifInterface.TAG_ARTIST, "Synthetic private author")
            setAttribute(ExifInterface.TAG_COPYRIGHT, "Synthetic copyright")
            setLatLong(12.5, -34.25); setAltitude(56.5)
            setAttribute(ExifInterface.TAG_MAKER_NOTE, "PRIVATE-MAKER")
            setAttribute(ExifInterface.TAG_USER_COMMENT, "PRIVATE-COMMENT")
            setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, "PRIVATE-SERIAL")
            setAttribute(ExifInterface.TAG_SOFTWARE, "PRIVATE-EDITOR")
            setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "PRIVATE-PATH")
            setAttribute(ExifInterface.TAG_IMAGE_UNIQUE_ID, "PRIVATE-ID")
            setAttribute(ExifInterface.TAG_XMP, "<private>PRIVATE-XMP</private>")
            saveAttributes()
        }
        return file
    }

    private fun run(file: File, directory: File, format: OutputFormat, metadata: MetadataPolicy): Pair<PrivateExecutionResult.Ready, EncodeRequest> {
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val image = DraftImage(file.name.removeSuffix(".source"), "synthetic-input", PixelSize(48, 64), false,
            contentSha256 = hash)
        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
            batch = EditSettings(background = BackgroundChoice.White, format = format, metadata = metadata))
            .addImages(listOf(image))
        val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
        val result = PrivateOutputExecutor(ReviewSources(app), LosslessEncoder(directory, heap), heap, directory)
            .execute(draft, recipe)
        assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
        result as PrivateExecutionResult.Ready
        assertFalse(result.payloadPreserved)
        assertTrue(ReviewSources(app).matchesFingerprint(image.id, hash))
        assertTrue(PrivateCandidateVerifier.accepts(result.file, recipe.request))
        return result to recipe.request
    }

    @Test fun explicitPreservationKeepsOnlyAdvertisedFieldsAcrossPngWebpAndJpegConversion() {
        val expectedTags = setOf("DateTimeOriginal", "DateTimeDigitized", "OffsetTimeOriginal", "OffsetTimeDigitized",
            "Make", "Model", "LensMake", "LensModel", "Artist", "Copyright", "GPSLatitudeRef", "GPSLatitude",
            "GPSLongitudeRef", "GPSLongitude", "GPSAltitudeRef", "GPSAltitude")
        assertEquals(expectedTags, MetadataAllowlist.sourceTags.toSet())
        for ((jpeg, format) in listOf(false to OutputFormat.PNG, true to OutputFormat.PNG,
                false to OutputFormat.WEBP_LOSSLESS)) {
            val source = fixture(jpeg)
            val directory = File(app.cacheDir, "metadata-preserve-" + UUID.randomUUID())
            try {
                val original = ExifInterface(source)
                val (result, request) = run(source, directory, format, MetadataPolicy.PRESERVE_ALLOWLIST)
                val output = ExifInterface(result.file)
                for (tag in MetadataAllowlist.sourceTags) {
                    assertNotNull("source fixture $tag", original.getAttribute(tag))
                    assertEquals("$format/$tag", original.getAttribute(tag), output.getAttribute(tag))
                }
                for (tag in forbidden) assertNull("forbidden $tag", output.getAttribute(tag))
                assertFalse(output.hasThumbnail())
                assertEquals(1, output.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0))
                assertEquals(64, output.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0))
                assertEquals(64, output.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0))
                assertFalse(PrivateCandidateVerifier.accepts(result.file,
                    request.copy(metadataPolicy = MetadataPolicy.REMOVE_IDENTIFYING)))
                val pixels = BitmapFactory.decodeFile(result.file.path)!!
                try {
                    assertEquals(64, pixels.width); assertEquals(64, pixels.height)
                    assertEquals(Color.WHITE, pixels.getPixel(0, 0))
                    if (!jpeg) assertEquals(Color.RED, pixels.getPixel(51, 3))
                } finally { pixels.recycle() }
                ExifInterface(result.file).apply {
                    setAttribute(ExifInterface.TAG_LIGHT_SOURCE, null)
                    setAttribute(ExifInterface.TAG_USER_COMMENT, "injected forbidden comment"); saveAttributes()
                }
                assertFalse(PrivateCandidateVerifier.accepts(result.file, request))
            } finally { source.delete(); directory.deleteRecursively() }
        }
    }

    @Test fun defaultRemovalDoesNotLeakAnySelectedOrOpaqueSourceField() {
        val source = fixture()
        val directory = File(app.cacheDir, "metadata-remove-" + UUID.randomUUID())
        try {
            for (format in listOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
                val (result, _) = run(source, directory, format, MetadataPolicy.REMOVE_IDENTIFYING)
                val output = ExifInterface(result.file)
                for (tag in MetadataAllowlist.sourceTags + forbidden) assertNull(tag, output.getAttribute(tag))
                assertFalse(output.hasThumbnail()); assertNull(output.latLong)
            }
            ExifInterface(source).apply { setAttribute(ExifInterface.TAG_ARTIST, "A".repeat(513)); saveAttributes() }
            val (bounded, _) = run(source, directory, OutputFormat.PNG, MetadataPolicy.PRESERVE_ALLOWLIST)
            assertNull(ExifInterface(bounded.file).getAttribute(ExifInterface.TAG_ARTIST))
            assertEquals("Synthetic camera", ExifInterface(bounded.file).getAttribute(ExifInterface.TAG_MAKE))
            assertEquals(MetadataPolicy.REMOVE_IDENTIFYING, DraftSession().batch.metadata)
        } finally { source.delete(); directory.deleteRecursively() }
    }

    @Test fun closedTiffVerifierRejectsUnknownTagsOffsetsCyclesAndStaleDimensions() {
        val request = EncodeRequest(PixelSize(32, 32), metadataPolicy = MetadataPolicy.PRESERVE_ALLOWLIST)
        fun valid(): ByteArray {
            val b = ByteBuffer.allocate(92).order(ByteOrder.LITTLE_ENDIAN)
            b.put(73.toByte()); b.put(73.toByte()); b.putShort(42.toShort()); b.putInt(8)
            b.putShort(4.toShort())
            fun entry(tag: Int, type: Int, value: Int) {
                b.putShort(tag.toShort()); b.putShort(type.toShort()); b.putInt(1); b.putInt(value)
            }
            entry(0x100, 4, 32); entry(0x101, 4, 32); entry(0x112, 3, 1); entry(0x8769, 4, 62)
            b.putInt(0); b.putShort(2.toShort())
            entry(0xa002, 4, 32); entry(0xa003, 4, 32); b.putInt(0)
            return b.array()
        }
        assertTrue(ExifAllowlistStructure.accepts(valid(), request))
        fun rejected(edit: (ByteBuffer) -> Unit) {
            val bytes = valid(); edit(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
            assertFalse(ExifAllowlistStructure.accepts(bytes, request))
        }
        rejected { it.putShort(10, 0x9286.toShort()) } // UserComment outside primary allowlist.
        rejected { it.putInt(54, Int.MAX_VALUE) } // Directory pointer outside container.
        rejected { it.putInt(58, 8) } // Nonzero next IFD/thumbnail pointer.
        rejected { it.putInt(18, 99) } // Stale width.
        rejected { it.putShort(8, 0x7fff.toShort()) } // Unbounded directory count.
        rejected { it.putInt(54, 8) } // Cyclic directory.
        assertFalse(ExifAllowlistStructure.accepts(ByteArray(ExifAllowlistStructure.MAX_BYTES + 1), request))
    }
}
