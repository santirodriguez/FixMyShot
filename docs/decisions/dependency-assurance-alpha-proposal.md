# Approved candidate-only dependency assurance decision

Status: **OPTION B APPROVED — 2026-11-02**. The maintainer explicitly chose
“B. Sigamos” after reviewing this proposal. This records acceptance of residual
issuer risk for the exact candidate below; proof classifications, active Gradle
verification, build inputs and separate signing/publication authority do not change.
The proposal filename is retained to preserve existing references.

## Current bounded facts

Validated executable source: 7cbed17de44f4bc4ff31b62270ace3bd5a51b492.
Exact unsigned APK: 12,340,030 bytes,SHA256
1e38786776a466b3809dca84d7cfba45dc6f3196781f3aca7979507260f1ee42.
Two clean builds match fully; 99 core and 135 instrumented tests and reported
manual PNG/WebP review passed. Current source materials are staged.

Publisher review remains 325/858 frozen files. Of 95 runtime binary archives,
93 have reviewed issuer evidence. `kotlinx-coroutines-android:1.9.0` and
`JSpecify:1.0.0` remain pending, plus six runtime modules/three POMs and
522 other frozen files. No counts change here. See the
[exact partition and required next action](../feasibility/google-source-copy-review.md#exact-remaining-runtime-partition).
The official [coroutines 1.9.0 release](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.9.0)
and [`JSpecify:1.0.0` release](https://github.com/jspecify/jspecify/releases/tag/v1.0.0)
expose no independent binary assets in the inspected GitHub release
metadata; their release text/Maven links do not authenticate the JAR bytes.
No new acceptable issuer anchor was established.

## Option A — retained rule for every other artifact

Keep the issuer/distribution gate open until materially new credible
primary evidence exists. Do not repeat completed captures or reinterpret
checksums/source availability as publisher proof. Development and the
already completed technical staging can remain accepted; publication
readiness is not claimed.

## Option B — explicitly accept residual issuer risk for this exact alpha

The explicit maintainer decision accepts the missing
publisher corroboration for the exact frozen input set and exact
0.1.0-alpha.1 / version code 1 unsigned APK above. Keep all 325 proofs and all 533
pending identities accurately classified; do not relabel them authenticated.
The exception is artifact-specific, not namespace/key trust, not
permission to download changed same-version bytes, and not approval of
a future source/version/hash. It does not enable a new active Gradle
verification policy; any such implementation needs separate review.

Supporting evidence is the locked inputs, existing critical/runtime
issuer proofs, exact source materials, tested output and independent
byte reproduction. Residual risk remains: two builds can repeat the
same compromised repository artifact, and source availability is not
proof of the binary's publisher or source equivalence.

This option does **not** waive source/license/preferred-form disposition,
other applicable device/resource/accessibility requirements, stable
signing custody, signed APK inspection, sequential upgrade/Obtainium,
truthful screenshots, or separate merge/release/deployment authority.
It does not authorize credentials, signing or publication.

Option B now applies only to the exact source/version/unsigned APK and frozen
inputs above. Any changed binding requires a new decision; Option A remains the
rule elsewhere. The machine-readable [assurance receipt](../releases/0.1.0-alpha.1-assurance.json)
pins the inventory/proof-record fingerprints, runtime lock and component catalog.
The [unsigned BUILD-INFO draft](../releases/0.1.0-alpha.1-unsigned-build-info.json)
records verified preparation and explicitly unperformed signing/install steps.
