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
