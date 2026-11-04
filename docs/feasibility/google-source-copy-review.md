# Exact Google POM source-copy review

Scope: frozenf943487-component/858-artifact inventory. The initial21 Google
POM proofs below are supplemented by44 exact runtime archive/module copies and
six exact JetBrains-owned checksum records and three exact Okio signatures.
Current inactive subset:325/858 reviewed,533pending; all six critical files retain evidence. Runtime-associated
files are217reviewed/11pending; other files are108reviewed/522pending. No app/build
inputs, dependencies, active trust policy or runtime gates changed. All previous
244 and historical387 proof entries are preserved.

## Publisher anchor and checks

Google maintains [platform/prebuilts/tools](https://android.googlesource.com/platform/prebuilts/tools).
The inspected [studio-2026.2.1 release tag](https://android.googlesource.com/platform/prebuilts/tools/+/refs/tags/studio-2026.2.1)
objecteeb949f3e8d3a0305fbf4bd01b3b3bfb9d223b27 targets immutable commit
7fd4bf2b791a54211e000dba6b24d696773dc792. This publisher-controlled release
source is separate from the Maven download whose hash was frozen by the build.
The initial POM review considers exact androidx./com.android. files only;
the runtime archive/module extension below preserves that publisher scope. Google mirroring an
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


## Earlier244-file runtime priority snapshot

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


## Runtime source-copy extension —44 exact files

The same immutable AOSP release contains all44 remaining AndroidX runtime
archive/module files selected by the current app lock:25AAR,4JAR,15module.
A single bounded data-only capture obtained every exact file. The independent
review checks frozen SHA256, actual size and recomputed Git blob against unique
regular-file membership in the pinned raw Gitiles directory. It does not infer
module/POM provenance from an adjacent JAR. This advances244→288; no detached
signature, namespace-wide approval or active policy is claimed. Six offline
boundary tests passed. Unavailable same-release identities are excluded from
automatic replanning before network/output. The old POM reviewer stays unchanged
so its historical223→224→244 replay retains exact output hashes.

[The receipt](evidence/runtime-source-review-ebfe90ec.json) pins the actual
44-row report and original producer. Raw binary evidence is in the prepared
1,243,209-byte runtime-publisher-source-ebfe90ec.zip, SHA256
84f2b82d7f9468df49f364e1a0a0c635fa97adbbd85e394e978f9c3699d033fb.
Its94safe regular members/CRC/every manifest entry passed against the actual
uploaded bytes in commit f7c1fa7f5e64a92659a3b507ff6b96e92880b930, tree
7ee0bc4f40e000ccb226d9cc7c54847a5298a89a. Remote persistence is VERIFIED;
the self-contained raw replay evidence is committed at the receipt's exact path.

After verifying that ZIP's SHA256, extract safely into a fresh directory. From
the repository root, using fresh output paths:

```sh
python3 scripts/review-runtime-source-copies.py review \
  /tmp/fixmyshot-runtime-source/metadata.xml \
  /tmp/fixmyshot-runtime-source/prior-review244.json \
  /tmp/fixmyshot-runtime-source/app.lockfile \
  /tmp/fixmyshot-runtime-source/runtime \
  --expected-prior 38c8e5c17d2fcc40f38f73e2728ec99e52ad49d71fc46cda325ae486c5659ee0 \
  --expected-lock 82e2f2f1f037385a526645b68e25bc1ac12a1c9882e3004b929d42cc3c3c3935 \
  --expected-report 7386dfc8bf09a1ecdccd69efff6a50fd3d9b7ed70a5b5cf1d74a38f56ac5f2e0 \
  --output /tmp/fixmyshot-reviewed288
```

Expected288-file JSON SHA256:
1fc1f4351872a87f63df853443ff2a07f6126fc245624390db2129613d2bd7fb.
The package's producer-v1.py matches the report producer pin. Review captured
archives only as data; do not execute them.

## Exact JetBrains-owned release source checksum records

The official Kotlin v2.4.21 tag resolves to immutable commit
9180ce6a7a7d3627f88b424717583f28ddca052d. Its
[committed checksum record](https://github.com/JetBrains/kotlin/blob/9180ce6a7a7d3627f88b424717583f28ddca052d/gradle/verification-metadata.xml)
is325983bytes/SHA256
60d44a5f54f4a439583db9808af334feb9a7468c17806f661449e3e34e3a042a,
Git blob274270d7a46c4e1b6ce6734a32dca82721e58368. The exact full source
bytes and GitHub blob hash were independently compared. This is a separately
published publisher-owned release-source checksum record, not a PGP signature.
Its Generated by Gradle origin does not turn local generated build hashes into
issuer proof: the independent anchor is the publisher's own immutable release
source, and only its own exact namespace files are considered here.

Six previously unreviewed exact JARs match: annotations13.0/23.0.0, coroutines
core-jvm1.7.3/1.8.0/1.9.0 and serialization-core-jvm1.7.3. Three belong to
current app runtime; three are other captured files. A publisher's checksum for
foreign dependencies such as Okio is deliberately ignored. No adjacent POM or
module gets approved from a JAR checksum. Four offline source/pin/conflict/scope
tests and actual whole-prefix preservation passed. This advances288→294.

```sh
python3 scripts/review-kotlin-source-checksums.py \
  docs/feasibility/evidence/gradle-baseline-f9434ab9.xml \
  /tmp/fixmyshot-reviewed288/dependency-provenance-review.json \
  docs/feasibility/evidence/kotlin-release-source-2.4.21.xml \
  /tmp/fixmyshot-reviewed294 \
  --expected-prior 1fc1f4351872a87f63df853443ff2a07f6126fc245624390db2129613d2bd7fb
```

Expected 294-file checkpoint JSON SHA256:
9dc7776b37f4fd9e8eee1c62e4dff3ce687ab507971489b72a7f01a3041c3e77.
Expected XML SHA256:
6ab2153c7e16887870f0e9f81f2202415ab67b05d51be93dbac3e1dad6ec9230.
The [receipt](evidence/kotlin-source-checksum-review-2.4.21.json) lists all16
then-remaining runtime identities and its historical212/16 plus82/548 counts. Old attempts
remain recorded; new independent source proof supersedes an older unavailable
signature only for its exact file. Full strict policy and remaining issuer,
security, physical and distribution gates remain open.


## Three exact Okio signatures: 294 to 297

The historical capture already retains all three exact frozen files and detached
signatures: Okio 3.4.0 root module, Okio JVM 3.4.0 JAR and JVM module. The previous
854-byte keys.openpgp.org bundle has no valid UID/self-certification: GnuPG marks
the primary invalid and refuses the signatures. This was a key-bundle limitation,
not an artifact mismatch. A new 1,712-byte public ASCII bundle from the Ubuntu
keyserver includes the UID/self-certification and validates all three SHA512
signatures with the existing validity and strong-hash checks. No check was relaxed.

The issuer fingerprint DBD744ACE7ADE6AA50DD591F66B50994442D2D40 remains
independently anchored in the already inspected publisher security source,
lysine-dev/okhttp commit ba92108a334394a9fb2ee27858f35375cffa6cae,
blob aa9020585609634c850ca79d9bea53e42a9293bd. Its historical-key statement
covers releases before 2026-07-21. The keyserver is transport, not the issuer
anchor. The prior 294 proofs are unchanged. No wildcard trust, active policy,
new binary capture or key freshness/absent-revocation claim is made.

[Receipt](evidence/okio-publisher-review-3.4.0.json) lists all three VALIDSIG
records, exact file/signature/key/registry/archive/producer hashes and the 13
remaining runtime identities. Four offline tests exercise actual signatures,
all five input mutations, changed artifact and wrong primary, and rejection
of the previous stripped bundle. No downloaded code is executed or user keyring used.

For a standalone replay after the preceding 244→288→294 steps, use that exact
294 JSON as `PRIOR294` and a fresh output directory:

```sh
python3 scripts/review-okio-publisher.py \
  docs/feasibility/evidence/gradle-baseline-f9434ab9.xml \
  "$PRIOR294" \
  docs/feasibility/evidence/runtime-publisher-anchors.json \
  docs/feasibility/evidence/square-issuer-key-2021.asc \
  docs/feasibility/evidence/runtime-publishers-04a8aa02.zip \
  /tmp/fixmyshot-reviewed297
```

Expected historical checkpoint: 297 reviewed / 561 pending, including 215
reviewed / 13 pending runtime-associated files. Compare both output files with
`gradle/migration-f9434ab9/` at commit
13d083523658569b675ee0aed674b7286b008e77. The current 325-file subset adds
the following reviewed batch. The old capture and all historical reviewers remain
unchanged. No Android or unchanged OSV rerun is needed for this data-only review.


## Twenty-six Google modules and two Guava files: 297 to 325

The [new replay evidence](evidence/google-source-extension-13d08352/CAPTURE.md)
retains the complete original 47-file module report, 26 actual UTF8 module files,
26 corresponding raw Gitiles directories, and exact Guava ownership/directory
sources. All new file SHA256s and unique regular Git blob entries agree with
the frozen inventory and the same immutable Google AOSP release. The 21 missing
directories remain pending and are recorded to prevent repeated same-release
lookups. This is a new module selection, not a retry of unavailable POM URLs.
The imported collector's original runtime label is explicitly disclosed; this
review independently verifies the complete metadata selection without inferring
runtime membership from the label.

The additional Guava ListenableFuture 1.0 JAR and POM reuse their actual committed
capture bytes. Their source-copy proof is scoped to exactly these two artifacts.
Google's ownership is independently established by the immutable google/guava
README and the exact futures/listenablefuture1/pom.xml project source. No foreign
mirror, namespace-wide approval or new PGP assertion is introduced.

The [receipt](evidence/google-source-extension-13d08352/review.json) records all
28 new proofs, 21 unavailable directories, source/report/producer hashes and all
11 remaining runtime identities. Six new offline checks cover actual replay,
mutated modules/directories/report/ownership/archive/prior/metadata, fixed
release/approval flags and exclusion of other publishers or adjacent JARs.
The historical Okio replay test now tolerates later append-only review batches
while reconstructing its exact pinned 294 predecessor. No historical production
reviewer or prior proof changes. Current total: 325/858; runtime 217/11, other
108/522. No app/build inputs, active verification policy or codec gates change.


## Exact remaining runtime partition

The [current delivery-control receipt](evidence/delivery-control-status-e516aa89.json)
joins the frozen inventory, actual app runtime lock and unchanged325-file proof
record. Of95runtime binary archives,93have reviewed publisher evidence. The
two pending binaries are kotlinx-coroutines-android1.9.0and JSpecify1.0.0.
The other nine pending runtime-associated files are six modules and three POMs;
all11exact identities/hashes remain listed. Other captured files still have
522pending reviews. No count, proof rule or active policy changed.

The bounded follow-up found no new acceptable primary issuer anchor. Kotlin's
already pinned own release verification file has no trusted-key declarations
and verify-signatures=false; its existing six exact checksum proofs do not
approve adjacent metadata. Foreign key caches, short identifiers and source
availability do not satisfy the missing publisher evidence. Do not repeat
completed captures merely to seek another transport match. Progress requires
new credible primary evidence or a separately considered assurance-policy change.
Functional device evidence is a different acceptance dimension and cannot
silently close these distribution controls.
