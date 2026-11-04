package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.review.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID

/** Only synthetic app-private sources; no gallery/SAF writing or manual hardware claim. */
@RunWith(AndroidJUnit4::class)
class PrivateOutputExecutorTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()

    private data class Fixture(val image: DraftImage, val file: File)
    private fun fixture(width: Int, height: Int, alpha: Boolean = false,
                        orientation: Int = ExifInterface.ORIENTATION_NORMAL): Fixture {
        val dir = File(context.cacheDir, "review-sources").apply { mkdirs() }
        val id = UUID.randomUUID().toString().replace("-", "")
        val file = File(dir, "$id.source")
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                bitmap.setHasAlpha(alpha)
                bitmap.eraseColor(if (alpha) Color.TRANSPARENT else Color.BLUE)
                bitmap.setPixel(width / 2, height / 2, Color.RED)
                file.outputStream().use { output ->
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                }
            } finally { bitmap.recycle() }
        } else {
            // A deterministic real JPEG with EXIF orientation 6, not an API-dependent writer.
            require(!alpha && width == 36 && height == 60 &&
                orientation == ExifInterface.ORIENTATION_ROTATE_90)
            InstrumentationRegistry.getInstrumentation().context.assets
                .open("orientation6_solid_36x60.jpg").use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            val parsed = FileInputStream(file).use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            }
            assertEquals(orientation, parsed)
        }
        val sha = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val rotated = orientation in setOf(5, 6, 7, 8)
        return Fixture(DraftImage(id, "synthetic-" + id,
            if (rotated) PixelSize(height, width) else PixelSize(width, height),
            alpha, contentSha256 = sha), file)
    }

    private fun executor(directory: File) =
        PrivateOutputExecutor(ReviewSources(context), LosslessEncoder(directory, heap), heap)

    @Test fun pngPaddedSourcePixelsRemainExactAndOriginalFingerprintSurvives() {
        val source = fixture(40, 80)
        val directory = File(context.cacheDir, "private-png-" + System.nanoTime())
        try {
            val draft = DraftSession(batch = EditSettings(background = BackgroundChoice.CustomRgb(0x12AB34)))
                .addImages(listOf(source.image))
            val frozen = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            assertEquals(PixelSize(320, 320), frozen.canvas)
            val sourceBytes = source.file.readBytes()
            val retained = File(context.cacheDir, "private-retained-" + System.nanoTime()).apply { mkdirs() }
            try {
                val sentinel = File(retained, "previous-candidate.png").apply { writeBytes(byteArrayOf(2, 4, 6)) }
                val blocked = PrivateOutputExecutor(ReviewSources(context), LosslessEncoder(retained, heap), heap,
                    copyDirectory = retained, hasWorkingSpace = { false }).execute(draft, frozen)
                assertEquals(PrivateExecutionResult.Failed(PrivateExecutionFailure.PRIVATE_STORAGE_LOW), blocked)
                assertEquals(listOf(sentinel.name), retained.listFiles()!!.map { it.name })
                assertArrayEquals(byteArrayOf(2, 4, 6), sentinel.readBytes())
                assertArrayEquals(sourceBytes, source.file.readBytes())
                assertTrue(ReviewSources(context).matchesFingerprint(source.image.id, frozen.sourceSha256))
            } finally { retained.deleteRecursively() }
            val result = executor(directory).execute(draft, frozen)
            assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
            result as PrivateExecutionResult.Ready
            try {
                assertTrue(PrivateCandidateVerifier.accepts(result.file, frozen.request))
                val decoded = requireNotNull(BitmapFactory.decodeFile(result.file.absolutePath))
                try {
                    val x = frozen.placement.left
                    val y = frozen.placement.top
                    assertEquals(Color.rgb(0x12, 0xAB, 0x34), decoded.getPixel(0, 0))
                    assertEquals(Color.BLUE, decoded.getPixel(x + 1, y + 1))
                    assertEquals(Color.RED, decoded.getPixel(x + 20, y + 40))
                    assertEquals(Color.rgb(0x12, 0xAB, 0x34), decoded.getPixel(319, 319))
                } finally { decoded.recycle() }
                assertTrue(ReviewSources(context).matchesFingerprint(source.image.id, result.sourceSha256))
            } finally { result.file.delete() }
        } finally { source.file.delete(); directory.deleteRecursively() }
    }

    @Test fun actualWebpLosslessPreservesTransparentPixelsAndPlanGeometry() {
        val source = fixture(64, 32, alpha = true)
        val directory = File(context.cacheDir, "private-webp-" + System.nanoTime())
        try {
            val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
                batch = EditSettings(format = OutputFormat.WEBP_LOSSLESS,
                    background = BackgroundChoice.Transparent)).addImages(listOf(source.image))
            val frozen = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            val result = executor(directory).execute(draft, frozen)
            assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
            result as PrivateExecutionResult.Ready
            try {
                assertEquals(Color.TRANSPARENT, result.resolvedBackgroundArgb)
                assertEquals(if (Build.VERSION.SDK_INT >= 30) CodecRoute.ANDROID_WEBP_LOSSLESS else CodecRoute.LIBWEBP,
                    result.codecRoute)
                val decoded = requireNotNull(BitmapFactory.decodeFile(result.file.absolutePath))
                try {
                    assertEquals(64, decoded.width)
                    assertEquals(64, decoded.height)
                    assertEquals(0, Color.alpha(decoded.getPixel(0, 0)))
                    assertEquals(Color.RED, decoded.getPixel(32, frozen.placement.top + 16))
                    assertEquals(0, Color.alpha(decoded.getPixel(0, 63)))
                } finally { decoded.recycle() }
            } finally { result.file.delete() }
        } finally { source.file.delete(); directory.deleteRecursively() }
    }

    @Test fun stalePlanAndModifiedSourceCannotCreateAnEncodedCandidate() {
        val source = fixture(48, 96)
        val directory = File(context.cacheDir, "private-reject-" + System.nanoTime())
        try {
            val draft = DraftSession().addImages(listOf(source.image))
            val frozen = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            val edited = draft.setBatch(draft.batch.copy(background = BackgroundChoice.Black))
            assertEquals(PrivateExecutionResult.Failed(PrivateExecutionFailure.STALE_PLAN),
                executor(directory).execute(edited, frozen))
            source.file.appendBytes(byteArrayOf(1))
            assertEquals(PrivateExecutionResult.Failed(PrivateExecutionFailure.SOURCE_CHANGED),
                executor(directory).execute(draft, frozen))
            assertEquals(0, directory.listFiles()?.size ?: 0)
        } finally { source.file.delete(); directory.deleteRecursively() }
    }

    @Test fun automaticDarkBackgroundIsResolvedFromFullOriginal() {
        val source = fixture(44, 88)
        val directory = File(context.cacheDir, "private-auto-" + System.nanoTime())
        try {
            val draft = DraftSession().addImages(listOf(source.image))
            val frozen = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            val result = executor(directory).execute(draft, frozen)
            assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
            result as PrivateExecutionResult.Ready
            try {
                assertEquals(Color.rgb(18, 18, 18), result.resolvedBackgroundArgb)
                val decoded = requireNotNull(BitmapFactory.decodeFile(result.file.absolutePath))
                try { assertEquals(Color.rgb(18, 18, 18), decoded.getPixel(0, 0)) }
                finally { decoded.recycle() }
            } finally { result.file.delete() }
        } finally { source.file.delete(); directory.deleteRecursively() }
    }

    @Test fun rotatedJpegIsNormalizedBeforeAnOpaquePngCandidate() {
        val source = fixture(36, 60, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        val directory = File(context.cacheDir, "private-jpeg-" + System.nanoTime())
        try {
            val draft = DraftSession(destination = DestinationChoice.Custom.of(3, 2),
                batch = EditSettings(background = BackgroundChoice.White)).addImages(listOf(source.image))
            val frozen = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            assertEquals(PixelSize(60, 36), source.image.orientedSize)
            val result = executor(directory).execute(draft, frozen)
            assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
            result as PrivateExecutionResult.Ready
            try {
                assertTrue(PrivateCandidateVerifier.accepts(result.file, frozen.request))
                val decoded = requireNotNull(BitmapFactory.decodeFile(result.file.absolutePath))
                try {
                    assertEquals(60, decoded.width)
                    assertEquals(40, decoded.height)
                    assertTrue(Color.blue(decoded.getPixel(1, frozen.placement.top + 1)) > 200)
                } finally { decoded.recycle() }
            } finally { result.file.delete() }
        } finally { source.file.delete(); directory.deleteRecursively() }
    }

    @Test fun croppedPngMatchesTheSharedPreviewAndOriginalPixels() {
        val source = fixture(80, 120)
        val directory = File(context.cacheDir, "private-crop-" + System.nanoTime())
        try {
            val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
                batch = EditSettings(fit = FitMode.CROP, crop = CropFocus(0.5, 0.5, 2.0),
                    background = BackgroundChoice.White)).addImages(listOf(source.image))
            val frozen = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            assertEquals(SourceRectangle(20.0, 40.0, 40.0, 40.0), frozen.crop)
            val result = executor(directory).execute(draft, frozen)
            assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
            result as PrivateExecutionResult.Ready
            try {
                val decoded = requireNotNull(BitmapFactory.decodeFile(result.file.absolutePath))
                val original = ReviewSources(context).decodeOriginal(source.image.id, source.image.orientedSize)
                try {
                    val preview = ReviewBitmapRenderer.preview(original, source.image.orientedSize,
                        draft.plans(Build.VERSION.SDK_INT).single()).image
                    try {
                        assertEquals(40, decoded.width)
                        assertEquals(40, decoded.height)
                        for (y in 0 until 40) for (x in 0 until 40) {
                            assertEquals(original.getPixel(x + 20, y + 40), decoded.getPixel(x, y))
                            assertEquals(preview.getPixel(x, y), decoded.getPixel(x, y))
                        }
                    } finally { preview.recycle() }
                } finally { decoded.recycle(); original.recycle() }
                assertTrue(ReviewSources(context).matchesFingerprint(source.image.id, result.sourceSha256))
            } finally { result.file.delete() }
        } finally { source.file.delete(); directory.deleteRecursively() }
    }

    @Test fun damagedPrivateContainerIsRejected() {
        val source = fixture(40, 80)
        val directory = File(context.cacheDir, "private-damage-" + System.nanoTime())
        try {
            val draft = DraftSession().addImages(listOf(source.image))
            val frozen = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            val result = executor(directory).execute(draft, frozen)
            assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
            result as PrivateExecutionResult.Ready
            try {
                result.file.appendBytes(byteArrayOf(1))
                assertFalse(PrivateCandidateVerifier.accepts(result.file, frozen.request))
            } finally { result.file.delete() }
        } finally { source.file.delete(); directory.deleteRecursively() }
    }

    @Test fun exactCropPreviewAndFinalPixelsAgreeForReductionAndOptedInEnlargement() {
        val source = fixture(80, 120)
        val directory = File(context.cacheDir, "private-exact-crop-" + System.nanoTime())
        try {
            for (size in listOf(20, 100)) {
                val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
                    batch = EditSettings(fit = FitMode.CROP, crop = CropFocus(0.5, 0.5, 2.0),
                        size = OutputSizeChoice.Exact(PixelSize(size, size), allowEnlargement = size > 40)))
                    .addImages(listOf(source.image))
                val frozen = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
                assertEquals(SourceRectangle(20.0, 40.0, 40.0, 40.0), frozen.crop)
                val result = executor(directory).execute(draft, frozen)
                assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
                result as PrivateExecutionResult.Ready
                try {
                    val decoded = requireNotNull(BitmapFactory.decodeFile(result.file.absolutePath))
                    val original = ReviewSources(context).decodeOriginal(source.image.id, source.image.orientedSize)
                    val preview = ReviewBitmapRenderer.preview(original, source.image.orientedSize,
                        draft.plans(Build.VERSION.SDK_INT).single()).image
                    try {
                        assertEquals(size, decoded.width); assertEquals(size, decoded.height)
                        for (y in 0 until size) for (x in 0 until size) assertEquals(preview.getPixel(x, y), decoded.getPixel(x, y))
                        assertTrue(ReviewSources(context).matchesFingerprint(source.image.id, result.sourceSha256))
                    } finally { decoded.recycle(); preview.recycle(); original.recycle() }
                } finally { result.file.delete() }
            }
        } finally { source.file.delete(); directory.deleteRecursively() }
    }
}

