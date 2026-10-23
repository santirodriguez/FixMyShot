package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.content.Intent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.MediaStore29Port
import org.cajapersonal.fixmyshot.export.OwnedOutputPort
import org.cajapersonal.fixmyshot.export.BatchItemPhase
import org.cajapersonal.fixmyshot.review.ExportUiPhase
import org.cajapersonal.fixmyshot.review.ReviewExportState
import org.cajapersonal.fixmyshot.review.ReviewViewModel
import org.cajapersonal.fixmyshot.review.ReviewSources
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Full staged source -> immutable batch -> PNG encode -> verified destination -> UI result. */
@RunWith(AndroidJUnit4::class)
class ReviewExportFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext

    private class Owner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
        val model get() = ViewModelProvider(this)[ReviewViewModel::class.java]
    }

    private class MemoryPort : OwnedOutputPort {
        val data = ConcurrentHashMap<String, ByteArray>()
        var inserted = 0
        var published = 0
        override fun create(name: String, mime: String): Uri {
            val id = ++inserted
            return Uri.parse("content://fixmyshot-test/export/$id")
        }
        override fun openWrite(uri: Uri): OutputStream = object : ByteArrayOutputStream() {
            override fun close() {
                data[uri.toString()] = toByteArray()
                super.close()
            }
        }
        override fun openRead(uri: Uri): InputStream? =
            data[uri.toString()]?.let(::ByteArrayInputStream)
        override fun publish(uri: Uri): Boolean {
            if (!data.containsKey(uri.toString())) return false
            published++
            return true
        }
        override fun delete(uri: Uri): Boolean = data.remove(uri.toString()) != null
    }

    private class RecordingPort(private val delegate: OwnedOutputPort,
        private val failCreateNumber: Int = 0,
        private val leaveFirstPending: Boolean = false) : OwnedOutputPort by delegate {
        val created = mutableListOf<Uri>()
        var createCalls = 0
        private var publishCalls = 0
        override fun create(name: String, mime: String): Uri? {
            // This injected refusal occurs before calling the real provider: no output can remain.
            if (++createCalls == failCreateNumber) return null
            return delegate.create(name, mime)?.also { created += it }
        }
        override fun publish(uri: Uri): Boolean =
            if (++publishCalls == 1 && leaveFirstPending) false else delegate.publish(uri)
        fun deleteOwnOutputs() { created.forEach { runCatching { delegate.delete(it) } } }
    }

    private fun stageSynthetic(color: Int): Pair<DraftImage, File> {
        val root = File(app.cacheDir, "review-sources").apply { mkdirs() }
        val id = UUID.randomUUID().toString().replace("-", "")
        val file = File(root, "$id.source")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setHasAlpha(false)
            bitmap.eraseColor(color)
            file.outputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally { bitmap.recycle() }
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return DraftImage(id, "test-$id", PixelSize(64, 64), false,
            contentSha256 = digest) to file
    }

    private fun draftOf(vararg images: DraftImage) =
        DraftSession(destination = DestinationChoice.Custom.of(1, 1),
            batch = EditSettings(background = BackgroundChoice.White))
            .addImages(images.toList())

    private fun checkEmptyExportHistory(model: ReviewViewModel) {
        instrumentation.runOnMainSync { model.checkPastExports(app) }
        val deadline = System.currentTimeMillis() + 10_000L
        while (model.exportState.phase == ExportUiPhase.CHECKING_HISTORY &&
            System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            Thread.sleep(40)
        }
        assertEquals("An existing output needs explicit review", ExportUiPhase.IDLE,
            model.exportState.phase)
    }

    private fun waitForResult(model: ReviewViewModel): ReviewExportState {
        val deadline = System.currentTimeMillis() + 130_000L
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            val state = model.exportState
            if (state.phase !in setOf(ExportUiPhase.CHOOSING_FOLDER,
                    ExportUiPhase.RUNNING, ExportUiPhase.IDLE)) return state
            Thread.sleep(90)
        }
        fail("Export did not finish within the instrumentation limit")
        return model.exportState
    }

    private fun waitForOutputCheck(model: ReviewViewModel): ReviewExportState {
        val deadline = System.currentTimeMillis() + 15_000L
        while (model.exportState.outputBusy && System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync(); Thread.sleep(40)
        }
        assertFalse("Output action did not finish", model.exportState.outputBusy)
        return model.exportState
    }

    private inline fun withCleanup(block: (MutableList<File>, Owner) -> Unit) {
        val previously = File(app.filesDir, "export-jobs").listFiles()
            ?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        val owner = Owner()
        val files = mutableListOf<File>()
        try { block(files, owner) } finally {
            owner.viewModelStore.clear()
            files.forEach { it.delete() }
            File(app.filesDir, "export-jobs").listFiles()
                ?.filter { it.name !in previously }?.forEach { it.deleteRecursively() }
        }
    }

    @Test fun stagedImagesProduceVerifiedPngAndNoAutomaticSecondPublication() = withCleanup { files, owner ->
        val (a, af) = stageSynthetic(Color.BLUE)
        val (b, bf) = stageSynthetic(Color.RED)
        files += af; files += bf
        val port = MemoryPort()
        val model = owner.model
        checkEmptyExportHistory(model)
        instrumentation.runOnMainSync {
            model.draft = draftOf(a, b)
            assertTrue(model.prepareExport())
            model.startPreparedExport(app, port)
        }
        val done = waitForResult(model)
        assertEquals(ExportUiPhase.COMPLETED, done.phase)
        assertEquals(2, done.published)
        assertEquals(2, port.inserted)
        assertEquals(2, port.published)
        assertEquals(0, done.failed + done.needsReview)
        port.data.values.forEach { bytes ->
            val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            try {
                assertEquals(64, bitmap.width)
                assertEquals(64, bitmap.height)
                assertTrue(bitmap.getPixel(32, 32) in setOf(Color.BLUE, Color.RED))
            } finally { bitmap.recycle() }
        }
        instrumentation.runOnMainSync {
            assertFalse("Same revision must require explicit reset before retry", model.prepareExport())
        }
        assertEquals(2, port.published)
    }

    @Test fun rejectedWholeBatchCannotCreateAFile() = withCleanup { files, owner ->
        val (a, af) = stageSynthetic(Color.GREEN)
        files += af
        val missing = a.copy(id = UUID.randomUUID().toString().replace("-", ""),
            sourceToken = "test-missing", contentSha256 = null)
        val model = owner.model
        checkEmptyExportHistory(model)
        instrumentation.runOnMainSync {
            model.draft = draftOf(a, missing)
            assertFalse(model.prepareExport())
            assertEquals(ExportUiPhase.BLOCKED, model.exportState.phase)
            assertEquals(1, model.exportState.blocked)
        }
    }

    @Test fun restartedViewModelRestoresAndRetriesOnlyTheFailedMediaStoreItem() = withCleanup { files, owner ->
        if (Build.VERSION.SDK_INT < 29) return@withCleanup
        val (a, af) = stageSynthetic(Color.BLUE)
        val (b, bf) = stageSynthetic(Color.GREEN)
        files += af; files += bf
        val port = RecordingPort(MediaStore29Port(app.contentResolver), failCreateNumber = 2)
        val restoredOwner = Owner()
        try {
            val first = owner.model
            checkEmptyExportHistory(first)
            instrumentation.runOnMainSync {
                first.draft = draftOf(a, b)
                assertTrue(first.prepareExport())
                first.startPreparedExport(app, port)
            }
            val before = waitForResult(first)
            assertEquals(1, before.published)
            assertEquals(1, before.failed)
            assertTrue(before.retryReady)
            val keptUri = before.items.first().uri
            instrumentation.runOnMainSync {
                first.selectAllShareItems()
                assertEquals(setOf(a.id), first.selectedShareIds)
                first.toggleShareItem(b.id)
                assertEquals(setOf(a.id), first.selectedShareIds)
                first.clearShareSelection()
                assertTrue(first.selectedShareIds.isEmpty())
                assertTrue(first.prepareRetryExport())
                first.destinationCancelled()
                assertEquals(before.items, first.exportState.items)
                assertTrue(first.exportState.retryReady)
                owner.viewModelStore.clear()
            }
            val restored = restoredOwner.model
            instrumentation.runOnMainSync { restored.checkPastExports(app) }
            val deadline = System.currentTimeMillis() + 15_000L
            while (restored.exportState.phase == ExportUiPhase.CHECKING_HISTORY && System.currentTimeMillis() < deadline) {
                instrumentation.waitForIdleSync(); Thread.sleep(50)
            }
            assertEquals(ExportUiPhase.NEEDS_REVIEW, restored.exportState.phase)
            assertTrue(restored.draft.images.isEmpty())
            assertTrue(restored.exportState.canRestoreDraft)
            assertEquals(1, restored.exportState.retryableIds.size)
            instrumentation.runOnMainSync {
                restored.restorePreviousDraft()
                assertEquals(listOf(a.id, b.id), restored.draft.images.map { it.id })
                assertEquals(DestinationChoice.Custom.of(1, 1), restored.draft.destination)
                assertTrue(restored.prepareRetryExport())
                restored.startPreparedExport(app, port)
            }
            val done = waitForResult(restored)
            assertEquals(2, done.published)
            assertEquals(0, done.failed + done.needsReview)
            assertEquals(keptUri, done.items.first().uri)
            assertEquals(3, port.createCalls) // one refusal, two successful creations in total
            assertEquals(2, port.created.size)
            assertTrue(done.retryableIds.isEmpty())
            assertTrue(ReviewSources(app).matchesFingerprint(a.id, requireNotNull(a.contentSha256)))
            assertTrue(ReviewSources(app).matchesFingerprint(b.id, requireNotNull(b.contentSha256)))
        } finally {
            instrumentation.runOnMainSync { restoredOwner.viewModelStore.clear() }
            port.deleteOwnOutputs()
        }
    }

    @Test fun explicitPendingRemovalEnablesOnlyADeliberateRetryAndPreservesTheSource() = withCleanup { files, owner ->
        if (Build.VERSION.SDK_INT < 29) return@withCleanup
        val (image, source) = stageSynthetic(Color.YELLOW)
        files += source
        val port = RecordingPort(MediaStore29Port(app.contentResolver), leaveFirstPending = true)
        try {
            val model = owner.model
            checkEmptyExportHistory(model)
            instrumentation.runOnMainSync {
                model.draft = draftOf(image)
                assertTrue(model.prepareExport())
                model.startPreparedExport(app, port)
            }
            assertEquals(ExportUiPhase.NEEDS_REVIEW, waitForResult(model).phase)
            val pending = port.created.single()
            assertTrue(model.exportState.retryableIds.isEmpty())
            instrumentation.runOnMainSync {
                model.removePendingOutput(app, image.id) // no positive read-only pending check yet
                assertFalse(model.exportState.outputBusy)
                model.refreshOutputs(app)
            }
            val checked = waitForOutputCheck(model)
            assertEquals("OUTPUT_PENDING", checked.items.single().reason)
            assertEquals(1, port.createCalls)
            instrumentation.runOnMainSync { model.removePendingOutput(app, image.id) }
            val removed = waitForOutputCheck(model)
            assertEquals(ExportUiPhase.COMPLETED, removed.phase)
            assertFalse(removed.outputError)
            assertEquals(BatchItemPhase.FAILED, removed.items.single().phase)
            assertEquals("UNFINISHED_REMOVED", removed.items.single().reason)
            assertNull(removed.items.single().uri)
            assertEquals(setOf(image.id), removed.retryableIds)
            assertTrue(removed.retryReady)
            app.contentResolver.query(pending, arrayOf(android.provider.BaseColumns._ID), null, null, null).use {
                assertNotNull(it); assertFalse(it!!.moveToFirst())
            }
            port.created.remove(pending) // the explicit action already deleted this owned fixture
            assertEquals(1, port.createCalls) // no automatic retry/export
            instrumentation.runOnMainSync {
                assertTrue(model.prepareRetryExport())
                model.startPreparedExport(app, port)
            }
            val done = waitForResult(model)
            assertEquals(1, done.published)
            assertEquals(0, done.failed + done.needsReview)
            assertEquals(2, port.createCalls)
            assertNotEquals(pending.toString(), done.items.single().uri)
            assertEquals(port.created.single().toString(), done.items.single().uri)
            assertTrue(ReviewSources(app).matchesFingerprint(image.id, requireNotNull(image.contentSha256)))
        } finally { port.deleteOwnOutputs() }
    }

    @Test fun api29WritesAndVerifiesSelectedSingleAndMixedSharesThenRemovesOwnOutputs() = withCleanup { files, owner ->
        if (Build.VERSION.SDK_INT < 29) return@withCleanup
        val (image, file) = stageSynthetic(Color.MAGENTA)
        val (second, secondFile) = stageSynthetic(Color.CYAN)
        files += file; files += secondFile
        val port = RecordingPort(MediaStore29Port(app.contentResolver))
        try {
            val model = owner.model
            checkEmptyExportHistory(model)
            instrumentation.runOnMainSync {
                model.draft = draftOf(image, second.copy(overrides = ItemOverrides(
                    format = ItemValue.Custom(OutputFormat.WEBP_LOSSLESS))))
                assertTrue(model.prepareExport())
                model.startPreparedExport(app, port)
            }
            val state = waitForResult(model)
            assertEquals(ExportUiPhase.COMPLETED, state.phase)
            assertEquals(2, state.published)
            assertEquals(2, port.created.size)
            port.created.forEach { uri ->
                app.contentResolver.query(uri,
                    arrayOf(MediaStore.MediaColumns.IS_PENDING,
                        MediaStore.MediaColumns.RELATIVE_PATH), null, null, null).use { cursor ->
                    val row = requireNotNull(cursor)
                    assertTrue(row.moveToFirst())
                    assertEquals(0, row.getInt(0))
                    assertEquals("Pictures/FixMyShot", row.getString(1).trimEnd('/'))
                }
            }
            val shared = AtomicReference<Intent?>()
            val signal = CountDownLatch(1)
            instrumentation.runOnMainSync {
                model.selectAllShareItems()
                assertEquals(setOf(image.id, second.id), model.selectedShareIds)
                model.clearShareSelection()
                assertTrue(model.selectedShareIds.isEmpty())
                model.toggleShareItem(image.id)
                assertEquals(1, model.selectedShareIds.size)
                model.shareSelected(app) { intent ->
                    shared.set(intent)
                    signal.countDown()
                }
            }
            assertTrue("Verified share intent not ready", signal.await(40, TimeUnit.SECONDS))
            val send = requireNotNull(shared.get())
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("image/png", send.type)
            assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(1, send.clipData!!.itemCount)
            assertEquals(port.created.first(), send.clipData!!.getItemAt(0).uri)

            val mixed = AtomicReference<Intent?>()
            val mixedSignal = CountDownLatch(1)
            instrumentation.runOnMainSync {
                model.selectAllShareItems()
                model.shareSelected(app) { intent ->
                    mixed.set(intent)
                    mixedSignal.countDown()
                }
                assertTrue(model.shareBusy)
                model.clearShareSelection()
                model.toggleShareItem(image.id)
                assertEquals(setOf(image.id, second.id), model.selectedShareIds)
            }
            assertTrue("Mixed share intent not ready", mixedSignal.await(40, TimeUnit.SECONDS))
            val sendMixed = requireNotNull(mixed.get())
            assertEquals(Intent.ACTION_SEND_MULTIPLE, sendMixed.action)
            assertEquals("image/*", sendMixed.type)
            assertTrue(sendMixed.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            val clip = requireNotNull(sendMixed.clipData)
            assertEquals(2, clip.itemCount)
            assertEquals(port.created, (0 until clip.itemCount).map { clip.getItemAt(it).uri })
            assertEquals(2, port.createCalls)

            // Only mutate our own synthetic output. A stale receipt must refuse the entire share.
            requireNotNull(app.contentResolver.openOutputStream(port.created.last(), "wt")).use {
                it.write(byteArrayOf(1, 2, 3))
            }
            val rejected = AtomicReference<Intent?>(sendMixed)
            val rejectedSignal = CountDownLatch(1)
            instrumentation.runOnMainSync {
                model.shareSelected(app) { intent ->
                    rejected.set(intent)
                    rejectedSignal.countDown()
                }
            }
            assertTrue("Changed output not checked", rejectedSignal.await(40, TimeUnit.SECONDS))
            assertNull(rejected.get())
            assertTrue(model.shareError)
            assertEquals(setOf(image.id, second.id), model.selectedShareIds)
            assertEquals(2, port.createCalls)
            assertTrue(ReviewSources(app).matchesFingerprint(image.id, requireNotNull(image.contentSha256)))
            assertTrue(ReviewSources(app).matchesFingerprint(second.id, requireNotNull(second.contentSha256)))
        } finally { port.deleteOwnOutputs() }
    }
}

