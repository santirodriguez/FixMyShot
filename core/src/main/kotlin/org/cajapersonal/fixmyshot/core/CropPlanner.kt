package org.cajapersonal.fixmyshot.core

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Center/focus are normalized to the unmodified, orientation-corrected source pixels. */
data class CropFocus(val x: Double = 0.5, val y: Double = 0.5, val zoom: Double = 1.0) {
    init {
        require(x.isFinite() && y.isFinite() && x in 0.0..1.0 && y in 0.0..1.0)
        require(zoom.isFinite() && zoom in 1.0..64.0)
    }
}

data class SourceRectangle(val left: Double, val top: Double, val width: Double, val height: Double) {
    init {
        require(left.isFinite() && top.isFinite() && width.isFinite() && height.isFinite())
        require(left >= 0.0 && top >= 0.0 && width > 0.0 && height > 0.0)
    }
}

data class CropGeometry(
    val visibleSource: SourceRectangle,
    val output: PixelSize,
    val normalizedCenterX: Double,
    val normalizedCenterY: Double,
    val focusWasClamped: Boolean,
) {
    val nativePixels: Long get() = output.pixels
}

/** A pure plan; the final renderer samples the original source once, not a cropped preview. */
object CropPlanner {
    fun plan(
        source: PixelSize,
        ratio: ExactRatio,
        focus: CropFocus = CropFocus(),
        maximumOutputPixels: Long = GeometryPlanner.DEFAULT_MAX_CANVAS_PIXELS,
    ): CropGeometry {
        require(maximumOutputPixels > 0L)
        val width = source.width.toDouble()
        val height = source.height.toDouble()
        val aspect = ratio.p.toDouble() / ratio.q.toDouble()
        require(aspect.isFinite() && aspect > 0.0)

        // Minimum-fill frame, without anisotropic stretching or background exposure.
        val unzoomedWidth = min(width, height * aspect)
        val unzoomedHeight = min(height, width / aspect)
        val cropWidth = unzoomedWidth / focus.zoom
        val cropHeight = unzoomedHeight / focus.zoom
        require(cropWidth.isFinite() && cropHeight.isFinite() && cropWidth > 0.0 && cropHeight > 0.0)

        val wantedX = focus.x * width
        val wantedY = focus.y * height
        val centerX = wantedX.coerceIn(cropWidth / 2.0, width - cropWidth / 2.0)
        val centerY = wantedY.coerceIn(cropHeight / 2.0, height - cropHeight / 2.0)
        val left = max(0.0, centerX - cropWidth / 2.0)
        val top = max(0.0, centerY - cropHeight / 2.0)
        require(left + cropWidth <= width + 1e-7 && top + cropHeight <= height + 1e-7)

        // Largest integer p:q raster guaranteed to fit within the visible source rectangle.
        val units = floor(min(cropWidth / ratio.p.toDouble(), cropHeight / ratio.q.toDouble())).toLong()
        require(units >= 1L) { "Crop too small for an integer output at this ratio" }
        val outWidth = Math.multiplyExact(ratio.p, units)
        val outHeight = Math.multiplyExact(ratio.q, units)
        require(outWidth in 1..Int.MAX_VALUE.toLong() && outHeight in 1..Int.MAX_VALUE.toLong())
        require(Math.multiplyExact(outWidth, outHeight) <= maximumOutputPixels)

        return CropGeometry(
            SourceRectangle(left, top, cropWidth, cropHeight),
            PixelSize(outWidth.toInt(), outHeight.toInt()),
            centerX / width, centerY / height,
            abs(centerX - wantedX) > 1e-8 || abs(centerY - wantedY) > 1e-8,
        )
    }
}
