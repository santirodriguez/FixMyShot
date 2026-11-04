package org.cajapersonal.fixmyshot.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CropInteractionTest {
    private val source = PixelSize(1344, 2992)
    private val ratio = ExactRatio.of(9, 16)

    @Test fun pinchKeepsTheSourcePointUnderTheFingers() {
        val before = CropInteraction.start(source, ratio, CropFocus(zoom = 2.0))
        val after = CropInteraction.move(source, ratio, before, 900.0, 1600.0,
            675.0, 640.0, 0.0, 0.0, 1.2)
        val a = before.geometry.visibleSource; val b = after.geometry.visibleSource
        assertEquals(a.left + a.width * .75, b.left + b.width * .75, 1e-8)
        assertEquals(a.top + a.height * .4, b.top + b.height * .4, 1e-8)
        assertEquals(2.4, after.focus.zoom, 1e-12)
    }

    @Test fun movementAlwaysUsesTheSameExactExportPlanner() {
        val random = Random(314)
        var current = CropInteraction.start(source, ratio, CropFocus())
        repeat(1001) {
            current = CropInteraction.move(source, ratio, current, 1080.0, 1400.0,
                random.nextDouble(0.0, 1080.0), random.nextDouble(0.0, 1400.0),
                random.nextDouble(-70.0, 70.0), random.nextDouble(-90.0, 90.0), random.nextDouble(.8, 1.25))
            val geometry = current.geometry
            val expected = CropPlanner.plan(source, ratio, current.focus)
            assertEquals(expected, geometry)
            assertTrue(geometry.visibleSource.left >= 0 && geometry.visibleSource.top >= 0)
            assertTrue(geometry.visibleSource.left + geometry.visibleSource.width <= source.width + 1e-7)
            assertTrue(geometry.visibleSource.top + geometry.visibleSource.height <= source.height + 1e-7)
            assertEquals(geometry.output.width * 16L, geometry.output.height * 9L)
            assertFalse(geometry.enlarged)
        }
    }

    @Test fun exactOutputAndLetterboxingDoNotChangeFingerMapping() {
        val exact = PixelSize(1080, 1920)
        val before = CropInteraction.start(source, ratio, CropFocus(zoom = 2.0), exact)
        val after = CropInteraction.move(source, ratio, before, 1400.0, 1600.0,
            700.0, 800.0, 12.0, -10.0, 1.0, exact)
        assertEquals(exact, after.geometry.output)
        assertEquals(before.geometry.visibleSource.left - 12.0 / 900 * before.geometry.visibleSource.width,
            after.geometry.visibleSource.left, 1e-8)
        assertEquals(before.geometry.visibleSource.top + 10.0 / 1600 * before.geometry.visibleSource.height,
            after.geometry.visibleSource.top, 1e-8)
    }

    @Test fun nativeResolutionZoomAndCropDisclosureStayHonest() {
        val initial = CropInteraction.start(source, ratio, CropFocus())
        assertEquals(0.2014260249554367, CropInteraction.removedFraction(source, initial.geometry), 1e-12)
        val zoomed = CropInteraction.move(source, ratio, initial, 900.0, 1600.0, 450.0, 800.0, 0.0, 0.0, 2.0)
        assertTrue(zoomed.geometry.output.pixels < initial.geometry.output.pixels)
        assertTrue(CropInteraction.removedFraction(source, zoomed.geometry) > .7)
        val small = CropInteraction.start(PixelSize(65, 67), ratio, CropFocus())
        val clamped = CropInteraction.move(PixelSize(65, 67), ratio, small, 900.0, 1600.0,
            450.0, 800.0, 0.0, 0.0, 1000.0)
        assertEquals(PixelSize(9, 16), clamped.geometry.output)
    }

    @Test fun malformedInputCannotCreateNonfiniteTransforms() {
        val initial = CropInteraction.start(source, ratio, CropFocus())
        for (scale in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            try {
                CropInteraction.move(source, ratio, initial, 900.0, 1600.0, 0.0, 0.0, 0.0, 0.0, scale)
                fail("Invalid scale was accepted")
            } catch (_: IllegalArgumentException) { }
        }
    }
}
