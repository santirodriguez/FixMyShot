# First public 1.0.0 — integrated visual candidate dependency assurance

**Decision (2026-11-05): the maintainer explicitly approved bounded Option B for this new, exact unsigned candidate** after review of the fully reproducible build and current source materials. This is a **new candidate-specific issuer-provenance risk acceptance**, not an extension of the prior signing authority.

| Binding | Verified value |
| --- | --- |
| Application | `org.cajapersonal.fixmyshot` / `1.0.0` / versionCode `1` |
| Executable source | `3437722060019fdf8f892fddc4cc8121e0b61aa4` |
| Executable source tree | `5025043fb7aacc4330eb52458bbd80da708c0590` |
| Unsigned APK | **12,373,110 bytes**, SHA-256 `974a0b70982b9950d19f4ed476ab2915a23dbf72614d28c1790679147b3f73b0` |
| Baseline | [Android Actions 42112670798](https://github.com/santirodriguez/FixMyShot/actions/runs/42112670798), artifact `13454568712`, original ZIP SHA-256 `3662c7c565131b30f0d475bd96de1e07f5abe8c5e640eb6e8db55e5a8f12e7cf` |
| Independent rebuild | [Actions 42119230855](https://github.com/santirodriguez/FixMyShot/actions/runs/42119230855), artifact `13457827673`; **exact unsigned bytes match**; artifact ZIP SHA-256 `802397ec0ae81d45317d1d1853beb3211e8296fa888d5db9352d6bd679c62ebd` |
| Source materials | [Actions 42119231007](https://github.com/santirodriguez/FixMyShot/actions/runs/42119231007), artifact `13457213846`; source-materials archive **35,851,337 bytes**, SHA-256 `d9a71e1c71425a74226e38f1a72c70473ad070ce38a6f1e33bf29cad9894ccc4` |
| Preparation control | `release/1.0.0` commit `d4975930f40462ca0f5c1d34ce7df3c4f0162473`; not the executable source commit |

The earlier signed-source commit `70f3baeebb096461eb5072ca125f0c05334703da` is an ancestor of the new executable source. Their Git comparison has **41 changed paths across 52 intervening commits**, limited to application visuals, tests, release control, documentation, localized resources and approved canonical screenshots: no frozen Gradle dependency input, runtime lock, component catalog or issuer-proof inventory was changed. The prior pinned inventory, proof record, runtime lock and component-catalog checksums are carried forward **without treating unchanged bytes as independent publisher authentication**.

**Publisher provenance remains only 325 reviewed / 533 pending out of 858 frozen files**, including two pending runtime binary archives. Pending files are still pending, not authenticated. This decision does not establish a new publisher anchor, namespace/key trust, binary-source equivalence or an active Gradle dependency-verification policy. Two clean builds can reproduce the same unverified artifact bytes.

**Accepted risk:** the missing independent publisher corroboration for this exact app identity/version, executable source/tree, frozen dependency inputs and unsigned APK SHA-256. Any changed source, input, unsigned bytes, package identity or version requires a separate decision. The approved scope does not extend to other candidates or future releases.

**Not waived by Option B:** signing needed its own exact-candidate authorization (recorded separately below); key creation/rotation; merge to `main`, tag, GitHub Release or deployment; complete corresponding-source, license/notices or distribution clearance; detailed physical/16 KiB runtime, legacy SAF, language/accessibility, privacy/metadata/color, resource or upgrade/Obtainium acceptance. The source kit is technically prepared and verified, **not** a definitive legal-distribution approval.

**Historical signing provenance stays intact:** the previous [1.0.0 decision](dependency-assurance-1.0.0.md), [issuer-risk receipt](../releases/1.0.0-assurance.json) and [signed test-candidate receipt](../releases/1.0.0-signed-build-info.json) remain bound exclusively to `70f3baeebb096461eb5072ca125f0c05334703da`, unsigned SHA-256 `11ff3b0554a616ea3eff73a6ac89fb80b9bffc32936e8cbc1bedaba90c633ff0` and its old signed APK. That candidate's historical signing approval **is not transferable**. The machine-readable [new decision receipt](../releases/1.0.0-3437722-assurance.json) is the risk authority for the integrated visual candidate.

## Separate exact-candidate signing authorization and result — 2026-11-05

The maintainer subsequently and explicitly authorized signing ONLY the integrated visual 1.0.0/code1 unsigned APK SHA-256 `974a0b70982b9950d19f4ed476ab2915a23dbf72614d28c1790679147b3f73b0` from source `3437722060019fdf8f892fddc4cc8121e0b61aa4`, using the existing FixMyShot signing identity and guarded workflow. This later authorization is distinct from Option B and does not extend to the earlier signed candidate or subsequent builds.

[GitHub Actions 42123950177](https://github.com/santirodriguez/FixMyShot/actions/runs/42123950177) completed SUCCESS. The original signed APK is **12,417,941 bytes**, SHA-256 `1e67611b53dbb79a039dbee7e22a9f9731d076c6ebd9167d8028b6ba44cc4cde`, using the original public certificate SHA-256 `33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445`. Independent downloaded-byte inspection checked ZIP CRC, actual certificate DER, APK signatures v2/v3 and content digests, unchanged 467 original entries, three signing metadata entries and nine ZIP-aligned native libraries. See [new signed build receipt](../releases/1.0.0-3437722-signed-build-info.json).

The one-time signing request was returned to `PREPARED`; the older signed candidate and its signed/assurance receipts remain unchanged. **No main merge, tag, GitHub release, publication, website deployment, key/credential/permission change, physical install or full source/license/functional release acceptance** followed from this signing step.
