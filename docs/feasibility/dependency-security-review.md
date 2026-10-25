# Dependency security · evidence before verdicts

> Historical773 snapshot:24October2026 · **inventory complete for that graph; security gate OPEN**

The first bounded OSV inventory covers the captured **514 Maven components** and the exact native WebP source commit. It found **54 advisory IDs across 25 Maven components**. This is a database-match inventory, not an exploitability verdict or release approval.

| Selection scope | Components queried | Components with active matches |
| --- | ---: | ---: |
| App `releaseRuntimeClasspath` | 132 | 0 |
| Selected in other project configurations | 176 | 18 |
| Captured without project-lockfile selection | 206 | 7 |
| Native WebP commit | 1 | 0 |
| **Total** | **515** | **25** |

No match means **no database match at the scan time**, including for the native commit. It does not prove safety, exhaustive ecosystem coverage, publisher authenticity or absence of vulnerable code inside Android/system codecs or shaded binaries. Runtime selection is from the app lockfile, not a complete APK reachability analysis.

## What needs action

| Family | Evidence and applicability | Next action |
| --- | --- | --- |
| Kotlin Gradle plugin 2.2.21 | `GHSA-r937-wjx7-w2jp` / `CVE-2026-53914`. The plugin is explicitly selected by `gradle/libs.versions.toml` and the root/module build scripts despite absence from project dependency lockfiles. The linked upstream fix changes **KAPT incremental-cache deserialization**. None of the current root/app/core/native/settings/catalog files applies KAPT or declares annotation processors. | Treat the advisory as a real tooling match with **no configured KAPT execution path found in this source review**. Recheck if KAPT is introduced. Review a compatible maintained Kotlin/AGP/Gradle set at the next toolchain checkpoint; do not close the security gate or claim that disabling Gradle build-cache alone fixes it. |
| Netty 4.1.93 / 4.1.110 | Twelve matched coordinates belong to `_internal-unified-test-platform-*` configurations in app/native locks. They are not selected in the app's release runtime. Advisory records include HTTP/TLS/resource-consumption findings, including database-rated high/critical entries. | Review the owning AGP/Unified Test Platform upgrade and its resolved graph together. Do not force a partial Netty family upgrade or suppress the findings merely because they are tooling. Keep device/emulator control isolated from untrusted network peers. |
| Protobuf 3.24.4 | Two matched coordinates are selected only in Unified Test Platform configurations. `GHSA-735f-pc8j-v9w8` remains associated with both Java and Kotlin packages. | Resolve with the owning test-tool stack and verify its selected patched runtime. No app-runtime exposure follows from these lock associations. |
| Bouncy Castle | Three selected matched coordinates (`bcpg` 1.80, `bcpkix` 1.80, `bcprov` 1.80.2) belong to `kotlinBouncyCastleConfiguration`. Other captured versions lack project-lockfile selection. | Review the Kotlin-owned tooling configuration, actual use and compatible updated resolution. Preserve the distinction between vulnerable package versions, exercised crypto operations and release-runtime inclusion. |
| OpenTelemetry 1.41.0 | One matched coordinate belongs to `swiftExportClasspathResolvable`, including in the core lock. | Review through its Kotlin tooling owner; do not report it as Android app telemetry. |
| Seven captures without project-lockfile selection | Kotlin plugin plus older Commons Compress, HttpClient, jose4j, JDOM and two Bouncy Castle coordinates. Lockfile absence alone cannot determine buildscript/plugin selection or deselection. | Resolve their owners from buildscript/plugin dependency reports at the next grouped toolchain check. **The Kotlin plugin is already confirmed selected.** Keep unresolved entries open until configuration evidence establishes their disposition. |

The Kotlin applicability inference is based on the exact upstream [fix commit `bf51df66`](https://github.com/JetBrains/kotlin/commit/bf51df665b458fda7c3eaf436c4d88dc119d7ec6) and the inspected current Gradle source files. The [OSV advisory](https://osv.dev/vulnerability/GHSA-r937-wjx7-w2jp) supplies the database match; its fixed ranges are retained as advisory data, not a tested upgrade prescription. The independent rebuild already used a fresh Gradle home and `--no-build-cache --rerun-tasks`; that evidence is not a general vulnerability remediation.

No dependency version, lockfile, application behavior or build-cache setting was changed by this inventory. Upgrades need a coherent graph refresh, compatibility review and one deliberately grouped Android validation. Publisher verification, complete license/notice review, native issuer trust and physical-device gates remain open independently.

## Prioritize the next checkpoint

The 387-file reviewed subset at c667c23 leaves 534 exact baseline files pending. Cross-checking the captured inventory with the pinned app `releaseRuntimeClasspath` and the reviewed identities gives the following order:

| Pending scope | Files | Breakdown |
| --- | ---: | --- |
| Components selected in the app release runtime | 83 | 25 AAR, 15 JAR, 23 module and 20 POM files |
| Remaining captured tooling, tests and metadata | 451 | Not selected in that app runtime configuration; buildscript/plugin use may still apply |

This is component-selection evidence, not proof that every listed file is packaged in the APK. Of the original 91 priority files, 61 use the already attempted Android/Google Maven route and remain deferred. The other 30 were attempted in the runtime publisher capture: eight exact Kotlin files passed, while 22 remain deferred (16 lack an issuer anchor, three signatures are unverified and three captures unavailable). The current 83-file runtime backlog is those 61 plus 22. See [the retained capture](evidence/README.md#runtime-publisher-capture) for reproduction and limits. Do not automatically retry the 61 deferred files, trust a signature key obtained only from the artifact's signature, or activate an incomplete verification policy. The other 451 files still matter to a strict build freeze.

Handle tooling remediation in one deliberate checkpoint: first recover buildscript/plugin configuration ownership for the unresolved captures, then choose a compatible AGP/UTP and Kotlin set, refresh the complete owned graphs and rerun the bounded inventory and Android validation once. Avoid forced individual Netty/Protobuf/Bouncy Castle overrides that leave their owning stack untested. Until then, keep the security gate open and retain the precise KAPT applicability limitation above.

The next ordinary Android checkpoint captures Gradle's `buildEnvironment` reports
for the root, app, core and codec-native projects in
`evidence/buildscript-dependencies.txt`, with a SHA256 receipt, alongside the
existing exact source and build evidence. Read the selected versions and parent
paths (including conflict-resolution arrows), not just matching coordinate text.
These reports describe the selected buildscript classpaths; they do not prove
that every tooling operation ran, establish APK inclusion or cover every detached
configuration. A missing path remains unresolved. No dependency overrides or
toolchain upgrades are introduced merely to collect this evidence. Review the
actual report before choosing a compatible upgrade; collection alone does not
close the security gate.

## Evidence that survives the hosted artifact

| Receipt | Value |
| --- | --- |
| Control source | `64745e1f88df2ecf488f1fc650854f186f6abe7a` |
| Hosted run / job | [40101103449](https://github.com/santirodriguez/FixMyShot/actions/runs/40101103449) / `120893241464`, SUCCESS |
| Scan time | `2026-10-24T14:07:58.707977+00:00` |
| Hosted artifact | `12563041227`, 218,443 bytes |
| [Retained ZIP](evidence/dependency-review-64745e1f.zip) SHA256 | `7b6326aa2630771de0a8b8f4819e2c576e843f68fe6d93b177ff3ad2bc9857c7` |
| Security request SHA256 | `a4db419df21aa031a8d9344f70487583d023e5b5280ab66bf0634c9d273540ba` |
| Captured security report SHA256 | `15642d453e400aebf63ae551c23ba021155c745c447a966188136b890c172f10` |
| [Offline replay receipt](evidence/dependency-security-review-64745e1f.json) SHA256 | `44256ded200958ca584443e7305baa9b7a8217507b230a21ed3198b02a81e4dc` |

The ZIP contains the two publisher manifests plus six OSV query batches and 54 advisory detail responses. The paired request pins the baseline XML, all three lockfiles and the WebP CMake source. Each captured request/response has a byte count and SHA256. The replay validates all 68 security capture receipts, exact query order/identities, scopes, complete results and detail identities. It also checks withdrawn advisories and groups aliases without adding alias IDs to the advisory count.

The original collector called lockfile-absent captures `capture_only_unselected`. The replay and current collector use **`capture_without_lockfile_selection`**, because the original label overstates what lockfile evidence establishes. Original bytes remain intact in the pinned ZIP.

## Reproduce offline

For this historical replay, use a pristine checkout of immutable8a6ba7828488a02382a0ba64b368f030b57c3959 with its own scripts and old lockfiles; the migrated checkout intentionally refuses those old input pins. From that historical repository root, verify the ZIP digest above before extracting it into a fresh directory. The inspected archive has 73 unique regular files, no unsafe paths or links, and passed CRC validation. No captured file should be executed.

```bash
sha256sum docs/feasibility/evidence/dependency-review-64745e1f.zip
unzip -q docs/feasibility/evidence/dependency-review-64745e1f.zip \
  -d /tmp/fixmyshot-dependency-evidence-64745e1f
python3 scripts/review-dependency-security.py \
  /tmp/fixmyshot-dependency-evidence-64745e1f/security \
  /tmp/fixmyshot-security-replayed.json \
  --expected-control 64745e1f88df2ecf488f1fc650854f186f6abe7a \
  --expected-request a4db419df21aa031a8d9344f70487583d023e5b5280ab66bf0634c9d273540ba \
  --expected-report 15642d453e400aebf63ae551c23ba021155c745c447a966188136b890c172f10
cmp /tmp/fixmyshot-security-replayed.json \
  docs/feasibility/evidence/dependency-security-review-64745e1f.json
```

For the Kotlin extension, recover the previous receipt with `git show cbb9414a58be1efc77c501e70d7fcb8026fbfb19:gradle/dependency-provenance-review.json` into a temporary file, then run `scripts/review-kotlin-manifests.py` with the baseline XML, that prior receipt, `evidence/kotlin-publisher-manifests.json` and the pins in [committed inputs](committed-gradle-inputs.md). Its JSON/XML outputs reproduce the historical **379-file inactive candidate** at 8a6ba782, not the current 387-file receipt. Compare with that immutable historical receipt; then replay the runtime extension using the separate commands in [the evidence owner](evidence/README.md). This does not run Gradle or activate `gradle/verification-metadata.xml`.


## Selected buildscript ownership — exact ad075068

The [retained Gradle report](evidence/buildscript-ad075068.txt) is31565B, SHA256
20d443df827c3fa07fd8314ceb0f7eef131d2f6d3e1835f7dfa231457e22647e, recovered
from build artifact12579476722 and verified against its emitted SHA256 receipt.
Its exact source/run/package acceptance is in [the checkpoint receipt](evidence/android-ad075068.json).
Root classpath owns the plugin graph; app/core/codec-native buildEnvironment
report no separate classpath dependencies. Those empty child reports do not
remove inherited plugin use. The first expanded root paths establish:

| Previously lock-absent capture | Observed selected path | Disposition |
| --- | --- | --- |
| commons-compress1.21 | AGP8.13.2 → sdk-common31.13.2 → sdklib/repository →1.21 | Selected tooling match; OPEN |
| httpclient4.5.6 | AGP → sdklib → httpmime4.5.6 → httpclient4.5.6 **→4.5.14** | This path selects4.5.14; captured4.5.6 is not selected here. Not a global safety verdict. |
| jose4j0.9.5 | AGP → bundletool1.18.1 →0.9.5 | Selected tooling match; OPEN |
| bcpkix1.79 | AGP → sdk-common31.13.2 →1.79 | Selected tooling match; OPEN |
| bcprov1.79 | AGP → sdk-common/bcpkix/builder/apkzlib →1.79 | Selected tooling match; OPEN, separate from Kotlin's locked1.80.x |
| jdom2.0.6 | AGP → jetifier-processor1.0.0-beta10 →2.0.6 | Selected tooling match; OPEN |
| kotlin-gradle-plugin2.2.21 | Kotlin Android/JVM plugin markers →2.2.21 | Selected tooling match; existing no-configured-KAPT inference retained |

Six captured versions are confirmed selected, including the already known
Kotlin plugin. One captured requested version is superseded on this graph.
No match is dismissed merely as tooling. These paths do not establish exercised
vulnerable operations, all detached configurations or Android APK inclusion.
No version/override/lock changes were made. Next remediation must address the
owning AGP/UTP and Kotlin configurations together, regenerate evidence and
check the complete resolved graph instead of forcing leaf-library versions.

Primary-source research on24October2026 identified Kotlin2.4.21 as a stable
release ([release history](https://kotlinlang.org/docs/releases.html)).
[The KGP compatibility matrix](https://kotlinlang.org/docs/gradle-configure-project.html)
and [Android D8/R8 requirements](https://developer.android.com/build/kotlin-support)
must be checked together; a KGP matrix entry alone is insufficient. AGP9 also
introduces [built-in Kotlin and DSL migration](https://developer.android.com/build/migrate-to-built-in-kotlin).
This is a significant build migration, not an approved drop-in version bump.
No candidate stack has yet been qualified or claimed to remediate every finding.
Keep the current verified baseline and obtain approval of the migration blueprint
before implementation. No existing feature or physical gate is waived.


## Migrated graph — new inventory required

The accepted migration experiment40152344573 selected AGP9.2.1 and KGP/Compose
compiler2.4.21 and regenerated all three locks. The experimental XML contains
553 components/924 exact artifacts, distinct from the historical514/921 scan
above. Existing387 proofs and54-advisory/25-component counts remain bound to
the historical graph. They are not the current migration's trust/security verdict.
The [migration receipt](evidence/toolchain-migration-afea6e45.json) and
[retained root classpath](evidence/toolchain-buildscript-afea6e45.txt) own the
actual derived evidence; child/UTP associations remain in the reviewed locks.

Commons Compress now selects1.27.1, but older Bouncy Castle/jose4j/JDOM and
UTP Netty versions remain in owning configurations. Kotlin's version change
alone cannot close those findings. First validate the incorporated source and
recover its exact generated inventory; then pin a fresh bounded security scan
to that inventory/source/locks/native input. Do not force leaf versions or label
the compiler upgrade complete vulnerability remediation.


## Security-only refresh after migrated source qualification

A separate contents:read workflow now supports an explicit
`.ci/security-inventory-request.json` containing
`collection_scope=STANDALONE_PINNED_SECURITY_INVENTORY`, the exact baseline source
commit, an allowed `baseline_path` under the canonical evidence directory, and
SHA256 pins for that XML, all three locks and the native source definition.
It is not triggered until that exact request file changes. It runs the same
bounded OSV API collector with `--standalone`, without SDK/Gradle/emulators or
publisher downloads. No previously deferred publisher capture is retried.

The previous provenance-paired mode retains its no-network unpaired-request
skip. Standalone mode requires explicit scope and validates every pinned input
before creating a network client; arbitrary baseline paths are refused. Six
offline inventory/refusal tests and four replay tests pass. The replay schema
continues to bind the exact raw request, hashes, rows, captures and conclusions.
No request for the migrated graph has yet been issued: first recover and verify
the source-frozen checkpoint's exact generated XML, then pin that graph and
locks in the new request. Its inventory remains non-approving evidence.

## Source-frozen migrated build — f9434ab9 inventory requested

Ordinary Actions40160890302/build121072016773 passed core69/lint/three APKs
with unchanged committed version locks. Downloaded build12586877386 matches
size28691208B/SHA2569f97cabc4b853400c956bdcf65cc934a3d6945505a7a59a780fcfd47102f82a6,
CRC/source/archive246-file Git tree/build-input audit and actual native/legal
checks. API36 is still in progress; build success alone does not close it.

The exact frozen [XML](evidence/gradle-baseline-f9434ab9.xml) has487components/
858artifacts, all858 identical to the derived554/925 capture subset. It contains
all364coordinates in the current three version locks. The derived whole-report
experiment captured67additional files/components not exercised by this normal
build; do not conflate its count with the frozen build snapshot. The retained
[root report](evidence/buildscript-f9434ab9.txt) binds actual owning selection.

The standalone security request now pins this frozen XML, all three locks and
native source:488queries including WebP, with explicit per-lock runtime/other
scopes. It does not rerun publisher capture. The separate publisher request
collects only the new official Kotlin2.4.21 checksum manifest; the independent
rebuild request pins this exact source/tree and actual unsigned APK11835411B/
SHA256ae45b1fbefa652c851278f9a720c90597e34ece6eaebd93ff9ac7a5f640f29ab.
These independent requests can run while API36 finishes; all actual terminal
reports/bytes still require review. No trust/security/license approval follows.

## Migrated OSV replay complete — review remains open

Actions40161516912/control6135c1d1 completed all488queries at
2026-10-25T04:20:30.880820Z with no pagination/error. The downloaded154842B
artifact12585884358 matches SHA256/CRC and62safe unique members. Independent
offline replay validates every request/input pin, batch order/count, detail
identity, withdrawn state and conclusion. The [compact receipt](evidence/security-f9434ab9.json)
retains49advisory IDs/18matching components and full raw-replay hashes.
There were no matches for133app-release-runtime coordinates or exactWebPcommit
at scan time; this is not a safety or APK-reachability verdict.

| Matching inputs | Verified owning scope | Remaining review |
| --- | --- | --- |
|12Netty coordinates, versions4.1.93/4.1.110|UTP/test tooling in locked configurations; no app release-runtime selection|Network/HTTP/TLS advisories and actual tool paths remain open; no forced leaf override|
|Commons Lang3.16.0|AGP sdk repository→Commons Compress1.27.1→Lang; also lint/UTP listener locks|GHSA-j288-q9x7-2f5v concerns recursive ClassUtils.getClass on long input; owning-tooling remediation/applicability still open|
|HttpClient4.5.6|Actual lint/UTP listener lock selection; root classpath instead selects4.5.14|GHSA-7r82-7xv7-xcpj concerns malformed URI authority. Root upgrade does not close the separately selected old version|
|jose4j0.9.5, JDOM2.0.6|Root AGP→bundletool / jetifier; absent from project locks does not mean unselected|Compressed-JWE/XXE findings still require owning-tool review|
|BouncyCastle bcpkix/bcprov1.79|AGP/tooling and selected other configurations|PKIX/signature/crypto advisory paths remain open; no app/runtime or full-remediation claim|

Do not compare49/18 to historical54/25 as a seven-component remediation count:
the frozen487-component capture differs from the historical514 and derived554
captures. All364locked coordinates are included, but67extra derived-only
components lie outside this frozen scan. The corresponding raw report, original
49detail responses and derived complete replay are in the evidence package.
Its actual uploaded bytes at98bdedb9 match size/SHA256/Git blob/CRC/manifest;
remote evidence persistence is verified. No strict policy, whole security/license or device approval follows.


## Actual packaged tooling-family exposure —source5d568a7e

The unchanged frozen scan still has49advisory IDs across18tooling components;
no new scan or vulnerability count is claimed. A read-only DEX parser checked
the actual accepted debug and unsigned release APKs, identified by their
existing exact SHA256. It validates DEX header/file integrity, bounded string/
type/class tables and all standard multidex members, then examines actual
class definitions and type descriptors. Six offline tests include positive
matches, the tenth DEX, exact package boundaries, malformed tables/integrity
and duplicate/missing payload refusal. No downloaded code is executed.

Debug contains17705class definitions/22565type references; release contains
16986definitions/20769references. Both contain zero definitions and zero type
references under io.netty, org.apache.commons.lang3, org.apache.http,
org.jose4j, org.bouncycastle and org.jdom2. All18matching Maven coordinates map
to these six families. The
[actual package report](evidence/apk-tooling-types-5d568a7e.json) and
[component/49-ID mapping](evidence/tooling-risk-review-5d568a7e.json) retain
per-DEX hashes/counts and every advisory's selected scope. This strengthens
package-inclusion evidence; it does not cover reflection/dynamic code loading,
all Android platform libraries, actual exploitability or build-host safety.
The known package hashes and unchanged graph bind this result to5d source.

| Remaining build operation boundary | Review disposition |
| --- | --- |
| UTP Netty HTTP/HTTP2/SPDY/proxy/CORS/SNI/TLS/decompression and Windows-specific operations | Tool/configuration ownership established; endpoint/input reachability and compatible owning-tool fixes remain open. No finding is waived merely because the current runner is Linux or the app lacks these classes. |
| Commons Lang ClassUtils recursion | Review actual AGP/lint/UTP call sites and lengths of attacker-controlled class names; no universal non-execution assertion. |
| HttpClient URI/authority handling | Root's4.5.14 selection does not remove lint/UTP4.5.6. Review the owning downloader/listener operations and redirects. |
| bundletool jose4j compressed JWE and jetifier JDOM XML parsing | Review enabled operations and untrusted encrypted/XML inputs through their owning tools; dependency presence alone neither proves exploitation nor clears it. |
| Bouncy Castle ASN.1/name constraints/LDAP/GOST/certificate algorithms | Review the selected algorithms, parsing and certificate/key inputs in their owning build tools. No app-key/signing operations were performed. |

Existing build controls were inspected: contents:read workflows on GitHub-hosted
ubuntu-24.04, immutable action pins, checkout persist-credentials:false, no
signing-secret configuration in these build jobs, central dependency repository
control with FAIL_ON_PROJECT_REPOS and explicit official repositories. The
independent rebuild starts from empty caches. These constrain impact and
reproducibility; they do not authenticate every dependency, isolate all network
inputs or eliminate49known vulnerable-version matches. Remaining issuer trust
and actual build-operation applicability/remediation stay open.

## Newer owning-tool candidate examined without another migration

Official [AGP9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
list9.3.3, minimum Gradle9.5.0/JDK17 and default NDK28.2.13676358. A bounded
read-only comparison of official9.2.1/9.3.3 root POMs plus9.3.3 repository,
sdk-common and lint POMs found unchanged owner inputs: bundletool1.18.3,
jetifier1.0.0-beta10, httpmime4.5.6, repository→Compress1.27.1 and
sdk-common→bcpkix/bcprov1.79. Raw original POMs/URLs/SHA256 and observed direct
edges are in [the comparison](evidence/agp933-security-comparison/comparison.json).

This is POM evidence, not a newly resolved/conflict-selected candidate graph or
qualified9.3.3build. No claim all49findings persist unchanged in every newer
stack is made. Merely choosing that newer version would not establish
remediation of these unchanged routes; avoid an otherwise unmotivated rebuild
and repeat migration. Current accepted AGP9.2.1/Gradle9.4.1/Kotlin2.4.21 stay
unchanged. Review owning operations/fixes before selecting another coherent
stack; no forced leaf override, automatic risk acceptance or active generated
trust follows from this comparison.
