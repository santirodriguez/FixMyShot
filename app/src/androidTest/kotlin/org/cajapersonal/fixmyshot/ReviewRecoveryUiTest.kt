package org.cajapersonal.fixmyshot

import android.app.Instrumentation
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.export.*
import org.cajapersonal.fixmyshot.review.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReviewRecoveryUiTest {
    @Test fun restartFindsJobUntilExplicitReview() {
        val test = InstrumentationRegistry.getInstrumentation()
        val app = test.targetContext
        val token = UUID.randomUUID().toString().replace("-", "")
        val folder = File(app.filesDir, "export-jobs/$token").apply { mkdirs() }
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        try {
            BatchExportJournal(File(folder, "batch.json")).write(
                BatchCheckpoint(token, 1L, listOf(BatchItemCheckpoint(
                    "item", "a".repeat(64), BatchItemPhase.ENCODING))))
            val vm = ViewModelProvider(owner)[ReviewViewModel::class.java]
            test.runOnMainSync { vm.checkPastExports(app) }
            waitFor(test, vm, ExportUiPhase.NEEDS_REVIEW)
            assertFalse(vm.prepareExport())
            test.runOnMainSync { vm.acknowledgePreviousExport(app) }
            waitFor(test, vm, ExportUiPhase.IDLE)
            assertTrue(app.getSharedPreferences("export-reviewed", 0).getBoolean(token, false))
        } finally {
            owner.viewModelStore.clear()
            app.getSharedPreferences("export-reviewed", 0).edit().remove(token).commit()
            folder.deleteRecursively()
        }
    }

    private fun waitFor(test: Instrumentation, vm: ReviewViewModel, expected: ExportUiPhase) {
        val deadline = System.currentTimeMillis() + 10_000L
        while (vm.exportState.phase != expected && System.currentTimeMillis() < deadline) {
            test.waitForIdleSync()
            Thread.sleep(50)
        }
        assertEquals(expected, vm.exportState.phase)
    }
}
