package org.cajapersonal.fixmyshot.codec

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicBoolean

internal object NativeWebp {
    // A failed load must not poison object initialization for subsequent jobs.
    @Volatile private var loaded = false
    @Synchronized fun ensureLoaded() {
        if (!loaded) { System.loadLibrary("fixmyshot_webp"); loaded = true }
    }
    external fun encode(bitmap: Bitmap, fd: Int, cancelled: AtomicBoolean, timeoutMillis: Long, premultiplied: Boolean): Int
}
