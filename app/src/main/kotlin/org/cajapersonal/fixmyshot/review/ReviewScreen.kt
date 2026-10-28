package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.net.Uri
import org.cajapersonal.fixmyshot.export.PendingMediaStoreRemoval
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.ContainGeometry
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.ContainPosition
import org.cajapersonal.fixmyshot.core.DestinationChoice
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
    onTemplate: (DestinationChoice) -> Unit = onDestination,
    onApplyFraming: (DraftSession, String, org.cajapersonal.fixmyshot.core.EditSettings) -> Unit = { _, _, _ -> },
) {
    val plans = remember(draft, api) { draft.plans(api) }
    val selected = draft.images.firstOrNull { it.id == selectedId }
    val plan = plans.firstOrNull { it.itemId == selectedId }
    val editEnabled = !busy && !shareBusy && !sessionRestoreBlocked && pendingImports == 0 && !exportState.isLocked
    var confirmingOutput by remember { mutableStateOf<String?>(null) }
    var removingPending by remember { mutableStateOf<String?>(null) }
    var confirmingBatchReset by remember(draft) { mutableStateOf<DraftSession?>(null) }
    var confirmingDiscard by remember(draft) { mutableStateOf<DraftSession?>(null) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val advancedState = rememberSaveableStateHolder()
    var focusOpen by rememberSaveable { mutableStateOf(false) }
    var focusId by rememberSaveable { mutableStateOf<String?>(null) }
    var focusRevision by rememberSaveable { mutableStateOf(-1L) }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    var backgroundsOpen by rememberSaveable { mutableStateOf(false) }
    var comparing by remember { mutableStateOf(false) }
    var requestCrop by remember { mutableStateOf(false) }
    var confirmingSavedDiscard by remember { mutableStateOf(false) }
    val hasResults = exportState.phase !in setOf(ExportUiPhase.IDLE, ExportUiPhase.CHECKING_HISTORY) ||
        exportState.previousJobs > 0 || exportState.canAcknowledge
    val hasFeedback = importProgress != null || busy || pendingImports > 0 || invalidImports > 0 ||
        importReport != null || importLimitExceeded || sessionRestored || sessionExpired || sessionSaveError || sessionRestoreBlocked
    LaunchedEffect(exportState.phase) {
        if (exportState.phase in setOf(ExportUiPhase.RUNNING, ExportUiPhase.COMPLETED, ExportUiPhase.BLOCKED,
                ExportUiPhase.NEEDS_REVIEW, ExportUiPhase.CANCELLED, ExportUiPhase.PAUSED, ExportUiPhase.ERROR)) infoOpen = true
    }
    LaunchedEffect(draft.revision, selectedId, editEnabled) {
        if (focusOpen && (focusRevision != draft.revision || focusId != selectedId || !editEnabled)) focusOpen = false
        comparing = false
    }
    fun expandPreview() {
        if (editEnabled && selected != null && preview != null) {
            focusId = selected.id; focusRevision = draft.revision; focusOpen = true
        }
    }
    BackHandler(enabled = advanced && !focusOpen && !infoOpen) { advanced = false }
    var showOriginalOutline by rememberSaveable(editorSessionKey) { mutableStateOf(false) }
    var tool by rememberSaveable(editorSessionKey) { mutableStateOf(ReviewTool.DESTINATION) }
    var crop by remember(selectedId, plan?.sessionRevision) { mutableStateOf(plan?.settings?.crop ?: CropFocus()) }
    var position by remember(selectedId, plan?.sessionRevision) { mutableStateOf(plan?.settings?.position ?: ContainPosition()) }
    fun editCrop(next: CropFocus) {
        if (!editEnabled || selected == null || plan == null) return
        try {
            val geometry = org.cajapersonal.fixmyshot.core.CropPlanner.plan(selected.orientedSize,
                plan.destination.cropRatio(selected.orientedSize, plan.settings.size), next,
                exactOutput = (plan.settings.size as? OutputSizeChoice.Exact)?.canvas)
            crop = CropFocus(geometry.normalizedCenterX, geometry.normalizedCenterY, next.zoom)
        } catch (_: IllegalArgumentException) { /* Keep the last valid frame. */ }
    }
    val stagedPlan = remember(plan, crop, position) {
        if (plan == null || selected == null) plan else when (val geometry = plan.geometry) {
            is PlannedGeometry.Contained -> plan.copy(settings = plan.settings.copy(position = position),
                geometry = PlannedGeometry.Contained(geometry.value.positioned(position)))
            is PlannedGeometry.Cropped -> plan.copy(settings = plan.settings.copy(crop = crop),
                geometry = PlannedGeometry.Cropped(org.cajapersonal.fixmyshot.core.CropPlanner.plan(
                    selected.orientedSize, plan.destination.cropRatio(selected.orientedSize, plan.settings.size), crop,
                    exactOutput = (plan.settings.size as? OutputSizeChoice.Exact)?.canvas)))
            null -> plan
        }
    }
    fun finishGesture() {
        if (!editEnabled || plan == null) return
        if (plan.geometry is PlannedGeometry.Cropped && crop != plan.settings.crop) onCrop(crop)
        else if (plan.geometry is PlannedGeometry.Contained && position != plan.settings.position) onPosition(position)
    }
    val customCount = draft.images.count { it.overrides.hasCustomValues }
    val convertedCount = draft.images.count { it.sourceColor != SourceColor.SRGB }
    val batchResetDraft = confirmingBatchReset
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
        }) { Text(stringResource(R.string.quick_discard)) } },
        dismissButton = { TextButton(onClick = { confirmingDiscard = null }) {
            Text(stringResource(R.string.quick_keep_editing))
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

    val panel: @Composable (Boolean) -> Unit = { showTools ->
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sessionRestored) Text(stringResource(R.string.session_restored), style = MaterialTheme.typography.bodySmall)
        if (sessionExpired) Text(stringResource(R.string.session_expired))
        if (sessionRestoreBlocked) {
            Text(stringResource(R.string.session_restore_failed), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { confirmingSavedDiscard = true }, enabled = !busy && !exportState.isLocked) {
                Text(stringResource(R.string.session_discard_saved))
            }
        } else if (sessionSaveError) {
            Text(stringResource(R.string.session_save_failed), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetrySessionSave, enabled = !busy && !exportState.isLocked) {
                Text(stringResource(R.string.session_retry_save))
            }
        }
        if (showTools && draft.images.isEmpty()) Button(onClick = onChoose, enabled = editEnabled) { Text(stringResource(R.string.choose_images)) }
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
        if (showTools) {
        if (draft.images.isEmpty()) {
            Text(stringResource(R.string.choose_prompt))
            Text(stringResource(R.string.preview_only_notice), style = MaterialTheme.typography.bodySmall)
        } else {
            if (draft.images.size > 1) Text(stringResource(R.string.batch_count, draft.images.size), style = MaterialTheme.typography.titleMedium)
            // A lazy strip avoids composing 100 image controls at once.
            // Thumbnails depict originals, not scaled output or completed exports.
            if (draft.images.size > 1) LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            androidx.compose.runtime.key(editorSessionKey) {
                ReviewControls(
                    tool = tool, draft = draft, plan = stagedPlan, enabled = editEnabled,
                    selectedOnly = editSelectedOnly, onSelectedOnly = onEditSelectedOnly,
                    onDestination = onDestination, onFormat = onFormat, onBackground = onBackground,
                    onFit = onFit, onSize = onSize, onMetadata = onMetadata,
                )
            }
            if (draft.images.size > 1 && customCount > 0 && tool == ReviewTool.EXPORT) {
                Text(stringResource(R.string.batch_custom_count, customCount))
                OutlinedButton(enabled = editEnabled, onClick = { confirmingBatchReset = draft }) {
                    Text(stringResource(R.string.batch_apply_all))
                }
            }
            if (tool == ReviewTool.FRAMING && selected != null && stagedPlan != null) {
                Text(stringResource(if (stagedPlan.geometry is PlannedGeometry.Cropped) R.string.crop_notice
                    else R.string.editor_move_help), style = MaterialTheme.typography.bodySmall)
                if (stagedPlan.geometry is PlannedGeometry.Cropped) {
                    CropControls(selected, stagedPlan, editEnabled, ::editCrop, ::finishGesture)
                } else {
                    ContainPositionControls(stagedPlan, editEnabled, onPosition)
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = showOriginalOutline,
                    role = Role.Checkbox, onValueChange = { showOriginalOutline = it }),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(showOriginalOutline, onCheckedChange = null)
                    Text(stringResource(R.string.preview_original_outline))
                }
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
                if (tool == ReviewTool.EXPORT) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onResetSelected, enabled = editEnabled) { Text(stringResource(R.string.reset_image)) }
                    OutlinedButton(onClick = onRemove, enabled = editEnabled) { Text(stringResource(R.string.remove_image)) }
                }
                if (tool == ReviewTool.EXPORT && draft.images.size > 1) {
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
                Text(stringResource(if (api >= 29) R.string.export_destination_pictures
                    else R.string.export_destination_saf), style = MaterialTheme.typography.bodySmall)
                if (exportState.phase == ExportUiPhase.IDLE) {
                    Text(stringResource(R.string.preview_only_notice),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
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
    if (confirmingSavedDiscard && sessionRestoreBlocked) AlertDialog(
        onDismissRequest = { confirmingSavedDiscard = false },
        title = { Text(stringResource(R.string.quick_discard)) },
        text = { Text(stringResource(R.string.discard_session_confirmation)) },
        dismissButton = { TextButton(onClick = { confirmingSavedDiscard = false }) { Text(stringResource(R.string.quick_keep_editing)) } },
        confirmButton = { TextButton(onClick = { confirmingSavedDiscard = false; onDiscardUnavailableSession() },
            enabled = !busy && !exportState.isLocked) { Text(stringResource(R.string.quick_discard)) } },
    )
    if (requestCrop) CropConsentDialog(onCancel = { requestCrop = false }, onConfirm = {
        requestCrop = false; onFit(FitMode.CROP, true)
    })
    if (focusOpen && focusId == selectedId && focusRevision == draft.revision && selected != null && preview != null && editEnabled) {
        androidx.compose.runtime.key(editorSessionKey, selected.id, focusRevision) {
            FocusEditor(draft, selected.id, preview, api, onApplyFraming,
                onDismiss = { focusOpen = false }, onDiscard = { confirmingDiscard = draft })
        }
    }
    if (infoOpen) Dialog(onDismissRequest = { infoOpen = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (hasResults) R.string.quick_results else R.string.quick_details),
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { infoOpen = false }) { Text(stringResource(R.string.quick_done)) }
                }
                Box(Modifier.weight(1f)) { panel(false) }
            }
        }
    }
    val stage: @Composable () -> Unit = {
        val live = stagedPlan
        val contained = (live?.geometry as? PlannedGeometry.Contained)?.value
        val cropped = (live?.geometry as? PlannedGeometry.Cropped)?.value
        val output = contained?.canvas ?: cropped?.output
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (selected != null && live != null && output != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.editor_summary, formatLabel(live.settings.format),
                            stringResource(if (live.settings.fit == FitMode.CONTAIN) R.string.quick_keep_all else R.string.quick_fill_crop),
                            output.width, output.height), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(if (live.settings.metadata == MetadataPolicy.REMOVE_IDENTIFYING)
                            R.string.editor_metadata_removed else R.string.editor_metadata_kept),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = ::expandPreview, enabled = editEnabled && preview != null) {
                        Text(stringResource(R.string.quick_expand))
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    if (comparing && preview != null) Image(preview.asImageBitmap(), stringResource(R.string.quick_original),
                        Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else if (preview != null) ReviewCanvas(preview, selected, live,
                        editEnabled && (!advanced || tool == ReviewTool.FRAMING), showOriginalOutline, Modifier.fillMaxSize(),
                        ::editCrop, { position = it }, ::finishGesture, onCropCommitted = onCrop,
                        onPositionCommitted = onPosition, onTap = ::expandPreview,
                        onContainZoom = { requestCrop = true },
                        onGestureCancelled = { crop = plan?.settings?.crop ?: CropFocus(); position = plan?.settings?.position ?: ContainPosition() })
                    else Text(stringResource(if (previewUnavailable) R.string.preview_unavailable else R.string.preview_loading))
                }
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(if (selected == null) R.string.choose_prompt else R.string.crop_unavailable))
            }
        }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painterResource(R.drawable.brand_header), stringResource(R.string.app_name),
                modifier = Modifier.weight(1f).height(42.dp), contentScale = ContentScale.Fit)
            if (draft.workingSourceIds.isNotEmpty() || sessionRestoreBlocked) TextButton(onClick = {
                if (!editEnabled && !sessionRestoreBlocked) infoOpen = true
                else if (sessionRestoreBlocked) confirmingSavedDiscard = true else confirmingDiscard = draft
            }) { Text(stringResource(R.string.quick_discard)) }
            TextButton(onClick = onSettings) { Text(stringResource(R.string.quick_settings)) }
        }
        if (hasResults || hasFeedback) TextButton(onClick = { infoOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (hasResults) R.string.quick_open_results else R.string.quick_open_feedback))
        }
        if (busy || exportState.phase == ExportUiPhase.CHECKING_HISTORY) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (draft.images.isEmpty()) {
            Box(Modifier.weight(1f)) { BrandStart(editEnabled, onChoose, draft.canUndo, onUndo) }
        } else {
            if (advanced) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { finishGesture(); advanced = false }) { Text(stringResource(R.string.quick_automatic)) }
                    Text(stringResource(R.string.quick_customize), style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onChoose, enabled = editEnabled) { Text(stringResource(R.string.editor_add)) }
                }
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(ReviewTool.entries) { _, value ->
                        FilterChip(selected = tool == value, enabled = true,
                            onClick = { finishGesture(); tool = value }, label = { Text(stringResource(value.label)) })
                    }
                }
                ReviewWorkspace(Modifier.weight(1f), hasImage = selected != null, stage = stage, controls = {
                    advancedState.SaveableStateProvider("advanced") { panel(true) }
                })
            } else {
                if (draft.images.size > 1) QuickBatchStrip(draft, plans, selectedId, thumbnails, editEnabled, onSelect, onRemove)
                QuickWorkspace(Modifier.weight(1f), stage = stage, tools = {
                    QuickTemplateCards(draft.destination, editEnabled) { finishGesture(); onTemplate(it) }
                    Column(Modifier.padding(horizontal = 12.dp)) {
                        val quick = stagedPlan?.settings
                        if (quick != null) QuickFramingRow(quick.fit, editEnabled, { onFit(it, true) })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            CompareOriginalButton(comparing, preview != null && editEnabled) { comparing = it }
                            TextButton(onClick = { backgroundsOpen = !backgroundsOpen }) { Text(stringResource(R.string.editor_background)) }
                            TextButton(onClick = { finishGesture(); advanced = true }) { Text(stringResource(R.string.quick_customize)) }
                        }
                        if (backgroundsOpen && quick != null) QuickBackgroundRow(quick.background, editEnabled) { onBackground(it, true) }
                        if (plans.any { it.requiresReview }) TextButton(onClick = { advanced = true; tool = ReviewTool.EXPORT }) {
                            Text(stringResource(R.string.editor_review_required), color = MaterialTheme.colorScheme.error)
                        }
                        if (customCount > 0) Text(stringResource(R.string.quick_custom_count, customCount),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (draft.canUndo) TextButton(onClick = { crop = plan?.settings?.crop ?: CropFocus(); onUndo() }, enabled = editEnabled) {
                    Text(stringResource(R.string.undo_last))
                }
                Button(onClick = { finishGesture(); onExport() }, enabled = editEnabled && canPrepareExport,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    val formats = plans.map { it.settings.format }.distinct()
                    Text(if (formats.size == 1 && formats.single() == OutputFormat.PNG)
                        stringResource(R.string.quick_export_png, draft.images.size)
                        else stringResource(R.string.export_batch, draft.images.size))
                }
            }
        }
    }

}

/** Exact ContentScale.Fit viewport of the final output bitmap, not the source rectangle. */
internal fun previewOutputCanvasRect(previewSize: PixelSize, viewport: Size): Rect {
    val scale = minOf(viewport.width / previewSize.width, viewport.height / previewSize.height)
    val width = previewSize.width * scale
    val height = previewSize.height * scale
    val left = (viewport.width - width) / 2f
    val top = (viewport.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

/** Project immutable source placement onto the actual ContentScale.Fit viewport, including letterboxing. */
internal fun previewOriginalContentRect(previewSize: PixelSize, viewport: Size, geometry: ContainGeometry): Rect {
    val output = previewOutputCanvasRect(previewSize, viewport)
    val placement = geometry.placement
    val canvas = geometry.canvas
    return Rect(
        output.left + output.width * placement.left / canvas.width,
        output.top + output.height * placement.top / canvas.height,
        output.left + output.width * (placement.left + placement.width) / canvas.width,
        output.top + output.height * (placement.top + placement.height) / canvas.height,
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


