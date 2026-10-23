# FixMyShot

![FixMyShot](assets/branding/header.png)

Screenshots, ready anywhere.

FixMyShot is an Android application being built to prepare screenshots for publication: import, choose a destination, review and export a new copy. The default preserves all content with minimal centered margins, uses PNG and removes identifying metadata.

## Current state

**Unreleased development build: `0.1.0-alpha.1`.** This is an incomplete first functional release, with no published APK download. The review flow accepts supported static SDR JPEG/PNG/WebP images through the system picker or Android sharing from another app. It supports destination/ratio choices, batch and per-image settings, bounded previews, explicit PNG/lossless WebP export, results, selected-output sharing and read-only review of previous output records.

Settings now provide light/dark/system appearance, persistent English/Argentine Spanish/Catalan selection, offline help/privacy and About links. Theme uses Preferences DataStore; language uses the standard AppCompat/platform per-app locale bridge. Imports belong to the ViewModel so configuration recreation retains in-flight work and the current draft. Offers beyond the 100-image batch limit are rejected with an explanation instead of being silently truncated. Incoming images arriving during a locked export wait for the current operation; they never change its frozen plan.

API29+ output uses owned MediaStore publication in Pictures/FixMyShot. API26–28 currently require an explicitly selected SAF folder. The scoped real-provider API28 exercise passed at `9aa67f1`; the later API26 exercise identified a WebP candidate-validation failure even though the real provider probe succeeded. Older compatibility is now best effort and has an optional extended validation profile; it will not drive complex changes or block ordinary product development. The declared minSdk remains 26 for now. See [the compatibility decision](docs/decisions/compatibility-and-validation.md).

PNG and identifying-metadata removal remain new-session defaults. Original files are read only; bounded private working copies are removed on discard or age-based cleanup. The app requests no permissions and has no networking, accounts or analytics. External links open only after a user action.

Explicit crop is now wired through drag/pinch plus zoom/directional/reset controls, normalized source coordinates and a shared preview/final renderer. Preserve-content mode remains the default. Preserve-pixels crop zoom reduces native output dimensions. Exact output size, uniform enlargement and custom RGB backgrounds are explicit choices; enlargement requires opt-in. Destination conflicts remain visible and block export until resolved.

**Still incomplete:** broader color/ICC/HDR/partial-alpha acceptance, complete private receipt retirement and uncertain-output cleanup, foreground/interruption and100-item provider scenarios, native HEIC/AVIF and the bounded JPEG XL decision, real external receivers/physical devices, accessibility/locale visual review and distribution readiness. Exact size/crop controls, selective metadata preservation and safe remaining-item retry are already implemented. The accepted bounded API36 metadata checkpoint is `d6b63ae`; editable-session tests passed at54bf003, while its full run failed the deferred locale diagnostic. New foreground/session-finish, recognized SDR color conversion and detailed result feedback changes have not yet run on Android. The complete contract and amendments remain in [the release blueprint](docs/blueprints/first-functional-release.md) and linked decisions.

Export jobs now persist their original batch/per-image settings and staged-source references. After restart, explicitly restore that export session and retry only failed, cancelled or unstarted items with proof of no remaining output. Already saved and uncertain outputs are never re-exported by this action. Retries use the recorded plan; older jobs without a frozen draft remain review-only. Successful output records and share selection retain their original item positions.


Editable drafts now checkpoint locally after edits, including selected image, inherited/custom fields and one-step undo. Reopening within24 hours restores that session before age cleanup and shows its retained format/privacy choices; no export starts automatically. Incoming offers are recounted against the restored100-image limit. A corrupt journal or missing staged image blocks silent replacement until explicit discard. Cache eviction can still make a saved draft unavailable. New sessions retain PNG/removal defaults. Its four isolated session cases passed at54bf003. The aggregate run failed a recurrent automatic-locale-switch case, now deferred to opt-in diagnostics. Broader manual/lifecycle acceptance remains open.

| Delivery stage | Current boundary |
|---|---|
|0 — Research/blueprint | Complete; repository-owned contract and compatibility amendment |
|1 — Foundation/codecs | PNG/WebP and builds work; native HEIC/AVIF, JXL decision and reproducibility/dependency trust remain open |
|2 — Image/privacy core | Geometry, crop, sizes, metadata and recognized SDR color conversion implemented; broader color/alpha/ICC/HDR acceptance open |
|3 — Import/review | Picker/sharing, batches, themes, languages and detailed import feedback implemented; manual accessibility/locale/visual acceptance open |
|4 — Reliable output | Save/share/retry/recovery implemented; editable drafts and pause/finish behavior implemented; accumulated validation, receipt retirement and extensive lifecycle/provider scenarios open |
|5 — Integral acceptance | Real Pixel/additional hardware, receivers/gallery, performance and usability still required |
|6 — Delivery | Version/signing authorization, reproducible artifact, install/upgrade and final release evidence still required |
|7 — Public1.0 | Later stabilization from actual use; website/store publication is not a prerequisite |


Leaving the app now requests a pause after the current bounded export transaction. Rotation keeps the active job. Returning does not automatically continue remaining items; “Continue remaining” uses the existing verified no-output retry path and preserves saved copies. After a fully successful batch, “Finish session” clears that session’s private working copies/current+undo/frozen settings and resets the new-session defaults, retaining public output and receipts. Partial/uncertain jobs retain their recovery protection. These new lifecycle changes are implemented but pending a later grouped Android checkpoint.

Recognized 8-bit SDR Display P3 and Adobe RGB sources now have an explicit color-managed path to canonical sRGB for preview and PNG/lossless WebP export. Review shows the original color space, conversion and batch count; out-of-sRGB colors are limited and originals remain unchanged. This is implemented, pending grouped Android execution. Unknown/malformed profiles, unsupported high precision and recognized HDR/gain-map descriptors are rejected where detected. Universal HDR conversion remains out of scope.

The latest import now summarizes added, rejected and duplicate items, with expandable numbered reasons and next steps. Valid images stay in the draft. Export results explain individual failures and uncertain copies, while a blocked preflight identifies affected images for review. Retry availability still comes from verified no-output recovery state. Feedback uses English, Spanish and Catalan; no technical exception text or private source names/URIs appear in these notices. This new block is implemented and pending grouped Android validation.

## Development

See [CONTRIBUTING](CONTRIBUTING.md), [test scope](docs/TESTING.md) and [codec status](docs/FORMATS.md). At explicit larger validation checkpoints and pull requests to `main`, GitHub Actions builds debug/unsigned candidates and runs synthetic emulator checks; ordinary development commits do not launch the costly emulator matrix. A green bootstrap run does not establish physical-device codec acceptance, final translation quality or reproducible release readiness.

The intended first functional distribution is a GitHub prerelease with Obtainium support. There is no published release, production signing identity or deployed project website established by this bootstrap.

## Artwork

The shared icon is [the canonical 512px asset](fastlane/metadata/android/en-US/images/icon.png). The Android build packages it as a generated resource; README uses the shared header directly. Source art, supplied size variants and provenance are described in [ARTWORK](docs/ARTWORK.md). These illustrations are branding, not app screenshots. No F-Droid/Play listing is published or claimed ready.

## Author and license

Created by Santiago Rodriguez. Source is licensed under GNU GPL version 3 only; see [LICENSE](LICENSE). Third-party source/build information is tracked in [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES).


