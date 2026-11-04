package org.cajapersonal.fixmyshot.review

import org.cajapersonal.fixmyshot.core.EncodeRequest
import org.cajapersonal.fixmyshot.core.MetadataPolicy
import java.io.RandomAccessFile
import java.util.zip.CRC32

/** Strict generated-PNG allowlist; CRC checks cover every chunk. */
internal object PrivatePngStructure {
    fun accepts(input: RandomAccessFile, request: EncodeRequest): Boolean {
        if (input.length() < 57 || input.readLong() != 0x89504e470d0a1a0aUL.toLong()) return false
        val preserve = request.metadataPolicy == MetadataPolicy.PRESERVE_ALLOWLIST
        val allowed = setOf("IHDR", "IDAT", "IEND", "sRGB", "gAMA", "cHRM", "sBIT") +
            if (preserve) setOf("eXIf") else emptySet()
        var headerSeen = false
        var dataSeen = false
        var significantBitsSeen = false
        var exifSeen = false
        var dataClosed = false
        var chunkCount = 0
        val buffer = ByteArray(8192)
        while (input.filePointer < input.length()) {
            if (++chunkCount > 100_000 || input.length() - input.filePointer < 12) return false
            val size = input.readInt().toLong() and 0xffffffffL
            if (size > input.length() - input.filePointer - 8) return false
            val tag = ByteArray(4).also { input.readFully(it) }
            val name = tag.toString(Charsets.US_ASCII)
            if (name !in allowed || (!headerSeen && name != "IHDR") ||
                (dataSeen && name !in setOf("IDAT", "IEND", "eXIf"))) return false
            val crc = CRC32().apply { update(tag) }
            var remaining = size
            if (name == "IHDR") {
                if (headerSeen || size != 13L) return false
                val ihdr = ByteArray(13).also { input.readFully(it) }
                crc.update(ihdr)
                fun intAt(index: Int): Int =
                    ((ihdr[index].toInt() and 255) shl 24) or
                    ((ihdr[index + 1].toInt() and 255) shl 16) or
                    ((ihdr[index + 2].toInt() and 255) shl 8) or
                    (ihdr[index + 3].toInt() and 255)
                if (intAt(0) != request.size.width || intAt(4) != request.size.height ||
                    ihdr[8].toInt() != 8 ||
                    ihdr[9].toInt() != (if (request.hasAlpha) 6 else 2) ||
                    ihdr[10].toInt() != 0 || ihdr[11].toInt() != 0 || ihdr[12].toInt() != 0) return false
                headerSeen = true
                remaining = 0L
            } else if (name == "sBIT") {
                // Android Bitmap.compress emits 8-bit significant-channel hints.
                // They are not identifying metadata and do not change decoded pixels.
                val channels = if (request.hasAlpha) 4 else 3
                if (significantBitsSeen || size != channels.toLong() || dataSeen) return false
                val bits = ByteArray(channels).also { input.readFully(it) }
                if (bits.any { it.toInt() != 8 }) return false
                crc.update(bits)
                significantBitsSeen = true
                remaining = 0L
            } else if (name == "eXIf") {
                if (!preserve || exifSeen || size !in 14L..ExifAllowlistStructure.MAX_BYTES.toLong()) return false
                val exif = ByteArray(size.toInt()).also { input.readFully(it) }
                if (!ExifAllowlistStructure.accepts(exif, request)) return false
                crc.update(exif)
                remaining = 0L
                exifSeen = true
                if (dataSeen) dataClosed = true
            } else if (name == "IDAT") {
                if (dataClosed) return false
                dataSeen = true
            } else if (name == "IEND") {
                if (!dataSeen || size != 0L || (preserve && !exifSeen)) return false
            }
            while (remaining > 0L) {
                val read = minOf(remaining, buffer.size.toLong()).toInt()
                input.readFully(buffer, 0, read)
                crc.update(buffer, 0, read)
                remaining -= read
            }
            if ((input.readInt().toLong() and 0xffffffffL) != crc.value) return false
            if (name == "IEND") return input.filePointer == input.length()
        }
        return false
    }
}
