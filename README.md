# FixMyShot

Screenshots, ready anywhere.

FixMyShot is an Android application being built to prepare screenshots for publication: import, choose a destination, review and export a new copy. The default will preserve all content with minimal centered margins, use PNG and remove identifying metadata.

## Current state

**Unreleased development bootstrap: `0.1.0-alpha.1`.** This is not the first functional release. The launcher only describes development status. Import, editing, saving and sharing are not implemented yet; no download or completed-format support is promised.

The initial engineering slice contains Kotlin/Compose modules, a bounded PNG/lossless WebP codec adapter and synthetic device tests. HEIC/AVIF integration and the bounded JPEG XL experiment remain Phase 1 work, not accepted features or rejected requirements. The complete implementation contract is in [the release blueprint](docs/blueprints/first-functional-release.md).

Android 8/API 26 is the minimum application target. The bootstrap includes English, Argentine Spanish and Catalan copy; full localized product coverage and manual selection belong to the later interaction phase. It requests no permissions and does not include networking, accounts or analytics.

## Development

See [CONTRIBUTING](CONTRIBUTING.md), [test scope](docs/TESTING.md) and [codec status](docs/FORMATS.md). GitHub Actions builds debug/unsigned candidates and runs synthetic emulator checks. A green bootstrap run does not establish physical-device codec acceptance, final translation quality or reproducible release readiness.

The intended first functional distribution is a GitHub prerelease with Obtainium support. There is no published release, production signing identity or deployed project website established by this bootstrap.

## Author and license

Created by Santiago Rodriguez. Source is licensed under GNU GPL version 3 only; see [LICENSE](LICENSE). Third-party source/build information is tracked in [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES).
