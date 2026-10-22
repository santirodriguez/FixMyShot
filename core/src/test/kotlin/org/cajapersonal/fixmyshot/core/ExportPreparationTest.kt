package org.cajapersonal.fixmyshot.core

import org.junit.Assert.*
import org.junit.Test

/** These tests verify only immutable preparation, never encoding or publication. */
class ExportPreparationTest {
    private val hash = "a".repeat(64)
    private val heap = 512L * 1024 * 1024

    private fun source(id: String = "a", size: PixelSize = PixelSize(1344, 2992)) =
        DraftImage(id, "private-test-source", size, false, contentSha256 = hash)

    @Test fun playPhoneSourceAndPixelPlacementAreFrozenWithoutChangingFormat() {
        val draft = DraftSession().addImages(listOf(source()))
        val result = ExportPreparation.inspect(draft, 26, heap)
        assertEquals(0, result.blockedCount)
        val item = result.candidates.single()
        assertEquals("a", item.sourceId)
        assertEquals(hash, item.sourceSha256)
        assertEquals(PixelSize(1496, 2992), item.canvas)
        assertEquals(PixelPlacement(76, 0, 1344, 2992), item.placement)
        assertEquals(OutputFormat.PNG, item.request.format)
        assertEquals(CodecRoute.ANDROID_PNG, item.codecRoute)
        assertEquals(draft.revision, item.sessionRevision)
        assertTrue(item.needsBackgroundResolution)
        assertTrue(item.estimatedWorkingSetBytes > 0L)
    }

    @Test fun exactHashRequiredAndInvalidDigestRejectedByDraft() {
        val missing = DraftSession().addImages(listOf(DraftImage("a", "private", PixelSize(20, 40), false)))
        assertTrue(PrivatePreparationBlock.SOURCE_FINGERPRINT_MISSING in
            ExportPreparation.inspect(missing, 26, heap).items.single().blocks)
        assertThrows(IllegalArgumentException::class.java) {
            DraftImage("a", "private", PixelSize(20, 40), false, contentSha256 = "NOT_SHA")
        }
    }

    @Test fun playCompatibilityConflictDoesNotSilentlySubstitutePngForWebp() {
        val draft = DraftSession().addImages(listOf(source()))
            .setBatch(EditSettings(format = OutputFormat.WEBP_LOSSLESS))
        val item = ExportPreparation.inspect(draft, 36, heap).items.single()
        assertNull(item.recipe)
        assertTrue(PrivatePreparationBlock.REVIEW_CONFLICT in item.blocks)
        assertFalse(ExportPreparation.inspect(draft, 36, heap).candidates.any())
        assertEquals(OutputFormat.WEBP_LOSSLESS, draft.batch.format)
    }

    @Test fun supportedCustomWebpTransparentPlanKeepsSelectedAlpha() {
        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1))
            .addImages(listOf(source(size = PixelSize(400, 200))))
            .setBatch(EditSettings(format = OutputFormat.WEBP_LOSSLESS,
                background = BackgroundChoice.Transparent))
        val plan = ExportPreparation.inspect(draft, 26, heap).candidates.single()
        assertEquals(PixelSize(400, 400), plan.canvas)
        assertTrue(plan.request.hasAlpha)
        assertEquals(CodecRoute.LIBWEBP, plan.codecRoute)
        assertFalse(plan.needsBackgroundResolution)
    }

    @Test fun unacceptedNativeFormatAndMetadataOptOutRemainBlocked() {
        val custom = DraftSession(destination = DestinationChoice.Custom.of(1, 1))
            .addImages(listOf(source(size = PixelSize(100, 100))))
        val heic = custom.setBatch(EditSettings(format = OutputFormat.HEIC))
        val item = ExportPreparation.inspect(heic, 36, heap).items.single()
        assertNull(item.recipe)
        assertTrue(PrivatePreparationBlock.FORMAT_NOT_READY in item.blocks)
        val optOut = custom.setBatch(EditSettings(metadata = MetadataPolicy.PRESERVE_ALLOWLIST))
        val other = ExportPreparation.inspect(optOut, 36, heap).items.single()
        assertNull(other.recipe)
        assertTrue(PrivatePreparationBlock.CODEC_UNAVAILABLE in other.blocks)
    }

    @Test fun cropFreezesTheVisibleSourceWithoutRelabelingItAsContain() {
        val draft = DraftSession(destination = DestinationChoice.Preset(DestinationCatalog.SOCIAL_SQUARE))
            .addImages(listOf(source(size = PixelSize(100, 200))))
            .setBatch(EditSettings(fit = FitMode.CROP, crop = CropFocus(0.5, 0.4, 2.0)))
        val recipe = ExportPreparation.inspect(draft, 36, heap).candidates.single()
        assertEquals(PixelSize(50, 50), recipe.canvas)
        assertEquals(SourceRectangle(25.0, 55.0, 50.0, 50.0), recipe.crop)
        assertEquals(PixelPlacement(0, 0, 50, 50), recipe.placement)
        assertEquals(PixelSize(100, 200), recipe.orientedSource)
    }

    @Test fun undersizedAndRepositionedCropsRequireReviewWithoutChangingTheDraft() {
        val image = source(size = PixelSize(100, 200))
        val small = DraftSession(destination = DestinationChoice.Preset(DestinationCatalog.PLAY_PORTRAIT),
            batch = EditSettings(fit = FitMode.CROP)).addImages(listOf(image))
        assertTrue(DraftReviewFlag.DIMENSIONS_REQUIRE_REVIEW in small.plans(36).single().reviewFlags)
        assertEquals(1, ExportPreparation.inspect(small, 36, heap).blockedCount)
        val moved = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
            batch = EditSettings(fit = FitMode.CROP, crop = CropFocus(0.0, 0.0)))
            .addImages(listOf(image))
        assertEquals(1, ExportPreparation.inspect(moved, 36, heap).blockedCount)
        assertEquals(CropFocus(0.0, 0.0), moved.batch.crop)
        val accepted = moved.editItem(image.id,
            ItemOverrides(crop = ItemValue.Custom(CropFocus(0.5, 0.25))))
        assertEquals(SourceRectangle(0.0, 0.0, 100.0, 100.0),
            ExportPreparation.inspect(accepted, 36, heap).candidates.single().crop)
    }

    @Test fun dynamicWorkingSetCeilingBlocksWithoutAllocatingBitmaps() {
        val draft = DraftSession().addImages(listOf(source()))
        val item = ExportPreparation.inspect(draft, 26, 64L * 1024 * 1024).items.single()
        assertNull(item.recipe)
        assertTrue(PrivatePreparationBlock.MEMORY_LIMIT in item.blocks)
    }

    @Test fun revisionAndFingerprintAreImmutableAcrossLaterEdits() {
        val original = DraftSession().addImages(listOf(source()))
        val first = ExportPreparation.inspect(original, 36, heap).candidates.single()
        val changed = original.setBatch(original.batch.copy(background = BackgroundChoice.Black))
        val second = ExportPreparation.inspect(changed, 36, heap).candidates.single()
        assertNotEquals(first.sessionRevision, second.sessionRevision)
        assertEquals(BackgroundChoice.Automatic, first.background)
        assertEquals(BackgroundChoice.Black, second.background)
        assertEquals(hash, first.sourceSha256)
        assertEquals(hash, second.sourceSha256)
    }
}

