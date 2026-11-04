package org.cajapersonal.fixmyshot.review

import org.cajapersonal.fixmyshot.core.EncodeRequest
import org.cajapersonal.fixmyshot.core.MetadataPolicy
import java.io.RandomAccessFile

internal object PrivateWebpStructure {
    fun accepts(input: RandomAccessFile, request: EncodeRequest): Boolean {
        if (input.length() < 25L || fourcc(input) != "RIFF") return false
        val declared = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
        if (declared != input.length() - 8 || fourcc(input) != "WEBP") return false
        var found = false
        var flags = 0
        var extended = false
        var profileSeen = false
        var exifSeen = false
        val preserve = request.metadataPolicy == MetadataPolicy.PRESERVE_ALLOWLIST
        var count = 0
        while (input.filePointer < input.length()) {
            if (++count > 16 || input.length() - input.filePointer < 8) return false
            val kind = fourcc(input)
            val bytes = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            val end = input.filePointer + bytes + (bytes and 1L)
            if (end > input.length() || kind !in setOf("VP8X", "ICCP", "VP8L", "EXIF")) return false
            if (kind == "VP8X") {
                if (count != 1 || bytes != 10L || found) return false
                flags = input.readUnsignedByte()
                // EXIF is permitted only by explicit policy and a closed tag/directory check below.
                // Animation, XMP and reserved flags remain rejected.
                if ((flags and (if (preserve) 0xc7 else 0xcf)) != 0) return false
                extended = true
                input.skipBytes(3)
                fun uint24(): Int = input.readUnsignedByte() or
                    (input.readUnsignedByte() shl 8) or (input.readUnsignedByte() shl 16)
                if (uint24() + 1 != request.size.width || uint24() + 1 != request.size.height) return false
            } else if (kind == "ICCP") {
                if (!extended || found || profileSeen || (flags and 0x20) == 0 ||
                    bytes !in 132L..8192L) return false
                val profile = ByteArray(bytes.toInt()).also { input.readFully(it) }
                fun fourAt(index: Int): String = String(profile, index, 4, Charsets.US_ASCII)
                val size = ((profile[0].toInt() and 255) shl 24) or
                    ((profile[1].toInt() and 255) shl 16) or
                    ((profile[2].toInt() and 255) shl 8) or
                    (profile[3].toInt() and 255)
                if (size != profile.size || fourAt(12) != "mntr" ||
                    fourAt(16) != "RGB " || fourAt(20) != "XYZ " ||
                    fourAt(36) != "acsp") return false
                profileSeen = true
            } else if (kind == "EXIF") {
                if (!preserve || !extended || !found || exifSeen || (flags and 0x08) == 0 ||
                    bytes !in 14L..ExifAllowlistStructure.MAX_BYTES.toLong()) return false
                val exif = ByteArray(bytes.toInt()).also { input.readFully(it) }
                if (!ExifAllowlistStructure.accepts(exif, request)) return false
                exifSeen = true
            } else {
                if (exifSeen) return false
                if (found || bytes < 5L || input.readUnsignedByte() != 0x2f) return false
                val a = input.readUnsignedByte()
                val b = input.readUnsignedByte()
                val c = input.readUnsignedByte()
                val d = input.readUnsignedByte()
                val width = 1 + (a or ((b and 63) shl 8))
                val height = 1 + ((b ushr 6) or (c shl 2) or ((d and 15) shl 10))
                if (width != request.size.width || height != request.size.height) return false
                found = true
            }
            input.seek(end)
        }
        return found && (flags and 0x20 == 0 || profileSeen) &&
            (if (preserve) exifSeen && (flags and 0x08) != 0 else !exifSeen) &&
            input.filePointer == input.length()
    }

    private fun fourcc(input: RandomAccessFile): String =
        ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
}
