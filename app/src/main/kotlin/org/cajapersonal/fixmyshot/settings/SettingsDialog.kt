package org.cajapersonal.fixmyshot.settings

import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
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
    onDismiss: () -> Unit,
) {
    val locale = AppLanguages.selected(LocalContext.current)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
                AppTheme.entries.forEach { value ->
                    val label = stringResource(when (value) {
                        AppTheme.SYSTEM -> R.string.follow_system
                        AppTheme.LIGHT -> R.string.theme_light
                        AppTheme.DARK -> R.string.theme_dark
                    })
                    Choice(label, theme == value, editable, { onTheme(value) })
                }
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
                Choice(stringResource(R.string.follow_system), locale.isEmpty(), editable,
                    { onLanguage("") })
                LanguageCatalog.supported.forEach { language ->
                    Choice(language.name, locale == language.tag, editable,
                        { onLanguage(language.tag) }, language.flag)
                }
                if (!editable) Text(stringResource(R.string.settings_wait))
                if (saveError) Text(stringResource(R.string.settings_save_error),
                    color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.help_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.help_body))
                Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.privacy_body))
                Text(stringResource(R.string.about_version, version))
                Text(stringResource(R.string.about_author))
                Text(stringResource(R.string.about_license))
                TextButton(onClick = { onLink("https://github.com/santirodriguez/FixMyShot") }) {
                    Text(stringResource(R.string.about_source))
                }
                TextButton(onClick = { onLink("https://github.com/santirodriguez/FixMyShot/blob/release/0.1.0/THIRD_PARTY_NOTICES") }) {
                    Text(stringResource(R.string.about_notices))
                }
                TextButton(onClick = { onLink("https://santiagorodriguez.com/") }) {
                    Text(stringResource(R.string.about_website))
                }
                TextButton(onClick = { onLink("https://santiagorodriguez.com/donate") }) {
                    Text(stringResource(R.string.about_donate))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } },
    )
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


