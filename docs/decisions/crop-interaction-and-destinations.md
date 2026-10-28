# Menu and interaction refinement

This scoped refinement is based on `a6b80304944b94c669fe3a6f72cc4afb1d719715`, not the older accepted UX2.0 binary. It preserves all UX2.1 functionality, stable platform IDs, existing per-image matte semantics, whole-source crop inspection, seven earlier interaction cases, advanced editor and recovery behavior. The original complete first-functional blueprint is unchanged.

## Draw-only crop updates

The prior candidate isolated draft publication but still read transient focus during canvas composition and repeated planning there. `ReviewCanvas` now reads its transient `CropMotion` and position only inside its draw lambda. Pointer motion neither updates the parent composable nor rebuilds the draft/journal or full export preflight. `CropInteraction` retains the event-local exact geometry, computes the focal-point transform in the actual fitted frame, and invokes the unchanged CropPlanner once per move. The gesture commits one normalized focus at pointer-up; cancellation restores committed pixels without publishing. Changed identity, revision, settings or edit lock invalidates the local state. The expanded editor keeps its existing Apply/Cancel transaction and excluded-source overview.

The live renderer owns reusable matrix, rectangles and paint, never shares them across threads and uses exactly the existing source/output mapping and filtering rules. No output-size bitmap is allocated per event; export still reads the original full-resolution source. Fine arrows can move by one original pixel, while the previous coarse step and 2% relative zoom buttons remain available. This is an implementation strategy, not an independently measured physical speedup.

## Visual refinement without another editor

The selected destination remains one compact row. A bottom sheet groups the existing Google Play, Instagram, TikTok, WhatsApp and Facebook surfaces into expandable platform sections, with clear selection state, a small ratio badge, concise evidence-qualified notes and a Custom/Customize entry. Every previously committed ID and output contract is retained. The sheet does not convert photo layouts into video support or uploader certification. TikTok API JPEG/WebP and WhatsApp Status quality caveats remain explicit; PNG/privacy/full-content defaults and per-image exceptions remain unchanged.

Framing is one segmented control. The background choices become a readable two-column palette rather than four compressed buttons. Match edges still resolves the already implemented median vertical-edge RGB into the existing selected-image CustomRgb setting; it does not reinterpret Automatic drafts or pending retries, create gradients, invent content or mutate originals. Real Pixel source/export seams remain unaccepted. No decorative card or unverified Instagram 3:4 preset is silently introduced.

Settings uses the same sheet style, a fixed heading/close action, grouped theme and language controls, and expandable Help, Privacy and About. All original localized help, metadata descriptions, native diagnostics, source/license/author/donation links remain available. Existing brand artwork/colors and light/dark/system are retained. No new framework, fonts, dependencies, permissions, network, production identity or signing key.

## Validation

The first UX2.1 run `40760690325` at a6b80304 FAILED in one of 84 core tests: DraftCases still asserted the old six-entry catalog. Its actual artifact/source/JUnit receipt is retained as negative evidence; it is not an Android compile/runtime PASS. The corrected fixture checks the exact 14-entry catalog and uniqueness while retaining all original IDs, rejection and geometry assertions. The standalone runner now executes draft cases too, so it cannot miss that boundary again.

Five new CropInteractionTest cases add focal anchoring, 1001 repeated exact-planner comparisons, letterbox/exact-output behavior, native-pixel zoom limits, removed area and invalid-input checks. Expected core total: 89. Local Kotlin logic checks are distinct from JUnit/Android acceptance.

The editor suite retains the seven earlier cases including matte and excluded-source checks and adds three (10 total): compact platform/solid matte behavior; real pointer movement with zero parent publications/recompositions during motion, visible raster changes, one commit at release and correct cancellation; dark Settings/appearance/language/help/native-entry accessibility. Existing eight render cases additionally compare repeated cached-transform output pixels against ordinary previews. Current-profile PNG/WebP, metadata/privacy, source preservation, import/export/recovery and all previous requirements stay intact.

The pointer case records Android FrameMetrics as `SYNTHETIC_EMULATOR_DEBUG_NOT_PIXEL_ACCEPTANCE`, including `physicalBaselineAvailable=false`. It does not claim an on-device before/after comparison. Capture actual current-source screenshots for menus, Settings, framing and export before handoff. Missing images or failed tests remain missing/failing, not waived. Physical Pixel pan/pinch jank, latency/allocations, full-resolution PNG seams/text/colors, TalkBack, 200% text, landscape/keyboard, second hardware, native formats, 16KiB runtime, legacy SAF, authenticated provenance and production signing/distribution remain open.

The diagnostic package is `.touch` / FixMyShot Touch to avoid assuming ephemeral CI certificates can upgrade previous `.automatic` builds. Do not uninstall or overwrite older test apps automatically. No merge, tag, production signing or release follows from this iteration.

## Primary design references (checked 2026-10-28)

- https://developer.android.com/develop/ui/compose/performance/bestpractices : defer rapidly changing state reads to drawing; not measured evidence for this APK.
- https://support.google.com/googleplay/android-developer/answer/9866151 : upload constraints versus 9:16 recommendations.
- https://developers.tiktok.com/docs/en/content-posting-api-media-transfer-guide : photo API formats, not every manual uploader.
- https://faq.whatsapp.com/643144237275579/ : Status quality caveats. Social ratios remain layout guidance.
