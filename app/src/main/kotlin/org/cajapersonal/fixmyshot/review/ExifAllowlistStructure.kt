package org.cajapersonal.fixmyshot.review

import org.cajapersonal.fixmyshot.core.EncodeRequest

/** Independent bounded TIFF directory check: unknown tags, thumbnails and nested opaque IFDs fail closed. */
internal object ExifAllowlistStructure {
    const val MAX_BYTES = 65_536
    private val primary = setOf(0x100, 0x101, 0x10f, 0x110, 0x112, 0x132, 0x13b, 0x8298, 0x8769, 0x8825)
    private val exif = setOf(0x9003, 0x9004, 0x9011, 0x9012, 0xa002, 0xa003, 0xa433, 0xa434)
    private val gps = setOf(1, 2, 3, 4, 5, 6)
    private val typeBytes = mapOf(1 to 1, 2 to 1, 3 to 2, 4 to 4, 5 to 8, 7 to 1, 9 to 4, 10 to 8)

    fun accepts(payload: ByteArray, request: EncodeRequest): Boolean = try {
        check(payload.size in 14..MAX_BYTES)
        val base = if (payload.size >= 6 && payload.copyOfRange(0, 6)
                .contentEquals(byteArrayOf(69, 120, 105, 102, 0, 0))) 6 else 0
        fun byte(at: Int): Int { check(at in payload.indices); return payload[at].toInt() and 255 }
        val little = when ("${byte(base).toChar()}${byte(base + 1).toChar()}") {
            "II" -> true
            "MM" -> false
            else -> error("Invalid TIFF byte order")
        }
        fun number(at: Int, size: Int): Long {
            check(at >= base && size <= payload.size - at)
            var value = 0L
            for (n in 0 until size) value = (value shl 8) or byte(at + if (little) size - 1 - n else n).toLong()
            return value
        }
        fun offset(at: Int): Int {
            val value = number(at, 4)
            check(value in 8L..(payload.size - base - 2).toLong())
            return base + value.toInt()
        }
        check(number(base + 2, 2) == 42L)
        val pending = java.util.ArrayDeque<Pair<Int, Int>>()
        pending.add(offset(base + 4) to 0)
        val visited = mutableSetOf<Int>()
        val dimensions = mutableSetOf<Int>()
        var orientation = false
        while (pending.isNotEmpty()) {
            val (start, group) = pending.removeFirst()
            check(visited.add(start) && visited.size <= 3)
            val count = number(start, 2).toInt()
            check(count in 1..32 && start.toLong() + 2 + count * 12L + 4 <= payload.size)
            val tags = mutableSetOf<Int>()
            val allowed = when (group) { 0 -> primary; 1 -> exif; else -> gps }
            for (index in 0 until count) {
                val entry = start + 2 + index * 12
                val tag = number(entry, 2).toInt()
                val type = number(entry + 2, 2).toInt()
                check(tag in allowed && tags.add(tag))
                val componentBytes = typeBytes[type] ?: error("Unsupported EXIF type")
                val components = number(entry + 4, 4)
                check(components in 1..513)
                val length = components * componentBytes
                val data = if (length <= 4) entry + 8 else offset(entry + 8)
                check(length <= payload.size - data)
                if (group == 0 && tag in setOf(0x8769, 0x8825)) {
                    check(type == 4 && components == 1L)
                    pending.add(offset(entry + 8) to if (tag == 0x8769) 1 else 2)
                } else if (tag in setOf(0x100, 0x101, 0x112, 0xa002, 0xa003)) {
                    check(group != 2 && type in setOf(3, 4) && components == 1L)
                    val value = number(data, componentBytes)
                    if (tag == 0x112) { check(value == 1L); orientation = true }
                    else {
                        check(value == (if (tag == 0x100 || tag == 0xa002) request.size.width else request.size.height).toLong())
                        dimensions.add(tag)
                    }
                }
            }
            // No IFD1/thumbnail, additional image, interoperability or MakerNote directory.
            check(number(start + 2 + count * 12, 4) == 0L)
        }
        orientation && dimensions.containsAll(setOf(0x100, 0x101, 0xa002, 0xa003))
    } catch (_: IllegalStateException) { false } catch (_: IllegalArgumentException) { false }
}
