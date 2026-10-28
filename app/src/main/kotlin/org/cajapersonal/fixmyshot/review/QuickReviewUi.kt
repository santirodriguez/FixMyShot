package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.disabled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.*

/** The empty state has one product action, not an extra onboarding step. */
@Composable
internal fun BrandStart(enabled: Boolean, onChoose: () -> Unit, canUndo: Boolean, onUndo: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        colors.primaryContainer.copy(alpha = .32f), colors.background, colors.background)))) {
        val wide = maxWidth > maxHeight
        val art: @Composable () -> Unit = {
            Image(painterResource(R.drawable.brand_icon), contentDescription = null,
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).aspectRatio(1f),
                contentScale = ContentScale.Fit)
        }
        val copy: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Image(painterResource(R.drawable.brand_header), contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp).aspectRatio(3f), contentScale = ContentScale.Fit)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center)
                Text(stringResource(R.string.quick_start_detail), color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                Button(onClick = onChoose, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.choose_images), style = MaterialTheme.typography.titleMedium)
                }
                if (canUndo) TextButton(onClick = onUndo, enabled = enabled) { Text(stringResource(R.string.undo_last)) }
                Text(stringResource(R.string.quick_share_hint), style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center, color = colors.onSurfaceVariant)
                Text(stringResource(R.string.quick_local_promise), style = MaterialTheme.typography.labelMedium,
                    color = colors.primary, textAlign = TextAlign.Center)
            }
        }
        if (wide) Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.weight(.8f)) { art() }
            Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) { copy() }
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.widthIn(max = 260.dp)) { art() }
            copy()
        }
    }
}

@Composable
internal fun QuickTemplateCards(destination: DestinationChoice, enabled: Boolean,
    onSelect: (DestinationChoice) -> Unit) {
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)) {
        items(QuickTemplates.ids, key = { it }) { id ->
            val active = destination == DestinationChoice.Preset(id)
            val label = stringResource(quickTemplateLabel(id))
            val ratio = DestinationCatalog.resolve(DestinationChoice.Preset(id)).ratio
            Surface(onClick = { onSelect(DestinationChoice.Preset(id)) }, enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(if (active) 2.dp else 1.dp,
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.width(128.dp).heightIn(min = 72.dp).semantics {
                    selected = active; contentDescription = label
                }) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val accent = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Canvas(Modifier.size(22.dp, 38.dp)) {
                        val aspect = ratio?.let { it.p.toFloat() / it.q } ?: .5f
                        val w = minOf(size.width, size.height * aspect)
                        val h = w / aspect
                        val origin = Offset((size.width - w) / 2, (size.height - h) / 2)
                        drawRect(accent.copy(alpha = .12f), origin, Size(w, h))
                        drawRect(accent, origin, Size(w, h), style = Stroke(1.5.dp.toPx()))
                    }
                    Text(label, style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

internal fun quickTemplateLabel(id: String): Int = when (id) {
    DestinationCatalog.PLAY_PORTRAIT -> R.string.quick_play_portrait
    DestinationCatalog.PLAY_PHONE -> R.string.quick_play_flexible
    DestinationCatalog.SOCIAL_PORTRAIT -> R.string.quick_portrait
    DestinationCatalog.SOCIAL_STORY -> R.string.quick_story
    else -> R.string.quick_square
}

@Composable
internal fun QuickFramingRow(fit: FitMode, enabled: Boolean, onFit: (FitMode) -> Unit,
    modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = fit == FitMode.CONTAIN, onClick = { onFit(FitMode.CONTAIN) }, enabled = enabled,
            label = { Text(stringResource(R.string.quick_keep_all)) }, modifier = Modifier.weight(1f))
        FilterChip(selected = fit == FitMode.CROP, onClick = { onFit(FitMode.CROP) }, enabled = enabled,
            label = { Text(stringResource(R.string.quick_fill_crop)) }, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun QuickBackgroundRow(choice: BackgroundChoice, enabled: Boolean, onChange: (BackgroundChoice) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(BackgroundChoice.Automatic to R.string.preview_background_auto,
            BackgroundChoice.White to R.string.preview_background_white,
            BackgroundChoice.Black to R.string.preview_background_black).forEach { (value, label) ->
            FilterChip(selected = choice == value, onClick = { onChange(value) }, enabled = enabled,
                label = { Text(stringResource(label)) }, modifier = Modifier.weight(1f))
        }
    }
}

/** Batch selection is compact; it never steals the touch canvas or changes another image's settings. */
@Composable
internal fun QuickBatchStrip(draft: DraftSession, plans: List<PreviewPlan>, selectedId: String?,
    thumbnails: Map<String, Bitmap>, enabled: Boolean, onSelect: (String) -> Unit, onRemove: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val current = draft.images.indexOfFirst { it.id == selectedId } + 1
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.weight(1f)) {
            Text(stringResource(if (expanded) R.string.quick_hide_batch else R.string.quick_show_batch,
                current, draft.images.size))
        }
        TextButton(onClick = onRemove, enabled = enabled) { Text(stringResource(R.string.quick_remove_image)) }
    }
    if (expanded) LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(draft.images, key = { _, image -> image.id }) { index, image ->
            val warning = plans.firstOrNull { it.itemId == image.id }?.requiresReview == true
            val label = stringResource(R.string.thumbnail_description, index + 1, draft.images.size)
            Surface(onClick = { onSelect(image.id) }, enabled = enabled, shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (image.id == selectedId) 2.dp else 1.dp,
                    if (warning) MaterialTheme.colorScheme.error else if (image.id == selectedId)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.width(62.dp).semantics { selected = image.id == selectedId; contentDescription = label }) {
                Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    thumbnails[image.id]?.takeUnless { it.isRecycled }?.let {
                        Image(it.asImageBitmap(), null, Modifier.size(48.dp), contentScale = ContentScale.Fit)
                    }
                    Text("${index + 1}" + if (warning) " !" else if (image.overrides.hasCustomValues) " *" else "",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** Holding Original is inspection only; the accessible click action offers the same comparison. */
@Composable
internal fun CompareOriginalButton(comparing: Boolean, enabled: Boolean, onCompare: (Boolean) -> Unit) {
    val latest by rememberUpdatedState(onCompare)
    val label = stringResource(if (comparing) R.string.quick_result else R.string.quick_original)
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.heightIn(min = 48.dp).semantics {
            contentDescription = label; role = Role.Button
            if (enabled) onClick { latest(!comparing); true } else disabled()
        }.onKeyEvent {
            if (enabled && it.key in listOf(Key.Enter, Key.Spacebar, Key.DirectionCenter)) {
                latest(it.type == KeyEventType.KeyDown); true
            } else false
        }.focusable(enabled).pointerInput(enabled) {
            if (enabled) detectTapGestures(onPress = {
                latest(true)
                try { tryAwaitRelease() } finally { latest(false) }
            })
        }) { Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        } }
}

/** Always fit the true output; the automatic panel no longer consumes half the screen. */
@Composable
internal fun QuickWorkspace(modifier: Modifier, stage: @Composable () -> Unit, tools: @Composable () -> Unit) {
    BoxWithConstraints(modifier) {
        val toolsHeightLimit = maxHeight * .46f
        if (maxWidth > maxHeight) Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1.5f).fillMaxHeight()) { stage() }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { tools() }
        } else Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { stage() }
            Column(Modifier.fillMaxWidth().heightIn(max = toolsHeightLimit).verticalScroll(rememberScrollState())) { tools() }
        }
    }
}
