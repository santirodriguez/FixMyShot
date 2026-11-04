# Runtime license declarations and embedded notices

## N01 closed by implementation correspondence — 2026-11-02

[Actions41517838551](https://github.com/santirodriguez/FixMyShot/actions/runs/41517838551)
completed the isolated comparison at controld5a9030e. The
[original-artifact receipt](evidence/n01-implementation-7cbed17d/review.json)
and full original/reviewed reports retain the evidence, including negative results.
All229original artifact members and input hashes were independently verified.

The26unchanged Java source files compile to all49classes in Emoji2's
repackaged implementation. All actual class layouts (121fields/513methods),
available instruction listings and lambda bootstrap handles/arguments correspond
after the two declared namespace relocations and the bounded representation
review below. All4Noto-generated classes match the strict disassembly comparison.
This closes the specific N01 material-correspondence hold; it does not identify
the original historical build checkout.

The initial disassembly report remains90/94, **DIFFERENCES_REQUIRE_REVIEW**.
The4differences were inspected, not discarded:
- Published FlatBufferBuilder/FlexBuffersBuilder and compiled FlatBufferBuilder
  use ldc versus ldc_w after constant-pool rewriting. Mapping byte positions to
  actual instruction ordinals preserves every branch/switch destination, operand
  and literal; no instruction is removed.
- javap displays the compiled Table$1 synthetic constructor without its
  synthetic parameters. Both actual JVM descriptors are
  (Landroidx/emoji2/text/flatbuffer/Table;Ljava/nio/ByteBuffer;)V. Actual member
  names/flags/descriptors are checked before normalizing the pretty argument list.
  Its instructions are unchanged.

The offline reviewer passes94/94 bounded correspondence checks, retaining all90
strict matches. Seven collection and five presentation-review tests pass,
including wrong branch targets, literals/whitespace, opcodes, descriptors,
bootstrap targets/arguments and invalid listings. An actual compiled-member
mutation is refused by the pinned original manifest before review.
The first run41517192700 failed before comparison because Java7 rejects the
actual lambda. Both original binaries are major52/Java8; the corrected build uses
the existing JDK17 with --release8 and -proc:none. That failure is preserved.

The exact candidate's engineering source/license review is now complete with
the recorded limits. The prepared source-material archive remains unchanged.
Do not reopen N01 merely because class bytes, debug/annotation/other attributes,
formal semantic equivalence or the exact historical checkout were not proved.
Those broader claims are not made. The original0/45raw-byte result and all
publisher classifications remain unchanged; OptionB is still issuer-only.
Final signed asset/source co-location and release handoff remain pending under
separate authority, alongside the other applicable delivery controls. This
comparison builds no APK and claims no new phone test, signing or publication.

To replay only the offline review, extract the exact original artifact to a
fresh directory and run:
`python3 scripts/review-repackaged-instructions.py <original-artifact-directory> <new-review.json>`.
It verifies the pinned original manifest/report and refuses changed members.
The source workflow's explicit operation remains n01-implementation; a future
source-material generation must explicitly select source-materials in
.ci/source-material-operation.json. No unchanged regeneration is needed now.

<a id="exact-alpha-license-and-source-disposition--2026-11-02"></a>

## Exact-alpha license and source disposition — 2026-11-02 (before N01 comparison)

Reviewed candidate: executable7cbed17de44f4bc4ff31b62270ace3bd5a51b492,
unsigned12,340,030-byte APK SHA256
1e38786776a466b3809dca84d7cfba45dc6f3196781f3aca7979507260f1ee42.
The [bounded review receipt](evidence/source-license-disposition-7cbed17d.json)
separates completed checks from the one remaining source-correspondence hold.

| Review boundary | Disposition |
| --- | --- |
| Declared managed runtime licenses | No incompatibility identified:132Apache-2.0 and oneBSD-3-Clause declarations, under the unchanged133-component catalog |
| Native notices | Existing WebP COPYING/PATENTS, NDK BSD/Apache and AndroidX Apache source records retained |
| Actual current APK canonical legal assets | PASS: all8required entries match the exact compiled-source originals |
| Published source-archive license texts |47instances of one original Apache text retained; its terms match the canonical text's Apache portion. The canonical file also retains additional build-tool license texts, which do not prove those tools are packaged runtime components |
| Exact source-material archive | All1282members and every manifest digest pass;95source archives (94companions plus mapped Protobuf) are readable |
| Published AAR resource/build inputs |1074files:747parseable XML,161text files,166PNG. PNG owners are AppCompat150/Core16; no compiled npTc nine-patch chunks found. These observations do not prove every image's original preferred editing form |
| Known supplemental omissions | DataStore JNI, Core stable AIDL/build inputs, FlatBuffers Java, Noto schema/generated Java and both relocation mappings are staged |
| Remaining source correspondence | **HOLD N01**: exact external Noto/generation/relocation correspondence for Emoji2 is not established by declaration/constant-set matching alone |

Apache's [compatibility statement](https://apache.org/licenses/GPL-compatibility.html)
supports inclusion of Apache-2.0 software in GPLv3 projects; its
[redistribution conditions](https://www.apache.org/licenses/LICENSE-2.0)
still require the applicable license/notices. The exact project LICENSE
§§1/6 remains the authority for corresponding source. No universal copyright,
patent, security or whole build-tool review is claimed.

N01 is a specific evidentiary limit, not a newly missing library or a request
to repeat the completed FlatBuffers1.12version-identification exercise.
The source/version inventory and notice-byte checks are complete. Final
corresponding-source/distribution clearance stays open until N01 has a defensible
disposition. A useful next investigation must establish the release's external
source/generation context or compare the relevant generated/relocated
implementation with the frozen classes. Repeating the same45declarations,
checksums or source capture cannot close it.

Do not require an entirely offline toolchain or byte-identical upstream AARs
merely to label source material available: those are different claims. Conversely,
a source bundle and a byte-identical app rebuild do not by themselves establish
that historical Noto source corresponds to the bundled implementation.
The [approved issuer-risk exception](../decisions/dependency-assurance-alpha-proposal.md)
does not waive N01. No new APK build, emulator/phone test, dependency/legal-asset
change, signing, merge or publication occurred.

Scope: frozen sourcef9434ab9,487/858 generated inventory and exact app lock.
This review covers declared runtime licenses and archived text, not all native
source obligations, publisher authentication, legal compatibility or release
readiness. Complete input/capture/source pins are in the
[receipt](evidence/runtime-license-review-f9434ab9.json).

## Completed evidence

The first bounded capture obtained110POMs/76archives;26component rows needed
completion. A separately pinned resume reused107complete rows and fetched only
the26missing rows. The final capture has133POMs and95archives matching the exact
frozen SHA256, with zero capture errors. Those archives contain57license-text
instances, one unique Apache text. Archive counts refer to captured artifacts
associated with selected coordinates, not proof all their classes are packaged.

132components directly declare a license; listenablefuture1.0 inherits from
Guava parent26.0-android. That exact official parent POM declares Apache-2.0.
Reviewed declarations total132Apache-2.0 and oneBSD-3-Clause. The normalized
[offline catalog](../../LICENSES/runtime-components.json) retains component and
declaration-source identity, including metadata-only entries. Neither an origin
claim nor a generated hash is promoted into the separate publisher trust review.

## Corrected Protobuf omission

The exact datastore-preferences-external-protobuf1.1.7 JAR matches
fcb3f373cf743426c85ebb0d3cc644aefaf26f39b7fa55bb90238d41b689eca7.
Its POM declares BSD-3-Clause, but the archive has no embedded license/notice.
The published sources JAR is261bytes and contains no source/license payload;
no source availability was inferred from its filename.

Google's [1.1.7 release entry](https://developer.android.com/jetpack/androidx/releases/datastore#1.1.7)
links release commit420da5f291449f11801e378c6789a328a06ec6cb. Its exact
[build file](https://android.googlesource.com/platform/frameworks/support/+/420da5f291449f11801e378c6789a328a06ec6cb/datastore/datastore-preferences-external-protobuf/build.gradle)
relocates protobufLite into androidx.datastore.preferences.protobuf, and its
[version catalog](https://android.googlesource.com/platform/frameworks/support/+/420da5f291449f11801e378c6789a328a06ec6cb/gradle/libs.versions.toml)
pins protobuf-javalite4.28.2. Gitiles bytes were recovered directly; the GitHub
mirror lacks that release-branch commit. The exact upstream v28.2 license at
[commit9fff46d7](https://github.com/protocolbuffers/protobuf/blob/9fff46d7327c699ef970769d5c9fd0e44df08fc7/LICENSE)
is retained verbatim at LICENSES/Protobuf-BSD-3-Clause.txt, including copyright,
conditions, disclaimer and generated-code qualification.

THIRD_PARTY_NOTICES links this component and its license. The existing generated
legal task copies the two new canonical files into the APK. The package checker
now requires seven exact canonical assets and still reports full-review=false.
The oldf943 package fails the updated canonical check. Normal sourcee8fd9c0f
now passes build/native/seven-filelegal/API36, with actual package bytes checked
against the exact archived canonical sources. Independent40192020106 also passed; actual downloaded unsigned bytes equal
the baseline exactly. Updated source has bounded build/runtime/reproducibility
acceptance, with broader native/source/legal gates unchanged. No dependency,
format, permission, source pixel or export behavior changed.

## Evidence persistence and remaining scope

The original migrated225KB evidence ZIP is uploaded at98bdedb9 and actual
SHA256/Git blob/CRC/manifest match. The new dependency-audit-746cc9d1.zip holds
this audit's raw POMs/notices, original and completed reports/producers, source
references and the new Google signature capture. Its
[package receipt](evidence/dependency-audit-746cc9d1.json) records13170421B/SHA256
6c1befb07d88cb24b24792ec5def69a7ecc1d151820ca9a3c1dd5798037bd833;
actual remote bytes uploaded atb9256abd match size/SHA256/Git blob,282members,
CRC and every manifest entry; remote persistence is verified.

Source/native notices, all required attribution and broader legal compatibility
still need their appropriate review. This does not assert universal copyright
or patent clearance, whole build-tool licensing or complete artifact trust.
The current publisher subset is325/858,533pending, with all six critical files
corroborated. The [source-copy review](google-source-copy-review.md) distinguishes
exact official Google source copies and JetBrains-owned checksum records
from detached signatures and earlier release manifests.


## Native notice follow-up

The [native source review](native-source-review.md) separately traced libwebp,
its compiled NDK cpufeatures source and AndroidX native C++ files. The NDK
source has a BSD two-clause header distinct from its module's Apache NOTICE;
both are now preserved in a new canonical legal text. The package checker
requires eight assets for this newer source. e8's seven-file acceptance remains
historical. Normal5d568a7e now passes actual installed NDK source pins, both
eight-asset APK checks, build/native/core/lint/API36 and local report replay.
Its new exact-source independent rebuild40197042011 also passed; the actual
downloaded unsigned APK matches the original full file byte for byte. This
closes updated-package reproducibility, not whole source/security/legal gates.


The exact accepted e516aa89project/full-WebP source kit is now prepared and
independently regenerated/restored; see [its receipt](evidence/source-kit-e516aa89/review.json).
This stages recoverable project/native material without changing packaged
notices or claiming complete source distribution for every other runtime
component. Source-kit generation is offline and does not build/sign/publish.


The [runtime source review](runtime-source-review.md) now stages94published
companions and mapped upstream Protobuf4.28.2 sources, with complete archive
hash/CRC/member-digest replay. Seven empty companions were checked against
actual exact-hash frozen binaries: all contain zero class implementations;
original small resource/manifest/rule texts are retained. This source-material
staging does not approve additional publisher files or final distribution.
