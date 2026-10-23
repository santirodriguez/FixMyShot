# Contributing

## Bootstrap build

Use JDK 17 and Android SDK platform 36, build tools 35.0.0, NDK 28.2.13676358 and CMake 3.22.1. The initial candidate pins AGP 8.13.2, Gradle 8.13, Kotlin 2.2.21 and Compose BOM 2025.12.00. These are starting pins, not claims of current security approval or independently reproducible output. Review the actual resolved dependency/security inventory before freezing G1.

```sh
bash scripts/bootstrap-gradle.sh
python3 scripts/check-bootstrap.py
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest
```

The temporary bootstrap downloads the official Gradle distribution, verifies its published SHA-256 and generates/checks the standard wrapper in an isolated empty project. **Committing the reviewed standard wrapper, resolved lockfiles and verification metadata is still required before G1 closes.** CI captures those files as candidates and does not commit them or grant itself repository write access. Initial `--write-verification-metadata` output records observed bytes; it does not prove their trustworthiness. Replace trust-capture mode with strict committed verification after review.

HEIC/AVIF native writer feasibility uses the pinned AndroidX HeifWriter release candidate; inspect actual device output before advertising those formats. A bounded Photo Picker / received-share / app-private review import path exists, with immutable PNG/WebP preflight, a test-only private output executor, and an internal API29+ journaled MediaStore transaction adapter. An experimental user-triggered Export UI now uses the frozen sequential coordinator: owned MediaStore on API29+, explicit user-selected SAF folder on API26–28, localized progress and per-item results. Source-bound build/API26/API36 tests now cover synthetic export results, selected-output share payloads and read-only recovery checks; genuine selected SAF trees, external receiving apps and complete recovery remain unverified. See docs/TESTING.md for the exact receipt; this is not G4/release acceptance. No storage permission was added.

Native WebP is built from the immutable upstream revision in `codec-native/src/main/cpp/CMakeLists.txt`. Builds need network access to trusted artifact repositories and that source revision; the installed application does not. Never add opaque prebuilt codec binaries or runtime downloads.

## Development validation cadence

Work in coherent implementation batches instead of launching both Android emulators on every small commit. During a batch, use relevant source review, Kotlin/JVM or dependency-free Python checks, and other inexpensive targeted validation. Such checks are not equivalent to an Android build or physical-device acceptance.

The release branch runs the build and a representative API36 emulator only when a commit explicitly updates `.ci/android-validation-request.txt`. Include the batch purpose and the precise source state in that checkpoint commit; inspect that exact Actions run and source-bound artifacts before declaring the batch accepted. The last line of the marker selects the profile: `profile=current` runs the build and API36 regression block; `profile=extended` additionally runs ordinary API26 coverage and the dedicated API26/28 real SAF exercise. Pull requests use that same explicit profile. Extended runs are optional compatibility diagnostics, not the default development gate; known failures remain recorded rather than being relabelled as passes. The SAF phases check actual DocumentsUI selection/cancellation, persisted grants, PNG/WebP publication, completed-job restart and revoked reads. They do not replace receiver or physical-device acceptance and must only run through the guarded disposable-emulator script. A manual workflow-dispatch button is unavailable while this workflow does not exist on the default branch; do not merge prematurely just to expose one.

Do not write a checkpoint file at the start of development or for documentation-only commits. Rework related failures as one bounded slice, and honor the 5-minute normal/10-minute hard passive CI waiting limit. Preserve applicable hardware, codec, reproducibility and signing gates; legacy compatibility requirements are qualified by docs/decisions/compatibility-and-validation.md.


For the **isolated JXL 0.12 reference feasibility** run, review the pinned source-build helper and then update `.ci/jxl-probe-request.txt` on the release branch. The separate read-only Actions matrix builds the three native ABIs without launching full app/emulator CI; its library-size and ELF alignment reports do not certify a shipping encoder. Before interpreting the Gradle trust candidates, run `python3 scripts/test-gradle-candidates.py` and `python3 scripts/audit-gradle-candidates.py <source-bound-bootstrap-artifact.zip>`; generated hashes remain untrusted until independently corroborated. Details and actual experiment state are in `docs/feasibility/phase1-batch-preflight.md`.

## Ownership

- `core`: platform-independent format, eligibility and resource contracts.
- `codec-native`: source-built WebP/JNI, bounded lossless encoding and a separate native HEIC/AVIF writer feasibility adapter.
- `app`: Compose Photo Picker/review UI, bounded staged-source reader, 24 MiB thumbnail cache, localized resources and Android fixture tests.

The encoder accepts only already-normalized 8-bit sRGB bitmaps and returns an app-private candidate. It is not the import, metadata-preservation, geometry or public-export pipeline. Call it off the main thread, preserve its sequential execution and dispose of candidates explicitly.

## Translations and reports

Android XML resources are canonical. `python3 scripts/check-bootstrap.py` checks the currently implemented keys (150 in the output-controls implementation), duplicate/missing entries, placeholders, XML and bootstrap permissions without Android Studio. The check also compares the packaged locale configuration with the language catalog; full future plural/array/markup and APK-language review remains open. Use Argentine Spanish and natural Catalan; automated structure checks do not replace linguistic review.

Keep implementation and technical documentation in English. Do not include private screenshots, source filenames, content URIs, credentials or personal metadata in reports. Use the synthetic harness and identify the exact source SHA, API, device and check performed. See [TESTING](docs/TESTING.md) for acceptance boundaries.


## Appearance and language

`settings/AppPreferences.kt` stores only theme in Preferences DataStore (1.1.7). AppCompat (1.7.1) owns language persistence on older Android. API33+ uses LocaleManager from the actual Activity context for selection and platform storage, through settings/AppLanguages.kt; no static delegate is used to find a possibly stale Activity service. Do not add a second language preference or persist metadata opt-out/output format as a new-session default. These stable versions deliberately fit the existing toolchain; they are not latest-version claims. Language resources remain offline and settings changes retain the ViewModel draft. The original flag vectors have provenance in THIRD_PARTY_NOTICES.

## Crop rendering

Preserve-pixels crop freezes the source rectangle from CropPlanner in PrivateOutputRecipe. ReviewBitmapRenderer.drawContent maps that same rectangle from the sampled preview or full original into its destination. Integer-aligned 1:1 placements are unfiltered; actual fractional/scale changes are filtered once. The original staged image is never cropped in place. Contain stays the new-session default. Crop gestures and directional/zoom/reset controls change only the selected image transform; batch fit changes preserve per-image overrides. Exact-size crop now uses the same source rectangle and renderer with an explicit target raster. If it exceeds the visible source resolution, preflight blocks until enlargement is explicitly enabled.


## Export recovery and deliberate retry

New jobs atomically persist ExportDraftJournal before batch publication can begin. It stores bounded, versioned private staged references and the exact destination/batch/per-item fields, including inheritance and crop/size/metadata policy. It excludes undo snapshots and image bytes. Restoration verifies catalog/revision/item/hash identity and never silently replaces a non-empty draft. Old jobs without this journal cannot be reconstructed from the current draft.

BatchRetryPolicy permits only no-output states: a failed/cancelled/unstarted item with no URI and either no transaction receipt or a terminal failed/cancelled receipt with no remaining URI. Reserved, visible, published, indeterminate, corrupt or mismatched transactions are not retryable. Only a user-requested target set is reconsidered under the job's OS file lock. Published item positions/receipts remain; eligible terminal receipts are cleared only after durable explicit retry intent. New attempts get new output transaction names. OS locks release on process death; keep the lock-file inode after release so two writers cannot lock different inodes. No startup action creates, publishes or deletes external output.

The grouped Android block includes the seven-case BatchRetryFlowTest plus a real owned MediaStore/ViewModel restart/restore/retry case in ReviewExportFlowTest (now four). Keep collector and mock counts aligned. Journal readers now bound bytes before full allocation. Full mid-write crash/device/low-space/manual receiver and cleanup acceptance remain open.

FrozenPlanFingerprint binds geometry, background choice, crop rectangle, output dimensions, codec/format/alpha/metadata and source/revision identity to the frozen journal. Render-policy semantic changes must bump its policy version. Dynamic heap estimates are deliberately excluded and rechecked separately. Missing/mismatched plan identity permits review/restoration but forbids retry; never silently recompute an old export under changed output semantics.


## Output review and private cleanup

ExportRecoveryActions opens only a matching, visible recorded output through ACTION_VIEW and a temporary read grant. Merely reopening or rechecking never changes publication state. After a separate explicit confirmation, the same job OS lock and a fresh digest/provider check permit recording that existing output as PUBLISHED. Missing, changed, pending, conflicting or inaccessible outputs remain unresolved; no provider write, delete or duplicate export occurs.

PrivateSessionMaintenance protects current draft IDs and every unreviewed job. Corrupt/unknown recovery state blocks source cleanup. Explicit review removes the frozen private settings/URI journal under the job lock, releases unused private sources and retains output receipts plus the stable lock inode. Current draft sources remain until removal/discard; new sessions keep PNG/metadata-removal defaults. Startup retries interrupted reviewed-draft cleanup and removes abandoned sources/candidates older than24h, before imports start. There is no secure-erasure or exact-time background deletion promise. Output receipts currently remain until app data is cleared/uninstalled; full receipt retirement and unresolved provider-output cleanup remain open.

The grouped recovery suite now has16 cases, including four output-action and four private-maintenance cases. Keep the collector and its host mock in sync. These tests do not replace external-viewer delivery, complete process-kill, hardware or low-space acceptance.

## Output controls and undo ownership

Review offers a manual six-digit RGB background, preserve-pixels or explicit output dimensions, and opt-in uniform enlargement. Batch versus selected scope reads the corresponding settings; global changes retain individual overrides. Exact crop derives its frame from the exact target aspect and reuses the same final renderer; incompatible destination dimensions remain conflicts. Target pixels, source bounds and dynamic memory remain checked. The UI discloses reduction/enlargement before Export.

Applying all batch settings requires confirmation of affected custom-image count and all replaced fields, with one-step undo. Reordering uses stable IDs. DraftSession.workingSourceIds includes both current images and the undo snapshot: remove never deletes its source until undo is replaced or the session is explicitly discarded. Undo remains available with an empty current list. Imports, startup pruning and result cleanup protect those working IDs. Unsaved drafts/undo still do not survive process death.

No existing accepted preserve-pixels rendering semantics changed. Old exact-crop jobs were blocked and could not be exported; the new exact output geometry is already covered by frozen-plan fingerprints. The render-policy version is not bumped just for these newly eligible plans. Runtime acceptance of the new block must come from its exact source checkpoint.

## Payload-preserving exports

PayloadPreservingCopy is a bounded PNG/WebP container sanitizer before the renderer, not a separate codec. Only unchanged normal-orientation geometry, matching format/alpha and accepted sRGB descriptors qualify. PNG IDAT and WebP VP8L bytes remain intact; identifying metadata is removed. Unknown color/profile or unsupported structure falls back to ordinary validated rendering; corrupt/animated/HDR-signalled imports are rejected where detected. Explicit preservation goes through MetadataAllowlist after fresh pixel encoding; it never bypasses the renderer or copies an opaque source metadata block. Native candidates, source fingerprints and public transactional output checks are not bypassed.

ReviewSources reads EXIF orientation from JPEG/PNG/WebP using the existing pinned AndroidX dependency; do not reapply it through an auto-orienting decoder. Render-policy version2 invalidates old frozen-plan retries while preserving review/restoration. The original files are never changed. Four new payload/orientation cases join the private executor suite, now12 total; host simulations do not establish Android acceptance. Primary container references: https://www.w3.org/TR/png-3/ and https://developers.google.com/speed/webp/docs/riff_container (checked2026-10-23).


MetadataAllowlist owns the16 supported source fields documented in docs/FORMATS.md. Keep AndroidX ExifInterface pinned; do not expand the tag set implicitly or transfer thumbnails, serials, XMP/IPTC, comments or original paths. ExifAllowlistStructure independently validates bounded TIFF directories and normalized dimensions/orientation; PNG/WebP parsers permit EXIF only for PRESERVE_ALLOWLIST. The low-level LosslessEncoder still refuses preservation requests because it has no source metadata. A failed writer/reopen/container check must delete the private candidate before provider publication. New-session metadata removal is always enabled; preserve per-item inheritance and frozen-plan identity.

AndroidX can synthesize Orientation=0 for a source without an explicit tag. ReviewSources maps only this undefined value to identity;1–8 retain their existing transforms and other values remain invalid. PayloadCopyTest exercises a plain provider import/full decode before all eight explicit PNG orientations. MetadataPolicyTest covers actual PNG/WebP preservation and JPEG conversion, default removal, forbidden-field injection and bounded TIFF rejection; the combined private suite requires15 cases.


ReviewSessionJournal stores one bounded4MiB atomic editable-session document plus one undo snapshot, using the shared ExportDraftJournal field codec. Frozen export schema1 and plan identity remain unchanged; empty image arrays are allowed only for the editable/current/undo codec calls. Keep process-wide journal serialization because AtomicFile requires caller coordination. Saved IDs must be private32-hex source IDs with hashes; duplicate undo/current identities must match their immutable source attributes.

MainActivity attaches editable persistence to its ViewModel. Gesture/selection/import edits coalesce into the latest immutable checkpoint on the existing serial worker; private cleanup/export work uses that same queue. Restore and check staged-file ownership/size before pruning, protect saved current+undo references, and recheck actual hashes through the existing export executor before any output. A closed session older than24h is expired; current in-process sessions remain protected. A corrupt/missing saved session blocks import/edit replacement until explicit discard, while export-recovery review remains available. A failed checkpoint retains protection for the last saved references; never clean those bytes merely because memory moved on. Do not log source URIs or serialized sessions. No background/secure-erasure guarantee or automatic export/retry is introduced.

ReviewSessionTest adds4 isolated-context/new-ViewModel cases: current/undo/settings/selection and coalesced last-edit restoration, old undo-source survival, matching frozen-job duplicate barrier and explicit discard/default reset, expired/missing/malformed/oversized state, and a queued offer against a restored100-image draft. Import/settings collector requires9 total (prior5+new4); existing Activity fixtures explicitly discard their own synthetic persisted sessions. This is fresh ViewModel/disk recovery evidence, not a claim of OS force-stop/mid-write crash acceptance.


Foreground export now has a separate latched pause flag, not the cancellation flag. Activity.onStop outside configuration changes requests pause; finish the current transaction and mark only untouched targets CANCELLED/PAUSED, leaving committed receipts unchanged. Returning to the Activity must not clear an active pause request. Explicit continuation creates the normal verified remaining-item retry. A full successful matching session can be explicitly finished: acknowledge first, clear its editable journal, release its private current/undo/frozen sources under existing protections, then reset UI/defaults. Partial/uncertain/unrelated drafts never take this finish path; provider outputs and stable lock/receipt files remain.

Development commits do not require CI. Keep .ci/android-validation-request.txt unchanged until an explicit larger accumulated checkpoint. Automatic locale recreation is an opt-in diagnostic with instrumentation argument localeDiagnostics=true; default coverage still verifies import/recreation and all locale resources, while logs explicitly report locale switching NOT_RUN. Do not present the deferred diagnostic as passing. The retained strict assertions still run when explicitly requested.


SourceAppearance validates bounded input ICC structure and agreement with the decoder before accepting recognized Display P3/Adobe RGB SDR conversion. Do not replace BitmapFactory color conversion with Bitmap.setColorSpace or remove a profile while retaining unconverted channel values. SourceColor is immutable source identity: carry it through editable/frozen journals and preparation, verify it before full decode, show conversion before export and disable payload copy for converted inputs. Existing sRGB version2 fingerprints remain unchanged; newly eligible wide-gamut plans append a named policy extension. Unknown legacy values fail closed, while absent schema1 color defaults to SRGB. Future changes to already executable color semantics require fingerprint invalidation.

SourceAppearanceTest adds4 cases to the private group (now19): recognized RGB profiles in three real containers and both codecs against independent numeric colors, malformed/unsupported descriptors, journal/retry identity, and partial-alpha/native/enlarged compositing over light/dark/transparent backgrounds. The collector/mock count is aligned. Keep the CI request markers unchanged while accumulating implementation. Local fixture checks are not Android execution or hardware acceptance. See FORMATS/TESTING for exact support and limits.


## Import and export feedback

ImportReport describes the latest completed staging queue in received order, at most100 items, retaining only counts/positions and typed failures. ReviewViewModel records it after filtering actual accepted IDs; duplicates are ignored rather than counted as corrupt images. Prior sender-level failures retain their generic summary. A new completed queue replaces the detailed report; dismiss, discard or successful finish clears feedback without mutating source/output state. Do not persist the report or put filenames/URIs/exception text in it.

SourceAppearance.Rejected adds typed diagnostic categories for malformed/color/animation/high-precision/HDR inputs without changing the allowed appearance/pixel path. BatchItemProcessor preserves useful bounded codec failures (timeout/resource/unavailable/I/O/metadata/output validation) before the provider boundary. ResultFeedback maps these and existing journal states to resources; NEEDS_REVIEW overrides retry-like reason strings. Never use this presentation mapper to authorize retry/publication. The existing proof-bound retry policy and receipts remain authoritative. Preflight carries its blocked item IDs/reasons into the UI, with selection controls; editing resets stale blockers.

ResultsFeedbackTest adds4 cases to the import/settings/session group, now14. Private-output remains19, retry8 and recovery16. The existing import/recreation and appearance-descriptor tests also gain scoped assertions. Keep the collector/mock aligned. Resource structure and host collector checks do not compile or execute Android UI/storage/codec code. The new diagnostics/lifecycle/color blocks await one later accumulated checkpoint; do not update CI trigger markers for ordinary development commits.


Unused-alpha WebP acceptance requires a full rendered-canvas pixel scan, not source/preview sampling or a Bitmap.hasAlpha hint. PrivateOutputExecutor alone produces opaqueWebpProven evidence, carried with Ready through the strict verifier and transactional saver. Restrict it to WEBP_LOSSLESS with an alpha-capable request; PNG and requested-opaque contracts remain strict. The scan uses16KiB tiles with cancellation/deadline and rejects any nonopaque pixel. Recovery still binds provider digests and never authorizes a retry from this flag. Keep the new proof/refusal/publication case in SourceAppearanceTest (private group now20).

The alpha appearance oracle compares exact alpha and premultiplied visible channels within one8-bit level, plus explicit clear/opaque regions and light/dark compositing. Bitmap.getPixel returns straight sRGB channels; direct straight-RGB comparisons amplify quantization at low alpha (https://developer.android.com/reference/android/graphics/Bitmap#getPixel(int,int)). This does not weaken codec exact-pixel tests or permit flattening. ResultsFeedbackTest removes its already-deleted missing-URI fixture from the cleanup ownership set, retaining all other cleanup assertions; do not blanket-catch deletion failures or add permissions.
