package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap

/**
 * In-memory LRU of source thumbnails, never exported pixels.
 *
 * Entries are at most 256 px on the long side, with a 24 MiB resident budget.
 * Eviction only drops ownership: Compose may still be drawing an older snapshot,
 * so recycling here would be unsafe. Android releases the bitmap when unreferenced.
 * Use only from the activity's main thread.
 */
internal class ReviewThumbnailCache(private val maxBytes: Long = 24L * 1024L * 1024L) {
    init { require(maxBytes > 0) }

    private val entries = LinkedHashMap<String, Bitmap>(16, 0.75f, true)
    var residentBytes: Long = 0L
        private set

    fun contains(id: String): Boolean = entries.containsKey(id)

    fun touch(id: String): Boolean = entries[id] != null

    fun snapshot(): Map<String, Bitmap> = entries.toMap()

    fun put(id: String, image: Bitmap): Boolean {
        require(id.isNotBlank() && !image.isRecycled)
        require(maxOf(image.width, image.height) <= 256 && image.width > 0 && image.height > 0) {
            "Thumbnails must be decoded with a maximum side of 256 px"
        }
        val bytes = image.allocationByteCount.toLong()
        if (bytes <= 0L || bytes > maxBytes) return false
        entries.remove(id)?.let { residentBytes -= it.allocationByteCount.toLong() }
        while (residentBytes + bytes > maxBytes && entries.isNotEmpty()) {
            val oldest = entries.entries.iterator()
            val victim = oldest.next()
            residentBytes -= victim.value.allocationByteCount.toLong()
            oldest.remove()
        }
        entries[id] = image
        residentBytes += bytes
        check(residentBytes in 0..maxBytes)
        return true
    }

    fun remove(id: String) {
        entries.remove(id)?.let { residentBytes -= it.allocationByteCount.toLong() }
    }

    fun clear() {
        entries.clear()
        residentBytes = 0L
    }
}
