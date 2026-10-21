# Codec status

## Initial Phase 1 slice

| Output | Implemented route | Current acceptance boundary |
|---|---|---|
| PNG | Android PNG encoder on API 26+ | Synthetic instrumentation asserts opaque RGB / alpha RGBA, container CRCs, independent reopen and exact normalized pixels. Runtime/device results must come from the matching CI/device report. |
| Lossless WebP | Source-built libwebp 1.6.0 on API 26-29; explicit Android `WEBP_LOSSLESS` on API 30+ | Tests require a real VP8L bitstream and exact decoded opaque pixels. API eligibility alone is not acceptance. |
| HEIC | Not integrated; selected writer contract starts at API 29 | Required Phase 1 continuation. Native output is planned as lossy and opaque; physical Pixel and another hardware family remain mandatory. |
| AVIF | Not integrated; selected native route starts at API 34 | Required Phase 1 continuation, with the same device/configuration distinction. |
| JXL | Not integrated or exposed in UI | Bounded reference-implementation experiment remains pending. No inclusion or deferral decision has been made. |

JPEG input is required by the functional blueprint but import is not yet implemented. JPEG output is excluded. Unsupported selections return typed errors and are never replaced with another format.

## Adapter limits

Inputs must already be ARGB_8888, SDR sRGB, oriented and within 24 megapixels plus the lower dynamic memory budget. WebP also enforces its 16,383-pixel side limit. These checks do not promise support for every in-range image. Working-set estimates are provisional; Java/native/PSS peak measurements are still required.

PNG and WebP receive pixels only, not source EXIF/XMP. Metadata-preservation requests fail explicitly until the bounded allowlist pipeline exists. Semi-transparent edge/color fidelity, all EXIF orientations, HDR handling and payload-preserving no-op exports remain later acceptance work. The current synthetic alpha fixtures cover transparent/opaque edges, not the full semi-transparent color contract.

Candidates remain app-private; no MediaStore/SAF publication occurs. PNG/platform WebP cancellation is checked at output writes and after encode, not guaranteed instantaneous within platform code. Native WebP also checks progress and scanline preparation. Timeout tests and measured cancellation latency remain open.

## Native writer integration compatibility checkpoint

An isolated Phase 1 integration experiment at commit `a9215c2ad467856467053e998191bfbf6f050327` attempted `androidx.heifwriter:heifwriter:1.2.0-rc01` for HEIC/AVIF bitmap encoding. The real Android build (Actions run `39535673813`) failed at `:app:processDebugMainManifest`: that AAR requires `minSdkVersion 29`, while FixMyShot's fixed app minimum remains 26. The experiment was not built or executed as an APK and must not be reported as codec acceptance.

The branch restores the last passing API 26 application/dependency layout until a reviewed packaging strategy can retain API 26 installations **without silently bypassing dependency API safety**. HEIC/AVIF remain first-functional-delivery targets, not deferred decisions: resolve a safe API-gated integration architecture and run actual encoding/reopen/color validation on capable devices. API 26 must continue to launch and execute its complete PNG/WebP matrix.

## Source

libwebp 1.6.0: `webmproject/libwebp@4fa21912338357f89e4fd51cf2368325b59e9bd9`, built with CMake and bounded JNI output to a duplicated file descriptor. The API 26-29 route deliberately avoids the ambiguous legacy Android WebP compression enum. Runtime codec acceptance and input-decoder support are separate claims.
