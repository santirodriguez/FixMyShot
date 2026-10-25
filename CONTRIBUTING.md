# Contributing

## Release preparation

Run `python3 scripts/check-release.py` for the checked version ledger, source identity and release-note fields. `python3 scripts/test-release.py` exercises its offline refusal boundaries. See [RELEASING](docs/RELEASING.md) for the notes/build-receipt templates, exact artifact naming, reproducibility, signature and Obtainium checks. These source checks do not inspect or approve a signed APK and do not publish anything.

## Bootstrap build

Use JDK 17 and Android SDK platform 36, build tools 36.0.0, NDK 28.2.13676358 and CMake 3.22.1. The migrated candidate pins AGP 9.2.1, Gradle 9.4.1, Kotlin 2.4.21 and Compose BOM 2025.12.00. These are starting pins, not claims of current security approval or independently reproducible output. Review the actual resolved dependency/security inventory before freezing G1.

```sh
bash scripts/bootstrap-gradle.sh
python3 scripts/check-bootstrap.py
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest
```

The standard Gradle9.4.1 wrapper and three resolved version lockfiles are committed. `bootstrap-gradle.sh` checks the wrapper JAR, launcher bytes and distribution properties before execution; it does not regenerate or download a missing replacement. `./gradlew` performs the normal checksum-verified distribution download when needed. Integration builds respect the committed lock state and do not pass `--write-locks`; deliberate dependency changes require a reviewed update. These version locks do not authenticate dependency artifacts. Generated `--write-verification-metadata` output remains unapproved trust capture: independent artifact authentication, reviewed strict verification and an independent clean rebuild are still required before G1 closes. See [committed Gradle inputs](docs/feasibility/committed-gradle-inputs.md) for exact source/hash boundaries and offline checks.

HEIC/AVIF native writer feasibility uses the pinned AndroidX HeifWriter release candidate; inspect actual device output before advertising those formats. A bounded Photo Picker / received-share / app-private review import path exists, with immutable PNG/WebP preflight, a test-only private output executor, and an internal API29+ journaled MediaStore transaction adapter. An experimental user-triggered Export UI now uses the frozen sequential coordinator: owned MediaStore on API29+, explicit user-selected SAF folder on API26–28, localized progress and per-item results. Source-bound build/API26/API36 tests now cover synthetic export results, selected-output share payloads and read-only recovery checks; genuine selected SAF trees, external receiving apps and complete recovery remain unverified. See docs/TESTING.md for the exact receipt; this is not G4/release acceptance. No storage permission was added.

Native WebP is built from the immutable upstream revision in `codec-native/src/main/cpp/CMakeLists.txt`. Builds need network access to trusted artifact repositories and that source revision; the installed application does not. Never add opaque prebuilt codec binaries or runtime downloads.

## Development validation cadence

Work in coherent implementation batches instead of launching both Android emulators on every small commit. During a batch, use relevant source review, Kotlin/JVM or dependency-free Python checks, and other inexpensive targeted validation. Such checks are not equivalent to an Android build or physical-device acceptance.

The release branch runs the build and a representative API36 emulator only when a commit explicitly updates `.ci/android-validation-request.txt`. Include the batch purpose and the precise source state in that checkpoint commit; inspect that exact Actions run and source-bound artifacts before declaring the batch accepted. The last line of the marker selects the profile: `profile=current` runs the build and API36 regression block; `profile=extended` additionally runs ordinary API26 coverage and the dedicated API26/28 real SAF exercise. Pull requests use that same explicit profile. Extended runs are optional compatibility diagnostics, not the default development gate; known failures remain recorded rather than being relabelled as passes. The SAF phases check actual DocumentsUI selection/cancellation, persisted grants, PNG/WebP publication, completed-job restart and revoked reads. They do not replace receiver or physical-device acceptance and must only run through the guarded disposable-emulator script. A manual workflow-dispatch button is unavailable while this workflow does not exist on the default branch; do not merge prematurely just to expose one.

Do not write a checkpoint file at the start of development or for documentation-only commits. Rework related failures as one bounded slice, and honor the 5-minute normal/10-minute hard passive CI waiting limit. Preserve applicable hardware, codec, reproducibility and signing gates; legacy compatibility requirements are qualified by docs/decisions/compatibility-and-validation.md.


JXL is [deferred for v0.1.0](docs/decisions/jxl-first-delivery.md) after the completed three-ABI reference experiment. Keep its opt-in helper/workflow as historical tooling; do not trigger another reference build as part of ordinary app work. Any later scoped reconsideration must measure the actual optimized package/runtime costs. Before interpreting Gradle trust candidates, run `python3 scripts/test-gradle-candidates.py` and `python3 scripts/audit-gradle-candidates.py <source-bound-bootstrap-artifact.zip>`; generated hashes remain untrusted until independently corroborated. The source-built JXL reports and their binary/measurement limits remain in `docs/feasibility/phase1-batch-preflight.md`.

## Ownership

- `core`: platform-independent format, eligibility and resource contracts.
- `codec-native`: source-built WebP/JNI, bounded lossless encoding and a separate native HEIC/AVIF writer feasibility adapter.
- `app`: Compose Photo Picker/review UI, bounded staged-source reader, 24 MiB thumbnail cache, localized resources and Android fixture tests.

The encoder accepts only already-normalized 8-bit sRGB bitmaps and returns an app-private candidate. It is not the import, metadata-preservation, geometry or public-export pipeline. Call it off the main thread, preserve its sequential execution and dispose of candidates explicitly.

## Translations and reports

Android XML resources are canonical. `python3 scripts/check-bootstrap.py` checks the currently implemented keys (236 in the current implementation), duplicate/missing entries, placeholders, XML and bootstrap permissions without Android Studio. The check also compares the packaged locale configuration with the language catalog; full future plural/array/markup and APK-language review remains open. Use Argentine Spanish and natural Catalan; automated structure checks do not replace linguistic review.

Keep implementation and technical documentation in English. Do not include private screenshots, source filenames, content URIs, credentials or personal metadata in reports. Use the synthetic harness and identify the exact source SHA, API, device and check performed. See [TESTING](docs/TESTING.md) for acceptance boundaries.


## Appearance and language

### Review editing context

The ViewModel retains the batch/selected-image scope and an editor-session token across Activity recreation. Only the small pending ratio, RGB, size and enlargement fields use Compose saved instance state, nested under that token, the active image/scope and the relevant applied value. Do not save sources, grants, plans or bitmaps in this state. Changing image/scope drops unapplied fields; applying a different value reseeds its fields. Custom-ratio fields start from the actual applied ratio. Destination remains batch-wide; per-image overrides remain authoritative.

Discard/finish resets the editor token and scope. A fresh ViewModel also starts a fresh editor context, so journal restoration recovers applied settings without reviving old uncommitted fields. Removing the last image keeps the context while undo still owns sources. Keep scope changes and draft/removal callbacks guarded during import/share or blocked/locked work; selected-only callbacks must refuse a missing selection rather than editing the batch. Confirmation dialogs remain short-lived and are not restored. See [Compose state saving](https://developer.android.com/develop/ui/compose/state-saving) and [rememberSaveable](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/package-summary): restoration does not revalidate remembered inputs, so explicit composition keys define these boundaries.

`settings/AppPreferences.kt` stores only theme in Preferences DataStore (1.1.7). AppCompat (1.7.1) owns language persistence on older Android. API33+ uses LocaleManager from the actual Activity context for selection and platform storage, through settings/AppLanguages.kt; no static delegate is used to find a possibly stale Activity service. Do not add a second language preference or persist metadata opt-out/output format as a new-session default. These stable versions deliberately fit the existing toolchain; they are not latest-version claims. Language resources remain offline and settings changes retain the ViewModel draft. The original flag vectors have provenance in THIRD_PARTY_NOTICES.

## Crop rendering

Preserve-pixels crop freezes the source rectangle from CropPlanner in PrivateOutputRecipe. ReviewBitmapRenderer.drawContent maps that same rectangle from the sampled preview or full original into its destination. Integer-aligned 1:1 placements are unfiltered; actual fractional/scale changes are filtered once. The original staged image is never cropped in place. Contain stays the new-session default. Crop gestures and directional/zoom/reset controls change only the selected image transform; batch fit changes preserve per-image overrides. Exact-size crop now uses the same source rectangle and renderer with an explicit target raster. If it exceeds the visible source resolution, preflight blocks until enlargement is explicitly enabled.


## Export recovery and deliberate retry

The output-port creation contract is strict: returning null means no owned output remains; throwing can mean allocation happened without a returned URI. TransactionalOutputSaver keeps the durable RESERVED receipt and returns an indeterminate result for a throwing create, without guessing a URI, deleting by filename or allowing retry. Recovery labels that reservation OUTPUT_CREATION_UNCERTAIN and leaves the existing no-output proof policy in charge. SAF throws for an unexpected URI without touching it, and a wrong/unconfirmed name may return null only after confirmed cleanup of the newly created in-tree document. Catch ordinary provider RuntimeException at the transaction boundary: a known URI before publication follows owned cleanup; failed cleanup stays indeterminate; any attempted publication stays indeterminate without deletion. Never catch fatal VM errors, infer success from matching bytes or automatically retry uncertain creation. Existing schema/frozen render identity stays unchanged.

OutputStorage reads non-root available bytes from the already-open regular-file descriptor using [Os.fstatvfs](https://developer.android.com/reference/android/system/Os). Before copying and roughly every 1 MiB, TransactionalOutputSaver requires the remaining measured candidate bytes plus 16 MiB headroom. This is advisory, not a disk reservation or provider quota guarantee. Pipes, virtual/closed descriptors and unavailable/invalid/overflowing statistics remain unknown; ordinary writes, close and full digest reopening still decide. A known shortage uses LOW_STORAGE and the existing owned-output cleanup; failed cleanup remains indeterminate and nonretryable. No original or previously published output is removed. The availability callback is a test seam, not a user setting.

The production private executor also checks a minimum 16 MiB working reserve in its configured candidate directory before creating a payload-copy temporary file or decoding/rendering. It rechecks before metadata rewriting and, best effort, when an encoder reports I/O failure. PRIVATE_STORAGE_LOW is distinct from destination LOW_STORAGE and does not imply a source-image problem. Unknown statistics do not reject an export; this is not a predicted encoded-size reservation. Real allocation failures, metadata rewrites and concurrent disk use still require ordinary failure handling. Isolated executors without a known copyDirectory cannot assert this directory preflight. Keep candidate cleanup scoped to the current attempt, never discard existing sources/other candidates or silently lower quality.


Flush/close still applies to every stream. Sync only regular file descriptors: a ParcelFileDescriptor.AutoCloseOutputStream can wrap a pipe, on which fsync is invalid. Regular files retain mandatory sync; a descriptor-inspection failure there becomes a typed I/O failure, not success. Skipping pipe sync never skips the post-close digest verification or publication journal. This does not qualify remote/cloud SAF providers or promise cancellable blocked provider I/O.


New jobs atomically persist ExportDraftJournal before batch publication can begin. It stores bounded, versioned private staged references and the exact destination/batch/per-item fields, including inheritance and crop/size/metadata policy. It excludes undo snapshots and image bytes. Restoration verifies catalog/revision/item/hash identity and never silently replaces a non-empty draft. Old jobs without this journal cannot be reconstructed from the current draft.

BatchRetryPolicy permits only no-output states: a failed/cancelled/unstarted item with no URI and either no transaction receipt or a terminal failed/cancelled receipt with no remaining URI. Reserved, visible, published, indeterminate, corrupt or mismatched transactions are not retryable. Only a user-requested target set is reconsidered under the job's OS file lock. Published item positions/receipts remain; eligible terminal receipts are cleared only after durable explicit retry intent. New attempts get new output transaction names. OS locks release on process death; keep the lock-file inode after release so two writers cannot lock different inodes. No startup action creates, publishes or deletes external output.

The grouped Android block includes eight BatchRetryFlowTest cases plus real owned MediaStore/ViewModel restart/restore/retry and pending-output-removal cases in ReviewExportFlowTest (five). HundredItemBatchTest adds two bounded 100-item exercises through the real PNG/WebP renderer, encoder, transaction saver and journals, with an in-memory provider for explicit write/publication faults. It checks cancellation/reopened-coordinator retry, unchanged committed receipts/pixels, uncertain-output refusal and intact sources. The batch collector requires seven cases (five orchestration plus two real-codec batch cases); keep its mock aligned. These prepared tests are not physical process-kill, low-space or large-screenshot performance acceptance; the hundred-item provider is simulated. Journal readers bound bytes before full allocation; broader gates remain open.

FrozenPlanFingerprint binds geometry, background choice, crop rectangle, output dimensions, codec/format/alpha/metadata and source/revision identity to the frozen journal. Render-policy semantic changes must bump its policy version. Dynamic heap estimates are deliberately excluded and rechecked separately. Missing/mismatched plan identity permits review/restoration but forbids retry; never silently recompute an old export under changed output semantics.


## Output review and private cleanup

ExportRecoveryActions opens only a matching, visible recorded output through ACTION_VIEW and a temporary read grant. Merely reopening or rechecking never changes publication state. After a separate explicit confirmation, the same job OS lock and a fresh digest/provider check permit recording that existing output as PUBLISHED. Missing, changed, pending, conflicting or inaccessible outputs remain unresolved; no provider write, delete or duplicate export occurs.

Pending output removal is a separate explicit action after read-only recheck reports OUTPUT_PENDING. It requires its own confirmation and the existing job lock, safe private journal paths, a SAVING/NEEDS_REVIEW batch item and matching PENDING/VALIDATED/INDETERMINATE output receipt. PendingMediaStoreRemoval accepts only an API29+ external_primary image-item URI. Query IS_PENDING=1 plus this package, exact recorded name/MIME and Pictures/FixMyShot; repeat every predicate in the deletion selection and require exactly one deleted row. NULL/unknown ownership, foreign/SAF/collection URIs, published/changed/inaccessible states and zero/failed deletion do not authorize a retry. Never replace the filtered delete with the ordinary port's unconditional delete. See [MediaColumns](https://developer.android.com/reference/android/provider/MediaStore.MediaColumns) and [ContentResolver.delete](https://developer.android.com/reference/android/content/ContentResolver#delete(android.net.Uri,%20java.lang.String,%20java.lang.String[])) (checked2026-10-23).

After positive deletion, write the item receipt FAILED/no URI, then the batch item FAILED/no URI/UNFINISHED_REMOVED; the existing no-output/frozen-plan retry policy remains authoritative. No new export starts. A provider exception or crash between provider deletion and durable receipt updates can leave conservative review-only state; missing rows alone are not deletion proof. Rechecking, startup, acknowledgement and successful-receipt retirement never invoke this provider action. The API29+ ViewModel integration adds one case to export-flow (now5), plus existing recovery4 cases gain refusal checks; keep collector/mock counts aligned. These cases and reviewed-job retirement passed the scoped639c5cad API36 run; its strict partial-alpha failure prevents aggregate acceptance. See TESTING for exact limits.

PrivateSessionMaintenance protects current draft IDs and every unreviewed job. Corrupt/unknown recovery state blocks source cleanup. Explicit review removes the frozen private settings/URI journal under the job lock and releases unused private sources. Current draft sources remain until removal/discard; new sessions keep PNG/metadata-removal defaults. Startup retries interrupted reviewed-draft cleanup and removes abandoned sources/candidates older than24h, before imports start. There is no secure-erasure or exact-time background deletion promise.

ReviewedJobRetirement retires only durably acknowledged, entirely PUBLISHED batches with matching published item IDs/URIs and no remaining frozen draft. Under the existing job lock, validate direct private ownership, recognized regular files and all receipts, then rename the whole directory to the sibling export-jobs-retired root. Close the original stable lock inode before deleting the retired files. Never delete individual committed receipts while their directory is still visible to the recovery scanner. A process-wide mutex coordinates retirement/remnant cleanup; bounded startup cleanup retries directly owned remnants, retaining an empty directory if preference cleanup cannot commit. Remove the acknowledgement only after all retired files are gone. This is private state disposal, not a fresh provider/pixel verification or a guarantee of directory fsync/secure erasure. Partial, uncertain, mismatched, corrupt, locked or unrecognized jobs retain their receipts; complete retirement of those states and unresolved provider-output cleanup remain open. Neither this class nor startup cleanup accesses or mutates public output.

The grouped recovery suite now has16 cases, including four output-action and four private-maintenance cases. Keep the collector and its host mock in sync. These tests do not replace external-viewer delivery, complete process-kill, hardware or low-space acceptance.

## Output controls and undo ownership

Preview-only work uses LatestPreviewWorker: one in-flight task and at most the latest pending task. Pending closures must not acquire bitmaps or completion promises before execution, and this replaceable queue must never run export, import, checkpoint or cleanup work. Invalidate the volatile generation before clearing pending previews; check it before decode, before render, after render and again on main-thread delivery with selected ID/revision. Recycle a stale rendered candidate; always release its sampled source. Closing interrupts best effort and drops queued work, without promising native decode cancellation. ReviewRenderTest now has6 cases (4 rendering plus2 queue burst/shutdown cases); collector and mock must agree. No latency/memory/device acceptance follows from source review.

Session discard and apply-all confirmations retain the exact immutable DraftSession and reset on draft changes. Pass that expected draft into the Activity callback and reject changed/busy/locked state before mutation. Discard retains existing journal/source-protection cleanup, never deletes originals/public output and does not acknowledge uncertain jobs. Share selection is one labelled full-row checkbox target; crop zoom semantics announce the actual zoom, not the slider's relative position in its range.

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


Foreground export now has a separate latched pause flag, not the cancellation flag. Activity.onStop outside configuration changes requests pause; finish the current transaction and mark only untouched targets CANCELLED/PAUSED, leaving committed receipts unchanged. Returning to the Activity must not clear an active pause request. Explicit continuation creates the normal verified remaining-item retry. A full successful matching session can be explicitly finished: acknowledge first, clear its editable journal, release its private current/undo/frozen sources under existing protections, retire its fully published receipts, then reset UI/defaults. Partial/uncertain/unrelated drafts never take this finish path; their receipt/lock protection and all provider outputs remain.

Development commits do not require CI. Keep .ci/android-validation-request.txt unchanged until an explicit larger accumulated checkpoint. Automatic locale recreation is an opt-in diagnostic with instrumentation argument localeDiagnostics=true; default coverage still verifies import/recreation and all locale resources, while logs explicitly report locale switching NOT_RUN. Do not present the deferred diagnostic as passing. The retained strict assertions still run when explicitly requested.


SourceAppearance validates bounded input ICC structure and agreement with the decoder before accepting recognized Display P3/Adobe RGB SDR conversion. Do not replace BitmapFactory color conversion with Bitmap.setColorSpace or remove a profile while retaining unconverted channel values. SourceColor is immutable source identity: carry it through editable/frozen journals and preparation, verify it before full decode, show conversion before export and disable payload copy for converted inputs. Existing sRGB version2 fingerprints remain unchanged; newly eligible wide-gamut plans append a named policy extension. Unknown legacy values fail closed, while absent schema1 color defaults to SRGB. Future changes to already executable color semantics require fingerprint invalidation.

SourceAppearanceTest adds4 cases to the private group (now19): recognized RGB profiles in three real containers and both codecs against independent numeric colors, malformed/unsupported descriptors, journal/retry identity, and partial-alpha/native/enlarged compositing over light/dark/transparent backgrounds. The collector/mock count is aligned. Keep the CI request markers unchanged while accumulating implementation. Local fixture checks are not Android execution or hardware acceptance. See FORMATS/TESTING for exact support and limits.


## Import and export feedback

Private staging checks [app-available filesystem bytes](https://developer.android.com/reference/android/os/StatFs#getAvailableBytes()) before opening the provider and again roughly every1MiB copied, reserving16MiB plus the next1MiB interval. This is advisory headroom, not a reservation or a guarantee against concurrent disk use. If the filesystem query is unavailable, normal bounded I/O still decides; do not request a storage permission or remove existing sources to make room. A known shortage returns LOW_STORAGE, removes only the rejected private copy through the existing cleanup path, and maps to a localized reselect-after-freeing-space hint. Accepted images and originals stay intact; no automatic retry is added. The100MiB source and1GiB staging caps remain separate. The injected availability reader is a test seam, not a user setting.

ImportProgress is ViewModel-owned memory state with processed/total/stopping counts. stopImport requests a cooperative boundary after the current stage call and clears offers already waiting at that tap; keep completed staging results and count untouched active/queued offers in ImportReport.notAttempted, never as corrupt/failed inputs. Later explicit offers are separate imports. Do not increment importGeneration for Stop: that existing generation invalidates the whole result only for discard/clear. Worker completion clears progress/stop state, and onCleared requests stop before normal shutdown. No deadline/forced interruption of a blocked provider is promised. All input cap/dedup/private-source protections remain.

ReviewSources.stage contains directory access inside its typed failure boundary and maps unexpected provider RuntimeException to UNREADABLE without logging exception text. Failed private-copy cleanup is best effort; normal conservative abandoned-source pruning still applies. Do not catch fatal VM errors or silently accept an unsupported image. Two ResultsFeedbackTest cases add controlled in-flight cancellation plus queued offers/explicit later import and cache/provider-failure completion. With the added storage-headroom case, import/settings group is now21; renderer6/private20/recovery16/batch7 and other counts remain. The Activity cleanup test must reflect discardDraft(DraftSession) with the exact current fixture draft.

ImportReport describes the latest completed staging queue in received order, at most100 items, retaining only counts/positions and typed failures. ReviewViewModel records it after filtering actual accepted IDs; duplicates are ignored rather than counted as corrupt images. Prior sender-level failures retain their generic summary. A new completed queue replaces the detailed report; dismiss, discard or successful finish clears feedback without mutating source/output state. Do not persist the report or put filenames/URIs/exception text in it.

SourceAppearance.Rejected adds typed diagnostic categories for malformed/color/animation/high-precision/HDR inputs without changing the allowed appearance/pixel path. BatchItemProcessor preserves useful bounded codec failures (timeout/resource/unavailable/I/O/metadata/output validation) before the provider boundary. ResultFeedback maps these and existing journal states to resources; NEEDS_REVIEW overrides retry-like reason strings. Never use this presentation mapper to authorize retry/publication. The existing proof-bound retry policy and receipts remain authoritative. Preflight carries its blocked item IDs/reasons into the UI, with selection controls; editing resets stale blockers.

ResultsFeedbackTest adds4 cases to the import/settings/session group, now14. Private-output remains19, retry8 and recovery16. The existing import/recreation and appearance-descriptor tests also gain scoped assertions. Keep the collector/mock aligned. Resource structure and host collector checks do not compile or execute Android UI/storage/codec code. The new diagnostics/lifecycle/color blocks await one later accumulated checkpoint; do not update CI trigger markers for ordinary development commits.

NativeCodecDiagnostic reuses the existing public NativeStillEncoder for a separate explicit Settings operation, not an export capability decision. Keep its fixed synthetic cases, closed text-report vocabulary and APK digest binding; do not pass selected sources, filenames, content URIs or raw exceptions to the report. Run off the UI thread on the ViewModel’s existing serial worker, request cancellation on foreground departure/clear, retain progress across configuration changes, and release every directly owned temporary candidate. Native adapter/renderer/plan semantics remain unchanged. Codec stop timeouts are cooperative; do not promise a hard wall-clock kill or use an external provider for this check.

NativeDiagnosticTest adds4 meaningful metric/report/cancellation/owned-candidate/API-budget/own-APK identity cases to the settings block, now18 (prior14+4). The tests inject failures/disguised candidates rather than exercising real native encoding, and do not qualify a phone codec. Private20/batch7/retry8/recovery16 and separate native-probe counts stay unchanged. New controls/errors/disclosures are localized in en/es/ca (215 keys). Actual phone checks remain manual acceptance, not an automatic CI loop; see docs/feasibility/native-device-check.md.


Unused-alpha WebP acceptance requires a full rendered-canvas pixel scan, not source/preview sampling or a Bitmap.hasAlpha hint. PrivateOutputExecutor alone produces opaqueWebpProven evidence, carried with Ready through the strict verifier and transactional saver. Restrict it to WEBP_LOSSLESS with an alpha-capable request; PNG and requested-opaque contracts remain strict. The scan uses16KiB tiles with cancellation/deadline and rejects any nonopaque pixel. Recovery still binds provider digests and never authorizes a retry from this flag. Keep the new proof/refusal/publication case in SourceAppearanceTest (private group now20).

The alpha oracle separates stored pixels from default decoder premultiplication. Reopen a second test-only bitmap with inPremultiplied=false, never draw it on Canvas, and require exact full-image alpha plus exact visible stored RGBA against the full shared render (hidden RGB at alpha0 is not promised). Preserve the fixture's independent centre/blend/edge-hue checks; transition hue uses stored channels so renderer rounding and decoder rounding are not charged to a single budget. Default reopened pixels still compare with the rendered image at every pixel with exact alpha and the unchanged one 8-bit premultiplied-channel allowance. No threshold, renderer, codec route or production verifier changes. Bitmap.getPixel returns straight channels (https://developer.android.com/reference/android/graphics/Bitmap#getPixel(int,int)); see TESTING for the source-bound captured evidence. ResultsFeedbackTest removes its already-deleted missing-URI fixture from the cleanup ownership set after deletion, retaining all other assertions; do not blanket-catch deletion failures or add permissions.

### Finishing export review

The Results finish action opens a confirmation before acknowledging a job or clearing a successfully completed session. Reuse the localized explanation of the private recovery data being retired; saved public copies remain. Cancellation/dismissal does not acknowledge or clean anything. The dialog is deliberately not saved across recreation and resets when result state, finish mode or busy eligibility changes. Disable it during import/native work, output verification or sharing, and preserve the ViewModel guards. This does not authorize retry, output deletion or a background job.

### Rechecking export preparation

Use ReviewViewModel.canPrepareExport for both the Export button and the preparation guard. IDLE, BLOCKED or ERROR may be checked again only before a batch attempt, with no active/review token, acknowledgment, output action, busy operation, share verification, queued import or blocked session restore. Keep the same-draft attempted-job barrier. A refused destination or preflight does not require a cosmetic edit; pressing Export explicitly repeats the full preflight and then requests a destination. It does not retry an existing batch, skip blockers, reset settings, remove outputs or export automatically.

### Sharing verified output without a second provider lookup

ExportShareIntents builds only the payload for a user-selected, already verified set of committed outputs. The ViewModel must still re-read each receipt, require PUBLISHED and matching item/URI identity, and verify current provider bytes before calling it. Build ClipData directly from the verified MIME list and selected URIs; do not use ClipData.newUri or a resolver lookup while constructing the payload. That would add provider metadata I/O after byte verification and could fail independently of the read. Exact single-format MIME or image/* for mixed PNG/WebP remains on the Intent, while ClipDescription contains every distinct verified MIME type. Preserve selection order, the inclusive100 cap, duplicate/unsafe/unsupported rejection and temporary read-only grants. This is not a guarantee that a later external receiver can still access a provider URI.

### Pin the source identity when auditing build artifacts

The offline Gradle-candidate auditor reconstructs Git blobs, executable modes and the recursive source tree from the archive without extracting or running it. Obtain the expected commit/tree pair from the independently inspected repository commit, not from the ZIP or its generated report, and supply both --expected-source and --expected-tree. A wrong/missing pin, changed tracked build input, duplicate/conflicting member, link/special file, unsafe path or exceeded archive limit fails before a successful report. Run python3 scripts/test-gradle-candidates.py (13 offline cases) when changing this boundary.

Without pins, source_identity_verified remains false. Historical captures with not-yet-committed wrapper/lock files can still be inventoried, but source_build_inputs_verified is false with the untracked list; they cannot pass the pinned-source gate. Matching source identity does not authenticate Maven artifacts, prove that an APK was compiled from that source, independently reproduce it or verify a production signature. Generated metadata remains unapproved.

### Independent unsigned rebuild

Independent-rebuild requests use .ci/independent-rebuild-request.json and the separately guarded independent-rebuild workflow. This is a durable release-readiness check, not another emulator regression profile. Change the request only for an intentional rebuild of a previously inspected commit/tree and unsigned-APK baseline. Check the original artifact ZIP/source/unsigned bytes first; do not invent or copy an unreviewed hash. The normal Android validation marker remains unchanged.

The controller validates exact request fields, checks a pristine checkout of the pinned source and empty Gradle/project caches, then compares source identity and actual unsigned APK SHA256/size. It stays outside the checkout while the workflow replaces the workspace with that exact source. A separate workflow control commit identifies the controller, not the compiled source. Existing tracked files must remain unchanged after building. Run python3 scripts/test-independent-rebuild.py (6 offline cases, Python and Git) before changing it.

The hosted job uses pinned actions, source-pinned SDK/wrapper/locks, a fresh Gradle user home and --no-build-cache --rerun-tasks; it runs core tests and assembleRelease without signing or emulators. Always retain source/tool records, generated-only dependency capture, actual unsigned bytes and the comparison report, including a mismatch. byte_identical is only the unsigned byte comparison; it does not approve dependency authenticity, production signatures, physical acceptance or distribution.

### Publisher evidence without another Android build

The dependency-provenance request/workflow is an explicit data-only collection, separate from Android validation and independent rebuilding. It supports exact signed-artifact batches and independently pinned publisher manifests. Fixed HTTPS hosts, redirect/size/count/deadline limits and fresh output prevent unrelated fetches or mixed captures. Downloaded JAR/AAR data is never executed. Run python3 scripts/test-dependency-provenance.py (11 offline cases) when changing this boundary.

A matched build-capture hash is not an authentication decision. The collector always leaves authenticated=false; absent signatures and partial failures remain explicit, and mismatches retain the report and fail the job. Review the exact Kotlin publisher manifest and verify JUnit's detached signature against its publisher-maintained release key in an isolated public-key context before preparing any reviewed verification subset. Do not import arbitrary keys into the user keyring, approve generated hashes or activate incomplete Gradle verification metadata.

### Review the captured publisher subset offline

Use scripts/review-dependency-provenance.py on the verified flat evidence directory and its exact baseline metadata XML, with the independently inspected collector control commit. It pins the inspected Kotlin release manifest and JUnit publisher KEYS bytes, matches complete Maven coordinate/file paths, and verifies the exact JUnit JAR/signature with gpgv using only a temporary public keyring. No user keyring, signing key, agent or keyserver is used. Run python3 scripts/test-dependency-review.py (9 offline boundary cases); the recorded real-evidence invocation separately exercises actual cryptography.

The committed gradle/dependency-provenance-review.json and reviewed-verification-candidate.xml contain only exact files with independently reviewed publisher evidence. The JSON owns the current reviewed/pending counts; dated evidence receipts remain in TESTING and committed inputs. The candidate has a deliberately nonstandard filename and does not enable Gradle verification. It is a checksum candidate whose provenance was reviewed offline, not a signature-verification build policy. Complete all remaining artifact/metadata reviews and current security review before preparing an active strict policy. Do not widen this reviewed subset to an entire group or silently activate an incomplete file.


## Inspect a publisher key without importing user trust

The data-only collector also accepts a publisher-key entry with an independently inspected full primary fingerprint and a maximum of 128 KiB. It inspects public identities in a disposable GnuPG context; a subkey ID cannot satisfy the primary pin. The computed file hash and primary match are collection evidence only: authenticated and cryptographically_verified remain false. Separate detached-signature verification and primary/subkey binding review are required. The prior c2a93031 request fetched only Google’s public key; it did not repeat Android validation or execute downloaded code. The completed review below uses the captured primary-key bundle to verify three exact Maven artifacts.


### Reproduce the Google signature subset

Supply both --google-key-evidence and --expected-key-control to the offline reviewer, alongside the original captured artifacts/baseline/control. The inspected Google key bundle is pinned by SHA256 and the publisher’s full primary fingerprint. Each exact file must pass gpgv with a VALIDSIG bound to that primary and a SHA256-or-stronger digest. Good signature text or a short key ID is insufficient. Expired unused historical subkeys in the same bundle do not invalidate a different valid signing subkey; an expired/revoked actual signer, weak signature digest or wrong primary fails review.

Later batches extend the initial reviewed subset; the candidate remains incomplete and inactive. This verifies the three captured files, not all Google coordinates, an entire group or current vulnerability/license status. Nine offline reviewer tests and the actual altered-AAR refusal guard the boundary; no additional Android CI is needed for this evidence-only update.


### Collect the next bounded publisher batch

Use scripts/plan-google-batch.py with the exact baseline metadata and reviewed record. It separates up to 48 JAR/AAR, 24 module and 24 POM files, retains Gradle aliases, and omits already reviewed or previously attempted files. Canonical Maven URLs are only retrieval routes; exact capture hashes and detached signatures decide subsequent file review. Each plan reports its declared footprint, including signature allowances; the collector enforces a 256 MiB cap. Collection uses one 120-second deadline, no retries and no build. Missing signatures and partial failures remain pending. Run scripts/test-google-batch.py (4 cases) plus the collector tests before changing this boundary.


### Review a batch without trusting a group

Use scripts/review-google-batch.py with independently inspected control, request-SHA256 and prior-review-SHA256 pins. The previous reviewed receipt is an explicit immutable input; the new batch must independently prove each exact file against the baseline and pinned Google public key. The receipt preserves the base producer’s source hash and adds the batch/verifier source hashes. Missing or unverified signatures remain pending and are omitted from later automatic plans; they are never silently approved or retried. Five offline receipt/partial-capture tests supplement the four planner, eleven collector and nine verifier cases.

Gpgv validates the signature but does not enforce key expiry/revocation itself. The verifier separately inspects the pinned public bundle and requires the actual signing subkey and primary to be usable, unexpired and not marked revoked/invalid, with consistent creation/signature times. This covers the captured bundle; it is not an online revocation or current vulnerability check. An incomplete candidate never activates Gradle verification.

### Review additional Kotlin release manifests

Use `scripts/review-kotlin-manifests.py` with explicit control, request, prior-receipt and release-registry pins. It preserves previous exact proofs and appends only matching full Maven paths from the appropriate official release asset. A duplicate/unsafe path, changed asset or conflicting checksum fails closed. An unavailable manifest contributes no approval. Run `python3 scripts/test-kotlin-manifest-review.py` (7 offline cases). That historical Kotlin2.2.21 receipt remains unchanged; it does not authenticate the current Kotlin2.4.21 tooling.

### Inventory current vulnerability database matches

An explicitly paired `.ci/dependency-security-request.json` adds an OSV inventory to the same data-only job. It pins the baseline, three lockfiles and native source commit, and becomes a no-network skip when the provenance request changes. Collection uses only the fixed official OSV API, batches of 100, no retries, a 180-second deadline, response/total limits and at most 100 advisory-detail fetches. Pagination or incomplete details cannot pass as a complete inventory.

Run `python3 scripts/test-dependency-security.py` (6 cases), then replay retained evidence with `scripts/review-dependency-security.py`; its 4 offline tests cover scope changes, missing/paginated results, false clean claims and withdrawn advisories. Release-runtime selection comes from the app lockfile. Absence from these project lockfiles does **not** prove a plugin/buildscript dependency is unselected. Database matches require applicability/remediation review; no match means only no database match at the recorded time. [Current triage and reproduction](docs/feasibility/dependency-security-review.md) own the findings and open actions.


### Review the runtime publisher capture

Use `scripts/review-runtime-publishers.py` with independently inspected control,
request, prior-receipt, publisher-registry and app-lock SHA pins. It limits review
to the exact previously unreviewed non-Google-route files selected in the app's
release runtime configuration. A configuration selection does not prove APK
packaging. Every accepted file needs matching baseline bytes and a real detached
signature bound to the separately anchored primary, with a strong digest and
usable signing key. A key-service fingerprint match alone grants no approval.

Run `python3 scripts/test-runtime-publishers.py` (8 offline refusal cases).
[The retained capture and replay commands](docs/feasibility/evidence/README.md)
recover the actual public evidence without networking or another Android build.
Missing issuer anchors, unverifiable signatures and unavailable captures stay
pending; do not automatically retry them or broaden trust to a publisher group.
The candidate stays inactive and current security/license gates remain open.


The migrated security inventory can be requested separately from publisher
capture after an exact source-bound XML/lock snapshot is reviewed. The opt-in
`.ci/security-inventory-request.json` and `--standalone` collector mode require
explicit STANDALONE_PINNED_SECURITY_INVENTORY scope and complete hash pins.
See the [security owner](docs/feasibility/dependency-security-review.md#security-only-refresh-after-migrated-source-qualification).
This mode never retries publisher collection or approves trust/security.

## Reuse exact publisher proofs after a dependency migration

`scripts/rebase-publisher-review.py` accepts separately pinned old metadata,
old review and new metadata, plus inspected immutable source identities. It
first validates the complete historical receipt, then inherits only an exact
group/name/version/file identity with the same SHA256. A hash conflict fails;
a different version has no inherited approval. The original records and
immutable receipt locator are retained, with counts recomputed for the new
inventory. Historical JSON/XML are not overwritten. Unchanged attempted/deferred identities retain their original control/report locators so existing planners skip unavailable routes after migration; full historical records remain at the immutable inherited receipt. The new partial output
and XML stay inactive; current revocation, native trust, security and license
acceptance are not established by reuse. Run
`python3 scripts/test-rebased-publisher-review.py` for five offline boundary cases.

The separately inspected official Kotlin2.4.21 manifest registry is at
`docs/feasibility/evidence/kotlin-manifest-registry-2.4.21.json`. It pins a real
GitHub release/asset ID, size and provider digest; it does not mean the manifest
bytes have been collected or any new file authenticated. After the source-bound
inventory is recovered, the existing bounded publisher collector and manifest
reviewer can consume this registry without repeating older unavailable routes.

## Runtime license declaration and notice audit

`scripts/audit-runtime-licenses.py` pins the source build inventory and app lock
before networking. It selects releaseRuntimeClasspath coordinates, fetches only
reviewed Google/Central routes with bounded time/bytes/concurrency and no
redirects, compares actual archives with frozen SHA256, and reads embedded
LICENSE/NOTICE texts including nested JARs without executing downloads. POMs
without a frozen hash remain publisher-origin declarations, not issuer proofs.
The report never grants whole license/security/source approval. Five offline
cases cover pins/routes, parent-only declarations, unsafe XML, nested exact
notice bytes and traversal refusal. Run `python3 scripts/test-runtime-licenses.py`.

An explicit `--previous` report and `--expected-previous` digest allow completing
missing rows from the same source/inventory/lock only. Complete prior rows are
reused; cached text hashes are checked. The original partial report/producer
and new report retain separate identities. See the current
[license review](docs/feasibility/runtime-license-review.md).

## Exact Google POM source-copy review

A separate official AOSP release can corroborate an exact Google-published POM
when detached-signature collection did not resolve it.
`scripts/review-google-source-copy.py` checks an explicitly pinned registry,
release commit, exact canonical route, raw Gitiles directory/blob membership,
size and frozen SHA256. It never approves other publishers mirrored by Google.
`scripts/collect-google-source-poms.py` is a bounded data-only collector;
`scripts/review-google-source-poms.py` independently replays matching rows and
records all outcomes. The same-release planner omits earlier failed lookups.
Run `python3 scripts/test-google-source-copy.py` and the actual offline replay in
[the owner](docs/feasibility/google-source-copy-review.md). The candidate remains
inactive; source-copy evidence must never be described as PGP verification.


## Compiled NDK helper notices

libwebp's Android target compiles the installed NDK cpufeatures implementation.
Its BSD source notice differs from the module's Apache NOTICE. Preserve both
in LICENSES/Android-NDK-cpufeatures.txt. The grouped build runs
scripts/check-ndk-native-notices.py against the actual source-pinned NDK and
retains its small raw source inputs/report. An SDK/hash/canonical-text mismatch
fails instead of approving a different source. The APK checker now requires
eight canonical legal assets. See [the owner](docs/feasibility/native-source-review.md).
