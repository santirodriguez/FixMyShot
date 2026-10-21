# Testing and evidence

## Checks in this slice

`core` runs 23 cases for format/default policy, API boundaries, unavailable routes, dimension overflow, WebP limits, memory estimates and metadata-preservation rejection. These are policy tests, not codec execution.

`CodecSmokeTest` contains four instrumentation tests: Activity startup, the synthetic PNG/WebP matrix, pre-cancellation/invalid-input/metadata rejection, and insufficient-budget rejection without a candidate. The image test generates 20 synthetic fixtures, including 1344x2992, odd dimensions, dark/light surfaces, one-pixel colored strokes and binary-alpha edges. It encodes PNG and lossless WebP twice per fixture (80 encodes per API), reads container structure/CRC/VP8L and compares every decoded opaque pixel. Transparent hidden RGB is excluded; partial-alpha tests remain pending.

The schema-2 device report records first/repeat timings and before/after memory snapshots: **neither process-cold performance nor peak memory**. It records the actual normal process heap limit, memory class and page size via `Os.sysconf(OsConstants._SC_PAGE_SIZE)`. The shell `getconf` command is optional because the API 26 system image used here lacks it. The matrix uses a `pixel_2` emulator hardware profile with API 26 and 36 and verifies that its actual heap can admit the largest fixture before allocating it. The profile alone does not raise API 36's normal heap-growth limit. A separate CI-only helper sets both Dalvik heap properties to 512 MiB and restarts the emulator runtime, after checking Actions, an emulator serial, QEMU properties, the expected API and a debuggable image. It refuses physical/unknown devices, leaves API 26 unchanged, records before/after properties and still requires the in-app measured heap. No SELinux policy is disabled. On source `986aee4340aa3be00bb3d0b8e48336b0d36721d8`, the CI-only API 36 helper additionally permits at most three `adb root`/transport reconnection attempts, then requires verified UID 0 before changing any VM properties; unavailable root still fails without a property write. This addresses reproducible cold-boot transport closures on Actions run `39614780466` attempts 1 and 2, where the build and API 26 matrix succeeded but API 36 never reached instrumentation. Ten host helper regression tests cover a transient root error and permanent root rejection. Actual subsequent API 36 runtime acceptance must come from a different completed, source-bound CI/device report. An emulator profile is not physical Pixel acceptance. Production resource limits and the absence of `largeHeap` are unchanged. A separate test exercises the real adapter's rejection with a deliberately insufficient budget.

`check-apk.py` inspects each shipped ELF and APK data offset for 16 KiB alignment and requires arm64-v8a, armeabi-v7a and x86_64 with the JNI library. This is not a 16 KiB runtime test.

`check-device-report.py` rejects incomplete/failed/crashed instrumentation, incorrect test counts, missing or duplicated matrix results, wrong dimensions/routes, invalid measurements and missing synthetic output files. Its ten host-side regression tests also exercise the collector with a missing `getconf` and unusable report text. Eight additional tests cover the CI heap helper's guards, unchanged API 26 behavior, intended API 36 setup and property-change failure. Mocked collector/setup tests are not Android codec evidence.

## Verified API 26/36 native feasibility classification (21c49ca3)

GitHub Actions [39605715333](https://github.com/santirodriguez/FixMyShot/actions/runs/39605715333), source `21c49ca3b362c76a9f183761ae11cc82feff8428`, completed SUCCESS (build, API 26 and API 36). Independently downloaded and checked ZIP SHA-256, CRC, source identity, PNG/WebP pixels and native-report contents:

| Artifact | GitHub ID | ZIP SHA-256 |
|---|---:|---|
| Build | 12347534056 | `5cf719881fd1fe807a74a821a1d26075c3ea63a3520b117de91e94e0da9b7c1f` |
| API 26 | 12347969759 | `a604d0c0b4308dc221e5abe78b6a360d0c41b5f7701f142a1eacac39cd719b96` |
| API 36 | 12348114344 | `f9c21619c346df9276201ac2d7e867a80d2f0e0f37cfa8c74540dce4f2f62692` |

Both x86_64 emulators passed four PNG/WebP instrumentation tests, 80 synthetic PNG/WebP encodes each, and one native classification instrumentation test each. The four fixture-0 (1344x2992) PNG/WebP outputs, decoded independently using Pillow 12.3.0/libwebp 1.6.0, matched all 4,021,248 synthetic RGB pixels. All decoded RGB SHA-256 hashes were `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861`. Measured process heap was 512 MiB and page size 4096 bytes in both emulators; the CI-only heap setup and snapshots are **not** device-memory or 16 KiB runtime acceptance.

Native writer test reports were **PROBE_COMPLETE but neither format was successfully encoded and reopened**:

| Environment | HEIC 65x67 | AVIF 65x67 |
|---|---|---|
| API 26 emulator | `UNAVAILABLE / UNSUPPORTED_API` | `UNAVAILABLE / UNSUPPORTED_API` |
| API 36 emulator | `UNAVAILABLE / INVALID_OUTPUT` | `UNAVAILABLE / INVALID_OUTPUT` |

API 26 therefore demonstrated correct version gating, app startup and unaffected lossless routing. On API 36, the native candidates had plausible container brand and decoded bounds but failed a bounded BitmapFactory pixel decode; they were deleted, with no format substitution. This is evidence about these emulators, **not** proof that HEIC/AVIF cannot work on capable phones or proof of their availability. The next diagnostic tries the distinct API 28+ ImageDecoder pixel path on the same sample while retaining strict rejection if both decoders fail; its CI result must be verified separately. Full-size Pixel 9 Pro XL, a second physical hardware family, odd-size fidelity/color, peak memory, 16 KiB runtime and all remaining G1 requirements remain NOT RUN.

## ImageDecoder alternative: exact-source regression receipt (`fab2c362`)

GitHub Actions [39609785200](https://github.com/santirodriguez/FixMyShot/actions/runs/39609785200), source `fab2c362783ffd8469644b5bade2195837b4d364`, finished with **build SUCCESS, API 26 SUCCESS, API 36 SUCCESS**. Three provider ZIP files were downloaded, CRC and SHA-256 validated, and their source identity checked:

| Artifact | ID | SHA-256 |
|---|---:|---|
| Build | 12350595578 | `1e322e85a3d8ce6ef542d54d4355b04464deea3850f3589794ccbeda449b8128` |
| API 26 | 12349993586 | `4fa4bcc52e92ce48eb87247dd493f8910aa2c379efd848ef249eab04765038fa` |
| API 36 | 12350307997 | `2809c42f74099ac6de280654afaa81431401e2c9e8b8e97ce3282be4d533efa6` |

The source-bound build completed 23 JVM tests with no failures/errors; all three development APKs were built and native ELF/ZIP alignment reports passed. Both x86_64 emulator jobs passed four lossless tests + one native availability/classification test and 80 PNG/WebP encodes each. Independently decoded fixture-0 PNG and WebP bytes at API 26 and 36 still match all 4,021,248 RGB pixels of the 1344x2992 synthetic source, SHA-256 `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861`. Normal app heap was measured as 512 MiB; page size was 4096 bytes in each emulator, not a 16 KiB runtime acceptance.

**Native device truth remains unchanged even with the ImageDecoder alternative:** API 26 HEIC and AVIF are `UNAVAILABLE / UNSUPPORTED_API`; API 36 HEIC and AVIF are both `UNAVAILABLE / INVALID_OUTPUT`. No encoded HEIC or AVIF files were retained. A passing typed classification is not proof of native encoder availability or output fidelity. The next bounded question is whether platform codec behavior changes with even vs odd geometry or on the required physical Pixel 9 Pro XL and a second supported hardware family; those acceptance gates remain NOT RUN.

## Experimental grid, padding and RGB diagnostics (`28aa20b8`)

`NativeGridDiagnosticTest` is an **additional separate one-test instrumentation class**, not an application encoder selector or a new product capability. It compares both HEIC and AVIF under the following exact requested configurations:

| Encoded size | Source-content ROI | Grid request | Interpretation |
|---|---|---|---|
| 64x64 | 64x64 | OFF | Compare with the earlier successful 64x64 default-grid writer |
| 65x67 | 65x67 | OFF | Test whether disabling grid affects odd geometry |
| 66x68 | 65x67 with replicated right/bottom edge pixels | OFF | Distinguish even-aligned padded encoding from actual 65x67 export |
| 66x68 | Same padded 65x67 source | ON | Distinguish requested grid setting from output size |

Eight format/geometry outcomes appear in `g1-grid-diagnostic.json` alongside the original four native probes in `g1-native-probe.json`. Each experimental output must have the expected BMFF brand, actual decoded size, sampled pixel reopening, bounded size/time and typed failure handling; the host validator rejects missing/extra/false-positive files, duplicate cases, silently changed dimensions and unsupported API claims. Successful experimental bytes are named `g1-grid-<format>-<size>-<off|on>.<extension>` and retained only in the private test artifact. Padded output is **66x68** and must never be represented as lossless or faithful 65x67 output. Small image sizes may cause the AndroidX writer to automatically disable real tiling even when grid is requested; these settings record the requested configuration, **not a verified internal grid layout**. In the AndroidX 1.2 implementation, HEIF/AVIF grid tile constants are 512×512; for video-codec-backed small images the writer normally disables requested grid mode and may **force** grid if the selected encoder rejects the original dimensions. Non-grid encoded width can also be aligned to HEVC 32-pixel or AV1 64-pixel blocks. These eight outputs therefore test **requested** configurations and padding, not proof of actual grid activation or of hidden edge pixels. Review the encoded container and observed display dimensions independently before changing production behavior.

For decodable outputs, the Android diagnostic reports RGB PSNR and channel-wise median absolute errors over the **unpadded input ROI**, plus whether the decoded bitmap identifies sRGB. The report explicitly sets `color_profile_inspection=NOT_PERFORMED`: Android pixel reopening does not prove that ICC/nclx tags are present or correct. Any positively retained artifact still requires independent full decode, color-box/profile inspection and user-visible quality checks. This high-frequency synthetic fixture does **not** replace the calibrated full-size screenshot regression target or physical Pixel9ProXL/second-family gates.

Host-only checks: `python3 scripts/test-native-grid.py` (7 cases), existing `scripts/test-native-probe.py` (9), `scripts/test-emulator-heap.py` (10), collector simulation `scripts/test-device-report.py` and `scripts/check-bootstrap.py` passed before commit. Actions [39625536410](https://github.com/santirodriguez/FixMyShot/actions/runs/39625536410) at source `28aa20b8d65a31d97001d9d0e2c6cddd44e9c952` **FAILED during AndroidTest Kotlin compilation, before device testing**, because the instrumentation class imported HeifWriter/AvifWriter without a test compile classpath entry. Source `acbdebac55421652cd5fdaedc0cdaf423d4c5127` adds **only** `androidTestCompileOnly(libs.heifwriter)` to the app; the dependency is already packaged at runtime through `codec-native`. This preserves the app's API 26 minimum and keeps the writer module's `implementation` encapsulation. Actions [39626832584](https://github.com/santirodriguez/FixMyShot/actions/runs/39626832584) on source `acbdebac55421652cd5fdaedc0cdaf423d4c5127` completed **FAILURE overall**, with build and API 36 successful, and API 26 failing only in the new diagnostic's JUnit4 initialization. The API 26 runner reflected synthetic method signatures referring directly to `android.graphics.ImageDecoder` (API 28+), resulting in `NoClassDefFoundError` before the eight-case grid test could run. Its established four PNG/WebP tests, 80 actual lossless encodes, and existing four-case native typed-rejection probe passed. The new test did **not** run on API 26; that failure is not evidence of a PNG/WebP regression.

API 36 did finish the full 8-case grid diagnostic: **HEIC 64x64 grid-request OFF encoded/reopened (3035 bytes)**; **HEIC 66x68 edge-padded from 65x67 encoded/reopened in both grid-request OFF and ON (3438 bytes each, byte-identical)**; HEIC 65x67 and all four AVIF combinations returned `UNAVAILABLE/INVALID_OUTPUT`. Android-reported RGB PSNR on the deliberately high-frequency source ROI was approximately **19.99 dB** at 64x64 and **10.53 dB** on both padded 66x68 cases. This does not meet calibrated full-size fidelity acceptance. Byte-identical on/off results do not independently prove active tiling, and a padded 66x68 candidate must never be called a faithful 65x67 export. Embedded color tags and effective coded-vs-visible dimensions need independent device/profile investigation.

Provider artifacts: build ID `12356843793` SHA-256 `6fb33035d4a67e2fd52fced05fd14e4f5a345329abc4d2b84b518a9bf2c57612`; API 26 ID `12357986745` SHA-256 `d903dd1e38660fa4f1365cb9bfa6707a43561c90148cdfef34b635c539d2e882`; API 36 ID `12357862886` SHA-256 `24e277960c5ad3a92263dfe35a6c45f3c228e20613f496fed566b5677166a837`. Source SHA, ZIP SHA-256/CRC and native report structure were checked. API 26's missing grid report contains a device-side `cat` error, correctly rejected by the fail-closed checker.

The scoped compatibility fix `fa6dd24f00c2b053c0dcbce16f41c7659d9f5590` moves Android API 28+ ImageDecoder references into an `@TargetApi(28)` helper, keeping the JUnit4 test class free of unavailable Android 8 method signatures, and adds one host regression test. Local host checks passed (grid validator 8, native probe validator 9, emulator heap helper 10, bootstrap checks). **Android verification after this fix PASSED** in GitHub Actions [39631701532](https://github.com/santirodriguez/FixMyShot/actions/runs/39631701532), exact executable source `fa6dd24f00c2b053c0dcbce16f41c7659d9f5590`: build SUCCESS, API 26 smoke SUCCESS and API 36 smoke SUCCESS. The source-bound build passed **23/23 JVM tests**, Android lint 0 errors/13 warnings, and its three APKs compiled with native ELF/ZIP alignment PASS (not an actual 16 KiB page-size runtime test). All three downloaded artifacts passed provider ZIP SHA-256 and CRC checks:

| Source-bound Actions artifact | ID | ZIP SHA-256 |
|---|---:|---|
| Build | `12360066348` | `f20d6e57336f3c4f48035685553d1d03b1f544bc5bd6d650f4431b1caabdc967` |
| API 26 | `12359333579` | `6ed07a332a351a99d2852c56ed98a293f8746ec568d224c63cb9d12e1f99711b` |
| API 36 | `12360356322` | `6f79877c9049c53af2d96e35ac5e7d8ce999afacc729c82b587198b9b1da6b7b` |

**API 26 JUnit regression definitively fixed:** the new grid diagnostic actually executed one test and reported `DIAGNOSTIC_COMPLETE` with exactly eight `UNAVAILABLE/UNSUPPORTED_API` combinations; the existing native probe reported four correctly rejected cases. Both API 26 and API 36 executed the established four PNG/WebP tests and **80 real encodes per API**. Independent Pillow 1344×2992 fixture-0 decoded PNG and lossless WebP RGB SHA-256 `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861` on both devices, matching every source pixel. Device page sizes were **4096 bytes** and normal app heap 512 MiB in these disposable x86_64 emulators, not physical Pixel/low-memory/16 KiB runtime acceptance. The exact source archive extracted from the build independently reconstructs Git tree `248c3321db4868e1796e04a945f3d5a110186f90`.

**API 36 native geometry outcome unchanged after the fix:** the eight-case grid report returned HEIC64×64 grid-OFF `ENCODED_AND_REOPENED` (3035 bytes), HEIC66×68 padded OFF and ON `ENCODED_AND_REOPENED` (3438 bytes each, **byte-identical**) and HEIC65×67 plus all four AVIF cases `INVALID_OUTPUT`. Independent `heif-info` on retained HEIC reports 8-bit YCbCr 4:2:0, **no embedded color profile and no metadata**. For padded 66×68, the HEIC container's logical image is 66×68 but independent `heif-convert` yields a **96×68** raster (and FFprobe reports stream96×68, coded96×72); Android reopening reported 66×68. This visible/coded-size and conversion discrepancy remains UNRESOLVED and prohibits claims of general dimensional compatibility. The grid ON/OFF bitstream identity also does not prove actual grid usage.

Color remains an open gate: Android synthetic RGB ROI PSNR was **19.99 dB** for HEIC64×64 and **10.53 dB** for both padded HEIC66×68; an independent libheif decoded ROI comparison against the exact fixture returned **19.99 and 20.08 dB**, respectively, showing a device-vs-host decode discrepancy. None of these tiny deliberately high-frequency fixtures alone passes or replaces the calibrated ≥38 dB full-size G1 target, and none establishes ICC/nclx or correct SDR transfer/range/appearance. Neither native format is made generally available, and a padded 66×68 diagnostic file is **not** a faithful 65×67 export.

## Bounded native geometry outcome (986aee43)

GitHub Actions [39618903599](https://github.com/santirodriguez/FixMyShot/actions/runs/39618903599), source `986aee4340aa3be00bb3d0b8e48336b0d36721d8`, **COMPLETED SUCCESS**: build, API 26 smoke, and API 36 smoke. The preceding source `1c327a1b` already contained the geometry/codec-inventory instrumentation; the only intervening runtime modification retries a transient `adb root` handshake under the CI-only emulator guards. `main` is not involved.

| Source-bound artifact | GitHub artifact ID | ZIP SHA-256 |
|---|---:|---|
| Build | `12354222511` | `4680f67fd9fbfcc17fdf5547ffed6f7aa5116206a517d8032cbc81c6d2806459` |
| API 26 | `12354860377` | `d435b40b0d412425d1d170b27e1d6cb305481806518b556399c803b751433886` |
| API 36 | `12354317953` | `e7fd42a948055a9236a32f57ca83ab7eb81687dee54c2b5fd7939a25f2019cda` |

Both emulator ZIPs passed SHA-256/CRC/source-identity checks. The repository's own independent host checkers accepted the four lossless instrumentation tests, **80 actual PNG/WebP encodes per API**, one native instrumentation test and all four schema-2 native format/geometry records. The four fixture-0 (1344×2992) decoded PNG/WebP rasters still have matching RGB SHA-256 `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861`. Both x86_64 emulators reported normal app heap 512 MiB and page size **4096 bytes**. API 36 heap settings were modified only in its disposable CI emulator; this is neither physical-device nor 16 KiB runtime acceptance.

| Emulator | HEIC 64×64 | AVIF 64×64 | HEIC 65×67 | AVIF 65×67 |
|---|---|---|---|---|
| API 26 | `UNSUPPORTED_API` | `UNSUPPORTED_API` | `UNSUPPORTED_API` | `UNSUPPORTED_API` |
| API 36 | **`ENCODED_AND_REOPENED`** | `INVALID_OUTPUT` | `INVALID_OUTPUT` | `INVALID_OUTPUT` |

API 36 advertised a `c2.android.hevc.encoder` and a `c2.android.av1.encoder` under the respective *video* MIME types, as well as HEVC/AV1 decoders. It advertised no encoders/decoders under the two still-image MIME types queried. The inventory describes component advertisement, not functional still-image support; the actual write/reopen results above are authoritative for these specific configurations. API 26 advertised one HEVC decoder, with no corresponding HEVC or AV1 encoder or AV1 decoder.

**Independent HEIC sample analysis:** the retained `g1-native-64x64.heic` (3035 bytes, SHA-256 `177efa7bba6492d90c35ac60153bad2e4b18255498f35a14f015caede01d59d4`) has a real `heic` ISO BMFF brand. libheif 1.19.8 independently identified and decoded one 64×64 8-bit YCbCr 4:2:0 image, with no alpha, embedded color profile or metadata. FFmpeg 7.1.5 also decoded it. Comparing their decoded RGB output against an independently reconstructed version of the exact `NativeCodecProbeTest` pixel pattern yielded approximately **20.0 dB PSNR** (libheif 19.99 dB; FFmpeg 20.00 dB); the libheif per-channel median absolute error was [2, 1, 1]/255. This deliberately high-frequency 64×64 synthetic pattern is **not** the full calibrated screenshot set and does not establish or replace the required ≥38 dB regression target. It documents a substantial fidelity risk, and the missing explicit color profile also needs investigation. One successful tiny even-dimension HEIC file is **not** accepted HEIC support on arbitrary sizes or devices.

**Still NOT RUN / FAILING G1:** real full-size HEIC and AVIF output on the Pixel 9 Pro XL and another manufacturer, odd-size native rendering, HDR/SDR transfer and sRGB appearance preservation, color-tag and metadata policy, calibrated screenshots/text-edge visual inspection, peak memory, p95 speed, cancellation and 16 KiB runtime. AVIF had no positive output here. Native quality `100` must never be reported as lossless. No format is selected as available globally, silently substituted or exposed through the unfinished UI.

## Native HEIC/AVIF feasibility spike

`NativeCodecProbeTest` runs **one separate instrumentation test** after the established four-test/80-encode PNG/WebP matrix. Its bounded geometry diagnostic covers opaque synthetic **64×64 (even) and 65×67 (odd)** bitmap input for HEIC and AVIF independently: four format/size cases. Schema-2 `g1-native-probe.json` lists an individual typed outcome for each case and a `codec_inventory` snapshot from Android `MediaCodecList.ALL_CODECS`. The inventory lists advertised encoder/decoder component names for `video/hevc`, `video/av01`, `image/vnd.android.heic` and `image/avif`; **an advertised component does not establish a working still-image encoder**. Only actually encoded, reopened and verified files are retained as `g1-native-64x64.heic` / `.avif` or `g1-native-65x67.heic` / `.avif` for independent container and appearance inspection. Host validation requires an actual positive instrumentation result and truthful samples, refusing corrupt, mismatched or missing output. A malformed native file or a file that only exposes bounds/MIME but cannot decode actual pixels yields typed `INVALID_OUTPUT`; the synthetic probe then continues to the other format. The collector filters device-side `cat: ... No such file` text even when `adb` returns status zero; successful output still requires independent BMFF validation. A native probe failure does **not** downgrade to another format or purport to be availability. An emulator failure is evidence of that environment, not device-wide impossibility. The AndroidX 1.2.0-rc01 release candidate contains color-aspect and AVIF fixes absent from observed stable 1.1.0; build success alone is not quality acceptance.


The geometry and codec-inventory diagnostic was introduced on source `1c327a1bc2c75c28de1015f393a0ef84bd5f72fc`. Local host checks passed (9 native-report tests, 10 collector regressions, 8 emulator-heap regressions, bootstrap resource checks); **actual device outcome for this source must be read from its exact CI artifacts, not inferred from test creation**. The selected writers request grid-enabled output, but the library can change actual grid use according to dimensions and encoder capability; the inventory snapshot does not measure tiling or successful output. Neither sample replaces physical-device, color, memory or full-resolution acceptance.

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

## Aligned-width native codec follow-up — exact source \`952307d\`

**Source:** \`952307d31a15850e7549bc52e3a24dae1dd91c80\` on \`release/0.1.0\`. GitHub Actions [39657526056](https://github.com/santirodriguez/FixMyShot/actions/runs/39657526056) completed with **build SUCCESS, API 26 smoke SUCCESS and API 36 smoke SUCCESS**. This is an experimental alignment diagnosis, not an accepted device capability or a change to the production native writer. The executable change adds 96×68 and 128×96 padded controls to the existing 64×64, 65×67 and 66×68 grid experiment (12 format/geometry attempts in total).

All three provider ZIP archives were downloaded and independently verified with SHA-256, ZIP CRC and exact \`source-sha.txt\`:

| Evidence | GitHub artifact ID | ZIP SHA-256 |
|---|---:|---|
| Build | \`12373235060\` | \`f5ba518f5f9affed142e4a7bf904747710f6ba72fd8089d8d3a7727475b800ed\` |
| API 26 | \`12372747068\` | \`21c2c8f47ffa576450bb3a1200b219b8082ae0d361ae4a5d66931b4d2fb365c4\` |
| API 36 | \`12371393881\` | \`ff76d34c07902a545d89a43233c143b421515390da2d82c8fec292fd3ed6a4d2\` |

The exact archived source's host validators accepted both emulator reports; the source's eight host grid regressions and nine native-probe regressions passed independently. The build contains 23 core contract and 14 geometry tests, **0 failures/errors**, and lint reported **0 errors / 13 warnings**. Each emulator passed four established PNG/WebP instrumentation tests and 80 real synthetic encodes. Independently reopening its fixture-0 PNG and WebP with Pillow 12.3.0 yielded the same **1344×2992 raster / 4,021,248 RGB pixels** with SHA-256 \`214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861\` in all four outputs. The measured emulator page size remained 4096 bytes; API 36 uses the documented disposable emulator-only 512 MiB heap adjustment.

**API 26:** every one of the 12 native diagnostic attempts returned \`UNAVAILABLE / UNSUPPORTED_API\` and retained no HEIC/AVIF output. **API 36:** five HEIC attempts encoded and reopened; **all six AVIF attempts** and the unpadded HEIC 65×67 attempt returned \`UNAVAILABLE / INVALID_OUTPUT\`, retaining no file.

| API 36 HEIC diagnostic | Container \`ispe\` dimensions | Android reopened dimensions | Android synthetic ROI PSNR (dB) | Independent libheif 1.19.8 source-ROI PSNR (dB) |
|---|---|---|---:|---:|
| 64×64, grid off | 64×64 | 64×64 | 19.9909 | 19.9947 |
| 66×68, grid off | 66×68 | 66×68 | **10.5327** | 20.0830 |
| 66×68, grid on | 66×68 | 66×68 | **10.5327** | 20.0830 |
| 96×68, grid off | 96×68 | 96×68 | 20.0810 | 20.0830 |
| 128×96, grid off | 128×96 | 128×96 | 20.0850 | 20.0886 |

For padded images, the reference ROI is the same original **65×67** high-frequency synthetic pattern, edge-replicated into the larger input; the 64×64 row uses its own 64×64 reference. **Padded output is not a valid export at the original 65×67 dimensions.** These high-frequency synthetic PSNR values are neither the calibrated full-size ≥38 dB target nor a visual screenshot-quality acceptance.

**Exact binary finding:** \`g1-grid-heic-66x68-off.heic\` (3438 bytes; SHA-256 \`8b42b0f4bbef1ac325e7b626390d246afdfe52969fde119215e3742fbe1665af\`) and \`g1-grid-heic-96x68-off.heic\` (3438 bytes; SHA-256 \`6d728db1ef71ecd0c09766f6f33efcb5a42b61a394c3cce46e02fd8b20e36ac5\`) differ at **exactly one byte, zero-based file offset 295**, changing the \`ispe\` width value \`0x42\` (66) to \`0x60\` (96). Their \`mdat\` image payloads are byte-for-byte identical, SHA-256 \`847ca43594ed63f23384109f9c89d2593c80155a954b0f141e4c5ffe358a70b7\`. The 66×68 grid-on and grid-off files are also byte-identical, not evidence of grid tiling.

Independent \`heif-info\` reports a 66×68 logical image for the 66-width file, but \`ffprobe\` reports a 96×68 HEVC stream (coded 96×72). \`heif-convert\` 1.19.8 decodes **both the 66-width and 96-width files into identical 96×68 PNG rasters**, while the Android decoder reports a 66×68 bitmap and a severely degraded ROI comparison for the 66-width case. The evidence isolates a **container-dimension / underlying coded-width / decoder-interpretation disagreement** on this emulator. It does not establish which specific Android/AndroidX/libheif component is defective or that a differently padded production file would solve the issue.

All positive HEIC files were 8-bit YCbCr 4:2:0 with no embedded ICC/color profile reported by \`heif-info\` and no \`colr\` box in the observed \`meta\` containers. The actual sRGB/transfer/range appearance contract remains unverified. No AVIF sample was recovered. Neither native format should be globally advertised or enabled based on this probe. **G1 remains OPEN** for calibrated 1344×2992 screenshots, exact user-selected dimensions, Pixel 9 Pro XL plus another hardware family, color/metadata, 16 KiB runtime, cancellation, peak resource and p95 measurements.

## Open G1 acceptance

The initial slice does not close G1. Standard wrapper/lockfile/verification review and persistence, security/license inventory, complete PNG/WebP performance and native/PSS peaks, mid-encode cancellation/timeouts, physical Pixel and second-family HEIC/AVIF output, bounded JXL build/cost decision, 16 KiB runtime and independent clean-build comparison remain open. A successful synthetic emulator matrix does not replace these gates or the real-image/manual evidence in the [governing blueprint](blueprints/first-functional-release.md).
