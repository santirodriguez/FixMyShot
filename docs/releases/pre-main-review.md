# Pre-main review — 2026-11-04

## Public presentation

Caja Personal is the current public identity. README now uses a linked hero,
three visual feature columns, genuine unchanged Home/Settings captures, compact
installation/status sections and one donation button. No store/download badge
claims an unpublished APK is available. Current release notes and their reusable
template use the same identity and canonical project/support links.

Sites confirms the project active at https://fixmyshot.cajapersonal.org/ with
custom hostname/TLS active, published version3. Hosted privacy/support route
contents were not independently verified. No website or DNS changes were made.

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

## Remaining integration gates

PR1 is already merged into release/0.1.0; main remains the original LICENSE-only
baseline. Prepare a draft integration PR and inspect its exact-head Android CI.
Keep this candidate's current screenshots and complete functional acceptance
limits visible. No main merge, tag, release, new signature or physical Pixel
exercise is authorized by this documentation/test correction.

The original signed alpha.2 bytes, certificate, source kit and previously
accepted manual update/launch remain unchanged. The broader pending functional
coverage and actual Obtainium source/install/update remain unverified.
