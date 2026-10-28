package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.DestinationCatalog
import org.cajapersonal.fixmyshot.core.DestinationChoice
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.core.PreviewPlan
import org.cajapersonal.fixmyshot.core.FitMode
import org.cajapersonal.fixmyshot.core.OutputSizeChoice
import org.cajapersonal.fixmyshot.core.MetadataPolicy

internal enum class ReviewTool(val label: Int) {
    DESTINATION(R.string.editor_destination), FRAMING(R.string.editor_framing),
    BACKGROUND(R.string.editor_background), EXPORT(R.string.editor_export),
}

@Composable
internal fun ReviewControls(
    tool: ReviewTool,
    draft: DraftSession,
    plan: PreviewPlan?,
    enabled: Boolean,
    selectedOnly: Boolean,
    onSelectedOnly: (Boolean) -> Unit,
    onDestination: (DestinationChoice) -> Unit,
    onFormat: (OutputFormat, Boolean) -> Unit,
    onBackground: (BackgroundChoice, Boolean) -> Unit,
    onFit: (FitMode, Boolean) -> Unit = { _, _ -> },
    onSize: (OutputSizeChoice, Boolean) -> Unit = { _, _ -> },
    onMetadata: (MetadataPolicy, Boolean) -> Unit = { _, _ -> },
) {
    var chooseDestination by remember { mutableStateOf(false) }
    val effective = if (selectedOnly) plan?.settings ?: draft.batch else draft.batch
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tool == ReviewTool.DESTINATION) {
            Text(stringResource(R.string.destination_heading), style = MaterialTheme.typography.titleMedium)
            Box {
                OutlinedButton(enabled = enabled, onClick = { chooseDestination = true }) {
                    Text(destinationLabel(draft.destination))
                }
                DropdownMenu(expanded = chooseDestination, onDismissRequest = { chooseDestination = false }) {
                    DestinationCatalog.presets.filter { it.id != DestinationCatalog.CUSTOM_RATIO }.forEach { preset ->
                        DropdownMenuItem(enabled = enabled, text = { Text(destinationLabel(DestinationChoice.Preset(preset.id))) },
                            onClick = { chooseDestination = false; onDestination(DestinationChoice.Preset(preset.id)) })
                    }
                }
            }
            key(draft.destination) {
                var custom by rememberSaveable { mutableStateOf(draft.destination is DestinationChoice.Custom) }
                if (!custom) TextButton(onClick = { custom = true }, enabled = enabled) {
                    Text(stringResource(R.string.custom_ratio_label))
                }
                if (custom) CustomRatioControl(draft.destination, enabled, onDestination)
            }
            if (draft.images.size > 1) Text(stringResource(R.string.destination_batch_notice), style = MaterialTheme.typography.bodySmall)
            }
            if (draft.images.size > 1 && tool != ReviewTool.DESTINATION) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                value = selectedOnly, enabled = enabled, role = Role.Checkbox,
                onValueChange = onSelectedOnly), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selectedOnly, enabled = enabled, onCheckedChange = null)
                Text(stringResource(R.string.selected_only))
            }
            Text(stringResource(if (selectedOnly) R.string.edit_scope_image else R.string.edit_scope_batch),
                style = MaterialTheme.typography.titleSmall)
            }
            // A new image/scope drops unapplied fields; recreation of the same scope restores them.
            key(selectedOnly, if (selectedOnly) plan?.itemId else null) {
                var chooseFormat by remember { mutableStateOf(false) }
                var chooseBackground by remember { mutableStateOf(false) }
                val settingsEnabled = enabled && (!selectedOnly || plan != null)
                if (tool == ReviewTool.FRAMING) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fit = effective.fit
                    androidx.compose.material3.FilterChip(modifier = Modifier.weight(1f), selected = fit == FitMode.CONTAIN,
                        enabled = settingsEnabled, onClick = { onFit(FitMode.CONTAIN, selectedOnly) },
                        label = { Text(stringResource(R.string.preserve_content)) })
                    androidx.compose.material3.FilterChip(modifier = Modifier.weight(1f), selected = fit == FitMode.CROP,
                        enabled = settingsEnabled, onClick = { onFit(FitMode.CROP, selectedOnly) },
                        label = { Text(stringResource(R.string.crop_mode)) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tool == ReviewTool.EXPORT) Box {
                        OutlinedButton(enabled = settingsEnabled, onClick = { chooseFormat = true }) {
                            Text(formatLabel(effective.format))
                        }
                        DropdownMenu(expanded = chooseFormat, onDismissRequest = { chooseFormat = false }) {
                            OutputFormat.entries.filter { it != OutputFormat.JXL }.forEach { format ->
                                DropdownMenuItem(enabled = settingsEnabled, text = { Text(formatLabel(format)) },
                                    onClick = { chooseFormat = false; onFormat(format, selectedOnly) })
                            }
                        }
                    }
                    if (tool == ReviewTool.BACKGROUND) Box {
                        OutlinedButton(enabled = settingsEnabled, onClick = { chooseBackground = true }) {
                            Text(backgroundLabel(effective.background))
                        }
                        DropdownMenu(expanded = chooseBackground, onDismissRequest = { chooseBackground = false }) {
                            listOf(BackgroundChoice.Automatic, BackgroundChoice.White, BackgroundChoice.Black,
                                BackgroundChoice.Transparent).forEach { choice ->
                                DropdownMenuItem(enabled = settingsEnabled, text = { Text(backgroundLabel(choice)) },
                                    onClick = { chooseBackground = false; onBackground(choice, selectedOnly) })
                            }
                        }
                    }
                }
                if (tool == ReviewTool.BACKGROUND) {
                    var custom by rememberSaveable { mutableStateOf(effective.background is BackgroundChoice.CustomRgb) }
                    if (!custom) TextButton(onClick = { custom = true }, enabled = settingsEnabled) {
                        Text(stringResource(R.string.background_custom))
                    }
                    if (custom) CustomBackgroundControl(settingsEnabled, effective.background) { onBackground(it, selectedOnly) }
                }
                if (tool == ReviewTool.EXPORT) {
                    var exact by rememberSaveable { mutableStateOf(effective.size is OutputSizeChoice.Exact) }
                    TextButton(onClick = { exact = !exact }) { Text(stringResource(R.string.size_heading)) }
                    if (exact) OutputSizeControls(effective.size, settingsEnabled) { onSize(it, selectedOnly) }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                    value = effective.metadata == MetadataPolicy.REMOVE_IDENTIFYING,
                    enabled = settingsEnabled, role = Role.Checkbox, onValueChange = {
                            onMetadata(if (it) MetadataPolicy.REMOVE_IDENTIFYING else MetadataPolicy.PRESERVE_ALLOWLIST, selectedOnly)
                        }), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = effective.metadata == MetadataPolicy.REMOVE_IDENTIFYING,
                        enabled = settingsEnabled, onCheckedChange = null)
                    Text(stringResource(R.string.metadata_remove_option))
                }
                Text(stringResource(if (effective.metadata == MetadataPolicy.REMOVE_IDENTIFYING)
                    R.string.metadata_removed else R.string.metadata_preserve_warning),
                    style = MaterialTheme.typography.bodySmall)
                }
            }
    }
}


@Composable
private fun CustomRatioControl(destination: DestinationChoice, enabled: Boolean,
    onDestination: (DestinationChoice) -> Unit) {
    val ratio = (destination as? DestinationChoice.Custom)?.ratio
    var ratioW by rememberSaveable { mutableStateOf(ratio?.p?.toString() ?: "9") }
    var ratioH by rememberSaveable { mutableStateOf(ratio?.q?.toString() ?: "16") }
    Text(stringResource(R.string.custom_ratio_label), style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(ratioW, enabled = enabled, onValueChange = { ratioW = it.take(5).filter(Char::isDigit) },
            label = { Text(stringResource(R.string.ratio_width)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(ratioH, enabled = enabled, onValueChange = { ratioH = it.take(5).filter(Char::isDigit) },
            label = { Text(stringResource(R.string.ratio_height)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.weight(1f))
    }
    val numerator = ratioW.toIntOrNull()
    val denominator = ratioH.toIntOrNull()
    OutlinedButton(enabled = enabled && numerator != null && denominator != null && numerator in 1..10000 && denominator in 1..10000,
        onClick = { onDestination(DestinationChoice.Custom.of(requireNotNull(numerator), requireNotNull(denominator))) }) {
        Text(stringResource(R.string.apply_custom_ratio))
    }
}

@Composable
private fun destinationLabel(choice: DestinationChoice): String = when (choice) {
    is DestinationChoice.Custom -> stringResource(R.string.destination_custom)
    is DestinationChoice.Preset -> when (choice.id) {
        DestinationCatalog.PLAY_PHONE -> stringResource(R.string.destination_play_phone)
        DestinationCatalog.PLAY_PORTRAIT -> stringResource(R.string.destination_play_portrait)
        DestinationCatalog.SOCIAL_SQUARE -> stringResource(R.string.destination_social_square)
        DestinationCatalog.SOCIAL_PORTRAIT -> stringResource(R.string.destination_social_portrait)
        DestinationCatalog.SOCIAL_STORY -> stringResource(R.string.destination_social_story)
        else -> stringResource(R.string.destination_custom)
    }
}

@Composable
internal fun formatLabel(format: OutputFormat): String = when (format) {
    OutputFormat.PNG -> stringResource(R.string.format_png)
    OutputFormat.WEBP_LOSSLESS -> stringResource(R.string.format_webp)
    OutputFormat.HEIC -> stringResource(R.string.format_heic)
    OutputFormat.AVIF -> stringResource(R.string.format_avif)
    OutputFormat.JXL -> stringResource(R.string.format_jxl)
}

@Composable
private fun backgroundLabel(choice: BackgroundChoice): String = when (choice) {
    BackgroundChoice.Automatic -> stringResource(R.string.background_auto)
    BackgroundChoice.White -> stringResource(R.string.background_white)
    BackgroundChoice.Black -> stringResource(R.string.background_black)
    BackgroundChoice.Transparent -> stringResource(R.string.background_transparent)
    is BackgroundChoice.CustomRgb -> stringResource(R.string.background_custom)
}


