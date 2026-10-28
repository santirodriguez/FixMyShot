package org.cajapersonal.fixmyshot.core

/** Recommendations are layout choices, never inferred publication destinations. */
object QuickTemplates {
    val ids = listOf(DestinationCatalog.PLAY_PORTRAIT, DestinationCatalog.PLAY_PHONE,
        DestinationCatalog.SOCIAL_PORTRAIT, DestinationCatalog.SOCIAL_STORY, DestinationCatalog.SOCIAL_SQUARE)

    fun remembered(id: String?): DestinationChoice.Preset? =
        id?.takeIf { it in ids }?.let { DestinationChoice.Preset(it) }

    fun initial(images: List<DraftImage>, preferred: String?): DestinationChoice.Preset =
        remembered(preferred) ?: DestinationChoice.Preset(
            if (images.firstOrNull()?.orientedSize?.let { it.height > it.width } == true)
                DestinationCatalog.PLAY_PORTRAIT else DestinationCatalog.PLAY_PHONE)
}
