package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.cajapersonal.fixmyshot.core.DraftSession

/** Survives rotation, but process-death journal recovery is a separate export-phase task. */
internal class ReviewViewModel : ViewModel() {
    var draft by mutableStateOf(DraftSession())
    var selectedId by mutableStateOf<String?>(null)
    var preview by mutableStateOf<Bitmap?>(null)
    var isBusy by mutableStateOf(false)
    var invalidImports by mutableStateOf(0)
    var previewUnavailable by mutableStateOf(false)

    private val thumbnailCache = ReviewThumbnailCache()
    var thumbnails by mutableStateOf<Map<String, Bitmap>>(emptyMap())
        private set

    fun hasThumbnail(id: String): Boolean = thumbnailCache.contains(id)
    fun selectThumbnail(id: String) { thumbnailCache.touch(id) }

    /** Called on the main thread after the worker has produced a bounded bitmap. */
    fun storeThumbnail(id: String, bitmap: Bitmap) {
        if (thumbnailCache.put(id, bitmap)) thumbnails = thumbnailCache.snapshot()
    }

    fun removeThumbnail(id: String) {
        thumbnailCache.remove(id)
        thumbnails = thumbnailCache.snapshot()
    }

    fun clearThumbnails() {
        thumbnailCache.clear()
        thumbnails = emptyMap()
    }
}
