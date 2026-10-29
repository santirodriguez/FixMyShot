# Shared artwork

One checked-in asset set serves the Android app, repository documentation and future distribution metadata. The repository owner's five PNG uploads from commit `8f52caac29f450db3858c602e2011b5e0e3f8e47` are moved without changing a byte. Original Git blobs and SHA-256 below preserve provenance. Original filenames under `docs/` are removed by the same tree update as the moves; Git history retains them.

| Original upload | Canonical path | Dimensions | Use |
|---|---|---|---|
| `FixMyShot_1254.png` | `assets/branding/icon-source.png` | 1254×1254 | Largest supplied source for future exports |
| `FixMyShot_512.png` | `fastlane/metadata/android/en-US/images/icon.png` | 512×512 | Shared icon; F-Droid-compatible metadata path; Android packaging source |
| `FixMyShot_192.png` | `assets/branding/icon-192.png` | 192×192 | Supplied small variant, available for future web/app uses |
| `FixMyShot_64.png` | `assets/branding/icon-64.png` | 64×64 | Supplied small variant, available for future web/app uses |
| `Neon FixMyShot Editing Banner2.png` | `assets/branding/header.png` | 1629×543 | README/shared header; original 3:1 proportions |

Android's `prepareBrandingResources` task copies the canonical 512px icon into `app/build/generated/branding/res/drawable-nodpi/brand_icon.png`. `preBuild` owns that dependency, so clean builds, lint and APK assembly obtain it without a manual file-copy step. The adaptive foreground centers the artwork in a 66dp frame on a 108dp layer; launcher masks still require real visual review. Android 13+ additionally retains the original capture-frame vector as its monochrome layer. No generated PNG belongs in source control. README references the canonical header rather than a separate documentation copy.

The product name is invariant across locales. The shared metadata image therefore lives once in the F-Droid fallback locale `en-US`; do not duplicate the same bytes into `es-419`/`ca`. Future localized artwork is warranted only when its actual content differs. Authentic app screenshots belong under `fastlane/metadata/android/<locale>/images/phoneScreenshots/` and must come from the actual candidate build; these supplied illustrated logos/headers must never be presented as app screenshots.

## Future store preparation

The fastlane directory is shared metadata structure supported by F-Droid and Play tooling; using it does not submit or publish a listing. F-Droid accepts PNG/JPEG and uses `en-US` as fallback. Google Play currently specifies a 512×512 sRGB 32-bit PNG icon, and a 1024×500 feature graphic. The supplied images are palette PNGs with transparency; the 512px icon still needs a deliberate 32-bit export and visual review for Play. Keep the untouched source available. The 1629×543 header is not a ready 1024×500 feature graphic: create a separate purposeful store composition from the shared source when store work is authorized, retaining aspect ratio rather than stretching it. No store acceptance, final masked-icon review or future listing readiness is claimed here.

Primary references, checked 2026-10-22:

- [F-Droid metadata/graphics structure](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)
- [Google Play preview assets](https://support.google.com/googleplay/android-developer/answer/9866151)

## Original byte receipts

| Canonical asset | SHA-256 |
|---|---|
| `icon-source.png` | `9491ad8354182ddbf5b2191eb3c1ce16eef8b98d5d827e7f4f0a4e5709d33ec8` |
| `icon.png` 512 | `25996cb5b977602c3303a6b1d9baee751de2ca466fce77151d5c659ff7449028` |
| `icon-192.png` | `177467ad972b84ae722e6f814ca82e893f2c500e3f4ed4b22aec7cb03097cf30` |
| `icon-64.png` | `9276ce5ca20282ec77757c380e8149f69f80b8b7be4ca7b67263e38b4e7276c6` |
| `header.png` | `92dc719ba303f0417c9c378a6dfe24f5d618f747e4cd005ed9e62d4d0d84d349` |

## Development screenshots and support badge

The README reuses the canonical `en-US/images/phoneScreenshots/1.png` (Start) and
`2.png` (dark Settings) metadata images. These are unedited 1080×1920 PNG captures
from the actual Android36 emulator build at `13b50c1827317fa208829fa50381d5111fbb32d1`,
Actions40954636417, not reconstructed interface images or physical-device acceptance.
Store publication still requires the final candidate screenshot review. Editor test
patterns are retained as engineering evidence rather than promotional screenshots.

`docs/assets/badges/donate.svg` is the repository's GPL-3.0-only support button, with
accessible title/description and the existing author donation destination.
