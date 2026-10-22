package org.cajapersonal.fixmyshot.core

/**
 * Immutable, source-bound preflight for future app-private PNG/WebP candidates.
 * This is NOT permission to write to MediaStore or a claim that encoding succeeds.
 * The execution stage must rehash the staged file, normalize its original pixels,
 * resolve the automatic background exactly once and recheck available memory.
 */
enum class PrivatePreparationBlock {
    SOURCE_FINGERPRINT_MISSING,
    REVIEW_CONFLICT,
    FORMAT_NOT_READY,
    CONTAIN_RENDER_ONLY,
    CODEC_UNAVAILABLE,
    MEMORY_LIMIT,
}

data class PrivateOutputRecipe(
    val sourceId: String,
    val sourceSha256: String,
    val orientedSource: PixelSize,
    val itemRevision: Long,
    val sessionRevision: Long,
    val catalogVersion: Int,
    val destinationId: String,
    val canvas: PixelSize,
    val placement: PixelPlacement,
    val background: BackgroundChoice,
    val codecRoute: CodecRoute,
    val request: EncodeRequest,
    val estimatedWorkingSetBytes: Long,
    val crop: SourceRectangle? = null,
) {
    val needsBackgroundResolution: Boolean get() = background == BackgroundChoice.Automatic
}

data class PrivateItemPreflight(
    val itemId: String,
    val recipe: PrivateOutputRecipe?,
    val blocks: Set<PrivatePreparationBlock>,
) {
    init { require((recipe == null) == blocks.isNotEmpty()) }
}

data class PrivateBatchPreflight(val revision: Long, val items: List<PrivateItemPreflight>) {
    val candidates: List<PrivateOutputRecipe> get() = items.mapNotNull { it.recipe }
    val blockedCount: Int get() = items.count { it.recipe == null }
}

object ExportPreparation {
    private const val MIB = 1_048_576L

    fun inspect(draft: DraftSession, apiLevel: Int, processHeapLimitBytes: Long): PrivateBatchPreflight {
        require(processHeapLimitBytes > 0L)
        val plans = draft.plans(apiLevel)
        val items = draft.images.zip(plans).map { (image, preview) ->
            check(image.id == preview.itemId) { "Draft image/plan order changed" }
            val blocks = linkedSetOf<PrivatePreparationBlock>()
            val hash = image.contentSha256
            if (hash == null) blocks += PrivatePreparationBlock.SOURCE_FINGERPRINT_MISSING
            if (preview.reviewFlags.isNotEmpty()) blocks += PrivatePreparationBlock.REVIEW_CONFLICT
            if (preview.settings.format !in setOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
                blocks += PrivatePreparationBlock.FORMAT_NOT_READY
            }
            val geometry = preview.geometry
            val canvas = when (geometry) {
                is PlannedGeometry.Contained -> geometry.value.canvas
                is PlannedGeometry.Cropped -> geometry.value.output
                null -> null
            }
            if (canvas == null) blocks += PrivatePreparationBlock.CONTAIN_RENDER_ONLY

            var route: CodecRoute? = null
            var request: EncodeRequest? = null
            var estimate = 0L
            if (canvas != null) {
                request = EncodeRequest(
                    size = canvas,
                    format = preview.settings.format,
                    hasAlpha = preview.settings.background == BackgroundChoice.Transparent,
                    metadataPolicy = preview.settings.metadata,
                )
                when (val eligibility = CodecCapabilities.assess(apiLevel, request)) {
                    is CodecEligibility.Unavailable -> blocks += PrivatePreparationBlock.CODEC_UNAVAILABLE
                    is CodecEligibility.ProbeRequired -> route = eligibility.route
                }
                if (route !in setOf(CodecRoute.ANDROID_PNG, CodecRoute.ANDROID_WEBP_LOSSLESS, CodecRoute.LIBWEBP)) {
                    blocks += PrivatePreparationBlock.FORMAT_NOT_READY
                }
                try {
                    estimate = Math.addExact(
                        CodecMemoryBudget.estimatedBytes(request),
                        Math.addExact(
                            Math.multiplyExact(image.orientedSize.pixels, 4L),
                            Math.multiplyExact(canvas.pixels, 4L),
                        ),
                    )
                    val limit = minOf(256L * MIB, processHeapLimitBytes / 2L)
                    if (estimate > limit || !CodecMemoryBudget.permits(request, processHeapLimitBytes)) {
                        blocks += PrivatePreparationBlock.MEMORY_LIMIT
                    }
                } catch (_: IllegalArgumentException) {
                    blocks += PrivatePreparationBlock.MEMORY_LIMIT
                } catch (_: ArithmeticException) {
                    blocks += PrivatePreparationBlock.MEMORY_LIMIT
                }
            }

            val recipe = if (blocks.isEmpty()) {
                val output = requireNotNull(canvas)
                val contained = (geometry as? PlannedGeometry.Contained)?.value
                val crop = (geometry as? PlannedGeometry.Cropped)?.value
                PrivateOutputRecipe(
                    sourceId = image.id,
                    sourceSha256 = requireNotNull(hash),
                    orientedSource = image.orientedSize,
                    itemRevision = image.revision,
                    sessionRevision = draft.revision,
                    catalogVersion = preview.catalogVersion,
                    destinationId = preview.destination.preset.id,
                    canvas = output,
                    placement = contained?.placement ?: PixelPlacement(0, 0, output.width, output.height),
                    crop = crop?.visibleSource,
                    background = preview.settings.background,
                    codecRoute = requireNotNull(route),
                    request = requireNotNull(request),
                    estimatedWorkingSetBytes = estimate,
                )
            } else null
            PrivateItemPreflight(image.id, recipe, blocks)
        }
        return PrivateBatchPreflight(draft.revision, items)
    }
}

