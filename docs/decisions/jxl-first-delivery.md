# JPEG XL decision for the first functional delivery

Decision: **DEFER JXL for v0.1.0**. Date: 2026-10-23.

The conditional gate in the immutable first-functional-release blueprint permits deferral after a bounded source-build/measurement attempt. That attempt is complete. The measured reference build is useful evidence, but there is no integrated, cost-qualified Android encoder/decoder ready to ship. Keep optional JXL work outside the remaining first-delivery critical path.

## Evidence used

The pinned official libjxl0.12.0 source `a7a9c787341cf703dede03c2009fa460cae5e5df` compiled for arm64-v8a, armeabi-v7a and x86_64 in [Actions39646055054](https://github.com/santirodriguez/FixMyShot/actions/runs/39646055054), application-source commit `0887a7279510962560ae7cf2b9c360dd9dd4cecf`. Complete provenance, per-library findings and ZIP hashes are in [the feasibility record](../feasibility/phase1-batch-preflight.md#7-source-bound-three-abi-libjxl-012-reference-build-2026-10-21).

| ABI | Reported unstripped library total | Reported per-library DEFLATE sum |
|---|---:|---:|
| armeabi-v7a | 55,631,612 B | 17,137,684 B |
| arm64-v8a | 138,120,928 B | 29,498,399 B |
| x86_64 | 82,829,352 B | 21,387,261 B |

These measured configurations include six libraries, some potentially redundant, and are not a narrowed shipping dependency set. The reports do not supply the original ELF files for independent reinspection. **The sums are not stripped APK deltas and do not prove that a properly optimized adapter would exceed the 12MiB universal/5MiB arm64 incremental budgets.** No compliant shipping-cost result exists. Reference compilation alone also leaves JNI integration, Android encode/decode fidelity, license/security review, cancellation, physical16KiB operation and measured Pixel latency/Java/native/PSS unresolved. Host FFmpeg/libjxl0.11.1 synthetic round trips are not evidence for the selected Android0.12 shipping route.

The reason for deferral is the gap between the measured reference and a qualified product implementation, after an actual bounded build/measurement attempt. It is not a claim that libjxl cannot compile, cannot fit or produces incorrect images. No new full app CI or reference build is needed merely to restate these limitations.

## Product effect and reconsideration

- The format menu does not offer JXL for new selections. Existing serialized JXL draft values remain readable and explicitly unavailable; no silent substitution, automatic retry or schema/fingerprint change occurs.
- Keep the core format identity and source-build evidence for compatibility and later evaluation. No JXL runtime dependency, binary or permission is added.
- PNG stays the new-session default; lossless WebP remains the implemented alternative. HEIC/AVIF are separate desired first-delivery features whose device/fidelity/fallback gates remain mandatory. This decision does not waive them or authorize shipping without resolving their scope.
- Reconsider JXL in a later scoped update with an optimized necessary-library/JNI design, actual compressed package delta, independently inspected ELF bytes, dependency audit and Android fidelity/cancellation/memory/latency/device evidence meeting the blueprint budgets. Do not enable a dead selector on the strength of a reference build.
