# Toolchain migration — isolated qualification

Status: implementation authorized; candidate experiment, not release acceptance.
Research:24October2026. Preserve the accepted ad075068 baseline and its byte-identical
independent rebuild while qualifying the owning tooling stack together.

## Selected candidate and authority

| Input | Candidate |
| --- | --- |
| AGP |9.2.1|
| Gradle |9.4.1|
| Kotlin JVM / Compose compiler |2.4.21|
| Android Kotlin |AGP built-in Kotlin; higher KGP selected through root JVM/Compose plugin graph|
| SDK / Build Tools |compile/target36 unchanged; Build Tools36.0.0|
| JDK / NDK / CMake |17 /28.2.13676358 /3.22.1 unchanged|
| Product / formats / ABIs / permissions |Unchanged; no new runtime features or format availability claims|

Primary references: [AGP9.2 compatibility](https://developer.android.com/build/releases/agp-9-2-0-release-notes),
[KGP matrix](https://kotlinlang.org/docs/gradle-configure-project.html),
[Kotlin release history](https://kotlinlang.org/docs/releases.html),
[built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin),
[separate D8/R8 requirements](https://developer.android.com/build/kotlin-support).
The matrices justify a candidate, not a passing build/security assertion. Check the
actually resolved KGP/Compose/D8/R8 versions, advisories and native/runtime output.

Gradle [official checksum reference](https://gradle.org/release-checksums/) and
[official release asset](https://github.com/gradle/gradle-distributions/releases/tag/v9.4.1)
agree on binary ZIP SHA256
2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb.
Official wrapper JAR SHA256 is
55243ef57851f12b070ad14f7f5bb8302daceeebc5bce5ece5fa6edb23e1145c.
No checksum is inferred from a generated or downloaded candidate itself.

## Stage1: source-pinned derived checkout

The opt-in toolchain-migration workflow copies the reviewed controller/request
from its control commit, checks out immutablead075068/treeb5ecadc6, verifies
original launchers before Java execution and checks every mutation input hash.
The recipe removes only kotlin-android plugin declarations, updates AGP/Kotlin/
Build Tools/distribution pins, and resets the three version locks only inside
this disposable pristine checkout. No repository ref is written by the workflow.
The original checked wrapper launches the new checksum-pinned distribution to
generate its standard wrapper; the new JAR is checked against the independent
official anchor before subsequent execution.

Resolve all app/core/native configuration reports together; regenerate version
locks and SHA256 inventory, compile core/lint/debug/release/test APKs, and inspect
release native/legal bytes. Preserve raw reports, exact derived input files,
controller/request/base identities, binary diff and truthful partial outputs.
The artifact is explicitly EXPERIMENTAL_DERIVED_CHECKOUT: gitHEAD identifies its
base, not the changed sources. Build success does not qualify a source-frozen
release, complete publisher trust or runtime regressions. No emulator is run
against these uncommitted derived inputs. Four offline tests enforce source/
input scope and validation before mutation.

## Stage2: reconcile and freeze actual generated inputs

Inspect terminal logs, actual selected graph (including conflict arrows),
wrapper/launcher bytes, complete regenerated locks and metadata against base
and recipe. Review compatibility, security changes and licenses, including any
remaining vulnerable versions. Keep the historical514/921 baseline and387-file
partial issuer review intact; they cannot approve a different dependency graph.
Preserve unchanged exact-file proofs only after comparing identities and hashes.
Do not activate generated trust or force Netty/Protobuf/crypto leaf overrides.

Only then commit the reviewed new build inputs and wrapper with updated guards/
source-audit profiles, toolchain docs and a decision receipt. Run the ordinary
source-frozen build/API36 checkpoint and inspect actual APK/native/legal bytes;
refresh security inventory for its new XML/lock/native pins. Validate new unsigned
reproducibility independently. Failures require scoped corrections; no weak
assertions, hidden format fallback or deletion of old useful evidence.

## Remaining delivery gates

This migration does not complete transitive licenses, publisher/native trust,
physical Pixel plus second-family HEIC/AVIF/fallback/resource/color acceptance,
real-provider/process-death/quota/receiver behavior, language/accessibility,
16KiB runtime, final screenshots, signing, Obtainium install/upgrade or publication.
JXL remains deferred. Main/merge/signing/publication stay separately gated.


## Recipe2: generated-source registration correction

Experiment40149344439/control6c920674 failed before wrapper generation, during
app configuration: AGP9 rejects Provider values passed to the old SourceSet
registration for the branding resource and legal assets. Gradle9.4.1 launched
from its checksum-pinned distribution; no migrated wrapper/graph/APK acceptance
was reached. [The partial receipt](../feasibility/evidence/toolchain-migration-6c920674.json)
preserves exact source/run/artifact identity and the observed boundary.

Recipe2 replaces the complete pinned generator block only in the disposable
checkout. Typed branding/legal tasks expose input files/directories and output
DirectoryProperty values. Each variant owns separate generated tasks/directories;
[addGeneratedSourceDirectory](https://developer.android.com/reference/tools/gradle-api/9.2/com/android/build/api/variant/SourceDirectories)
wires the output and task dependency to resource/asset processing. Source PNG and
legal bytes keep the same canonical owners and APK paths. The legal task clears
only its generated legal subtree before copying current source texts, avoiding
stale generated content. No SourceSet Provider bypass or legacy DSL flag is added.
The original accepted app build remains unchanged on the release branch until
Stage2 review; recipe revision is explicit in the request and controller.


## Experiment2 passed; reviewed inputs incorporated for qualification

Actions40152344573/controlafea6e45 completed SUCCESS: new standard wrapper matched
the official9.4.1 JAR anchor, whole configuration resolution completed, core69
zero-failure/error/skip tests, lint and all three APK compiles passed. Actual
release native9libraries/3ABIs/alignment and five canonical legal assets passed.
[The retained receipt](../feasibility/evidence/toolchain-migration-afea6e45.json)
records all derived input hashes, archive identity and precise limits.
The raw generated XML is retained under a non-active experimental evidence path;
it is not gradle/verification-metadata.xml or a publisher-approved baseline.

Reviewed wrapper/launchers, source recipe outputs and complete three lockfiles
are now incorporated, with the matching pre-execution wrapper guard and dual
historical/current source-audit profiles. The old773 inventory and its387 proofs
are retained unchanged. The candidate has553 components/924 artifacts;582 exact
identities overlap without hash drift and157 prior proof records could be reused
after frozen-source reconciliation. No proof promotion or active trust occurred.

Resolved KGP/Compose compiler are2.4.21; Commons Compress changes to1.27.1.
Some Bouncy Castle1.79, jose4j0.9.5, JDOM2.0.6 and UTP Netty versions remain
selected in tooling configurations. Therefore this is not a complete security
remediation. A fresh bounded inventory and applicability review are required.
Normal source-frozen build/API36/package/source-audit and a subsequent independent
unsigned rebuild are pending. Device/manual/native-format/signing gates remain.

## Locked-read failure and graph completion

Normal source-frozen Actions40156666980 atff01c514 failed in
:app:processDebugNavigationResources: kotlin-stdlib-common2.4.21 was resolved
in debugRuntimeClasspath but absent from that configuration's lock state.
The previous experiment compiled with --write-locks and therefore did not
prove locked-read completeness. API36 was skipped; migration acceptance is open.

Validation revision3 preserves the first generated locks, then lets Gradle
update only org.jetbrains.kotlin:kotlin-stdlib-common with existing graph
constraints while executing the real compile/lint tasks. It records the exact
log and snapshots all three final locks plus metadata. A subsequent full
--no-build-cache --rerun-tasks build uses no lock/metadata write option and
requires all four snapshots unchanged. Failure preserves partial evidence.
This tests a bounded correction; no hand-written lock entry, disabled lock
validation, version override or claim of source-frozen acceptance is introduced.
Only exact observed output followed by this read-mode pass can be considered
for incorporation and another ordinary source-frozen/API36 checkpoint.

## Locked-read correction passed; normal qualification requested

Actions40157298004/control3af69cdd passed. Gradle's bounded common-module update
added only debugRuntimeClasspath, debugUnitTestRuntimeClasspath and
releaseRuntimeClasspath to kotlin-stdlib-common2.4.21 in the app lock. Core/native
locks and all other incorporated derived inputs are byte-identical. The following
read-mode build executed all219tasks with no-build-cache/rerun-tasks, no lock or
metadata writes, and unchanged three-lock/XML digests. Core69, lint/three APKs,
native9libraries/3ABIs/alignment and five canonical legal files passed.
The exact [receipt](../feasibility/evidence/toolchain-migration-3af69cdd.json) and
two raw build logs retain identities, scope and limits. Only the observed generated
app lock correction is incorporated. Metadata554/925 remains an unauthenticated
capture, not an active trust policy. The ordinary source-frozen/API36 request is
renewed; its actual output and security inventory still need independent review.

## Normal frozen-source/runtime and independent rebuild accepted

Sourcef9434ab9/treeec4efb43 passed ordinary40160890302: core69/lint/threeAPK
build, exact source246-file tree/build-input audit, actual canonical legal/native
bytes and complete API36 suites/80real encodes/four local report guards.
[The receipt](../feasibility/evidence/android-f9434ab9.json) retains precise scope.
Independent40161516814 rebuilt that same source with empty caches and produced
exactly identical11835411-byte unsigned output, confirmed against downloaded
actual bytes; [its receipt](../feasibility/evidence/independent-rebuild-f9434ab9.json)
owns the comparison. This closes the migration's bounded build/runtime/reproducibility
qualification, without extending emulator diagnostics to physical formats/16KiB.

The frozen build captures487/858, an exact subset of derived554/925 with no hash
drift and all364locked coordinates present. New security and publisher evidence
are separately reviewed:488queries complete with49tooling advisory IDs/18matching
components;149unchanged exact proofs plus67new Kotlin-manifest proofs yield216/858,
642pending. Old387/921 is unchanged. Raw dependency evidence is committed at98bdedb9 and actual uploaded bytes match
size/SHA256/Git blob/CRC/manifest. Strict trust, full security/licenses,
physical/native formats and signing/distribution remain open. No active policy,
leaf override, format availability or shipping permission changed.
