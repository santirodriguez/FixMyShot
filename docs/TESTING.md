# Testing and evidence

## Checks in this slice

`core` runs 23 cases for format/default policy, API boundaries, unavailable routes, dimension overflow, WebP limits, memory estimates and metadata-preservation rejection. These are policy tests, not codec execution.

`CodecSmokeTest` launches the development Activity and generates 20 synthetic fixtures, including 1344x2992, odd dimensions, dark/light surfaces, one-pixel colored strokes and binary-alpha edges. It encodes PNG and lossless WebP twice per fixture, reads container structure/CRC/VP8L and compares every decoded opaque pixel. Transparent hidden RGB is excluded; partial-alpha tests remain pending. Rejections/pre-cancellation must leave no candidate. The fixture report records first/repeat timings and before/after memory snapshots: **neither process-cold performance nor peak memory**.

`check-apk.py` inspects each shipped ELF and APK data offset for 16 KiB alignment and requires arm64-v8a, armeabi-v7a and x86_64 with the JNI library. This is not a 16 KiB runtime test. The emulator matrix uses API 26 and 36 and records actual page size. Increased emulator heap is a test environment setting, not an application `largeHeap` request or proof of low-memory-device support.

## Running

```sh
python3 scripts/check-bootstrap.py
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleRelease
python3 scripts/check-apk.py app/build/outputs/apk/debug/app-debug.apk
bash scripts/run-device-smoke.sh
```

For a JVM-only check with an existing Kotlin compiler, compile `CodecContract.kt`, `ContractCases.kt` and `StandaloneChecks.kt`, then run the generated main class. Record the actual compiler/JDK: a standalone check with a different compiler is not the pinned Gradle build.

## Evidence ownership

Actions artifacts are named with the exact commit SHA. Build artifacts contain JDK/Gradle versions, test/lint results, unsigned/debug APK candidates, native size/alignment JSON and generated wrapper/dependency-verification candidates. Device artifacts contain source SHA, emulator build/page size, instrumentation results and `g1-smoke.json` when execution reached the fixture test.

No passing build or device run is recorded merely because a workflow was authored. Inspect the exact run result. Artifact retention is 14 days; accepted benchmark/decision receipts must be promoted into their durable repository owners before relying on them in later phases. Never commit private images or device identifiers from a user's real phone without consent.

## Open G1 acceptance

The initial slice does not close G1. Standard wrapper/lockfile/verification review and persistence, security/license inventory, actual build/lint/device results, PNG/WebP performance and native/PSS peaks, mid-encode cancellation/timeouts, physical Pixel and second-family HEIC/AVIF output, bounded JXL build/cost decision, 16 KiB runtime and independent clean-build comparison remain open. The full gates and required real-image/manual evidence are in the [governing blueprint](blueprints/first-functional-release.md).
