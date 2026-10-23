package org.cajapersonal.fixmyshot.core

/** These are offline layout contracts, not a claim that a third-party uploader accepts every codec. */
enum class DestinationEvidence { OFFICIAL_UPLOAD, OFFICIAL_RECOMMENDATION, LAYOUT_GUIDANCE, USER_DEFINED }
enum class FormatPolicy { PNG_RGB_ONLY, UNVERIFIED_UPLOAD, USER_SELECTED }

data class DestinationPreset(
    val id: String,
    val labelKey: String,
    val evidence: DestinationEvidence,
    val formatPolicy: FormatPolicy,
    val ratio: ExactRatio?,
    val minimumShortSide: Int = 0,
    val maximumLongSide: Int? = null,
    val requiresOpaque: Boolean = true,
) {
    init {
        require(id.isNotBlank() && labelKey.isNotBlank())
        require(minimumShortSide >= 0)
        require(maximumLongSide == null || maximumLongSide > 0)
    }
}

sealed interface DestinationChoice {
    data class Preset(val id: String) : DestinationChoice {
        init { require(id.isNotBlank()) }
    }

    data class Custom private constructor(val ratio: ExactRatio) : DestinationChoice {
        companion object {
            fun of(numerator: Int, denominator: Int): Custom {
                require(numerator in 1..10_000 && denominator in 1..10_000) {
                    "Custom ratio sides must be between 1 and 10000"
                }
                return Custom(ExactRatio.of(numerator.toLong(), denominator.toLong()))
            }
        }
    }
}

object DestinationCatalog {
    const val VERSION = 1
    const val PLAY_PHONE = "play-phone"
    const val PLAY_PORTRAIT = "play-portrait"
    const val SOCIAL_SQUARE = "social-square"
    const val SOCIAL_PORTRAIT = "social-portrait"
    const val SOCIAL_STORY = "social-story"
    const val CUSTOM_RATIO = "custom-ratio"

    val presets: List<DestinationPreset> = listOf(
        DestinationPreset(PLAY_PHONE, "preset_play_phone", DestinationEvidence.OFFICIAL_UPLOAD,
            FormatPolicy.PNG_RGB_ONLY, ratio = null, maximumLongSide = 3840),
        DestinationPreset(PLAY_PORTRAIT, "preset_play_portrait", DestinationEvidence.OFFICIAL_RECOMMENDATION,
            FormatPolicy.PNG_RGB_ONLY, ExactRatio.of(9, 16), minimumShortSide = 1080, maximumLongSide = 3840),
        DestinationPreset(SOCIAL_SQUARE, "preset_social_square", DestinationEvidence.LAYOUT_GUIDANCE,
            FormatPolicy.UNVERIFIED_UPLOAD, ExactRatio.of(1, 1)),
        DestinationPreset(SOCIAL_PORTRAIT, "preset_social_portrait", DestinationEvidence.LAYOUT_GUIDANCE,
            FormatPolicy.UNVERIFIED_UPLOAD, ExactRatio.of(4, 5)),
        DestinationPreset(SOCIAL_STORY, "preset_social_story", DestinationEvidence.LAYOUT_GUIDANCE,
            FormatPolicy.UNVERIFIED_UPLOAD, ExactRatio.of(9, 16)),
        DestinationPreset(CUSTOM_RATIO, "preset_custom_ratio", DestinationEvidence.USER_DEFINED,
            FormatPolicy.USER_SELECTED, ratio = null, requiresOpaque = false),
    )

    private val byId: Map<String, DestinationPreset> = presets.associateBy { it.id }.also { all ->
        require(all.size == presets.size) { "Duplicate stable destination ID" }
    }

    fun resolve(choice: DestinationChoice): ResolvedDestination = when (choice) {
        is DestinationChoice.Preset -> {
            val preset = requireNotNull(byId[choice.id]) { "Unknown destination: ${choice.id}" }
            require(preset.id != CUSTOM_RATIO) { "Custom ratio requires explicit dimensions" }
            ResolvedDestination(preset, preset.ratio)
        }
        is DestinationChoice.Custom -> ResolvedDestination(
            requireNotNull(byId[CUSTOM_RATIO]), choice.ratio,
        )
    }
}

data class ResolvedDestination(val preset: DestinationPreset, val ratio: ExactRatio?) {
    fun cropRatio(source: PixelSize, size: OutputSizeChoice = OutputSizeChoice.PreservePixels): ExactRatio =
        (size as? OutputSizeChoice.Exact)?.let { ExactRatio.of(it.canvas.width.toLong(), it.canvas.height.toLong()) } ?: ratio ?: when {
        source.height.toLong() > 2L * source.width -> ExactRatio.of(1, 2)
        source.width.toLong() > 2L * source.height -> ExactRatio.of(2, 1)
        else -> ExactRatio.of(source.width.toLong(), source.height.toLong())
    }

    fun contain(source: PixelSize, size: OutputSizeChoice): ContainGeometry = when (size) {
        is OutputSizeChoice.Exact -> GeometryPlanner.exactCanvas(source, size.canvas, size.allowEnlargement)
        OutputSizeChoice.PreservePixels -> when (preset.id) {
            DestinationCatalog.PLAY_PHONE -> GeometryPlanner.playPhoneEnvelope(source)
            else -> GeometryPlanner.preservePixelsAtRatio(
                source, requireNotNull(ratio), minimumShortSide = preset.minimumShortSide,
            )
        }
    }
}
