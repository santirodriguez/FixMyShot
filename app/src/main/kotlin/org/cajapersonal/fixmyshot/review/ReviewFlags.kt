package org.cajapersonal.fixmyshot.review

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.DraftReviewFlag

@Composable
internal fun reviewFlagLabel(flag: DraftReviewFlag): String = when (flag) {
    DraftReviewFlag.DESTINATION_REQUIRES_PNG -> stringResource(R.string.review_needs_png)
    DraftReviewFlag.DESTINATION_REQUIRES_OPAQUE -> stringResource(R.string.review_needs_opaque)
    DraftReviewFlag.CODEC_UNAVAILABLE, DraftReviewFlag.CODEC_RUNTIME_PROBE_REQUIRED -> stringResource(R.string.review_codec_unavailable)
    DraftReviewFlag.OVERSIZED_SOURCE, DraftReviewFlag.GEOMETRY_UNAVAILABLE -> stringResource(R.string.review_size_unavailable)
    DraftReviewFlag.DIMENSIONS_REQUIRE_REVIEW -> stringResource(R.string.review_dimensions)
    DraftReviewFlag.CROP_FOCUS_REPOSITIONED, DraftReviewFlag.EXACT_CROP_SAMPLING_NOT_READY -> stringResource(R.string.review_crop_pending)
}
