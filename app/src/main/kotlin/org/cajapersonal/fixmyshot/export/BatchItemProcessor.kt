package org.cajapersonal.fixmyshot.export

import android.net.Uri
import org.cajapersonal.fixmyshot.core.DraftSession
import org.cajapersonal.fixmyshot.core.PrivateOutputRecipe
import org.cajapersonal.fixmyshot.review.PrivateExecutionFailure
import org.cajapersonal.fixmyshot.review.PrivateExecutionResult
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal sealed interface BatchItemOutcome {
    data class Published(val uri: Uri) : BatchItemOutcome
    data class Failed(val reason: String) : BatchItemOutcome
    data class Cancelled(val reason: String) : BatchItemOutcome
    data class Review(val uri: Uri?, val reason: String) : BatchItemOutcome
}

/** One bounded private candidate and one independent output transaction at a time. */
internal class BatchItemProcessor(
    private val candidateRoot: File,
    private val render: (DraftSession, PrivateOutputRecipe, AtomicBoolean) -> PrivateExecutionResult,
    private val save: (PrivateOutputRecipe, PrivateExecutionResult.Ready, OutputJournal, AtomicBoolean) -> SaveResult,
) {
    fun process(
        draft: DraftSession,
        recipe: PrivateOutputRecipe,
        outputJournal: OutputJournal,
        cancelled: AtomicBoolean,
        beforeSave: () -> Boolean,
    ): BatchItemOutcome {
        val result = try { render(draft, recipe, cancelled) } catch (_: Exception) {
            return BatchItemOutcome.Failed("ENCODER_FAILED")
        }
        if (result is PrivateExecutionResult.Failed) {
            return if (cancelled.get() || result.reason == PrivateExecutionFailure.CANCELLED)
                BatchItemOutcome.Cancelled("CANCELLED")
            else BatchItemOutcome.Failed(result.reason.name)
        }
        val ready = result as PrivateExecutionResult.Ready
        val owned = try {
            ready.file.canonicalPath.startsWith(candidateRoot.canonicalPath + File.separator)
        } catch (_: IOException) { false }
        if (!owned) return BatchItemOutcome.Review(null, "INVALID_CANDIDATE")
        try {
            if (!beforeSave()) return BatchItemOutcome.Review(null, "JOURNAL_ERROR")
            return when (val saved = try { save(recipe, ready, outputJournal, cancelled) }
                catch (_: Exception) { null }) {
                is SaveResult.Published -> BatchItemOutcome.Published(saved.uri)
                is SaveResult.Indeterminate -> BatchItemOutcome.Review(saved.uri, "PROVIDER_ERROR")
                is SaveResult.Failed -> when (saved.reason) {
                    SaveFailure.CANCELLED -> BatchItemOutcome.Cancelled("CANCELLED")
                    SaveFailure.JOURNAL_ERROR, SaveFailure.PREVIOUS_JOB_UNRESOLVED ->
                        BatchItemOutcome.Review(null, saved.reason.name)
                    else -> BatchItemOutcome.Failed(saved.reason.name)
                }
                null -> {
                    // A provider may have published just before throwing. Inspect only; never retry.
                    val old = try { outputJournal.read() } catch (_: IOException) { null }
                    BatchItemOutcome.Review(old?.uri?.let(Uri::parse), "PROVIDER_ERROR")
                }
            }
        } finally {
            // Only delete the candidate inside our own private output directory.
            ready.file.delete()
        }
    }
}
