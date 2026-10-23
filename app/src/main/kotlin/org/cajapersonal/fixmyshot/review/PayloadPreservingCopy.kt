package org.cajapersonal.fixmyshot.review

import org.cajapersonal.fixmyshot.core.EncodeRequest
import org.cajapersonal.fixmyshot.core.MetadataPolicy
import org.cajapersonal.fixmyshot.core.OutputFormat
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.CRC32

/** Bounded container copy, never an image codec. Unsupported appearance data requires rendering. */
internal object PayloadPreservingCopy {
    private val pngSignature = 0x89504e470d0a1a0aUL.toLong()
    private class NotEligible : IOException()

    fun write(source: File, target: File, request: EncodeRequest, cancelled: AtomicBoolean): Boolean {
        if (request.metadataPolicy != MetadataPolicy.REMOVE_IDENTIFYING ||
            source.length() !in 1L..100L * 1024 * 1024) return false
        var retained = false
        val deadline = System.nanoTime() + 30_000_000_000L
        fun stopped() {
            if (cancelled.get() || System.nanoTime() >= deadline) throw InterruptedIOException("Copy stopped")
        }
        try {
            RandomAccessFile(source, "r").use { input ->
                RandomAccessFile(target, "rw").use { output ->
                    output.setLength(0)
                    when (request.format) {
                        OutputFormat.PNG -> png(input, output, request, ::stopped)
                        OutputFormat.WEBP_LOSSLESS -> webp(input, output, request, ::stopped)
                        else -> throw NotEligible()
                    }
                    stopped()
                    output.fd.sync()
                }
            }
            retained = true
            return true
        } catch (_: NotEligible) { return false }
        finally { if (!retained) target.delete() }
    }

    private fun requireCopy(value: Boolean) { if (!value) throw NotEligible() }
    private fun tag(input: RandomAccessFile) = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
    private fun copy(input: RandomAccessFile, output: RandomAccessFile?, bytes: Long,
        stopped: () -> Unit, crc: CRC32? = null) {
        val buffer = ByteArray(16 * 1024)
        var left = bytes
        while (left > 0) {
            stopped()
            val count = minOf(buffer.size.toLong(), left).toInt()
            input.readFully(buffer, 0, count)
            crc?.update(buffer, 0, count)
            output?.write(buffer, 0, count)
            left -= count
        }
    }

    private fun png(input: RandomAccessFile, output: RandomAccessFile, request: EncodeRequest, stopped: () -> Unit) {
        requireCopy(input.length() >= 57 && input.readLong() == pngSignature)
        output.writeLong(pngSignature)
        val seen = mutableSetOf<String>()
        var data = false
        var dataEnded = false
        var count = 0
        while (input.filePointer < input.length()) {
            stopped()
            requireCopy(++count <= 4096 && input.length() - input.filePointer >= 12)
            val length = input.readInt().toLong() and 0xffffffffL
            val name = tag(input)
            requireCopy(length <= input.length() - input.filePointer - 4)
            requireCopy(count != 1 || name == "IHDR")
            val keep = name in setOf("IHDR", "IDAT", "IEND", "sRGB", "gAMA", "cHRM", "sBIT")
            requireCopy(keep || name in setOf("eXIf", "tEXt", "zTXt", "iTXt", "tIME"))
            if (name != "IDAT" && name !in setOf("tEXt", "zTXt", "iTXt")) requireCopy(seen.add(name))
            if (name == "IDAT") { requireCopy(!dataEnded); data = true }
            else if (data) dataEnded = true
            if (name in setOf("sRGB", "gAMA", "cHRM", "sBIT")) requireCopy(!data)
            val crc = CRC32().apply { update(name.toByteArray(Charsets.US_ASCII)) }
            if (keep) { output.writeInt(length.toInt()); output.writeBytes(name) }
            if (name in setOf("IHDR", "sRGB", "gAMA", "cHRM", "sBIT")) {
                val expected = when (name) {
                    "IHDR" -> 13; "sRGB" -> 1; "gAMA" -> 4; "cHRM" -> 32
                    else -> if (request.hasAlpha) 4 else 3
                }
                requireCopy(length == expected.toLong())
                val bytes = ByteArray(expected).also { input.readFully(it) }
                crc.update(bytes)
                when (name) {
                    "IHDR" -> {
                        fun intAt(i: Int) = ((bytes[i].toInt() and 255) shl 24) or
                            ((bytes[i + 1].toInt() and 255) shl 16) or
                            ((bytes[i + 2].toInt() and 255) shl 8) or (bytes[i + 3].toInt() and 255)
                        requireCopy(intAt(0) == request.size.width && intAt(4) == request.size.height &&
                            bytes[8].toInt() == 8 && bytes[9].toInt() == (if (request.hasAlpha) 6 else 2) &&
                            bytes.sliceArray(10..12).all { it.toInt() == 0 })
                    }
                    "sRGB" -> requireCopy(bytes[0].toInt() in 0..3)
                    "gAMA" -> requireCopy(bytes.contentEquals(byteArrayOf(0, 0, 0xb1.toByte(), 0x8f.toByte())))
                    "cHRM" -> {
                        val values = listOf(31270,32900,64000,33000,30000,60000,15000,6000)
                        requireCopy(values.flatMap { v -> listOf((v ushr 24).toByte(),(v ushr 16).toByte(),(v ushr 8).toByte(),v.toByte()) }.toByteArray().contentEquals(bytes))
                    }
                    "sBIT" -> requireCopy(bytes.all { it.toInt() == 8 })
                }
                output.write(bytes)
            } else {
                if (name == "IEND") requireCopy(data && length == 0L)
                copy(input, if (keep) output else null, length, stopped, crc)
            }
            val stored = input.readInt()
            requireCopy((stored.toLong() and 0xffffffffL) == crc.value)
            if (keep) output.writeInt(stored)
            if (name == "IEND") { requireCopy(input.filePointer == input.length()); return }
        }
        throw NotEligible()
    }

    private fun webp(input: RandomAccessFile, output: RandomAccessFile, request: EncodeRequest, stopped: () -> Unit) {
        requireCopy(input.length() >= 25 && tag(input) == "RIFF")
        requireCopy((Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL) == input.length() - 8 && tag(input) == "WEBP")
        output.writeBytes("RIFF"); output.writeInt(0); output.writeBytes("WEBP")
        val seen = mutableSetOf<String>()
        var flags = 0
        var count = 0
        while (input.filePointer < input.length()) {
            stopped()
            requireCopy(++count <= 16 && input.length() - input.filePointer >= 8)
            val name = tag(input)
            val size = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            val end = input.filePointer + size + (size and 1L)
            requireCopy(end <= input.length() && name in setOf("VP8X", "VP8L", "EXIF", "XMP ") && seen.add(name))
            when (name) {
                "VP8X" -> {
                    requireCopy(count == 1 && size == 10L)
                    val bytes = ByteArray(10).also { input.readFully(it) }
                    flags = bytes[0].toInt() and 255
                    requireCopy(flags and 0xe3 == 0 && bytes.sliceArray(1..3).all { it.toInt() == 0 })
                    fun dim(i: Int) = 1 + ((bytes[i].toInt() and 255) or ((bytes[i + 1].toInt() and 255) shl 8) or ((bytes[i + 2].toInt() and 255) shl 16))
                    requireCopy(dim(4) == request.size.width && dim(7) == request.size.height)
                    requireCopy((flags and 0x10 != 0) == request.hasAlpha)
                    bytes[0] = (flags and 0x10).toByte()
                    output.writeBytes(name); output.writeInt(Integer.reverseBytes(10)); output.write(bytes)
                }
                "VP8L" -> {
                    requireCopy(size >= 5 && (count == 1 || "VP8X" in seen) && "EXIF" !in seen && "XMP " !in seen)
                    val start = input.filePointer
                    requireCopy(input.readUnsignedByte() == 0x2f)
                    val a = input.readUnsignedByte(); val b = input.readUnsignedByte()
                    val c = input.readUnsignedByte(); val d = input.readUnsignedByte()
                    requireCopy(1 + (a or ((b and 63) shl 8)) == request.size.width &&
                        1 + ((b ushr 6) or (c shl 2) or ((d and 15) shl 10)) == request.size.height &&
                        d and 0xe0 == 0 && (d and 0x10 != 0) == request.hasAlpha)
                    input.seek(start)
                    output.writeBytes(name); output.writeInt(Integer.reverseBytes(size.toInt()))
                    copy(input, output, size, stopped)
                    if (size and 1L != 0L) { requireCopy(input.readUnsignedByte() == 0); output.writeByte(0) }
                }
                else -> {
                    requireCopy("VP8X" in seen && "VP8L" in seen && size <= 1024 * 1024)
                    input.seek(end)
                }
            }
        }
        requireCopy("VP8L" in seen && (flags and 8 != 0) == ("EXIF" in seen) && (flags and 4 != 0) == ("XMP " in seen))
        val size = output.length() - 8
        output.seek(4); output.writeInt(Integer.reverseBytes(size.toInt()))
    }
}
