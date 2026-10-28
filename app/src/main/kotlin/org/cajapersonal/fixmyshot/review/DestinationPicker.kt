package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.*

private data class DestinationGroup(val name: String, val ids: List<String>)
private val destinationGroups = listOf(
    DestinationGroup("Google Play", listOf(DestinationCatalog.PLAY_PORTRAIT, DestinationCatalog.PLAY_PHONE)),
    DestinationGroup("Instagram", listOf(DestinationCatalog.INSTAGRAM_FEED,
        DestinationCatalog.INSTAGRAM_STORY, DestinationCatalog.INSTAGRAM_SQUARE)),
    DestinationGroup("TikTok", listOf(DestinationCatalog.TIKTOK_PHOTO)),
    DestinationGroup("WhatsApp", listOf(DestinationCatalog.WHATSAPP_STATUS, DestinationCatalog.WHATSAPP_CHAT)),
    DestinationGroup("Facebook", listOf(DestinationCatalog.FACEBOOK_FEED,
        DestinationCatalog.FACEBOOK_SQUARE)),
)

/** A surface is a stable output contract, not just an application logo or an aspect ratio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DestinationPicker(destination: DestinationChoice, enabled: Boolean,
    onSelect: (DestinationChoice) -> Unit, onCustomize: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val id = (destination as? DestinationChoice.Preset)?.id
    val initialGroup = destinationGroups.firstOrNull { id in it.ids }?.name ?: "custom"
    var group by rememberSaveable(destination) { mutableStateOf(initialGroup) }
    val chooseLabel = stringResource(R.string.quick_choose_destination)
    Surface(onClick = { open = true }, enabled = enabled, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(min = 64.dp)
            .semantics { contentDescription = chooseLabel }) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ux_destination), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text(destinationTitle(destination), style = MaterialTheme.typography.titleSmall)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(destinationRatio(destination), Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text("\u2304", style = MaterialTheme.typography.titleLarge)
        }
    }
    if (open) ModalBottomSheet(onDismissRequest = { open = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Text(chooseLabel, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.ux_destination_hint), Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val panelHeight = LocalConfiguration.current.screenHeightDp.dp * .72f
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = panelHeight),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(destinationGroups, key = { it.name }) { platform ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column {
                        TextButton(onClick = { group = if (group == platform.name) "" else platform.name },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(platform.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(if (group == platform.name) "\u2212" else "+")
                        }
                        if (group == platform.name) {
                            platform.ids.forEach { option ->
                                DestinationOption(option, id == option, enabled) {
                                    onSelect(DestinationChoice.Preset(option)); open = false
                                }
                            }
                            Text(stringResource(when (platform.name) {
                                "Google Play" -> R.string.ux_play_note
                                "TikTok" -> R.string.ux_tiktok_note
                                "WhatsApp" -> R.string.ux_whatsapp_note
                                else -> R.string.ux_social_note
                            }), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { group = if (group == "custom") "" else "custom" },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.destination_custom), modifier = Modifier.weight(1f))
                    Text(if (group == "custom") "\u2212" else "+")
                }
                if (group == "custom") {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { open = false; onCustomize() }, enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.quick_customize))
                        }
                        // Previously saved generic IDs remain selectable without changing their meaning.
                        listOf(DestinationCatalog.SOCIAL_PORTRAIT, DestinationCatalog.SOCIAL_STORY,
                            DestinationCatalog.SOCIAL_SQUARE).forEach { option ->
                            DestinationOption(option, id == option, enabled) {
                                onSelect(DestinationChoice.Preset(option)); open = false
                            }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { open = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.quick_done))
                }
            }
        }
    }
}

@Composable
private fun DestinationOption(id: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(quickTemplateLabel(id))
    Surface(onClick = onClick, enabled = enabled,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).heightIn(min = 48.dp)
            .semantics { selected = active; contentDescription = label }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            if (active) Text("\u2713", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun destinationTitle(choice: DestinationChoice): String = when (choice) {
    is DestinationChoice.Custom -> stringResource(R.string.destination_custom) + " ${choice.ratio.p}:${choice.ratio.q}"
    is DestinationChoice.Preset -> {
        val label = stringResource(quickTemplateLabel(choice.id))
        val platform = destinationGroups.firstOrNull { choice.id in it.ids }?.name
        if (platform == null || platform == "Google Play") label else "$platform · $label"
    }
}

@Composable
private fun destinationRatio(choice: DestinationChoice): String {
    val ratio = DestinationCatalog.resolve(choice).ratio
    return ratio?.let { "${it.p}:${it.q}" } ?: stringResource(
        if ((choice as? DestinationChoice.Preset)?.id == DestinationCatalog.WHATSAPP_CHAT)
            R.string.ux_source_ratio else R.string.ux_flexible_ratio)
}
