package org.cajapersonal.fixmyshot.core

import org.junit.Assert.*
import org.junit.Test

class FramePlannerTest {
    private val source = PixelSize(690, 1536)
    private val ratio = ExactRatio.of(9, 16)

    @Test fun crossingFillThresholdExposesRealBackgroundWithoutScalingPixels() {
        val fill = FramePlanner.plan(source, ratio, FrameFocus(zoom = 1.0))
        val midway = FramePlanner.plan(source, ratio, FrameFocus(zoom = 0.85))
        val contain = FramePlanner.plan(source, ratio, FrameFocus(zoom = .01))
        assertEquals(PixelSize(684, 1216), fill.canvas)
        assertTrue(fill.cropped)
        assertFalse(fill.padded)
        assertTrue(midway.canvas.width > fill.canvas.width)
        assertTrue(midway.canvas.width < contain.canvas.width)
        assertTrue(midway.cropped || midway.padded)
        assertEquals(PixelSize(864, 1536), contain.canvas)
        assertFalse(contain.cropped)
        assertTrue(contain.padded)
        assertEquals(source.width, contain.placement.width)
        assertEquals(source.height, contain.placement.height)
        assertEquals(0.0, contain.trimmedFraction, 1e-9)
    }

    @Test fun leftAndRightAnchorsMoveSourceWithinActualClippingOrPadding() {
        val left = FramePlanner.plan(source, ratio, FrameFocus(x = 0.0, y = 0.0, zoom = 1.0))
        val right = FramePlanner.plan(source, ratio, FrameFocus(x = 1.0, y = 1.0, zoom = 1.0))
        assertEquals(0, left.placement.left)
        assertEquals(0, left.placement.top)
        assertEquals(-6, right.placement.left)
        assertEquals(-320, right.placement.top)
        val paddedLeft = FramePlanner.plan(source, ratio, FrameFocus(x = 0.0, zoom = .01))
        val paddedRight = FramePlanner.plan(source, ratio, FrameFocus(x = 1.0, zoom = .01))
        assertEquals(0, paddedLeft.placement.left)
        assertEquals(174, paddedRight.placement.left)
    }

    @Test fun pixelGridRemainsExactAndBoundedForManyZoomLevels() {
        repeat(2001) { index ->
            val zoom = .01 + index * 64.0 / 2000.0
            val state = FramePlanner.plan(PixelSize(1344, 2992), ratio, FrameFocus(zoom = zoom))
            assertEquals(0L, state.canvas.width.toLong() * ratio.q -
                state.canvas.height.toLong() * ratio.p)
            assertEquals(1344, state.placement.width)
            assertEquals(2992, state.placement.height)
            assertTrue(state.canvas.pixels <= GeometryPlanner.DEFAULT_MAX_CANVAS_PIXELS)
            assertTrue(state.trimmedFraction >= 0.0 && state.trimmedFraction <= 1.0)
        }
    }

    @Test fun invalidFocusOrOutputBudgetNeverProducesAPlan() {
        assertThrows(IllegalArgumentException::class.java) { FrameFocus(zoom = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { FrameFocus(x = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) {
            FramePlanner.plan(source, ratio, FrameFocus(), maximumOutputPixels = 50)
        }
    }
}
