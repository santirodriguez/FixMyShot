package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.cajapersonal.fixmyshot.R

/** Only used for fully published, current-draft-matching export batches.
 * Explicit Edit another image / Continue editing actions review results through the existing journal path.
 * It cannot be shown for partial, uncertain or stale output.
 */
@Composable
internal fun QuickSavedResults(count: Int, total: Int, enabled: Boolean, nextImageEnabled: Boolean,
    onView: () -> Unit, onShare: () -> Unit, onNextImage: () -> Unit,
    onDetails: () -> Unit, onContinueEditing: () -> Unit, onDismiss: () -> Unit) {
    require(total > 0 && count == total)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
            color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.quick_saved_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDetails) { Text(stringResource(R.string.quick_result_details)) }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.quick_saved_count, count, total),
                        style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.quick_saved_hint), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onNextImage, enabled = enabled && nextImageEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.quick_edit_another))
                    }
                    Text(stringResource(R.string.quick_edit_another_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    OutlinedButton(onClick = onView, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.quick_view_saved))
                    }
                    OutlinedButton(onClick = onShare, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.quick_share_saved))
                    }
                    TextButton(onClick = onContinueEditing, enabled = enabled) {
                        Text(stringResource(R.string.quick_keep_editing))
                    }
                }
            }
        }
    }
}
