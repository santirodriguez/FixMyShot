package org.cajapersonal.fixmyshot.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/** Separate from legacy CropFocus: older crop plans retain zoom >= 1 and their exact fingerprints.
 * Below 1, the source occupies less of the ratio-correct output canvas and a genuine matte appears.
 * Every source pixel is either copied at native size or clipped; no fictitious pixels or stretching.
 */
data class FrameFocus(
    val x: Double = 0.5,
    val y: Double = 0.5,
    val zoom: Double = 1.0,
) {
    init {
        require(x.isFinite() && y.isFinite() && x in 0.0..1.0 && y in 0.0..1.0)
        require(zoom.isFinite() && zoom in 0.01..64.0)
    }
}

data class FrameGeometry(
    val canvas: PixelSize,
    val placement: PixelPlacement,
    /** Effective zoom is quantized by the destination ratio's integer pixel grid. */
    val effectiveZoom: Double,
    val fillThreshold: Double,
) {
    val cropped: Boolean get() = placement.left < 0 || placement.top < 0 ||
        placement.left.toLong() + placement.width > canvas.width ||
        placement.top.toLong() + placement.height > canvas.height
    val padded: Boolean get() = placement.left > 0 || placement.top > 0 ||
        placement.left.toLong() + placement.width < canvas.width ||
        placement.top.toLong() + placement.height < canvas.height

    val trimmedFraction: Double get() {
        val visibleWidth = minOf(canvas.width.toLong(), placement.width.toLong())
        val visibleHeight = minOf(canvas.height.toLong(), placement.height.toLong())
        return 1.0 - (visibleWidth.toDouble() * visibleHeight /
            (placement.width.toDouble() * placement.height))
    }
}

/** Pure fixed-source-pixel framing. PreservePixels output side grows toward native contain,
 * instead of enlarging a screenshot to a fixed, arbitrarily bigger full-bleed canvas.
 */
object FramePlanner {
    fun plan(source: PixelSize, ratio: ExactRatio, focus: FrameFocus,
        maximumOutputPixels: Long = GeometryPlanner.DEFAULT_MAX_CANVAS_PIXELS): FrameGeometry {
        require(maximumOutputPixels > 0L)
        val minimumUnits = floor(minOf(source.width.toDouble() / ratio.p,
            source.height.toDouble() / ratio.q)).toLong()
        require(minimumUnits > 0L) { "Source is too small for this exact ratio" }
        val containedUnits = maxOf(ceil(source.width.toDouble() / ratio.p).toLong(),
            ceil(source.height.toDouble() / ratio.q).toLong())
        require(containedUnits >= minimumUnits)
        // No need to create empty canvas beyond the fully contained original.
        val wantedUnits = floor(minimumUnits / focus.zoom).toLong()
            .coerceIn(1L, containedUnits)
        val width = Math.multiplyExact(wantedUnits, ratio.p)
        val height = Math.multiplyExact(wantedUnits, ratio.q)
        require(width in 1..Int.MAX_VALUE.toLong() && height in 1..Int.MAX_VALUE.toLong())
        require(Math.multiplyExact(width, height) <= maximumOutputPixels) { "Frame exceeds pixel budget" }
        val freeX = width - source.width
        val freeY = height - source.height
        val left = (freeX.toDouble() * focus.x).roundToLong().coerceIn(
            minOf(0L, freeX), maxOf(0L, freeX)).toInt()
        val top = (freeY.toDouble() * focus.y).roundToLong().coerceIn(
            minOf(0L, freeY), maxOf(0L, freeY)).toInt()
        return FrameGeometry(PixelSize(width.toInt(), height.toInt()),
            PixelPlacement(left, top, source.width, source.height),
            minimumUnits.toDouble() / wantedUnits.toDouble(),
            minimumUnits.toDouble() / containedUnits.toDouble())
    }
}
