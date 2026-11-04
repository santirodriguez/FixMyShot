package org.cajapersonal.fixmyshot

import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.*
import org.cajapersonal.fixmyshot.review.PrivateExecutionResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Orchestration-only faults; actual output bytes/provider validation has separate tests. */
@RunWith(AndroidJUnit4::class)
class BatchOutputCoordinatorTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()

    private class Fixture {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "batch-test-" + UUID.randomUUID()).apply { mkdirs() }
        val candidates = File(root, "candidates").apply { mkdirs() }
        val jobs = File(root, "jobs")
        fun close() { root.deleteRecursively() }
    }

    private fun draft(count: Int, missingHash: Boolean = false): DraftSession {
        val images = (1..count).map { index ->
            DraftImage("item-$index", "source-$index", PixelSize(64, 64), false,
                contentSha256 = if (missingHash && index == count) null
                    else index.toString().repeat(64))
        }
        return DraftSession(destination = DestinationChoice.Custom.of(1, 1)).addImages(images)
    }

    private fun fixtureCoordinator(
        fixture: Fixture,
        onRender: (PrivateOutputRecipe) -> Unit = {},
        onSave: (PrivateOutputRecipe, OutputJournal) -> SaveResult,
    ): BatchOutputCoordinator {
        val processor = BatchItemProcessor(fixture.candidates,
            { _, recipe, _ ->
                onRender(recipe)
                val file = File.createTempFile("candidate-", ".png", fixture.candidates)
                file.writeBytes(byteArrayOf(1, 2, 3))
                PrivateExecutionResult.Ready(file, recipe.codecRoute, recipe.sourceSha256,
                    recipe.canvas, Color.BLUE)
            },
            { recipe, _, journal, _ -> onSave(recipe, journal) })
        return BatchOutputCoordinator(fixture.jobs,
            { session -> ExportPreparation.inspect(session, Build.VERSION.SDK_INT, heap) },
            processor)
    }

    private fun published(recipe: PrivateOutputRecipe, journal: OutputJournal): SaveResult.Published {
        val uri = Uri.parse("content://fixmyshot.synthetic/" + recipe.sourceId)
        journal.write(OutputRecord("a".repeat(32), recipe.sourceId,
            "FixMyShot_20261022T000000Z_aaaaaaaa.png", "image/png",
            "b".repeat(64), OutputStage.PUBLISHED, uri.toString()))
        return SaveResult.Published(uri, "FixMyShot_20261022T000000Z_aaaaaaaa.png")
    }

    @Test fun keepsIndependentResultsAndDeletesAllPrivateCandidates() {
        val fixture = Fixture()
        try {
            val plan = draft(3)
            val token = BatchOutputCoordinator.newToken()
            val saved = mutableListOf<String>()
            val phases = mutableListOf<BatchItemPhase>()
            val coordinator = fixtureCoordinator(fixture,
                onRender = { recipe ->
                    val stage = coordinatorJournal(fixture, token).read()!!.items
                        .single { it.itemId == recipe.sourceId }.phase
                    assertEquals(BatchItemPhase.ENCODING, stage)
                },
                onSave = { recipe, journal ->
                    val current = coordinatorJournal(fixture, token).read()!!.items
                        .single { it.itemId == recipe.sourceId }.phase
                    assertEquals(BatchItemPhase.SAVING, current)
                    saved += recipe.sourceId
                    if (recipe.sourceId == "item-2") SaveResult.Failed(SaveFailure.PROVIDER_ERROR)
                    else published(recipe, journal)
                })
            val result = coordinator.start(plan, token, onProgress = { snapshot ->
                phases += snapshot.items.first().phase
            })
            assertTrue(result.toString(), result is BatchRunResult.Finished)
            result as BatchRunResult.Finished
            assertEquals(listOf(BatchItemPhase.PUBLISHED, BatchItemPhase.FAILED,
                BatchItemPhase.PUBLISHED), result.snapshot.items.map { it.phase })
            assertEquals(listOf("item-1", "item-2", "item-3"), saved)
            assertTrue(phases.contains(BatchItemPhase.ENCODING))
            assertTrue(phases.contains(BatchItemPhase.SAVING))
            assertEquals(0, fixture.candidates.listFiles()?.size ?: 0)
            val outputs = coordinator.outputRecords(token)
            assertEquals(3, outputs.size)
            assertEquals(OutputStage.PUBLISHED, outputs[0]?.stage)
            assertNull(outputs[1])
            assertEquals(OutputStage.PUBLISHED, outputs[2]?.stage)
        } finally { fixture.close() }
    }

    @Test fun restartCannotAutomaticallyDuplicateCommittedOutputs() {
        val fixture = Fixture()
        try {
            val token = BatchOutputCoordinator.newToken()
            var calls = 0
            val coordinator = fixtureCoordinator(fixture, onSave = { recipe, journal ->
                calls++
                published(recipe, journal)
            })
            val first = coordinator.start(draft(2), token)
            assertTrue(first is BatchRunResult.Finished)
            val afterRestart = fixtureCoordinator(fixture, onSave = { _, _ ->
                calls++
                throw IllegalStateException("Must not retry")
            }).start(draft(2), token)
            assertTrue(afterRestart is BatchRunResult.Existing)
            assertEquals(2, calls)
            assertEquals(listOf(BatchItemPhase.PUBLISHED, BatchItemPhase.PUBLISHED),
                coordinator.checkpoint(token)!!.items.map { it.phase })
        } finally { fixture.close() }
    }

    @Test fun cancellationKeepsPublishedItemsAndStopsBeforeNextItem() {
        val fixture = Fixture()
        try {
            val cancel = AtomicBoolean(false)
            var saves = 0
            val coordinator = fixtureCoordinator(fixture, onSave = { recipe, journal ->
                saves++
                val done = published(recipe, journal)
                cancel.set(true)
                done
            })
            val result = coordinator.start(draft(3), BatchOutputCoordinator.newToken(), cancel)
            assertTrue(result is BatchRunResult.Finished)
            result as BatchRunResult.Finished
            assertEquals(listOf(BatchItemPhase.PUBLISHED, BatchItemPhase.CANCELLED,
                BatchItemPhase.CANCELLED), result.snapshot.items.map { it.phase })
            assertEquals(1, saves)
        } finally { fixture.close() }
    }

    @Test fun uncertainPublicationHaltsWithoutTouchingRemainingItem() {
        val fixture = Fixture()
        try {
            val token = BatchOutputCoordinator.newToken()
            var calls = 0
            val coordinator = fixtureCoordinator(fixture, onSave = { recipe, journal ->
                calls++
                val uri = Uri.parse("content://fixmyshot.synthetic/uncertain")
                journal.write(OutputRecord("c".repeat(32), recipe.sourceId,
                    "FixMyShot_20261022T000000Z_cccccccc.png", "image/png",
                    "d".repeat(64), OutputStage.INDETERMINATE, uri.toString()))
                SaveResult.Indeterminate(uri, "FixMyShot_20261022T000000Z_cccccccc.png")
            })
            val result = coordinator.start(draft(3), token)
            assertTrue(result is BatchRunResult.NeedsReview)
            result as BatchRunResult.NeedsReview
            assertEquals(listOf(BatchItemPhase.NEEDS_REVIEW, BatchItemPhase.QUEUED,
                BatchItemPhase.QUEUED), result.snapshot!!.items.map { it.phase })
            assertEquals(1, calls)
            assertTrue(coordinator.start(draft(3), token) is BatchRunResult.Existing)
            assertEquals(1, calls)
        } finally { fixture.close() }
    }

    @Test fun incompletePreflightNeverCreatesBatchOutputOrJournal() {
        val fixture = Fixture()
        try {
            val coordinator = fixtureCoordinator(fixture, onSave = { _, _ ->
                fail("Preflight must prevent saving"); SaveResult.Failed(SaveFailure.PROVIDER_ERROR)
            })
            assertTrue(coordinator.start(draft(2, missingHash = true),
                BatchOutputCoordinator.newToken()) is BatchRunResult.Blocked)
            assertEquals(0, fixture.jobs.listFiles()?.size ?: 0)
            assertEquals(0, fixture.candidates.listFiles()?.size ?: 0)
        } finally { fixture.close() }
    }

    private fun coordinatorJournal(fixture: Fixture, token: String) =
        BatchExportJournal(File(File(fixture.jobs, token), "batch.json"))
}
