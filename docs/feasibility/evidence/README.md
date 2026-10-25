# Captured build inputs, with the receipt attached

This folder preserves the original generated dependency inventory so later reviews do not depend on an expiring Actions artifact or another Android build.

| Evidence | Identity |
| --- | --- |
| Application source | 773b34f0f3eebb124cb415da41811aede369ecf9 |
| Source tree | 3574454a5e70607765a648eb33880777de1fb3ed |
| Original run | [40047858334](https://github.com/santirodriguez/FixMyShot/actions/runs/40047858334) |
| Build artifact | 12542905033 |
| Artifact ZIP SHA256 | 9fdd171c785a2e766b492403b57a4d087a8487b08b3ed9967087553f30224134 |
| Original XML SHA256 | e3122954d679a56876ba33efb4b8b25dad6510dc10c257c23b246c2aadadcb67 |
| Captured inventory | 514 components; 921 exact artifact hashes; generated, unauthenticated |

The XML is byte-for-byte equal to the downloaded original build artifact. Its nonstandard path and filename are deliberately inactive: Gradle does not load this as verification-metadata.xml. Generated hashes record observed bytes; they do not establish publisher authenticity. The independently reviewed subset lives in [the review record](../../../gradle/dependency-provenance-review.json), with full scope and limits in [committed inputs](../committed-gradle-inputs.md).

The Google-route planner currently has **zero unattempted files**. Do not replace the active request with an empty plan or automatically retry the 118 deferred files. Inspect the current review record before selecting another publisher.

The [retained dependency-review ZIP](dependency-review-64745e1f.zip) contains the Kotlin 2.2.0/2.2.20 manifests and complete bounded OSV captures. [Security triage](../dependency-security-review.md) owns the artifact/control/request/report digests, interpretation limits and offline security replay. These are repository evidence, not a trust policy.

Reproduce the Kotlin extension from the repository root after checking/extracting that ZIP as described in the triage owner:

```bash
git show cbb9414a58be1efc77c501e70d7fcb8026fbfb19:gradle/dependency-provenance-review.json \
  > /tmp/fixmyshot-prior-368.json
python3 scripts/review-kotlin-manifests.py \
  /tmp/fixmyshot-dependency-evidence-64745e1f \
  docs/feasibility/evidence/gradle-baseline-773b34f0.xml \
  /tmp/fixmyshot-prior-368.json \
  docs/feasibility/evidence/kotlin-publisher-manifests.json \
  /tmp/fixmyshot-kotlin-replayed \
  --expected-control 64745e1f88df2ecf488f1fc650854f186f6abe7a \
  --expected-request eda0cfc56b209076d45a1a2c1d3349c88b78c738e24ed71dfefd1d98faf33099 \
  --expected-prior 49f7a64bf093b7a4b699014a7ab9bf7614f7917529af096ed9575f63968d63dc \
  --expected-registry cb3fc072e6e97e7dd692e649cbc974e01f4c7bae4b9ac5d638753ec1e2ecccbe
git show 8a6ba7828488a02382a0ba64b368f030b57c3959:gradle/dependency-provenance-review.json \
  > /tmp/fixmyshot-after-kotlin-379.json
cmp /tmp/fixmyshot-kotlin-replayed/dependency-provenance-review.json \
  /tmp/fixmyshot-after-kotlin-379.json
git show 8a6ba7828488a02382a0ba64b368f030b57c3959:gradle/reviewed-verification-candidate.xml \
  > /tmp/fixmyshot-after-kotlin-379.xml
cmp /tmp/fixmyshot-kotlin-replayed/reviewed-verification-candidate.xml \
  /tmp/fixmyshot-after-kotlin-379.xml
```

Review performs no network request, build or trust activation. Previously attempted unsigned/unverified files remain deferred until explicitly reconsidered. Current acceptance gates and signing/publication boundaries still apply.


## Runtime publisher capture

`runtime-publishers-04a8aa02.zip` preserves the original 59-file public-only
Actions artifact12570037960. Expected size2,152,718 bytes and SHA256
`019f31f7526cbafc2b3d4da5e352bdb89334edd05954b24e9bafb655bcddfdc3`.
Before extraction, check the hash, ZIP CRC, unique relative regular-file names,
no links/special files/traversal, and member sizes within4MiB. Extract into a
new empty temporary directory. Never execute downloaded artifact contents.
The original job finished FAILURE/partial capture; retaining it does not change
that result. Detailed outcomes and source pins are in
[committed inputs](../committed-gradle-inputs.md).

After verifying/extracting to `/tmp/fixmyshot-runtime-evidence-04a8aa02`, replay
from the repository root into a fresh output directory:

```bash
git show 04a8aa0252d69b68bec6bbb1197f075b1f7beb2a:gradle/dependency-provenance-review.json \
  > /tmp/fixmyshot-prior-379.json
python3 scripts/review-runtime-publishers.py \
  /tmp/fixmyshot-runtime-evidence-04a8aa02 \
  docs/feasibility/evidence/gradle-baseline-773b34f0.xml \
  /tmp/fixmyshot-prior-379.json \
  docs/feasibility/evidence/runtime-publisher-anchors.json \
  app/gradle.lockfile /tmp/fixmyshot-runtime-replayed \
  --expected-control 04a8aa0252d69b68bec6bbb1197f075b1f7beb2a \
  --expected-request 5c965e9a09b70683bc70529c2bb2cceafca890a3d48fa517821f83dd0786066b \
  --expected-prior 3fa1fd05c8261169a3d8c625c8419410f9306c740b6870945d4728db5a67a207 \
  --expected-registry b3d34114ca6b80308aa74520aa6f4268ea5054dfa72adc1286abc6e5592ea9e8 \
  --expected-lock 97ac0f54a4a9a7368f9472ba72717a0b4dae60d30b6b643dc307350f8f84977b
cmp /tmp/fixmyshot-runtime-replayed/dependency-provenance-review.json \
  gradle/dependency-provenance-review.json
cmp /tmp/fixmyshot-runtime-replayed/reviewed-verification-candidate.xml \
  gradle/reviewed-verification-candidate.xml
```

Expected result:387 reviewed/534 pending, with8 newly verified exact files and
all379 previous proofs retained. Review is offline and uses disposable public
keyrings; it changes neither active Gradle trust nor the prior security snapshot.

## Migrated f9434ab9 dependency evidence

`dependency-evidence-f9434ab9.json` records the225042-byte package
`dependency-evidence-f9434ab9.zip`, SHA256
7b1fe28d705c14049ba6401e9dadbd346a1e2ba881ef249bc16d1505c974fbea.
Local CRC,70safe members, manifest hashes and raw-capture/replay checks passed.
The package still awaits maintainer upload at this directory on release/0.1.0;
do not claim remote persistence until the actual uploaded bytes are verified.
It contains publisher/ (original Kotlin2.4.21 manifest capture), security/
(all488queries/49detailresponses and capture report), inherited-review.json
(the149-proof seed with unchanged attempted identities) and the full offline
security replay receipt. It contains no application artifact or private input.

Replay with the current repository scripts and frozen gradle-baseline-f9434ab9.xml
plus the three matching locks. Use the explicit control/request/report/registry/
prior pins in the package receipt: review-kotlin-manifests.py accepts publisher/,
inherited-review.json and the pinned official registry; review-dependency-security.py
accepts security/. Compare output bytes/counts to the migrated216/858 partial
review and security-f9434ab9.json/full replay hash. Original387/921 remains at its
old canonical path. Missing raw bytes cannot be reconstructed from this summary.
