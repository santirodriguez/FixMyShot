package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Looper
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.CodecRoute
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.ExportPreparation
import org.cajapersonal.fixmyshot.core.FailureCode
import org.cajapersonal.fixmyshot.core.PixelSize
import org.cajapersonal.fixmyshot.core.PrivateOutputRecipe
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

enum class PrivateExecutionFailure {
    STALE_PLAN, SOURCE_CHANGED, SOURCE_UNREADABLE, UNSUPPORTED_SOURCE,
    RESOURCE_LIMIT, CANCELLED, ENCODER_FAILED, INVALID_CANDIDATE
}

sealed interface PrivateExecutionResult {
    /** App-private output only. Caller must delete file after processing. */
    data class Ready(
        val file: File,
        val codecRoute: CodecRoute,
        val sourceSha256: String,
        val output: PixelSize,
        val resolvedBackgroundArgb: Int,
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
            original = sources.decodeOriginal(recipe.sourceId, recipe.orientedSource)
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
            val canvas = Canvas(rendered)
            if (placement.width == full.width && placement.height == full.height) {
                // Identity placement avoids any filtering or unnecessary scaling of source pixels.
                canvas.drawBitmap(full, placement.left.toFloat(), placement.top.toFloat(), Paint())
            } else {
                canvas.drawBitmap(full, Rect(0, 0, full.width, full.height),
                    RectF(placement.left.toFloat(), placement.top.toFloat(),
                        (placement.left + placement.width).toFloat(),
                        (placement.top + placement.height).toFloat()),
                    Paint(Paint.FILTER_BITMAP_FLAG))
            }
            full.recycle()
            original = null
            if (cancelled.get()) return PrivateExecutionResult.Failed(PrivateExecutionFailure.CANCELLED)
            when (val encoded = encoder.encode(rendered, recipe.request, cancelled, 120_000)) {
                is EncodeResult.Failed -> return PrivateExecutionResult.Failed(
                    if (encoded.reason == FailureCode.CANCELLED) PrivateExecutionFailure.CANCELLED
                    else PrivateExecutionFailure.ENCODER_FAILED, encoded.reason)
                is EncodeResult.Encoded -> {
                    candidate = encoded.file
                    if (encoded.route != recipe.codecRoute ||
                        !PrivateCandidateVerifier.accepts(encoded.file, recipe.request)) {
                        return PrivateExecutionResult.Failed(PrivateExecutionFailure.INVALID_CANDIDATE)
                    }
                }
            }
            if (cancelled.get()) return PrivateExecutionResult.Failed(PrivateExecutionFailure.CANCELLED)
            if (!sources.matchesFingerprint(recipe.sourceId, recipe.sourceSha256)) {
                return PrivateExecutionResult.Failed(PrivateExecutionFailure.SOURCE_CHANGED)
            }
            keepCandidate = true
            return PrivateExecutionResult.Ready(requireNotNull(candidate), recipe.codecRoute,
                recipe.sourceSha256, recipe.canvas, background)
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
