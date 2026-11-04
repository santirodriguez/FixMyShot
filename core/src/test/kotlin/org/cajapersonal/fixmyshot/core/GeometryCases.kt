package org.cajapersonal.fixmyshot.core

/** Device-independent assertions that mirror the immutable Phase 0 §7 geometry examples. */
fun geometryCases(): List<Pair<String, () -> Unit>> {
    val pixel = PixelSize(1344, 2992)
    fun plan(p: Long, q: Long) = GeometryPlanner.preservePixelsAtRatio(pixel, ExactRatio.of(p, q))
    fun expectFailure(block: () -> Unit) = check(runCatching(block).exceptionOrNull() is IllegalArgumentException)
    return listOf(
        "9:16 Pixel screenshot uses minimal unchanged pixels" to {
            val g = plan(9, 16)
            check(g.canvas == PixelSize(1683, 2992))
            check(g.placement == PixelPlacement(169, 0, 1344, 2992))
            check(g.rightMargin == 170 && g.bottomMargin == 0 && !g.resized)
        },
        "square Pixel screenshot exactly centers unchanged pixels" to {
            val g = plan(1, 1)
            check(g.canvas == PixelSize(2992, 2992) && g.placement == PixelPlacement(824, 0, 1344, 2992))
            check(g.rightMargin == 824 && !g.resized)
        },
        "4:5 Pixel screenshot rounds the exact ratio grid upward" to {
            val g = plan(4, 5)
            check(g.canvas == PixelSize(2396, 2995))
            check(g.placement == PixelPlacement(526, 1, 1344, 2992))
            check(g.rightMargin == 526 && g.bottomMargin == 2)
        },
        "Play phone envelope only adds mandatory margins" to {
            val g = GeometryPlanner.playPhoneEnvelope(pixel)
            check(g.canvas == PixelSize(1496, 2992) && g.placement == PixelPlacement(76, 0, 1344, 2992))
            check(g.rightMargin == 76 && !g.resized)
        },
        "explicit 1080x1920 contain discloses uniform downscale" to {
            val g = GeometryPlanner.exactCanvas(pixel, PixelSize(1080, 1920))
            check(g.canvas == PixelSize(1080, 1920))
            check(g.placement == PixelPlacement(109, 0, 862, 1920))
            check(g.resized && !g.enlarged && g.rightMargin == 109)
        },
        "ratio reduction uses identical minimal integer geometry" to {
            check(plan(18, 32) == plan(9, 16))
        },
        "ratio cannot be zero or negative" to {
            expectFailure { ExactRatio.of(0, 2) }
            expectFailure { ExactRatio.of(2, -1) }
        },
        "no implicit enlargement on an exact larger canvas" to {
            val g = GeometryPlanner.exactCanvas(PixelSize(40, 20), PixelSize(120, 120))
            check(!g.resized && !g.enlarged)
            check(g.placement == PixelPlacement(40, 50, 40, 20))
        },
        "explicit opted-in uniform enlargement" to {
            val g = GeometryPlanner.exactCanvas(PixelSize(40, 20), PixelSize(120, 120), allowEnlargement = true)
            check(g.placement == PixelPlacement(0, 30, 120, 60))
            check(g.resized && g.enlarged)
        },
        "container resource cap rejects excessive canvas rather than clipping" to {
            expectFailure { GeometryPlanner.preservePixelsAtRatio(pixel, ExactRatio.of(1, 100)) }
            expectFailure { GeometryPlanner.exactCanvas(pixel, PixelSize(8000, 8000)) }
            expectFailure { GeometryPlanner.playPhoneEnvelope(PixelSize(1, 24_000_000)) }
            expectFailure { GeometryPlanner.preservePixelsAtRatio(pixel, ExactRatio.of(9, 16), 0) }
        },
        "extreme ratios fail closed before unsafe Int conversion" to {
            expectFailure { GeometryPlanner.preservePixelsAtRatio(PixelSize(1, 2), ExactRatio.of(Long.MAX_VALUE, 1)) }
            expectFailure { GeometryPlanner.preservePixelsAtRatio(PixelSize(1, 1), ExactRatio.of(Int.MAX_VALUE.toLong(), 1)) }
            expectFailure { GeometryPlanner.preservePixelsAtRatio(PixelSize(1, 1), ExactRatio.of(1, Int.MAX_VALUE.toLong())) }
        },
        "odd padding always flows to right and bottom" to {
            val g = GeometryPlanner.preservePixelsAtRatio(PixelSize(7, 11), ExactRatio.of(2, 3))
            check(g.canvas == PixelSize(8, 12))
            check(g.placement.left == 0 && g.rightMargin == 1)
            check(g.placement.top == 0 && g.bottomMargin == 1)
        },
        "Play phone accepts portrait, landscape, square and small fixtures" to {
            for (size in listOf(PixelSize(1, 1), PixelSize(1, 2992), PixelSize(2992, 1), PixelSize(150, 500))) {
                val g = GeometryPlanner.playPhoneEnvelope(size)
                check(g.canvas.width >= maxOf(size.width, 320))
                check(g.canvas.height >= maxOf(size.height, 320))
                val side = maxOf(g.canvas.width, g.canvas.height)
                val shortSide = minOf(g.canvas.width, g.canvas.height)
                check(side <= 2 * shortSide)
                check(g.placement.width == size.width && g.placement.height == size.height)
            }
        },
        "generated preserve pixel cases are minimal and never crop" to {
            var seed = 0x1234ABCDL
            fun next(bound: Int): Int { seed = (1664525L * seed + 1013904223L) and 0xFFFFFFFFL; return (seed % bound).toInt() + 1 }
            repeat(1000) {
                val size = PixelSize(next(500), next(500))
                val ratio = ExactRatio.of(next(16).toLong(), next(16).toLong())
                val g = GeometryPlanner.preservePixelsAtRatio(size, ratio)
                check(!g.resized && !g.enlarged)
                check(g.placement.width == size.width && g.placement.height == size.height)
                check(g.placement.left >= 0 && g.placement.top >= 0 && g.rightMargin >= 0 && g.bottomMargin >= 0)
                check(kotlin.math.abs(g.placement.left - g.rightMargin) <= 1)
                check(kotlin.math.abs(g.placement.top - g.bottomMargin) <= 1)
                check(g.canvas.width.toLong() * ratio.q == g.canvas.height.toLong() * ratio.p)
                val k = g.canvas.width.toLong() / ratio.p
                if (k > 1) check((k - 1) * ratio.p < size.width || (k - 1) * ratio.q < size.height)
            }
        },
    )
}
