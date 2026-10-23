package org.cajapersonal.fixmyshot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
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
        val maintenance = PrivateSessionMaintenance(cache, jobs) { reviewed }
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
            f.reviewed = true
            assertTrue(f.maintenance.releaseReviewedDraft(f.token, setOf(f.sourceId)))
            assertFalse(File(f.folder, "draft.json").exists())
            assertTrue(f.source.exists())
            assertTrue(f.maintenance.release(setOf(f.sourceId)))
            assertFalse(f.source.exists())
        } finally { f.root.deleteRecursively() }
    }

    @Test fun acknowledgedRecoveryRemovesPrivateDraftAndSourcesButKeepsReceipts() {
        val f = Fixture()
        try {
            val receipt = File(f.folder, "batch.json").readBytes()
            f.reviewed = true
            assertTrue(f.maintenance.releaseReviewedDraft(f.token, emptySet()))
            assertFalse(f.source.exists())
            assertFalse(File(f.folder, "draft.json").exists())
            assertTrue(File(f.folder, ".active").exists())
            assertArrayEquals(receipt, File(f.folder, "batch.json").readBytes())
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
        } finally { f.root.deleteRecursively() }
    }
}
