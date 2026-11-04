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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.FilterChip
import org.cajapersonal.fixmyshot.core.CropInteraction
import org.cajapersonal.fixmyshot.core.CropMotion
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
        var fine by rememberSaveable { mutableStateOf(true) }
        FilterChip(selected = fine, enabled = enabled, onClick = { fine = !fine },
            label = { Text(stringResource(R.string.ux_pixel_steps)) })
        val dx = if (fine) 1.0 / image.orientedSize.width else
            (geometry?.visibleSource?.width ?: base.visibleSource.width) / image.orientedSize.width * 0.1
        val dy = if (fine) 1.0 / image.orientedSize.height else
            (geometry?.visibleSource?.height ?: base.visibleSource.height) / image.orientedSize.height * 0.1
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

/** Pointer updates stay inside the canvas draw state; only pointer-up publishes a crop. */
@Composable
internal fun Modifier.cropGestures(image: DraftImage?, plan: PreviewPlan?, enabled: Boolean,
    onPreview: (CropMotion) -> Unit, onFinished: (CropFocus) -> Unit, onTap: (() -> Unit)? = null,
    onStarted: () -> Unit = {}, onCancelled: () -> Unit = {}): Modifier {
    val latestPlan by rememberUpdatedState(plan)
    val latestPreview by rememberUpdatedState(onPreview)
    val latestFinish by rememberUpdatedState(onFinished)
    val latestTap by rememberUpdatedState(onTap)
    val latestStart by rememberUpdatedState(onStarted)
    val latestCancel by rememberUpdatedState(onCancelled)
    return pointerInput(image?.id, enabled, plan?.sessionRevision, plan?.itemRevision,
        plan?.settings?.fit, plan?.settings?.crop, plan?.destination) {
        if (!enabled || image == null || plan?.settings?.fit != FitMode.CROP) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val initial = latestPlan ?: return@awaitEachGesture
            val ratio = initial.destination.cropRatio(image.orientedSize, initial.settings.size)
            val exact = (initial.settings.size as? OutputSizeChoice.Exact)?.canvas
            var motion = CropInteraction.start(image.orientedSize, ratio, initial.settings.crop, exact)
            var accumulatedPan = Offset.Zero
            var accumulatedZoom = 1f
            var dragging = false
            var changed = false
            var completed = false
            var cancelled = false
            latestStart()
            try {
                do {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.isConsumed }) { cancelled = true; break }
                    val pan = event.calculatePan()
                    val zoom = event.calculateZoom()
                    accumulatedPan += pan
                    accumulatedZoom *= zoom
                    if (!dragging) dragging = accumulatedPan.getDistance() > viewConfiguration.touchSlop ||
                        abs(1f - accumulatedZoom) * event.calculateCentroidSize(false) > viewConfiguration.touchSlop
                    if (dragging && (pan != Offset.Zero || zoom != 1f)) {
                        val centroid = event.calculateCentroid(false)
                        try {
                            motion = CropInteraction.move(image.orientedSize, ratio, motion,
                                size.width.toDouble(), size.height.toDouble(), centroid.x.toDouble(), centroid.y.toDouble(),
                                pan.x.toDouble(), pan.y.toDouble(), zoom.toDouble(), exact)
                            latestPreview(motion)
                            changed = true
                        } catch (_: IllegalArgumentException) { /* Retain the last valid event-local crop. */ }
                    }
                    if (dragging) event.changes.forEach { if (it.positionChanged()) it.consume() }
                } while (event.changes.any { it.pressed })
                if (!cancelled) {
                    completed = true
                    if (changed) latestFinish(motion.focus) else latestTap?.invoke()
                }
            } finally { if (!completed) latestCancel() }
        }
    }
}
