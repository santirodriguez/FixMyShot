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
import androidx.compose.runtime.Composable
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

@Composable
internal fun ReviewControls(
    draft: DraftSession,
    plan: PreviewPlan?,
    enabled: Boolean,
    onDestination: (DestinationChoice) -> Unit,
    onFormat: (OutputFormat, Boolean) -> Unit,
    onBackground: (BackgroundChoice, Boolean) -> Unit,
    onFit: (FitMode, Boolean) -> Unit = { _, _ -> },
    onSize: (OutputSizeChoice, Boolean) -> Unit = { _, _ -> },
    onMetadata: (MetadataPolicy, Boolean) -> Unit = { _, _ -> },
) {
    var chooseDestination by remember { mutableStateOf(false) }
    var chooseFormat by remember { mutableStateOf(false) }
    var chooseBackground by remember { mutableStateOf(false) }
    var selectedOnly by remember { mutableStateOf(false) }
    var ratioW by remember { mutableStateOf("9") }
    var ratioH by remember { mutableStateOf("16") }
    val effective = if (selectedOnly) plan?.settings ?: draft.batch else draft.batch
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                value = selectedOnly, enabled = enabled, role = Role.Checkbox,
                onValueChange = { selectedOnly = it }), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selectedOnly, enabled = enabled, onCheckedChange = null)
                Text(stringResource(R.string.selected_only))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val fit = effective.fit
                androidx.compose.material3.FilterChip(modifier = Modifier.weight(1f), selected = fit == FitMode.CONTAIN,
                    enabled = enabled, onClick = { onFit(FitMode.CONTAIN, selectedOnly) },
                    label = { Text(stringResource(R.string.preserve_content)) })
                androidx.compose.material3.FilterChip(modifier = Modifier.weight(1f), selected = fit == FitMode.CROP,
                    enabled = enabled, onClick = { onFit(FitMode.CROP, selectedOnly) },
                    label = { Text(stringResource(R.string.crop_mode)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(enabled = enabled, onClick = { chooseFormat = true }) {
                        Text(formatLabel(effective.format))
                    }
                    DropdownMenu(expanded = chooseFormat, onDismissRequest = { chooseFormat = false }) {
                        OutputFormat.entries.filter { it != OutputFormat.JXL }.forEach { format ->
                            DropdownMenuItem(enabled = enabled, text = { Text(formatLabel(format)) },
                                onClick = { chooseFormat = false; onFormat(format, selectedOnly) })
                        }
                    }
                }
                Box {
                    OutlinedButton(enabled = enabled, onClick = { chooseBackground = true }) {
                        Text(backgroundLabel(effective.background))
                    }
                    DropdownMenu(expanded = chooseBackground, onDismissRequest = { chooseBackground = false }) {
                        listOf(BackgroundChoice.Automatic, BackgroundChoice.White, BackgroundChoice.Black,
                            BackgroundChoice.Transparent).forEach { choice ->
                            DropdownMenuItem(enabled = enabled, text = { Text(backgroundLabel(choice)) },
                                onClick = { chooseBackground = false; onBackground(choice, selectedOnly) })
                        }
                    }
                }
            }
            CustomBackgroundControl(enabled, effective.background) {
                onBackground(it, selectedOnly)
            }
            OutputSizeControls(effective.size, enabled) { onSize(it, selectedOnly) }
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                value = effective.metadata == MetadataPolicy.REMOVE_IDENTIFYING,
                enabled = enabled, role = Role.Checkbox, onValueChange = {
                        onMetadata(if (it) MetadataPolicy.REMOVE_IDENTIFYING else MetadataPolicy.PRESERVE_ALLOWLIST, selectedOnly)
                    }), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = effective.metadata == MetadataPolicy.REMOVE_IDENTIFYING,
                    enabled = enabled, onCheckedChange = null)
                Text(stringResource(R.string.metadata_remove_option))
            }
            Text(stringResource(if (effective.metadata == MetadataPolicy.REMOVE_IDENTIFYING)
                R.string.metadata_removed else R.string.metadata_preserve_warning),
                style = MaterialTheme.typography.bodySmall)
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
private fun formatLabel(format: OutputFormat): String = when (format) {
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


