# FixMyShot

Screenshots, ready anywhere.

FixMyShot is an Android application being built to prepare screenshots for publication: import, choose a destination, review and export a new copy. The default will preserve all content with minimal centered margins, use PNG and remove identifying metadata.

## Current state

**Unreleased development bootstrap: `0.1.0-alpha.1`.** This is not the first functional release. The launcher now offers a first system Photo Picker and a bounded, plan-driven review preview for supported SDR JPEG/PNG/WebP sources. Destination, format, background, batch and per-image planning are available; **there is still no public export, share-result flow or release download**. This is a development slice, not a finished editor.

The engineering groundwork contains Kotlin/Compose modules, a bounded PNG/lossless WebP codec adapter, an offline destination catalog, immutable crop and per-image batch-setting models, a private staged-image reader with source fingerprints, a bounded 24 MiB thumbnail cache, an sRGB contain preview renderer, an immutable preflight and an experimental full-resolution app-private PNG/WebP encoder path with source-fingerprint and output-container verification, plus synthetic device tests. Only developer instrumentation invokes the private encoder and the new single-item API29+ journaled MediaStore transaction adapter. The Save/Share UI and API26–28 save alternatives are not wired; no user-facing public export path is enabled. HEIC/AVIF integration and the bounded JPEG XL experiment remain Phase 1 work, not accepted features or rejected requirements. The complete implementation contract is in [the release blueprint](docs/blueprints/first-functional-release.md).

Android 8/API 26 is the minimum application target. The bootstrap includes English, Argentine Spanish and Catalan copy; full localized product coverage and manual selection belong to the later interaction phase. The review picker requests no broad storage permission; sources are copied to bounded private cache storage for the current session and are removed on user discard or age-based cleanup. Process-death recovery, full color-management/HDR, animated-image handling, manual locale selection and share-intent entry remain incomplete. The app itself requests no permissions and includes no networking, accounts or analytics.

## Development

See [CONTRIBUTING](CONTRIBUTING.md), [test scope](docs/TESTING.md) and [codec status](docs/FORMATS.md). At explicit batch validation checkpoints and pull requests to `main`, GitHub Actions builds debug/unsigned candidates and runs synthetic emulator checks; ordinary development commits do not launch the costly emulator matrix. A green bootstrap run does not establish physical-device codec acceptance, final translation quality or reproducible release readiness.

The intended first functional distribution is a GitHub prerelease with Obtainium support. There is no published release, production signing identity or deployed project website established by this bootstrap.

## Author and license

Created by Santiago Rodriguez. Source is licensed under GNU GPL version 3 only; see [LICENSE](LICENSE). Third-party source/build information is tracked in [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES).
