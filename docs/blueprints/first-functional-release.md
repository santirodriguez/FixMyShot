# FixMyShot — first functional release blueprint

Document version: 1.0 · Research date: 2026-10-20 UTC · Phase 0 governing artifact

Repository: `santirodriguez/FixMyShot`  
Baseline: `eb430e13ac71fdf334978b88623acf1ae1944c7e` on `main`  
Work branch: `release/0.1.0`  
First functional delivery: `0.1.0` (GitHub prerelease)  
Public stable milestone: `1.0.0`

## 1. Purpose, authority and execution boundary

Build a small Android application that prepares screenshots for publication with as little work as possible. The core journey is **pick or share images → choose destination or ratio → review → export**. It is not a general photo editor.

This document is the complete product and implementation contract for the first delivery. Read it in full before implementing. Supporting links provide evidence, not permission to change this contract. A later implementation decision must record its evidence, affected requirement IDs and acceptance impact; an explicit requirement cannot disappear because a library or device makes it inconvenient.

Phase 0 authorizes research, non-destructive inspection, one work branch and this blueprint. It does **not** authorize application implementation, merge, tags, releases, deployment, credential or permission changes, destructive operations, or writes to reference repositories. The subsequent implementation phases below are instructions for the separately adopted Code v3 workstream. Do not install tools on the user's computer as a prerequisite. If the execution environment lacks an indispensable capability, preserve the completed work and identify the blocked gate.

The implementation must not depend on the originating conversation. On adoption, reconcile this file, its branch and the live repository before Phase 1. Do not restart product discovery or ask again about the settled preferences documented here. If another contributor has changed the baseline, inspect the new material and reconcile rather than overwrite it.

### Identity and language

| Field | Decision |
|---|---|
| Product / application ID | **FixMyShot** / `org.cajapersonal.fixmyshot` |
| License | GNU GPL version 3; retain the existing `LICENSE`. Use `GPL-3.0-only` for new project SPDX declarations; do not infer an additional later-version grant from the license template. Preserve compatible third-party notices. |
| Planned website | `https://fixmyshot.cajapersonal.org/` |
| Planned privacy URL | `https://fixmyshot.cajapersonal.org/privacy/` |
| Website status | Identity decisions only. No deployment, DNS, hosting or availability was verified. |
| Engineering language | English for code, technical documentation, branches and commits. Localized product content is the exception. |
| Author / donations | Santiago Rodriguez; `https://santiagorodriguez.com/`; `https://santiagorodriguez.com/donate`, observed in CountAway. Open links only after a user action, in an external browser. |

## 2. Verified starting point and inspection coverage

Initial inspection and the pre-branch revalidation on 2026-10-20 agreed:

| Surface | Observation | Validation status |
|---|---|---|
| Repository metadata | Public, default branch `main`; GitHub identifies GPLv3 | Verified through GitHub |
| History | One root commit, `eb430e13ac71fdf334978b88623acf1ae1944c7e`, “Initial commit”, 2026-10-20 | Verified; no parents |
| Complete tracked tree | Only `LICENSE`, 35,149 bytes, blob `f288702d2fa16d3cdf0035b15a9fcbc552cd88e7` | Tree and license inspected |
| Actual commit tree | `bf3f59f47f185149a7133ba08b9b38b9a67c5426` | Commit metadata verified |
| Refs before this work | Only `refs/heads/main` | Verified |
| Tags / releases | None | Verified refs and releases |
| Source, configuration, tests, local instructions, CI, metadata, assets | None in the tracked repository | Not applicable to baseline review |
| Existing build / lint / test commands | None | **Not run: no application or build system exists** |
| Target exclusions | No tracked file excluded. Untracked user machines, unrelated repositories, secrets and hosting are outside inspection scope. | Explicit boundary |

This is a new implementation in an existing repository. Absence of an application is not a confirmed defect. Phase 0 does not establish a working baseline, a successful Android build, passing application tests, working encoders or a release signing identity.

Read-only reference inspection covered CountAway's `README.md`, `CONTRIBUTING.md`, language catalog and XML, localization validator, app build configuration, Android CI, release workflow and release documentation at observed main commit `6f15d78055b22fc11cdb8f2cb1377600054f26ae`. Its release workflow was reviewed for relevant structure and gates, not executed or adopted wholesale. No CountAway widget behavior, signing key, application identity or version sequence is inherited.

## 3. Research method and competitor findings

Evidence labels used throughout:

- **Observed**: directly retrieved repository/API state or inspected live UI.
- **Source-reviewed**: behavior inferred from specific implementation code, without running the Android application.
- **Developer claim**: product page, store declaration or documentation; not independent runtime verification.
- **Design decision**: FixMyShot's chosen behavior, not an assertion about a third party.
- **Unverified**: device, upload or runtime behavior still requiring a named acceptance gate.

No competitor APK was installed and no private image was uploaded during Phase 0. Step counts below are decision stages, not stopwatch or instrumented tap measurements. Picker navigation, Android permission prompts and the number of selected items can add taps.

| Alternative and evidence | Actual or reconstructable journey | Defaults / framing / batches | Privacy and export | Implication for FixMyShot |
|---|---|---|---|---|
| **Image Toolbox**; source-reviewed at `9fddc74dc1883992550f5cb7cfd97dca45777d8e`; F-Droid description [S01] | Enter Resize & Convert → choose images → configure dimensions/preset/resize options and format → preview → save. Roughly 4–6 decision stages depending on entry path. `ResizeAndConvertContent.kt` and `ResizeAndConvertComponent.kt` expose many controls and per-file save results. Not a measured tap count. | Extensive resize/convert modes, quality, formats and metadata controls; batch support. Source defaults can depend on stored settings, so no universal first-run default is asserted here. | FOSS local image tooling; broad format and feature claims on the listing. No independent network, metadata or encoder-quality test in this research. | Keep the useful batch/result concepts; do not copy its menu/catalog breadth. Screenshot preparation should be the entry screen, not one tool among dozens. |
| **Batchpad**; developer site and Play listing inspected [S02] | Advertised “3 taps”; the developer's explanation actually separates image selection, preset, background, optional decoration and export. Approximately 3 required decisions plus optional stages; not tested on-device. | Claims batches of 100+, full-resolution padding, ratios, colors and decorations. Exact defaults and override reconciliation not independently verified. | Claims local processing and retained EXIF. Play listing declares ads/IAP and data collection/sharing categories; these are developer declarations, not traffic measurements. Listing observed updated 2026-10-17. | Batch padding is relevant demand evidence for the task. Do not inherit metadata retention, ads, decoration tools or a speed claim. |
| **PadSnap** at `padsnap.app`; live controls observed in a desktop browser, version **1.9.1** [S03] | Select/drop files → select ratio/size → optionally adjust background/zoom → Process Images → Download All. Four required decision/action stages including picking when the ratio changes; one fewer if the initial square is suitable. No end-to-end processing was performed because no image was supplied. | Live UI: square 1080×1080, landscape 1080×566, portrait 1080×1350 and custom size; white `#ffffff`; blur off; zoom off at 100%; multiple-file input. Download disabled before processing. The indexed page still described v0.6.9; live state takes precedence. | Site claims local, ad-free processing. Export packaging, metadata and output fidelity were not verified. | A focused flow is feasible. FixMyShot adds native receiving/sharing, honest file-format capability and stronger metadata/batch contracts. No blur feature is needed. |

The evidence supports a narrow padding/framing workflow. It does **not** demonstrate comparative processing speed, superior codec quality, verified screenshot demand on every social platform, or a quantified market share. The user-reported friction with Image Toolbox is a product requirement, not a defect report against that project.

## 4. Settled scope and version sequence

### First functional delivery (`0.1.0`)

Include single and multiple import; receive share intents; all three languages; themes; offline destination catalog; preserve-content padding; optional crop with drag/pinch/reset and accessible alternatives; shared batch configuration with persistent per-image overrides; previews; PNG default; lossless WebP; HEIC and AVIF on validated native-capable devices; metadata controls; gallery save and sharing; recovery from failed/cancelled exports; signed GitHub APK delivery and Obtainium upgrade instructions. JXL has the bounded inclusion gate in §8. HEIC and AVIF are not deferred wholesale: implementation and representative-device validation belong to this delivery.

Use `0.1.0-alpha.N` for incomplete integration builds, `0.1.0-beta.N` for feature-complete manual testing, `0.1.0-rc.N` for release candidates, then `0.1.0` as the first functional GitHub prerelease. Incomplete alpha builds must say which gates remain open and must not be called the functional delivery. No version, tag or release is created during Phase 0.

Reserve `1.0.0` for a stable public milestone after real use, final visual identity, complete evidence and no unresolved release-blocking defects. This does not promise an extra feature set. If `0.1.0` is already suitable, stabilize the same scope. A website, F-Droid entry or Google Play approval is never a prerequisite for `0.1.0` or `1.0.0` on GitHub.

`versionName` is SemVer without the Git tag's `v`; Git tags later use `v0.1.0-alpha.1`, etc. Maintain a checked-in release ledger with a strictly increasing positive `versionCode`: start at 1 for the first distributed build and increment for every subsequently distributed APK, including prereleases. Do not derive it from dates, branch names or CountAway. Never reuse a code for different public bytes. The release check compares the ledger, APK manifest and GitHub release metadata. Stable and prerelease builds use the same production application ID and signing lineage so users can upgrade. Local debug builds use `.debug` and a distinct visible name.

### Excluded or deferred

No JPEG output; ZIP export; filters; blur backgrounds; AI; generated/reconstructed interface content; drawing; video; camera capture; photo library management; accounts; backend; analytics; advertisements; subscriptions; in-app updater; store submission; hosted website deployment; widgets. No hidden upload or cloud processing. Do not add a function merely because a competitor offers it.

## 5. Product flow and interface contract

### Screens and defaults

1. **Start**: one prominent “Choose images” action, concise tagline, Settings/About access. The system picker accepts multiple images. A received share intent opens the same review flow directly.
2. **Review**: main preview and a horizontal thumbnail strip or compact grid for a batch. Destination/ratio is the primary control. “Keep all content” is visibly selected. Format is visibly **PNG**. Show the output dimensions and whether padding, scaling or cropping will occur. Export is immediately available after required checks; an extra confirmation screen is not part of the normal path.
3. **Per-image adjustment**: open a thumbnail for crop, background or output overrides. Clearly label “Uses batch settings” versus “Custom settings”. Provide “Reset this image to batch settings”. Optional crop uses an initially centered frame, drag, pinch and reset; also provide zoom buttons/slider and directional controls for assistive input.
4. **Export results**: progress and cancellation; then counts for saved, failed, skipped and review-required items. “Share” opens the Android chooser; “Retry failed” does not re-export successes. The user may open an output through a compatible external viewer.
5. **Settings / Help / About**: theme, language, short offline help, privacy, license/notices, source/author/donation links and application version. Format and metadata controls remain visible in Review because they affect this export.

Do not require onboarding, account setup or destination-app login. Cold start has no welcome carousel. Returning to a draft retains that draft's explicit settings; starting a **new session always selects PNG and enables removal of identifying metadata**, regardless of the previous session. Persist theme, language, last destination/ratio and useful background choice. Do not persist an unsafe destination override, lossy format or metadata opt-out as a new-session default. Importing more images into the current batch follows its current configuration and displays that fact.

The normal repeated flow is choose images, choose/accept destination, inspect preview and tap Export. Count picker operations separately when reporting usability measurements. No speed or tap-count claim goes into marketing until observed on the implementation.

### Backgrounds and transparency

Offer **Automatic**, **White**, **Black**, **Custom color**. Automatic uses an opaque neutral background: derive a robust median luminance from small edge samples of the orientation-corrected image; choose white for light edges and `#121212` for dark edges at a fixed 0.5 linear-luminance threshold. Ignore fully transparent samples; if no usable sample exists, choose white. The choice is independent of the app theme, deterministic across preview/export and frozen in the export plan. This is padding color selection, not content reconstruction. Do not extend/clone edges or create fake interface areas.

Offer transparency only in the background control when both encoder and destination policy permit it. PNG and lossless WebP can carry alpha; the chosen native HEIC/AVIF paths in this release do not promise alpha. Google Play screenshots require opaque RGB PNG. Social layout templates default to opaque output because transparent-image handling is not universally documented. When transparency becomes invalid, keep the previous choice in the draft, mark a conflict, and offer an explicit opaque-background resolution. Do not silently flatten a user's explicit transparent choice.

Opaque backgrounds also composite existing source alpha. In a transparent custom export, preserve alpha through the full pipeline and display a checkerboard only in the preview, never in exported pixels.

## 6. Destination research and offline preset catalog

### Evidence and boundaries

Google Play's official help specifies JPEG or 24-bit PNG without alpha; each side 320–3840 pixels and the long side no more than twice the short side for phone screenshots. A separate recommendation/promotion condition uses 9:16 or 16:9 and at least 1080 pixels on the short side, with additional screenshot-count conditions. Do not turn that recommendation into the universal upload minimum [S04].

Meta's Instagram publishing API documentation specifies JPEG and additional constraints for that **API** [S05]. This is not evidence that the Instagram app accepts only JPEG, nor evidence of current organic-upload ratios. The public Instagram help endpoint returned rate limiting during research, and Facebook's ads guide redirected to a login wall. Therefore FixMyShot must not advertise officially verified upload compatibility for Instagram/Facebook's manual photo surfaces from this evidence. Preserve the agreed destinations as clearly described **layout templates** and verify actual upload behavior during manual acceptance. No Instagram API integration is planned; JPEG output remains excluded.

TikTok's September 2026 carousel **advertising** specification allows JPEG/PNG with separate horizontal, square and vertical dimensions and requires music [S06]. It is not an organic-photo specification or a video preset. A static-image utility cannot promise completion of an ad workflow that also requires music. Pinterest's primary guidance recommends 2:3 imagery for Pins [S07], but this research did not establish sufficient screenshot-conversion demand to add another branded entry.

Pew's 2025 US-adult survey reports Facebook 71%, Instagram 50% and TikTok 37% usage [S08]. Use only the Facebook-before-Instagram order as an explicitly limited, documented audience proxy for secondary labels. It is neither a global/Argentine ranking nor evidence of screenshot-conversion demand. The initial catalog is justified by the user's agreed destinations and concrete geometry needs, not the platforms' overall popularity.

### Catalog version 1

Default dimension policy is **Preserve pixels**: add the minimum canvas required by the selected constraint and do not scale if the result satisfies hard limits. “Exact size” is an explicit advanced alternative. All entries default to contain, opaque neutral background, PNG and metadata removal.

| Stable ID / primary label | Secondary destinations / surface | Constraint and output policy | Evidence status and decision |
|---|---|---|---|
| `play-phone` / Google Play screenshots | Google Play · phone listing | No fixed ratio; keep an already valid image. Otherwise add only enough padding for long ≤ 2×short and both sides ≥320; enforce the 3840 maximum. Use RGB PNG without alpha. If over the maximum, explicitly show the minimum uniform downscale required. Do not upscale small source pixels automatically. | Official constraints [S04]; INCLUDE |
| `play-portrait` / Google Play portrait 9:16 | Google Play · recommended presentation | Exact 9:16, opaque RGB PNG; preserve pixels when 320–3840 and recommendation minima can be met. Label recommendation separately from upload validity. Optional exact 1080×1920. Provide 16:9 landscape by swapping axes. | Official ratio/recommendation [S04]; INCLUDE |
| `social-square` / Square 1:1 | Facebook · Instagram — photo layout | Exact 1:1; preserve pixels; opaque PNG. Optional exact 1080×1080. | Layout guidance, **not verified upload certification**; INCLUDE agreed destinations |
| `social-portrait` / Portrait 4:5 | Facebook · Instagram — photo layout | Exact 4:5; preserve pixels; opaque PNG. Optional exact 1080×1350. | Conservative layout template; do not call it the full current allowed ratio range; INCLUDE |
| `social-story` / Story 9:16 | Facebook · Instagram — story layout | Exact 9:16; preserve pixels; opaque PNG. Optional exact 1080×1920. Note that destination UI can cover edges; no invented universal safe-area guarantee. | Layout guidance only; INCLUDE, separate from video |
| `custom-ratio` / Custom ratio | No compatibility claim | Positive integer numerator/denominator, 1–10,000 each, reduced by GCD; orientation swap; optional exact pixel size subject to resource limits. Alpha allowed for PNG/WebP. | User-defined; INCLUDE |

Only group destinations when the full output contract is equivalent: dimensions policy, min/max limits, color, alpha, codec restrictions, publication surface and evidence status. Sharing a ratio alone is insufficient. For example, Play portrait and social story remain separate even at 9:16. If future verification reveals a difference between Facebook and Instagram, split the grouped record without changing existing saved plans silently.

**Carousels:** offer a “Use one canvas size for this batch” switch for social photo batches. Start with the smallest common exact-ratio canvas that contains all inputs without scaling; the user may explicitly choose a fixed size. This provides consistent output geometry, not a claim about platform slide counts or first-slide cropping. Ordinary batches retain independent minimal dimensions. Mixed per-item dimensions/ratio overrides are shown as exceptions; do not label such a batch carousel-ready. No account or publication automation is included.

**Deferred branded entries:** TikTok organic photos/stories until an official relevant source and real screenshot task justify them; TikTok carousel ads (different dimensions/music requirement); Pinterest, LinkedIn, X, Reddit, messaging apps and YouTube thumbnails until demand and surface-specific rules justify entries. Generic/custom ratios already support those geometry needs. Video/Reels/Shorts exports are outside scope. An unverified new 3:4 Instagram allowance must not be asserted from community posts; users can select custom 3:4 meanwhile.

### Offline schema and maintenance

Bundle a versioned catalog with stable IDs and localized labels, `surface`, rational ratio or envelope constraint, dimension policy/options, supported format policy, alpha/color policy, evidence level, source URL, source observation date and a brief localized caveat. No network refresh and no remote configuration. Keep source facts separate from UI labels so translations cannot alter geometry. Unit tests validate all entries and complete locale keys. Store the catalog version in export plans; a later app update must ask before migrating a saved draft whose constraints changed. Recheck official sources before a release and whenever a compatibility issue is reported; update through ordinary app releases. Expired evidence downgrades a claim, not the user's file or selected format.

## 7. Geometry and batch reconciliation contracts

### Coordinate system and minimal changes

Normalize EXIF orientation, including mirrored cases, into the decoded image coordinate system once. Subsequent geometry uses orientation-corrected width `w` and height `h`, positive 64-bit arithmetic and finite normalized coordinates. Never stretch axes independently. A preview and full-resolution export must consume the same immutable plan, not separately reimplement fit/crop mathematics.

For an exact reduced ratio `p:q` in preserve-pixels contain mode:

```text
k = max(ceil(w / p), ceil(h / q))
canvas = (p*k, q*k)
left = floor((canvasWidth - w) / 2)
top  = floor((canvasHeight - h) / 2)
right/bottom receive any remaining odd pixel
```

This is the smallest integer canvas of that exact ratio that contains every source pixel. Up to `p-1` or `q-1` additional pixels can arise from the integer ratio grid; document this instead of claiming a fractional canvas is possible. Validate overflow and resource limits before allocation.

For `play-phone`, first bound each canvas side below by 320 without scaling the image; if the resulting long side exceeds twice the short side, extend the short side to `ceil(long/2)`. Center the original on that canvas. For a recommended ratio with a minimum side, include that minimum in the integer-grid calculation and pad instead of automatically enlarging source pixels. For an explicit exact canvas, use one uniform fit scale capped at 1 by default, round the raster bounds deterministically, center and disclose any downscale. An explicit “Enlarge to fill” choice can permit uniform upscaling and must show it in export details. For an oversized input, choose the largest valid canvas that satisfies the destination, show the required uniform downscale, and require acceptance before the first export of that plan. Do not alter quality or size to meet an undocumented byte limit.

Worked acceptance examples for **Pixel 9 Pro XL screenshots, 1344×2992**:

| Choice | Expected result |
|---|---|
| Play phone minimum adjustment | 1496×2992, 76 opaque pixels on each side; source pixels unchanged |
| 9:16 preserve pixels | 1683×2992, 169 left / 170 right; source pixels unchanged |
| 1:1 preserve pixels | 2992×2992, 824 on each side |
| 4:5 exact ratio, preserve pixels | 2396×2995, 526 each side and 1 top / 2 bottom |
| Exact 1080×1920 contain, explicitly chosen | Uniform downscale; raster source approximately 862×1920 with 109 pixels each side under the chosen rounding rule. Verify against the common renderer. |

### Crop

Crop is explicit and never the initial mode. Model a crop as normalized source focus `(cx, cy)`, zoom relative to the minimum fill transform, and the target ratio. Initial focus is `(0.5, 0.5)`, relative zoom 1. Clamp panning so no background enters a crop frame. Changing ratio retains the normalized focus and relative zoom, projects them to the new frame and marks the image for review if clamping changes its visible region. Keep the prior transform in the draft's one-step undo snapshot. Reset returns to centered minimum-fill crop; switching back to contain preserves the full source. Never bake intermediate crops into the original or reuse an already cropped preview as input.

In preserve-pixels crop mode, output the largest integer `p:q` raster that fits the selected visible source rectangle: `k=floor(min(visibleWidth/p, visibleHeight/q))`, output `p*k × q*k`. Zooming therefore reduces available native output resolution instead of silently upscaling back to the original padded canvas. Keep subpixel gesture coordinates in the plan; perform the one required final sampling only at export. A crop too small to satisfy a destination minimum is a conflict: offer less zoom, contain mode or explicit uniform enlargement. For an envelope-only destination such as `play-phone`, derive the initial crop ratio from the source aspect clamped to the permitted 1:2–2:1 interval. For an explicit exact-size crop, disclose any upscaling and require the same enlargement choice. A 1:1-scale integer-aligned source copy uses unfiltered pixel placement; filtering is used only for actual sampling changes.

### Overrides and global changes

Represent each editable field as `Inherit` or `Override(value)`. Do not copy global values into every image: that loses provenance. Fields include fit mode, crop transform, background, format, size policy and metadata policy. A session has a monotonically increasing revision; each item has a stable ID and revision.

| User action | Required reconciliation |
|---|---|
| Change a global setting | Apply only to inherited fields. Preserve overrides; display how many images use custom values. |
| Change destination | Re-evaluate every effective plan. Preserve compatible overrides. Reproject crop as above; flag incompatible codec/alpha/size overrides and changed crop framing. Never erase or replace them silently. |
| Apply batch settings to all | Explicit action listing the affected fields and number of overrides replaced; offer one-step undo. Do not use it implicitly when choosing a destination. |
| Reset one image | Change its selected fields to `Inherit`; show the resulting preview before export. |
| Import additional images | Apply current batch settings; existing IDs/order/overrides remain. Duplicate URI selections are de-duplicated within one import action; an explicit “Add another copy” is unnecessary for v0.1.0. |
| Remove/reorder an image | Update only that item/order, not index-based settings on other items. |
| Export while conflicts remain | Show review count and the conflicting items; do not silently skip them. “Export ready images” is an explicit separate action. |

Before export, freeze effective settings, source fingerprint, destination version, crop coordinates, resolved background, dimensions, codec choice and metadata policy in an `ExportPlan`. Editing the draft while a batch is running cannot modify that plan. Stale preview jobs are cancelled or discarded by revision ID; Export cannot use a stale preview as evidence of a newer plan.

## 8. Image formats: real capabilities and implementation choices

### Decision

**PNG is the absolute initial and new-session output default.** No automatic format selection, “modern format” substitution or remembered non-PNG default is permitted. A user can explicitly choose another offered format. An unavailable/incompatible selection stays visible with a reason and resolution options; it never silently becomes PNG or another format.

Use Android's bitmap codecs for the common paths, a small source-built libwebp bridge for API 26–28 lossless output, and AndroidX `HeifWriter` / `AvifWriter` backed by actual device encoders for HEIC/AVIF. These writers are not `Bitmap.compress`. Probe the required configuration and verify an encoded sample before advertising a device capability. API level alone is insufficient. Keep codec selection outside UI and geometry code.

Native HEIC/AVIF output is a **maximum-quality lossy** option in this release, not a lossless promise. A quality value of 100 does not remove chroma subsampling or guarantee identical pixels. The normal screenshot-fidelity path remains PNG or lossless WebP. Do not label lossy HEIC/AVIF as equivalent to PNG. The output selector must explain the distinction and destination compatibility.

### Decode / encode matrix

All capability claims below are an implementation plan grounded in documented APIs; runtime status remains **not tested in Phase 0**. “Supported input” always also requires successful safe decode, supported static content/color and the resource preflight in §10.

| Format | Input path on minSdk 26+ | Output path | Fidelity and alpha | First-delivery availability / destination |
|---|---|---|---|---|
| **PNG** | Platform decoder, all supported APIs; orientation/color inspection | `Bitmap.compress(PNG)`; quality parameter is irrelevant. Verify actual PNG header and emitted color metadata. | Lossless coding of the rendered pixels; RGB or RGBA. For Play, require actual 24-bit RGB/no alpha, not just visually opaque RGBA. | Required everywhere; default. Play accepted subject to constraints; social templates use opaque PNG. |
| **JPEG** | Platform decoder; handle EXIF orientations 1–8 and ICC | **No output encoder exposed** | Already-lossy input does not regain information when saved to PNG. | Required input; no JPEG output or API-publishing workaround. |
| **WebP lossless** | Platform decode on API 26+ | API 30+: `WEBP_LOSSLESS`; API 29: legacy `WEBP` with quality 100, documented lossless behavior; API 26–28: source-built libwebp lossless RGBA encoder | Real lossless mode, including alpha. Effort is not quality. Test decoded canonical RGBA; invisible RGB under zero alpha may be normalized, and color conversion is a separate disclosed transform. | Required output across supported APIs. Custom/file sharing; not offered as Play-compatible. Social upload acceptance unverified. |
| **HEIC / HEIF** | API 28+ platform `ImageDecoder` only when supported; inspect primary still image; API 26–27 unavailable unless a separately justified software decoder is included | API 29+ `HeifWriter`, single image, bitmap/surface input, native HEIC or HEVC-backed route. Probe exact width/height/grid/profile. `.heic`, `image/heic`; accept `.heif` input when content identifies a supported still image. | Chosen path: opaque 8-bit SDR, maximum-quality lossy encoding, typically YUV 4:2:0. No alpha or lossless guarantee. HEIF is a container, not a synonym for one codec. | Required feature on validated capable devices in v0.1.0. Disabled with a precise reason elsewhere. Not a Play/social compatibility claim. |
| **AVIF** | API 31+ platform baseline 8-bit decode; optional higher-depth behavior must be checked separately | API 34+ `AvifWriter`/MediaCodec AV1 route, single still. Probe configuration and encode/decode result, not merely decoder MIME support. `.avif`, `image/avif`. | Chosen native path: opaque 8-bit SDR, maximum-quality lossy; no native alpha/lossless promise. AVIF as a format can support more than this adapter. | Required feature on validated capable devices in v0.1.0. Earlier APIs have no promised native writer. Destination acceptance is independent of local encoding. |
| **JPEG XL / JXL** | No baseline Android platform guarantee. If included, use the same vetted software implementation for encode and decode. | Evaluate official `libjxl` C API, version 0.12.0 or a later audited security-fixed stable version, source-built JNI; lossless distance 0 | Lossless RGB/RGBA possible; explicit color/alpha handling required. Do not rely on JPEG reconstruction mode to preserve source metadata. | Conditional inclusion gate below. `.jxl`, `image/jxl`; gallery/recipient support not assumed. No initial branded-destination compatibility. |
| Other static raster formats | Accept only formats safely decoded by the existing platform path, with a recognized MIME and bounded dimensions (e.g. static BMP/GIF where supported) | Convert to a selected supported output | Never silently flatten animation or choose one frame from multi-image content | Optional incidental input support, documented per tested format. Reject animated GIF/WebP/APNG, SVG, PDF, RAW, video and ambiguous multi-image content with a localized explanation. No extra general-format dependency collection. |

Android's current format table documents HEIF decoding from API 28, HEIF encoding through MediaCodec/HeifWriter from API 29, AVIF decoding from API 31 and encoding through MediaCodec from API 34 [S09]. Those minimums do not prove a particular device configuration works. PNG/WebP API details are in [S10]. The platform may change capabilities through an OS update, so invalidate cached probes on app/OS/codec-version changes.

### Native writer contract and version gate

Use `androidx.heifwriter:heifwriter:1.2.0-rc01` as the initial Phase 1 candidate, explicitly recording that it is a release candidate. The observed stable release was 1.1.0; the candidate's release notes include color-aspect, AVIF MIME/buffer and EXIF-bound fixes relevant to this application [S11]. Compare against stable 1.1.0 in the codec spike. If a stable successor containing these fixes is available at implementation time, prefer it after the same tests and record the exact version. Do not silently accept an older color defect to avoid the prerelease label, and do not claim a release candidate has passed merely because it builds.

Source review of `HeifWriter`, `AvifWriter` and `EncoderBase` confirms the sequence **construct → start → add bitmap/buffer/surface image → stop with timeout → close**, native encoder selection and distinct color handling [S12]. Implement one still per file, explicit orientation normalization, a bounded timeout, resource closure in `finally`, cancellation and error reporting. Use SDR 8-bit sRGB input; verify the encoded color tags and decoded appearance. Codec alignment or grid tiling must not change visible dimensions without disclosure. Odd dimensions, padding tiles and final crop/display bounds are mandatory fixtures.

A probe returns `Available(profile)` or a stable reason such as `API_TOO_LOW`, `ENCODER_MISSING`, `UNSUPPORTED_DIMENSIONS`, `COLOR_VALIDATION_FAILED`, `ENCODE_FAILED`, `RESOURCE_LIMIT` or `DESTINATION_REJECTS_FORMAT`. Probe tiny synthetic content without user data and perform a real render/export validation for the selected size. Never cache “all sizes work”. A failed user export does not downgrade to a different format.

### Library alternatives and costs

No APK size, device memory or throughput benchmark was executed in Phase 0. The relative costs below are architectural observations; the numeric budgets that follow are **project acceptance targets**, not measured third-party performance.

| Route | Maintenance / licensing evidence | APK, memory, performance and build implications | Decision |
|---|---|---|---|
| Platform codecs + AndroidX writers | Android/AndroidX primary docs and source [S09–S12]; AndroidX Apache-2.0 | Small Java/Kotlin dependency overhead; no bundled HEVC/AV1 encoder. Device-dependent speed, color and capability; surface/YUV buffers still consume memory. | INCLUDE, with device/configuration gate. Lowest additional native maintenance for desired HEIC/AVIF. |
| **libwebp** | Maintained WebP reference library; BSD-style license; explicit lossless encoder API [S13] | Small focused C/C++ JNI boundary, but ABI and 16 KB checks still required. Build only encode functions needed for API 26–28; no command-line tools or unrelated image loaders. | INCLUDE for true lossless output on Android 8/9; pin released source/checksum in Phase 1. |
| **libavif + libaom** | Maintained C API, BSD-2-Clause libavif; separately audit codec notices [S14] | Source CMake build plus AV1 codec; slower software encoding and larger native package/working set than platform route. Supports alpha and lossless configurations when correctly configured, including RGB/YUV conversion constraints. Its provided Android JNI module is **decoder-only**, so it is not a drop-in Android encoder. dav1d/libgav1 are decoders, not writer substitutes. | Bounded fallback if native AVIF fails on required test hardware, or later demand justifies older-API/alpha support. A custom bounded JNI encoder adapter is real work. |
| **libheif + HEVC codec** | libheif LGPL; libde265 decoder; x265 GPL or kvazaar BSD encoder options [S15] | Container + encoder + optional decoder increases source-build, ABI, licensing and memory surface. Disable unused plugins/codecs/tools; do not bundle every advertised format. Patent availability must not be inferred from an open-source license. | Fallback investigation if native HEIC fails; prefer avoiding a bundled HEVC stack in the first APK when the native route works. No claim of universal API 26 support. |
| **awxkee/avif-coder** | Actual Android HEIC/AVIF encode/decode wrapper, API 24+ claimed; repository and README reviewed [S16] | Convenient API, but current transitive Rust/native stacks, HDR/color paths and byte-array output require audit. Byte arrays can duplicate a whole compressed output in memory. Published artifact claims do not prove reproducibility or 16 KB compatibility. | Evaluate as fallback prototype, not an automatic opaque AAR dependency. Compare its pinned sources/licenses/bytes with the direct route before adoption. |
| **libjxl** | JPEG publishes it as reference software; BSD-3-Clause; v0.12.0 observed released 2026-07-01 with an upstream security-update recommendation [S17] | C++/CMake, Highway/Brotli and a meaningful native build surface. Disable tools/tests/extra input codecs for shipping; preserve source build and notices. Encoder effort and memory must be bounded. | Conditional first-delivery gate; do not describe integration as trivial. |
| **awxkee/jxl-coder** | Android wrapper with real encoding APIs; current backend differs from simply bundling the reference libjxl [S18] | More convenient JNI API but additional dependency/toolchain and byte-array costs. API 21+ claim is not a measured support guarantee. | Comparison candidate only; direct reference implementation is the preferred auditable spike. |

### Phase 1 codec gate (G1)

Before building the complete UI, make a small test harness for each chosen path and record: exact versions/source SHAs/licenses, ABI sizes before/after compression, build duration and toolchain, peak Java/native/PSS memory, output dimensions/color/alpha/metadata, decoded pixel results, p50/p95 encoding time and cancellation. Use at least 20 synthetic and consented real screenshot samples, including 1344×2992, flat colors, one-pixel colored text, dark UI, alpha, odd dimensions and EXIF rotations. Report cold and warm runs separately.

- Mandatory PNG and WebP must pass exact decoded-pixel checks for opaque sRGB fixtures without geometric changes. Validate the actual lossless WebP bitstream, not a quality label.
- HEIC and AVIF must actually encode, reopen and display correctly on the Pixel 9 Pro XL and one additional supported hardware family before `0.1.0` claims these routes. Do not substitute an emulator's codec availability for the Pixel result. If hardware is unavailable, label the gate NOT RUN and keep the build at an incomplete alpha/beta status.
- For the selected lossy route, use calibrated solid-color patches (median channel error ≤2/255, no systematic wrong transfer/range), record PSNR with a ≥38 dB regression target on the fixture set, and manually inspect small colored text/edges. These are project targets, not a losslessness claim; a metric alone cannot approve appearance. A missed PSNR target requires a documented visual review of the affected fixtures and the cause, rather than an automatic codec exclusion. Wrong transfer/range, corrupt output or visibly unacceptable text remains a failure: disable that route and exercise the fallback; do not present it as screenshot-faithful.
- Project budgets: universal release APK target ≤40 MiB; optional JXL incremental compressed cost ≤12 MiB across shipping ABIs and ≤5 MiB for arm64 alone; 4 MP screenshot p95 ≤5 seconds for PNG/WebP and ≤15 seconds for native HEIC/AVIF/JXL on the Pixel, excluding external provider I/O. Keep each job inside the dynamic memory budget in §10. Revisit a failed target with measured evidence; do not silently lower image quality or remove a mandatory format.
- JXL is included only if it builds from pinned source on all shipping ABIs, passes license/security/16 KB/fidelity checks and fits those incremental cost, latency and memory budgets. Otherwise record the measured reason and **DEFER JXL**, as the user's proportionality condition allows. “It is hard” without a bounded build/measurement attempt is insufficient. Do not ship a dead JXL selector as implemented support.
- If the Pixel's native HEIC/AVIF route fails, attempt the corresponding bounded software fallback above, measure it and retain the exact failure evidence. A decision to ship the first functional version without the desired HEIC or AVIF feature, or to exceed a material package/maintenance budget, requires explicit scope resolution; it is not a routine implementation shortcut.

The first release consequently supports minSdk 26 for the application and PNG/WebP, while HEIC/AVIF availability is device/API-specific. This is a visible capability limitation with alternatives (PNG/WebP or another capable device), not a silent format replacement or a promise that every Android 8 device can write every format.

## 9. Metadata, orientation, color and minimum recompression

### Default privacy policy

Produce new copies with identifying metadata removed by default: EXIF camera/owner/serial/GPS/timestamps, XMP/IPTC descriptions and identities, embedded thumbnails/previews, comments and arbitrary PNG/WebP/container text. Generate neutral output names rather than reusing source filenames. Do not add a username, device model, original path or editor history. Technical width/height, necessary color information, codec/container structure and normalized orientation are not personal metadata and may be retained or regenerated.

Metadata removal does not redact text or identities visible in image pixels. Explain that briefly in offline help; no OCR/redaction tool is introduced. Do not log full content URIs, source names, image bytes or metadata. Diagnostics are local, redacted and opt-in for manual sharing.

### Opt-out and explicit preservation limits

“Remove identifying metadata” can be disabled for the current session or image. Explain that some original metadata may then be retained. The off state is not remembered for a new session. Provide a short before-export description of what this selected output can preserve.

Preservation is **best effort through a documented allowlist**, never an opaque blob copy after rendering:

- For PNG and WebP, use AndroidX ExifInterface 1.4.2 or a later audited stable release for supported EXIF fields. Preserve supported source capture time, camera/lens information, artist/copyright and GPS only when the user explicitly opts out; record the exact supported tag set in tests and help. Recompute dimensions and normalize orientation to 1/absent. Do not preserve MakerNote, stale thumbnails, image digests, arbitrary XMP/IPTC or source filename/path.
- For native HEIC/AVIF in v0.1.0, do not promise general metadata round-trip. The initial adapter preserves only necessary technical/color information; the opt-out UI reports that other source metadata cannot currently be carried into this output. ExifInterface reads more formats than it can write: it writes JPEG/PNG/WebP, not arbitrary HEIC/AVIF [S19]. A future bounded EXIF serializer passed to `addExifData` can extend this, but it is not assumed to exist. Offer the user an explicit switch to PNG/WebP if metadata preservation matters; no automatic substitution.
- If JXL is included, default removal is mandatory; identifying-metadata preservation may remain unsupported with the same explicit explanation. Do not embed the complete original JPEG for reconstruction as an accidental privacy bypass.

The settings control is operable in every session; an unsupported preservation route shows the limitation before export. No file is represented as preserving all original metadata. Default-stripping tests inspect the output container independently of the UI setting.

### Appearance contract

1. Inspect bounds, format, animation/multi-image status, EXIF orientation and color descriptors before full decode. Apply all eight EXIF orientation cases once; do not rotate twice when an API already applied it.
2. For the initial rendered pipeline, use **8-bit SDR sRGB**. Recognized wide-gamut SDR images are color-converted to sRGB using a tested color-managed path and tagged correctly; indicate this conversion in export details. Do not drop ICC and reinterpret values unchanged. Color-space transforms and geometric scaling are distinct from whether a codec is lossless.
3. Detect HDR, gain maps and unsupported high-bit-depth content. Do not silently flatten or clip it. If a tested SDR conversion path exists, offer it explicitly; otherwise reject that item with a clear reason while retaining the rest of the batch. Supporting all photography HDR formats is outside the initial screenshot scope.
4. Keep only validated appearance-critical profiles or replace them with a canonical sRGB description; sanitize free-text/profile identity fields. Enforce profile byte limits and reject malformed descriptors. For social/Play presets always produce the documented sRGB/opaque result. Compare preview and decoded output on a color-managed reference.
5. Avoid platform-dependent premultiplied-alpha halos: composite in the declared color pipeline, unpremultiply only when the encoder API requires it, and test transparent edges over both light and dark backgrounds. Pixel equality tests use the declared normalized representation; fully transparent hidden RGB is not a recoverable-content guarantee.

### No unnecessary recompression

Do not decode/re-encode an unchanged file just because Export was tapped. Implement an **eligible no-op path** for PNG and lossless WebP when dimensions, pixels, orientation, destination, format and color policy already agree. If metadata removal is on, use a bounded container sanitizer that copies image payload chunks without recompressing them and drops disallowed ancillary data. Validate lengths, ordering, CRC/container sizes and allowed critical chunks with established format readers; unknown unsafe content fails closed to the ordinary validated render path, with the reason recorded. Never strip orientation/color before deciding whether a render is necessary.

If metadata removal is off and the file is already fully compliant, a byte-for-byte copy may be used only when the preservation policy is explicitly compatible with that copy; otherwise sanitize to the stated allowlist. Do not use direct-copy as a way to carry unknown metadata under a “metadata removed” claim. Native HEIC/AVIF can use an exact byte copy only for a validated unchanged source and explicitly compatible metadata policy; container rewriting is not a first-version requirement. A conversion from JPEG to PNG is necessarily a new encoding and must not claim to restore lost JPEG quality.

For padded PNG output, rendering/re-encoding is expected but the unscaled opaque sRGB source rectangle must remain pixel-identical. If Android emits the wrong PNG color type or color information for a Play export, G1 must resolve it using a vetted minimal PNG writer (source-built libpng is the fallback), not a fake extension or a home-grown image codec. Tests parse IHDR and color chunks independently.

## 10. Import, memory, export and recovery

### Import and permission boundaries

Use AndroidX Activity's Photo Picker contracts when available, with the documented `ACTION_OPEN_DOCUMENT` fallback on older/unsupported devices [S20]. Do not add a proprietary services dependency just to obtain the picker. Support `ACTION_SEND` and `ACTION_SEND_MULTIPLE`, `ClipData` and URI grants. Inspect and validate each `content://` URI; never turn an untrusted path/name into a filesystem destination. Do not fetch network URLs from shared text. Do not request broad image-library read access when a picker grant suffices.

MIME labels and extensions are hints: inspect signatures and decoder results. Reject unsupported/animated/malformed sources individually, retaining good items. Request persistable URI permission only when the provider offers it; a received temporary grant must be staged to app-private storage before it expires if draft recovery needs it. Non-seekable providers are copied through a bounded stream. The Android picker/provider may fetch cloud-backed files independently; FixMyShot itself has no network permission and cannot guarantee an external provider's offline availability. Local inputs must work in airplane mode.

Use generic localized error categories with a useful action: unsupported format, animation not supported, unreadable/revoked source, too large for this device, malformed file, insufficient space, encoder unavailable, incompatible destination, export interrupted. Keep technical stack traces out of product messages. Import additions while exporting affect the next draft, not the frozen running job.

### Resource contract

Bound work before allocating full-size pixels. Read headers first with checked 64-bit multiplication and a compressed-input byte limit. Initial safety ceilings: 100 items per active batch, 100 MiB per compressed input, 64 megapixels declared source dimensions, 24 megapixels rendered output, and 1 GiB total private staging. These are documented protective ceilings, not advertised support for every file below them. A lower dynamic device budget may reject an otherwise in-range image. Explain the failing limit and offer explicit smaller output or fewer items; do not silently reduce resolution or truncate the selection.

Process **one full-resolution export at a time**, with at most two small preview decodes. Bound the thumbnail cache to 24 MiB and trim it on memory pressure. Keep thumbnails around 256 px and main previews within 1600 px on the long edge; never allocate one full bitmap per batch item. Do not copy whole outputs into Kotlin `ByteArray` if the encoder can write a file descriptor or stream. Preview sampling and final rendering must use the same geometry/color contract.

Set a job working-set budget initially to the smaller of 256 MiB and half the process's available heap class, with an additional native/PSS budget measured by the codec gate. Compute input/output bitmap residency, temporary buffers and codec estimates before starting. Account for native allocations separately; `Bitmap` Java-heap measurements alone are insufficient. If a full-resolution image cannot fit, use a proven sampled/region decode only when it implements an explicitly accepted resize; otherwise stop that item with an explanation. No `largeHeap` workaround. Catch expected decode/codec exceptions, close resources and continue valid batch items; process-level OOM is a design failure to prevent, not the normal error handler.

### Output transaction

Default location is **`Pictures/FixMyShot`**. Keep originals read-only and never overwrite them. Output names use `FixMyShot_<UTC timestamp>_<short random ID>.<extension>`; avoid source names and collision-prone counters alone. Timestamp in a new filename describes export time, not recovered source metadata. A batch manifest remains app-private, never alongside the user's images.

- **API 29+**: insert an owned MediaStore item with `RELATIVE_PATH=Pictures/FixMyShot`, correct MIME/name and `IS_PENDING=1`; encode or copy from a private validated temporary file, flush/close, validate the output, then publish with `IS_PENDING=0`. No broad storage permission is needed to create the app's own media [S21]. Keep the pending URI in the recovery journal before writing.
- **API 26–28**: request `WRITE_EXTERNAL_STORAGE` only when the user first chooses to save to Pictures, with `maxSdkVersion=28`; use a unique temporary file in the target directory, close/validate, rename to the final name and invoke MediaScanner. If permission is denied, offer the system document/tree picker. A user-selected SAF folder is an explicit alternative location, not a claim that it is `Pictures/FixMyShot`.
- Some OS/media providers cannot index a MIME such as JXL, or a gallery app may not display a valid file. Prefer MediaStore when it correctly supports the type; otherwise use an explicit SAF destination and explain gallery visibility limits. Do not save unknown types into the image table with a false MIME. HEIC/AVIF gallery visibility is tested per device. A correctly saved file and gallery visibility are separate results.
- Preflight available space where meaningful, retaining headroom for temporary output. A provider may still run out of space mid-write; handle the real exception and clean only the app-owned unfinished entry. Never delete user-selected originals or unrelated files.

Validate encoded MIME/signature, dimensions, decodability with a compatible decoder, color/alpha constraints and metadata policy before publishing. Full pixel comparisons belong in tests, not every user export; runtime validation uses bounded checks. A successful item is committed once. Failures remove only its unfinished output. Cancellation stops before the next item and cooperatively interrupts the current operation where supported; completed images stay saved. If an encoder cannot stop instantly, show that it is finishing/cancelling and enforce its timeout.

### Job state and process death

Use a small versioned app-private JSON journal, atomically replaced, for the current session and export job. Room, a server and a general job platform are unnecessary for this scope. Store stable item IDs, required URI grants/staged-source references, effective plans, owned temporary/pending output IDs and statuses, not raw EXIF or a history of all images ever processed.

State transitions are `READY → ENCODING → VALIDATED → PUBLISHED` or `FAILED/CANCELLED`. Record the output URI before attempting publication. On restart, inspect only recorded owned outputs: if an item is already published and valid, mark it successful; if unfinished, clean or offer retry. Do not blindly rerun the whole batch. If a provider gives an indeterminate result, show “Check output” with the known URI and obtain an explicit retry to avoid duplicates. External source changes invalidate the item's fingerprint and require re-import/review.

Execution is foreground-oriented in v0.1.0. Rotation retains work in a ViewModel; leaving the application pauses before the next item and allows the current bounded transaction to finish if the process remains alive. Do not promise background completion. Show this behavior in the progress UI. No foreground-service or notification permission is needed for this first design. Process death is recovered through the journal, not an invisible auto-retry. The user explicitly resumes remaining items.

Purge private staged sources, thumbnails and unfinished temporary files when the session is discarded or successfully completed. Keep only a short result list until the user leaves Results; copies in Pictures/SAF remain under user control. On startup, purge abandoned private session data older than 24 hours after reconciling its pending outputs. This is cleanup on the next app start, not a background promise that disk bytes disappear exactly after 24 hours; explain that retained drafts can remain until reopening, explicit discard, clearing app data or uninstall. A visible “Discard session” clears remaining private working data. Disable Android cloud/device backup of session content and grant records; preferences may be backed up only through an explicit minimal allowlist. App-private data is not a secure-erasure guarantee on flash storage; no such claim belongs in privacy copy.

Share only successful selected outputs through content URIs and temporary read grants. Use `ACTION_SEND`/`ACTION_SEND_MULTIPLE`, correct single MIME or `image/*` for mixed images, and ClipData. Do not expose `file://` paths. No ZIP and no automatic posting to another application.

## 11. Android architecture and build contract

Kotlin + Jetpack Compose + Material 3 is selected. It supports the required minSdk 26 and localized/accessible state-driven interface without a cross-platform runtime. Keep the dependency graph small. Use stable AndroidX components except the explicitly justified writer candidate in §8.

### Modules and ownership

| Boundary | Owns | Must not own |
|---|---|---|
| `:core` (pure Kotlin/JVM) | Rational geometry, immutable plans, override reconciliation, preset schema, resource estimates and error/state models | Android UI, filesystem, codec implementations |
| `:app` | Compose screens, ViewModels/StateFlow, Android import/export adapters, locale/theme preferences, journal and lifecycle orchestration | Duplicate geometry or format fallback policy |
| `:codec-native` (small Android library) | Source-built libwebp/JNI; optional accepted JXL or fallback native codec adapters, native build definitions | Product UI, destination rules, network/download behavior |

Keep codecs behind `ImageDecoderAdapter`, `ImageEncoderAdapter`, `CodecCapabilities`, `MetadataPolicy` and `OutputSink` interfaces. `ImageHeader` describes oriented bounds, MIME, color/alpha/frame information and source fingerprint. `ExportPlan` is an immutable, validated value; `ExportResult` distinguishes saved, failed, cancelled and indeterminate. Adapters return typed failures rather than localized strings; the UI maps them to resources. Inject dependencies with simple constructors; no dependency-injection framework is needed. Use coroutines with an explicit bounded dispatcher and structured cancellation. Preferences use DataStore. Do not introduce Room, WorkManager, Hilt, networking libraries or a plugin system without a demonstrated need beyond this contract.

### Initial toolchain selection

Use a reproducible conservative starting set: JDK 17, AGP 8.13.2, Gradle 8.13, Kotlin/Compose compiler plugin 2.2.21, compileSdk/targetSdk 36, minSdk 26, and the stable Compose BOM 2025.12.00 (December release documented by Android). AGP documents this Gradle/JDK line and API support through 36.1 [S22]; these are deliberately compatible starting pins, **not claims that they are the latest releases**. Phase 1 must resolve all artifacts, check known security issues and compile both API 26 and current targets before freezing the catalog. If a dependency requires a newer compiler/SDK, update the compatible set together in a recorded decision rather than mixing incompatible versions. A newer current Kotlin release is not by itself a reason to add a migration to the critical path.

Pin exact AndroidX Activity/Lifecycle/DataStore/AppCompat and other required stable versions in `libs.versions.toml` during the bootstrap build, based on their resolved compatibility and release notes; no `+`, snapshots or unpinned Git commits. Pin SDK/build-tools, a source-build-capable stable NDK **r28 or newer**, CMake and native library revisions/checksums in the build manifest. This is a Phase 1 resolution deliverable, not permission to download a moving “latest” on every build. NDK r28+ and AGP 8.5.1+ provide a useful 16 KB starting point, but every shipped ELF and ZIP alignment still needs verification [S23].

Use only official trusted artifact repositories and pinned source archives; do not use JitPack as an unexplained binary origin. Commit the Gradle wrapper including distribution checksum, dependency verification metadata and lock/version catalogs. Source-built native dependencies must be available to independent builders; no opaque generated AAR without corresponding source/build instructions. Shipping ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64` in one universal APK. Do not silently drop 32-bit Android 8 devices or emulator/x86 coverage to pass a package-size target. Record any proposed architecture change for scope review.

Manifest inspection must show no `INTERNET`, `ACCESS_NETWORK_STATE`, broad storage/media read permission, all-files access, notification, camera or location permission. The sole planned legacy permission is storage write with maxSdk 28. Explicitly declare exported components required for launcher/share receiving; validate incoming intents. Use backup/data-extraction rules that exclude user images/session state. Baseline target behavior includes edge-to-edge/insets, predictive-back handling where required, rotation and multi-window without losing the draft.

### Proposed repository layout after implementation begins

```text
app/                         Android UI, adapters and resources
core/                        geometry, plans, presets and unit tests
codec-native/                bounded JNI/source-build integration
gradle/                      wrapper, version catalog, verification
scripts/                     localization, asset, release and format checks
docs/                        architecture, formats, testing, release and privacy owners
docs/blueprints/              this governing blueprint
fastlane/metadata/android/    canonical localized listing text/assets when produced
.github/workflows/           focused build/check/release preparation workflows
README.md CONTRIBUTING.md CHANGELOG.md PRIVACY.md LICENSE
```

This is a future layout, not files claimed to exist at the baseline. Do not generate empty infrastructure merely to mirror it.

## 12. Localization, accessibility and visual identity

### Language catalog

Use a small single language catalog patterned after the compatible CountAway concept, not its entire architecture. Android resources remain the canonical localized copy; catalog fields connect locale tags, resource qualifiers, endonyms, flags and future distribution locales.

| Language | Android resources / selection | Visible name / flag | Tagline | Future metadata mapping |
|---|---|---|---|---|
| US English | `values`, locale `en` | `English` / US flag | Screenshots, ready anywhere. | `en-US` |
| Argentine Spanish | `values-es`, locale `es`, copy written in Argentine Spanish | `Español` / Argentina flag | Capturas listas para publicar. | Play `es-419`; verify F-Droid locale mapping when used |
| Catalan | `values-ca`, locale `ca` | `Català` / traditional Senyera | Captures llestes per publicar. | `ca` |

Use `es` resources so Spanish system variants receive complete Spanish copy instead of falling back to English; the wording remains Argentine. Do not show regional qualifiers in the selector. “Follow system” is a separate localized choice; resolve the first supported system language, otherwise English. Manual choice must persist and change strings, plurals, help, errors and accessibility labels consistently without clearing the session. Use AppCompat per-app locales on older APIs and platform per-app locale support on API 33+, with one synchronization path. Test process restart and a manual-to-system transition.

All three languages ship offline in the first functional APK. For any future AAB, disable language splitting or otherwise demonstrate that every language remains available offline; do not depend on a Play download after changing language. Flags are bundled vector assets with provenance, not emoji whose appearance varies by device; the Catalan graphic is the traditional striped Senyera. Flags supplement names and are decorative to screen readers. Product name remains FixMyShot everywhere.

The localization validator must parse Android XML rather than compare raw text. Check missing/unexpected/duplicate keys, untranslated entries except an explicit short invariant allowlist (name, URLs, technical identifiers), placeholder types and indexes, positional formatting, plurals, arrays, markup/escaping and catalog/resource consistency. Cover error codes, content descriptions, hints, help and privacy text. CI validates every locale and the final APK's packaged languages. Automated checks verify completeness/structure, **not translation quality**: manually review Argentine wording and Catalan grammar, including expanded text and plural forms. Contributors can edit resource files without having to run Android Studio; `CONTRIBUTING.md` documents the standalone validator.

### Accessibility and presentation acceptance

Material 3, restrained surfaces and a clear primary action; light, dark and system themes. Avoid a decorative editing toolbox. Controls have at least 48 dp touch targets, readable contrast, focus order and meaningful TalkBack labels/state descriptions. Announce selected item, batch position, output dimensions, crop mode, custom overrides and errors. Do not encode failure/selection only in color. Do not announce every progress tick; announce meaningful state changes. Crop must be usable without multi-touch. Respect reduced animation/system animation settings, font scaling to 200%, landscape, keyboard/switch navigation and display cutouts. Text must not overlap the preview/export action.

Use a provisional development icon and explore a capture frame with a precise central “shot” alignment motif. Finalize an original, licensed logo before `1.0.0`; test adaptive launcher masks, monochrome/themed icon and small sizes. Do not present provisional artwork as a completed brand. Optional About behavior: five activations of the brand mark may play a brief frame-centering pulse confined to the mark; provide an equivalent static response with animations disabled. It must not change export settings, require network, display an invented reference or delay a task. This is optional polish, not a functional-release gate.

### Official screenshots versus user adaptations

User exports may intentionally add margins or crop as described above. **Official FixMyShot screenshots must be authentic captures of the actual build at a suitable viewport**, never old screenshots stretched/padded/repainted or reconstructed to look current. Plan genuine portrait 1080×1920 RGB PNG captures, or genuine landscape 1920×1080 when useful, with four meaningful views if practical. This is a presentation plan, not a universal store minimum. Use synthetic non-sensitive content and truly localized UI.

When store assets are eventually produced, keep one canonical set per locale/device class under `fastlane/metadata/android/<locale>/images/phoneScreenshots/` and reuse it from README where sensible. Temporary store-specific packages come from that source; no independent second gallery. Preserve accepted user-provided captures unless replacement is approved. Inspect actual image bytes from the exact release candidate for truthful UI, version, locale, order and consistency with final public copy before merge/publication. Mechanical dimension checks cannot replace visual review. Future catalog submissions remain separately authorized and do not gate the first GitHub build.

## 13. Quality, documentation, distribution and maintenance

### Focused CI

Once implementation is authorized, create a small GitHub Actions workflow for pull requests/pushes that runs localization/catalog validation, pure Kotlin tests, Android lint, debug build, release compilation without secrets, native build checks and focused emulator instrumentation. Pin third-party actions by immutable commit; give read-only default workflow permissions. Do not expose signing secrets to pull requests or untrusted code. Use caches keyed by dependency/toolchain lock data. Documentation-only changes need documentation/link checks, not an expensive device matrix when no relevant inputs changed.

Test APIs 26, 29/30, 33, 34 and the current supported target through an efficient matrix: fast JVM checks on every change, representative emulators for import/storage/locale/lifecycle changes, and full release gates for candidate commits. Physical codec/visual results remain separate. Do not report an emulator boot or compilation as codec acceptance. A change invalidates only the affected evidence, but all release claims must be tied to the exact candidate SHA.

Keep fixtures synthetic or explicitly approved for redistribution. Fixtures must include known metadata/orientation/color cases and malformed input. Never commit the user's actual private screenshots by default. Provide a local diagnostic export only through deliberate user action and sanitize it before suggesting a public bug report.

### Required documentation owners

| File | Contents by functional-delivery gate |
|---|---|
| `README.md` | Focused product explanation, honest current state, screenshot(s), GitHub installation, Obtainium source, supported Android/API-dependent formats, short language/privacy/license/author/donation information. No claim that an unpublished release or undeployed site is live. |
| `CONTRIBUTING.md` | Build/test commands, architecture entry points, translation workflow/catalog/validator, issue reproduction with redacted data, GPL/contribution expectations, code style and source-built codec guidance. Reuse CountAway's contributor-friendly translation concept. |
| `CHANGELOG.md` | Real changes by version; distinguish unreleased work and published versions. Do not invent a prior application release. |
| `PRIVACY.md` plus bundled localized help | Local processing, permissions by API, metadata defaults/limits, staging retention/cleanup, external picker/share/browser behavior, no network/accounts/ads/analytics/subscriptions, and how to discard session data. Online `/privacy/` is planned; offline privacy must work without it. |
| `docs/FORMATS.md` | Tested read/write/API/device matrix, lossy/lossless distinctions, alpha/color/metadata limits, native/library versions and unsupported cases. |
| `docs/TESTING.md` | Reproducible test commands, fixtures, automated vs manual ownership, device matrix, exact-SHA evidence and known limitations. |
| `docs/RELEASING.md` | Versions/ledger, source/unsigned/signed artifact relationship, stable signing identity, checksums, rebuild procedure, Obtainium install/upgrade verification and publication authority boundary. |
| `docs/DECISIONS.md` | Only material measured decisions, such as codec gate results, JXL inclusion/deferral and justified deviations. Avoid a second copy of mutable product requirements. |
| `THIRD_PARTY_NOTICES` / machine-readable dependency manifest | Licenses, exact sources, build-time/runtime scope and corresponding-source locations for bundled dependencies. |

Technical owners are in English. User-facing help/privacy/error explanations are complete in all three languages. README may contain concise localized introductions; it need not duplicate the full engineering documentation three times. The planned website can later mirror the approved privacy copy through a separately authorized deployment. Do not invent an email/support identity from the domain.

### Signing, reproducibility and release artifacts

There is no existing FixMyShot signing identity to reuse. Phase 0 does not create one. Before the first externally distributed production-ID APK, obtain the separate required authorization to establish the long-lived signing key and secure backups. Preserve one independent FixMyShot certificate across prereleases and v1.0. Never borrow CountAway's key or silently rotate/regenerate a missing key. Record only its **public certificate fingerprint** in release evidence. Key/password custody is outside repository, documentation and continuity records.

Build an unsigned release candidate from a clean, immutable source commit using pinned tools and source-built native dependencies. Rebuild independently in a clean environment; compare unsigned APK bytes. Diagnose differences in ZIP ordering, timestamps, generated paths, resource packaging and native stripping. Do not claim reproducibility from two builds that reuse the same output/cache. APK reproducibility is a build property, not a promise that hardware codecs create byte-identical user images on every phone. F-Droid's reproducibility guidance is a useful engineering reference, not a submission requirement [S24].

After authorization and acceptance, sign the exact verified artifact with the established key, verify signature/package/version, and retain a non-secret receipt binding source SHA, unsigned/signed SHA-256, toolchain and certificate fingerprint. Signing is not permission to merge or publish. Plan one universal asset named `FixMyShot-<versionName>-universal.apk`, plus `SHA256SUMS.txt`, `BUILD-INFO.json` and corresponding source/build instructions (including pinned native dependency sources). Do not attach several indistinguishable APKs or mark a debug APK as the release.

Run `apksigner verify --verbose --print-certs`, package manifest inspection, APK ZIP/ELF 16 KB alignment checks and a fresh installation/upgrade test on the exact signed bytes. Use signing schemes suitable for minSdk 26 and the future distribution target; do not copy a signature onto different bytes. The release candidate must already have coherent README/current-version links, changelog, About version, assets and release notes before a later authorized merge/publication. Historical changelog references stay historical. Keep wording accurate during the merge-to-publish interval.

### Obtainium and future stores

The GitHub repository itself is the Obtainium source: `https://github.com/santirodriguez/FixMyShot`. Document adding that URL, enabling prereleases while using the 0.x channel, and selecting the universal release APK if filtering is needed. A proposed stable APK filter is `^FixMyShot-.*-universal\.apk$`; validate it against actual asset names. Verify **install and update** between two successively versioned APKs signed with the same certificate. Obtainium tracks releases directly; optional community-catalog inclusion is not needed [S25]. Do not create a catalog request in this workstream.

Future F-Droid compatibility means FLOSS dependencies, complete source/build instructions, no opaque proprietary SDK, reproducible-build effort and compatible metadata. Its inclusion policy must be rechecked when an actual submission is authorized [S26]. Future Google Play compatibility means sensible current target SDK, proper package/signing planning, privacy and localized assets, 16 KB native support and eventual AAB validation. Do not set up Play App Signing, upload keys, Console tracks, store accounts or a second store automation project for the first GitHub delivery. A catalog rejection, age/star threshold or website availability cannot block it.

### Maintenance

Before releases, review native dependency security advisories, license/source availability, destination evidence dates, Android target requirements and broken public links. Keep native source versions pinned and update deliberately; unknown codec input is a security boundary. Add a new preset only with a concrete screenshot use case and a documented surface/output contract. Add a new format only with independent read/write/fidelity/cost evidence. No automatic remote catalog or dependency downloads at application runtime.

## 14. Findings and disposition

There are **no confirmed application bugs at the inspected baseline**, because no application exists. The table classifies design and integration findings without relabeling missing implementation as a defect.

| ID | Classification | Decision | Finding and implementation consequence |
|---|---|---|---|
| F01 | NOT_AN_ISSUE | NOT_JUSTIFIED | Empty app/build/test baseline is expected for this first implementation. Do not fabricate bug fixes or passing tests. |
| F02 | IMPROVEMENT_OPPORTUNITY | INCLUDE | Focused import → destination → preview → export flow addresses the stated friction; avoid a general toolbox. |
| F03 | PLAUSIBLE_RISK | INCLUDE | Global batch updates can destroy individual work; use typed inheritance, reconciliation and review flags (§7). |
| F04 | PLAUSIBLE_RISK | INCLUDE | Preview/export divergence, orientation and rounding can cut content; use one plan and exact geometry fixtures. |
| F05 | PLAUSIBLE_RISK | INCLUDE | Hardware encode support is not decoder support or an SDK-only fact; require G1 native probes and physical-device evidence. |
| F06 | PLAUSIBLE_RISK | INCLUDE | Quality 100 and modern formats can still lose screenshot detail; label native HEIC/AVIF lossy, keep PNG default and test color/text. |
| F07 | PLAUSIBLE_RISK | INCLUDE | Metadata stripping can rotate images or alter color; normalize orientation and retain/regenerate appearance-critical data. |
| F08 | PLAUSIBLE_RISK | INCLUDE | Temporary sources, thumbnails and crash recovery can retain private images; private journal, backup exclusion and explicit cleanup limits. |
| F09 | PLAUSIBLE_RISK | INCLUDE | Batch OOM, malicious dimensions and slow encoders; preflight, sequential full-res work, bounded caches/timeouts and per-item errors. |
| F10 | PLAUSIBLE_RISK | INCLUDE | Partial exports, denied legacy storage and process death can lose/duplicate outputs; per-item transactions and recovery. |
| F11 | PLAUSIBLE_RISK | INCLUDE | Equal ratios do not establish equal destination compatibility; offline typed catalog with evidence and surface separation. |
| F12 | IMPROVEMENT_OPPORTUNITY | INCLUDE | Eligible PNG/WebP payload-preserving export avoids unnecessary recompression. |
| F13 | IMPROVEMENT_OPPORTUNITY | INCLUDE | Complete three-language catalog/validators and accessible crop controls prevent a language/gesture-only first release. |
| F14 | PLAUSIBLE_RISK | INCLUDE | Native libraries can break ABI/16 KB/reproducible builds; source pins, measured size and ELF/ZIP/runtime checks. |
| F15 | IMPROVEMENT_OPPORTUNITY | INCLUDE | One universal APK, stable signing and explicit prerelease instructions simplify Obtainium updates. |
| F16 | IMPROVEMENT_OPPORTUNITY | DEFER | Universal software HEIC/AVIF across all Android 8 devices adds substantial dependency/maintenance scope; keep native first and invoke bounded fallback only if the required route fails. Document per-device availability. |
| F17 | IMPROVEMENT_OPPORTUNITY | INCLUDE | Evaluate JXL in G1; shipping inclusion is conditional on measured proportionality. Record DEFER only if that bounded attempt fails its stated gate. |
| F18 | IMPROVEMENT_OPPORTUNITY | DEFER | TikTok ads/extra branded presets lack a justified screenshot-specific first-release need or relevant verified organic rules. Custom ratios remain available. |
| F19 | NOT_AN_ISSUE | NOT_JUSTIFIED | Optional directory inclusion, store approval and a deployed website are not first-GitHub-release prerequisites. |
| F20 | NOT_AN_ISSUE | NOT_JUSTIFIED | Competitor catalog size is not a requirement to add filters, blur, AI, video, drawing, JPEG output or ZIP. |
| F21 | PLAUSIBLE_RISK | INCLUDE | Metadata opt-out can be mistaken for exact preservation; show format-specific allowlist/limits before export. |
| F22 | PLAUSIBLE_RISK | INCLUDE | Official promo images could misrepresent app UI if adapted like user files; capture genuine current viewports and inspect exact candidate image bytes. |

## 15. Executable phases, dependencies and gates

Every phase ends with a concrete artifact and evidence. Record `PASS`, `FAIL`, `NOT RUN` or `NOT APPLICABLE`, exact source SHA, environment/device and evidence location. A proposed test is not a passed test. No phase can erase a mandatory requirement to obtain a green status. Keep implementation on the established work branch unless later collaboration genuinely requires a separately authorized branch strategy.

| Phase | Depends on | Deliverables / work | Exit acceptance |
|---|---|---|---|
| **0 — Investigation and blueprint** | Authorized research | Baseline inventory, competitor/source research, decisions, this file, one branch, verified persistence and adoption-ready continuity | Complete document retrieved from immutable commit; coverage review; same continuity workstream reconciled. No application implementation. |
| **1 — Bootstrap and codec feasibility** | Adoption + Phase 0 | Minimal Android/Kotlin modules and reproducible toolchain, source/dependency pins, capability adapters, G1 fixture harness and benchmark report, JXL decision, provisional icon | `assembleDebug`, unit-test harness and lint run; API 26 startup; PNG/WebP true encode/decode; native HEIC/AVIF route evidence and explicit unavailable paths; native ABI/16 KB/source/license cost results. Hardware-unavailable items stay NOT RUN. Resolve mandatory codec failures before declaring first-delivery feasibility. |
| **2 — Geometry, color and privacy core** | Phase 1 decisions | Shared planner/renderer, crop math, metadata allowlist/sanitizers, sRGB/orientation pipeline, no-op paths, versioned offline catalog | G2: property/fixture tests for contain/crop/overrides; exact Pixel dimensions; source-rectangle equality; all orientations; metadata absence/preservation limits; color/alpha and unsupported-HDR behavior. |
| **3 — Import and review experience** | Phases 1–2 | Picker/share-intent adapters, preview scheduler/cache, single/batch UI, global/individual reconciliation, themes, all three locales, accessible controls | G3: full interaction on API 26 and a current device; no stale preview; no silent override reset; language completeness; TalkBack/large text/manual locale checks. |
| **4 — Reliable export and sharing** | Phases 2–3 | MediaStore/legacy/SAF transactions, sequential queue, cancellation/retry/results, journal/recovery/cleanup and share chooser | G4: 100-item mixed-result exercise; cancellation/process-kill/low-space/provider-denial cases; no original mutation, partial-file leak or duplicate success; correct Pictures path or explicit alternate; gallery/receiver tests per format. |
| **5 — Functional acceptance and hardening** | Phases 1–4 | Targeted CI, actual Pixel and additional-hardware runs, usability/locale/color review, resolved blocker log, complete docs/privacy/notices | G5: automated suite and manual matrix below; required formats/languages work as claimed; real-device performance within justified budgets; no undisclosed limitations. This is the functional completeness boundary. |
| **6 — GitHub delivery preparation** | Phase 5 + signing/publication authority when needed | Version ledger, reproducible unsigned build, signed candidate and receipts, checksums/build info, Obtainium install/upgrade evidence, release-ready README/changelog/assets/notes | G6: exact signed artifact verified; independent rebuild evidence; stable cert; upgrade succeeds; final docs/screenshots inspected. Stop before merge/tag/release until separately authorized. First functional version is `0.1.0`, flagged prerelease. |
| **7 — Public v1.0 stabilization** | Functional release and user feedback | Correct evidenced issues, final logo/launcher assets, refreshed compatibility/security evidence, public stable copy and authentic current screenshots | G7: all mandatory gates still pass, final identity approved, no release-blocking issues. No automatic feature expansion or store/website prerequisite. Publication still requires authority. |

Implementation order within Phase 1 is deliberate: establish PNG and lossless WebP first, then actual native HEIC/AVIF output and their color/metadata limits, then the bounded JXL experiment. Do not build a large UI around unproven format claims. If a material dependency is unavailable, keep the verified work and the precise blocked gate; do not replace the requirement with a vague TODO or fabricate measurements.

## 16. Verification matrix

### Automated acceptance (future implementation, not executed in Phase 0)

| ID | Test and assertion | Gate |
|---|---|---|
| A01 | Geometry properties over random positive bounds/ratios: containment, exact integer ratio, minimum canvas, centering within one pixel, no nonuniform scale, overflow rejection; fixed Pixel examples in §7 | G2 |
| A02 | Crop focus/zoom/reprojection, reset, edge clamping and render/preview transform agreement; inherited vs overridden fields survive destination changes; undo restores exact draft values | G2/G3 |
| A03 | PNG output parsed as required RGB/RGBA; lossless WebP bitstream mode; decode equality on unscaled sRGB fixture pixels; source rectangle unchanged after padding; no-op payload bytes unchanged when eligible | G1/G2 |
| A04 | Encoder capability vs API/device/configuration, unsupported choices remain explicit, no silent fallback; HEIC/AVIF/JXL outputs independently decode with correct type/dimensions | G1 |
| A05 | All eight EXIF orientations, RGB/ICC/sRGB transforms, transparent edge compositing, malformed/oversized profiles, gain-map/HDR rejection or explicit conversion, no double rotation | G2 |
| A06 | Inject EXIF GPS/serial/time/artist, XMP/IPTC/comments/thumbnail and sensitive filenames; default outputs contain no identifying fields; opt-out preserves only advertised fields; dimensions/orientation corrected | G2/G4 |
| A07 | Header bombs, corrupt/truncated streams, animation, wrong MIME/extension, non-seekable provider, revoked grant, oversized source/target, import cap; per-item failures do not discard valid inputs | G3/G4 |
| A08 | Fake storage/codec faults at insert/write/validate/publish/journal boundaries; process restart reconciling published/pending/indeterminate outputs; cancellation leaves success intact, retry excludes successes | G4 |
| A09 | Intent/ClipData single/multiple import and share, URI grants, API 26–28 denial/SAF path, API 29 pending publication, filenames/MIME/collisions; source hashes unchanged | G4 |
| A10 | Locale catalog/XML key and placeholder/plural/array validation; offline help/error/a11y coverage; packaged en/es/ca; product name/taglines/flag identity assertions | G3/G5 |
| A11 | Compose state/semantics checks for selected item/custom conflicts/progress, rotation/back restoration, font-scale layouts; stale preview discarded by revision | G3/G5 |
| A12 | Manifest/dependency scan: no INTERNET or prohibited permissions/SDKs; legacy write maxSdk 28; backup rules exclude images/journal/grants; no private URIs in diagnostic fixtures | G5 |
| A13 | Native source/license pins, dependency verification, every shipping ABI, 16 KB ELF/ZIP checks, controlled build-size report and resource instrumentation | G1/G5/G6 |
| A14 | APK version/application ID/cert/checksums; release ledger monotonicity; two clean unsigned builds; current-version reference scan; asset format/dimensions | G6 |
| A15 | Offline preset schema/source/evidence/locale consistency; grouping compares full output contract, not ratio only; migrated draft catalog conflicts require review | G2/G5 |

Use meaningful tests against output containers/pixels/state boundaries. Do not add tests that merely repeat constants or mock away the encoder/storage failure being claimed as covered. Unit/property tests, Android instrumentation and independent file inspection have different roles.

### Manual/device acceptance (must be recorded separately)

| ID | Required exercise | Evidence and pass condition |
|---|---|---|
| M01 | **Pixel 9 Pro XL, actual 1344×2992 screenshots**, light/dark UI and colored small text | Record OS/build, app commit/version, input dimensions/hash without private content; verify all §7 worked examples visually and by container dimensions. No clipping, stretching or unexplained scaling. |
| M02 | Pixel native HEIC and AVIF, plus one other hardware family with the documented API/configuration | Real encode → independently reopen → gallery/compatible viewer → share. Inspect text edges, color patches, odd-size edges, native errors and time/memory. Decoder availability alone does not pass. |
| M03 | API 26–28 device/emulator and API 29/30/current storage behavior | PNG/WebP, legacy permission denial, SAF fallback, Pictures visibility, no broad read permission. Test 16 KB runtime on an appropriate emulator/device for native libraries. |
| M04 | Mixed batch of 100 items, including 1344×2992 and different ratios, corrupt and unsupported files | Edit a few overrides, change global destination/background/format, inspect conflicts, cancel mid-export, kill/restart, retry failed. No silent reset/duplicate/original modification. |
| M05 | English, Argentine Spanish and Catalan; system/manual selection; airplane mode | Review every screen, error, help and accessibility message; restart and switch back to system; correct names and flag assets. Human review of natural wording, not just key coverage. |
| M06 | TalkBack, 200% text, landscape, dark/light/system, keyboard/switch input where available | Complete import, crop without pinch, conflict resolution, export and share. Labels/focus/contrast usable, no clipped controls. |
| M07 | Metadata fixtures and color comparisons in an independent inspector/viewer | Verify orientation/color appearance, default absence of identifying fields, accurately stated opt-out limitations, transparent corners and explicit HDR behavior. |
| M08 | Manual Instagram/Facebook photo/story upload preview on current apps using synthetic outputs, only in an authorized test context | Record app/date/surface/format/dimensions and observed acceptance/crop/recompression. Do not publish a real post without authorization. An observed preview is not official compatibility certification. Retain “layout guidance” unless evidence supports a narrower verified claim. |
| M09 | Obtainium fresh install and upgrade between two sequential signed test/release candidates | Same certificate/app ID, increasing versionCode, prerelease option and APK filter work; settings retained, new sessions still default PNG/metadata removal. No community catalog dependency. |
| M10 | Exact candidate public screenshots, README, About/version, changelog and release notes | Actual images match authentic UI/locale/version and accepted identity. No stale current-release references, fictitious website availability or invented passing results. |

M01, M02 and M05–M07 are material functional-acceptance gates. Missing hardware or reviewer access is **NOT RUN**, not PASS. M08 may remain a documented limitation without blocking a functional layout-template release; it blocks stronger social-compatibility marketing claims. M09/M10 apply to delivery, not to early local debug builds. Store publication is never used as a proxy for application correctness.

## 17. Requirement-to-delivery traceability

This table is a coverage index, not a replacement for the detailed contracts above. “Phase” means implementation phase unless explicitly 0.

| Requirement | Binding decision / implementation owner | Verification |
|---|---|---|
| R01 — Phase 0 only; first implementation in existing repo | §§1–2; one blueprint branch; no invented baseline or product code | Phase 0 tree/history/readback audit |
| R02 — Name, package, GPLv3, planned site/privacy identity | §1; app manifest/license/docs; URLs not asserted deployed | A12/A14, M10 |
| R03 — Resolve preliminary versions and preserve public v1.0 | §4; 0.1.0 alpha/beta/rc → functional prerelease; 1.0.0 stable; monotonic ledger | A14, G6/G7 |
| R04 — Focused pick/share → destination → preview → one-tap export | §5; Start/Review/Results; no general editor | G3, M01/M04 |
| R05 — Single/multiple import and manageable non-screenshot images | §§8/10; picker/share adapters, static-format/resource checks | A07/A09, M03/M04 |
| R06 — Preserve content, centered minimal margins, never distort | §7 planner; contain default and rational minimum canvas | A01/A03, M01 |
| R07 — Avoid unnecessary scaling/cropping/recompression | §§7/9; explicit resize consent, PNG/WebP no-op payload path | A01/A03/A06, M01/M07 |
| R08 — Explicit centered crop, drag, pinch, reset | §§5/7/12; normalized crop and accessible alternatives | A02/A11, M06 |
| R09 — Batch settings plus per-image exceptions and destination reconciliation | §7; Inherit/Override, typed conflicts, one-step undo | A02/A11, M04 |
| R10 — Thumbnails and preview before export | §§5/7/10; bounded previews tied to plan revision | A11, M01/M04 |
| R11 — Copies in Pictures/FixMyShot, gallery where possible, share, no ZIP, originals intact | §10; MediaStore/legacy/explicit SAF, URI sharing | A08/A09, M02–M04 |
| R12 — Automatic neutral/white/black/manual backgrounds | §5; deterministic edge-luminance choice and manual override | A01/A03, M01/M07 |
| R13 — Transparency conditioned on codec/destination, destination fidelity | §§5/6/8/9; alpha conflicts and RGB Play output | A03–A06/A15, M07 |
| R14 — Do not fabricate UI/content or misrepresent adapted captures | §§5/12; no reconstruction, authentic official viewports | M01/M10 |
| R15 — Strip EXIF/GPS/identifying metadata by default, opt-out and explicit limits | §9; policy allowlists and format-specific preservation disclosure | A06, M07 |
| R16 — Correct orientation and color despite metadata removal | §9; one orientation normalization, managed SDR pipeline, explicit HDR limit | A05, M07 |
| R17 — Local/no Internet/accounts/telemetry/ads/subscriptions; minimal permissions | §§10/11/13; manifest, dependency and storage boundaries | A12, airplane-mode M05 |
| R18 — Remember useful preferences without changing mandatory defaults | §5; PNG and metadata-removal reset for new sessions | A11/A12, M05/M09 |
| R19 — PNG absolute output default; JPEG input, no JPEG output | §§4/8; selector and encoder registry | A03/A04/A11 |
| R20 — Lossless WebP, effective HEIC/HEIF and AVIF first-version encoding | §8; API-specific real writers, libwebp fallback, G1 required formats | A03/A04/A13, M02/M03 |
| R21 — Evaluate official JXL implementation if proportionate | §8; measured reference-codec spike with package/memory/latency gates | G1 decision record; no speculative PASS/DEFER |
| R22 — Native Android, API 26, Kotlin/Compose validation, minimal Material 3/themes | §§11/12; compatible toolchain and system theme | G1/G3, M03/M06 |
| R23 — Provisional/final logo with capture/shot exploration | §12; provisional development identity, final before stable milestone | M10, G7 |
| R24 — No filters/AI/video/drawing/photo suite | §4 exclusions and dependency/UI review | G3/G5 scope audit |
| R25 — Complete English/Argentine Spanish/Catalan offline | §12 catalog/resources/help/errors/a11y and human wording review | A10, M05/M06 |
| R26 — Manual/system language, exact names/flags/name/taglines | §12; packaged flags, locale bridge, invariant strings | A10/A11, M05 |
| R27 — Automated localization verification | §12; XML/placeholder/plural/array/catalog and packaged-resource checks | A10, CI |
| R28 — GitHub only first, Obtainium install/update; future F-Droid/Play without submission blockers | §13; universal APK and direct source; compatibility preparation only | M09, G6 |
| R29 — Signed APK/checksums/coherent versions/reproducibility/stable independent signing | §§4/13; release ledger and artifact chain; separate signing authority | A13/A14, M09 |
| R30 — README/CONTRIBUTING/CHANGELOG/privacy, CountAway author/donation reference | §13 owners; §1 observed links; §12 translation workflow | G5/G6, M05/M10 |
| R31 — Engineering/documentation/branches/commits English | §§1/13; localized product copy exception | Phase reviews and document audit |
| R32 — Current competitors and actual journey/default/batch/privacy/export comparison | §3 and S01–S03; observation separated from claims | Phase 0 evidence review; no invented device tests |
| R33 — Play/Instagram/Facebook/custom presets, TikTok/others examined | §6; agreed layouts preserved; justified additions/deferrals | A15, M08 where available |
| R34 — Popularity is not screenshot demand; equivalent outputs/surface distinctions/offline catalog | §6; evidence-qualified grouping and limited survey proxy | A15; source review before release |
| R35 — Read-only CountAway concepts/validators/CI/distribution, no identity/architecture inheritance | §§2/11–13 and S27 | Phase 0 read-only audit; future notices/source review |
| R36 — All material target content/history/instructions and honest validation status | §2 and §19; complete baseline coverage | Phase 0 inventory; reconcile again on adoption |
| R37 — Findings classified and explicit inclusion/deferral decisions | §14; no missing feature called a defect | Phase 0 coverage review |
| R38 — Executable phases/dependencies/contracts/acceptance and Pixel 9 Pro XL 1344×2992 | §§7/15/16; detailed gates and numerical fixtures | A01, M01/M02 |
| R39 — Robust batch/export/metadata/codec limits; no silent scope cuts | §§7–10/14–16; conflicts, transactions, recovery and G1 fallback | A02–A09/A13, M02/M04/M07 |
| R40 — Fully recoverable governing artifact and explicit Code v3 handoff | §§1/18/19; immutable commit/readback plus current branch reconciliation | Phase 0 persistence/continuity audit |

## 18. Source register and evidence dates

All entries were retrieved or inspected on **2026-10-20 UTC** unless a publication/release date is explicitly given. External pages and default branches can change. Preserve exact implementation dependency pins separately at G1; these research references are not runtime dependencies. Official pages that were inaccessible are recorded as limitations rather than replaced with unsupported claims.

| ID | Primary source / observed locator | What it supports and limits |
|---|---|---|
| S01 | [Image Toolbox source](https://github.com/T8RIN/ImageToolbox/tree/9fddc74dc1883992550f5cb7cfd97dca45777d8e), [F-Droid listing](https://f-droid.org/en/packages/ru.tech.imageresizershrinker/). Source files under `feature/resize-convert/src/main/java/com/t8rin/imagetoolbox/feature/resize_convert/presentation/`: `ResizeAndConvertContent.kt`, `screenLogic/ResizeAndConvertComponent.kt`. | Source-reviewed control flow/settings and advertised breadth; no installed-app timing or privacy audit. |
| S02 | [Batchpad developer page](https://yellowsquared.us/apps/batchpad/), [Play listing](https://play.google.com/store/apps/details?id=com.yellowsquared.batchpad) | Developer journey/batch/local/EXIF claims and declared ads/IAP/data-safety information; observed listing update 2026-10-17. |
| S03 | [PadSnap live application](https://padsnap.app/) | Live v1.9.1 control/default inspection without image upload; indexed v0.6.9 differs. No export test. |
| S04 | [Google Play preview assets](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en) | Phone screenshot format/dimension limits versus separate recommendation conditions. Listing review remains outside scope. |
| S05 | [Instagram publishing API media reference](https://developers.facebook.com/documentation/instagram-platform/instagram-graph-api/reference/ig-user/media.md/), [Instagram resolution help](https://help.instagram.com/1631821640426723/), [Facebook image ads guide](https://www.facebook.com/business/ads-guide/image/facebook-feed) | API constraints are not app-uploader constraints. Help endpoint rate-limited and ads guide login-blocked; manual social templates intentionally carry limited claims. |
| S06 | [TikTok carousel advertising specifications](https://ads.tiktok.com/resources/help/article/specifications-for-carousel-ads?lang=en), updated September 2026; [organic posting help](https://support.tiktok.com/en/using-tiktok/creating-videos/making-a-post) | Ads JPEG/PNG, 1200×628 / 640×640 / 720×1280 alternatives, music requirement; organic help retrieval blocked. Do not transfer ad rules to organic photos. |
| S07 | [Pinterest creative guidance](https://business.pinterest.com/creative-best-practices/) | 2:3/1000×1500 guidance; no screenshot-specific demand inference. |
| S08 | [Pew Research: Americans' Social Media Use 2025](https://www.pewresearch.org/internet/2025/11/20/americans-social-media-use-2025/) | Published 2025-11-20; US adult survey, 5,022 respondents, fielded 2025-02-05 to 2025-06-18. Usage proxy only, not global screenshot conversion demand. |
| S09 | [Android supported media formats](https://developer.android.com/media/platform/supported-formats), observed update 2026-10-07 | Separate encoder/decoder API support, optional capability and native HEIF/AVIF routes. |
| S10 | [Bitmap.CompressFormat](https://developer.android.com/reference/android/graphics/Bitmap.CompressFormat) | PNG and WebP mode/quality semantics; no HEIC/AVIF/JXL enum. |
| S11 | [AndroidX HeifWriter release notes](https://developer.android.com/jetpack/androidx/releases/heifwriter), 2026-10-07 entries | Observed stable 1.1.0 and 1.2.0-rc01 candidate; relevant color/AVIF/EXIF fixes. |
| S12 | [HeifWriter API](https://developer.android.com/reference/androidx/heifwriter/HeifWriter), [AvifWriter API](https://developer.android.com/reference/androidx/heifwriter/AvifWriter), [AndroidX writer sources](https://github.com/androidx/androidx/tree/androidx-main/heifwriter/heifwriter/src/main/java/androidx/heifwriter) | Actual writer lifecycle/configuration and source-reviewed `EncoderBase` behavior. Moving source observed on research date; implementation must pin the dependency. |
| S13 | [WebP codec API](https://developers.google.com/speed/webp/docs/api), [WebP container specification](https://developers.google.com/speed/webp/docs/riff_container) | Real lossless encoder interface and chunk structure; no measured Android APK overhead asserted. |
| S14 | [libavif](https://github.com/AOMediaCodec/libavif/tree/b1e4d339c1ff5b547664c85f1713279f4997d694), including `android_jni/README.md` | C API, codec dependencies and decoder-only supplied Android JNI module. |
| S15 | [libheif](https://github.com/strukturag/libheif) | Container/codec architecture, licenses, build options and HEIC encoder/decoder alternatives. Not a ready-made zero-cost Android solution. |
| S16 | [awxkee/avif-coder](https://github.com/awxkee/avif-coder) | Actual HEIC/AVIF encode API and current native dependency/build surface; claims require independent validation. |
| S17 | [JPEG XL reference software](https://jpeg.org/jpegxl/software.html), [libjxl v0.12.0](https://github.com/libjxl/libjxl/tree/v0.12.0), [build instructions](https://github.com/libjxl/libjxl/blob/v0.12.0/BUILDING.md) | Official reference implementation, source-build complexity, lossless path and security-release baseline. Release observed dated 2026-07-01. |
| S18 | [awxkee/jxl-coder](https://github.com/awxkee/jxl-coder) | Real Android wrapper alternative; inspect actual pinned backend rather than assume it is simply the official reference build. |
| S19 | [ExifInterface reference](https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface), [release notes](https://developer.android.com/jetpack/androidx/releases/exifinterface) | Read/write asymmetry; stable 1.4.2 observed dated 2025-12-03. |
| S20 | [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker), [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) | Picker contracts, document fallback and scoped URI access. |
| S21 | [Android shared media storage](https://developer.android.com/training/data-storage/shared/media) | MediaStore ownership, relative paths, pending publication and API-specific permissions. |
| S22 | [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Compose December 2025 release](https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html), [Compose BOM](https://developer.android.com/develop/ui/compose/bom), [Kotlin releases](https://kotlinlang.org/docs/releases.html) | Compatible starting toolchain and explicit BOM/compiler independence. Current docs have newer versions; selected pins are conservative, subject to G1 resolution/security review. |
| S23 | [Android 16 KB page-size guidance](https://developer.android.com/guide/practices/page-sizes) | Native ELF/packaging/runtime compatibility and build-tool considerations. |
| S24 | [F-Droid reproducible builds](https://f-droid.org/en/docs/Reproducible_Builds/) | Independent unsigned build/signature verification concepts and common nondeterminism sources. |
| S25 | [Obtainium sources](https://wiki.obtainium.imranr.dev/sources/), [tracking](https://wiki.obtainium.imranr.dev/app_tracking/), [UI overview](https://wiki.obtainium.imranr.dev/ui_overview/) | Direct GitHub release tracking/install/update; select one APK when multiple assets exist. Check actual current prerelease controls in M09. |
| S26 | [F-Droid inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/), [Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en) | Future build/source/target compatibility considerations; no current submission or acceptance claim. |
| S27 | [CountAway inspected commit](https://github.com/santirodriguez/CountAway/tree/6f15d78055b22fc11cdb8f2cb1377600054f26ae) | Read-only reference: `README.md`, `CONTRIBUTING.md`, `app/src/main/java/com/santiagorodriguez/countaway/ui/LanguageCatalog.kt`, `app/src/main/res/xml/supported_languages.xml`, `scripts/verify-localizations.py`, `scripts/verify-bundle-languages.sh`, `app/build.gradle.kts`, `.github/workflows/android.yml`, `.github/workflows/release.yml`, `docs/RELEASING.md`. Reuse compatible concepts; retain notices if code is later adapted. |
| S28 | [FixMyShot baseline](https://github.com/santirodriguez/FixMyShot/tree/eb430e13ac71fdf334978b88623acf1ae1944c7e) | Complete initial tree/history/license and absence of application/build/tests/tags/releases on the research date. |

## 19. Phase 0 acceptance and next action

Phase 0 research established the baseline, compared focused and broad alternatives, separated official destination constraints from layout guidance, investigated actual HEIF/AVIF/JXL encoding routes, selected a bounded architecture and resolved the product/version contract. The implementation/runtime gates remain intentionally unexecuted because application implementation was not authorized in this phase.

**Verified in Phase 0:** GitHub baseline/history/refs/license; complete target tree coverage; read-only reference material; direct PadSnap control/default inspection; cited primary-source claims and their retrieval limitations; one work branch created from the revalidated baseline. **Not run:** Android builds/tests, codec benchmarks, physical Pixel checks, social uploads, signing/reproducibility or Obtainium installation. **Not applicable:** existing application regression tests or existing CI runs, because none existed. The final persistence receipt must identify the commit containing this file and confirm readback/content coverage before the handoff is marked complete.

Known implementation uncertainties are bounded gates, not unresolved product preferences: native device/configuration/color support (G1/M02), actual package/memory/latency measurements and JXL proportionality (G1), translation/visual/hardware acceptance (G3/G5), and separately authorized signing/publication (G6). Social templates retain limited compatibility claims until relevant evidence is available; this does not silently remove the agreed destinations. No dependency on an approved store listing or deployed website is introduced.

**Next for Code v3 after explicit adoption:** recover this entire governing artifact from the recorded immutable commit, reconcile the live work branch/main and current applicable instructions, then begin **Phase 1 — Bootstrap and codec feasibility**. Preserve the same continuity workstream. Do not mark overall application development complete when Phase 0 is complete.
