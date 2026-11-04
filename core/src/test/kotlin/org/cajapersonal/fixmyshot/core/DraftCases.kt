package org.cajapersonal.fixmyshot.core

/** Focused JVM contract checks for the shared planner; no hardware approval is implied. */
fun draftCases(): List<Pair<String, () -> Unit>> {
    val source = PixelSize(1344, 2992)
    fun item(id: String = "a", token: String = "source-a") = DraftImage(id, token, source, false)
    fun plan(id: String) = DraftSession(destination = DestinationChoice.Preset(id))
        .addImages(listOf(item())).plans(26).single()
    fun expectFailure(action: () -> Unit) = check(runCatching(action).exceptionOrNull() is IllegalArgumentException)
    return listOf(
        "catalog IDs preserve all legacy entries and add distinct platform surfaces" to {
            check(DestinationCatalog.VERSION == 1)
            check(DestinationCatalog.presets.map { it.id } == listOf("original", "play-phone", "play-portrait",
                "social-square", "social-portrait", "social-story", "instagram-feed", "instagram-story",
                "instagram-square", "tiktok-photo", "whatsapp-status", "whatsapp-chat",
                "facebook-feed", "facebook-square", "custom-ratio"))
            check(DestinationCatalog.presets.map { it.id }.distinct().size == 15)
            expectFailure { DestinationCatalog.resolve(DestinationChoice.Preset("unknown")) }
            expectFailure { DestinationCatalog.resolve(DestinationChoice.Preset("custom-ratio")) }
        },
        "new draft defaults to PNG and identifying metadata removal" to {
            val d = DraftSession()
            check(d.batch.format == OutputFormat.PNG)
            check(d.batch.metadata == MetadataPolicy.REMOVE_IDENTIFYING)
            check(d.batch.fit == FitMode.CONTAIN)
        },
        "Play phone pixel geometry exactly matches the governing screenshot" to {
            val p = plan(DestinationCatalog.PLAY_PHONE)
            check((p.geometry as PlannedGeometry.Contained).value.canvas == PixelSize(1496, 2992))
            check(p.reviewFlags.isEmpty())
        },
        "portrait Play recommendation pads small sources without automatic enlargement" to {
            val p = DraftSession(destination = DestinationChoice.Preset(DestinationCatalog.PLAY_PORTRAIT))
                .addImages(listOf(DraftImage("tiny", "s", PixelSize(150, 300), false))).plans(26).single()
            val g = (p.geometry as PlannedGeometry.Contained).value
            check(g.canvas == PixelSize(1080, 1920))
            check(!g.resized && !g.enlarged)
        },
        "square and portrait-social preserve Pixel source content with exact ratio" to {
            check((plan(DestinationCatalog.SOCIAL_SQUARE).geometry as PlannedGeometry.Contained).value.canvas == PixelSize(2992, 2992))
            check((plan(DestinationCatalog.SOCIAL_PORTRAIT).geometry as PlannedGeometry.Contained).value.canvas == PixelSize(2396, 2995))
            check(plan(DestinationCatalog.SOCIAL_SQUARE).destination.preset.evidence == DestinationEvidence.LAYOUT_GUIDANCE)
        },
        "custom input ratio is reduced and bounded" to {
            val d = DraftSession(destination = DestinationChoice.Custom.of(18, 32)).addImages(listOf(item()))
            val g = (d.plans(26).single().geometry as PlannedGeometry.Contained).value
            check(g.canvas == PixelSize(1683, 2992))
            expectFailure { DestinationChoice.Custom.of(10_001, 2) }
            expectFailure { DestinationChoice.Custom.of(0, 2) }
        },
        "crop produces a bounded centered rectangle with no default data loss" to {
            val p = CropPlanner.plan(PixelSize(100, 200), ExactRatio.of(1, 1))
            check(p.visibleSource == SourceRectangle(0.0, 50.0, 100.0, 100.0))
            check(p.output == PixelSize(100, 100) && !p.focusWasClamped)
        },
        "crop pan clamps and zoom reduces original native output resolution" to {
            val a = CropPlanner.plan(PixelSize(100, 200), ExactRatio.of(1, 1), CropFocus(0.0, 0.0, 1.0))
            val b = CropPlanner.plan(PixelSize(100, 200), ExactRatio.of(1, 1), CropFocus(0.0, 0.0, 2.0))
            check(a.focusWasClamped && b.focusWasClamped)
            check(b.output == PixelSize(50, 50) && a.output == PixelSize(100, 100))
            check(a.visibleSource.left >= 0 && a.visibleSource.top >= 0)
        },
        "crop never silently upsizes on a large integer ratio grid" to {
            expectFailure { CropPlanner.plan(PixelSize(1, 2), ExactRatio.of(4, 5)) }
            expectFailure { CropFocus(0.5, 0.5, Double.NaN) }
            expectFailure { CropFocus(0.5, 0.5, 0.5) }
        },
        "changing global settings leaves each item override intact" to {
            val original = DraftSession().addImages(listOf(item(), item("b", "source-b")))
                .editItem("a", ItemOverrides(format = ItemValue.Custom(OutputFormat.WEBP_LOSSLESS),
                    metadata = ItemValue.Custom(MetadataPolicy.REMOVE_IDENTIFYING)))
            val revised = original.setBatch(original.batch.copy(format = OutputFormat.AVIF,
                metadata = MetadataPolicy.PRESERVE_ALLOWLIST))
            check(revised.images[0].overrides == original.images[0].overrides)
            check(revised.plans(36).first().settings.format == OutputFormat.WEBP_LOSSLESS)
            check(revised.plans(36).last().settings.format == OutputFormat.AVIF)
            check(revised.plans(36).first().settings.metadata == MetadataPolicy.REMOVE_IDENTIFYING)
            check(revised.plans(36).last().settings.metadata == MetadataPolicy.PRESERVE_ALLOWLIST)
            check(revised.revision > original.revision)
        },
        "destination switch preserves crop and per-item choices" to {
            val focus = CropFocus(0.8, 0.2, 2.0)
            val d = DraftSession().addImages(listOf(item()))
                .editItem("a", ItemOverrides(fit = ItemValue.Custom(FitMode.CROP),
                    crop = ItemValue.Custom(focus), format = ItemValue.Custom(OutputFormat.WEBP_LOSSLESS)))
            val changed = d.setDestination(DestinationChoice.Preset(DestinationCatalog.SOCIAL_SQUARE))
            check(changed.images.single().overrides.crop == ItemValue.Custom(focus))
            check(changed.plans(26).single().settings.crop == focus)
            check(changed.revision == d.revision + 1)
        },
        "explicit apply-to-all reports replacements and one-step undo restores values" to {
            val d = DraftSession().addImages(listOf(item(), item("b", "source-b")))
                .editItem("a", ItemOverrides(format = ItemValue.Custom(OutputFormat.WEBP_LOSSLESS),
                    background = ItemValue.Custom(BackgroundChoice.White)))
            val outcome = d.applyBatchSettingsToAll(setOf(EditableField.FORMAT))
            check(outcome.itemsChanged == 1)
            check(outcome.draft.images.first().overrides.format == ItemValue.Inherit)
            check(outcome.draft.images.first().overrides.background == ItemValue.Custom(BackgroundChoice.White))
            val undone = outcome.draft.undo()
            check(undone.images.first().overrides == d.images.first().overrides)
            check(undone.revision > outcome.draft.revision)
        },
        "reset one item never changes a different item" to {
            val d = DraftSession().addImages(listOf(item(), item("b", "source-b")))
                .editItem("a", ItemOverrides(background = ItemValue.Custom(BackgroundChoice.Black)))
                .editItem("b", ItemOverrides(format = ItemValue.Custom(OutputFormat.WEBP_LOSSLESS)))
            val updated = d.resetItem("a")
            check(updated.images.first().overrides == ItemOverrides())
            check(updated.images.last() == d.images.last())
        },
        "invalid and duplicate imports are handled without moving existing items" to {
            val d = DraftSession().addImages(listOf(item(), item("b", "source-b")))
            check(d.addImages(listOf(item("x", "source-a"), item("b", "source-c"))) == d)
            val distinctAfterDuplicate = d.addImages(listOf(item("x", "source-a"), item("x", "source-x")))
            check(distinctAfterDuplicate.images.last().id == "x")
            check(distinctAfterDuplicate.images.last().sourceToken == "source-x")
            val reordered = d.reorder(listOf("b", "a"))
            check(reordered.images.map { it.id } == listOf("b", "a"))
            check(reordered.removeItem("b").images.single().id == "a")
            expectFailure { d.reorder(listOf("a", "a")) }
        },
        "Play PNG restriction is a conflict and never rewrites chosen WebP" to {
            val d = DraftSession().addImages(listOf(item()))
                .setBatch(EditSettings(format = OutputFormat.WEBP_LOSSLESS))
            val p = d.plans(26).single()
            check(DraftReviewFlag.DESTINATION_REQUIRES_PNG in p.reviewFlags)
            check(p.settings.format == OutputFormat.WEBP_LOSSLESS)
        },
        "transparency conflict retains a user-selected transparent background" to {
            val d = DraftSession().addImages(listOf(item()))
                .setBatch(EditSettings(background = BackgroundChoice.Transparent))
            check(DraftReviewFlag.DESTINATION_REQUIRES_OPAQUE in d.plans(26).single().reviewFlags)
            check(d.plans(26).single().settings.background == BackgroundChoice.Transparent)
        },
        "unavailable codec is explicit and not silently replaced by PNG" to {
            val p = DraftSession(destination = DestinationChoice.Preset(DestinationCatalog.SOCIAL_STORY))
                .addImages(listOf(item())).setBatch(EditSettings(format = OutputFormat.JXL)).plans(36).single()
            check(DraftReviewFlag.CODEC_UNAVAILABLE in p.reviewFlags)
            check(p.settings.format == OutputFormat.JXL)
        },
        "oversized Play screenshot gets a conflict instead of silent scaling" to {
            val p = DraftSession().addImages(listOf(DraftImage("l", "large", PixelSize(2000, 4000), false)))
                .plans(26).single()
            check(DraftReviewFlag.DIMENSIONS_REQUIRE_REVIEW in p.reviewFlags)
            check((p.geometry as PlannedGeometry.Contained).value.canvas.width == 2000)
        },
        "format eligibility checks resulting explicit size rather than original size" to {
            val d = DraftSession(destination = DestinationChoice.Preset(DestinationCatalog.SOCIAL_SQUARE))
                .addImages(listOf(DraftImage("l", "large", PixelSize(5000, 5000), false)))
                .setBatch(EditSettings(size = OutputSizeChoice.Exact(PixelSize(1080, 1080))))
            val p = d.plans(26).single()
            check(DraftReviewFlag.CODEC_UNAVAILABLE !in p.reviewFlags)
            check((p.geometry as PlannedGeometry.Contained).value.resized)
        },
        "new session resets mandatory defaults even after previous changed settings" to {
            val old = DraftSession().setBatch(EditSettings(format = OutputFormat.HEIC,
                metadata = MetadataPolicy.PRESERVE_ALLOWLIST))
            check(old.batch.format == OutputFormat.HEIC)
            val fresh = DraftSession()
            check(fresh.batch.format == OutputFormat.PNG)
            check(fresh.batch.metadata == MetadataPolicy.REMOVE_IDENTIFYING)
        },
    )
}
