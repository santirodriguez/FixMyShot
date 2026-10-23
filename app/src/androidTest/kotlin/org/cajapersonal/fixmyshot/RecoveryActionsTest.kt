package org.cajapersonal.fixmyshot

import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.export.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecoveryActionsTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private class Fixture(cache: File) {
        val root = File(cache, "resolution-" + UUID.randomUUID()).apply { mkdirs() }
        val token = "a".repeat(32)
        val folder = File(root, token).apply { mkdirs() }
        val batch = BatchExportJournal(File(folder, "batch.json"))
        val item = OutputJournal(File(folder, "item-0.json"))
        val uri = "content://fixmyshot.synthetic/owned"
        val output = OutputRecord("c".repeat(32), "item",
            "FixMyShot_20261022T000000Z_aaaaaaaa.png", "image/png",
            "b".repeat(64), OutputStage.INDETERMINATE, uri)
        init {
            batch.write(BatchCheckpoint(token, 1L, listOf(BatchItemCheckpoint(
                "item", "d".repeat(64), BatchItemPhase.SAVING))))
            item.write(output)
        }
    }

    @Test fun explicitConfirmationChangesOnlyReceiptsAndOpenHasReadGrant() {
        val f = Fixture(app.cacheDir)
        try {
            val actions = ExportRecoveryActions(f.root) { OutputRecoveryEvidence.MATCHED }
            val before = f.item.read()
            val intent = actions.open(f.token, "item")!!
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(f.uri, intent.data.toString())
            assertEquals("image/png", intent.type)
            assertEquals(intent.data, intent.clipData!!.getItemAt(0).uri)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(before, f.item.read())
            assertEquals(BatchItemPhase.SAVING, f.batch.read()!!.items.single().phase)
            assertTrue(actions.confirmPublished(f.token, "item"))
            assertEquals(before!!.copy(stage = OutputStage.PUBLISHED), f.item.read())
            assertEquals(BatchItemPhase.PUBLISHED, f.batch.read()!!.items.single().phase)
            assertTrue(BatchRetryPolicy.eligible(f.folder, f.batch.read()!!).isEmpty())
        } finally { f.root.deleteRecursively() }
    }

    @Test fun changedMissingPendingAndConflictingOutputsCannotBeAcceptedOrOpened() {
        val f = Fixture(app.cacheDir)
        try {
            val before = File(f.folder, "item-0.json").readBytes()
            for (evidence in OutputRecoveryEvidence.values().filter { it != OutputRecoveryEvidence.MATCHED }) {
                val actions = ExportRecoveryActions(f.root) { evidence }
                assertNull(actions.open(f.token, "item"))
                assertFalse(actions.confirmPublished(f.token, "item"))
                assertArrayEquals(before, File(f.folder, "item-0.json").readBytes())
            }
            val old = f.batch.read()!!
            f.batch.write(old.copy(items = listOf(old.items.single().copy(uri = "content://other/changed"))))
            val actions = ExportRecoveryActions(f.root) { OutputRecoveryEvidence.MATCHED }
            assertFalse(actions.confirmPublished(f.token, "item"))
            assertNull(actions.open(f.token, "item"))
            assertFalse(actions.confirmPublished("../escape", "item"))
            assertFalse(actions.confirmPublished(f.token, "another-item"))
        } finally { f.root.deleteRecursively() }
    }

    @Test fun liveExportLockRejectsConfirmationThenReleasedLockAllowsIt() {
        val f = Fixture(app.cacheDir)
        try {
            val actions = ExportRecoveryActions(f.root) { OutputRecoveryEvidence.MATCHED }
            RandomAccessFile(File(f.folder, ".active"), "rw").channel.use { channel ->
                val lock = channel.lock()
                try { assertFalse(actions.confirmPublished(f.token, "item")) }
                finally { lock.release() }
            }
            assertTrue(actions.confirmPublished(f.token, "item"))
            assertTrue(File(f.folder, ".active").exists())
        } finally { f.root.deleteRecursively() }
    }

    @Test fun realOwnedMediaStoreOutputRequiresVisibleMatchingBytesAndExplicitAcceptance() {
        if (Build.VERSION.SDK_INT < 29) return
        val f = Fixture(app.cacheDir)
        val port = MediaStore29Port(app.contentResolver)
        val uri = port.create(f.output.filename, f.output.mime)!!
        val pixels = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
        try {
            pixels.eraseColor(android.graphics.Color.BLUE)
            val bytes = java.io.ByteArrayOutputStream().use { output ->
                assertTrue(pixels.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
            port.openWrite(uri)!!.use { it.write(bytes) }
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            f.item.write(f.output.copy(uri = uri.toString(), digest = digest))
            val actions = ExportRecoveryActions(f.root, OutputRecoveryVerifier(app.contentResolver)::verify)
            assertFalse(actions.confirmPublished(f.token, "item"))
            assertTrue(port.publish(uri))
            val before = f.item.read()
            assertNotNull(actions.open(f.token, "item"))
            assertEquals(before, f.item.read())
            assertTrue(actions.confirmPublished(f.token, "item"))
            assertEquals(uri.toString(), f.batch.read()!!.items.single().uri)
            assertArrayEquals(bytes, port.openRead(uri)!!.use { it.readBytes() })
        } finally { pixels.recycle(); port.delete(uri); f.root.deleteRecursively() }
    }
}
