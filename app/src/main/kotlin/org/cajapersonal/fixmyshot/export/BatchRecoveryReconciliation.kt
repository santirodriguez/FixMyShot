package org.cajapersonal.fixmyshot.export

import java.io.File
import java.io.IOException

/**
 * Read-only reconciliation of batch receipts, item receipts and optional provider evidence.
 * An uncertain publication is NEVER silently promoted to PUBLISHED, even if bytes match.
 */
internal object BatchRecoveryReconciliation {
    fun inspect(
        folder: File,
        batch: BatchCheckpoint,
        verifyOutput: ((OutputRecord) -> OutputRecoveryEvidence)? = null,
    ): BatchCheckpoint = batch.copy(items = batch.items.mapIndexed { index, item ->
        val record = try {
            OutputJournal(File(folder, "item-$index.json")).read()
        } catch (_: IOException) {
            return@mapIndexed item.copy(
                phase = BatchItemPhase.NEEDS_REVIEW, reason = "CHECK_OUTPUT")
        }

        val matchedItem = record == null || record.itemId == item.itemId
        val matchedUri = record == null || item.uri == record.uri
        val consistent = matchedItem && when (item.phase) {
            BatchItemPhase.PUBLISHED ->
                record != null && record.stage == OutputStage.PUBLISHED && matchedUri
            BatchItemPhase.FAILED ->
                record == null || record.stage == OutputStage.FAILED
            BatchItemPhase.CANCELLED ->
                record == null || record.stage == OutputStage.CANCELLED
            else -> false
        }

        val evidence = if (matchedItem && matchedUri && record?.uri != null && verifyOutput != null) {
            try { verifyOutput(record) } catch (_: Exception) {
                OutputRecoveryEvidence.UNVERIFIABLE
            }
        } else null

        if (consistent && item.phase == BatchItemPhase.PUBLISHED &&
            (verifyOutput == null || evidence == OutputRecoveryEvidence.MATCHED)) {
            return@mapIndexed item
        }
        if (consistent && item.phase in setOf(
                BatchItemPhase.FAILED, BatchItemPhase.CANCELLED)) {
            return@mapIndexed item
        }

        val reason = when (evidence) {
            OutputRecoveryEvidence.MATCHED -> "OUTPUT_PRESENT_UNCERTAIN"
            OutputRecoveryEvidence.MISSING -> "OUTPUT_MISSING"
            OutputRecoveryEvidence.MODIFIED -> "OUTPUT_CHANGED"
            OutputRecoveryEvidence.PENDING -> "OUTPUT_PENDING"
            OutputRecoveryEvidence.UNVERIFIABLE -> "OUTPUT_UNVERIFIABLE"
            null -> "CHECK_OUTPUT"
        }
        item.copy(
            phase = BatchItemPhase.NEEDS_REVIEW,
            uri = item.uri ?: record?.uri,
            reason = reason)
    })
}
