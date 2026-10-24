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

The actual unsigned APK baseline is23,495,607 bytes/SHA25666ae94204ff3de3f76936dbf7f3f96245783e15dfbb89a995297b0ff4e6b4738, matching its archived receipt. Independent byte inspection passed9 native libraries/arm64-v8a/armeabi-v7a/x86_64/ELF-ZIP alignment. The explicit rebuild request pins this exact source and baseline. Its new workflow/controller compile that commit afresh with empty project/Gradle caches and no build-cache reuse, keeping source and control identities distinct. Six local controller refusal/comparison cases passed. Actual clean compilation/byte comparison is pending the requested run; no rebuild success is inferred from source-audit success.

This check does not promote generated dependency metadata. Independent artifact/publisher authenticity, reviewed strict verification and final release reproducibility/signing/physical gates remain open. Keep the exact original and rebuilt evidence, and investigate a byte mismatch before another run. See [RELEASING](../RELEASING.md) for the controlled procedure.
