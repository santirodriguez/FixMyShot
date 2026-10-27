package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.ContainPosition
import org.cajapersonal.fixmyshot.core.DestinationCatalog
import org.cajapersonal.fixmyshot.core.DestinationChoice
import org.cajapersonal.fixmyshot.core.DraftImage
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.EditSettings
import org.cajapersonal.fixmyshot.core.ExactRatio
import org.cajapersonal.fixmyshot.core.GeometryPlanner
import org.cajapersonal.fixmyshot.core.FitMode
import org.cajapersonal.fixmyshot.core.PixelSize
import org.cajapersonal.fixmyshot.review.ReviewBitmapRenderer
import org.cajapersonal.fixmyshot.review.LatestPreviewWorker
import org.cajapersonal.fixmyshot.review.previewOriginalContentRect
import org.cajapersonal.fixmyshot.review.previewOutputCanvasRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** No external images, gallery, export or physical-device acceptance implied. */
@RunWith(AndroidJUnit4::class)
class ReviewRenderTest {
    @Test fun previewBurstKeepsOnlyTheLatestPendingTask() {
        val worker = LatestPreviewWorker()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val seen = Collections.synchronizedList(mutableListOf<Int>())
        try {
            worker.execute {
                started.countDown()
                try { release.await(); seen.add(0) }
                catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            for (value in 1..1000) worker.execute {
                seen.add(value)
                if (value == 1000) finished.countDown()
            }
            release.countDown()
            assertTrue(finished.await(5, TimeUnit.SECONDS))
            assertEquals(listOf(0, 1000), synchronized(seen) { seen.toList() })
        } finally { release.countDown(); worker.close() }
    }

    @Test fun closingPreviewWorkInterruptsRunningAndDropsPendingAndNewTasks() {
        val worker = LatestPreviewWorker()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val discardedRan = AtomicBoolean(false)
        try {
            worker.execute {
                started.countDown()
                try { release.await() }
                catch (_: InterruptedException) { Thread.currentThread().interrupt() }
                finally { stopped.countDown() }
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            worker.execute { discardedRan.set(true) }
            worker.close()
            assertTrue(stopped.await(5, TimeUnit.SECONDS))
            worker.execute { discardedRan.set(true) }
            assertFalse(discardedRan.get())
        } finally { release.countDown(); worker.close() }
    }

    private fun plan(source: PixelSize, settings: EditSettings, destination: String): org.cajapersonal.fixmyshot.core.PreviewPlan =
        DraftSession(destination = DestinationChoice.Preset(destination), batch = settings)
            .addImages(listOf(DraftImage("synthetic", "synthetic-only", source, false))).plans(26).single()

    @Test fun boundedUnscaledContainUsesTheCorePlacement() {
        val source = Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888)
        val bitmap = try {
            source.eraseColor(Color.BLUE)
            ReviewBitmapRenderer.preview(source, PixelSize(40, 80), plan(
                PixelSize(40, 80), EditSettings(background = BackgroundChoice.White),
                DestinationCatalog.PLAY_PHONE), maxLongSide = 500).image
        } finally { source.recycle() }
        try {
            assertEquals(320, bitmap.width)
            assertEquals(320, bitmap.height)
            assertEquals(Color.WHITE, bitmap.getPixel(0, 0))
            assertEquals(Color.BLUE, bitmap.getPixel(150, 140))
            assertEquals(Color.WHITE, bitmap.getPixel(319, 319))
        } finally { bitmap.recycle() }
        val movedSource = Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888)
        val moved = try {
            movedSource.eraseColor(Color.BLUE)
            ReviewBitmapRenderer.preview(movedSource, PixelSize(40, 80),
                plan(PixelSize(40, 80), EditSettings(background = BackgroundChoice.White,
                    position = ContainPosition(1.0, 0.0)), DestinationCatalog.PLAY_PHONE),
                maxLongSide = 500).image
        } finally { movedSource.recycle() }
        try {
            assertEquals(Color.WHITE, moved.getPixel(0, 0))
            assertEquals(Color.BLUE, moved.getPixel(319, 0))
            assertEquals(Color.WHITE, moved.getPixel(319, 80))
        } finally { moved.recycle() }
    }

    @Test fun edgeMedianUsesDeterministicOpaqueColors() {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            source.eraseColor(Color.WHITE)
            assertEquals(Color.WHITE, ReviewBitmapRenderer.automaticColor(source))
            source.eraseColor(Color.BLACK)
            assertEquals(Color.rgb(18, 18, 18), ReviewBitmapRenderer.automaticColor(source))
            source.eraseColor(Color.TRANSPARENT)
            assertEquals(Color.WHITE, ReviewBitmapRenderer.automaticColor(source))
        } finally { source.recycle() }
    }

    @Test fun boundedPreviewPreservesTheExactPlanDimensionsWithoutUsingSourceForExport() {
        val source = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888)
        val plan = plan(PixelSize(400, 800), EditSettings(), DestinationCatalog.SOCIAL_SQUARE)
        try {
            source.eraseColor(Color.WHITE)
            val preview = ReviewBitmapRenderer.preview(source, PixelSize(400, 800), plan, maxLongSide = 300)
            try {
                assertEquals(PixelSize(800, 800), preview.plannedOutput)
                assertTrue(preview.sampled)
                assertEquals(300, preview.image.width)
                assertEquals(300, preview.image.height)
            } finally { preview.image.recycle() }
        } finally { source.recycle() }
    }

    @Test fun originalOutlineFollowsPlayPaddingInsideLetterboxedPreview() {
        val geometry = GeometryPlanner.playPhoneEnvelope(PixelSize(1344, 2992))
        assertEquals(PixelSize(1496, 2992), geometry.canvas)
        assertEquals(76, geometry.placement.left)
        assertEquals(76, geometry.rightMargin)
        val exported = previewOutputCanvasRect(PixelSize(600, 1200), Size(360f, 320f))
        assertEquals(100f, exported.left, 0.001f)
        assertEquals(260f, exported.right, 0.001f)
        assertEquals(0f, exported.top, 0.001f)
        assertEquals(320f, exported.bottom, 0.001f)
        val bounds = previewOriginalContentRect(PixelSize(600, 1200), Size(360f, 320f), geometry)
        assertTrue(bounds.left > exported.left && bounds.right < exported.right)
        assertEquals(108.128f, bounds.left, 0.02f)
        assertEquals(251.872f, bounds.right, 0.02f)
        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(320f, bounds.bottom, 0.001f)
    }

    @Test fun originalOutlineFollowsTopBottomMarginsWithCenteredFit() {
        val geometry = GeometryPlanner.preservePixelsAtRatio(PixelSize(80, 40), ExactRatio.of(1, 1))
        val exported = previewOutputCanvasRect(PixelSize(400, 400), Size(240f, 320f))
        assertEquals(0f, exported.left, 0.001f)
        assertEquals(240f, exported.right, 0.001f)
        assertEquals(40f, exported.top, 0.001f)
        assertEquals(280f, exported.bottom, 0.001f)
        val bounds = previewOriginalContentRect(PixelSize(400, 400), Size(240f, 320f), geometry)
        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(240f, bounds.right, 0.001f)
        assertEquals(100f, bounds.top, 0.001f)
        assertEquals(220f, bounds.bottom, 0.001f)
    }

    @Test fun centeredCropUsesOriginalCoordinatesWithoutPadding() {
        val source = Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888)
        try {
            val crop = plan(PixelSize(40, 80), EditSettings(fit = FitMode.CROP), DestinationCatalog.SOCIAL_SQUARE)
            assertTrue(crop.geometry != null)
            source.eraseColor(Color.BLUE)
            source.setPixel(20, 40, Color.RED)
            val rendered = ReviewBitmapRenderer.preview(source, PixelSize(40, 80), crop)
            try {
                assertEquals(PixelSize(40, 40), rendered.plannedOutput)
                assertEquals(Color.BLUE, rendered.image.getPixel(0, 0))
                assertEquals(Color.RED, rendered.image.getPixel(20, 20))
            } finally { rendered.image.recycle() }
            source.eraseColor(Color.BLUE)
            for (x in 0 until 40) for (y in 0 until 20) source.setPixel(x, y, Color.RED)
            val movedFocus = org.cajapersonal.fixmyshot.core.CropFocus(0.5, 0.25)
            val movedPlan = crop.copy(settings = crop.settings.copy(crop = movedFocus),
                geometry = org.cajapersonal.fixmyshot.core.PlannedGeometry.Cropped(
                    org.cajapersonal.fixmyshot.core.CropPlanner.plan(PixelSize(40, 80),
                        crop.destination.cropRatio(PixelSize(40, 80)), movedFocus)))
            val centeredPreview = ReviewBitmapRenderer.preview(source, PixelSize(40, 80), crop).image
            val movedPreview = ReviewBitmapRenderer.preview(source, PixelSize(40, 80), movedPlan).image
            try {
                assertEquals(Color.BLUE, centeredPreview.getPixel(20, 1))
                assertEquals(Color.RED, movedPreview.getPixel(20, 1))
                assertEquals(40, movedPreview.height)
                assertEquals(40, movedPreview.width)
            } finally { centeredPreview.recycle(); movedPreview.recycle() }
        } finally { source.recycle() }
    }
}

