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

## Subsequent separate signing authorization (not yet executed)

The maintainer later explicitly authorized **signing only** the exact icon1 APK specified above (`bc1cc4f0ca0be8e1469081b323da760d576d87b608bc5d5a59b6d162993776e2`, 12,629,654 bytes) with the existing FixMyShot certificate SHA-256 `33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445`. This authorization was separate from the bounded issuer-risk decision. It **does not** extend to any different APK, keys, credentials, merge or public release.

**Execution remains blocked and NOT RUN.** The current signing workflow accepts an Android-bootstrap artifact tied to the source SHA; the original byte-identical clean APK is from the independent-rebuild workflow, not that Android-bootstrap artifact. The workflow must not be bypassed or supplied the temporary PR-merge APK, whose embedded revision changes its SHA-256. An attempted signing-workflow edit was rejected by the available tool safety check. Preserve this limitation without weakening provenance verification. `.ci/signing-candidate-request.json` remains `PREPARED`; `production_signing_executed=false`. Manual/device, matching public screenshots, corresponding-source legal-distribution and publication requirements remain open.
