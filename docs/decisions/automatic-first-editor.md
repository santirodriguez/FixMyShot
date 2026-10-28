# Automatic-first review workspace

The automatic workspace is the default presentation for new work, including images received through Android sharing. Customize is the existing complete editor, not a separate image pipeline. Both presentations use the same ViewModel, immutable DraftSession, normalized original sample, planner, export preflight and journal. This document amends the interface presentation of the first-functional-release blueprint, not its privacy, fidelity, codec or distribution gates.

## Product contract

- Start uses the existing canonical icon and header, with one Choose images action pinned within the portrait safe drawing area even on short screens; secondary artwork and copy scroll separately. No artificial splash delay. Light, dark and system themes remain available.
- An untouched new import receives a visible layout recommendation: the last explicitly selected built-in template, or Play portrait for a portrait source and flexible Play otherwise. This is a recommendation, not recognition of the user's publication destination. Only the validated built-in template ID is remembered; every new session retains PNG, metadata removal and full-content defaults.
- Template selection is one immutable undoable action. Batch settings and per-image overrides survive template and mode changes; incompatible explicit formats, alpha and dimensions remain conflicts. Play 9:16 and flexible Play keep different existing catalog IDs and geometry. Social cards remain layout guidance, not upload certification.
- Automatic has a large directly editable final-output preview, compact templates and an export action. Crop is explicitly selected. Pinching in full-content mode asks before enabling crop. Moving full content only redistributes real margins; no content is stretched, cloned or reconstructed.
- Expand opens a screen-sized framing view with local numeric editing state. Apply commits fit/focus/position once; Cancel changes no draft or journal bytes. Activity recreation retains those small fields. A change to the selected source, draft revision or edit lock invalidates the overlay rather than applying stale state. Inspect is clearly labelled view-only zoom and never changes output geometry. Original comparison is also inspection-only.
- Customize retains all previous tools, exact sizing, formats, metadata policy, backgrounds, scope selection, overrides, reorder, recovery and output actions. A SaveableStateHolder retains unfinished advanced text fields when returning to Automatic; session/source/scope keys remain authoritative. New work resets the presentation to Automatic, not the applied state of an existing restored draft.
- Discard is visible in the shared header and expanded editor. Confirmation describes private draft/undo removal while preserving originals and published exports. Locked or uncertain work routes to its existing result/recovery controls; it is not force-deleted. Removing one batch image remains distinct and undoable.
- Export feedback has a dedicated results surface using the established verification, share, retry and acknowledgement handlers. No automatic retry, duplicate publication or recovery-policy change is introduced.

## Ownership and resource boundaries

`QuickReviewUi.kt` owns the branded start, template cards, compact batch strip and automatic layout. `FocusEditor.kt` owns the isolated apply/cancel framing and inspection view. `ReviewScreen.kt` coordinates presentation and retains existing advanced/recovery controls. `ReviewCanvas.kt` and `CropControls.kt` share the retained-sample geometry path and commit actual event-local transforms once at gesture completion, discarding cancelled intermediates. `BrandTheme.kt` supplies appearance only; no brand color, border, inspection transform or artwork enters exported pixels.

`DraftSession.applyTemplate` and `applyFraming` define atomic revisions and preserve unaffected provenance. `QuickTemplates` validates remembered IDs. AppPreferences stores the last template alongside the existing theme preference, without adding format, metadata or language preferences. The build copies the exact canonical `assets/branding/header.png` and shared icon into generated resources; original source artwork is unchanged. Locale strings are split into `quick_strings.xml` for en/es/ca, and the existing resource checker verifies all string XML files and actual Kotlin references.

The expanded preview still uses one bounded sample. It is not an export source or proof of full-resolution fidelity. No codec, library, toolchain, permission, server, network or production signing change is included. The isolated development package is `.automatic` / FixMyShot Auto because CI debug signing is ephemeral; do not uninstall or overwrite earlier diagnostics to resolve a signature conflict.

## Acceptance and known limits

Focused core cases cover first-use recommendations, validated remembered IDs, atomic template undo, exact Pixel 1344x2992 -> 1683x2992 geometry with 169/170 side margins, preserved incompatible overrides, and no-op/fullscreen framing provenance. Android editor checks retain the prior two gesture/zoom cases and add automatic/mode switching, larger apply/cancel framing, inspection-only pinch, explicit crop consent and discoverable confirmed discard. Existing renderer, private PNG/WebP, import, transaction and recovery suites remain required in the accumulated API36 checkpoint.

Host-only Kotlin checks do not establish an Android build. Build, lint, instrumentation, actual screenshots, TalkBack, 200% text, landscape, keyboard and physical Pixel responsiveness/fidelity each need their own evidence. Device smoothness and visual acceptance are not inferred from compilation or synthetic input. The hardware/native, other-device-family, 16 KiB, legacy-provider, publisher/provenance and final signing/distribution gates remain open.

## UX 2.1 — crop responsiveness and destination clarity (implementation candidate)

The touch editor keeps a gesture-local crop focus while a pointer is down. The canvas updates its
sampled image without updating the draft, the full-screen preflight, other control composables,
or the undo journal on each pointer event. A completed gesture commits once; a cancelled gesture
drops its transient state. The exact CropPlanner remains the geometric authority. An optional
full-source inspection marks the excluded rectangle and displays the percentage trimmed;
it is never exported. Physical Pixel frame-time and full-resolution output comparison remain
required before claiming responsiveness or image-fidelity acceptance.

An explicitly selected **Match edges** background samples a median RGB value from the normalized
preview's vertical source edges and stores that fixed value using the existing CustomRgb policy.
The final exporter uses exactly that RGB; old Automatic drafts, frozen plans and retries retain
their previous behavior. This is a solid matte, not cloned/blurred/fabricated content. It is
initially opt-in pending comparison against actual Pixel source and resulting PNG margins.

The compact destination picker groups distinct offline layout contracts by Google Play,
Instagram, TikTok, WhatsApp and Facebook, followed by legacy generic layouts and Customize.
Existing catalog IDs remain stable, so remembered templates and saved work are not silently
reinterpreted. New social presets are layout guidance rather than upload certification. The
TikTok Content Posting API lists JPEG/WebP for photos, so the TikTok layout does not claim the
mandatory PNG default can be sent through that API. WhatsApp Status may compress media.
Google Play's portrait recommendation stays separate from its general phone screenshot contract.

Evidence: https://support.google.com/googleplay/android-developer/answer/9866151 ;
https://developers.tiktok.com/docs/en/content-posting-api-media-transfer-guide ;
https://faq.whatsapp.com/643144237275579/ .
Do not infer physical responsiveness, Play listing permission, actual social posting compatibility,
or production release approval from successful host/CI checks alone.

## Menu and draw-phase refinement

The subsequent [menu and interaction refinement](crop-interaction-and-destinations.md) preserves the stable UX2.1 IDs, matte semantics and excluded-source overview while moving transient crop reads entirely to the draw phase. Settings and destinations use coherent compact sheets. The isolated diagnostic package for that candidate is `.touch` / FixMyShot Touch; earlier `.automatic` APKs do not contain it. All existing privacy, fidelity, manual-device and release gates remain.
