# A good release should be boring to install

<p align="center"><strong>One APK. Clear notes. The same app when you update.</strong><br /><sub>FixMyShot · GitHub first · Obtainium-friendly</sub></p>

> [!IMPORTANT]
> **Preparation, not publication.** The current candidate is unreleased `0.1.0-alpha.1`, versionCode `1`. Production signing, independent reproducibility, install/upgrade and the image/device gates remain open. This guide does not authorize signing, merge, tags or publication.

## The release kit

| Owner | Purpose |
|---|---|
| [ledger.json](releases/ledger.json) | Candidate version and distributed-version history |
| [Notes template](releases/TEMPLATE.md) | Benefits, improvements, limits, downloads and an expandable technical receipt |
| [BUILD-INFO template](releases/BUILD-INFO.template.json) | Non-secret source/build/signature receipt, completed from actual evidence |
| [CHANGELOG](../CHANGELOG.md) | Real changes, clearly marked unreleased until distribution |
| [check-release.py](../scripts/check-release.py) | Offline source identity check and optional notes-draft rendering |

## 01 · Keep the identity straight

Production uses `org.cajapersonal.fixmyshot` through prereleases and eventual public 1.0. Debug uses `.debug`; it cannot establish a production upgrade path. Keep FixMyShot's independent signing identity across updates.

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

Record the immutable candidate commit. Use a clean checkout and the exact tools/wrapper/locks/native pins in [CONTRIBUTING](../CONTRIBUTING.md). Preserve corresponding source, native dependency sources/notices and actual tool versions.

```sh
bash scripts/bootstrap-gradle.sh
python3 scripts/check-release.py
./gradlew --no-daemon :core:test :app:lintDebug :app:assembleRelease
```

The output `app/build/outputs/apk/release/app-release-unsigned.apk` is **unsigned**. Existing CI debug/test APKs are also not production releases. Independently rebuild the unsigned candidate in another clean environment and compare exact hashes. Reusing the same output/cache is not independent. Investigate differences before claiming reproducibility. Version locks and captured hashes alone do not authenticate dependencies.

## 02b · Compare one independent unsigned build

After inspecting a baseline build, commit an explicit [rebuild request](../.ci/independent-rebuild-request.json) containing its exact source commit/tree, original run/artifact IDs, ZIP checksum and actual unsigned APK checksum/size. The [independent-rebuild workflow](../.github/workflows/independent-rebuild.yml) runs only when that request changes on a release branch. It has one hosted build job, no emulator, no signing and no restored dependency/build caches. The ordinary validation marker remains separate.

The controller validates the request, then the workflow checks out the exact requested source into the normal workspace. The controller/request survive outside that checkout. Before building, require the pinned commit/tree, pristine worktree, empty Gradle user home and absent project outputs. Build with the source's SDK/wrapper/locks and --no-build-cache --rerun-tasks, run core tests, inspect the actual release APK libraries and compare unsigned SHA256 plus byte length. A mismatch fails after writing its report; keep the actual APK for local entry/native-digest diagnosis instead of rerunning blindly.

Artifact names identify the workflow control commit and run attempt; evidence/source-sha.txt, source-tree.txt, source.tar and independent-rebuild.json identify the compiled source. Inspect the actual run and downloaded artifact before accepting the comparison. A fresh [GitHub-hosted VM](https://docs.github.com/en/actions/concepts/runners/github-hosted-runners) and an empty cache are execution properties, not dependency/publisher authentication. Even identical APK bytes leave strict dependency verification, physical acceptance and signing gates open. This same-environment byte comparison also does not prove portability to every build host.

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

Before requesting separately authorized merge/tag/publication, reconcile exact source and signed bytes, applicable gates, independent rebuild, ledger, APK manifest, tag, GitHub prerelease flag and final assets/hashes. Scan README, changelog, About/version, authentic screenshots and notes for stale claims. Branding artwork is not an app screenshot. Do not claim the planned website or stores are live.

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
with `python3 scripts/test-apk-legal.py`. A pass establishes those five files
only: it does not approve transitive copyright/NOTICE completeness, dependency
licenses, corresponding source, publisher trust or distribution.
