package org.cajapersonal.fixmyshot.export

import java.io.File

/** Proof of no remaining output, never just an optimistic status label. */
internal object BatchRetryPolicy {
    fun safe(item: BatchItemCheckpoint, record: OutputRecord?): Boolean {
        if (item.phase !in setOf(BatchItemPhase.FAILED, BatchItemPhase.CANCELLED,
                BatchItemPhase.QUEUED, BatchItemPhase.ENCODING, BatchItemPhase.SAVING) || item.uri != null) return false
        return record == null || (record.itemId == item.itemId && record.uri == null &&
            record.stage in setOf(OutputStage.FAILED, OutputStage.CANCELLED))
    }

    fun eligible(folder: File, snapshot: BatchCheckpoint): Set<String> =
        snapshot.items.mapIndexedNotNull { index, item ->
            try {
                if (safe(item, OutputJournal(File(folder, "item-$index.json")).read())) item.itemId else null
            } catch (_: Exception) { null }
        }.toSet()
}
