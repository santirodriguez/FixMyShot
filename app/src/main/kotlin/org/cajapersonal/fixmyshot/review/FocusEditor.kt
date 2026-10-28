package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.*
import kotlin.math.roundToInt

/** Local numeric framing only. Cancel leaves the journal/draft untouched; Apply is one edit. */
@Composable
internal fun FocusEditor(draft: DraftSession, itemId: String, sample: Bitmap, api: Int,
    onApply: (DraftSession, String, EditSettings) -> Unit, onDismiss: () -> Unit, onDiscard: () -> Unit) {
    val image = draft.images.firstOrNull { it.id == itemId } ?: return
    val original = image.overrides.resolve(draft.batch)
    var fitName by rememberSaveable { mutableStateOf(original.fit.name) }
    var x by rememberSaveable { mutableStateOf(original.crop.x) }
    var y by rememberSaveable { mutableStateOf(original.crop.y) }
    var zoom by rememberSaveable { mutableStateOf(original.crop.zoom) }
    var px by rememberSaveable { mutableStateOf(original.position.x) }
    var py by rememberSaveable { mutableStateOf(original.position.y) }
    var inspect by rememberSaveable { mutableStateOf(false) }
    var viewScale by remember { mutableStateOf(1f) }
    var viewX by remember { mutableStateOf(0f) }
    var viewY by remember { mutableStateOf(0f) }
    var comparing by remember { mutableStateOf(false) }
    var precise by rememberSaveable { mutableStateOf(false) }
    var requestCrop by remember { mutableStateOf(false) }
    val fit = FitMode.valueOf(fitName)
    // Freeze destination and codec review during touch edits: one preflight per dialog.
    val basePlan = remember(draft, itemId, api) { draft.plans(api).first { it.itemId == itemId } }
    val plan = remember(basePlan, fit, x, y, zoom, px, py) {
        val settings = basePlan.settings.copy(fit = fit, crop = CropFocus(x, y, zoom),
            position = ContainPosition(px, py))
        val flags = (basePlan.reviewFlags - setOf(DraftReviewFlag.GEOMETRY_UNAVAILABLE,
            DraftReviewFlag.CROP_FOCUS_REPOSITIONED, DraftReviewFlag.CROP_ENLARGEMENT_REQUIRED)).toMutableSet()
        val geometry = try {
            if (fit == FitMode.CONTAIN) PlannedGeometry.Contained(
                basePlan.destination.contain(image.orientedSize, settings.size).positioned(settings.position))
            else {
                val exact = settings.size as? OutputSizeChoice.Exact
                val frame = CropPlanner.plan(image.orientedSize,
                    basePlan.destination.cropRatio(image.orientedSize, settings.size),
                    settings.crop, exactOutput = exact?.canvas)
                if (frame.focusWasClamped) flags += DraftReviewFlag.CROP_FOCUS_REPOSITIONED
                if (frame.enlarged && exact?.allowEnlargement != true)
                    flags += DraftReviewFlag.CROP_ENLARGEMENT_REQUIRED
                PlannedGeometry.Cropped(frame)
            }
        } catch (_: IllegalArgumentException) {
            flags += DraftReviewFlag.GEOMETRY_UNAVAILABLE
            null
        } catch (_: ArithmeticException) {
            flags += DraftReviewFlag.GEOMETRY_UNAVAILABLE
            null
        }
        basePlan.copy(settings = settings, geometry = geometry, reviewFlags = flags)
    }
    fun changeCrop(value: CropFocus) {
        val next = try { CropPlanner.plan(image.orientedSize,
            plan.destination.cropRatio(image.orientedSize, plan.settings.size), value,
            exactOutput = (plan.settings.size as? OutputSizeChoice.Exact)?.canvas) }
            catch (_: IllegalArgumentException) { return }
        x = next.normalizedCenterX; y = next.normalizedCenterY; zoom = value.zoom
    }
    fun reset() { x = .5; y = .5; zoom = 1.0; px = .5; py = .5; viewScale = 1f; viewX = 0f; viewY = 0f }
    var gestureStart by remember { mutableStateOf(plan.settings) }
    fun cancelGesture() {
        x = gestureStart.crop.x; y = gestureStart.crop.y; zoom = gestureStart.crop.zoom
        px = gestureStart.position.x; py = gestureStart.position.y
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.crop_editor_cancel)) }
                    Text(stringResource(R.string.quick_focus_title), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDiscard) { Text(stringResource(R.string.quick_discard)) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(if (inspect) R.string.quick_inspection_zoom else R.string.quick_export_zoom,
                        if (inspect) (viewScale * 100).roundToInt() else (zoom * 100).roundToInt()),
                        style = MaterialTheme.typography.labelMedium)
                    CompareOriginalButton(comparing, true) { comparing = it }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp).clipToBounds(),
                    contentAlignment = Alignment.Center) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()
                    val transform = rememberTransformableState { scale, offset, _ ->
                        viewScale = (viewScale * scale).coerceIn(1f, 4f)
                        viewX = (viewX + offset.x).coerceIn(-widthPx * (viewScale - 1) / 2, widthPx * (viewScale - 1) / 2)
                        viewY = (viewY + offset.y).coerceIn(-heightPx * (viewScale - 1) / 2, heightPx * (viewScale - 1) / 2)
                    }
                    val description = stringResource(R.string.quick_focus_canvas)
                    Box(Modifier.fillMaxSize().transformable(transform, enabled = inspect && !comparing)
                        .semantics { contentDescription = description }) {
                        if (comparing) Image(sample.asImageBitmap(), stringResource(R.string.quick_original),
                            Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        else if (plan.geometry != null) ReviewCanvas(sample, image, plan, !inspect, false,
                            Modifier.fillMaxSize().graphicsLayer {
                                scaleX = if (inspect) viewScale else 1f; scaleY = if (inspect) viewScale else 1f
                                translationX = if (inspect) viewX else 0f; translationY = if (inspect) viewY else 0f
                            }, onCrop = ::changeCrop, onPosition = { px = it.x; py = it.y },
                            onGestureFinished = {}, onCropCommitted = ::changeCrop,
                            onPositionCommitted = { px = it.x; py = it.y },
                            onContainZoom = { requestCrop = true }, onGestureStarted = { gestureStart = plan.settings },
                            onGestureCancelled = ::cancelGesture)
                        else Text(stringResource(R.string.crop_unavailable), Modifier.align(Alignment.Center))
                    }
                }
                Column(Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    QuickFramingRow(fit, true, { fitName = it.name; inspect = false })
                    Text(stringResource(if (inspect) R.string.quick_inspect_hint else if (fit == FitMode.CROP)
                        R.string.quick_crop_hint else R.string.quick_contain_hint),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { inspect = !inspect; viewScale = 1f; viewX = 0f; viewY = 0f }) {
                            Text(stringResource(if (inspect) R.string.quick_edit_image else R.string.quick_inspect))
                        }
                        TextButton(onClick = ::reset) { Text(stringResource(R.string.quick_recenter)) }
                        TextButton(onClick = { precise = true }) { Text(stringResource(R.string.quick_precision)) }
                    }
                    Button(onClick = { onApply(draft, itemId, plan.settings); onDismiss() }, enabled = plan.geometry != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.quick_apply_frame))
                    }
                }
            }
        }
        if (precise) AlertDialog(onDismissRequest = { precise = false },
            title = { Text(stringResource(R.string.quick_precision)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                if (fit == FitMode.CROP) CropControls(image, plan, true, ::changeCrop)
                else ContainPositionControls(plan, true) { px = it.x; py = it.y }
            } }, confirmButton = { TextButton(onClick = { precise = false }) { Text(stringResource(R.string.quick_done)) } })
        if (requestCrop) CropConsentDialog(onCancel = { requestCrop = false }, onConfirm = {
            requestCrop = false; fitName = FitMode.CROP.name; inspect = false
        })
    }
}

@Composable
internal fun CropConsentDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, title = { Text(stringResource(R.string.quick_crop_question)) },
        text = { Text(stringResource(R.string.quick_crop_consent)) },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.quick_keep_all)) } },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.quick_fill_crop)) } })
}
