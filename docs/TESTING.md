# Testing and evidence

## Checks in this slice

`core` runs 23 cases for format/default policy, API boundaries, unavailable routes, dimension overflow, WebP limits, memory estimates and metadata-preservation rejection. These are policy tests, not codec execution.

`CodecSmokeTest` contains four instrumentation tests: Activity startup, the synthetic PNG/WebP matrix, pre-cancellation/invalid-input/metadata rejection, and insufficient-budget rejection without a candidate. The image test generates 20 synthetic fixtures, including 1344x2992, odd dimensions, dark/light surfaces, one-pixel colored strokes and binary-alpha edges. It encodes PNG and lossless WebP twice per fixture (80 encodes per API), reads container structure/CRC/VP8L and compares every decoded opaque pixel. Transparent hidden RGB is excluded; partial-alpha tests remain pending.

The schema-2 device report records first/repeat timings and before/after memory snapshots: **neither process-cold performance nor peak memory**. It records the actual normal process heap limit, memory class and page size via `Os.sysconf(OsConstants._SC_PAGE_SIZE)`. The shell `getconf` command is optional because the API 26 system image used here lacks it. The matrix uses a `pixel_2` emulator hardware profile with API 26 and 36 and verifies that its actual heap can admit the largest fixture before allocating it. The profile alone does not raise API 36's normal heap-growth limit. A separate CI-only helper sets both Dalvik heap properties to 512 MiB and restarts the emulator runtime, after checking Actions, an emulator serial, QEMU properties, the expected API and a debuggable image. It refuses physical/unknown devices, leaves API 26 unchanged, records before/after properties and still requires the in-app measured heap. No SELinux policy is disabled. An emulator profile is not physical Pixel acceptance. Production resource limits and the absence of `largeHeap` are unchanged. A separate test exercises the real adapter's rejection with a deliberately insufficient budget.

`check-apk.py` inspects each shipped ELF and APK data offset for 16 KiB alignment and requires arm64-v8a, armeabi-v7a and x86_64 with the JNI library. This is not a 16 KiB runtime test.

`check-device-report.py` rejects incomplete/failed/crashed instrumentation, incorrect test counts, missing or duplicated matrix results, wrong dimensions/routes, invalid measurements and missing synthetic output files. Its ten host-side regression tests also exercise the collector with a missing `getconf` and unusable report text. Eight additional tests cover the CI heap helper's guards, unchanged API 26 behavior, intended API 36 setup and property-change failure. Mocked collector/setup tests are not Android codec evidence.

## Native HEIC/AVIF feasibility spike

`NativeCodecProbeTest` is a **separate one-test Android instrumentation run** after the existing four-test/80-encode PNG/WebP matrix. It attempts one already-normalized, opaque 65×67 synthetic image per native format through the selected AndroidX bitmap writer. The result `g1-native-probe.json` records either a real encoded-and-reopened sample or a typed unavailable reason. Successful output bytes are retained under `g1-native-fixture.heic` / `g1-native-fixture.avif` for independent container and appearance inspection. Host validation requires an actual positive instrumentation result and truthful samples, refusing corrupt, mismatched or missing output. A malformed native file or a file that only exposes bounds/MIME but cannot decode actual pixels yields typed `INVALID_OUTPUT`; the synthetic probe then continues to the other format. The collector filters device-side `cat: ... No such file` text even when `adb` returns status zero; successful output still requires independent BMFF validation. A native probe failure does **not** downgrade to another format or purport to be availability. An emulator failure is evidence of that environment, not device-wide impossibility. The AndroidX 1.2.0-rc01 release candidate contains color-aspect and AVIF fixes absent from observed stable 1.1.0; build success alone is not quality acceptance.

These tiny probes do **not** satisfy the mandatory full-size Pixel/other-hardware, calibrated-patch, text-edge, PSNR, color-tag, performance or cancellation gates. The lossless G1 matrix remains independently regression-tested and unchanged.

## Running

```sh
python3 scripts/check-bootstrap.py
python3 scripts/test-device-report.py
python3 scripts/test-emulator-heap.py
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

## Bounded API 26 codec receipt

Run `39522597379`, source `36aff069f7676ba7b58f8855c066b7530b85c112`, passed all four tests and all 80 PNG/WebP encodes on the API 26 x86_64 emulator. The measured normal heap was 536,870,912 bytes and page size was 4096 bytes: this is **not** 16 KiB runtime acceptance. WebP used the source-built `LIBWEBP` route. Container checks and all fixture pixel comparisons passed; pre-cancellation, invalid requests and insufficient-budget rejection left no candidate.

Independent host decoding with Pillow 12.3.0/libwebp 1.6.0 compared every pixel of both retained synthetic 1344x2992 fixture-0 outputs to a separately regenerated reference:

| Output | Bytes | File SHA-256 |
|---|---:|---|
| RGB PNG | 92,485 | `47e4ec5d95fff2aef67d55a913fd9581c4a28284e7ae5ee6b80675a093f9f734` |
| VP8L WebP | 2,084 | `1f9ddb0ee982acaa1a68582f782c3f0dfd75b49bd88deef0165b6e8f534a99e2` |

Both decoded RGB rasters had SHA-256 `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861`. The synthetic PNG contained IHDR/sBIT/IDAT/IEND, not an explicit sRGB/ICC profile; complete color-tag/profile policy remains open. The WebP contained VP8L. These simple synthetic sizes and timings are not representative compression/performance claims, and this is not a complete metadata/color acceptance test.

The fixture-0 first/repeat encode times were PNG 0.104/0.146 s and WebP 0.861/0.814 s. They are emulator observations, not Pixel benchmarks, p95 results or process-cold measurements. PSS snapshots are not peak memory.

Artifact `12312502424` has ZIP SHA-256 `f3a13e276df39aaea4ea9476bbbe7857d1335c146bc16268ff3106a3f416b9d0`. The build artifact is `12311762324`, ZIP SHA-256 `f7ce3dd1b514dd78b64b3470c37c207a7094b3fe873cce7028efa12dc2ac08e6`. They expire under the normal 14-day retention; this receipt and the source fixture generator preserve the acceptance scope, not an indefinite archive of every transient report.

API 36 in that same run passed three tests but rejected the image matrix before allocation: actual heap limit 201,326,592 bytes (192 MiB), provisional WebP estimate 113,287,168 bytes, greater than half that heap. Its report correctly remained `INCOMPLETE`, with zero successful matrix samples. The fail-closed collector rejected it. Artifact `12312167597` has ZIP SHA-256 `75607197f16e4177ef35a2d65fd6418b6e06bbbe253097ac4a508984233edc15`. The subsequent CI-only heap setup must pass a fresh actual run before API 36 codec acceptance is claimed. Production resource estimates/limits are unchanged.

The runtime reads the growth limit separately from the maximum heap; see the [AOSP AndroidRuntime source](https://android.googlesource.com/platform/frameworks/base/+/68a9de91fe00f08542dbd1a3a6ca824c039b8abb/core/jni/AndroidRuntime.cpp). A hardware heap-size setting alone did not change the observed API 36 growth limit.

## Verified current-head API 26 and API 36 matrix

Source `29281389acd27c5addb3609a727ba9a5f2bb305e`, GitHub Actions run [39523935503](https://github.com/santirodriguez/FixMyShot/actions/runs/39523935503), completed SUCCESS on all three jobs. The build job repeated all 23 JVM tests and 18 host regressions, with zero lint errors and 13 unsuppressed warnings. Debug, unsigned release and test APKs were built and passed the shipping-ABI/ELF/ZIP inspection. Verified bootstrap artifact `12311724558` has SHA-256 `097bee43fb0c779db950cf6bafb18095f62be88f906beb3f6a2578133ca3761e`.

Both x86_64 emulator jobs passed all four instrumentation tests and all 80 synthetic PNG/WebP encodes. Each fixture was encoded twice per format, validated on-device for header/chunks, independent reopen and normalized pixel equality; failures/cancellation/insufficient budgets remained explicit. API 26 used Android PNG and source-built LIBWEBP; API 36 used Android PNG and explicit Android WEBP_LOSSLESS. The two device ZIPs passed provider SHA-256 and ZIP CRC checks, with their source SHA matching the build:

| API | Device artifact | ZIP SHA-256 | Measured normal heap | Actual page size | Native output route |
|---|---|---|---:|---:|---|
| 26 | `12311224775` | `7ffef84f8bce7cca7569a0a7ad33a201a4322b4224b3c4333cb7294320044e66` | 512 MiB | 4096 bytes | LIBWEBP |
| 36 | `12312665165` | `4d8f0567716ad894ab107a1a9bcc30affd5d6dd6ba1c01789e8a409697b89b5c` | 512 MiB | 4096 bytes | ANDROID_WEBP_LOSSLESS |

The disposable API 36 emulator initially reported heapgrowthlimit=192m and heapsize=576m; the CI-only checked helper set both to 512m and restarted its runtime before instrumentation. The application still enforces its original half-heap/dynamic memory guard, and this adjusted emulator is **not** evidence that the same full-size case works on a 192 MiB device.

Independently of Android's decoder, Pillow 12.3.0/libwebp 1.6.0 reopened both 1344×2992 fixture-0 exports from **each** API. Every one of their 4,021,248 RGB pixels matched an independently regenerated fixture. All four decoded RGB rasters have SHA-256 `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861`. The two APIs produced different file bytes while preserving the normalized raster:

| API | PNG SHA-256 / bytes | WebP SHA-256 / bytes | Color-tag observation |
|---|---|---|---|
| 26 | `47e4ec5d95fff2aef67d55a913fd9581c4a28284e7ae5ee6b80675a093f9f734` / 92,485 | `1f9ddb0ee982acaa1a68582f782c3f0dfd75b49bd88deef0165b6e8f534a99e2` / 2,084 | PNG has no explicit sRGB/ICC tag; WebP has a VP8L chunk |
| 36 | `e6a0f5fa4860b96417fa1b7aa74d63391bbd41b9fee4e0b852c31d0aa2ee7013` / 92,498 | `04919ebce818eee266bc53d6122245f1ec6f58ba3b11c191a1585a49984eb85c` / 2,354 | PNG has sRGB; WebP has VP8X/ICCP (456-byte ICC payload)/VP8L |

First/repeat fixture-0 encode time (seconds): API 26 PNG 0.115/0.085, WebP 0.802/0.816; API 36 PNG 1.361/0.891, WebP 0.439/0.388. These are emulator fixture observations, **not** process-cold Pixel performance, p95, peak Java/native/PSS memory, universal color-management acceptance, real-device evidence or 16 KiB runtime acceptance. The lack of explicit sRGB tagging on API 26 remains a color-policy follow-up even though synthetic pixels match.

## Open G1 acceptance

The initial slice does not close G1. Standard wrapper/lockfile/verification review and persistence, security/license inventory, complete PNG/WebP performance and native/PSS peaks, mid-encode cancellation/timeouts, physical Pixel and second-family HEIC/AVIF output, bounded JXL build/cost decision, 16 KiB runtime and independent clean-build comparison remain open. A successful synthetic emulator matrix does not replace these gates or the real-image/manual evidence in the [governing blueprint](blueprints/first-functional-release.md).
