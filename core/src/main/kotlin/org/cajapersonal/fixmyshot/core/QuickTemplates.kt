package org.cajapersonal.fixmyshot.core

/** Recommendations are layout choices, never inferred publication destinations. */
object QuickTemplates {
    val ids = listOf(DestinationCatalog.ORIGINAL, DestinationCatalog.PLAY_PORTRAIT, DestinationCatalog.PLAY_PHONE,
        DestinationCatalog.INSTAGRAM_FEED, DestinationCatalog.INSTAGRAM_STORY,
        DestinationCatalog.INSTAGRAM_SQUARE, DestinationCatalog.TIKTOK_PHOTO,
        DestinationCatalog.WHATSAPP_STATUS, DestinationCatalog.WHATSAPP_CHAT,
        DestinationCatalog.FACEBOOK_FEED, DestinationCatalog.FACEBOOK_SQUARE,
        // Old generic IDs remain valid for restored drafts and remembered choices.
        DestinationCatalog.SOCIAL_PORTRAIT, DestinationCatalog.SOCIAL_STORY, DestinationCatalog.SOCIAL_SQUARE)

    fun remembered(id: String?): DestinationChoice.Preset? =
        id?.takeIf { it in ids }?.let { DestinationChoice.Preset(it) }

    // Orientation alone is not evidence of a publishing destination.
    // Explicitly selected, valid previous templates remain a useful convenience.
    fun initial(images: List<DraftImage>, preferred: String?): DestinationChoice.Preset =
        remembered(preferred) ?: DestinationChoice.Preset(DestinationCatalog.ORIGINAL)
}
