package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.DestinationCatalog
import org.cajapersonal.fixmyshot.core.DestinationChoice
import org.cajapersonal.fixmyshot.core.DraftImage
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.EditSettings
import org.cajapersonal.fixmyshot.core.FitMode
import org.cajapersonal.fixmyshot.core.PixelSize
import org.cajapersonal.fixmyshot.review.ReviewBitmapRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** No external images, gallery, export or physical-device acceptance implied. */
@RunWith(AndroidJUnit4::class)
class ReviewRenderTest {
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
        } finally { source.recycle() }
    }
}

