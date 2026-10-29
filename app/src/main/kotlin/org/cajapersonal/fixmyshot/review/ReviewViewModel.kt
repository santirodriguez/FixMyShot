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
import org.cajapersonal.fixmyshot.export.BatchExportJournal
import org.cajapersonal.fixmyshot.export.BatchRetryPolicy
import org.cajapersonal.fixmyshot.export.ExportDraftJournal
import org.cajapersonal.fixmyshot.export.ReviewSessionJournal
import org.cajapersonal.fixmyshot.export.ReviewSessionSnapshot
import org.cajapersonal.fixmyshot.export.BatchOutputCoordinator
import org.cajapersonal.fixmyshot.export.BatchRunResult
import org.cajapersonal.fixmyshot.export.BatchRecoveryScanner
import org.cajapersonal.fixmyshot.export.BatchRecoveryReconciliation
import org.cajapersonal.fixmyshot.export.ExportRecoveryActions
import org.cajapersonal.fixmyshot.export.PendingMediaStoreRemoval
import org.cajapersonal.fixmyshot.export.PrivateSessionMaintenance
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
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.diagnostics.NativeCodecDiagnostic
import org.cajapersonal.fixmyshot.diagnostics.NativeCheckReport
import org.cajapersonal.fixmyshot.diagnostics.NativeCheckSample

/** Owns the active draft and explicit, journal-backed export recovery across recreation. */
internal class ReviewViewModel : ViewModel() {
    var draft by mutableStateOf(DraftSession())
    var preferredTemplate: String? = null
    var quickPreferencesReady by mutableStateOf(false)
    var selectedId by mutableStateOf<String?>(null)
    // Retained only with this ViewModel. Applied settings remain owned by the draft journal.
    var editorSessionKey by mutableStateOf(java.util.UUID.randomUUID().toString())
        private set
    var editSelectedOnly by mutableStateOf(false)
        private set

    fun changeEditScope(value: Boolean) {
        if (isBusy || shareBusy || sessionRestoreBlocked || pendingImportCount != 0 || exportState.isLocked) return
        if (value && draft.images.none { it.id == selectedId }) return
        editSelectedOnly = value
    }

    private fun resetEditorContext() {
        editSelectedOnly = false
        editorSessionKey = java.util.UUID.randomUUID().toString()
    }
    // One bounded ORIGINAL sample, not a rendered output. Never used as export input.
    var preview by mutableStateOf<Bitmap?>(null)
    var previewSourceKey: String? = null
    var previewEpoch by mutableStateOf(0L)
    fun trimPreview() { preview = null; previewSourceKey = null }

    var isBusy by mutableStateOf(false)
    var invalidImports by mutableStateOf(0)
    var latestImport by mutableStateOf<ImportReport?>(null)
        private set
    var previewUnavailable by mutableStateOf(false)
    var importLimitExceeded by mutableStateOf(false)
    var pendingImportCount by mutableStateOf(0)
        private set
    private val importWorker = Executors.newSingleThreadExecutor()
    private val pendingImports = linkedMapOf<String, Uri>()
    private var importGeneration = 0L
    private var activeImports: Set<String> = emptySet()
    private var importStop: AtomicBoolean? = null
    private var stoppedQueuedImports = 0
    var importProgress by mutableStateOf<ImportProgress?>(null)
        private set
    private var sessionContext: Context? = null
    private val sessionPending = AtomicReference<ReviewSessionSnapshot?>(null)
    private val sessionWriterQueued = AtomicBoolean(false)
    var sessionRestored by mutableStateOf(false)
        private set
    var sessionExpired by mutableStateOf(false)
        private set
    var sessionSaveError by mutableStateOf(false)
        private set
    var sessionRestoreBlocked by mutableStateOf(false)
        private set

    /** The launcher binds editable-session persistence; isolated export coordinators need not. */
    fun attachSession(context: Context) { sessionContext = context.applicationContext }

    private fun sessionJournal(context: Context) = ReviewSessionJournal(File(context.filesDir, "review-session.json"))

    /** Coalesce rapid gesture/selection edits; serialize checkpoints before source cleanup/export. */
    fun checkpointDraft() {
        val app = sessionContext ?: return
        if (!checkedPreviousJobs || sessionRestoreBlocked) return
        sessionPending.set(ReviewSessionSnapshot(draft,
            selectedId?.takeIf { id -> draft.images.any { it.id == id } } ?: draft.images.firstOrNull()?.id,
            System.currentTimeMillis()))
        if (!sessionWriterQueued.compareAndSet(false, true)) return
        exportWorker.execute {
            while (true) {
                val next = sessionPending.getAndSet(null)
                if (next == null) {
                    sessionWriterQueued.set(false)
                    if (sessionPending.get() != null && sessionWriterQueued.compareAndSet(false, true)) continue
                    break
                }
                val failed = try { sessionJournal(app).write(next); false } catch (_: Exception) { true }
                main.post { sessionSaveError = failed }
            }
        }
    }

    /** Corrupt/missing working bytes require explicit discard; never overwrite their journal on import. */
    fun discardUnavailableSession() {
        val app = sessionContext ?: return
        if (!sessionRestoreBlocked || isBusy || exportState.isLocked) return
        isBusy = true
        val current = draft.workingSourceIds
        exportWorker.execute {
            val cleared = try {
                sessionJournal(app).clear()
                maintenance(app).pruneExpired(current)
                true
            } catch (_: Exception) { false }
            main.post {
                isBusy = false
                sessionRestoreBlocked = !cleared
                sessionSaveError = !cleared
                if (cleared && draft.workingSourceIds.isNotEmpty()) checkpointDraft()
            }
        }
    }

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
        if (isBusy || sessionRestoreBlocked || exportState.isLocked || pendingImports.isEmpty()) return
        val known = draft.images.mapTo(mutableSetOf()) { it.sourceToken }
        val wanted = pendingImports.values.filter { it.toString() !in known }
        pendingImports.clear()
        pendingImportCount = 0
        // An incoming offer may have arrived before the previous session was restored.
        if (draft.images.size + wanted.size > 100) { importLimitExceeded = true; return }
        if (wanted.isEmpty()) return
        activeImports = wanted.mapTo(mutableSetOf(), Uri::toString)
        val stop = AtomicBoolean(false)
        importStop = stop
        stoppedQueuedImports = 0
        importProgress = ImportProgress(0, wanted.size)
        isBusy = true
        val token = importGeneration
        val sources = ReviewSources(context.applicationContext)
        importWorker.execute {
            val staged = mutableListOf<ReviewSources.ImportResult>()
            for (uri in wanted) {
                if (stop.get()) break
                staged += sources.stage(uri)
                val processed = staged.size
                main.post {
                    if (importStop === stop) importProgress = ImportProgress(processed, wanted.size, stop.get())
                }
            }
            main.post {
                activeImports = emptySet()
                importStop = null
                importProgress = null
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
                val report = ImportReport.from(staged, additions.mapTo(mutableSetOf()) { it.id },
                    offered = wanted.size + stoppedQueuedImports)
                invalidImports += report.problems.size
                latestImport = report
                // Recommend only for an untouched new session, never a restored/custom draft.
                val base = if (draft.revision == 0L && draft.images.isEmpty() && additions.isNotEmpty() &&
                    draft.batch == org.cajapersonal.fixmyshot.core.EditSettings())
                    draft.copy(destination = org.cajapersonal.fixmyshot.core.QuickTemplates.initial(additions, preferredTemplate))
                else draft
                val updated = base.addImages(additions)
                val released = draft.workingSourceIds - updated.workingSourceIds
                draft = updated
                if (released.isNotEmpty()) releasePrivateSources(context.applicationContext, released)
                onDraftEdited()
                if (selectedId == null) selectedId = draft.images.firstOrNull()?.id
                isBusy = false
            }
        }
    }

    /** Finish the in-flight provider operation; preserve completed imports and skip untouched offers. */
    fun stopImport() {
        val stop = importStop ?: return
        if (stop.getAndSet(true)) return
        stoppedQueuedImports += pendingImports.size
        pendingImports.clear()
        pendingImportCount = 0
        importLimitExceeded = false
        importProgress = importProgress?.copy(stopping = true)
    }

    fun clearPendingImports() {
        ++importGeneration
        importStop?.set(true)
        pendingImports.clear()
        pendingImportCount = 0
        importLimitExceeded = false
    }

    /** Dismiss feedback only; this never discards a draft, pending import or source grant. */
    fun clearImportFeedback() { latestImport = null; invalidImports = 0 }

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
    private val foregroundPause = AtomicBoolean(false)
    private var inForeground = true
    var nativeCheckRunning by mutableStateOf(false)
        private set
    var nativeCheckStopping by mutableStateOf(false)
        private set
    var nativeCheckSamples by mutableStateOf<List<NativeCheckSample>>(emptyList())
        private set
    var nativeCheckReport by mutableStateOf<NativeCheckReport?>(null)
        private set
    var nativeCheckShareError by mutableStateOf(false)
        private set
    private var nativeCheckCancel: AtomicBoolean? = null

    /** Explicit synthetic check, serialized with private export/session work and never auto-started. */
    fun startNativeCheck(context: Context) {
        if (isBusy || exportState.isLocked || shareBusy || pendingImportCount > 0 || !inForeground) return
        val app = context.applicationContext
        val cancel = AtomicBoolean(false)
        nativeCheckCancel = cancel
        nativeCheckRunning = true
        nativeCheckStopping = false
        nativeCheckSamples = emptyList()
        nativeCheckReport = null
        nativeCheckShareError = false
        isBusy = true
        exportWorker.execute {
            val report = try {
                NativeCodecDiagnostic(File(app.cacheDir, "batch-candidates"), Runtime.getRuntime().maxMemory())
                    .run(cancel) { samples -> main.post {
                        if (nativeCheckCancel === cancel) nativeCheckSamples = samples
                    } }.withAppIdentity(app, cancel)
            } catch (_: Exception) { NativeCheckReport(cancelled = cancel.get(), failed = true) }
            main.post {
                if (nativeCheckCancel !== cancel) return@post
                nativeCheckReport = if (cancel.get()) report.copy(cancelled = true) else report
                nativeCheckSamples = report.samples
                nativeCheckCancel = null
                nativeCheckRunning = false
                nativeCheckStopping = false
                isBusy = false
            }
        }
    }

    fun cancelNativeCheck() {
        if (!nativeCheckRunning) return
        nativeCheckCancel?.set(true)
        nativeCheckStopping = true
    }

    fun nativeCheckShareUnavailable() { nativeCheckShareError = true }

    fun foregroundChanged(visible: Boolean) {
        inForeground = visible
        if (!visible) cancelNativeCheck()
        if (!visible && activeToken != null) {
            foregroundPause.set(true)
            exportState = exportState.copy(pauseRequested = true)
        } else if (visible && activeToken == null) foregroundPause.set(false)
    }

    val canFinishSession: Boolean get() = exportState.canAcknowledge && exportState.items.isNotEmpty() &&
        exportState.items.all { it.phase == BatchItemPhase.PUBLISHED } &&
        lastAttemptDraft?.let { ExportDraftJournal.same(draft, it) } == true
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

    private fun maintenance(context: Context): PrivateSessionMaintenance {
        val prefs = context.getSharedPreferences("export-reviewed", Context.MODE_PRIVATE)
        return PrivateSessionMaintenance(context.cacheDir, File(context.filesDir, "export-jobs"),
            forgetAcknowledged = { prefs.edit().remove(it).commit() }) {
            prefs.getBoolean(it, false)
        }
    }

    private fun currentAndSavedSources(context: Context, current: Set<String>): Set<String>? = try {
        current + if (sessionContext != null) sessionJournal(context).read()?.draft?.workingSourceIds.orEmpty()
            else emptySet()
    } catch (_: Exception) { null }

    /** Call after an explicit remove/discard action; another unresolved job keeps its sources. */
    fun releasePrivateSources(context: Context, ids: Set<String>) {
        val app = context.applicationContext
        val current = draft.workingSourceIds
        exportWorker.execute {
            val released = try {
                // A failed newer checkpoint must not strand the last recoverable saved session.
                currentAndSavedSources(app, current)?.let { maintenance(app).release(ids, it) } ?: false
            } catch (_: Exception) { false }
            if (!released) main.post { exportState = exportState.copy(outputError = true) }
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
            val compatible = frozen?.let {
                val current = ExportPreparation.inspect(it, Build.VERSION.SDK_INT, Runtime.getRuntime().maxMemory())
                current.blockedCount == 0 && ExportDraftJournal(File(folder, "draft.json")).matchesPlan(current.candidates)
            } == true
            RetryDetails(frozen, if (!compatible) emptySet() else BatchRetryPolicy.eligible(folder, snapshot))
        } catch (_: Exception) { RetryDetails(null, emptySet()) }
    }

    private fun showHistory(item: RecoveryItem?, details: RetryDetails) {
        selectedShareIds = emptySet()
        shareError = false
        reviewToken = item?.token
        retryDraft = details.draft
        if (details.draft?.let { ExportDraftJournal.same(draft, it) } == true) {
            // An automatically restored editable draft still represents this already-attempted job.
            lastAttemptDraft = details.draft
        }
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
        if (!exportState.canRestoreDraft || draft.images.isNotEmpty() || isBusy || shareBusy || exportState.outputBusy) return
        draft = frozen
        lastAttemptDraft = frozen
        selectedId = frozen.images.firstOrNull()?.id
        preview = null
        clearThumbnails()
        selectedShareIds = emptySet()
        exportState = exportState.copy(canRestoreDraft = false,
            retryReady = exportState.retryableIds.isNotEmpty())
        checkpointDraft()
    }

    /** A previous process may have published output even if its final callback was lost. */
    fun checkPastExports(context: Context) {
        if (checkedPreviousJobs || inspectingPreviousJobs || activeToken != null) return
        inspectingPreviousJobs = true
        exportState = ReviewExportState(ExportUiPhase.CHECKING_HISTORY)
        val app = context.applicationContext
        val current = draft.workingSourceIds
        val beforeRestore = draft
        exportWorker.execute {
            var restored: ReviewSessionSnapshot? = null
            var blocked = false
            var expired = false
            if (sessionContext != null) {
                try {
                    val saved = sessionJournal(app).read()
                    if (saved?.expired(System.currentTimeMillis()) == true) {
                        sessionJournal(app).clear()
                        expired = true
                    } else if (saved != null) {
                        val root = File(app.cacheDir, "review-sources")
                        require(root.canonicalFile == File(app.cacheDir.canonicalFile, "review-sources"))
                        var total = 0L
                        for (id in saved.draft.workingSourceIds) {
                            val source = File(root, "$id.source")
                            require(source.canonicalFile == File(root.canonicalFile, source.name) &&
                                source.isFile && source.length() in 1L..100L * 1024 * 1024)
                            total += source.length()
                            require(total <= 1024L * 1024 * 1024)
                        }
                        restored = saved
                    }
                } catch (_: Exception) { blocked = true }
            }
            if (!blocked) maintenance(app).pruneExpired(current + restored?.draft?.workingSourceIds.orEmpty())
            val finding = try { scanHistory(app) }
                catch (_: Exception) { RecoveryItem(null, null, 1) }
            val details = retryDetails(app, finding?.token, finding?.snapshot)
            val savedSession = restored
            main.post {
                sessionRestoreBlocked = blocked
                sessionExpired = expired
                if (savedSession != null && draft == beforeRestore && draft.workingSourceIds.isEmpty()) {
                    draft = savedSession.draft
                    selectedId = savedSession.selectedId ?: draft.images.firstOrNull()?.id
                    sessionRestored = true
                }
                inspectedHistoryResult(finding, details)
                if (sessionRestored) checkpointDraft()
            }
        }
    }

    private fun inspectedHistoryResult(finding: RecoveryItem?, details: RetryDetails) {
        inspectingPreviousJobs = false
        checkedPreviousJobs = true
        if (activeToken == null) showHistory(finding, details)
    }

    /** Only an explicit UI action records that past outputs were reviewed. */
    fun acknowledgePreviousExport(context: Context) = reviewPreviousExport(context, keepDraft = false)

    /** Explicit next-image action; the picker can open only after successful, guarded session finish. */
    fun startNextImage(context: Context, onReady: () -> Unit) {
        if (exportState.phase != ExportUiPhase.COMPLETED || !canFinishSession || isBusy ||
            sessionRestoreBlocked || pendingImportCount != 0) return
        reviewPreviousExport(context, keepDraft = false, onFinished = onReady)
    }

    /** A completed result can be reviewed without discarding the editable draft or its undo sources. */
    fun continueEditing(context: Context) {
        if (exportState.phase != ExportUiPhase.COMPLETED || !canFinishSession || isBusy || sessionRestoreBlocked) return
        reviewPreviousExport(context, keepDraft = true)
    }

    private fun reviewPreviousExport(context: Context, keepDraft: Boolean, onFinished: (() -> Unit)? = null) {
        val token = reviewToken ?: return
        if (!exportState.canAcknowledge || activeToken != null || shareBusy || exportState.outputBusy ||
            exportState.phase == ExportUiPhase.CHECKING_HISTORY) return
        val finishSession = canFinishSession && !keepDraft
        val finishingDraft = draft
        exportState = exportState.copy(phase = ExportUiPhase.CHECKING_HISTORY,
            canAcknowledge = false)
        val app = context.applicationContext
        val current = draft.workingSourceIds
        exportWorker.execute {
            var saved = false
            var cleanupFailed = false
            var finishedSession = false
            val next = try {
                val scanner = recoveryScanner(app)
                if (scanner.inspect()?.token == token) {
                    saved = app.getSharedPreferences("export-reviewed", Context.MODE_PRIVATE)
                        .edit().putBoolean(token, true).commit()
                    if (saved && finishSession) {
                        try {
                            if (sessionContext != null && !sessionJournal(app).clearMatching(finishingDraft)) {
                                throw java.io.IOException("A different saved session must be preserved")
                            }
                            finishedSession = true
                            val cleaner = maintenance(app)
                            val releasedDraft = cleaner.releaseReviewedDraft(token, emptySet())
                            val releasedUndo = cleaner.release(current, emptySet())
                            cleanupFailed = !releasedDraft || !releasedUndo || !cleaner.retirePublishedJob(token)
                        } catch (_: Exception) { cleanupFailed = true }
                    } else if (saved) {
                        cleanupFailed = currentAndSavedSources(app, current)?.let {
                            !maintenance(app).releaseReviewedDraft(token, it)
                        } ?: true
                        if (keepDraft && !cleanupFailed) cleanupFailed = !maintenance(app).retirePublishedJob(token)
                    }
                }
                scanHistory(app)
            } catch (_: Exception) { RecoveryItem(null, null, 1) }
            val details = retryDetails(app, next?.token, next?.snapshot)
            main.post {
                if (activeToken == null && reviewToken == token) {
                    if (saved || next?.token == token) {
                        if (finishedSession) {
                            draft = DraftSession()
                            resetEditorContext()
                            selectedId = null
                            preview = null
                            clearThumbnails()
                            sessionRestored = false
                            sessionExpired = false
                            sessionSaveError = false
                            sessionRestoreBlocked = false
                            clearImportFeedback()
                            importLimitExceeded = false
                            previewUnavailable = false
                            lastAttemptDraft = null
                        }
                        showHistory(next, details)
                        if (cleanupFailed) exportState = exportState.copy(outputError = true)
                        if (finishedSession && !cleanupFailed && next == null) onFinished?.invoke()
                    } else {
                        exportState = ReviewExportState(ExportUiPhase.NEEDS_REVIEW,
                            previousJobs = 1)
                        reviewToken = null
                    }
                }
            }
        }
    }

    /** Recheck the displayed job; never write provider bytes or silently resume it. */
    fun refreshOutputs(context: Context) = actOnOutput(context, null, false, null)

    fun openOutput(context: Context, id: String, onReady: (Intent?) -> Unit) =
        actOnOutput(context, id, false, onReady)

    /** Invoked only after the confirmation dialog, never from startup or mere verification. */
    fun confirmOutput(context: Context, id: String) = actOnOutput(context, id, true, null)

    /** Separate confirmation only, after a read-only check reported a pending image. */
    fun removePendingOutput(context: Context, id: String) {
        if (exportState.items.none { it.itemId == id && it.phase == BatchItemPhase.NEEDS_REVIEW &&
                it.reason == "OUTPUT_PENDING" }) return
        actOnOutput(context, id, false, null, removePending = true)
    }

    private fun actOnOutput(context: Context, id: String?, confirm: Boolean, onReady: ((Intent?) -> Unit)?,
        removePending: Boolean = false) {
        val token = reviewToken ?: return
        if (!exportState.canAcknowledge || exportState.outputBusy || shareBusy || activeToken != null ||
            exportState.phase == ExportUiPhase.CHECKING_HISTORY) return
        if (id != null && exportState.items.none { it.itemId == id && it.uri != null }) return
        val previous = exportState
        val app = context.applicationContext
        exportState = exportState.copy(outputBusy = true, outputError = false)
        exportWorker.execute {
            var intent: Intent? = null
            var failed = false
            val snapshot = try {
                val jobs = File(app.filesDir, "export-jobs")
                val verifier = OutputRecoveryVerifier(app.contentResolver)
                val actions = ExportRecoveryActions(jobs, verifier::verify)
                if (id != null) {
                    if (removePending) failed = !actions.discardPending(token, id,
                        PendingMediaStoreRemoval(app.contentResolver, app.packageName)::remove)
                    else if (confirm) failed = !actions.confirmPublished(token, id)
                    else { intent = actions.open(token, id); failed = intent == null }
                }
                val folder = File(jobs, token)
                val batch = BatchExportJournal(File(folder, "batch.json")).read()
                if (batch?.token != token) null else BatchRecoveryReconciliation.inspect(
                    folder, batch, OutputRecoveryVerifier(app.contentResolver)::verify)
            } catch (_: Exception) { null }
            val details = retryDetails(app, token, snapshot)
            main.post {
                if (reviewToken != token) return@post
                selectedShareIds = emptySet()
                retryDraft = details.draft
                exportState = previous.copy(items = snapshot?.items ?: previous.items,
                    phase = if (snapshot?.items?.any { it.phase == BatchItemPhase.NEEDS_REVIEW } != false)
                        ExportUiPhase.NEEDS_REVIEW else if (previous.phase == ExportUiPhase.NEEDS_REVIEW)
                        ExportUiPhase.COMPLETED else previous.phase,
                    retryableIds = details.ids, retryReady = details.draft?.let {
                        ExportDraftJournal.same(draft, it) } == true && details.ids.isNotEmpty(),
                    outputBusy = false, outputError = failed || snapshot == null)
                onReady?.invoke(intent)
            }
        }
    }

    fun outputViewerUnavailable() { exportState = exportState.copy(outputError = true) }

    /** A refused preflight/destination is retryable; an attempted or unresolved job is not a new export. */
    val canPrepareExport: Boolean get() = checkedPreviousJobs &&
        exportState.phase in setOf(ExportUiPhase.IDLE, ExportUiPhase.BLOCKED, ExportUiPhase.ERROR) &&
        reviewToken == null && activeToken == null && !exportState.canAcknowledge &&
        !exportState.outputBusy && !isBusy && !shareBusy && pendingImportCount == 0 &&
        draft.images.isNotEmpty() && !sessionRestoreBlocked && lastAttemptDraft != draft

    /** Verify the whole batch before requesting a destination or creating output. */
    fun prepareExport(): Boolean {
        if (!canPrepareExport) return false
        val preparation = try {
            ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, Runtime.getRuntime().maxMemory())
        } catch (_: Exception) {
            exportState = ReviewExportState(ExportUiPhase.ERROR)
            return false
        }
        if (preparation.blockedCount > 0 || preparation.candidates.size != draft.images.size) {
            exportState = ReviewExportState(ExportUiPhase.BLOCKED,
                blocked = maxOf(1, preparation.blockedCount),
                blockedItems = preparation.items.filter { it.blocks.isNotEmpty() })
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
        if (!exportState.retryReady || exportState.retryableIds.isEmpty() || isBusy || shareBusy || exportState.outputBusy ||
            activeToken != null || !ExportDraftJournal.same(draft, frozen) || exportState.phase !in setOf(
                ExportUiPhase.COMPLETED, ExportUiPhase.CANCELLED, ExportUiPhase.PAUSED, ExportUiPhase.NEEDS_REVIEW, ExportUiPhase.ERROR)) return false
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
        foregroundPause.set(!inForeground)
        val cancelled = AtomicBoolean(false)
        cancelJob = cancelled
        exportState = ReviewExportState(ExportUiPhase.RUNNING)
        val app = context.applicationContext
        exportWorker.execute {
            val outcome = try {
                val coordinator = BatchOutputCoordinator.forPort(app, port, foregroundPause)
                val progress: (BatchCheckpoint) -> Unit = { checkpoint ->
                    main.post {
                        if (activeToken == token) {
                            exportState = ReviewExportState.fromCheckpoint(
                                ExportUiPhase.RUNNING, checkpoint).copy(
                                cancelRequested = cancelled.get(), pauseRequested = foregroundPause.get())
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
                        when {
                            outcome.snapshot.items.any { it.phase == BatchItemPhase.CANCELLED && it.reason == "PAUSED" } -> ExportUiPhase.PAUSED
                            outcome.snapshot.items.any { it.phase == BatchItemPhase.CANCELLED } -> ExportUiPhase.CANCELLED
                            else -> ExportUiPhase.COMPLETED
                        },
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
                        phase = ExportUiPhase.BLOCKED, blocked = outcome.items, blockedItems = outcome.details, retryReady = false)
                        ?: ReviewExportState(ExportUiPhase.BLOCKED, blocked = outcome.items, blockedItems = outcome.details)
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
        if (!canChangeShareSelection()) return
        if (exportState.items.none { it.itemId == id &&
                it.phase == BatchItemPhase.PUBLISHED && it.uri != null }) return
        selectedShareIds = if (id in selectedShareIds) selectedShareIds - id
            else selectedShareIds + id
        shareError = false
    }

    fun selectAllShareItems() {
        if (!canChangeShareSelection()) return
        selectedShareIds = exportState.items.filter {
            it.phase == BatchItemPhase.PUBLISHED && it.uri != null
        }.mapTo(linkedSetOf()) { it.itemId }
        shareError = false
    }

    fun clearShareSelection() {
        if (!canChangeShareSelection()) return
        selectedShareIds = emptySet()
        shareError = false
    }

    private fun canChangeShareSelection(): Boolean = !shareBusy && !exportState.outputBusy &&
        exportState.phase in setOf(ExportUiPhase.COMPLETED, ExportUiPhase.CANCELLED,
            ExportUiPhase.PAUSED, ExportUiPhase.NEEDS_REVIEW)

    /**
     * The chooser is prepared only after re-reading each committed item journal and verifying
     * its provider bytes. Uncertain and failed items are never shared or silently retried.
     */
    fun shareSelected(context: Context, onReady: (Intent?) -> Unit) {
        if (shareBusy || exportState.outputBusy || exportState.phase !in setOf(
                ExportUiPhase.COMPLETED, ExportUiPhase.CANCELLED, ExportUiPhase.PAUSED, ExportUiPhase.NEEDS_REVIEW)) return
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
                    ExportShareIntents.build(outputs)
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
        sessionExpired = false
        if (draft.workingSourceIds.isEmpty()) {
            sessionRestored = false
            sessionExpired = false
            resetEditorContext()
        }
        checkpointDraft()
        selectedShareIds = emptySet()
        shareError = false
        if (exportState.isLocked || reviewToken != null ||
            exportState.phase == ExportUiPhase.NEEDS_REVIEW) return
        exportState = ReviewExportState()
    }

    override fun onCleared() {
        nativeCheckCancel?.set(true)
        cancelJob?.set(true)
        importStop?.set(true)
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


