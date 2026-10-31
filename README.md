<p align="center">
  <img src="assets/branding/header.png" alt="FixMyShot" width="800" />
</p>

<h1 align="center">Screenshots, ready anywhere.</h1>

<p align="center">
  <strong>A simple Android app for framing screenshots before you share them.</strong><br />
  The screenshot is ready. The frame should be, too.
</p>

<p align="center">
  <a href="#on-your-phone"><img src="https://img.shields.io/badge/Android-8%2B-82DFC4?style=flat-square&amp;logo=android&amp;logoColor=white" alt="Android 8 and later; older devices best effort" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/GPL--3.0--only-BDA7FF?style=flat-square" alt="GNU GPL version 3 only" /></a>
</p>

<p align="center">
  <a href="#pick-frame-share">How it works</a> ·
  <a href="#screenshots">Screenshots</a> ·
  <a href="#on-your-phone">Installation</a> ·
  <a href="docs/FORMATS.md">Formats</a> ·
  <a href=".github/CONTRIBUTING.md">Contribute</a>
</p>

## A better fit for the conversation

FixMyShot prepares screenshots to accompany a message, bug report or post.
Fit the whole capture into the right frame, make a small adjustment if needed,
and save a fresh copy. Your screenshot shouldn't need its own editing session.

### Pick. Frame. Share.

1. **Pick your screenshots.** Choose one or a batch, or share them into FixMyShot from another app.
2. **Let Automatic do the simple work.** Keep Original or choose a destination, check the large preview, then export. All content stays in the frame by default.
3. **Send the result.** View your saved copies, share them, keep editing or start new work.

Need a more deliberate crop? Move and pinch in the preview, expand the canvas,
or open **Customize** for precise framing, backgrounds and output settings.
Batch settings work together with individual adjustments.

- **Originals stay intact.** Export creates separate copies.
- **PNG first.** Lossless WebP is an explicit option where supported.
- **Comfortable on your phone.** Light, dark and system themes; English, Español and Català.

Images are processed on your device. Identifying metadata is removed from new
copies by default; visible content is still visible. [Privacy details](docs/PRIVACY.md).

## Screenshots

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="FixMyShot start screen with Choose images" width="46%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="FixMyShot dark Settings with appearance and language choices" width="46%" />
</p>

<p align="center"><sub>Development preview · Start &amp; Settings</sub></p>

## On your phone

**Development preview · 0.1.0-alpha.1.** A public release APK is not available yet.

The first release is planned for GitHub, with Obtainium updates. F-Droid and
Google Play are future distribution channels.

<details>
<summary><strong>Installation & compatibility</strong></summary>

On Android 10 and later, saved copies go to **Pictures/FixMyShot**.
Android 8–9 uses a folder you select, with [best-effort compatibility](docs/decisions/compatibility-and-validation.md).

Development APKs are separate test apps. They do not establish the production
signing identity or upgrade path.

Once signed releases are published, add
`https://github.com/santirodriguez/FixMyShot` to Obtainium and enable prereleases
for the 0.x channel. See the [release guide](docs/RELEASING.md) for installation,
artifact and update details.

</details>

<details>
<summary><strong>Development status</strong></summary>

PNG and lossless WebP are implemented. Real-device fidelity and accessibility,
native HEIC/AVIF qualification, broader recovery scenarios and signed distribution
still have acceptance work ahead. JPEG XL is [deferred for v0.1.0](docs/decisions/jxl-first-delivery.md).

The [status page](docs/STATUS.md) tracks the remaining work;
[testing evidence](docs/TESTING.md) records exact commits and limitations.
A green build is useful. It is not a victory lap.

</details>

<details>
<summary><strong>Build & contribute</strong></summary>

Use **JDK 17**, **Android SDK 36** and the pinned native tools documented in
[Contributing](.github/CONTRIBUTING.md). Run commands from the repository root:

```sh
bash scripts/bootstrap-gradle.sh
python3 scripts/check-release.py
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Translations, clearer flows and useful bug reports are welcome. Please use
synthetic or carefully redacted examples. See the [changelog](CHANGELOG.md),
[format notes](docs/FORMATS.md), [release guide](docs/RELEASING.md)
and [artwork provenance](docs/ARTWORK.md).

</details>

---

## Made by a person

<p align="center">
  Created by <a href="https://santiagorodriguez.com">Santiago Rodriguez</a>.
</p>

<p align="center">
  <a href="https://santiagorodriguez.com/donate"><img src="docs/assets/badges/donate.svg" alt="Donate to support FixMyShot" height="46" /></a>
</p>

<p align="center"><sub>GNU GPL version 3 only · <a href="LICENSE">License</a> · <a href="THIRD_PARTY_NOTICES">Third-party notices</a></sub></p>
