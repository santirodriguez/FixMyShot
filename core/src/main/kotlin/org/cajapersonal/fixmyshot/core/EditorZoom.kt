package org.cajapersonal.fixmyshot.core

import kotlin.math.ln
import kotlin.math.pow

/** Equal slider distances represent equal relative changes, retaining the existing 1x..64x range. */
object EditorZoom {
    fun fromSlider(position: Double, maximum: Double): Double {
        require(maximum.isFinite() && maximum in 1.0..64.0)
        return maximum.pow(position.coerceIn(0.0, 1.0))
    }

    fun toSlider(zoom: Double, maximum: Double): Double {
        require(maximum.isFinite() && maximum in 1.0..64.0)
        return if (maximum == 1.0) 0.0 else ln(zoom.coerceIn(1.0, maximum)) / ln(maximum)
    }

    fun step(zoom: Double, direction: Int, maximum: Double): Double =
        (zoom * 1.02.pow(direction.coerceIn(-1, 1))).coerceIn(1.0, maximum)
}
