package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.DestinationCatalog
import org.cajapersonal.fixmyshot.core.DestinationChoice
import org.cajapersonal.fixmyshot.core.DraftReviewFlag
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.core.FitMode
import org.cajapersonal.fixmyshot.core.CropFocus
import org.cajapersonal.fixmyshot.core.PlannedGeometry
import org.cajapersonal.fixmyshot.core.OutputSizeChoice
import org.cajapersonal.fixmyshot.core.MetadataPolicy
import org.cajapersonal.fixmyshot.export.BatchItemPhase

/** Review plus deliberate, foreground-only batch export and bounded progress. */
@Composable
internal fun ReviewScreen(
    draft: DraftSession,
    selectedId: String?,
    preview: Bitmap?,
    thumbnails: Map<String, Bitmap>,
    busy: Boolean,
    invalidImports: Int,
    previewUnavailable: Boolean,
    api: Int,
    exportState: ReviewExportState,
    onExport: () -> Unit,
    onCancelExport: () -> Unit,
    onReviewedPrevious: () -> Unit,
    selectedShareIds: Set<String>,
    shareBusy: Boolean,
    shareError: Boolean,
    onToggleShare: (String) -> Unit,
    onShare: () -> Unit,
    onChoose: () -> Unit,
    onSelect: (String) -> Unit,
    onDestination: (DestinationChoice) -> Unit,
    onFormat: (OutputFormat, Boolean) -> Unit,
    onBackground: (BackgroundChoice, Boolean) -> Unit,
    onResetSelected: () -> Unit,
    onUndo: () -> Unit,
    onRemove: () -> Unit,
    onDiscard: () -> Unit,
    importLimitExceeded: Boolean = false,
    pendingImports: Int = 0,
    onSettings: () -> Unit = {},
    onFit: (FitMode, Boolean) -> Unit = { _, _ -> },
    onCrop: (CropFocus) -> Unit = {},
    onRetryExport: () -> Unit = {},
    onRestoreDraft: () -> Unit = {},
    onRefreshOutputs: () -> Unit = {},
    onOpenOutput: (String) -> Unit = {},
    onConfirmOutput: (String) -> Unit = {},
    onSize: (OutputSizeChoice, Boolean) -> Unit = { _, _ -> },
    onApplyBatchToAll: () -> Unit = {},
    onMoveSelected: (Int) -> Unit = {},
    onMetadata: (MetadataPolicy, Boolean) -> Unit = { _, _ -> },
) {
    val plans = remember(draft, api) { draft.plans(api) }
    val selected = draft.images.firstOrNull { it.id == selectedId }
    val plan = plans.firstOrNull { it.itemId == selectedId }
    val editEnabled = !busy && pendingImports == 0 && !exportState.isLocked
    var confirmingOutput by remember { mutableStateOf<String?>(null) }
    var confirmingBatchReset by remember { mutableStateOf(false) }
    val customCount = draft.images.count { it.overrides.hasCustomValues }
    if (confirmingBatchReset) AlertDialog(
        onDismissRequest = { confirmingBatchReset = false },
        title = { Text(stringResource(R.string.batch_apply_all)) },
        text = { Text(stringResource(R.string.batch_apply_explanation, customCount)) },
        confirmButton = { TextButton(onClick = {
            confirmingBatchReset = false
            onApplyBatchToAll()
        }) { Text(stringResource(R.string.batch_apply_all)) } },
        dismissButton = { TextButton(onClick = { confirmingBatchReset = false }) {
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

    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onSettings) { Text(stringResource(R.string.settings_title)) }
        Button(onClick = onChoose, enabled = editEnabled) { Text(stringResource(R.string.choose_images)) }
        if (busy) Text(stringResource(R.string.import_in_progress))
        if (pendingImports > 0) Text(stringResource(R.string.import_waiting, pendingImports))
        if (importLimitExceeded) Text(stringResource(R.string.import_limit),
            color = MaterialTheme.colorScheme.error)
        if (invalidImports > 0) Text(stringResource(R.string.import_skipped, invalidImports),
            color = MaterialTheme.colorScheme.error)
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
                    } else {
                        Text(stringResource(if (previewUnavailable) R.string.preview_unavailable
                            else R.string.preview_loading))
                    }
                }
            }
            ReviewControls(
                draft = draft, plan = plan, enabled = editEnabled,
                onDestination = onDestination, onFormat = onFormat, onBackground = onBackground, onFit = onFit, onSize = onSize,
                onMetadata = onMetadata,
            )
            if (customCount > 0) {
                Text(stringResource(R.string.batch_custom_count, customCount))
                OutlinedButton(enabled = editEnabled, onClick = { confirmingBatchReset = true }) {
                    Text(stringResource(R.string.batch_apply_all))
                }
            }
            if (selected != null && plan != null) CropControls(selected, plan, editEnabled, onCrop)
            if (selected != null && plan != null) {
                val output = when (val geometry = plan.geometry) {
                    is PlannedGeometry.Contained -> geometry.value.canvas
                    is PlannedGeometry.Cropped -> geometry.value.output
                    null -> null
                }
                if (output != null) Text(stringResource(R.string.output_dimensions, output.width, output.height))
                val contained = (plan.geometry as? PlannedGeometry.Contained)?.value
                val cropped = (plan.geometry as? PlannedGeometry.Cropped)?.value
                val resized = contained?.resized == true || cropped?.resized == true
                val enlarged = contained?.enlarged == true || cropped?.enlarged == true
                if (resized) Text(stringResource(if (enlarged) R.string.size_will_enlarge else R.string.size_will_reduce),
                    style = MaterialTheme.typography.bodySmall)
                if (contained != null) {
                    Text(stringResource(R.string.margin_details, contained.placement.left,
                        contained.rightMargin, contained.placement.top, contained.bottomMargin))
                }
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
                Button(onClick = onExport, enabled = editEnabled &&
                    exportState.phase == ExportUiPhase.IDLE) {
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
        if (draft.workingSourceIds.isNotEmpty()) OutlinedButton(onClick = onDiscard, enabled = editEnabled) {
            Text(stringResource(R.string.discard_session))
        }
        if (exportState.phase != ExportUiPhase.IDLE) ExportProgress(exportState, onCancelExport)
        if (exportState.published > 0 && exportState.phase in setOf(
                ExportUiPhase.COMPLETED, ExportUiPhase.CANCELLED, ExportUiPhase.NEEDS_REVIEW)) {
            ExportShareControls(exportState, selectedShareIds, shareBusy || exportState.outputBusy, shareError,
                onToggleShare, onShare)
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
                Text(stringResource(R.string.export_retry_remaining, exportState.retryableIds.size))
            }
        }
        if (exportState.previousJobs > 0 || exportState.canAcknowledge) {
            ExportJobNotice(exportState, onReviewedPrevious)
        }
    }
}

@Composable
private fun ExportProgress(state: ReviewExportState, onCancel: () -> Unit) {
    val headline = when (state.phase) {
        ExportUiPhase.IDLE -> null
        ExportUiPhase.CHECKING_HISTORY -> R.string.export_history_checking
        ExportUiPhase.CHOOSING_FOLDER -> R.string.export_select_folder
        ExportUiPhase.RUNNING -> R.string.export_in_progress
        ExportUiPhase.COMPLETED -> R.string.export_finished
        ExportUiPhase.BLOCKED -> R.string.export_blocked
        ExportUiPhase.NEEDS_REVIEW -> R.string.export_requires_review
        ExportUiPhase.CANCELLED -> R.string.export_cancelled
        ExportUiPhase.ERROR -> R.string.export_destination_error
    }
    if (headline != null) {
        val message = when (state.phase) {
            ExportUiPhase.COMPLETED -> stringResource(headline, state.published, state.failed)
            ExportUiPhase.BLOCKED -> stringResource(headline, state.blocked)
            else -> stringResource(headline)
        }
        Text(message, style = MaterialTheme.typography.titleSmall)
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
                BatchItemPhase.CANCELLED -> R.string.export_item_cancelled
                BatchItemPhase.NEEDS_REVIEW -> R.string.export_item_needs_review
            }
            Text(stringResource(R.string.export_item_status, index + 1, stringResource(label)),
                style = MaterialTheme.typography.bodySmall)
            if (item.phase == BatchItemPhase.NEEDS_REVIEW) {
                val reason = when (item.reason) {
                    "OUTPUT_PRESENT_UNCERTAIN" -> R.string.export_output_present_uncertain
                    "OUTPUT_MISSING" -> R.string.export_output_missing
                    "OUTPUT_CHANGED" -> R.string.export_output_changed
                    "OUTPUT_PENDING" -> R.string.export_output_pending
                    "OUTPUT_UNVERIFIABLE" -> R.string.export_output_unverifiable
                    else -> null
                }
                if (reason != null) {
                    Text(stringResource(reason), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (state.phase == ExportUiPhase.RUNNING) {
        OutlinedButton(onClick = onCancel, enabled = !state.cancelRequested) {
            Text(stringResource(R.string.export_cancel_action))
        }
    }
}

/** Selection is explicit; only committed outputs may be offered to another application. */
@Composable
private fun ExportShareControls(
    state: ReviewExportState,
    selected: Set<String>,
    busy: Boolean,
    error: Boolean,
    onToggle: (String) -> Unit,
    onShare: () -> Unit,
) {
    Text(stringResource(R.string.export_share_choose), style = MaterialTheme.typography.titleSmall)
    state.items.forEachIndexed { index, item ->
        if (item.phase == BatchItemPhase.PUBLISHED && item.uri != null) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = item.itemId in selected, enabled = !busy,
                    onCheckedChange = { onToggle(item.itemId) })
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


