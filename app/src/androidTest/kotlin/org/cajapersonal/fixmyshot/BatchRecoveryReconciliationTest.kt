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
class BatchRecoveryReconciliationTest {
    private val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    private val uri = "content://fixmyshot.synthetic/saved"
    private val digest = "b".repeat(64)

    @Test fun inconsistentPublishedRecordMustBeReviewed() {
        val folder = File(cache, "reconcile-" + UUID.randomUUID()).apply { mkdirs() }
        try {
            val snapshot = BatchCheckpoint("a".repeat(32), 1L,
                listOf(BatchItemCheckpoint("item", digest, BatchItemPhase.PUBLISHED, uri)))
            val missing = BatchRecoveryReconciliation.inspect(folder, snapshot)
            assertEquals(BatchItemPhase.NEEDS_REVIEW, missing.items.single().phase)

            OutputJournal(File(folder, "item-0.json")).write(
                OutputRecord("c".repeat(32), "item",
                    "FixMyShot_20261022T000000Z_aaaaaaaa.png", "image/png",
                    digest, OutputStage.PUBLISHED, uri))
            val matched = BatchRecoveryReconciliation.inspect(folder, snapshot)
            assertEquals(BatchItemPhase.PUBLISHED, matched.items.single().phase)

            val interrupted = BatchRecoveryReconciliation.inspect(folder,
                snapshot.copy(items = listOf(snapshot.items.single().copy(
                    phase = BatchItemPhase.SAVING, uri = null))))
            assertEquals(BatchItemPhase.NEEDS_REVIEW, interrupted.items.single().phase)
        } finally { folder.deleteRecursively() }
    }
}
