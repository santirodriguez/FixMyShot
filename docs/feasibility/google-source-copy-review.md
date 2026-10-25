# Exact Google POM source-copy review

Scope: frozenf943487-component/858-artifact inventory. This adds exact
publisher-maintained source-copy evidence for21 Google POMs, without changing
application/build inputs, dependencies, generated trust policy or runtime gates.
Current inactive subset:244/858 reviewed,614pending; all six critical file
checks have evidence. The preceding223 and historical387 entries are preserved.

## Publisher anchor and checks

Google maintains [platform/prebuilts/tools](https://android.googlesource.com/platform/prebuilts/tools).
The inspected [studio-2026.2.1 release tag](https://android.googlesource.com/platform/prebuilts/tools/+/refs/tags/studio-2026.2.1)
objecteeb949f3e8d3a0305fbf4bd01b3b3bfb9d223b27 targets immutable commit
7fd4bf2b791a54211e000dba6b24d696773dc792. This publisher-controlled release
source is separate from the Maven download whose hash was frozen by the build.
Only exact androidx./com.android. POMs are considered. Google mirroring an
unrelated publisher's file does not establish that publisher's provenance.

The Compose BOM2025.12.00 POM has39897bytes, SHA256
08809b3b7ae43ba6bbd16bc22ef049561acda6968db4c0e8e6f2690907af0f17
and Git blob6eedce3f09ebe4622a3114c8f1a8644c93328240. The reviewer checks
exact SHA256 against the frozen build row and pinned source registry, plus
unique regular-file membership and recomputed Git blob in the raw pinned
Gitiles directory. Registry/release/route changes or same-size byte changes fail.
This proves a matching publisher-maintained copy; it is not a PGP signature,
full repository checkout or signature verification of the release tag.

The subsequent bounded capture selected60 unreviewed Google POMs at that same
release:20 exact matches and40 HTTP404 outcomes. Only exact matching bytes were
accepted. Every outcome is retained to avoid automatic retries at the same
source commit. An absent copy says nothing about whether Maven publishes it.
No other publisher, namespace-wide trust or active Gradle verification is approved.

## Offline replay

From the repository root, recover the immutable prior223 receipt and replay
the single BOM followed by the batch. Use fresh output directories. No network,
SDK, downloaded JAR execution or Android rebuild is needed:

```sh
git show e8fd9c0faba5e0d7f60002433504f189e50d381e:gradle/migration-f9434ab9/dependency-provenance-review.json > /tmp/fixmyshot-prior223.json
python3 scripts/review-google-source-copy.py \
  docs/feasibility/evidence/gradle-baseline-f9434ab9.xml \
  /tmp/fixmyshot-prior223.json \
  docs/feasibility/evidence/aosp-compose-bom-source.json \
  docs/feasibility/evidence/aosp-compose-bom-directory.gitiles.json \
  docs/feasibility/evidence/aosp-compose-bom-2025.12.00.pom \
  /tmp/fixmyshot-reviewed224 \
  --expected-prior 0a3c46dd9fcbf43397014e1771714c087ce523aefcb72a37cdeb2d552f294f5b \
  --expected-registry 4a3bc94e81bf2ee037e7affa3068e44b478608d9b9a664c47cd76cbd66ec2cf4
python3 scripts/review-google-source-poms.py \
  docs/feasibility/evidence/gradle-baseline-f9434ab9.xml \
  /tmp/fixmyshot-reviewed224/dependency-provenance-review.json \
  docs/feasibility/evidence/aosp-google-poms \
  /tmp/fixmyshot-reviewed244 \
  --expected-prior 85fcf9c1f80cea5a062fb81ed2cec74f59338093fb9a9fbb8b8a464fcbd8f687 \
  --expected-report 564392599bbc10c45a95725c996e3db1bb2bd03c8e5a65cc5ef41b659799d776
```

Expected final JSON SHA256:
38c8e5c17d2fcc40f38f73e2728ec99e52ad49d71fc46cda325ae486c5659ee0.
Expected XML SHA256:
9463806d106c28cfe9f65e06881b768b181d3524ee76297dde10b0ddb77b0f43.
The raw collector-v1.py snapshot matches the capture report's producer hash.
The current collector additionally excludes recorded same-release attempts and
refuses an empty plan before creating output or making a request.

Five offline mutation tests cover exact scope, pinned bytes, release/route,
directory membership and refusal of other publishers. Actual report replay
also rejects altered release and automatic-approval fields. This evidence does
not close remaining artifact authentication, tooling security, native/source
license obligations or physical/distribution acceptance.


## Current remaining runtime priority

Cross-checking this244-file receipt against the byte-identical frozen XML and
the133-component runtime catalog yields228 exact files associated with app
release-runtime components:165 reviewed,63 pending (25AAR,11JAR,23module,4POM).
The other630 captured files are79 reviewed/551pending. These sum to244/614.
This is component selection, not proof every selected archive/class is packaged.
Prioritize the63 runtime-associated files when a new independent publisher
evidence route exists. Keep the551 tooling/test/other files in the strict-policy
backlog; do not drop them or retry previously unavailable signatures blindly.
The inputs are the canonical frozen XML, current reviewed JSON SHA256
38c8e5c17d2fcc40f38f73e2728ec99e52ad49d71fc46cda325ae486c5659ee0
and LICENSES/runtime-components.json SHA256
f3b2d280a7c140e431ff643565c6bab8fe01f04edfb6bbfef9c7e3d073c8b407.
