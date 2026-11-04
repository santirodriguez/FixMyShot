package org.cajapersonal.fixmyshot

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import java.io.File
import java.io.IOException

/** Keep API 28+ framework references out of the JUnit4 test class on Android 8. */
@TargetApi(28)
internal object Api28GridDecoder {
    fun decode(file: File): Bitmap? = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
        }
    } catch (_: IOException) {
        null
    } catch (_: RuntimeException) {
        null
    }
}
