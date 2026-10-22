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
) {
    companion object {
        fun newToken(): String = UUID.randomUUID().toString().replace("-", "")

        fun forPort(context: Context, port: OwnedOutputPort): BatchOutputCoordinator {
            val app = context.applicationContext
            val budget = Runtime.getRuntime().maxMemory()
            val candidateDir = File(app.cacheDir, "batch-candidates")
            val executor = PrivateOutputExecutor(
                ReviewSources(app), LosslessEncoder(candidateDir, budget), budget)
            return BatchOutputCoordinator(
                File(app.filesDir, "export-jobs"),
                { draft -> ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, budget) },
                BatchItemProcessor(candidateDir,
                    { draft, recipe, cancelled -> executor.execute(draft, recipe, cancelled) },
                    { recipe, ready, journal, cancelled ->
                        TransactionalOutputSaver(app.cacheDir, port, journal)
                            .save(recipe, ready, cancelled)
                    }),
            )
        }
    }

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

    @Synchronized
    fun start(
        draft: DraftSession,
        token: String,
        cancelled: AtomicBoolean = AtomicBoolean(false),
        onProgress: (BatchCheckpoint) -> Unit = {},
    ): BatchRunResult {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Batch encoding must run off the UI thread" }
        val folder = jobFolder(token)
        val store = BatchExportJournal(File(folder, "batch.json"))
        val previous = try { store.read() } catch (_: IOException) {
            return BatchRunResult.NeedsReview(null)
        }
        if (previous != null) return BatchRunResult.Existing(previous)
        val plan = preflight(draft)
        if (plan.items.isEmpty()) return BatchRunResult.Blocked(0)
        if (plan.blockedCount != 0 || plan.candidates.size != draft.images.size ||
            plan.revision != draft.revision ||
            plan.candidates.map { it.sourceId } != draft.images.map { it.id }) {
            return BatchRunResult.Blocked(maxOf(plan.blockedCount, 1))
        }
        if (!folder.isDirectory && !folder.mkdirs()) return BatchRunResult.NeedsReview(null)
        // An abandoned lock is an explicit recovery condition, never an auto-retry.
        val lock = File(folder, ".active")
        if (!(try { lock.createNewFile() } catch (_: IOException) { false })) {
            return BatchRunResult.NeedsReview(null)
        }
        try {
            // Another instance can finish between the first read and lock acquisition.
            val settled = try { store.read() } catch (_: IOException) {
                return BatchRunResult.NeedsReview(null)
            }
            if (settled != null) return BatchRunResult.Existing(settled)
            var snapshot = BatchCheckpoint(token, draft.revision, plan.candidates.map {
                BatchItemCheckpoint(it.sourceId, it.sourceSha256, BatchItemPhase.QUEUED)
            })
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
            fun cancelRest(startIndex: Int): Boolean {
                for (index in startIndex until snapshot.items.size) {
                    if (snapshot.items[index].phase == BatchItemPhase.QUEUED &&
                        !change(index, BatchItemPhase.CANCELLED, reason = "CANCELLED")) return false
                }
                return true
            }
            for ((index, recipe) in plan.candidates.withIndex()) {
                if (cancelled.get()) {
                    if (!cancelRest(index)) return BatchRunResult.NeedsReview(snapshot)
                    break
                }
                if (!change(index, BatchItemPhase.ENCODING)) return BatchRunResult.NeedsReview(snapshot)
                val itemJournal = OutputJournal(File(folder, "item-${index}.json"))
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
            lock.delete()
        }
    }
}
