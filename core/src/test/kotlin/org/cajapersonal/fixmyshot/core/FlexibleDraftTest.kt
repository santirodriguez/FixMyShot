package org.cajapersonal.fixmyshot.core

import org.junit.Assert.*
import org.junit.Test

class FlexibleDraftTest {
    private val source = DraftImage("safe", "synthetic-safe", PixelSize(690, 1536), false,
        contentSha256 = "b".repeat(64))

    @Test fun flexibleModeProducesSourceBoundRecipeAndOneUndoableEdit() {
        val before = DraftSession(destination = DestinationChoice.Preset(DestinationCatalog.SOCIAL_STORY),
            images = listOf(source))
        val focus = FrameFocus(.4, .7, .81)
        val after = before.applyFraming("safe", FitMode.FLEXIBLE, CropFocus(), ContainPosition(), focus)
        val plan = after.plans(36).single()
        val geometry = (plan.geometry as PlannedGeometry.Framed).value
        assertEquals(FramePlanner.plan(source.orientedSize,
            ExactRatio.of(9, 16), focus), geometry)
        assertEquals(before.revision + 1, after.revision)
        assertEquals(focus, plan.settings.frame)
        assertEquals(OutputFormat.PNG, plan.settings.format)
        assertEquals(MetadataPolicy.REMOVE_IDENTIFYING, plan.settings.metadata)
        assertTrue(plan.reviewFlags.isEmpty())
        val prepared = ExportPreparation.inspect(after, 36, 512L * 1024 * 1024)
        assertEquals(0, prepared.blockedCount)
        val recipe = prepared.candidates.single()
        assertTrue(recipe.flexibleFrame)
        assertEquals(geometry.canvas, recipe.canvas)
        assertEquals(geometry.placement, recipe.placement)
        assertNull(recipe.crop)
        assertEquals(before.destination, after.undo().destination)
        assertEquals(before.images.single().overrides, after.undo().images.single().overrides)
    }

    @Test fun legacyCropPlanStaysUnchangedAfterAddingFlexibleState() {
        val before = DraftSession(destination = DestinationChoice.Preset(DestinationCatalog.SOCIAL_STORY),
            batch = EditSettings(fit = FitMode.CROP, crop = CropFocus(.55, .3, 1.1)),
            images = listOf(source))
        val planned = before.plans(36).single()
        assertTrue(planned.geometry is PlannedGeometry.Cropped)
        val recipe = ExportPreparation.inspect(before, 36, 512L * 1024 * 1024).candidates.single()
        assertFalse(recipe.flexibleFrame)
        assertNotNull(recipe.crop)
        assertEquals(CropPlanner.plan(source.orientedSize, ExactRatio.of(9, 16),
            before.batch.crop), (planned.geometry as PlannedGeometry.Cropped).value)
    }

    @Test fun explicitExactSizeIsNeverSilentlyReplacedForFlexibleMode() {
        val size = OutputSizeChoice.Exact(PixelSize(1080, 1920))
        val draft = DraftSession(destination = DestinationChoice.Preset(DestinationCatalog.SOCIAL_STORY),
            images = listOf(source), batch = EditSettings(fit = FitMode.FLEXIBLE, size = size))
        val plan = draft.plans(36).single()
        assertTrue(plan.geometry == null)
        assertTrue(DraftReviewFlag.GEOMETRY_UNAVAILABLE in plan.reviewFlags)
        assertEquals(1, ExportPreparation.inspect(draft, 36, 512L * 1024 * 1024).blockedCount)
    }
}
