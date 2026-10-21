package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.DestinationCatalog
import org.cajapersonal.fixmyshot.core.DestinationChoice
import org.cajapersonal.fixmyshot.core.DraftReviewFlag
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.core.PlannedGeometry

/** A functional picker/review slice, not a claim that output publication is implemented. */
@Composable
internal fun ReviewScreen(
    draft: DraftSession,
    selectedId: String?,
    preview: Bitmap?,
    busy: Boolean,
    invalidImports: Int,
    previewUnavailable: Boolean,
    api: Int,
    onChoose: () -> Unit,
    onSelect: (String) -> Unit,
    onDestination: (DestinationChoice) -> Unit,
    onFormat: (OutputFormat, Boolean) -> Unit,
    onBackground: (BackgroundChoice, Boolean) -> Unit,
    onResetSelected: () -> Unit,
    onUndo: () -> Unit,
    onRemove: () -> Unit,
    onDiscard: () -> Unit,
) {
    val plans = remember(draft, api) { draft.plans(api) }
    val selected = draft.images.firstOrNull { it.id == selectedId }
    val plan = plans.firstOrNull { it.itemId == selectedId }


    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onChoose, enabled = !busy) { Text(stringResource(R.string.choose_images)) }
        if (busy) Text(stringResource(R.string.import_in_progress))
        if (invalidImports > 0) Text(stringResource(R.string.import_skipped, invalidImports),
            color = MaterialTheme.colorScheme.error)
        if (draft.images.isEmpty()) {
            Text(stringResource(R.string.choose_prompt))
            Text(stringResource(R.string.preview_only_notice), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(stringResource(R.string.batch_count, draft.images.size), style = MaterialTheme.typography.titleMedium)
            // The first slice uses bounded numbered selectors; image thumbnails follow with the cache.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.images.forEachIndexed { index, image ->
                    OutlinedButton(onClick = { onSelect(image.id) }) {
                        val marker = if (image.id == selectedId) "● " else ""
                        Text("$marker${index + 1} · ${image.orientedSize.width}×${image.orientedSize.height}")
                    }
                }
            }
            if (selected != null) {
                Box(Modifier.fillMaxWidth().height(320.dp).background(MaterialTheme.colorScheme.surfaceVariant),
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
                draft = draft, plan = plan,
                onDestination = onDestination, onFormat = onFormat, onBackground = onBackground,
            )
            Text(stringResource(R.string.metadata_removed), style = MaterialTheme.typography.bodySmall)
            if (selected != null && plan != null) {
                val output = when (val geometry = plan.geometry) {
                    is PlannedGeometry.Contained -> geometry.value.canvas
                    is PlannedGeometry.Cropped -> geometry.value.output
                    null -> null
                }
                if (output != null) Text(stringResource(R.string.output_dimensions, output.width, output.height))
                val contained = (plan.geometry as? PlannedGeometry.Contained)?.value
                if (contained != null) {
                    Text(stringResource(R.string.margin_details, contained.placement.left,
                        contained.rightMargin, contained.placement.top, contained.bottomMargin))
                }
                plan.reviewFlags.forEach { flag ->
                    Text(reviewFlagLabel(flag), color = MaterialTheme.colorScheme.error)
                }
                if (selected.overrides.hasCustomValues) Text(stringResource(R.string.custom_settings_label))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onResetSelected) { Text(stringResource(R.string.reset_image)) }
                    OutlinedButton(onClick = onUndo) { Text(stringResource(R.string.undo_last)) }
                    OutlinedButton(onClick = onRemove) { Text(stringResource(R.string.remove_image)) }
                }
                Button(onClick = {}, enabled = false) { Text(stringResource(R.string.export_not_ready)) }
                Text(stringResource(R.string.preview_only_notice), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onDiscard) { Text(stringResource(R.string.discard_session)) }
        }
    }
}

