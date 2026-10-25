# Runtime license declarations and embedded notices

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
