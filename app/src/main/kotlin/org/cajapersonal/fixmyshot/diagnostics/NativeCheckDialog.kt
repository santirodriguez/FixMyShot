package org.cajapersonal.fixmyshot.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R

@Composable
internal fun NativeCheckDialog(
    running: Boolean, stopping: Boolean, canStart: Boolean,
    samples: List<NativeCheckSample>, report: NativeCheckReport?, shareError: Boolean,
    onStart: () -> Unit, onCancel: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.native_check_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.native_check_body))
                val heading = when {
                    stopping -> stringResource(R.string.native_check_stopping)
                    running -> stringResource(R.string.native_check_running, samples.size, 6)
                    report?.failed == true -> stringResource(R.string.native_check_failed)
                    report?.cancelled == true -> stringResource(R.string.native_check_cancelled)
                    report != null -> stringResource(R.string.native_check_complete)
                    else -> null
                }
                if (heading != null) Text(heading, modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                })
                for (size in NativeCodecDiagnostic.SIZES) for (format in NativeCodecDiagnostic.FORMATS) {
                    val sample = samples.firstOrNull { it.size == size && it.format == format }
                    Text(stringResource(R.string.native_check_result, format.name, size.width, size.height),
                        style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(when {
                        sample == null -> R.string.native_check_not_run
                        sample.failure != null -> R.string.native_check_unavailable
                        else -> R.string.native_check_encoded
                    }))
                    sample?.metrics?.let {
                        Text(stringResource(if (it.meetsNumericTargets) R.string.native_check_target_met
                            else R.string.native_check_target_missed), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!running && !canStart) Text(stringResource(R.string.native_check_disabled))
                if (report != null && !running) {
                    Text(stringResource(R.string.native_check_share_note), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onShare) { Text(stringResource(R.string.native_check_share)) }
                }
                if (shareError) Text(stringResource(R.string.native_check_share_error),
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            if (running) TextButton(enabled = !stopping, onClick = onCancel) {
                Text(stringResource(R.string.native_check_cancel))
            } else TextButton(enabled = canStart, onClick = onStart) {
                Text(stringResource(R.string.native_check_start))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } },
    )
}
