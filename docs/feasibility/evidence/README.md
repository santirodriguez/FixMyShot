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

Prepare another bounded request from the repository root:

```bash
python3 scripts/plan-google-batch.py \
  docs/feasibility/evidence/gradle-baseline-773b34f0.xml \
  gradle/dependency-provenance-review.json \
  /tmp/fixmyshot-next-publisher-request.json
```

Planning performs no network request or trust activation. Inspect the generated request before deliberately replacing the collection marker. Previously attempted unsigned/unverified files remain deferred until explicitly reconsidered. The current acceptance gates and signing/publication boundaries still apply.
