package org.cajapersonal.fixmyshot.core

import kotlin.math.min

/** Event-local geometry. No draft, export validation, journal or image allocation belongs here. */
data class CropMotion(val focus: CropFocus, val geometry: CropGeometry)

object CropInteraction {
    fun start(source: PixelSize, ratio: ExactRatio, focus: CropFocus,
        exactOutput: PixelSize? = null): CropMotion {
        val geometry = CropPlanner.plan(source, ratio, focus, exactOutput = exactOutput)
        return CropMotion(CropFocus(geometry.normalizedCenterX, geometry.normalizedCenterY, focus.zoom),
            if (geometry.focusWasClamped) geometry.copy(focusWasClamped = false) else geometry)
    }

    /** One exact planner call per move. The point beneath the fingers stays anchored until clamped. */
    fun move(source: PixelSize, ratio: ExactRatio, current: CropMotion,
        viewportWidth: Double, viewportHeight: Double, centroidX: Double, centroidY: Double,
        panX: Double, panY: Double, scale: Double, exactOutput: PixelSize? = null): CropMotion {
        require(viewportWidth.isFinite() && viewportHeight.isFinite() && centroidX.isFinite() &&
            centroidY.isFinite() && panX.isFinite() && panY.isFinite() && scale.isFinite())
        require(viewportWidth > 0 && viewportHeight > 0 && scale > 0)
        val visible = current.geometry.visibleSource
        val output = current.geometry.output
        val viewScale = min(viewportWidth / output.width, viewportHeight / output.height)
        val frameWidth = output.width * viewScale
        val frameHeight = output.height * viewScale
        val u = ((centroidX - (viewportWidth - frameWidth) / 2) / frameWidth).coerceIn(0.0, 1.0)
        val v = ((centroidY - (viewportHeight - frameHeight) / 2) / frameHeight).coerceIn(0.0, 1.0)
        val zoomLimit = if (exactOutput != null) 64.0 else min(64.0,
            min(visible.width * current.focus.zoom / ratio.p, visible.height * current.focus.zoom / ratio.q)).coerceAtLeast(1.0)
        val zoom = (current.focus.zoom * scale).coerceIn(1.0, zoomLimit)
        val width = visible.width * current.focus.zoom / zoom
        val height = visible.height * current.focus.zoom / zoom
        val x = visible.left + visible.width * u - (u - .5) * width - panX / frameWidth * width
        val y = visible.top + visible.height * v - (v - .5) * height - panY / frameHeight * height
        return start(source, ratio, CropFocus((x / source.width).coerceIn(0.0, 1.0),
            (y / source.height).coerceIn(0.0, 1.0), zoom), exactOutput)
    }

    fun removedFraction(source: PixelSize, geometry: CropGeometry): Double =
        (1 - geometry.visibleSource.width * geometry.visibleSource.height / source.pixels).coerceIn(0.0, 1.0)
}
