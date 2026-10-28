package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.InputDevice
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.review.ReviewViewModel
import org.cajapersonal.fixmyshot.export.ReviewSessionJournal
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Real Compose accessibility/pointer interaction with an app-private synthetic Pixel-sized source. */
@RunWith(AndroidJUnit4::class)
class EditorInteractionTest {
    private val test = InstrumentationRegistry.getInstrumentation()
    private val app = test.targetContext

    private fun settleUi() {
        test.waitForIdleSync()
        test.uiAutomation.waitForIdle(150, 5_000)
    }

    private var caseName = "editor"

    private fun capture() {
        val tree = StringBuilder()
        fun dump(node: AccessibilityNodeInfo?, depth: Int = 0) {
            if (node == null) return
            val rect = Rect().also { node.getBoundsInScreen(it) }
            tree.appendLine("depth=$depth text=${node.text} description=${node.contentDescription} visible=${node.isVisibleToUser} scrollable=${node.isScrollable} bounds=$rect")
            for (i in 0 until node.childCount) dump(node.getChild(i), depth + 1)
        }
        dump(test.uiAutomation.rootInActiveWindow)
        File(app.filesDir, "editor-$caseName.txt").writeText(tree.toString())
        val screenshot = test.uiAutomation.takeScreenshot() ?: return
        try { File(app.filesDir, "editor-framing.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { screenshot.recycle() }
        File(app.filesDir, "editor-framing.png").copyTo(File(app.filesDir, "editor-$caseName.png"), overwrite = true)
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition() && SystemClock.uptimeMillis() < deadline) {
            test.waitForIdleSync(); SystemClock.sleep(40)
        }
        if (!condition()) {
            capture()
            fail("Editor did not settle; synthetic UI captured")
        }
    }

    private fun find(label: String, root: AccessibilityNodeInfo? = test.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isVisibleToUser && (root.text?.toString() == label || root.contentDescription?.toString() == label)) return root
        for (i in 0 until root.childCount) find(label, root.getChild(i))?.let { return it }
        return null
    }

    private fun click(label: String) {
        await { find(label) != null }
        var node = requireNotNull(find(label))
        while (!node.isClickable && node.parent != null) node = node.parent
        assertTrue("Control not clickable: $label", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        settleUi()
    }

    private fun scroll() {
        fun scrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            val rect = Rect().also { node.getBoundsInScreen(it) }
            // The active tool pane can be below OR beside the canvas. Exclude the
            // horizontal tool/thumbnail rows by requiring a vertical scroll action.
            if (node.isScrollable && rect.height() > 100 &&
                node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id }) return node
            for (i in 0 until node.childCount) scrollable(node.getChild(i))?.let { return it }
            return null
        }
        val node = requireNotNull(scrollable(test.uiAutomation.rootInActiveWindow)) { "No vertical tool pane" }
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
        settleUi()
    }

    private fun bounds(label: String): Rect = Rect().also { requireNotNull(find(label)).getBoundsInScreen(it) }

    private fun fixture(block: (ActivityScenario<MainActivity>, ReviewViewModel, (Int) -> String) -> Unit) {
        val id = UUID.randomUUID().toString().replace("-", "")
        val file = File(app.cacheDir, "review-sources/$id.source").apply { parentFile!!.mkdirs() }
        val source = Bitmap.createBitmap(1344, 2992, Bitmap.Config.ARGB_8888)
        try {
            source.eraseColor(Color.WHITE)
            val canvas = android.graphics.Canvas(source)
            val paint = android.graphics.Paint()
            for (row in 0..29) {
                paint.color = if (row % 2 == 0) Color.rgb(30, 90, 180) else Color.rgb(240, 210, 90)
                canvas.drawRect(0f, row * 100f, 1344f, row * 100f + 50f, paint)
            }
            file.outputStream().use { assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { source.recycle() }
        val bytes = file.readBytes()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val journal = File(app.filesDir, "review-session.json")
        val before = journal.takeIf { it.exists() }?.readBytes()
        val journalStore = ReviewSessionJournal(journal)
        journalStore.clear()
        try { ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: ReviewViewModel
            lateinit var strings: android.content.res.Resources
            scenario.onActivity { model = ViewModelProvider(it)[ReviewViewModel::class.java]; strings = it.resources }
            await { model.exportState.phase != org.cajapersonal.fixmyshot.review.ExportUiPhase.CHECKING_HISTORY }
            scenario.onActivity {
                model.draft = DraftSession().addImages(listOf(DraftImage(id, "synthetic-editor", PixelSize(1344, 2992), false,
                    contentSha256 = hash)))
                model.selectedId = id
            }
            await { model.preview != null && find(strings.getString(R.string.preview_description)) != null }
            try { block(scenario, model) { strings.getString(it) } }
            catch (failure: Throwable) { capture(); throw failure }
            finally {
                scenario.onActivity { model.draft = DraftSession(); model.selectedId = null; model.trimPreview(); model.onDraftEdited() }
                await { journalStore.read() == null }
            }
        }
        } finally {
            journalStore.clear()
            if (before != null) journal.writeBytes(before)
            try { assertArrayEquals(bytes, file.readBytes()) } finally { file.delete() }
        }
    }

    @Test fun gesturesKeepTheSampleAndCommitOneUndoableEdit() = fixture { scenario, model, label ->
        caseName = "gesture"
        click(label(R.string.editor_framing))
        click(label(R.string.crop_mode))
        await { model.draft.batch.fit == FitMode.CROP }
        settleUi()
        val sample = model.preview
        val before = model.draft
        val rect = bounds(label(R.string.preview_description))
        val down = SystemClock.uptimeMillis()
        fun event(action: Int, x: Float, y: Float) {
            val e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
            e.source = InputDevice.SOURCE_TOUCHSCREEN
            try { test.sendPointerSync(e) } finally { e.recycle() }
        }
        val x = rect.exactCenterX()
        val y = rect.exactCenterY()
        event(MotionEvent.ACTION_DOWN, x, y)
        for (step in 1..12) { event(MotionEvent.ACTION_MOVE, x, y + step * 4f); SystemClock.sleep(12) }
        assertEquals("Pointer moves must not checkpoint every frame", before.revision, model.draft.revision)
        event(MotionEvent.ACTION_UP, x, y + 48f)
        await { model.draft.revision > before.revision }
        assertEquals(before.revision + 1, model.draft.revision)
        assertSame(sample, model.preview)
        click(label(R.string.undo_last))
        await { model.draft.images.single().overrides == before.images.single().overrides }
        scenario.recreate()
        await { find(label(R.string.preview_description)) != null }
        assertSame("Recreation should retain the bounded original sample", sample, model.preview)
    }

    @Test fun arrowsAndFineZoomDoNotScrollTheCanvasAway() = fixture { _, model, label ->
        caseName = "arrows"
        click(label(R.string.editor_framing))
        click(label(R.string.crop_mode))
        await { model.draft.batch.fit == FitMode.CROP }
        settleUi()
        val previewLabel = label(R.string.preview_description)
        val initial = bounds(previewLabel)
        val zoomPlus = "+"
        capture()
        repeat(6) { if (find(zoomPlus) == null) scroll() }
        click(zoomPlus)
        await { model.draft.images.single().overrides.resolve(model.draft.batch).crop.zoom > 1.0 }
        assertEquals(1.02, model.draft.images.single().overrides.resolve(model.draft.batch).crop.zoom, 1e-8)
        val down = label(R.string.crop_down)
        repeat(6) { if (find(down) == null) scroll() }
        click(down)
        assertEquals(initial, bounds(previewLabel))
        capture()
        println("EDITOR_SAMPLE=${model.preview!!.width}x${model.preview!!.height}; synthetic interaction only; physical performance NOT_RUN")
    }
}
