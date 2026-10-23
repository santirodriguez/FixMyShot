package org.cajapersonal.fixmyshot.export

import android.content.Context
import android.os.Build
import android.os.Looper
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.ExportPreparation
import org.cajapersonal.fixmyshot.core.PrivateBatchPreflight
import org.cajapersonal.fixmyshot.core.PrivateOutputRecipe
import org.cajapersonal.fixmyshot.review.PrivateOutputExecutor
import org.cajapersonal.fixmyshot.review.PrivateExecutionResult
import org.cajapersonal.fixmyshot.review.ReviewSources
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal sealed interface BatchRunResult {
    data class Blocked(val items: Int) : BatchRunResult
    data class Existing(val snapshot: BatchCheckpoint) : BatchRunResult
    data class Finished(val snapshot: BatchCheckpoint) : BatchRunResult
    data class NeedsReview(val snapshot: BatchCheckpoint?) : BatchRunResult
}

/**
 * Foreground-only, sequential, frozen-draft internal batch runner.
 * Never resumes, retries, shares or publishes without a new explicit user action.
 */
internal class BatchOutputCoordinator(
    private val jobsRoot: File,
    private val preflight: (DraftSession) -> PrivateBatchPreflight,
    private val processor: BatchItemProcessor,
    private val verifyOutput: ((OutputRecord) -> OutputRecoveryEvidence)? = null,
) {
    companion object {
        fun newToken(): String = UUID.randomUUID().toString().replace("-", "")

        fun forPort(context: Context, port: OwnedOutputPort,
            pauseBeforeNext: AtomicBoolean = AtomicBoolean(false)): BatchOutputCoordinator {
            val app = context.applicationContext
            val budget = Runtime.getRuntime().maxMemory()
            val candidateDir = File(app.cacheDir, "batch-candidates")
            val executor = PrivateOutputExecutor(
                ReviewSources(app), LosslessEncoder(candidateDir, budget), budget, copyDirectory = candidateDir)
            return BatchOutputCoordinator(
                File(app.filesDir, "export-jobs"),
                { draft -> ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, budget) },
                BatchItemProcessor(candidateDir,
                    { draft, recipe, cancelled -> executor.execute(draft, recipe, cancelled) },
                    { recipe, ready, journal, cancelled ->
                        TransactionalOutputSaver(app.cacheDir, port, journal)
                            .save(recipe, ready, cancelled)
                    }),
                OutputRecoveryVerifier(app.contentResolver)::verify,
            ).also { it.pauseBeforeNext = pauseBeforeNext }
        }
    }

    private var pauseBeforeNext = AtomicBoolean(false)

    /** Latches a foreground pause; the current bounded transaction finishes normally. */
    fun pauseAfterCurrent() { pauseBeforeNext.set(true) }

    private fun jobFolder(token: String): File {
        require(token.matches(Regex("[0-9a-f]{32}")))
        return File(jobsRoot, token)
    }

    /** Restart inspection is read-only and never initiates another output write. */
    fun checkpoint(token: String): BatchCheckpoint? =
        BatchExportJournal(File(jobFolder(token), "batch.json")).read()

    /** Individual durable output records can reveal a commit missed by the batch journal. */
    fun outputRecords(token: String): List<OutputRecord?> {
        val snapshot = checkpoint(token) ?: return emptyList()
        val folder = jobFolder(token)
        return snapshot.items.indices.map { index ->
            OutputJournal(File(folder, "item-${index}.json")).read()
        }
    }

    fun restoreDraft(token: String): DraftSession? {
        val snapshot = checkpoint(token) ?: return null
        val draft = ExportDraftJournal(File(jobFolder(token), "draft.json")).read() ?: return null
        return draft.takeIf { it.revision == snapshot.sessionRevision &&
            it.images.map { image -> image.id to image.contentSha256 } ==
                snapshot.items.map { item -> item.itemId to item.sourceSha256 } }
    }

    fun retryableIds(token: String): Set<String> {
        val snapshot = checkpoint(token) ?: return emptySet()
        return BatchRetryPolicy.eligible(jobFolder(token), snapshot)
    }

    @Synchronized
    fun start(draft: DraftSession, token: String,
        cancelled: AtomicBoolean = AtomicBoolean(false),
        onProgress: (BatchCheckpoint) -> Unit = {}): BatchRunResult =
        run(draft, token, null, cancelled, onProgress)

    /** Explicit action only. The original frozen plan, not the current editable draft, owns retry. */
    @Synchronized
    fun retry(token: String, selectedIds: Set<String>,
        cancelled: AtomicBoolean = AtomicBoolean(false),
        onProgress: (BatchCheckpoint) -> Unit = {}): BatchRunResult {
        val existing = try { checkpoint(token) } catch (_: Exception) { null }
            ?: return BatchRunResult.NeedsReview(null)
        val draft = try { restoreDraft(token) } catch (_: Exception) { null }
            ?: return BatchRunResult.NeedsReview(existing)
        if (selectedIds.isEmpty()) return BatchRunResult.Existing(existing)
        return run(draft, token, selectedIds, cancelled, onProgress)
    }

    private fun run(draft: DraftSession, token: String, retryIds: Set<String>?,
        cancelled: AtomicBoolean, onProgress: (BatchCheckpoint) -> Unit): BatchRunResult {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Batch encoding must run off the UI thread" }
        val folder = jobFolder(token)
        val store = BatchExportJournal(File(folder, "batch.json"))
        val previous = try { store.read() } catch (_: IOException) {
            return BatchRunResult.NeedsReview(null)
        }
        if (retryIds == null && previous != null) return BatchRunResult.Existing(previous)
        if (retryIds != null && previous == null) return BatchRunResult.NeedsReview(null)
        val plan = preflight(draft)
        if (plan.items.isEmpty()) return BatchRunResult.Blocked(0)
        if (plan.blockedCount != 0 || plan.candidates.size != draft.images.size ||
            plan.revision != draft.revision ||
            plan.candidates.map { it.sourceId } != draft.images.map { it.id }) {
            return BatchRunResult.Blocked(maxOf(plan.blockedCount, 1))
        }
        if (!folder.isDirectory && !folder.mkdirs()) return BatchRunResult.NeedsReview(null)
        // OS locks expire on process death. A stale file alone must not permanently block recovery.
        // Keep the inode after release: deleting it could let two writers lock different files.
        val channel = try { RandomAccessFile(File(folder, ".active"), "rw").channel }
            catch (_: IOException) { return BatchRunResult.NeedsReview(previous) }
        val lock = try { channel.tryLock() }
            catch (_: OverlappingFileLockException) { null }
            catch (_: IOException) { null }
        if (lock == null) { channel.close(); return BatchRunResult.NeedsReview(previous) }
        try {
            val settled = try { store.read() } catch (_: IOException) {
                return BatchRunResult.NeedsReview(null)
            }
            if (retryIds == null && settled != null) return BatchRunResult.Existing(settled)
            val draftStore = ExportDraftJournal(File(folder, "draft.json"))
            var snapshot = if (retryIds == null) {
                try { draftStore.write(draft, FrozenPlanFingerprint.of(plan.candidates)) } catch (_: Exception) { return BatchRunResult.NeedsReview(null) }
                BatchCheckpoint(token, draft.revision, plan.candidates.map {
                    BatchItemCheckpoint(it.sourceId, it.sourceSha256, BatchItemPhase.QUEUED)
                })
            } else {
                val frozen = try { restoreDraft(token) } catch (_: Exception) { null }
                if (settled == null || frozen == null || !ExportDraftJournal.same(draft, frozen) ||
                    !draftStore.matchesPlan(plan.candidates)) {
                    return BatchRunResult.NeedsReview(settled)
                }
                val checked = BatchRecoveryReconciliation.inspect(folder, settled, verifyOutput)
                if (!BatchRetryPolicy.eligible(folder, checked).containsAll(retryIds)) {
                    return BatchRunResult.NeedsReview(checked)
                }
                checked
            }
            val targets = snapshot.items.indices.filter {
                retryIds == null || snapshot.items[it].itemId in retryIds
            }.toSet()
            fun write(): Boolean = try {
                store.write(snapshot)
                try { onProgress(snapshot) } catch (_: Exception) { /* UI callback is not authority */ }
                true
            } catch (_: IOException) { false }
            if (!write()) return BatchRunResult.NeedsReview(null)
            fun change(index: Int, phase: BatchItemPhase, uri: String? = null,
                       reason: String? = null): Boolean {
                val items = snapshot.items.toMutableList()
                items[index] = items[index].copy(phase = phase, uri = uri, reason = reason)
                val next = snapshot.copy(items = items)
                try { store.write(next) } catch (_: IOException) { return false }
                snapshot = next
                try { onProgress(snapshot) } catch (_: Exception) { /* safe to ignore */ }
                return true
            }
            if (retryIds != null) for (index in targets) {
                val journal = OutputJournal(File(folder, "item-$index.json"))
                val old = try { journal.read() } catch (_: IOException) { return BatchRunResult.NeedsReview(snapshot) }
                if (!BatchRetryPolicy.safe(snapshot.items[index], old) ||
                    !change(index, BatchItemPhase.QUEUED)) return BatchRunResult.NeedsReview(snapshot)
                // Only a no-output terminal receipt is cleared, after recording explicit retry.
                journal.clear()
                if (journal.read() != null) return BatchRunResult.NeedsReview(snapshot)
            }
            fun cancelRest(startIndex: Int, reason: String = "CANCELLED"): Boolean {
                for (index in targets.filter { it >= startIndex }) {
                    if (snapshot.items[index].phase == BatchItemPhase.QUEUED &&
                        !change(index, BatchItemPhase.CANCELLED, reason = reason)) return false
                }
                return true
            }
            for ((index, recipe) in plan.candidates.withIndex()) {
                if (index !in targets) continue
                if (cancelled.get()) {
                    if (!cancelRest(index)) return BatchRunResult.NeedsReview(snapshot)
                    break
                }
                if (pauseBeforeNext.get()) {
                    if (!cancelRest(index, "PAUSED")) return BatchRunResult.NeedsReview(snapshot)
                    break
                }
                if (!change(index, BatchItemPhase.ENCODING)) return BatchRunResult.NeedsReview(snapshot)
                val itemJournal = OutputJournal(File(folder, "item-$index.json"))
                val result = processor.process(draft, recipe, itemJournal, cancelled) {
                    change(index, BatchItemPhase.SAVING)
                }
                val outcome = when (result) {
                    is BatchItemOutcome.Published -> change(index, BatchItemPhase.PUBLISHED,
                        uri = result.uri.toString())
                    is BatchItemOutcome.Failed -> change(index, BatchItemPhase.FAILED, reason = result.reason)
                    is BatchItemOutcome.Cancelled -> change(index, BatchItemPhase.CANCELLED, reason = result.reason)
                    is BatchItemOutcome.Review -> change(index, BatchItemPhase.NEEDS_REVIEW,
                        uri = result.uri?.toString(), reason = result.reason)
                }
                if (!outcome) return BatchRunResult.NeedsReview(snapshot)
                if (result is BatchItemOutcome.Review) return BatchRunResult.NeedsReview(snapshot)
                if (result is BatchItemOutcome.Cancelled) {
                    if (!cancelRest(index + 1)) return BatchRunResult.NeedsReview(snapshot)
                    break
                }
            }
            return BatchRunResult.Finished(snapshot)
        } finally {
            try { lock.release() } finally { channel.close() }
        }
    }
}
