package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
    var frameX by rememberSaveable { mutableStateOf(original.frame.x) }
    var frameY by rememberSaveable { mutableStateOf(original.frame.y) }
    var frameZoom by rememberSaveable { mutableStateOf(original.frame.zoom) }
    var px by rememberSaveable { mutableStateOf(original.position.x) }
    var py by rememberSaveable { mutableStateOf(original.position.y) }
    var inspect by rememberSaveable { mutableStateOf(false) }
    var viewScale by remember { mutableStateOf(1f) }
    var viewX by remember { mutableStateOf(0f) }
    var viewY by remember { mutableStateOf(0f) }
    var comparing by remember { mutableStateOf(false) }
    var precise by rememberSaveable { mutableStateOf(false) }
    var requestCrop by remember { mutableStateOf(false) }
    var showTrimmedArea by rememberSaveable { mutableStateOf(false) }
    val fit = FitMode.valueOf(fitName)
    // Freeze destination and codec review during touch edits: one preflight per dialog.
    val basePlan = remember(draft, itemId, api) { draft.plans(api).first { it.itemId == itemId } }
    val plan = remember(basePlan, fit, x, y, zoom, frameX, frameY, frameZoom, px, py) {
        val settings = basePlan.settings.copy(fit = fit, crop = CropFocus(x, y, zoom),
            position = ContainPosition(px, py), frame = FrameFocus(frameX, frameY, frameZoom))
        val flags = (basePlan.reviewFlags - setOf(DraftReviewFlag.GEOMETRY_UNAVAILABLE,
            DraftReviewFlag.CROP_FOCUS_REPOSITIONED, DraftReviewFlag.CROP_ENLARGEMENT_REQUIRED)).toMutableSet()
        val geometry = try {
            when (fit) {
                FitMode.CONTAIN -> PlannedGeometry.Contained(
                    basePlan.destination.contain(image.orientedSize, settings.size).positioned(settings.position))
                FitMode.CROP -> {
                    val exact = settings.size as? OutputSizeChoice.Exact
                    val crop = CropPlanner.plan(image.orientedSize,
                        basePlan.destination.cropRatio(image.orientedSize, settings.size),
                        settings.crop, exactOutput = exact?.canvas)
                    if (crop.focusWasClamped) flags += DraftReviewFlag.CROP_FOCUS_REPOSITIONED
                    if (crop.enlarged && exact?.allowEnlargement != true)
                        flags += DraftReviewFlag.CROP_ENLARGEMENT_REQUIRED
                    PlannedGeometry.Cropped(crop)
                }
                FitMode.FLEXIBLE -> {
                    require(settings.size == OutputSizeChoice.PreservePixels)
                    PlannedGeometry.Framed(FramePlanner.plan(image.orientedSize,
                        basePlan.destination.cropRatio(image.orientedSize, settings.size), settings.frame))
                }
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
    fun changeFrame(value: FrameFocus) {
        frameX = value.x; frameY = value.y; frameZoom = value.zoom
    }
    fun reset() {
        x = .5; y = .5; zoom = 1.0
        frameX = .5; frameY = .5; frameZoom = 1.0
        px = .5; py = .5; viewScale = 1f; viewX = 0f; viewY = 0f
    }
    var gestureStart by remember { mutableStateOf(plan.settings) }
    fun cancelGesture() {
        x = gestureStart.crop.x; y = gestureStart.crop.y; zoom = gestureStart.crop.zoom
        px = gestureStart.position.x; py = gestureStart.position.y
        frameX = gestureStart.frame.x; frameY = gestureStart.frame.y; frameZoom = gestureStart.frame.zoom
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
                    val zoomLabel = when {
                        inspect -> R.string.quick_inspection_zoom
                        fit == FitMode.FLEXIBLE -> R.string.quick_frame_zoom
                        else -> R.string.quick_export_zoom
                    }
                    val percent = when {
                        inspect -> (viewScale * 100).roundToInt()
                        fit == FitMode.FLEXIBLE -> (frameZoom * 100).roundToInt()
                        else -> (zoom * 100).roundToInt()
                    }
                    Text(stringResource(zoomLabel, percent),
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
                        else if (showTrimmedArea && fit == FitMode.CROP &&
                            plan.geometry is PlannedGeometry.Cropped)
                            CropTrimOverview(sample, image.orientedSize,
                                (plan.geometry as PlannedGeometry.Cropped).value.visibleSource, Modifier.fillMaxSize())
                        else if (showTrimmedArea && fit == FitMode.FLEXIBLE &&
                            plan.geometry is PlannedGeometry.Framed)
                            CropTrimOverview(sample, image.orientedSize,
                                (plan.geometry as PlannedGeometry.Framed).value.visibleSource(), Modifier.fillMaxSize())
                        else if (plan.geometry != null) ReviewCanvas(sample, image, plan, !inspect, false,
                            Modifier.fillMaxSize().graphicsLayer {
                                scaleX = if (inspect) viewScale else 1f; scaleY = if (inspect) viewScale else 1f
                                translationX = if (inspect) viewX else 0f; translationY = if (inspect) viewY else 0f
                            }, onCrop = ::changeCrop, onPosition = { px = it.x; py = it.y },
                            onGestureFinished = {}, onCropCommitted = ::changeCrop,
                            onPositionCommitted = { px = it.x; py = it.y },
                            onFrame = ::changeFrame, onFrameCommitted = ::changeFrame,
                            onContainZoom = { requestCrop = true }, onGestureStarted = { gestureStart = plan.settings },
                            onGestureCancelled = ::cancelGesture)
                        else Text(stringResource(R.string.crop_unavailable), Modifier.align(Alignment.Center))
                    }
                }
                Column(Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    QuickFramingRow(fit, true, { fitName = it.name; inspect = false; showTrimmedArea = false })
                    val framingHint = when {
                        inspect -> R.string.quick_inspect_hint
                        fit == FitMode.FLEXIBLE -> R.string.quick_flexible_hint
                        fit == FitMode.CROP -> R.string.quick_crop_hint
                        else -> R.string.quick_contain_hint
                    }
                    Text(stringResource(framingHint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val flexible = (plan.geometry as? PlannedGeometry.Framed)?.value
                    if (fit == FitMode.FLEXIBLE && flexible != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.quick_crop_trimmed,
                                (flexible.trimmedFraction * 100.0).roundToInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = { showTrimmedArea = !showTrimmedArea; inspect = false }) {
                                Text(stringResource(if (showTrimmedArea) R.string.quick_crop_show_result
                                    else R.string.quick_crop_show_trim))
                            }
                        }
                    }
                    val cropFrame = (plan.geometry as? PlannedGeometry.Cropped)?.value
                    if (fit == FitMode.CROP && cropFrame != null) {
                        val discarded = (100.0 * (1.0 - cropFrame.visibleSource.width *
                            cropFrame.visibleSource.height / image.orientedSize.pixels)).roundToInt()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.quick_crop_trimmed, discarded),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showTrimmedArea = !showTrimmedArea; inspect = false }) {
                                Text(stringResource(if (showTrimmedArea) R.string.quick_crop_show_result
                                    else R.string.quick_crop_show_trim))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { inspect = !inspect; showTrimmedArea = false; viewScale = 1f; viewX = 0f; viewY = 0f }) {
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
                when (fit) {
                    FitMode.CROP -> CropControls(image, plan, true, ::changeCrop)
                    FitMode.FLEXIBLE -> FlexibleFrameControls(FrameFocus(frameX, frameY, frameZoom),
                        true, ::changeFrame)
                    FitMode.CONTAIN -> ContainPositionControls(plan, true) { px = it.x; py = it.y }
                }
            } }, confirmButton = { TextButton(onClick = { precise = false }) { Text(stringResource(R.string.quick_done)) } })
        if (requestCrop) CropConsentDialog(onCancel = { requestCrop = false }, onConfirm = {
            requestCrop = false; fitName = FitMode.FLEXIBLE.name; inspect = false; showTrimmedArea = false
        })
    }
}

/** Inspection-only: shade the EXACT source rectangle excluded by the export crop.
 * No screenshot bitmap is regenerated, and this guide never reaches the exporter.
 */
@Composable
private fun CropTrimOverview(sample: Bitmap, source: PixelSize, crop: SourceRectangle,
    modifier: Modifier = Modifier) {
    val description = stringResource(R.string.quick_crop_trim_accessibility)
    Canvas(modifier.semantics { contentDescription = description }) {
        val full = previewOutputCanvasRect(source, size)
        drawIntoCanvas { output ->
            output.nativeCanvas.drawBitmap(sample, null,
                android.graphics.RectF(full.left, full.top, full.right, full.bottom),
                android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        }
        val left = full.left + full.width * (crop.left / source.width).toFloat()
        val top = full.top + full.height * (crop.top / source.height).toFloat()
        val right = full.left + full.width * ((crop.left + crop.width) / source.width).toFloat()
        val bottom = full.top + full.height * ((crop.top + crop.height) / source.height).toFloat()
        val shade = Color.Black.copy(alpha = .68f)
        drawRect(shade, Offset(full.left, full.top), Size(full.width, top - full.top))
        drawRect(shade, Offset(full.left, bottom), Size(full.width, full.bottom - bottom))
        drawRect(shade, Offset(full.left, top), Size(left - full.left, bottom - top))
        drawRect(shade, Offset(right, top), Size(full.right - right, bottom - top))
        drawRect(Color.White, Offset(left, top), Size(right - left, bottom - top),
            style = Stroke(2f * density))
    }
}

@Composable
internal fun CropConsentDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, title = { Text(stringResource(R.string.quick_crop_question)) },
        text = { Text(stringResource(R.string.quick_crop_consent)) },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.quick_keep_all)) } },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.quick_fill_crop)) } })
}
