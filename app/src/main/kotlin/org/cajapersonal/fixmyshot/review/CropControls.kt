package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.CropFocus
import org.cajapersonal.fixmyshot.core.CropPlanner
import org.cajapersonal.fixmyshot.core.DraftImage
import org.cajapersonal.fixmyshot.core.DraftReviewFlag
import org.cajapersonal.fixmyshot.core.FitMode
import org.cajapersonal.fixmyshot.core.PlannedGeometry
import org.cajapersonal.fixmyshot.core.PreviewPlan
import org.cajapersonal.fixmyshot.core.OutputSizeChoice
import kotlin.math.min
import kotlin.math.roundToInt

/** Accessible alternatives to the same normalized transform used by drag/pinch. */
@Composable
internal fun CropControls(image: DraftImage, plan: PreviewPlan, enabled: Boolean,
    onCrop: (CropFocus) -> Unit) {
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
        Text(stringResource(R.string.crop_notice))
        Text(stringResource(R.string.crop_zoom, (focus.zoom * 100).roundToInt()))
        if (maxZoom > 1.0) Slider(value = focus.zoom.toFloat().coerceIn(1f, maxZoom.toFloat()),
            onValueChange = { onCrop(focus.copy(zoom = it.toDouble())) },
            valueRange = 1f..maxZoom.toFloat(), enabled = enabled)
        val dx = (geometry?.visibleSource?.width ?: base.visibleSource.width) / image.orientedSize.width * 0.1
        val dy = (geometry?.visibleSource?.height ?: base.visibleSource.height) / image.orientedSize.height * 0.1
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onCrop(focus.copy(x = (focus.x - dx).coerceIn(0.0, 1.0))) }) {
                Text(stringResource(R.string.crop_left))
            }
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onCrop(focus.copy(x = (focus.x + dx).coerceIn(0.0, 1.0))) }) {
                Text(stringResource(R.string.crop_right))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onCrop(focus.copy(y = (focus.y - dy).coerceIn(0.0, 1.0))) }) {
                Text(stringResource(R.string.crop_up))
            }
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onCrop(focus.copy(y = (focus.y + dy).coerceIn(0.0, 1.0))) }) {
                Text(stringResource(R.string.crop_down))
            }
        }
        OutlinedButton(enabled = enabled, onClick = { onCrop(CropFocus()) }) {
            Text(stringResource(R.string.crop_reset))
        }
        if (geometry != null && DraftReviewFlag.CROP_FOCUS_REPOSITIONED in plan.reviewFlags) {
            OutlinedButton(enabled = enabled, onClick = {
                onCrop(CropFocus(geometry.normalizedCenterX, geometry.normalizedCenterY, focus.zoom))
            }) { Text(stringResource(R.string.crop_accept_position)) }
        }
    }
}

/** Coordinates account for the actual fitted preview frame rather than its letterbox. */
@Composable
internal fun Modifier.cropGestures(image: DraftImage?, plan: PreviewPlan?, enabled: Boolean,
    onCrop: (CropFocus) -> Unit): Modifier {
    val latestPlan by rememberUpdatedState(plan)
    val latestCallback by rememberUpdatedState(onCrop)
    return pointerInput(image?.id, enabled, plan?.settings?.fit) {
        if (!enabled || image == null || plan?.settings?.fit != FitMode.CROP) return@pointerInput
        detectTransformGestures { centroid, pan, zoom, _ ->
            val current = latestPlan ?: return@detectTransformGestures
            val geometry = (current.geometry as? PlannedGeometry.Cropped)?.value ?: return@detectTransformGestures
            val visible = geometry.visibleSource
            val scale = min(size.width.toDouble() / geometry.output.width,
                size.height.toDouble() / geometry.output.height)
            val frameW = geometry.output.width * scale
            val frameH = geometry.output.height * scale
            if (frameW <= 0.0 || frameH <= 0.0) return@detectTransformGestures
            val u = ((centroid.x - (size.width - frameW) / 2.0) / frameW).coerceIn(0.0, 1.0)
            val v = ((centroid.y - (size.height - frameH) / 2.0) / frameH).coerceIn(0.0, 1.0)
            val focus = current.settings.crop
            val newZoom = (focus.zoom * zoom).coerceIn(1.0, 64.0)
            val newWidth = visible.width * focus.zoom / newZoom
            val newHeight = visible.height * focus.zoom / newZoom
            val cx = visible.left + visible.width * u - (u - 0.5) * newWidth - pan.x / frameW * newWidth
            val cy = visible.top + visible.height * v - (v - 0.5) * newHeight - pan.y / frameH * newHeight
            latestCallback(CropFocus((cx / image.orientedSize.width).coerceIn(0.0, 1.0),
                (cy / image.orientedSize.height).coerceIn(0.0, 1.0), newZoom))
        }
    }
}
