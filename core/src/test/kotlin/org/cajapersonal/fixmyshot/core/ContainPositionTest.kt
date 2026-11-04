package org.cajapersonal.fixmyshot.core

import org.junit.Assert.*
import org.junit.Test

/** Position only redistributes contain padding; destination/output geometry is unchanged. */
class ContainPositionTest {
    @Test fun playPhoneCenterAndBothExtremesPreserveSourceGeometry() {
        val original = GeometryPlanner.playPhoneEnvelope(PixelSize(1344, 2992))
        assertEquals(PixelPlacement(76, 0, 1344, 2992), original.placement)
        val left = original.positioned(ContainPosition(0.0, 0.0))
        val right = original.positioned(ContainPosition(1.0, 1.0))
        assertEquals(PixelPlacement(0, 0, 1344, 2992), left.placement)
        assertEquals(PixelPlacement(152, 0, 1344, 2992), right.placement)
        assertEquals(PixelSize(1496, 2992), left.canvas)
        assertEquals(original.canvas, right.canvas)
        assertFalse(left.resized)
        assertEquals(152, left.rightMargin)
        assertEquals(0, right.rightMargin)
    }

    @Test fun portraitAndOddSlackRespectAxesAndLegacyCenteredRounding() {
        val vertical = GeometryPlanner.preservePixelsAtRatio(PixelSize(80, 41), ExactRatio.of(1, 1))
        assertEquals(0, vertical.placement.left)
        assertEquals(19, vertical.placement.top)
        assertEquals(39, vertical.bottomMargin + vertical.placement.top)
        assertEquals(0, vertical.positioned(ContainPosition(1.0, 0.0)).placement.top)
        assertEquals(39, vertical.positioned(ContainPosition(0.0, 1.0)).placement.top)
        assertEquals(0, vertical.positioned(ContainPosition(0.0, 1.0)).placement.left)
        val odd = GeometryPlanner.preservePixelsAtRatio(PixelSize(3, 4), ExactRatio.of(7, 4))
        assertEquals(2, odd.placement.left)
        assertEquals(4, odd.positioned(ContainPosition(1.0, 0.5)).placement.left)
        assertEquals(0, odd.positioned(ContainPosition(0.0, 0.5)).placement.left)
    }

    @Test fun invalidNormalizedValuesFailBeforeAnyPlanIsExported() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, -0.01, 1.01)) {
            assertThrows(IllegalArgumentException::class.java) { ContainPosition(value, 0.5) }
            assertThrows(IllegalArgumentException::class.java) { ContainPosition(0.5, value) }
        }
    }
}
