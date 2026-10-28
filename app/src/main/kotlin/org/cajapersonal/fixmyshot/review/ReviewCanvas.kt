package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
) {
    val contained = (plan.geometry as? PlannedGeometry.Contained)?.value
    val cropped = (plan.geometry as? PlannedGeometry.Cropped)?.value
    val output = contained?.canvas ?: cropped?.output ?: return
    val currentPlan by rememberUpdatedState(plan)
    val currentPosition by rememberUpdatedState(onPosition)
    val currentFinish by rememberUpdatedState(onGestureFinished)
    val bg = remember(sample, plan.settings.background) {
        when (val choice = plan.settings.background) {
            BackgroundChoice.Automatic -> Color(ReviewBitmapRenderer.automaticColor(sample))
            BackgroundChoice.White -> Color.White
            BackgroundChoice.Black -> Color.Black
            BackgroundChoice.Transparent -> Color.Transparent
            is BackgroundChoice.CustomRgb -> Color(0xFF000000.toInt() or choice.rgb)
        }
    }
    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .25f)
    val border = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.preview_description)
    Canvas(modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        .semantics { contentDescription = description }
        .cropGestures(image, plan, enabled, onCrop) { focus -> onCropCommitted(focus); onGestureFinished() }
        .pointerInput(image.id, enabled, plan.settings.fit) {
            if (!enabled || contained == null) return@pointerInput
            detectDragGestures(onDragEnd = { currentFinish() }, onDragCancel = { currentFinish() }) { change, drag ->
                change.consume()
                val live = currentPlan
                val geometry = (live.geometry as? PlannedGeometry.Contained)?.value ?: return@detectDragGestures
                val bounds = previewOutputCanvasRect(geometry.canvas, Size(size.width.toFloat(), size.height.toFloat()))
                val spaceX = geometry.canvas.width - geometry.placement.width
                val spaceY = geometry.canvas.height - geometry.placement.height
                currentPosition(ContainPosition(
                    if (spaceX > 0) (live.settings.position.x + drag.x / bounds.width * geometry.canvas.width / spaceX).coerceIn(0.0, 1.0) else .5,
                    if (spaceY > 0) (live.settings.position.y + drag.y / bounds.height * geometry.canvas.height / spaceY).coerceIn(0.0, 1.0) else .5))
            }
        }) {
        val bounds = previewOutputCanvasRect(output, size)
        val gap = 22f * density
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
