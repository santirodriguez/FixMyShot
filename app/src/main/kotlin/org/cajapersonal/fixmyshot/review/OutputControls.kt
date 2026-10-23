package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.OutputSizeChoice
import org.cajapersonal.fixmyshot.core.PixelSize
import java.util.Locale

@Composable
internal fun CustomBackgroundControl(enabled: Boolean, background: BackgroundChoice,
    onApply: (BackgroundChoice) -> Unit) = key(background) {
    val rgb = (background as? BackgroundChoice.CustomRgb)?.rgb
    var hex by rememberSaveable { mutableStateOf(String.format(Locale.ROOT, "%06X", rgb ?: 0xFFFFFF)) }
    val color = hex.takeIf { it.length == 6 }?.toIntOrNull(16)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = hex, onValueChange = { value ->
            hex = value.removePrefix("#").filter { it in "0123456789abcdefABCDEF" }.take(6).uppercase(Locale.ROOT)
        }, enabled = enabled, singleLine = true,
            label = { Text(stringResource(R.string.background_hex)) }, modifier = Modifier.weight(1f))
        if (color != null) Box(Modifier.size(32.dp).background(Color(0xFF000000.toInt() or color)))
    }
    OutlinedButton(enabled = enabled && color != null,
        onClick = { onApply(BackgroundChoice.CustomRgb(requireNotNull(color))) }) {
        Text(stringResource(R.string.background_apply_custom))
    }
}

/** Editing these fields never changes the effective plan until the explicit Apply action. */
@Composable
internal fun OutputSizeControls(size: OutputSizeChoice, enabled: Boolean,
    onApply: (OutputSizeChoice) -> Unit) = key(size) {
    val exact = size as? OutputSizeChoice.Exact
    var width by rememberSaveable { mutableStateOf(exact?.canvas?.width?.toString() ?: "1080") }
    var height by rememberSaveable { mutableStateOf(exact?.canvas?.height?.toString() ?: "1920") }
    var allowEnlargement by rememberSaveable { mutableStateOf(exact?.allowEnlargement ?: false) }
    val w = width.toIntOrNull()
    val h = height.toIntOrNull()
    val valid = w != null && h != null && w > 0 && h > 0 && w.toLong() * h <= 24_000_000L
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.size_heading), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(if (exact == null) R.string.size_preserve else R.string.size_exact_selected))
        OutlinedButton(enabled = enabled && exact != null, onClick = { onApply(OutputSizeChoice.PreservePixels) }) {
            Text(stringResource(R.string.size_preserve))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(width, onValueChange = { width = it.filter(Char::isDigit).take(6) },
                enabled = enabled, label = { Text(stringResource(R.string.size_width)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(height, onValueChange = { height = it.filter(Char::isDigit).take(6) },
                enabled = enabled, label = { Text(stringResource(R.string.size_height)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
            value = allowEnlargement, enabled = enabled, role = Role.Checkbox,
            onValueChange = { allowEnlargement = it }), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = allowEnlargement, enabled = enabled, onCheckedChange = null)
            Text(stringResource(R.string.size_allow_enlargement))
        }
        Text(stringResource(R.string.size_exact_notice), style = MaterialTheme.typography.bodySmall)
        if (!valid) Text(stringResource(R.string.size_invalid), color = MaterialTheme.colorScheme.error)
        OutlinedButton(enabled = enabled && valid, onClick = {
            onApply(OutputSizeChoice.Exact(PixelSize(requireNotNull(w), requireNotNull(h)), allowEnlargement))
        }) { Text(stringResource(R.string.size_apply_exact)) }
    }
}
