package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.*
import org.cajapersonal.fixmyshot.review.PrivateExecutionResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Synthetic transaction faults. A single API29+ case checks real MediaStore and deletes its own output. */
@RunWith(AndroidJUnit4::class)
class TransactionalOutputSaverTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()

    private class MemoryPort : OwnedOutputPort {
        val uri = Uri.parse("content://fixmyshot-test/owned/1")
        var bytes = ByteArray(0)
        var created = false
        var deleted = false
        var published = false
        var failWrite = false
        override var visibleBeforeCommit: Boolean = false
        var onOpenWrite: (() -> Unit)? = null
        var failPublish = false
        var corruptRead = false
        override fun create(name: String, mime: String): Uri {
            created = true
            return uri
        }
        override fun openWrite(uri: Uri): OutputStream = (object : ByteArrayOutputStream() {
            override fun write(b: ByteArray, off: Int, len: Int) {
                if (failWrite) throw IOException("Synthetic provider write failure")
                super.write(b, off, len)
            }
            override fun close() {
                bytes = toByteArray()
                super.close()
            }
        }).also { onOpenWrite?.invoke() }
        override fun openRead(uri: Uri): InputStream =
            ByteArrayInputStream(if (corruptRead) bytes + byteArrayOf(9) else bytes)
        override fun publish(uri: Uri): Boolean {
            published = !failPublish
            return !failPublish
        }
        override fun delete(uri: Uri): Boolean {
            deleted = true
            bytes = ByteArray(0)
            return true
        }
    }

    private data class Input(
        val recipe: PrivateOutputRecipe,
        val ready: PrivateExecutionResult.Ready,
        val directory: File,
    ) {
        fun cleanup() { directory.deleteRecursively() }
    }

    private fun prepared(): Input {
        val directory = File(context.cacheDir, "save-test-" + UUID.randomUUID()).apply { mkdirs() }
        val size = PixelSize(64, 64)
        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1))
            .addImages(listOf(DraftImage("a", "test", size, false,
                contentSha256 = "1".repeat(64))))
        val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setHasAlpha(false)
            bitmap.eraseColor(Color.BLUE)
            val encoded = LosslessEncoder(directory, heap).encode(bitmap, recipe.request)
            assertTrue(encoded.toString(), encoded is EncodeResult.Encoded)
            encoded as EncodeResult.Encoded
            return Input(recipe, PrivateExecutionResult.Ready(encoded.file, encoded.route,
                recipe.sourceSha256, size, Color.BLUE), directory)
        } finally { bitmap.recycle() }
    }

    private fun journal(): OutputJournal = OutputJournal(File(context.cacheDir,
        "save-journal-" + UUID.randomUUID() + ".json"))

    @Test fun validatesCopyAndPublishesOnceWithAtomicCheckpoint() {
        val input = prepared()
        val fake = MemoryPort()
        val journal = journal()
        try {
            val saved = TransactionalOutputSaver(context.cacheDir, fake, journal)
                .save(input.recipe, input.ready)
            assertTrue(saved.toString(), saved is SaveResult.Published)
            assertTrue(fake.created && fake.published && !fake.deleted)
            assertArrayEquals(input.ready.file.readBytes(), fake.bytes)
            assertEquals(OutputStage.PUBLISHED, journal.read()?.stage)
            assertEquals(fake.uri.toString(), journal.read()?.uri)
            assertEquals(SaveFailure.PREVIOUS_JOB_UNRESOLVED,
                (TransactionalOutputSaver(context.cacheDir, fake, journal)
                    .save(input.recipe, input.ready) as SaveResult.Failed).reason)
        } finally { journal.clear(); input.cleanup() }
    }

    @Test fun visibleSafStyleOutputHasExplicitJournalStateBeforeCopy() {
        val input = prepared()
        val fake = MemoryPort().apply { visibleBeforeCommit = true }
        val journal = journal()
        try {
            var observed = false
            fake.onOpenWrite = {
                observed = true
                assertEquals(OutputStage.VISIBLE_WRITING, journal.read()?.stage)
            }
            val result = TransactionalOutputSaver(context.cacheDir, fake, journal)
                .save(input.recipe, input.ready)
            assertTrue(result.toString(), result is SaveResult.Published)
            assertTrue(observed && fake.created && !fake.deleted)
            assertEquals(OutputStage.PUBLISHED, journal.read()?.stage)
        } finally { journal.clear(); input.cleanup() }
    }

    @Test fun safPortRejectsNonTreeUrisWithoutGrantingFileAccess() {
        assertThrows(IllegalArgumentException::class.java) {
            SafTreeOutputPort(context.contentResolver,
                Uri.parse("content://example.invalid/document/42"))
        }
    }

    @Test fun unexpectedSafCreatedUriIsRejectedWithoutTouchingIt() {
        val chosen = Uri.parse("content://example.invalid/tree/chosen")
        val unexpected = listOf(
            Uri.parse("content://example.invalid/tree/other/document/file"),
            Uri.parse("content://other.invalid/tree/chosen/document/file"),
            Uri.parse("content://example.invalid/document/file"),
        )
        for (uri in unexpected) {
            var cleanupCalls = 0
            val port = SafTreeOutputPort(context.contentResolver, chosen,
                createDocument = { _, _, _ -> uri },
                deleteDocument = { cleanupCalls++; true })
            assertThrows(IOException::class.java) {
                port.create("FixMyShot_20261022T120000Z_1234abcd.png", "image/png")
            }
            assertNull(port.openRead(uri))
            assertNull(port.openWrite(uri))
            assertFalse(port.publish(uri))
            assertFalse(port.delete(uri))
            assertEquals("Unexpected provider URI must never be deleted", 0, cleanupCalls)
        }
        for (cleanupSucceeds in listOf(false, true)) {
            var cleanupCalls = 0
            val port = SafTreeOutputPort(context.contentResolver, chosen,
                createDocument = { _, _, _ -> Uri.parse("content://example.invalid/tree/chosen/document/created") },
                deleteDocument = { cleanupCalls++; cleanupSucceeds })
            // This synthetic provider cannot confirm the requested name. Null is safe only after cleanup.
            if (cleanupSucceeds) assertNull(port.create("FixMyShot_20261022T120000Z_1234abcd.png", "image/png"))
            else assertThrows(IOException::class.java) {
                port.create("FixMyShot_20261022T120000Z_1234abcd.png", "image/png")
            }
            assertEquals(1, cleanupCalls)
        }
    }

    @Test fun invalidCandidateNeverCreatesAnExternalItem() {
        val input = prepared()
        val fake = MemoryPort()
        val journal = journal()
        try {
            input.ready.file.appendBytes(byteArrayOf(1))
            assertEquals(SaveResult.Failed(SaveFailure.INVALID_CANDIDATE),
                TransactionalOutputSaver(context.cacheDir, fake, journal)
                    .save(input.recipe, input.ready))
            assertFalse(fake.created)
            assertNull(journal.read())
        } finally { journal.clear(); input.cleanup() }
    }

    @Test fun cancellationBeforeInsertCreatesNothing() {
        val input = prepared()
        val fake = MemoryPort()
        val journal = journal()
        try {
            assertEquals(SaveResult.Failed(SaveFailure.CANCELLED),
                TransactionalOutputSaver(context.cacheDir, fake, journal)
                    .save(input.recipe, input.ready, AtomicBoolean(true)))
            assertFalse(fake.created)
        } finally { journal.clear(); input.cleanup() }
    }

    @Test fun providerWriteFailureRemovesItsOwnPendingItem() {
        val input = prepared()
        val fake = MemoryPort().apply { failWrite = true }
        val journal = journal()
        try {
            assertEquals(SaveResult.Failed(SaveFailure.PROVIDER_ERROR),
                TransactionalOutputSaver(context.cacheDir, fake, journal)
                    .save(input.recipe, input.ready))
            assertTrue(fake.deleted && !fake.published)
            assertEquals(OutputStage.FAILED, journal.read()?.stage)
        } finally { journal.clear(); input.cleanup() }
    }

    @Test fun providerExceptionsRespectCreationCleanupAndPublicationBoundaries() {
        val input = prepared()
        try {
            // A create that allocates and then throws must not become a retryable no-output failure.
            for ((index, failure) in listOf(IOException("Synthetic I/O"), IllegalStateException("Synthetic runtime")).withIndex()) {
                val fake = MemoryPort()
                val port = object : OwnedOutputPort by fake {
                    override fun create(name: String, mime: String): Uri? {
                        fake.create(name, mime)
                        throw failure
                    }
                }
                val folder = File(input.directory, "create-$index")
                val journal = OutputJournal(File(folder, "item-0.json"))
                val result = TransactionalOutputSaver(context.cacheDir, port, journal).save(input.recipe, input.ready)
                assertTrue(result is SaveResult.Indeterminate)
                assertNull((result as SaveResult.Indeterminate).uri)
                assertTrue(fake.created && !fake.deleted && !fake.published)
                assertEquals(OutputStage.RESERVED, OutputJournal(File(folder, "item-0.json")).read()!!.stage)
                val item = BatchItemCheckpoint(input.recipe.sourceId, input.recipe.sourceSha256, BatchItemPhase.SAVING)
                val batch = BatchCheckpoint("a".repeat(32), input.recipe.sessionRevision, listOf(item))
                val restored = BatchRecoveryReconciliation.inspect(folder, batch)
                assertEquals(BatchItemPhase.NEEDS_REVIEW, restored.items.single().phase)
                assertEquals("OUTPUT_CREATION_UNCERTAIN", restored.items.single().reason)
                assertTrue(BatchRetryPolicy.eligible(folder, restored).isEmpty())
                assertFalse(BatchRetryPolicy.safe(item.copy(phase = BatchItemPhase.FAILED), journal.read()))
            }
            // With a confirmed URI, an unexpected write failure follows the ordinary owned cleanup.
            for (cleanupFails in listOf(false, true)) {
                val fake = MemoryPort().apply { onOpenWrite = { throw IllegalStateException("Synthetic write") } }
                val port = object : OwnedOutputPort by fake {
                    override fun delete(uri: Uri): Boolean {
                        if (cleanupFails) throw IllegalStateException("Synthetic cleanup")
                        return fake.delete(uri)
                    }
                }
                val journal = OutputJournal(File(input.directory, "write-$cleanupFails.json"))
                val result = TransactionalOutputSaver(context.cacheDir, port, journal).save(input.recipe, input.ready)
                assertFalse(fake.published)
                if (cleanupFails) {
                    assertTrue(result is SaveResult.Indeterminate)
                    assertEquals(OutputStage.INDETERMINATE, journal.read()!!.stage)
                    assertEquals(fake.uri.toString(), journal.read()!!.uri)
                    assertFalse(fake.deleted)
                } else {
                    assertEquals(SaveResult.Failed(SaveFailure.PROVIDER_ERROR), result)
                    assertEquals(OutputStage.FAILED, journal.read()!!.stage)
                    assertTrue(fake.deleted)
                }
            }
            val fake = MemoryPort()
            val port = object : OwnedOutputPort by fake {
                override fun publish(uri: Uri): Boolean {
                    fake.publish(uri)
                    throw IllegalStateException("Synthetic post-publication failure")
                }
            }
            val journal = OutputJournal(File(input.directory, "publish.json"))
            val result = TransactionalOutputSaver(context.cacheDir, port, journal).save(input.recipe, input.ready)
            assertTrue(result is SaveResult.Indeterminate)
            assertTrue(fake.published && !fake.deleted)
            assertArrayEquals(input.ready.file.readBytes(), fake.bytes)
            assertEquals(OutputStage.INDETERMINATE, journal.read()!!.stage)
            assertEquals(fake.uri.toString(), journal.read()!!.uri)
            // A known refusal still proves no output and may remain an ordinary failure.
            val refusing = object : OwnedOutputPort by fake {
                override fun create(name: String, mime: String): Uri? = null
            }
            assertEquals(SaveResult.Failed(SaveFailure.PROVIDER_ERROR),
                TransactionalOutputSaver(context.cacheDir, refusing,
                    OutputJournal(File(input.directory, "refusal.json"))).save(input.recipe, input.ready))
            assertTrue(fake.published && !fake.deleted)
            assertArrayEquals(input.ready.file.readBytes(), fake.bytes)
        } finally { input.cleanup() }
    }

    @Test fun providerReadbackMismatchDeletesOnlyTheUnpublishedOwnedItem() {
        val input = prepared()
        val fake = MemoryPort().apply { corruptRead = true }
        val journal = journal()
        try {
            assertEquals(SaveResult.Failed(SaveFailure.PROVIDER_ERROR),
                TransactionalOutputSaver(context.cacheDir, fake, journal)
                    .save(input.recipe, input.ready))
            assertTrue(fake.deleted)
            assertFalse(fake.published)
            assertEquals(OutputStage.FAILED, journal.read()?.stage)
        } finally { journal.clear(); input.cleanup() }
    }

    @Test fun unconfirmedPublishBecomesIndeterminateRatherThanDuplicate() {
        val input = prepared()
        val fake = MemoryPort().apply { failPublish = true }
        val journal = journal()
        try {
            val result = TransactionalOutputSaver(context.cacheDir, fake, journal)
                .save(input.recipe, input.ready)
            assertTrue(result is SaveResult.Indeterminate)
            assertFalse(fake.deleted)
            assertEquals(OutputStage.INDETERMINATE, journal.read()?.stage)
        } finally { journal.clear(); input.cleanup() }
    }

    @Test fun actualApi29MediaStoreWritesToPicturesAndOwnOutputIsDeleted() {
        if (Build.VERSION.SDK_INT < 29) return // API26 does not auto-write without scoped permission.
        val input = prepared()
        val journal = journal()
        try {
            Api29MediaStoreOutputCheck.verify(context, input.recipe, input.ready, journal)
        } finally {
            journal.clear()
            input.cleanup()
        }
    }
}
