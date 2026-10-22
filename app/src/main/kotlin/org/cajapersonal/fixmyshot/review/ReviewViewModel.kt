package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.cajapersonal.fixmyshot.core.ExportPreparation
import org.cajapersonal.fixmyshot.export.BatchItemPhase
import org.cajapersonal.fixmyshot.export.BatchOutputCoordinator
import org.cajapersonal.fixmyshot.export.BatchRunResult
import org.cajapersonal.fixmyshot.export.OwnedOutputPort
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
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

    var exportState by mutableStateOf(ReviewExportState())
        private set

    private val exportWorker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var plannedDraft: DraftSession? = null
    private var lastAttemptDraft: DraftSession? = null
    private var cancelJob: AtomicBoolean? = null
    private var activeToken: String? = null

    /** Verify the whole batch before requesting a destination or creating output. */
    fun prepareExport(): Boolean {
        if (exportState.isLocked || isBusy || draft.images.isEmpty() ||
            lastAttemptDraft == draft) return false
        val preparation = try {
            ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, Runtime.getRuntime().maxMemory())
        } catch (_: Exception) {
            exportState = ReviewExportState(ExportUiPhase.ERROR)
            return false
        }
        if (preparation.blockedCount > 0 || preparation.candidates.size != draft.images.size) {
            exportState = ReviewExportState(ExportUiPhase.BLOCKED,
                blocked = maxOf(1, preparation.blockedCount))
            return false
        }
        plannedDraft = draft
        exportState = ReviewExportState(ExportUiPhase.CHOOSING_FOLDER)
        return true
    }

    fun destinationCancelled() {
        if (exportState.phase != ExportUiPhase.CHOOSING_FOLDER) return
        plannedDraft = null
        exportState = ReviewExportState()
    }

    fun destinationUnavailable() {
        if (exportState.phase != ExportUiPhase.CHOOSING_FOLDER) return
        plannedDraft = null
        exportState = ReviewExportState(ExportUiPhase.ERROR)
    }

    /** Called only from a real, explicit export click after destination authorization. */
    fun startPreparedExport(context: Context, port: OwnedOutputPort) {
        val frozen = plannedDraft ?: return
        if (exportState.phase != ExportUiPhase.CHOOSING_FOLDER) return
        plannedDraft = null
        lastAttemptDraft = frozen
        val token = BatchOutputCoordinator.newToken()
        activeToken = token
        val cancelled = AtomicBoolean(false)
        cancelJob = cancelled
        exportState = ReviewExportState(ExportUiPhase.RUNNING)
        val app = context.applicationContext
        exportWorker.execute {
            val outcome = try {
                val coordinator = BatchOutputCoordinator.forPort(app, port)
                coordinator.start(frozen, token, cancelled) { checkpoint ->
                    main.post {
                        if (activeToken == token) {
                            exportState = ReviewExportState.fromCheckpoint(
                                ExportUiPhase.RUNNING, checkpoint).copy(
                                cancelRequested = cancelled.get())
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
            main.post {
                if (activeToken != token) return@post
                cancelJob = null
                activeToken = null
                exportState = when (outcome) {
                    is BatchRunResult.Finished -> ReviewExportState.fromCheckpoint(
                        if (outcome.snapshot.items.any { it.phase == BatchItemPhase.CANCELLED })
                            ExportUiPhase.CANCELLED else ExportUiPhase.COMPLETED,
                        outcome.snapshot)
                    is BatchRunResult.NeedsReview ->
                        outcome.snapshot?.let {
                            ReviewExportState.fromCheckpoint(ExportUiPhase.NEEDS_REVIEW, it)
                        } ?: ReviewExportState(ExportUiPhase.NEEDS_REVIEW)
                    is BatchRunResult.Existing ->
                        ReviewExportState.fromCheckpoint(ExportUiPhase.NEEDS_REVIEW, outcome.snapshot)
                    is BatchRunResult.Blocked ->
                        ReviewExportState(ExportUiPhase.BLOCKED, blocked = outcome.items)
                    null -> ReviewExportState(ExportUiPhase.NEEDS_REVIEW)
                }
            }
        }
    }

    fun cancelExport() {
        if (exportState.phase != ExportUiPhase.RUNNING) return
        cancelJob?.set(true)
        exportState = exportState.copy(cancelRequested = true)
    }

    fun onDraftEdited() {
        if (exportState.isLocked) return
        exportState = ReviewExportState()
    }

    override fun onCleared() {
        cancelJob?.set(true)
        exportWorker.shutdown()
        super.onCleared()
    }

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
