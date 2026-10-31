package org.cajapersonal.fixmyshot.core

import org.junit.Assert.*
import org.junit.Test

class EditorZoomTest {
    @Test fun fineRangeDoesNotJumpSeveralMultiplesAtTheStart() {
        assertEquals(1.0, EditorZoom.fromSlider(0.0, 64.0), 1e-12)
        assertEquals(64.0, EditorZoom.fromSlider(1.0, 64.0), 1e-12)
        assertTrue(EditorZoom.fromSlider(.01, 64.0) < 1.05)
        assertEquals(8.0, EditorZoom.fromSlider(.5, 64.0), 1e-12)
    }

    @Test fun sliderRoundTripsExistingZoomAndStaysMonotone() {
        for (maximum in listOf(1.0, 1.5, 16.0, 64.0)) {
            var previous = 1.0
            for (step in 0..1000) {
                val fraction = step / 1000.0
                val zoom = EditorZoom.fromSlider(fraction, maximum)
                assertTrue(zoom >= previous)
                if (maximum > 1) assertEquals(fraction, EditorZoom.toSlider(zoom, maximum), 1e-10)
                previous = zoom
            }
        }
        assertEquals(32.0, EditorZoom.fromSlider(EditorZoom.toSlider(32.0, 64.0), 64.0), 1e-10)
    }

    @Test fun buttonsMakeSmallReversibleChangesAndRespectBothEnds() {
        assertEquals(1.02, EditorZoom.step(1.0, 1, 64.0), 1e-12)
        assertEquals(3.0, EditorZoom.step(EditorZoom.step(3.0, 1, 64.0), -1, 64.0), 1e-12)
        assertEquals(1.0, EditorZoom.step(1.0, -1, 64.0), 0.0)
        assertEquals(64.0, EditorZoom.step(64.0, 1, 64.0), 0.0)
        assertEquals(1.0, EditorZoom.toSlider(64.0, 64.0), 0.0)
    }
}
