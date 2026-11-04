package org.cajapersonal.fixmyshot

import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.*
import org.cajapersonal.fixmyshot.review.PrivateExecutionResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Durable batch policy; the real MediaStore/ViewModel retry is in ReviewExportFlowTest. */
@RunWith(AndroidJUnit4::class)
class BatchRetryFlowTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()
    private class Fixture {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "retry-test-${UUID.randomUUID()}").apply { mkdirs() }
        val candidates = File(root, "candidates").apply { mkdirs() }
        val jobs = File(root, "jobs")
        val token = BatchOutputCoordinator.newToken()
        val folder get() = File(jobs, token)
        val journal get() = BatchExportJournal(File(folder, "batch.json"))
    }
    private fun draft() = DraftSession(destination = DestinationChoice.Custom.of(1, 1))
        .addImages((1..3).map { DraftImage("item-$it", "source-$it", PixelSize(64, 64), false,
            contentSha256 = it.toString().repeat(64)) })
    private fun coordinator(f: Fixture, save: (PrivateOutputRecipe, OutputJournal) -> SaveResult) =
        BatchOutputCoordinator(f.jobs,
            { ExportPreparation.inspect(it, Build.VERSION.SDK_INT, heap) },
            BatchItemProcessor(f.candidates, { _, recipe, _ ->
                val file = File.createTempFile("candidate-", ".png", f.candidates).apply { writeBytes(byteArrayOf(1)) }
                PrivateExecutionResult.Ready(file, recipe.codecRoute, recipe.sourceSha256, recipe.canvas, Color.BLUE)
            }, { recipe, _, journal, _ -> save(recipe, journal) }))
    private fun published(recipe: PrivateOutputRecipe, journal: OutputJournal): SaveResult {
        val uri = Uri.parse("content://fixmyshot.synthetic/${recipe.sourceId}")
        journal.write(OutputRecord("a".repeat(32), recipe.sourceId,
            "FixMyShot_20261022T000000Z_aaaaaaaa.png", "image/png", "b".repeat(64), OutputStage.PUBLISHED, uri.toString()))
        return SaveResult.Published(uri, "FixMyShot_20261022T000000Z_aaaaaaaa.png")
    }

    @Test fun retriesOnlyFailedItemsAndPreservesPublishedReceipts() {
        val f = Fixture()
        try {
            var fail = true
            val writes = mutableListOf<String>()
            val runner = coordinator(f) { recipe, journal ->
                writes += recipe.sourceId
                if (recipe.sourceId == "item-2" && fail) {
                    journal.write(OutputRecord("c".repeat(32), recipe.sourceId,
                        "FixMyShot_20261022T000000Z_cccccccc.png", "image/png", "d".repeat(64), OutputStage.FAILED))
                    SaveResult.Failed(SaveFailure.PROVIDER_ERROR)
                } else published(recipe, journal)
            }
            assertTrue(runner.start(draft(), f.token) is BatchRunResult.Finished)
            val committed = File(f.folder, "item-0.json").readBytes()
            assertEquals(setOf("item-2"), runner.retryableIds(f.token))
            assertTrue(runner.start(draft(), f.token) is BatchRunResult.Existing)
            fail = false
            val done = runner.retry(f.token, setOf("item-2")) as BatchRunResult.Finished
            assertTrue(done.snapshot.items.all { it.phase == BatchItemPhase.PUBLISHED })
            assertEquals(listOf("item-1", "item-2", "item-3", "item-2"), writes)
            assertArrayEquals(committed, File(f.folder, "item-0.json").readBytes())
            assertTrue(runner.retryableIds(f.token).isEmpty())
        } finally { f.root.deleteRecursively() }
    }

    @Test fun foregroundPauseFinishesCurrentImageAndExplicitContinuationPreservesItsReceipt() {
        val f = Fixture()
        try {
            val writes = mutableListOf<String>()
            lateinit var runner: BatchOutputCoordinator
            runner = coordinator(f) { recipe, journal ->
                writes += recipe.sourceId
                if (recipe.sourceId == "item-1") runner.pauseAfterCurrent()
                published(recipe, journal)
            }
            val paused = runner.start(draft(), f.token) as BatchRunResult.Finished
            assertEquals(BatchItemPhase.PUBLISHED, paused.snapshot.items.first().phase)
            assertTrue(paused.snapshot.items.drop(1).all { it.phase == BatchItemPhase.CANCELLED && it.reason == "PAUSED" })
            assertEquals(listOf("item-1"), writes)
            val committed = File(f.folder, "item-0.json").readBytes()
            val resumed = coordinator(f) { recipe, journal -> writes += recipe.sourceId; published(recipe, journal) }
            val done = resumed.retry(f.token, resumed.retryableIds(f.token)) as BatchRunResult.Finished
            assertTrue(done.snapshot.items.all { it.phase == BatchItemPhase.PUBLISHED })
            assertEquals(listOf("item-1", "item-2", "item-3"), writes)
            assertArrayEquals(committed, File(f.folder, "item-0.json").readBytes())
        } finally { f.root.deleteRecursively() }
    }

    @Test fun restartResumesOnlyProvenUnstartedOrCancelledItems() {
        val f = Fixture()
        try {
            val original = draft()
            val first = coordinator(f) { _, _ -> fail("Cancelled start cannot save"); SaveResult.Failed(SaveFailure.CANCELLED) }
            first.start(original, f.token, AtomicBoolean(true))
            val checkpoint = f.journal.read()!!
            f.journal.write(checkpoint.copy(items = checkpoint.items.mapIndexed { i, item ->
                item.copy(phase = if (i == 0) BatchItemPhase.ENCODING else BatchItemPhase.QUEUED)
            }))
            val saved = mutableListOf<String>()
            val restarted = coordinator(f) { recipe, journal -> saved += recipe.sourceId; published(recipe, journal) }
            assertTrue(ExportDraftJournal.same(original, restarted.restoreDraft(f.token)!!))
            assertTrue(restarted.start(original, f.token) is BatchRunResult.Existing)
            assertTrue(saved.isEmpty())
            val done = restarted.retry(f.token, restarted.retryableIds(f.token)) as BatchRunResult.Finished
            assertEquals(3, done.snapshot.items.count { it.phase == BatchItemPhase.PUBLISHED })
            assertEquals(3, saved.size)
        } finally { f.root.deleteRecursively() }
    }

    @Test fun ambiguousOutputIsNeverRetriedButUnstartedItemsCanContinue() {
        val f = Fixture()
        try {
            val saved = mutableListOf<String>()
            val first = coordinator(f) { recipe, journal ->
                saved += recipe.sourceId
                val uri = Uri.parse("content://fixmyshot.synthetic/uncertain")
                journal.write(OutputRecord("c".repeat(32), recipe.sourceId,
                    "FixMyShot_20261022T000000Z_cccccccc.png", "image/png", "d".repeat(64), OutputStage.INDETERMINATE, uri.toString()))
                SaveResult.Indeterminate(uri, "FixMyShot_20261022T000000Z_cccccccc.png")
            }
            assertTrue(first.start(draft(), f.token) is BatchRunResult.NeedsReview)
            val retry = coordinator(f) { recipe, journal -> saved += recipe.sourceId; published(recipe, journal) }
            assertTrue(retry.retry(f.token, setOf("item-1")) is BatchRunResult.NeedsReview)
            assertEquals(listOf("item-1"), saved)
            val done = retry.retry(f.token, setOf("item-2", "item-3")) as BatchRunResult.Finished
            assertEquals(BatchItemPhase.NEEDS_REVIEW, done.snapshot.items[0].phase)
            assertEquals(2, done.snapshot.items.count { it.phase == BatchItemPhase.PUBLISHED })
            assertEquals(listOf("item-1", "item-2", "item-3"), saved)
        } finally { f.root.deleteRecursively() }
    }

    @Test fun liveLockBlocksAnotherWriterAndStaleFileDoesNotBlockExplicitRecovery() {
        val f = Fixture()
        try {
            var saves = 0
            val runner = coordinator(f) { recipe, journal -> saves++; published(recipe, journal) }
            runner.start(draft(), f.token, AtomicBoolean(true))
            RandomAccessFile(File(f.folder, ".active"), "rw").use { file ->
                val held = file.channel.lock()
                try {
                    assertTrue(runner.retry(f.token, runner.retryableIds(f.token)) is BatchRunResult.NeedsReview)
                    assertEquals(0, saves)
                } finally { held.release() }
            }
            assertTrue(File(f.folder, ".active").exists())
            assertTrue(runner.retry(f.token, runner.retryableIds(f.token)) is BatchRunResult.Finished)
            assertEquals(3, saves)
        } finally { f.root.deleteRecursively() }
    }

    @Test fun frozenDraftRetainsCropSizeMetadataAndOverrideProvenance() {
        val f = Fixture()
        try {
            val original = draft().setDestination(DestinationChoice.Custom.of(4, 5))
                .setBatch(EditSettings(background = BackgroundChoice.CustomRgb(0x123456), format = OutputFormat.WEBP_LOSSLESS,
                    size = OutputSizeChoice.Exact(PixelSize(1080, 1920), true), metadata = MetadataPolicy.PRESERVE_ALLOWLIST))
                .editItem("item-2", ItemOverrides(fit = ItemValue.Custom(FitMode.CROP),
                    crop = ItemValue.Custom(CropFocus(0.4, 0.6, 2.0)), format = ItemValue.Custom(OutputFormat.PNG)))
            val journal = ExportDraftJournal(File(f.root, "draft.json"))
            journal.write(original)
            val restored = journal.read()!!
            assertTrue(ExportDraftJournal.same(original, restored))
            assertEquals(ItemValue.Inherit, restored.images[0].overrides.crop)
            assertEquals(ItemValue.Custom(CropFocus(0.4, 0.6, 2.0)), restored.images[1].overrides.crop)
        } finally { f.root.deleteRecursively() }
    }

    @Test fun changedPlanCannotReuseTheOriginalRetryPermission() {
        val f = Fixture()
        try {
            var saves = 0
            val runner = coordinator(f) { recipe, journal -> saves++; published(recipe, journal) }
            runner.start(draft(), f.token, AtomicBoolean(true))
            val file = File(f.folder, "draft.json")
            val json = org.json.JSONObject(file.readText())
            json.getJSONObject("batch").put("background", org.json.JSONObject().put("kind", "white"))
            file.writeText(json.toString())
            val result = runner.retry(f.token, runner.retryableIds(f.token))
            assertTrue(result is BatchRunResult.NeedsReview)
            assertEquals(0, saves)
        } finally { f.root.deleteRecursively() }
    }

    @Test fun oversizedOrUnknownRecoveryJsonFailsClosed() {
        val f = Fixture()
        try {
            val file = File(f.root, "draft.json")
            file.writeText("{\"schema\":99}")
            assertThrows(IOException::class.java) { ExportDraftJournal(file).read() }
            file.writeBytes(ByteArray(2 * 1024 * 1024 + 1))
            assertThrows(IOException::class.java) { ExportDraftJournal(file).read() }
            val output = File(f.root, "output.json").apply { writeBytes(ByteArray(16_385)) }
            assertThrows(IOException::class.java) { OutputJournal(output).read() }
        } finally { f.root.deleteRecursively() }
    }
}
