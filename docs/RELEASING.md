# A good release should be boring to install

## Latest verified icon1 signed test candidate — 2026-11-06

[Actions 42226227569](https://github.com/santirodriguez/FixMyShot/actions/runs/42226227569) completed the **separately authorized exact icon1** signing with the existing app identity. The original signed `FixMyShot-1.0.0-universal.apk` is **12,675,989 bytes**, SHA-256 `92efebea8ebfce6a88865a8b4842f61e4362d8951fb7949ea626c0a0245ea428`; the [machine receipt](releases/1.0.0-7649f0e-signed-build-info.json) records the source, original input artifacts, byte-identical rebuild, certificate and independent v2/v3 signatures, preserved APK content and static 16 KiB alignment. This supersedes earlier sections' **signing pending** statements for icon1 only; their old-source evidence remains historical.

The request control returned to `PREPARED` with its prior historical stage identity. **Do not** use that old `.ci/development-release-request.json` as a new-icon release kit: it is still `PREPARED`, points to the old-icon signed APK and has `branding_revision_pending=true`. Before any public staging, reconcile the exact new signed APK, matching source assets, release tests and full corresponding-source/license/legal and physical/manual gates under a separately agreed preparation step. Screenshot #1 new launcher identity, real 16 KiB device runtime, locales/accessibility/privacy/SAF, merge, tag, GitHub Release, Obtainium and website/store release remain OPEN. No public release is authorized by candidate signing.

## New-icon 1.0.0 release gate — 2026-11-05

The selected next-launcher artwork is the original `icon1.png`, now `assets/branding/primary/app-icon.png` and its exact canonical Fastlane `en-US/images/icon.png` copy. The original 1024×500 `promo.png` is the future Google Play/F-Droid `en-US/images/featureGraphic.png`; older designs remain alternatives. See [ARTWORK](ARTWORK.md).

**The old signed APK was built before this launcher update** (compiled source `3437722`, signed SHA-256 `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde`). Although the maintainer reports a successful basic APK trial, it is not a tested new-icon executable. The stable release request remains `PREPARED`, with `branding_revision_pending=true`. Both the staging and publication verifier explicitly refuse that stale signed candidate even if other flags are changed. Rebuild and independently compare the updated source, regenerate exact source materials, obtain a new candidate-specific issuer-risk disposition and **separate signing authorization** before proposing stable 1.0.0 staging. Physical/visual and complete source/license acceptance remain independently pending. No merge, tag, new signing or publication is authorized by this artwork change.

## Exact first-public 1.0.0 release-kit preparation — gated

The [release request](../.ci/development-release-request.json) now contains exact, original artifact pins for the **integrated** `3437722` executable and the separately signed APK `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde`. The four intended assets are `FixMyShot-1.0.0-universal.apk`, `FixMyShot-1.0.0-source-materials.tar.gz`, `BUILD-INFO.json` and `SHA256SUMS.txt`. The [new unsigned candidate receipt](releases/1.0.0-3437722-unsigned-build-info.json), [signed receipt](releases/1.0.0-3437722-signed-build-info.json), [accepted bounded issuer-risk receipt](releases/1.0.0-3437722-assurance.json) and [static source review](releases/1.0.0-3437722-static-source-review.json) are selected explicitly; original `1.0.0-unsigned-build-info.json` and `1.0.0-signed-build-info.json` remain **historical**.

The staging code checks candidate identities, SHA-256, source/manifest pins, issuer-risk scope, actual signature verification, original signing/source run and artifact IDs, current corresponding-source review and release acceptance gates. Both `stable_acceptance_reviewed` and `source_distribution_reviewed` remain **false** and the request remains **PREPARED**. Merely editing a version or replacing an old receipt cannot promote this candidate to READY. Stable staging requires separate recorded functional and distribution approval, and the publication verifier also checks those approval flags.

An isolated local QA rehearsal with **temporarily simulated approval flags** reproduced the intended four-asset layout using the unchanged original 12,417,941-byte APK and 35,851,337-byte sources. That was a technical test, **not** actual release approval, release-kit Actions staging, signed-device acceptance or legal clearance. The [physical-device handoff](releases/1.0.0-3437722-device-handoff.md) identifies the exact APK and still-open exercises without asking for another signature or repeating accepted screenshot captures.

Only after independently recorded legal/distribution disposition and physical/manual acceptance may a later explicitly scoped preparation step review a switch to READY, inspect its actual staging run and decide whether to ask for **separate** `main` merge, `v1.0.0` tag and GitHub release authority. Actual Obtainium fresh install follows accessible publication, and a real code-increasing update requires a future version.

## Integrated first-public 1.0.0: signed test candidate — 2026-11-05

The user separately approved signing the fully verified integrated visual source `3437722`/unsigned SHA-256 `974a0b70982b9950d19f4ed476ab2915a23dbf72614d28c1790679147b3f73b0` after the [new bounded Option B decision](decisions/dependency-assurance-1.0.0-3437722.md). [Actions 42123950177](https://github.com/santirodriguez/FixMyShot/actions/runs/42123950177) signed the original artifact with the existing FixMyShot identity. The [new signed receipt](releases/1.0.0-3437722-signed-build-info.json) records independently verified APK SHA-256 `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde`, 12,417,941 bytes and the original public certificate. No rebuild or change of executable source was involved. The previous signed `70f3bae` candidate is historical-only; its proofs were not reused as the new signature.

The one-time request is now `PREPARED`; this exact operation authorizes no further signing. Full corresponding-source/license distribution review, physical/device/16 KiB runtime/accessibility/privacy/locale/manual acceptance, future `main` merge, tag, stable GitHub release and real Obtainium checks remain separate. **Do not release or install on someone else's device under this authorization.**

## First-public 1.0.0 public-copy and signing boundary — 2026-11-05

The [current pre-merge gate matrix](STATUS.md#first-public-100-pre-merge-readiness--2026-11-05)
covers visual PR #4 already merged into `release/1.0.0` and draft release
PR #3, which is still unmerged into `main`. The six authentic 1.0.0 emulator
screenshots are approved, committed to canonical Fastlane paths and reused by
README. CHANGELOG and README still correctly describe the release as **not yet
published**; the file
`releases/1.0.0-notes.md` remains a release-note **template** with
`{{CONTROL_COMMIT}}` deliberately unresolved until the actual reviewed
release-control commit exists. Never publish that literal placeholder, invent
a commit, or label the previous signed source as the current visual source.

The **new integrated-visual executable** is source `3437722060019fdf8f892fddc4cc8121e0b61aa4` /
tree `5025043fb7aacc4330eb52458bbd80da708c0590`, unsigned APK SHA-256
`974a0b70982b9950d19f4ed476ab2915a23dbf72614d28c1790679147b3f73b0` (12,373,110 bytes).
[Exact Android build/API36 smoke](https://github.com/santirodriguez/FixMyShot/actions/runs/42112670798),
[independent byte-identical rebuild](https://github.com/santirodriguez/FixMyShot/actions/runs/42119230855)
and [technical source materials](https://github.com/santirodriguez/FixMyShot/actions/runs/42119231007)
are verified. The maintainer explicitly accepted **bounded Option B** only
for that candidate: see [new decision](decisions/dependency-assurance-1.0.0-3437722.md)
and [machine receipt](releases/1.0.0-3437722-assurance.json).
The earlier signed `70f3bae` candidate remains historical and retains its
original signing/disposition receipts unchanged. Signing was authorized separately after that issuer-risk decision and verified
in [Actions 42123950177](https://github.com/santirodriguez/FixMyShot/actions/runs/42123950177).
Complete license/source distribution clearance, manual functional acceptance,
main merge, tag and publication remain unapproved.
Signing/distribution requests remain `PREPARED`; do not promote them by
documentation review.

The custom FixMyShot Site and the advertised `/privacy/` route are not
independently verified as reachable/consistent with [PRIVACY](PRIVACY.md)
using the available external browser; the donation link is likewise
unverified here. Verify the actual URL/content through a suitable live
browser before publication. An inaccessible verification tool does not prove
an outage. Do not change Sites, DNS, Hostinger, Cloudflare, credentials or
permissions as part of this gate.

This copy audit does not waive any physical, locale/accessibility,
metadata/color, legacy SAF, 16 KiB runtime, corresponding-source/license,
branding or first-install acceptance gates. Obtainium's actual fresh
installation can be verified only after an accessible release; a genuine
higher-version-code update must await a later version. Merge, tags, signing
and publication remain separate approvals.

<a id="current-handoff-main-github-and-obtainium"></a>

## Current handoff: first stable 1.0.0

[Approved release identity and counter reset](decisions/first-public-release.md)
govern the current candidate: **1.0.0 / versionCode1**, stable GitHub channel,
`org.cajapersonal.fixmyshot`, existing FixMyShot certificate, Caja Personal About.
The public release ledger starts at 1.0.0/code1; the next public update uses
code2 and later updates strictly increase. Private alpha candidates live only
in a separate historical ledger section and cannot determine the public counter.
The changelog's 1.0.0 heading identifies the target, not a claim of publication.

The **historical signed test candidate** has **not been published**:
1.0.0 / code1, source
`70f3baeebb096461eb5072ca125f0c05334703da`, signed APK SHA-256
`03ab939bf8cc2f0afe3901b1c311ed2698528d63e6b90ca1d98c3eb8f138eb39`,
previous FixMyShot certificate unchanged. The [one-time signing request](../.ci/signing-candidate-request.json)
was reset to `PREPARED` after completion. The distribution request remains
`PREPARED` until a complete reviewed release kit and separate authority exist.

1. **DONE:** build and test new immutable source `3437722`, inspect actual unsigned
   APK/manifest/checksums and original reports.
2. **DONE:** bind independent rebuild and source materials to that exact source,
   verify both original artifacts and the matching bytes.
3. **ISSUER-RISK OPTION B APPROVED FOR NEW SOURCE ONLY:** use the
   [new candidate decision](decisions/dependency-assurance-1.0.0-3437722.md)
   and [receipt](releases/1.0.0-3437722-assurance.json).
   **NEW SIGNING SEPARATELY AUTHORIZED AND VERIFIED IN ACTIONS 42123950177; REQUEST RESET TO PREPARED.** The previous [signed receipt](releases/1.0.0-signed-build-info.json)
   is historical-only and must not be substituted for `3437722`.
   Source/legal/manual gates remain separately open.
4. Review current genuine screenshots, notes, four-asset kit and remaining
   acceptance evidence before recommending the separately authorized main merge.
5. After separate publication authority, publish v1.0.0 as stable/latest using
   the reviewed kit and verify all hosted bytes and the Obtainium button.

Do not publish the old alpha kit, rename its APK or reuse its source archive for
1.0.0. Alpha.2/code2 requires uninstall before code1; app-private settings/drafts
are lost. Real Obtainium update testing waits for a later increasing-code release;
a reinstall is not an update. Other physical/manual limitations retain their
recorded scope. The new release request stays PREPARED until exact assets exist.

## Historical alpha preparation and evidence

The sections below preserve the former alpha development evidence and its
original timestamps, hashes and exact-APK bindings. Wording such as "current",
"first release" or "publish" in this historical section refers only to its
old checkpoint. **None of those steps is executable release guidance now.**
Do not use the historical alpha request, tag, notes or staged artifact to
publish FixMyShot. The active first-public contract is 1.0.0/code1 above.


<p align="center"><strong>One APK. Clear notes. The same app when you update.</strong><br /><sub>FixMyShot · GitHub first · Obtainium-friendly</sub></p>

> [!IMPORTANT]
> **Signed alpha.2 validation candidate.** The current candidate is `0.1.0-alpha.2` / versionCode `2` integrated through [PR #1](https://github.com/santirodriguez/FixMyShot/pull/1).
> Alpha.1/code1 was signed in [Actions41543938314](https://github.com/santirodriguez/FixMyShot/actions/runs/41543938314), independently inspected and installed/opened by the maintainer. Its ledger row preserves the exact source, APK and public certificate.
> Alpha.2 retains the production application ID and accepted Preview2 UI. Its unsigned preparation passed: core99/API36instrumented135, actual three-state contrast captures, source materials and exact-source independent full-byte rebuild. The [alpha.2 receipt](releases/0.1.0-alpha.2-unsigned-build-info.json) binds the verified bytes. The maintainer approved the [exact alpha.2 assurance disposition](releases/0.1.0-alpha.2-assurance.json) on 2026-11-03; signing subsequently passed [Actions41719563373](https://github.com/santirodriguez/FixMyShot/actions/runs/41719563373) and independent inspection; see the [signed receipt](releases/0.1.0-alpha.2-signed-build-info.json). The maintainer reports a successful alpha.1 → alpha.2 update and launch; detailed settings/default retention and Obtainium remain unverified. Historical alpha.1 receipts below remain bound to their original bytes.
> No merge, release, store upload or publication is authorized by candidate signing.

<a id="proposed-development-prerelease-path--decision-pending"></a>

## Historical handoff: main, GitHub and Obtainium

The first public candidate is the explicitly incomplete **0.1.0-alpha.2 / code2**
development prerelease, using the verified signed APK and exact source archive.
Public presentation uses Caja Personal. The frozen app still has its original
About links; a different APK requires a new versionCode and acceptance.

1. Review and separately authorize [PR #2](https://github.com/santirodriguez/FixMyShot/pull/2)
   into `main` using a merge commit, preserving the reviewed control ancestry.
   A squash/rebase requires a new reconciled kit before publication.
   Record the successful push run ID and control SHA from
   **Prepare development release kit** on `release/0.1.0`.
2. After separately authorizing publication, run **Publish reviewed development
   prerelease** from `main`, with that `prepared_run_id` and the exact confirmation
   `PUBLISH v0.1.0-alpha.2`. A merge does not dispatch publication.
3. The workflow checks the staging run's repository, branch, workflow, event and
   successful result; downloads its exact artifact; verifies the four assets and
   checksums; and requires the packaging-control commit to be contained in `main`.
   The tag identifies that reviewed control commit. BUILD-INFO separately pins
   compiled source `1f89c979`; no rebuild of the documentary tag is claimed.
4. It creates a draft, uploads the APK/source/BUILD-INFO/checksums and downloads
   every uploaded asset to check its full size and SHA256. Only then is the
   prerelease published, without marking it latest. Conflicting tags/assets/
   releases are refused, never moved or replaced. An interrupted run retains
   its draft; diagnose and retry with the same kit.
5. Verify the live links and actual Obtainium source/install/update behavior.
   The README button configures the source, prereleases and universal-APK filter.
   Its configuration is tested automatically; device acceptance remains separate.

Staging is read-only and does not build or sign. The repository currently caps
artifact retention at7days (confirmed by the original upload log); the workflows
use that limit. Always inspect the artifact's actual `expires_at` before handoff.
The prepared kit can outlive its original signing/source Actions inputs.
If the kit itself expires, recover the exact inputs; never substitute different
bytes with the same code. Only the manually dispatched publishing job uses the
short-lived repository GITHUB_TOKEN with `contents: write`; no new secret,
environment or signing-key operation is required. It becomes executable after
integration into `main`.

The [Obtainium deep-link specification](https://wiki.obtainium.imranr.dev/deep_links/)
defines the README configuration link; no community listing is needed.
Offline refusal/recovery tests run in PR CI. Actual GitHub API writes remain
unexecuted until explicit publication approval. Full functional0.1.0 and manual
coverage limits remain in [TESTING](TESTING.md#current-delivery-disposition--2026-11-04).

## Approved development prerelease preparation — 2026-11-04

Status: **ORIGINAL PREPARATION VERIFIED; MAIN INTEGRATION AND PUBLICATION REQUIRE SEPARATE APPROVAL**.

The maintainer's Go approved preparing this bounded test-release kit. The
request-triggered [staging workflow](../.github/workflows/prepare-development-release.yml)
only copies and checks the existing signed APK/source archive; it cannot build,
sign, merge, create a tag or publish a release. Its output separates the four
release assets from review-only notes, source/tag/integration plan and receipt.

The current alpha.2 already has verified build, source materials, byte-identical
independent rebuild, exact-artifact assurance approval, a stable production
signature and maintainer-reported update/launch. Its current English Home and
Settings images were read back and visually accepted. The unresolved scope is
listed in [the current delivery disposition](TESTING.md#current-delivery-disposition--2026-11-04),
not a demand to repeat completed checks.

Approved preparation scope: prepare the existing `0.1.0-alpha.2` as a **development
prerelease for testing**, explicitly stating that full functional0.1.0 acceptance
is incomplete. Keep the immutable blueprint and its remaining M03–M07/M09/M10
requirements. Do not relabel NOT_RUN as PASS or advertise a full functional
release. The preparation is approved; publication remains a separate decision and no functional gate is waived.

The approved bounded work is:
1. Stage the exact signed `FixMyShot-0.1.0-alpha.2-universal.apk`
   (12,401,557 bytes; SHA256
   `2ebaf204f51e6580c7ce625af4e86394552b23dd045dbaf10e98147a71617c57`)
   with `BUILD-INFO.json`, `SHA256SUMS.txt` and the already verified complete
   corresponding-source archive; no rebuild/re-sign or replacement code2 bytes.
2. Prepare the release note/download metadata around those exact files and
   documented limitations; preserve genuine canonical screenshots and current
   author/privacy/format links without claiming unpublished assets are live.
3. Reconcile the intended tag/release-control commit with the separately pinned
   compiled source `1f89c979` in BUILD-INFO and source instructions. Do not
   claim a later documentation commit produced this APK or sign new bytes merely
   to align a documentary tag.
4. Present the complete asset manifest, exact source/tag plan and release copy
   for the separate merge/publication decision. Do not create a tag, publish a
   release, change the default branch, enroll a store or deploy a website.
5. After a separately authorized accessible test release exists, verify the
   actual Obtainium source/install/update path. Until then retain NOT_RUN; an
   Actions ZIP or the reported manual APK update is not Obtainium acceptance.

Store submissions, extra store-localized screenshot sets and stable1.0 are outside
this proposed GitHub development-preview scope. The completed dependency/N01
review and accepted manual comparisons are reused. Actual16KiB runtime,
100-item mixed failures, full human locale/accessibility, wider independent
privacy/color checks and detailed settings/default retention retain their
recorded dispositions.

## Historical verified development release kit — 2026-11-04

[Actions41851782509](https://github.com/santirodriguez/FixMyShot/actions/runs/41851782509) **passed**. The [original prepared kit](https://github.com/santirodriguez/FixMyShot/actions/runs/41851782509/artifacts/13335176928) contains four release assets and three review files; it expires 2026-11-11. Its downloaded ZIP digest/size/CRC and all seven original files were independently checked. The APK and source archive remain byte-identical to their accepted inputs. See the [durable manifest and verification receipt](releases/0.1.0-alpha.2-staging.json).

The original kit proposed tag `v0.1.0-alpha.2` at packaging-control commit `c9137faf6dd2e84fdac6122f66ae812f72a5bd58`; the actual compiled source remains `1f89c979a7b432e6117670d170ce8ca0df5e2567`. That historical receipt did not shift the proposed target. The kit's review directory preserves its original notes and integration snapshot; current Caja Personal copy supersedes those review notes. PR #1 subsequently merged into `release/0.1.0`; separately authorized default-branch integration and a reconciled documentary-tag plan remain necessary before publication. No tag, release or new signing occurred. Full functional0.1.0 acceptance remains incomplete.

## Alpha.2 exact-candidate signing — executed and verified

The maintainer separately authorized the exact signing operation on 2026-11-03.
It completed in Actions41719563373 and its original signed artifact was
independently inspected. The existing app-signing identity and repository secrets
were reused; no key, secret, credential, permission or environment setting changed.
[Download the original signed candidate artifact](https://github.com/santirodriguez/FixMyShot/actions/runs/41719563373/artifacts/13272275462) before
2026-11-10. The maintainer reports successful update and launch over alpha.1; remaining M09 scope is recorded in TESTING.

| Binding | Exact executed target |
| --- | --- |
| Compiled source/tree | 1f89c979a7b432e6117670d170ce8ca0df5e2567 / 5b30eeeb7596ca595ac1a3ba880c64c356d17859 |
| Original build/artifact | Actions41710230009 / bootstrap13266133766 |
| Downloaded artifact | 46,166,656 bytes; SHA256 d1c0e9ce9ca1842f3a93847d07d46928d491ce1d6fb7adad8a0e83b83ee58da9 |
| Unsigned APK | 12,356,414 bytes; SHA256 3fde1c9d38ac5517a4571ae8d4841f34e7f70ff2ddff64d24eea80f91d15a0fa |
| Application/version | org.cajapersonal.fixmyshot / 0.1.0-alpha.2 / code2 |
| Existing public signer SHA256 | 33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445 |
| Output | FixMyShot-0.1.0-alpha.2-universal.apk plus non-secret verification receipt |

The authorized workflow/script pins and request were updated to these exact
values. A copy of the verified original APK was signed without rebuilding. Require its source/hash/size,
production package/version, existing signer certificate, API26+ signature and
ELF/ZIP alignment; inspect the actual signed APK and confirm that every original
APK entry remains unchanged except the expected signing additions. Remove
temporary key material through the existing cleanup path. Stage the signed
candidate in the existing seven-day Actions artifact channel, whose visibility
was accepted for candidate delivery. Never expose secret values in reports.

The current executable signing workflow and request now bind this exact alpha.2
operation. Rerunning its authorized run signs the same bound candidate again;
a changed input needs a new decision. Signing does not authorize a merge, tag,
GitHub release, store upload, deployment or a new signing identity. Physical
installation and the increasing-code upgrade remain unexecuted until actually
reported or independently evidenced; Obtainium keeps its separate source limit.

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

## Request-triggered candidate signing automation — prepared

The signing workflow in [.github/workflows/sign-candidate.yml](../.github/workflows/sign-candidate.yml)
runs only on pushes changing [.ci/signing-candidate-request.json](../.ci/signing-candidate-request.json)
on `release/0.1.0-editor`. This supports preparation before a default-branch merge.
The committed request is `PREPARED`: only the request job runs; the signing job,
environment and secrets are skipped. Changing the state to `SIGN_CANDIDATE`
requires explicit authority to sign this exact candidate. Use a fresh request ID.
Rerunning an authorized signing run signs the same bound candidate again.

For the completed alpha.1 operation, the maintainer configured repository Actions
secrets directly; no Environment is required by the current workflow:

- `FIXMYSHOT_APP_KEYSTORE_BASE64`: the app-signing PKCS12 encoded as Base64.
- `FIXMYSHOT_APP_KEYSTORE_PASSWORD`: its password (also the PKCS12 key password).

Base64 is an encoding, not encryption. Store these values only as Actions secrets;
never commit them or paste them into chat. The Play upload key is not used here.
The connector cannot provision these settings; their existence is not established
by this workflow. A missing secret fails the signing step.

The implementation pins the original run41338068328 artifact and exact unsigned
APK size/hash, package/version and expected public signer certificate. It uses
Build Tools36.0.0, signs a copy, verifies the actual signature from API26,
ZIP alignment and package identity, then uploads only the signed APK, checksums,
public signature report and non-secret receipt. No downloaded artifact script is
executed. Temporary key material is removed on normal/error shell exit.

The signed Actions artifact is publicly downloadable and retained for seven days.
The maintainer approved that visibility; this is not a GitHub release/tag or
functional-release acceptance. Installation, upgrades, Obtainium and the remaining
manual matrix keep their own status. The alpha.1 execution subsequently completed in Actions41543938314;
this section's remaining preparation wording is historical and does not authorize code2. The original unsigned artifact currently expires2026-11-08;
expiry must fail without a silent rebuild or input substitution.

The older bounded plan below is retained as preparation history. Local key
creation has since been completed and the app certificate fingerprint is pinned
by the signing script; independent backup completion remains unverified.
Actual signing execution, environment provisioning and repository merge remain
separate consequential steps.

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

Production uses `org.cajapersonal.fixmyshot` through prereleases and eventual public 1.0. Ordinary current debug builds use `.visualspacing`; opt-in AVIF probes use `.avifprobe`. Earlier isolated `.heicbytes`, `.ux22next` and `.debug` diagnostics remain historical, and do not establish codec support. None establishes the production upgrade path. Keep FixMyShot's independent signing identity across updates.

`versionName` is SemVer without the tag's `v`; the checker supports plain/prerelease versions without build metadata. The first public candidate tag is `v0.1.0-alpha.2`. `versionCode` starts at 1 and increases for every subsequently distributed production-ID APK, including prereleases. Never reuse a code for different distributed bytes. A changed public APK needs a new version/code, not a replacement asset under the old identity.

The ledger is ordered by increasing code, with at most one `planned` row, last. Keep its name/code aligned with `app/build.gradle.kts`, README and the status-appropriate changelog heading. After actual distribution, record `distributed`, signed-APK SHA-256, public certificate SHA-256 and immutable source commit. Retain that row unchanged when appending the next candidate. A never-distributed planned row can be corrected. The checker validates current files; it does not authenticate edited history or inspect GitHub release state.

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
apksigner verify --verbose --print-certs FixMyShot-0.1.0-alpha.2-universal.apk
apkanalyzer manifest print FixMyShot-0.1.0-alpha.2-universal.apk
python3 /path/to/FixMyShot/scripts/check-apk.py FixMyShot-0.1.0-alpha.2-universal.apk
```

Compare actual app ID/name/code/minSdk, non-debuggable state and permissions with source/ledger. Compare the public certificate to the established identity. Check all shipping ABIs and actual ELF/ZIP 16 KiB alignment; alignment is not runtime 16 KiB acceptance. Retain unsigned and signed hashes. Never edit/realign signed bytes; reverify the signature after final staging.

Primary tool references, checked 2026-10-23: [apksigner](https://developer.android.com/tools/apksigner) · [apkanalyzer](https://developer.android.com/tools/apkanalyzer). These are maintainer-environment steps, not a request to install tools on the user's computer.

## 04 · Package four clear assets

| Public asset | Contents |
|---|---|
| `FixMyShot-<versionName>-universal.apk` | One signed production-ID APK, all shipping ABIs |
| `BUILD-INFO.json` | Source/tag, unsigned/signed digests, public cert, actual tools and exact acceptance evidence |
| `SHA256SUMS.txt` | Hashes of the APK, source archive and completed BUILD-INFO.json |
| `FixMyShot-<versionName>-source-materials.tar.gz` | Exact project/native/runtime source materials |

Complete the BUILD-INFO template from actual records; null/NOT_RUN values are reminders, not final evidence. Include recoverable corresponding source and pinned native build/license records. Exclude operator paths, device serials, private images and credentials. A checksum cannot contain itself.

Once BUILD-INFO is final, from the isolated asset directory:

```sh
sha256sum FixMyShot-0.1.0-alpha.2-universal.apk FixMyShot-0.1.0-alpha.2-source-materials.tar.gz BUILD-INFO.json > SHA256SUMS.txt
sha256sum -c SHA256SUMS.txt
```

Use the notes template, keep user benefits first and technical receipts expandable. Replace every EDIT marker and missing field with verified facts. Use tag-bound artwork/source links. Verify all four asset links after authorized publication. Keep README accurate before publication instead of presenting a dead Download button. Rendering notes does not establish release acceptance.

## 05 · Actually install and update

Use exact signed bytes for a fresh install and an upgrade from a successively versioned same-ID/same-certificate APK. The first release still needs a recorded sequential signed install/upgrade exercise. Confirm preferences, new-session PNG/metadata-removal defaults, imports, gallery output and receiver behavior. Physical/fidelity/accessibility and other blueprint gates remain required; see [TESTING](TESTING.md).

For **Obtainium**, add `https://github.com/santirodriguez/FixMyShot`, enable prereleases for 0.x and use `^FixMyShot-.*-universal\.apk$` if filtering is needed. The checker validates the proposed asset name against this pattern. Actual tracking/install/update remain NOT RUN; no community-catalog request is required.

## 06 · The final handoff

Before requesting separately authorized merge/tag/publication, reconcile the **then-current** candidate source and signed bytes, applicable gates, independent same-source rebuild, corresponding-source closure, publisher evidence/explicit disposition, ledger, APK manifest, tag, GitHub prerelease flag and final assets/hashes. The diagnostic APK acceptance does not authorize production signing or release. Scan README, changelog, About/version, authentic screenshots and notes for stale claims. Branding artwork is not an app screenshot. Verify actual website/store state before making availability claims.

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

