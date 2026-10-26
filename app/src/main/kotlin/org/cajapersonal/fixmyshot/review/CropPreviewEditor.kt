package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.CropFocus
import org.cajapersonal.fixmyshot.core.CropGeometry
import org.cajapersonal.fixmyshot.core.CropPlanner
import org.cajapersonal.fixmyshot.core.DraftImage
import org.cajapersonal.fixmyshot.core.OutputSizeChoice
import org.cajapersonal.fixmyshot.core.PlannedGeometry
import org.cajapersonal.fixmyshot.core.PreviewPlan
import kotlin.math.min
import kotlin.math.roundToInt

/** Shows the true original and a sampled output built from the exact same CropPlanner geometry.
 *  This dialog does not modify the draft until Apply is selected.
 */
@Composable
internal fun CropPreviewEditor(
    image: DraftImage,
    sample: Bitmap,
    plan: PreviewPlan,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onApply: (CropFocus) -> Unit,
) {
    var focus by remember(image.id, plan.sessionRevision) { mutableStateOf(plan.settings.crop) }
    val exact = (plan.settings.size as? OutputSizeChoice.Exact)?.canvas
    val ratio = plan.destination.cropRatio(image.orientedSize, plan.settings.size)
    fun edit(next: CropFocus) {
        try {
            val bounded = CropPlanner.plan(image.orientedSize, ratio, next, exactOutput = exact)
            focus = CropFocus(bounded.normalizedCenterX, bounded.normalizedCenterY, next.zoom)
        } catch (_: IllegalArgumentException) {
            // Keep the previous valid frame; never imply that an invalid crop is exportable.
        }
    }
    val geometry = remember(image.orientedSize, ratio, focus, exact) {
        CropPlanner.plan(image.orientedSize, ratio, focus, exactOutput = exact)
    }
    val staged = remember(plan, focus, geometry) {
        plan.copy(settings = plan.settings.copy(crop = focus),
            geometry = PlannedGeometry.Cropped(geometry))
    }
    val output = remember(sample, image.orientedSize, staged) {
        ReviewBitmapRenderer.preview(sample, image.orientedSize, staged, maxLongSide = 900).image
    }
    DisposableEffect(output) { onDispose { output.recycle() } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.crop_editor_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.crop_editor_help), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.crop_editor_original), style = MaterialTheme.typography.titleSmall)
                CropOriginalFrame(sample, image, geometry, focus, enabled, ::edit)
                Text(stringResource(R.string.crop_editor_result), style = MaterialTheme.typography.titleSmall)
                Image(output.asImageBitmap(), contentDescription = stringResource(R.string.crop_editor_result),
                    modifier = Modifier.fillMaxWidth().height(250.dp), contentScale = ContentScale.Fit)
                Text(stringResource(R.string.output_dimensions, geometry.output.width, geometry.output.height),
                    style = MaterialTheme.typography.bodySmall)
                val lostWidth = (image.orientedSize.width - geometry.visibleSource.width).roundToInt().coerceAtLeast(0)
                val lostHeight = (image.orientedSize.height - geometry.visibleSource.height).roundToInt().coerceAtLeast(0)
                Text(stringResource(R.string.crop_editor_removed, lostWidth, lostHeight),
                    style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.crop_editor_guide), style = MaterialTheme.typography.bodySmall)
                CropControls(image, staged, enabled, ::edit)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.crop_editor_cancel)) }
                    OutlinedButton(enabled = enabled, onClick = { onApply(focus) }) {
                        Text(stringResource(R.string.crop_editor_apply))
                    }
                }
            }
        }
    }
}

/** Project the actual crop rectangle over the entire normalized original sample. */
@Composable
private fun CropOriginalFrame(
    sample: Bitmap, image: DraftImage, geometry: CropGeometry, focus: CropFocus,
    enabled: Boolean, onCrop: (CropFocus) -> Unit,
) {
    val currentFocus by rememberUpdatedState(focus)
    val currentCrop by rememberUpdatedState(onCrop)
    val imageBitmap = remember(sample) { sample.asImageBitmap() }
    Canvas(Modifier.fillMaxWidth().height(310.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .pointerInput(sample, image.id, enabled) {
            if (!enabled) return@pointerInput
            detectTransformGestures { _, pan, zoom, _ ->
                val fraction = min(size.width.toFloat() / sample.width, size.height.toFloat() / sample.height)
                val dw = sample.width * fraction
                val dh = sample.height * fraction
                if (dw <= 0f || dh <= 0f) return@detectTransformGestures
                val previous = currentFocus
                currentCrop(CropFocus(
                    (previous.x + pan.x / dw).coerceIn(0.0, 1.0),
                    (previous.y + pan.y / dh).coerceIn(0.0, 1.0),
                    (previous.zoom * zoom).coerceIn(1.0, 64.0)))
            }
        }) {
        val scale = min(size.width / sample.width.toFloat(), size.height / sample.height.toFloat())
        val dw = sample.width * scale
        val dh = sample.height * scale
        val left = (size.width - dw) / 2f
        val top = (size.height - dh) / 2f
        drawImage(imageBitmap, dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(dw.roundToInt().coerceAtLeast(1), dh.roundToInt().coerceAtLeast(1)))
        val rect = geometry.visibleSource
        val x0 = left + dw * (rect.left / image.orientedSize.width).toFloat()
        val y0 = top + dh * (rect.top / image.orientedSize.height).toFloat()
        val x1 = left + dw * ((rect.left + rect.width) / image.orientedSize.width).toFloat()
        val y1 = top + dh * ((rect.top + rect.height) / image.orientedSize.height).toFloat()
        val shade = Color.Black.copy(alpha = 0.6f)
        drawRect(shade, Offset(left, top), Size(dw, (y0 - top).coerceAtLeast(0f)))
        drawRect(shade, Offset(left, y1), Size(dw, (top + dh - y1).coerceAtLeast(0f)))
        drawRect(shade, Offset(left, y0), Size((x0 - left).coerceAtLeast(0f), (y1 - y0).coerceAtLeast(0f)))
        drawRect(shade, Offset(x1, y0), Size((left + dw - x1).coerceAtLeast(0f), (y1 - y0).coerceAtLeast(0f)))
        drawRect(Color.Black, Offset(x0, y0), Size(x1 - x0, y1 - y0),
            style = Stroke(width = 3.dp.toPx()))
        drawRect(Color.White, Offset(x0, y0), Size(x1 - x0, y1 - y0),
            style = Stroke(width = 1.5.dp.toPx()))
    }
}
