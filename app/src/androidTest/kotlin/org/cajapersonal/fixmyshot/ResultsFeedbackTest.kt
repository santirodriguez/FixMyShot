package org.cajapersonal.fixmyshot

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.*
import org.cajapersonal.fixmyshot.review.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.CRC32

/** Typed failures through real staging/preflight and the processor/journal boundary; synthetic only. */
@RunWith(AndroidJUnit4::class)
class ResultsFeedbackTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext
    private class Owner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
        val model get() = ViewModelProvider(this)[ReviewViewModel::class.java]
    }
    private inner class Fixture {
        val root = File(app.cacheDir, "results-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                app.getSharedPreferences(root.name + "-" + name, mode)
        }
        val owner = Owner()
        val uris = mutableListOf<Uri>()
        val model: ReviewViewModel
        init {
            lateinit var created: ReviewViewModel
            instrumentation.runOnMainSync {
                created = owner.model
                created.attachSession(context)
                created.checkPastExports(context)
            }
            model = created
            await { model.exportState.phase == ExportUiPhase.IDLE }
        }
        fun media(data: ByteArray): Uri {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "FixMyShot_synthetic_${UUID.randomUUID()}.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FixMyShot")
            }
            val uri = app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            uris += uri
            app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(data) }
            return uri
        }
        fun close() {
            val workers = listOf("exportWorker", "importWorker").map {
                ReviewViewModel::class.java.getDeclaredField(it).apply { isAccessible = true }.get(model) as ExecutorService
            }
            instrumentation.runOnMainSync { owner.viewModelStore.clear() }
            workers.forEach { assertTrue(it.awaitTermination(10, TimeUnit.SECONDS)) }
            instrumentation.waitForIdleSync()
            uris.forEach { app.contentResolver.delete(it, null, null) }
            root.deleteRecursively()
        }
    }
    private fun await(done: () -> Boolean) {
        val until = System.currentTimeMillis() + 15_000
        while (!done() && System.currentTimeMillis() < until) { instrumentation.waitForIdleSync(); Thread.sleep(20) }
        assertTrue("Expected owned operation to finish", done())
    }
    private fun png(): ByteArray {
        val bitmap = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888)
        return try {
            bitmap.eraseColor(Color.BLUE)
            ByteArrayOutputStream().also { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray()
        } finally { bitmap.recycle() }
    }
    private fun animated(data: ByteArray): ByteArray {
        val payload = byteArrayOf(0, 0, 0, 2, 0, 0, 0, 0)
        val tag = "acTL".toByteArray()
        val chunk = ByteArrayOutputStream().also { out -> DataOutputStream(out).use {
            it.writeInt(payload.size); it.write(tag); it.write(payload)
            it.writeInt(CRC32().apply { update(tag); update(payload) }.value.toInt())
        } }.toByteArray()
        return data.copyOfRange(0, 33) + chunk + data.copyOfRange(33, data.size)
    }

    @Test fun mixedRealImportKeepsGoodPixelsAndReportsReceivedPositionsWithoutPrivateNames() {
        if (Build.VERSION.SDK_INT < 29) return
        val f = Fixture()
        try {
            val data = png()
            val good = f.media(data)
            val animation = f.media(animated(data))
            val corrupt = f.media(byteArrayOf(1, 2, 3))
            val missing = f.media(data).also {
                assertEquals(1, app.contentResolver.delete(it, null, null))
                f.uris.remove(it) // Already removed by this test; do not delete a revoked URI again.
            }
            instrumentation.runOnMainSync {
                f.model.enqueueImports(listOf(good, animation, corrupt, missing))
                f.model.drainImports(f.context)
            }
            await { !f.model.isBusy && f.model.latestImport != null }
            val report = f.model.latestImport!!
            assertEquals(4, report.total); assertEquals(1, report.added); assertEquals(0, report.ignored)
            assertEquals(listOf(2, 3, 4), report.problems.map { it.position })
            assertEquals(listOf(ReviewSources.Failure.ANIMATED, ReviewSources.Failure.UNSUPPORTED, ReviewSources.Failure.UNREADABLE),
                report.problems.map { it.reason })
            assertEquals(3, f.model.invalidImports)
            val image = f.model.draft.images.single()
            assertTrue(ReviewSources(f.context).matchesFingerprint(image.id, image.contentSha256!!))
            assertFalse(report.toString().contains("content://"))
            assertFalse(report.toString().contains("FixMyShot_synthetic"))
            instrumentation.runOnMainSync {
                f.model.enqueueImports(listOf(corrupt)); f.model.drainImports(f.context)
            }
            await { !f.model.isBusy && f.model.latestImport?.total == 1 }
            assertEquals(listOf(1), f.model.latestImport!!.problems.map { it.position })
            assertEquals(4, f.model.invalidImports)
            instrumentation.runOnMainSync { f.model.clearImportFeedback() }
            assertNull(f.model.latestImport); assertEquals(0, f.model.invalidImports)
            assertEquals(image, f.model.draft.images.single())
            assertTrue(ReviewSources(f.context).matchesFingerprint(image.id, image.contentSha256!!))
        } finally { f.close() }
    }

    @Test fun preflightIdentifiesEveryBlockedImageAndRepairDoesNotCreateAnyOutput() {
        val f = Fixture()
        try {
            val images = (1..3).map { DraftImage(it.toString().repeat(32), "synthetic-$it", PixelSize(32, 48), false,
                contentSha256 = if (it == 1) null else "a".repeat(64),
                overrides = if (it == 2) ItemOverrides(format = ItemValue.Custom(OutputFormat.AVIF)) else ItemOverrides()) }
            instrumentation.runOnMainSync {
                f.model.draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1)).addImages(images)
                f.model.onDraftEdited()
                assertFalse(f.model.prepareExport())
                assertEquals(ExportUiPhase.BLOCKED, f.model.exportState.phase)
                assertEquals(setOf(images[0].id, images[1].id), f.model.exportState.blockedItems.map { it.itemId }.toSet())
                assertTrue(PrivatePreparationBlock.SOURCE_FINGERPRINT_MISSING in f.model.exportState.blockedItems.first().blocks)
                f.model.selectedId = images[1].id
                assertEquals(2, f.model.exportState.blockedItems.size)
                val fixed = images.map { it.copy(contentSha256 = "a".repeat(64), overrides = ItemOverrides()) }
                f.model.draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1)).addImages(fixed)
                f.model.onDraftEdited()
                assertTrue(f.model.exportState.blockedItems.isEmpty())
                assertTrue(f.model.prepareExport())
                f.model.destinationCancelled()
            }
            assertTrue(File(f.context.filesDir, "export-jobs").listFiles().orEmpty().isEmpty())
        } finally { f.close() }
    }

    @Test fun codecFailuresKeepUsefulCausesWithoutCallingProviderOrLeavingReceipts() {
        val directory = File(app.cacheDir, "result-processor-${UUID.randomUUID()}").apply { mkdirs() }
        val image = DraftImage("a".repeat(32), "synthetic", PixelSize(32, 48), false, contentSha256 = "b".repeat(64))
        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1)).addImages(listOf(image))
        val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, Runtime.getRuntime().maxMemory()).candidates.single()
        var saves = 0
        try {
            for ((failure, reason) in listOf(FailureCode.TIMEOUT to "ENCODER_TIMEOUT", FailureCode.RESOURCE_LIMIT to "RESOURCE_LIMIT",
                FailureCode.NATIVE_UNAVAILABLE to "CODEC_UNAVAILABLE", FailureCode.IO_ERROR to "ENCODER_IO")) {
                val journalFile = File(directory, "${failure.name}.json")
                val processor = BatchItemProcessor(directory,
                    { _, _, _ -> PrivateExecutionResult.Failed(PrivateExecutionFailure.ENCODER_FAILED, failure) },
                    { _, _, _, _ -> saves++; throw AssertionError("Failed preparation must not reach provider") })
                val outcome = processor.process(draft, recipe, OutputJournal(journalFile), AtomicBoolean(false)) { true }
                assertEquals(BatchItemOutcome.Failed(reason), outcome)
                assertFalse(journalFile.exists())
                assertTrue(BatchRetryPolicy.safe(BatchItemCheckpoint(image.id, image.contentSha256!!, BatchItemPhase.FAILED, reason = reason), null))
            }
            val cancelled = BatchItemProcessor(directory,
                { _, _, _ -> PrivateExecutionResult.Failed(PrivateExecutionFailure.ENCODER_FAILED, FailureCode.CANCELLED) },
                { _, _, _, _ -> saves++; throw AssertionError("Cancelled preparation must not reach provider") })
            assertEquals(BatchItemOutcome.Cancelled("CANCELLED"), cancelled.process(draft, recipe,
                OutputJournal(File(directory, "cancel.json")), AtomicBoolean(false)) { true })
            assertEquals(0, saves); assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally { directory.deleteRecursively() }
    }

    @Test fun uncertainPublicationOverridesRetryLikeReasonAndNeverDisplaysJournalText() {
        val folder = File(app.cacheDir, "result-journal-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            for (reason in listOf("ENCODER_TIMEOUT", "SOURCE_CHANGED", "PRIVATE_PATH_DATA")) {
                val item = BatchItemCheckpoint("synthetic", "a".repeat(64), BatchItemPhase.NEEDS_REVIEW,
                    "content://synthetic/private-output", reason)
                val journal = BatchExportJournal(File(folder, "batch.json"))
                journal.write(BatchCheckpoint("b".repeat(32), 0, listOf(item)))
                val restored = journal.read()!!.items.single()
                assertEquals(R.string.result_check_output, ResultFeedback.exportReason(restored))
                assertFalse(BatchRetryPolicy.safe(restored, null))
                val messages = listOf("en", "es", "ca").map { tag ->
                    val configuration = Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
                    app.createConfigurationContext(configuration).getString(ResultFeedback.exportReason(restored)!!)
                }
                assertEquals(3, messages.distinct().size)
                assertTrue(messages.all { !it.contains(reason) && !it.contains("content://") && !it.contains("private-output") })
                assertNull(ResultFeedback.exportReason(item.copy(phase = BatchItemPhase.PUBLISHED)))
            }
        } finally { folder.deleteRecursively() }
    }
}
