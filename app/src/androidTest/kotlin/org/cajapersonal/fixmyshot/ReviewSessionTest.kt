package org.cajapersonal.fixmyshot

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
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
import java.io.File
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ReviewSessionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext
    private class Owner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
        val model get() = ViewModelProvider(this)[ReviewViewModel::class.java]
    }
    private inner class Fixture {
        val root = File(app.cacheDir, "review-session-test-" + UUID.randomUUID()).apply { mkdirs() }
        val context: Context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(root, "cache").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                app.getSharedPreferences(root.name + "-" + name, mode)
        }
        val file = File(context.filesDir, "review-session.json")
        val journal = ReviewSessionJournal(file)
        val owners = mutableListOf<Owner>()
        fun image(): DraftImage {
            val id = UUID.randomUUID().toString().replace("-", "")
            val source = File(File(context.cacheDir, "review-sources").apply { mkdirs() }, "$id.source")
            val bitmap = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888)
            try {
                bitmap.setHasAlpha(false); bitmap.eraseColor(Color.BLUE)
                source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
            val hash = MessageDigest.getInstance("SHA-256").digest(source.readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            return DraftImage(id, "content://synthetic/$id", PixelSize(32, 48), false, contentSha256 = hash)
        }
        fun owner(): Owner = Owner().also { owner ->
            owners += owner
            instrumentation.runOnMainSync { owner.model.attachSession(context); owner.model.checkPastExports(context) }
            await { owner.model.exportState.phase != ExportUiPhase.CHECKING_HISTORY }
        }
        fun close(owner: Owner) {
            val model = owner.model
            val workers = listOf("exportWorker", "importWorker").map { name ->
                ReviewViewModel::class.java.getDeclaredField(name).apply { isAccessible = true }.get(model) as ExecutorService
            }
            instrumentation.runOnMainSync { owner.viewModelStore.clear() }
            workers.forEach { assertTrue("Owned test worker still active", it.awaitTermination(10, TimeUnit.SECONDS)) }
            owners.remove(owner)
        }
        fun cleanup() {
            owners.toList().forEach(::close)
            app.deleteSharedPreferences(root.name + "-export-reviewed")
            root.deleteRecursively()
        }
    }
    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000L
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) return
            Thread.sleep(20)
        }
        assertTrue("Session operation did not settle", condition())
    }

    @Test fun freshViewModelRestoresSettingsSelectionOverridesAndLatestCoalescedEditWithoutExport() {
        val f = Fixture()
        try {
            val a = f.image(); val b = f.image()
            var draft = DraftSession(destination = DestinationChoice.Custom.of(4, 5),
                batch = EditSettings(format = OutputFormat.WEBP_LOSSLESS, metadata = MetadataPolicy.PRESERVE_ALLOWLIST,
                    background = BackgroundChoice.CustomRgb(0x123456), size = OutputSizeChoice.Exact(PixelSize(320, 400))))
                .addImages(listOf(a, b)).editItem(a.id, ItemOverrides(metadata = ItemValue.Custom(MetadataPolicy.REMOVE_IDENTIFYING),
                    fit = ItemValue.Custom(FitMode.CROP), crop = ItemValue.Custom(CropFocus(0.4, 0.6, 2.0))))
            val first = f.owner()
            instrumentation.runOnMainSync {
                first.model.draft = draft; first.model.selectedId = b.id
                for (value in 1..40) {
                    draft = draft.setBatch(draft.batch.copy(background = BackgroundChoice.CustomRgb(value)))
                    first.model.draft = draft; first.model.onDraftEdited()
                }
            }
            await { runCatching { f.journal.read()?.draft == draft }.getOrDefault(false) }
            val saved = f.journal.read()!!
            assertEquals(b.id, saved.selectedId)
            assertTrue(saved.draft.canUndo)
            assertFalse(f.journal.clearMatching(draft.setDestination(DestinationChoice.Custom.of(1, 1))))
            assertEquals(draft, f.journal.read()!!.draft)
            f.close(first)
            val restored = f.owner().model
            assertEquals(draft, restored.draft)
            assertEquals(b.id, restored.selectedId)
            assertTrue(restored.sessionRestored)
            assertFalse(restored.exportState.isLocked)
            assertFalse(File(f.context.filesDir, "export-jobs").exists())
            for (image in listOf(a, b)) assertTrue(ReviewSources(f.context)
                .matchesFingerprint(image.id, requireNotNull(image.contentSha256)))
        } finally { f.cleanup() }
    }

    @Test fun removeLastUndoSurvivesRestartAndExplicitDiscardRemovesSavedReferencesAndResetsDefaults() {
        val f = Fixture()
        try {
            val image = f.image()
            val source = File(f.context.cacheDir, "review-sources/${image.id}.source")
            assertTrue(source.setLastModified(System.currentTimeMillis() - 25L * 60 * 60 * 1000))
            val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
                batch = EditSettings(metadata = MetadataPolicy.PRESERVE_ALLOWLIST))
                .addImages(listOf(image)).removeItem(image.id)
            f.journal.write(ReviewSessionSnapshot(draft, null, System.currentTimeMillis()))
            var owner = f.owner(); var model = owner.model
            assertTrue(model.draft.images.isEmpty()); assertTrue(model.draft.canUndo)
            assertTrue("Saved undo source must be protected before age cleanup", source.exists())
            instrumentation.runOnMainSync { model.draft = model.draft.undo(); model.onDraftEdited() }
            await { runCatching { f.journal.read()?.draft?.images?.single()?.id == image.id }.getOrDefault(false) }
            assertTrue(ReviewSources(f.context).matchesFingerprint(image.id, requireNotNull(image.contentSha256)))
            // Restoring the editable session must also preserve an existing export's duplicate barrier.
            val attempted = model.draft
            val plans = ExportPreparation.inspect(attempted, Build.VERSION.SDK_INT, Runtime.getRuntime().maxMemory())
            assertEquals(0, plans.blockedCount)
            f.close(owner)
            val token = UUID.randomUUID().toString().replace("-", "")
            val folder = File(f.context.filesDir, "export-jobs/$token").apply { mkdirs() }
            ExportDraftJournal(File(folder, "draft.json")).write(attempted, FrozenPlanFingerprint.of(plans.candidates))
            BatchExportJournal(File(folder, "batch.json")).write(BatchCheckpoint(token, attempted.revision,
                listOf(BatchItemCheckpoint(image.id, requireNotNull(image.contentSha256), BatchItemPhase.CANCELLED))))
            owner = f.owner(); model = owner.model
            assertTrue(model.sessionRestored); assertTrue(model.exportState.retryReady)
            instrumentation.runOnMainSync { model.acknowledgePreviousExport(f.context) }
            await { model.exportState.phase == ExportUiPhase.IDLE }
            instrumentation.runOnMainSync { assertFalse("Already-attempted restored draft cannot silently become a new ordinary export", model.prepareExport()) }
            instrumentation.runOnMainSync {
                val ids = model.draft.workingSourceIds
                model.draft = DraftSession(); model.selectedId = null; model.onDraftEdited()
                model.releasePrivateSources(f.context, ids)
            }
            await { !f.file.exists() && !source.exists() }
            f.close(owner)
            val fresh = f.owner().model
            assertTrue(fresh.draft.images.isEmpty()); assertFalse(fresh.draft.canUndo)
            assertEquals(OutputFormat.PNG, fresh.draft.batch.format)
            assertEquals(MetadataPolicy.REMOVE_IDENTIFYING, fresh.draft.batch.metadata)
        } finally { f.cleanup() }
    }

    @Test fun expiredMissingAndMalformedSessionsHaveExplicitNonExportingRecovery() {
        val f = Fixture()
        try {
            val image = f.image(); val draft = DraftSession().addImages(listOf(image))
            f.journal.write(ReviewSessionSnapshot(draft, image.id, System.currentTimeMillis() - 25L * 60 * 60 * 1000))
            val expired = f.owner()
            assertTrue(expired.model.sessionExpired); assertTrue(expired.model.draft.images.isEmpty())
            assertFalse(f.file.exists()); f.close(expired)
            f.journal.write(ReviewSessionSnapshot(draft, image.id, System.currentTimeMillis()))
            assertTrue(File(f.context.cacheDir, "review-sources/${image.id}.source").delete())
            val missing = f.owner()
            assertTrue(missing.model.sessionRestoreBlocked)
            val before = f.file.readBytes()
            instrumentation.runOnMainSync { missing.model.checkpointDraft() }
            assertArrayEquals(before, f.file.readBytes())
            assertFalse(missing.model.prepareExport())
            instrumentation.runOnMainSync { missing.model.discardUnavailableSession() }
            await { !missing.model.sessionRestoreBlocked && !f.file.exists() }
            f.close(missing)
            for (bad in listOf("{broken", "{\"schema\":999}", "x".repeat(ReviewSessionJournal.LIMIT + 1))) {
                f.file.writeText(bad)
                assertTrue(runCatching { f.journal.read() }.isFailure)
            }
            f.journal.clear()
        } finally { f.cleanup() }
    }

    @Test fun incomingOfferIsRecountedAgainstTheRestoredHundredImageBatch() {
        val f = Fixture()
        try {
            val images = (1..100).map { f.image() }
            val draft = DraftSession().addImages(images)
            f.journal.write(ReviewSessionSnapshot(draft, images.last().id, System.currentTimeMillis()))
            val owner = Owner(); f.owners += owner
            instrumentation.runOnMainSync {
                owner.model.attachSession(f.context)
                owner.model.enqueueImports(listOf(Uri.parse("content://synthetic/new-offer")))
                owner.model.checkPastExports(f.context)
            }
            await { owner.model.exportState.phase == ExportUiPhase.IDLE }
            instrumentation.runOnMainSync { owner.model.drainImports(f.context) }
            assertTrue(owner.model.importLimitExceeded)
            assertEquals(100, owner.model.draft.images.size)
            assertEquals(0, owner.model.pendingImportCount)
            assertFalse(owner.model.isBusy)
            assertFalse(File(f.context.filesDir, "export-jobs").exists())
        } finally { f.cleanup() }
    }

    @Test fun finishingSuccessfulSessionClearsCurrentUndoAndJournalButKeepsPublishedBytes() {
        val f = Fixture()
        try {
            val a = f.image(); val b = f.image()
            val owner = f.owner(); val model = owner.model
            var output: ByteArray? = null
            var deletions = 0
            val port = object : OwnedOutputPort {
                override fun create(name: String, mime: String): Uri = Uri.parse("content://synthetic/finished-output")
                override fun openWrite(uri: Uri): OutputStream = object : ByteArrayOutputStream() {
                    override fun close() { output = toByteArray(); super.close() }
                }
                override fun openRead(uri: Uri): InputStream? = output?.let(::ByteArrayInputStream)
                override fun publish(uri: Uri): Boolean = output != null
                override fun delete(uri: Uri): Boolean { deletions++; return false }
            }
            instrumentation.runOnMainSync {
                model.draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
                    batch = EditSettings(format = OutputFormat.WEBP_LOSSLESS, metadata = MetadataPolicy.PRESERVE_ALLOWLIST))
                    .addImages(listOf(a, b)).removeItem(b.id)
                model.selectedId = a.id
                model.onDraftEdited()
                assertTrue(model.prepareExport())
                model.startPreparedExport(f.context, port)
            }
            await { model.exportState.phase == ExportUiPhase.COMPLETED }
            assertEquals(1, model.exportState.published); assertTrue(model.canFinishSession)
            assertTrue(f.file.exists())
            val published = requireNotNull(output).copyOf()
            instrumentation.runOnMainSync { model.acknowledgePreviousExport(f.context) }
            await { model.exportState.phase == ExportUiPhase.IDLE }
            assertTrue(model.draft.workingSourceIds.isEmpty()); assertFalse(model.draft.canUndo)
            assertEquals(OutputFormat.PNG, model.draft.batch.format)
            assertEquals(MetadataPolicy.REMOVE_IDENTIFYING, model.draft.batch.metadata)
            assertFalse(f.file.exists())
            for (image in listOf(a, b)) assertFalse(File(f.context.cacheDir, "review-sources/${image.id}.source").exists())
            assertArrayEquals(published, output); assertEquals(0, deletions)
            assertTrue(File(f.context.filesDir, "export-jobs").listFiles()!!.isEmpty())
            assertTrue(File(f.context.filesDir, "export-jobs-retired").listFiles()!!.isEmpty())
            assertTrue(f.context.getSharedPreferences("export-reviewed", Context.MODE_PRIVATE).all.isEmpty())
        } finally { f.cleanup() }
    }
}
