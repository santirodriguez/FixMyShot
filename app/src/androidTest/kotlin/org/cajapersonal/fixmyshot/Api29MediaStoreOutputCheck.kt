package org.cajapersonal.fixmyshot

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.cajapersonal.fixmyshot.core.PrivateOutputRecipe
import org.cajapersonal.fixmyshot.export.MediaStore29Port
import org.cajapersonal.fixmyshot.export.OutputJournal
import org.cajapersonal.fixmyshot.export.SaveResult
import org.cajapersonal.fixmyshot.export.TransactionalOutputSaver
import org.cajapersonal.fixmyshot.review.PrivateExecutionResult
import org.junit.Assert.*

/** Isolate API29-only MediaStore fields from API26 JUnit class discovery. */
@TargetApi(29)
internal object Api29MediaStoreOutputCheck {
    fun verify(
        context: Context,
        recipe: PrivateOutputRecipe,
        candidate: PrivateExecutionResult.Ready,
        journal: OutputJournal,
    ) {
        val port = MediaStore29Port(context.contentResolver)
        var ownUri: Uri? = null
        try {
            val result = TransactionalOutputSaver(context.cacheDir, port, journal)
                .save(recipe, candidate)
            // Try to remove even an indeterminate synthetic output on test cleanup.
            ownUri = when (result) {
                is SaveResult.Published -> result.uri
                is SaveResult.Indeterminate -> result.uri
                is SaveResult.Failed -> null
            }
            assertTrue(result.toString(), result is SaveResult.Published)
            result as SaveResult.Published
            context.contentResolver.query(result.uri,
                arrayOf(MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.IS_PENDING), null, null, null).use { cursor ->
                val row = requireNotNull(cursor)
                assertTrue(row.moveToFirst())
                assertEquals("Pictures/FixMyShot", row.getString(0).trimEnd('/'))
                assertEquals(0, row.getInt(1))
            }
        } finally {
            ownUri?.let { port.delete(it) }
        }
    }
}
