package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.ContainGeometry
import org.cajapersonal.fixmyshot.core.ContainPosition
import kotlin.math.min
import kotlin.math.roundToInt

/** Full-canvas edit preview. Only Apply changes the persistent draft/export plan. */
@Composable
internal fun ContainPositionEditor(
    imageId: String,
    preview: Bitmap,
    geometry: ContainGeometry,
    initial: ContainPosition,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onApply: (ContainPosition) -> Unit,
) {
    var current by remember(imageId, initial, geometry.canvas) { mutableStateOf(initial) }
    val availableX = geometry.canvas.width - geometry.placement.width
    val availableY = geometry.canvas.height - geometry.placement.height
    val placement = geometry.positioned(current)
    fun nudge(dx: Int, dy: Int) {
        current = ContainPosition(
            if (availableX > 0) (current.x + dx.toDouble() / availableX).coerceIn(0.0, 1.0) else 0.5,
            if (availableY > 0) (current.y + dy.toDouble() / availableY).coerceIn(0.0, 1.0) else 0.5)
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.preview_position_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.preview_position_help), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.margin_details, placement.placement.left, placement.rightMargin,
                    placement.placement.top, placement.bottomMargin), style = MaterialTheme.typography.bodySmall)
                Box(Modifier.fillMaxWidth().height(420.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    val image = remember(preview) { preview.asImageBitmap() }
                    // An already-bounded sampled render supplies the original pixels and the resolved background.
                    val sx = (preview.width.toDouble() * geometry.placement.left / geometry.canvas.width).roundToInt()
                    val sy = (preview.height.toDouble() * geometry.placement.top / geometry.canvas.height).roundToInt()
                    val sw = (preview.width.toDouble() * geometry.placement.width / geometry.canvas.width)
                        .roundToInt().coerceIn(1, preview.width - sx)
                    val sh = (preview.height.toDouble() * geometry.placement.height / geometry.canvas.height)
                        .roundToInt().coerceIn(1, preview.height - sy)
                    val bgx = if (geometry.placement.left > 0) 0 else if (geometry.rightMargin > 0) preview.width - 1
                        else preview.width / 2
                    val bgy = if (geometry.placement.left > 0 || geometry.rightMargin > 0) preview.height / 2
                        else if (geometry.placement.top > 0) 0 else preview.height - 1
                    val bg = Color(preview.getPixel(bgx, bgy))
                    Canvas(Modifier.fillMaxSize().pointerInput(imageId, enabled, geometry.canvas,
                            geometry.placement.width, geometry.placement.height, preview) {
                        if (!enabled) return@pointerInput
                        detectDragGestures { change, drag ->
                            change.consume()
                            val factor = min(size.width.toFloat() / preview.width,
                                size.height.toFloat() / preview.height)
                            if (factor <= 0f) return@detectDragGestures
                            val imagePixelsPerScreenPixel = geometry.canvas.width.toDouble() / (preview.width * factor)
                            val outputDx = drag.x * imagePixelsPerScreenPixel
                            val outputDy = drag.y * imagePixelsPerScreenPixel
                            current = ContainPosition(
                                if (availableX > 0) (current.x + outputDx / availableX).coerceIn(0.0, 1.0) else 0.5,
                                if (availableY > 0) (current.y + outputDy / availableY).coerceIn(0.0, 1.0) else 0.5)
                        }
                    }) {
                        val factor = min(size.width / preview.width.toFloat(), size.height / preview.height.toFloat())
                        val cw = preview.width * factor
                        val ch = preview.height * factor
                        val left = (size.width - cw) / 2f
                        val top = (size.height - ch) / 2f
                        drawRect(bg, Offset(left, top), Size(cw, ch))
                        val destLeft = left + cw * placement.placement.left / geometry.canvas.width
                        val destTop = top + ch * placement.placement.top / geometry.canvas.height
                        val destWidth = cw * placement.placement.width / geometry.canvas.width
                        val destHeight = ch * placement.placement.height / geometry.canvas.height
                        val ix = destLeft.roundToInt()
                        val iy = destTop.roundToInt()
                        val iw = destWidth.roundToInt().coerceAtLeast(1)
                        val ih = destHeight.roundToInt().coerceAtLeast(1)
                        drawImage(image, srcOffset = IntOffset(sx, sy), srcSize = IntSize(sw, sh),
                            dstOffset = IntOffset(ix, iy), dstSize = IntSize(iw, ih),
                            filterQuality = FilterQuality.Low)
                        drawRect(Color.White, Offset(destLeft, destTop), Size(destWidth, destHeight),
                            style = Stroke(width = 1.dp.toPx()))
                    }
                }
                Text(stringResource(R.string.preview_position_outline_note),
                    style = MaterialTheme.typography.bodySmall)
                if (availableX > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = enabled && placement.placement.left > 0, onClick = { nudge(-8, 0) }) {
                        Text(stringResource(R.string.preview_position_left))
                    }
                    OutlinedButton(enabled = enabled && placement.rightMargin > 0, onClick = { nudge(8, 0) }) {
                        Text(stringResource(R.string.preview_position_right))
                    }
                }
                if (availableY > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = enabled && placement.placement.top > 0, onClick = { nudge(0, -8) }) {
                        Text(stringResource(R.string.preview_position_up))
                    }
                    OutlinedButton(enabled = enabled && placement.bottomMargin > 0, onClick = { nudge(0, 8) }) {
                        Text(stringResource(R.string.preview_position_down))
                    }
                }
                OutlinedButton(enabled = enabled && current != ContainPosition(),
                    onClick = { current = ContainPosition() }) {
                    Text(stringResource(R.string.preview_position_center))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.preview_position_cancel)) }
                    OutlinedButton(enabled = enabled, onClick = { onApply(current) }) {
                        Text(stringResource(R.string.preview_position_apply))
                    }
                }
            }
        }
    }
}
