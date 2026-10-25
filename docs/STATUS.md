# Completion status

This index links current evidence. The [full blueprint](blueprints/first-functional-release.md)
and [compatibility amendment](decisions/compatibility-and-validation.md) still govern.

Import, editing, PNG and lossless WebP export are implemented. Source
`5d568a7e` passed [normal validation](https://github.com/santirodriguez/FixMyShot/actions/runs/40196478234):
69 core tests, lint, three APKs, exact source/tree/wrapper/lock audit, actual NDK
source pins, eight canonical legal assets in both debug/release packages, native
libraries across three ABIs with ELF/ZIP alignment, and API 36 regressions with
80 real encodes. Four downloaded-report guards were replayed independently.
The [package receipt](feasibility/evidence/android-5d568a7e.json) identifies the bytes.

[Independent rebuild](https://github.com/santirodriguez/FixMyShot/actions/runs/40197042011)
used the exact source and empty caches. The downloaded unsigned APK is identical
to the original full file: 11,841,246 bytes, SHA256
`cc902a395a73ca8c18d98b03a36b4f823ee33cdf3d59e88f4aaf5b5f2eeff360`.
This closes bounded build/runtime/reproducibility acceptance for that source.
Subsequent source-review tools and evidence change no app/build inputs.

The Protobuf BSD and compiled NDK cpufeatures notice omissions are corrected.
The 133-component runtime declaration catalog is packaged; its 133 POMs and
95 hash-matched archives were reviewed. Exact WebP source, original notices, NDK
helper and AndroidX native release sources were checked. The two earlier raw
ZIP uploads are verified. The [license owner](feasibility/runtime-license-review.md)
and [native owner](feasibility/native-source-review.md) retain full-review limits.

The frozen dependency inventory remains byte-identical at 487 components and
858 files. Inactive publisher review covers **297/858 files**, with 561 pending:
**13 runtime-associated files** and 548 other captured files. All six critical
files retain evidence. New source-copy and publisher-checksum proofs preserve
all earlier entries and remain distinct from PGP signatures. Three additional
Okio files have verified detached signatures. The 1.24 MB raw evidence ZIP is
verified at upload commit `f7c1fa7f`; all 94 members and manifest entries pass. See the
[source-proof owner](feasibility/google-source-copy-review.md).

The unchanged OSV snapshot records 49 advisory IDs across 18 tooling components
and no matches for the 133 app-runtime components at scan time. Actual debug
and release DEX definitions/type references contain none of the six affected
package families. This narrows packaged exposure; build-operation risk remains
open. No generated trust policy is activated. See the
[security owner](feasibility/dependency-security-review.md).

| Remaining work | Required evidence |
| --- | --- |
| Dependency/security/source review | Remaining 561 exact issuer/metadata files, owning-tool applicability/remediation, deliberate strict policy and final corresponding-source distribution materials |
| Physical acceptance | Pixel 9 Pro XL plus a second family: HEIC/AVIF geometry/fidelity/color/resources/cancellation/fallback, providers/batches/process death/storage pressure/gallery/receivers, TalkBack/text scaling/rotation/languages/16 KiB runtime |
| Distribution preparation | Separately authorized signing identity, manifest/certificate checks, sequential install/upgrade/Obtainium, authentic captures and final receipts/docs |

HEIC/AVIF remain unavailable for ordinary export pending their gates. JXL is
explicitly deferred for 0.1.0. Legacy diagnostics are best effort; skipped means
NOT RUN. Signing, merge and publication remain separately gated.
