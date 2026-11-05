# FixMyShot branding

These are **original, unmodified PNG files**. `assets/branding/` stores primary artwork and retained alternatives; the canonical store copies under `fastlane/metadata/android/en-US/images/` are consumed by Android Gradle, future Google Play tooling, and F-Droid metadata. Nothing here constitutes an app-store submission.

## Primary selection

| Source file | Former name | Size / PNG type | Use |
| --- | --- | --- | --- |
| `primary/app-icon.png` | `icon1.png` | 512×512, RGBA/8-bit | Selected Android launcher and store icon |
| `primary/google-play-feature-graphic.png` | `promo.png` | 1024×500, RGB/8-bit, no alpha | Google Play / F-Droid feature graphic |
| `primary/readme-header.png` | `header.png` | 1629×543, indexed/transparency | README and generated in-app header |

Exact byte-for-byte copies (no recompression/resizing):
- `fastlane/metadata/android/en-US/images/icon.png` = `primary/app-icon.png`.
- `fastlane/metadata/android/en-US/images/featureGraphic.png` = `primary/google-play-feature-graphic.png`.

The Android Gradle `prepareBrandingResources` task generates `drawable-nodpi/brand_icon.png` from the canonical Fastlane icon and uses `primary/readme-header.png` for its other branding resource. The adaptive icon foreground uses the generated bitmap. Its existing monochrome vector stays for Android 13+ themed icons, pending visual consistency review. Generated `brand_icon.png` belongs in the build directory, not Git.

## Preserved alternatives

| File | Original |
| --- | --- |
| `alternatives/app-icon-02-phone.png` | `icon2.png` |
| `alternatives/app-icon-03-editor.png` | `icon3.png` |
| `alternatives/app-icon-04-framing.png` | `icon4.png` |
| `alternatives/app-icon-previous-512.png` | prior Fastlane `icon.png` |
| `alternatives/app-icon-previous-source-1254.png` | `icon-source.png` |
| `alternatives/app-icon-previous-192.png` | `icon-192.png` |
| `alternatives/app-icon-previous-64.png` | `icon-64.png` |

The user-provided new files were originally uploaded at commit `1d5f2918304567ad00b3d031fe7e5da7bb97389c`. No alternative is silently selected for packaging. Git blob identities are retained by these renames, and the former primary icon remains recoverable.

## Future listing and release limits

The canonical `en-US` directory is the existing F-Droid fallback; duplicate identical images for other locales only when genuinely needed. Approved actual app screenshots are already in `fastlane/metadata/android/en-US/images/phoneScreenshots/1.png` through `6.png` and are **not** replaced by these promotional illustrations.

The selected icon matches Google Play's 512×512 32-bit PNG requirement and the feature graphic matches 1024×500 24-bit RGB PNG without alpha. Inspect adaptive launcher masks, icon legibility at small sizes, feature-graphic crop-safe areas and artwork rights before actual Play/F-Droid listing submissions.

**Important:** The earlier signed APK was built before `icon1` became the primary icon. Its SHA-256 `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde` remains a historical validation, **not** verification of a new APK showing this icon. The release request remains `PREPARED` with `branding_revision_pending=true`, refusing staging/publication until a new exact candidate is compiled, verified and separately authorized for signing.
