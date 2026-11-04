package org.cajapersonal.fixmyshot.review

import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.export.BatchItemCheckpoint
import org.cajapersonal.fixmyshot.export.BatchItemPhase
import org.cajapersonal.fixmyshot.core.PrivatePreparationBlock

/** Closed localized vocabulary: journal reason strings are never displayed as product text. */
internal object ResultFeedback {
    fun importReason(reason: ReviewSources.Failure): Int = when (reason) {
        ReviewSources.Failure.UNREADABLE -> R.string.import_reason_unreadable
        ReviewSources.Failure.UNSUPPORTED -> R.string.import_reason_unsupported
        ReviewSources.Failure.TOO_LARGE -> R.string.import_reason_large
        ReviewSources.Failure.LOW_STORAGE -> R.string.import_reason_storage
        ReviewSources.Failure.COLOR_UNSUPPORTED -> R.string.import_reason_color
        ReviewSources.Failure.MALFORMED -> R.string.import_reason_malformed
        ReviewSources.Failure.ANIMATED -> R.string.import_reason_animated
        ReviewSources.Failure.HIGH_PRECISION -> R.string.import_reason_precision
        ReviewSources.Failure.HDR_UNSUPPORTED -> R.string.import_reason_hdr
    }

    fun preflightReason(reason: PrivatePreparationBlock): Int = when (reason) {
        PrivatePreparationBlock.SOURCE_FINGERPRINT_MISSING -> R.string.result_source_changed
        PrivatePreparationBlock.REVIEW_CONFLICT -> R.string.result_review_settings
        PrivatePreparationBlock.FORMAT_NOT_READY, PrivatePreparationBlock.CODEC_UNAVAILABLE -> R.string.result_codec_unavailable
        PrivatePreparationBlock.CONTAIN_RENDER_ONLY -> R.string.review_size_unavailable
        PrivatePreparationBlock.MEMORY_LIMIT -> R.string.result_resource_limit
    }

    fun exportReason(item: BatchItemCheckpoint): Int? {
        // Publication uncertainty owns the action, even when the underlying code resembles
        // a retryable encoder/provider fault. This is presentation, never retry authority.
        if (item.phase == BatchItemPhase.NEEDS_REVIEW) return when (item.reason) {
            "OUTPUT_CREATION_UNCERTAIN" -> R.string.result_creation_uncertain
            "OUTPUT_PRESENT_UNCERTAIN" -> R.string.export_output_present_uncertain
            "OUTPUT_MISSING" -> R.string.export_output_missing
            "OUTPUT_CHANGED" -> R.string.export_output_changed
            "OUTPUT_PENDING" -> R.string.export_output_pending
            "OUTPUT_UNVERIFIABLE" -> R.string.export_output_unverifiable
            else -> R.string.result_check_output
        }
        if (item.phase == BatchItemPhase.CANCELLED) return when (item.reason) {
            "PAUSED" -> R.string.result_paused
            "INTERRUPTED" -> R.string.result_interrupted
            else -> R.string.result_cancelled
        }
        if (item.phase != BatchItemPhase.FAILED) return null
        return when (item.reason) {
            "UNFINISHED_REMOVED" -> R.string.export_pending_removed
            "STALE_PLAN" -> R.string.result_review_settings
            "SOURCE_CHANGED" -> R.string.result_source_changed
            "SOURCE_UNREADABLE" -> R.string.result_source_changed
            "UNSUPPORTED_SOURCE" -> R.string.import_reason_unsupported
            "RESOURCE_LIMIT" -> R.string.result_resource_limit
            "CODEC_UNAVAILABLE", "UNSUPPORTED_FORMAT" -> R.string.result_codec_unavailable
            "ENCODER_TIMEOUT" -> R.string.result_encoder_timeout
            "ENCODER_IO", "PROVIDER_ERROR" -> R.string.result_write_error
            "LOW_STORAGE" -> R.string.result_storage_shortage
            "PRIVATE_STORAGE_LOW" -> R.string.result_private_storage_shortage
            "METADATA_UNAVAILABLE" -> R.string.result_metadata_error
            "INVALID_CANDIDATE" -> R.string.result_invalid_candidate
            "JOURNAL_ERROR", "PREVIOUS_JOB_UNRESOLVED" -> R.string.result_check_output
            else -> R.string.result_encoder_error
        }
    }
}
