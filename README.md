# FixMyShot

Screenshots, ready anywhere.

FixMyShot is an Android application being built to prepare screenshots for publication: import, choose a destination, review and export a new copy. The default will preserve all content with minimal centered margins, use PNG and remove identifying metadata.

## Current state

**Unreleased development build: `0.1.0-alpha.1`.** This is an incomplete first functional release, with no published APK download. The review flow accepts supported static SDR JPEG/PNG/WebP images through the system picker or Android sharing from another app. It supports destination/ratio choices, batch and per-image settings, bounded previews, explicit PNG/lossless WebP export, results, selected-output sharing and read-only review of previous output records.

Settings now provide light/dark/system appearance, persistent English/Argentine Spanish/Catalan selection, offline help/privacy and About links. Theme uses Preferences DataStore; language uses the standard AppCompat/platform per-app locale bridge. Imports belong to the ViewModel so configuration recreation retains in-flight work and the current draft. Offers beyond the 100-image batch limit are rejected with an explanation instead of being silently truncated. Incoming images arriving during a locked export wait for the current operation; they never change its frozen plan.

API29+ output uses owned MediaStore publication in Pictures/FixMyShot. API26–28 currently require an explicitly selected SAF folder. The scoped real-provider API28 exercise passed at `9aa67f1`; the later API26 exercise identified a WebP candidate-validation failure even though the real provider probe succeeded. Older compatibility is now best effort and has an optional extended validation profile; it will not drive complex changes or block ordinary product development. The declared minSdk remains 26 for now. See [the compatibility decision](docs/decisions/compatibility-and-validation.md).

PNG and identifying-metadata removal remain new-session defaults. Original files are read only; bounded private working copies are removed on discard or age-based cleanup. The app requests no permissions and has no networking, accounts or analytics. External links open only after a user action.

**Still incomplete:** actual crop controls/export integration, full color/HDR/metadata-preservation policy, complete draft/process-death resume and explicit retry, native HEIC/AVIF and the bounded JPEG XL decision, external receiver and physical-device acceptance, accessibility/locale visual review and distribution readiness. The new import/settings block requires its own source-bound Android validation; earlier successful CI does not validate it. The complete implementation contract and recorded amendments are in [the release blueprint](docs/blueprints/first-functional-release.md) and linked decisions.

## Development

See [CONTRIBUTING](CONTRIBUTING.md), [test scope](docs/TESTING.md) and [codec status](docs/FORMATS.md). At explicit batch validation checkpoints and pull requests to `main`, GitHub Actions builds debug/unsigned candidates and runs synthetic emulator checks; ordinary development commits do not launch the costly emulator matrix. A green bootstrap run does not establish physical-device codec acceptance, final translation quality or reproducible release readiness.

The intended first functional distribution is a GitHub prerelease with Obtainium support. There is no published release, production signing identity or deployed project website established by this bootstrap.

## Author and license

Created by Santiago Rodriguez. Source is licensed under GNU GPL version 3 only; see [LICENSE](LICENSE). Third-party source/build information is tracked in [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES).

