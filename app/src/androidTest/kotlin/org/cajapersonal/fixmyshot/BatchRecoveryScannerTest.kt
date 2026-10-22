package org.cajapersonal.fixmyshot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.export.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BatchRecoveryScannerTest {
    private val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    private fun token() = UUID.randomUUID().toString().replace("-", "")

    @Test fun previouslyRecordedJobIsVisibleAfterRestart() {
        val root = File(cache, "recovery-" + token()).apply { mkdirs() }
        try {
            val id = token()
            val folder = File(root, id).apply { mkdirs() }
            val snapshot = BatchCheckpoint(id, 1L,
                listOf(BatchItemCheckpoint("item", "a".repeat(64),
                    BatchItemPhase.PUBLISHED, "content://synthetic/saved")))
            BatchExportJournal(File(folder, "batch.json")).write(snapshot)
            val current = requireNotNull(BatchRecoveryScanner(root).inspect())
            assertEquals(id, current.token)
            assertEquals(snapshot, current.snapshot)
            assertEquals(1, current.pendingCount)
        } finally { root.deleteRecursively() }
    }

    @Test fun explicitReviewedStateAdvancesOneBatchAtATime() {
        val root = File(cache, "recovery-" + token()).apply { mkdirs() }
        try {
            repeat(2) { File(root, token()).mkdirs() }
            val reviewed = mutableSetOf<String>()
            val scanner = BatchRecoveryScanner(root) { it in reviewed }
            val first = requireNotNull(scanner.inspect())
            assertEquals(2, first.pendingCount)
            reviewed += requireNotNull(first.token)
            val second = requireNotNull(scanner.inspect())
            assertEquals(1, second.pendingCount)
            assertNotEquals(first.token, second.token)
            reviewed += requireNotNull(second.token)
            assertNull(scanner.inspect())
        } finally { root.deleteRecursively() }
    }
}
