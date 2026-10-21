package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.DestinationCatalog
import org.cajapersonal.fixmyshot.core.DestinationChoice
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.core.PreviewPlan

@Composable
internal fun ReviewControls(
    draft: DraftSession,
    plan: PreviewPlan?,
    onDestination: (DestinationChoice) -> Unit,
    onFormat: (OutputFormat, Boolean) -> Unit,
    onBackground: (BackgroundChoice, Boolean) -> Unit,
) {
    var chooseDestination by remember { mutableStateOf(false) }
    var chooseFormat by remember { mutableStateOf(false) }
    var chooseBackground by remember { mutableStateOf(false) }
    var selectedOnly by remember { mutableStateOf(false) }
    var ratioW by remember { mutableStateOf("9") }
    var ratioH by remember { mutableStateOf("16") }
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.destination_heading), style = MaterialTheme.typography.titleMedium)
            Box {
                OutlinedButton(onClick = { chooseDestination = true }) {
                    Text(destinationLabel(draft.destination))
                }
                DropdownMenu(expanded = chooseDestination, onDismissRequest = { chooseDestination = false }) {
                    DestinationCatalog.presets.filter { it.id != DestinationCatalog.CUSTOM_RATIO }.forEach { preset ->
                        DropdownMenuItem(text = { Text(destinationLabel(DestinationChoice.Preset(preset.id))) },
                            onClick = { chooseDestination = false; onDestination(DestinationChoice.Preset(preset.id)) })
                    }
                }
            }
            Text(stringResource(R.string.custom_ratio_label), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(ratioW, onValueChange = { ratioW = it.take(5).filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.ratio_width)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(ratioH, onValueChange = { ratioH = it.take(5).filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.ratio_height)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f))
            }
            val numerator = ratioW.toIntOrNull()
            val denominator = ratioH.toIntOrNull()
            OutlinedButton(enabled = numerator != null && denominator != null && numerator in 1..10000 && denominator in 1..10000,
                onClick = { onDestination(DestinationChoice.Custom.of(requireNotNull(numerator), requireNotNull(denominator))) }) {
                Text(stringResource(R.string.apply_custom_ratio))
            }
            Text(stringResource(R.string.preserve_content), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selectedOnly, onCheckedChange = { selectedOnly = it })
                Text(stringResource(R.string.selected_only))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { chooseFormat = true }) {
                        Text(formatLabel(plan?.settings?.format ?: draft.batch.format))
                    }
                    DropdownMenu(expanded = chooseFormat, onDismissRequest = { chooseFormat = false }) {
                        OutputFormat.entries.forEach { format ->
                            DropdownMenuItem(text = { Text(formatLabel(format)) },
                                onClick = { chooseFormat = false; onFormat(format, selectedOnly) })
                        }
                    }
                }
                Box {
                    OutlinedButton(onClick = { chooseBackground = true }) {
                        Text(backgroundLabel(plan?.settings?.background ?: draft.batch.background))
                    }
                    DropdownMenu(expanded = chooseBackground, onDismissRequest = { chooseBackground = false }) {
                        listOf(BackgroundChoice.Automatic, BackgroundChoice.White, BackgroundChoice.Black,
                            BackgroundChoice.Transparent).forEach { choice ->
                            DropdownMenuItem(text = { Text(backgroundLabel(choice)) },
                                onClick = { chooseBackground = false; onBackground(choice, selectedOnly) })
                        }
                    }
                }
            }
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

