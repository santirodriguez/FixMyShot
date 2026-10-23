# FixMyShot

<p align="center">
  <img src="assets/branding/header.png" alt="FixMyShot" width="880" />
</p>

<p align="center">
  <strong>Screenshots, ready anywhere.</strong>
</p>

<p align="center">
  Choose a shape. Keep the content. Save a new copy.
</p>

<p align="center">
  <sub>Local processing · no accounts · no ads · no analytics · no Internet permission</sub>
</p>

**Unreleased development build: `0.1.0-alpha.1`.** There is no published APK download yet. The features below are implemented development behavior; acceptance limits are recorded in [TESTING](docs/TESTING.md).

## The idea

Your screenshot already did the hard part. It captured the screen.

FixMyShot prepares it for wherever it is going next: choose a destination or ratio, review the framing and save a separate image. The default keeps all content, adds minimal centered margins, uses PNG and removes identifying metadata. Crop and enlargement are explicit choices.

## Highlights

- **Start with your images:** import supported static SDR JPEG, PNG or WebP through Android's picker or another app's Share action.
- **Review a batch:** up to 100 images, batch settings and individual overrides, with clear reasons when an image needs attention.
- **Choose the framing:** preserve content or crop deliberately; adjust ratios, output dimensions and backgrounds before exporting.
- **Save separate copies:** PNG or lossless WebP, leaving originals unchanged.
- **Share deliberately:** choose individual saved outputs or select all successful copies. Each selected file is verified before Android's share chooser is prepared.
- **Pick up unfinished work:** editable drafts, one-step undo, explicit remaining-item retry and review of previous output records.
- **Resolve an interrupted save:** explicitly remove a verified unfinished copy created by FixMyShot, then choose whether to retry. Saved copies stay put.
- **Keep control:** leaving the app requests a pause after the current transaction; continuing is your choice.
- **Make it comfortable:** system/light/dark appearance, English, Argentine Spanish and Catalan, with offline help and privacy information.

Android 10/API 29 and later saves owned copies to **Pictures/FixMyShot**. Android 8–9/API 26–28 uses an explicitly selected folder; older compatibility is [best effort](docs/decisions/compatibility-and-validation.md).

## Privacy, by design

Images stay local. There is no account to create and no cloud dashboard to admire.

Private working copies support review and recovery. Finishing a successful session clears its working data and retires its consistent private receipts; unresolved jobs keep the evidence needed for safe recovery. Cleanup can be delayed and is not secure erasure. Saved output remains under your control. See [PRIVACY](PRIVACY.md) for retention, metadata and external picker/share behavior.

## What still needs work

This is an incomplete first functional delivery.

| Area | Remaining boundary |
|---|---|
| Image appearance | Broader color/ICC/HDR and partial-alpha acceptance; a strict alpha-edge failure remains unresolved |
| Reliable output | Broader 100-item real-provider, interruption, process-kill and low-space scenarios; remaining unresolved-job cleanup. Bulk sharing, completed-job retirement and pending-copy removal have scoped API36 coverage |
| Native formats | HEIC/AVIF hardware, fidelity and fallback qualification; [JPEG XL is deferred for v0.1.0](docs/decisions/jxl-first-delivery.md) |
| Real-world use | Pixel plus another hardware family, receiving apps/gallery, performance, accessibility and visual language review |
| Distribution | Dependency authentication, independent reproducible build, signing authorization, install/upgrade and final release evidence |

Settings includes a separate [synthetic HEIC/AVIF device check](docs/feasibility/native-device-check.md). It uses no selected images or gallery writes and can share a text report after an explicit tap. It does not enable native exports or establish physical appearance acceptance.

The exact contract and phases live in [the release blueprint](docs/blueprints/first-functional-release.md). Build and runtime evidence belongs in [TESTING](docs/TESTING.md); a compiled feature is not automatically an accepted release feature.

## Help improve FixMyShot

Found a confusing translation, a bug or an idea worth sharing? See [CONTRIBUTING](CONTRIBUTING.md). Use synthetic examples in public reports and keep private screenshots and image references out of them.

## Build

Requires **JDK 17** and **Android SDK 36**, plus the pinned native tools listed in [CONTRIBUTING](CONTRIBUTING.md).

```sh
bash scripts/bootstrap-gradle.sh
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleRelease
```

The Gradle wrapper and resolved version locks are committed. GitHub Actions runs at explicit larger checkpoints; ordinary development commits do not launch the emulator matrix. The intended first distribution is a GitHub prerelease with Obtainium support. No release, store listing or deployed website is claimed ready.

## Artwork

The app uses [one canonical icon](fastlane/metadata/android/en-US/images/icon.png), and this README uses the shared header. [ARTWORK](docs/ARTWORK.md) records source provenance and reusable locations for future distribution. Branding illustrations are not app screenshots.

## Author

Created by [Santiago Rodriguez](https://santiagorodriguez.com).

## License

GNU GPL version 3 only. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES).


