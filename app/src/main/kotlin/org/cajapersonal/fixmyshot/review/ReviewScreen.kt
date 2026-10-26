package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.net.Uri
import org.cajapersonal.fixmyshot.export.PendingMediaStoreRemoval
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.ContainGeometry
import org.cajapersonal.fixmyshot.core.ContainPosition
import org.cajapersonal.fixmyshot.core.DestinationCatalog
import org.cajapersonal.fixmyshot.core.DestinationChoice
import org.cajapersonal.fixmyshot.core.DraftReviewFlag
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.core.PixelSize
import org.cajapersonal.fixmyshot.core.FitMode
import org.cajapersonal.fixmyshot.core.CropFocus
import org.cajapersonal.fixmyshot.core.PlannedGeometry
import org.cajapersonal.fixmyshot.core.OutputSizeChoice
import org.cajapersonal.fixmyshot.core.MetadataPolicy
import org.cajapersonal.fixmyshot.core.SourceColor
import org.cajapersonal.fixmyshot.core.PrivatePreparationBlock
import org.cajapersonal.fixmyshot.export.BatchItemPhase

/** Review plus deliberate, foreground-only batch export and bounded progress. */
@Composable
internal fun ReviewScreen(
    draft: DraftSession,
    selectedId: String?,
    editorSessionKey: String,
    editSelectedOnly: Boolean,
    onEditSelectedOnly: (Boolean) -> Unit,
    preview: Bitmap?,
    thumbnails: Map<String, Bitmap>,
    cropSource: Bitmap? = null,
    cropSourceId: String? = null,
    cropLoadingId: String? = null,
    onOpenCropEditor: (String) -> Unit = {},
    onCloseCropEditor: () -> Unit = {},
    busy: Boolean,
    invalidImports: Int,
    previewUnavailable: Boolean,
    api: Int,
    exportState: ReviewExportState,
    canPrepareExport: Boolean,
    onExport: () -> Unit,
    onCancelExport: () -> Unit,
    onReviewedPrevious: () -> Unit,
    selectedShareIds: Set<String>,
    shareBusy: Boolean,
    shareError: Boolean,
    onToggleShare: (String) -> Unit,
    onSelectAllShare: () -> Unit,
    onClearShareSelection: () -> Unit,
    onShare: () -> Unit,
    onChoose: () -> Unit,
    onSelect: (String) -> Unit,
    onDestination: (DestinationChoice) -> Unit,
    onFormat: (OutputFormat, Boolean) -> Unit,
    onBackground: (BackgroundChoice, Boolean) -> Unit,
    onResetSelected: () -> Unit,
    onUndo: () -> Unit,
    onRemove: () -> Unit,
    onDiscard: (DraftSession) -> Unit,
    importProgress: ImportProgress? = null,
    onStopImport: () -> Unit = {},
    importLimitExceeded: Boolean = false,
    pendingImports: Int = 0,
    onSettings: () -> Unit = {},
    onFit: (FitMode, Boolean) -> Unit = { _, _ -> },
    onCrop: (CropFocus) -> Unit = {},
    onPosition: (ContainPosition) -> Unit = {},
    onRetryExport: () -> Unit = {},
    onRestoreDraft: () -> Unit = {},
    onRefreshOutputs: () -> Unit = {},
    onOpenOutput: (String) -> Unit = {},
    onConfirmOutput: (String) -> Unit = {},
    onRemovePendingOutput: (String) -> Unit = {},
    onSize: (OutputSizeChoice, Boolean) -> Unit = { _, _ -> },
    onApplyBatchToAll: (DraftSession) -> Unit = {},
    onMoveSelected: (Int) -> Unit = {},
    onMetadata: (MetadataPolicy, Boolean) -> Unit = { _, _ -> },
    sessionRestored: Boolean = false,
    sessionExpired: Boolean = false,
    sessionSaveError: Boolean = false,
    sessionRestoreBlocked: Boolean = false,
    onRetrySessionSave: () -> Unit = {},
    onDiscardUnavailableSession: () -> Unit = {},
    finishSessionOnReview: Boolean = false,
    importReport: ImportReport? = null,
    onClearImportFeedback: () -> Unit = {},
) {
    val plans = remember(draft, api) { draft.plans(api) }
    val selected = draft.images.firstOrNull { it.id == selectedId }
    val plan = plans.firstOrNull { it.itemId == selectedId }
    val editEnabled = !busy && !shareBusy && !sessionRestoreBlocked && pendingImports == 0 && !exportState.isLocked
    var confirmingOutput by remember { mutableStateOf<String?>(null) }
    var removingPending by remember { mutableStateOf<String?>(null) }
    var confirmingBatchReset by remember(draft) { mutableStateOf<DraftSession?>(null) }
    var confirmingDiscard by remember(draft) { mutableStateOf<DraftSession?>(null) }
    var showOriginalOutline by rememberSaveable { mutableStateOf(true) }
    var positionEditorFor by remember { mutableStateOf<String?>(null) }
    val customCount = draft.images.count { it.overrides.hasCustomValues }
    val convertedCount = draft.images.count { it.sourceColor != SourceColor.SRGB }
    val batchResetDraft = confirmingBatchReset
    if (selected != null && plan != null && cropSource != null &&
        cropSourceId == selected.id && plan.geometry is PlannedGeometry.Cropped) {
        CropPreviewEditor(image = selected, sample = cropSource, plan = plan, enabled = editEnabled,
            onDismiss = onCloseCropEditor,
            onApply = { proposed -> onCloseCropEditor(); onCrop(proposed) })
    }
    if (selected != null && plan != null && preview != null && positionEditorFor == selected.id) {
        val contained = (plan.geometry as? PlannedGeometry.Contained)?.value
        if (contained != null && (contained.placement.left + contained.rightMargin > 0 ||
                contained.placement.top + contained.bottomMargin > 0)) {
            ContainPositionEditor(selected.id, preview, contained, plan.settings.position, editEnabled,
                onDismiss = { positionEditorFor = null },
                onApply = { onPosition(it); positionEditorFor = null })
        }
    }
    if (batchResetDraft != null && batchResetDraft == draft && editEnabled) AlertDialog(
        onDismissRequest = { confirmingBatchReset = null },
        title = { Text(stringResource(R.string.batch_apply_all)) },
        text = { Text(stringResource(R.string.batch_apply_explanation, customCount)) },
        confirmButton = { TextButton(onClick = {
            confirmingBatchReset = null
            onApplyBatchToAll(batchResetDraft)
        }) { Text(stringResource(R.string.batch_apply_all)) } },
        dismissButton = { TextButton(onClick = { confirmingBatchReset = null }) {
            Text(stringResource(R.string.export_keep_cancel))
        } },
    )
    val discardedDraft = confirmingDiscard
    if (discardedDraft != null && discardedDraft == draft && editEnabled && !shareBusy) AlertDialog(
        onDismissRequest = { confirmingDiscard = null },
        title = { Text(stringResource(R.string.discard_session)) },
        text = { Text(stringResource(R.string.discard_session_confirmation)) },
        confirmButton = { TextButton(onClick = {
            confirmingDiscard = null
            onDiscard(discardedDraft)
        }) { Text(stringResource(R.string.discard_session)) } },
        dismissButton = { TextButton(onClick = { confirmingDiscard = null }) {
            Text(stringResource(R.string.export_keep_cancel))
        } },
    )
    val confirmationId = confirmingOutput
    if (confirmationId != null) AlertDialog(
        onDismissRequest = { confirmingOutput = null },
        title = { Text(stringResource(R.string.export_keep_output)) },
        text = { Text(stringResource(R.string.export_keep_explanation)) },
        confirmButton = { TextButton(onClick = {
            confirmingOutput = null
            onConfirmOutput(confirmationId)
        }) { Text(stringResource(R.string.export_keep_output)) } },
        dismissButton = { TextButton(onClick = { confirmingOutput = null }) {
            Text(stringResource(R.string.export_keep_cancel))
        } },
    )

    val removalId = removingPending
    if (removalId != null) AlertDialog(
        onDismissRequest = { removingPending = null },
        title = { Text(stringResource(R.string.export_remove_pending)) },
        text = { Text(stringResource(R.string.export_remove_pending_explanation)) },
        confirmButton = { TextButton(onClick = {
            removingPending = null
            onRemovePendingOutput(removalId)
        }, enabled = !busy && !shareBusy && !exportState.outputBusy) {
            Text(stringResource(R.string.export_remove_pending))
        } },
        dismissButton = { TextButton(onClick = { removingPending = null }) {
            Text(stringResource(R.string.export_keep_cancel))
        } },
    )

    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onSettings) { Text(stringResource(R.string.settings_title)) }
        if (sessionRestored) Text(stringResource(R.string.session_restored), style = MaterialTheme.typography.bodySmall)
        if (sessionExpired) Text(stringResource(R.string.session_expired))
        if (sessionRestoreBlocked) {
            Text(stringResource(R.string.session_restore_failed), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onDiscardUnavailableSession, enabled = !busy && !exportState.isLocked) {
                Text(stringResource(R.string.session_discard_saved))
            }
        } else if (sessionSaveError) {
            Text(stringResource(R.string.session_save_failed), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetrySessionSave, enabled = !busy && !exportState.isLocked) {
                Text(stringResource(R.string.session_retry_save))
            }
        }
        Button(onClick = onChoose, enabled = editEnabled) { Text(stringResource(R.string.choose_images)) }
        if (importProgress != null) {
            Text(stringResource(R.string.import_progress, importProgress.processed, importProgress.total),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text(stringResource(R.string.import_stop_notice), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onStopImport, enabled = !importProgress.stopping) {
                Text(stringResource(if (importProgress.stopping) R.string.import_stopping else R.string.import_stop))
            }
        } else if (busy) Text(stringResource(R.string.import_in_progress))
        if (pendingImports > 0) Text(stringResource(R.string.import_waiting, pendingImports))
        if (importLimitExceeded) Text(stringResource(R.string.import_limit),
            color = MaterialTheme.colorScheme.error)
        val otherImportFailures = (invalidImports - (importReport?.problems?.size ?: 0)).coerceAtLeast(0)
        if (otherImportFailures > 0) Text(stringResource(R.string.import_skipped, otherImportFailures),
            color = MaterialTheme.colorScheme.error)
        if (importReport != null) ImportFeedback(importReport, !busy, onClearImportFeedback)
        else if (invalidImports > 0) TextButton(onClick = onClearImportFeedback, enabled = !busy) {
            Text(stringResource(R.string.import_feedback_dismiss))
        }
        if (draft.canUndo) OutlinedButton(onClick = onUndo, enabled = editEnabled) {
            Text(stringResource(R.string.undo_last))
        }
        if (draft.images.isEmpty()) {
            Text(stringResource(R.string.choose_prompt))
            Text(stringResource(R.string.preview_only_notice), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(stringResource(R.string.batch_count, draft.images.size), style = MaterialTheme.typography.titleMedium)
            // A lazy strip avoids composing 100 image controls at once.
            // Thumbnails depict originals, not scaled output or completed exports.
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(draft.images, key = { _, image -> image.id }) { index, image ->
                    val selectedThumbnail = image.id == selectedId
                    OutlinedButton(onClick = { onSelect(image.id) }, enabled = editEnabled,
                        modifier = Modifier.semantics { this.selected = selectedThumbnail }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val thumbnail = thumbnails[image.id]
                            if (thumbnail != null && !thumbnail.isRecycled) {
                                Image(thumbnail.asImageBitmap(),
                                    contentDescription = stringResource(R.string.thumbnail_description,
                                        index + 1, draft.images.size),
                                    modifier = Modifier.size(64.dp), contentScale = ContentScale.Fit)
                            } else {
                                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                    Text("…")
                                }
                            }
                            val marker = if (selectedThumbnail) "● " else ""
                            Text("$marker${index + 1}")
                        }
                    }
                }
            }
            if (selected != null) {
                val contained = (plan?.geometry as? PlannedGeometry.Contained)?.value
                val cropped = (plan?.geometry as? PlannedGeometry.Cropped)?.value
                val output = contained?.canvas ?: cropped?.output
                val hasMargins = contained != null && (contained.placement.left > 0 ||
                    contained.rightMargin > 0 || contained.placement.top > 0 ||
                    contained.bottomMargin > 0)
                if (output != null) {
                    Text(stringResource(R.string.preview_dimensions_summary,
                        selected.orientedSize.width, selected.orientedSize.height,
                        output.width, output.height), style = MaterialTheme.typography.bodySmall)
                    if (hasMargins && contained != null) {
                        Text(stringResource(R.string.margin_details, contained.placement.left,
                            contained.rightMargin, contained.placement.top, contained.bottomMargin),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Box(Modifier.fillMaxWidth().height(320.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                    .cropGestures(selected, plan, editEnabled, onCrop),
                    contentAlignment = Alignment.Center) {
                    if (preview != null) {
                        if (plan?.settings?.background == BackgroundChoice.Transparent) {
                            Canvas(Modifier.fillMaxSize()) {
                                val step = 22.dp.toPx()
                                for (y in 0 until (size.height / step).toInt() + 1) {
                                    for (x in 0 until (size.width / step).toInt() + 1) {
                                        drawRect(if ((x + y) % 2 == 0) Color.LightGray else Color.White,
                                            topLeft = Offset(x * step, y * step), size = Size(step, step))
                                    }
                                }
                            }
                        }
                        Image(preview.asImageBitmap(), contentDescription = stringResource(R.string.preview_description),
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        if (hasMargins && showOriginalOutline && contained != null) {
                            PreviewOriginalOutline(preview, contained)
                        }
                    } else {
                        Text(stringResource(if (previewUnavailable) R.string.preview_unavailable
                            else R.string.preview_loading))
                    }
                }
                if (hasMargins) {
                    OutlinedButton(onClick = { positionEditorFor = selected.id }, enabled = editEnabled && preview != null) {
                        Text(stringResource(R.string.preview_position_open))
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                        value = showOriginalOutline, role = Role.Checkbox,
                        onValueChange = { showOriginalOutline = it }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showOriginalOutline, onCheckedChange = null)
                        Text(stringResource(R.string.preview_original_outline))
                    }
                    Text(stringResource(R.string.preview_background_quick_heading),
                        style = MaterialTheme.typography.bodySmall)
                    val quickBackground = if (editSelectedOnly) plan?.settings?.background else draft.batch.background
                    FlowRow(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            BackgroundChoice.Automatic to R.string.preview_background_auto,
                            BackgroundChoice.Black to R.string.preview_background_black,
                            BackgroundChoice.White to R.string.preview_background_white,
                        ).forEach { (choice, label) ->
                            FilterChip(selected = quickBackground == choice,
                                onClick = { onBackground(choice, editSelectedOnly) },
                                enabled = editEnabled && (!editSelectedOnly || plan != null),
                                label = { Text(stringResource(label)) })
                        }
                    }
                    Text(stringResource(if (editSelectedOnly) R.string.edit_scope_image
                        else R.string.edit_scope_batch), style = MaterialTheme.typography.bodySmall)
                }
            }
            androidx.compose.runtime.key(editorSessionKey) {
                ReviewControls(
                    draft = draft, plan = plan, enabled = editEnabled,
                    selectedOnly = editSelectedOnly, onSelectedOnly = onEditSelectedOnly,
                    onDestination = onDestination, onFormat = onFormat, onBackground = onBackground,
                    onFit = onFit, onSize = onSize, onMetadata = onMetadata,
                )
            }
            if (customCount > 0) {
                Text(stringResource(R.string.batch_custom_count, customCount))
                OutlinedButton(enabled = editEnabled, onClick = { confirmingBatchReset = draft }) {
                    Text(stringResource(R.string.batch_apply_all))
                }
            }
            if (selected != null && plan != null && plan.geometry is PlannedGeometry.Cropped) {
                OutlinedButton(onClick = { onOpenCropEditor(selected.id) },
                    enabled = editEnabled && cropLoadingId != selected.id) {
                    Text(stringResource(R.string.crop_editor_open))
                }
                if (cropLoadingId == selected.id) Text(stringResource(R.string.preview_loading))
                CropControls(selected, plan, editEnabled, onCrop)
            }
            if (selected != null && plan != null) {
                if (selected.sourceColor != SourceColor.SRGB) Text(stringResource(R.string.color_conversion_details,
                    if (selected.sourceColor == SourceColor.DISPLAY_P3) "Display P3" else "Adobe RGB"),
                    style = MaterialTheme.typography.bodySmall)
                val contained = (plan.geometry as? PlannedGeometry.Contained)?.value
                val cropped = (plan.geometry as? PlannedGeometry.Cropped)?.value
                val resized = contained?.resized == true || cropped?.resized == true
                val enlarged = contained?.enlarged == true || cropped?.enlarged == true
                if (resized) Text(stringResource(if (enlarged) R.string.size_will_enlarge else R.string.size_will_reduce),
                    style = MaterialTheme.typography.bodySmall)
                plan.reviewFlags.forEach { flag ->
                    Text(reviewFlagLabel(flag), color = MaterialTheme.colorScheme.error)
                }
                if (selected.overrides.hasCustomValues) Text(stringResource(R.string.custom_settings_label))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onResetSelected, enabled = editEnabled) { Text(stringResource(R.string.reset_image)) }
                    OutlinedButton(onClick = onRemove, enabled = editEnabled) { Text(stringResource(R.string.remove_image)) }
                }
                if (draft.images.size > 1) {
                    val position = draft.images.indexOfFirst { it.id == selectedId }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = editEnabled && position > 0, onClick = { onMoveSelected(-1) }) {
                            Text(stringResource(R.string.batch_move_earlier))
                        }
                        OutlinedButton(enabled = editEnabled && position < draft.images.lastIndex, onClick = { onMoveSelected(1) }) {
                            Text(stringResource(R.string.batch_move_later))
                        }
                    }
                }
                if (convertedCount > 0) Text(stringResource(R.string.color_conversion_batch, convertedCount),
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = onExport, enabled = editEnabled && canPrepareExport) {
                    Text(stringResource(R.string.export_batch, draft.images.size))
                }
                Text(stringResource(if (api >= 29) R.string.export_destination_pictures
                    else R.string.export_destination_saf), style = MaterialTheme.typography.bodySmall)
                if (exportState.phase == ExportUiPhase.IDLE) {
                    Text(stringResource(R.string.preview_only_notice),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (draft.workingSourceIds.isNotEmpty()) OutlinedButton(onClick = { confirmingDiscard = draft },
            enabled = editEnabled && !shareBusy) {
            Text(stringResource(R.string.discard_session))
        }
        if (exportState.phase != ExportUiPhase.IDLE) ExportProgress(exportState, onCancelExport)
        if (exportState.phase == ExportUiPhase.BLOCKED) {
            exportState.blockedItems.forEach { blocked ->
                val index = draft.images.indexOfFirst { it.id == blocked.itemId }
                if (index >= 0) {
                    OutlinedButton(onClick = { onSelect(blocked.itemId) }, enabled = editEnabled) {
                        Text(stringResource(R.string.result_review_image, index + 1))
                    }
                    val flags = plans.firstOrNull { it.itemId == blocked.itemId }?.reviewFlags.orEmpty()
                    if (PrivatePreparationBlock.REVIEW_CONFLICT in blocked.blocks && flags.isNotEmpty()) {
                        flags.forEach { Text(reviewFlagLabel(it), color = MaterialTheme.colorScheme.error) }
                    }
                    blocked.blocks.filterNot { it == PrivatePreparationBlock.REVIEW_CONFLICT && flags.isNotEmpty() }
                        .map(ResultFeedback::preflightReason).distinct().forEach { label ->
                            Text(stringResource(label), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (exportState.published > 0 && exportState.phase in setOf(
                ExportUiPhase.COMPLETED, ExportUiPhase.CANCELLED, ExportUiPhase.PAUSED, ExportUiPhase.NEEDS_REVIEW)) {
            ExportShareControls(exportState, selectedShareIds, shareBusy || exportState.outputBusy, shareError,
                onToggleShare, onSelectAllShare, onClearShareSelection, onShare)
        }
        if (exportState.canAcknowledge) {
            val actionsEnabled = !busy && !shareBusy && !exportState.outputBusy
            OutlinedButton(onClick = onRefreshOutputs, enabled = actionsEnabled) {
                Text(stringResource(R.string.export_recheck_outputs))
            }
            exportState.items.forEachIndexed { index, item ->
                if (item.uri != null) {
                    OutlinedButton(onClick = { onOpenOutput(item.itemId) }, enabled = actionsEnabled) {
                        Text(stringResource(R.string.export_open_output, index + 1))
                    }
                    if (item.phase == BatchItemPhase.NEEDS_REVIEW && item.reason == "OUTPUT_PRESENT_UNCERTAIN") {
                        OutlinedButton(onClick = { confirmingOutput = item.itemId }, enabled = actionsEnabled) {
                            Text(stringResource(R.string.export_keep_item, index + 1))
                        }
                    }
                    if (api >= 29 && item.phase == BatchItemPhase.NEEDS_REVIEW && item.reason == "OUTPUT_PENDING" &&
                        PendingMediaStoreRemoval.supportedUri(Uri.parse(item.uri))) {
                        OutlinedButton(onClick = { removingPending = item.itemId }, enabled = actionsEnabled) {
                            Text(stringResource(R.string.export_remove_pending_item, index + 1))
                        }
                    }
                }
            }
            if (exportState.outputBusy) Text(stringResource(R.string.export_checking_output))
        }
        if (exportState.outputError) Text(stringResource(R.string.export_output_action_failed),
            color = MaterialTheme.colorScheme.error)
        if (exportState.canRestoreDraft) {
            OutlinedButton(onClick = onRestoreDraft, enabled = !busy && !shareBusy && !exportState.outputBusy) {
                Text(stringResource(R.string.export_restore_draft))
            }
        }
        if (exportState.retryableIds.isNotEmpty() && exportState.canAcknowledge) {
            Text(stringResource(R.string.export_retry_notice))
            Button(onClick = onRetryExport, enabled = exportState.retryReady && !busy && !shareBusy && !exportState.outputBusy) {
                Text(stringResource(if (exportState.phase == ExportUiPhase.PAUSED) R.string.export_resume_remaining
                    else R.string.export_retry_remaining, exportState.retryableIds.size))
            }
        }
        if (exportState.previousJobs > 0 || exportState.canAcknowledge) {
            ExportJobNotice(exportState, onReviewedPrevious, finishSessionOnReview,
                enabled = !busy && !shareBusy)
        }
    }
}

/** Drawn above the Compose preview only; never written into an export bitmap. */
@Composable
private fun PreviewOriginalOutline(preview: Bitmap, geometry: ContainGeometry) {
    Canvas(Modifier.fillMaxSize()) {
        val bounds = previewOriginalContentRect(PixelSize(preview.width, preview.height), size, geometry)
        drawRect(Color.Black.copy(alpha = 0.8f), topLeft = bounds.topLeft,
            size = bounds.size, style = Stroke(width = 3.dp.toPx()))
        drawRect(Color.White, topLeft = bounds.topLeft,
            size = bounds.size, style = Stroke(width = 1.dp.toPx()))
    }
}

/** Project immutable source placement onto the actual ContentScale.Fit viewport, including letterboxing. */
internal fun previewOriginalContentRect(previewSize: PixelSize, viewport: Size, geometry: ContainGeometry): Rect {
    val scale = minOf(viewport.width / previewSize.width, viewport.height / previewSize.height)
    val displayWidth = previewSize.width * scale
    val displayHeight = previewSize.height * scale
    val displayLeft = (viewport.width - displayWidth) / 2f
    val displayTop = (viewport.height - displayHeight) / 2f
    val placement = geometry.placement
    val canvas = geometry.canvas
    return Rect(
        displayLeft + displayWidth * placement.left / canvas.width,
        displayTop + displayHeight * placement.top / canvas.height,
        displayLeft + displayWidth * (placement.left + placement.width) / canvas.width,
        displayTop + displayHeight * (placement.top + placement.height) / canvas.height,
    )
}

@Composable
private fun ExportProgress(state: ReviewExportState, onCancel: () -> Unit) {
    val headline = when (state.phase) {
        ExportUiPhase.IDLE -> null
        ExportUiPhase.CHECKING_HISTORY -> R.string.export_history_checking
        ExportUiPhase.CHOOSING_FOLDER -> R.string.export_select_folder
        ExportUiPhase.RUNNING -> if (state.pauseRequested) R.string.export_pausing else R.string.export_in_progress
        ExportUiPhase.COMPLETED -> R.string.export_finished
        ExportUiPhase.BLOCKED -> R.string.export_blocked
        ExportUiPhase.NEEDS_REVIEW -> R.string.export_requires_review
        ExportUiPhase.CANCELLED -> R.string.export_cancelled
        ExportUiPhase.PAUSED -> R.string.export_paused
        ExportUiPhase.ERROR -> R.string.export_destination_error
    }
    if (headline != null) {
        val message = when (state.phase) {
            ExportUiPhase.COMPLETED -> stringResource(headline, state.published, state.failed)
            ExportUiPhase.BLOCKED -> stringResource(headline, state.blocked)
            else -> stringResource(headline)
        }
        Text(message, style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
    if (state.total > 0) {
        Text(stringResource(R.string.export_progress, state.processed, state.total),
            style = MaterialTheme.typography.bodyMedium)
        state.items.forEachIndexed { index, item ->
            val label = when (item.phase) {
                BatchItemPhase.QUEUED -> R.string.export_item_queued
                BatchItemPhase.ENCODING -> R.string.export_item_encoding
                BatchItemPhase.SAVING -> R.string.export_item_saving
                BatchItemPhase.PUBLISHED -> R.string.export_item_published
                BatchItemPhase.FAILED -> R.string.export_item_failed
                BatchItemPhase.CANCELLED -> if (item.reason == "PAUSED") R.string.export_item_paused else R.string.export_item_cancelled
                BatchItemPhase.NEEDS_REVIEW -> R.string.export_item_needs_review
            }
            Text(stringResource(R.string.export_item_status, index + 1, stringResource(label)),
                style = MaterialTheme.typography.bodySmall)
            val reason = ResultFeedback.exportReason(item)
            if (reason != null) {
                Text(stringResource(reason), style = MaterialTheme.typography.bodySmall,
                    color = if (item.phase in setOf(BatchItemPhase.FAILED, BatchItemPhase.NEEDS_REVIEW))
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
            if (state.canAcknowledge && state.retryReady && item.itemId in state.retryableIds &&
                item.phase in setOf(BatchItemPhase.FAILED, BatchItemPhase.CANCELLED)) {
                Text(stringResource(R.string.result_retry_available), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (state.phase == ExportUiPhase.RUNNING) {
        OutlinedButton(onClick = onCancel, enabled = !state.cancelRequested) {
            Text(stringResource(R.string.export_cancel_action))
        }
    }
}

@Composable
private fun ImportFeedback(report: ImportReport, enabled: Boolean, onClear: () -> Unit) {
    var expanded by remember(report) { mutableStateOf(false) }
    Text(stringResource(R.string.import_feedback_summary, report.added, report.problems.size, report.ignored),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodySmall)
    if (report.notAttempted > 0) Text(stringResource(R.string.import_not_attempted, report.notAttempted),
        style = MaterialTheme.typography.bodySmall)
    if (report.problems.isNotEmpty()) {
        OutlinedButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.import_feedback_hide else R.string.import_feedback_show))
        }
        if (expanded) {
            Text(stringResource(R.string.import_feedback_order), style = MaterialTheme.typography.bodySmall)
            report.problems.forEach { problem ->
                Text(stringResource(R.string.import_feedback_item, problem.position),
                    style = MaterialTheme.typography.titleSmall)
                Text(stringResource(ResultFeedback.importReason(problem.reason)),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    TextButton(onClick = onClear, enabled = enabled) { Text(stringResource(R.string.import_feedback_dismiss)) }
}

/** Selection is explicit; only committed outputs may be offered to another application. */
@Composable
private fun ExportShareControls(
    state: ReviewExportState,
    selected: Set<String>,
    busy: Boolean,
    error: Boolean,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
) {
    Text(stringResource(R.string.export_share_choose), style = MaterialTheme.typography.titleSmall)
    val eligible = state.items.filter { it.phase == BatchItemPhase.PUBLISHED && it.uri != null }
        .mapTo(linkedSetOf()) { it.itemId }
    TextButton(onClick = onSelectAll, enabled = !busy && eligible.isNotEmpty() && selected != eligible) {
        Text(stringResource(R.string.export_share_select_all))
    }
    TextButton(onClick = onClear, enabled = !busy && selected.isNotEmpty()) {
        Text(stringResource(R.string.export_share_clear))
    }
    state.items.forEachIndexed { index, item ->
        if (item.phase == BatchItemPhase.PUBLISHED && item.uri != null) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                    value = item.itemId in selected, enabled = !busy, role = Role.Checkbox,
                    onValueChange = { onToggle(item.itemId) }), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = item.itemId in selected, enabled = !busy,
                    onCheckedChange = null)
                Text(stringResource(R.string.export_share_item, index + 1))
            }
        }
    }
    Button(onClick = onShare, enabled = !busy && selected.isNotEmpty()) {
        Text(stringResource(R.string.export_share_selected, selected.size))
    }
    if (busy) Text(stringResource(R.string.export_share_verifying))
    if (error) Text(stringResource(R.string.export_share_unavailable),
        color = MaterialTheme.colorScheme.error)
}


