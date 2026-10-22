package org.cajapersonal.fixmyshot.review

import org.cajapersonal.fixmyshot.export.BatchCheckpoint
import org.cajapersonal.fixmyshot.export.BatchItemCheckpoint
import org.cajapersonal.fixmyshot.export.BatchItemPhase

/** User-visible session status. Never embeds original URIs or image bytes. */
internal enum class ExportUiPhase {
    IDLE, CHOOSING_FOLDER, RUNNING, COMPLETED, BLOCKED, NEEDS_REVIEW, CANCELLED, ERROR
}

internal data class ReviewExportState(
    val phase: ExportUiPhase = ExportUiPhase.IDLE,
    val items: List<BatchItemCheckpoint> = emptyList(),
    val blocked: Int = 0,
    val cancelRequested: Boolean = false,
) {
    val total: Int get() = items.size
    val published: Int get() = items.count { it.phase == BatchItemPhase.PUBLISHED }
    val failed: Int get() = items.count { it.phase == BatchItemPhase.FAILED }
    val cancelled: Int get() = items.count { it.phase == BatchItemPhase.CANCELLED }
    val needsReview: Int get() = items.count { it.phase == BatchItemPhase.NEEDS_REVIEW }
    val processed: Int get() = published + failed + cancelled + needsReview
    val isLocked: Boolean get() =
        phase == ExportUiPhase.CHOOSING_FOLDER || phase == ExportUiPhase.RUNNING

    companion object {
        fun fromCheckpoint(phase: ExportUiPhase, snapshot: BatchCheckpoint): ReviewExportState =
            ReviewExportState(phase, snapshot.items.toList())
    }
}
