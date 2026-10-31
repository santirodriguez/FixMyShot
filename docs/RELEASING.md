# A good release should be boring to install

<p align="center"><strong>One APK. Clear notes. The same app when you update.</strong><br /><sub>FixMyShot · GitHub first · Obtainium-friendly</sub></p>

> [!IMPORTANT]
> **Preparation, not publication.** The functional editor candidate is unreleased `0.1.0-alpha.1`/versionCode `1` at `d54eb9c7` in [draft PR #1](https://github.com/santirodriguez/FixMyShot/pull/1), not the default branch. [Exact-head build/lint/API36 checks](https://github.com/santirodriguez/FixMyShot/actions/runs/40966098425) and the user's next-image diagnostic exercise passed, **not** the physical codec/fidelity/accessibility or production distribution gates. Same-source unsigned reproducibility is proven only for the historical `5d568a7e`/`e516aa89` source baselines, **not** this HEAD. Production signing, new final-source rebuild and source-closure checks, sequential upgrades, final screenshots, merge, tags and publication remain open; see [status](STATUS.md). Nothing here authorizes a key, signature, merge or release.

## The release kit

| Owner | Purpose |
|---|---|
| [ledger.json](releases/ledger.json) | Candidate version and distributed-version history |
| [Notes template](releases/TEMPLATE.md) | Benefits, improvements, limits, downloads and an expandable technical receipt |
| [BUILD-INFO template](releases/BUILD-INFO.template.json) | Non-secret source/build/signature receipt, completed from actual evidence |
| [CHANGELOG](../CHANGELOG.md) | Real changes, clearly marked unreleased until distribution |
| [check-release.py](../scripts/check-release.py) | Offline source identity check and optional notes-draft rendering |

## 01 · Keep the identity straight

Production uses `org.cajapersonal.fixmyshot` through prereleases and eventual public 1.0. Current isolated diagnostics use the `.ux22next` debug suffix (`FixMyShot Next`); earlier diagnostics used other separate suffixes, including `.debug`. None establishes the production upgrade path. Keep FixMyShot's independent signing identity across updates.

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

[The source-material assembler](../scripts/prepare-source-material-bundle.py)
combines the exact existing project/WebP kit,94published companions, mapped
upstream Protobuf, original AAR resource/manifest/rule inputs and committed
supplemental source/review evidence. It performs no network, build or signing.

```sh
python3 scripts/prepare-source-material-bundle.py \
  /stage/FixMyShot-source-e516aa89.tar.gz /stage/runtime-source-companions-e516 \
  /stage/runtime-frozen-binaries-e516 \
  docs/feasibility/evidence/runtime-source-companions-e516aa89 \
  docs/feasibility/evidence/runtime-source-closure-e516aa89 \
  /stage/protobuf-javalite-4.28.2-sources.jar \
  /new/FixMyShot-source-materials-e516aa89.tar.gz /new/source-materials.json
```

The [historical e516aa89 receipt](feasibility/evidence/source-material-bundle-e516aa89.json)
records two identical assemblies and full1,282member validation. It is not yet a
corresponding-source asset for current `d54eb9c7` or a later final candidate. After extraction,
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
