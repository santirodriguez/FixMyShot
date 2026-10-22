package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import org.cajapersonal.fixmyshot.core.PixelPlacement
import org.cajapersonal.fixmyshot.core.SourceRectangle
import android.graphics.Rect
import android.graphics.RectF
import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.PixelSize
import org.cajapersonal.fixmyshot.core.PlannedGeometry
import org.cajapersonal.fixmyshot.core.PreviewPlan
import java.lang.Math.round
import kotlin.math.min
import kotlin.math.pow

/**
 * Bitmap preview is derived exclusively from the immutable full-size plan; no alternate fit math.
 * Downsampled previews are never source pixels for a final export or fidelity proof.
 */
internal object ReviewBitmapRenderer {
    data class Rendered(val image: Bitmap, val plannedOutput: PixelSize, val sampled: Boolean)

    fun preview(sample: Bitmap, orientedSource: PixelSize, plan: PreviewPlan, maxLongSide: Int = 1200): Rendered {
        require(!sample.isRecycled && maxLongSide in 64..1600)
        require(sample.config == Bitmap.Config.ARGB_8888 && sample.colorSpace?.isSrgb == true) {
            "Review preview requires a normalized SDR sRGB bitmap"
        }
        val contained = (plan.geometry as? PlannedGeometry.Contained)?.value
        val cropped = (plan.geometry as? PlannedGeometry.Cropped)?.value
        val canvasSize = contained?.canvas ?: cropped?.output
            ?: throw IllegalArgumentException("Preview geometry unavailable")
        val outScale = min(1.0, maxLongSide.toDouble() / maxOf(canvasSize.width, canvasSize.height))
        val targetWidth = maxOf(1, round(canvasSize.width * outScale).toInt())
        val targetHeight = maxOf(1, round(canvasSize.height * outScale).toInt())
        require(targetWidth.toLong() * targetHeight <= 1600L * 1600L)
        require(orientedSource.width > 0 && orientedSource.height > 0)
        val image = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val transparent = plan.settings.background == BackgroundChoice.Transparent
        image.setHasAlpha(transparent)
        val background = when (val selected = plan.settings.background) {
            BackgroundChoice.Automatic -> automaticColor(sample)
            BackgroundChoice.White -> Color.WHITE
            BackgroundChoice.Black -> Color.BLACK
            BackgroundChoice.Transparent -> Color.TRANSPARENT
            is BackgroundChoice.CustomRgb -> Color.rgb(
                (selected.rgb shr 16) and 255, (selected.rgb shr 8) and 255, selected.rgb and 255)
        }
        image.eraseColor(background)
        drawContent(image, sample, orientedSource, canvasSize,
            contained?.placement ?: PixelPlacement(0, 0, canvasSize.width, canvasSize.height),
            cropped?.visibleSource)
        return Rendered(image, canvasSize, sample.width != orientedSource.width ||
            sample.height != orientedSource.height || targetWidth != canvasSize.width || targetHeight != canvasSize.height)
    }

    /** One mapping for sampled previews and original-resolution final output. */
    fun drawContent(target: Bitmap, source: Bitmap, orientedSource: PixelSize,
        plannedOutput: PixelSize, placement: PixelPlacement, crop: SourceRectangle? = null) {
        val canvas = Canvas(target)
        if (crop == null) {
            val rect = RectF(
                target.width.toFloat() * placement.left / plannedOutput.width,
                target.height.toFloat() * placement.top / plannedOutput.height,
                target.width.toFloat() * (placement.left + placement.width) / plannedOutput.width,
                target.height.toFloat() * (placement.top + placement.height) / plannedOutput.height)
            val identity = rect.width() == source.width.toFloat() && rect.height() == source.height.toFloat() &&
                rect.left == rect.left.toInt().toFloat() && rect.top == rect.top.toInt().toFloat()
            canvas.drawBitmap(source, Rect(0, 0, source.width, source.height), rect,
                Paint(if (identity) 0 else Paint.FILTER_BITMAP_FLAG))
        } else {
            require(crop.left + crop.width <= orientedSource.width + 1e-7 &&
                crop.top + crop.height <= orientedSource.height + 1e-7)
            val scaleX = source.width.toDouble() / orientedSource.width
            val scaleY = source.height.toDouble() / orientedSource.height
            val rect = RectF((crop.left * scaleX).toFloat(), (crop.top * scaleY).toFloat(),
                ((crop.left + crop.width) * scaleX).toFloat(),
                ((crop.top + crop.height) * scaleY).toFloat())
            val identity = rect.width() == target.width.toFloat() && rect.height() == target.height.toFloat() &&
                rect.left == rect.left.toInt().toFloat() && rect.top == rect.top.toInt().toFloat()
            val transform = Matrix().apply {
                setRectToRect(rect, RectF(0f, 0f, target.width.toFloat(), target.height.toFloat()), Matrix.ScaleToFit.FILL)
            }
            canvas.drawBitmap(source, transform, Paint(if (identity) 0 else Paint.FILTER_BITMAP_FLAG))
        }
    }

    /** The median linear edge luminance picks white or #121212 without cloning user pixels. */
    fun automaticColor(image: Bitmap): Int {
        val values = ArrayList<Double>(512)
        val strideX = maxOf(1, image.width / 128)
        val strideY = maxOf(1, image.height / 128)
        fun add(pixel: Int) {
            if (Color.alpha(pixel) == 0) return
            fun linear(channel: Int): Double {
                val normalized = channel / 255.0
                return if (normalized <= 0.04045) normalized / 12.92
                    else ((normalized + 0.055) / 1.055).pow(2.4)
            }
            val lightness = 0.2126 * linear(Color.red(pixel)) +
                0.7152 * linear(Color.green(pixel)) + 0.0722 * linear(Color.blue(pixel))
            values.add(lightness)
        }
        for (x in 0 until image.width step strideX) {
            add(image.getPixel(x, 0))
            add(image.getPixel(x, image.height - 1))
        }
        for (y in 0 until image.height step strideY) {
            add(image.getPixel(0, y))
            add(image.getPixel(image.width - 1, y))
        }
        if (values.isEmpty()) return Color.WHITE
        values.sort()
        val middle = values.size / 2
        val median = if (values.size % 2 == 0) (values[middle - 1] + values[middle]) / 2.0 else values[middle]
        return if (median >= 0.5) Color.WHITE else Color.rgb(0x12, 0x12, 0x12)
    }
}

