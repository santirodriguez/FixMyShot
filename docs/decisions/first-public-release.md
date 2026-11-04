# First public release — 1.0.0

Approved on 2026-11-04. This amendment supersedes the original blueprint's
0.1.0-public-prerelease sequence (R03, R29 and phases 6–7) and the alpha.2
publication proposal. It does not claim that unfinished validation passed.

| Field | First public release |
| --- | --- |
| Product | FixMyShot |
| Public author | Caja Personal |
| Application ID / namespace | org.cajapersonal.fixmyshot |
| Version / tag | 1.0.0 / v1.0.0 |
| Android versionCode | 1 |
| GitHub channel | Stable; not prerelease |
| Initial distribution | GitHub and Obtainium, one universal APK |
| Signing | Existing independent FixMyShot app certificate; no key rotation |
| Website | https://fixmyshot.cajapersonal.org/ |
| Privacy | https://fixmyshot.cajapersonal.org/privacy/ |
| Support | https://cajapersonal.org/donate/ |

## One-time version counter reset

Alpha.1/code1 and alpha.2/code2 were signed testing candidates handed to the
maintainer through Actions, not GitHub releases. Preserve their receipts and
history under `private_candidates` in the release ledger; do not relabel their
APK bytes or erase the installation evidence. The public release sequence
starts at 1.0.0/code1. Subsequent publicly distributed APKs must strictly increase
the code, including any public prereleases; this reset must never repeat after
public distribution.

The maintainer explicitly accepts uninstalling alpha.2 before installing 1.0.0.
Android will normally reject code1 over code2 as a downgrade. Uninstalling removes
app-private drafts/preferences; published copies remain user-owned. Do not call
this transition an upgrade, promise preference retention, manufacture an older
APK or change the production package to work around Android's check.

## Preserved scope and evidence

PNG and lossless WebP remain the normal exports. The previously approved
HEIC/AVIF/JXL deferrals apply to this renamed first-delivery scope, with their
original failed/unqualified evidence retained. No new codec, dependency,
permission, network capability or editor redesign is included.

Accepted manual UX/PNG/WebP/picker comparisons remain valid for unchanged paths;
the new About identity, version, exact build/source material, independent rebuild,
APK inspection and signature need current evidence. Genuine alpha.2 screenshots
remain explicitly historical until actual 1.0.0 captures are inspected; never
repaint version labels. Existing artwork remains the approved visual baseline.

The artifact-specific alpha dependency-assurance exceptions do not automatically
apply to a changed source/version/hash. Prepare the actual unsigned candidate
and input comparison before resolving its exact assurance disposition. Do not
change proof classifications or claim broader publisher authentication.

Manual resource, language/accessibility, privacy/color and preference checks keep
the recorded status in TESTING. Calling the target stable does not mark them
passed. Obtainium fresh installation follows accessible publication; real update
acceptance must use a later increasing-code same-certificate release, never an
equal-code reinstall. Until then report that update exercise NOT_RUN.

Implementation and candidate preparation are approved. Merge, tag, GitHub
publication, website deployment and store submission remain separate actions.
