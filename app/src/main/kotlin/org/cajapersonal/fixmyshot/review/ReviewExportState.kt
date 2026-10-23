package org.cajapersonal.fixmyshot.review

import org.cajapersonal.fixmyshot.export.BatchCheckpoint
import org.cajapersonal.fixmyshot.export.BatchItemCheckpoint
import org.cajapersonal.fixmyshot.export.BatchItemPhase

/** User-visible session status. Never embeds original URIs or image bytes. */
internal enum class ExportUiPhase {
    IDLE, CHECKING_HISTORY, CHOOSING_FOLDER, RUNNING, COMPLETED, BLOCKED, NEEDS_REVIEW, CANCELLED, PAUSED, ERROR
}

internal data class ReviewExportState(
    val phase: ExportUiPhase = ExportUiPhase.IDLE,
    val items: List<BatchItemCheckpoint> = emptyList(),
    val blocked: Int = 0,
    val cancelRequested: Boolean = false,
    val previousJobs: Int = 0,
    val canAcknowledge: Boolean = false,
    val retryableIds: Set<String> = emptySet(),
    val retryReady: Boolean = false,
    val canRestoreDraft: Boolean = false,
    val outputBusy: Boolean = false,
    val outputError: Boolean = false,
    val pauseRequested: Boolean = false,
) {
    val total: Int get() = items.size
    val published: Int get() = items.count { it.phase == BatchItemPhase.PUBLISHED }
    val failed: Int get() = items.count { it.phase == BatchItemPhase.FAILED }
    val cancelled: Int get() = items.count { it.phase == BatchItemPhase.CANCELLED }
    val needsReview: Int get() = items.count { it.phase == BatchItemPhase.NEEDS_REVIEW }
    val processed: Int get() = published + failed + cancelled + needsReview
    val isLocked: Boolean get() =
        phase == ExportUiPhase.CHECKING_HISTORY || phase == ExportUiPhase.CHOOSING_FOLDER ||
            phase == ExportUiPhase.RUNNING || canAcknowledge || outputBusy

    companion object {
        fun fromCheckpoint(phase: ExportUiPhase, snapshot: BatchCheckpoint): ReviewExportState =
            ReviewExportState(phase, snapshot.items.toList())
    }
}

