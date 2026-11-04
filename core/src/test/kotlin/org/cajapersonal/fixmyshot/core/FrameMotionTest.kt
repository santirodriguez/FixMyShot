package org.cajapersonal.fixmyshot.core

import org.junit.Assert.*
import org.junit.Test

class FrameMotionTest {
    @Test fun pinchOutCrossesCropAndContainWithoutLosingOriginalPixels() {
        val source = PixelSize(690, 1536)
        val ratio = ExactRatio.of(9, 16)
        val start = FrameInteraction.start(source, ratio, FrameFocus())
        val intermediate = FrameInteraction.move(source, ratio, start,
            1080.0, 1920.0, 540.0, 960.0, 0.0, 0.0, 0.85)
        val contained = FrameInteraction.move(source, ratio, intermediate,
            1080.0, 1920.0, 540.0, 960.0, 0.0, 0.0, 0.5)
        assertTrue(start.geometry.cropped)
        assertTrue(intermediate.geometry.canvas.width > start.geometry.canvas.width)
        assertFalse(contained.geometry.cropped)
        assertEquals(PixelSize(864, 1536), contained.geometry.canvas)
        assertEquals(source.width, contained.geometry.placement.width)
        assertEquals(source.height, contained.geometry.placement.height)
    }

    @Test fun dragMovesCropTowardFingerWithClampedExactSourceCoordinates() {
        val source = PixelSize(690, 1536)
        val ratio = ExactRatio.of(9, 16)
        val start = FrameInteraction.start(source, ratio, FrameFocus())
        val dragged = FrameInteraction.move(source, ratio, start,
            1080.0, 1920.0, 540.0, 960.0, 45.0, -45.0, 1.0)
        assertTrue(dragged.geometry.placement.left > start.geometry.placement.left)
        assertTrue(dragged.geometry.placement.top < start.geometry.placement.top)
        assertTrue(dragged.focus.x in 0.0..1.0)
        assertTrue(dragged.focus.y in 0.0..1.0)
    }
}
