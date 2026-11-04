package org.cajapersonal.fixmyshot.codec

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import java.io.File
import java.io.IOException

/** A second platform still-image decode route. Only callable after the API 29+ codec gate. */
@TargetApi(28)
internal object PlatformStillDecoder {
    fun decode(file: File, sampleSize: Int): Bitmap? = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
            decoder.setTargetSampleSize(sampleSize)
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
        }
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: UnsupportedOperationException) {
        null
    }
}
