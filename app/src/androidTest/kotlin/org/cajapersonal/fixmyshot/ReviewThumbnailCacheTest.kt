package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cajapersonal.fixmyshot.review.ReviewThumbnailCache
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic in-memory fixtures, no provider URI or personal image involved. */
@RunWith(AndroidJUnit4::class)
class ReviewThumbnailCacheTest {
    @Test fun boundedLruKeepsRecentlySelectedOriginals() {
        val a = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val c = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        try {
            val single = a.allocationByteCount.toLong()
            val cache = ReviewThumbnailCache(maxBytes = single * 2)
            assertTrue(cache.put("a", a))
            assertTrue(cache.put("b", b))
            assertTrue(cache.touch("a"))
            assertTrue(cache.put("c", c))
            assertTrue(cache.contains("a"))
            assertFalse(cache.contains("b"))
            assertTrue(cache.contains("c"))
            assertEquals(single * 2, cache.residentBytes)
            assertEquals(setOf("a", "c"), cache.snapshot().keys)
        } finally { a.recycle(); b.recycle(); c.recycle() }
    }

    @Test fun invalidOversizedThumbnailIsRejectedBeforeRetention() {
        val big = Bitmap.createBitmap(257, 1, Bitmap.Config.ARGB_8888)
        val normal = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            val cache = ReviewThumbnailCache()
            assertThrows(IllegalArgumentException::class.java) { cache.put("big", big) }
            assertTrue(cache.put("normal", normal))
            cache.remove("normal")
            assertTrue(cache.snapshot().isEmpty())
            assertEquals(0L, cache.residentBytes)
        } finally { big.recycle(); normal.recycle() }
    }

    @Test fun trimDropsCacheOwnershipWithoutRecyclingVisibleBitmap() {
        val image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        try {
            val cache = ReviewThumbnailCache()
            assertTrue(cache.put("a", image))
            val oldSnapshot = cache.snapshot()
            cache.clear()
            assertTrue(cache.snapshot().isEmpty())
            assertFalse(image.isRecycled)
            assertSame(image, oldSnapshot["a"])
            assertEquals(0L, cache.residentBytes)
        } finally { image.recycle() }
    }
}
