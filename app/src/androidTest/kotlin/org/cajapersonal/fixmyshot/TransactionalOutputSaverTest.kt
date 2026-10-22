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
        var failPublish = false
        var corruptRead = false
        override fun create(name: String, mime: String): Uri {
            created = true
            return uri
        }
        override fun openWrite(uri: Uri): OutputStream = object : ByteArrayOutputStream() {
            override fun write(b: ByteArray, off: Int, len: Int) {
                if (failWrite) throw IOException("Synthetic provider write failure")
                super.write(b, off, len)
            }
            override fun close() {
                bytes = toByteArray()
                super.close()
            }
        }
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
