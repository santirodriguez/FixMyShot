# Testing and evidence

## Checks in this slice

`core` runs 23 cases for format/default policy, API boundaries, unavailable routes, dimension overflow, WebP limits, memory estimates and metadata-preservation rejection. These are policy tests, not codec execution.

`CodecSmokeTest` contains four instrumentation tests: Activity startup, the synthetic PNG/WebP matrix, pre-cancellation/invalid-input/metadata rejection, and insufficient-budget rejection without a candidate. The image test generates 20 synthetic fixtures, including 1344x2992, odd dimensions, dark/light surfaces, one-pixel colored strokes and binary-alpha edges. It encodes PNG and lossless WebP twice per fixture (80 encodes per API), reads container structure/CRC/VP8L and compares every decoded opaque pixel. Transparent hidden RGB is excluded; partial-alpha tests remain pending.

The schema-2 device report records first/repeat timings and before/after memory snapshots: **neither process-cold performance nor peak memory**. It records the actual normal process heap limit, memory class and page size via `Os.sysconf(OsConstants._SC_PAGE_SIZE)`. The shell `getconf` command is optional because the API 26 system image used here lacks it. The matrix uses a `pixel_2` emulator hardware profile with API 26 and 36 and verifies that its actual heap can admit the largest fixture before allocating it. An emulator profile is not physical Pixel acceptance. Production resource limits and the absence of `largeHeap` are unchanged. A separate test exercises the real adapter's rejection with a deliberately insufficient budget.

`check-apk.py` inspects each shipped ELF and APK data offset for 16 KiB alignment and requires arm64-v8a, armeabi-v7a and x86_64 with the JNI library. This is not a 16 KiB runtime test.

`check-device-report.py` rejects incomplete/failed/crashed instrumentation, incorrect test counts, missing or duplicated matrix results, wrong dimensions/routes, invalid measurements and missing synthetic output files. Its ten host-side regression tests also exercise the collector with a missing `getconf` and unusable report text. Mocked collector tests are not Android codec evidence.

## Running

```sh
python3 scripts/check-bootstrap.py
python3 scripts/test-device-report.py
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
python3 scripts/check-apk.py app/build/outputs/apk/debug/app-debug.apk
bash scripts/run-device-smoke.sh
```

Run the device script only against an explicitly selected, authorized emulator/test device with no competing adb target. It installs the already-built debug application and instrumentation APK, clears only its own synthetic report/output files, runs the four tests, and retrieves evidence before uninstalling or replacing the application. No user images are imported. The debug packages remain on that test device for inspection; uninstalling them later removes their synthetic working data.

In CI, `run-device-smoke.sh candidate` verifies the downloaded build's source SHA against the checkout and checks all APK hashes before installation. Both emulator jobs use the **same built APK bytes**, not separate builds. `am instrument` returning zero is insufficient: the collector requires successful instrumentation completion, all four tests and the full device report. It preserves two fixture-0 PNG/WebP outputs for independent host decoding. They are synthetic test artifacts, not promotional app screenshots.

For a JVM-only check with an existing Kotlin compiler, compile `CodecContract.kt`, `ContractCases.kt` and `StandaloneChecks.kt`, then run the generated main class. Record the actual compiler/JDK: a standalone check with a different compiler is not the pinned Gradle build.

## Evidence ownership

Actions artifacts are named with the exact commit SHA. Build artifacts contain JDK/Gradle versions, test/lint results, unsigned/debug/instrumentation APK candidates, APK SHA-256 values, native size/alignment JSON, the exact source archive and generated wrapper/dependency-verification candidates. Device artifacts contain source SHA, emulator build/heap properties, instrumentation output, `g1-smoke.json` and the two synthetic full-size outputs when available.

No passing build or device run is recorded merely because a workflow was authored. Inspect the exact run result and actual report/image bytes. Artifact retention is 14 days; accepted benchmark/decision receipts must be promoted into their durable repository owners before relying on them in later phases. Never commit private images or device identifiers from a user's real phone without consent.

## Verified bootstrap baseline

At `1d2be729aca19e3ce02bff8f10004c365d0a8674`, Actions run `39497402929` passed the build job: 23 JVM tests, debug lint, debug/unsigned-release/instrumentation APK builds and native ELF/ZIP checks. Independent artifact inspection confirmed those results. Lint reported zero errors and 13 warnings (tool/dependency update suggestions, the conservative target, a redundant resource qualifier and the provisional icon's missing monochrome layer). Those warnings were not suppressed or represented as a clean lint result.

The corrected source `36aff069f7676ba7b58f8855c066b7530b85c112` also passed the build job in run `39522597379`, including all 23 JVM and ten host collector tests, lint and all three APK builds. The source archive reconstructed the exact Git tree `42287a75241d21dcef80905828b8ebfcce2f0c53`; the governing blueprint and LICENSE were unchanged. The unsigned release candidate was 21,424,726 bytes (SHA-256 `6756cb1c9653269a0b5d2e01400ed885aed7ab8719872290e33ee6672f625346`). This is an incomplete development build, not a signed functional release or an independent reproducibility result. Device acceptance is a separate check of the same run.

The earlier `1d2be729` run did **not** pass the device matrix. API 26 stopped before tests because `getconf` was missing. API 36 passed two of three tests but its generic emulator's normal heap could not admit the first WebP fixture under the existing resource policy. The subsequent `run-as` collection also returned an unknown-package error after Gradle's connected-test cleanup. The corrected collector runs prebuilt APKs directly, validates reports instead of trusting exit codes, measures page size in-app and uses a suitable phone profile without weakening the application budget.

## Open G1 acceptance

The initial slice does not close G1. Standard wrapper/lockfile/verification review and persistence, security/license inventory, complete PNG/WebP performance and native/PSS peaks, mid-encode cancellation/timeouts, physical Pixel and second-family HEIC/AVIF output, bounded JXL build/cost decision, 16 KiB runtime and independent clean-build comparison remain open. A successful synthetic emulator matrix does not replace these gates or the real-image/manual evidence in the [governing blueprint](blueprints/first-functional-release.md).
