# Compatibility and validation scope — 2026-10-22

## Approved AVIF scope amendment — 2026-10-31

Decision: **DEFER AVIF for0.1.0**, explicitly approved by the user after
the bounded native/software and420-versus444 investigation. This qualifies
the AVIF portion of blueprint §§4/8/15/16,R20,A04 and G1/M02 only.
The immutable blueprint remains preserved; this amendment does not
defer HEIC or weaken any remaining output/privacy/transactional gate.

[Actions41212246198](https://github.com/santirodriguez/FixMyShot/actions/runs/41212246198)
completed FAILURE: all three Android routes on both444/420 gave
maximum channel error63 and7896pixels>2; independent host420 gave
maximum64. Direct/host444 retained maximum1, which cannot qualify
external receivers. Full [evidence](../feasibility/native-device-check.md#synthetic-yuv420-versus-yuv444-encoding-comparison--2026-10-31)
and unsuccessful measurements remain valid, not waived or relabeled PASS.

Remove AVIF from new format selections, retaining its enum, localized
label, serialized draft identity and diagnostic code. Restored AVIF drafts
remain explicitly unavailable under the existing FORMAT_NOT_READY gate;
never rewrite them to PNG/WebP, retry with a different format or alter
session fingerprints. PNG remains the new-session default. The existing
PNG/lossless WebP routes remain in scope; JXL keeps its separate deferral.
No software AVIF decoder/library enters normal export.

AVIF-specific production integration, physical handoff and qualification
leave the0.1.0 critical path. No new date/version for AVIF is promised.
Reconsider it only with a bounded proposal addressing exact output and
receiver fidelity, source/license/security review, actual shipping package
cost, physical memory/latency and applicable ABI/16KiB acceptance.
A future diagnostic pass alone does not reinstate shipping support.

HEIC remains a desired0.1.0 output with its independent unresolved
G1/M02 qualification. Its completed Pixel six-case report is retained;
do not repeat those unchanged tests or infer HEIC behavior from AVIF.
No final-release readiness, signing, merge, publication or physical-test
approval follows from this amendment.

Product direction now prioritizes complete useful functionality in coherent implementation batches. Backward compatibility is welcome when it remains straightforward. It must not require disproportionate complexity or repeated narrow debugging at the expense of the current Android experience.

This qualifies the original blueprint's strict legacy requirements (R22; API26 portions of A09, G3/G4 and M03). The existing minSdk26 and permission-free SAF path remain for now. No new legacy storage-write permission or default-Pictures adapter is planned solely to satisfy the original API26–28 requirement. Legacy-only failures are known limitations and may justify raising minSdk in a later focused decision; they are not hidden or silently described as supported. Current-device privacy, source integrity, truthful formats and transactional output requirements remain unchanged.

Ordinary checkpoints run the complete build/host checks plus API36 instrumented regressions. An explicit `profile=extended` as the last line of `.ci/android-validation-request.txt` opts into API26 smoke and API26/28 real-provider diagnostics. `profile=current` is the normal profile. A skipped legacy job is NOT RUN, never PASS. This reduces repeated old-device work while keeping a way to revisit it when useful. Validate once after a substantive block; fix concrete failures together and avoid rerunning an expensive matrix for documentation-only changes.

Known legacy evidence at source553042a / Actions39862769432: build and ordinary API26/API36 smoke passed; API26 real-provider export failed on WEBP_LOSSLESS with both items INVALID_CANDIDATE. The failure-only provider probe completed create/write/flush/sync/readback with matching bytes and deleted its own probe. Therefore the observed failure is candidate validation, not failed SAF writing. No production validation is weakened. API28 failed in its test harness when launching an ActivityResultLauncher belonging to an activity that had become unregistered; the earlier source9aa67f1 real-provider acceptance remains scoped evidence, not a pass for newer source.

Physical-device/manual acceptance remains pending. The change in compatibility priority does not authorize signing, publication or a claim of functional-release readiness.

## Implementation cadence amendment — 2026-10-23

Prioritize useful functional completion across larger groups of changes. A development turn or small completed block does not require a new CI run. Commit reviewed implementation, use proportionate local checks and accumulate work for an explicit larger validation checkpoint. Do not repeatedly rerun the emulator matrix to chase secondary or unstable diagnostic details. A concrete unresolved build, source-integrity, privacy or output-corruption problem still blocks dependent acceptance; deferred diagnostic failures must remain visible and must not be called passing.

The repeated automatic-locale-switch diagnostic is deferred and opt-in while ordinary import/recreation and resource completeness remain in normal coverage. This does not remove the three languages or establish language-switch/manual acceptance. Native diagnostic tuning, legacy-only issues and cosmetic receipt bookkeeping are secondary to working import/edit/export behavior. Do not silently reduce the final functional-release requirements; record what remains open and verify it at the appropriate larger acceptance checkpoint.

## AVIF receiver compatibility contract — 2026-10-31

This resolves how to apply the existing blueprint §§8/10/16,
A04/A05/A09/A13 and M02 to the measured AVIF discrepancy. It does not
defer AVIF, waive HEIC, change thresholds or authorize a shipping
decoder. The immutable blueprint and JXL disposition remain unchanged.

### Independent acceptance results

| Boundary | Required evidence | Current result |
| --- | --- | --- |
| Encoded file | Actual bytes, truthful MIME/extension, dimensions, color/metadata policy and independent decode | The built-in 6092B fixture independently decodes correctly; not general encoder acceptance |
| Android display route | Correct dimensions, opaque sRGB, PSNR >=38dB and maximum channel error <=2 on the synthetic gate | FAIL in all three measured API36 routes: PSNR44.6781555, max63,7896pixels>2 |
| Direct software route | The same fidelity gate plus shipping dependency, resource and lifecycle qualification | Synthetic fidelity PASS only: PSNR58.7403380, max1; production qualification OPEN |
| Storage and receiving application | Actual saved bytes, gallery/compatible-viewer reopen and share through real URI grants on the named device/application | NOT RUN for this software candidate; no inference from internal preview |
| First functional delivery | Approved AVIF scope amendment below; remaining G1/M02 and independent release gates | AVIF deferred from0.1.0; HEIC and other mandatory gates remain OPEN |

Full measurements and original artifact receipts:
[native device evidence](../feasibility/native-device-check.md#isolated-direct-yuv444-software-decoder-comparison--2026-10-31),
runtime50e77c66, [Actions41160768239](https://github.com/santirodriguez/FixMyShot/actions/runs/41160768239).
Its terminal FAILURE remains valid. Successful FFmpeg or direct decoding
does not override the failed Android route.

### Bounded receiver acceptance

Apply M02 to the Pixel 9 Pro XL and one other hardware family once a
candidate is eligible for the separately controlled physical handoff.
Record the installed OS/build and actual gallery/viewer package/version;
do not assume that an application name or API level identifies its
decoder. No universal Android/gallery/social-app compatibility claim
is made. The blueprint permits a compatible external viewer and an
explicit gallery limitation; it does not require every receiver to pass.

For each candidate, bind one receipt to source commit, APK hash,
output-file hash, encoder settings/chroma/color tags, dimensions and
the selected destination. Test separately:

1. Save a copy using the existing transaction rules. Verify read-back
   bytes and actual visibility/indexing. Saving, indexing and rendering
   are separate outcomes.
2. Open the saved URI in the named gallery or compatible external viewer.
   Inspect text, blue/white transitions, color and odd-size edges. A
   chooser launch or thumbnail alone is not a full-image fidelity pass.
3. Share the same output using actual temporary read grants. Record the
   chosen receiver and whether it opens the image. If the receiver
   transcodes it, distinguish received/transformed bytes and their
   appearance from the original saved AVIF. Do not upload private images
   or require account login merely to manufacture evidence.

Quantitative RGB sampling remains restricted to the integrated synthetic
fixture. Representative user screenshots require authorized visual
review and aggregate measurements only; no coordinate RGB logs, source
images or identifying metadata enter public receipts. An unavailable
receiver or absent physical evidence is NOT RUN; a visible failure stays
FAIL. A failed receiver cannot be relabeled passing because a different
receiver works. Report supported combinations and the specific limitation.

Keep unsupported choices explicit and offer the existing PNG/lossless
WebP alternative for a new user choice; never silently transcode, rename
an output or change a draft's selected format. Gallery limitations may
use the blueprint's explicit SAF/compatible-viewer path, with truthful
MIME and clear destination disclosure. This is an acceptance contract,
not new UI implementation or an automatic production fallback.

### Disposition and next engineering decision

The current YUV444 file is unsuitable for claiming compatibility with
the measured API36 framework path. Adding an internal software decoder
would repair only the application's own decode, leaving external
receivers unqualified. Keep normal AVIF disabled and the physical
handoff hold in force. Preserve the passing isolated decoder as evidence;
do not integrate it merely to make the fidelity check green.

The bounded synthetic420-versus444 comparison is now complete at
runtime7cf26bbb / [Actions41212246198](https://github.com/santirodriguez/FixMyShot/actions/runs/41212246198).
Both encodings FAIL the three Android routes (maximum63,7896pixels>2).
Independent host420 also FAILs (maximum64); host/direct444 remains
maximum1. The exact decoded420 U/V planes equal rounded2x2 means of
decoded444, with unchanged Y. See the [complete evidence](../feasibility/native-device-check.md#synthetic-yuv420-versus-yuv444-encoding-comparison--2026-10-31).
This supersedes the earlier proposed experiment, not its unchanged
fidelity thresholds. No runner defect or unchanged rerun is indicated.

The user approved deferring AVIF from0.1.0 on2026-10-31 after reviewing
this bounded failure evidence. The scope amendment at the top of this
document governs that decision. HEIC and every other release gate remain
independent.
Do not repeat the same experiment, change emulator properties, integrate
an internal decoder as an external-receiver fix, or hand off the Pixel
APK under this failed evidence.

The explicit scope resolution now permits0.1.0 without AVIF. It does not
lower the quality bar or qualify a failed codec. Future AVIF integration
requires a new bounded decision and the retained fidelity/receiver gates.
