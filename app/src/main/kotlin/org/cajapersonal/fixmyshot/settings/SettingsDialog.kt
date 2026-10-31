package org.cajapersonal.fixmyshot.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R

internal object LanguageCatalog {
    data class Language(val tag: String, val name: String, val flag: Int)
    val supported = listOf(
        Language("en", "English", R.drawable.flag_us),
        Language("es", "Español", R.drawable.flag_ar),
        Language("ca", "Català", R.drawable.flag_ca),
    )
}

@Composable
internal fun SettingsDialog(
    theme: AppTheme,
    editable: Boolean,
    saveError: Boolean,
    version: String,
    onTheme: (AppTheme) -> Unit,
    onLanguage: (String) -> Unit,
    onLink: (String) -> Unit,
    onNativeCheck: () -> Unit,
    onOddAvifCheck: () -> Unit,
    onDismiss: () -> Unit,
    onHeicSample: () -> Unit = {},
) {
    val locale = AppLanguages.selected(LocalContext.current)
    // A single full-screen scroll owner avoids nested ModalBottomSheet drag and
    // verticalScroll competing at the top/bottom of a long Settings page.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSection(stringResource(R.string.settings_theme)) {
                    AppTheme.entries.forEach { value ->
                        val label = stringResource(when (value) {
                            AppTheme.SYSTEM -> R.string.follow_system
                            AppTheme.LIGHT -> R.string.theme_light
                            AppTheme.DARK -> R.string.theme_dark
                        })
                        Choice(label, theme == value, editable, { onTheme(value) })
                    }
                }
                SettingsSection(stringResource(R.string.settings_language)) {
                    Choice(stringResource(R.string.follow_system), locale.isEmpty(), editable, { onLanguage("") })
                    LanguageCatalog.supported.forEach { language ->
                        Choice(language.name, locale == language.tag, editable,
                            { onLanguage(language.tag) }, language.flag)
                    }
                }
                if (!editable) Text(stringResource(R.string.settings_wait))
                if (saveError) Text(stringResource(R.string.settings_save_error), color = MaterialTheme.colorScheme.error)
                SettingsDisclosure(stringResource(R.string.help_title)) {
                    Text(stringResource(R.string.help_body))
                    Text(stringResource(R.string.metadata_preserve_warning))
                    Text(stringResource(R.string.metadata_supported_tags), style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = editable, onClick = onHeicSample) { Text(stringResource(R.string.heic_sample_title)) }
                    TextButton(enabled = editable, onClick = onNativeCheck) { Text(stringResource(R.string.native_check_title)) }
                    TextButton(enabled = editable, onClick = onOddAvifCheck) { Text(stringResource(R.string.odd_check_title)) }
                }
                SettingsDisclosure(stringResource(R.string.privacy_title)) { Text(stringResource(R.string.privacy_body)) }
                SettingsDisclosure(stringResource(R.string.ux_about)) {
                    Text(stringResource(R.string.about_version, version))
                    Text(stringResource(R.string.about_author))
                    Text(stringResource(R.string.about_license))
                    TextButton(onClick = { onLink("https://github.com/santirodriguez/FixMyShot") }) {
                        Text(stringResource(R.string.about_source))
                    }
                    TextButton(onClick = { onLink("https://github.com/santirodriguez/FixMyShot/blob/release/0.1.0/THIRD_PARTY_NOTICES") }) {
                        Text(stringResource(R.string.about_notices))
                    }
                    TextButton(onClick = { onLink("https://santiagorodriguez.com/") }) { Text(stringResource(R.string.about_website)) }
                    TextButton(onClick = { onLink("https://santiagorodriguez.com/donate") }) { Text(stringResource(R.string.about_donate)) }
                }
            }
        }
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, enabled: Boolean,
    onClick: () -> Unit, flag: Int? = null) {
    // One labelled radio target; flags are decorative and never replace the language name.
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        if (flag != null) Image(painterResource(flag), contentDescription = null,
            modifier = Modifier.size(28.dp, 20.dp))
        Text(label)
    }
}


@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SettingsDisclosure(title: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(if (expanded) R.string.ux_expanded else R.string.ux_collapsed)
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth()) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                .semantics { stateDescription = state }) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(if (expanded) "\u2212" else "+")
            }
            if (expanded) Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}
