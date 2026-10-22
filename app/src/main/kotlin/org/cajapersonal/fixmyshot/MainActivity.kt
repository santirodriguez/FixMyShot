package org.cajapersonal.fixmyshot

import android.content.ComponentCallbacks2
import android.content.Intent
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
import org.cajapersonal.fixmyshot.export.MediaStore29Port
import org.cajapersonal.fixmyshot.export.BatchRecoveryScanner
import java.io.File
import org.cajapersonal.fixmyshot.export.SafTreeOutputPort
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

    // On Android 8/9, output is permitted only inside a folder explicitly selected here.
    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (!::model.isInitialized) return@registerForActivityResult
        if (uri == null) {
            model.destinationCancelled()
        } else {
            try {
                contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val granted = contentResolver.persistedUriPermissions.any { grant ->
                    grant.uri == uri && grant.isReadPermission && grant.isWritePermission
                }
                if (!granted) {
                    model.destinationUnavailable()
                    return@registerForActivityResult
                }
                model.startPreparedExport(applicationContext,
                    SafTreeOutputPort(contentResolver, uri))
            } catch (_: SecurityException) {
                model.destinationUnavailable()
            } catch (_: IllegalArgumentException) {
                model.destinationUnavailable()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sources = ReviewSources(applicationContext)
        model = ViewModelProvider(this)[ReviewViewModel::class.java]
        model.isBusy = false // An old activity can be destroyed before its import callback.
        model.checkPastExports(applicationContext)
        worker.execute {
            // Keep staged bytes needed to inspect an interrupted export until its records
            // have been explicitly reviewed; never prune them first.
            val prefs = getSharedPreferences("export-reviewed", MODE_PRIVATE)
            val safeToPrune = try {
                BatchRecoveryScanner(File(filesDir, "export-jobs")) { token ->
                    prefs.getBoolean(token, false)
                }.inspect() == null
            } catch (_: Exception) { false }
            if (safeToPrune) sources.pruneExpired()
        }
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
                        exportState = model.exportState,
                        onExport = { beginExport() },
                        onCancelExport = { model.cancelExport() },
                        onReviewedPrevious = { model.acknowledgePreviousExport(applicationContext) },
                        selectedShareIds = model.selectedShareIds,
                        shareBusy = model.shareBusy,
                        shareError = model.shareError,
                        onToggleShare = { model.toggleShareItem(it) },
                        onShare = { beginShare() },
                        onChoose = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onSelect = selectImage@{ id ->
                            if (model.exportState.isLocked) return@selectImage
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

    private fun beginExport() {
        if (!model.prepareExport()) return
        if (Build.VERSION.SDK_INT >= 29) {
            model.startPreparedExport(applicationContext, MediaStore29Port(contentResolver))
        } else {
            folderPicker.launch(null)
        }
    }

    /** A user-selected set of successful output copies is passed through Android Sharesheet. */
    private fun beginShare() {
        model.shareSelected(applicationContext) { ready ->
            if (ready == null || isDestroyed) return@shareSelected
            try {
                val chooser = Intent.createChooser(ready, getString(R.string.export_share_chooser))
                chooser.clipData = ready.clipData
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(chooser)
            } catch (_: android.content.ActivityNotFoundException) {
                model.shareChooserUnavailable()
            } catch (_: SecurityException) {
                model.shareChooserUnavailable()
            }
        }
    }

    private fun updateDraft(value: DraftSession) {
        if (model.exportState.isLocked || value == model.draft) return
        model.draft = value
        model.onDraftEdited()
        if (model.selectedId !in value.images.map { it.id }) model.selectedId = value.images.firstOrNull()?.id
        loadPreview()
    }

    private fun addImages(uris: List<Uri>) {
        if (model.isBusy || model.exportState.isLocked) return
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
                model.onDraftEdited()
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
        if (model.exportState.isLocked) return
        val id = model.selectedId ?: return
        model.draft = model.draft.removeItem(id)
        model.onDraftEdited()
        model.selectedId = model.draft.images.firstOrNull()?.id
        model.removeThumbnail(id)
        worker.execute { sources.discard(id) }
        loadPreview()
    }

    private fun discardDraft() {
        if (model.exportState.isLocked) return
        val ids = model.draft.images.map { it.id }
        model.draft = DraftSession()
        model.onDraftEdited()
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
