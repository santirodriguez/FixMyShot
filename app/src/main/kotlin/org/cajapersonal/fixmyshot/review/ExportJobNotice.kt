package org.cajapersonal.fixmyshot.review

import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cajapersonal.fixmyshot.R

@Composable
internal fun ExportJobNotice(state: ReviewExportState, onReviewed: () -> Unit) {
    if (state.previousJobs > 0) {
        Text(stringResource(R.string.export_history_pending, state.previousJobs))
    }
    if (state.canAcknowledge) {
        Text(stringResource(R.string.export_finish_review_notice))
        OutlinedButton(onClick = onReviewed, enabled = !state.outputBusy) {
            Text(stringResource(R.string.export_history_acknowledge))
        }
    }
}

