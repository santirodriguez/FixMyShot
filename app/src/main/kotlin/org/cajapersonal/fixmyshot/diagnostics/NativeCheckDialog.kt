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

/** Separate opt-in narrow experiment; never reruns the accepted six-case diagnostic. */
@Composable
internal fun OddAvifCheckDialog(
    running: Boolean, stopping: Boolean, canStart: Boolean, progress: Int,
    report: OddAvifReport?, shareError: Boolean,
    sampleRunning: Boolean, sampleStopping: Boolean,
    sample: AvifByteSample?, sampleShareError: Boolean,
    onStart: () -> Unit, onCancel: () -> Unit, onShare: () -> Unit,
    onCaptureSample: () -> Unit, onCancelSample: () -> Unit,
    onShareSample: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.odd_check_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.odd_check_body))
                val heading = when {
                    stopping -> stringResource(R.string.native_check_stopping)
                    running -> stringResource(R.string.odd_check_running, progress)
                    report?.failed == true -> stringResource(R.string.native_check_failed)
                    report?.cancelled == true -> stringResource(R.string.native_check_cancelled)
                    report != null -> stringResource(R.string.odd_check_complete)
                    else -> null
                }
                if (heading != null) Text(heading, modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                })
                for (mode in OddAvifDiagnostic.MODES) {
                    val grid = stringResource(if (mode.grid) R.string.odd_check_grid_on else R.string.odd_check_grid_off)
                    Text(stringResource(R.string.odd_check_mode, mode.width, mode.height, grid),
                        style = MaterialTheme.typography.titleSmall)
                    val sample = report?.samples?.firstOrNull { it.mode == mode }
                    Text(stringResource(when {
                        sample == null -> R.string.native_check_not_run
                        sample.reason != null -> R.string.native_check_unavailable
                        else -> R.string.odd_check_measured
                    }))
                }
                if (report != null && !running) {
                    Text(stringResource(R.string.odd_check_share_note), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onShare) { Text(stringResource(R.string.odd_check_share)) }
                }
                Text(stringResource(R.string.avif_sample_note),
                    style = MaterialTheme.typography.bodySmall)
                if (sampleRunning) Text(stringResource(if (sampleStopping)
                    R.string.native_check_stopping else R.string.avif_sample_running),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (sample != null && !sampleRunning) {
                    Text(stringResource(when {
                        sample.ready -> R.string.avif_sample_ready
                        sample.cancelled -> R.string.native_check_cancelled
                        else -> R.string.avif_sample_failed
                    }))
                    if (sample.ready) TextButton(onClick = onShareSample) {
                        Text(stringResource(R.string.avif_sample_share))
                    }
                }
                if (!sampleRunning && !running) TextButton(enabled = canStart,
                    onClick = onCaptureSample) {
                    Text(stringResource(R.string.avif_sample_start))
                }
                if (shareError || sampleShareError)
                    Text(stringResource(R.string.native_check_share_error),
                        color = MaterialTheme.colorScheme.error)
                if (!running && !sampleRunning && !canStart)
                    Text(stringResource(R.string.native_check_disabled))
            }
        },
        confirmButton = {
            if (sampleRunning) TextButton(enabled = !sampleStopping,
                onClick = onCancelSample) {
                Text(stringResource(R.string.native_check_cancel))
            } else if (running) TextButton(enabled = !stopping, onClick = onCancel) {
                Text(stringResource(R.string.native_check_cancel))
            } else TextButton(enabled = canStart, onClick = onStart) {
                Text(stringResource(R.string.odd_check_start))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } },
    )
}

/** Independent single HEIC sample action; never starts the six-case device check. */
@Composable
internal fun HeicByteSampleDialog(
    running: Boolean, stopping: Boolean, canStart: Boolean,
    sample: AvifByteSample?, shareError: Boolean,
    onStart: () -> Unit, onCancel: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.heic_sample_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.heic_sample_note))
                if (running) Text(stringResource(if (stopping) R.string.native_check_stopping
                    else R.string.heic_sample_running), modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                })
                if (sample != null && !running) {
                    Text(stringResource(if (sample.ready) R.string.heic_sample_ready
                        else R.string.heic_sample_failed))
                    if (sample.ready) TextButton(onClick = onShare) {
                        Text(stringResource(R.string.heic_sample_share))
                    }
                }
                if (shareError) Text(stringResource(R.string.native_check_share_error),
                    color = MaterialTheme.colorScheme.error)
                if (!running && !canStart) Text(stringResource(R.string.native_check_disabled))
            }
        },
        confirmButton = {
            if (running) TextButton(enabled = !stopping, onClick = onCancel) {
                Text(stringResource(R.string.native_check_cancel))
            } else TextButton(enabled = canStart, onClick = onStart) {
                Text(stringResource(R.string.heic_sample_start))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } },
    )
}
