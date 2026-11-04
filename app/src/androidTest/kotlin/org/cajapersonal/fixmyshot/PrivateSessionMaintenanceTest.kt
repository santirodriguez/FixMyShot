package org.cajapersonal.fixmyshot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PrivateSessionMaintenanceTest {
    private class Fixture {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "private-cleanup-" + UUID.randomUUID()).apply { mkdirs() }
        val cache = File(root, "cache").apply { mkdirs() }
        val jobs = File(root, "jobs").apply { mkdirs() }
        val sourceId = "b".repeat(32)
        val token = "a".repeat(32)
        val folder = File(jobs, token).apply { mkdirs() }
        val source = File(File(cache, "review-sources").apply { mkdirs() }, "$sourceId.source")
            .apply { writeText("private synthetic bytes") }
        var reviewed = false
        var forgetSucceeds = true
        val forgotten = mutableSetOf<String>()
        val maintenance = PrivateSessionMaintenance(cache, jobs, forgetAcknowledged = {
            if (forgetSucceeds) { forgotten += it; true } else false
        }) { reviewed }
        val retiredFolder get() = File(root, "jobs-retired/$token")
        fun published(): OutputRecord {
            val uri = "content://synthetic/saved-output"
            val output = OutputRecord(token, sourceId, "FixMyShot_20260101T000000Z_1234abcd.png",
                "image/png", "d".repeat(64), OutputStage.PUBLISHED, uri)
            OutputJournal(File(folder, "item-0.json")).write(output)
            BatchExportJournal(File(folder, "batch.json")).write(BatchCheckpoint(token, 1,
                listOf(BatchItemCheckpoint(sourceId, "c".repeat(64), BatchItemPhase.PUBLISHED, uri))))
            return output
        }
        init {
            val image = DraftImage(sourceId, "content://synthetic/source", PixelSize(64, 64), false,
                contentSha256 = "c".repeat(64))
            val draft = DraftSession().addImages(listOf(image))
            ExportDraftJournal(File(folder, "draft.json")).write(draft)
            BatchExportJournal(File(folder, "batch.json")).write(BatchCheckpoint(token, draft.revision,
                listOf(BatchItemCheckpoint(sourceId, "c".repeat(64), BatchItemPhase.CANCELLED))))
        }
    }

    @Test fun unresolvedJobAndCurrentDraftProtectTheirSources() {
        val f = Fixture()
        try {
            assertTrue(f.maintenance.release(setOf(f.sourceId)))
            assertTrue(f.source.exists())
            assertFalse(f.maintenance.retirePublishedJob(f.token))
            f.reviewed = true
            assertTrue(f.maintenance.releaseReviewedDraft(f.token, setOf(f.sourceId)))
            assertFalse(File(f.folder, "draft.json").exists())
            assertTrue(f.source.exists())
            assertTrue(f.maintenance.release(setOf(f.sourceId)))
            assertFalse(f.source.exists())
        } finally { f.root.deleteRecursively() }
    }

    @Test fun reviewedCleanupRetiresOnlyConsistentPublishedReceiptsUnderTheJobLock() {
        val f = Fixture()
        try {
            val receipt = File(f.folder, "batch.json").readBytes()
            f.reviewed = true
            assertTrue(f.maintenance.releaseReviewedDraft(f.token, emptySet()))
            assertFalse(f.source.exists())
            assertFalse(File(f.folder, "draft.json").exists())
            assertTrue(File(f.folder, ".active").exists())
            assertArrayEquals(receipt, File(f.folder, "batch.json").readBytes())
            assertFalse(f.maintenance.retirePublishedJob(f.token)) // cancelled work retains its receipts
            val output = f.published()
            for (stage in listOf(OutputStage.INDETERMINATE, OutputStage.VALIDATED, OutputStage.PENDING)) {
                OutputJournal(File(f.folder, "item-0.json")).write(output.copy(stage = stage))
                assertFalse(f.maintenance.retirePublishedJob(f.token))
                assertTrue(f.folder.exists())
            }
            OutputJournal(File(f.folder, "item-0.json")).write(output.copy(uri = "content://synthetic/other"))
            assertFalse(f.maintenance.retirePublishedJob(f.token))
            OutputJournal(File(f.folder, "item-0.json")).write(output)
            RandomAccessFile(File(f.folder, ".active"), "rw").channel.use { channel ->
                val held = channel.lock()
                try { assertFalse(f.maintenance.retirePublishedJob(f.token)) }
                finally { held.release() }
            }
            assertTrue(File(f.folder, "batch.json").exists())
            val savedCopy = File(f.root, "saved-copy.png").apply { writeText("published synthetic bytes") }
            assertTrue(f.maintenance.retirePublishedJob(f.token))
            assertFalse(f.folder.exists()); assertFalse(f.retiredFolder.exists())
            assertEquals(setOf(f.token), f.forgotten)
            assertEquals("published synthetic bytes", savedCopy.readText())
        } finally { f.root.deleteRecursively() }
    }

    @Test fun unreadableRecoveryAndSymlinkDoNotAuthorizeDeletion() {
        val f = Fixture()
        try {
            File(f.folder, "batch.json").writeText("corrupt")
            assertFalse(f.maintenance.release(setOf(f.sourceId)))
            assertTrue(f.source.exists())
            f.reviewed = true
            val external = File(f.root, "outside").apply { writeText("preserve") }
            val id = "d".repeat(32)
            val link = File(f.source.parentFile, "$id.source")
            android.system.Os.symlink(external.path, link.path)
            assertFalse(f.maintenance.release(setOf(id)))
            assertEquals("preserve", external.readText())
            f.published()
            assertTrue(f.maintenance.releaseReviewedDraft(f.token, emptySet()))
            val unknown = File(f.folder, "unknown.txt").apply { writeText("keep") }
            assertFalse(f.maintenance.retirePublishedJob(f.token))
            assertEquals("keep", unknown.readText()); assertTrue(unknown.delete())
            val output = File(f.folder, "item-0.json")
            val outsideReceipt = File(f.root, "outside-receipt.json").apply { writeBytes(output.readBytes()) }
            val original = outsideReceipt.readBytes()
            assertTrue(output.delete())
            android.system.Os.symlink(outsideReceipt.path, output.path)
            assertFalse(f.maintenance.retirePublishedJob(f.token))
            assertArrayEquals(original, outsideReceipt.readBytes())
            assertTrue(File(f.folder, "batch.json").exists())
            assertTrue(f.forgotten.isEmpty())
        } finally { f.root.deleteRecursively() }
    }

    @Test fun startupAgeCleanupKeepsActiveAndUnresolvedSourcesAndRecentCandidates() {
        val f = Fixture()
        try {
            val now = System.currentTimeMillis()
            val old = now - 25L * 60 * 60 * 1000
            assertTrue(f.source.setLastModified(old))
            val abandoned = File(f.source.parentFile, "d".repeat(32) + ".source").apply {
                writeText("abandoned"); setLastModified(old)
            }
            val activeId = "e".repeat(32)
            val active = File(f.source.parentFile, "$activeId.source").apply {
                writeText("active"); setLastModified(old)
            }
            val candidates = File(f.cache, "batch-candidates").apply { mkdirs() }
            val stale = File(candidates, "old.png").apply { writeText("old"); setLastModified(old) }
            val recent = File(candidates, "recent.png").apply { writeText("recent") }
            f.maintenance.pruneExpired(setOf(activeId), now)
            assertTrue(f.source.exists()); assertTrue(active.exists()); assertTrue(recent.exists())
            assertFalse(abandoned.exists()); assertFalse(stale.exists())
            f.reviewed = true
            f.published()
            assertTrue(f.maintenance.releaseReviewedDraft(f.token, setOf(activeId)))
            val receipt = File(f.folder, "item-0.json").readBytes()
            f.forgetSucceeds = false
            assertFalse(f.maintenance.retirePublishedJob(f.token))
            assertFalse(f.folder.exists()); assertTrue(f.retiredFolder.exists())
            assertTrue(f.forgotten.isEmpty())
            // Model a retained receipt after interrupted deletion, then retry on a fresh maintenance instance.
            File(f.retiredFolder, "item-0.json").writeBytes(receipt)
            f.forgetSucceeds = true
            PrivateSessionMaintenance(f.cache, f.jobs, forgetAcknowledged = {
                f.forgotten += it; true
            }) { f.reviewed }.pruneExpired(setOf(activeId), now)
            assertFalse(f.retiredFolder.exists()); assertEquals(setOf(f.token), f.forgotten)
            assertTrue(active.exists()); assertTrue(recent.exists())
        } finally { f.root.deleteRecursively() }
    }
}
