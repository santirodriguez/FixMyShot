# Completion status

## First public 1.0.0 conditional delivery acceptance — 2026-11-06

This is the latest release-readiness decision; older dated status below remains historical. Exact icon1 signed source `7649f0e`, SHA-256 `92efebea8ebfce6a88865a8b4842f61e4362d8951fb7949ea626c0a0245ea428`. The original signed APK installed, launched and relaunched successfully on a genuine emulated Android35 x86_64 **16,384-byte** page-size system in [Actions #42292629807](https://github.com/santirodriguez/FixMyShot/actions/runs/42292629807); the native signed export and physical 16KiB device were **NOT_RUN**. Separate debug JNI/WebP/metadata smoke and maintainer-reported signed functional behavior retain their own bounds. The previously agreed first GitHub release scope explicitly accepts remaining 100-item mixed, exhaustive human languages/TalkBack, independent signed EXIF/GPS/color and physical-device observations as `NOT_RUN`/`PARTIAL`, not PASS.

The [later bounded matching-source/license review](releases/1.0.0-7649f0e-final-source-license-disposition.md) and [release approval decision](releases/1.0.0-7649f0e-final-release-approval.json) document original 95 frozen runtime binary owners, 57 embedded original license members and no original packaged `NOTICE` member, plus the exact full source TAR, original third-party legal texts and required same-release public source access. Original signed-era `THIRD_PARTY_NOTICES`, risk and source-capture snapshots are retained, **not retroactively changed**. If any required notice/source is found absent or GitHub cannot make exact original source publicly accessible, publication must stop. This review does not claim an offline Android build or exact historic generated-byte equality.

Current rollout remains **conditional**: exact original signed/source stage and hash confirmation, final PR merge-ref CI, main merge and stable public GitHub Release each require their real check. The publisher workflow has an explicit manual-dispatch boundary. This paragraph does not itself assert publication or tag creation.

## icon1 Home screenshot now canonical — 2026-11-06

The maintainer approved the actual API36 Home capture from [Actions 42226235342](https://github.com/santirodriguez/FixMyShot/actions/runs/42226235342) and committed the exact image as the **only** modified path in [08bfa43](https://github.com/santirodriguez/FixMyShot/commit/08bfa43d9f4cc06b8f0d9d6b713aa547e68703ed). `fastlane/metadata/android/en-US/images/phoneScreenshots/1.png` is **363925 bytes**, approved-original SHA-256 `c9c55bed86c56478eeb3474130f00e3726e4ea89fb87375cbff2b23d4169975f`, confirmed blob `829599968a412393fc80fbf722edba071e3675f8`. Canonical `2.png`–`6.png` are byte-identical to their previous approved Git blobs. The README image URL and Home alt text are already correct. [ARTWORK](ARTWORK.md#current-canonical-home-screenshot--icon1-approved-2026-11-06) owns the exact provenance; dated sections below that still call screenshot #1 stale are historical.

**Still blocked:** adaptive launcher mask/themed icon review, remaining explicit physical/manual/API26–28/real 16 KiB, language/accessibility/privacy/provider acceptance, final source/license distribution clearance, and stable asset kit/publication. The existing release staging request intentionally remains `PREPARED` with `branding_revision_pending=true` because it is still pinned to an old-icon signed APK, not the accepted icon1 candidate. Do not mark the old kit READY, merge, tag, sign again or publish from this screenshot approval.

## Signed icon1 APK field acceptance and CI-trigger discipline — 2026-11-06

The maintainer **approved the exact signed icon1 APK in a basic real-world trial** (`92efebea8ebfce6a88865a8b4842f61e4362d8951fb7949ea626c0a0245ea428`, 12,675,989 bytes; see [TESTING](TESTING.md#signed-icon1-apk-basic-maintainer-acceptance--2026-11-06)). Record `MAINTAINER_REPORTED_BASIC_APK_ACCEPTANCE`; preserve separate missing clean-install details, runtime 16 KiB, screenshot #1, full manual/accessibility/locale/privacy/SAF and full corresponding-source/license distribution gates. This is not public release authorization.

Draft pull-request updates no longer start both heavy workflows on every `synchronize` event. Android CI retains explicit release-branch request-file pushes; release-kit verification retains its explicit release-preparation pushes. Both also validate on PR `opened`, `reopened` and `ready_for_review` only. New changes after a PR was marked ready must be freshly validated against the exact current merge candidate before any separately authorized merge. See [release CI policy](RELEASING.md#draft-pr-ci-trigger-policy--2026-11-06).

## Current exact icon1 signed validation candidate — 2026-11-06

The first-public 1.0.0/code1 icon1 candidate, compiled from `7649f0e002aca9c91cdf1294ab3033a9572029a9` / tree `a222fb9964e2f328c59acf352e23e67e45650744`, was signed **without rebuilding** by [Actions 42226227569](https://github.com/santirodriguez/FixMyShot/actions/runs/42226227569). The original signed APK is **12,675,989 bytes**, SHA-256 `92efebea8ebfce6a88865a8b4842f61e4362d8951fb7949ea626c0a0245ea428`; its certificate SHA-256 `33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445` is the established independent FixMyShot app signer. Independent checks passed for the original Actions ZIP and unsigned provenance, v2/v3 RSA signatures/content digests, 467/467 unchanged APK payloads and 9/9 static 16 KiB native-library ZIP/ELF alignment. See [current signed receipt](releases/1.0.0-7649f0e-signed-build-info.json) and [bounded issuer-risk receipt](releases/1.0.0-7649f0e-assurance.json). Earlier 2026-11-05 old-icon signed status sections below are historical.

The one-time signing request is **PREPARED** again and cannot authorize a second signing. Public release staging remains **PREPARED** on the earlier historical APK and is explicitly blocked by `branding_revision_pending=true`; it must later be reconciled to icon1 only after separate source/license distribution and physical/manual acceptance. The new-icon signed APK now has maintainer-reported basic working-app acceptance; clean-install details, actual 16 KiB runtime, screenshot #1 approval, full accessibility/locales/privacy/SAF and publication gates remain open. No `main` merge, tag, GitHub Release, website, store submission or key/permission change occurred. Administrative disarm/receipt commit intentionally skips redundant PR CI; final required checks must run before any authorized merge.

## Branding refresh and limited signed-APK field report — 2026-11-05

The maintainer selected `icon1.png` as the new Android/store icon and uploaded `promo.png` for Google Play/F-Droid. These original files are organized under [`assets/branding/primary/`](../assets/branding/primary) and copied byte-identically to canonical Fastlane `en-US/images/icon.png` (512×512 RGBA) and `featureGraphic.png` (1024×500 RGB). All other original icon variants are retained in `assets/branding/alternatives/`, README/Gradle refer to the renamed header, and all six approved native screenshots remain unchanged. See [ARTWORK](ARTWORK.md).

**Maintainer field report:** the previously delivered, signed 1.0.0 APK was tried successfully in a basic check. This is a useful positive manual observation for that **old-icon APK**, but no complete device, 16 KiB runtime, 100-item, SAF, metadata/color, accessibility or three-locale matrix was reported. The signature `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde` is bound to prior compiled source `3437722`. The newly selected launcher icon changes shipped resource inputs and requires a new build and independent checks; the earlier signed APK must not be treated as the public new-icon candidate. The stable request stays `PREPARED` with `branding_revision_pending=true` as a hard staging/publishing refusal. Previous approvals do not automatically extend to the changed executable. Full source/license and other manual gates also remain open; no signing, merge, tag or release authorized.

## Exact 1.0.0 release-kit binding and device handoff — 2026-11-05

The stable [release request](../.ci/development-release-request.json) is now pinned to the independently verified integrated visual candidate, compiled source `3437722060019fdf8f892fddc4cc8121e0b61aa4` / tree `5025043fb7aacc4330eb52458bbd80da708c0590`, signed APK SHA-256 `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde`, original [signing run 42123950177](https://github.com/santirodriguez/FixMyShot/actions/runs/42123950177) and corresponding source archive SHA-256 `d9a71e1c71425a74226e38f1a72c70473ad070ce38a6f1e33bf29cad9894ccc4`, original [source run 42119231007](https://github.com/santirodriguez/FixMyShot/actions/runs/42119231007). The [new unsigned build receipt](releases/1.0.0-3437722-unsigned-build-info.json) is distinct from the historical `70f3bae` receipt. The stage producer explicitly verifies the nested signed/unsigned/issuer-risk/static-source receipts and refuses a historical same-version receipt.

**Not authorized/ready:** the release request remains `PREPARED`, with `stable_acceptance_reviewed=false` and `source_distribution_reviewed=false`. The signing request remains `PREPARED`; full corresponding-source/license legal-distribution acceptance and exact signed-device functional acceptance are OPEN. The production staging workflow must skip its stage job, and the publication guard requires separate stable/source review approval. No main merge, tag, public release, website or store operation is requested.

Offline tests passed using the frozen source snapshot and local evidence fixtures. A separate rehearsal staged and checked the original four output assets (APK, full sources, BUILD-INFO and SHA256SUMS) using **synthetic, transient approval flags solely for the local integration test**; those flags are not committed, do not authorize staging or represent real acceptance. The [signed APK physical handoff](releases/1.0.0-3437722-device-handoff.md) records the exact file/checksum, the alpha.2/code2 downgrade warning and individual device/16 KiB/locale/accessibility/privacy/Obtainium gates as NOT_RUN or PARTIAL. Preserve previous accepted limited evidence without upgrading skipped cases.

## Exact signed 1.0.0: offline source/license and static APK review — 2026-11-05

The original signed and source Actions archives were inspected again for compiled source `3437722` / exact signed APK SHA-256 `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde`. The new [bounded distribution review](releases/1.0.0-3437722-source-distribution-review.md) and [machine receipt](releases/1.0.0-3437722-static-source-review.json) verify **1,282/1,282** source archive manifest entries, **1,146/1,146** nested project/WebP entries, **95** safe source archives, **eight** exact packaged legal assets, **467** unchanged signed APK source entries, native ZIP/ELF 16 KiB alignment for all **nine** libraries and current APK binary-manifest identity/permissions. The earlier N01 source correspondence remains closed without new publisher corroboration.

**Disposition: TECHNICAL SOURCE/APK STATIC REVIEW PASS; COMPLETE DISTRIBUTION/LICENSE APPROVAL OPEN.** The current source archive has not been reviewed as a definitive legal release-source handoff and is not hosted alongside a published APK. Physical 16 KiB runtime, signed fresh install, accessibility/language/privacy/color, legacy SAF and Obtainium acceptance remain separately unverified. This documentation check does not request another signature, READY staging, merge, tag, release or deployment.

## Integrated 1.0.0 signing checkpoint — 2026-11-05

The maintainer separately approved signing the exact `3437722` integrated visual candidate after bounded Option B. [Guarded Actions 42123950177](https://github.com/santirodriguez/FixMyShot/actions/runs/42123950177) succeeded; the downloaded [signed build receipt](releases/1.0.0-3437722-signed-build-info.json) binds **12,417,941 bytes** / SHA-256 `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde` to original unsigned SHA-256 `974a0b70982b9950d19f4ed476ab2915a23dbf72614d28c1790679147b3f73b0` and the existing FixMyShot certificate `33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445`. Independent verification checked APK v2/v3 signatures and actual content digests, 467/467 unchanged original payload entries, three signing metadata additions, and 9/9 ZIP-aligned native libraries. The one-time signing request is disabled again (`PREPARED`). The previously signed `70f3bae` candidate remains historical-only.

This is signed-candidate verification, **not** full source/license distribution approval, physical/16 KiB/device/privacy/accessibility acceptance, `main` merge or public GitHub/Obtainium publication. No installation on a maintainer device was performed.

## First-public 1.0.0 pre-merge readiness — 2026-11-05

**Scope:** first-public release-copy and outstanding-gate review. [PR #4](https://github.com/santirodriguez/FixMyShot/pull/4)
(visual UI and screenshots) has already merged into `release/1.0.0`.
[PR #3](https://github.com/santirodriguez/FixMyShot/pull/3) remains OPEN/DRAFT
against `main`; the stable GitHub release and `v1.0.0` tag do not exist.
The new integrated-visual candidate's issuer-risk decision is [approved only
for its exact unsigned bytes](decisions/dependency-assurance-1.0.0-3437722.md). This review
is **not** a signing, merge or publication approval.

| Area | Grounded status | Required reconciliation |
| --- | --- | --- |
| Identity and public copy | **CONSISTENT, PREPUBLICATION:** README, CHANGELOG, [release notes](releases/1.0.0-notes.md) and [privacy source](PRIVACY.md) describe FixMyShot, Caja Personal, `1.0.0`/code1, local PNG/lossless WebP output, default metadata removal and deferred HEIC/AVIF/JXL without claiming a published APK. | Confirm combined source/notes at the eventual control commit. The notes are a **template**: `{{CONTROL_COMMIT}}` must resolve in the generated publication asset; it is not an unknown source SHA to guess now. |
| Screenshots / visual source | **APPROVED:** all six genuine, unchanged `en-US` API36 emulator PNGs in Fastlane and README were read back and matched original SHA-256 and Git blob identifiers; [ARTWORK](ARTWORK.md) retains the evidence. | Recheck against the final source only if executable UI or images change; do not imply other locales or a signed-device screenshot review. |
| Current Android build | **PASS for integrated source:** [Actions 42112670798](https://github.com/santirodriguez/FixMyShot/actions/runs/42112670798), build and API36 smoke on compiled source `3437722`; legacy SAF **SKIPPED** under `profile=current`. | Keep the API36 success separate from physical/device and legacy SAF coverage. |
| Signed APK, independent rebuild and source kit | **NEW VISUAL CANDIDATE SIGNED/VERIFIED:** source `3437722`, unsigned SHA-256 `974a0b70982b9950d19f4ed476ab2915a23dbf72614d28c1790679147b3f73b0` reproduced byte-identically; [source materials 42119231007](https://github.com/santirodriguez/FixMyShot/actions/runs/42119231007) technically checked; [guarded signing 42123950177](https://github.com/santirodriguez/FixMyShot/actions/runs/42123950177) PASS and [exact signed receipt](releases/1.0.0-3437722-signed-build-info.json) independently verified. Historical signed `70f3bae` receipt preserved. | Complete separate full source/legal and physical/manual acceptance; do not confuse structural ZIP alignment with actual 16 KiB-device runtime. |
| Dependency issuer and legal/source assurance | **EXACT-CANDIDATE OPTION B ACCEPTED:** [new 1.0.0 decision](decisions/dependency-assurance-1.0.0-3437722.md) and [machine receipt](releases/1.0.0-3437722-assurance.json) bind source `3437722`, frozen input hashes, current unsigned APK and verified rebuild/source material. **325 reviewed / 533 pending** issuer files retain their classification; N01's narrower hold was already closed. | Full corresponding-source, notices and legal-distribution approval remain independently OPEN; prior candidate's signing approval is not inherited. |
| API26–28 / SAF / 16 KiB | **LIMITED / NOT_RUN:** API26–28 SAF remains best-effort under the approved [compatibility amendment](decisions/compatibility-and-validation.md). A skipped SAF job is not a pass; physical 16 KiB runtime has not been established. | Decide and document the explicit scoped validation/limitations against [TESTING](TESTING.md); do not infer physical/runtime acceptance from ELF structural checks. |
| Manual device, resource and interaction coverage | **PARTIAL / NOT_RUN:** the older signed candidate was reportedly installed and used, not the new visual candidate. Full mixed 100-image/provider/process-death, physical privacy/color/metadata, detailed preference retention, three-language human review, TalkBack, 200% text and other M03–M07/M09 exercises have no complete new-source acceptance record. | Preserve valid older limited evidence; record exact new-candidate checks and any explicitly accepted limitations before claiming functional release readiness. Optional M08 social-post preview and future Play/F-Droid submission are not prerequisites for initial GitHub delivery. |
| Brand and localized listing claims | **LIMITED:** destination references use original descriptive pictograms, not claimed official platform marks. The accepted six screenshots show the English UI only. | Review final text and visual claims; do not represent a third-party endorsement, official logo license or translated screenshot set without evidence. |
| Public web links | **UNVERIFIED from this review:** the project has a separately maintained ChatGPT Site reported by the maintainer, but live `https://fixmyshot.cajapersonal.org/`, `/privacy/` and the donor URL could not be independently accessed by the available external browser. Inaccessibility to that tool is **not** proof the sites are down. | Check actual live DNS/HTTPS and privacy-page content through a suitable independent browser before publication; do not silently change DNS, Sites or hosting. The repository [privacy source](PRIVACY.md) remains the app policy authority. |
| Release control and Obtainium | **SIGNATURE VERIFIED; NOT_PUBLISHED:** signing request has been returned to `PREPARED`; public release request remains `PREPARED`; no GitHub Release/tag exists. | Signed bytes are verified, but manual/device/source-license and release-kit gates remain OPEN. A separately authorized `main` merge and publication plus real Obtainium fresh install are later stages; a higher-code public update requires a later version. |

For the detailed historically bound M01–M10 tests and their original positive
and negative evidence, see [TESTING](TESTING.md). A limited or skipped result
does not become PASS because a newer Android CI run is green. Do not
regenerate accepted screenshots or re-run closed diagnostics without a change
that materially invalidates their evidence.

## First-public 1.0.0 visual screenshots — approved and verified 2026-11-05

The maintainer approved the full six-role 1.0.0 screenshot set in draft
[PR #4](https://github.com/santirodriguez/FixMyShot/pull/4).
All six exact original 1080×1920, 8-bit RGB PNGs from passing Android
[Actions 42101163387](https://github.com/santirodriguez/FixMyShot/actions/runs/42101163387)
(compiled source `784bbdab`, artifact `13450950882`) were uploaded to the
canonical `fastlane/metadata/android/en-US/images/phoneScreenshots/1.png`
through `6.png` paths. GitHub readback **verified all six remote SHA-256,
Git blob identities, sizes and image geometry**, with no pixel changes.
The README now references these exact six files in Home, Editor, Destinations,
Framing, Saved and Settings/About order. See [ARTWORK](ARTWORK.md) for the
per-image original hashes, upload commit and source binding.

This is an approved visual **screenshot-set preparation**, not first-public
release acceptance. The newer PR #4 UI source has not been signed using the
prior independent candidate receipt below; detailed device/locale/privacy,
brand-rights, source/issuer/signing and combined release-copy reviews remain
independent. PR #4 is DRAFT/unmerged; no main merge, tag, release or publication.

## First-public 1.0.0 signed candidate — 2026-11-05

The exact source-frozen `org.cajapersonal.fixmyshot` **1.0.0 / code1**
candidate was signed with the existing FixMyShot key in
[Actions 41964708826](https://github.com/santirodriguez/FixMyShot/actions/runs/41964708826).
The original signed archive (artifact **13386877823**) was downloaded and
verified (SHA-256 `abee52b2fa83e874e7b2f891b5944b219b2bb021646dd295316ebeafb693aa35`).
`FixMyShot-1.0.0-universal.apk` is **12,401,557 bytes**,
SHA-256 `03ab939bf8cc2f0afe3901b1c311ed2698528d63e6b90ca1d98c3eb8f138eb39`.
The embedded certificate SHA-256
`33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445`
matches the previously established FixMyShot signer.
Original Android `apksigner` v2/v3 verification passed; independent inspection
confirmed both signature records over their signed data and unchanged content
for all **467** original ZIP members (only three signing metadata entries added).
The signed [build receipt](releases/1.0.0-signed-build-info.json) records
the source, signature and remaining limits. The one-time signing request is
now disabled again (`PREPARED`); no additional signing use is authorized.

**Maintainer feedback (2026-11-05):** the maintainer reported installing
the signed 1.0.0 APK and that the application works. This is a reported
basic installation/use PASS, without a device log or detailed checklist;
it does not establish full physical/device, export/privacy, 16 KiB,
accessibility or legacy SAF acceptance. The maintainer also approved only
review images **1, 2, 5 and 7** from the genuine 1.0.0 source artifact;
images **3, 4, 6 and 8** were rejected. See [ARTWORK](ARTWORK.md)
for exact filenames/provenance. These were **partial approvals of the earlier signed-source contact sheet**,
not the later six-image visual set. The newer PR #4 captures were subsequently
approved and replaced the canonical Fastlane files; see the current section
above. The old signed APK still does not contain that newer interface.

This verifies a *signed candidate*, **not release acceptance**. The prior
[bounded issuer-risk decision](decisions/dependency-assurance-1.0.0.md)
remains exact to this source and APK; no new publisher authentication is
claimed. Signed physical install, 16 KiB device runtime, legacy SAF,
human locale/accessibility/privacy/resource checks, truthful accepted screenshots,
complete corresponding-source/license distribution disposition, and Obtainium
installation/update remain incomplete or NOT_RUN as documented.
No merge to `main`, GitHub release, tag or deployment has occurred.

## Earlier unsigned 1.0.0 evidence — 2026-11-05

The source `70f3bae` passed
[branch build and API36 smoke 41958251409](https://github.com/santirodriguez/FixMyShot/actions/runs/41958251409);
legacy SAF was SKIPPED. The original unsigned APK was 12,356,302 bytes,
SHA-256 `11ff3b0554a616ea3eff73a6ac89fb80b9bffc32936e8cbc1bedaba90c633ff0`.
[Independent rebuild 41958986377](https://github.com/santirodriguez/FixMyShot/actions/runs/41958986377)
matched all original bytes; [source materials 41958986353](https://github.com/santirodriguez/FixMyShot/actions/runs/41958986353)
verified all 1,282 listed archive members.
See [unsigned build receipt](releases/1.0.0-unsigned-build-info.json).

## First public 1.0.0 preparation — 2026-11-04

The maintainer approved first public stable version **1.0.0 / code1**, unchanged
production package and existing signing identity, and clean installation after
uninstalling alpha.2. [The release amendment](decisions/first-public-release.md)
supersedes alpha publication/numbering instructions. PR2 has already merged into
main at c99f8df; subsequent alpha-oriented status entries are historical.

The new candidate requires its own build, exact source material, independent
rebuild, signing/inspection and truthful screenshot review. No alpha receipt
proves these new bytes. Accepted unchanged UX/output checks remain preserved;
manual tests not recorded below remain NOT_RUN. No 1.0.0 publication is claimed.

The current [draft PR #3](https://github.com/santirodriguez/FixMyShot/pull/3)
prepares 1.0.0/code1. Its exact-head Android build and API36 smoke passed in
[Actions 41910543641](https://github.com/santirodriguez/FixMyShot/actions/runs/41910543641);
legacy SAF was skipped. Release-automation checks passed, but signing inputs
remain PREPARED and no 1.0.0 signed/public artifact or current-version screenshot
has been accepted. Main still contains PR2. Publication/merge are not authorized.

<a id="current-pre-main-review--2026-11-04"></a>
## Historical PR2 pre-main review — 2026-11-04

PR1 is merged into release/0.1.0. [PR2](https://github.com/santirodriguez/FixMyShot/pull/2)
prepares main integration; no main merge or publication has occurred.
Android CI41858529484 passed all135instrumented tests and its build checks.
Original artifacts and current canonical screenshots were inspected; see the
[pre-main review](releases/pre-main-review.md). Deferred AVIF retains its known
negative diagnostic evidence and remains outside normal export.

Release preparation now includes an Obtainium configuration button, current
Caja Personal notes and an explicit manual publishing workflow. It verifies a
reviewed kit already merged into main, preserves the exact signed APK/source,
checks uploaded bytes before publishing and refuses conflicting remote state.
The release/API write path awaits explicit execution authority. Full functional
0.1.0 and actual Obtainium/device coverage remain incomplete; see
[the release handoff](RELEASING.md#current-handoff-main-github-and-obtainium).
Earlier dated status/proposal entries below are historical.

## Delivery scope reconciled — 2026-11-04

Signed alpha.2, its reported update/launch and the current English Home/Settings
screenshots remain accepted within their recorded scopes. No application code,
APK bytes, signing identity or prior test result changed in this reconciliation.
The [current disposition](TESTING.md#current-delivery-disposition--2026-11-04)
separates those completed items from the remaining manual/resource/privacy and
Obtainium evidence; historical matrices below retain their original bindings.

Full functional0.1.0 acceptance remains open. The
[proposed development-prerelease path](RELEASING.md#proposed-development-prerelease-path--decision-pending)
would prepare the already signed alpha.2 as an explicitly incomplete test release.
Preparation was subsequently approved; merge/tag/publication retain their separate authority.
No repeat of accepted visual, output, picker, update/launch or screenshot checks
is requested.

## Alpha.2 update/launch accepted by maintainer — 2026-11-03

The maintainer confirms the signed alpha.1/code1 → alpha.2/code2 update and
successful launch within the requested handoff. This is reported manual
acceptance, not agent-operated device evidence. M09 remains PARTIAL for detailed
settings/default retention and Obtainium. Do not repeat the accepted update
or general Preview2/PNG/WebP/picker comparison.

The maintainer uploaded the prepared alpha.2 Home/dark Settings images in
e742f5c2. Exact commit-pinned GitHub readback, hashes/formats and full visual
inspection now PASS; README/ARTWORK identify the actual emulator provenance.
The bounded M10 English Home/Settings screenshot task is complete.
M03–M07 outstanding manual/resource coverage and other final-delivery limits
retain the scope in [TESTING](TESTING.md). No merge or publication authority.

Earlier signed-preparation statements below retain their historical scope.

## Alpha.2 unsigned preparation complete — 2026-11-03

Compiled source `1f89c979` passed core99/API36instrumented135 and original
dark/light/return capture inspection. Exact-source independent rebuild41713619611
reproduced every unsigned APK byte; complete source materials41714329031 and
their original archive/member hashes were independently verified.
See the [alpha.2 receipt](releases/0.1.0-alpha.2-unsigned-build-info.json).

The maintainer separately approved the [exact alpha.2 Option B disposition](releases/0.1.0-alpha.2-assurance.json)
on 2026-11-03, preserving all pending publisher classifications. Subsequent exact-candidate signing passed Actions41719563373 and independent signature/content inspection. Physical alpha.2 install,
sequential upgrade, Obtainium acceptance, merge and publication remain unperformed.
Remaining manual/final-assets limits retain their [recorded scope](TESTING.md);
do not repeat accepted general Preview2/PNG/WebP/picker tests.
Earlier preparation entries below remain historical.


## Unsigned alpha.2 preparation — 2026-11-03

The maintainer approved preparing alpha.2/code2 with the accepted Preview2 UI,
current system-bar correction and unchanged output/dependency/permission scope.
Alpha.1/code1 remains recorded as the signed, maintainer-installed Actions
candidate; no GitHub release exists. Current build and exact-source receipts for
alpha.2 are pending. The light-dialog capture now waits for its selected radio
state before collection; runtime validation must confirm the actual image.

Do not inherit alpha.1's exact-artifact assurance exception or signing authority.
Reuse unchanged completed review and accepted physical checks. Outstanding
manual/upgrade/Obtainium/final-assets scope remains in [TESTING](TESTING.md).
Older sections below retain their historical source and acceptance boundaries.

## Delivery evidence reconciled; private signing scope prepared — 2026-11-02

The [current delivery matrix](TESTING.md#current-delivery-reconciliation--2026-11-02)
reuses accepted automated/manual evidence and closes stale rebuild/source/four-file
pending wording. Source/license engineering and N01 remain complete; runtime7cbed17d
and the exact unsignedAPK/source assets are unchanged. No new CI or device exercise.

Remaining functional scope includes actual16KiB runtime (legacy SAF remains best effort),
full100-item failure exercise, human locale/accessibility and broader independent
inspector cases. M08 remains layout-guidance-only under the blueprint; no social
post is requested. Final candidate screenshots and M09 signed install/sequential
upgrade/Obtainium remain open. This is not full functional-publication readiness.

Both tracked1080x1920RGB screenshots were visually inspected and matched to the
current Git blobs. They remain authentic13b50c18development captures, not newly
captured7cbed17d screenshots. README already labels them honestly; no replacement
or fabricated current-build claim was made. [Artwork evidence](ARTWORK.md#current-tracked-screenshot-inspection--2026-11-02)
records exact bytes and the visible-panel source comparison.

A [bounded private signing plan](RELEASING.md#bounded-private-candidate-signing-plan--not-authorized)
pins one exactalpha1/code1unsignedAPK. Existing external key existence/custody is
unknown; no public production certificate or release is established in GitHub.
No private-key channel is provisioned. The plan is PREPARED, NOT AUTHORIZED.
M09 needs a second increasing-code signed candidate and an authorized accessible
Obtainium source; it cannot be claimed from the lone frozen code1CI artifact.
No version/code/key/secrets/permissions/signing/merge/tag/release change is made.


## N01 closed; source/license engineering review complete — 2026-11-02

The [bounded implementation review](feasibility/runtime-license-review.md#n01-closed-by-implementation-correspondence--2026-11-02)
closes N01 using all49compiled/frozen classes (121fields/513methods), including
strict matches for all4Noto-generated classes and resolved lambda bootstraps.
[Actions41517838551](https://github.com/santirodriguez/FixMyShot/actions/runs/41517838551)
SUCCESS collected the original evidence. Its90/94strict result is retained;
offline review accounts for the4ldc-width/label/synthetic-display differences,
yielding94/94bounded correspondence checks. Twelve focused tests pass.

The exact candidate's source/license engineering review is complete with recorded
limits. Source archives, runtime7cbed17d and the unsignedAPK remain unchanged;
no Android/phone retest or source-kit regeneration. Final signed asset/source
handoff, applicable remaining delivery acceptance and separate signing/install/
upgrade/Obtainium/publication controls persist. N01 is not an open gate.
Earlier sections retain their status at the time.

## License checks closed; one source-correspondence hold remained — 2026-11-02 (before implementation comparison)

The [bounded review](feasibility/runtime-license-review.md#exact-alpha-license-and-source-disposition--2026-11-02)
finds no declared runtime/native license incompatibility, rechecks all8canonical
legal assets in the actual unsigned APK and all1282source-material manifest
members, and classifies1074AAR inputs. No new Android/phone tests were needed.
Source inventory, notices and FlatBuffers version identification are complete.
The one remaining source hold is **N01**, correspondence of the supplied Noto
generation/relocation material to Emoji2's frozen implementation. Final
corresponding-source clearance remains open; repeat captures/declaration-only
comparisons cannot close N01. B remains approved for issuer risk only.
Other delivery/signing/install/publication gates retain their distinct scope.

## Exact-alpha issuer-risk exception approved; unsigned delivery receipt prepared — 2026-11-02

The maintainer explicitly chose Option B. The [approved decision](decisions/dependency-assurance-alpha-proposal.md)
and [binding receipt](releases/0.1.0-alpha.1-assurance.json) apply only to
0.1.0-alpha.1/code 1, compiled7cbed17d and the exact unsigned APK/input set.
All325reviewed/533pending classifications remain unchanged. Further issuer
capture is not required for that exact exception; no active Gradle verification
or broader publisher trust is introduced. The [unsigned BUILD-INFO draft](releases/0.1.0-alpha.1-unsigned-build-info.json)
now records original tools, APK/source identity, CI, rebuild and source assets.
Its signed fields remain null/NOT_RUN. No new build or phone test is needed for
this documentation-only decision.

Source/license/preferred-form disposition and other applicable device/resource/
accessibility requirements, final screenshots, stable signing custody, signed
inspection, sequential upgrade/Obtainium and separate publication authority remain.
No key/signing/merge/release/deployment is authorized. Earlier entries below
preserve their status at the time; the former NOT ADOPTED choice is superseded.

## Technical source preparation complete; issuer disposition remained — 2026-11-02 (before approval)

The unchanged7cbed17d runtime has passed99core/135API36 instrumented
tests, accepted maintainer PNG/WebP manual confirmation, an independent
full-byte unsigned rebuild, and now exact source-material staging.
[Actions41373814270](https://github.com/santirodriguez/FixMyShot/actions/runs/41373814270)
and [original-byte receipt](feasibility/evidence/source-materials-7cbed17d.json)
verify the34104279-byte/1282-member archive: exact748project/343WebP
sources,94published companions,mapped Protobuf and1074AAR inputs.
The PAX kit preserves long paths and restores/regenerates identically.

This closes preparation of those technical materials. It does not
authenticate the two remaining runtime binaries or nine runtime metadata
files;522other frozen files also remain unreviewed under the current
issuer policy. [The proposed bounded-alpha assurance choice](decisions/dependency-assurance-alpha-proposal.md)
is explicitly NOT ADOPTED. Existing source/license/preferred-form and
separate resource/device/accessibility limits, stable signing/upgrade/
Obtainium and final screenshots/publication requirements remain distinct.
No signing,merge,release,deployment,policy relaxation or new phone test.
Do not repeat accepted UX, four-file or deferred-codec checks.
Earlier entries preserve status at their respective dates.

## Independent runtime rebuild PASS; exact sources being staged — 2026-11-02 UTC

[Actions41369848088](https://github.com/santirodriguez/FixMyShot/actions/runs/41369848088)
completed SUCCESS for compiled7cbed17d. The independently downloaded
original archive and actual unsigned APK were inspected; all12340030
APK bytes match the original baseline exactly. Core99passed.
The [full receipt](TESTING.md#exact-runtime-independent-unsigned-rebuild-pass--2026-11-02-utc)
keeps compiled/control identities, archive/APK hashes, fresh-cache proof
and limits. This closes that exact-runtime comparison.

The PNG/WebP four-file review remains closed by accepted maintainer
confirmation; no upload or repeat request. Ordinary CI41338068328
core99/API36 instrumented135 and the HEIC/AVIF/JXL deferrals stand.

The new explicit source-kit request reuses the existing offline packer
on GitHub-hosted checkout of exact project7cbed17d and libwebp4fa21912.
Only source preparation is requested: no APK build, dependency change,
signing or public release. Two generations plus restored regeneration
must agree and every source member/hash/Git identity must validate.
Actual job/artifact acceptance remains pending until inspected.

Remaining work is exact candidate source-material completeness/disposition
and applicable dependency/license review, remaining separate device/
resource/accessibility requirements and controlled signing/upgrade/
Obtainium/final screenshots/publication. A source kit alone is not full
corresponding-source or publisher approval. Historical sections below
retain their scoped results and must not restart accepted tests.

## Manual PNG/WebP confirmation accepted; independent rebuild requested — 2026-11-01

The maintainer accepted the requested four-file output comparison by
personal confirmation and asked to continue. It is closed as reported
manual acceptance, without an upload or another equivalent request.
No independent real-image pixel/PSNR/metadata measurement or new
agent-run phone test is claimed. See the [evidence clarification](decisions/compatibility-and-validation.md#accepted-manual-pngwebp-confirmation--2026-11-01).

Current runtime7cbed17d remains unchanged, with CI41338068328
SUCCESS/core99/API36 instrumented135. HEIC/AVIF/JXL stay deferred;
new output choices PNG/lossless WebP, PNG default. The existing
independent-rebuild request now pins that exact runtime/tree,
verified original bootstrap ZIP and unsigned APK12340030B/SHA256
1e38786776a466b3809dca84d7cfba45dc6f3196781f3aca7979507260f1ee42.
Its clean-build comparison is pending, not a new emulator/phone matrix
or acceptance of a future changed distribution source.

[Candidate release notes](releases/0.1.0-alpha.1.md) are prepared without
dead download/tag links or claims of signing/publication. The candidate
version/code remain0.1.0-alpha.1/1, with no production distribution.
Remaining source/license/dependency disposition and final-frozen source
materials, applicable resource/device/accessibility requirements and
separately controlled signing/upgrade/Obtainium/publication steps remain
distinct. Reuse accepted manual/UX evidence; do not restart it.
Earlier sections below retain the status applicable at their dates.

## Current automated validation PASS; final delivery gates remain — 2026-11-01

[Actions41338068328](https://github.com/santirodriguez/FixMyShot/actions/runs/41338068328)
at runtime7cbed17de44f4bc4ff31b62270ace3bd5a51b492 completed SUCCESS:
build/lint/core99,API36 instrumented135tests including editor12 PASS.
Optional legacySAF skipped/NOT RUN. The [exact artifact/APK/source
receipt](TESTING.md#current-pngwebp-automated-acceptance--2026-11-01)
records independent original-byte checks and limits. No new runtime
change or CI rerun is needed for this documentation reconciliation.

HEIC/AVIF/JXL0.1.0 deferrals are approved and implemented. Normal new
output choices are PNG/lossless WebP, PNG default. Deferred drafts stay
readable and unavailable without substitution. All80synthetic lossless
encodes PASS; independently decoded1344x2992 PNG/WebP agree exactly.
Prior failed measurements remain failed and physical acceptance is separate.

Three remaining delivery blocks:
1. Complete only missing final PNG/WebP physical full-output fidelity,
   applicable resource/receiver/device and accessibility evidence,
   reusing accepted UX/import/share/next-image results.
2. Freeze final distribution source, finish the applicable source/
   dependency/license review and regenerate final source materials plus
   exact-source independent unsigned rebuild. Do not recreate old kits
   or repeat known-unavailable publisher captures automatically.
3. Separately authorize stable signing custody, verify signed APK/
   sequential upgrade/Obtainium, and finalize accepted screenshots/docs
   before any separately authorized merge/tag/publication.

The actual unsigned release APK is12340030bytes,version0.1.0-alpha.1/code1,
production ID,not debuggable,no Internet/dangerous runtime permission,
nine native libraries/three ABIs with structural16KiB alignment and
eight exact canonical legal assets. This is not a signed/installable
public delivery or complete physical/source/legal/reproducibility approval.
PR remains OPEN/DRAFT/unmerged; no signing,merge,release or deployment.
Entries below preserve earlier attempts and status at their dates.

## HEIC-deferral CI: mode-switch observation failure — 2026-11-01

[Actions41324448154](https://github.com/santirodriguez/FixMyShot/actions/runs/41324448154)
at ed81e7fb completed FAILURE. Build/lint/core tests passed; API36
instrumentation passed134/135 tests, with editor11/12. Only
quickTemplatesAndModeSwitchPreserveTheSameDraft timed out waiting for
Customize after switching back to Automatic. Optional legacySAF was
SKIPPED/NOT RUN. This is not an export/color/HEIC fidelity failure.

Original smoke artifact13091345767,2371886bytes, ZIP SHA256
1c891781a0fbba2b8aeb335dff6876c610d32c85b9115f9185866709f77c6ac5
was downloaded and checked for digest, CRC and unique safe paths.
Its failure screenshot and accessibility tree already show Automatic
and Customize; they do not demonstrate a persistent mode-switch bug.
The precise transient accessibility/viewport timing cause is not proven.

The focused instrumentation change reveals Customize within the existing
scrolling tool viewport after mode switch and recreation, then checks
that the advanced-mode Automatic action is absent. Draft equality,
preview object identity and recreation assertions remain. No increased
timeout, removed assertion, runtime change or codec retest is introduced.
Current-profile CI must validate the candidate; this adjustment is not
yet a runtime PASS. HEIC/AVIF/JXL stay deferred, PNG/WebP unchanged.

## HEIC deferral approved; delivery focuses on PNG/WebP — 2026-11-01

The user approved **deferring HEIC from0.1.0**, alongside AVIF and JXL.
The [HEIC scope amendment](decisions/compatibility-and-validation.md#approved-heic-scope-amendment--2026-11-01)
qualifies the immutable blueprint's HEIC requirement and fallback
obligation for this delivery. Normal new-format choices are PNG and
lossless WebP; PNG remains the default. Restored HEIC drafts stay
readable and explicitly unavailable, without silent conversion.
Existing diagnostics, codec identities and original evidence remain.

The [Pixel compressed-byte diagnosis](feasibility/native-device-check.md#pixel-heic-compressed-byte-diagnosis--verified-2026-11-01)
is complete: the original HEIC independently decodes at26.087/26.067dB,
below38dB. Encoded chroma explains dominant edge loss; the precise
component remains unresolved. Those failures are not waived or fixed.
No more HEIC/AVIF/JXL investigation or repeated accepted phone checks
are required to finish this scoped delivery.

The bounded runtime change only removes HEIC from the Customize
format menu; Automatic already offers PNG/WebP. The normal export
preflight still rejects deferred formats. No encoder, dependency,
permission, schema, metadata policy or input capability changes.

Latest completed runtime CI before this change:
[41311422265](https://github.com/santirodriguez/FixMyShot/actions/runs/41311422265)
SUCCESS at7eaae5e6. The current-profile validation requested with this
batch must be checked separately; it is not yet a PASS.

Remaining work focuses on applicable PNG/WebP real-device full-output
fidelity/metadata/geometry, resource/performance and receiver evidence,
accessibility/localization and recovery acceptance, plus source/license/
dependency authenticity and distribution readiness. Reconcile existing
accepted evidence before requesting new checks. Signing, merge,
publication and deployment remain separately controlled. Deferral is
not a functional-release readiness claim. Older entries below preserve
the scope and evidence applicable at their dates.

## AVIF deferral approved; HEIC remains open — 2026-10-31

The user approved **deferring AVIF from0.1.0** after the completed
[Actions41212246198](https://github.com/santirodriguez/FixMyShot/actions/runs/41212246198)
chroma comparison. Its FAILURE remains the verified diagnostic result:
all three Android routes on both444 and420 have max-channel error63;
host420 has64; direct/host444 has1. The existing limit2 is unchanged.
The [scope amendment](decisions/compatibility-and-validation.md#approved-avif-scope-amendment--2026-10-31)
qualifies only the AVIF release requirement, preserving all measured
evidence and the immutable blueprint. AVIF is removed from new selections;
old drafts remain readable/unavailable without silent conversion.

HEIC is not deferred or qualified by this change. The completed Pixel
native report measured HEIC PSNR28.234/26.078/24.739dB at64x64/65x67/
1344x2992, patch median error1, with no independently reopened original
HEIC bytes. Do not repeat that unchanged six-case check. The next useful
HEIC evidence is a separately bounded synthetic encoded-byte comparison,
not an inference from AVIF or an unapproved production fallback.

PNG/lossless WebP and accepted next-image UX remain intact. Other
physical/resource/source/distribution gates still apply; this is not
functional-release or signing/merge/publication approval. No new physical
test is claimed. The older feasibility entries below remain historical.

## Isolated AVIF JNI APK cost and API 36 — verified 2026-10-30

Pinned source-built libavif/libaom JNI completed
[Actions 41134727668](https://github.com/santirodriguez/FixMyShot/actions/runs/41134727668)
**SUCCESS** at exact source `a511232d8dce0896cb98d352f45b16903df16df0`:
three ABI shared-library jobs, same-source/same-debug-package
APKs, and x86_64 API 36 instrumented synthetic encoding.
The real diagnostic APK delta is **8,629,021 B (8.229 MiB)**:
16,940,589 B without JNI versus 25,569,610 B with JNI.
All 473 preexisting ZIP members retain identical uncompressed bytes;
three libraries and three original license/patent texts are the only additions.
Packaged ELF/ZIP structural 16 KiB checks PASS.

API 36 loaded the JNI and performed 20 small 65x67 / five
1344x2992 sequential encodes. Encoder-only medians were **64.423 ms**
and **8,962.779 ms**; nearest-rank p95 **130.076 ms** and
**12,268.763 ms**, respectively. Maximum post-encode PSS
was **94,257 / 98,948 KiB**, while whole-process cumulative
`ru_maxrss` reached **201,952 / 285,800 KiB**.
PSS was sampled outside the actual encoding window: it is
**not peak PSS**, and no Pixel memory or p95 is established.
Independent host decoding of Android outputs reproduced exact
65x67 / 1344x2992 geometry, full-range YUV444, RGB PSNR
**58.6887 / 58.7403 dB**, and maximum per-channel error **1**.

The [full evidence and scope limits](feasibility/native-device-check.md#isolated-jni-avif-diagnostic-apk-and-api-36-feasibility--2026-10-30)
separate this synthetic debug variant from ordinary exports.
The diagnostic emulator uses 4 KiB pages; physical 16 KiB
runtime, real-image/device fidelity, peak PSS, second hardware,
publisher/patent/compliance review and G1/M02 remain **OPEN**.
No AVIF or HEIC regular export, permission, signing, merge,
release or production APK changed.


## Isolated Android software AVIF reference — verified 2026-10-30

The source-pinned standalone libavif 1.4.2 / libaom 3.15.2 experiment
was compiled for `arm64-v8a`, `armeabi-v7a` and `x86_64`. After
correcting the runner's missing FFprobe dependency, exact-head
[Actions 41132550109](https://github.com/santirodriguez/FixMyShot/actions/runs/41132550109)
at `c15f882e5fd4606917485f02be686273c1d13e1d` completed
**SUCCESS** in all four jobs (three ABI builds and API 36 x86_64 fixture).
The actual emulator-produced 466-byte 65×67 AVIF was independently
decoded on a separate FFmpeg 7.1.5 host: YUV 4:4:4/full range,
RGB PSNR **58.6887 dB**, maximum patch error **1**.
Single emulator encode-write time was **18.088442 ms** and entire-process
`ru_maxrss` **12,736 KiB**; neither is a Pixel p95 or PSS measure.
The emulator reported a 4 KiB page size; 16 KiB ELF properties are
structural only. The [complete evidence and limits](feasibility/native-device-check.md#three-abi-android-software-avif-reference--successful-synthetic-ci--2026-10-30)
remain separately recorded. **No JNI, APK integration, normal AVIF
export, hardware acceptance, source-license clearance, signing or release**
follows from this experiment. G1/M02 remain OPEN.


## AVIF geometry alternative spike — 2026-10-30

The **host-only metadata/crop remux** of the verified 65×67 Pixel AVIF
is not interoperable: libheif presented 65×67, Pillow returned 128×67
and FFmpeg rejected this prototype. A separate Pillow/libavif 1.4.2
**software 4:4:4** control yielded **65×67** through all three host
readers with **55.74–56.50 dB** PSNR and patch error 1; the 4:2:0
control missed 38 dB. One 1344×2992 host compression sample completed
in 1.095 s in 4:4:4 but whole-process RSS (~268 MiB) includes
Python/NumPy and is NOT Pixel memory/performance evidence.
The [evidence owner](feasibility/native-device-check.md#host-only-avif-correction-feasibility--2026-10-30)
retains exact modes and limits. No Android code changes, new APK,
device test or production codec enablement. G1/M02 remain OPEN.

## Earlier AVIF alignment checkpoint — 2026-10-30

PR #1 at `508b8b95cf0712b4ca04929d145f09957353bfa7` remains **OPEN /
DRAFT / unmerged**. [Exact-source CI 40989504238](https://github.com/santirodriguez/FixMyShot/actions/runs/40989504238)
completed SUCCESS on its second attempt (build/lint/core99 and API36
instrumented groups; optional API26/28 SAF SKIPPED / NOT RUN).
The delivered isolated `FixMyShot AVIF Check` APK, SHA-256
`665a3c3165470e26d7782c4af43e2bda87ca86c7112ee4c414b115c19a71f8d4`,
was used for the distinct [five-case Pixel API37 alignment diagnostic](feasibility/native-device-check.md#pixel-9-pro-xl-focused-avif-alignment-result--2026-10-30).
All **5/5** cases encoded/reopened but failed numeric fidelity; the
worst 3×3 patch median-channel error was **129** in every case.
Grid on/off yielded matching bitstreams at 65×67 and 66×68; padding
to 66×68 and 96×68 did not repair the problem. This is **new device
diagnostic evidence, not HEIC/AVIF export, independent decoder or
release acceptance**. No more unchanged grid tests are indicated.
Pixel-produced encoded bytes/independent host decode and the existing
G1/M02, other-hardware, color, resource and release gates remain open.

## Current AVIF byte-level finding — 2026-10-30

Latest editor diagnostic HEAD `82a40bf22a479c035ee608c3196f2ec616513be2`,
PR #1 **OPEN/DRAFT/unmerged**, passed
[exact-source CI 41107077558](https://github.com/santirodriguez/FixMyShot/actions/runs/41107077558)
(build/lint/core99 and API36 131 instrumented tests; optional legacy SAF
NOT RUN). The user completed the separate **single 65×67 Base64 AVIF
handoff** on the Pixel 9 Pro XL/API37: 1,358 source-bound bytes, SHA-256
`3bd7556515d9b5532faef95059e2e20afa4bbc263969f1ffbe5321ae2f51f19c`.
Host decoders exposed a **128×67 AV1 raster** despite a **65×67 AVIF
`ispe`**: Android/Pillow 65×67 display was severely horizontally
distorted (reported PSNR ~7.25 dB, patch error129), while the
leftmost 65 columns from FFmpeg/libheif reconstructed the
expected pattern (PSNR ~26.1–26.4 dB, patch error2–3).
A one-field `ispe` experiment corroborates geometry handling as
the dominant cause but **does not produce valid 65×67 export**.
See [full exact-byte evidence](feasibility/native-device-check.md#pixel-6567-avif-independent-byte-level-decode--2026-10-30).
The earlier five-case and six-case phone reports are complete.
HEIC/AVIF remain gated; G1/M02, color/profile, second hardware,
physical resources/16KiB, provenance and distribution are OPEN.

## Earlier accepted editor baseline — 2026-10-29

The first functional Android implementation is on `release/0.1.0-editor` at
[`d54eb9c7`](https://github.com/santirodriguez/FixMyShot/commit/d54eb9c7ff9e95a5b55cb3f2ab5b2501797bfcb7),
in [PR #1](https://github.com/santirodriguez/FixMyShot/pull/1) (**OPEN / DRAFT / not merged**).
The compiled source remains unreleased `0.1.0-alpha.1`/versionCode 1.

- **Implemented and automatically checked:** picker and Android share import, Automatic with an
  Original/no-platform first-use default, explicit destination/format and flexible framing,
  complete Customize/batch controls, verified PNG/lossless WebP transactional export,
  guarded review/continue editing/next-image picker and private recovery. PNG and metadata
  removal remain new-session defaults. The exact-head [API 36 run](https://github.com/santirodriguez/FixMyShot/actions/runs/40966098425)
  passed build/lint, 99 core tests, 12 editor tests, 23 import/settings/session tests
  and all other retained instrumentation groups. Legacy SAF was **SKIPPED / NOT RUN**.
- **Accepted by the user:** the delivered isolated `FixMyShot Next` diagnostic and
  consecutive-image/picker-cancellation phone exercise: “Hecho. Todo funciona bien.”
  Earlier general UX 2.2 visual feedback was positive. This is not pixel-by-pixel,
  performance, accessibility or native-codec acceptance.
- **Pixel native diagnostic received (not export acceptance):** the user-shared API37
  [synthetic HEIC/AVIF report](feasibility/native-device-check.md#pixel-9-pro-xl-synthetic-result--2026-10-29)
  matches the previously delivered diagnostic APK's reported SHA-256. All six cases
  report encode/reopen, but **0/6** meet numeric fidelity targets; 65×67 AVIF has
  PSNR 7.247 dB and patch median-channel error 129. No codec is enabled by this result.
- **Still open:** physical Pixel output/color/metadata/matte and resource checks, a second
  hardware family, native HEIC/AVIF output fidelity and any qualified fallback,
  real-provider/process-death/100-item manual cases, manual locale/TalkBack/200% layouts,
  16 KiB runtime, conditional legacy SAF, exact-final-source reproducibility/corresponding
  source and publisher clearance, authentic final screenshots, production signing,
  sequential upgrade/Obtainium, merge and publication. JXL is deferred for 0.1.0.
- **Authority:** no production key creation/rotation, merge, tag, release, publication,
  deployment or permission change is approved. The earlier same-source independent
  unsigned rebuild and source-material bundle are verified **for e516aa89**, not an
  independent rebuilt release at the current editor HEAD.

## Focused AVIF geometry check — completed diagnostic, not format approval

The separately signed `.avifodd` candidate has passed exact-head CI and
one user-reported Pixel 9 Pro XL five-mode synthetic check. Results and
identity receipts are owned by [native device evidence](feasibility/native-device-check.md).
Ordinary export remains PNG/lossless WebP; HEIC/AVIF are not enabled.
The accepted editor/picker exercise remains completed and must not be repeated.

## Earlier source-bound evidence (preserved)

The sections below preserve earlier technical baselines and exact receipts. They are
not claims that `5d568a7e`/`e516aa89` are the latest application implementation.
This index links current evidence. The [full blueprint](blueprints/first-functional-release.md)
and [compatibility amendment](decisions/compatibility-and-validation.md) still govern.

Import, editing, PNG and lossless WebP export are implemented. Source
`5d568a7e` passed [normal validation](https://github.com/santirodriguez/FixMyShot/actions/runs/40196478234):
69 core tests, lint, three APKs, exact source/tree/wrapper/lock audit, actual NDK
source pins, eight canonical legal assets in both debug/release packages, native
libraries across three ABIs with ELF/ZIP alignment, and API 36 regressions with
80 real encodes. Four downloaded-report guards were replayed independently.
The [package receipt](feasibility/evidence/android-5d568a7e.json) identifies the bytes.

[Independent rebuild](https://github.com/santirodriguez/FixMyShot/actions/runs/40197042011)
used the exact source and empty caches. The downloaded unsigned APK is identical
to the original full file: 11,841,246 bytes, SHA256
`cc902a395a73ca8c18d98b03a36b4f823ee33cdf3d59e88f4aaf5b5f2eeff360`.
This closes bounded build/runtime/reproducibility acceptance for that source.
Later source-review evidence preserves those application inputs. The subsequent
e516aa89 settings guard changes build configuration; its clean-build and exact
payload/reproducibility checks are described below.

The Protobuf BSD and compiled NDK cpufeatures notice omissions are corrected.
The 133-component runtime declaration catalog is packaged; its 133 POMs and
95 hash-matched archives were reviewed. Exact WebP source, original notices, NDK
helper and AndroidX native release sources were checked. The two earlier raw
ZIP uploads are verified. The [license owner](feasibility/runtime-license-review.md)
and [native owner](feasibility/native-source-review.md) retain full-review limits.

The frozen dependency inventory remains byte-identical at 487 components and
858 files. Inactive publisher review covers **325/858 files**, with 533 pending:
**11 runtime-associated files** and 522 other captured files. All six critical
files retain evidence. New source-copy and publisher-checksum proofs preserve
all earlier entries and remain distinct from PGP signatures. Three additional
Okio files have verified detached signatures. Another 26 Google module files and
two Guava files have exact publisher-maintained source-copy evidence. The 1.24 MB raw evidence ZIP is
verified at upload commit `f7c1fa7f`; all 94 members and manifest entries pass. See the
[source-proof owner](feasibility/google-source-copy-review.md).

The unchanged OSV snapshot records 49 advisory IDs across 18 tooling components
and no matches for the 133 app-runtime components at scan time. Actual debug
and release DEX definitions/type references contain none of the six affected
package families. This narrows packaged exposure. The current observed host-task and known SDK
certificate route review is complete within its recorded limits; the full
security gate and future-input/signing review remain open. No generated trust policy is activated. See the
[security owner](feasibility/dependency-security-review.md).

## The path to delivery

These are distinct remaining acceptance tracks, not a request to repeat the
already accepted next-image phone exercise. The blueprint records actual device acceptance before
final distribution acceptance. A diagnostic candidate may be exercised while
publisher/source controls remain open; retain its exact source/artifact and
all limits. This does not authorize distribution, signing, merge or publication,
or turn any pending control into PASS.

1. **Finish the technical delivery controls.** Application implementation and
   bounded automated acceptance are recorded above. Publisher authenticity
   and corresponding-source materials are
   still open. The current observed build-task security review is complete;
   new tasks/inputs and production signing reopen its documented limits.
   The533pending files are one dependency-review backlog, not533
   product features. Further issuer attempts need a materially new primary
   anchor; recorded unavailable routes must not be retried automatically.
2. **Complete the unverified real-device gates.** Pixel9ProXL and a second hardware family:
   HEIC/AVIF geometry/fidelity/color/resources/cancellation/fallback, providers,
   batches/process death/storage pressure/gallery/receivers, accessibility,
   languages and16KiB runtime. These checks are postponed, not waived.
3. **Prepare and verify distribution.** Separately authorized signing identity,
   manifest/certificate checks, sequential install/upgrade/Obtainium, authentic
   captures and final receipts/docs. No store submission or deployed website
   is required for the initial GitHub delivery.

The latest technical change blocks Jetifier activation at the settings boundary,
addressing a known AGP→Jetifier→JDOM operation. A focused clean-build comparison
of guarded source e516aa89 against accepted5d runs in
[40208750635](https://github.com/santirodriguez/FixMyShot/actions/runs/40208750635).
Actual default compilation/core69 and true-override rejection pass. Its overall
literal cross-source comparison failed only because the Git source marker
changed; the other465APK payloads match. A second same-source clean build,
[40209306060](https://github.com/santirodriguez/FixMyShot/actions/runs/40209306060),
passed: both downloaded same-source APKs match full bytes
(11,841,246B/SHA256b677f044625cfee76ad2c77a3e2f812a1c4bcfd53429f87023fea015be02a8dd),
with69core tests, the actual override guard, eight legal assets and native
alignment verified. [The receipt](feasibility/evidence/build-operation-review-e516aa89/independent-e516aa89.json)
retains the exact scope. No new device acceptance follows. The vulnerable
classpath version is not claimed patched. A separate source-level bundletool/
jose4j call review narrows its compressed-JWE finding; full security remains open.
See the [operation receipt](feasibility/evidence/build-operation-review-e516aa89/review.json).

HEIC/AVIF remain unavailable for ordinary export pending their gates. JXL is
explicitly deferred for0.1.0. Legacy diagnostics are best effort; skipped means
NOT RUN. Signing, merge and publication remain separately gated.


Source preparation now has an executable, validated stage: the exact accepted
project and full pinned WebP source produce identical archives and restore their
original Git identities without a network request. [Source-kit evidence](feasibility/evidence/source-kit-e516aa89/review.json)
records all818members. Complete other-runtime source/distribution review remains
open. The new [Commons call review](feasibility/evidence/commons-lang-operation-review/review.json)
finds no vulnerable class-name resolver through inspected Compress calls; it
does not reduce the vulnerability inventory or approve the entire toolchain.
No new application build or phone acceptance is claimed by these preparations.


The grouped host-tool review now has actual successful evidence from
[40214535917](https://github.com/santirodriguez/FixMyShot/actions/runs/40214535917):
core69, lint, three APK tasks and full unsigned-byte equality to accepted e516.
Thirteen JVM traces/126398class loads cover all49frozen findings as bounded
observations. Netty/jose4j/JDOM were absent in those traces; the observed URI
resolver and ClassUtils origins are the newer Gradle-bundled JARs. Four BC
prerequisite classes were absent. Lazy ASN.1 was loaded, but the inspected SDK
local-certificate route uses the non-lazy constructor. The [assessment](feasibility/evidence/host-tool-operation-review/assessment.json)
retains every finding, source/trace limits and explicit reopen triggers. The
initial observer failure is preserved; the corrected run does not erase it.
This closes the current scoped operation-review batch, with publisher/source,
physical and separately authorized signing/distribution gates still open.


Runtime source preparation now covers94published companions,3,673code files,
and the mapped upstream Protobuf4.28.2 source archive (159Java files), with zero
capture errors and actual hash/CRC replay. Seven empty companions match exact
frozen binaries containing no class implementations; their small original
manifest/resource/rule text is retained. [The owner](feasibility/runtime-source-review.md)
records the38exact metadata source-owner mappings (34redirects, three BOMs
and one dependency-only stdlib wrapper). Actual packaged-source closure and
final corresponding-source/build-material review remain explicit. Publisher325/858 and physical/signing gates are unchanged.
No new Android CI or heavy upload is needed for this source-staging batch.


The accepted APK's16,986class names now have explicit frozen-archive,
D8-name/resource-generation or project-namespace attribution; none is unassigned.
Actual95archives match frozen hashes. The source stage now adds DataStore's
missing JNI file,24FlatBuffers Java sources/two Noto metadata files and five
Core stable AIDL inputs. A real [source-material bundle](feasibility/evidence/source-material-bundle-e516aa89.json)
(32,779,542B/1,282members) passed all manifest checks and two byte-identical full
assemblies. This completes that assembly stage. The [source owner](feasibility/runtime-source-review.md)
retains398SourceFile-name diagnostics, the negative0/45naive relocation comparison
and the unproved exact Noto build-checkout relation. Final source/distribution
approval and publisher325/858 remain open. No new APK or phone result is claimed.


The [repackaged-symbol review](feasibility/evidence/repackaged-source-review-e516aa89/review.json)
corroborates FlatBuffers1.12.0material identification:45matching declaration
layouts,31matching unique constant sets and14sets with original-only class
reference differences; no new frozen constants/members. No method-body/source
or publisher equivalence is inferred, and the earlier byte mismatch remains.
No source change or new bundle/build is needed.

The [delivery-control partition](feasibility/evidence/delivery-control-status-e516aa89.json)
now makes the remaining runtime work concrete:93/95binary archives have reviewed
publisher evidence; the two pending binaries are coroutines-android1.9.0and
JSpecify1.0.0. Six module files/three POMs account for the other nine pending
runtime files;522other captured files remain pending. New issuer searches found
no accepted primary anchor, so325/858and the inactive policy are unchanged.
Functionality/device acceptance and distribution approval must be reported
separately. No phone run or APK handoff is claimed here.

