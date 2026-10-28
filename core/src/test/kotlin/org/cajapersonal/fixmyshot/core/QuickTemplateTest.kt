package org.cajapersonal.fixmyshot.core

import org.junit.Assert.*
import org.junit.Test

class QuickTemplateTest {
    private val portrait = DraftImage("p", "synthetic-p", PixelSize(1344, 2992), false,
        contentSha256 = "a".repeat(64))

    @Test fun firstRecommendationIsSafeAndRememberedTemplatesAreValidated() {
        assertEquals(DestinationCatalog.PLAY_PORTRAIT, QuickTemplates.initial(listOf(portrait), null).id)
        assertEquals(DestinationCatalog.PLAY_PHONE, QuickTemplates.initial(emptyList(), "not-a-template").id)
        assertEquals(DestinationCatalog.SOCIAL_SQUARE,
            QuickTemplates.initial(listOf(portrait), DestinationCatalog.SOCIAL_SQUARE).id)
        assertNull(QuickTemplates.remembered(DestinationCatalog.CUSTOM_RATIO))
        assertEquals(OutputFormat.PNG, DraftSession().batch.format)
        assertEquals(MetadataPolicy.REMOVE_IDENTIFYING, DraftSession().batch.metadata)
    }

    @Test fun templateChangeIsOneUndoableEditAndPreservesIndividualFields() {
        val overrides = ItemOverrides(background = ItemValue.Custom(BackgroundChoice.Black),
            metadata = ItemValue.Custom(MetadataPolicy.PRESERVE_ALLOWLIST))
        val before = DraftSession(images = listOf(portrait.copy(overrides = overrides)))
        val after = before.applyTemplate(DestinationChoice.Preset(DestinationCatalog.PLAY_PORTRAIT))
        assertEquals(before.revision + 1, after.revision)
        assertEquals(before.images, after.images)
        assertEquals(before.batch, after.batch)
        assertEquals(before.destination, after.undo().destination)
        assertEquals(overrides, after.undo().images.single().overrides)
        assertEquals(portrait.contentSha256, after.images.single().contentSha256)
    }

    @Test fun portraitAndFlexiblePlayKeepDifferentGeometries() {
        val before = DraftSession(images = listOf(portrait))
        val portraitPlan = before.applyTemplate(DestinationChoice.Preset(DestinationCatalog.PLAY_PORTRAIT)).plans(36).single()
        val geometry = (portraitPlan.geometry as PlannedGeometry.Contained).value
        assertEquals(PixelSize(1683, 2992), geometry.canvas)
        assertEquals(PixelPlacement(169, 0, 1344, 2992), geometry.placement)
        assertEquals(170, geometry.rightMargin)
        assertFalse(geometry.resized)
        assertTrue(portraitPlan.reviewFlags.isEmpty())
        assertEquals(PixelSize(1496, 2992), (before.plans(36).single().geometry as PlannedGeometry.Contained).value.canvas)
    }

    @Test fun incompatibleExplicitSettingsRemainVisibleInsteadOfBeingSubstituted() {
        val before = DraftSession(images = listOf(portrait.copy(overrides = ItemOverrides(
            format = ItemValue.Custom(OutputFormat.WEBP_LOSSLESS),
            background = ItemValue.Custom(BackgroundChoice.Transparent)))))
        val after = before.applyTemplate(DestinationChoice.Preset(DestinationCatalog.PLAY_PORTRAIT))
        val plan = after.plans(36).single()
        assertEquals(OutputFormat.WEBP_LOSSLESS, plan.settings.format)
        assertTrue(DraftReviewFlag.DESTINATION_REQUIRES_PNG in plan.reviewFlags)
        assertTrue(DraftReviewFlag.DESTINATION_REQUIRES_OPAQUE in plan.reviewFlags)
        assertSame(before, before.applyTemplate(before.destination))
    }

    @Test fun fullscreenFramingCommitsOnceWithoutResettingOtherFieldsOrImages() {
        val other = portrait.copy(id = "q", sourceToken = "synthetic-q")
        val before = DraftSession(images = listOf(portrait, other), batch = EditSettings(
            background = BackgroundChoice.CustomRgb(0x112233), metadata = MetadataPolicy.PRESERVE_ALLOWLIST))
        val after = before.applyFraming("p", FitMode.CROP, CropFocus(.5, .6, 1.02), ContainPosition())
        assertEquals(before.revision + 1, after.revision)
        assertEquals(other, after.images[1])
        val settings = after.images[0].overrides.resolve(after.batch)
        assertEquals(before.batch.background, settings.background)
        assertEquals(before.batch.metadata, settings.metadata)
        assertEquals(ItemValue.Inherit, after.images[0].overrides.position)
        assertEquals(before.images.map { it.overrides }, after.undo().images.map { it.overrides })
    }

    @Test fun inspectionOrUnchangedApplyDoesNotCreateAnEdit() {
        val before = DraftSession(images = listOf(portrait))
        val settings = portrait.overrides.resolve(before.batch)
        assertSame(before, before.applyFraming("p", settings.fit, settings.crop, settings.position))
    }
    @Test fun platformSpecificLayoutsAreNotCollapsedBySharedRatio() {
        val play = DestinationCatalog.resolve(DestinationChoice.Preset(DestinationCatalog.PLAY_PORTRAIT))
        val instagram = DestinationCatalog.resolve(DestinationChoice.Preset(DestinationCatalog.INSTAGRAM_STORY))
        val tiktok = DestinationCatalog.resolve(DestinationChoice.Preset(DestinationCatalog.TIKTOK_PHOTO))
        val whatsapp = DestinationCatalog.resolve(DestinationChoice.Preset(DestinationCatalog.WHATSAPP_STATUS))
        assertEquals(ExactRatio.of(9, 16), play.ratio)
        assertEquals(play.ratio, instagram.ratio)
        assertEquals(instagram.ratio, tiktok.ratio)
        assertEquals(tiktok.ratio, whatsapp.ratio)
        assertEquals(DestinationEvidence.OFFICIAL_RECOMMENDATION, play.preset.evidence)
        assertEquals(DestinationEvidence.LAYOUT_GUIDANCE, tiktok.preset.evidence)
        assertEquals(FormatPolicy.PNG_RGB_ONLY, play.preset.formatPolicy)
        assertEquals(FormatPolicy.UNVERIFIED_UPLOAD, whatsapp.preset.formatPolicy)
        assertEquals(13, QuickTemplates.ids.distinct().size)
        assertEquals(DestinationCatalog.TIKTOK_PHOTO,
            QuickTemplates.remembered(DestinationCatalog.TIKTOK_PHOTO)?.id)
    }

    @Test fun whatsappChatKeepsAnUntouchedSourceSizedCanvas() {
        val draft = DraftSession(images = listOf(portrait)).applyTemplate(
            DestinationChoice.Preset(DestinationCatalog.WHATSAPP_CHAT))
        val plan = draft.plans(36).single()
        val contain = (plan.geometry as PlannedGeometry.Contained).value
        assertEquals(portrait.orientedSize, contain.canvas)
        assertEquals(PixelPlacement(0, 0, 1344, 2992), contain.placement)
        assertFalse(contain.resized)
        assertTrue(plan.reviewFlags.isEmpty())
        assertEquals(ExactRatio.of(1344, 2992), plan.destination.cropRatio(portrait.orientedSize))
        assertEquals(OutputFormat.PNG, draft.batch.format)
        assertEquals(MetadataPolicy.REMOVE_IDENTIFYING, draft.batch.metadata)
    }

}
