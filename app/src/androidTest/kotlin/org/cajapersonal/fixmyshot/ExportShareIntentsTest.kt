package org.cajapersonal.fixmyshot

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.export.ExportShareIntents
import org.cajapersonal.fixmyshot.export.ShareImage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportShareIntentsTest {
    private val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
    private val first = Uri.parse("content://fixmyshot.synthetic/first")
    private val second = Uri.parse("content://fixmyshot.synthetic/second")

    @Test fun singleImageHasReadGrantAndMatchingMime() {
        val result = requireNotNull(ExportShareIntents.build(resolver,
            listOf(ShareImage(first, "image/png"))))
        assertEquals(Intent.ACTION_SEND, result.action)
        assertEquals("image/png", result.type)
        assertTrue(result.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(1, requireNotNull(result.clipData).itemCount)
        assertEquals(first, result.clipData!!.getItemAt(0).uri)
        @Suppress("DEPRECATION")
        val shared = result.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals(first, shared)
        assertEquals(0, result.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    @Test fun multipleFormatsShareAllUrisViaClipData() {
        val result = requireNotNull(ExportShareIntents.build(resolver,
            listOf(ShareImage(first, "image/png"), ShareImage(second, "image/webp"))))
        assertEquals(Intent.ACTION_SEND_MULTIPLE, result.action)
        assertEquals("image/*", result.type)
        assertEquals(2, requireNotNull(result.clipData).itemCount)
        assertEquals(second, result.clipData!!.getItemAt(1).uri)
        assertTrue(result.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        @Suppress("DEPRECATION")
        val shared = result.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals(listOf(first, second), shared)
    }

    @Test fun invalidAndRepeatedItemsAreRejected() {
        assertNull(ExportShareIntents.build(resolver, emptyList()))
        assertNull(ExportShareIntents.build(resolver,
            listOf(ShareImage(Uri.parse("file:///bad.png"), "image/png"))))
        assertNull(ExportShareIntents.build(resolver,
            listOf(ShareImage(first, "image/jpeg"))))
        assertNull(ExportShareIntents.build(resolver,
            listOf(ShareImage(first, "image/png"), ShareImage(first, "image/png"))))
    }
    @Test fun overBatchLimitIsRejectedBeforePreparingChooser() {
        val items = (0..100).map { index ->
            ShareImage(Uri.parse("content://fixmyshot.synthetic/item-$index"), "image/png")
        }
        assertNull(ExportShareIntents.build(resolver, items))
    }

}
