# Pre-main review — 2026-11-04

## Public presentation

Caja Personal is the current public identity. README now uses a linked hero,
three visual feature columns, genuine unchanged Home/Settings captures, compact
installation/status sections and one donation button. No store/download badge
claims an unpublished APK is available. Current release notes and their reusable
template use the same identity and canonical project/support links.

Sites confirms the project active at https://fixmyshot.cajapersonal.org/ with
custom hostname/TLS active. A separately authorized Site-only update published
version4 on2026-11-04 (source90cc9622). Home/privacy and EN/ES/CA selection were
verified in the published site. Donation link targets were checked; no payment
flow is claimed. This site deployment did not change Android APK bytes.

The [identity amendment](../decisions/public-identity.md) preserves immutable
source, legal provenance and the signed APK. Its About attribution/links are
still original; a full in-app identity change needs a new versioned candidate.
Current notes supersede the older kit's review-only copy. Reconcile documentary
tag/BUILD-INFO/notes together before publication; do not reuse obsolete copy.

## All six merge-triggered workflows inspected

Bound merge: 1b03caf076bcadd8dfb3dd8806d6e859b6224472; tree exactly equals PR1 head.

| Workflow | Run | Terminal outcome |
| --- | --- | --- |
| Android bootstrap | [41855218755](https://github.com/santirodriguez/FixMyShot/actions/runs/41855218755) | Build PASS; API36 134/135 tests PASS, one test-helper failure; optional legacy SAF SKIPPED |
| Independent unsigned rebuild | [41855218813](https://github.com/santirodriguez/FixMyShot/actions/runs/41855218813) | PASS; pinned compiled-source request unchanged |
| Exact source materials | [41855218824](https://github.com/santirodriguez/FixMyShot/actions/runs/41855218824) | PASS; completed N01 intentionally SKIPPED |
| AVIF software reference | [41855219574](https://github.com/santirodriguez/FixMyShot/actions/runs/41855219574) | Three ABI jobs and API36 fixture PASS |
| AVIF JNI/package comparison | [41855218748](https://github.com/santirodriguez/FixMyShot/actions/runs/41855218748) | Three ABI jobs, package comparison and API36 JNI PASS |
| AVIF display fidelity | [41855219394](https://github.com/santirodriguez/FixMyShot/actions/runs/41855219394) | Native/APK PASS; API36 synthetic fidelity FAIL, retained |

These were automatic merge-triggered runs, not new requested codec experiments.
Job/step outcomes were inspected for all six. Original downloaded evidence:

- Android smoke artifact13338046801: 2582996 bytes, SHA256
  a61f59acad5cbe76d066637dc8439f10c6a36648e35ebfc029ed871e37b38217.
  All73unique safe ZIP members and CRC verified. Fourteen other instrumentation
  groups pass123tests; editor11/12 passes. The failing case is
  quickTemplatesAndModeSwitchPreserveTheSameDraft at reveal/line490.
  Original editor-automatic.txt shows a visible non-scrollable Background dialog
  and its Match edges control inside the dialog bounds. The helper refreshed
  rootInActiveWindow separately for control, viewport and window containment,
  permitting inconsistent window snapshots. The correction uses one fresh root
  for that condition, retaining assertions, all12cases and the15-second timeout.
  New exact-head runtime acceptance is required; no application/exporter change.
- AVIF synthetic artifact13337841211:29425bytes, SHA256
  60b5ddc19e0823c60b2af553e92337c864160b629ba13e9bd9ae47da8fa08762.
  All13unique safe ZIP members and CRC verified. All three Android routes retain
  max channel error63 and7896pixels above2; direct444 max error1/zero affected,
  independent host420 max64/7896affected. This repeats the known failed receiver
  fidelity gate; it is not a new production regression or format qualification.
  PSNR>=38dB and max-channel-error<=2 remain required; AVIF stays deferred.

## Original integration checkpoint — superseded by current review

PR1 is already merged into release/0.1.0; main remains the original LICENSE-only
baseline. Prepare a draft integration PR and inspect its exact-head Android CI.
Keep this candidate's current screenshots and complete functional acceptance
limits visible. No main merge, tag, release, new signature or physical Pixel
exercise is authorized by this documentation/test correction.

The original signed alpha.2 bytes, certificate, source kit and previously
accepted manual update/launch remain unchanged. The broader pending functional
coverage and actual Obtainium source/install/update remain unverified.


## Current main-integration and publishing review

The normal PR2 run [41858529484](https://github.com/santirodriguez/FixMyShot/actions/runs/41858529484)
is SUCCESS: build/core/lint/packages and135/135API36 instrumentation, including
all12editor cases. Original smoke artifact13340300321 (2580636bytes, SHA256
6b8a00b01e125b949d8c6ac70b74e9bf3307524a522c2613ee1544cbc107e679)
has73unique safe members, valid CRC and135actual passing test results. Its
compiled synthetic mergef9bc363b has the identical tree as headf8dcc53e. This
closes the test-helper regression above without changing production code.

The current canonical1.png/2.png were opened in full and match the reviewed
alpha.2 originals and current candidate Git blobs. No screenshots or APKs were
replaced. The signed hash/source/certificate and accepted manual reports remain.

The release automation update adds a local Obtainium button with explicit
GitHub/prerelease/universal-APK configuration; refreshes staging for the current
branch/PR; and adds a manually dispatched publisher for main. Its reviewed kit
control must be contained in main. The publisher creates a draft, independently
reads back all four uploaded assets before publication and refuses remote
conflicts. Interrupted drafts can be resumed without clobbering assets.
Offline refusal/recovery tests and real-input local restaging passed. The new
hosted staging/PR checks must complete for this update before main is recommended.
No live release API write, new signing or physical acceptance is claimed.

The tag identifies the reviewed packaging-control commit, while BUILD-INFO and
the unchanged source archive identify compiled1f89c979. The historicalc9137faf
tag proposal is superseded. Full functional0.1.0 and actual Obtainium acceptance
remain incomplete; this is only the approved development-prerelease path.
