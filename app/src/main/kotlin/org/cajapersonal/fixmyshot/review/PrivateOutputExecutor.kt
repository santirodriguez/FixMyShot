package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Looper
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.CodecRoute
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.ExportPreparation
import org.cajapersonal.fixmyshot.core.FailureCode
import org.cajapersonal.fixmyshot.core.MetadataPolicy
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.core.PixelSize
import org.cajapersonal.fixmyshot.core.PrivateOutputRecipe
import org.cajapersonal.fixmyshot.export.OutputStorage
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

enum class PrivateExecutionFailure {
    STALE_PLAN, SOURCE_CHANGED, SOURCE_UNREADABLE, UNSUPPORTED_SOURCE,
    RESOURCE_LIMIT, PRIVATE_STORAGE_LOW, CANCELLED, ENCODER_FAILED, INVALID_CANDIDATE
}

sealed interface PrivateExecutionResult {
    /** App-private output only. Caller must delete file after processing. */
    data class Ready(
        val file: File,
        val codecRoute: CodecRoute,
        val sourceSha256: String,
        val output: PixelSize,
        val resolvedBackgroundArgb: Int,
        val payloadPreserved: Boolean = false,
        /** Full rendered-canvas scan proved that WebP may omit entirely unused alpha. */
        val opaqueWebpProven: Boolean = false,
    ) : PrivateExecutionResult
    data class Failed(
        val reason: PrivateExecutionFailure,
        val codecFailure: FailureCode? = null,
    ) : PrivateExecutionResult
}

/** Sequential full-resolution execution, never an OS gallery or sharing transaction. */
internal class PrivateOutputExecutor(
    private val sources: ReviewSources,
    private val encoder: LosslessEncoder,
    private val processHeapLimitBytes: Long,
    private val copyDirectory: File? = null,
    private val hasWorkingSpace: (File) -> Boolean = OutputStorage::hasDirectoryHeadroom,
) {
    @Synchronized
    fun execute(
        draft: DraftSession,
        recipe: PrivateOutputRecipe,
        cancelled: AtomicBoolean = AtomicBoolean(false),
    ): PrivateExecutionResult {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Private encoding must run off the main thread" }
        if (cancelled.get()) return PrivateExecutionResult.Failed(PrivateExecutionFailure.CANCELLED)
        val current = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, processHeapLimitBytes)
            .candidates.firstOrNull { it.sourceId == recipe.sourceId }
        if (current != recipe) return PrivateExecutionResult.Failed(PrivateExecutionFailure.STALE_PLAN)
        if (!sources.matchesFingerprint(recipe.sourceId, recipe.sourceSha256)) {
            return PrivateExecutionResult.Failed(PrivateExecutionFailure.SOURCE_CHANGED)
        }

        var original: Bitmap? = null
        var rendered: Bitmap? = null
        var candidate: File? = null
        var keepCandidate = false
        try {
            if (copyDirectory != null) {
                if (!copyDirectory.isDirectory && !copyDirectory.mkdirs()) throw IOException("Private copy directory unavailable")
                if (!hasWorkingSpace(copyDirectory)) {
                    return PrivateExecutionResult.Failed(PrivateExecutionFailure.PRIVATE_STORAGE_LOW)
                }
                val copied = File.createTempFile("payload-", ".${recipe.request.format.extension}", copyDirectory)
                candidate = copied
                if (sources.copyPayload(recipe, copied, cancelled) && PrivateCandidateVerifier.accepts(copied, recipe.request)) {
                    if (cancelled.get()) return PrivateExecutionResult.Failed(PrivateExecutionFailure.CANCELLED)
                    if (!sources.matchesFingerprint(recipe.sourceId, recipe.sourceSha256)) {
                        return PrivateExecutionResult.Failed(PrivateExecutionFailure.SOURCE_CHANGED)
                    }
                    keepCandidate = true
                    // No background is visible in this exact full-source copy.
                    return PrivateExecutionResult.Ready(copied, recipe.codecRoute, recipe.sourceSha256,
                        recipe.canvas, Color.TRANSPARENT, payloadPreserved = true)
                }
                copied.delete()
                candidate = null
            }
            original = sources.decodeOriginal(recipe.sourceId, recipe.orientedSource, recipe.sourceColor)
            if (original.config != Bitmap.Config.ARGB_8888 || original.colorSpace?.isSrgb != true) {
                return PrivateExecutionResult.Failed(PrivateExecutionFailure.UNSUPPORTED_SOURCE)
            }
            val full = original
            val background = when (val value = recipe.background) {
                BackgroundChoice.Automatic -> ReviewBitmapRenderer.automaticColor(full)
                BackgroundChoice.White -> Color.WHITE
                BackgroundChoice.Black -> Color.BLACK
                BackgroundChoice.Transparent -> Color.TRANSPARENT
                is BackgroundChoice.CustomRgb -> Color.rgb(
                    (value.rgb shr 16) and 255, (value.rgb shr 8) and 255, value.rgb and 255)
            }
            val width = recipe.canvas.width
            val height = recipe.canvas.height
            val placement = recipe.placement
            if (width.toLong() * height.toLong() > 24_000_000L ||
                placement.left < 0 || placement.top < 0 ||
                placement.width <= 0 || placement.height <= 0 ||
                placement.left.toLong() + placement.width > width ||
                placement.top.toLong() + placement.height > height) {
                return PrivateExecutionResult.Failed(PrivateExecutionFailure.RESOURCE_LIMIT)
            }
            rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            rendered.setHasAlpha(recipe.request.hasAlpha)
            rendered.eraseColor(background)
            ReviewBitmapRenderer.drawContent(rendered, full, recipe.orientedSource,
                recipe.canvas, recipe.placement, recipe.crop)
            full.recycle()
            original = null
            if (cancelled.get()) return PrivateExecutionResult.Failed(PrivateExecutionFailure.CANCELLED)
            val opaqueWebpProven = recipe.request.format == OutputFormat.WEBP_LOSSLESS &&
                recipe.request.hasAlpha && PrivateCandidateVerifier.isFullyOpaque(rendered, cancelled)
            val pixelRequest = recipe.request.copy(metadataPolicy = MetadataPolicy.REMOVE_IDENTIFYING)
            when (val encoded = encoder.encode(rendered, pixelRequest, cancelled, 120_000)) {
                is EncodeResult.Failed -> return PrivateExecutionResult.Failed(
                    if (encoded.reason == FailureCode.CANCELLED) PrivateExecutionFailure.CANCELLED
                    else if (encoded.reason == FailureCode.IO_ERROR && copyDirectory != null && !hasWorkingSpace(copyDirectory))
                        PrivateExecutionFailure.PRIVATE_STORAGE_LOW
                    else PrivateExecutionFailure.ENCODER_FAILED, encoded.reason)
                is EncodeResult.Encoded -> {
                    candidate = encoded.file
                    if (encoded.route != recipe.codecRoute ||
                        !PrivateCandidateVerifier.accepts(encoded.file, pixelRequest, opaqueWebpProven)) {
                        return PrivateExecutionResult.Failed(PrivateExecutionFailure.INVALID_CANDIDATE)
                    }
                    if (recipe.request.metadataPolicy == MetadataPolicy.PRESERVE_ALLOWLIST) {
                        if (copyDirectory != null && !hasWorkingSpace(copyDirectory)) {
                            return PrivateExecutionResult.Failed(PrivateExecutionFailure.PRIVATE_STORAGE_LOW)
                        }
                        try {
                            MetadataAllowlist.apply(sources.sourceForMetadata(recipe.sourceId), encoded.file, recipe.request)
                        } catch (_: IOException) {
                            return PrivateExecutionResult.Failed(PrivateExecutionFailure.INVALID_CANDIDATE)
                        }
                        if (!PrivateCandidateVerifier.accepts(encoded.file, recipe.request, opaqueWebpProven)) {
                            return PrivateExecutionResult.Failed(PrivateExecutionFailure.INVALID_CANDIDATE)
                        }
                    }
                }
            }
            if (cancelled.get()) return PrivateExecutionResult.Failed(PrivateExecutionFailure.CANCELLED)
            if (!sources.matchesFingerprint(recipe.sourceId, recipe.sourceSha256)) {
                return PrivateExecutionResult.Failed(PrivateExecutionFailure.SOURCE_CHANGED)
            }
            keepCandidate = true
            return PrivateExecutionResult.Ready(requireNotNull(candidate), recipe.codecRoute,
                recipe.sourceSha256, recipe.canvas, background, opaqueWebpProven = opaqueWebpProven)
        } catch (_: java.io.InterruptedIOException) {
            return PrivateExecutionResult.Failed(if (cancelled.get()) PrivateExecutionFailure.CANCELLED else PrivateExecutionFailure.ENCODER_FAILED)
        } catch (_: IOException) {
            return PrivateExecutionResult.Failed(PrivateExecutionFailure.SOURCE_UNREADABLE)
        } catch (_: SecurityException) {
            return PrivateExecutionResult.Failed(PrivateExecutionFailure.SOURCE_UNREADABLE)
        } catch (_: IllegalArgumentException) {
            return PrivateExecutionResult.Failed(PrivateExecutionFailure.UNSUPPORTED_SOURCE)
        } finally {
            original?.takeUnless { it.isRecycled }?.recycle()
            rendered?.takeUnless { it.isRecycled }?.recycle()
            if (!keepCandidate) candidate?.delete()
        }
    }
}

