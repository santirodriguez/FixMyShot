package org.cajapersonal.fixmyshot.core

/** Editable values retain provenance: a batch change must not erase item-specific choices. */
sealed interface ItemValue<out T> {
    data object Inherit : ItemValue<Nothing>
    data class Custom<T>(val value: T) : ItemValue<T>
}

private fun <T> ItemValue<T>.resolve(batch: T): T = when (this) {
    ItemValue.Inherit -> batch
    is ItemValue.Custom -> value
}

enum class FitMode { CONTAIN, CROP, FLEXIBLE }

sealed interface OutputSizeChoice {
    data object PreservePixels : OutputSizeChoice
    data class Exact(val canvas: PixelSize, val allowEnlargement: Boolean = false) : OutputSizeChoice
}

sealed interface BackgroundChoice {
    data object Automatic : BackgroundChoice
    data object White : BackgroundChoice
    data object Black : BackgroundChoice
    data object Transparent : BackgroundChoice
    data class CustomRgb(val rgb: Int) : BackgroundChoice {
        init { require(rgb in 0..0xFFFFFF) }
    }
}

data class EditSettings(
    val fit: FitMode = FitMode.CONTAIN,
    val crop: CropFocus = CropFocus(),
    val background: BackgroundChoice = BackgroundChoice.Automatic,
    val format: OutputFormat = OutputFormat.PNG,
    val size: OutputSizeChoice = OutputSizeChoice.PreservePixels,
    val metadata: MetadataPolicy = MetadataPolicy.REMOVE_IDENTIFYING,
    val position: ContainPosition = ContainPosition(),
    /** Only consulted for FLEXIBLE; legacy CropFocus and its frozen plans are unchanged. */
    val frame: FrameFocus = FrameFocus(),
)

data class ItemOverrides(
    val fit: ItemValue<FitMode> = ItemValue.Inherit,
    val crop: ItemValue<CropFocus> = ItemValue.Inherit,
    val background: ItemValue<BackgroundChoice> = ItemValue.Inherit,
    val format: ItemValue<OutputFormat> = ItemValue.Inherit,
    val size: ItemValue<OutputSizeChoice> = ItemValue.Inherit,
    val metadata: ItemValue<MetadataPolicy> = ItemValue.Inherit,
    val position: ItemValue<ContainPosition> = ItemValue.Inherit,
    val frame: ItemValue<FrameFocus> = ItemValue.Inherit,
) {
    fun resolve(batch: EditSettings): EditSettings = EditSettings(
        fit.resolve(batch.fit), crop.resolve(batch.crop), background.resolve(batch.background),
        format.resolve(batch.format), size.resolve(batch.size), metadata.resolve(batch.metadata),
        position.resolve(batch.position), frame.resolve(batch.frame),
    )

    fun clear(fields: Set<EditableField>): ItemOverrides = copy(
        fit = if (EditableField.FIT in fields) ItemValue.Inherit else fit,
        crop = if (EditableField.CROP in fields) ItemValue.Inherit else crop,
        background = if (EditableField.BACKGROUND in fields) ItemValue.Inherit else background,
        format = if (EditableField.FORMAT in fields) ItemValue.Inherit else format,
        size = if (EditableField.SIZE in fields) ItemValue.Inherit else size,
        metadata = if (EditableField.METADATA in fields) ItemValue.Inherit else metadata,
        position = if (EditableField.POSITION in fields) ItemValue.Inherit else position,
        frame = if (EditableField.FRAME in fields) ItemValue.Inherit else frame,
    )

    val hasCustomValues: Boolean get() = this != ItemOverrides()
}

enum class EditableField { FIT, CROP, BACKGROUND, FORMAT, SIZE, METADATA, POSITION, FRAME }

/** Original appearance; recognized wide-gamut SDR is converted, never relabelled as sRGB. */
enum class SourceColor { SRGB, DISPLAY_P3, ADOBE_RGB }

data class DraftImage(
    val id: String,
    val sourceToken: String,
    val orientedSize: PixelSize,
    val sourceHasAlpha: Boolean,
    val revision: Long = 0,
    val overrides: ItemOverrides = ItemOverrides(),
    /** SHA-256 of the privately staged original bytes; verified again before any future export. */
    val contentSha256: String? = null,
    val sourceColor: SourceColor = SourceColor.SRGB,
) {
    init {
        require(id.isNotBlank() && sourceToken.isNotBlank() && revision >= 0)
        require(contentSha256 == null || Regex("[0-9a-f]{64}").matches(contentSha256)) {
            "Invalid source fingerprint"
        }
    }
}

enum class DraftReviewFlag {
    DESTINATION_REQUIRES_PNG,
    DESTINATION_REQUIRES_OPAQUE,
    CODEC_UNAVAILABLE,
    CODEC_RUNTIME_PROBE_REQUIRED,
    OVERSIZED_SOURCE,
    DIMENSIONS_REQUIRE_REVIEW,
    GEOMETRY_UNAVAILABLE,
    CROP_FOCUS_REPOSITIONED,
    CROP_ENLARGEMENT_REQUIRED,
}

sealed interface PlannedGeometry {
    data class Contained(val value: ContainGeometry) : PlannedGeometry
    data class Cropped(val value: CropGeometry) : PlannedGeometry
    data class Framed(val value: FrameGeometry) : PlannedGeometry
}

/** A deterministic preview proposal, NOT a validated export transaction or device codec probe. */
data class PreviewPlan(
    val itemId: String,
    val sessionRevision: Long,
    val itemRevision: Long,
    val catalogVersion: Int,
    val destination: ResolvedDestination,
    val settings: EditSettings,
    val geometry: PlannedGeometry?,
    val reviewFlags: Set<DraftReviewFlag>,
) {
    val requiresReview: Boolean get() = reviewFlags.isNotEmpty()
}

/** One-step snapshot preserves before-change values; revisions still advance on undo. */
data class PreviousDraft(
    val destination: DestinationChoice,
    val batch: EditSettings,
    val images: List<DraftImage>,
)

data class ApplyAllResult(val draft: DraftSession, val itemsChanged: Int)

data class DraftSession(
    val destination: DestinationChoice = DestinationChoice.Preset(DestinationCatalog.PLAY_PHONE),
    val batch: EditSettings = EditSettings(),
    val images: List<DraftImage> = emptyList(),
    val revision: Long = 0,
    private val previous: PreviousDraft? = null,
) {
    /** Includes the one-step undo snapshot so removing an image does not destroy undo bytes. */
    val workingSourceIds: Set<String> get() = (images + previous?.images.orEmpty()).mapTo(mutableSetOf()) { it.id }
    val canUndo: Boolean get() = previous != null
    /** One immutable undo snapshot; persisted review sessions do not retain an edit history. */
    val undoSnapshot: PreviousDraft? get() = previous

    init {
        require(revision >= 0)
        DestinationCatalog.resolve(destination)
        require(images.map { it.id }.toSet().size == images.size) { "Duplicate item ID" }
        require(images.map { it.sourceToken }.toSet().size == images.size) { "Duplicate source" }
        require(images.size <= 100) { "Batch limit exceeded" }
    }

    private fun nextRevision(): Long {
        require(revision < Long.MAX_VALUE) { "Revision overflow" }
        return revision + 1
    }

    private fun snapshot(): PreviousDraft = PreviousDraft(destination, batch, images)

    fun setDestination(value: DestinationChoice): DraftSession {
        DestinationCatalog.resolve(value)
        return if (value == destination) this else copy(destination = value, revision = nextRevision(), previous = snapshot())
    }

    /** One user action and undo boundary; explicit item and batch choices are never erased. */
    fun applyTemplate(value: DestinationChoice, restoreDefaults: Boolean = false): DraftSession {
        DestinationCatalog.resolve(value)
        val settings = if (restoreDefaults) EditSettings() else batch
        return if (value == destination && settings == batch) this else copy(
            destination = value, batch = settings, revision = nextRevision(), previous = snapshot(),
        )
    }

    /** Commit a fullscreen framing edit once, keeping all unrelated fields and provenance. */
    fun applyFraming(itemId: String, fit: FitMode, crop: CropFocus, position: ContainPosition,
        frame: FrameFocus? = null): DraftSession {
        val item = requireNotNull(images.find { it.id == itemId }) { "Unknown item" }
        val effective = item.overrides.resolve(batch)
        return editItem(itemId, item.overrides.copy(
            fit = if (fit == effective.fit) item.overrides.fit else ItemValue.Custom(fit),
            crop = if (crop == effective.crop) item.overrides.crop else ItemValue.Custom(crop),
            position = if (position == effective.position) item.overrides.position else ItemValue.Custom(position),
            frame = if (frame == null || frame == effective.frame) item.overrides.frame
                else ItemValue.Custom(frame),
        ))
    }

    fun setBatch(value: EditSettings): DraftSession = if (value == batch) this else
        copy(batch = value, revision = nextRevision(), previous = snapshot())

    fun addImages(additions: List<DraftImage>): DraftSession {
        val seenIds = images.mapTo(mutableSetOf()) { it.id }
        val seenTokens = images.mapTo(mutableSetOf()) { it.sourceToken }
        val unique = additions.filter { item ->
            if (item.id in seenIds || item.sourceToken in seenTokens) false else {
                seenIds.add(item.id)
                seenTokens.add(item.sourceToken)
                true
            }
        }
        if (unique.isEmpty()) return this
        require(images.size + unique.size <= 100) { "Batch limit exceeded" }
        return copy(images = images + unique, revision = nextRevision(), previous = snapshot())
    }

    fun editItem(itemId: String, overrides: ItemOverrides): DraftSession {
        val item = images.find { it.id == itemId } ?: throw IllegalArgumentException("Unknown item")
        if (item.overrides == overrides) return this
        require(item.revision < Long.MAX_VALUE) { "Item revision overflow" }
        return copy(
            images = images.map { if (it.id == itemId) it.copy(overrides = overrides, revision = it.revision + 1) else it },
            revision = nextRevision(), previous = snapshot(),
        )
    }

    fun resetItem(itemId: String): DraftSession = editItem(itemId, ItemOverrides())

    fun applyBatchSettingsToAll(fields: Set<EditableField>): ApplyAllResult {
        if (fields.isEmpty()) return ApplyAllResult(this, 0)
        var changes = 0
        val updated = images.map { item ->
            val overrides = item.overrides.clear(fields)
            if (overrides == item.overrides) item else {
                require(item.revision < Long.MAX_VALUE)
                changes++
                item.copy(overrides = overrides, revision = item.revision + 1)
            }
        }
        return if (changes == 0) ApplyAllResult(this, 0) else
            ApplyAllResult(copy(images = updated, revision = nextRevision(), previous = snapshot()), changes)
    }

    fun removeItem(itemId: String): DraftSession {
        require(images.any { it.id == itemId }) { "Unknown item" }
        return copy(images = images.filterNot { it.id == itemId }, revision = nextRevision(), previous = snapshot())
    }

    fun reorder(itemIds: List<String>): DraftSession {
        require(itemIds.size == images.size && itemIds.toSet() == images.map { it.id }.toSet()) {
            "Reorder must contain exactly the current stable IDs"
        }
        if (itemIds == images.map { it.id }) return this
        val itemsById = images.associateBy { it.id }
        return copy(images = itemIds.map { requireNotNull(itemsById[it]) }, revision = nextRevision(), previous = snapshot())
    }

    fun undo(): DraftSession {
        val old = previous ?: return this
        val nowRevisions = images.associate { it.id to it.revision }
        return copy(
            destination = old.destination, batch = old.batch,
            images = old.images.map { item ->
                val current = maxOf(item.revision, nowRevisions[item.id] ?: 0L)
                require(current < Long.MAX_VALUE)
                item.copy(revision = current + 1)
            },
            revision = nextRevision(), previous = null,
        )
    }

    fun plans(apiLevel: Int): List<PreviewPlan> {
        val selected = DestinationCatalog.resolve(destination)
        return images.map { image ->
            val settings = image.overrides.resolve(batch)
            val flags = linkedSetOf<DraftReviewFlag>()
            if (selected.preset.formatPolicy == FormatPolicy.PNG_RGB_ONLY && settings.format != OutputFormat.PNG) {
                flags += DraftReviewFlag.DESTINATION_REQUIRES_PNG
            }
            val outputHasAlpha = settings.background == BackgroundChoice.Transparent
            if (outputHasAlpha && (selected.preset.requiresOpaque || settings.format !in
                    setOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS))) {
                flags += DraftReviewFlag.DESTINATION_REQUIRES_OPAQUE
            }
            if (image.orientedSize.pixels > 64_000_000L) flags += DraftReviewFlag.OVERSIZED_SOURCE
            val geometry = try {
                when (settings.fit) {
                    FitMode.CONTAIN -> PlannedGeometry.Contained(
                        selected.contain(image.orientedSize, settings.size).positioned(settings.position))
                    FitMode.CROP -> {
                        val exact = settings.size as? OutputSizeChoice.Exact
                        val crop = CropPlanner.plan(image.orientedSize,
                            selected.cropRatio(image.orientedSize, settings.size),
                            settings.crop, exactOutput = exact?.canvas)
                        if (crop.focusWasClamped) flags += DraftReviewFlag.CROP_FOCUS_REPOSITIONED
                        if (crop.enlarged && exact?.allowEnlargement != true)
                            flags += DraftReviewFlag.CROP_ENLARGEMENT_REQUIRED
                        PlannedGeometry.Cropped(crop)
                    }
                    FitMode.FLEXIBLE -> {
                        // Exact-size flexible framing needs a separately reviewed scaling contract.
                        // Never silently stretch or replace the requested native-pixel output.
                        require(settings.size == OutputSizeChoice.PreservePixels)
                        PlannedGeometry.Framed(FramePlanner.plan(image.orientedSize,
                            selected.cropRatio(image.orientedSize, settings.size), settings.frame))
                    }
                }
            } catch (_: IllegalArgumentException) {
                flags += DraftReviewFlag.GEOMETRY_UNAVAILABLE
                null
            } catch (_: ArithmeticException) {
                flags += DraftReviewFlag.GEOMETRY_UNAVAILABLE
                null
            }
            val visible = when (geometry) {
                is PlannedGeometry.Contained -> geometry.value.canvas
                is PlannedGeometry.Cropped -> (settings.size as? OutputSizeChoice.Exact)?.canvas ?: geometry.value.output
                is PlannedGeometry.Framed -> geometry.value.canvas
                null -> null
            }
            if (visible != null && !matchesDestination(visible, selected)) {
                flags += DraftReviewFlag.DIMENSIONS_REQUIRE_REVIEW
            }
            val eligibility = CodecCapabilities.assess(apiLevel, EncodeRequest(
                visible ?: image.orientedSize, settings.format, outputHasAlpha, settings.metadata,
            ))
            when (eligibility) {
                is CodecEligibility.Unavailable -> flags += DraftReviewFlag.CODEC_UNAVAILABLE
                is CodecEligibility.ProbeRequired -> if (eligibility.route in setOf(
                    CodecRoute.NATIVE_HEIC, CodecRoute.NATIVE_AVIF, CodecRoute.JXL_REFERENCE,
                )) flags += DraftReviewFlag.CODEC_RUNTIME_PROBE_REQUIRED
            }
            PreviewPlan(image.id, revision, image.revision, DestinationCatalog.VERSION,
                selected, settings, geometry, flags)
        }
    }

    private fun matchesDestination(output: PixelSize, spec: ResolvedDestination): Boolean {
        if (minOf(output.width, output.height) < spec.preset.minimumShortSide) return false
        val maxSide = spec.preset.maximumLongSide
        if (maxSide != null && maxOf(output.width, output.height) > maxSide) return false
        if (spec.preset.id == DestinationCatalog.PLAY_PHONE) {
            if (minOf(output.width, output.height) < 320) return false
            return maxOf(output.width.toLong(), output.height.toLong()) <=
                2L * minOf(output.width.toLong(), output.height.toLong())
        }
        val ratio = spec.ratio ?: return true
        return output.width.toLong() * ratio.q == output.height.toLong() * ratio.p
    }
}
