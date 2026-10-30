# Optional on-device HEIC/AVIF check

Status: **IMPLEMENTED; CURRENT API36 AUTOMATED VALIDATION PASSED, PHYSICAL NATIVE ACCEPTANCE OPEN**. The full current-source UI/core/API36 CI checkpoint is [40966098425](https://github.com/santirodriguez/FixMyShot/actions/runs/40966098425) at compiled commit `d54eb9c7`; the separate native-check report has not yet been supplied from actual hardware. The check gathers preliminary device evidence; it does not enable native export or close G1/M02. Existing HEIC/AVIF writer/API/fidelity/fallback requirements remain unchanged. No new native library or permission is introduced.

## Use and boundaries

Open Settings → Check HEIC and AVIF → Run check. It works with an empty draft and uses only deterministic synthetic opaque sRGB images:64×64,65×67 and1344×2992, each attempted once as HEIC and AVIF. It never reads selected images, source grants, output journals or public provider files. The current native adapter requests maximum-quality lossy encoding, grid enabled, with an8-second timeout for each attempt. Vendor start/add/stop/close operations may return late; this is not a hard wall-clock watchdog. Stop/Back/Close or leaving the foreground requests cancellation, then waits for the current codec to return before further work can start.

The ViewModel owns progress, cancellation and the latest report through Activity recreation. Work is serialized on the existing private export/session worker; queued imports wait and new exports are blocked until completion through the normal UI. A second diagnostic runner cannot start while another runner owns its process-wide guard. No background service, automatic startup check, repeated benchmarking, format substitution or retry is added. The report is transient, replaced on a deliberate rerun and lost when the process is gone.

Returned candidates must be directly owned native-still files in batch-candidates. The check independently examines bounded ftyp brands, actual MIME/bounds and full decoded dimensions/opacity before scoring. Files are removed after each attempt, including rejected or cancelled candidates; the encoder owns failure cleanup before returning. An unexpected outside path is never deleted. A process kill can leave a synthetic candidate in the existing private cache; the existing conservative24-hour startup candidate pruning applies. No images are inserted into MediaStore or an SAF folder, and no encoded samples are shared by this feature.

## What the measurements mean

The fixture has six known color patches, dark/light blocks and fine colored edges. Full reopened pixels are normalized to software ARGB8888/sRGB for comparison. The report records exact RGB equality, full-image PSNR and the largest channel error among3×3 patch medians. Exact alpha255 is required. The blueprint's PSNR38dB/patch error2 levels are reported as numeric targets for that one case, never as a support or appearance approval. An exact RGB match uses exact_pixels=true and psnr_db=null, avoiding a nonfinite JSON number.

Adapter elapsed time includes its encode/validation path. This is one attempt per case, not cold/warm p50/p95, native/PSS/Java peak memory, embedded-color-profile correctness, independently decoded appearance, gallery/receiver or physical16KiB acceptance. Numeric failures remain evidence to investigate; they do not automatically discard a desired native format or authorize a software fallback decision without its actual bounded experiment.

Share report opens Android's text chooser only after a separate tap. Closed JSON fields contain API level, bounded manufacturer/model, application version/code, the installed base APK SHA256 when readable, typed outcomes, dimensions, byte count, adapter time and synthetic metrics. They contain no selected-image name/URI/hash/pixels, installation path, serial/account identifiers or raw exception text. Compare the base APK digest with a known source-bound build receipt before associating the report with a commit; it does not cover an arbitrary installed split set or prove device appearance. Cancellation/read failure may leave the digest null. Nothing is sent automatically, and a sharing-app failure remains a localized UI error.

## Validation state

Four NativeDiagnosticTest cases are prepared: independent exact/single-pixel/wrong-color numeric oracle; closed report/exception omission and existing non-native adapter refusal; cancellation/owned disguised-PNG rejection cleanup; zero-encode cancelled/low-budget cases plus own-APK identity. These use an injected encoder for policy/failure boundaries and do not establish native codec success. They join the grouped import/settings/session block, now18 (prior14+4); the separate native probe group remains unchanged. At that earlier checkpoint, the new Android integration was still NOT RUN. The later current-source API36 CI passed the native diagnostic/regression groups and the imported editor flow, but did not establish that native HEIC/AVIF works on physical hardware or satisfy G1/M02. The native user-device report and manual sharing are still NOT RUN.

## Current targeted physical handoff (not a repeat of the accepted edit flow)

The isolated `FixMyShot Next` APK was already delivered for the next-image UI flow and the user accepted that flow. Its compiled source is `d54eb9c7`, SHA-256 `1b526d18191363657ea0d8eadbc7c8a6f61d5a930c4b41d4838ef312390c7743`, debug package `org.cajapersonal.fixmyshot.ux22next`. Do **not** uninstall earlier isolated diagnostics or assume production signing. With this same installed APK, run **Settings → Check HEIC and AVIF → Run check → Share report** once on the Pixel 9 Pro XL. Share the generated JSON text privately for analysis. No personal images are accessed by this synthetic diagnostic.

The offline report checker validates the exact six-case order, typed result fields, independent numeric-target arithmetic, closed/no-URI JSON field set and the reported installed APK hash against an explicitly supplied expected digest. For this diagnostic:

```sh
python3 scripts/check-native-device-report.py /path/to/native-report.json \
  --expected-apk-sha256 1b526d18191363657ea0d8eadbc7c8a6f61d5a930c4b41d4838ef312390c7743
python3 scripts/test-native-device-report.py
```

A valid report is **only structurally and source-identity consistent as self-reported by the app**; the hash is not a remote attestation and a test harness cannot establish hardware truth. Each of the three sizes for each format remains separately available/unavailable and numerically qualified/unqualified. A missing/mismatched APK hash is not accepted as source-bound evidence. An encoded/reopened synthetic still is not sufficient for final pixel/color-profile, real screenshot, gallery/viewer/share, peak-memory, cold/warm latency, cancellation or other-hardware acceptance. Keep HEIC/AVIF export gated, do not silently fall back to PNG, and do not change the codec or release policy based only on this diagnostic.

Pixel full-resolution fidelity is a **separate** remaining exercise: privately compare the original 1344×2992 non-sensitive screenshot with the actual exported PNG under explicit Keep all and, if relevant, a chosen 9:16 crop; inspect dimensions, all source UI content, margins, color/text edges, metadata and original-file immutability. Do not repeat the already accepted consecutive-image/picker-cancel test. A second hardware family, measured resources, 16 KiB runtime and manual accessibility/localization remain separate gates. The original [verification matrix](../blueprints/first-functional-release.md#16-verification-matrix) remains authoritative.
