package org.cajapersonal.fixmyshot

import android.content.ComponentCallbacks2
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.cajapersonal.fixmyshot.importing.IncomingImages
import org.cajapersonal.fixmyshot.settings.AppPreferences
import org.cajapersonal.fixmyshot.settings.AppLanguages
import org.cajapersonal.fixmyshot.settings.AppTheme
import org.cajapersonal.fixmyshot.settings.SettingsDialog
import org.cajapersonal.fixmyshot.diagnostics.NativeCheckDialog
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
import org.cajapersonal.fixmyshot.core.CropFocus
import org.cajapersonal.fixmyshot.core.CropPlanner
import org.cajapersonal.fixmyshot.core.DestinationCatalog
import org.cajapersonal.fixmyshot.core.EditableField
import org.cajapersonal.fixmyshot.core.OutputSizeChoice
import org.cajapersonal.fixmyshot.export.MediaStore29Port
import org.cajapersonal.fixmyshot.export.SafTreeOutputPort
import org.cajapersonal.fixmyshot.review.ReviewBitmapRenderer
import org.cajapersonal.fixmyshot.review.ReviewScreen
import org.cajapersonal.fixmyshot.review.ReviewSources
import org.cajapersonal.fixmyshot.review.ReviewViewModel
import org.cajapersonal.fixmyshot.review.LatestPreviewWorker
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val worker = LatestPreviewWorker()
    // A separate serial queue allows one main preview and one small thumbnail decode at most.
    private val thumbnailWorker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var model: ReviewViewModel
    private lateinit var sources: ReviewSources
    @Volatile private var generation = 0L
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
        model.attachSession(applicationContext)
        model.checkPastExports(applicationContext)
        if (savedInstanceState == null) receiveImages(intent)
        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            val theme by preferences.theme.collectAsState(initial = AppTheme.SYSTEM)
            val scope = rememberCoroutineScope()
            var settingsOpen by rememberSaveable { mutableStateOf(false) }
            var nativeCheckOpen by rememberSaveable { mutableStateOf(false) }
            var settingsError by remember { mutableStateOf(false) }
            val dark = when (theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            LaunchedEffect(model.pendingImportCount, model.isBusy, model.exportState.isLocked) {
                model.drainImports(applicationContext)
            }
            LaunchedEffect(model.draft.revision, model.selectedId) {
                loadPreview()
                scheduleThumbnails(model.draft.images.map { it.id })
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !dark
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !dark
            }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                if (settingsOpen) SettingsDialog(
                    theme = theme,
                    editable = !model.isBusy && !model.exportState.isLocked,
                    saveError = settingsError,
                    version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(),
                    onTheme = { value -> scope.launch {
                        try { preferences.setTheme(value); settingsError = false }
                        catch (_: IOException) { settingsError = true }
                    } },
                    onLanguage = { tag ->
                        AppLanguages.select(this@MainActivity, tag)
                    },
                    onLink = { url -> openLink(url) },
                    onNativeCheck = { settingsOpen = false; nativeCheckOpen = true },
                    onDismiss = { settingsOpen = false },
                )
                if (nativeCheckOpen) NativeCheckDialog(
                    running = model.nativeCheckRunning, stopping = model.nativeCheckStopping,
                    canStart = !model.isBusy && !model.exportState.isLocked && !model.shareBusy && model.pendingImportCount == 0,
                    samples = model.nativeCheckSamples, report = model.nativeCheckReport,
                    shareError = model.nativeCheckShareError,
                    onStart = { model.startNativeCheck(applicationContext) },
                    onCancel = model::cancelNativeCheck,
                    onShare = {
                        val text = model.nativeCheckReport?.toJson()
                        if (text != null) try {
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }, getString(R.string.native_check_share)))
                        } catch (_: android.content.ActivityNotFoundException) { model.nativeCheckShareUnavailable() }
                        catch (_: SecurityException) { model.nativeCheckShareUnavailable() }
                    },
                    onDismiss = {
                        if (model.nativeCheckRunning) model.cancelNativeCheck() else nativeCheckOpen = false
                    },
                )
                Surface(Modifier.fillMaxSize()) {
                    ReviewScreen(
                        draft = model.draft,
                        selectedId = model.selectedId,
                        editorSessionKey = model.editorSessionKey,
                        editSelectedOnly = model.editSelectedOnly,
                        onEditSelectedOnly = model::changeEditScope,
                        preview = model.preview,
                        thumbnails = model.thumbnails,
                        busy = model.isBusy,
                        invalidImports = model.invalidImports,
                        importProgress = model.importProgress,
                        onStopImport = model::stopImport,
                        importReport = model.latestImport,
                        onClearImportFeedback = model::clearImportFeedback,
                        previewUnavailable = model.previewUnavailable,
                        api = Build.VERSION.SDK_INT,
                        exportState = model.exportState,
                        sessionRestored = model.sessionRestored,
                        sessionExpired = model.sessionExpired,
                        sessionSaveError = model.sessionSaveError,
                        sessionRestoreBlocked = model.sessionRestoreBlocked,
                        onRetrySessionSave = { model.checkpointDraft() },
                        onDiscardUnavailableSession = { model.discardUnavailableSession() },
                        onExport = { beginExport() },
                        onCancelExport = { model.cancelExport() },
                        onRetryExport = { beginRetryExport() },
                        onRestoreDraft = { model.restorePreviousDraft() },
                        onReviewedPrevious = { model.acknowledgePreviousExport(applicationContext) },
                        finishSessionOnReview = model.canFinishSession,
                        onRefreshOutputs = { model.refreshOutputs(applicationContext) },
                        onOpenOutput = { id -> model.openOutput(applicationContext, id) { ready ->
                            if (ready != null && !isDestroyed) {
                                try { startActivity(ready) }
                                catch (_: android.content.ActivityNotFoundException) { model.outputViewerUnavailable() }
                                catch (_: SecurityException) { model.outputViewerUnavailable() }
                            }
                        } },
                        onConfirmOutput = { model.confirmOutput(applicationContext, it) },
                        onRemovePendingOutput = { model.removePendingOutput(applicationContext, it) },
                        selectedShareIds = model.selectedShareIds,
                        shareBusy = model.shareBusy,
                        shareError = model.shareError,
                        onToggleShare = { model.toggleShareItem(it) },
                        onSelectAllShare = { model.selectAllShareItems() },
                        onClearShareSelection = { model.clearShareSelection() },
                        onShare = { beginShare() },
                        importLimitExceeded = model.importLimitExceeded,
                        pendingImports = model.pendingImportCount,
                        onSettings = { settingsOpen = true },
                        onChoose = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onSelect = selectImage@{ id ->
                            if (model.exportState.isLocked) return@selectImage
                            model.selectedId = id
                            model.checkpointDraft()
                            model.selectThumbnail(id)
                            scheduleThumbnails(listOf(id))
                        },
                        onDestination = { value -> updateDraft(model.draft.setDestination(value)) },
                        onFormat = format@{ value, onlySelected ->
                            val item = model.draft.images.firstOrNull { it.id == model.selectedId }
                            if (onlySelected && item == null) return@format
                            updateDraft(if (onlySelected && item != null) {
                                model.draft.editItem(item.id, item.overrides.copy(format = ItemValue.Custom(value)))
                            } else model.draft.setBatch(model.draft.batch.copy(format = value)))
                        },
                        onBackground = background@{ value, onlySelected ->
                            val item = model.draft.images.firstOrNull { it.id == model.selectedId }
                            if (onlySelected && item == null) return@background
                            updateDraft(if (onlySelected && item != null) {
                                model.draft.editItem(item.id, item.overrides.copy(background = ItemValue.Custom(value)))
                            } else model.draft.setBatch(model.draft.batch.copy(background = value)))
                        },
                        onSize = size@{ value, onlySelected ->
                            val item = model.draft.images.firstOrNull { it.id == model.selectedId }
                            if (onlySelected && item == null) return@size
                            updateDraft(if (onlySelected && item != null) {
                                model.draft.editItem(item.id, item.overrides.copy(size = ItemValue.Custom(value)))
                            } else model.draft.setBatch(model.draft.batch.copy(size = value)))
                        },
                        onMetadata = metadata@{ value, onlySelected ->
                            val item = model.draft.images.firstOrNull { it.id == model.selectedId }
                            if (onlySelected && item == null) return@metadata
                            updateDraft(if (onlySelected && item != null) {
                                model.draft.editItem(item.id, item.overrides.copy(metadata = ItemValue.Custom(value)))
                            } else model.draft.setBatch(model.draft.batch.copy(metadata = value)))
                        },
                        onApplyBatchToAll = { expected ->
                            if (!model.isBusy && model.pendingImportCount == 0 && model.draft == expected) {
                                updateDraft(expected.applyBatchSettingsToAll(EditableField.entries.toSet()).draft)
                            }
                        },
                        onMoveSelected = { delta ->
                            val ids = model.draft.images.map { it.id }.toMutableList()
                            val index = ids.indexOf(model.selectedId)
                            if (index >= 0 && index + delta in ids.indices) {
                                val id = ids.removeAt(index)
                                ids.add(index + delta, id)
                                updateDraft(model.draft.reorder(ids))
                            }
                        },
                        onFit = fit@{ value, onlySelected ->
                            val item = model.draft.images.firstOrNull { it.id == model.selectedId }
                            if (onlySelected && item == null) return@fit
                            updateDraft(if (onlySelected && item != null) {
                                model.draft.editItem(item.id, item.overrides.copy(fit = ItemValue.Custom(value)))
                            } else model.draft.setBatch(model.draft.batch.copy(fit = value)))
                        },
                        onCrop = { updateCrop(it) },
                        onResetSelected = {
                            model.selectedId?.let { updateDraft(model.draft.resetItem(it)) }
                        },
                        onUndo = { updateDraft(model.draft.undo()) },
                        onRemove = { removeSelected() },
                        onDiscard = { expected -> discardDraft(expected) },
                    )
                }
            }
        }
    }

    private fun beginExport() {
        if (!model.prepareExport()) return
        chooseExportDestination()
    }

    private fun beginRetryExport() {
        if (!model.prepareRetryExport()) return
        chooseExportDestination()
    }

    private fun chooseExportDestination() {
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
        if (model.isBusy || model.shareBusy || model.pendingImportCount != 0 ||
            model.exportState.isLocked || model.sessionRestoreBlocked || value == model.draft) return
        val released = model.draft.workingSourceIds - value.workingSourceIds
        model.draft = value
        model.onDraftEdited()
        if (model.selectedId !in value.images.map { it.id }) model.selectedId = value.images.firstOrNull()?.id
        if (released.isNotEmpty()) model.releasePrivateSources(applicationContext, released)
    }

    private fun updateCrop(focus: CropFocus) {
        if (model.isBusy || model.exportState.isLocked) return
        val item = model.draft.images.firstOrNull { it.id == model.selectedId } ?: return
        val settings = item.overrides.resolve(model.draft.batch)
        val ratio = DestinationCatalog.resolve(model.draft.destination).cropRatio(item.orientedSize, settings.size)
        try {
            val geometry = CropPlanner.plan(item.orientedSize, ratio, focus,
                exactOutput = (settings.size as? OutputSizeChoice.Exact)?.canvas)
            val clamped = CropFocus(geometry.normalizedCenterX, geometry.normalizedCenterY, focus.zoom)
            updateDraft(model.draft.editItem(item.id, item.overrides.copy(crop = ItemValue.Custom(clamped))))
        } catch (_: IllegalArgumentException) {
            // A too-small crop is not silently enlarged or substituted; keep the last valid transform.
        }
    }

    private fun addImages(uris: List<Uri>) { model.enqueueImports(uris) }

    private fun receiveImages(value: Intent) {
        val selection = IncomingImages.parse(value)
        model.invalidImports += selection.rejected
        if (selection.overLimit) model.importLimitExceeded = true
        else if (selection.uris.isNotEmpty()) addImages(selection.uris)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveImages(intent)
    }

    private fun openLink(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(this, R.string.link_unavailable, android.widget.Toast.LENGTH_SHORT).show()
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
        worker.cancelPending()
        val id = model.selectedId
        val image = model.draft.images.firstOrNull { it.id == id }
        val plan = model.draft.plans(Build.VERSION.SDK_INT).firstOrNull { it.itemId == id }
        model.preview = null
        model.previewUnavailable = false
        if (image == null || plan == null || plan.geometry == null) return
        worker.execute {
            if (token != generation) return@execute
            var rendered: android.graphics.Bitmap? = null
            try {
                val source = sources.decodeSample(image.id)
                try {
                    if (token != generation) return@execute
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
            if (token != generation) {
                candidate?.recycle()
                return@execute
            }
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
        if (model.isBusy || model.shareBusy || model.sessionRestoreBlocked ||
            model.pendingImportCount != 0 || model.exportState.isLocked) return
        val id = model.selectedId ?: return
        updateDraft(model.draft.removeItem(id))
        model.removeThumbnail(id)
    }

    private fun discardDraft(expected: DraftSession) {
        if (model.exportState.isLocked || model.isBusy || model.shareBusy || model.sessionRestoreBlocked ||
            model.pendingImportCount > 0 || model.draft != expected) return
        val ids = model.draft.workingSourceIds
        model.clearPendingImports()
        model.draft = DraftSession()
        model.onDraftEdited()
        model.selectedId = null
        model.preview = null
        model.clearThumbnails()
        model.clearImportFeedback()
        ++generation
        worker.cancelPending()
        model.releasePrivateSources(applicationContext, ids.toSet())
    }

    override fun onStart() {
        if (::model.isInitialized) model.foregroundChanged(true)
        super.onStart()
    }

    override fun onStop() {
        if (::model.isInitialized && !isChangingConfigurations) model.foregroundChanged(false)
        super.onStop()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::model.isInitialized && level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            model.clearThumbnails()
        }
    }

    override fun onDestroy() {
        generation++
        worker.close()
        thumbnailWorker.shutdownNow()
        super.onDestroy()
    }
}


