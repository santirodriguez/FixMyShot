package org.cajapersonal.fixmyshot.core

import kotlin.math.min

/** Pure event-local flexible framing. No session, journal or output bitmap per gesture tick. */
data class FrameMotion(val focus: FrameFocus, val geometry: FrameGeometry)

object FrameInteraction {
    fun start(source: PixelSize, ratio: ExactRatio, focus: FrameFocus): FrameMotion =
        FrameMotion(focus, FramePlanner.plan(source, ratio, focus))

    /** Anchor the source pixel beneath the touch centroid as the output frame zooms. */
    fun move(source: PixelSize, ratio: ExactRatio, motion: FrameMotion,
        viewportWidth: Double, viewportHeight: Double,
        centroidX: Double, centroidY: Double, panX: Double, panY: Double,
        zoomScale: Double): FrameMotion {
        require(listOf(viewportWidth, viewportHeight, centroidX, centroidY, panX, panY, zoomScale)
            .all { it.isFinite() })
        require(viewportWidth > 0 && viewportHeight > 0 && zoomScale > 0)
        val previous = motion.geometry
        val previousScale = min(viewportWidth / previous.canvas.width, viewportHeight / previous.canvas.height)
        val sourceX = (centroidX - (viewportWidth - previous.canvas.width * previousScale) / 2.0) /
            previousScale - previous.placement.left
        val sourceY = (centroidY - (viewportHeight - previous.canvas.height * previousScale) / 2.0) /
            previousScale - previous.placement.top

        val nextZoom = (motion.focus.zoom * zoomScale).coerceIn(0.01, 64.0)
        val initial = FramePlanner.plan(source, ratio, motion.focus.copy(zoom = nextZoom))
        val nextScale = min(viewportWidth / initial.canvas.width, viewportHeight / initial.canvas.height)
        val desiredX = (centroidX + panX - (viewportWidth - initial.canvas.width * nextScale) / 2.0) /
            nextScale - sourceX
        val desiredY = (centroidY + panY - (viewportHeight - initial.canvas.height * nextScale) / 2.0) /
            nextScale - sourceY
        val freeX = initial.canvas.width.toDouble() - source.width
        val freeY = initial.canvas.height.toDouble() - source.height
        val x = if (freeX == 0.0) 0.5 else (desiredX / freeX).coerceIn(0.0, 1.0)
        val y = if (freeY == 0.0) 0.5 else (desiredY / freeY).coerceIn(0.0, 1.0)
        return start(source, ratio, FrameFocus(x, y, nextZoom))
    }
}
