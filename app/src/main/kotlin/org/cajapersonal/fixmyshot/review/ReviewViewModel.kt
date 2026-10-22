package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.content.Context
import java.io.File
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.cajapersonal.fixmyshot.core.ExportPreparation
import org.cajapersonal.fixmyshot.export.BatchItemPhase
import org.cajapersonal.fixmyshot.export.BatchOutputCoordinator
import org.cajapersonal.fixmyshot.export.BatchRunResult
import org.cajapersonal.fixmyshot.export.BatchRecoveryScanner
import org.cajapersonal.fixmyshot.export.BatchRecoveryReconciliation
import org.cajapersonal.fixmyshot.export.RecoveryItem
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

    var exportState by mutableStateOf(ReviewExportState(ExportUiPhase.CHECKING_HISTORY))
        private set

    private val exportWorker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var plannedDraft: DraftSession? = null
    private var lastAttemptDraft: DraftSession? = null
    private var cancelJob: AtomicBoolean? = null
    private var activeToken: String? = null
    private var checkedPreviousJobs = false
    private var inspectingPreviousJobs = false
    private var reviewToken: String? = null

    private fun recoveryScanner(context: Context): BatchRecoveryScanner {
        val prefs = context.getSharedPreferences("export-reviewed", Context.MODE_PRIVATE)
        return BatchRecoveryScanner(File(context.filesDir, "export-jobs")) { id ->
            prefs.getBoolean(id, false)
        }
    }

    private fun scanHistory(context: Context): RecoveryItem? {
        val found = recoveryScanner(context).inspect() ?: return null
        val token = found.token
        val snapshot = found.snapshot
        return if (token != null && snapshot != null) {
            found.copy(snapshot = BatchRecoveryReconciliation.inspect(
                File(File(context.filesDir, "export-jobs"), token), snapshot))
        } else found
    }

    private fun showHistory(item: RecoveryItem?) {
        reviewToken = item?.token
        exportState = if (item == null) ReviewExportState() else ReviewExportState(
            phase = ExportUiPhase.NEEDS_REVIEW,
            items = item.snapshot?.items.orEmpty(),
            previousJobs = item.pendingCount,
            canAcknowledge = item.token != null,
        )
    }

    /** A previous process may have published output even if its final callback was lost. */
    fun checkPastExports(context: Context) {
        if (checkedPreviousJobs || inspectingPreviousJobs || activeToken != null) return
        inspectingPreviousJobs = true
        exportState = ReviewExportState(ExportUiPhase.CHECKING_HISTORY)
        val app = context.applicationContext
        exportWorker.execute {
            val finding = try { scanHistory(app) }
                catch (_: Exception) { RecoveryItem(null, null, 1) }
            main.post {
                inspectedHistoryResult(finding)
            }
        }
    }

    private fun inspectedHistoryResult(finding: RecoveryItem?) {
        inspectingPreviousJobs = false
        checkedPreviousJobs = true
        if (activeToken == null) showHistory(finding)
    }

    /** Only an explicit UI action records that past outputs were reviewed. */
    fun acknowledgePreviousExport(context: Context) {
        val token = reviewToken ?: return
        if (!exportState.canAcknowledge || activeToken != null ||
            exportState.phase == ExportUiPhase.CHECKING_HISTORY) return
        exportState = exportState.copy(phase = ExportUiPhase.CHECKING_HISTORY,
            canAcknowledge = false)
        val app = context.applicationContext
        exportWorker.execute {
            var saved = false
            val next = try {
                val scanner = recoveryScanner(app)
                if (scanner.inspect()?.token == token) {
                    saved = app.getSharedPreferences("export-reviewed", Context.MODE_PRIVATE)
                        .edit().putBoolean(token, true).commit()
                }
                scanHistory(app)
            } catch (_: Exception) { RecoveryItem(null, null, 1) }
            main.post {
                if (activeToken == null && reviewToken == token) {
                    if (saved || next?.token == token) showHistory(next) else {
                        exportState = ReviewExportState(ExportUiPhase.NEEDS_REVIEW,
                            previousJobs = 1)
                        reviewToken = null
                    }
                }
            }
        }
    }

    /** Verify the whole batch before requesting a destination or creating output. */
    fun prepareExport(): Boolean {
        if (!checkedPreviousJobs || exportState.phase != ExportUiPhase.IDLE ||
            reviewToken != null || isBusy || draft.images.isEmpty() ||
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
                reviewToken = if (outcome is BatchRunResult.Blocked) null else token
                exportState = when (outcome) {
                    is BatchRunResult.Finished -> ReviewExportState.fromCheckpoint(
                        if (outcome.snapshot.items.any { it.phase == BatchItemPhase.CANCELLED })
                            ExportUiPhase.CANCELLED else ExportUiPhase.COMPLETED,
                        outcome.snapshot).copy(canAcknowledge = true, previousJobs = 1)
                    is BatchRunResult.NeedsReview ->
                        outcome.snapshot?.let {
                            ReviewExportState.fromCheckpoint(ExportUiPhase.NEEDS_REVIEW, it)
                        }?.copy(canAcknowledge = true, previousJobs = 1)
                            ?: ReviewExportState(ExportUiPhase.NEEDS_REVIEW,
                                canAcknowledge = true, previousJobs = 1)
                    is BatchRunResult.Existing ->
                        ReviewExportState.fromCheckpoint(ExportUiPhase.NEEDS_REVIEW, outcome.snapshot)
                            .copy(canAcknowledge = true, previousJobs = 1)
                    is BatchRunResult.Blocked ->
                        ReviewExportState(ExportUiPhase.BLOCKED, blocked = outcome.items)
                    null -> ReviewExportState(ExportUiPhase.NEEDS_REVIEW,
                        canAcknowledge = true, previousJobs = 1)
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
        if (exportState.isLocked || reviewToken != null ||
            exportState.phase == ExportUiPhase.NEEDS_REVIEW) return
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
