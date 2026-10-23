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

- Add editable draft recovery, one-step undo, foreground pause/continue and explicit remaining-item retry.
- Review uncertain outputs, verify selected saved files before sharing, and select all successful copies when useful.
- Retire consistent private receipts after successful session review and explicitly remove verified owned pending MediaStore copies before a separately requested retry.
- Add localized import/preflight/result feedback, light/dark/system themes and offline English, Español and Català help.
- Add a separate synthetic HEIC/AVIF device diagnostic without enabling native exports.

### Around the app

- Integrate shared icon/header artwork, a redesigned product README and reusable release-note/build-receipt templates.
- Commit the standard Gradle wrapper and resolved version locks; add a checked release ledger and offline source-metadata validation.
- Make the selected-image scope and enlargement option full-row, labelled checkbox targets.

### Still open

This is unfinished development, not a published application release. Scoped API 36 tests passed the main PNG/WebP/session/recovery/sharing paths and two 100-item real-codec cases with simulated provider faults. One strict enlarged transparent-WebP edge still fails. Broader appearance, native HEIC/AVIF hardware/fidelity/fallback, real-provider/process-kill/low-space, physical accessibility/locale, dependency authentication, reproducibility, signing and install/upgrade remain open. JPEG XL is deferred for v0.1.0. See [TESTING](docs/TESTING.md) and [FORMATS](docs/FORMATS.md) for evidence and limits.
