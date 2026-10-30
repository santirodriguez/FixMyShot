# FixMyShot AVIF software source-build spike (standalone)

**Status:** a bounded experiment, NOT integrated into the app or approved for export.

- Fixture: deterministic 65x67 opaque sRGB, no image input or user data.
- Output: one AVIF using libavif and an AV1 encoder, 8-bit YUV444, quality 100.
- Host proof: tested against installed libavif 1.2.1 / libaom 3.12.1, not the proposed Android pins.
- Android proposed pins: libavif 1.4.2 commit `c5240fc79fe5c2407e10afd35f5505ef6333ea49`, libaom 3.15.2 commit `af3dc9aadc793c1b00edc003e3fc19a44fdeb574`.
- Android NDK: 28.2.13676358; ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64`.
- Only a standalone Android executable is specified, to be built on CI once repository integration is authorized and available. No APK, JNI, or production dependency is created.

## Build boundaries

`build-android.sh NEW_OUTPUT_DIR ABI` requires a preinstalled, matching NDK and network access to official git sources. It checks release-tag commit IDs; mismatches stop the build. Uses unoptimized generic CPU selection to reduce assembly/toolchain variables, **not** a performance-optimized shipping configuration. Results must be retained as source-bound CI artifacts. No source tarball or vendored binary is in this package.

The toolchain and dependencies were **not available for Android compilation in this analysis runtime**; Android build, ELF alignment, three-ABI size, Android runtime memory, emulator quality and Pixel performance are NOT RUN. Size reported by the build script is only a compressed standalone CLI estimate, **not an actual APK delta**. Do not publish/ship this as a replacement encoder.

## Host-only baseline

Compile `avif_spike.c` against the installed host libavif with:

`cc -std=c11 -D_POSIX_C_SOURCE=200809L -Wall -Wextra -Werror $(pkg-config --cflags libavif) avif_spike.c -o avif_spike_host $(pkg-config --libs libavif)`

Run the executable with a *new private output path*, then use `python3 check_fixture.py OUTPUT.avif` to reject wrong geometry, wrong chroma mode, invalid containers or incorrect synthetic pixels. The two reference host encodes (including a previously verified 4:4:4 file) were accepted; an intentionally wrong-chroma control was rejected.

## Licensing and audit gate

libavif is BSD-2-Clause, while libaom's BSD 2-Clause source distribution includes an Alliance for Open Media patent license in PATENTS. Before packaging a native `.so` in an APK, capture exact upstream license and patent notices, audit transitive code, verify source provenance and ABI/NDK features and establish GPLv3 compatibility with the main FixMyShot app. No license or patent clearance beyond that preliminary source review is claimed.

Sources: <https://github.com/AOMediaCodec/libavif/releases/tag/v1.4.2>, <https://aomedia.googlesource.com/aom/+log/v3.15.1..v3.15.2>, <https://developer.android.com/guide/practices/page-sizes>.
