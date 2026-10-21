package org.cajapersonal.fixmyshot.core

/** Geometry operates on an already orientation-normalized source; no codec or view dependency. */
data class ExactRatio private constructor(val p: Long, val q: Long) {
    companion object {
        fun of(width: Long, height: Long): ExactRatio {
            require(width > 0 && height > 0) { "Ratio must be positive" }
            fun gcd(a: Long, b: Long): Long {
                var x = a
                var y = b
                while (y != 0L) {
                    val next = x % y
                    x = y
                    y = next
                }
                return x
            }
            val divisor = gcd(width, height)
            return ExactRatio(width / divisor, height / divisor)
        }
    }
}

/** The source is not cropped; downscale/upscale is explicit in the resulting immutable plan. */
data class PixelPlacement(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

data class ContainGeometry(
    val canvas: PixelSize,
    val placement: PixelPlacement,
    val resized: Boolean,
    val enlarged: Boolean,
) {
    val rightMargin: Int get() = canvas.width - placement.left - placement.width
    val bottomMargin: Int get() = canvas.height - placement.top - placement.height
}

/** Conservative pixel-space plans: never crop, stretch, or silently exceed the allocation limit. */
object GeometryPlanner {
    const val DEFAULT_MAX_CANVAS_PIXELS = 24_000_000L
    private const val PLAY_MIN_SIDE = 320L

    fun preservePixelsAtRatio(
        source: PixelSize,
        ratio: ExactRatio,
        maxCanvasPixels: Long = DEFAULT_MAX_CANVAS_PIXELS,
        minimumShortSide: Int = 0,
    ): ContainGeometry {
        require(minimumShortSide >= 0) { "Minimum side cannot be negative" }
        val k = maxOf(
            ceilDiv(source.width.toLong(), ratio.p),
            ceilDiv(source.height.toLong(), ratio.q),
            if (minimumShortSide == 0) 0L else ceilDiv(minimumShortSide.toLong(), minOf(ratio.p, ratio.q)),
        )
        val canvasWidth = checkedMultiply(ratio.p, k)
        val canvasHeight = checkedMultiply(ratio.q, k)
        return place(source, canvasWidth, canvasHeight, source.width.toLong(), source.height.toLong(), maxCanvasPixels)
    }

    /** Play phone minimum envelope: each side >=320 and aspect no more extreme than 1:2. */
    fun playPhoneEnvelope(source: PixelSize, maxCanvasPixels: Long = DEFAULT_MAX_CANVAS_PIXELS): ContainGeometry {
        var width = maxOf(source.width.toLong(), PLAY_MIN_SIDE)
        var height = maxOf(source.height.toLong(), PLAY_MIN_SIDE)
        if (height > 2L * width) width = ceilDiv(height, 2L)
        if (width > 2L * height) height = ceilDiv(width, 2L)
        return place(source, width, height, source.width.toLong(), source.height.toLong(), maxCanvasPixels)
    }

    /** Explicit exact-size contain, using one uniform scale capped at 1 unless enlargement is opted into. */
    fun exactCanvas(
        source: PixelSize,
        target: PixelSize,
        allowEnlargement: Boolean = false,
        maxCanvasPixels: Long = DEFAULT_MAX_CANVAS_PIXELS,
    ): ContainGeometry {
        val widthScale = target.width.toDouble() / source.width.toDouble()
        val heightScale = target.height.toDouble() / source.height.toDouble()
        val uniform = minOf(widthScale, heightScale, if (allowEnlargement) Double.POSITIVE_INFINITY else 1.0)
        val outWidth = minOf(target.width.toLong(), maxOf(1L, kotlin.math.floor(source.width * uniform + 0.5).toLong()))
        val outHeight = minOf(target.height.toLong(), maxOf(1L, kotlin.math.floor(source.height * uniform + 0.5).toLong()))
        return place(source, target.width.toLong(), target.height.toLong(), outWidth, outHeight, maxCanvasPixels)
    }

    private fun ceilDiv(value: Long, divisor: Long): Long = (value - 1L) / divisor + 1L

    private fun checkedMultiply(a: Long, b: Long): Long = try {
        Math.multiplyExact(a, b)
    } catch (e: ArithmeticException) {
        throw IllegalArgumentException("Geometry dimensions overflow", e)
    }

    private fun place(
        source: PixelSize,
        canvasWidth: Long,
        canvasHeight: Long,
        contentWidth: Long,
        contentHeight: Long,
        maxCanvasPixels: Long,
    ): ContainGeometry {
        require(maxCanvasPixels > 0L) { "Invalid canvas budget" }
        require(canvasWidth > 0 && canvasHeight > 0 && contentWidth > 0 && contentHeight > 0)
        require(canvasWidth <= Int.MAX_VALUE && canvasHeight <= Int.MAX_VALUE) { "Canvas side is too large" }
        require(contentWidth <= canvasWidth && contentHeight <= canvasHeight) { "Content does not fit canvas" }
        require(checkedMultiply(canvasWidth, canvasHeight) <= maxCanvasPixels) { "Canvas exceeds pixel budget" }
        val left = ((canvasWidth - contentWidth) / 2L).toInt()
        val top = ((canvasHeight - contentHeight) / 2L).toInt()
        return ContainGeometry(
            canvas = PixelSize(canvasWidth.toInt(), canvasHeight.toInt()),
            placement = PixelPlacement(left, top, contentWidth.toInt(), contentHeight.toInt()),
            resized = contentWidth != source.width.toLong() || contentHeight != source.height.toLong(),
            enlarged = contentWidth > source.width.toLong() || contentHeight > source.height.toLong(),
        )
    }
}
