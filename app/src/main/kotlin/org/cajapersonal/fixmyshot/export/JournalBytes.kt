package org.cajapersonal.fixmyshot.export

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Bound private recovery JSON before allocating the whole file. Never used for image payloads. */
internal fun readJournalBytes(input: InputStream, maximum: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(maximum, 4096))
    val block = ByteArray(4096)
    while (true) {
        val count = input.read(block)
        if (count < 0) return output.toByteArray()
        if (count > maximum - output.size()) throw IOException("Journal exceeds limit")
        output.write(block, 0, count)
    }
}
