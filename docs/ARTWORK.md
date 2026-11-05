# Shared artwork

## 1.0.0 visual refresh — six native screenshots approved, pending Fastlane upload

The maintainer explicitly approved the complete six-image visual set on
2026-11-05 after a full-resolution review. These are **genuine, unmodified UI
captures** from the first-public 1.0.0 visual candidate, not screenshots of a
signed published APK. Their source is the passing isolated Android Actions
[run 42101163387](https://github.com/santirodriguez/FixMyShot/actions/runs/42101163387),
compiled commit `784bbdab61ac79c98a54c8de671bdf93332a0398`, smoke artifact
`13450950882`. The current visual PR #4 implementation is source/test/script
equivalent to that validated commit; only the CI trigger and this artwork
documentation differ. API36 smoke and the six-capture instrumentation passed;
SAF-provider was skipped in the current profile, not qualified.

The six approved native 1080×1920 RGB PNGs retain exactly the captured pixels.
Their numeric target files belong under
`fastlane/metadata/android/en-US/images/phoneScreenshots/` on
`ux/1.0.0-visual-polish` (PR #4). **The binary assets are not yet uploaded
to the repository.** Do not replace README/metadata claims or represent the
old tracked alpha.2 `1.png`/`2.png` as these 1.0.0 images before remote
uploads are verified.

| Proposed slot / role | Original capture | Approved SHA-256 |
| --- | --- | --- |
| `1.png` — Home | `visual-01-start.png` | `66c21df74523fea3a1e8dc49b3c876a52cfd0591a73d75ae07595b7c7716ee45` |
| `2.png` — Editor | `visual-02-editor.png` | `460aea244e0fac3add9868eceed4dac43b20fe036f4bc5f57f8290f6c891f84c` |
| `3.png` — Destinations | `visual-03-destinations.png` | `2baf40561df7774a8f47584a41b158726c67783948a361283d0ea2fad820a642` |
| `4.png` — Framing | `visual-04-framing.png` | `adac0d4f3f521d10ad317e75dc471b3b4647678ba249d3588adf4c837c5c694e` |
| `5.png` — Saved result | `visual-05-saved.png` | `fdc9f49084e1a5da18c3e169c49695a05c2a1e339cf29bc7f847fecd29fbe401` |
| `6.png` — Settings/About | `visual-06-settings.png` | `71194a1d2ee14bb30316fa45873a559e3f2d08b00322c468a75fe5d078b00cbb` |

The last UI iteration fixed the Saved dialog's dark status-bar icon contrast
and replaced the debug-only `FixMyShot Preview 2` text in About with the
canonical `FixMyShot` name and dynamic version 1.0.0. The normal debug
launcher label remains distinct. The screenshots were visually checked for
those regressions; all six were accepted together. Renaming `visual-*.png`
to numeric files does not modify their image bytes.

The input is a standalone non-sensitive café still life, imported through
the app's normal share-intent path, from instrumentation-only fixture
`app/src/androidTest/assets/FixMyShot-visual-sample-cafe-hq.jpg`
(JPEG 941×1672, 387,379 bytes, SHA-256
`50ff45c57d4c8e93570e5e5e3df99f7a863d625537fd7e9381963d785f968051`,
Git blob `676b272413e8199607b7dfecf2c37b89f3fadd50`).
The fixture is not bundled into the production APK.

Next gate: maintainer uploads only the six approved, numerically named PNG
files into the target PR branch. Read back all six remote blobs, verify their
SHA-256/size/dimensions and visual identity, and then synchronize README and
other current-release documentation with that exact canonical screenshot set
**before any default-branch release merge**. Do not repaint or transform the
screenshots to pass review. Brand usage, localization and physical-device
acceptance, exact candidate/signing/source assurance, merger and publication
remain independent gates.


## First public 1.0.0 screenshot review — maintainer decision 2026-11-05

The maintainer reviewed the eight-image contact sheet assembled from the genuine
API36 screenshots for compiled source `70f3baeebb096461eb5072ca125f0c05334703da`,
[Android Actions run 41958251409](https://github.com/santirodriguez/FixMyShot/actions/runs/41958251409),
original smoke artifact `13385305314`. The contact-sheet indices below refer to
that review order, **not** permanent Fastlane/store slot numbers:

| Review # | Original screenshot PNG | Maintainer visual decision |
| --- | --- | --- |
| 1 | `editor-start.png` | APPROVED |
| 2 | `editor-settings-dark.png` | APPROVED |
| 3 | `editor-settings-light.png` | REJECTED |
| 4 | `editor-settings-return-light.png` | REJECTED |
| 5 | `editor-destinations.png` | APPROVED |
| 6 | `editor-automatic.png` | REJECTED |
| 7 | `editor-next-image.png` | APPROVED |
| 8 | `editor-arrows.png` | REJECTED |

The four approved original files are valid visual candidates but this is
**not approval of a complete public screenshot set**. Rejected captures must not
be promoted or silently retouched. Replacement editor captures should use
a representative realistic, non-sensitive example image rather than the current
colored line test pattern; retain the actual captured application UI and
unmodified PNG bytes. No new screenshot pixels or Fastlane files were changed
by recording this decision. The existing two canonical Fastlane/README
screenshots remain genuine *historical alpha.2* evidence until an explicitly
approved complete replacement set is available.

## First public 1.0.0 preparation — 2026-11-04

The maintainer approved first public stable version **1.0.0 / code1**, unchanged
production package and existing signing identity, and clean installation after
uninstalling alpha.2. [The release amendment](decisions/first-public-release.md)
supersedes alpha publication/numbering instructions. PR2 has already merged into
main at c99f8df; subsequent alpha-oriented status entries are historical.

The new candidate requires its own build, exact source material, independent
rebuild, signing/inspection and truthful screenshot review. No alpha receipt
proves these new bytes. Accepted unchanged UX/output checks remain preserved;
manual tests not recorded below remain NOT_RUN. No 1.0.0 publication is claimed.


## Alpha.2 canonical screenshots verified

The maintainer uploaded the two prepared images in commit
`e742f5c20e5fa04a02aa6b3e69aee8c082c1f5a3`. Both files were read back through
GitHub at that immutable commit, decoded, checked for exact length/SHA256/Git
blob identity and opened for full visual inspection. Both are 1080×1920 RGB
PNGs without alpha. Home has a single main logo and the information action;
dark Settings has readable status and gesture indicators.

| Canonical image | Bytes | SHA256 | Git blob |
| --- | --- | --- | --- |
| en-US/images/phoneScreenshots/1.png |452844 |58936d22d58972251a592812771291617e026c5ac87dcb531ad25f5a10e6b6ff |55b3c21cc7cdaf9db10aa88eac311f29673b01bf |
| en-US/images/phoneScreenshots/2.png |93245 |234cbc5677ea8d7f4b9cd3e9f06abd88b4a66e04c65561b62c200c1ae477d1d0 |3d06e640ada80fcb2b5c97b034b899979656e5b4 |

These exact bytes match the previously verified original API36 captures from
compiled source `1f89c979a7b432e6117670d170ce8ca0df5e2567`, alpha.2/code2.
They show the separate debug build of the same app source; they are authentic
emulator captures, not signed physical-device screenshots. No pixels, supplied
logos/header or APK bytes were changed. README continues to reuse these two
canonical paths and now identifies the actual version and emulator provenance.

The bounded M10 English Home/Settings replacement and readback task is complete.
This does not establish a full localized store/capability screenshot set, store
submission readiness or completion of every final release asset. Earlier
screenshot/preparation sections below retain their historical state.

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
accessible title/description and the Caja Personal support destination.
`docs/assets/badges/obtainium.svg` is an original GPL-3.0-only project button,
not a copy of the Obtainium logo. Its configuration selects the GitHub source,
prereleases and the single universal APK; no catalog listing is implied.

## Current tracked screenshot inspection — 2026-11-02

The actual current tracked image bytes were opened and visually inspected.
Their Git blob hashes match the inspected candidate tree; both are1080x1920,
8-bit RGB PNG without alpha. No image was replaced or edited.

| Image | Git blob | SHA256 | Observed content |
| --- | --- | --- | --- |
|1.png |5a4f9214806648ffaf89342578623b026ae9c056 |1840199c8d6e6d88300097298e016cd393c1e924f8713f2c02d1ca40820b3da8 |Light Start, original supplied branding, tagline and Choose images |
|2.png |5783c08185e193913565787332357805a86c1442 |9f6081c5ee460aad5d1eeadbde766a734aaf206476d38deace2923107e28caea |Dark Settings, theme/language choices and closed Help/Privacy disclosures |

They remain authentic development captures from13b50c18. Source comparison
13b50c18..7cbed17d shows the Settings additions are inside the closed Help
disclosure and callback wiring; that supports consistency of the visible panel,
not a claim that these files were captured from7cbed17d. Start callback changes
likewise do not constitute a new capture. No visible version label was repainted.
The dark Settings capture also has low-contrast status-bar icons; it is historical
image evidence, not proof of a current runtime defect.

README already labels these images Development preview and makes no public
download claim. Preserve that honest wording and the accepted captures.
M10's final exact-candidate/locale/public-asset review remains open; source
consistency, hashes and format alone do not close it. F-Droid/Play image export,
additional marketing images and store submissions remain future work.

## Alpha.2 current screenshot candidates — pending canonical upload

Historical preparation entry, superseded by the verified canonical upload above.

Two original English API36 PNGs were recovered from smoke artifact13266399380
in [Actions41710230009](https://github.com/santirodriguez/FixMyShot/actions/runs/41710230009).
The original ZIP size/SHA256/CRC and source receipt were verified again.
Capture source is `1f89c979a7b432e6117670d170ce8ca0df5e2567`, versionalpha.2/code2,
the separate debug build of the same compiled app source used for the signed
candidate. These are genuine emulator UI captures, not signed physical-device
screenshots. No user image, version repaint, crop, resize or other pixel edit
is included. Both entire images were visually inspected.

| Original capture | Canonical upload target | Bytes | SHA256 | Git blob |
| --- | --- | --- | --- | --- |
| editor-start.png | fastlane/metadata/android/en-US/images/phoneScreenshots/1.png |452844 |58936d22d58972251a592812771291617e026c5ac87dcb531ad25f5a10e6b6ff |55b3c21cc7cdaf9db10aa88eac311f29673b01bf |
| editor-settings-dark.png | fastlane/metadata/android/en-US/images/phoneScreenshots/2.png |93245 |234cbc5677ea8d7f4b9cd3e9f06abd88b4a66e04c65561b62c200c1ae477d1d0 |3d06e640ada80fcb2b5c97b034b899979656e5b4 |

Both are1080×1920,8-bit/channel RGB PNG without alpha. Home shows the single
main branding and outlined information action; dark Settings shows readable
status and gesture indicators. The two staged files retain the existing
Home/Settings ordering. They do not claim a full localized store screenshot set,
four capability screens or store submission readiness.

The existing canonical images remain historical13b50c18captures until these
exact replacements are uploaded and their remote blobs/readback verified.
Do not update the README caption to currentalpha.2 before that replacement.
Its canonical paths can remain unchanged; no second independently edited
screenshot set is needed.

