package org.cajapersonal.fixmyshot.review

import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.cajapersonal.fixmyshot.core.ExportPreparation
import org.cajapersonal.fixmyshot.export.BatchItemPhase
import org.cajapersonal.fixmyshot.export.BatchCheckpoint
import org.cajapersonal.fixmyshot.export.BatchRetryPolicy
import org.cajapersonal.fixmyshot.export.ExportDraftJournal
import org.cajapersonal.fixmyshot.export.BatchOutputCoordinator
import org.cajapersonal.fixmyshot.export.BatchRunResult
import org.cajapersonal.fixmyshot.export.BatchRecoveryScanner
import org.cajapersonal.fixmyshot.export.BatchRecoveryReconciliation
import org.cajapersonal.fixmyshot.export.OutputRecoveryVerifier
import org.cajapersonal.fixmyshot.export.OutputRecoveryEvidence
import org.cajapersonal.fixmyshot.export.OutputJournal
import org.cajapersonal.fixmyshot.export.OutputStage
import org.cajapersonal.fixmyshot.export.ShareImage
import org.cajapersonal.fixmyshot.export.ExportShareIntents
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
    var importLimitExceeded by mutableStateOf(false)
    var pendingImportCount by mutableStateOf(0)
        private set
    private val importWorker = Executors.newSingleThreadExecutor()
    private val pendingImports = linkedMapOf<String, Uri>()
    private var importGeneration = 0L
    private var activeImports: Set<String> = emptySet()

    /** Hold temporary grants in this process and stage as soon as the current operation permits. */
    fun enqueueImports(uris: List<Uri>) {
        val known = draft.images.mapTo(mutableSetOf()) { it.sourceToken }
        val offered = uris.distinctBy(Uri::toString).filter {
            it.toString() !in known && it.toString() !in pendingImports && it.toString() !in activeImports
        }
        if (draft.images.size + pendingImports.size + activeImports.size + offered.size > 100) {
            importLimitExceeded = true
            return
        }
        importLimitExceeded = false
        offered.forEach { pendingImports[it.toString()] = it }
        pendingImportCount = pendingImports.size
    }

    /** ViewModel ownership keeps imports alive through rotation and locale recreation. */
    fun drainImports(context: Context) {
        if (isBusy || exportState.isLocked || pendingImports.isEmpty()) return
        val wanted = pendingImports.values.toList()
        pendingImports.clear()
        pendingImportCount = 0
        activeImports = wanted.mapTo(mutableSetOf(), Uri::toString)
        isBusy = true
        val token = importGeneration
        val sources = ReviewSources(context.applicationContext)
        importWorker.execute {
            val staged = wanted.map { sources.stage(it) }
            main.post {
                activeImports = emptySet()
                val added = staged.filterIsInstance<ReviewSources.ImportResult.Added>().map { it.image }
                if (token != importGeneration) {
                    added.forEach { sources.discard(it.id) }
                    isBusy = false
                    return@post
                }
                val known = draft.images.mapTo(mutableSetOf()) { it.sourceToken }
                val additions = added.filter { known.add(it.sourceToken) }
                val discarded = added.filter { it !in additions }
                if (discarded.isNotEmpty()) importWorker.execute {
                    discarded.forEach { sources.discard(it.id) }
                }
                invalidImports += staged.size - additions.size
                draft = draft.addImages(additions)
                onDraftEdited()
                if (selectedId == null) selectedId = draft.images.firstOrNull()?.id
                isBusy = false
            }
        }
    }

    fun clearPendingImports() {
        ++importGeneration
        pendingImports.clear()
        pendingImportCount = 0
        importLimitExceeded = false
    }

    var exportState by mutableStateOf(ReviewExportState(ExportUiPhase.CHECKING_HISTORY))
        private set

    private val exportWorker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var plannedDraft: DraftSession? = null
    private var retryDraft: DraftSession? = null
    private var plannedRetryToken: String? = null
    private var plannedRetryIds: Set<String> = emptySet()
    private var beforeDestination: ReviewExportState? = null
    private var lastAttemptDraft: DraftSession? = null
    private var cancelJob: AtomicBoolean? = null
    private var activeToken: String? = null
    private var checkedPreviousJobs = false
    private var inspectingPreviousJobs = false
    private var reviewToken: String? = null

    var selectedShareIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var shareBusy by mutableStateOf(false)
        private set
    var shareError by mutableStateOf(false)
        private set

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
            val verifier = OutputRecoveryVerifier(context.contentResolver)
            found.copy(snapshot = BatchRecoveryReconciliation.inspect(
                File(File(context.filesDir, "export-jobs"), token), snapshot, verifier::verify))
        } else found
    }

    private data class RetryDetails(val draft: DraftSession?, val ids: Set<String>)

    /** Private bytes are read on the existing worker, never during composition. */
    private fun retryDetails(context: Context, token: String?, snapshot: BatchCheckpoint?): RetryDetails {
        if (token == null || snapshot == null) return RetryDetails(null, emptySet())
        return try {
            val folder = File(context.filesDir, "export-jobs/$token")
            val frozen = ExportDraftJournal(File(folder, "draft.json")).read()?.takeIf { candidate ->
                candidate.revision == snapshot.sessionRevision &&
                    candidate.images.map { it.id to it.contentSha256 } ==
                        snapshot.items.map { it.itemId to it.sourceSha256 }
            }
            RetryDetails(frozen, if (frozen == null) emptySet() else BatchRetryPolicy.eligible(folder, snapshot))
        } catch (_: Exception) { RetryDetails(null, emptySet()) }
    }

    private fun showHistory(item: RecoveryItem?, details: RetryDetails) {
        selectedShareIds = emptySet()
        shareError = false
        reviewToken = item?.token
        retryDraft = details.draft
        exportState = if (item == null) ReviewExportState() else ReviewExportState(
            phase = ExportUiPhase.NEEDS_REVIEW,
            items = item.snapshot?.items.orEmpty(),
            previousJobs = item.pendingCount,
            canAcknowledge = item.token != null,
            retryableIds = details.ids,
            canRestoreDraft = details.draft != null && draft.images.isEmpty(),
            retryReady = details.draft?.let { ExportDraftJournal.same(draft, it) } == true && details.ids.isNotEmpty(),
        )
    }

    /** Deliberate restoration never replaces a non-empty newer draft or writes public output. */
    fun restorePreviousDraft() {
        val frozen = retryDraft ?: return
        if (!exportState.canRestoreDraft || draft.images.isNotEmpty() || isBusy || shareBusy) return
        draft = frozen
        lastAttemptDraft = frozen
        selectedId = frozen.images.firstOrNull()?.id
        preview = null
        clearThumbnails()
        selectedShareIds = emptySet()
        exportState = exportState.copy(canRestoreDraft = false,
            retryReady = exportState.retryableIds.isNotEmpty())
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
            val details = retryDetails(app, finding?.token, finding?.snapshot)
            main.post { inspectedHistoryResult(finding, details) }
        }
    }

    private fun inspectedHistoryResult(finding: RecoveryItem?, details: RetryDetails) {
        inspectingPreviousJobs = false
        checkedPreviousJobs = true
        if (activeToken == null) showHistory(finding, details)
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
            val details = retryDetails(app, next?.token, next?.snapshot)
            main.post {
                if (activeToken == null && reviewToken == token) {
                    if (saved || next?.token == token) showHistory(next, details) else {
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
            reviewToken != null || isBusy || pendingImportCount > 0 || draft.images.isEmpty() ||
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
        plannedRetryToken = null
        plannedRetryIds = emptySet()
        beforeDestination = null
        exportState = ReviewExportState(ExportUiPhase.CHOOSING_FOLDER)
        return true
    }

    fun prepareRetryExport(): Boolean {
        val frozen = retryDraft ?: return false
        val token = reviewToken ?: return false
        if (!exportState.retryReady || exportState.retryableIds.isEmpty() || isBusy || shareBusy ||
            activeToken != null || !ExportDraftJournal.same(draft, frozen) || exportState.phase !in setOf(
                ExportUiPhase.COMPLETED, ExportUiPhase.CANCELLED, ExportUiPhase.NEEDS_REVIEW, ExportUiPhase.ERROR)) return false
        plannedDraft = frozen
        plannedRetryToken = token
        plannedRetryIds = exportState.retryableIds.toSet()
        beforeDestination = exportState
        exportState = exportState.copy(phase = ExportUiPhase.CHOOSING_FOLDER, canAcknowledge = false)
        return true
    }

    fun destinationCancelled() {
        if (exportState.phase != ExportUiPhase.CHOOSING_FOLDER) return
        plannedDraft = null
        plannedRetryToken = null
        plannedRetryIds = emptySet()
        exportState = beforeDestination ?: ReviewExportState()
        beforeDestination = null
    }

    fun destinationUnavailable() {
        if (exportState.phase != ExportUiPhase.CHOOSING_FOLDER) return
        plannedDraft = null
        plannedRetryToken = null
        plannedRetryIds = emptySet()
        exportState = beforeDestination?.copy(phase = ExportUiPhase.ERROR) ?: ReviewExportState(ExportUiPhase.ERROR)
        beforeDestination = null
    }

    /** Called only from a real, explicit export click after destination authorization. */
    fun startPreparedExport(context: Context, port: OwnedOutputPort) {
        val frozen = plannedDraft ?: return
        if (exportState.phase != ExportUiPhase.CHOOSING_FOLDER) return
        val retryToken = plannedRetryToken
        val retryIds = plannedRetryIds
        val priorResults = beforeDestination
        plannedDraft = null
        plannedRetryToken = null
        plannedRetryIds = emptySet()
        beforeDestination = null
        selectedShareIds = emptySet()
        shareError = false
        lastAttemptDraft = frozen
        val token = retryToken ?: BatchOutputCoordinator.newToken()
        activeToken = token
        val cancelled = AtomicBoolean(false)
        cancelJob = cancelled
        exportState = ReviewExportState(ExportUiPhase.RUNNING)
        val app = context.applicationContext
        exportWorker.execute {
            val outcome = try {
                val coordinator = BatchOutputCoordinator.forPort(app, port)
                val progress: (BatchCheckpoint) -> Unit = { checkpoint ->
                    main.post {
                        if (activeToken == token) {
                            exportState = ReviewExportState.fromCheckpoint(
                                ExportUiPhase.RUNNING, checkpoint).copy(
                                cancelRequested = cancelled.get())
                        }
                    }
                }
                if (retryToken == null) coordinator.start(frozen, token, cancelled, progress)
                else coordinator.retry(token, retryIds, cancelled, progress)
            } catch (_: Exception) {
                null
            }
            val snapshot = when (outcome) {
                is BatchRunResult.Finished -> outcome.snapshot
                is BatchRunResult.Existing -> outcome.snapshot
                is BatchRunResult.NeedsReview -> outcome.snapshot
                else -> null
            }
            val details = retryDetails(app, token, snapshot)
            main.post {
                if (activeToken != token) return@post
                cancelJob = null
                activeToken = null
                reviewToken = if (outcome is BatchRunResult.Blocked && retryToken == null) null else token
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
                    is BatchRunResult.Blocked -> priorResults?.copy(
                        phase = ExportUiPhase.BLOCKED, blocked = outcome.items, retryReady = false)
                        ?: ReviewExportState(ExportUiPhase.BLOCKED, blocked = outcome.items)
                    null -> priorResults?.copy(phase = ExportUiPhase.NEEDS_REVIEW, retryReady = false)
                        ?: ReviewExportState(ExportUiPhase.NEEDS_REVIEW,
                            canAcknowledge = true, previousJobs = 1)
                }
                retryDraft = details.draft
                exportState = exportState.copy(retryableIds = details.ids,
                    canRestoreDraft = details.draft != null && draft.images.isEmpty(),
                    retryReady = details.draft?.let { ExportDraftJournal.same(draft, it) } == true && details.ids.isNotEmpty())
            }
        }
    }

    /** Explicit selection is restricted to already published items in the displayed result. */
    fun toggleShareItem(id: String) {
        if (shareBusy || exportState.phase !in setOf(
                ExportUiPhase.COMPLETED, ExportUiPhase.CANCELLED, ExportUiPhase.NEEDS_REVIEW)) return
        if (exportState.items.none { it.itemId == id &&
                it.phase == BatchItemPhase.PUBLISHED && it.uri != null }) return
        selectedShareIds = if (id in selectedShareIds) selectedShareIds - id
            else selectedShareIds + id
        shareError = false
    }

    /**
     * The chooser is prepared only after re-reading each committed item journal and verifying
     * its provider bytes. Uncertain and failed items are never shared or silently retried.
     */
    fun shareSelected(context: Context, onReady: (Intent?) -> Unit) {
        if (shareBusy || exportState.phase !in setOf(
                ExportUiPhase.COMPLETED, ExportUiPhase.CANCELLED, ExportUiPhase.NEEDS_REVIEW)) return
        val token = reviewToken ?: return
        val selected = selectedShareIds.toSet()
        val items = exportState.items.toList()
        if (selected.isEmpty() || items.count { it.itemId in selected &&
                it.phase == BatchItemPhase.PUBLISHED && it.uri != null } != selected.size) {
            shareError = true
            return
        }
        val app = context.applicationContext
        shareBusy = true
        shareError = false
        exportWorker.execute {
            val prepared = try {
                val verifier = OutputRecoveryVerifier(app.contentResolver)
                val outputs = mutableListOf<ShareImage>()
                var safe = true
                for ((index, item) in items.withIndex()) {
                    if (item.itemId !in selected) continue
                    val record = OutputJournal(File(app.filesDir,
                        "export-jobs/$token/item-$index.json")).read()
                    val outputUri = record?.uri
                    if (record == null || outputUri == null || record.itemId != item.itemId ||
                        record.stage != OutputStage.PUBLISHED || outputUri != item.uri ||
                        verifier.verify(record) != OutputRecoveryEvidence.MATCHED) {
                        safe = false
                        break
                    }
                    outputs += ShareImage(Uri.parse(outputUri), record.mime)
                }
                if (safe && outputs.size == selected.size) {
                    ExportShareIntents.build(app.contentResolver, outputs)
                } else null
            } catch (_: Exception) { null }
            main.post {
                shareBusy = false
                if (reviewToken == token && selectedShareIds == selected) {
                    if (prepared == null) shareError = true
                    onReady(prepared)
                }
            }
        }
    }

    fun shareChooserUnavailable() { shareError = true }

    fun cancelExport() {
        if (exportState.phase != ExportUiPhase.RUNNING) return
        cancelJob?.set(true)
        exportState = exportState.copy(cancelRequested = true)
    }

    fun onDraftEdited() {
        selectedShareIds = emptySet()
        shareError = false
        if (exportState.isLocked || reviewToken != null ||
            exportState.phase == ExportUiPhase.NEEDS_REVIEW) return
        exportState = ReviewExportState()
    }

    override fun onCleared() {
        cancelJob?.set(true)
        ++importGeneration
        exportWorker.shutdown()
        importWorker.shutdown()
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


