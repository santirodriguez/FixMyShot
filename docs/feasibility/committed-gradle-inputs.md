# Committed Gradle wrapper and dependency resolution state

Status: **WRAPPER AND VERSION LOCKS COMMITTED; ARTIFACT AUTHENTICATION OPEN**. Date: 2026-10-23. This is partial G1 groundwork, not an independent reproducible-build or security acceptance.

## Source-bound input and review

The inputs come from the successful build job120315940893 in [Actions39924038144](https://github.com/santirodriguez/FixMyShot/actions/runs/39924038144), exact application source `a047cfe6ba8cad93f9ceb8c53e7984c106b5fd78`, artifact12489096076 (`bootstrap-a047cfe6ba8cad93f9ceb8c53e7984c106b5fd78`). The actual31,377,312-byte ZIP was downloaded and checked: SHA256 `f357e7cad31c0481788cb41cdc27634a207aec58ca89392fb275cbddc1dd9b84`, ZIP CRC passed, embedded source SHA matched. Only the standard wrapper and resolved version locks are promoted; the captured verification XML remains unapproved.

The [official Gradle checksum reference](https://gradle.org/release-checksums/) was rechecked for8.13. The wrapper JAR SHA256 is `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`; the pinned distribution SHA256 is `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`. Both match. The wrapper was generated in the already executed isolated bootstrap using that verified distribution; standard launcher bytes are retained, not rewritten. POSIX launcher SHA256 `734b3879d3501dce471cf0522d3bcbafe76873d9fc5129345b67fb43bd15e933`; Windows launcher SHA256 `57931b17dd228e5c24dac90e815d0bf82477e831a4618dfab4136f5446b42a9f`. Gradle's Apache2.0 license and wrapper notices are retained.

| Committed lockfile | Resolved coordinate rows | SHA256 |
|---|---:|---|
| app/gradle.lockfile | 307 | 97ac0f54a4a9a7368f9472ba72717a0b4dae60d30b6b643dc307350f8f84977b |
| core/gradle.lockfile | 35 | c6c472230eb7772de2dc71b2e4a04889f9e8651a5be76b7ab58c7a20115925a7 |
| codec-native/gradle.lockfile | 161 | 98f03edd2fce07085f9208a86c65c7d65038f21639f3bc43e600898c53088832 |

The bytes are the complete generated files, including configuration mappings and empty configurations, without hand-editing versions. Captured locked modules are compared with the verification inventory and direct catalog/build inputs before promotion. A lockfile records selected versions; it does not authenticate artifact bytes or establish that a dependency is safe/licensed for release.

All308 distinct locked coordinates occur in the captured metadata. The archived settings, root/module build files and prior bootstrap/workflow exactly match the working branch base; subsequent changes are the wrapper/lock-consumption changes described here. Wrapper-specific Git attributes retain the original POSIX LF/Windows CRLF bytes across checkouts.

## Build behavior

`bash scripts/bootstrap-gradle.sh` now checks the committed JAR, both launchers and complete distribution properties with Python before Java or network execution. It refuses altered/missing input instead of downloading/generating a replacement. `./gradlew` then uses the pinned standard wrapper and its normal verified Gradle distribution download when necessary. The Python guard is an integrity review aid, not a tamper-proof signature for an independently altered source checkout.

Ordinary integration builds no longer pass `--write-locks`. Existing `lockAllConfigurations()` uses the committed resolution state, and CI checks that all three lockfiles remain unchanged. Deliberate dependency changes require a reviewed lock update and catalog/notice/compatibility review. The current Gradle default lock mode checks configurations having lock state; this step does not claim strict coverage of every new/future configuration or plugin-resolution input.

CI still uses `--write-verification-metadata sha256` as explicit **unapproved trust capture**. Current generated metadata has551 components/958 artifacts and958 SHA256 entries, all generated, with0 PGP signatures. That supersedes the earlier502/874 inventory; it is not a publisher authenticity or current-security verdict. Do not commit those generated hashes as trusted merely because the build passed. Independent authentication of critical and remaining artifacts, a reviewed strict verification policy, complete plugin/native-source trust and an independent clean rebuild remain open.

No new integration CI is requested for this batch. Four offline wrapper boundary cases, resource/XML/catalog checks, Bash syntax, inventory/lock consistency and the existing host collector checks are proportionate local evidence. The changed build invocation and app selector still require a later grouped Android build; earlier a047 compilation cannot certify this source.


## Later source-bound inventory —7c832427

The build artifact12539628706 from [Actions40042133996](https://github.com/santirodriguez/FixMyShot/actions/runs/40042133996), exact7c832427b47cfa7006aeb99993b381f27df2a6b5, was independently downloaded and read:31,671,962 bytes, SHA25671d029af941b6b26f242a5a4184513d9230473bdc9136be5ecd5da1169d132a5; ZIPCRC/source identity passed. Reconstructing every archived Git blob/mode and recursive tree produced exactlycaae554261b98bdf658a5f2d7bbfba312977e1d2 (193 source blobs). This confirms the archived source, not independent rebuild reproducibility.

The existing read-only audit passed on that exact ZIP; its7 refusal-boundary cases and4 wrapper boundary cases also passed. The generated XML SHA256 is e3122954d679a56876ba33efb4b8b25dad6510dc10c257c23b246c2aadadcb67, with514 components/921 artifacts,921 generated SHA256 claims,0 PGP signatures and signature verification false. This is a later observed capture rather than a replacement for the earlier551/958 source-bound receipt. All308 distinct locked coordinates occur in this capture; the three lockfiles have the hashes above and match the current branch. The old local a047 ZIP copy was unreadable, so a byte-for-byte baseline archive comparison was NOT RUN; no comparison or authenticity claim is inferred from that failed input.

Every critical artifact remains publisher_sha_independently_verified=false and the result remains GENERATED_ONLY__NOT_APPROVED_FOR_STRICT_FREEZE. Generated inventory coverage and matching versions do not authenticate dependency bytes or establish current security status. No captured XML was promoted or committed as trust, no verification policy/dependency/lock was changed and no rebuild/signing operation ran. Actual publisher/artifact authentication, reviewed strict verification and an independent clean rebuild remain open.


## Offline source-identity gate

The auditor now reconstructs the source tar's Git blob/mode/tree identity and checks independently archived wrapper/lock bytes against the tracked source. Obtain both expected hashes from an independently inspected repository commit; copying hashes out of the artifact would not establish this boundary. For the already inspected7c archive:

```sh
python3 scripts/test-gradle-candidates.py
python3 scripts/audit-gradle-candidates.py bootstrap-7c832427.zip \
  --expected-source 7c832427b47cfa7006aeb99993b381f27df2a6b5 \
  --expected-tree caae554261b98bdf658a5f2d7bbfba312977e1d2
```

The actual downloaded7c ZIP passed with source_identity_verified=true, source_build_inputs_verified=true,193 regular files and the exact tree above. Without both pins the identity flag remains false; supplying only one or a mismatched pin fails. Legacy captures lacking committed wrapper/lock files remain observational with explicit untracked inputs and cannot pass the pinned-source gate. Changed tracked input bytes always fail. The baseline archive identity remains unverified unless separately checked against trusted repository references.

Limits are64MiB source tar/16MiB source file/4096 members,64 path components/4096 UTF-8 path bytes, and512MiB expanded ZIP total/128MiB per ZIP entry. Duplicate ZIP/tar names, unsafe paths, file/directory conflicts, links and special files fail without extraction or execution. Directories are implicit in Git's tracked-file tree. The13 offline cases include an independent git write-tree fixture reference, executable-bit/Git directory sorting, unsafe/duplicate/conflicting/linked input, limits, paired/wrong/changed identity, legacy untracked captures and mismatched build inputs.

These are source-archive integrity checks. They do not authenticate publisher artifacts, prove compile provenance, inspect a production signature or replace an independent clean rebuild. The generated dependency inventory remains GENERATED_ONLY__NOT_APPROVED_FOR_STRICT_FREEZE; no verification XML, version lock or trust policy was promoted. No Android run is needed for this host-only script change.


## Exact773 baseline and one clean rebuild request

The773b34f0 build ZIP12542905033 from Actions40047858334 was downloaded and verified:31,676,803 bytes/SHA2569fdd171c785a2e766b492403b57a4d087a8487b08b3ed9967087553f30224134, ZIPCRC/exact embedded source PASS. The pinned-source auditor passed193 files/tree3574454a5e70607765a648eb33880777de1fb3ed and matching tracked wrapper/locks. Generated capture514/921/SHA256e3122954d679a56876ba33efb4b8b25dad6510dc10c257c23b246c2aadadcb67 remains generated-only with0 PGP signatures and all critical publisher-authentication flags false.

The actual unsigned APK baseline is23,495,607 bytes/SHA25666ae94204ff3de3f76936dbf7f3f96245783e15dfbb89a995297b0ff4e6b4738, matching its archived receipt. Independent byte inspection passed9 native libraries/arm64-v8a/armeabi-v7a/x86_64/ELF-ZIP alignment. The explicit rebuild request pins this exact source and baseline. Its new workflow/controller compile that commit afresh with empty project/Gradle caches and no build-cache reuse, keeping source and control identities distinct. Six local controller refusal/comparison cases passed. The controlled clean compilation and byte comparison subsequently passed in Actions40054940065; the separate receipt below records its exact scope. Source-audit success alone still cannot establish rebuild success.

This check does not promote generated dependency metadata. Independent artifact/publisher authenticity, reviewed strict verification and final release reproducibility/signing/physical gates remain open. Keep the exact original and rebuilt evidence, and investigate a byte mismatch before another run. See [RELEASING](../RELEASING.md) for the controlled procedure.


## Independent unsigned773 rebuild — scoped PASS

[Actions40054940065](https://github.com/santirodriguez/FixMyShot/actions/runs/40054940065), workflow control commit eef2ddae50f30361e0aca664e11558e59026652d, job120755741931 SUCCESS. The compiled source remained exactly773b34f0f3eebb124cb415da41811aede369ecf9/tree3574454a5e70607765a648eb33880777de1fb3ed. Actual logs confirm the pristine source/empty Gradle and project caches, source-pinned tools, --no-build-cache --rerun-tasks, core tests and unsigned release (`BUILD SUCCESSFUL in3m25s`). It used one fresh hosted job, without emulator or production signing.

Artifact12544544566 was downloaded:14,742,573 bytes/SHA25609ef3a0b1d2aaf995baa4eb4ddbdb6c533ebcd7106deb5d5f836ed597f20cb75. ZIPCRC, actual report/control/source identity and reconstructed193-file tree passed. Its unsigned APK is23,495,607 bytes/SHA25666ae94204ff3de3f76936dbf7f3f96245783e15dfbb89a995297b0ff4e6b4738, matching the independent original baseline; a direct full-byte comparison also returned identical. The69 JVM cases report0 failures/errors/skips and native inspection reports9 libraries/three shipping ABIs/ELF-ZIP alignment PASS.

The independent generated dependency capture again has514 components/921 artifacts/0 PGP signatures; all six critical coordinate/file checksums equal the baseline capture. This corroborates repeat downloads, not publisher/key authenticity. The scoped same-layout/toolchain unsigned byte-reproducibility check PASSED; it does not establish every-host portability, strict dependency trust, APK production signatures, hardware/receiver/fidelity acceptance or release readiness. No generated trust XML, dependency version, signing identity, application behavior or release was changed. No further build is requested by this receipt.


## Pinned publisher evidence — collected and reviewed subset

The [Kotlin 2.2.21 publisher release](https://github.com/JetBrains/kotlin/releases/tag/v2.2.21) exposes maven-2.2.21-sha256.txt, asset 307650744, 158,512 bytes; the publisher release API reports SHA256 791a6bf5ff0404afe4f8eaf903faa6db5f97652f4d1e79b9795404f3111bb3c8. This is a publisher checksum manifest separate from the build's generated capture. Its downloaded bytes and exact coordinate/file matches were subsequently checked in the scoped review below.

JUnit's release tag r4.13.2 resolves through tag object ecafc199b864458638129460ce195dd8c91a96bb to commit 05fe2a64f59127c02135be22f416e91260d6ede6. The publisher's [KEYS file at that commit](https://github.com/junit-team/junit4/blob/05fe2a64f59127c02135be22f416e91260d6ede6/KEYS) explicitly identifies the release-signing key fingerprint FF6E2C001948C5F2F38B0CC385911F425EC61B51. Its 3,833 bytes/SHA256 fcb938a928b643fb76985f9ce3634d9dad170dfe6c1b276e920844822538d5f7 match the current primary repository KEYS file at inspection. This is the key-provenance anchor used by the separately recorded actual-JAR signature verification below.

The separate JSON-triggered evidence workflow retrieves the exact six baseline 773 critical artifacts from Google Maven/Maven Central, their optional published checksums/signatures and these two pinned publisher files. It is one data-only hosted job, with no SDK/build/emulator/signing step. HTTPS hosts and redirects are restricted, responses and batch/deadline are bounded, downloaded data is not executed and partial results survive. Six offline collector refusal/truthfulness cases passed. The captured-byte/signature/release-manifest review below completed a limited subset; the collector itself never sets authentication true or promotes Gradle trust. The normal application validation and rebuild requests remain unchanged.


## Initial reviewed publisher subset — 75 of 921 artifacts

[Actions 40058428491](https://github.com/santirodriguez/FixMyShot/actions/runs/40058428491), control fa1cc608ac2d07a22525c7170eb7683d1eda7880/job 120766462087, completed SUCCESS in 14 seconds without SDK/build/emulators. Downloaded artifact 12546357394: 34,257,328 B / SHA256 126b1c20b6529ed3ed619353fb4fbd13d0d3486fd6a5429787b3276e9a474ab8. ZIPCRC/control/baseline/pins passed; all six exact artifact SHA256 values and both independent publisher-file pins matched. All six published checksum files also matched (SHA256 for the four Google entries, SHA1 for Kotlin/JUnit); those transport observations alone were not promoted to trust. Five detached signatures were captured; Compose BOM returned 404 for its signature.

The official Kotlin release manifest has 982 path/checksum rows. Matching full Maven group/module/version/file paths against the exact 773 capture identified 74 Kotlin 2.2.21 artifacts with identical published SHA256, including kotlin-gradle-plugin-2.2.21-gradle813.jar. No basename-only matching or group wildcard was used. This is independent publisher-release checksum corroboration, not a claim that Kotlin's PGP key was authenticated.

The actual 384,581-byte JUnit 4.13.2 JAR/SHA256 8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3 passed detached PGP verification with the exact publisher-maintained KEYS fingerprint FF6E2C001948C5F2F38B0CC385911F425EC61B51. gpgv returned0/VALIDSIG for that fingerprint, with no bad/expired/revoked status. Signature SHA256 a8538f87f58b6f54b7a6ceffd01262b082c389622b0886c520878de1efb9eab9. Verification used a temporary public-only keyring created by dearmoring the pinned public key; no agent, user keyring, secret key, keyserver or owner-trust setting was involved. The inspected current publisher KEYS matched the release-commit file.

The initial review established 75 exact artifacts/proof kinds. The [current review record](../../gradle/dependency-provenance-review.json) and [partial checksum candidate](../../gradle/reviewed-verification-candidate.xml) retain exact per-file evidence and authoritative current counters. The dated results below are scoped snapshots; the candidate remains incomplete. The candidate is not named verification-metadata.xml and is not loaded by Gradle. No active trust policy changed. The default generated-capture auditor still reports unapproved provenance unless the separate reviewed evidence is considered; generated counts are not blanket issuer approval.

The offline reviewer reproduces the result from the exact evidence and baseline XML; 5 refusal tests plus real positive signature/manifest checks and altered-JAR/altered-manifest rejection passed. Further Google artifacts, the native issuer key, unsigned metadata, other versions/publishers and current vulnerability/security/license review remain open. A publisher digest/signature check is not a current safety verdict, independent build or release acceptance. No application/dependency version, Android permission, signing identity or ordinary validation request changed.


## Google publisher-key collection — bounded request

Google’s official [key details](https://www.google.com/linuxrepositories/) identify active primary EB4C1BFD4F042F6DDDCCEC917721F63BD38B4796 and its [public-key file](https://dl.google.com/linux/linux_signing_key.pub). Web retrieval of that file failed. The completed data-only collection requested only this bounded key file; the collector pins the complete primary fingerprint and records the observed SHA256. A primary identity match alone does not verify any Maven signature, establish subkey binding or activate trust. The separately recorded actual signature review below adds only three exact Google artifacts to the partial subset.


## Google primary binding and three exact files — verified

The [7-second data-only run](https://github.com/santirodriguez/FixMyShot/actions/runs/40060745763) at c2a930310c3aebc2f2a748f1a7863ce49a009a4f fetched only Google’s 19,640-byte public key bundle. Artifact 12547168236 / 15,849 B / ZIP SHA256 118d7726ba635ed219b699989079e7838ef84d91ddfa35e3c166ac8df4f1d730 passed size, CRC and source/request checks. Key-file SHA256 54dea5f6c2a26091578cf52a999cebc6b64df478d37ad4dce96376b711e3b27c and primary EB4C1BFD4F042F6DDDCCEC917721F63BD38B4796 are pinned by the offline reviewer; the primary identity comes from Google’s official key-details page, not a keyserver lookup.

| Captured artifact | Verified signing subkey | Result |
| --- | --- | --- |
| Android Gradle plugin 8.13.2 JAR | 0F06FF86BEEAF4E71866EE5232EE5355A6BC6E42 | gpgv 0; SHA256; primary binding valid |
| HeifWriter 1.2.0-rc01 AAR | 0E225917414670F4442C250DFD533C07C264648F | gpgv 0; SHA256; primary binding valid |
| Activity Compose 1.11.0 AAR | 0F06FF86BEEAF4E71866EE5232EE5355A6BC6E42 | gpgv 0; SHA256; primary binding valid |

Exact detached-signature hashes and VALIDSIG statuses are retained in the current review JSON. Temporary public-only keyrings verified the actual captured bytes. Unused expired historical subkeys do not override the valid signer result; expired/revoked actual signers, wrong primary binding or weak digest are refused. The real altered-AAR check failed as required. Nine collector / eight reviewer cases passed.

That review initially brought the candidate to 78/921; later batches extend that snapshot. The current reviewed record owns the remaining-file count. Other publisher/native identities, unsigned metadata and current security/license review remain open. No group-wide Google trust, build-time signature policy or final release acceptance is inferred. No application, dependency version, normal validation request, signing identity or Android build input changed.


## Larger Google Maven batch — requested

The next data-only request selects 48 remaining JAR/AAR files, 24 Gradle module files and 24 POM files from exact baseline773 identities. Its 96 entries declare 255,852,544 bytes including detached-signature allowances; the collector caps requests at 128 entries / 64 KiB / 256 MiB / 120 seconds. Signature-only collection avoids redundant published-checksum requests while keeping SHA256 capture pins and unauthenticated report defaults.

Gradle aliases such as ui.aar and annotation-metadata-*.jar are retained separately from canonical Maven filenames. A guessed retrieval route cannot approve bytes: both the exact captured checksum and a separately verified signature bound to the inspected primary key are required. Four offline planning cases cover aliases, baseline identity, namespace isolation, size/count budgets and skipping already attempted unsigned/failed files; eleven collector cases cover capture refusal and truthful reports. Actual batch collection and signature review completed below. The expanded partial subset stays inactive; no Android build inputs or normal validation request changed.


## First larger batch — 55 additional exact files verified

[Actions 40061369748](https://github.com/santirodriguez/FixMyShot/actions/runs/40061369748), control be41345736cc65642c81c9e12b9c9a083d298dfd / job 120775403769, SUCCESS in 45 seconds; collection took 37 seconds. Actual artifact 12546604299 / 30,429,102 B / ZIP SHA256 3f096b69f4ea3ccf3927d61879282d0ff44b2ab591df541d1877330d10b308fd passed size, CRC, source/request and all 96 captured-file pins. Request SHA256 d6afd620fee71b219c097204c76448ac24ae5b9b6376cc0e2e3f187bcbff0963; inspected prior 78-file receipt SHA256 9a4ea0f5f82fa438c74b4487299a7b2bcfa820ab459b34733adfa819de62e8c6.

| Files attempted | Verified | Signature absent | Signature unverified with inspected key |
| --- | ---: | ---: | ---: |
| 48 JAR/AAR | 38 | 3 | 7 |
| 48 module/POM | 17 | 19 | 12 |
| **96 total** | **55** | **22** | **19** |

Each new accepted file passed exact baseline SHA256, detached signature, strong digest, Google primary binding and a separate signing-key validity check. [GNU documents](https://www.gnupg.org/documentation/manuals/gnupg/gpgv.html) that gpgv itself does not check expired/revoked keys; the helper now inspects primary/actual-signer state and times in the pinned public bundle independently. The original 78 proofs were rechecked with that guard and retain the same exact artifact/proof identities. A real changed batch artifact and the actual Google signer after its advertised expiry were rejected. This does not establish fresh online revocation/security status.

After the first larger batch, the inactive XML/JSON subset contained 133/921 exact files, 788 pending: 64 AAR, 263 JAR, 175 module and 286 POM files. The 41 unsigned/unverified attempts are explicit deferred outcomes; the planner skips them automatically. No broad namespace trust, weak-signature allowance, active strict policy, Android rebuild or release acceptance follows. Canonical source hashes and per-file results are in the review JSON; raw capture/proof receipts remain source-bound in TESTING.


## Second larger batch — 77 additional exact files verified

[Actions 40062206602](https://github.com/santirodriguez/FixMyShot/actions/runs/40062206602), control 81af60aa7ddee8d71dedb217d178f1c6599022de / job 120777943158, SUCCESS in 30 seconds; collection took 25 seconds. Actual artifact 12547517709 / 6,319,600 B / ZIP SHA256 a8b77a2438fcda6e2c2c6f413e9ae5fdac8620860b5c0d40b6ae48354289d221 passed size, CRC, source/request and all 96 captured-file pins. Request SHA256 89c768f9f5ba2fceee1f995c43200df5aa914315896b8e0cff4e474d8deb675d; prior accepted 133-file receipt SHA256 288ea7b181f487857833e8f8b10fbdffeedc6207a523057d36f86dde4c5e1e50.

| Files attempted | Verified | Signature absent | Signature unverified with inspected key |
| --- | ---: | ---: | ---: |
| 48 JAR/AAR | 31 | 12 | 5 |
| 48 module/POM | 46 | 2 | 0 |
| **96 total** | **77** | **14** | **5** |

Each new accepted file passed exact baseline SHA256, actual detached signature, strong digest, inspected Google primary binding and explicit signing-key validity checks. All 133 previous reviewed identities/proofs remain unchanged. Source collector tests passed; actual changed batch bytes and a wrong control pin were refused. The new request was independently checked against the stable repository baseline and omitted all previous attempts before execution.

After this batch, the inactive candidate contains 210/921 exact files, 711 pending: 47 AAR, 249 JAR, 151 module and 264 POM. Sixty deferred unsigned/unverified attempts across both batches remain within that backlog and are excluded from automatic replanning. No group-wide trust, current vulnerability/license verdict, active strict policy, signing, Android rebuild or release acceptance. Current counts and per-file outcomes are owned by the review JSON; snapshots here describe their specific source-bound runs.


## Third larger batch — partial capture, 63 additional exact files verified

[Actions 40097278077](https://github.com/santirodriguez/FixMyShot/actions/runs/40097278077), control f60178606ad269097ecbf9740361bbc5f7fd95d9 / job 120882057272, finished FAILURE in 27 seconds; collection took 19 seconds. This is an incomplete data capture, not a failed Android build. Actual artifact 12561213357 / 9,231,510 B / ZIP SHA256 ae1876cecab60a3349aa2662bec817664f1945f3fda3af234324bfc14b04f07d passed size, CRC, source/request checks and all 87 matched capture pins. The remaining capture, com.android.tools.analytics-library:protos:31.13.2 JAR, returned FETCH_OR_VALIDATION_ERROR under its 4 MiB limit; its precise cause was not separately established. It remains deferred, with no retry or increased limit.

Request SHA256 e60ec6979cbd0fb7e9dd6585d9c67611e53c9d4d6e5bbceb290f23b1e94ad2ff; prior accepted 210-file receipt SHA256 8f6810b272af7f5b69309ea59ad0dd9a62b8edd131199eea6f37b3dc6906b858. A successful hash match is still not publisher approval: each new accepted file separately passed its actual detached signature, strong digest, inspected primary binding and signing-key validity checks.

| Files attempted | Verified | Signature absent | Signature unverified | Capture unavailable |
| --- | ---: | ---: | ---: | ---: |
| 48 JAR/AAR | 30 | 10 | 7 | 1 |
| 40 module/POM | 33 | 3 | 4 | 0 |
| **88 total** | **63** | **13** | **11** | **1** |

All 210 earlier artifact/proof identities are preserved. After this batch: 273/921 reviewed, 648 pending (34 AAR, 232 JAR, 134 module, 248 POM), including 85 deferred outcomes across 280 attempts. No automatically retried failures, broad issuer trust, active strict policy, Android rebuild, current vulnerability/license verdict or release acceptance.

The then-staged [128-file request](https://github.com/santirodriguez/FixMyShot/blob/2852666a38cf8c5e0f43653e1658bc97557c3504/.ci/google-maven-next-request.json) combined all 128 remaining unattempted files on the Google Maven route: 46 JAR and 82 module files. Its 58,505-byte request declares 252,706,816 bytes including signatures (241 MiB), within the existing 64 KiB / 128-entry / 256 MiB / 120-second bounds. Exact baseline identities, hashes, canonical routes and omission of all 280 attempts/all 273 approved files were checked. That separately staged filename did not trigger workflows; it was deliberately activated for the fourth batch below and its duplicate was retired. The ordinary planner is unchanged. Current counters and exact outcomes remain owned by the review JSON.


## Combined Google-route batch — 95 additional exact files verified

[Actions 40098770322](https://github.com/santirodriguez/FixMyShot/actions/runs/40098770322), control 45b620e855ea1324cf07d1c03e443e30b3dc9269 / job 120886424226, finished FAILURE in 39 seconds; collection took 32 seconds. The exact staged 128-file request was promoted and its duplicate retired. Artifact 12561454540 / 9,986,707 B / ZIP SHA256 4e387bbcd4b6ff703c35557d992d7179f37a3936bdfa3df113a700b696864863 passed size, CRC, source/request checks and all 123 matched-file pins. No SHA256_MISMATCH was reported.

Five JAR captures returned FETCH_OR_VALIDATION_ERROR under their existing 4 MiB limits: builder 8.13.2, bundletool 1.18.1, intellij-core 31.13.2, kotlin-compiler 31.13.2 and lint-checks 31.13.2. Their precise failure causes were not separately established. They remain deferred without retries or raised limits. Request SHA256 40f06ba4d2dac6f860aa72c3f368760a75ab5ee100ccb10a8e44cee82c394865; prior accepted 273-file receipt SHA256 4c2f67aba06d5e4a266fa25923d819665c539d7821ea2c8b00eee8ad1e460da0.

| Files attempted | Verified | Signature absent | Signature unverified | Capture unavailable |
| --- | ---: | ---: | ---: | ---: |
| 46 JAR | 38 | 3 | 0 | 5 |
| 82 module | 57 | 13 | 12 | 0 |
| **128 total** | **95** | **16** | **12** | **5** |

Each new approved file independently passed exact baseline bytes, actual detached signature, strong digest, Google primary binding and explicit signing-key validity checks. All 273 earlier artifact/proof identities are preserved. After this batch: 368/921 reviewed, 553 pending (34 AAR, 194 JAR, 77 module, 248 POM). All 408 planned Google-route attempts are recorded; the three earlier critical files bring that route to 293 verified / 118 deferred, with no unattempted files left. The remaining backlog is 118 deferred Google-route files plus 435 files from other publishers. No broad issuer trust, current security/license verdict, active strict policy, Android rebuild or release acceptance.

## Next publisher evidence — pinned Kotlin release manifests

The official JetBrains/kotlin release API exposes independently published SHA256 pins for two checksum manifests. [Source anchors](evidence/kotlin-publisher-manifests.json) retain release/asset identities, sizes and URLs: v2.2.0 asset 266482769 (150,918 B, SHA256 abde7191f66e42bc01f38cb7190ecc4c142ef3303ce9dc0573c18a34519ab363) and v2.2.20 asset 291372484 (158,512 B, SHA256 12df17501d75511d19cd5a34120f44afa48b501b7ac6950930e9be673987b5f8).

The staged [.ci/kotlin-publisher-next-request.json](../../.ci/kotlin-publisher-next-request.json) requests only these two manifests, at most 512 KiB total. It is not a workflow trigger and does not approve any artifact. Their actual bytes and full coordinate/file matches remain unreviewed. Four older matching Maven-manifest assets were found without a published API digest; the expected filename was not found on three other queried releases. No substitute hash or authentication was invented. This prepares a separate publisher step while keeping the 118 deferred Google-route cases out of automatic retries.
