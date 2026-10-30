# Optional on-device HEIC/AVIF check

Status: **IMPLEMENTED; TWO DISTINCT USER-REPORTED PIXEL 9 PRO XL API37 SYNTHETIC CHECKS COMPLETED; NATIVE FIDELITY/EXPORT STILL OPEN**. The full current-source UI/core/API36 CI checkpoint is [40966098425](https://github.com/santirodriguez/FixMyShot/actions/runs/40966098425) at compiled commit `d54eb9c7`; the separate user-shared physical Pixel synthetic-check results are recorded below. The check gathers preliminary device evidence; it does not enable native export or close G1/M02. Existing HEIC/AVIF writer/API/fidelity/fallback requirements remain unchanged. No new native library or permission is introduced.

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
the explicit current API36 validation checkpoint. Until that
exact-source CI succeeds and the isolated APK is checked, this is
an **unverified implementation candidate** and no user retest is
requested. After a qualified handoff, request only this new one-file
Pixel capture; do not repeat the two completed diagnostic reports.

The expected comparison is against the previous **65×67 grid-on**
digest `3bd7556515d9b5532faef95059e2e20afa4bbc263969f1ffbe5321ae2f51f19c`.
A different SHA from a new build/encoder run is evidence to inspect,
not grounds to fabricate a match. Both the native pipeline and
software fallback decisions remain subject to G1/M02 and independent
color, visible-geometry, resource and other-device acceptance.
