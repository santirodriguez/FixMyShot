package org.cajapersonal.fixmyshot.review

import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import org.cajapersonal.fixmyshot.core.SourceColor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.zip.CRC32
import java.util.zip.InflaterInputStream
import kotlin.math.abs

/** Bounded appearance descriptors, before the decoder can discard an invalid/unknown profile. */
internal object SourceAppearance {
    private const val LIMIT = 64 * 1024
    class Rejected(val reason: ReviewSources.Failure) : IOException("Unsupported source appearance")
    private fun invalid(reason: ReviewSources.Failure = ReviewSources.Failure.MALFORMED): Nothing = throw Rejected(reason)
    private fun tag(bytes: ByteArray, offset: Int = 0, size: Int = 4) =
        bytes.copyOfRange(offset, offset + size).toString(Charsets.US_ASCII)
    private fun uint(bytes: ByteArray, offset: Int): Long =
        (0..3).fold(0L) { value, index -> (value shl 8) or (bytes[offset + index].toLong() and 255L) }

    fun inspect(file: File, bounds: BitmapFactory.Options): SourceColor {
        // Container precision is checked below. A decoder's suggested bitmap configuration
        // describes its destination allocation, not necessarily the source bit depth.
        val profile = RandomAccessFile(file, "r").use { input ->
            when (bounds.outMimeType) {
                "image/png" -> png(input)
                "image/jpeg" -> jpeg(input)
                "image/webp" -> webp(input)
                else -> invalid()
            }
        }
        if (profile != null) validateProfile(profile)
        val space = bounds.outColorSpace
        if (space == null) {
            // Untagged inputs keep the ordinary decoder's sRGB convention. A supplied profile
            // must actually be recognized; ignoring an unreadable profile is not conversion.
            if (profile != null) invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
            return SourceColor.SRGB
        }
        if (profile != null) validateDecodedProfile(profile, space)
        if (space.isSrgb) return SourceColor.SRGB
        if (profile == null || space !is ColorSpace.Rgb || space.transferParameters == null ||
            (0..2).any { space.getMinValue(it) != 0f || space.getMaxValue(it) != 1f }) invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
        val adapted = ColorSpace.adapt(space, ColorSpace.ILLUMINANT_D50) as ColorSpace.Rgb
        for ((name, result) in listOf(ColorSpace.Named.DISPLAY_P3 to SourceColor.DISPLAY_P3,
            ColorSpace.Named.ADOBE_RGB to SourceColor.ADOBE_RGB)) {
            val known = ColorSpace.get(name) as ColorSpace.Rgb
            val matrix = (ColorSpace.adapt(known, ColorSpace.ILLUMINANT_D50) as ColorSpace.Rgb).transform
            // ICC encodings quantize matrices/curves; compare actual appearance, never names.
            if (adapted.transform.zip(matrix).all { abs(it.first - it.second) < 0.002f } &&
                (0..32).all { abs(space.eotf.applyAsDouble(it / 32.0) - known.eotf.applyAsDouble(it / 32.0)) < 0.002 }) {
                return result
            }
        }
        invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
    }

    /** Structural bounds are independent of the platform's profile parser. No profile text copies. */
    internal fun validateProfile(bytes: ByteArray) {
        if (bytes.size !in 132..LIMIT || uint(bytes, 0) != bytes.size.toLong() || tag(bytes, 36) != "acsp") invalid()
        if (tag(bytes, 16) != "RGB " || tag(bytes, 20) != "XYZ " ||
            tag(bytes, 12) !in setOf("mntr", "scnr", "spac")) invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
        val count = uint(bytes, 128)
        if (count !in 1L..128L || 132L + count * 12 > bytes.size) invalid()
        val tags = mutableSetOf<String>()
        val ranges = mutableListOf<Pair<Long, Long>>()
        repeat(count.toInt()) { index ->
            val row = 132 + index * 12
            if (!tags.add(tag(bytes, row))) invalid()
            val start = uint(bytes, row + 4); val length = uint(bytes, row + 8)
            val end = start + length
            if (start < 132 + count * 12 || start % 4 != 0L || length < 8 || end > bytes.size) invalid()
            // Identical tag payloads may be shared; partial overlaps are malformed.
            if (ranges.any { start < it.second && end > it.first && (start != it.first || end != it.second) }) invalid()
            ranges += start to end
        }
        if (!tags.containsAll(setOf("rXYZ", "gXYZ", "bXYZ", "rTRC", "gTRC", "bTRC")) ||
            tags.any { it.startsWith("A2B") || it.startsWith("B2A") }) invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
    }

    /** Reject platform fallback that silently ignores a supplied matrix/TRC profile. */
    private fun validateDecodedProfile(bytes: ByteArray, space: ColorSpace) {
        if (space !is ColorSpace.Rgb || space.transferParameters == null) invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
        val entries = (0 until uint(bytes, 128).toInt()).associate { index ->
            val row = 132 + index * 12
            tag(bytes, row) to bytes.copyOfRange(uint(bytes, row + 4).toInt(),
                (uint(bytes, row + 4) + uint(bytes, row + 8)).toInt())
        }
        fun fixed(data: ByteArray, offset: Int) = uint(data, offset).toInt() / 65536.0
        val matrix = (ColorSpace.adapt(space, ColorSpace.ILLUMINANT_D50) as ColorSpace.Rgb).transform
        for ((column, channel) in listOf("r", "g", "b").withIndex()) {
            val xyz = entries.getValue(channel + "XYZ")
            if (xyz.size != 20 || tag(xyz) != "XYZ ") invalid()
            for (row in 0..2) if (abs(fixed(xyz, 8 + row * 4) - matrix[column * 3 + row]) >= 0.002) invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
            val curve = entries.getValue(channel + "TRC")
            fun decode(x: Double): Double = when (tag(curve)) {
                "curv" -> {
                    if (curve.size < 12) invalid()
                    val count = uint(curve, 8)
                    if (count > 4096 || 12L + count * 2 != curve.size.toLong()) invalid()
                    fun word(i: Int) = ((curve[12 + i * 2].toInt() and 255) shl 8) or (curve[13 + i * 2].toInt() and 255)
                    when (count.toInt()) {
                        0 -> x
                        1 -> Math.pow(x, word(0) / 256.0)
                        else -> {
                            val position = x * (count - 1)
                            val low = position.toInt(); val high = minOf(low + 1, count.toInt() - 1)
                            (word(low) + (word(high) - word(low)) * (position - low)) / 65535.0
                        }
                    }
                }
                "para" -> {
                    if (curve.size < 12) invalid()
                    val kind = ((curve[8].toInt() and 255) shl 8) or (curve[9].toInt() and 255)
                    val sizes = listOf(1, 3, 4, 5, 7)
                    if (kind !in 0..4 || curve.size != 12 + sizes[kind] * 4) invalid()
                    val p = (0 until sizes[kind]).map { fixed(curve, 12 + it * 4) }
                    val g = p[0]
                    when (kind) {
                        0 -> Math.pow(x, g)
                        1 -> if (x >= -p[2] / p[1]) Math.pow(p[1] * x + p[2], g) else 0.0
                        2 -> if (x >= -p[2] / p[1]) Math.pow(p[1] * x + p[2], g) + p[3] else p[3]
                        3 -> if (x >= p[4]) Math.pow(p[1] * x + p[2], g) else p[3] * x
                        else -> if (x >= p[4]) Math.pow(p[1] * x + p[2], g) + p[5] else p[3] * x + p[6]
                    }
                }
                else -> invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
            }
            for (step in 0..32) {
                val expected = decode(step / 32.0)
                if (!expected.isFinite()) invalid()
                if (abs(expected - space.eotf.applyAsDouble(step / 32.0)) >= 0.002) invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
            }
        }
    }

    private fun png(input: RandomAccessFile): ByteArray? {
        if (input.length() < 33 || input.readLong() != 0x89504e470d0a1a0aUL.toLong()) invalid()
        var profile: ByteArray? = null
        var chunks = 0
        var pixels = false
        var srgb = false
        while (input.filePointer < input.length()) {
            if (++chunks > 4096 || input.length() - input.filePointer < 12) invalid()
            val length = input.readInt().toLong() and 0xffffffffL
            val kind = ByteArray(4).also { input.readFully(it) }
            val name = tag(kind)
            val start = input.filePointer
            if (length > input.length() - start - 4) invalid()
            if (chunks == 1) {
                if (name != "IHDR" || length != 13L) invalid()
                val header = ByteArray(13).also { input.readFully(it) }
                val depth = header[8].toInt() and 255
                if (depth == 16) invalid(ReviewSources.Failure.HIGH_PRECISION)
                if (depth !in setOf(1, 2, 4, 8)) invalid()
            }
            if (name in setOf("acTL", "fcTL", "fdAT")) invalid(ReviewSources.Failure.ANIMATED)
            if (name in setOf("cICP", "cLLI", "mDCV")) invalid(ReviewSources.Failure.HDR_UNSUPPORTED)
            if (name == "sRGB") { if (srgb || profile != null || pixels) invalid(); srgb = true }
            if (name == "iCCP") {
                if (profile != null || srgb || pixels || length !in 4L..LIMIT.toLong()) invalid()
                val payload = ByteArray(length.toInt()).also { input.readFully(it) }
                val crc = CRC32().apply { update(kind); update(payload) }.value
                if (crc != (input.readInt().toLong() and 0xffffffffL)) invalid()
                val endName = payload.indexOf(0.toByte())
                if (endName !in 1..79 || endName + 2 >= payload.size || payload[endName + 1] != 0.toByte()) invalid()
                profile = InflaterInputStream(ByteArrayInputStream(payload, endName + 2, payload.size - endName - 2)).use {
                    val output = ByteArrayOutputStream()
                    val block = ByteArray(4096)
                    while (true) {
                        val size = it.read(block)
                        if (size < 0) break
                        if (output.size() + size > LIMIT) invalid()
                        output.write(block, 0, size)
                    }
                    output.toByteArray()
                }
            }
            if (name == "IDAT") pixels = true
            input.seek(start + length + 4)
            if (name == "IEND") {
                if (length != 0L || !pixels || input.filePointer != input.length()) invalid()
                return profile
            }
        }
        invalid()
    }

    private fun jpeg(input: RandomAccessFile): ByteArray? {
        if (input.length() < 4 || input.readUnsignedShort() != 0xffd8) invalid()
        val pieces = mutableMapOf<Int, ByteArray>()
        var expected = 0
        var profileSize = 0
        var sof = false
        repeat(4096) {
            if (input.readUnsignedByte() != 255) invalid()
            var marker = input.readUnsignedByte()
            while (marker == 255) marker = input.readUnsignedByte()
            if (marker == 0xda) {
                if (!sof || pieces.size != expected) invalid()
                if (pieces.isEmpty()) return null
                return ByteArrayOutputStream(profileSize).apply {
                    for (sequence in 1..expected) write(pieces[sequence] ?: invalid())
                }.toByteArray()
            }
            if (marker in setOf(0xd8, 0xd9, 0x00, 0x01) || marker in 0xd0..0xd7) invalid()
            val length = input.readUnsignedShort() - 2
            if (length < 0 || length > input.length() - input.filePointer) invalid()
            val start = input.filePointer
            if (marker in setOf(0xc0, 0xc1, 0xc2)) {
                if (length < 6) invalid()
                if (input.readUnsignedByte() != 8) invalid(ReviewSources.Failure.HIGH_PRECISION)
                input.skipBytes(4)
                if (input.readUnsignedByte() !in setOf(1, 3)) invalid(ReviewSources.Failure.COLOR_UNSUPPORTED)
                sof = true
            } else if (marker in 0xc0..0xcf && marker !in setOf(0xc4, 0xc8, 0xcc)) invalid()
            if (marker == 0xe2) {
                val payload = ByteArray(length).also { input.readFully(it) }
                // Multi-picture JPEG can carry an HDR gain map; do not flatten it.
                if (length >= 4 && tag(payload) == "MPF\u0000") invalid(ReviewSources.Failure.HDR_UNSUPPORTED)
                if (length >= 12 && tag(payload, 0, 12) == "ICC_PROFILE\u0000") {
                    if (length < 15) invalid()
                    val sequence = payload[12].toInt() and 255; val total = payload[13].toInt() and 255
                    if (total == 0 || sequence !in 1..total || (expected != 0 && expected != total) || pieces.containsKey(sequence)) invalid()
                    expected = total; profileSize += length - 14
                    if (profileSize > LIMIT) invalid()
                    pieces[sequence] = payload.copyOfRange(14, length)
                }
            }
            if (marker == 0xe1) {
                val text = ByteArray(length).also { input.readFully(it) }.toString(Charsets.ISO_8859_1)
                if (text.contains("http://ns.adobe.com/hdr-gain-map/") ||
                    text.contains("urn:iso:std:iso:ts:21496:") ||
                    text.contains("http://ns.google.com/photos/1.0/container/")) invalid(ReviewSources.Failure.HDR_UNSUPPORTED)
            }
            input.seek(start + length)
        }
        invalid()
    }

    private fun webp(input: RandomAccessFile): ByteArray? {
        fun readTag() = tag(ByteArray(4).also { input.readFully(it) })
        if (input.length() < 12 || readTag() != "RIFF") invalid()
        val total = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
        if (total != input.length() - 8 || readTag() != "WEBP") invalid()
        var profile: ByteArray? = null
        var hasFlag = false
        var chunks = 0
        while (input.filePointer < input.length()) {
            if (++chunks > 4096 || input.length() - input.filePointer < 8) invalid()
            val kind = readTag(); val length = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            val end = input.filePointer + length + (length and 1L)
            if (end > input.length()) invalid()
            if (kind in setOf("ANIM", "ANMF")) invalid(ReviewSources.Failure.ANIMATED)
            if (kind == "VP8X") {
                if (chunks != 1 || length != 10L) invalid()
                val flags = input.readUnsignedByte()
                if (flags and 2 != 0) invalid(ReviewSources.Failure.ANIMATED)
                hasFlag = flags and 32 != 0
            }
            if (kind == "ICCP") {
                if (profile != null || !hasFlag || length !in 132L..LIMIT.toLong()) invalid()
                profile = ByteArray(length.toInt()).also { input.readFully(it) }
            }
            input.seek(end)
        }
        if (hasFlag != (profile != null)) invalid()
        return profile
    }
}
