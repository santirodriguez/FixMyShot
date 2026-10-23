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
