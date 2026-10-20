# Contributing

## Bootstrap build

Use JDK 17 and Android SDK platform 36, build tools 35.0.0, NDK 28.2.13676358 and CMake 3.22.1. The initial candidate pins AGP 8.13.2, Gradle 8.13, Kotlin 2.2.21 and Compose BOM 2025.12.00. These are starting pins, not claims of current security approval or independently reproducible output. Review the actual resolved dependency/security inventory before freezing G1.

```sh
bash scripts/bootstrap-gradle.sh
python3 scripts/check-bootstrap.py
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest
```

The temporary bootstrap downloads the official Gradle distribution, verifies its published SHA-256 and generates/checks the standard wrapper in an isolated empty project. **Committing the reviewed standard wrapper, resolved lockfiles and verification metadata is still required before G1 closes.** CI captures those files as candidates and does not commit them or grant itself repository write access. Initial `--write-verification-metadata` output records observed bytes; it does not prove their trustworthiness. Replace trust-capture mode with strict committed verification after review.

Native WebP is built from the immutable upstream revision in `codec-native/src/main/cpp/CMakeLists.txt`. Builds need network access to trusted artifact repositories and that source revision; the installed application does not. Never add opaque prebuilt codec binaries or runtime downloads.

## Ownership

- `core`: platform-independent format, eligibility and resource contracts.
- `codec-native`: source-built WebP/JNI and bounded Android lossless encoding.
- `app`: provisional Compose launcher, localized resources and Android fixture tests.

The encoder accepts only already-normalized 8-bit sRGB bitmaps and returns an app-private candidate. It is not the import, metadata-preservation, geometry or public-export pipeline. Call it off the main thread, preserve its sequential execution and dispose of candidates explicitly.

## Translations and reports

Android XML resources are canonical. `python3 scripts/check-bootstrap.py` checks the four implemented keys, duplicate/missing entries, placeholders, XML and bootstrap permissions without Android Studio. It is intentionally not the full future plural/array/markup/packaged-language validator. Use Argentine Spanish and natural Catalan; automated structure checks do not replace linguistic review.

Keep implementation and technical documentation in English. Do not include private screenshots, source filenames, content URIs, credentials or personal metadata in reports. Use the synthetic harness and identify the exact source SHA, API, device and check performed. See [TESTING](docs/TESTING.md) for acceptance boundaries.
