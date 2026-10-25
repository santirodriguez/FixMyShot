# Testing and evidence

Current pending block: canonical Protobuf BSD notice and133component declaration
catalog. Seven exact canonical legal assets are now required. The runtime-license
audit completed133POMs/95hash-matched archives and reviewed the Guava parent and
pinned Protobuf source license. Five new parser/boundary cases and four legal
cases pass locally; the grouped normal build/API36 is requested. Current
acceptedf943 runtime/reproducibility evidence below remains its historical scope.


Current complete bounded checkpoint: **f9434ab9**, Actions40160890302.
Core69/lint/threeAPK, source246-file tree/input audit, actual debug+release
native/legal checks, API36 renderer6/cache3/private20/transaction12/export5/
share4/settings21/batch7/retry8/recovery16,80realPNG/WebPencodes and all four
report guards passed. Optional legacy SAF was skipped/NOT RUN. Emulated page
size4096 is not physical16KiB or general HEIC/AVIF acceptance.
The [exact receipt](feasibility/evidence/android-f9434ab9.json) binds downloaded
build/smoke archive hashes, reconstructed source tree and actual package bytes.
Independent40161516814 recreated the identical unsigned11835411-byte APK in a
clean build; actual downloaded bytes were compared. See its
[comparison receipt](feasibility/evidence/independent-rebuild-f9434ab9.json).

The separate current dependency review inherited149exact proofs and accepted
67newKotlinfiles, retaining all387historical proofs unchanged. OSV488queries
replay completely, with49advisory IDs/18tooling components and no133runtime
matches at scan time. These are bounded evidence, not whole trust/security/
license acceptance. Raw dependency evidence is uploaded and independently verified; see [its package receipt](feasibility/evidence/dependency-evidence-f9434ab9.json).
All earlier entries below are historical checkpoints with their original scopes.

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

## Aligned-width native codec follow-up — exact source `952307d`

**Source:** `952307d31a15850e7549bc52e3a24dae1dd91c80` on `release/0.1.0`. GitHub Actions [39657526056](https://github.com/santirodriguez/FixMyShot/actions/runs/39657526056) completed with **build SUCCESS, API 26 smoke SUCCESS and API 36 smoke SUCCESS**. This is an experimental alignment diagnosis, not an accepted device capability or a change to the production native writer. The executable change adds 96×68 and 128×96 padded controls to the existing 64×64, 65×67 and 66×68 grid experiment (12 format/geometry attempts in total).

All three provider ZIP archives were downloaded and independently verified with SHA-256, ZIP CRC and exact `source-sha.txt`:

| Evidence | GitHub artifact ID | ZIP SHA-256 |
|---|---:|---|
| Build | `12373235060` | `f5ba518f5f9affed142e4a7bf904747710f6ba72fd8089d8d3a7727475b800ed` |
| API 26 | `12372747068` | `21c2c8f47ffa576450bb3a1200b219b8082ae0d361ae4a5d66931b4d2fb365c4` |
| API 36 | `12371393881` | `ff76d34c07902a545d89a43233c143b421515390da2d82c8fec292fd3ed6a4d2` |

The exact archived source's host validators accepted both emulator reports; the source's eight host grid regressions and nine native-probe regressions passed independently. The build contains 23 core contract and 14 geometry tests, **0 failures/errors**, and lint reported **0 errors / 13 warnings**. Each emulator passed four established PNG/WebP instrumentation tests and 80 real synthetic encodes. Independently reopening its fixture-0 PNG and WebP with Pillow 12.3.0 yielded the same **1344×2992 raster / 4,021,248 RGB pixels** with SHA-256 `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861` in all four outputs. The measured emulator page size remained 4096 bytes; API 36 uses the documented disposable emulator-only 512 MiB heap adjustment.

**API 26:** every one of the 12 native diagnostic attempts returned `UNAVAILABLE / UNSUPPORTED_API` and retained no HEIC/AVIF output. **API 36:** five HEIC attempts encoded and reopened; **all six AVIF attempts** and the unpadded HEIC 65×67 attempt returned `UNAVAILABLE / INVALID_OUTPUT`, retaining no file.

| API 36 HEIC diagnostic | Container `ispe` dimensions | Android reopened dimensions | Android synthetic ROI PSNR (dB) | Independent libheif 1.19.8 source-ROI PSNR (dB) |
|---|---|---|---:|---:|
| 64×64, grid off | 64×64 | 64×64 | 19.9909 | 19.9947 |
| 66×68, grid off | 66×68 | 66×68 | **10.5327** | 20.0830 |
| 66×68, grid on | 66×68 | 66×68 | **10.5327** | 20.0830 |
| 96×68, grid off | 96×68 | 96×68 | 20.0810 | 20.0830 |
| 128×96, grid off | 128×96 | 128×96 | 20.0850 | 20.0886 |

For padded images, the reference ROI is the same original **65×67** high-frequency synthetic pattern, edge-replicated into the larger input; the 64×64 row uses its own 64×64 reference. **Padded output is not a valid export at the original 65×67 dimensions.** These high-frequency synthetic PSNR values are neither the calibrated full-size ≥38 dB target nor a visual screenshot-quality acceptance.

**Exact binary finding:** `g1-grid-heic-66x68-off.heic` (3438 bytes; SHA-256 `8b42b0f4bbef1ac325e7b626390d246afdfe52969fde119215e3742fbe1665af`) and `g1-grid-heic-96x68-off.heic` (3438 bytes; SHA-256 `6d728db1ef71ecd0c09766f6f33efcb5a42b61a394c3cce46e02fd8b20e36ac5`) differ at **exactly one byte, zero-based file offset 295**, changing the `ispe` width value `0x42` (66) to `0x60` (96). Their `mdat` image payloads are byte-for-byte identical, SHA-256 `847ca43594ed63f23384109f9c89d2593c80155a954b0f141e4c5ffe358a70b7`. The 66×68 grid-on and grid-off files are also byte-identical, not evidence of grid tiling.

Independent `heif-info` reports a 66×68 logical image for the 66-width file, but `ffprobe` reports a 96×68 HEVC stream (coded 96×72). `heif-convert` 1.19.8 decodes **both the 66-width and 96-width files into identical 96×68 PNG rasters**, while the Android decoder reports a 66×68 bitmap and a severely degraded ROI comparison for the 66-width case. The evidence isolates a **container-dimension / underlying coded-width / decoder-interpretation disagreement** on this emulator. It does not establish which specific Android/AndroidX/libheif component is defective or that a differently padded production file would solve the issue.

**Upstream alignment mechanism (source-review hypothesis, not runtime root-cause proof):** AndroidX `HeifEncoder` defines HEVC's `ENCODING_BLOCK_SIZE` as 32. In a reviewed upstream `EncoderBase` revision, its non-grid HEVC fallback rounds the codec input width to the next multiple of 32 while keeping the requested width separately; `HeifEncoder.HevcEncoderCallback` then advertises the requested width in `MediaFormat.KEY_WIDTH` for the HEIC container. For a requested 66-pixel width, that route calculates **96 encoded pixels**, consistent with the measured 96-wide HEVC payload paired with a 66-wide `ispe` item in this experiment. Sources: [HeifEncoder (read-only upstream revision)](https://android.googlesource.com/platform/frameworks/support/+/1cd0fc33beddf6a2b0d8d0a34b0d5d9c9f39f50d/heifwriter/heifwriter/src/main/java/androidx/heifwriter/HeifEncoder.java), [EncoderBase (read-only upstream revision)](https://android.googlesource.com/platform/frameworks/support/+/f94ca66270e01938c2f7b4bb201776b1d0cbf89c/heifwriter/heifwriter/src/main/java/androidx/heifwriter/EncoderBase.java), and [AndroidX release notes](https://developer.android.com/jetpack/androidx/releases/heifwriter). Independent byte inspection also found **no `clap` clean-aperture box** in either 66×68 or 96×68 diagnostic file. The reviewed upstream source revisions have **not** been independently matched byte-for-byte to the exact 1.2.0-rc01 AAR shipped in this Actions APK, and no encoder fallback-selection or Android decode stride was recorded on-device. Therefore this explains the 32-pixel alignment **mechanism** but does not prove that the Android decoder, AndroidX, codec driver or container writer alone is defective. A focused follow-up should record the actual selected HEIC/HEVC codec, configured/output `MediaFormat` dimensions and Android-decoded row/crop behavior on the affected emulator and required real hardware. Do not patch `ispe`, substitute a padded-size output or enable HEIC/AVIF production routing as an unvalidated workaround.

All positive HEIC files were 8-bit YCbCr 4:2:0 with no embedded ICC/color profile reported by `heif-info` and no `colr` box in the observed `meta` containers. The actual sRGB/transfer/range appearance contract remains unverified. No AVIF sample was recovered. Neither native format should be globally advertised or enabled based on this probe. **G1 remains OPEN** for calibrated 1344×2992 screenshots, exact user-selected dimensions, Pixel 9 Pro XL plus another hardware family, color/metadata, 16 KiB runtime, cancellation, peak resource and p95 measurements.

## Instrumented HEIC codec selection and row geometry — exact source `7dac2661`

**Source:** `7dac26612c1dfc8f16b96374bc4b53712eda61f5`, child of the test-only instrumentation commit `f8f4170212f866fd1dce344f74d2e69d3cb9e7a8`. [GitHub Actions 39669792028](https://github.com/santirodriguez/FixMyShot/actions/runs/39669792028) completed **build SUCCESS, API 26 SUCCESS, API 36 SUCCESS**. The initial instrumentation run [39669631900](https://github.com/santirodriguez/FixMyShot/actions/runs/39669631900) failed *before Android compilation* because a host collector test still fabricated grid report schema 1; `7dac2661` updated only that synthetic fixture and the CI trigger to schema 2. No product/native writer, output geometry, codec selector, permissions, or user-data handling changed.

The three downloaded Actions artifact ZIPs independently passed SHA-256 and ZIP CRC checks, and `source-sha.txt` identified the same exact source. The checked source archive reconstructed Git tree `17b5cb6b16114de0d5f22b7bdc520d67776e7b01`, matching the source commit. These are source-bound experiment receipts, not physical hardware results:

| Artifact | GitHub ID | ZIP SHA-256 |
|---|---:|---|
| Build | `12378808439` | `14fc8bed0deab3b3a43896bd9f9cf1684d85b93ead2f9486bad885f71d09e661` |
| API 26 | `12378703795` | `e020fbc4aa22696598534d4f1c1120674f9936d25dfbe748ecdf23e8899f7402` |
| API 36 | `12378963753` | `2209b451061dff0e9544550daf7af160f0bfcb272a130904c804007d52aa0fd9` |

The build's archived test XML contains **23 core policy and 14 geometry cases, zero failures/errors**. Both API emulators independently passed the report checkers for four lossless instrumentation tests and **80 real PNG/WebP encodes per API**. An independent Pillow reopen of the four retained full-size 1344×2992 fixture-0 PNG/WebP files returned the same normalized RGB SHA-256 `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861`. The checked source's grid validator accepted API 26 **0/12 positive** (all `UNSUPPORTED_API`) and API 36 **5/12 positive** (five HEIC, no AVIF); nine native-probe and ten grid-validator host regressions passed separately. Emulators report **4096-byte pages**; API 36 retains the disposable CI-only 512 MiB heap adjustment. No real Pixel, other hardware, 16 KiB runtime, or calibrated full-resolution HEIC acceptance is implied.

**Actual API 36 writer introspection:** the test reads the `MediaCodec` owned by the constructed AndroidX `HeifWriter` and its configured `MediaFormat` (test-only, best-effort reflection). All six HEIC construction attempts reported `CAPTURED` with codec **`c2.android.hevc.encoder`** and configured/output-polled MIME **`video/hevc`**. A pre-`stop()` `codec.outputFormat` poll returned `AVAILABLE` for each test case. The poll observes the raw codec's format *before* final writer closure, **not** AndroidX's private muxer callback `MediaFormat`; it must not be described as the container's advertised dimensions or as proof of the exact compiled AAR implementation.

| HEIC requested dimensions / grid request | Real configured and pre-stop polled codec dimensions | AndroidX internal effective grid | Android decoded bitmap / rowBytes | Diagnostic status |
|---|---|---|---|---|
| 64×64 / OFF | 64×64 | OFF | 64×64 / 256, `ARGB_8888` | `ENCODED_AND_REOPENED` |
| 65×67 / OFF | 512×512 | ON (forced internally) | none | `INVALID_OUTPUT` |
| 66×68 / OFF | **96×68** | OFF | 66×68 / 264, `ARGB_8888` | `ENCODED_AND_REOPENED` |
| 66×68 / ON | **96×68** | **OFF** (request disabled internally) | 66×68 / 264, `ARGB_8888` | `ENCODED_AND_REOPENED` |
| 96×68 / OFF | 96×68 | OFF | 96×68 / 384, `ARGB_8888` | `ENCODED_AND_REOPENED` |
| 128×96 / OFF | 128×96 | OFF | 128×96 / 512, `ARGB_8888` | `ENCODED_AND_REOPENED` |

This is direct runtime evidence that for the 66×68 cases AndroidX configured the **96×68** HEVC codec path while retaining requested visible width 66 in the produced HEIC `ispe`. The rowBytes are consistent with Android's *final visible bitmaps* at 4 bytes/pixel; they **do not** reveal the decoder's intermediate frame stride/crop or establish where the image was reinterpreted. The forced 512×512 grid route for 65×67 produced no valid output; internal grid selection without an output does not prove a viable odd-dimension tiling path.

**Reconfirmed exact binary and independent decoder control:** API 36 66×68 OFF and 96×68 OFF files are each 3438 bytes, differing at exactly zero-based offset **295** (`ispe` width 66 versus 96); their prior recorded SHA-256 values remained unchanged. `heif-info` identified the 66×68 logical item, whereas `ffprobe` observed an underlying 96×68 HEVC stream (coded 96×72). An independent `heif-convert` decode produced identical 96×68 RGB images for both, normalized RGB SHA-256 **`7a95956725c0aa61090fb189e877ac0b2502a0a9f1312921167095208d24c850`**. Android's 66×68 decoded ROI still measures **10.5327 dB PSNR** versus **20.0810 dB** on the 96×68 control, using the same deliberately high-frequency original 65×67 source ROI. This further localizes the anomaly to container-visible versus coded-width interpretation on this emulator, but does **not** distinguish a framework decoder defect from writer/container or codec incompatibility. The files still lack an independently verified color-profile/transfer contract and are not accepted as screenshot-faithful.

**Next bounded diagnosis:** compare Android `BitmapFactory` and `ImageDecoder` full synthetic pixel positions for the byte-neighbor 66×68 and 96×68 HEIC samples; measure row/column alignment against the common 96×68 decoded control and record container crop/clean-aperture behavior without patching any production HEIC bytes or exporting padded user images. Then run exact-size, color and gallery/share acceptance on Pixel 9 Pro XL plus a second hardware family when available. All native HEIC/AVIF production claims, full-size/color/latency/memory acceptance, JXL integration and independent Gradle publisher trust remain **OPEN**.


## Paired Android HEIC decoder pixel-layout diagnostic — exact source `21251da`

**Source:** `21251dab2c613e6cc84f42a64e339f53da60bd01`, commit on `release/0.1.0`. [GitHub Actions run 39675348923](https://github.com/santirodriguez/FixMyShot/actions/runs/39675348923) completed **SUCCESS** for build, API 26 and API 36 smoke. This is a synthetic test-only diagnostic; the native production encoder, requested output dimensions, metadata policy, format selector, dependencies and permissions are unchanged. The new `NativeHeicPixelLayoutTest` reuses the *already encoded* HEIC 66×68 grid-OFF and 96×68 grid-OFF files from the same runtime fixture set; it does not produce a separate HEIC or modify either file to make them compare.

All three provider ZIPs were independently downloaded and checked for SHA-256, archive CRC and exact `source-sha.txt`. The build's archived `evidence/source.tar` reconstructs Git tree `e17d66f03f0a2fb4a340231b2ba3fb9e9a79390f`, matching the exact commit. The source-bound checker scripts `check-native-grid.py`, `check-native-probe.py` and `check-heic-layout.py` accepted both emulator reports when executed independently from that archived source. The new validator's **9** host regressions, existing grid validator's **10**, and native-probe validator's **9** all passed. The archived JVM XML reports **23 contract + 14 geometry tests, zero failures/errors**; Android lint reported **0 errors / 13 warnings**. Both emulators passed their four established PNG/WebP tests and **80 real synthetic lossless encodes each**. Independently decoded 1344×2992 fixture-0 PNG/WebP outputs on both APIs retain the exact expected normalized RGB SHA-256 `214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861`.

| Exact source-bound Actions artifact | GitHub artifact ID | ZIP SHA-256 |
|---|---:|---|
| Build | `12381736789` | `f06344e6871ebc4a56a5ff751d1bd16298edc923912e5161cfbd5e9fd4274d67` |
| API 26 | `12382451805` | `1bb370084e7c9094cb8926a5a95e5e227dad708c23dce747053c61e57bece764` |
| API 36 | `12382776552` | `54a0e77c202b699e8cc100a86da220bcaf4f9504adf06129e0f4675b245cc53c` |

**API 26:** the paired layout instrumentation actually ran one test and recorded `UNSUPPORTED_API` with no synthetic HEIC pixel comparisons. The separate grid experiment still reports 12 native attempts correctly rejected by API gating; no HEIC/AVIF sample is claimed. **API 36:** its paired layout instrumentation ran one test, reported `DIAGNOSTIC_COMPLETE`, and was independently accepted by the source-bound file/hash/measurement checker. The grid set still contains five small positive HEIC outcomes and zero AVIF successes; native HEIC 65×67 remains `INVALID_OUTPUT`.

**Android pixel-layout results on API 36:** both `BitmapFactory` and the API28+-gated `ImageDecoder` produced **byte-identical normalized RGB rasters** at each visible dimension: 4,488/4,488 exact RGB pixels for the 66×68 sample and 6,528/6,528 for the 96×68 sample. Both routes reported final `ARGB_8888` bitmaps, sRGB interpretation and visible `rowBytes` of **264** and **384**, respectively. Their identical outputs do *not* establish independent implementations: they may share internal Android/MediaCodec behavior, and the reported rowBytes are those of the **final Bitmap**, not the codec's intermediate YUV stride or crop.

| Narrow 66×68 image compared to the 96×68 control, same decoder | Result |
|---|---:|
| Same origin (columns 0–65) RGB PSNR | **10.7152 dB** |
| Exact RGB pixels at same origin | **1,449 / 4,488** |
| Best horizontal-crop offset over columns 0–30 | **10 pixels** |
| PSNR of that best horizontal crop | **12.2101 dB** |
| PSNR under simple tightly packed 66-wide row reinterpretation | **7.9263 dB** |

Both Android decoder routes produced the same metrics. The first 10 rows' mean absolute RGB discrepancy was zero (consistent with the known flat synthetic fixture), while disagreement sharply increased from the first patterned rows; these observations do not identify an intermediate decoder stride. Neither a simple horizontal crop nor direct RGB row repacking adequately accounts for the mismatch. The result rules out **switching between these two Android API entry points as a fix in this emulator**, not all alternative software decoder implementations.

**Independent binary/reference control:** both original HEIC files remain 3,438 bytes and differ at **only zero-based offset 295** (`ispe` visible width 66 versus 96). Their retained SHA-256 values are `8b42b0f4bbef1ac325e7b626390d246afdfe52969fde119215e3742fbe1665af` (66) and `6d728db1ef71ecd0c09766f6f33efcb5a42b61a394c3cce46e02fd8b20e36ac5` (96). `heif-info` identifies corresponding logical dimensions and reports 8-bit YCbCr 4:2:0, no color profile, no alpha and no metadata. On the independent libheif decoder, `heif-convert` produced the **same 96×68 RGB raster for both files**, SHA-256 `7a95956725c0aa61090fb189e877ac0b2502a0a9f1312921167095208d24c850`. The prior FFprobe observation of 96×68 stream / 96×72 coded geometry still applies. This narrows the anomaly to the interaction among HEIC container-visible width, padded HEVC coded width and Android's decode path; the exact defective component, applicable HEIF clean-aperture/crop semantics and hardware portability remain **unresolved**.

**Acceptance boundary and next step:** quality 100 is lossy; synthetic 10–20 dB comparisons here are not the calibrated ≥38 dB screenshot acceptance set. No 1344×2992 HEIC image, independent actual Pixel 9 Pro XL/second-family result, gallery/share result, fidelity/color-profile/transfer/range acceptance, actual decoder YUV-plane stride, native PSS/latency/cancellation measurement or AVIF/JXL acceptance is established. Do **not** patch `ispe`, automatically pad original dimensions, replace the user's format, or enable native formats from this evidence. A future bounded follow-up may instrument the HEVC decoder's output `MediaFormat` crop keys and YUV plane row/pixel strides on this synthetic pair, or prioritize the specified physical-hardware gate; preserve exact user-selected visible dimensions and the documented color/metadata contracts.

## Batch thumbnails and private PNG/WebP preflight — source-bound CI receipt

The initial feature source `817a26e5a9e5ba4abc97c0f1a969e903da13b271` added a bounded (256 px / 24 MiB) original-image thumbnail LRU, lazily composed batch strip, streamed SHA-256 fingerprints for staged image copies and immutable per-item private-output preflight for **PNG / lossless WebP contain plans only**. The preflight preserves user format choices and typed blockers for missing fingerprints, unsupported crop/metadata paths, destination conflicts and conservative memory ceilings. Eight new pure Kotlin preflight cases and three synthetic Android cache tests cover the corresponding contracts. **No full-resolution production render, encoded candidate, MediaStore/SAF publication, share-result flow or manually accepted device/UI result is established by this preflight.** Original file hashes must be rechecked and automatic backgrounds frozen by the future output executor.

Two exact-source integration failures were identified and repaired without speculative product changes:

- [Actions 39691682708](https://github.com/santirodriguez/FixMyShot/actions/runs/39691682708) failed in the pre-Android collector regression because its mock returned four tests for the new three-test thumbnail class. Source `043bc371dc943c7141bc1fc83a585e71c64b5edf` changed only the fixture and CI trigger.
- [Actions 39691864840](https://github.com/santirodriguez/FixMyShot/actions/runs/39691864840) then passed initial host checks, but failed `:app:compileDebugKotlin` on a shadowed Compose `selected` semantics property at `ReviewScreen.kt:106`. Source `b6d9440aabe1e34a4a82fbbd31a7852b4ace4272` qualified the receiver (`this.selected`) and updated only the CI marker. Intermediate source `10526887947c5978fa278f3b0cd5f586697b5b32` changed only the three privacy/build/format documentation owners to describe the already present private Photo Picker path and its retention limitations.

**Exact executable validation:** [Actions 39692377099](https://github.com/santirodriguez/FixMyShot/actions/runs/39692377099), source **`b6d9440aabe1e34a4a82fbbd31a7852b4ace4272`**, finished **SUCCESS** for build, API 26 and API 36. The build ran `scripts/check-bootstrap.py` (52 locale keys in en/es/ca), existing Python host regressions, `:core:test` including the new preflight test class, Android lint, debug/release builds and AndroidTest compilation. Each emulator ran the three synthetic `ReviewThumbnailCacheTest` cases with **`OK (3 tests)`**, the pre-existing four `ReviewRenderTest` cases, and the 80 real synthetic PNG/WebP encodes per API. Existing native HEIC/AVIF probes remain strictly diagnostic and are **not** evidence of globally enabled formats. GitHub produced source-bound artifact IDs `12389381668` (build), `12390460052` (API26), and `12390340369` (API36). Their job logs and result association were checked; this receipt does not claim independent artifact-byte comparison or physical-device acceptance.

The new thumbnail eviction tests show cache ownership, size and non-recycling behavior on synthetic emulator bitmaps. Neither those checks nor `ExportPreparation.inspect` exercise a complete real-image import/gallery export, source fingerprint revalidation, automatic-color full-resolution render, file metadata sanitizer, interruption/recovery transaction or manual device UX. Those remain **NOT RUN** or **OPEN** under G2–G5. User-requested manual physical-device checks are postponed, not waived.


## Source-bound AndroidX EXIF and SAF adapter smoke — 2026-10-22

[GitHub Actions 39710667723](https://github.com/santirodriguez/FixMyShot/actions/runs/39710667723) executed **`059f116ba411f905cb3fc20e5bd8586e12b767f0`** and completed **SUCCESS**: Gradle build, core tests, lint and Android test APK succeeded, plus independent smoke jobs for API **26** and **36**. The build checked the 52 resource keys in en/es/ca and ran `:core:test`. The two emulator job logs explicitly include:

| Executed suite | Android 8 / API 26 | API 36 |
|---|---|---|
| `PrivateOutputExecutorTest` (real private candidate + EXIF-6 JPEG) | `OK (6 tests)` | `OK (6 tests)` |
| `TransactionalOutputSaverTest` (fake provider faults and API29+ conditional path) | `OK (9 tests)` | `OK (9 tests)` |
| `ReviewThumbnailCacheTest` | `OK (3 tests)` | `OK (3 tests)` |
| `ReviewRenderTest` | `OK (4 tests)` | `OK (4 tests)` |
| Actual PNG / lossless WebP encoder matrix | 80 synthetic encodes PASS | 80 synthetic encodes PASS |

The previously failing JPEG EXIF-6 fixture passed on API 26 after switching the app's import/decoder and instrumentation to pinned official `androidx.exifinterface:exifinterface:1.4.2`. The API29+ fake/real-output seam, including the SAF visibility journal state and non-tree rejection, passed on its defined instrumentation routes. API26 did **not** publish a MediaStore item or exercise an actual chosen SAF provider tree; API36's real MediaStore test owns and deletes only its synthetic item. The existing native HEIC/AVIF diagnostic attempts are **not acceptance** or a reason to expose those formats. Both emulators report 4 KiB pages, not the required 16 KiB native-runtime test.

This receipt does **not** establish a real batch export UI, persistent batch resume, 100-item mixed-result operation, physical Pixel 9 Pro XL/other-vendor output, color/HDR/metadata opt-out or manual gallery/share acceptance. The public Export action remains disabled pending G2–G5. Source-bound CI is the only claim here; hardware and manual acceptance stay **NOT RUN**.

## Open G1 acceptance

The initial slice does not close G1. Standard wrapper/lockfile/verification review and persistence, security/license inventory, complete PNG/WebP performance and native/PSS peaks, mid-encode cancellation/timeouts, physical Pixel and second-family HEIC/AVIF output, bounded JXL build/cost decision, 16 KiB runtime and independent clean-build comparison remain open. A successful synthetic emulator matrix does not replace these gates or the real-image/manual evidence in the [governing blueprint](blueprints/first-functional-release.md).


## Foreground batch export and persisted recovery — exact-source CI `e1a30a6`

[GitHub Actions run 39798085167](https://github.com/santirodriguez/FixMyShot/actions/runs/39798085167) completed **SUCCESS** for all three jobs at exact executable source **`e1a30a6a42fde2804c543dc5ad3557185d4455f1`**: Android build/lint and x86_64 emulator smoke on API 26 and API 36. The job logs were checked for actual instrumented suite counts, not only the green overall status:

| Test class / path | API 26 | API 36 |
|---|---:|---:|
| `ReviewExportFlowTest` (synthetic staged PNG → verified destination and UI) | 3 pass | 3 pass |
| `BatchOutputCoordinatorTest` (serialized mixed outcomes and duplicate prevention) | 5 pass | 5 pass |
| `BatchRecoveryScannerTest`, `BatchRecoveryReconciliationTest`, `ReviewRecoveryUiTest` (historical receipts and review barrier) | 4 pass | 4 pass |
| `TransactionalOutputSaverTest` (fault-injected item commit boundary) | 9 pass | 9 pass |
| `PrivateOutputExecutorTest` (private render and candidate verification) | 6 pass | 6 pass |
| `ReviewRenderTest` / `ReviewThumbnailCacheTest` | 4 / 3 pass | 4 / 3 pass |
| PNG / lossless WebP synthetic fixture matrix | 80 encodes pass | 80 encodes pass |

The source-bound host/bootstrap check found **75 matching localization keys** in English, Argentine Spanish and Catalan; the API 26 and API 36 logs reported **4096-byte pages**. Native HEIC/AVIF instrumentation remains diagnostic, **not production acceptance**. No independent artifact-ZIP SHA-256/CRC verification or manual physical-device tests are claimed by this new receipt.

**Exact behavior tested:** `e1a30a6` discovers prior app-private batch receipts on the next start, displays them even when the draft was lost, compares batch and per-item journals, blocks a new export until the user explicitly acknowledges each old job, and does not perform automatic retry, publication or deletion. The success of these synthetic tests does **not** verify that a recorded output still exists in a gallery/SAF provider, that its digest still matches, or that a publication marked indeterminate actually completed.

**Remaining G4 work:** bounded read-only provider verification after restart, explicit handling of provider uncertainty, selected-output sharing, actual API 26/28 user-selected SAF folder/provider tests and permission recovery, kill/restart/provider-denial/low-space cases, and the full 100-image mixed-result exercise. Pixel 9 Pro XL plus second-hardware-family and other manual acceptance remain NOT RUN. Do not describe G4 as completed from this checkpoint.


## Output recovery verification regression — exact source `e794a5e`

[GitHub Actions 39846364699](https://github.com/santirodriguez/FixMyShot/actions/runs/39846364699), exact source **`e794a5e3d94ab48b7f1872f6dd80c46d8954745f`**, completed **SUCCESS** for build, Android API 26 smoke and API 36 smoke. The actual emulator job logs show **`OK (8 tests)` on both APIs** for the combined recovery test classes (`BatchRecoveryScannerTest`, `BatchRecoveryReconciliationTest`, `ReviewRecoveryUiTest`, `OutputRecoveryVerifierTest`), plus **`ReviewExportFlowTest OK (3)`**, **`BatchOutputCoordinatorTest OK (5)`**, **`TransactionalOutputSaverTest OK (9)`**, **`PrivateOutputExecutorTest OK (6)`**, **`ReviewRenderTest OK (4)`**, **`ReviewThumbnailCacheTest OK (3)`** and 80 actual synthetic PNG/lossless WebP encodes on each API. Both emulators reported 4096-byte pages.

The prior source `4f0a0b5626c8c139509d402e31aaf4fa8f2eba44` [Actions 39838521430](https://github.com/santirodriguez/FixMyShot/actions/runs/39838521430) compiled but **FAILED** both emulator jobs: 7/8 recovery tests passed and `uncertainPublicationIsNeverPromotedByMatchingBytes` failed because the batch snapshot lacked the URI that already existed in the per-item journal. The one-line fix in `e794a5e` allows read-only verification through the journal URI when the batch URI is absent, while retaining mismatched-known-URI blocking and never automatically promoting an indeterminate publication to success.

The new bounded read-only `OutputRecoveryVerifier` distinguishes matching content, missing/changed bytes, pending MediaStore entries and unverifiable provider results. API 36's `OutputRecoveryVerifierTest` uses an app-owned synthetic MediaStore image and removes it after testing. API 26 executes its non-MediaStore cases; its MediaStore-specific test exits via API gating. This verifies **no automatic retry/deletion in the recovery path**, not authentic chosen SAF tree selection, gallery listing, receiver acceptance, manual device behavior, complete process-death safety or G4 closure. No independent artifact ZIP byte validation is claimed for this receipt.

## Selected-output sharing — exact-source CI `3df5262`

[GitHub Actions 39853261424](https://github.com/santirodriguez/FixMyShot/actions/runs/39853261424) completed **SUCCESS** at executable source **`3df5262ec8676b42ecf5ec35a39b521bb744bd54`** on `release/0.1.0`: build, API 26 and API 36. The build log records **86 matching resource keys** in en/es/ca, successful core tests/lint, debug/unsigned-release/test APK compilation and native APK inspection. Actual emulator logs contain:

| Suite / evidence | API 26 | API 36 |
|---|---|---|
| `ExportShareIntentsTest` | `OK (4 tests)` | `OK (4 tests)` |
| `ReviewExportFlowTest` | `OK (3 tests)`; real MediaStore case API-gated out | `OK (3 tests)`; includes owned MediaStore output and verified share payload |
| Combined recovery classes | `OK (8 tests)`; MediaStore-specific body API-gated out | `OK (8 tests)`; includes owned provider verification |
| Batch coordinator / transaction / private executor | 5 / 9 / 6 pass | 5 / 9 / 6 pass |
| Renderer / thumbnail cache | 4 / 3 pass | 4 / 3 pass |
| PNG / lossless WebP matrix | 80 real synthetic encodes pass | 80 real synthetic encodes pass |

The share builder checks single/multiple content-URI payloads, MIME, ClipData and temporary read grants. API 36's export test selects an actually published synthetic MediaStore output, rechecks its journal/provider bytes through the ViewModel and obtains ACTION_SEND, then deletes its own test image. This exercises the fix for per-item transaction tokens being distinct from batch tokens. Neither test launches a receiving application or proves that an external receiver can open the grant. API 26's green suite does **not** prove a genuine selected SAF folder or persisted grant.

Artifact association and provider-reported digests were inspected; these digests were **not independently verified against downloaded ZIP bytes** in this receipt:

| Artifact | GitHub ID | Provider-reported ZIP SHA-256 |
|---|---:|---|
| Build | 12458757171 | `b011a6ae8a02386a5d3b280189ada831974f80232a51a45e58e6682d99272b54` |
| API 26 | 12459122225 | `4f687d55c652020ba125ae36709641b4cc1fd96ee97ce5d852ad66023359e0e1` |
| API 36 | 12459152743 | `4dcd4df4871838c68041ca20874ded849063ee8dce3e15d24b20092dba8dbc3e` |

Both emulators report 4096-byte pages. The superseded run 39852723231 was cancelled and is not acceptance evidence. Physical devices, external receivers, 16 KiB runtime and G1–G7 remain open. Later source changes require their own relevant validation.

### Next SAF acceptance slice — API 26 and 28

**Status: NOT RUN.** Use disposable authorized emulators and synthetic files; record exact app SHA, OS image, provider identity and resulting receipts. Physical-device review remains separate.

1. On each API, create a uniquely named empty test folder using the system file manager. Import one synthetic PNG and one JPEG orientation fixture; explicitly choose PNG and then lossless WebP for separate exports. From Export, use the actual system folder picker to choose the test folder. Do not inject a URI, bypass grants or substitute an in-memory output port for this acceptance.
2. Verify that both read and write persistable grants are present, outputs appear only in the chosen folder with truthful MIME/dimensions, source hashes stay unchanged, and results acknowledge that SAF files can be visible during writing. A selected folder is an alternative location, not automatic Pictures/FixMyShot.
3. Cancel the picker, then test a read-only/refusing provider. No batch output may start, the draft must survive, and the error must be localized. Reject unexpected/out-of-tree provider URIs without reading, writing, publishing or deleting them.
4. After one successful batch, force-stop and reopen the app. Verify the persisted grant and read-only reconciliation of recorded outputs. Confirm that matching published bytes do not trigger duplicate exports, and uncertain results require explicit review. Revoke access or remove the test folder externally, reopen, and verify a useful unavailable/unverifiable result without automatic retry/deletion.
5. Select one and multiple successful outputs, open Sharesheet and use an external test receiver to read the actual streams with temporary grants. Check PNG/WebP and mixed MIME, deselection, modified/missing output rejection and cancellation. Intent construction alone does not pass receiver acceptance.
6. Remove only outputs/folders created by this exercise and release only its grants. Keep the 100-item, low-space, mid-write process-kill and physical-gallery matrix open until separately executed.

The blueprint's API 26–28 permission-mediated default Pictures path remains **unimplemented**; the current alpha uses only explicit SAF selection there. This acceptance plan does not waive that requirement or authorize a new permission. G4 cannot close from the current SAF-only path.

### SAF unexpected-URI regression following `3df5262`

Source review found that `SafTreeOutputPort.create` attempted cleanup even when a provider returned a URI outside the selected tree. The adapter now rejects that URI without attempting cleanup; same-tree name-mismatch cleanup remains bounded to the selected tree. Two constructor callbacks preserve the real DocumentsContract defaults and allow fault injection of create/delete results. A new transaction-suite case checks different-tree, different-authority and non-tree returns, rejects read/write/publication/deletion and asserts zero cleanup calls. It creates no external files and does not simulate a genuine persisted grant.

The transaction suite now has **10** cases; the collector and host mock require that count. **Android execution PASSED** in [Actions 39855672761](https://github.com/santirodriguez/FixMyShot/actions/runs/39855672761), exact source `04860d740dca058d3a008bf82bdf21e99a69aaa3`: build, API 26 and API 36 succeeded. Both emulator logs explicitly name `unexpectedSafCreatedUriIsRejectedWithoutTouchingIt` and report `TransactionalOutputSaverTest OK (10 tests)`, plus four share-intent, three export-flow, eight recovery, five batch, six private-executor, four renderer and three cache cases, and 80 real synthetic PNG/WebP encodes per API. The build checked 86 locale keys and compiled/linted the APKs. Both devices had 4096-byte pages. This is the fault-injected URI boundary regression, not genuine folder/grant or receiving-app acceptance; no independent artifact ZIP-byte verification is claimed. Actual API 26/28 picker/provider tests above remain NOT RUN.


### Dedicated real SAF-provider exercise — initial cancellation harness failure

The opt-in Android checkpoint now also builds/runs `SafProviderFlowTest` in dedicated API 26/28 emulators. The first instrumentation phase prepares private synthetic PNG plus the fixed EXIF-6 JPEG, prepares the real ViewModel export, and launches MainActivity's registered folder picker with an initial-location hint through a test-only reflection seam. The returned URI and permission are **not injected or mocked**: UiAutomation confirms the actual system DocumentsUI Select action, then the existing Activity callback persists/checks the real read/write grant and runs the production encoder/SAF adapter. The test also cancels a picker before publication.

This is not complete click-by-click import/review/Export-button acceptance: the draft is staged programmatically and the private launcher seam avoids unrelated Compose scrolling. Two two-item batches produce four actual PNG/WebP documents and check MIME, decoded 64x64 / 60x60 geometry and unchanged source hashes. The shell force-stops the app between phases. The second phase requires a different PID, reads the real persisted grant/documents, checks recovery does not duplicate outputs, releases the test grant and requires subsequent reads of the still-existing files to fail with SecurityException.

Only the shell creates/removes its isolated synthetic directory on a guarded disposable emulator. No production permission, manifest, dependency or application source is changed. Logs plus a source-bound JSON receipt must pass `check-saf-provider.py`; seven host rejection checks do not execute Android or prove provider acceptance. The initial [Actions 39858979290](https://github.com/santirodriguez/FixMyShot/actions/runs/39858979290), source `bed728d11ef1273945468762e479faf163beec15`, passed build and API26/API36 regressions but **FAILED both SAF jobs**. Each reached the actual folder picker, then failed during cancellation at `Instrumentation.sendKeyDownUpSync` with `SecurityException: Injecting to another application requires INJECT_EVENTS permission`. Publication and the second process were not reached; no real SAF export/grant acceptance is established. The test-only correction uses `UiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)` and asserts successful dispatch, preserving the cancellation and all downstream grant/restart/revocation requirements. It adds no app permission. The corrected source's API28 exercise passed in Actions39860824433; its API26 export failed later with zero published items. See the source-bound receipt below; the two-API exercise is not accepted. External receivers, refusing/cloud providers, low-space, mid-write kill, 100-item batches, physical devices, legacy default Pictures saving and G4 remain open.

Implementation references for the bounded picker automation: AOSP Android 8/9 `picker/PickFragment.java` and `res/layout/fragment_pick.xml` use `android:id/button1` for Select; `picker/ActionHandler.java` handles EXTRA_INITIAL_URI as a location hint. Those source observations are not a runtime pass.

## Real SAF provider — API 28 accepted slice, API 26 unresolved (`9aa67f1`)

[Actions 39860824433](https://github.com/santirodriguez/FixMyShot/actions/runs/39860824433), exact source **`9aa67f10e1c9f86f822f887b08fd26a77205bac2`**, passed build, ordinary API26/API36 regressions and **saf-provider (28)**. **saf-provider (26) failed**, so the overall matrix is not accepted.

**API28 evidence:** both separate instrumentation invocations report `OK (1 test)`; the source-bound checker reports `PASS_REAL_SAF_EMULATOR`. The original artifact ZIP (ID **12462421603**, SHA-256 **`e7a25aa7fd0145228409e756f16bc1be33ebf5d15a368477c68c5b08a5f69024`**) was independently downloaded and checked for SHA-256 and ZIP CRC. Its source SHA matched, and the exact source's checker (Git blob `8f3e7aa81cf81d6f9117a6185abaa7f8edc80b66`) independently accepted both phase logs and final JSON.

Environment: Android9 x86_64 emulator, `google/sdk_gphone_x86_64/generic_x86_64:9/PSR1.180720.122/6736742:userdebug/dev-keys`, actual `com.android.externalstorage.documents`. Recorded process IDs **3277 -> 4188** differ after the shell force-stop. Actual picker cancellation, persisted read/write grants, four document receipts (PNG and lossless WebP, each 64x64 and 60x60), unchanged private source hashes, duplicate-free completed-job restart, and read denial after releasing the grant all passed the scoped assertions. The ZIP contains receipts/logs, **not the four exported image files**: output hashes/dimensions were measured on Android; independent off-device pixel reopening is not claimed.

**API26 failure:** cancellation no longer throws INJECT_EVENTS. The first instrumentation phase later fails at the assertion requiring two published outputs: `expected:<2> but was:<0>` at test line206. The UI phase had reached COMPLETED, which does not imply item success. The original log lacks per-item reasons and the second-process phase did not run; the cause of the rejected publications is not established. Do not infer a provider-sync, permission or codec defect from that count alone.

The next test-only checkpoint preserves all success assertions and adds failure diagnostics: item phase/reason plus a tiny real-provider probe inside the same explicitly selected synthetic folder, tracing create/open/write/flush/sync/readback and deleting only that probe. Sync errors are recorded separately so subsequent diagnostic readback can be compared, **without changing the production saver or treating that diagnostic as successful export**. The guarded CI runner retains the synthetic diagnostic JSON and bounded provider error logs. Its Bash syntax check passed; new Android execution is pending.

This receipt covers the programmatically staged draft and actual Activity picker callback, not full click-by-click import/review or an external receiving app. API26, refusing/cloud providers, mid-write process death, low-space, 100-image results, physical devices, legacy default Pictures saving and complete G4 remain open.



## Import, appearance and help block — implementation checkpoint

The next coherent functional block receives ACTION_SEND/ACTION_SEND_MULTIPLE and ClipData through the same bounded private source stager, preserves import work across Activity recreation, defers incoming additions while a frozen export is locked, and explicitly rejects oversized selections without silently dropping images. It adds theme persistence, the AppCompat/platform language bridge, a packaged locale config and original flag vectors, offline localized help/privacy and About/version links. New-session PNG/metadata-removal defaults remain unchanged.

`ImportSettingsFlowTest` covers merged/deduplicated stream and ClipData offers, rejection of file paths/malformed/oversized inputs, theme persistence through a new preferences reader, and a real owned MediaStore PNG received by MainActivity, retained across recreation and Spanish/Catalan/system changes without replacing its draft. The runner requires the whole four-case block, alongside existing regression suites. The synthetic MediaStore fixture and its private staged copy are removed by the test; this is not independent external-sender/receiver acceptance.

Android execution is **PENDING for this new source**. Resource/catalog checks, source review and shell syntax are local checks only. Physical-device behavior, visual large-text/TalkBack review, process restart locale storage, external grant delivery and full G3 remain open.

The previous diagnostic [Actions39862769432](https://github.com/santirodriguez/FixMyShot/actions/runs/39862769432), source553042a, completed: build and ordinary smoke26/36 SUCCESS; real SAF26/28 FAILURE. API26 diagnostics identify WEBP_LOSSLESS, both items FAILED/INVALID_CANDIDATE; its actual provider probe completed CREATE/OPEN_WRITE/WRITE/FLUSH/SYNC/CLOSE/READBACK, bytes matched, probe_deleted=true. API28 failed launching an unregistered ActivityResultLauncher in its test harness. No full SAF acceptance is claimed for553042a. The earlier independently verified API28 receipt remains above.

The [compatibility decision](decisions/compatibility-and-validation.md) qualifies older mandatory API26/legacy-Pictures language in historical plans above. Normal checkpoints now use API36; `profile=extended` explicitly requests the legacy matrix. Legacy skipped means NOT RUN. The known API26 WebP failure is deferred; current-device acceptance and output validation are not weakened.


## Preserve-pixels crop block — implementation checkpoint

Crop now has an explicit contain/crop choice for batch or selected image, per-image normalized focus, drag/pinch, zoom slider, directional controls and reset. Ratio changes preserve focus/zoom; a clamped framing retains its review flag until the user explicitly accepts the repositioned crop or edits/resets it. Crop too small for the selected destination remains a conflict; no silent enlargement or contain fallback is performed. Exact-size crop remains blocked pending its disclosure/enlargement policy.

PrivateOutputRecipe freezes the CropPlanner source rectangle and its native integer output dimensions. ReviewBitmapRenderer.drawContent is the common mapping for sampled preview and full-original export, with unfiltered integer 1:1 copies and filtered actual sampling changes. Metadata/source fingerprint checks and transactional output boundaries remain in place. The destination validator now also enforces each preset minimum short side for crop, rather than allowing an undersized crop to bypass it.

The core preflight test now requires the exact normalized crop rectangle/output recipe. The four-case renderer suite replaces its former crop-rejection case with actual centered crop pixels. PrivateOutputExecutorTest grows from6 to7 with original staged PNG -> cropped candidate -> strict decode; every aligned crop pixel must equal both its original source rectangle and the common preview, and the original fingerprint must still match. The collector and its mock count are updated together. These are synthetic crop checks; full gesture/TalkBack/large-text, fractional/zoomed visual quality, all EXIF orientations and physical-device acceptance remain open. Current source Android execution is **PENDING**.


## Current-device functional acceptance — source27255a6

[Actions39868462277](https://github.com/santirodriguez/FixMyShot/actions/runs/39868462277), exact source **27255a6a1f8d7970570541d80655bdd79d29fdef**, completed SUCCESS for build and API36 smoke. The optional legacy SAF job was SKIPPED under profile=current; this is NOT RUN, not legacy acceptance. Actual emulator logs explicitly show renderer4, thumbnail3, private executor7 (including `croppedPngMatchesTheSharedPreviewAndOriginalPixels`), transaction10, export flow3, share intents4, import/settings4, batch5 and combined recovery8 cases, plus80 real synthetic PNG/lossless WebP encodes. The source-bound checker reported4096-byte pages. Native HEIC/AVIF probes remain diagnostics, not accepted output formats. No independent artifact ZIP-byte verification or physical-device/receiver/16KiB acceptance is claimed by this receipt.

The earlier source73e7269 / Actions39867251032 passed build and all older suites but failed the new locale case while it expected `es` and still observed `en` immediately after requesting the asynchronous configuration change. The same test/source behavior passed in27255a6 without a locale-production change, indicating an intermittent configuration-observation timing problem. The next harness correction waits up to10 seconds for the actual requested Activity resource locale, including return-to-system, before asserting unchanged ViewModel/draft and translated resources. It retains the original success assertions and fails if the condition never arrives; it is not a blind sleep or a locale fallback.

## Shared artwork and locale-observation checkpoint

The supplied five PNGs are moved byte-for-byte to their canonical paths in docs/ARTWORK.md; downloaded bytes matched original Git blob IDs and recorded SHA-256. Each image was decoded and visually reviewed at its actual dimensions. The Android adaptive icon packages a generated copy of the canonical512px image, with a separate monochrome vector on API33+. README reuses the canonical header. No screenshots or store-ready feature graphic are fabricated. Resource/XML checks and the host collector regression suite are local checks; clean Android generated-resource packaging and the bounded locale-observation correction require the next source-bound CI. Real launcher-mask/contrast review remains manual.



## Export retry and frozen-session recovery — implementation checkpoint

New exports persist their complete original destination, batch settings and per-item inheritance/overrides, geometry/crop/size/metadata fields, revisions and staged-source fingerprints before publication. A bounded versioned private journal excludes image bytes and undo history. On restart the UI offers explicit restoration only into an empty draft; it does not substitute the current settings or auto-export anything. Jobs created before the frozen journal was introduced stay review-only.

The retry action preserves all item positions and already saved receipts. It targets only the explicitly offered failed/cancelled/unstarted set with no remaining output URI and no uncertain transaction. Eligibility is checked again under a process-level OS file lock. A durable QUEUED checkpoint precedes clearing a proven terminal/no-output receipt. Each new save receives its own fresh transaction/name. Reserved/visible/validated/published/indeterminate, corrupt and mismatched receipts are never cleared for retry. Ordinary start still returns Existing and never retries implicitly. A stale lock-file inode is safe after process death; a live OS lock blocks another writer. Provider bytes are rechecked read-only for prior published items; uncertain publication is never promoted by matching bytes.

Seven new BatchRetryFlowTest cases cover mixed-result targeted retry without rewriting successful receipts, restored frozen plans with interrupted/unstarted items, continuing untouched items around an uncertain output, live/stale OS locks, full settings/override round-trip, changed-plan rejection and oversized/unknown journal rejection. ReviewExportFlowTest grows from3 to4 with a real API29+ MediaStore refusal, new ViewModel discovery, explicit draft restore, destination cancellation retaining results, and retry of only the failed item: two successful creations total, prior URI unchanged, source fingerprints retained, all owned test outputs removed. API26 gates out that MediaStore body; no old-platform pass is inferred. Existing transaction/recovery/share/crop suites remain required in the grouped profile=current run. New exact-source Android execution is PENDING.

### Previous branding checkpoint and current locale correction

Actions39875883737 atc615074 compiled/linted all APKs and generated the shared icon successfully. API36 passed renderer4/cache3/private7/transaction10/export-flow3/share4/batch5/recovery8 plus the codec matrix, but FAILED ImportSettingsFlowTest at the Catalan locale deadline after10 seconds. Therefore the earlier timing-only explanation was incomplete; c615074 is not full acceptance. No production locale bug is claimed uniquely identified from that observation.

The current block routes API33+ language selection and reads through the actual Activity/context LocaleManager service, leaving AppCompat persistence only for older Android. The same helper is used by UI and instrumentation; the test still requires actual Spanish/Catalan/system resources and retained ViewModel/draft, with a bounded condition wait. Failure diagnostics include the selected platform language. This avoids a static delegate choosing an old Activity service, but its runtime acceptance remains PENDING with the recovery block. The canonical image files and generated launcher integration are retained without another art rewrite. Real launcher masks, physical-device locale/restart,16KiB, full process-kill/cleanup and receiving-app acceptance remain open.


The final batch also persists a deterministic frozen-plan fingerprint. Retry compares current preflight against the original source/revision, placement/crop/output geometry, background, codec/format/alpha/metadata and render-policy version before clearing any receipt. Missing or changed identity is review-only. The added changed-plan case edits the stored background while keeping its old fingerprint and requires zero saves. This guards original-plan semantics across later engine changes; heap limits are rechecked without becoming part of the fingerprint.



## Accepted retry/locale block — source f8cca4b

[Actions39883541691](https://github.com/santirodriguez/FixMyShot/actions/runs/39883541691), exact source **f8cca4b44dacda437f0d2a91d1e57749b2c0a476**, completed SUCCESS for build and API36. Optional legacy SAF was SKIPPED/NOT RUN. The actual source-bound smoke log was read: renderer4, cache3, private7, transaction10, export-flow4, share4, import/settings4, batch5, retry7, recovery8, plus80 synthetic PNG/lossless WebP encodes and the source-bound checker PASS. This includes the actual Spanish/Catalan/system resources and new owned MediaStore restore/retry path. Generated branding packaging and APK compilation succeeded. No independent downloaded ZIP-byte verification is claimed for this receipt. Page size4096 is not16KiB runtime or physical-device acceptance. Native formats remain diagnostic.

This supersedes the PENDING execution status of the preceding retry/locale implementation checkpoint, without changing its manual/hardware limitations.

## Output review and private cleanup — implementation checkpoint

Results now offer recheck, verified ACTION_VIEW with ClipData/read grant, and a separate explicit confirmation to keep an existing matching output as successful. Confirmation rechecks under the same job OS lock; read-only discovery never promotes uncertainty. Missing/changed/pending/unverifiable/conflicting records cannot be accepted or opened by this route. No provider output is rewritten, deleted or recreated. A crash between receipt updates remains reviewable.

Private cleanup runs after durable explicit review, removes the frozen settings/source-URI journal and unused staged bytes, preserves sources referenced by the active draft or unreviewed jobs, and fails closed on malformed recovery state. Startup pruning now shares that protection with in-process drafts, runs before import work, retries interrupted private cleanup and removes old abandoned candidates. Only direct regular private files are deleted, without following file symlinks. Output receipts and lock inodes remain; complete receipt retirement is still open.

RecoveryActionsTest adds4 cases: explicit acceptance/read-only opening/grants; missing/changed/pending/conflicting rejection; live lock exclusion; and real API29+ owned MediaStore pending refusal followed by visible matching-byte acceptance without changing output bytes. PrivateSessionMaintenanceTest adds4 cases for draft/unresolved protection, reviewed release with unchanged receipts, corrupt-state/symlink refusal and age cleanup. Combined recovery count is16. Host locale/collector checks are not Android execution. Exact-source Android validation of this new block is PENDING; external-viewer grant delivery, mid-write process-kill, low-space, full private lifecycle and physical-device acceptance remain open.


## Accepted output review/cleanup — source81f457d

[Actions39889883085](https://github.com/santirodriguez/FixMyShot/actions/runs/39889883085), exact source **81f457dd5a015e3deefb1492f2d31406e26130b6**, passed build and API36. Optional legacy SAF was SKIPPED/NOT RUN. The actual API36 job120208357488 log reports renderer4/cache3/private7/transaction10/export-flow4/share4/import-settings4/batch5/retry7/recovery16 and80 real PNG/lossless WebP encodes, plus source-bound checker PASS and4096-byte pages. All eight new output-action/maintenance cases ran, including the owned MediaStore pending/visible verification case. No independently downloaded ZIP-byte validation, real external viewer/receiver, physical hardware or16KiB acceptance is claimed.

This replaces the PENDING execution status of the previous output-review/cleanup checkpoint; full receipt retirement and private process-death lifecycle remain open.

## Manual color, exact size and undo controls — implementation checkpoint

Review now exposes manual RGB, exact dimensions or preserve pixels, explicit enlargement, stable-ID reorder and confirmed apply-batch-settings-to-all. Batch scope shows batch values rather than selected overrides. Crop uses the exact output aspect and same frozen source rectangle/renderer; enlargement is a review conflict unless explicitly allowed, and destination constraints remain independently enforced. No default format, privacy policy or original source bytes are changed.

Removing the final image previously deleted staged bytes immediately and hid Undo. Working IDs now include the one-step undo snapshot; removal preserves bytes, Undo is visible for empty lists, and a subsequent edit that replaces the snapshot releases unused files on the existing worker. Explicit discard clears the current/undo sources, subject to pending-job protection. This does not add unsaved-draft persistence.

Three JVM preflight cases cover exact crop reduction/opt-in enlargement, target aspect/destination conflicts and source ownership over undo expiry. PrivateOutputExecutorTest grows7 to8, comparing exact20x20 reduction and opted-in100x100 enlargement of the same40x40 source rectangle against the shared preview, with intact source hash. Its existing padded PNG test now verifies a nontrivial custom RGB border plus unchanged source pixels. ImportSettingsFlowTest grows4 to5 with an Activity-level remove-last/undo/fingerprint check followed by asynchronous cleanup after undo replacement; it uses synthetic private bytes and the actual callbacks through a reflection seam, not click-by-click manual UI acceptance.

Collector/mock require private8, import/settings5 and existing recovery16. Local resource/host checks are separate from Android execution. Exact-source Android acceptance is PENDING for this new block. Manual text size, TalkBack, crop gestures, visual color/size control review and physical-device checks remain open.


## Accepted output controls/undo — sourceb5395605

[Actions39891163948](https://github.com/santirodriguez/FixMyShot/actions/runs/39891163948), exact source **b5395605f4e8b4b8c598376fd0694d2c21f53aab**, passed build and API36. The actual smoke job120212671134 log reports private8/import-settings5/recovery16/retry7/export-flow4 plus renderer4/cache3/transaction10/share4/batch5 and80 real synthetic encodes. Source-bound checker PASS;4096-byte pages. Optional SAF SKIPPED/NOT RUN. No independent ZIP-byte, hardware/16KiB/receiver acceptance is claimed. This supersedes PENDING for that controls block.

## Payload-preserving export and PNG/WebP orientation — implementation checkpoint

A strictly eligible unchanged PNG/lossless WebP can now be copied as a private sanitized candidate, retaining original IDAT/VP8L compressed bytes. The ordinary source digest, destination/preflight and candidate verification still run. No copy is allowed for crop, padding, scaling, changed format, orientation normalization, unsupported color/profile/chunks or alpha/compositing changes. The existing renderer is the fallback for otherwise supported static sources. Metadata-preservation opt-out remains blocked; this does not implement its future allowlist.

The bounded PNG copier checks signature, chunk sizes/order/count/CRCs and strict8-bit RGB/RGBA geometry; drops EXIF/text/time; retains only accepted canonical sRGB-related chunks. Unknown/unsupported appearance data requires rendering, never blind profile deletion. The WebP copier accepts static VP8L plus optional VP8X/EXIF/XMP, clears removed flags and corrects RIFF size/padding. ICC-bearing/unknown routes require rendering. Both paths stream bounded blocks, enforce cancellation/deadline, remove incomplete copies and undergo strict container/native-color-space reopen before provider publication. They are container transformations, not home-grown image encoders.

ReviewSources now applies AndroidX EXIF orientation to PNG/WebP as well as JPEG. It scans PNG/WebP container boundaries to reject animation, recognized PNG HDR signalling and oversized EXIF/profile chunks before metadata reads/full decode. Existing HDR/gamut limitations remain; this is not universal HDR/color support. Render-policy version2 intentionally makes older frozen jobs review/restore-only for retry because orientation/output processing semantics changed. No old receipt is cleared by the policy change.

PayloadCopyTest adds4 synthetic cases: PNG EXIF/GPS/text removal with exact IDAT preservation and reopened pixels; real lossless WebP VP8L preservation with XMP removal; noncanonical color/bad CRC/cancellation refusal and zero retained candidates; all8 PNG EXIF orientations through actual owned MediaStore staging plus APNG rejection. The WebP body uses API30+ and the real provider body API29+; earlier APIs do not inherit acceptance. Combined private executor/payload suite count12. Current exact-source Android acceptance is PENDING. Full metadata opt-out, cross-format orientation/color/alpha/ICC/HDR acceptance, hardware and receiver tests remain open.


## Rejected payload checkpoint and undefined-orientation correction — source1206409

[Actions39898060905](https://github.com/santirodriguez/FixMyShot/actions/runs/39898060905), exact source **1206409bb4fea5f3226144f51ab25fb65d5a6138**, passed build but FAILED API36 job120234748085. Actual logs: combined private/payload12 had8 SOURCE_UNREADABLE failures; export-flow4 had3 failures and import/settings5 had1 failure. Explicit all8 PNG EXIF orientations passed; recovery16/retry7/batch5/share4 also passed. The failures reject this executable checkpoint; b5395605 remains the last accepted complete API36 source. Optional legacy SAF was SKIPPED/NOT RUN.

The shared failure is consistent with AndroidX's synthesized undefined orientation0 on ordinary PNG/WebP. The correction maps only0 to normal identity; invalid nonzero values still fail and transforms1–8 remain. The provider-backed orientation test now imports and fully decodes a plain no-orientation PNG before exercising all eight explicit values and APNG refusal. New runtime acceptance is PENDING, not inherited from either earlier successes or this diagnosis.

## Selective metadata export — implementation checkpoint

Review now has a default-enabled removal checkbox at batch/selected scope, localized en/es/ca, plus a format-specific preservation/privacy disclosure. PNG/lossless WebP opt-out copies only16 documented supported fields into a fresh rendered output through ExifInterface1.4.2, derives DateTime from retained capture time, regenerates dimensions and orientation, and verifies the saved values. Source blobs, thumbnails, opaque XMP/IPTC and serial/editor/path fields are never copied. The low-level pixel encoder retains its explicit metadata-preservation refusal.

Independent closed TIFF verification bounds the EXIF area to64KiB, directories to3 and entries to32 each, bounds component counts/offsets and rejects unknown tags, duplicate/cyclic/thumbnail directories and stale dimensions/orientation. PNG EXIF receives CRC checks; WebP requires matching EXIF presence/feature flag and normal lossless image ordering. Default-removal candidates still reject EXIF. Failed writes/verification are never published.

MetadataPolicyTest adds3 cases: the exact supported tag set and actual PNG/WebP/JPEG-to-PNG field preservation with normalized rendered pixels/dimensions, forbidden-field absence/injection rejection and unchanged source hashes; default PNG/WebP removal of supported and opaque source fields and omission of an overlong opted-in field; positive/negative bounded TIFF fixtures for unknown tags, pointer overflow, next IFD/thumbnail, cycles, stale dimensions and excessive counts. Existing core cases now distinguish supported source-aware preservation eligibility from unsupported native policy and the low-level encoder boundary. Combined private suite grows12 to15; all import/settings5, recovery16, retry7, export-flow4 and earlier suites remain required in one current-profile run.

Local checks:153 matching en/es/ca resources/XML/catalog/manifest, Bash syntax and10 host collector cases PASS. Android compilation/runtime for this coherent fix and writer path is PENDING until exact-source Actions succeeds. Manual translation/layout/accessibility, physical hardware, receiver apps, broader ICC/HDR/partial-alpha, native formats,16KiB and distribution gates remain open.


## Accepted metadata correction/preservation — sourced6b63ae

[Actions39905266496](https://github.com/santirodriguez/FixMyShot/actions/runs/39905266496), exact source **d6b63ae835f581d0d21f2ee890b01d033e11feff**, passed build and API36 job120257484988. Actual logs were read: combined private/payload/metadata15, renderer4/cache3/transaction10/export-flow4/share4/import-settings5/batch5/retry7/recovery16, plus80 real PNG/lossless WebP encodes and source-bound checker PASS. Page size4096; optional legacy SAF SKIPPED/NOT RUN. This accepts the bounded correction/selective metadata block, including the plain PNG import regression and actual allowlisted metadata writer tests. It does not establish independent ZIP-byte, real receiver/hardware/16KiB or functional-release acceptance.

## Editable session continuity — implementation checkpoint

ReviewSessionJournal atomically checkpoints current editable settings, selection and one-step undo in one bounded4MiB private document. It shares the existing frozen-draft field codec, retaining schema1/export-plan semantics and requiring explicit allowEmpty only for editable/undo snapshots. A process-wide lock serializes AtomicFile readers/writers. Stable private source IDs, hashes, matching current/undo immutable identity and bounded arrays/strings are validated. No opaque EXIF/image bytes are journaled.

MainActivity binds persistence; edits coalesce to the latest immutable snapshot on the existing worker. Startup restores a valid non-expired session before pruning its current/undo sources. Ownership/size checks precede restoration; actual source SHA-256 is independently rechecked by the existing executor before output, rather than hashing up to1GiB during startup. An editable draft matching a previous frozen job also retains the already-attempted barrier after output review, rather than enabling an accidental same-draft ordinary export. A visible reminder preserves the distinction between a resumed session's explicit format/privacy choices and a new session's PNG/removal defaults. Missing/corrupt journal/source state blocks silent replacement and requires explicit discard. Expiration occurs on reopening more than24h after the last saved update; active in-process drafts and unresolved jobs remain protected. Failed checkpoints are visible/retryable and retain the last saved source references. No automatic export, retry or original/provider mutation occurs.

Four new ReviewSessionTest cases use isolated private directories and fresh ViewModels: all settings/provenance/selection/undo and last of40 rapid edits; remove-last undo source older than24h restored before pruning, matching frozen-job acknowledgement without duplicate ordinary export, explicit discard and default reset; expired/missing/malformed/unknown-schema/oversized state with no silent overwrite/export;100-image saved batch plus incoming offer rejected as a whole after restoration. Existing Activity import/settings cases now discard only their synthetic session checkpoints before fixture cleanup. These cases exercise disk/new-ViewModel continuity; actual OS force-stop/mid-write crash, final receipt retirement, foreground pause and full100-item mixed-result/provider tests remain open.

Collector/mock require import/settings9, private15, recovery16, retry7, export-flow4 and all earlier suites in one profile=current checkpoint. Local resource/XML/manifest/catalog, Bash syntax and10 collector cases are checked separately. Android compilation/runtime for this new session block is PENDING. Earlierd6b63ae success does not transfer to it.


## Scoped session acceptance and deferred locale diagnostic — source54bf003

[Actions39907709146](https://github.com/santirodriguez/FixMyShot/actions/runs/39907709146), exact source **54bf0038d2b9221d4df8968e13434253347233c3**: build SUCCESS; API36 job120265125517 FAILURE with exactly1 failure in9 combined import/settings/session cases. Actual instrumentation status0 confirms all4 ReviewSessionTest cases passed. The failed case requested Catalan but observed selected/system localeempty. Its cause is unresolved; repeated related locale failures are not proof that production language switching is sound or that only the harness is defective. Private15/transaction10/export-flow4/share4/batch5/retry7/recovery16, renderer/cache and codec instrumentation passed. The final source-bound checker was not reached after the failed count assertion. No aggregate PASS, independent ZIP-byte, hardware/16KiB or receiver acceptance is claimed.

Automatic locale switching is now deferred to explicit instrumentation argument localeDiagnostics=true, with its strict assertions retained. The ordinary import test is named to disclose optional diagnostics, still verifies actual incoming PNG/recreation/ViewModel/draft/source integrity, and prints LOCALE_SWITCH_DIAGNOSTIC=NOT_RUN when disabled. Resource completeness remains mandatory. Do not rerun the matrix solely for this diagnostic or call deferred coverage passing.

## Foreground pause and successful session finish — accumulated implementation

A latched foreground flag pauses between export images while the current bounded transaction completes. Untouched targets record CANCELLED/PAUSED and use explicit normal remaining-item retry; published/uncertain items are never silently rerun. Rotation does not pause, and returning while the current item is still finishing does not clear the request. Pause status and explicit continuation are localized. A fully successful matching session has a distinct Finish session action with disclosure; acknowledgement precedes clearing its editable journal/current+undo/frozen source references and UI reset. Existing unresolved-job/source and provider-output protections remain. Failed/partial/uncertain/unrelated sessions do not take that cleanup path. Receipt/lock-file retirement is deferred.

Two additional synthetic cases are prepared for the next larger checkpoint: pause requested during the first item’s save, current publication completing once and explicit retry preserving its receipt; actual private WebP export followed by full session finish, current+undo/journal deletion/default reset and unchanged published bytes/zero provider deletion. Import/settings/session count10; retry count8. These new Android cases have NOT RUN. Only local resource/XML/catalog/manifest, collector and Bash checks are available this turn. No new CI is requested and the validation marker remains unchanged. Accumulated Android changes must not inherit the scoped54bf003 or earlierd6b63ae passes.


## Accumulated source block: recognized SDR color and partial-alpha coverage

Implemented after94ce14b without changing either CI request marker. SourceAppearance inspects8-bit static PNG/JPEG/WebP appearance descriptors before normalization, bounds raw/inflated ICC to64KiB and tag counts/offsets/overlap, requires matrix/TRC agreement with the actual Android decoder descriptor, and recognizes SDR Display P3/Adobe RGB by D50 matrix/transfer behavior. Known HDR/gain-map/multi-picture, high precision, unknown/malformed profiles fail closed where detected; this is not universal HDR/ICC support. Explicit BitmapFactory sRGB conversion must return ARGB8888/sRGB without a gain map. Original classification survives editable/frozen journals and is rechecked before full decode; converted inputs cannot use the compressed-payload shortcut. Review has localized selected-image conversion/gamut-limit and batch-count notices. Existing sRGB version2 frozen fingerprints are unchanged; newly eligible color plans have a bound extension.

Prepared SourceAppearanceTest4 (NOT RUN):
- Independently generated P3/Adobe RGB matrix/TRC profiles in actual PNG/JPEG/lossless WebP containers; sampled/full decode, both PNG/WebP output paths, sRGB output validation and independent standard-primary color oracle, no payload shortcut, intact original digest; owned MediaStore PNG import onAPI29+ only.
- Invalid profile offsets/truncation/inflated size, unsupported transfer, explicit PNG HDR and16-bit rejection, JPEG gain-map/missing ICC sequence and incoherent WebP ICC flag rejection before normalization.
- Source classification in editable/frozen/legacy schema1 documents, unknown enum rejection, changed-color frozen retry refusal.
- Partial-alpha native64 and explicit enlarged96 output through PNG/WebP over white/black/transparent, analytic blend values, transparent edge-hue checks and shared preview/final comparison. Fully transparent hidden RGB is deliberately not asserted.

Collector now requires private-output19 (prior15+4); import/settings/session10, retry8, recovery16 and other suite counts are unchanged. Host collector mocks use the same count. New Android compilation/runtime is NOT RUN; no new Actions run requested. Latest full bounded aggregate pass remains d6b63ae, with scoped build/session evidence at54bf003 and its unresolved deferred locale diagnostic. Foreground pause/finish94ce14b also remains pending accumulated Android acceptance.

Cheap checks executed:167 matching en/es/ca XML/resource/catalog/manifest keys; Bash collector syntax;10 host collector regression cases. A separate local LittleCMS/Pillow check reopened the exact synthetic profile-construction constants and converted(204,128,64): P3→(217,123,46), Adobe RGB→(227,129,57), matching the independent numeric oracle within3 levels (in fact exact for these patches). This checks fixture/oracle coherence only; it does not execute Android, prove displayed appearance, validate real photos/HDR, establish full alpha fidelity or close a release gate. Hardware/color management, full lifecycle/provider batches, receivers and release readiness remain open.


## Accumulated source block: actionable import/results and per-image preflight

Implemented after4c7aad8 with both CI request markers unchanged. Latest completed import has a <=100-item received-order report of actual additions, ignores and typed failure positions; valid sources remain intact. Details are localized, expandable and contain no names/URIs/bytes. Prior sender-level failures keep their generic count. Dismissal clears feedback only; reports survive Activity recreation in memory, not process death, and are replaced by the next completion or cleared on discard/finish. Appearance rejection categories now distinguish animation, HDR/multi-picture, high precision, malformed descriptors and unsupported color without changing accepted pixels.

Export preflight retains blocked item IDs/reasons for direct selection/review and clears them after edits. BatchItemProcessor preserves timeout/resource/unavailable/I/O/metadata/invalid-output codec causes before any save. Closed ResultFeedback resources explain terminal item causes/actions; NEEDS_REVIEW takes priority over retry-like/unknown reason strings. The UI mentions offered retry only for receipt-eligible items in a compatible current draft. No new automatic retry, format substitution, provider mutation or journal/fingerprint schema change. Job headings and completed import summaries have polite live announcements; this is not TalkBack/manual accessibility acceptance.

Four ResultsFeedbackTest cases prepared, **NOT RUN**:
- API29+ real owned MediaStore good PNG/APNG/corrupt/deleted input through the actual ViewModel queue: keeps valid pixels/hash, reports rejection order/categories, replaces the latest report and dismisses feedback without deleting the draft. Earlier APIs do not inherit this provider acceptance.
- Multi-image preflight identifies missing-hash/native-format blockers; selection preserves the report, repair resets blockers and destination cancellation creates no output/job.
- Injected codec timeout/resource/unavailable/I/O/cancellation at the processor boundary preserves useful results with zero provider calls, candidates or receipts.
- Persisted uncertain-publication records with retry-like/unknown codes remain nonretryable and get generic localized output-review text in all3 resource configurations; no raw journal strings/URI in displayed text. This does not exercise automatic locale switching.

ImportSettingsFlowTest also checks latest-import feedback across existing recreation; SourceAppearanceTest checks typed descriptor rejection reasons. Future collector: import/settings/session14 (prior10+4), private19/retry8/recovery16/export-flow4 and other suites unchanged. Collector/mock aligned. Local PASS:196 matching en/es/ca resource/XML/catalog/manifest keys, Bash syntax and10 host collector cases. Android compilation/execution is NOT RUN; latest full bounded pass remainsd6b63ae with scoped54bf003 evidence/deferred locale failure. Pause/finish94ce14b, color4c7aad8 and this feedback block remain accumulated, unaccepted Android source until an exact checkpoint executes. Physical hardware/native formats, large lifecycle/provider batches, accessibility/receivers and distribution gates remain open.


## Accumulated checkpoint00ede761 — build passed, three runtime failures

[Actions39917822763](https://github.com/santirodriguez/FixMyShot/actions/runs/39917822763), exact00ede761c79b1a672d7804047dd441dde99961fd. Build120295532422 SUCCESS: core tests, lint, debug/unsigned-release/test APK compilation and native APK inspection completed; actual logs read. API36 job120297970845 FAILURE, legacy SAF SKIPPED/NOT RUN. Actual logs read: renderer4/cache3/transaction10/export-flow4/share4/batch5/retry8/recovery16 passed; all5 ReviewSessionTest cases (including successful finish) and the explicit foreground-pause/retry case passed. Private group19 had2 SourceAppearanceTest failures; import/settings/session14 had1 ResultsFeedbackTest failure. The complete aggregate is NOT accepted; its final source-bound report checker was not reached after the failed private count guard. Earlier standalone codec4 test statuses passed, but no new artifact-ZIP/physical/16KiB claim follows.

Failures and bounded corrections:
- Wide-gamut output returned INVALID_CANDIDATE after its color sample/classification passed. The first reported assertion lacked format context. Inspection identified the strict alpha-presence equality: fully opaque lossless WebP legitimately omits unused alpha. A host libwebp/Pillow encode independently reproduced opaqueRGBA→decodedRGB and partialRGBA→decodedRGBA with exact pixels. The corrective path requires a full rendered-canvas opacity proof before accepting that WebP case, propagates proof to final transactional verification and leaves the default verifier/actual-transparent/PNG/opaque-destination rules strict. The next run must verify this diagnosis; the original log alone did not uniquely identify the candidate subcheck.
- Partial-alpha check expected straightARGB0x80f01808 but observed0x80ed1808 (red240→237 at alpha128). The prior straight-channel oracle ignored the8-bit premultiplied representation. Corrective checks require exact alpha, one-level premultiplied-channel agreement, explicit transparent/opaque regions, edge hue, light/dark blends and shared preview/final appearance. Existing opaque source-pixel/codec equality checks are unchanged. No production renderer changed for this oracle correction.
- Mixed import's visible exception was SecurityException at Fixture.close deleting the URI deliberately removed earlier for a missing-source test. Remove that URI from the fixture-owned cleanup list immediately after its first successful deletion. Do not blanket-ignore cleanup exceptions or add permissions. Because finally can mask an earlier assertion, the entire mixed-import case still needs a clean rerun; it is not yet accepted as passing.

The correction adds one meaningful full-canvas opacity/refusal/transactional-publication case, taking private20; import/settings/session14, retry8 and recovery16 stay unchanged. SourceAppearanceTest reports synthetic format/size/background context for future failures. Local196 resource/XML/catalog/manifest, Bash and10 host collector checks pass; these do not accept new Android code. Corrections are accumulated into one follow-up current-device checkpoint. Main/signing/publication/native enablement/artwork remain untouched; broader hardware/native/receiver/distribution gates remain open.

## Follow-up a047cfe6 — scoped corrections passed; appearance still open

[Actions39924038144](https://github.com/santirodriguez/FixMyShot/actions/runs/39924038144), exacta047cfe6ba8cad93f9ceb8c53e7984c106b5fd78, tree2159f2b742eb5250298712e22dca105b1506a351. Build120315940893 SUCCESS; API36job120318176161 FAILURE; SAF skipped/NOT RUN. Actual runtime logs read: all prior private15 cases and the new opaque-WebP full-canvas proof/strict-refusal/transactional-save case passed. All4 ResultsFeedbackTest cases, including the previously masked mixed real import, passed. Import5/ReviewSession5/retry8/recovery16/transaction10/export-flow4/share4/renderer4/cache3/batch5 passed. Standalone codec4 and native diagnostics had passing statuses, which do not establish native-format/device acceptance. The final source-bound report checker was NOT REACHED after private20 failed2. No complete aggregate, independent artifact-ZIP, physical-device or16KiB acceptance is claimed.

Two remaining failures:
- Wide-gamut case now advances beyond the first PNG outputs to a JPEG fixture and fails SourceAppearance.jpeg line228, rejecting an inconsistent/duplicate ICC sequence. The fixture had prepended its synthetic profile to the complete Bitmap.compress JPEG, potentially retaining an encoder ICC profile. The correction removes only pre-scan ICC APP2 segments from the generated test JPEG before adding its single synthetic profile. Other segments and the entire scan remain byte-for-byte intact. The production parser remains strict; the rejection case now explicitly includes duplicated ICC sequence1. This diagnosis follows from the generated header and rejection branch; actual device JPEG bytes were not independently extracted.
- Enlarged partial-alpha edge expected0x68f01808, actual0x68ee1405 atline335. At alpha104 the expected/actual premultiplied green levels are10/8, exceeding the strict one-level oracle. This is unresolved appearance evidence, not a proven test-only defect. The assertions and renderer remain unchanged. Defer repeated per-case CI tuning while retaining the color/alpha acceptance gate.

## Prepared 100-item transaction exercises and JPEG fixture correction

HundredItemBatchTest adds two Android cases using100 distinct synthetic opaque PNG sources with varying bounds/colors and alternating PNG/lossless-WebP output overrides. Both use the real preflight, full-source renderer, encoder, candidate verifier, transactional saver, frozen draft, batch/item journals and a newly constructed coordinator for restart inspection/retry. Only the external provider is an explicitly bounded in-memory test port; no user/gallery files are written.

1. A provider write refusal at attempt13 is durably cleaned up; cancellation after37 processed items leaves36 published/1 failed/63 cancelled. Read-only restart creates nothing; explicit retry targets exactly64 no-output items. Final100 published outputs require101 create attempts, unchanged earlier receipts/provider bytes, no leftover candidates, intact source hashes, actual decoded dimensions/MIME/opaque results and exact unscaled source-rectangle pixels for both codecs. An empty retry cannot publish again.
2. Publication at attempt37 becomes visible but returns no confirmation. Its receipt stays indeterminate and nonretryable, even with matching bytes. Explicit retry of that item must refuse without a create call; the63 untouched items can continue. Final state is99 published plus1 still requiring review,100 visible outputs, no deletion and unchanged original provider bytes/uncertain receipt.

Batch group becomes7 (prior5+2); private20/import-settings14/retry8/recovery16 and other groups stay unchanged. Collector/mock counts are aligned. The new cases and corrected JPEG fixture have NOT been Android-compiled or executed. They do not close G4's real-provider,100 mixed real/synthetic large-image, OS process-kill, low-space, gallery/receiver or hardware/performance gates. No CI marker was changed and no run requested for this source block. The strict partial-alpha failure remains visible rather than being relabelled or removed.

## Committed build inputs and optional JXL disposition

The existing a047 build artifact12489096076 was independently downloaded and checked (31,377,312B; SHA256f357e7cad31c0481788cb41cdc27634a207aec58ca89392fb275cbddc1dd9b84; ZIP CRC and embedded source SHA PASS). Its standard Gradle8.13 wrapper JAR matches the rechecked official checksum; generated launcher/properties bytes and app307/core35/codec-native161 lock rows are retained exactly. All308 distinct locked GAVs exist in the captured verification inventory; captured settings/root/module build/bootstrap/workflow blobs match the current branch base. This promotes a verified wrapper and selected version state, not dependency authenticity. See [the exact input record](feasibility/committed-gradle-inputs.md).

The compatibility bootstrap now validates committed bytes with Python without Java/network/regeneration. Offline tests4 PASS: valid wrapper, each changed executable, changed distribution URL/digest/duplicate properties and missing-JAR refusal. Existing Gradle trust-candidate tests7 PASS. Resource/XML/catalog/manifest196 PASS, Bash syntax and workflow YAML/marker-only-trigger/locked-invocation checks PASS. The workflow no longer writes locks automatically and checks for lockfile drift; unapproved checksum metadata capture remains explicit, with current551 components/958 artifacts/0PGP. Strict authenticated verification, plugin/native-source review and an independent clean rebuild remain open; the changed Gradle invocation has not yet been executed in an Android build.

[JXL is deferred for v0.1.0](decisions/jxl-first-delivery.md) under the blueprint's conditional gate after the retained three-ABI reference attempt. Unstripped reported library sums are not optimized APK deltas, and do not prove an unavoidable budget overrun. No new native measurement or runtime support is claimed. New format selection hides JXL; stored JXL values still decode and remain blocked without substitution/schema/fingerprint changes. English/Spanish/Catalan retained-value labels explain deferral. HEIC/AVIF gates are unchanged and cannot be silently dropped.

No CI marker changed and no new app/reference CI was requested. No earlier runtime pass transfers to this build/selector block or the prepared100-item/JPEG fixtures. Partial-alpha remains a strict unresolved appearance gate. Manual hardware, receivers, accessibility/locale visual acceptance and all release/signing/distribution gates retain their prior truthful status.

## Explicit synthetic native-device check — accumulated implementation

Settings now exposes a separate HEIC/AVIF diagnostic using the existing NativeStillEncoder unchanged. Fixed opaque sRGB fixtures64×64,65×67 and1344×2992 are attempted once per format under the existing API/memory gates and an8-second requested codec timeout. This is not a hard watchdog; a vendor operation may return late. Stop/Back/Close/foreground departure/clear requests cancellation without starting another case. The ViewModel keeps progress/report through configuration recreation and serializes the check with ordinary private export/session work; normal imports/exports wait. No automatic start, native export enablement, geometry/fingerprint/schema change or external-provider action occurs.

Returned candidates require direct private ownership, actual ftyp brands/MIME/dimensions and a full software ARGB8888/sRGB reopen. Comparison checks opaque alpha, exact RGB/finite PSNR and six3×3 median color-patch errors against the blueprint numeric targets. Targets passing is not appearance approval. Each owned candidate is deleted after inspection/rejection/cancellation; existing24-hour abandoned-candidate cleanup applies after process death. Outside paths cannot authorize deletion. The closed transient JSON contains only API/manufacturer/model, app version/code and installed base APK hash when readable, typed per-case outcomes and synthetic measurements; no selected-image data, paths, serials or exceptions. Sharing text is a separate user tap. See [the exact workflow and measurement limits](feasibility/native-device-check.md).

Four Android NativeDiagnosticTest cases are prepared, NOT RUN: independent exact/single-pixel/wrong-color scoring and comparison cancellation; closed report/error omission/existing non-native adapter refusal; cooperative stop plus disguised-PNG candidate rejection/cleanup; cancelled/low-budget zero-encode behavior and own-APK hash binding. They use injected failures/candidates for policy coverage, not actual native format acceptance. The grouped import/settings/session block becomes18; private20/batch7/retry8/recovery16 and existing codec/native probe counts remain unchanged. Local215 resource/XML/catalog/manifest keys, Bash,4 wrapper boundary and10 host collector cases PASS. No Android compilation/runtime or phone/cancel/share/appearance claim follows from those host checks.

This completes another coherent source block after the pending9f17 JPEG/100-item and34407bd3 wrapper/lock/menu changes. One combined profile=current Android checkpoint will validate the accumulated source rather than per-case reruns. The strict known partial-alpha failure remains an open gate; record actual scoped outcomes even if it keeps the aggregate red. No legacy/automatic-locale/JXL experiment is requested, and no merge/signing/publication or physical-device acceptance is implied.

## Accumulated7a5b75b0 build receipt and runtime retrieval limit

The previously requested [Actions39977393747](https://github.com/santirodriguez/FixMyShot/actions/runs/39977393747), exact source7a5b75b0757aa9ac999fbca540fd234e2e20e2a6, was observed with build120495476144 SUCCESS. Actual build logs were read: core tests, lint, debug/unsigned-release/test APK compilation and native APK inspection completed; committed wrapper validation and the unchanged dependency-lock check passed (`BUILD SUCCESSFUL in5m39s`,2026-10-23T15:26:34Z). This compiles the JPEG fixture correction,100-item tests, committed Gradle inputs/JXL selector disposition and synthetic native-device diagnostic. It does not execute their Android cases.

API36 job120498638432 was last observed IN_PROGRESS; SAF120498641303 was skipped/NOT RUN. Subsequent run/jobs/log queries returned404 and the source-filtered run listing returned zero entries. This does not establish the runtime result or why retrieval became unavailable. No rerun was requested to work around retrieval. Native-policy4,100-item2 and JPEG appearance runtime outcomes remain unverified; the known strict partial-alpha failure and all physical-device gates remain open. Build artifact metadata12513250417 was observed but its ZIP was not downloaded/inspected, so no new independent artifact-byte acceptance follows. The latest full bounded aggregate remainsd6b63ae; earlier scoped runtime evidence retains its exact-source limits.

## Explicit bulk selection for verified sharing — prepared source

Results now provide localized select-all and clear-selection controls in English, Spanish and Catalan. Select-all includes only PUBLISHED items with recorded output URIs; failed, cancelled, queued and uncertain items remain excluded. Individual toggles remain available. Selection changes are refused while sharing/output inspection runs. These actions create no output and launch no chooser. The existing share path still re-reads every selected item receipt, verifies the provider bytes and refuses the whole request on any mismatch before building temporary-read-granted content-URI intents. No automatic selection, posting, fallback share or re-export was added.

Two existing ReviewExportFlowTest cases were extended, keeping export-flow count4 and all collector counts unchanged. The real API29+ owned-MediaStore retry case checks that a failed item cannot enter bulk selection. The existing real-provider save/share case now writes synthetic PNG and lossless WebP outputs, checks their published Pictures location, exercises select-all/clear/individual selection, verifies single and mixed MIME intents/ClipData/read grants, checks that selection stays frozen during verification, then corrupts only its own synthetic output and requires a null share intent plus an error with no extra output creation. Both source fingerprints must remain intact; only fixture-owned outputs are removed. Receiving applications and manual chooser behavior are still unverified.

Local PASS:217 matching resource/XML/catalog/manifest keys,4 wrapper boundary tests,10 host collector regressions, Bash syntax and complete application string references. The new bulk controls and extended Android cases have NOT been Android-compiled/executed; they cannot inherit7a's earlier build or runtime evidence. Neither CI request marker changed and no new run was requested for this block. Keep the changes for the next accumulated checkpoint rather than individual reruns.

## Reviewed successful-job retirement — prepared source

Finish session now retires private receipts after durable acknowledgement and successful editable/frozen/current/undo cleanup. Startup also attempts retirement of previously reviewed, entirely published jobs after their frozen draft is released. ReviewedJobRetirement requires a matching batch token, PUBLISHED batch items and matching PUBLISHED item IDs/URIs, no remaining draft, recognized regular files and direct canonical ownership. It acquires the existing job lock, renames the complete directory to the private sibling export-jobs-retired area, closes the original stable lock inode, then removes only recognized files. It makes no provider call and does not newly verify saved pixels or promote a publication state. Partial/cancelled/uncertain/corrupt/mismatched/locked/unknown-path jobs remain retained.

The separate namespace keeps interrupted deletion out of active recovery scanning. Bounded startup remnant cleanup can finish it later; an empty directory retains a retry point if removing the acknowledgement cannot commit. No directory-fsync, secure-erasure or exact-time cleanup guarantee is claimed. Receipt retirement for unresolved states, real OS-kill during rename/deletion and large historical backlogs remain open.

Existing Android cases extended, with counts unchanged (private-maintenance4 within recovery16; ReviewSession5 within import/settings/session18): current/unreviewed protection; cancelled/intermediate/indeterminate/mismatched receipts refusing retirement; live OS-lock refusal; published-job retirement with unrelated saved bytes intact; unknown file/symlink preservation; failed preference cleanup and a recreated maintenance instance draining a simulated interrupted-deletion remnant while current sources/recent candidates survive. The full successful ViewModel session case now requires empty active/retired job directories and no review acknowledgement, in addition to unchanged published bytes/zero provider deletion/default reset/source cleanup.

These source changes and the previous bulk-sharing block are NOT Android-compiled/executed. The prior7a build receipt does not validate them; its runtime queries still return404 at the bounded recheck. Local resource/XML/reference, collector and Bash checks are recorded separately from Android acceptance. No CI marker changed and no new workflow run was requested. README now presents product behavior/limits concisely; precise historical evidence remains here rather than in the product introduction.

## Explicit pending MediaStore cleanup — prepared source

After a deliberate output recheck reports OUTPUT_PENDING, Results onAPI29+ offers a separate confirmed removal action for an external_primary MediaStore image-item URI. ExportRecoveryActions holds the existing job lock, rejects unsafe private journal paths/published or conflicting receipts and requires fresh PENDING evidence. PendingMediaStoreRemoval independently checks exactly one row for IS_PENDING=1, owner package, exact saved filename/MIME and Pictures/FixMyShot, then repeats those predicates in the deletion selection. Only exactly one positively deleted row permits writing FAILED/no-URI item and batch receipts, with UNFINISHED_REMOVED feedback. Existing frozen-draft/no-output rules decide retry eligibility; no export is automatic. A resolved review heading returns to completed results. No permission, source mutation, published-copy deletion, SAF deletion, background cleanup or silent publication is introduced.

Existing RecoveryActionsTest cases remain4 and gain no-callback checks for nonpending/conflicting/path-escape/symlink/live-lock/published states, deletion-refusal receipt preservation, and real owned-MediaStore negative checks for foreign package, old API, wrong name/MIME, published receipt, foreign URI and already visible output. A new fifth ReviewExportFlowTest case uses the actual ViewModel, renderer/PNG encoder/journals and MediaStore, with only the first publication confirmation deliberately refused while leaving its entry pending. Removal before recheck is refused; explicit recheck then removal must produce no URI, a missing owned provider row and retry eligibility with unchanged create count. A separate explicit retry must publish one different URI on the second create, preserve the staged source fingerprint and leave no duplicate pending copy. Only fixture-owned rows are cleaned up.

Collector/mock now require export-flow5; recovery16/private20/settings18/batch7/retry8/share4/transaction10/renderer4/cache3 and native probes unchanged. Local PASS221 matching en/es/ca resource/XML/catalog/manifest keys,10 host collector regressions and Bash syntax; Android compilation/execution is NOT RUN for this block,9c bulk sharing orbaa successful retirement. No API36/provider SQL-race/device/OSkill acceptance follows from source review or mocks. A crash/exception after provider deletion before durable receipt updates can retain uncertain state; absent rows never independently authorize retry. Other volumes/SAF, published/modified/missing/inaccessible/uncertain visible outputs and broader cleanup remain conservative. Both CI request markers remain unchanged; no new run requested and the bounded recheck of7a still404.

## Combined639c5cad acceptance — scoped G4 successes, one appearance failure

[Actions39988437485](https://github.com/santirodriguez/FixMyShot/actions/runs/39988437485), exact639c5cad3a271cb802d33a0b76dc88aed13a9556/tree7bcf70281cae53ee7ec556366e4b5546b20b57f0: build120533623809 SUCCESS, API36 job120536631918 FAILURE, SAF120536634275 skipped/NOT RUN. Actual build/runtime logs were read. Core tests, lint, debug/unsigned-release/test APK compilation, native APK inspection and committed wrapper/lock checks passed (`BUILD SUCCESSFUL in5m18s`,2026-10-23T16:44:33Z).

Actual per-test instrumentation statuses confirm export-flow5/share4/settings-session-feedback-native-policy18/batch7/retry8/recovery16/transaction10/renderer4/cache3 PASS. This includes both100-item real-codec exercises with their simulated provider faults, all5 editable-session/finish cases, mixed verified sharing/select-all and changed-output refusal, published-job receipt retirement/remnant/refusal cases, guarded real-MediaStore pending removal and a separately requested retry, and4 injected native diagnostic policy/metric cases. These are bounded synthetic API36 successes, not receiver/hardware/native-format/real100-item-provider/process-kill/low-space acceptance. Private-output19/20 passed: corrected JPEG/wide-gamut path, malformed/high-depth/HDR refusal, journal color identity and full-canvas opaque-WebP proof passed alongside prior private/payload/metadata cases.

The sole failure remains partialAlphaSurvivesBothCodecsAndCompositesWithoutDarkOrLightHalos atSourceAppearanceTest361: WEBP_LOSSLESS/96/Transparent edge24 expected0x68f01808, actual0x68ee1405, premultiplied green10vs8 beyond the unchanged one-level allowance. PNG and native-size WebP preceding cases passed. This does not uniquely identify renderer, stored channels or decoder quantization as the cause. The failed candidate was removed by finally and is absent from these artifacts. No tolerance, render policy, codec route or production encoder was changed. The aggregate is NOT accepted; the final on-run source-bound checker was NOT REACHED after the strict private-count guard. The latest full bounded aggregate remainsd6b63ae.

Independently downloaded ZIPs passed provider SHA256, CRC and exact embedded source checks:

| Artifact | ID | Bytes | ZIP SHA256 |
|---|---:|---:|---|
| Build |12517526421|31577286|9523ba781761cea359081c976dd2242e93b9d33bab3b51741b9ede2a62da0547|
| API36 smoke |12516957806|46225|e481899b08164d0b825012fe4ffd4ac1cd9557d961918b64ff044bf75387a905|

All3 embedded APK hashes match the build receipt. Standalone host device/native/grid/layout report checkers passed on the extracted smoke artifact:80 codec samples, API36, page4096; native/grid/layout successes remain diagnostic classifications. Independently reconstructed fixture0 and decoded PNG/WebP match all4,021,248 RGB pixels, SHA256214ef1dd2d3e692b99c3c24e822424a38597051bdc791ac09806bb5468045861. These post-download checks do not convert the failed aggregate into a pass or establish independent build reproducibility/authenticated dependencies/16KiB/device acceptance.

The unchanged debug APK is25,059,605B, SHA256bc75f33840184b4312cc3563c5d9113e7cde93959e964bd3f6e7673d1f1252c3, packageorg.cajapersonal.fixmyshot.debug/version0.1.0-alpha.1/code1/minSDK26 with arm64-v8a,armeabi-v7a,x86_64. It is a development test build; no production signing identity, release, installation/upgrade or physical acceptance follows. A native-check text report must match that installed APK hash before being associated with this source.

## Prepared synthetic alpha failure evidence

The existing strict alpha case now captures its candidate, a reconstructed full-size shared-render PNG and a closed row report only when an assertion fails. The report separates reconstructed/default-reopened/unpremultiplied-reopened ARGB rows plus uninterpreted native bitmap bytes and byte order. It contains only the generated64px fixture/case description, no selected images, staged IDs, source names or URIs. Candidate bytes permit independent host decoding before choosing a production correction. Capture failure never replaces the original assertion; stale fixture names are cleared before the test.

The collector copies only four fixed optional names before the strict success guards, validates report identity and file magic, bounds each transfer and discards device error text. Existing host collector regressions now cover optional evidence absence, valid bytes and successful-adb error text, without changing their10-case count or any Android suite count. Local host collector/Bash checks pass. The new Android capture helper is NOT compiled/executed and has no retained alpha file yet; it will run at a later coherent checkpoint. No new CI request/rerun or production pixel/route/fingerprint change is made for this diagnostics block.

## Release presentation and source metadata preparation

The README now uses the shared banner, a centred product introduction, status/license/platform badges, direct navigation and three feature cards. No fabricated UI screenshot, live download, store availability or deployed site is introduced. The CHANGELOG is reconciled with implemented development work and explicit remaining gates. RELEASING and reusable notes/BUILD-INFO templates define the planned universal APK, hashes, public certificate, source/toolchain and install/upgrade evidence; templates are explicitly unpublished.

The initial release ledger records planned0.1.0-alpha.1/code1, with no fabricated distribution/signing/hash record. check-release.py checks source application/version identity, increasing unique codes/version names, distributed receipt fields and certificate continuity, changelog status, exact template fields and the Obtainium APK-name pattern. It optionally renders an exclusively created unpublished notes draft; it never signs, inspects an APK, contacts GitHub or publishes. Ten host regression cases cover current rendering/no-overwrite, invalid/reused codes, malformed versions, source identity drift, missing/unknown template fields, changelog status, sequential history/required receipts, duplicate history, reused bytes/certificate changes and misplaced planned rows. These source/fixture checks do not authenticate ledger history or prove actual APK/signature/GitHub metadata/reproducibility/Obtainium acceptance.

Review's selected-image scope and exact-size enlargement checkboxes now use full-width, minimum48dp toggleable rows with the existing localized text and one checkbox semantic target. Child Checkbox handlers are null; enabled state and original callbacks/state remain authoritative. This matches the existing metadata/settings controls. No geometry/render/fingerprint/defaults or Android test-suite counts change. Source review and221-key resource/XML/catalog/manifest checks passed; Android compilation, TalkBack, large-font/touch and physical acceptance of these two controls are NOT RUN. Both CI request markers and workflows remain unchanged; no new CI is requested for this block.

## Bounded interactive previews and draft-bound confirmations

MainActivity replaces its unbounded preview executor with LatestPreviewWorker: one running task and a single-slot pending queue replacing older previews. No bitmap is decoded before a task starts. A volatile generation rejects obsolete work before decode, before shared rendering, after rendering and on main-thread delivery alongside selected image and plan revision. Sampled sources and stale rendered candidates are recycled; valid displayed bitmap ownership is unchanged. Invalid selection/discard clears pending previews; destruction invalidates generation and closes the worker with best-effort interrupt. Rendering math, source normalization, final output, frozen fingerprints and export/import/checkpoint/cleanup workers remain unchanged. There is no hard native decoder interruption, measured speedup or memory budget claim.

Two additional ReviewRenderTest cases are prepared: a running task held by a latch plus1000 submitted edits must execute only that task and the latest pending request; closing with running/queued work must interrupt the cooperative running task and refuse queued/new work. The renderer group count is now6 (prior4 pixel cases unchanged); collector/mock agree. These Android tests and the new helper are NOT compiled/executed locally. Ten host collector cases pass; mocked instrumentation counts are not queue/render acceptance.

Discard now requires a localized confirmation explaining session/undo removal, private cleanup and intact originals/exports. Both discard and existing apply-all confirmations reset when DraftSession changes and pass the exact confirmed draft to guarded Activity callbacks. Busy/pending import/locked or changed drafts cannot be discarded through that callback; existing saved-session/unresolved-job source protections remain. Saved-output selection gains full-width min48dp checkbox rows with one labelled target; the crop slider now has a localized label and actual zoom state description. New strings bring matching en/es/ca resources to223 keys. Resource/XML/catalog/manifest, source string references, Bash syntax and release-source metadata checks pass. No Android build/UI/TalkBack/largefont/manual/performance/recovery acceptance is claimed. No new CI request or workflow/marker change is made.

## Import progress, cooperative stop and provider-failure completion

The active import now exposes ViewModel-owned processed/total/stopping state. Stop completes the in-flight stage call, preserves accepted items and skips remaining active offers plus offers already queued when Stop is tapped. ImportReport separately counts notAttempted; it never counts those offers as failed images or logs private input references. Cleared offers do not resume; a later deliberate picker/share offer is independent. Repeated Stop is idempotent. The original100-item cap/dedup and private draft/source/export protections remain. Progress is cleared on completion and the ViewModel requests stop on clear; rotation retains in-process state, not an import queue across process death. Blocking provider reads still require completion; no hard I/O cancellation or measured responsiveness guarantee is made.

ReviewSources.stage now includes directory/cache access within its failure boundary, constructs the accepted DraftImage before marking the copy accepted, and maps unexpected provider RuntimeException to typed UNREADABLE. Failed private-copy deletion remains best effort under the existing conservative cleanup policy. No fatal VM-error catch or pixel/color/parser/format/permission change is introduced.

Two ResultsFeedbackTest cases are prepared: a latch blocks the first real fixture import, Stop clears an additional queued offer, and release must retain1 image with3 notAttempted/0 failures, unchanged originals, no automatic restart and a successful later explicit import; a denied cache and throwing ContentResolver must yield typed failures, end busy/progress and leave no synthetic private copies/raw exception text. ImportSettingsFlowTest cleanup is corrected to the new discardDraft(DraftSession) signature; the earlier c671 test helper still reflected its removed zero-argument signature. Settings/import/session/native-policy now20, renderer6/private20/export-flow5/recovery16/batch7/retry8/share4/transaction10/cache3 and other counts aligned. New localized controls/feedback bring en/es/ca resources to228 keys.

Local PASS: resource/XML/catalog/manifest/string-reference checks,10 collector cases,10 release-source cases, release checker and Bash syntax. Android compilation/new tests/UI/provider/cancellation acceptance remains NOT RUN until the exact accumulated checkpoint executes. One current-profile checkpoint is requested for these changes together with8c alpha capture, cb release/checkbox and c671 preview/confirmation work. The existing workflow adds the inexpensive source release checker/tests; action pins/jobs/permissions and JXL marker remain unchanged. Preserve failed aggregate/scoped outcomes separately; no per-case rerun, relaxed alpha assertion, legacy/locale/JXL experiment or hardware/signing/publication claim.

## Accumulated acc925044 results and captured alpha evidence

[Actions40004818530](https://github.com/santirodriguez/FixMyShot/actions/runs/40004818530), exactacc925044cd87ef39b3713463b02c131cefd42b7/tree750e5f4a587edb6b65dc21cccd0fb9a8a80f0a78: build120589923630 SUCCESS (`BUILD SUCCESSFUL in6m28s`,2026-10-23T18:45:41Z), API36job120593811774 FAILURE, SAF120593814844 skipped/NOT RUN. Job logs and per-test instrumentation statuses were inspected. Core tests, lint, debug/unsigned-release/test APK compilation, native package checks, wrapper/locks and release-source checks passed. Both preview queue burst/shutdown cases passed (renderer6), and import stopping/queued-offer accounting/provider-runtime/cache failures plus corrected Activity cleanup passed (settings/session/feedback/native-policy20). Export-flow5/share4/batch7 including both100-item real-codec simulated-provider cases/retry8/recovery16/transaction10/cache3 also passed. Private19/20 passed; sole failure is the same partial-alpha WEBP_LOSSLESS/96/Transparent edge24 atSourceAppearanceTest413. Exact alpha128 centre, PNG and native-size WebP preceding cases passed. Native probe/grid/layout results are diagnostic classifications, not hardware/native output qualification. Final on-run report checkers were NOT REACHED after the strict private-count guard; the aggregate is not accepted.

Smoke artifact12525150035 (47809B, SHA256785155ffa2038ea5eb659608e659b783877a498cd146a25217258febee4c19c5) was downloaded and verified against provider digest, ZIP CRC and embedded exact source. Standalone host device/native/grid/layout checkers pass:80 codec encodes/API36/page4096 plus diagnostic classifications. Build artifact12524321225 (31623793B, provider SHA256985a39ba74f979a3978bc838a385517aa44ee8e39730d61701caf66ee86642d0) is listed but was not downloaded/independently verified here. No new APK is delivered; the previously supplied639c debug APK remains distinct.

The synthetic capture contains candidate WebP588B, reconstructed shared-render PNG352B and row report5807B, schema1/SYNTHETIC_ALPHA_FAILURE/CAPTURED. Host Pillow decoding finds all96×96=9216 RGBA pixels identical in the candidate and rendered PNG, decoded-byte SHA25618bbadefc7b61ac90acedd5da02408529415002a4197568286528b81c282f337. The Android unpremultiplied row matches the reconstructed row exactly; each channel of the reported default-decoded row differs by at most one premultiplied level from the reconstructed row.

At middle-row x=24, alpha=104:

| Boundary | Straight ARGB | Visible native RGB bytes |
|---|---|---|
| Ideal fixture hue at this alpha |`0x68f01808`| nearest premultiply:98,10,3 |
| Reconstructed shared render |`0x68ee1607`|97,9,3|
| Stored WebP, straight decode |`0x68ee1607`| straight-channel bitmap; host RGBA:238,22,7,104 |
| Default Android reopened bitmap |`0x68ee1405`|97,8,2|

The original assertion charged both render rounding and decoder premultiplication to a single one-level comparison with ideal hue: green10vs8 fails, while actual rendered9vsdecoded8 is one. Captured exact stored RGBA excludes lossy encoding as the cause for this particular frame. This does not establish all-format/ICC/alpha/display acceptance or prove every future/vendor decoder's behavior.

The existing alpha test is revised to require full-image stored alpha exactly and every nonzero-alpha stored RGBA pixel exactly equal to the shared render, using a separate inPremultiplied=false decode. The independent ideal centre/blend/clear/opaque/transition-hue checks remain; edge hue uses stored channels. Every default reopened pixel then compares to the render with exact alpha and the unchanged one-level premultiplied-channel allowance. Hidden RGB at alpha0 is excluded as before. No production code, encoder, renderer, verifier, fingerprint, threshold, test count or request marker changes. Host checks confirm the captured full stored pixels and reported default row satisfy those separate boundaries. The revised complete Android matrix is NOT RUN; collect it in a later coherent checkpoint, without a per-case rerun or claiming the failed run became green. Actual TalkBack/largefont, provider stalls, real-device/native formats, reproducibility and distribution gates stay open.

## Review context across recreation — accumulated implementation, Android NOT RUN

The batch/selected-image edit scope is now ViewModel-owned. A ViewModel-only session token scopes the small saved-instance ratio/RGB/width/height/enlargement fields. Within it, composition keys isolate the selected image/scope and each relevant applied value. Rotation of the same editor can restore pending fields; image/scope switches discard them, and applying a different value reseeds the corresponding fields. Custom-ratio defaults reflect the actual applied custom ratio. Destination is explicitly labelled batch-wide, the scope label covers fit/size/metadata as well as format/background, and a short notice explains when to apply pending text. No source URI, bitmap, plan, grant or new journal/default schema is saved in UI state. A new ViewModel or explicit discard/finish starts a new context; applied journal settings remain authoritative. Removing the last image retains scope/token while undo owns sources.

All five selected-only settings callbacks now refuse a missing selection. Draft updates/removal and scope changes reject busy/share-busy/pending-import/restore-blocked/locked states; the UI uses matching edit availability. This does not add background editing, automatic export, provider actions or a new permission. Existing discard/apply-all confirmations still reset on draft changes and are not made saveable.

The existing ImportSettingsFlowTest recreation case now performs a settled second Activity recreation and checks retained selected scope/token/draft without enabling the deferred locale diagnostic. Its remove-last/undo case also invokes actual Activity updates while import-busy and pending-import states block edits; removal preserves context with undo, and actual discard must reset it. Existing ReviewSessionTest cases check fresh-ViewModel applied restoration with a fresh editor context, and successful session finish resetting scope/token while preserving published bytes. At this source, counts remained settings20/private20/renderer6. The later low-storage block below adds one settings case. These extended Android assertions and Compose compilation/runtime have NOT RUN; there is no SDK in this editing environment and no CI request for this block. The revised alpha oracle at55a33357 also remains pending the next meaningful grouped checkpoint. Actions40004818530 remains a failed aggregate with the exact scoped successes documented above.

Local checks:232 matching en/es/ca resource keys, XML/catalog/manifest and source string references,10 host collector cases,10 release-source cases, release metadata checker and Bash syntax. These checks do not exercise UI restoration. Next grouped/manual review must type unapplied ratio/color/size values, rotate, switch between two images with identical applied settings and batch scope, apply/undo/reset values, discard/finish and reopen; verify no accidental apply or cross-image text, correct scope labels, large font/TalkBack and intact sources. Physical checks remain postponed, not waived. No CI marker/workflow, codec/renderer/fingerprint, wrapper/lock, main/signing/publication or artwork change.


## Private import storage headroom — grouped checkpoint requested

ReviewSources now queries StatFs app-available bytes before opening an incoming provider and approximately every1MiB copied. It requires16MiB working headroom plus the next1MiB interval; this conservative advisory cannot reserve space or predict concurrent writes. Unavailable queries fall through to existing bounded I/O and typed failures. Known insufficient space returns LOW_STORAGE with an en/es/ca free-space/reselect explanation; only that attempt’s unfinished private copy is cleaned. Existing sources, completed images, originals and public outputs are untouched, and offers are not automatically retried. The100MiB/input,1GiB staging,100-item and pixel/format/appearance checks remain authoritative. This covers private import staging only, not an external export-provider free-space guarantee or a disk-full stress result.

One synthetic ResultsFeedbackTest case stages a real small PNG, rejects a known-zero-space offer before provider access, switches available bytes to zero during a2MiB provider copy, checks partial-copy cleanup and exact retained/source-provider bytes, then accepts a real PNG with an unavailable advisory. Its received-position report must show two LOW_STORAGE failures with no private references. Availability is injected; actual disk exhaustion/quota/concurrency and physical-device behavior are NOT RUN. Settings/import/session/feedback/native-policy is now21; collector/mock match. Private20/renderer6 and other counts are unchanged.

Source review also found that8822a5d0’s extended Activity test wrote shareBusy despite its private setter. The invalid test-only assignments are removed; actual share locking remains implemented, but that test does not claim an injected share-busy assertion. Its genuine import-busy/pending update guards, recreation/undo/discard and the existing session cases remain. No production visibility is widened. Until the requested build executes, Android compilation is not claimed.

Local resource/XML/catalog/manifest/string-reference checks,10 collector and10 release-source cases, release checker and Bash syntax pass with233 matching locale keys. One profile=current checkpoint is requested for this block,8822a5d0’s edit context and55a33357’s separated stored-alpha/default-decoder oracle together. Require exact-source build/lint/APKs, settings21/private20/renderer6 and all existing source-bound report guards; preserve scoped results and a failed aggregate if any gate fails. No per-case rerun, legacy/automatic-locale/JXL experiment, hardware/native-format acceptance, signing/main/publication or new artwork. Manual tests remain postponed, not waived.


## Grouped521e08cb acceptance — current API36 checkpoint PASS

[Actions40016143705](https://github.com/santirodriguez/FixMyShot/actions/runs/40016143705), exact521e08cb5bb3e7fb66d7cdb938d6e5daa1a27a14/tree7ef66f8a4ca5e2c218dd81c3e57a6438246551d9, finished2026-10-23T20:14:59Z SUCCESS. Build120628572007 passed core/lint/debug/unsigned-release/test APK compilation, native package/wrapper/locks and release-source checks (`BUILD SUCCESSFUL in6m37s`). API36job120632025127 SUCCESS; SAF120632027187 skipped/NOT RUN under profile=current. Actual job logs and downloaded per-test records were inspected.

Renderer6/cache3/private20/transaction10/export-flow5/share4/settings-session-feedback-native-policy21/batch7/retry8/recovery16 all passed. This includes the revised full-image partial-alpha matrix, settled Activity recreation/undo/discard context, fresh-ViewModel/finish reset and injected low-storage/private-copy/original preservation case. Prior color/metadata/orientation/payload coverage remains. Four codec fixtures/80 real lossless encodes and final on-run source-bound smoke/native/grid/layout guards passed. This is the latest complete bounded current-profile aggregate, supersedingd6b63ae for that scope; it does not retroactively turn40004818530 green or accept later changes.

Smoke12528658216 downloaded46210B/SHA256678721346b47ae6e98d2cfac5e15332e51fca489d6d638dca19275bd5978fbf7: ZIP CRC and embedded source-sha.txt exactly matched521e08cb. Independent host smoke/native/grid/layout checkers passed on extracted contents: API36/page4096, grid5positive/12attempts, layoutDIAGNOSTIC_COMPLETE. These remain classifications, not hardware/native export/16KiB acceptance. Build12528188799 only listed31647575B/provider digestf8e53d983a395c05c5f054c77390a966cff09237adaab926cb71842faba44567, not independently downloaded/rehashed or delivered. Supplied APK remains639c; no newer install/signature claim.

Still NOT RUN: actual pending-field Compose interaction/visual restoration, TalkBack/200%font/keyboard/manual locales, real disk exhaustion/quota/concurrent writes, real-provider/process-kill/large-image performance, HEIC/AVIF physical fidelity/resources/fallback/second-family, receivers, authenticated dependencies/independent rebuild and signing/install-upgrade/distribution. No phase-wide approval or another CI follows this receipt.

## Provider exceptions and unlocated output creation — implementation after521e08cb

A provider create can allocate an output then throw before returning its URI. Previously caught I/O errors could become retryable FAILED/no URI, while unexpected runtime errors escaped to generic review. The saver now retains the durable RESERVED receipt and returns indeterminate when create throws, including cancellation-shaped exceptions. It never writes an invalid URI-less INDETERMINATE record, guesses a URI, deletes by filename or authorizes retry. A normal null still means proven refusal/no owned output under the port contract. SAF throws for unexpected created URIs without touching them, and returns null after a wrong/unconfirmed-name document only when its in-tree cleanup succeeded. Failed cleanup stays uncertain. Recovery exposes OUTPUT_CREATION_UNCERTAIN with localized explanation; unchanged safe-retry policy keeps reservations nonretryable.

Unexpected RuntimeException after a known URI follows existing boundaries: pre-publication failure cleans only its owned unfinished item, failed cleanup stays indeterminate with URI, any attempted publication stays indeterminate without deletion. Fatal VM errors are not caught; schema1/render/fingerprint/codec semantics unchanged. BatchItemProcessor carries the new reason and read-only reconciliation preserves it across restart.

One new transaction test uses an actually encoded PNG and injected create-after-allocation I/O/runtime faults, durable RESERVED/read-only restart/nonretryability, runtime write with successful/throwing cleanup, successful publication followed by an exception with exact bytes/no delete, and known null refusal. Existing SAF coverage checks throw/no-touch and failed/confirmed in-tree name-check cleanup. Transaction11/collector/mock aligned; other counts unchanged. New production code/assertions have NOT RUN and do not inherit521e08cb acceptance. Local234-key resource/XML/catalog/manifest/stringrefs/whitespace,10 collector/10 release-source cases/checker/Bash pass. CI markers/workflows unchanged; collect this in a later meaningful batch. Real-provider timing/physical/process-kill gates remain; no signing/main/publication/permission change.


## Destination storage and regular-file sync — grouped checkpoint requested

OutputStorage examines the actual opened FileOutputStream descriptor, accepts available-byte statistics only for a regular file with valid filesystem totals/fragment size/non-root free blocks, and rejects multiplication overflow. Before writing and approximately every 1 MiB copied, the saver requires remaining encoded-candidate bytes plus 16 MiB advisory headroom. Unknown pipe/virtual/closed/query results do not invent a shortage. LOW_STORAGE flows through known-URI cleanup and localized feedback; a failed removal keeps the URI/indeterminate receipt and remains nonretryable. Copy length is frozen and bounded to the validated candidate length, with end-of-copy size checking retained. No provider quota/reservation/concurrency guarantee or pre-render private-candidate storage preflight is claimed.

The existing stream sync assumed every FileOutputStream was a disk file. ParcelFileDescriptor streams can wrap pipes, so the saver now syncs regular descriptors only, retaining mandatory sync/error handling for files. Pipes still flush/close and require exact full reopened digest before any publication. This is a compatibility repair, not real cloud-provider or blocked-I/O acceptance.

One additional transaction case uses an actual deterministic dense 768×768 encoded PNG larger than 1 MiB. Injected availability refuses before copy or after 1 MiB, each with successful/failed owned cleanup; assertions cover transferred bytes, unchanged candidate, no publication, typed failure or protected indeterminate receipt. Actual private-file and pipe descriptors exercise availability/overflow/closed/unknown behavior and sync plus exact small-byte round-trip. This is not actual disk exhaustion. Transaction group12 and collector/mock agree; settings21/private20/renderer6 and other counts unchanged.

Local PASS: 235 matching locale keys/XML/catalog/manifest/string references/changed-file whitespace, 10 collector cases, 10 release-source cases/source checker and Bash syntax. One profile=current checkpoint now groups this implementation with a321b829’s create/write/cleanup/publication uncertainty changes and expanded SAF ownership assertions. Require exact-source build/lint/APKs, transaction12 and every existing final report guard. No individual-case/legacy/automatic-locale/JXL run, relaxed ownership/appearance gate, native export enablement, main/signing/publication or new permission. Android compilation/runtime of both blocks is pending that run. Latest previously accepted bounded source remains521e08cb; physical, real-provider/process-kill/quota/storage-pressure, usability and delivery gates remain open.


## Grouped01df3371 — scoped export safeguards pass, refusal fixture fails

[Actions40021226927](https://github.com/santirodriguez/FixMyShot/actions/runs/40021226927), exact01df3371a00f0838d975a4c8ec77842fafae4201/tree0be102ecf1d24105da0e3c749cf7a1243a26d68d, finished2026-10-23T20:53:09Z FAILURE. Build120645990886 SUCCESS/core/lint/debug/unsigned-release/test APKs (`BUILD SUCCESSFUL in6m17s`) and API36job120649225034 FAILURE; SAF120649227974 skipped/NOT RUN. Actual logs were read. Renderer6/cache3/private20/transaction12/share4/settings21/batch7/retry8/recovery16 and codec/native diagnostic groups passed. Both new provider-exception and low-destination-space/file/pipe cases passed. Export-flow4/5 passed; restartedViewModelRestoresAndRetriesOnlyTheFailedMediaStoreItem failed at222, expected failed count1 but observed0. Final source-bound report guards were NOT REACHED after the export-flow count assertion; aggregate not accepted. Latest complete bounded PASS remains521e08cb.

The failing RecordingPort deliberately threw IOException before calling its delegate, intending a proven no-output refusal. Under a321b829's conservative contract, any throwing create is uncertain because a real provider may already have allocated an unlocated output. The fixture now returns null at that known pre-delegate refusal boundary. Its existing assertions still require one failed/retryable item, restored frozen settings, preserved successful URI/source hashes and exactly two actual creations over three calls. The production throwing-create barrier and its passed transaction tests remain unchanged. This fixture correction has NOT RUN; no per-case CI was requested and the failed run is not relabelled green.

## Private candidate working-space reserve — accumulated implementation

PrivateOutputExecutor now checks the configured private candidate directory for at least 16 MiB available before payload temporary-file allocation and full decode/render. It rechecks before metadata rewriting and best-effort after encoder I/O failure. Known shortage returns PRIVATE_STORAGE_LOW with separate en/es/ca free-device-space guidance. Unknown statistics allow the ordinary bounded pipeline; copyDirectory-null isolated executors have no asserted directory preflight. This is a minimum working reserve, not encoded-size prediction, quota/reservation or actual disk-exhaustion acceptance. No source/provider deletion, automatic retry, quality/default/format/renderer/fingerprint/schema or permission change.

The existing padded-PNG pixel test adds an injected low-space execution before its ordinary successful render. It must preserve the exact original bytes/fingerprint and a pre-existing unrelated sentinel candidate, and allocate no new payload/candidate. Its original decoded border/source-pixel proof still runs afterwards. Test counts remain private20/transaction12/settings21 and all collectors unchanged. New executor/fixture assertions have NOT RUN. Local236 matching locale keys/XML/catalog/manifest/stringrefs/whitespace,10 collector/10 release-source cases/checker/Bash PASS. Both request markers/workflows are unchanged; validate this with a later coherent block rather than rerunning one fixture. Real-provider/process-kill/storage-pressure, hardware/native formats, actual UI/accessibility and delivery gates remain open.


## Deliberate finish-review confirmation — grouped checkpoint

The Results finish/review action now asks for confirmation before calling the existing acknowledgment path. Its existing en/es/ca notice explains removal of private restore/retry settings, or cleanup of a completely published session; saved public copies remain. Cancel, outside dismissal and Back leave the result/recovery state untouched. Confirmation is unsaved and keyed to the displayed result state, finish mode and external busy eligibility, so changed results or a newly busy action dismiss it. The button is disabled during import/native work, sharing or output verification; ViewModel acknowledgment guards remain authoritative. No journal, cleanup, retry proof, renderer, default, permission, signing or release change.

Source review and offline locale/catalog/manifest, collector and release checks are required locally. The grouped current-profile run must compile this Compose path together with the preceding48a2c11d private-reserve guard and known-refusal fixture correction, execute the existing complete API36 regression block and final source-bound guards. Instrumentation counts remain private20/transaction12/settings21/export-flow5/recovery16/renderer6. This simple dialog adds no test dependency or separate CI run. Actual dialog cancel/Back/confirm, rotation while open, verification/share eligibility, TalkBack and large-text acceptance remain a manual checklist, not an automated UI claim. Broader physical/provider/native-format and delivery gates remain open.


## Grouped7c832427 — accumulated storage/recovery checkpoint passes

[Actions40042133996](https://github.com/santirodriguez/FixMyShot/actions/runs/40042133996), exact7c832427b47cfa7006aeb99993b381f27df2a6b5/treecaae554261b98bdf658a5f2d7bbfba312977e1d2: build120715418716 SUCCESS (`BUILD SUCCESSFUL in4m53s`), API36job120717029432 SUCCESS, optionalSAF120717030990 skipped/NOT RUN. Actual build and runtime logs read. API36 completed2026-10-24T00:03:46Z. Renderer6/cache3/private20/transaction12/export-flow5/share4/settings21/batch7/retry8/recovery16 passed, as did80 codec encodes and all final source-bound smoke/native/grid/layout guards. The previously failed restartedViewModelRestoresAndRetriesOnlyTheFailedMediaStoreItem now passed with its proven-null refusal correction and unchanged two-actual-output/prior-success assertions. The private reserve assertions passed; the finish-review Compose path compiled, with manual dialog interaction still NOT RUN.

Smoke artifact12539952029 downloaded46323B/SHA256e26cbc8f32ddbbccb741d45759efb19a552552f2001db10bcc2bc3eec8d89cfe; size/ZIPCRC/exactsource and four local report validators passed. Page4096/grid5positive12attempts/layoutDIAGNOSTIC_COMPLETE remain diagnostic classifications, not physical/native-export/16KiB acceptance. Build12539628706 listed31671962B/providerSHA25671d029af941b6b26f242a5a4184513d9230473bdc9136be5ecd5da1169d132a5, not independently downloaded/rehashed/delivered. Latest complete bounded PASS is7c832427, superseding521e08cb; failed01df is not relabelled. No hardware/release or broad phase acceptance.

## Explicit preparation retry without a cosmetic edit — accumulated implementation

Before a job starts, a blocked/failed preflight or refused destination previously left Export disabled until the user edited the draft. ReviewViewModel.canPrepareExport now owns eligibility for both the button and prepareExport. It allows a deliberate full recheck in IDLE/BLOCKED/ERROR while requiring checked history, no active/review token, no acknowledgment/output action, no busy/share/queued import, an available nonempty draft and no previous attempt of that exact draft. Existing-job remaining-item retry remains separate. The action does not clear blockers, mutate settings, create output before successful preparation/destination authorization, discard a receipt or restart automatically.

Existing ReviewExportFlowTest cases now check refused destination -> unchanged draft/zero creations -> explicit preparation/cancellation/success, blocked preflight -> repeated rejection/unchanged draft, and published/uncertain results -> no fresh preparation. Counts stay export-flow5 and all collectors unchanged. Source/offline checks are required; these new eligibility assertions and production changes have NOT RUN on Android and do not inherit7c832427 acceptance. Both request markers/workflows remain unchanged; accumulate the change for a later coherent checkpoint. Physical UI/provider/storage-pressure/native-format and delivery gates remain open.


## Share payload without repeated provider metadata I/O — accumulated implementation

ExportShareIntents previously called ClipData.newUri, causing a fresh provider MIME lookup after the ViewModel had already verified each committed receipt and current output digest. Payload construction now has no ContentResolver argument and directly constructs ClipData from the selected URI list and distinct verified MIME types. Intent uses the exact shared type or image/* for mixed PNG/WebP; ClipDescription advertises all selected types. Preserve explicit selection/order, inclusive100 cap, URI/duplicate/format rejection, matching EXTRA_STREAM/ClipData and only temporary read permission. Receipt identity, PUBLISHED eligibility and fresh byte verification remain unchanged. The builder performs no provider I/O and cannot repair a lost read grant or promise receiver success if the provider later disappears.

Existing ExportShareIntentsTest cases now assert exact single/mixed ClipDescription types, first/second URI order, only the read flag and the complete ordered100-item payload before rejecting101. Synthetic URI payloads require no provider or resolver. Counts remain share4 and all existing groups/collectors unchanged. New production/call-site/assertions have NOT RUN on Android; exact-source7c832427 remains the latest complete bounded PASS. Local source/locale/catalog/manifest, collector and release checks are required. Accumulate this with bfdb823f preparation retry for a later coherent current-profile checkpoint; request markers/workflows remain unchanged. Actual receiver/grant-revocation/UI/provider-blocking acceptance remains open, as do physical/native-format and delivery gates.


## Grouped preparation and sharing checkpoint — requested after99db9cff

One current-profile checkpoint groups bfdb823f's deliberate pre-attempt preparation recheck with99db9cff's resolver-free verified share payload. Require exact-source core tests, lint/debug/unsigned-release/test APK build and the complete API36 regression block with existing final report guards. Export-flow5 retains blocked/destination-refusal/draft-preservation/no-allocation and no-fresh-export-of-attempted-output assertions; share4 retains exact URI order/extras, all distinct verified MIME types, read-only flags and inclusive100/exclusive101 boundary. No test count, dependency, permission, output verification, codec route, fingerprint, journal or workflow change. No per-case rerun or weakened guard. Actual new Android results remain pending; latest complete bounded PASS is7c832427. Receiver/grant revocation, physical UI/native-format/storage-pressure and release gates remain open.


## Downloaded7c832427 build artifacts — independent byte inspection

Build ZIP12539628706, source7c832427b47cfa7006aeb99993b381f27df2a6b5, was later downloaded:31,671,962 bytes/SHA25671d029af941b6b26f242a5a4184513d9230473bdc9136be5ecd5da1169d132a5. ZIPCRC/exact embedded source and reconstructed193-blob Git treecaae554261b98bdf658a5f2d7bbfba312977e1d2 passed. All three archived APK hashes match evidence/apk-sha256.txt. Debug25,085,921 bytes/SHA256eb067019035007582fda15c56d2e1c305d2919f305edc81b41733d7d96754e4c; unsigned release23,495,607 bytes/SHA256a809abf2fab3ff8cc7cb67c0337a72e423470cef35b2e3302060fd601bad1c1d. Neither was delivered as a new application build; the supplied APK remains older639c.

The source-verified check-apk script was re-executed over the actual debug/unsigned-release APK bytes. Both exactly match their archived native reports:9 libraries each, arm64-v8a/armeabi-v7a/x86_64 and ELF/ZIP16KiB alignment PASS. This is byte inspection, not16KiB runtime, signature, install/upgrade or independent build acceptance. Four archived JVM XML suites report Draft20+ExportPreparation12+Contract23+Geometry14=69 tests,0 failures/errors/skips. Lint XML contains0 errors/46 warnings/2 hints; these include version-update/style/plural/icon/platform suggestions and an application-context field warning, not a clean-warning or accessibility verdict. No speculative dependency migration/icon work was added.

The later trust-candidate capture is514 components/921 artifacts with0 PGP signatures, all308 locked coordinates covered and current lockfiles byte-identical. See [committed inputs](feasibility/committed-gradle-inputs.md) for the exact generated-only status and baseline comparison limitation. Dependency authentication, strict verification, independent rebuild and physical/native-format/receiver/UI/signing/distribution gates remain open. This historical artifact check does not certify the new773b34f0 preparation/sharing checkpoint, which has its own exact-source CI.


## Grouped773b34f0 — preparation and verified-sharing regressions pass

[Actions40047858334](https://github.com/santirodriguez/FixMyShot/actions/runs/40047858334), exact773b34f0f3eebb124cb415da41811aede369ecf9/tree3574454a5e70607765a648eb33880777de1fb3ed, completed SUCCESS. Build120733566836 SUCCESS (`BUILD SUCCESSFUL in6m5s`), API36job120735330563 SUCCESS/completed2026-10-24T01:12:21Z; optionalSAF120735332574 skipped/NOT RUN. Actual build/runtime logs read. Renderer6/cache3/private20/transaction12/export-flow5/share4/settings21/batch7/retry8/recovery16,80 real codec encodes and final source-bound smoke/native/grid/layout guards all passed. This includes bfdb823f preparation refusal/recheck/cancellation/no-duplicate barriers and99db9cff exact single/mixed MIME, URI order/read-only flags and100/101 share payload boundaries. Later44411dad changed documentation only; production source remains identical.

Smoke12542576471 downloaded46330B/SHA2563378f6e11f242c2be99dd5e3579dd77932bb37210cd4a457cf970fa5750061d2: ZIPCRC/exact773 source/four local report validators PASS. API36/page4096/grid5positive12attempts/layoutDIAGNOSTIC_COMPLETE remain diagnostics, not native-export/physical/16KiB acceptance. Build12542905033 only listed31676803B/providerSHA2569fdd171c785a2e766b492403b57a4d087a8487b08b3ed9967087553f30224134; not independently downloaded/rehashed/delivered. Latest complete bounded Android PASS is773b34f0, not a broad phase/hardware/release acceptance.

## Offline build-source binding — host-only validation

The existing Gradle-candidate auditor now reconstructs all regular file blobs, executable modes and Git tree order, supports paired independent expected commit/tree pins, and compares archived tracked wrapper/lock bytes to their separately captured build inputs. It rejects unsafe/duplicate/conflicting/linked/special/oversized inputs. Observational mode never claims source identity; old untracked wrapper/lock captures are explicitly unverified and cannot pass pinned mode. No archive code is extracted or executed by the auditor.

13 offline tests passed, including an independently obtained git write-tree fixture reference and adversarial identity/input/path/mode/limit cases. The actual7c ZIP passed pinned mode with193 files/treecaae554261b98bdf658a5f2d7bbfba312977e1d2 and all tracked build inputs matching; dependency publisher authentication remains false/generated-only. This is not independent compilation, APK compile provenance, production signing or delivery acceptance. Local locale/catalog/manifest/collector/release and wrapper guards are required; no new Android CI/marker/workflow, application behavior, dependency or trust policy change. Physical/native-format/receiver/UI and delivery gates remain open.


## One pinned independent unsigned build — requested aftercd650fb2

The new independent-rebuild workflow is opt-in through its separate JSON request, with contents:read only, pinned checkout/setup/upload actions, one fresh ubuntu-24.04 job and no emulator/signing step. It preserves a validated controller/request outside the checkout, checks out exactly773b34f0f3eebb124cb415da41811aede369ecf9/tree3574454a5e70607765a648eb33880777de1fb3ed, requires pristine source/empty Gradle and project caches, then runs core tests and unsigned assembleRelease using --no-build-cache --rerun-tasks. Recorded source, tool versions, generated metadata, actual unsigned APK/native inspection and JSON comparison remain available even on mismatch. A differing APK fails after the report is written. The ordinary Android request/workflow is unchanged.

The independently downloaded baseline ZIP12542905033 passed size31,676,803/SHA2569fdd171c785a2e766b492403b57a4d087a8487b08b3ed9967087553f30224134/CRC/exact-source and pinned-source audit193/tree3574454a. Actual unsigned APK23,495,607B/SHA25666ae94204ff3de3f76936dbf7f3f96245783e15dfbb89a995297b0ff4e6b4738 matches its receipt and passes9-library/three-ABI/ELF-ZIP inspection. This is not production signature/16KiB runtime/delivery acceptance; no new APK was supplied to the user.

Six offline Python/Git controller tests passed: strict request schema/pins/limits/duplicate fields, exact pristine source, empty cache/output requirements, exact/different bytes with truthful open gates, mismatch CLI nonzero with retained JSON and dirty/symlink/invalid-control refusal. Local source/locale/wrapper/trust-audit/collector/release and YAML checks are required. The actual fresh build and final byte comparison later passed Actions40054940065; the exact receipt below supersedes that requested-state boundary. Most recent full application regression remains773b34f0/40047858334; this opt-in rebuild is a different evidence gate, not a replacement regression. Dependency authentication, strict verification, physical/native-format/receiver/UI, signing/install-upgrade and distribution remain open.


## Native-source tag and actual packaged-notice crosscheck

CMake's pinned libwebp4fa21912338357f89e4fd51cf2368325b59e9bd9 matches the official Gitiles/GitHub mirror tree3a7830b9713d9187cf1211862d4571ff58b0d507 and annotated v1.6.0 tag objectb7e29b9d75bd31422b00c2a446d49d7af06c328d. The commit is reported unsigned; the tag signature is unknown_key, so authenticated key identity remains open. The immutable upstream COPYING/PATENTS bytes were compared against committed assets and actual assets/licenses/libwebp entries in the downloaded773 unsigned APK: exact match,1496/1436 bytes, SHA2565aec868f669e384a22372a4e8a1a6cd7d44c64cd451f960ca69cc170d1e13acf / cc3273e0694ea5896145e0677699b53471b03ea43021ddc50e7923fbb9f5023c. See the [native provenance receipt](feasibility/phase1-batch-preflight.md) for immutable primary references and scope limits.

No native/application/dependency/notice/permission change and no new validation request follows this read-only observation. It does not establish current vulnerability status, all transitive license completeness, signing-key identity, Maven publisher authenticity or independent compilation. The separate one-job rebuild passed Actions40054940065 with independently downloaded identical APK bytes; its exact receipt follows.


## Independent773 unsigned rebuild — downloaded identical bytes

[Actions40054940065](https://github.com/santirodriguez/FixMyShot/actions/runs/40054940065) and job120755741931 SUCCESS, workflow/controller eef2ddae50f30361e0aca664e11558e59026652d, compiled source773b34f0f3eebb124cb415da41811aede369ecf9/tree3574454a5e70607765a648eb33880777de1fb3ed. Actual logs confirm source/pristine/cache guards, fresh Gradle user home, no restored caches, --no-build-cache --rerun-tasks, core tests and unsigned release in3m25s. No emulators/signing were run. The ordinary Android regression workflow/marker remained unchanged.

Downloaded artifact12544544566:14,742,573B/SHA25609ef3a0b1d2aaf995baa4eb4ddbdb6c533ebcd7106deb5d5f836ed597f20cb75; ZIPCRC/source/controller/report/reconstructed193-file tree and tracked lock bytes PASS. JSON byte_identical=true was independently checked: unsigned APK23,495,607B/SHA25666ae94204ff3de3f76936dbf7f3f96245783e15dfbb89a995297b0ff4e6b4738, direct byte-for-byte equality with the original downloaded773 APK. Archived JVM XML sums69 cases/0 failures/errors/skips; native report has9 libraries/three ABIs/alignment PASS. Generated capture514/921/0 PGP and six critical hashes match the independent baseline, with issuer authentication still OPEN.

Scoped unsigned reproducibility for this pinned source under the tested runner/toolchain layout PASSED. This is not all-host portability, reviewed dependency authentication/strict verification, production signature, physical/native-format/receiver/UI/install-upgrade/distribution or broad phase acceptance. Source-tag/packaged native-notice observations are separate. Latest full application regression remains773b34f0/40047858334; no current app source, key, version, dependency, permission or publication change follows this receipt. Supplied APK remains older639c; neither unsigned candidate was delivered as a new install.


## Critical publisher-evidence collection — no application build

One separate opt-in dependency-provenance workflow captures the six exact critical baseline773 artifacts from canonical Google Maven/Maven Central URLs, optional checksum/signature files, Kotlin2.2.21 publisher Maven-checksum manifest pinned to release API asset307650744/SHA256791a6bf5ff0404afe4f8eaf903faa6db5f97652f4d1e79b9795404f3111bb3c8 and JUnit's publisher KEYS at05fe2a64f59127c02135be22f416e91260d6ede6/SHA256fcb938a928b643fb76985f9ce3634d9dad170dfe6c1b276e920844822538d5f7. This is a data-only read job, not an Android/Gradle/emulator or signature-generation run; downloaded files are not executed.

Six offline tests passed: capture/published-hash matches remain unauthenticated; wrong pins fail with retained report; publisher checksum mismatches are not ignored; absent signatures remain explicit; unsafe origins/redirects/IDs/limits/duplicates fail; oversized data/existing output cannot produce acceptance. Default collector fields authenticated=false and cryptographically_verified=false remain truthful until separate review. Actual retrieval completed40058428491; the separately reviewed subset below records the signature/manifest results. Local source/locale/JSON/YAML/Bash checks are required. Existing application source/version/dependencies/permissions/trust metadata and ordinary Android/rebuild requests are unchanged; last full application regression773 and independent identical unsigned rebuild remain accepted within their exact scopes.


## Publisher evidence and partial verification candidate — actual result

Data-only Actions 40058428491/control fa1cc608ac2d07a22525c7170eb7683d1eda7880/job 120766462087 SUCCESS, completed 2026-10-24T03:28:33Z. Actual logs and downloaded artifact 12546357394 / 34,257,328 B / SHA256 126b1c20b6529ed3ed619353fb4fbd13d0d3486fd6a5429787b3276e9a474ab8 passed size/ZIPCRC/control/baseline/file pins. Six collector tests ran; all eight required files matched their independent pins, all six published checksums matched and five raw signatures were captured. No SDK, Gradle, emulator, downloaded code execution or trust activation occurred in that job.

The pinned Kotlin release manifest matched 74 exact captured Maven coordinate/file SHA256 values; actual JUnit 4.13.2 JAR 8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3 passed gpgv VALIDSIG with publisher KEYS fingerprint FF6E2C001948C5F2F38B0CC385911F425EC61B51 in a temporary public-only keyring. No user/secret key or keyserver was used. The offline reviewer, 5 boundary tests and real altered-manifest/JAR rejection passed. Publisher manifest/KEYS/sig hashes and immutable primary anchors are retained in [committed inputs](feasibility/committed-gradle-inputs.md).

The initial reviewed subset contained 75 of 921 exact captured artifacts; the separately recorded Google review below expands the current non-active candidate to 78, with 843 remaining and complete_for_baseline=false. The candidate file does not activate strict Gradle verification or signature checking during builds; the JUnit signature was independently checked offline and the Kotlin rows use the separate publisher release checksum source. No group-wide trust, automatic generated-hash approval, current vulnerability verdict or broad G1/release acceptance. Remaining issuer/metadata/security and physical/native-format/receiver/UI/signing/install-upgrade/distribution gates remain open. No additional CI run follows this offline review; application regression 773 and identical independent unsigned rebuild remain accepted within their exact scopes.


## Google public-key evidence — bounded request

The data-only request now fetches only https://dl.google.com/linux/linux_signing_key.pub, maximum 128 KiB, with full primary fingerprint EB4C1BFD4F042F6DDDCCEC917721F63BD38B4796 independently published at https://www.google.com/linuxrepositories/. This is not a Maven artifact authentication decision. Nine offline collector cases cover original checksum boundaries plus primary/subkey separation, wrong/malformed/oversized pins, parser failure and truthful unauthenticated reports. Actual key retrieval and captured-signature binding review completed in the result below; the partial candidate now contains 78/921 exact files.


## Google signature subset — actual result

[Data-only Actions 40060745763](https://github.com/santirodriguez/FixMyShot/actions/runs/40060745763), control c2a930310c3aebc2f2a748f1a7863ce49a009a4f / job 120773484280, SUCCESS in 7 seconds, completed 2026-10-24T04:02:31Z. Actual logs and downloaded artifact 12547168236 / 15,849 B / SHA256 118d7726ba635ed219b699989079e7838ef84d91ddfa35e3c166ac8df4f1d730 passed ZIP CRC, control/request/baseline and key-file checks. The sole fetched file was the 19,640-byte Google public key bundle, SHA256 54dea5f6c2a26091578cf52a999cebc6b64df478d37ad4dce96376b711e3b27c; primary fingerprint matched the Google-published anchor. Collector fields remain unauthenticated.

Actual detached signatures for AGP 8.13.2 JAR, HeifWriter 1.2.0-rc01 AAR and Activity Compose 1.11.0 AAR each passed gpgv exit 0 / VALIDSIG bound to primary EB4C1BFD4F042F6DDDCCEC917721F63BD38B4796. Signers were 0F06FF86BEEAF4E71866EE5232EE5355A6BC6E42 (AGP/Activity) and 0E225917414670F4442C250DFD533C07C264648F (HeifWriter); all use SHA256 signatures. Historical expired unused subkeys were reported separately; neither actual signer was expired/revoked and no failing signature status occurred. Verification used temporary public-only keyrings, no user/secret keys, agent, keyserver, signing or trust activation.

Nine collector and eight reviewer boundary tests passed, plus real publisher-bundle parsing, real signature verification and refusal after a one-byte mutation of the actual HeifWriter AAR. Exact signatures/statuses/key identity are in gradle/dependency-provenance-review.json. The current XML/JSON subset contains 78/921 exact files, 843 pending, active policy unchanged and vulnerability review OPEN. Application/build inputs and accepted full Android773 / independent identical unsigned rebuild remain unchanged; no additional Android/emulator run followed this offline review.


## First larger Google batch — actual collection and review

[Actions 40061369748](https://github.com/santirodriguez/FixMyShot/actions/runs/40061369748), control be41345736cc65642c81c9e12b9c9a083d298dfd / job 120775403769, SUCCESS, completed 2026-10-24T04:12:25Z. The job took 45 seconds; collection took 37 seconds. No SDK, Gradle, emulator or downloaded-code execution. Actual logs show 11 collector cases passing, truthful unauthenticated defaults and no capture error. Artifact 12546604299 / 30,429,102 B / SHA256 3f096b69f4ea3ccf3927d61879282d0ff44b2ab591df541d1877330d10b308fd passed ZIP CRC, source/request and all 96 exact capture pins; 74 signatures captured, 22 HTTP404.

The offline reviewer pins source request SHA256 d6afd620fee71b219c097204c76448ac24ae5b9b6376cc0e2e3f187bcbff0963 and prior accepted 78-file receipt SHA256 9a4ea0f5f82fa438c74b4487299a7b2bcfa820ab459b34733adfa819de62e8c6. Actual signatures, primary/subkey binding, strong digest and explicit public-bundle signer-validity checks accepted 55 new files: 19 AAR, 19 JAR, 14 module and 3 POM. Nineteen signatures remain unverified with the inspected key; 22 are absent. Neither category is approved or automatically retried. All 96 attempts/outcomes are recorded; the collector never authenticates files itself.

Gpgv does not enforce key expiry/revocation itself. Separate public-bundle inspection now rejects an expired/revoked/invalid actual signer or primary and inconsistent key/signature times. Nine verifier, five batch-review, four planner and eleven collector cases passed, alongside source/236-key/release-metadata checks. The original 78 proofs were rechecked with the same identities; changed real batch bytes and the actual signer after its advertised expiry were refused. No online revocation/current vulnerability verdict.

After that first batch, JSON/XML contained 133/921 files, 788 remaining, inactive. Base-producer and current batch/verifier source hashes are retained separately. A real next-plan check omits all 96 attempted identities until explicit reconsideration. Application regression773 and the identical independent unsigned rebuild retain their exact scoped acceptance. Application/build inputs and normal validation request remain unchanged; no further CI follows this offline review.


## Second larger Google batch — actual collection and review

[Actions 40062206602](https://github.com/santirodriguez/FixMyShot/actions/runs/40062206602), control 81af60aa7ddee8d71dedb217d178f1c6599022de / job 120777943158, SUCCESS, completed 2026-10-24T04:24:55Z. The job took 30 seconds; collection took 25 seconds. No SDK, Gradle, emulator or downloaded-code execution. Actual logs show 11 collector cases passing, unauthenticated defaults and no capture error. Artifact 12547517709 / 6,319,600 B / SHA256 a8b77a2438fcda6e2c2c6f413e9ae5fdac8620860b5c0d40b6ae48354289d221 passed ZIP CRC, source/request and all 96 capture pins; 82 signatures captured, 14 HTTP404.

The reviewer pins request SHA256 89c768f9f5ba2fceee1f995c43200df5aa914315896b8e0cff4e474d8deb675d and prior accepted 133-file receipt SHA256 288ea7b181f487857833e8f8b10fbdffeedc6207a523057d36f86dde4c5e1e50. Actual detached signatures, primary/subkey binding, strong digest and pinned-bundle key validity checks accepted 77 new exact files: 17 AAR, 14 JAR, 24 module and 22 POM. Five signatures remain unverified with the inspected key; 14 are absent. These 19 attempts are deferred; neither category is approved or automatically retried. All 192 attempts across both larger batches are recorded without duplicates, including 60 deferred outcomes.

The prior 133 artifact/proof identities remain unchanged. Real altered batch bytes and a wrong control identity were refused; fresh review reproduces both JSON/XML outputs. Source/236-key/release-metadata and per-file count/hash checks passed. A real next-plan check excludes all 192 attempted identities. After this second batch: 210/921 reviewed, 711 remaining; active Gradle policy unchanged, current security/license review open. Application regression773 and the identical independent unsigned rebuild retain their exact scoped acceptance. No application/build input or normal validation request changed; no further CI follows this offline review.


## Third larger Google batch — partial capture and actual signature review

[Actions 40097278077](https://github.com/santirodriguez/FixMyShot/actions/runs/40097278077), control f60178606ad269097ecbf9740361bbc5f7fd95d9 / job 120882057272, completed FAILURE at 2026-10-24T13:14:54Z (27 seconds; 19 seconds collection). Eleven collector cases passed. The collector correctly exited nonzero with all_required_pins_matched=false: 87 exact files matched, one protos-31.13.2.jar capture returned FETCH_OR_VALIDATION_ERROR. No SHA256_MISMATCH was reported. No Android build or emulator ran; the failed data capture is not relabeled as a successful job.

Retained artifact 12561213357 / 9,231,510 B / SHA256 ae1876cecab60a3349aa2662bec817664f1945f3fda3af234324bfc14b04f07d passed size/ZIP CRC/control/request and every matched-file pin. Request SHA256 e60ec6979cbd0fb7e9dd6585d9c67611e53c9d4d6e5bbceb290f23b1e94ad2ff; prior reviewed 210-file receipt SHA256 8f6810b272af7f5b69309ea59ad0dd9a62b8edd131199eea6f37b3dc6906b858. Captured signatures were verified independently, accepting 63 new files (13 AAR, 17 JAR, 17 module, 16 POM). Thirteen signatures are absent, eleven remain unverified, and one capture is unavailable; all 25 remain pending without automatic retry.

The previous 210 artifact/proof identities are unchanged. Fresh review reproduces JSON/XML; actual changed batch bytes and a wrong control identity are refused. Source/236-key/release-metadata and current producer/count/hash checks passed. Current candidate after this batch: 273/921, 648 remaining, inactive. All 280 attempts, including 85 deferred outcomes, are recorded without duplicate identities. No online revocation/current security verdict; app regression773 and the identical independent unsigned rebuild retain only their original scoped acceptance.

A separate 128-entry next request was checked against the stable baseline, prior approvals/attempts, canonical routes and existing size/count limits. It combines 46 JAR and 82 module files and excludes all 280 attempts/all 273 approved identities. Workflow path filters were checked: its staged filename triggers no Android, collection or rebuild run. The active request remains the completed 88-file capture; no retry or additional CI follows the offline review/staging step.


## Combined remaining Google route — partial capture and actual signature review

[Actions 40098770322](https://github.com/santirodriguez/FixMyShot/actions/runs/40098770322), control 45b620e855ea1324cf07d1c03e443e30b3dc9269 / job 120886424226, completed FAILURE at 2026-10-24T13:35:49Z (39 seconds; 32 seconds collection). Eleven collector cases passed. The collector truthfully exited nonzero: 123 files matched, five captures returned FETCH_OR_VALIDATION_ERROR, all_required_pins_matched=false. No SHA256_MISMATCH was reported. Failed captures were builder, bundletool, intellij-core, kotlin-compiler and lint-checks JARs; exact coordinates are retained in the review JSON. No retry or limit increase occurred.

Retained artifact 12561454540 / 9,986,707 B / SHA256 4e387bbcd4b6ff703c35557d992d7179f37a3936bdfa3df113a700b696864863 passed size, ZIP CRC, source/request and all matched-file pins. Request SHA256 40f06ba4d2dac6f860aa72c3f368760a75ab5ee100ccb10a8e44cee82c394865; prior accepted 273-file receipt SHA256 4c2f67aba06d5e4a266fa25923d819665c539d7821ea2c8b00eee8ad1e460da0. Actual signatures, inspected-primary binding, strong digest and key-validity checks accepted 95 new files: 38 JAR, 57 module. Sixteen signatures are absent, twelve remain unverified and five captures unavailable; all 33 remain deferred.

All 273 earlier artifact/proof identities are unchanged. Fresh review reproduces JSON/XML; actual changed batch bytes and a wrong control identity are refused. Source/236-key/release-metadata, current producer and per-file count/hash checks passed. After this batch: 368/921 reviewed, 553 pending, inactive. There are 408 unique Google-route attempts and 118 deferred outcomes; the planner confirms no unattempted Google-route files remain. The three previously verified critical files are separate from those 408 attempts. No online revocation/current vulnerability verdict, app/build-input change, Android rerun or broad release acceptance.

At that checkpoint two Kotlin publisher-manifest anchors were staged; the completed collection and review below supersede that pending state.

## Kotlin manifests and dependency security — one combined data checkpoint

[Actions 40101103449](https://github.com/santirodriguez/FixMyShot/actions/runs/40101103449), control `64745e1f88df2ecf488f1fc650854f186f6abe7a` / job `120893241464`, completed SUCCESS at 2026-10-24T14:08:00Z in approximately 24 seconds. Eleven collector and five security-inventory boundary cases passed in the job. Both official Kotlin assets matched their separately inspected SHA256/size pins; the six OSV batches queried all 514 Maven components plus the exact native WebP commit, fetched 54 advisory records and reported no collection error. No SDK/build/emulator ran.

Artifact `12563041227`, 218,443 B / SHA256 `7b6326aa2630771de0a8b8f4819e2c576e843f68fe6d93b177ff3ad2bc9857c7`, passed size/CRC, unique safe regular-file paths, exact control/request/source and capture checks. The ZIP is retained under `docs/feasibility/evidence/dependency-review-64745e1f.zip`; hosted retention is not its only owner.

The new manifest reviewer accepted **11 exact files** (9 Kotlin 2.2.0, 2 Kotlin 2.2.20; 5 JAR, 3 module, 3 POM), preserving all 368 earlier artifact/proof identities. Current candidate: **379/921 reviewed, 542 pending**, inactive; no active Gradle verification file was introduced. Seven offline manifest cases cover pin/control/source changes, unsafe/duplicate paths, exact-path scope, conflicting hashes, unavailable captures and preservation of previous proofs.

Offline security replay validated all 68 raw request/response receipts and exact component order/scope/results. It found **25 matching Maven components / 54 advisory IDs**: 18 components selected in tooling/other configurations and 7 without project-lockfile selection. The 132 app release-runtime components and native commit had no database match at the recorded time. Lockfile absence does not prove a buildscript/plugin dependency unselected: source review explicitly confirms the Kotlin plugin selected. Its referenced fix changes KAPT cache deserialization; inspected Gradle/catalog files apply no KAPT or annotation processors. This is a limited applicability inference, not closure of the tooling/security gate.

Five inventory and four replay boundary cases cover source pins, native identity, scope, unpaired no-network skips, deadlines, incomplete/paginated results, false-clean conclusions and withdrawn advisories. Full triage, source references, raw/derived hashes, grouped remediation actions and reproduction commands are in [dependency security review](feasibility/dependency-security-review.md). No application/dependency/lock/build-input or ordinary validation marker changed. Android773 and the independent identical unsigned rebuild retain their exact original scope; security, strict verification, native trust, complete licensing and physical/release gates remain open.


## Runtime publisher evidence — recovered and reproduced offline

The data-only Actions40118368540 at exact control04a8aa0252d69b68bec6bbb1197f075b1f7beb2a
finished FAILURE/partial capture, not an Android regression failure. Artifact12570037960
(2,152,718 bytes; ZIP SHA256019f31f7526cbafc2b3d4da5e352bdb89334edd05954b24e9bafb655bcddfdc3)
is now retained as `feasibility/evidence/runtime-publishers-04a8aa02.zip`.
All59 unique safe regular members/CRC and exact control/request/baseline pins pass.
27of30 artifact captures match; three metadata aliases returned404. Both public
primary captures match. The unpaired security request skipped without network.

Recovery replays real detached signatures and keeps all379 earlier artifact/proof
records unchanged. Eight Kotlin JAR/POM files pass;16 files have no independent
issuer anchor, three signatures are unverified and three captures unavailable.
The Square primary-only response is insufficient; it receives no approval.
The inactive JSON/XML now contain387of921 exact files,534 pending. Full identities,
hashes and limits are in [committed inputs](feasibility/committed-gradle-inputs.md);
[retained evidence](feasibility/evidence/README.md) owns reproduction commands.

Executed recovery checks:8 runtime-review,12 collector,9 original-verifier,
4 planner,5 batch-review and10 release-source cases PASS. Actual altered Kotlin
JAR and wrong-primary cryptographic refusals PASS. Replaying the exact capture
reproduces JSON SHA2569c40b9db7aea62179c8351b1354827e38f8ef33b277d4fb5b47cbdddf9db0b26
and XML86026d319ab236ea5b733844a99478b08867ab6fa891f3da5f7de5e9461f3aa3.
This closes the interrupted persistence work; no active trust policy is introduced.
No app/build/lock/dependency/Android marker changed, so Android was not rerun.
Exact773 acceptance and its independent identical unsigned rebuild keep their
original limits. No physical/manual/signing/distribution gate is newly accepted.


## Canonical legal packaging — implementation awaiting grouped build

Inspection of the exact773 unsigned APK from artifact12542905033 confirmed
WebP COPYING/PATENTS and five AndroidX LICENSE.txt entries, but no explicit
project GPL LICENSE, THIRD_PARTY_NOTICES inventory or canonical Apache text.
The original build ZIP SHA256 is
9fdd171c785a2e766b492403b57a4d087a8487b08b3ed9967087553f30224134;
unsigned APK SHA25666ae94204ff3de3f76936dbf7f3f96245783e15dfbb89a995297b0ff4e6b4738.
This is a package-content finding, not a conclusion about all third-party
license obligations. The old APK correctly fails the new legal-assets checker.

prepareLegalAssets now generates assets/legal/ from the canonical root LICENSE,
THIRD_PARTY_NOTICES and LICENSES directory. Existing WebP assets remain.
The Android workflow checks actual debug/release package bytes against the
exact checkout, with separate JSON receipts. Four synthetic offline cases pass:
complete exact bytes without full-review approval; missing/duplicate entries;
same-size changed bytes; canonical-source changes invalidating an older package.
No downloaded code or license fixture is executed.

Gradle compilation, generated-task execution, actual updated APK packaging and
the grouped Android regression checkpoint are NOT RUN for this change. Earlier
773 unsigned byte-identical rebuild acceptance remains historical and does not
transfer to these changed build inputs. The next grouped checkpoint must inspect
the new APK bytes and refresh the independent rebuild before delivery. Full
transitive notices/licenses and physical/signing/distribution gates remain open.


## Grouped legal/tooling checkpoint — requested

The explicit current-profile request groups the canonical legal packaging at
3cd2e909 with source-bound buildscript ownership reports for root/app/core/native.
The build runs the existing core/lint/three-APK work and API36 regressions; no
dependency versions, locks, legacy profile or native availability are changed.
Build evidence must contain debug-legal.json, release-legal.json and the plain
buildscript-dependencies.txt report with its SHA256, in addition to the existing
source, APK and native receipts. A requested run is not acceptance. Inspect actual
reports and terminal jobs; buildscript selection is not executed-code or app
reachability evidence. Security remediation and independent rebuild remain open.


## Accepted grouped legal/tooling checkpoint — ad075068

[Actions40142693059](https://github.com/santirodriguez/FixMyShot/actions/runs/40142693059)
finished SUCCESS for exactad075068564dc8a7d8d0eca35fc1f69ff3d16464:
build121017552767 and API36121018857157 passed; optional SAF was skipped/NOT RUN.
The [compact receipt](feasibility/evidence/android-ad075068.json) records exact
archive/APK/source/report identities. Both downloaded ZIPs match provider size
and SHA256 and pass CRC. The build archive reconstructs the inspected228-file
Git tree and matches tracked build inputs/wrapper/locks. Generated514/921
dependency inventory is unchanged, with no newly activated trust.

Core69 tests have zero failures/errors/skips. Lint and three-APK build pass.
API36 renderer6/cache3/private20/transaction12/export-flow5/share4/settings21/
batch7/retry8/recovery16 and80 real PNG/WebP encodes pass. All four exact-source
smoke/native/grid/layout guards replay locally after recovering full evidence;
page size4096, grid5positive/12attempts and layoutDIAGNOSTIC_COMPLETE preserve
their existing diagnostic limits. This is not physical codec or16KiB runtime
acceptance.

Actual debug and unsigned release APKs independently pass the legal checker
against canonical bytes recovered from the exact archived source: five files
each, with full-license-review=false. The canonical Apache file also contains
upstream Gradle component-license appendices; its inclusion does not establish
that those Gradle tools are packaged runtime components. Native inspection of
both APKs passes9libraries/3ABIs/ELF+ZIP16KiB alignment. The actual unsigned APK
is23516325B/SHA2562ae8f0a8b2705fbf9e61d4bddf3933f22489cce0906225d573175f2f19dd7e27.

The retained [buildscript report](feasibility/evidence/buildscript-ad075068.txt)
matches its generated SHA256 receipt. Interpretation is owned by the security
review. Independent rebuild40147246792 is a separate pending gate; do not
transfer earlier773 byte equality. No new APK was delivered or signed. Physical,
complete publisher/security/license and distribution gates remain open.


## Independent ad075068 rebuild accepted; migration experiment prepared

Actions40147246792/control033a03929140a806e49402978b49f2c885dc444e finished
SUCCESS. Downloaded artifact12580449027 is17345839B, SHA256
6e6e2da466f7cf52b157287f5caa130b3e92e424a91da4668a3df1182ae80b29; size,
ZIP CRC, exact request/source and actual unsigned APK bytes match. The retained
[comparison receipt](feasibility/evidence/independent-rebuild-ad075068.json)
reports true byte equality:23516325B/SHA256
2ae8f0a8b2705fbf9e61d4bddf3933f22489cce0906225d573175f2f19dd7e27.
The inspected workflow requires pristine exactad075 source, empty Gradle/project
caches, no-build-cache/rerun-tasks and core/release rebuild. This supersedes the
previous pending status for that source only, with no signing/physical/security
approval.

The [approved migration plan](decisions/toolchain-migration.md) now owns the
AGP9.2.1/Gradle9.4.1/Kotlin2.4.21 experiment. Four local recipe refusal tests,
current bootstrap/wrapper checks and workflow YAML/permissions checks PASS.
Migration compatibility/build/graph/security results are NOT RUN until the exact
opt-in workflow finishes. Its mutated checkout is experimental derived source;
normal frozen-source/API36/independent acceptance is a later separate checkpoint.


## Migration recipe1 failure and recipe2 correction

Actions40149344439/control6c920674 is FAILURE; app configuration rejected old
SourceSet Provider registration before wrapper generation. Compilation was
NOT RUN. The62089B partial artifact12582287135 passes exact SHA256/CRC/unique
safe-member/control checks; its compact [receipt](feasibility/evidence/toolchain-migration-6c920674.json)
and [migration plan](decisions/toolchain-migration.md#recipe2-generated-source-registration-correction)
own the diagnosis/correction. Recipe2 uses typed per-variant generated tasks and
Variant API resource/asset wiring, retaining exact canonical PNG/legal bytes and
APK paths without a compatibility bypass. Four offline recipe refusal cases
still pass. New Gradle configuration, generator execution and package contents
remain unaccepted until the corrected experiment is reviewed.


## Migrated inputs incorporated — frozen checkpoint requested

The corrected experiment40152344573 passed; see the
[exact derived-input receipt](feasibility/evidence/toolchain-migration-afea6e45.json).
The known wrapper JAR checksum and generated launcher hashes now govern normal
execution. All three reviewed lockfiles and migrated source files are committed
for a new source-bound checkpoint. The archive auditor retains its historical8.13
profile and recognizes the reviewed9.4.1/AGP9.2.1/KGP2.4.21 profile separately.
Its historical13cases,4wrapper,4migration-boundary and4legal cases pass locally;
bootstrap236 and source release identity pass. Official Windows launcher CRLF
bytes are preserved; diff whitespace is checked with cr-at-eol.

The experiment did not run emulators or independent reproducibility. Its APK is
not a promoted release artifact. Normal current-profile build/API36/legal/native
acceptance and new-source security/reproducibility remain pending. No API limit,
permission, format availability, product version, signing or distribution changes.

## Migrated frozen checkpoint rejected — ff01c514

Actions40156666980/build121059397880 failed in
:app:processDebugNavigationResources because kotlin-stdlib-common2.4.21 was
missing from the debugRuntimeClasspath lock state. Core69 had zero failures,
errors or skips; the APK/lint stage did not complete. API36 and optional SAF
were skipped. The downloaded partial archive matches provider size/digest,
CRC, safe unique20members and exact source; [the receipt](feasibility/evidence/android-ff01c514.json)
records the boundary. This rejects migrated frozen-source acceptance.
The earlierad075 full checkpoint and independent rebuild retain historical scope.

Validation revision3 in the migration experiment preserves before/after locks,
updates only the affected Kotlin common module through Gradle, then runs all
compile/lint tasks with no lock/metadata writes, no build cache and rerun-tasks.
A digest snapshot must remain unchanged. This is a derived-checkout diagnostic;
ordinary frozen-source/API36 qualification still follows incorporation.
