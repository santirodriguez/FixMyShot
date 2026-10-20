package org.cajapersonal.fixmyshot.core

fun contractCases(): List<Pair<String, () -> Unit>> {
    fun request(format: OutputFormat, alpha: Boolean = false, w: Int = 1344, h: Int = 2992) =
        EncodeRequest(PixelSize(w, h), format, alpha)
    fun unavailable(api: Int, req: EncodeRequest, reason: FailureCode) {
        check(CodecCapabilities.assess(api, req) == CodecEligibility.Unavailable(reason))
    }
    fun route(api: Int, format: OutputFormat, expected: CodecRoute) {
        check(CodecCapabilities.assess(api, request(format)) == CodecEligibility.ProbeRequired(expected))
    }
    fun rejects(block: () -> Unit) { check(runCatching(block).exceptionOrNull() is IllegalArgumentException) }
    return listOf(
        "new requests restore PNG and metadata removal" to {
            val req = EncodeRequest(PixelSize(1, 1))
            check(req.format == OutputFormat.PNG && req.metadataPolicy == MetadataPolicy.REMOVE_IDENTIFYING)
        },
        "invalid dimensions are rejected" to {
            rejects { PixelSize(0, 1) }; rejects { PixelSize(1, -1) }
        },
        "dimension multiplication does not overflow Int" to {
            check(PixelSize(Int.MAX_VALUE, Int.MAX_VALUE).pixels == 4_611_686_014_132_420_609L)
        },
        "Android below minimum is unavailable" to {
            OutputFormat.values().forEach { unavailable(25, request(it), FailureCode.UNSUPPORTED_API) }
        },
        "PNG is eligible but not certified" to { route(26, OutputFormat.PNG, CodecRoute.ANDROID_PNG) },
        "API 26 WebP requires source built codec" to { route(26, OutputFormat.WEBP_LOSSLESS, CodecRoute.LIBWEBP) },
        "API 29 WebP does not use the ambiguous legacy enum" to { route(29, OutputFormat.WEBP_LOSSLESS, CodecRoute.LIBWEBP) },
        "API 30 WebP uses the explicit lossless mode" to { route(30, OutputFormat.WEBP_LOSSLESS, CodecRoute.ANDROID_WEBP_LOSSLESS) },
        "HEIC below selected API 29 writer remains unavailable" to { unavailable(28, request(OutputFormat.HEIC), FailureCode.UNSUPPORTED_API) },
        "HEIC is not enabled just by API level" to { unavailable(36, request(OutputFormat.HEIC), FailureCode.NOT_INTEGRATED) },
        "native HEIC rejects alpha" to { unavailable(36, request(OutputFormat.HEIC, true), FailureCode.UNSUPPORTED_ALPHA) },
        "AVIF below API 34 remains unavailable" to { unavailable(33, request(OutputFormat.AVIF), FailureCode.UNSUPPORTED_API) },
        "native AVIF rejects alpha" to { unavailable(36, request(OutputFormat.AVIF, true), FailureCode.UNSUPPORTED_ALPHA) },
        "AVIF is not enabled just by API level" to { unavailable(36, request(OutputFormat.AVIF), FailureCode.NOT_INTEGRATED) },
        "JXL is pending evaluation not silently substituted" to { unavailable(36, request(OutputFormat.JXL), FailureCode.NOT_INTEGRATED) },
        "WebP rejects its unrepresentable edge" to {
            unavailable(36, request(OutputFormat.WEBP_LOSSLESS, w = 16_384, h = 1), FailureCode.RESOURCE_LIMIT)
        },
        "PNG does not inherit the WebP edge limit" to {
            check(CodecCapabilities.assess(26, request(OutputFormat.PNG, w = 16_384, h = 1)) is CodecEligibility.ProbeRequired)
            check(CodecCapabilities.assess(26, request(OutputFormat.PNG, w = 16_385, h = 1)) is CodecEligibility.ProbeRequired)
        },
        "pixel bombs are rejected before allocation" to {
            unavailable(36, request(OutputFormat.PNG, w = 6000, h = 5000), FailureCode.RESOURCE_LIMIT)
            check(!CodecMemoryBudget.permits(request(OutputFormat.PNG, w = Int.MAX_VALUE, h = Int.MAX_VALUE), Long.MAX_VALUE))
        },
        "metadata opt out does not promise unimplemented preservation" to {
            unavailable(36, request(OutputFormat.PNG).copy(metadataPolicy = MetadataPolicy.PRESERVE_ALLOWLIST), FailureCode.METADATA_POLICY_UNAVAILABLE)
        },
        "insufficient heap is explicit" to {
            check(!CodecMemoryBudget.permits(request(OutputFormat.WEBP_LOSSLESS), 64L * 1_048_576))
            check(CodecMemoryBudget.permits(request(OutputFormat.WEBP_LOSSLESS), 256L * 1_048_576))
            rejects { CodecMemoryBudget.permits(request(OutputFormat.PNG), 0) }
        },
        "JPEG is not an output and native formats are not called lossless" to {
            check(OutputFormat.values().none { it.mimeType == "image/jpeg" })
            check(!OutputFormat.HEIC.lossless && !OutputFormat.AVIF.lossless)
        },
        "large requests cannot wrap the estimated byte count" to {
            rejects { CodecMemoryBudget.estimatedBytes(request(OutputFormat.PNG, w = Int.MAX_VALUE, h = Int.MAX_VALUE)) }
        },
        "resource estimate is monotone over generated bounds" to {
            var previous = 0L
            for (edge in 1..1000) {
                val bytes = CodecMemoryBudget.estimatedBytes(request(OutputFormat.WEBP_LOSSLESS, w = edge, h = edge))
                check(bytes > previous); previous = bytes
            }
        },
    )
}
