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
cmp /tmp/fixmyshot-kotlin-replayed/dependency-provenance-review.json \
  gradle/dependency-provenance-review.json
cmp /tmp/fixmyshot-kotlin-replayed/reviewed-verification-candidate.xml \
  gradle/reviewed-verification-candidate.xml
```

Review performs no network request, build or trust activation. Previously attempted unsigned/unverified files remain deferred until explicitly reconsidered. Current acceptance gates and signing/publication boundaries still apply.
