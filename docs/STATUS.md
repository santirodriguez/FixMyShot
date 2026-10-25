# Completion status

This is a current-state index, not an acceptance waiver or a replacement for
the [full blueprint](blueprints/first-functional-release.md) and
[compatibility amendment](decisions/compatibility-and-validation.md).

The import/edit/PNG/lossless-WebP workflow is implemented. The latest complete
Android checkpoint and byte-identical independent rebuild are sourcead075068.
The migrated AGP9.2.1/Gradle9.4.1/Kotlin2.4.21 inputs are committed but not
qualified: frozen-source run40156666980 failed on a Kotlin common lock entry
before API36. See [the exact receipt](feasibility/evidence/android-ff01c514.json).
Validation revision3 of the isolated migration is testing Gradle-generated
lock completion followed by a build that cannot write locks or metadata.

| Remaining work | Concrete completion evidence | Dependency |
| --- | --- | --- |
| Qualify migrated build | Read-mode experiment passes; incorporate exact reviewed locks; ordinary frozen-source core/lint/3APKs, source/native/legal audits and API36 pass | Current Kotlin lock correction |
| Refresh security review | New pinned XML/lock/native inventory, raw OSV replay and advisory applicability/remediation review | Qualified new dependency graph; old results do not transfer |
| Authenticate dependencies and finish licenses | Reviewed exact publisher evidence, corresponding source/notices and complete transitive obligations; deliberate strict-policy decision | Current graph; unavailable evidence remains unresolved, never approved by absence |
| Reproduce final unsigned bytes | Independent clean exact-source build and actual byte equality | Qualified frozen candidate |
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
