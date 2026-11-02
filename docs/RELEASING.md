# A good release should be boring to install

<p align="center"><strong>One APK. Clear notes. The same app when you update.</strong><br /><sub>FixMyShot · GitHub first · Obtainium-friendly</sub></p>

> [!IMPORTANT]
> **Preparation, not publication.** The unreleased candidate remains `0.1.0-alpha.1`/versionCode `1` in [draft PR #1](https://github.com/santirodriguez/FixMyShot/pull/1). Executable source `7cbed17d` passed [build/lint/core99 and API36 instrumented135tests](https://github.com/santirodriguez/FixMyShot/actions/runs/41338068328); original source/APK/artifact bytes were independently checked. HEIC/AVIF/JXL are explicitly deferred from0.1.0; normal new outputs are PNG and lossless WebP. The verified unsigned release APK is12,340,030bytes,SHA256 `1e38786776a466b3809dca84d7cfba45dc6f3196781f3aca7979507260f1ee42`. It is not signed or distributed. The exact-source independent rebuild and bounded source/license engineering review are complete. Applicable remaining manual acceptance, stable signing custody, sequential upgrades, final screenshots and separately authorized merge/tags/publication remain open; see [current acceptance](TESTING.md#current-pngwebp-automated-acceptance--2026-11-01) and [status](STATUS.md). The exact `7cbed17d` unsigned runtime now has a verified byte-identical independent rebuild in [Actions41369848088](https://github.com/santirodriguez/FixMyShot/actions/runs/41369848088); this does not prove a future changed distribution source or signed bytes. Nothing here authorizes a key, signature, merge or release.

## Approved assurance exception and unsigned receipt

The maintainer approved [Option B](decisions/dependency-assurance-alpha-proposal.md)
only for the exact alpha/input set/APK bound in the
[assurance receipt](releases/0.1.0-alpha.1-assurance.json). All325reviewed and
533pending identities retain their labels; no broader publisher trust or active
Gradle verification is introduced. Do not reopen completed issuer captures for
this exact exception. Changed bindings require a new decision.

The [unsigned BUILD-INFO draft](releases/0.1.0-alpha.1-unsigned-build-info.json)
is populated from original CI evidence, the independent byte-identical rebuild
and verified source-material receipts. It is not final public BUILD-INFO.json.
The [exact-alpha source/license review](feasibility/runtime-license-review.md#exact-alpha-license-and-source-disposition--2026-11-02)
now closes declared-license compatibility/current canonical notices; the
[N01 implementation review](feasibility/runtime-license-review.md#n01-closed-by-implementation-correspondence--2026-11-02)
also closes the remaining source-correspondence hold. All49compiled classes'
actual layouts/instructions/bootstrap references correspond within its recorded
scope. The exact candidate's source/license engineering review is complete.
The current source-material archive is reusable unchanged. Final signed asset/
source co-location and source/tag/build-info reconciliation still belong to the
separately authorized distribution handoff; they do not reopen N01. Applicable
remaining device/resource/accessibility requirements and truthful screenshots
retain their own scope.

No production signing identity is established by the repository evidence.
Before any separately authorized key/signing operation, resolve whether an
existing FixMyShot identity must be reused and how its stable custody/backup
is maintained. Do not infer that an external key does not exist. Never put keys
or passwords in chat, source, artifacts or receipts. After applicable preparation
and explicit signing authority, sign only the bound unsigned bytes, then record
the public certificate, verify actual signed APK/manifest/alignment, and complete
fresh install, sequential same-certificate upgrade and Obtainium evidence.
A later public release needs separate authority.

## Bounded private-candidate signing plan — not authorized

Status: **PREPARED / CUSTODY UNRESOLVED / NOT AUTHORIZED**.
This is a reviewable scope for a later explicit decision, not permission to
create a key, write repository secrets, sign, install, hand off or publish.

| Binding | Exact prepared target |
| --- | --- |
| Application | org.cajapersonal.fixmyshot |
| Version |0.1.0-alpha.1, versionCode1 |
| Compiled source/tree |7cbed17de44f4bc4ff31b62270ace3bd5a51b492 /32723ea9cbaac3b1db3f6f891cb368bf8b11aa18 |
| Unsigned APK |12,340,030bytes; SHA2561e38786776a466b3809dca84d7cfba45dc6f3196781f3aca7979507260f1ee42 |
| Baseline/reproduction |CI41338068328 / independent41369848088, full-byte identity |
| Source materials |Existing34104279-byte archivec9124e8d3d1e966d3b4dbfd1b1eb0fa8a88c7b51af655ce8a438ab9cbd5f48fa; N01closed |
| Intended output after future authority |One privately staged FixMyShot-0.1.0-alpha.1-universal.apk and non-secret verification receipt |

Before requesting execution, establish whether an independently held FixMyShot
identity already exists. If so, reuse its verified public certificate and approved
private custody; do not replace it. If not, a new FixMyShot-only long-lived
identity and independent encrypted backup need explicit authority and an approved
custody/access channel. Repository evidence establishes neither an existing key
nor its absence. No secret, keystore, password or recovery material belongs in
chat, source, public logs, build evidence or these receipts. This work does not
provision a private-key channel or ask the maintainer to install/run local tools.

The later authorized operation would:

1. Verify the exact original unsigned hash/size and preserve those bytes.
2. Use the established FixMyShot identity through its approved custody channel;
   record only the public certificate fingerprint.
3. Sign one output copy with the existing pinned Android SDK tooling, without
   rebuilding the app or changing ID/version/permissions/defaults/codecs.
4. Verify the actual signature, manifest/versions/permissions and ELF/ZIP alignment;
   update non-secret BUILD-INFO from actual signed bytes. Never alter signed bytes.
5. Stage the output privately for separately authorized acceptance. Installation/
   handoff, repository secrets/permissions, a second candidate, merge/tag/release,
   store upload and website deployment are not silently included.

Signing a validation candidate does not close the remaining [manual matrix](TESTING.md#current-delivery-reconciliation--2026-11-02)
or establish functional-release readiness. All pending acceptance must retain
its actual status. A Go for preparation is not execution authority.

### Sequential upgrade and Obtainium dependency

Only code1is frozen. M09 requires two same-ID/same-certificate candidates with
increasing codes. A future code2or other changed artifact requires a separate
concrete version/source/input review, build evidence and applicable assurance
decision; the exact code1B exception is not inherited. Do not change the planned
ledger, manufacture an older candidate or call an equal-code reinstall an upgrade.

The GitHub source in Obtainium reads release information/assets. No release
currently exists, so authentic install/update tracking cannot be established from
the unsigned CI artifact alone. Preserve it as NOT_RUN until a separately
authorized channel and suitable candidates exist. Any proposal to change M09's
first-release timing or requirement needs an explicit governing amendment;
none is adopted by this plan. This dependency does not authorize publication.

Primary references checked2026-11-02:
[Android signing/custody](https://developer.android.com/studio/publish/app-signing)
and [Obtainium GitHub source](https://wiki.obtainium.imranr.dev/sources/#github).

## The release kit

| Owner | Purpose |
|---|---|
| [ledger.json](releases/ledger.json) | Candidate version and distributed-version history |
| [Notes template](releases/TEMPLATE.md) | Benefits, improvements, limits, downloads and an expandable technical receipt |
| [BUILD-INFO template](releases/BUILD-INFO.template.json) | Non-secret source/build/signature receipt, completed from actual evidence |
| [CHANGELOG](../CHANGELOG.md) | Real changes, clearly marked unreleased until distribution |
| [check-release.py](../scripts/check-release.py) | Offline source identity check and optional notes-draft rendering |

## 01 · Keep the identity straight

Production uses `org.cajapersonal.fixmyshot` through prereleases and eventual public 1.0. The verified current debug diagnostic uses the `.heicbytes` suffix (`FixMyShot HEIC Sample`), a retained diagnostic identity rather than a claim of HEIC output support. Earlier diagnostics used separate suffixes such as `.ux22next` (`FixMyShot Next`) and `.debug`. None establishes the production upgrade path. Keep FixMyShot's independent signing identity across updates.

`versionName` is SemVer without the tag's `v`; the checker supports plain/prerelease versions without build metadata. Tags later use `v0.1.0-alpha.1`, etc. `versionCode` starts at 1 and increases for every subsequently distributed production-ID APK, including prereleases. Never reuse a code for different distributed bytes. A changed public APK needs a new version/code, not a replacement asset under the old identity.

The ledger is ordered by increasing code, with at most one `planned` row, last. Keep its name/code aligned with `app/build.gradle.kts`, README and the exact unreleased changelog heading. After actual distribution, record `distributed`, signed-APK SHA-256, public certificate SHA-256 and immutable source commit. Retain that row unchanged when appending the next candidate. A never-distributed planned row can be corrected. The checker validates current files; it does not authenticate edited history or inspect GitHub release state.

```sh
python3 scripts/check-release.py
python3 scripts/test-release.py
# Optional: a NEW local notes draft. Edit every EDIT marker afterward.
python3 scripts/check-release.py --notes-output /tmp/fixmyshot-release-notes-draft.md
```

The renderer refuses existing files and keeps the unpublished notice. It does not inspect/sign an APK, generate artifact receipts or upload anything.

## 02 · Build from one recoverable source

Record the immutable candidate commit. Use a clean checkout and the exact tools/wrapper/locks/native pins in [CONTRIBUTING](../.github/CONTRIBUTING.md). Preserve corresponding source, native dependency sources/notices and actual tool versions.

```sh
bash scripts/bootstrap-gradle.sh
python3 scripts/check-release.py
./gradlew --no-daemon :core:test :app:lintDebug :app:assembleRelease
```

The output `app/build/outputs/apk/release/app-release-unsigned.apk` is **unsigned**. Existing CI debug/test APKs are also not production releases. The earlier e516aa89 byte-identical rebuild must not be substituted for an independent rebuild of the eventual **frozen final candidate**. At that gate, independently rebuild the same exact source in another clean environment and compare hashes. Reusing the same output/cache is not independent. Investigate differences before claiming reproducibility. Version locks and captured hashes alone do not authenticate dependencies.

## 02b · Compare one independent unsigned build

After inspecting a baseline build, commit an explicit [rebuild request](../.ci/independent-rebuild-request.json) containing its exact source commit/tree, original run/artifact IDs, ZIP checksum and actual unsigned APK checksum/size. The [independent-rebuild workflow](../.github/workflows/independent-rebuild.yml) runs only when that request changes on a release branch. It has one hosted build job, no emulator, no signing and no restored dependency/build caches. The ordinary validation marker remains separate.

The controller validates the request, then the workflow checks out the exact requested source into the normal workspace. The controller/request survive outside that checkout. Before building, require the pinned commit/tree, pristine worktree, empty Gradle user home and absent project outputs. Build with the source's SDK/wrapper/locks and --no-build-cache --rerun-tasks, run core tests, inspect the actual release APK libraries and compare unsigned SHA256 plus byte length. A mismatch fails after writing its report; keep the actual APK for local entry/native-digest diagnosis instead of rerunning blindly.

Artifact names identify the workflow control commit and run attempt; evidence/source-sha.txt, source-tree.txt, source.tar and independent-rebuild.json identify the compiled source. Inspect the actual run and downloaded artifact before accepting the comparison. Compare two builds of the **same compiled source commit**. AGP embeds that Git
revision in META-INF/version-control-info.textproto, so even a build-settings
change can alter the full APK digest while all application payloads stay equal.
For a diagnosed cross-source transition, scripts/compare-apk-source-transition.py
checks both exact expected source records and every other entry, rejects changed
payloads/entry metadata/duplicates and records any compression/header relocation.
Its four refusal/positive checks run with scripts/test-apk-source-transition.py.
It is not a byte-identical rebuild verdict. Keep a failed literal comparison
visible, establish the actual new baseline, then use the unchanged strict
byte comparator for another clean build of that same source.

A fresh [GitHub-hosted VM](https://docs.github.com/en/actions/concepts/runners/github-hosted-runners) and an empty cache are execution properties, not dependency/publisher authentication. Even identical APK bytes leave strict dependency verification, physical acceptance and signing gates open. This same-environment byte comparison also does not prove portability to every build host.

## 02c · Stage recoverable project and native sources

The explicit [source-kit workflow](../.github/workflows/source-kit.yml)
reuses this existing offline packer when
[its pinned request](../.ci/source-kit-request.json) changes on a release
branch. It checks out the requested exact project and official pinned
WebP commit with read-only, non-persisted checkout credentials. It
generates twice, checks every manifest member, restores original Git
identities offline and regenerates a third identical archive. It
does not build/sign/publish an APK. The accepted7cbed17d kit is now verified in Actions41372832634. Use the bundled root PAX producer/helper from control185b465e rather than the older USTAR producer inside the unchanged compiled project snapshot. An Actions artifact is
temporary staging, not a public release/source download or complete
other-runtime/build-tool source mirror.

Use [prepare-source-kit.py](../scripts/prepare-source-kit.py) with existing local
public Git objects, an explicit accepted source commit/tree and the exact
libwebp repository. It performs no fetch, build, signing or upload. The example below is specifically for the **historical e516aa89 guarded source**,
not the current UX/editor head. Recreate the exact project-source kit only after
freezing the actual distribution candidate; do not relabel this old kit as its source:


```sh
python3 scripts/prepare-source-kit.py /path/to/FixMyShot /path/to/libwebp \
  /new/staging/FixMyShot-source-e516aa89.tar.gz \
  --source e516aa89efae94fe2376fd9f62e143218b49ccac \
  --tree 6089799f41df2239ab111e172b7989cccee07bf9 \
  --receipt /new/staging/source-kit-e516aa89.json
```

The tool reads exact Git blobs rather than dirty working files. It stages all
468project files and all343pinned WebP files, original notices, per-file
SHA256/Git blob/mode manifests, original public commit objects, the exact packer and an offline
restoration helper. It refuses unsafe/symlink/submodule entries, wrong source
pins and existing outputs. Sorted archive entries, fixed modes/ownership/time
and a timestamp-free gzip header make repeated output identical with the same
packer/Python/zlib. The [actual receipt](feasibility/evidence/source-kit-e516aa89/review.json)
records two full23003112-byte generations with identical bytes and successful
restoration of both exact pristine Git identities. The restored kit
regenerated the complete archive using its own bundled packer and Git objects,
without the original producer workspace or a network fetch. Six fixture tests exercise
mutations, missing/extra files, modes, symlinks, wrong commit/tree and existing
repositories. Run python3 scripts/test-source-kit.py.

Extract into a fresh staging directory with modes preserved, then use its
restore-source-snapshot.py to validate every file and restore local shallow
Git identities without a network request. AGP embeds the project commit in the
APK, so a source archive without its original Git identity is insufficient for
the existing full-byte reproduction procedure. The helper refuses existing
.git directories and writes only inside the two extracted source directories.

This kit supplies exact project/full-WebP sources; NDK helper sources and the
AndroidX native excerpts are already in its project evidence. It is not a mirror
of every Maven/SDK dependency, a complete offline build or completion of all
corresponding-source obligations. The [runtime source stage](feasibility/runtime-source-review.md) now adds94
published companions and mapped upstream Protobuf sources with exact archive
pins; it remains separate from this project/WebP kit. Complete the remaining
source/build-material and distribution review before calling the final release
source-complete. The
accepted CMake configuration remains unchanged; this does not validate a new
host or change the compiled source. The binary archive is regenerable from
public pinned Git objects; do not duplicate it as another large Git evidence
upload. Stage the final accepted source asset with release materials only when
the remaining gates and publication authority apply.

## 02d · Assemble the staged runtime source materials

Current7cbed17d materials are prepared and independently verified in
[Actions41373814270](https://github.com/santirodriguez/FixMyShot/actions/runs/41373814270).
The [receipt](feasibility/evidence/source-materials-7cbed17d.json) binds
the34104279-byte/1282-member archive to the exact nested project/WebP
kit,94companions,mapped Protobuf and1074original AAR inputs. Its two
generations and all members matched; previously captured input pins,
runtime lock and catalog were preserved. No publisher/source/license
clearance or publication follows. The current assembler pins7cbed;
historical e516 assets require their historical producer, not relabeling.

[The source-material assembler](../scripts/prepare-source-material-bundle.py)
combines the exact existing project/WebP kit,94published companions, mapped
upstream Protobuf, original AAR resource/manifest/rule inputs and committed
supplemental source/review evidence. It performs no network, build or signing.

```sh
python3 scripts/prepare-source-material-bundle.py \
  /stage/FixMyShot-source-7cbed17d.tar.gz /stage/runtime-source-companions-e516 \
  /stage/runtime-frozen-binaries-e516 \
  docs/feasibility/evidence/runtime-source-companions-e516aa89 \
  docs/feasibility/evidence/runtime-source-closure-e516aa89 \
  /stage/protobuf-javalite-4.28.2-sources.jar \
  /new/FixMyShot-source-materials-7cbed17d.tar.gz /new/source-materials.json
```

The [historical e516aa89 receipt](feasibility/evidence/source-material-bundle-e516aa89.json)
records two identical assemblies and full1,282member validation. It is not yet a
corresponding-source asset for the eventual frozen distribution candidate. After extraction,
run sha256sum -c MANIFEST.sha256; extract the nested original source kit separately
and retain its Git restoration instructions. This is a source-material asset,
not final corresponding-source or publisher approval. Review the [exact limits](feasibility/runtime-source-review.md)
before distribution; regenerate for a later accepted compiled source. Do not
upload the32.8MBbinary through connector blob/base64 calls. An actual future
manual upload must identify the exact file and destination explicitly.

## 03 · Verify the exact signed candidate

Creating FixMyShot's long-lived key, securing backups and signing require separate explicit authority. Never borrow another app's key or regenerate a missing identity. Keep keys/passwords outside the repository and receipts; record only the public certificate fingerprint.

After authorized signing of the verified artifact, run these in an isolated staging directory using the **existing Android SDK tools**:

```sh
apksigner verify --verbose --print-certs FixMyShot-0.1.0-alpha.1-universal.apk
apkanalyzer manifest print FixMyShot-0.1.0-alpha.1-universal.apk
python3 /path/to/FixMyShot/scripts/check-apk.py FixMyShot-0.1.0-alpha.1-universal.apk
```

Compare actual app ID/name/code/minSdk, non-debuggable state and permissions with source/ledger. Compare the public certificate to the established identity. Check all shipping ABIs and actual ELF/ZIP 16 KiB alignment; alignment is not runtime 16 KiB acceptance. Retain unsigned and signed hashes. Never edit/realign signed bytes; reverify the signature after final staging.

Primary tool references, checked 2026-10-23: [apksigner](https://developer.android.com/tools/apksigner) · [apkanalyzer](https://developer.android.com/tools/apkanalyzer). These are maintainer-environment steps, not a request to install tools on the user's computer.

## 04 · Package three clear assets

| Public asset | Contents |
|---|---|
| `FixMyShot-<versionName>-universal.apk` | One signed production-ID APK, all shipping ABIs |
| `BUILD-INFO.json` | Source/tag, unsigned/signed digests, public cert, actual tools and exact acceptance evidence |
| `SHA256SUMS.txt` | Hashes of the final signed APK and completed BUILD-INFO.json |

Complete the BUILD-INFO template from actual records; null/NOT_RUN values are reminders, not final evidence. Include recoverable corresponding source and pinned native build/license records. Exclude operator paths, device serials, private images and credentials. A checksum cannot contain itself.

Once BUILD-INFO is final, from the isolated asset directory:

```sh
sha256sum FixMyShot-0.1.0-alpha.1-universal.apk BUILD-INFO.json > SHA256SUMS.txt
sha256sum -c SHA256SUMS.txt
```

Use the notes template, keep user benefits first and technical receipts expandable. Replace every EDIT marker and missing field with verified facts. Use tag-bound artwork/source links. Verify all three asset links after authorized publication. Keep README accurate before publication instead of presenting a dead Download button. Rendering notes does not establish release acceptance.

## 05 · Actually install and update

Use exact signed bytes for a fresh install and an upgrade from a successively versioned same-ID/same-certificate APK. The first release still needs a recorded sequential signed install/upgrade exercise. Confirm preferences, new-session PNG/metadata-removal defaults, imports, gallery output and receiver behavior. Physical/fidelity/accessibility and other blueprint gates remain required; see [TESTING](TESTING.md).

For **Obtainium**, add `https://github.com/santirodriguez/FixMyShot`, enable prereleases for 0.x and use `^FixMyShot-.*-universal\.apk$` if filtering is needed. The checker validates the proposed asset name against this pattern. Actual tracking/install/update remain NOT RUN; no community-catalog request is required.

## 06 · The final handoff

Before requesting separately authorized merge/tag/publication, reconcile the **then-current** candidate source and signed bytes, applicable gates, independent same-source rebuild, corresponding-source closure, publisher evidence/explicit disposition, ledger, APK manifest, tag, GitHub prerelease flag and final assets/hashes. The diagnostic APK acceptance does not authorize production signing or release. Scan README, changelog, About/version, authentic screenshots and notes for stale claims. Branding artwork is not an app screenshot. Do not claim the planned website or stores are live.

Initial functional 0.1.0 is a GitHub prerelease; public 1.0 remains the later stable milestone. F-Droid/Google Play submissions, website deployment and key operations have separate authority. This preparation does not publish, merge or sign anything.


## Canonical legal assets

The app's per-variant PrepareLegalTask instances package the root GPL LICENSE,
THIRD_PARTY_NOTICES and LICENSES/ from their canonical files. WebP's exact
COPYING/PATENTS remain in the native module assets. Do not maintain another
manually edited copy of these texts under app/src/main/assets.

Run `python3 scripts/check-apk-legal.py <actual-candidate.apk>` against a clean
checkout of the exact candidate source. It requires one copy of each mandatory
asset, matching the canonical bytes, and refuses missing, duplicate or altered
entries. The ordinary Android workflow checks both debug and unsigned release
packages and preserves the JSON reports. The four offline checker cases run
with `python3 scripts/test-apk-legal.py`. A pass establishes those eight files
only: it does not approve transitive copyright/NOTICE completeness, dependency
licenses, corresponding source, publisher trust or distribution.

The canonical license directory also retains the exact upstream Protobuf BSD
notice for DataStore's repackaged protobuf-javalite and the133component runtime
declaration catalog. The checker verifies those two assets as well. The audit
[owner](feasibility/runtime-license-review.md) records source pins and limits;
asset presence alone is not full native/source/license compatibility acceptance.


The native-source follow-up adds LICENSES/Android-NDK-cpufeatures.txt: original
BSD source/header notice plus the separate module Apache NOTICE. The checker
now requires eight exact legal assets. The grouped build verifies the actual
installed NDK source against its pinned release before package acceptance. See
[native source review](feasibility/native-source-review.md); an earlier seven-
asset package does not qualify a later notice update.
