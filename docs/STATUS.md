# Completion status

This is a current-state index, not an acceptance waiver or a replacement for
the [full blueprint](blueprints/first-functional-release.md) and
[compatibility amendment](decisions/compatibility-and-validation.md).

The import/edit/PNG/lossless-WebP workflow is implemented. The latest complete Android checkpoint and byte-identical independent rebuild
are sourcef9434ab9.
The migrated AGP9.2.1/Gradle9.4.1/Kotlin2.4.21 source-frozen build now passes
atf9434ab9: core69, lint, three APKs, source/tree/lock audit and independently
inspected native/legal bytes. Android36 also passes in Actions40160890302, including80real encodes and all four report guards.
The independent unsigned rebuild also produced identical actual bytes.
The frozen487/858 inventory completed488OSVqueries (49advisories on18tooling
components; no133app-runtime matches at scan time), and67new Kotlin files were
authenticated. The inactive review now contains223of858files,635pending, after7newGoogle
signature proofs includingAGP9.2.1. Raw dependency evidence is committed and independently verified;
security/license/device gates remain open.
See [the build receipt](feasibility/evidence/android-f9434ab9.json).

| Remaining work | Concrete completion evidence | Dependency |
| --- | --- | --- |
| Refresh security review | New pinned XML/lock/native inventory, raw OSV replay and advisory applicability/remediation review | Qualified new dependency graph; old results do not transfer |
| Authenticate dependencies and finish licenses | Reviewed exact publisher evidence, corresponding source/notices and complete transitive obligations; deliberate strict-policy decision | Current graph; unavailable evidence remains unresolved, never approved by absence |
| Accept real-device behavior | Pixel9ProXL plus second hardware family: HEIC/AVIF full/odd geometry, fidelity/color, resources, cancellation/fallback; real providers/batches/process death/storage pressure/gallery/receivers; TalkBack/text scaling/rotation/languages and16KiB runtime | Physical hardware and exact candidate; synthetic CI cannot supply these results |
| Prepare distribution | Authorized signing identity, actual signed manifest/certificate checks, sequential install/upgrade/Obtainium, real screenshots and final source/notes/receipts | Technical and device gates, then separate exact signing/publication authority |

HEIC/AVIF remain unavailable for ordinary export until their applicable gates
pass. JXL remains explicitly deferred for0.1.0. Legacy-only diagnostics are best
effort under the amendment; skipped means NOT RUN. No signing/publication or
new artifact delivery is implied by this status. Detailed execution evidence
is in [TESTING](TESTING.md), the [migration plan](decisions/toolchain-migration.md),
[dependency review](feasibility/committed-gradle-inputs.md),
[security review](feasibility/dependency-security-review.md) and
[release guide](RELEASING.md).

Current candidate: the Protobuf BSD notice and133component license catalog are
added to canonical LICENSES/ and THIRD_PARTY_NOTICES. The checker now requires
seven canonical assets. A grouped normal build/API36 checkpoint is requested;
updated package bytes and independent reproducibility are pending. The scoped
runtime declaration audit completed133POMs/95hash-matched archives, resolving
one Guava parent declaration; it does not complete native/source/legal review.
See [the license review](feasibility/runtime-license-review.md).
