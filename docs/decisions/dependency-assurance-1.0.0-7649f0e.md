# FixMyShot 1.0.0 — icon1 issuer provenance exception

**Decision 2026-11-05: bounded Option B APPROVED.** The maintainer explicitly responded “Aprobado. Go” for the exact **unsigned** new-icon candidate. This is not signing or publication authorization.

- Application: `org.cajapersonal.fixmyshot`; version `1.0.0` / code `1`.
- Executable source: `7649f0e002aca9c91cdf1294ab3033a9572029a9`; Git tree: `a222fb9964e2f328c59acf352e23e67e45650744`.
- Unsigned APK: **12,629,654 bytes**, SHA-256 `bc1cc4f0ca0be8e1469081b323da760d576d87b608bc5d5a59b6d162993776e2`.
- Exact-source clean baseline: [Actions 42132693754](https://github.com/santirodriguez/FixMyShot/actions/runs/42132693754), artifact `13464257643`. That workflow failed when comparing against a temporary PR-merge revision; the retained clean-source APK is the baseline.
- Independent clean rebuild: [Actions 42134905825](https://github.com/santirodriguez/FixMyShot/actions/runs/42134905825), artifact `13465741212`: **byte-identical unsigned APK**, original artifacts inspected.
- Exact source materials: [Actions 42132693740](https://github.com/santirodriguez/FixMyShot/actions/runs/42132693740), artifact `13463158916`, source archive SHA-256 `07ec6845cb4e9dda7731a854f8130cf87b9803e920c8f02f376ab55ea879cd3a`. Technical verification is not final distribution clearance.

The frozen Gradle dependency locks, runtime catalog and issuer-proof inputs retain their previous fingerprints. **325/858 files reviewed, 533/858 pending**, including two runtime binary archives. Pending issuer identities remain unauthenticated; this exception accepts their residual publisher-corroboration risk only for the exact source/tree, inputs and unsigned APK above. It grants no new publisher trust, source-to-binary equivalence or active strict Gradle verification. Any changed binding requires another decision.

**Still OPEN:** new signing authorization, signing execution/certificate verification, full source and third-party license distribution clearance, physical/device/16 KiB and manual acceptance, launcher mask, outdated canonical screenshot #1, accessibility/locales/privacy and Obtainium. Signing and release requests remain `PREPARED`; `branding_revision_pending=true` continues blocking public staging. No merge to `main`, tag, GitHub Release, store listing, website or credential change is approved.

Prior [integrated-visual decision](dependency-assurance-1.0.0-3437722.md) and [signed receipt](../releases/1.0.0-3437722-signed-build-info.json) remain historical and **do not transfer** their signing approval. See [machine-readable current receipt](../releases/1.0.0-7649f0e-assurance.json).

## Subsequent separate signing authorization and verified execution — 2026-11-06

The maintainer separately approved signing **only** the exact icon1 APK, source and tree identified above with the existing FixMyShot certificate SHA-256 `33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445`. The previous workflow-provenance limitation was resolved by a manually reviewed change to `.github/workflows/sign-candidate.yml`, with no credential, environment, permission or signing-identity change. Approval is not transferable to any other APK.

[Actions 42226227569](https://github.com/santirodriguez/FixMyShot/actions/runs/42226227569) successfully validated the exact source-bound original independent-rebuild artifact, signed without rebuilding, verified the resulting v2/v3 signature and uploaded [artifact 13504789246](https://github.com/santirodriguez/FixMyShot/actions/runs/42226227569/artifacts/13504789246). Original signed APK: **12,675,989 bytes**, SHA-256 `92efebea8ebfce6a88865a8b4842f61e4362d8951fb7949ea626c0a0245ea428`. Independent download and cryptographic verification confirmed the original signer certificate, v2 and v3 RSA signatures and content digests, 467/467 unchanged source APK payload members, three signing metadata additions, and 9/9 native libraries with static 16 KiB ZIP/ELF alignment. Full receipt: [1.0.0-7649f0e-signed-build-info.json](../releases/1.0.0-7649f0e-signed-build-info.json).

The one-time signing request is reset to `PREPARED`, preserving the older prepared release-stage control data because `.ci/development-release-request.json` still points to the **historical old-icon** APK and remains blocked (`branding_revision_pending=true`). The new signed candidate is **not** authorized for public staging, distribution or installation on another person's device. Current screenshot #1 approval, comprehensive source/license distribution review, new-icon physical install/16 KiB runtime, accessibility/locales/privacy/SAF/manual acceptance, merge, tag, release, website, store and Obtainium remain independently OPEN.
