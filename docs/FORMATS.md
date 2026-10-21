# Codec status

## Initial Phase 1 slice

| Output | Implemented route | Current acceptance boundary |
|---|---|---|
| PNG | Android PNG encoder on API 26+ | Synthetic instrumentation asserts opaque RGB / alpha RGBA, container CRCs, independent reopen and exact normalized pixels. Runtime/device results must come from the matching CI/device report. |
| Lossless WebP | Source-built libwebp 1.6.0 on API 26-29; explicit Android `WEBP_LOSSLESS` on API 30+ | Tests require a real VP8L bitstream and exact decoded opaque pixels. API eligibility alone is not acceptance. |
| HEIC | Native AndroidX `HeifWriter` candidate from API 29; actual device probe required | Opaque sRGB, quality-100 **lossy** encoding; only a successfully encoded/reopened sample proves a particular configuration. Pixel and second-family acceptance remain mandatory. |
| AVIF | Native AndroidX `AvifWriter` candidate from API 34; actual device probe required | Opaque sRGB, quality-100 **lossy** encoding; not promised without a verified output. Pixel and second-family acceptance remain mandatory. |
| JXL | Not integrated or exposed in UI | Bounded reference-implementation experiment remains pending. No inclusion or deferral decision has been made. |

JPEG input is required by the functional blueprint but import is not yet implemented. JPEG output is excluded. Unsupported selections return typed errors and are never replaced with another format.

## Adapter limits

Inputs must already be ARGB_8888, SDR sRGB, oriented and within 24 megapixels plus the lower dynamic memory budget. WebP also enforces its 16,383-pixel side limit. These checks do not promise support for every in-range image. Working-set estimates are provisional; Java/native/PSS peak measurements are still required.

PNG and WebP receive pixels only, not source EXIF/XMP. Metadata-preservation requests fail explicitly until the bounded allowlist pipeline exists. Semi-transparent edge/color fidelity, all EXIF orientations, HDR handling and payload-preserving no-op exports remain later acceptance work. The current synthetic alpha fixtures cover transparent/opaque edges, not the full semi-transparent color contract.

Candidates remain app-private; no MediaStore/SAF publication occurs. PNG/platform WebP cancellation is checked at output writes and after encode, not guaranteed instantaneous within platform code. Native WebP also checks progress and scanline preparation. Timeout tests and measured cancellation latency remain open.

## Native writer candidate

The AndroidX 1.2.0-rc01 writer AAR declares `minSdk 29`; FixMyShot keeps app `minSdk 26`. The app uses the manifest merger's exact-package `tools:overrideLibrary="androidx.heifwriter"` only because native HEIC paths are entered at API 29+ and AVIF paths at API 34+, and no library writer code may be invoked on lower APIs. This exception is a **build-time compatibility mechanism, not evidence of runtime safety**; API 26 launch/PNG/WebP/native rejection tests and API 36 real encoding probes are mandatory acceptance gates. If API 26 fails, remove the exception and restore the last verified build. No `minSdk` increase or format substitution is allowed.

`NativeStillEncoder` uses `androidx.heifwriter:heifwriter:1.2.0-rc01`, a release candidate selected after comparison with stable 1.1.0: the 1.2 line contains material color-aspect and AVIF encoding fixes. It runs only off the main thread, accepts normalized opaque ARGB_8888 SDR sRGB pixels, refuses unsupported API/alpha/metadata policy/resource requests, uses AndroidX bitmap-input writers with quality 100 and a single still, and verifies ISO BMFF brand and platform reopened dimensions/MIME before retaining a candidate. A valid container brand and readable image dimensions are not sufficient: the adapter must also decode actual pixels (with bounded sampling for large inputs) before retaining an output. A decoder failure returns `INVALID_OUTPUT` and deletes the candidate, rather than counting it as native capability. Device-specific output and appearance still require independent validation. `NativeCodecProbeTest` attempts only a small odd-dimension synthetic sample on each test API and records actual success or a typed failure; an absent encoder is not presented as success. These routes are not yet exposed through an import/edit/export UI. Native writer cancellation is cooperative at boundaries and mid-encode latency remains an open G1 gate. **Quality 100 does not mean lossless.**

## Source

libwebp 1.6.0: `webmproject/libwebp@4fa21912338357f89e4fd51cf2368325b59e9bd9`, built with CMake and bounded JNI output to a duplicated file descriptor. The API 26-29 route deliberately avoids the ambiguous legacy Android WebP compression enum. Runtime codec acceptance and input-decoder support are separate claims.
