package org.cajapersonal.fixmyshot.export

import java.io.File
import java.io.IOException

/** Cross-check independent item journals without modifying provider files or job receipts. */
internal object BatchRecoveryReconciliation {
    fun inspect(folder: File, batch: BatchCheckpoint): BatchCheckpoint =
        batch.copy(items = batch.items.mapIndexed { index, item ->
            val record = try {
                OutputJournal(File(folder, "item-$index.json")).read()
            } catch (_: IOException) {
                return@mapIndexed item.copy(
                    phase = BatchItemPhase.NEEDS_REVIEW, reason = "CHECK_OUTPUT")
            }
            val complete = item.phase in setOf(
                BatchItemPhase.PUBLISHED, BatchItemPhase.FAILED, BatchItemPhase.CANCELLED)
            val published = item.phase == BatchItemPhase.PUBLISHED
            val consistent = if (record == null) !published else
                record.itemId == item.itemId &&
                    (record.stage == OutputStage.PUBLISHED) == published &&
                    (!published || record.uri == item.uri) &&
                    record.stage !in setOf(
                        OutputStage.RESERVED, OutputStage.PENDING, OutputStage.VISIBLE_WRITING,
                        OutputStage.VALIDATED, OutputStage.INDETERMINATE)
            if (complete && consistent) item else item.copy(
                phase = BatchItemPhase.NEEDS_REVIEW,
                uri = item.uri ?: record?.uri,
                reason = "CHECK_OUTPUT")
        })
}
