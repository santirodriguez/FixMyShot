package org.cajapersonal.fixmyshot.core

/** This is an output contract: JPEG input support does not imply JPEG output. */
enum class OutputFormat(val mimeType: String, val extension: String, val lossless: Boolean) {
    PNG("image/png", "png", true),
    WEBP_LOSSLESS("image/webp", "webp", true),
    HEIC("image/heic", "heic", false),
    AVIF("image/avif", "avif", false),
    JXL("image/jxl", "jxl", true),
}

enum class MetadataPolicy { REMOVE_IDENTIFYING, PRESERVE_ALLOWLIST }
enum class CodecRoute { ANDROID_PNG, ANDROID_WEBP_LOSSLESS, LIBWEBP, NATIVE_HEIC, NATIVE_AVIF, JXL_REFERENCE }
enum class FailureCode {
    INVALID_INPUT, RESOURCE_LIMIT, UNSUPPORTED_API, UNSUPPORTED_ALPHA,
    NOT_INTEGRATED, METADATA_POLICY_UNAVAILABLE, COLOR_CONVERSION_REQUIRED,
    ENCODE_FAILED, INVALID_OUTPUT, IO_ERROR, CANCELLED, TIMEOUT, NATIVE_UNAVAILABLE,
}

data class PixelSize(val width: Int, val height: Int) {
    init { require(width > 0 && height > 0) { "Dimensions must be positive" } }
    val pixels: Long get() = width.toLong() * height.toLong()
}

data class EncodeRequest(
    val size: PixelSize,
    val format: OutputFormat = OutputFormat.PNG,
    val hasAlpha: Boolean = false,
    val metadataPolicy: MetadataPolicy = MetadataPolicy.REMOVE_IDENTIFYING,
)

/** Eligibility is not proof of successful encoding on a device/configuration. */
sealed interface CodecEligibility {
    data class ProbeRequired(val route: CodecRoute) : CodecEligibility
    data class Unavailable(val reason: FailureCode) : CodecEligibility
}

object CodecCapabilities {
    fun assess(api: Int, request: EncodeRequest): CodecEligibility {
        if (api < 26) return CodecEligibility.Unavailable(FailureCode.UNSUPPORTED_API)
        if (request.size.pixels > 24_000_000L) {
            return CodecEligibility.Unavailable(FailureCode.RESOURCE_LIMIT)
        }
        if (request.metadataPolicy == MetadataPolicy.PRESERVE_ALLOWLIST &&
            request.format !in setOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
            return CodecEligibility.Unavailable(FailureCode.METADATA_POLICY_UNAVAILABLE)
        }
        return when (request.format) {
            OutputFormat.PNG -> CodecEligibility.ProbeRequired(CodecRoute.ANDROID_PNG)
            OutputFormat.WEBP_LOSSLESS -> {
                if (request.size.width > 16_383 || request.size.height > 16_383) {
                    CodecEligibility.Unavailable(FailureCode.RESOURCE_LIMIT)
                } else {
                    CodecEligibility.ProbeRequired(
                        if (api >= 30) CodecRoute.ANDROID_WEBP_LOSSLESS else CodecRoute.LIBWEBP,
                    )
                }
            }
            OutputFormat.HEIC -> futureNative(api, 29, request, CodecRoute.NATIVE_HEIC)
            OutputFormat.AVIF -> futureNative(api, 34, request, CodecRoute.NATIVE_AVIF)
            OutputFormat.JXL -> CodecEligibility.Unavailable(FailureCode.NOT_INTEGRATED)
        }
    }

    private fun futureNative(api: Int, minimumApi: Int, request: EncodeRequest, route: CodecRoute): CodecEligibility = when {
        api < minimumApi -> CodecEligibility.Unavailable(FailureCode.UNSUPPORTED_API)
        request.hasAlpha -> CodecEligibility.Unavailable(FailureCode.UNSUPPORTED_ALPHA)
        else -> CodecEligibility.ProbeRequired(route)
    }
}

/** Estimates are provisional; actual native/Java peak memory is a separate G1 gate. */
object CodecMemoryBudget {
    private const val MIB = 1_048_576L
    fun estimatedBytes(request: EncodeRequest): Long {
        require(request.size.pixels <= 24_000_000L) { "Pixel limit exceeded" }
        val bytesPerPixel = if (request.format == OutputFormat.PNG) 12L else 24L
        return Math.addExact(Math.multiplyExact(request.size.pixels, bytesPerPixel), 16L * MIB)
    }
    fun permits(request: EncodeRequest, processHeapLimitBytes: Long): Boolean {
        require(processHeapLimitBytes > 0)
        if (request.size.pixels > 24_000_000L) return false
        return estimatedBytes(request) <= minOf(256L * MIB, processHeapLimitBytes / 2)
    }
}
