package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.onClick
import kotlin.math.abs
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.*

/** A retained original sample, drawn with the exact export mapping. No raster is created here. */
@Composable
internal fun ReviewCanvas(sample: Bitmap, image: DraftImage, plan: PreviewPlan,
    enabled: Boolean, showOriginalOutline: Boolean, modifier: Modifier = Modifier,
    onCrop: (CropFocus) -> Unit, onPosition: (ContainPosition) -> Unit,
    onGestureFinished: () -> Unit,
    onCropCommitted: (CropFocus) -> Unit = onCrop,
    onPositionCommitted: (ContainPosition) -> Unit = onPosition,
    onTap: (() -> Unit)? = null,
    onContainZoom: () -> Unit = {},
    onGestureStarted: () -> Unit = {},
    onGestureCancelled: () -> Unit = {},
) {
    // Frame-local edits redraw only this canvas; commit the draft only at pointer up.
    var liveFocus by remember(image.id, plan.sessionRevision, plan.itemRevision, plan.settings.fit) {
        mutableStateOf<CropFocus?>(null)
    }
    val live = remember(plan, liveFocus) {
        val focus = liveFocus
        if (focus == null || plan.settings.fit != FitMode.CROP) plan else try {
            val frame = CropPlanner.plan(image.orientedSize,
                plan.destination.cropRatio(image.orientedSize, plan.settings.size), focus,
                exactOutput = (plan.settings.size as? OutputSizeChoice.Exact)?.canvas)
            plan.copy(settings = plan.settings.copy(crop = focus), geometry = PlannedGeometry.Cropped(frame))
        } catch (_: IllegalArgumentException) { plan }
    }
    val contained = (live.geometry as? PlannedGeometry.Contained)?.value
    val cropped = (live.geometry as? PlannedGeometry.Cropped)?.value
    val output = contained?.canvas ?: cropped?.output ?: return
    val currentPlan by rememberUpdatedState(live)
    val currentPosition by rememberUpdatedState(onPosition)
    val commitPosition by rememberUpdatedState(onPositionCommitted)
    val currentTap by rememberUpdatedState(onTap)
    val requestCrop by rememberUpdatedState(onContainZoom)
    val startGesture by rememberUpdatedState(onGestureStarted)
    val cancelGesture by rememberUpdatedState(onGestureCancelled)
    val bg = remember(sample, plan.settings.background) {
        when (val choice = plan.settings.background) {
            BackgroundChoice.Automatic -> Color(ReviewBitmapRenderer.automaticColor(sample))
            BackgroundChoice.White -> Color.White
            BackgroundChoice.Black -> Color.Black
            BackgroundChoice.Transparent -> Color.Transparent
            is BackgroundChoice.CustomRgb -> Color(0xFF000000.toInt() or choice.rgb)
        }
    }
    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .10f)
    val border = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.preview_description)
    Canvas(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
        .semantics {
            contentDescription = description
            if (enabled && onTap != null) onClick { onTap(); true }
        }
        .cropGestures(image, live, enabled, { liveFocus = it }, { finished ->
            liveFocus = null
            onCropCommitted(finished)
        }, onTap, onGestureStarted, {
            liveFocus = null
            onGestureCancelled()
        })
        .pointerInput(image.id, enabled, plan.settings.fit) {
            if (!enabled || contained == null) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                startGesture()
                var next = currentPlan.settings.position
                var panTotal = Offset.Zero
                var zoomTotal = 1f
                var moved = false
                var cropRequested = false
                var completed = false
                var cancelled = false
                try {
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.isConsumed }) { cancelled = true; break }
                        val pan = event.calculatePan()
                        panTotal += pan
                        zoomTotal *= event.calculateZoom()
                        if (abs(1 - zoomTotal) * event.calculateCentroidSize(false) > viewConfiguration.touchSlop)
                            cropRequested = true
                        if (panTotal.getDistance() > viewConfiguration.touchSlop) moved = true
                        if (moved && !cropRequested) {
                            val geometry = (currentPlan.geometry as? PlannedGeometry.Contained)?.value ?: break
                            val bounds = previewOutputCanvasRect(geometry.canvas, Size(size.width.toFloat(), size.height.toFloat()))
                            val spaceX = geometry.canvas.width - geometry.placement.width
                            val spaceY = geometry.canvas.height - geometry.placement.height
                            next = ContainPosition(
                                if (spaceX > 0) (next.x + pan.x / bounds.width * geometry.canvas.width / spaceX).coerceIn(0.0, 1.0) else .5,
                                if (spaceY > 0) (next.y + pan.y / bounds.height * geometry.canvas.height / spaceY).coerceIn(0.0, 1.0) else .5)
                            currentPosition(next)
                        }
                        if (moved || cropRequested) event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (!cancelled) {
                        completed = true
                        if (cropRequested) { cancelGesture(); requestCrop() }
                        else if (moved) commitPosition(next)
                        else currentTap?.invoke()
                    }
                } finally { if (!completed) cancelGesture() }
            }
        }) {
        val bounds = previewOutputCanvasRect(output, size)
        val gap = 32f * density
        var x = -size.height
        while (x <= size.width) {
            drawLine(line, Offset(x, 0f), Offset(x + size.height, size.height), strokeWidth = density)
            x += gap
        }
        if (bg == Color.Transparent) {
            val step = 18f * density
            var y = bounds.top
            var row = 0
            while (y < bounds.bottom) {
                var xx = bounds.left
                var col = 0
                while (xx < bounds.right) {
                    drawRect(if ((row + col) % 2 == 0) Color.LightGray else Color.White,
                        Offset(xx, y), Size(minOf(step, bounds.right - xx), minOf(step, bounds.bottom - y)))
                    xx += step; col++
                }
                y += step; row++
            }
        } else drawRect(bg, bounds.topLeft, bounds.size)
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val save = canvas.save()
            try {
                canvas.clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom)
                canvas.translate(bounds.left, bounds.top)
                canvas.scale(bounds.width / output.width, bounds.height / output.height)
                ReviewBitmapRenderer.drawContent(canvas, output.width, output.height, sample,
                    image.orientedSize, output, contained?.placement ?: PixelPlacement(0, 0, output.width, output.height),
                    cropped?.visibleSource)
            } finally { canvas.restoreToCount(save) }
        }
        drawRect(Color.Black, bounds.topLeft, bounds.size, style = Stroke(3f * density))
        drawRect(border, bounds.topLeft, bounds.size, style = Stroke(1.4f * density))
        if (showOriginalOutline && contained != null) {
            val source = previewOriginalContentRect(output, size, contained)
            drawRect(Color.Black, source.topLeft, source.size, style = Stroke(3f * density))
            drawRect(Color.White, source.topLeft, source.size, style = Stroke(density))
        }
    }
}
