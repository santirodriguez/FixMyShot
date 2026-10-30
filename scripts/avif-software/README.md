# FixMyShot AVIF software source-build spike (standalone)

**Status:** a bounded experiment, NOT integrated into the app or approved for export.

- Fixture: deterministic 65x67 opaque sRGB, no image input or user data.
- Output: one AVIF using libavif and an AV1 encoder, 8-bit YUV444, quality 100.
- Host proof: tested against installed libavif 1.2.1 / libaom 3.12.1, not the proposed Android pins.
- Android verified source pins: libavif 1.4.2 commit `c5240fc79fe5c2407e10afd35f5505ef6333ea49`, libaom 3.15.2 commit `af3dc9aadc793c1b00edc003e3fc19a44fdeb574`.
- Android NDK: 28.2.13676358; compiled ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64`.
- Only standalone Android executables are built. No APK, JNI, or production dependency has been created.

## Build boundaries

`build-android.sh NEW_OUTPUT_DIR ABI` requires a preinstalled, matching NDK and network access to official git sources. It checks release-tag commit IDs; mismatches stop the build. Uses unoptimized generic CPU selection to reduce assembly/toolchain variables, **not** a performance-optimized shipping configuration. Results must be retained as source-bound CI artifacts. No source tarball or vendored binary is in this package.

The standalone Android builds ran successfully through the isolated GitHub Actions workflow. They are not Android APK or JNI builds. All three ABI CLI ELF programs passed structural 16 KiB alignment checks; the API 36 emulator uses 4 KiB pages, so physical 16 KiB runtime remains untested. Size reported by the build script is a compressed standalone CLI estimate, **not an actual APK delta**. Pixel PSS/p95, full-size real-image fidelity, ABI loader behavior and source-license clearance are NOT RUN. Do not publish/ship this as a replacement encoder.

## Source-bound Android CI result — 2026-10-30

[Actions 41132550109](https://github.com/santirodriguez/FixMyShot/actions/runs/41132550109) at `c15f882e5fd4606917485f02be686273c1d13e1d` completed SUCCESS: three ABI builds plus an x86_64 API 36 synthetic fixture job. The previous run failed only because the Ubuntu runner lacked FFprobe; the narrow workflow correction installs FFmpeg/FFprobe before validation.

The actual 466-byte Android-generated AVIF (SHA-256 `9a88dd96dd1df5008aba8342fb8e70a9f3b51ccff0681ca1e40805d73c475fd9`) decoded at exactly 65×67 in full-range YUV444. Independent host FFmpeg 7.1.5 pixel comparison reproduced RGB PSNR 58.6887 dB and maximum patch error 1. The API 36 artifact ZIP (ID `13012914384`) SHA-256 `10b4178a731e51e40b5e22450571801f3f1faed78f2ee8a2badd29e44e36d178` passed independent member/CRC checks. A single `avifEncoderWrite` call recorded 18.088442 ms and process `ru_maxrss` 12,736 KiB; these are **not** Pixel latency, PSS or full-image measurements.

Full scope and three-ABI artifact digests: [native device evidence](../../docs/feasibility/native-device-check.md#three-abi-android-software-avif-reference--successful-synthetic-ci--2026-10-30). No regular HEIC/AVIF export is enabled.

## Host-only baseline

Compile `avif_spike.c` against the installed host libavif with:

`cc -std=c11 -D_POSIX_C_SOURCE=200809L -Wall -Wextra -Werror $(pkg-config --cflags libavif) avif_spike.c -o avif_spike_host $(pkg-config --libs libavif)`

Run the executable with a *new private output path*, then use `python3 check_fixture.py OUTPUT.avif` to reject wrong geometry, wrong chroma mode, invalid containers or incorrect synthetic pixels. The two reference host encodes (including a previously verified 4:4:4 file) were accepted; an intentionally wrong-chroma control was rejected.

## Licensing and audit gate

libavif is BSD-2-Clause, while libaom's BSD 2-Clause source distribution includes an Alliance for Open Media patent license in PATENTS. Before packaging a native `.so` in an APK, capture exact upstream license and patent notices, audit transitive code, verify source provenance and ABI/NDK features and establish GPLv3 compatibility with the main FixMyShot app. No license or patent clearance beyond that preliminary source review is claimed.

Sources: <https://github.com/AOMediaCodec/libavif/releases/tag/v1.4.2>, <https://aomedia.googlesource.com/aom/+log/v3.15.1..v3.15.2>, <https://developer.android.com/guide/practices/page-sizes>.
