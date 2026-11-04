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
import java.util.concurrent.CountDownLatch
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

    @Test fun stoppingAnImportKeepsTheCurrentImageAndReportsUntouchedQueuedOffers() {
        if (Build.VERSION.SDK_INT < 29) return
        val f = Fixture()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val once = AtomicBoolean(false)
        val blocked = object : ContextWrapper(f.context) {
            override fun getApplicationContext(): Context = this
            override fun getCacheDir(): File {
                if (once.compareAndSet(false, true)) {
                    entered.countDown()
                    release.await()
                }
                return f.context.cacheDir
            }
        }
        try {
            val data = png()
            val uris = (1..4).map { f.media(data) }
            instrumentation.runOnMainSync {
                f.model.enqueueImports(uris.take(3))
                f.model.drainImports(blocked)
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                assertEquals(ImportProgress(0, 3), f.model.importProgress)
                f.model.enqueueImports(listOf(uris.last()))
                assertEquals(1, f.model.pendingImportCount)
                f.model.stopImport()
                assertTrue(f.model.importProgress!!.stopping)
                assertEquals(0, f.model.pendingImportCount)
            }
            release.countDown()
            await { !f.model.isBusy && f.model.latestImport != null }
            assertNull(f.model.importProgress)
            val report = f.model.latestImport!!
            assertEquals(4, report.total)
            assertEquals(1, report.added)
            assertEquals(3, report.notAttempted)
            assertEquals(0, report.ignored)
            assertTrue(report.problems.isEmpty())
            assertEquals(0, f.model.invalidImports)
            val image = f.model.draft.images.single()
            assertTrue(ReviewSources(f.context).matchesFingerprint(image.id, image.contentSha256!!))
            assertEquals(1, File(f.context.cacheDir, "review-sources").listFiles().orEmpty().size)
            assertFalse(report.toString().contains("content://"))
            // A later explicit offer is a fresh import; stopped offers never restart themselves.
            instrumentation.runOnMainSync {
                f.model.enqueueImports(listOf(uris[1]))
                f.model.drainImports(f.context)
            }
            await { !f.model.isBusy && f.model.draft.images.size == 2 }
            assertEquals(0, f.model.latestImport!!.notAttempted)
            for (uri in uris) assertEquals(data.toList(), app.contentResolver.openInputStream(uri)!!.use { it.readBytes() }.toList())
        } finally { release.countDown(); f.close() }
    }

    @Test fun lowStorageStopsPrivateStagingWithoutTouchingCompletedImagesOrOriginals() {
        if (Build.VERSION.SDK_INT < 29) return
        val f = Fixture()
        try {
            val data = png()
            val good = f.media(data)
            val saved = ReviewSources(f.context) { Long.MAX_VALUE }.stage(good) as ReviewSources.ImportResult.Added
            val source = File(f.context.cacheDir, "review-sources/${saved.image.id}.source")
            val savedBytes = source.readBytes()
            val low = ReviewSources(f.context) { 0L }
                .stage(Uri.parse("content://synthetic/must-not-be-opened"))
            assertEquals(ReviewSources.ImportResult.Rejected(ReviewSources.Failure.LOW_STORAGE), low)
            // This larger offer must stop during copying, before its intentionally invalid pixels are parsed.
            val largeBytes = ByteArray(2 * 1024 * 1024) { 42 }
            val large = f.media(largeBytes)
            var checks = 0
            val interrupted = ReviewSources(f.context) {
                if (++checks == 1) Long.MAX_VALUE else 0L
            }.stage(large)
            assertEquals(ReviewSources.ImportResult.Rejected(ReviewSources.Failure.LOW_STORAGE), interrupted)
            assertTrue(checks >= 2)
            assertEquals(listOf(source.name), File(f.context.cacheDir, "review-sources").listFiles()!!.map { it.name })
            assertArrayEquals(savedBytes, source.readBytes())
            assertTrue(ReviewSources(f.context).matchesFingerprint(saved.image.id, saved.image.contentSha256!!))
            assertArrayEquals(data, app.contentResolver.openInputStream(good)!!.use { it.readBytes() })
            assertArrayEquals(largeBytes, app.contentResolver.openInputStream(large)!!.use { it.readBytes() })
            // An unavailable advisory must not invent a storage failure; real I/O still decides.
            val unknown = ReviewSources(f.context) { null }.stage(good)
            assertTrue(unknown is ReviewSources.ImportResult.Added)
            val report = ImportReport.from(listOf(saved, low, interrupted), setOf(saved.image.id))
            assertEquals(1, report.added)
            assertEquals(listOf(2, 3), report.problems.map { it.position })
            assertTrue(report.problems.all { it.reason == ReviewSources.Failure.LOW_STORAGE })
            assertFalse(report.toString().contains("content://"))
            assertEquals(R.string.import_reason_storage, ResultFeedback.importReason(ReviewSources.Failure.LOW_STORAGE))
        } finally { f.close() }
    }

    @Test fun providerRuntimeAndCachePermissionFailuresStayTypedAndReleaseTheImportState() {
        val f = Fixture()
        try {
            val denied = object : ContextWrapper(f.context) {
                override fun getCacheDir(): File = throw SecurityException("SYNTHETIC_PRIVATE_DETAILS")
            }
            assertEquals(ReviewSources.ImportResult.Rejected(ReviewSources.Failure.UNREADABLE),
                ReviewSources(denied).stage(Uri.parse("content://synthetic/unopened")))
            val broken = object : ContextWrapper(f.context) {
                override fun getApplicationContext(): Context = this
                override fun getContentResolver(): android.content.ContentResolver =
                    throw IllegalStateException("SYNTHETIC_PRIVATE_DETAILS")
            }
            instrumentation.runOnMainSync {
                f.model.enqueueImports((1..3).map { Uri.parse("content://synthetic/runtime-$it") })
                f.model.drainImports(broken)
            }
            await { !f.model.isBusy && f.model.latestImport != null }
            assertNull(f.model.importProgress)
            assertTrue(f.model.draft.images.isEmpty())
            val report = f.model.latestImport!!
            assertEquals(3, report.total)
            assertEquals(0, report.added)
            assertEquals(0, report.notAttempted)
            assertEquals(listOf(1, 2, 3), report.problems.map { it.position })
            assertTrue(report.problems.all { it.reason == ReviewSources.Failure.UNREADABLE })
            assertFalse(report.toString().contains("SYNTHETIC_PRIVATE_DETAILS"))
            assertFalse(report.toString().contains("content://"))
            assertTrue(File(f.context.cacheDir, "review-sources").listFiles().orEmpty().isEmpty())
        } finally { f.close() }
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
