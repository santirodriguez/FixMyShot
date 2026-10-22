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
    @Test fun publishedReceiptNeedsVerifiedProviderBytes() {
        val folder = File(cache, "reconcile-" + UUID.randomUUID()).apply { mkdirs() }
        try {
            val snapshot = BatchCheckpoint("a".repeat(32), 2L,
                listOf(BatchItemCheckpoint("item", digest, BatchItemPhase.PUBLISHED, uri)))
            OutputJournal(File(folder, "item-0.json")).write(
                OutputRecord("c".repeat(32), "item",
                    "FixMyShot_20261022T000000Z_aaaaaaaa.png", "image/png",
                    digest, OutputStage.PUBLISHED, uri))
            val correct = BatchRecoveryReconciliation.inspect(folder, snapshot) {
                OutputRecoveryEvidence.MATCHED
            }
            assertEquals(BatchItemPhase.PUBLISHED, correct.items.single().phase)

            val missing = BatchRecoveryReconciliation.inspect(folder, snapshot) {
                OutputRecoveryEvidence.MISSING
            }
            assertEquals(BatchItemPhase.NEEDS_REVIEW, missing.items.single().phase)
            assertEquals("OUTPUT_MISSING", missing.items.single().reason)

            val changed = BatchRecoveryReconciliation.inspect(folder, snapshot) {
                OutputRecoveryEvidence.MODIFIED
            }
            assertEquals(BatchItemPhase.NEEDS_REVIEW, changed.items.single().phase)
            assertEquals("OUTPUT_CHANGED", changed.items.single().reason)

            val pending = BatchRecoveryReconciliation.inspect(folder, snapshot) {
                OutputRecoveryEvidence.PENDING
            }
            assertEquals(BatchItemPhase.NEEDS_REVIEW, pending.items.single().phase)
            assertEquals("OUTPUT_PENDING", pending.items.single().reason)
        } finally { folder.deleteRecursively() }
    }

    @Test fun uncertainPublicationIsNeverPromotedByMatchingBytes() {
        val folder = File(cache, "reconcile-" + UUID.randomUUID()).apply { mkdirs() }
        try {
            val snapshot = BatchCheckpoint("a".repeat(32), 1L,
                listOf(BatchItemCheckpoint("item", digest, BatchItemPhase.SAVING)))
            OutputJournal(File(folder, "item-0.json")).write(
                OutputRecord("c".repeat(32), "item",
                    "FixMyShot_20261022T000000Z_aaaaaaaa.png", "image/png",
                    digest, OutputStage.INDETERMINATE, uri))
            val result = BatchRecoveryReconciliation.inspect(folder, snapshot) {
                OutputRecoveryEvidence.MATCHED
            }
            assertEquals(BatchItemPhase.NEEDS_REVIEW, result.items.single().phase)
            assertEquals("OUTPUT_PRESENT_UNCERTAIN", result.items.single().reason)
            assertEquals(uri, result.items.single().uri)
        } finally { folder.deleteRecursively() }
    }

    @Test fun failedBatchCannotConcealCancelledOrUnfinishedItemRecord() {
        val folder = File(cache, "reconcile-" + UUID.randomUUID()).apply { mkdirs() }
        try {
            val snapshot = BatchCheckpoint("a".repeat(32), 1L,
                listOf(BatchItemCheckpoint("item", digest, BatchItemPhase.FAILED)))
            OutputJournal(File(folder, "item-0.json")).write(
                OutputRecord("c".repeat(32), "item",
                    "FixMyShot_20261022T000000Z_aaaaaaaa.png", "image/png",
                    digest, OutputStage.CANCELLED))
            val result = BatchRecoveryReconciliation.inspect(folder, snapshot)
            assertEquals(BatchItemPhase.NEEDS_REVIEW, result.items.single().phase)
        } finally { folder.deleteRecursively() }
    }

}
