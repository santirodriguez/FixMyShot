package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.cajapersonal.fixmyshot.R

/** A bounded read-only decode of an already independently verified published file.
 * No image transformations, original source access, or media permission is required.
 */
@Composable
internal fun SavedImageViewer(image: Bitmap, onClose: () -> Unit) {
    var scale by remember(image) { mutableFloatStateOf(1f) }
    var shift by remember(image) { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.quick_view_saved),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text(stringResource(R.string.quick_done)) }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()
                    val gesture = rememberTransformableState { zoom, pan, _ ->
                        val next = (scale * zoom).coerceIn(1f, 5f)
                        shift = Offset(
                            (shift.x + pan.x).coerceIn(-widthPx * (next - 1f) / 2f, widthPx * (next - 1f) / 2f),
                            (shift.y + pan.y).coerceIn(-heightPx * (next - 1f) / 2f, heightPx * (next - 1f) / 2f))
                        scale = next
                    }
                    Image(image.asImageBitmap(), contentDescription = stringResource(R.string.quick_view_saved),
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()
                            .transformable(gesture).graphicsLayer {
                                scaleX = scale; scaleY = scale
                                translationX = shift.x; translationY = shift.y
                            })
                }
            }
        }
    }
}
