# First public 1.0.0 — candidate-specific dependency assurance

**Decision (2026-11-05): bounded Option B accepted for the exact 1.0.0 unsigned candidate.** The maintainer reaffirmed the previously approved residual publisher-provenance risk criterion. Earlier alpha decisions remain historical; their authorization is not automatically reused.

| Binding | Verified value |
| --- | --- |
| Package / version | `org.cajapersonal.fixmyshot` / `1.0.0` / code `1` |
| Compiled source | `70f3baeebb096461eb5072ca125f0c05334703da` |
| Compiled tree | `d091a8ad59a039072c9d768196e89955138ae3fc` |
| Unsigned APK | 12356302 bytes, SHA-256 `11ff3b0554a616ea3eff73a6ac89fb80b9bffc32936e8cbc1bedaba90c633ff0` |
| Independent rebuild | [Run 41958986377](https://github.com/santirodriguez/FixMyShot/actions/runs/41958986377) — identical bytes |
| Source materials | [Run 41958986353](https://github.com/santirodriguez/FixMyShot/actions/runs/41958986353) — 1282 files verified |

The prior frozen dependency inputs are unchanged in the source diff. The app runtime lockfile, component catalog and provenance report have identical Git blobs. The original issuer-review partition remains **325 reviewed / 533 pending (858 total)**, including two pending runtime binary archives. No pending item is reclassified as authenticated. No new publisher anchor, namespace/key trust, or active Gradle verification policy is established.

**Accepted risk:** missing independent publisher corroboration for this exact source, frozen inputs, version and unsigned APK, consistent with the previous Option B posture. Reproducible builds can reproduce the same unverified dependency bytes. A changed input, source, APK SHA-256 or version requires a new decision.

**Not authorized or waived:** APK signing or credentials; merge, tag, GitHub publication or deployment; full corresponding-source/license approval; physical, accessibility, language, privacy, 16 KiB runtime, signed-certificate and Obtainium acceptance. Their evidence remains separately required. See [machine-readable receipt](../releases/1.0.0-assurance.json) and [unsigned build evidence](../releases/1.0.0-unsigned-build-info.json).
