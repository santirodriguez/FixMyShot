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

HEIC/AVIF native writer feasibility uses the pinned AndroidX HeifWriter release candidate; inspect actual device output before advertising those formats. No imported input or public export pipeline exists yet.

Native WebP is built from the immutable upstream revision in `codec-native/src/main/cpp/CMakeLists.txt`. Builds need network access to trusted artifact repositories and that source revision; the installed application does not. Never add opaque prebuilt codec binaries or runtime downloads.

## Development validation cadence

Work in coherent implementation batches instead of launching both Android emulators on every small commit. During a batch, use relevant source review, Kotlin/JVM or dependency-free Python checks, and other inexpensive targeted validation. Such checks are not equivalent to an Android build or physical-device acceptance.

The release branch runs the full build plus API 26/API 36 emulator matrix only when a commit explicitly creates or updates `.ci/android-validation-request.txt`. Include the batch purpose and the precise source state in that checkpoint commit; inspect that exact Actions run and source-bound artifacts before declaring the batch accepted. A pull request into `main` retains the complete matrix before a separately authorized merge. A manual workflow-dispatch button is unavailable while this workflow does not exist on the default branch; do not merge prematurely just to expose one.

Do not write a checkpoint file at the start of development or for documentation-only commits. Rework related failures as one bounded slice, and honor the 5-minute normal/10-minute hard passive CI waiting limit. Preserve all formal Phase G1/G5/G6 hardware, codec, reproducibility and signing gates regardless of the reduced routine CI frequency.


For the **isolated JXL 0.12 reference feasibility** run, review the pinned source-build helper and then update `.ci/jxl-probe-request.txt` on the release branch. The separate read-only Actions matrix builds the three native ABIs without launching full app/emulator CI; its library-size and ELF alignment reports do not certify a shipping encoder. Before interpreting the Gradle trust candidates, run `python3 scripts/test-gradle-candidates.py` and `python3 scripts/audit-gradle-candidates.py <source-bound-bootstrap-artifact.zip>`; generated hashes remain untrusted until independently corroborated. Details and actual experiment state are in `docs/feasibility/phase1-batch-preflight.md`.

## Ownership

- `core`: platform-independent format, eligibility and resource contracts.
- `codec-native`: source-built WebP/JNI, bounded lossless encoding and a separate native HEIC/AVIF writer feasibility adapter.
- `app`: provisional Compose launcher, localized resources and Android fixture tests.

The encoder accepts only already-normalized 8-bit sRGB bitmaps and returns an app-private candidate. It is not the import, metadata-preservation, geometry or public-export pipeline. Call it off the main thread, preserve its sequential execution and dispose of candidates explicitly.

## Translations and reports

Android XML resources are canonical. `python3 scripts/check-bootstrap.py` checks the four implemented keys, duplicate/missing entries, placeholders, XML and bootstrap permissions without Android Studio. It is intentionally not the full future plural/array/markup/packaged-language validator. Use Argentine Spanish and natural Catalan; automated structure checks do not replace linguistic review.

Keep implementation and technical documentation in English. Do not include private screenshots, source filenames, content URIs, credentials or personal metadata in reports. Use the synthetic harness and identify the exact source SHA, API, device and check performed. See [TESTING](docs/TESTING.md) for acceptance boundaries.
