package org.cajapersonal.fixmyshot.review

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.cajapersonal.fixmyshot.R

@Composable
internal fun ExportJobNotice(
    state: ReviewExportState,
    onReviewed: () -> Unit,
    finishSession: Boolean = false,
    enabled: Boolean = true,
) {
    val actionEnabled = enabled && state.canAcknowledge && !state.outputBusy
    // Confirmation belongs to these exact results, never to a later check or busy operation.
    var confirming by remember(state, finishSession, enabled) { mutableStateOf(false) }
    val title = if (finishSession) R.string.quick_new_work else R.string.export_history_acknowledge
    val explanation = if (finishSession) R.string.export_finish_session_notice else R.string.export_finish_review_notice
    if (confirming && actionEnabled) AlertDialog(
        onDismissRequest = { confirming = false },
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(explanation)) },
        confirmButton = { TextButton(onClick = {
            confirming = false
            if (actionEnabled) onReviewed()
        }, enabled = actionEnabled) { Text(stringResource(title)) } },
        dismissButton = { TextButton(onClick = { confirming = false }) {
            Text(stringResource(R.string.export_keep_cancel))
        } },
    )
    if (state.previousJobs > 0) {
        Text(stringResource(R.string.export_history_pending, state.previousJobs))
    }
    if (state.canAcknowledge) {
        Text(stringResource(explanation))
        OutlinedButton(onClick = { confirming = true }, enabled = actionEnabled) {
            Text(stringResource(title))
        }
    }
}
