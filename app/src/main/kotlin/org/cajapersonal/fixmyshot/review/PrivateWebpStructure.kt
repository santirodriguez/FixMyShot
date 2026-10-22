package org.cajapersonal.fixmyshot.review

import org.cajapersonal.fixmyshot.core.EncodeRequest
import java.io.RandomAccessFile

internal object PrivateWebpStructure {
    fun accepts(input: RandomAccessFile, request: EncodeRequest): Boolean {
        if (input.length() < 25L || fourcc(input) != "RIFF") return false
        val declared = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
        if (declared != input.length() - 8 || fourcc(input) != "WEBP") return false
        var found = false
        var count = 0
        while (input.filePointer < input.length()) {
            if (++count > 16 || input.length() - input.filePointer < 8) return false
            val kind = fourcc(input)
            val bytes = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            val end = input.filePointer + bytes + (bytes and 1L)
            if (end > input.length() || kind !in setOf("VP8X", "VP8L")) return false
            if (kind == "VP8X") {
                if (count != 1 || bytes != 10L || found) return false
                if ((input.readUnsignedByte() and 0xef) != 0) return false
            } else {
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
        return found && input.filePointer == input.length()
    }

    private fun fourcc(input: RandomAccessFile): String =
        ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
}
