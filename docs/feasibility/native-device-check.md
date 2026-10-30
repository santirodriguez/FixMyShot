# Optional on-device HEIC/AVIF check

Status: **IMPLEMENTED; ONE PIXEL 9 PRO XL API37 SYNTHETIC REPORT RECEIVED; NATIVE FIDELITY/EXPORT STILL OPEN**. The full current-source UI/core/API36 CI checkpoint is [40966098425](https://github.com/santirodriguez/FixMyShot/actions/runs/40966098425) at compiled commit `d54eb9c7`; the separate user-shared physical Pixel synthetic-check results are recorded below. The check gathers preliminary device evidence; it does not enable native export or close G1/M02. Existing HEIC/AVIF writer/API/fidelity/fallback requirements remain unchanged. No new native library or permission is introduced.

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
