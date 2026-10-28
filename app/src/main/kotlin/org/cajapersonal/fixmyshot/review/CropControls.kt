package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.CropFocus
import org.cajapersonal.fixmyshot.core.CropPlanner
import org.cajapersonal.fixmyshot.core.EditorZoom
import org.cajapersonal.fixmyshot.core.DraftImage
import org.cajapersonal.fixmyshot.core.DraftReviewFlag
import org.cajapersonal.fixmyshot.core.FitMode
import org.cajapersonal.fixmyshot.core.PlannedGeometry
import org.cajapersonal.fixmyshot.core.PreviewPlan
import org.cajapersonal.fixmyshot.core.OutputSizeChoice
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.roundToInt

/** Accessible alternatives to the same normalized transform used by drag/pinch. */
@Composable
internal fun CropControls(image: DraftImage, plan: PreviewPlan, enabled: Boolean,
    onCrop: (CropFocus) -> Unit, onFinished: () -> Unit = {}) {
    if (plan.settings.fit != FitMode.CROP) return
    val geometry = (plan.geometry as? PlannedGeometry.Cropped)?.value
    val focus = plan.settings.crop
    val base = try {
        CropPlanner.plan(image.orientedSize, plan.destination.cropRatio(image.orientedSize, plan.settings.size),
            maximumOutputPixels = Long.MAX_VALUE, exactOutput = (plan.settings.size as? OutputSizeChoice.Exact)?.canvas)
    } catch (_: IllegalArgumentException) {
        Text(stringResource(R.string.crop_unavailable))
        return
    }
    val ratio = plan.destination.cropRatio(image.orientedSize, plan.settings.size)
    val maxZoom = if (plan.settings.size is OutputSizeChoice.Exact) 64.0 else
        min(64.0, min(base.visibleSource.width / ratio.p, base.visibleSource.height / ratio.q)).coerceAtLeast(1.0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val zoomDescription = stringResource(R.string.crop_zoom, (focus.zoom * 100).roundToInt())
        val zoomLabel = stringResource(R.string.crop_zoom_control)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = enabled && focus.zoom > 1.0, onClick = {
                onCrop(focus.copy(zoom = EditorZoom.step(focus.zoom, -1, maxZoom))); onFinished()
            }, modifier = Modifier.semantics { contentDescription = "− $zoomLabel" }) { Text("−") }
            Text(zoomDescription, modifier = Modifier.weight(1f))
            OutlinedButton(enabled = enabled && focus.zoom < maxZoom, onClick = {
                onCrop(focus.copy(zoom = EditorZoom.step(focus.zoom, 1, maxZoom))); onFinished()
            }, modifier = Modifier.semantics { contentDescription = "+ $zoomLabel" }) { Text("+") }
        }
        if (maxZoom > 1.0) Slider(value = EditorZoom.toSlider(focus.zoom, maxZoom).toFloat(),
            onValueChange = { onCrop(focus.copy(zoom = EditorZoom.fromSlider(it.toDouble(), maxZoom))) },
            onValueChangeFinished = onFinished,
            modifier = Modifier.semantics {
                contentDescription = zoomLabel
                stateDescription = zoomDescription
            },
            valueRange = 0f..1f, enabled = enabled)
        val dx = (geometry?.visibleSource?.width ?: base.visibleSource.width) / image.orientedSize.width * 0.1
        val dy = (geometry?.visibleSource?.height ?: base.visibleSource.height) / image.orientedSize.height * 0.1
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onCrop(focus.copy(x = (focus.x - dx).coerceIn(0.0, 1.0))); onFinished() }) {
                Text(stringResource(R.string.crop_left))
            }
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onCrop(focus.copy(x = (focus.x + dx).coerceIn(0.0, 1.0))); onFinished() }) {
                Text(stringResource(R.string.crop_right))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onCrop(focus.copy(y = (focus.y - dy).coerceIn(0.0, 1.0))); onFinished() }) {
                Text(stringResource(R.string.crop_up))
            }
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onCrop(focus.copy(y = (focus.y + dy).coerceIn(0.0, 1.0))); onFinished() }) {
                Text(stringResource(R.string.crop_down))
            }
        }
        OutlinedButton(enabled = enabled, onClick = { onCrop(CropFocus()); onFinished() }) {
            Text(stringResource(R.string.crop_reset))
        }
        if (geometry != null && DraftReviewFlag.CROP_FOCUS_REPOSITIONED in plan.reviewFlags) {
            OutlinedButton(enabled = enabled, onClick = {
                onCrop(CropFocus(geometry.normalizedCenterX, geometry.normalizedCenterY, focus.zoom)); onFinished()
            }) { Text(stringResource(R.string.crop_accept_position)) }
        }
    }
}

/** Coordinates account for the actual fitted preview frame rather than its letterbox. */
@Composable
internal fun Modifier.cropGestures(image: DraftImage?, plan: PreviewPlan?, enabled: Boolean,
    onCrop: (CropFocus) -> Unit, onFinished: (CropFocus) -> Unit): Modifier {
    val latestPlan by rememberUpdatedState(plan)
    val latestCallback by rememberUpdatedState(onCrop)
    val latestFinish by rememberUpdatedState(onFinished)
    return pointerInput(image?.id, enabled, plan?.settings?.fit) {
        if (!enabled || image == null || plan?.settings?.fit != FitMode.CROP) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val initial = latestPlan ?: return@awaitEachGesture
            var focus = initial.settings.crop
            var accumulatedPan = Offset.Zero
            var accumulatedZoom = 1f
            var dragging = false
            var changed = false
            do {
                val event = awaitPointerEvent()
                // Keep receiving changes until pointer up; consume moves after slop.
                val pan = event.calculatePan()
                val zoom = event.calculateZoom()
                accumulatedPan += pan
                accumulatedZoom *= zoom
                if (!dragging) dragging = accumulatedPan.getDistance() > viewConfiguration.touchSlop ||
                    abs(1f - accumulatedZoom) * event.calculateCentroidSize(useCurrent = false) > viewConfiguration.touchSlop
                if (dragging && (pan != Offset.Zero || zoom != 1f)) {
                    val current = latestPlan ?: initial
                    val ratio = current.destination.cropRatio(image.orientedSize, current.settings.size)
                    val exact = (current.settings.size as? OutputSizeChoice.Exact)?.canvas
                    val geometry = CropPlanner.plan(image.orientedSize, ratio, focus, exactOutput = exact)
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val visible = geometry.visibleSource
                    val scale = min(size.width.toDouble() / geometry.output.width,
                        size.height.toDouble() / geometry.output.height)
                    val frameW = geometry.output.width * scale
                    val frameH = geometry.output.height * scale
                    if (frameW <= 0.0 || frameH <= 0.0) break
                    val u = ((centroid.x - (size.width - frameW) / 2.0) / frameW).coerceIn(0.0, 1.0)
                    val v = ((centroid.y - (size.height - frameH) / 2.0) / frameH).coerceIn(0.0, 1.0)
                    val newZoom = (focus.zoom * zoom).coerceIn(1.0, 64.0)
                    val newWidth = visible.width * focus.zoom / newZoom
                    val newHeight = visible.height * focus.zoom / newZoom
                    val cx = visible.left + visible.width * u - (u - 0.5) * newWidth - pan.x / frameW * newWidth
                    val cy = visible.top + visible.height * v - (v - 0.5) * newHeight - pan.y / frameH * newHeight
                    try {
                        val next = CropPlanner.plan(image.orientedSize, ratio,
                            CropFocus((cx / image.orientedSize.width).coerceIn(0.0, 1.0),
                                (cy / image.orientedSize.height).coerceIn(0.0, 1.0), newZoom), exactOutput = exact)
                        focus = CropFocus(next.normalizedCenterX, next.normalizedCenterY, newZoom)
                        latestCallback(focus)
                        changed = true
                    } catch (_: IllegalArgumentException) { /* Retain the last valid crop. */ }
                }
                if (dragging) event.changes.forEach { if (it.positionChanged()) it.consume() }
            } while (event.changes.any { it.pressed })
            // Commit the event-local focus exactly once at pointer up.
            if (changed) latestFinish(focus)
        }
    }
}
