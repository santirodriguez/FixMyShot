# Optional on-device HEIC/AVIF check

Status: **IMPLEMENTED; PIXEL 9 PRO XL API37 SYNTHETIC BITSTREAM DECODED ON HOST; AVIF GEOMETRY MISMATCH CONFIRMED; NATIVE FIDELITY/EXPORT STILL OPEN**. The full current-source UI/core/API36 CI checkpoint is [40966098425](https://github.com/santirodriguez/FixMyShot/actions/runs/40966098425) at compiled commit `d54eb9c7`; the separate user-shared physical Pixel synthetic-check results are recorded below. The check gathers preliminary device evidence; it does not enable native export or close G1/M02. Existing HEIC/AVIF writer/API/fidelity/fallback requirements remain unchanged. No new native library or permission is introduced.

## Use and boundaries

Open Settings → Check HEIC and AVIF → Run check. It works with an empty draft and uses only deterministic synthetic opaque sRGB images:64×64,65×67 and1344×2992, each attempted once as HEIC and AVIF. It never reads selected images, source grants, output journals or public provider files. The current native adapter requests maximum-quality lossy encoding, grid enabled, with an8-second timeout for each attempt. Vendor start/add/stop/close operations may return late; this is not a hard wall-clock watchdog. Stop/Back/Close or leaving the foreground requests cancellation, then waits for the current codec to return before further work can start.

The ViewModel owns progress, cancellation and the latest report through Activity recreation. Work is serialized on the existing private export/session worker; queued imports wait and new exports are blocked until completion through the normal UI. A second diagnostic runner cannot start while another runner owns its process-wide guard. No background service, automatic startup check, repeated benchmarking, format substitution or retry is added. The report is transient, replaced on a deliberate rerun and lost when the process is gone.

Returned candidates must be directly owned native-still files in batch-candidates. The check independently examines bounded ftyp brands, actual MIME/bounds and full decoded dimensions/opacity before scoring. Files are removed after each attempt, including rejected or cancelled candidates; the encoder owns failure cleanup before returning. An unexpected outside path is never deleted. A process kill can leave a synthetic candidate in the existing private cache; the existing conservative24-hour startup candidate pruning applies. No images are inserted into MediaStore or an SAF folder, and no encoded samples are shared by this feature.

## What the measurements mean

The fixture has six known color patches, dark/light blocks and fine colored edges. Full reopened pixels are normalized to software ARGB8888/sRGB for comparison. The report records exact RGB equality, full-image PSNR and the largest channel error among3×3 patch medians. Exact alpha255 is required. The blueprint's PSNR38dB/patch error2 levels are reported as numeric targets for that one case, never as a support or appearance approval. An exact RGB match uses exact_pixels=true and psnr_db=null, avoiding a nonfinite JSON number.

Adapter elapsed time includes its encode/validation path. This is one attempt per case, not cold/warm p50/p95, native/PSS/Java peak memory, embedded-color-profile correctness, independently decoded appearance, gallery/receiver or physical16KiB acceptance. Numeric failures remain evidence to investigate; they do not automatically discard a desired native format or authorize a software fallback decision without its actual bounded experiment.

Share report opens Android's text chooser only after a separate tap. Closed JSON fields contain API level, bounded manufacturer/model, application version/code, the installed base APK SHA256 when readable, typed outcomes, dimensions, byte count, adapter time and synthetic metrics. They contain no selected-image name/URI/hash/pixels, installation path, serial/account identifiers or raw exception text. Compare the base APK digest with a known source-bound build receipt before associating the report with a commit; it does not cover an arbitrary installed split set or prove device appearance. Cancellation/read failure may leave the digest null. Nothing is sent automatically, and a sharing-app failure remains a localized UI error.

## Validation state

Four NativeDiagnosticTest cases are prepared: independent exact/single-pixel/wrong-color numeric oracle; closed report/exception omission and existing non-native adapter refusal; cancellation/owned disguised-PNG rejection cleanup; zero-encode cancelled/low-budget cases plus own-APK identity. These use an injected encoder for policy/failure boundaries and do not establish native codec success. They join the grouped import/settings/session block, now18 (prior14+4); the separate native probe group remains unchanged. At that earlier checkpoint, the new Android integration was still NOT RUN. The later current-source API36 CI passed the native diagnostic/regression groups and the imported editor flow, but did not establish that native HEIC/AVIF works on physical hardware or satisfy G1/M02. The native user-device report is now RECEIVED and structurally reviewed below. Actual source-bound gallery/viewer/share/independent pixel acceptance M02 remains NOT RUN.

## Current targeted physical handoff (not a repeat of the accepted edit flow)

The isolated `FixMyShot Next` APK was already delivered for the next-image UI flow and the user accepted that flow. Its compiled source is `d54eb9c7`, SHA-256 `1b526d18191363657ea0d8eadbc7c8a6f61d5a930c4b41d4838ef312390c7743`, debug package `org.cajapersonal.fixmyshot.ux22next`. Do **not** uninstall earlier isolated diagnostics or assume production signing. That same installed APK completed the **Settings → Check HEIC and AVIF → Run check → Share report** exercise on the Pixel 9 Pro XL. Do not repeat the unchanged six-case check on this device merely to obtain a duplicate report. No personal images are accessed by this synthetic diagnostic.

The offline report checker validates the exact six-case order, typed result fields, independent numeric-target arithmetic, closed/no-URI JSON field set and the reported installed APK hash against an explicitly supplied expected digest. For this diagnostic:

```sh
python3 scripts/check-native-device-report.py /path/to/native-report.json \
  --expected-apk-sha256 1b526d18191363657ea0d8eadbc7c8a6f61d5a930c4b41d4838ef312390c7743
python3 scripts/test-native-device-report.py
```

A valid report is **only structurally and source-identity consistent as self-reported by the app**; the hash is not a remote attestation and a test harness cannot establish hardware truth. Each of the three sizes for each format remains separately available/unavailable and numerically qualified/unqualified. A missing/mismatched APK hash is not accepted as source-bound evidence. An encoded/reopened synthetic still is not sufficient for final pixel/color-profile, real screenshot, gallery/viewer/share, peak-memory, cold/warm latency, cancellation or other-hardware acceptance. Keep HEIC/AVIF export gated, do not silently fall back to PNG, and do not change the codec or release policy based only on this diagnostic.

Pixel full-resolution fidelity is a **separate** remaining exercise: privately compare the original 1344×2992 non-sensitive screenshot with the actual exported PNG under explicit Keep all and, if relevant, a chosen 9:16 crop; inspect dimensions, all source UI content, margins, color/text edges, metadata and original-file immutability. Do not repeat the already accepted consecutive-image/picker-cancel test. A second hardware family, measured resources, 16 KiB runtime and manual accessibility/localization remain separate gates. The original [verification matrix](../blueprints/first-functional-release.md#16-verification-matrix) remains authoritative.

## Pixel 9 Pro XL synthetic result — 2026-10-29

User-shared JSON: `schema=1`, `SYNTHETIC_NATIVE_CHECK_V1`, Android API **37**,
`Google / Pixel 9 Pro XL`, `0.1.0-alpha.1`/code 1, compiled app source
`d54eb9c7`. Its self-reported base-APK SHA-256 matches the previously
independently checked and delivered isolated diagnostic APK:
`1b526d18191363657ea0d8eadbc7c8a6f61d5a930c4b41d4838ef312390c7743`.
`cancelled=false`; `failed=false`. The closed report fields, six ordered cases,
positive byte sizes, finite metrics and reported target flags were checked
independently against the current validator's invariants. This is *not*
hardware attestation or independent inspection of the encoded samples.

| Format | Input | Returned bytes | One adapter attempt | RGB PSNR | Max 3×3 patch median-channel error | Numeric target |
|---|---|---:|---:|---:|---:|---|
| HEIC | 64×64 | 1,247 | 553 ms | 28.234 dB | 1 | NOT MET |
| AVIF | 64×64 | 947 | 318 ms | 28.167 dB | 1 | NOT MET |
| HEIC | 65×67 | 1,759 | 388 ms | 26.078 dB | 1 | NOT MET |
| AVIF | 65×67 | 1,358 | 164 ms | 7.247 dB | **129** | NOT MET |
| HEIC | 1344×2992 | 209,314 | 1,558 ms | 24.739 dB | 1 | NOT MET |
| AVIF | 1344×2992 | 170,421 | 573 ms | 24.754 dB | 1 | NOT MET |

**Result:** all **6/6** report `ENCODED_AND_REOPENED`, but **0/6** meet the
synthetic numeric target (PSNR ≥38 dB and patch error ≤2). The unexpectedly
large odd-size AVIF mismatch (**65×67**, PSNR 7.247 dB, patch error 129)
needs targeted investigation; the report alone cannot assign the defect to
the writer, container, platform decoder, color transfer or stride/alignment.
The remaining five cases have patch error 1 but miss PSNR, plausibly related
to sharp high-contrast fixture boundaries; **do not assert a root cause or
general real-screenshot quality without independently examining output**.

The G1 blueprint explicitly requires cause/visual review when a PSNR regression
target is missed; PSNR alone does not automatically disqualify all lossy
output. It separately prohibits allowing wrong transfer/range, corrupt
output or visibly unacceptable text. Therefore the native paths remain
**UNQUALIFIED/UNAVAILABLE for ordinary export**, and no transparent fallback
or reduced assertion is authorized. M02's full independent decode/gallery/
receiver/real-screenshot review, second hardware family, color-profile
inspection, native/PSS/Java memory, cold/warm p95, cancellation and physical
16 KiB runtime are still OPEN/NOT RUN. Single elapsed times are not p95.

**Bounded next diagnosis:** inspect the *existing* grid/alignment experimental
cases for unpadded 65×67, edge-padded 66×68 and grid on/off, then design
a narrowly source-bound Pixel diagnostic that compares actual AVIF output
pixels in independent decoders plus color/profile and visible geometry.
Do not run another complete Android/legacy test matrix or ask the user to
repeat this unchanged report. A different encoder, software fallback or
format-scope change requires its own evidence and explicit decision.

## Independent host control and next Pixel evidence boundary — 2026-10-29

A bounded, non-Android control reconstructed the six-patch/high-contrast synthetic fixture
from NativeCodecDiagnostic.fixture. Pillow 12.3.0 / libavif 1.4.2 encoded 65×67
AVIF at quality 100 in explicit 4:2:0 and 4:4:4 modes. FFmpeg 7.1.5
confirmed yuv420p and yuv444p, respectively. Reopened RGB results
(PSNR against fixture, maximum 3×3 median-channel patch error):

| 65×67 software control | Pillow | FFmpeg | libheif 1.19.8 |
|---|---:|---:|---:|
| AVIF 4:2:0 | 29.550 dB / 1 | 27.270 dB / 1 | 28.887 dB / 1 |
| AVIF 4:4:4 | 55.741 dB / 1 | 56.500 dB / 1 | 56.500 dB / 1 |

With the same Pillow/libavif 4:2:0 parameters, 64×64, 66×68 and
1344×2992 returned 30.487, 29.623 and 37.811 dB respectively (all
patch errors 1). These are host controls, NOT Pixel output, matched AndroidX
codec settings or proof of independent decoder backend implementations.
They show that extreme 7.247 dB / patch error 129 is not an unavoidable
property of odd dimensions or nominal 4:2:0 chroma subsampling. An ordinary
4:2:0 encode of sharp boundaries can miss the 38 dB project target; this
requires G1 visual/cause review, not a silent relaxation or blanket exclusion.

The original [API36 artifact 40966098425](https://github.com/santirodriguez/FixMyShot/actions/runs/40966098425)
was retrieved: 2,279,411 bytes, SHA-256
5b8535fc3a9e1f8e97c540190b1d2544a9fe69ae55cddd766eeebdef15bdb07e,
68 distinct ZIP members and valid CRCs. Its grid report attempted six AVIF
modes, ALL UNAVAILABLE / INVALID_OUTPUT on the emulator. Related HEIC
66×68 and 96×68 observations are informative but cannot prove why
physical Pixel AVIF is degraded. Repeating the same emulator matrix
cannot replace actual Pixel bytes.

Next missing primary evidence: the actual small synthetic Pixel-encoded AVIF
for 65×67 at grid-on and grid-off, plus bounded 66×68 edge-padded and
96×68 aligned controls. Compare container geometry/color properties,
visible dimensions, the fixed 65×67 original-source ROI, Android's two
decoder front-ends and separate host decoders. Android BitmapFactory and
ImageDecoder may share an underlying implementation; agreement alone does
not prove independent decoding. Current user-shared JSON cannot provide
the compressed bytes (its candidates were safely deleted). The precise
encoder-versus-decoder cause therefore remains UNKNOWN.

A narrow opt-in diagnostic would need to generate ONLY deterministic synthetic
content, bound private buffers and report size, remove temporary candidate
files and share explicitly on user action. Do not read user photos, publish
diagnostic bitstreams in source control, repeat the completed six-case check
or enable native export prematurely. PNG/WebP and previously accepted editor
behavior stay unchanged.

## Focused AVIF alignment diagnostic — implementation candidate

A separate opt-in **Investigate AVIF alignment** button under Settings / Quick help
runs five synthetic attempts rather than repeating the accepted six-case report:
65×67 (grid on/off), 66×68 (edge padding, grid off/on), and 96×68 (aligned
width, grid off). The original input fixture is 65×67 for all comparisons;
the additional canvas is an experimental output geometry, never a normal
export workaround or a promise to preserve the original output dimensions.

The native writer's existing production calls continue to request grid on.
The new optional argument permits grid off only for this diagnostic. Work is
serialized on the same foreground worker, guarded by API34+, a request-level
8-second timeout, cancellation, private candidate ownership, 8 KiB
per-candidate size cap and deletion in all owned-output exit paths.
It compares a fixed 65×67 ROI through BitmapFactory and ImageDecoder with
software sRGB normalization. Both front ends may share the same platform
codec, so two Android results are **not independent host decoding**.

The numeric-only report includes device/build identity, grid/size, checked
byte lengths, output SHA256 and pixel metrics, but **does not include encoded
AVIF bytes, selected image data, paths or URIs**. Sharing requires a separate
tap. scripts/check-odd-avif-report.py validates exact case order, bounded
fields/metrics and the self-reported installed APK hash; these are not
hardware attestation or G1/M02 release qualification. Its companion script
has five synthetic host regression tests.

The diagnostic debug package .avifodd / **FixMyShot AVIF Check** is isolated
from previously accepted .ux22next / FixMyShot Next. Production identity,
signing policy, native availability and PNG/WebP defaults remain unchanged.
The exact-source [Actions 40989504238](https://github.com/santirodriguez/FixMyShot/actions/runs/40989504238)
passed on attempt 2 at diagnostic source `508b8b95` (build/lint/core99 and
API36 instrumented groups, including the three new geometry diagnostic tests).
The isolated `.avifodd` APK was independently checked and privately delivered.
The user has now completed this five-case check on the Pixel; see the
source-bound results below. Independent host decode of the **actual
Pixel-generated AVIF bytes** remains separate and not yet possible from this
numeric-only report. The experiment does not assign a writer/decoder root cause.

## Pixel 9 Pro XL focused AVIF alignment result — 2026-10-30

**Scope:** one user-shared `ODD_AVIF_NUMERIC_DIAGNOSTIC_V1` report,
`schema=1`, Google Pixel 9 Pro XL, Android API **37**, app
`0.1.0-alpha.1`/code 1. The report's *self-reported* base APK SHA-256
`665a3c3165470e26d7782c4af43e2bda87ca86c7112ee4c414b115c19a71f8d4`
matches the independently validated, delivered `FixMyShot AVIF Check`
diagnostic built from `508b8b95cf0712b4ca04929d145f09957353bfa7`.
This is not an Android device attestation. `cancelled=false`, `failed=false`.
Its closed field set, ordered five-mode list, digest shapes and typed metric
consistency were independently checked against
`scripts/check-odd-avif-report.py`'s declared contract.

| Encoded dimensions | Grid requested | Bytes | BitmapFactory PSNR | ImageDecoder PSNR | Max patch error (both) |
|---|---|---:|---:|---:|---:|
| 65×67 | On | 1,358 | 7.2468 dB | 7.2468 dB | **129** |
| 65×67 | Off | 1,358 | 7.2468 dB | 7.2468 dB | **129** |
| 66×68, padded | Off | 1,358 | 7.2397 dB | 7.2397 dB | **129** |
| 66×68, padded | On | 1,358 | 7.2397 dB | 7.2397 dB | **129** |
| 96×68, aligned/padded | Off | 1,358 | 9.2313 dB | 9.2313 dB | **129** |

The two grid settings produced the **same entire AVIF bytes** for each
same-dimension pair, as established by identical reported SHA-256 values:

- 65×67 on/off:
  `3bd7556515d9b5532faef95059e2e20afa4bbc263969f1ffbe5321ae2f51f19c`.
- 66×68 off/on:
  `8bff434bbd2e5d0df937f2d342a28ef3fd773ea6084ed83a8024b36a424625c3`.
- 96×68 off:
  `6cbd8cb6569bac6f888db63cb1a659b2640aa6ad0386bad053ce1123949e9c67`.

All five output sizes are exactly 1,358 bytes, but the three distinct
digests show that the three geometries do **not** have identical
bitstreams. All five were encoded, reopened and measured by both Android
front ends; none met the `>=38 dB` PSNR and `<=2` patch-error numeric
targets. BitmapFactory and ImageDecoder were bit-for-bit identical in
reported *metrics*, not independently verified decoded pixel arrays; both
may share a platform decoder.

**Conclusion and scope:** toggling `setGridEnabled` did not alter these
small outputs, and padding to even/aligned dimensions did not resolve the
severe 129-level patch discrepancy. This rules out a simple grid-toggle
or odd-dimension-only workaround *for the tested Pixel route*, **not**
every alignment/color path. The persistent anomaly is compatible with
several possibilities (native writer's bitmap/EGL-to-codec path, encoded
color/range transfer, or Android decoder behavior); none is demonstrated
as the root cause. HEIC has separate evidence; do not infer identical
behavior for it. Ordinary HEIC/AVIF export remains **gated and unqualified**.

**Next primary-evidence gate:** obtain **one** bounded synthetic
Pixel-produced 65×67 AVIF bitstream (with its matching SHA-256 above),
and optionally the 66×68 example, through a separately reviewed opt-in
handoff; compare its encoded geometry/color properties and independently
decoded 65×67 source ROI with the known fixture on the host. The existing
diagnostic deliberately deletes candidates and reports *only* metrics,
so it cannot provide these bytes retrospectively. Compare that evidence
before considering an alternate AVIF input pipeline or the bounded
software fallback already prescribed by G1. Do **not** repeat this
five-case test, weaken targets, enable native output or start another
full matrix solely for these unchanged results. M02 physical real-image
and second-hardware acceptance, resource/16 KiB runtime, and distribution
gates remain OPEN.

## One-file synthetic AVIF byte handoff — implementation candidate

A new opt-in action inside the existing **Investigate AVIF alignment**
dialog generates **one** deterministic **65×67** opaque-sRGB AVIF using
the existing AndroidX `AvifWriter` bitmap route with grid enabled.
This does **not** rerun the previously completed five-case alignment
matrix and does not read selected photos, draft sources, gallery media or
providers. Native export remains gated.

The encoder retains its normal 8-second bound and regular candidate
checks. The diagnostic additionally restricts the returned candidate
to the owned private `native-still-*.avif` path, actual native AVIF route,
valid `ftyp` AVIF brand and **8,192-byte maximum**. It encodes a
SHA-256 and standard no-wrap Base64 into a closed JSON report, then
deletes the temporary encoded file. The sample exists in ViewModel
memory only while the dialog is open and is discarded on dismissal.
Sharing requires an **independent explicit tap**, warns that the
selected sharing app will receive the synthetic bytes and app/device
metadata, and never transmits or publishes automatically. The debug
identity `.avifbytes` / **FixMyShot AVIF Sample** is distinct from the
accepted `.ux22next` and completed `.avifodd` diagnostics.

The companion `scripts/check-avif-byte-sample.py` validates the
source-bound APK digest, bounded Base64 length, exact decoded bytes,
SHA-256, AVIF `ftyp` brand and schema before writing any optional
`.avif` output for host-side independent decoding. The output cannot
be treated as genuine AVIF unless an external decoder also reads and
visually/quantitatively compares it. Three synthetic Android refusal/
cleanup checks and five host JSON/byte-integrity tests are added to
the explicit current API36 validation checkpoint. Exact-source [Actions 41107077558](https://github.com/santirodriguez/FixMyShot/actions/runs/41107077558)
passed build/lint, 99 core and 131 API36 instrumentation tests, with
optional legacy SAF NOT RUN. The separately signed `.avifbytes` APK was
verified and delivered; the user completed the single-sample Pixel
handoff. Its actual bitstream has now been recovered and decoded on
the host (results below). The two earlier phone diagnostic reports and
this one-file capture are complete; do not request repeats.

The received AVIF is exactly the same **65×67 grid-on** sample
(SHA-256 `3bd7556515d9b5532faef95059e2e20afa4bbc263969f1ffbe5321ae2f51f19c`)
seen in the previous alignment report; see the byte-level analysis below.
Native pipeline alternatives and software fallback decisions remain
subject to G1/M02, independently verified visible geometry, color,
resources and second-hardware acceptance.

## Pixel 65×67 AVIF independent byte-level decode — 2026-10-30

**Primary evidence received:** user-shared `SYNTHETIC_AVIF_BYTES_V1`,
`schema=1`, `format=AVIF`, Pixel 9 Pro XL / API37, app
`0.1.0-alpha.1` / code1, `cancelled=false`, `reason=null`, sample
65×67. The *self-reported* installed base APK SHA-256
`7895202a5046058f3999f864ec2e6fa50dc1e948dd2c070b0e67f099e09b4b79`
matches the separately checked `.avifbytes` APK built from `82a40bf22a479c035ee608c3196f2ec616513be2`
([successful current-source CI 41107077558](https://github.com/santirodriguez/FixMyShot/actions/runs/41107077558)).
The user also supplied fresh six-case and five-case numeric reports from the
same app APK, reproducing the previously accepted findings; these were **not**
new qualification runs. The Base64 was reconstructed as **1,358 bytes** and
independently SHA-256 checked against both its report and previous odd-size
grid-on sample: **`3bd7556515d9b5532faef95059e2e20afa4bbc263969f1ffbe5321ae2f51f19c`**.
These are source-bound receipts, not device attestation. No personal image
content, installation path or original user photo was read. Do not commit
raw Base64 or the diagnostic AVIF bitstream into the public repository.

**Container versus encoded geometry:** AVIF `ftyp` and the primary image's
`ispe` describe **65×67**; there is no `clap` clean-aperture property or
`colr` color profile property in this tiny file. FFmpeg 7.1.5 (libdav1d)
and libheif 1.19.8 expose a decoded AV1 raster of **128×67**, 4:2:0,
with 63 additional right-hand columns that largely repeat the last source
column. Pillow 12.3.0 / libavif 1.4.2 presents **65×67**, but its output
is visibly *horizontally compressed*; the Android diagnostic reports a
similarly compressed image. The differing host front ends may share
low-level AV1 codec backends and do not constitute independent backend
implementation proofs.

An exact reconstruction of `NativeCodecDiagnostic.fixture(PixelSize(65,67))`
was used to compute the repository's whole-image RGB PSNR and median-of-3×3
per-channel color-patch error. For decoders exposing 128 columns, metrics
are computed only on the *leftmost original 65×67 ROI*, an explicitly
experimental crop, **not a compliant production presentation**.

| Decoding/presentation of the same 1,358-byte AVIF | Dimensions exposed | RGB PSNR vs exact fixture | Max patch error |
|---|---|---:|---:|
| Android BitmapFactory and ImageDecoder (reported API37) | 65×67 | 7.2468 dB | **129** |
| Pillow/libavif | 65×67 | **7.2819 dB** | **129** |
| FFmpeg/libdav1d (left 65-column ROI) | 128×67 | **26.3742 dB** | **2** |
| libheif (left 65-column ROI) | 128×67 | **26.0928 dB** | **3** |

**Controlled metadata-only host experiment:** changing **only** the
`ispe` width from 65 to 128 in a **temporary copy**, without changing
compressed AV1 bytes, makes Pillow present 128×67; the leftmost
65×67 ROI then yields **27.3023 dB** with essentially correct six
solid-color patches (max patch error about 2). The temporary file has
a different SHA-256 and is **never** approved as an export or proposed
as a single-byte product fix: it would present the wrong dimensions.
A user-visible image must preserve the agreed output geometry.

**Interpretation:** the dramatic 129-level patch errors do **not** show
that original colors were destroyed by encoding: the source-like colors
are recoverable from the AV1 raster. There is a demonstrable
**encoded-versus-presented geometry disagreement**, which explains the
gross horizontal distortion. The exact conformance fault requires
reconciling the AV1 bitstream's decoded upscaled width, `ispe`
spatial-extents semantics, and any clean-aperture presentation rules
(see the [AOM AVIF specification](https://aomediacodec.github.io/av1-avif/latest-approved.html)).
Do **not** attribute the entire defect exclusively to either the
AndroidX writer, the device codec, libavif or Android's decoder without
that contract review. Missing explicit color property also requires
its own color-management acceptance.

**Still NOT qualified:** even the correct crop yields only 26–27 dB,
below the blueprint's 38 dB synthetic regression target; G1 requires
documented visual/cause review rather than a threshold waiver. Correct
gallery/viewer/share dimensions and colors, independent real screenshot
quality and metadata, second hardware, cancellation/resources,
physical 16KiB runtime, publisher/source evidence, stable signing,
and release authorization remain OPEN. Ordinary HEIC/AVIF are
**disabled**; PNG/lossless WebP remain unchanged. No more reruns of
the unchanged three Pixel synthetic checks are necessary.

**Next bounded technical decision:** prototype a standards-consistent
single-image 65×67 AVIF route with coherent encoded/presented dimensions,
e.g. a validated native input/mux path or a licensed size-bounded software
encoder, before involving personal photos. Compare against the same
fixture and external 65×67 decoders and only then consider the larger
G1/M02 hardware and performance gates. A malformed geometry must not be
patched silently or presented as a faithful output.

## Host-only AVIF correction feasibility — 2026-10-30

**This is a follow-up engineering experiment on the previously verified
synthetic Pixel AVIF bitstream, NOT a new phone test and NOT an exporter
change.** Current runtime stays at `82a40bf2`, with [green
CI 41107077558](https://github.com/santirodriguez/FixMyShot/actions/runs/41107077558).
No HEIC/AVIF format was enabled or published. The file's SHA-256 remains
`3bd7556515d9b5532faef95059e2e20afa4bbc263969f1ffbe5321ae2f51f19c`
(1,358 bytes).

### Metadata-only native-bitstream remux experiment

On a temporary host copy, retained the **exact AV1 `mdat` bytes and their
file offsets** while updating the underlying `ispe` from 65×67 to
**128×67** and associating a HEIF `clap` clean-aperture crop of **65×67**
anchored to the left/top (horizontal offset **−31.5** relative to the
128-wide image center). The added `clap` box/property association consumed
41 bytes of the original `free` box so file length/encoded payload offsets
remained unchanged. The resulting 1,358-byte test file has *different*
SHA-256 `3693e60b5e39a01af4f250726d0665222a0599f91d13110e40b9d3fbaf9a5bab`.

The host results **diverged**:

| Host decoder | Test remux presentation | Interpretation |
|---|---|---|
| libheif 1.19.8 | 65×67, left-hand image visible, PSNR **26.0928 dB** | Applies the clean aperture |
| Pillow 12.3.0/libavif 1.4.2 | **128×67** | Does not present a 65-pixel aperture in this path |
| FFmpeg 7.1.5 | Rejected during header parsing | No decoded image from this prototype |

This is a manually constructed, intentionally **unshipped** proof of concept.
It does **not** establish AVIF conformance or interoperability; rejection
may reflect the prototype's box arrangement or reader limitations. In
particular, replacing only `ispe`, or adding `clap` without verifying
independent-reader behavior, is NOT a safe product correction. Moreover
even the correctly displayed 65×67 crop is below the 38 dB G1 regression
target and still requires visual/color review. AVIF v1.2 §2.2.2–2.2.3
defines `ispe` in terms of AV1 `UpscaledWidth`/`FrameHeight` and
defines clean-aperture rules:
https://aomediacodec.github.io/av1-avif/v1.2.0.html .

### Software encoder reference (NOT an Android integration)

An exact reconstruction of the repository's 65×67 RGB fixture was encoded
on **Linux host only** using Pillow 12.3.0 / libavif 1.4.2,
quality=100, speed=6, full range, with explicit chroma modes:

| 65×67 software mode | Encoded size | Pillow PSNR / patch | FFmpeg PSNR / patch | libheif PSNR / patch |
|---|---:|---:|---:|---:|
| 4:2:0 | 479 B | 29.5502 dB / 1 | 27.2704 dB / 1 | 28.8866 dB / 1 |
| 4:4:4 | 465 B | **55.7412 dB / 1** | **56.4998 dB / 1** | **56.4998 dB / 1** |

All three host readers exposed **exactly 65×67**, and FFprobe showed
`yuv420p` / `yuv444p` respectively with explicitly declared full range.
This proves that a *software reference* can produce coherent small-image
geometry and quality within G1 numeric thresholds in **4:4:4**. It
does **not** prove that an Android build of the same libraries or every
social/gallery destination supports 4:4:4.

A single (not p95) host run with **four encoder threads** and the
deterministic **1344×2992** screenshot-like fixture returned:

| Mode | AVIF bytes | Single host encode | Whole-process peak RSS |
|---|---:|---:|---:|
| 4:2:0 | 9,078 | 0.973 s | 242,604 KiB |
| 4:4:4 | 8,657 | 1.095 s | 267,844 KiB |

The total RSS includes the **Python runtime and NumPy fixture**, so it
is NOT attributable solely to libavif, nor comparable to Pixel's dynamic
Java/native/PSS budget. The fixture is highly repetitive, not a real-world
compression-size benchmark; cold/warm, p50/p95, device memory and other
ABIs have **not** been measured. The 1344×2992 4:4:4 decode PSNR was
55.73–56.49 dB across Pillow, FFmpeg and libheif (patch error 1).
For 4:2:0 it ranged from 37.81 to 44.31 dB depending on reader; this
decoder variation also needs color pipeline investigation.

### Native YUV alternative: feasibility boundary

AndroidX `AvifWriter.INPUT_MODE_BUFFER` exposes a direct
`addYuvBuffer(ImageFormat.YUV_420_888, planarBytes)` route distinct
from the existing EGL/bitmap path. Its current `EncoderBase`
requires a tightly bounded `width*height*3/2` byte count and offers
only 4:2:0 for ordinary 8-bit input. At **odd 65×67**, correctly sized
4:2:0 chroma planes and this simple length rule do not trivially
agree; at even sizes the geometry still depends on the device encoder.
This mode was **read and assessed**, NOT run on the Pixel and NOT
qualified as a 65×67 native fix. An experiment must not reinterpret
the YUV byte array, silently pad/stretch/crop an exported image or
claim that bypassing EGL alone proves conformance. Reference:
https://developer.android.com/reference/androidx/heifwriter/AvifWriter .

**Decision:** retain regular HEIC/AVIF gating. Reject the metadata-only
remux as an export fix. For the next bounded implementation spike,
prioritize pinning and measuring a **licensed libavif + AV1 software
encoder** for Android ABIs, beginning with one synthetic 65×67 4:4:4
candidate. Measure actual added compressed APK cost, native/PSS memory,
time on Pixel and independently decoded pixels before considering
a production route. A narrow direct-YUV native probe may be considered
separately but has no positive Pixel evidence yet. Do not waive G1/M02,
second hardware, metadata/color, source licensing or 16 KiB gates.

## Three-ABI Android software AVIF reference — successful synthetic CI — 2026-10-30

**Scope:** standalone source-built **libavif v1.4.2**
(`c5240fc79fe5c2407e10afd35f5505ef6333ea49`) with **libaom
v3.15.2** (`af3dc9aadc793c1b00edc003e3fc19a44fdeb574`)
and Android NDK `28.2.13676358`. Only the synthetic 65×67 opaque
8-bit 4:4:4 encoder executable is included; no JPEG/HEIC experiment,
JNI bridge, normal Android app dependency, end-user images, format
enablement or production APK is part of this checkpoint.

The initial [Actions 41131994290](https://github.com/santirodriguez/FixMyShot/actions/runs/41131994290)
finished **FAILURE** because the Ubuntu runner lacked `ffprobe`
during its host decode check. Its three ABI builds had succeeded and its
emulator had already generated a valid AVIF, later decoded independently.
The narrowly corrected workflow commit
[`c15f882e`](https://github.com/santirodriguez/FixMyShot/commit/c15f882e5fd4606917485f02be686273c1d13e1d)
changed only host verifier provisioning and the isolated request marker.

[Corrected Actions 41132550109](https://github.com/santirodriguez/FixMyShot/actions/runs/41132550109)
at exact source `c15f882e5fd4606917485f02be686273c1d13e1d`
completed **SUCCESS on attempt 1** in all four jobs:

| Job | Result | Receipt |
|---|---|---|
| `arm64-v8a` | SUCCESS | Artifact `13013204440`, ZIP SHA-256 `3ee216c704abe77c01095b2cf2dc6c96dec6668ebf711bd3006bd860f573375b` |
| `armeabi-v7a` | SUCCESS | Artifact `13014095939`, ZIP SHA-256 `6bb4340eae3ec6dfa2523ebeb218dd54156b64e068b428a86dd2dea057914a9e` |
| `x86_64` | SUCCESS | Artifact `13012564670`, ZIP SHA-256 `f988e9187b58a0ebf9f8779e98bb1e424a60771930edd17a7822b517ee96c7d4` |
| API 36 x86_64 fixture | SUCCESS | Artifact `13012914384`, ZIP SHA-256 `10b4178a731e51e40b5e22450571801f3f1faed78f2ee8a2badd29e44e36d178` |

The final API 36 ZIP was independently downloaded and verified against
GitHub's declared SHA-256: **2,839 bytes, six distinct safe members,
all ZIP CRCs valid**. Its actual `fixture-65x67.avif` is 466 bytes with
SHA-256
`9a88dd96dd1df5008aba8342fb8e70a9f3b51ccff0681ca1e40805d73c475fd9`,
identical to the previously recovered Android-generated control.
Host FFprobe reported **65×67**, `yuv444p` and full range; decoding
the recovered bytes independently using **FFmpeg 7.1.5** and the
repository's deterministic RGB fixture reproduced **PSNR 58.6887 dB**
and **max 3×3 median patch-channel error 1**. The runner itself used
FFmpeg/FFprobe **6.1.1-3ubuntu5** and recorded its versions in the ZIP.

The single new `runtime.json` reports `status=0`,
`encode_ns=18088442` (**18.088442 ms**, timed `avifEncoderWrite` call
only, not source conversion/output write), `encoded_bytes=466`, and
`ru_maxrss_kib=12736` (**12,736 KiB**, entire process, NOT PSS or
codec-only memory). The emulator's `getconf PAGESIZE` was **4096**;
this does not exercise runtime compatibility on 16 KiB hardware.
All three CLI ELF builds previously passed structural 16 KiB
`PT_LOAD` alignment/congruence checks, but no shipping `.so` or APK
ZIP alignment/ABI load was tested.

**Footprint boundary:** prior exact-ABI standalone executables totaled
approximately **20.232 MiB** compressed; stripping debug sections on
temporary local copies yielded approximately **5.479 MiB**. These are
CLI-only estimates, **not** the incremental size of an integrated
universal APK or a release configuration. The prior one-off emulator
measurement (76.7 ms / 13,008 KiB) and this new one (18.09 ms /
12,736 KiB) are not comparable cold/warm p50/p95 samples.

**Still OPEN:** real packaged JNI/library delta, ABI loader behavior,
Pixel/second-device physical 16 KiB runtime when applicable, PSS and
cold/warm p50/p95 on 65×67 and 1344×2992, correct independent
gallery/viewer/share presentation, realistic text/color fidelity,
source/license/patent/provenance clearance and complete G1/M02. The
software spike does **not** fix the earlier Pixel native
AndroidX AVIF mismatch or qualify HEIC. Keep regular AVIF/HEIC export
gated and PNG/lossless WebP unchanged. The next proposed step is a
**separately reviewed isolated JNI/package-cost and resource
measurement**, with no production selector or default change.


## Isolated JNI AVIF diagnostic APK and API 36 feasibility — 2026-10-30

**Exact source:** `a511232d8dce0896cb98d352f45b16903df16df0` on
`release/0.1.0-editor`, [isolated Actions 41134727668](https://github.com/santirodriguez/FixMyShot/actions/runs/41134727668)
**SUCCESS** on attempt 1, all five jobs: three source-built JNI ABIs,
same-source diagnostic APK comparison and API 36 x86_64 instrumentation.
This experiment is opt-in via `fixmyshot.avifProbeMode`: no ordinary
FixMyShot exporter, new format selection, production APK, app permission,
signing identity, external file, release or publication was changed.
The debug diagnostic identity is `org.cajapersonal.fixmyshot.avifprobe`.

**Reproducibility/provenance scope:** libavif v1.4.2 source
`c5240fc79fe5c2407e10afd35f5505ef6333ea49`; libaom v3.15.2
source `af3dc9aadc793c1b00edc003e3fc19a44fdeb574`;
NDK `28.2.13676358`. The source script checks exact Git commits
before compiling. Original libavif/libaom LICENSE and libaom PATENTS
files were captured with hashes and packaged into the diagnostic APK.
These source and legal receipts do **not** complete patent, publisher,
GPL interoperability, transitive-license or production-distribution
clearance.

| Artifact from exact run | GitHub artifact ID | ZIP SHA-256 |
|---|---:|---|
| `arm64-v8a` JNI | `13016110805` | `6d83f80ad555dfe6bd4130e5fe2d2bb69aa7b12ef5583c9a703c70da2bfedc9a` |
| `armeabi-v7a` JNI | `13014812120` | `1d8943615964bc2eaa31be53bb50d4ba8027693101507646a6439552dad06b68` |
| `x86_64` JNI | `13014612715` | `42173a1ba66553fb54e6e54fb0e553c9c5c70875857458788694ff15b677ca79` |
| Diagnostic APK pair | `13016965577` | `7888d95555cf8755d686405cfdb3ebc5fd4e08097db99deb9510161e30617ab4` |
| API36 JNI results | `13015802549` | `8af04b6f88775e1829f6c56f9bb2932a6acb8f0ab507fe567ad5dad10279a834` |

All five downloaded ZIPs were verified against the provider digests;
member uniqueness, safe paths and CRCs passed. Independent APK inspection
verified the exact added payload and native structural alignment.

### Actual equivalent diagnostic APK cost

| Source-matched debug package | Exact size | SHA-256 |
|---|---:|---|
| Baseline without AVIF JNI | **16,940,589 bytes** | `3cc645b5a1ef56f67798200da8f78c1f7ff598ccc1d8b997d5ede7ee8252088e` |
| With AVIF JNI | **25,569,610 bytes** | `f13a5c28c4db3c428cc1a5f8067f2d6b3cccae5c837a572d6d8abe695b0af0fe` |
| Added size | **8,629,021 bytes** (**8.229 MiB**, +50.937%) | Same source, variant, debug identity |

All **473 existing APK ZIP members have identical uncompressed bytes**
between these two packages. The six additions are three source-built
`libavif_probe.so` files and three license/patent files. The packaged
JNI libraries are **2,973,176** (arm64), **2,516,908** (ARMv7)
and **3,097,648** (x86_64) bytes; combined **8,587,732** bytes,
stored uncompressed. The three added legal files total **27,432**
uncompressed bytes. The difference includes ZIP packaging/alignment
overhead. These are **actual diagnostic APK bytes**, not an extrapolated
compressed CLI estimate or a production release APK optimization.

All three packaged JNI ELFs each have three aligned/congruent
`PT_LOAD` segments with `p_align >= 16384`. All three
`ZIP_STORED` JNI member data offsets are divisible by 16,384;
the existing APK members were also independently preserved.
This is structural **16 KiB ELF/ZIP PASS**, not proof of runtime
loading on a physical 16 KiB device; the tested emulator page size
was **4096** bytes.

### JNI runtime: API 36 x86_64 synthetic output

The instrumented APK test finished **OK (1 test)** in 48.364 s.
It loaded `libavif_probe.so` inside the diagnostic app process
and ran **20 sequential 65x67** plus **5 sequential 1344x2992**
deterministic synthetic encodes (not 25 independent cold starts).
Times below measure `avifEncoderWrite` only, excluding fixture RGB
construction/conversion, file output and external provider operations.
P95 uses the nearest-rank statistic; five large iterations are
insufficient for a robust production percentile.

| API 36 x86_64 measure | 65x67 (n=20) | 1344x2992 (n=5) |
|---|---:|---:|
| Median encode | **64.423 ms** | **8,962.779 ms** |
| Nearest-rank p95 | **130.076 ms** | **12,268.763 ms** |
| First output size | **466 bytes** | **6,092 bytes** |
| Maximum post-encode PSS | **94,257 KiB** | **98,948 KiB** |
| Maximum cumulative process `ru_maxrss` | **201,952 KiB** | **285,800 KiB** |

PSS is sampled **before and after** each encode, not during the
operation; its maximum after value is **not peak encoding PSS**.
`ru_maxrss` is a cumulative whole-process peak RSS, not
codec-only memory or PSS. The large-source fixture is highly
repetitive, and its small encoded size does not measure compression
performance for a real screenshot. This experiment does not isolate
cold versus warm starts or measure Pixel CPU/memory/throughput.

Both **actual Android-generated** files were independently decoded
with a separate host FFmpeg and compared pixel by pixel against the
deterministic synthetic reference. FFprobe reported 65x67 and
1344x2992 respectively, YUV 4:4:4 full range with BT.709 primaries
and sRGB transfer. The small file is
`9a88dd96dd1df5008aba8342fb8e70a9f3b51ccff0681ca1e40805d73c475fd9`
SHA-256; the large file is
`a310682eaa8c56d27b0b6c104bd85e8af741a38426165dc1381c3b106a2c66f2`.
RGB PSNR is **58.6887 dB** (small) / **58.7403 dB** (large);
max 3x3 median patch-channel error **1**, max per-channel absolute
error **1** for both. Thus these two synthetic output geometries
and the numeric G1 fixture target pass without the Pixel AndroidX
AV1 raster/ispe mismatch. This does not prove metadata, HDR,
alpha, realistic colored text, gallery/receiver interoperation or
all decoders/devices.

**Decision and remaining gates:** the opt-in JNI/diagnostic APK
cost and API 36 synthetic encoding feasibility experiment is
**PASS in its explicitly bounded scope**. It is not production
integration or delivery acceptance. Normal HEIC/AVIF export remains
unavailable, PNG/lossless WebP remain unchanged, and the earlier
Pixel native AndroidX AVIF geometry failure is not repaired.
Physical Pixel and second-hardware real-photo/text/color checks,
16 KiB **runtime**, controlled peak native/PSS and device p95,
transitive source/license/publisher/patent audit, actual production
APK decision, signing, merge and release gates remain **OPEN**.
No earlier user-accepted Pixel diagnostic must be repeated.

## Physical AVIF probe API36 decoder discrepancy — 2026-10-30

**Scope and source:** new isolated debug diagnostic `avif-physical`,
not the shipping FixMyShot app. Exact source
`17db3c0e8d6e56dab32f792a626392fb21f49875`, isolated
[Actions 41142660997](https://github.com/santirodriguez/FixMyShot/actions/runs/41142660997),
attempt 1, completed **FAILURE**: both pinned native ABI jobs and
source-verifying/signed/zero-permissions diagnostic APK packaging
completed **SUCCESS**, but `api36-synthetic` failed its on-device
pixel-fidelity gate. The previous missing-file/JSON false-positive
was fixed; this run generated a **real** fixture and metrics receipt.
The app was **not** run on a physical Pixel.

The downloaded API36 evidence ZIP, artifact `13020286953`, SHA-256
`61d2b5aeafff5523154e4e34df93dee0abbb9e565e8cf7a4c07feabbc90adc38`
(8,227 bytes, five unique members, valid CRC), contains
`fixture-1344x2992.avif` (6,092 bytes, SHA-256
`a310682eaa8c56d27b0b6c104bd85e8af741a38426165dc1381c3b106a2c66f2`)
and a **valid** `report.json`. This is the exact AVIF byte identity
previously obtained in the synthetic JNI experiment; the encoder
output itself is reproducible. The file identifies 1344x2992
YUV 4:4:4, full range, BT.709 matrix/primaries and sRGB transfer.

| Source-bound measurement | Observed |
|---|---:|
| Emulator | Android API 36, x86_64, 4 KiB pages |
| RGB-to-YUV conversion | 793.973 ms (one sample) |
| `avifEncoderWrite` | 5,610.454 ms (one sample) |
| Conversion + encode + file/sample wall time | 6,539.441 ms |
| Concurrent Android PSS samples | 34, 0 failed |
| Maximum **sampled** in-operation PSS | 150,167 KiB |
| Maximum **sampled** native heap allocation | 225,153,648 bytes |
| Whole-process cumulative `ru_maxrss` | 270,476 KiB |
| Post-operation PSS | 60,636 KiB |

**Sampling limit:** the concurrent `Debug.getPss()` samples are not a
continuous instantaneous peak, do not isolate codec-only memory and
may perturb encoding. One highly repetitive synthetic 4 MP fixture is
neither a real-image benchmark nor a controlled cold/warm p95;
none of these numbers establishes Pixel performance or acceptance.

**Decoder divergence on the very same AVIF bytes:**

| Decode/comparison path | RGB PSNR | Maximum absolute channel error |
|---|---:|---:|
| Independent FFmpeg 7.1.5 raw RGB against reconstructed source | **58.7403 dB** | **1** |
| Android 36 `BitmapFactory.decodeFile` against original bitmap | **44.6782 dB** | **63** |

The independent FFmpeg comparison traversed all **1344x2992x3**
RGB components: maximum error **1**, mean squared error
**0.08690536**, and no channel error above 1.
The Android result reports MSE **2.21444454**, non-opaque
pixels **0**, and correct geometry / `device_decode_status=OK`.
The current synthetic API36 gate requires PSNR at least 38 dB
**and** maximum absolute channel error no greater than 2.
Therefore the actual run **FAILS** that latter gate and must not
be presented as green merely because the independent encoder output
is numerically faithful. This is an **observed difference between
decode paths**, not yet a proven Android decoder defect or an
acceptable visual result in third-party viewers.

The exact source-bound APK artifact `13019741923` ZIP SHA-256
`6bd8e2f850d65efa63e3b469229b38a6a5b02360929509e46ce2f36e12ee4f0c`
contains the 8,866,209-byte debug-only APK with SHA-256
`ec3b63f0af086b09bbe7ea110ee808e67efc93e9951f53fbe04a4451c972548b`.
Its exact-source native SHA, stripped ELF/ZIP 16 KiB structure,
upstream and project notices, package identity and zero requested
permissions passed the package job. These properties do not
qualify Android's viewer fidelity or release signing.

**Next bounded diagnostic:** compare explicitly sRGB-targeted
`BitmapFactory.Options` and `ImageDecoder` paths (and record
pixel-error locations / affected color patches) on a source-matched
disposable emulator run, without weakening the Android presentation
gate. Use the existing genuine AVIF bytes and upstream source pins.
Only if quality is demonstrated in the actual selected display path
should a debug APK be offered for voluntary Pixel screenshot testing.
Do not enable HEIC/AVIF in normal export or repeat earlier accepted
user Pixel native-codec diagnostics. Physical Pixel/second hardware,
real 1344x2992 image appearance, true peak PSS, hardware 16 KiB
runtime, licensing/publisher/patent review, G1/M02, merge/signing
and release remain **OPEN**.

## API36 three-decoder edge localization — 2026-10-30

**Exact-source run:** [Actions 41144625226](https://github.com/santirodriguez/FixMyShot/actions/runs/41144625226) at
`f63619b3db0565a8b38ffb068c7c74b2bb058273`, attempt 1:
pinned ARM64 and x86_64 native builds **SUCCESS**, source/notice/ELF/ZIP/
debug APK packaging **SUCCESS**, API36 synthetic decoder fidelity **FAILURE**.
No physical device was used; the separate `avif-physical` app remains
diagnostic only, not the shipping exporter.

**Independently verified downloaded artifacts:** API36 artifact ID
`13020496137`, ZIP 9,916 bytes, SHA-256
`0345c5312e94d4b21087fb877d18b825db3bfa0e4341428c324f2bbff51a6b2e`,
eight unique safe members, CRC PASS; its three SHA256SUMS receipt entries
match after normalizing the original CI directory prefix.
`decoder-comparison.json` SHA-256
`21e6b333c3b9630d525541ef987f23afc0e69c956a81bee85bcd1f4ffb706503`.
Actual synthetic `fixture-1344x2992.avif`: 6,092 bytes, SHA-256
`a310682eaa8c56d27b0b6c104bd85e8af741a38426165dc1381c3b106a2c66f2`,
identical to the previous JNI synthetic AVIF. Independent `ffprobe`
reports 1344×2992, YUV444 8-bit, full range.
APK artifact ID `13020655122`, ZIP 3,527,889 bytes, SHA-256
`75e1171814e10bd250c88b08f036c9f216c8a3e3ce4ecd47be942a450d3a1f3d`;
debug APK 8,866,209 bytes SHA-256
`b6a2550ca5776e074cec7cde16125ba59ab940505ccddfdbe7bc1cacb63edd3b`.
The APK is **not approved for Pixel installation**.

All three Android API36 paths returned 1344×2992 opaque ARGB_8888
with sRGB profile and **exactly identical** RGB comparison results:

| Android decoder entry point | PSNR | Maximum channel error | Pixels with max error >2 | Gate |
|---|---:|---:|---:|---|
| `BitmapFactory` default | 44.6781555 dB | 63 | 7,896 | FAIL |
| Explicit sRGB `BitmapFactory.Options` | 44.6781555 dB | 63 | 7,896 | FAIL |
| Software sRGB `ImageDecoder` | 44.6781555 dB | 63 | 7,896 | FAIL |

Of 4,021,248 pixels, each path records 3,304,000 exact pixels,
709,352 with max channel error **1**, and 7,896 with max channel error
**at least 32**. No pixels have max error 2–31. Channel counts >2:
R=7,896, G=3,948, B=3,948. All >2 errors are in the bottom quarter;
each of the six horizontal regions contains exactly 1,316 such
pixels, and the first worst coordinate is (0,2250). Pixels with
max error >2 are **0.19636%** of the fixture.

**Initial edge-localization correlation (superseded row hypothesis):**
the Java fixture draws blue two-pixel-wide strokes at x%32<=1 below
y=2244 while y%16<=10. The aggregate count is consistent with 84
stroke columns times 94 rows = 7,896, or 1,316 per horizontal sixth.
Aggregate counts alone do not identify those rows. The previously
suggested top-edge/blue-start assignment was an unverified inference
and is superseded by the RGB discriminator/reconstruction below:
sampled blue-start rows 2244/2256 have error at most 1; the model's
affected rows are blue-end/white-next pairs y%16=10/11. No full
Android error mask was captured.

**Independent RGB reproduction:** decoded the *exact artifact bytes*
through FFmpeg into 12,063,744 raw RGB24 channel bytes, reconstructed
the source image from the exact Java fixture formula and compared
every byte. MSE **0.0869053587**, PSNR **58.7403380 dB**, absolute
max channel error **1**, zero pixels with error >2.
At (0,2250), expected (20,100,220), FFmpeg output (20,101,220);
the adjacent white pixel at (0,2251) stays (255,255,255).
This supports accurate *bitstream* reproduction even at the sharp edge.

**Interpretation and limit:** all three Android Java entry points
produced the same discrepancy. They may share an underlying framework
AVIF decoder; [Skia decoder registration](https://android.googlesource.com/platform/external/skia/+/refs/heads/main/src/codec/SkCodec.cpp)
shows Android can select AVIF decoder implementations through the
framework. The actual API36 emulator backend/feature property was
**not captured**. A chroma reconstruction, edge filtering or color
conversion difference at narrow colored stroke boundaries is
plausible but **not demonstrated**; this does not establish an Android
framework defect, receiver behavior or real Pixel appearance.

**Next narrow deciding diagnostic:** synthetic-only capture of a few
source/decoded RGB triples at known stroke boundary/interior
coordinates, plus the active framework AVIF decoder selection where
available, to distinguish filtering from profile/range handling.
Do **not** export selected user images or log arbitrary user pixels.
Maintain the existing PSNR>=38/max channel error<=2 display gate,
and do not automatically switch previews or hand off the debug APK
while that route is red. Physical Pixel and second hardware, true
memory peak, physical 16KiB execution, representative screenshot
text/appearance, receiver behavior, source/license/patent approval,
G1/M02, signing, merge and release remain **OPEN**.

## API36 RGB discriminator and chroma-reduction reconstruction — 2026-10-30

The narrowly approved diagnostic commit is
[795ec6f2](https://github.com/santirodriguez/FixMyShot/commit/795ec6f26a69fd3f325a7c2e0e023929f50dd336):
exactly four modified paths, no file or mode changes outside the probe.
The normal GitHub tree/commit/ref operations succeeded; the earlier
tree-creation block is resolved without an alternate write surface.
[Actions 41157489912](https://github.com/santirodriguez/FixMyShot/actions/runs/41157489912)
owns the new compiled-source evidence.

Attempt 1 is **FAILURE**: ARM64 and x86_64 native builds and diagnostic
APK packaging **SUCCESS**, API36 report collection **FAILURE**.
Artifact 13025971559 (2,838 bytes; SHA256
3252f5770a8e32af1bf05a3ef6ec13505dfdf590748a5f70bee24beaf4ceab10)
contains a genuine synthetic report but no AVIF file or selector hints.
Both BitmapFactory routes completed and retained 19 RGB points each;
ImageDecoder returned FileNotFoundException. The runner then stopped at
the nonempty-private-fixture check. The private file's disappearance is
observed; its cause is **not established**. A single API36-only job retry
reuses the unchanged compiled APK; successful ABI/APK jobs are not rerun.

Both successful routes retain PSNR 44.67815553399034 dB, maximum
channel error 63 and 7,896 pixels above 2, so Android fidelity is **FAIL**.
Every one of their 19 actual RGB points equals the reconstruction below.
At (0,2250), source (20,100,220) -> Android (56,96,157);
at (0,2251), source white -> Android (219,255,255).
At (0,2244)/(0,2256), source blue -> (19,101,220), max error 1.
The adjacent white columns (2,2250), (31,2250), (34,2250) stay white.
The affected modeled rows are the 47 blue-end/white-next pairs,
y%16=10 and y%16=11, starting 2250/2251, across 84 blue-stroke
columns. Their 47*2*84=7,896 count supersedes the prior *inferred*
top-edge/blue-start membership; the aggregate count itself was correct.
Only 19 actual Android coordinates were captured, not its full error mask.

The two successful routes returned the following identical grouped samples
(all 19 fixed coordinates accounted for):

| Coordinates (x,y) | Source RGB | Android RGB |
|---|---|---|
| (0,2244), (0,2245), (0,2256), (0,2257) | (20,100,220) | (19,101,220) |
| (0,2250), (1,2250), (32,2250), (33,2250), (0,2266) | (20,100,220) | (56,96,157) |
| (0,2251), (0,2267) | (255,255,255) | (219,255,255) |
| (0,2243), (2,2250), (31,2250), (34,2250), (16,2250), (0,2255), (100,2000) | (255,255,255) | (255,255,255) |
| (100,100) | (64,64,64) | (64,64,64) |

Attempt-1 report SHA256:
f7ea7b3c2d00290f9715c9bf4479783993cc2cce6a2942f08ea083039180b0d7
(18,902 bytes), JSON source/delta/coordinate checks PASS locally.
Its single emulator encode was 22.329 s plus 0.149 s conversion;
109 concurrent samples recorded max observed PSS 250,053 KiB and
max observed native heap 414,273,824 bytes. These are noisy emulator
measurements, not a Pixel percentile or true instantaneous peak.

The single unchanged-APK retry (attempt 2) also ended FAILURE:
artifact 13025538696, ZIP 2,715 bytes, SHA256
36279976e8093d4b5d49f968b447ff1a549bed2776969427eef4c45558057923.
Default BitmapFactory again recorded the identical 19 samples and quality;
the explicit route now returned unavailable and ImageDecoder again
FileNotFoundException. No third blind retry was made.

Focused follow-up
[a615c8a6](https://github.com/santirodriguez/FixMyShot/commit/a615c8a6e79f961958d0270c1689552bb4bce8f4)
changes the same four probe paths only: atomically retain the built-in
synthetic AVIF in app-private files rather than evictable cache,
keep synthetic evidence through Activity teardown until AVD disposal,
capture read-only properties before launch and fixed codec-tag logcat
before extraction. The native encoder and real-user-image cache/cleanup
remain unchanged. This addresses an observed evidence-lifetime failure;
it does not claim that cache eviction or Activity teardown was traced
as the deleting actor. All display-fidelity gates stay strict.

[Actions 41158416408](https://github.com/santirodriguez/FixMyShot/actions/runs/41158416408)
at a615c8a6 completed **FAILURE**, specifically at the retained
Android display-fidelity gate. Both source-pinned ABI builds and APK
packaging **SUCCESS**. Evidence retention/collection now completed:
all three Android routes returned correct-size opaque sRGB bitmaps and
**all 57 captured source/decoded/delta samples match the chroma420 model
exactly**. Every route retains PSNR 44.67815553399034 dB, max channel
error 63, 7,896 pixels above 2 and identical histogram/grid.

**Runtime backend evidence:** fixed-tag logcat records three AV1 component
allocations/creations of **c2.android.av1-dav1d.decoder**, all in client
PID 2054, with input width 1344, height 2992, and output color-format
2135033992 (YUV420Flexible). C2SoftDav1dDec logs dav1d
1.5.0-120-gc83cabaf. This is direct component-selection evidence during
the three fixture decodes, stronger than the property hint; it is not
a trace of every internal conversion instruction. Unrelated Vorbis/HEVC
boot logs in the same fixed-tag capture are not fixture evidence.
The read-only media.avif.crabbyavif property was empty/unavailable;
its JSON correctly keeps actual_decoder_backend_confirmed=false because
that *property capture alone* proves no backend. Emulator fingerprint:
google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys,
SDK36, 4096-byte runtime pages.

**Original API36 artifact:** ID 13026425746, ZIP 16,687 bytes, SHA256
37bf7b4ac27fc0de31d20755ab686cf715b450b9fa782f5febc28faced80f61b;
10 unique safe entries, CRC PASS, all four SHA256SUMS entries verified.
Actual AVIF 6,092 bytes, SHA256
a310682eaa8c56d27b0b6c104bd85e8af741a38426165dc1381c3b106a2c66f2,
byte-identical to the earlier exact artifact. Independent decoding of
these **newly recovered bytes** again gives PSNR58.7403380dB/max1.
Report SHA256 80356382090b59c4515ecec0090b04fd6bccc11cc6a103da8d10e4a677f09924;
decoder matrix 743af27ef1c911ed8a342d9547222939ddd3b533066e9baa4f55ceae4687b652;
selector hints d49ed16aa36f6031a476651e94d12a6b32f79290ed1a384067403d4fee99194a;
backend log 2ad96a4ca85c3e227215ff3d08e1655f5932832f4fa2b705624fdb6e18b3a8d2.
The one emulator encode took6.370s plus0.365s conversion;
37 concurrent samples saw max PSS141,661KiB and native heap224,987,056B.
These are sampled emulator observations, not physical peaks or Pixel p95.

The exact APK artifact 13026490043
is 3,529,120 bytes, ZIP SHA256
699e95a497b4d12fb9742595bc27dc352c8dd46a1e29875fade7d504d5f88e5d.
Its source receipt binds a615c8a6; APK 8,866,209 bytes, SHA256
613de84f53a563ad05e4daaeeb2593629d8edd46a0f8501de7a9ccf92b81a592.
ZIP CRC/unique entries, APK/native/notice hashes and ELF/ZIP 16KiB
structure were independently checked; zero permissions and the
ephemeral debug v2 signature passed the CI package gate. This is
not a physical 16KiB test, a production signature or a Pixel handoff.



### Independent mechanism reconstruction

The original synthetic AVIF from Actions 41144625226 was recovered from
artifact 13020496137 and checked against its official ZIP SHA256
0345c5312e94d4b21087fb877d18b825db3bfa0e4341428c324f2bbff51a6b2e.
Its exact 6,092-byte AVIF retains SHA256
a310682eaa8c56d27b0b6c104bd85e8af741a38426165dc1381c3b106a2c66f2.
Independent FFmpeg 6.1.1-3ubuntu5 extraction of its original full-range YUV444 planes
and RGB24 output reproduces PSNR 58.7403380 dB, maximum error 1.

A host reconstruction changes **only the decoded chroma sampling**:
round each 2x2 U/V block average to the nearest integer, repeat that
sample over its 2x2 output block, retain every decoded Y sample,
and convert to RGB with libyuv's full-range BT709 integer coefficients.
Against the exact Java source fixture, this reproduces **every recorded
aggregate** from the prior Android run: MSE 2.214444537284611,
PSNR 44.67815553399034 dB, maximum error 63,
histogram [3304000,709352,0,0,0,0,7896],
RGB counts above 2 [7896,3948,3948], and the full 4x6 error grid.
The control retaining YUV444 with the *same integer converter*
has PSNR 60.1581555 dB, maximum error 1, zero pixels above 2.
The loss is therefore reproducible by chroma reduction alone;
changing an RGB color-space preference cannot recover the discarded chroma.

For the blue-end/white-next pair, the decoded full-resolution chroma
is U/V=(197,82) on blue and (128,128) on white. The rounded 2x2
reduction shares U/V=(163,105). Applying the cited integer conversion
with Y=92 on blue yields (56,96,157), while Y=255 on white yields
(219,255,255). This supplies a concrete pixel-level discriminator,
rather than merely correlating error locations with an edge.

Reproduction after extracting yuv444p with FFmpeg into three HxW planes
and reconstructing the exact Java expected fixture, using NumPy:

~~~python
p = decoded_yuv444.astype(np.int32)
def half_then_repeat(c):
    half = (c[::2, ::2] + c[::2, 1::2] +
            c[1::2, ::2] + c[1::2, 1::2] + 2) // 4
    return half.repeat(2, axis=0).repeat(2, axis=1)
y = p[0]
u = half_then_repeat(p[1]) - 128
v = half_then_repeat(p[2]) - 128
luma = ((y * 257 * 16320) >> 16) + 32
rgb = np.clip(np.stack(((luma + 101*v) >> 6,
                       (luma - 12*u - 30*v) >> 6,
                       (luma + 119*u) >> 6), axis=2), 0, 255)
~~~

Primary implementation references inspected on this date:
[Skia registration](https://android.googlesource.com/platform/external/skia/+/refs/heads/main/src/codec/SkCodec.cpp)
(blob 8cc94f0858a517080ff434bb77adfff2f3ebe58c) conditionally registers
CrabbyAVIF and otherwise documents the HEIF route;
[AOSP dav1d adapter](https://android.googlesource.com/platform/frameworks/av/+/refs/heads/main/media/codec2/components/dav1d/C2SoftDav1dDec.cpp)
(blob 44a8dd1748064a062ea3256a227c2d4d20670f58) routes 8-bit output
through a YV12 conversion;
[AOSP conversion helper](https://android.googlesource.com/platform/frameworks/av/+/refs/heads/main/media/codec2/components/base/SimpleC2Component.cpp)
(blob 7b51afe8d478737ce72a324455df35ac06a32d80) maps I444 to I420;
[libyuv integer conversion](https://chromium.googlesource.com/libyuv/libyuv/+/refs/heads/main/source/row_common.cc)
(blob 951d3c92739e8006f4f47f6390895b0c8e462d91) defines the F709
coefficients used above. These inspected upstream snapshots are
mechanism evidence, **not a trace of the emulator's actual loaded backend**.

### Profile and interoperability limit

FFprobe identifies the recovered synthetic file as AV1 **High**,
8-bit YUV444/full-range BT709 with sRGB transfer.
[AVIF section 8.2](https://aomediacodec.github.io/av1-avif/latest-approved.html#avif-baseline-profile)
limits the Baseline profile to AV1 Main, whereas section 8.3 permits
High for Advanced images. [Android 16 CDD section 5.1.5](https://source.android.com/docs/compatibility/16/android-16-cdd#5_1_5_image_decoding)
requires AVIF Baseline decoding. Accordingly, generic Android AVIF
availability cannot establish preservation of this High/YUV444 input.
The observed chroma loss is not by itself proof of a CDD violation.
A future app-local decoder preserving 4:4:4 would still leave external
Gallery/share receivers as separate compatibility gates; changing the
encoder to 4:2:0 would retain the demonstrated information loss.

### Interpretation and acceptance boundary

The captured Android RGB values and every aggregate have a complete
numerical explanation: decoded 4:4:4 chroma reduced to 4:2:0, followed by
full-range BT709 integer conversion. This is a **reproduced mechanism**. The additional runtime logs above
identify the selected C2 dav1d component for the fixture decodes; the
upstream source review and exact reconstruction explain its chroma-loss
behavior without claiming a trace of every binary instruction or
the exact Skia registration implementation. A property value alone
is not backend proof. The independent good
decoder/control results do not turn the Android display gate green.

Normal PNG/lossless WebP export is unchanged. Keep HEIC/AVIF normal
export and Pixel APK handoff blocked; no phone was tested in this work.
Physical 16KiB runtime, true instantaneous peak PSS, representative
screenshot appearance, second hardware, source/license/patent clearance,
G1/M02, signing, merge and release remain OPEN.

## Isolated direct YUV444 software decoder comparison — 2026-10-31

Source [50e77c66](https://github.com/santirodriguez/FixMyShot/commit/50e77c66bab7080b627188c46791e24fe82622ac)
implements the approved comparison in the existing isolated probe:
nine modified files plus one small historical size receipt. No normal
application source, manifest, permissions, source pins or production export
changed. The opt-in physical-probe CMake switch adds the existing pinned
libaom decoder; ordinary standalone/JNI experiments retain decoder-OFF.

Only the built-in synthetic emulator run adds libavif_aom_direct_444.
The native method accepts only the probe-owned files/physical_ci_0.avif,
one 1344x2992 8-bit opaque full-range YUV444 image with BT709/sRGB tags
and no transforms, bounded to16MiB compressed input. It decodes using
AVIF_CODEC_CHOICE_AOM and converts directly into a caller-owned bitmap
with libavif's built-in RGB conversion, two threads maximum, releasing
decoder/file/locked-pixel resources. It does not process selected real
images, log user RGB, replace the default BitmapFactory preview or
enable any normal format.

The three original Android gates remain independently mandatory:
PSNR>=38dB, maximum channel error<=2, opaque sRGB and correct dimensions.
The fourth route reports its own pass/fail; it cannot approve the
framework route, a Pixel APK handoff or an external receiver.

Cost is compared with the exact previously verified encoder-only
a615c8a6 diagnostic APK, source/run/artifact/APK/native hashes retained
in scripts/avif-software/direct-decode-baseline.json, whose exact JSON
bytes are pinned by the checker. It measures actual two-ABI diagnostic
APK and packaged-native growth. APK differences include Java/report,
JNI, ZIP alignment and signing overhead; they are not a pure decoder
attribution, a same-source matched APK pair or a production3ABI budget.

Decode wall time and concurrent sampled app-process PSS/native heap
are recorded per route; direct native parse/decode and RGB conversion
are timed separately. One sequential emulator decode is not p50/p95,
a controlled cold/warm comparison, physical-device evidence or a true
peak. Android codec-service allocations are excluded from app PSS;
the direct route runs in-process, so these are not comparable complete
device-memory totals.

[Isolated Actions41160768239](https://github.com/santirodriguez/FixMyShot/actions/runs/41160768239)
completed **FAILURE** on 2026-10-31T02:42:30Z at source50e77c66:
ARM64/x86_64 native builds and APK/permission/alignment checks PASS;
API36 synthetic collection completed, all three Android fidelity gates FAIL.
The independent direct probe PASS does not make the run or release pass.

| Route | PSNR dB | MSE RGB | Max channel error | Pixels >2 | Result |
| --- | ---: | ---: | ---: | ---: | --- |
| BitmapFactory default | 44.67815553399034 | 2.214444537284611 | 63 | 7896 | FAIL |
| BitmapFactory explicit sRGB | 44.67815553399034 | 2.214444537284611 | 63 | 7896 | FAIL |
| ImageDecoder software sRGB | 44.67815553399034 | 2.214444537284611 | 63 | 7896 | FAIL |
| Pinned libavif/libaom direct444 | 58.7403380424631 | 0.08690535873440285 | 1 | 0 | PASS, synthetic only |

All four outputs are opaque 1344x2992 sRGB. Direct native status0,
depth8, libavif pixel format1 (YUV444); parse/decode323.077ms,
RGB conversion80.268ms, full measured decode443.032ms.
Direct histogram [3307948,713300,0,0,0,0,0]; channel counts >2 [0,0,0],
4x6 grid all zero. The Android histogram/channel/grid/worst-point
remain exactly as recorded in the preceding discriminator section.

All19 predefined points (one Android column represents all three
identical system outputs); no selected real-image values were captured:

| x,y | Source RGB | Each Android RGB | Direct444 RGB |
| --- | --- | --- | --- |
| 0,2243 | 255,255,255 | 255,255,255 | 255,255,255 |
| 0,2244 | 20,100,220 | 19,101,220 | 20,101,220 |
| 0,2245 | 20,100,220 | 19,101,220 | 20,101,220 |
| 0,2250 | 20,100,220 | 56,96,157 | 20,101,220 |
| 0,2251 | 255,255,255 | 219,255,255 | 255,255,255 |
| 1,2250 | 20,100,220 | 56,96,157 | 20,101,220 |
| 2,2250 | 255,255,255 | 255,255,255 | 255,255,255 |
| 31,2250 | 255,255,255 | 255,255,255 | 255,255,255 |
| 32,2250 | 20,100,220 | 56,96,157 | 20,101,220 |
| 33,2250 | 20,100,220 | 56,96,157 | 20,101,220 |
| 34,2250 | 255,255,255 | 255,255,255 | 255,255,255 |
| 16,2250 | 255,255,255 | 255,255,255 | 255,255,255 |
| 0,2255 | 255,255,255 | 255,255,255 | 255,255,255 |
| 0,2256 | 20,100,220 | 19,101,220 | 20,101,220 |
| 0,2257 | 20,100,220 | 19,101,220 | 20,101,220 |
| 0,2266 | 20,100,220 | 56,96,157 | 20,101,220 |
| 0,2267 | 255,255,255 | 219,255,255 | 255,255,255 |
| 100,100 | 64,64,64 | 64,64,64 | 64,64,64 |
| 100,2000 | 255,255,255 | 255,255,255 | 255,255,255 |

Independent review verified all76 recorded RGB values against source
and prior models: Android matches the 2x2 chroma reduction plus libyuv
F709 integer model at every point; direct444 matches the independent
host RGB at every point. The AVIF is byte-identical to the preceding
test (6092B, SHA256 a310682eaa8c56d27b0b6c104bd85e8af741a38426165dc1381c3b106a2c66f2).
Thus the container/encoder preserves the needed chroma and a direct444
decode on this emulator avoids the system's visible blue/white edge
loss. This is evidence for the previously localized framework
444-to420 chroma-reduction mechanism; it does not identify a changed
line in the exact installed framework binary or prove a defect in
dav1d itself.

Runtime codec logs again contain three c2.android.av1-dav1d.decoder
selections, dav1d1.5.0-120-gc83cabaf and flexible420 format2135033992.
The read-only selector property is empty/unavailable; it remains a
hint, and the selector-hints JSON deliberately does not assert backend
confirmation from the property. API36 fingerprint remains
google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys;
page size4096. No physical Pixel test took place.

Single sequential emulator observations, NOT a performance ranking:

| Route | Decode ms | App PSS before/after KiB | Sampled max app PSS KiB | Sampled max native heap B | Samples |
| --- | ---: | ---: | ---: | ---: | ---: |
| bitmap_factory_default | 1312.163 | 59902 / 79014 | 90959 | 49531808 | 8 |
| bitmap_factory_explicit_srgb | 1139.602 | 79968 / 94575 | 80052 | 65651536 | 7 |
| image_decoder_srgb_software | 861.307 | 71811 / 87476 | 99995 | 89791888 | 5 |
| libavif_aom_direct_444 | 443.032 | 71748 / 90503 | 214935 | 196468784 | 3 |

All sample failures0. Direct cumulative process peak RSS323836KiB
includes earlier encode/decodes, so it is not a decode-specific peak.
App PSS excludes the framework codec service; the direct route adds
in-process decoder allocations. Sequential state, different starting
PSS,150ms sampling and3 direct samples preclude total-memory savings,
true peak or cold/warm/p50/p95 conclusions.

Actual two-ABI diagnostic APK9376881B (baseline8866209B):
+510672B (+5.76%). Packaged arm64 native3225320B (+251432B);
x86_64 native3363296B (+264912B). APK SHA256
a3e23f62b72f4da748ba5f1fba37dbc483e521f4dcf6f5d237866285743cb3c3.
Independent original-APK comparison found the same ZIP entry set;
only classes3.dex and the two native libraries changed. Manifest,
resources and original license/patent notices are byte-identical.
Both ABI source receipts confirm aom_decode=ON and unchanged source
pins; original native artifact hashes match packaged-input receipts.
This is a historical diagnostic delta, subject to the scope caveats above.

Original Actions artifacts (7-day retention; source-bound ZIP SHA256,
safe unique paths/CRC and supplied internal hashes independently checked):

| Artifact | ID | ZIP bytes | ZIP SHA256 |
| --- | ---: | ---: | --- |
| arm64-v8a | 13027475265 | 6383523 | 420ae022562c032c102b85b92760021791133b1c299b45f5099bac922f47ee04 |
| x86_64 | 13026703718 | 6336316 | 948e99d792b7bc72a8560aee08e85265593825b6b265f7585f246949a97d25f9 |
| APK/cost/permissions | 13026484581 | 3812264 | ec55aa7450c5f1d6278977163a3dde5c3ea76b006ee4ab194e19a85312668145 |
| API36 report/fixture/logs | 13026569673 | 18156 | 8b003dcfeac7645ff373fbcdf3bf3e0db29b1c1597e32348c7b400061809606d |

Local validation: shell/Python/embedded-Python syntax; native syntax
against the pinned libavif header (temporary JNI/Android stubs, not an
Android runtime test); exact baseline replay yields zero size delta;
altered baseline bytes and altered legal hashes are rejected. Official
CI supplied both actual Android ABI builds and the emulator execution.


Any passing direct result is limited to the built-in fixture and this
pinned Android ABI/build. It does not repair the framework's chroma loss
or establish Gallery/sharing/other hardware compatibility. Physical
Pixel/second hardware, representative screenshots, true peak memory,
physical16KiB runtime, source/license/patent/security review, G1/M02,
production integration/signing/merge and release remain open.
