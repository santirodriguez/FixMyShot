# Isolated physical AVIF feasibility diagnostic

This is a **separate, disposable debug APK**, not the FixMyShot application
and not a shipping AVIF output path. The project module is **excluded**
from ordinary `settings.gradle.kts`, so normal `:app` builds are unaffected.

## Purpose

Capture exploratory physical Pixel 9 Pro XL evidence for the pinned
libavif 1.4.2 / libaom 3.15.2 8-bit sRGB YUV444 path:
concurrent **sampled** process PSS/native heap during AVIF encoding,
three bounded repeated attempts on one real 1344x2992 screenshot,
on-device AVIF reopen and source-to-decoded pixel comparison.
This does **not** measure exact instantaneous peak PSS or qualify
production p50/p95/cold-warm CPU performance.

## Private manual phone exercise

1. Build/download only the verified isolated `FixMyShot-AVIF-Physical-debug.apk`
   from the source-bound `AVIF physical probe diagnostic APK (isolated)`
   GitHub Actions artifact. This APK uses an **ephemeral debug signer**,
   package `org.cajapersonal.fixmyshot.avifphysical`, and is *not*
   an updatable/releasable FixMyShot APK.
2. Install it by explicitly opening that APK on the Pixel 9 Pro XL.
   It will not overwrite the normal FixMyShot or previous `.avifprobe`
   diagnostics. It requests **zero Android permissions** and cannot
   access files until the user deliberately invokes the document picker.
3. Use **Select PNG screenshot** to pick one **opaque, sRGB, 1344x2992
   PNG** voluntarily. Choose a screenshot without sensitive content.
   The app reads it once into memory, never writes or uploads the
   source, and rejects other geometry or non-sRGB source profiles.
4. Tap **Run three encodes**, leave this screen open, and wait for
   completion. The local native encoder creates app-private AVIF
   candidates, deletes the extra trial outputs and retains only one
   private preview. No output is written to Gallery or SAF.
5. Toggle **View original / View AVIF** to compare small text, geometry,
   colors and edges, then record **Image looks correct** or
   **I see visual artifacts**. Neither choice auto-shares image bytes.
6. Tap **Save metrics JSON**, choose a local location, and share only
   that JSON report if willing. It contains no screenshot pixels,
   content URI, source filename, image hash, account ID or file bytes.
   If the preview fails, report `device_decode_status` records it
   and the encoding result remains distinguishable.

The existing accepted native Pixel/UX diagnostics must not be repeated;
this is a **new** software JNI physical measurement only. There is
no requirement to upload an original screenshot or APK to the repo.

## Measurement semantics and safety

- Android `Debug.getPss()` is sampled from a second thread at
  a **150ms target interval** while native conversion, encoding and
  private file output execute. Reports sample count, failures,
  highest observed PSS and highest observed native heap allocation.
  Sampling perturbs measurement and **can miss an instantaneous peak**;
  insufficient sampling must not be treated as a pass. The separate
  before/after PSS values do not repair this limit.
- `avifEncoderWrite` nanoseconds and RGB-to-YUV conversion are measured
  separately in JNI. Full wall time includes private file write and
  sampler contention. Three attempts on one screenshot are exploratory
  and are **not** an adequate cold/warm percentile study.
- The isolated decoder-matrix diagnostic compares the **same private AVIF bytes**
  through default `BitmapFactory`, explicit sRGB `BitmapFactory.Options`, and
  sRGB-targeted software `ImageDecoder`. It records each path's color space,
  geometry, PSNR, max channel error, error histogram, worst pixel coordinate,
  RGB per-channel error counts, and a 4×6 spatial distribution of errors >2.
  When a real PNG is selected, only aggregate metrics/coordinates enter the
  JSON report: **no original or decoded user RGB samples or filenames**.
  **Only for the built-in synthetic CI fixture**, 19 fixed edge/interior points
  also record source/decoded RGB triples and deltas for each successful
  Android display path. The emulator CI additionally captures the read-only
  `media.avif.crabbyavif` property and build fingerprint as **hints**, not
  proof of the decoder implementation actually used.
  The user preview **remains** the original default
  `BitmapFactory` result; a better alternate decode is diagnostic evidence,
  not an automatic viewer switch or acceptance. CI retains the PSNR ≥38 dB
  and max-channel-error ≤2 thresholds for the display routes. A red route
  stays red until separately resolved. This is a **device decode** comparison
  and not a substitute for independent host/viewer or human assessment.
  HDR, Display P3, alpha and EXIF-related appearance are deliberately
  not claimed in this narrow experiment.
- Encoder output cap 16MiB; only width 1344 and height 2992, 8-bit
  opaque ARGB source and matching private `getCacheDir()` paths are
  permitted. JNI writes exclusive `0600` private output. No
  Internet, broad storage, telemetry, automatic sharing, production
  signer or output/gallery integration is present. Uninstalling
  this disposable debug APK clears its app-private candidates.
- The synthetic CI mode accepts only an explicitly launched
  `synthetic_ci` extra on a recognized emulator. It generates its
  own in-memory fixture and uploads **no real user image**.
- Packaging includes the exact upstream license/patent notices and
  project GPL text, but does **not** constitute finished license,
  provenance, patent or distribution review.
- Actual Pixel execution, second hardware, physical 16KiB runtime,
  peak PSS and production performance remain **NOT RUN** until
  separate real-device receipts arrive. Normal HEIC/AVIF export
  remains disabled; PNG and lossless WebP stay unchanged.

## Build boundary

The opt-in CI is triggered only by
`.ci/avif-physical-request.txt`. It rebuilds
`libavif_physical.so` from exact libavif/libaom Git pins for the
Pixel `arm64-v8a` and API36 emulator `x86_64`, checks original
notices, creates the separate debug APK and runs a synthetic Android
smoke. Local construction (if needed) must explicitly supply all
three properties:

```sh
./gradlew :avif-physical:assembleDebug \
  -Pfixmyshot.avifPhysicalProbe=true \
  -Pfixmyshot.avifPhysicalLibsDir=PINNED_JNI_DIRECTORY \
  -Pfixmyshot.avifPhysicalNoticesDir=PINNED_ASSETS_DIRECTORY
```

No :app dependency, release variant, source-owned user photo, producer
signing material, public distribution or external mutation is authorized.
