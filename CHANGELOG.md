# Changelog

Real changes. No imaginary release history.

## Unreleased — 0.1.0-alpha.1

### Prepare the shot

- Import static SDR JPEG, PNG and WebP through the picker or Android share actions.
- Review up to 100 images with batch settings, individual overrides and thumbnails.
- Preserve content by default; explicitly crop with gestures/button alternatives, choose ratios, output sizes and backgrounds.
- Start new sessions with PNG and identifying-metadata removal; export separate PNG or lossless WebP copies.
- Add bounded recognized SDR P3/Adobe RGB normalization, orientation handling, payload-preserving no-op paths and an explicit metadata allowlist.

### Keep the work moving

- Retain the edit scope and pending ratio/color/size fields through screen recreation, isolate pending fields by image/scope and label batch-wide destination changes.
- Add editable draft recovery, one-step undo, foreground pause/continue and explicit remaining-item retry.
- Allow explicitly rechecking export after a refused preflight or destination without changing image settings.
- Show import progress and allow stopping after the current provider read, retaining completed images and separately counting untouched offers.
- Check device storage before and during private imports, explain low-space failures and preserve already prepared images.
- Keep provider runtime/cache-access failures typed so the import queue can finish without exposing private exception text.
- Check minimum private working space before preparing export candidates and distinguish it from destination storage failures.
- Check measurable destination space during export, handle provider pipes without file-only sync, report shortages and preserve uncertain cleanup for review.
- Keep uncertain destination creation out of retry, handle unexpected provider errors and explain when an output location could not be confirmed.
- Review uncertain outputs, verify selected saved files before sharing, and select all successful copies when useful.
- Build share payloads from verified image types without another provider metadata lookup; describe every selected PNG/WebP type.
- Confirm finishing export review before retiring restore/retry settings or clearing a completed session.
- Retire consistent private receipts after successful session review and explicitly remove verified owned pending MediaStore copies before a separately requested retry.
- Add localized import/preflight/result feedback, light/dark/system themes and offline English, Español and Català help.
- Add a separate synthetic HEIC/AVIF device diagnostic without enabling native exports.

### Around the app

- Integrate shared icon/header artwork, a redesigned product README and reusable release-note/build-receipt templates.
- Commit the standard Gradle wrapper and resolved version locks; add a checked release ledger and offline source-metadata validation.
- Make the selected-image scope and enlargement option full-row, labelled checkbox targets.
- Keep only the latest queued preview during rapid edits, discard obsolete preview work and guard result delivery by image/revision.
- Confirm session discard and bind discard/apply-to-all confirmations to the exact reviewed draft; changed batches require a fresh confirmation.
- Make saved-output share choices full-row checkbox targets and announce the actual crop zoom to accessibility services.

### Still open

This is unfinished development, not a published application release. Scoped API 36 tests passed the main PNG/WebP/session/recovery/sharing paths and two 100-item real-codec cases with simulated provider faults. The revised stored-alpha/render oracle passed the bounded API 36 matrix; wider color/alpha qualification remains open. Broader appearance, native HEIC/AVIF hardware/fidelity/fallback, real-provider/process-kill/low-space, physical accessibility/locale, dependency authentication, reproducibility, signing and install/upgrade remain open. JPEG XL is deferred for v0.1.0. See [TESTING](docs/TESTING.md) and [FORMATS](docs/FORMATS.md) for evidence and limits.
