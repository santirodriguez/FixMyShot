package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
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

    private fun freshRoot(): AccessibilityNodeInfo? {
        // API 34+ caches Compose semantics even after a frame changes; inspect a
        // fresh accessibility tree rather than the pre-recomposition tab contents.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            test.uiAutomation.clearCache()
        }
        return test.uiAutomation.rootInActiveWindow
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
        dump(freshRoot())
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

    private fun find(label: String, root: AccessibilityNodeInfo? = freshRoot()): AccessibilityNodeInfo? {
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

    private fun toolPane(node: AccessibilityNodeInfo? = freshRoot()): AccessibilityNodeInfo? {
        if (node == null) return null
        val rect = Rect().also { node.getBoundsInScreen(it) }
        // Select the actual vertical controls viewport, not the horizontal tool strip.
        if (node.isScrollable && rect.height() > rect.width() / 3 &&
            node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id }) return node
        for (i in 0 until node.childCount) toolPane(node.getChild(i))?.let { return it }
        return null
    }

    /** Find the accessible control inside the visible tool viewport, not above its clipped edge. */
    private fun reveal(label: String) {
        fun visibleInPane(): Boolean {
            val control = find(label) ?: return false
            val pane = toolPane() ?: return false
            val controlRect = Rect().also { control.getBoundsInScreen(it) }
            val paneRect = Rect().also { pane.getBoundsInScreen(it) }
            return controlRect.top >= paneRect.top && controlRect.bottom <= paneRect.bottom
        }
        if (visibleInPane()) return
        // An accessibility action may return false at either end; search both
        // directions instead of asserting that every intermediate scroll can advance.
        for (action in listOf(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
            repeat(10) {
                if (visibleInPane()) return
                val pane = requireNotNull(toolPane()) { "No vertical tool pane" }
                if (!pane.performAction(action)) return@repeat
                settleUi()
            }
        }
        assertTrue("Cannot reveal accessible control: $label", visibleInPane())
    }

    private fun bounds(label: String): Rect = Rect().also { requireNotNull(find(label)).getBoundsInScreen(it) }

    private fun fixture(allowPrivateCleanup: Boolean = false, block: (ActivityScenario<MainActivity>, ReviewViewModel, (Int) -> String) -> Unit) {
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
            try {
                if (file.exists()) assertArrayEquals(bytes, file.readBytes())
                else assertTrue("Only an explicit discard may remove the private fixture", allowPrivateCleanup)
            } finally { file.delete() }
        }
    }

    @Test fun gesturesKeepTheSampleAndCommitOneUndoableEdit() = fixture { scenario, model, label ->
        caseName = "gesture"
        click(label(R.string.quick_customize))
        click(label(R.string.editor_framing))
        click(label(R.string.crop_mode))
        await { model.draft.batch.fit == FitMode.CROP }
        await { model.draft.plans(Build.VERSION.SDK_INT).single().geometry is PlannedGeometry.Cropped }
        await { find("+ ${label(R.string.crop_zoom_control)}") != null }
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
        click(label(R.string.quick_customize))
        click(label(R.string.editor_framing))
        click(label(R.string.crop_mode))
        await { model.draft.batch.fit == FitMode.CROP }
        await { model.draft.plans(Build.VERSION.SDK_INT).single().geometry is PlannedGeometry.Cropped }
        await { find("+ ${label(R.string.crop_zoom_control)}") != null }
        settleUi()
        val previewLabel = label(R.string.preview_description)
        val initial = bounds(previewLabel)
        val zoomPlus = "+ ${label(R.string.crop_zoom_control)}"
        reveal(zoomPlus)
        click(zoomPlus)
        await { model.draft.images.single().overrides.resolve(model.draft.batch).crop.zoom > 1.0 }
        assertEquals(1.02, model.draft.images.single().overrides.resolve(model.draft.batch).crop.zoom, 1e-8)
        val down = label(R.string.crop_down)
        reveal(down)
        click(down)
        assertEquals(initial, bounds(previewLabel))
        capture()
        println("EDITOR_SAMPLE=${model.preview!!.width}x${model.preview!!.height}; synthetic interaction only; physical performance NOT_RUN")
    }

    private fun pinch(rect: Rect) {
        val down = SystemClock.uptimeMillis()
        val properties = Array(2) { index -> MotionEvent.PointerProperties().apply {
            id = index; toolType = MotionEvent.TOOL_TYPE_FINGER
        } }
        fun send(action: Int, count: Int, spread: Float) {
            val coordinates = Array(count) { index -> MotionEvent.PointerCoords().apply {
                x = rect.exactCenterX() + (if (index == 0) -spread else spread)
                y = rect.exactCenterY(); pressure = 1f; size = 1f
            } }
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, count,
                properties, coordinates, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
            try { test.sendPointerSync(event) } finally { event.recycle() }
        }
        val start = minOf(30f, rect.width() / 8f)
        send(MotionEvent.ACTION_DOWN, 1, start)
        send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, start)
        for (step in 1..10) { send(MotionEvent.ACTION_MOVE, 2, start + step * 3); SystemClock.sleep(15) }
        send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, start + 30)
        send(MotionEvent.ACTION_UP, 1, start + 30)
        settleUi()
    }

    @Test fun quickTemplatesAndModeSwitchPreserveTheSameDraft() = fixture { scenario, model, label ->
        caseName = "automatic"
        assertNotNull(find(label(R.string.quick_customize)))
        assertNull(find(label(R.string.editor_framing)))
        caseName = "automatic-initial"
        capture()
        caseName = "automatic"
        val sample = model.preview
        val before = model.draft
        click(label(R.string.quick_play_portrait))
        await { model.draft.destination == DestinationChoice.Preset(DestinationCatalog.PLAY_PORTRAIT) }
        assertEquals(before.revision + 1, model.draft.revision)
        val geometry = (model.draft.plans(36).single().geometry as PlannedGeometry.Contained).value
        assertEquals(PixelSize(1683, 2992), geometry.canvas)
        assertEquals(169, geometry.placement.left)
        assertEquals(170, geometry.rightMargin)
        val selected = model.draft
        click(label(R.string.quick_customize))
        await { find(label(R.string.editor_framing)) != null }
        click(label(R.string.quick_automatic))
        await { find(label(R.string.quick_customize)) != null }
        assertEquals(selected, model.draft)
        assertSame(sample, model.preview)
        scenario.recreate()
        await { find(label(R.string.quick_customize)) != null }
        assertEquals(selected, model.draft)
        capture()
    }

    @Test fun expandedFramingIsLargerCancelableAndCommitsOnce() = fixture { _, model, label ->
        caseName = "focus"
        val initial = bounds(label(R.string.preview_description))
        val before = model.draft
        click(label(R.string.quick_expand))
        await { find(label(R.string.quick_focus_canvas)) != null }
        assertTrue(bounds(label(R.string.preview_description)).height() > initial.height())
        click(label(R.string.quick_fill_crop))
        assertEquals(before, model.draft)
        capture()
        click(label(R.string.crop_editor_cancel))
        assertEquals(before, model.draft)
        click(label(R.string.quick_expand))
        click(label(R.string.quick_fill_crop))
        click(label(R.string.quick_apply_frame))
        await { model.draft.revision == before.revision + 1 }
        assertEquals(FitMode.CROP, model.draft.images.single().overrides.resolve(model.draft.batch).fit)
        click(label(R.string.undo_last))
        assertEquals(before.images.single().overrides, model.draft.images.single().overrides)
    }

    @Test fun inspectionZoomDoesNotModifyTheExport() = fixture { _, model, label ->
        caseName = "inspection"
        val before = model.draft
        click(label(R.string.quick_expand))
        click(label(R.string.quick_inspect))
        pinch(bounds(label(R.string.quick_focus_canvas)))
        assertEquals(before, model.draft)
        capture()
        click(label(R.string.quick_apply_frame))
        assertEquals(before, model.draft)
    }

    @Test fun containPinchRequiresExplicitCropConsent() = fixture { _, model, label ->
        caseName = "crop-consent"
        val before = model.draft
        pinch(bounds(label(R.string.preview_description)))
        await { find(label(R.string.quick_crop_question)) != null }
        assertEquals(before, model.draft)
        click(label(R.string.quick_keep_all))
        assertEquals(before, model.draft)
        pinch(bounds(label(R.string.preview_description)))
        click(label(R.string.quick_fill_crop))
        await { model.draft.images.single().overrides.resolve(model.draft.batch).fit == FitMode.CROP }
        assertEquals(before.revision + 1, model.draft.revision)
    }

    @Test fun discardIsVisibleAndCancelKeepsTheWork() = fixture(allowPrivateCleanup = true) { _, model, label ->
        caseName = "discard"
        val before = model.draft
        click(label(R.string.quick_discard))
        click(label(R.string.quick_keep_editing))
        assertEquals(before, model.draft)
        click(label(R.string.quick_customize))
        await { find(label(R.string.editor_framing)) != null }
        click(label(R.string.quick_discard))
        await { find(label(R.string.discard_session_confirmation)) != null }
        click(label(R.string.quick_discard))
        await { model.draft.images.isEmpty() }
        assertFalse(model.draft.canUndo)
        await { find(label(R.string.choose_images)) != null }
        val startAction = bounds(label(R.string.choose_images))
        val window = Rect().also { requireNotNull(freshRoot()).getBoundsInScreen(it) }
        assertTrue("Start Choose images must remain visible without scrolling", window.contains(startAction))
        caseName = "start"
        capture()
    }
}
