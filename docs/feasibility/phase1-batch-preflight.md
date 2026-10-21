# Phase 1 batch preflight — native image geometry, JXL and dependency trust

Status: **PARTIAL, NOT G1 ACCEPTANCE** (2026-10-21). This record accompanies a coherent development batch, not a format-support announcement or completed Android device benchmark.

## Source and acceptance boundaries

- Last complete Android bootstrap/emulator matrix: GitHub Actions [39631701532](https://github.com/santirodriguez/FixMyShot/actions/runs/39631701532) on `fa6dd24f00c2b053c0dcbce16f41c7659d9f5590`; build, API 26 and API 36 passed. The present pure-core geometry and feasibility-script changes have **not** been compiled by that run.
- Development branch uses explicit integration checkpoints (`.ci/android-validation-request.txt`), rather than running two emulators on ordinary code pushes. A full source-bound Android run remains required before accepting this batch.
- The unchanged code contract keeps Android 8/API 26, PNG as the new-session default, real lossless WebP, explicit unavailability, no substitution, no INTERNET permission and the full Phase 0 G1/G2 gates.

## 1. HEIC size/decoding discrepancy — independently repeated

Actual retained app-private synthetic samples from API 36, source `fa6dd24f`:

| Requested experiment | Container image | FFprobe stream / coded size | Independent cropped RGB PSNR, synthetic ROI |
|---|---|---|---|
| HEIC 64×64, grid request OFF | 64×64 | 64×64 | 19.9974 dB |
| HEIC 66×68, source 65×67 padded, grid request OFF | 66×68 | 96×68 / 96×72 | 20.0890 dB |
| HEIC 66×68, same source, grid request ON | 66×68 | Identical bitstream to grid OFF | Not an independent tiling test |

The 66×68 OFF and ON outputs are **byte-identical** (SHA-256 `8b42b0f4bbef1ac325e7b626390d246afdfe52969fde119215e3742fbe1665af`), 3,438 bytes. `heif-info` reports a logical image of 66×68, 8-bit YCbCr 4:2:0 and **no embedded color profile or metadata**; FFprobe exposes the larger coded width and a different apparent color-signaling interpretation. The independent host calculation explicitly cropped the decoded image to its *requested* visible width before comparing the original 65×67 region. Its ~20.09 dB result does **not** reproduce Android's ~10.53 dB value on the same region. The different decoder/color path, image bounds and metadata must be investigated before any production change. This experiment does not establish that the encoded content is correct, fully color-managed or generally supported.

- HEIC 65×67 remained `INVALID_OUTPUT` without padding.
- All four AVIF configurations remained `INVALID_OUTPUT` on that API 36 emulator.
- **No acceptance:** a padded 66×68 diagnostic is not a 65×67 export; small synthetic 20 dB results do not pass calibrated visual/color fidelity, and actual hardware remains untested.

## 2. JPEG XL feasibility — distinguish executed host work from Android build

Reference candidate: official [libjxl v0.12.0](https://github.com/libjxl/libjxl/releases/tag/v0.12.0), immutable tag commit `a7a9c787341cf703dede03c2009fa460cae5e5df`. The reference implementation is under a BSD-3-Clause license; its Brotli, Highway, color-management and other source dependencies require separate licensing/security and packaging audit. The project uses a **source-built** JNI path if JXL is included; no prebuilt wrapper or unverified AAR is an acceptable shortcut. The v0.12 release includes upstream security fixes and is preferred over older installed libraries.

Actual **host Linux baseline** (FFmpeg 7.1.5, dynamically linked to distro libjxl **0.11.1**, `-distance 0 -xyb 0 -effort 3`; synthetic RGB8; one encode/decode per fixture):

| Synthetic input | JXL file | RGB exact after decode | Encode elapsed / max process RSS |
|---|---:|---|---|
| 65×67 (13,065 bytes RGB) | 85 B | Yes, exact SHA-256 | 0.071 s / 33,536 KiB |
| 1344×2992 (12,063,744 bytes RGB) | 1,390,502 B | Yes, exact SHA-256 | 0.265 s / 160,384 KiB |

The small fixture is intentionally low-complexity; these sizes/times are **not** representative Android results. Measurements are process-level peak RSS for one FFmpeg invocation, not app Java/native/PSS budget, NDK 16 KiB compatibility or pixel appearance acceptance. Distro libjxl0.11.1 **must not be used as the shipping security baseline**; these are temporary tool-side experiments with synthetic data only.

The pinned source-build preflight `scripts/probe-jxl-reference.sh` was checked with `bash -n` and invoked once in the isolated execution environment. It returned `NOT_RUN: pinned Android NDK r28 toolchain unavailable`. External source fetching was also unavailable from this isolated container. **No v0.12.0 source build, Android ABI output, compressed APK delta or JNI encode/decode has occurred**. The helper is a bounded, explicit future integration-time Android build attempt targeting arm64-v8a, armeabi-v7a and x86_64, recording pinned commit/submodules/build logs/sizes. Its results are not automatically accepted; inspect actual linked native runtime objects and user-facing behavior before a G1 decision. The required official source-and-hardware run remains open. No JXL format selector should be enabled meanwhile.

## 3. Gradle trust-candidate audit

Source-bound Actions build ZIP `12360066348` SHA-256 `f20d6e57336f3c4f48035685553d1d03b1f544bc5bd6d650f4431b1caabdc967` was parsed read-only and compared against the earlier source-bound candidate `acbdebac`. Wrapper and lockfile findings:

- Gradle **8.13** binary distribution checksum exactly matches the published official value `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`.
- `gradle-wrapper.jar` independently hashes to `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`, identical to the [official Gradle 8.13 wrapper reference](https://gradle.org/release-checksums/).
- Generated dependency lock entries: app **276**, codec-native **161**, core **35**. No coordinate changes compared with the previous source-bound build candidate.
- Generated dependency-verification metadata contains **502 components, 874 artifacts and 874 SHA-256 entries**, with **zero PGP signature entries**. Its checksum matches the previous candidate (`0448ccbcb0f31a437693d6de3d8724f3e634ccd7efbfbfafdc7ca1c9a4bf294c`). It is a reproducible observed-byte list, **not** independent proof of artifact origin or integrity before the first download.

No generated lockfile or verification XML was committed or declared trustworthy by this audit. The next trust phase must review critical artifact provenance independently, determine signing/authenticity and freeze Gradle verification in strict mode, instead of keeping CI in `--write-locks --write-verification-metadata` capture mode indefinitely. [Gradle's documentation](https://docs.gradle.org/current/userguide/dependency_verification.html) explicitly warns that initial checksum generation needs independent review.

## 4. Independent pure-core geometry groundwork

`GeometryPlanner` in `:core` implements immutable arithmetic-only **contain** plans, not image rendering, import/export or UI:

- Exact reduced aspect ratio chooses the minimal integer canvas and centers source pixels without cropping or recompression.
- Play-phone envelope enforces minimum 320-pixel sides and maximum 2:1 aspect by adding margins only.
- Explicit exact-size canvas uses one uniform scaling factor, with no implicit enlargement; out-of-budget canvases reject rather than clip silently.
- 64-bit checked intermediates and the 24 MP provisional pixel cap prevent integer overflow/accidental oversized allocation.

A Kotlin 1.9 standalone local compile and execution passed **23 existing codec contracts + 14 geometry cases**, including 1,000 generated preserve-pixel cases and the Pixel 9 Pro XL examples from the immutable blueprint (§7). This local Kotlin compiler is not the pinned Gradle/Kotlin 2.2.21 toolchain and no Android renderer consumes these plans yet. G2 UI/renderer alignment, actual EXIF normalization, crop/overrides, dynamic memory and device acceptance remain NOT RUN.

## Integration checkpoint and next blockers

Do **not** infer new Android acceptance from the local work above. Continue safely batching independent work. At a genuine integration checkpoint, trigger exactly one source-bound full build + API26/API36 matrix by updating `.ci/android-validation-request.txt` in the release branch and examine the resulting APK/test evidence. The JXL reference Android ABI source-build helper is a separate costly one-off trial; execute it deliberately on an authorized disposable build runner, capture failures and full source provenance, and do not hide a failed/absent benchmark behind a green unrelated build.

Actual Pixel9ProXL and another physical manufacturer, 1344×2992 real screenshot/color/metadata/alpha/fidelity, native HEIC/AVIF actual encode/reopen/display, libjxl release/JNI/16KiB cost and independent artifact authenticity gates remain open. No signing, merging, publishing or deployment is implied by this preflight.
