package org.cajapersonal.fixmyshot

import android.content.ComponentCallbacks2
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.ItemValue
import org.cajapersonal.fixmyshot.review.ReviewBitmapRenderer
import org.cajapersonal.fixmyshot.review.ReviewScreen
import org.cajapersonal.fixmyshot.review.ReviewSources
import org.cajapersonal.fixmyshot.review.ReviewViewModel
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    // A separate serial queue allows one main preview and one small thumbnail decode at most.
    private val thumbnailWorker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var model: ReviewViewModel
    private lateinit var sources: ReviewSources
    private var generation = 0L
    private val pendingThumbnails = mutableSetOf<String>()

    private val picker = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(100)) { uris ->
        if (uris.isNotEmpty()) addImages(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sources = ReviewSources(applicationContext)
        model = ViewModelProvider(this)[ReviewViewModel::class.java]
        model.isBusy = false // An old activity can be destroyed before its import callback.
        worker.execute { sources.pruneExpired() }
        if (model.selectedId != null) loadPreview()
        scheduleThumbnails(model.draft.images.map { it.id })
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    ReviewScreen(
                        draft = model.draft,
                        selectedId = model.selectedId,
                        preview = model.preview,
                        thumbnails = model.thumbnails,
                        busy = model.isBusy,
                        invalidImports = model.invalidImports,
                        previewUnavailable = model.previewUnavailable,
                        api = Build.VERSION.SDK_INT,
                        onChoose = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onSelect = { id ->
                            model.selectedId = id
                            model.selectThumbnail(id)
                            scheduleThumbnails(listOf(id))
                            loadPreview()
                        },
                        onDestination = { value -> updateDraft(model.draft.setDestination(value)) },
                        onFormat = { value, onlySelected ->
                            val selected = model.selectedId
                            updateDraft(if (onlySelected && selected != null) {
                                val item = model.draft.images.first { it.id == selected }
                                model.draft.editItem(selected, item.overrides.copy(format = ItemValue.Custom(value)))
                            } else model.draft.setBatch(model.draft.batch.copy(format = value)))
                        },
                        onBackground = { value, onlySelected ->
                            val selected = model.selectedId
                            updateDraft(if (onlySelected && selected != null) {
                                val item = model.draft.images.first { it.id == selected }
                                model.draft.editItem(selected, item.overrides.copy(background = ItemValue.Custom(value)))
                            } else model.draft.setBatch(model.draft.batch.copy(background = value)))
                        },
                        onResetSelected = {
                            model.selectedId?.let { updateDraft(model.draft.resetItem(it)) }
                        },
                        onUndo = { updateDraft(model.draft.undo()) },
                        onRemove = { removeSelected() },
                        onDiscard = { discardDraft() },
                    )
                }
            }
        }
    }

    private fun updateDraft(value: DraftSession) {
        if (value == model.draft) return
        model.draft = value
        if (model.selectedId !in value.images.map { it.id }) model.selectedId = value.images.firstOrNull()?.id
        loadPreview()
    }

    private fun addImages(uris: List<Uri>) {
        if (model.isBusy) return
        val known = model.draft.images.mapTo(mutableSetOf()) { it.sourceToken }
        val remaining = 100 - model.draft.images.size
        val wanted = uris.distinctBy { it.toString() }.filter { it.toString() !in known }.take(remaining)
        if (wanted.isEmpty()) return
        model.isBusy = true
        worker.execute {
            val staged = wanted.map { sources.stage(it) }
            main.post {
                if (isDestroyed) {
                    staged.forEach { if (it is ReviewSources.ImportResult.Added) {
                        sources.discard(it.image.id)
                    } }
                    return@post
                }
                val current = model.draft.images.mapTo(mutableSetOf()) { it.sourceToken }
                val additions = staged.filterIsInstance<ReviewSources.ImportResult.Added>()
                    .map { it.image }.filter { image ->
                        if (current.add(image.sourceToken)) true else {
                            worker.execute { sources.discard(image.id) }
                            false
                        }
                    }
                model.invalidImports += staged.size - additions.size
                model.draft = model.draft.addImages(additions)
                if (model.selectedId == null) model.selectedId = model.draft.images.firstOrNull()?.id
                model.isBusy = false
                loadPreview()
                scheduleThumbnails(additions.map { it.id })
            }
        }
    }

    /** One bounded decode at a time; old UI snapshots remain valid after LRU eviction. */
    private fun scheduleThumbnails(ids: List<String>) {
        for (id in ids.distinct()) {
            if (model.hasThumbnail(id) || !pendingThumbnails.add(id)) continue
            thumbnailWorker.execute {
                val bitmap = try {
                    sources.decodeSample(id, maxLongSide = 256)
                } catch (_: IOException) {
                    null
                } catch (_: SecurityException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
                main.post {
                    pendingThumbnails.remove(id)
                    if (bitmap != null) {
                        if (!isDestroyed && model.draft.images.any { it.id == id }) {
                            model.storeThumbnail(id, bitmap)
                        } else bitmap.recycle()
                    }
                }
            }
        }
    }

    private fun loadPreview() {
        val token = ++generation
        val id = model.selectedId
        val image = model.draft.images.firstOrNull { it.id == id }
        val plan = model.draft.plans(Build.VERSION.SDK_INT).firstOrNull { it.itemId == id }
        model.preview = null
        model.previewUnavailable = false
        if (image == null || plan == null || plan.geometry == null) return
        worker.execute {
            var rendered: android.graphics.Bitmap? = null
            try {
                val source = sources.decodeSample(image.id)
                try {
                    rendered = ReviewBitmapRenderer.preview(source, image.orientedSize, plan).image
                } finally { source.recycle() }
            } catch (_: IOException) {
                // Display a generic localized error; never print source URIs or image names.
            } catch (_: IllegalArgumentException) {
                // Geometry or color is not currently previewable.
            } catch (_: SecurityException) {
                // A revoked provider grant cannot be assumed valid.
            }
            val candidate = rendered
            main.post {
                if (!isDestroyed && token == generation && model.selectedId == id &&
                    model.draft.revision == plan.sessionRevision) {
                    model.preview = candidate
                    model.previewUnavailable = candidate == null
                } else candidate?.recycle()
            }
        }
    }

    private fun removeSelected() {
        val id = model.selectedId ?: return
        model.draft = model.draft.removeItem(id)
        model.selectedId = model.draft.images.firstOrNull()?.id
        model.removeThumbnail(id)
        worker.execute { sources.discard(id) }
        loadPreview()
    }

    private fun discardDraft() {
        val ids = model.draft.images.map { it.id }
        model.draft = DraftSession()
        model.selectedId = null
        model.preview = null
        model.clearThumbnails()
        model.invalidImports = 0
        ++generation
        worker.execute { sources.discardAll(ids) }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::model.isInitialized && level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            model.clearThumbnails()
        }
    }

    override fun onDestroy() {
        generation++
        worker.shutdownNow()
        thumbnailWorker.shutdownNow()
        super.onDestroy()
    }
}
