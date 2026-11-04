# Changelog

Real changes. No imaginary release history.

## 0.1.0-alpha.2

Signed validation candidate handed off on 2026-11-03. Published downloads, when
available, are listed on GitHub Releases; the development prerelease preserves
these exact signed bytes.
The maintainer reports successful alpha.1/code1 to alpha.2/code2 update and launch.
Detailed settings/default retention and Obtainium acceptance remain pending.

- Refresh public documentation with Caja Personal identity and the project website; the frozen APK retains its original About links.
- Refine the home, information action and result buttons with a restrained layout and labelled icons.
- Put common destinations before Google Play while retaining Original as the neutral default.
- Separate background choices into a dialog and give lower editor actions clearer spacing.
- Restore readable system-bar icons when Settings opens, changes theme or returns to the editor.
- Preserve the installed alpha.1 signing identity and record the reported increasing-code update to code2.
- Keep PNG/lossless WebP output, permissions and dependency/native inputs unchanged.

This candidate was signed and independently inspected after its verified build,
source materials, byte-identical rebuild and exact artifact-specific assurance
approval. Update and launch are accepted by maintainer report; the remaining M09 checks
retain their recorded limits.
The maintainer accepted Preview2's visual/usability revision; the current API36
dark/light/return captures passed independent inspection. No new physical Pixel
measurement is inferred.
See [current evidence and remaining checks](docs/TESTING.md).

## 0.1.0-alpha.1

Signed candidate installed and opened by the maintainer; no GitHub release or
Obtainium/update acceptance. Historical development notes below describe the
original candidate; later acceptance is recorded in TESTING.



### Prepare the shot

- Import static SDR JPEG, PNG and WebP through the picker or Android share actions.
- Review up to 100 images with batch settings, individual overrides and thumbnails.
- Preserve content by default; explicitly crop with gestures/button alternatives, choose ratios, output sizes and backgrounds.
- Make Original / No platform the neutral clean-session destination, with explicit offline destination suggestions and a versioned Flexible frame for carefully showing margins below fill.
- Bring the PNG/lossless WebP choice into Automatic without changing the new-session PNG default; retain the full Customize and batch editor.
- Start new sessions with PNG and identifying-metadata removal; export separate PNG or lossless WebP copies.
- Defer HEIC, AVIF and JPEG XL outputs from 0.1.0; preserve existing draft identities with explicit unavailable-format feedback and no automatic replacement.
- Add bounded recognized SDR P3/Adobe RGB normalization, orientation handling, payload-preserving no-op paths and an explicit metadata allowlist.

### Keep the work moving

- Show verified saved output in a bounded in-app viewer, with direct View, Share and guarded Continue editing actions.
- Put Edit another image first after successful export: retire the matching private session safely, open the system picker directly and return to Start on cancellation without deleting saved copies.
- Simplify Settings and destination selection while preserving the neon identity, advanced controls and three offline languages.
- Retain the edit scope and pending ratio/color/size fields through screen recreation, isolate pending fields by image/scope and label batch-wide destination changes.
- Add editable draft recovery, one-step undo, foreground pause/continue and explicit remaining-item retry.
- Allow explicitly rechecking export after a refused preflight or destination without changing image settings.
- Show import progress and allow stopping after the current provider read, retaining completed images and separately counting untouched offers.
- Check device storage before and during private imports, explain low-space failures and preserve already prepared images.
- Keep provider runtime/cache-access failures typed so the import queue can finish without exposing private exception text.
- Check minimum private working space before preparing export candidates and distinguish it from destination storage failures.
- Check measurable destination space during export, handle provider pipes without file-only sync, report shortages and preserve uncertain cleanup for review.
- Keep uncertain destination creation out of retry, handle unexpected provider errors and explain when an output location could not be confirmed.
- Review uncertain outputs, verify selected saved files before sharing, and select all successful copies when useful.
- Build share payloads from verified image types without another provider metadata lookup; describe every selected PNG/WebP type.
- Confirm finishing export review before retiring restore/retry settings or clearing a completed session.
- Retire consistent private receipts after successful session review and explicitly remove verified owned pending MediaStore copies before a separately requested retry.
- Add localized import/preflight/result feedback, light/dark/system themes and offline English, Español and Català help.
- Add a separate synthetic HEIC/AVIF device diagnostic without enabling native exports.

### Around the app

- Integrate shared icon/header artwork, a redesigned product README and reusable release-note/build-receipt templates.
- Commit the standard Gradle wrapper and resolved version locks; add a checked release ledger and offline source-metadata validation.
- Make the selected-image scope and enlargement option full-row, labelled checkbox targets.
- Keep only the latest queued preview during rapid edits, discard obsolete preview work and guard result delivery by image/revision.
- Confirm session discard and bind discard/apply-to-all confirmations to the exact reviewed draft; changed batches require a fresh confirmation.
- Make saved-output share choices full-row checkbox targets and announce the actual crop zoom to accessibility services.

### Still open

This is an unpublished development candidate. Executable source 7cbed17d passed
[build/lint/99 core tests and 135 API 36 instrumented tests](https://github.com/santirodriguez/FixMyShot/actions/runs/41338068328),
including 12 editor tests and the next-image picker/cancel regression. The maintainer
accepted the requested PNG/WebP manual output review; no new real-image
pixel/PSNR/metadata measurements are inferred. Earlier accepted next-image UX,
two 100-item real-codec/simulated-provider cases and stored-alpha/render evidence
retain their recorded scope.

[Independent unsigned rebuild 41369848088](https://github.com/santirodriguez/FixMyShot/actions/runs/41369848088)
passed for that exact runtime: the actual rebuilt unsigned APK and original
12340030-byte APK are fully byte-identical. Exact project/libwebp kit and source-material stage are verified in
[Actions41373814270](https://github.com/santirodriguez/FixMyShot/actions/runs/41373814270).
This is not complete publisher/source/license or signed distribution acceptance.
Applicable remaining device/resource/accessibility/recovery coverage,
dependency/source/license disposition, final source materials, stable signing,
signed install/upgrade/Obtainium and truthful final screenshots still require
their acceptance. HEIC/AVIF/JXL implementation and qualification are deferred
from 0.1.0 and are outside this delivery's remaining critical path.
See [TESTING](docs/TESTING.md), [STATUS](docs/STATUS.md) and
[FORMATS](docs/FORMATS.md) for evidence, explicit scope and limits.

