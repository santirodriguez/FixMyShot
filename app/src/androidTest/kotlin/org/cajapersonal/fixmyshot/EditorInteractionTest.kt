package org.cajapersonal.fixmyshot

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.cajapersonal.fixmyshot.design.FixMyShotTheme
import org.cajapersonal.fixmyshot.review.ReviewCanvas
import java.util.concurrent.atomic.AtomicInteger
import java.util.Collections
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import org.json.JSONObject
import org.json.JSONArray
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
        try {
            File(app.filesDir, "editor-$caseName.png").outputStream().use {
                check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { screenshot.recycle() }
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
        // A viewport remains scrollable at the bottom even if SCROLL_DOWN is absent.
        // Use orientation, not the available scroll direction, to find its container.
        if (node.isScrollable && rect.height() > rect.width() / 3) return node
        for (i in 0 until node.childCount) toolPane(node.getChild(i))?.let { return it }
        return null
    }

    /** Find the accessible control inside the visible tool viewport, not above its clipped edge. */
    private fun reveal(label: String) {
        fun visibleInPane(): Boolean {
            val control = find(label) ?: return false
            val controlRect = Rect().also { control.getBoundsInScreen(it) }
            if (controlRect.width() <= 0 || controlRect.height() <= 0) return false
            val pane = toolPane()
            if (pane == null) {
                // Fully visible non-scrollable controls should never require a scroll action.
                val root = freshRoot() ?: return false
                val windowRect = Rect().also { root.getBoundsInScreen(it) }
                return windowRect.contains(controlRect) && control.isVisibleToUser
            }
            val paneRect = Rect().also { pane.getBoundsInScreen(it) }
            return paneRect.contains(controlRect) && control.isVisibleToUser
        }
        if (visibleInPane()) return
        // The destination sheet animates in: wait for a usable accessibility
        // viewport instead of treating a transient missing node as a failed control.
        await { visibleInPane() || toolPane() != null }
        if (visibleInPane()) return
        // Visit the start of the list first, THEN sweep forward through its
        // entire content. A symmetric forward/backward sweep from the middle
        // can return to the starting offset without ever reaching controls above it.
        for (action in listOf(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
            for (step in 0 until 32) {
                if (visibleInPane()) return
                val pane = toolPane()
                if (pane == null) {
                    test.waitForIdleSync()
                    SystemClock.sleep(40)
                    continue
                }
                if (!pane.performAction(action)) break
                settleUi()
            }
        }
        assertTrue("Cannot reveal accessible control: $label", visibleInPane())
    }

    private fun bounds(label: String): Rect = Rect().also { requireNotNull(find(label)).getBoundsInScreen(it) }

    private fun fixture(allowPrivateCleanup: Boolean = false, block: (ActivityScenario<MainActivity>, ReviewViewModel, (Int) -> String) -> Unit) {
        caseName = "setup"
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
            scenario.onActivity {
                // Only the instrumentation window stays awake; production behavior is unchanged.
                it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (Build.VERSION.SDK_INT >= 27) {
                    it.setShowWhenLocked(true)
                    it.setTurnScreenOn(true)
                } else {
                    @Suppress("DEPRECATION")
                    it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                }
                model = ViewModelProvider(it)[ReviewViewModel::class.java]; strings = it.resources
            }
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

    @Test fun savedResultContinueButtonUnlocksTheSameEditorAndAllowsAnEdit() = fixture { scenario, model, label ->
        caseName = "continue-editing"
        var output: ByteArray? = null
        var creates = 0
        var deletions = 0
        val port = object : org.cajapersonal.fixmyshot.export.OwnedOutputPort {
            override fun create(name: String, mime: String): android.net.Uri {
                creates++
                return android.net.Uri.parse("content://synthetic/editor-result")
            }
            override fun openWrite(uri: android.net.Uri): java.io.OutputStream = object : java.io.ByteArrayOutputStream() {
                override fun close() { output = toByteArray(); super.close() }
            }
            override fun openRead(uri: android.net.Uri): java.io.InputStream? = output?.let { java.io.ByteArrayInputStream(it) }
            override fun publish(uri: android.net.Uri): Boolean = output != null
            override fun delete(uri: android.net.Uri): Boolean { deletions++; return false }
        }
        val before = model.draft
        val sample = model.preview
        val editorKey = model.editorSessionKey
        scenario.onActivity {
            model.onDraftEdited()
            assertTrue(model.prepareExport())
            model.startPreparedExport(app, port)
        }
        await { model.exportState.phase == org.cajapersonal.fixmyshot.review.ExportUiPhase.COMPLETED }
        val published = requireNotNull(output).copyOf()
        assertTrue(model.exportState.isLocked)
        click(label(R.string.quick_keep_editing))
        await { !model.exportState.isLocked }
        assertEquals(before, model.draft)
        assertEquals(editorKey, model.editorSessionKey)
        assertSame(sample, model.preview)
        assertFalse(model.canPrepareExport)
        click(label(R.string.quick_customize))
        click(label(R.string.editor_framing))
        click(label(R.string.crop_mode))
        await { model.draft.batch.fit == FitMode.CROP }
        assertTrue(model.canPrepareExport)
        assertEquals(1, creates); assertEquals(0, deletions)
        assertArrayEquals(published, output)
        capture()
    }

    @Test fun savedResultOffersAnotherImageFirstAndOpensThePickerAfterCleanup() = fixture(allowPrivateCleanup = true) { scenario, model, label ->
        caseName = "next-image"
        var output: ByteArray? = null
        val port = object : org.cajapersonal.fixmyshot.export.OwnedOutputPort {
            override fun create(name: String, mime: String): android.net.Uri = android.net.Uri.parse("content://synthetic/next-image")
            override fun openWrite(uri: android.net.Uri): java.io.OutputStream = object : java.io.ByteArrayOutputStream() {
                override fun close() { output = toByteArray(); super.close() }
            }
            override fun openRead(uri: android.net.Uri): java.io.InputStream? = output?.let { java.io.ByteArrayInputStream(it) }
            override fun publish(uri: android.net.Uri): Boolean = output != null
            override fun delete(uri: android.net.Uri): Boolean = throw AssertionError("Published output must remain intact")
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.provider.MediaStore.ACTION_PICK_IMAGES)
            addAction(android.content.Intent.ACTION_OPEN_DOCUMENT)
            addAction("androidx.activity.result.contract.action.PICK_IMAGES")
        }
        val monitor = test.addMonitor(filter,
            android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null), true)
        try {
            scenario.onActivity {
                model.onDraftEdited()
                assertTrue(model.prepareExport())
                model.startPreparedExport(app, port)
            }
            await { model.exportState.phase == org.cajapersonal.fixmyshot.review.ExportUiPhase.COMPLETED }
            val published = requireNotNull(output).copyOf()
            await { find(label(R.string.quick_edit_another)) != null }
            assertTrue(bounds(label(R.string.quick_edit_another)).top < bounds(label(R.string.quick_view_saved)).top)
            capture()
            click(label(R.string.quick_edit_another))
            await { monitor.hits == 1 && model.draft.images.isEmpty() && !model.exportState.isLocked }
            await { find(label(R.string.choose_images)) != null }
            assertEquals(OutputFormat.PNG, model.draft.batch.format)
            assertEquals(MetadataPolicy.REMOVE_IDENTIFYING, model.draft.batch.metadata)
            assertArrayEquals(published, output)
        } finally { test.removeMonitor(monitor) }
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
        click(label(R.string.quick_choose_destination))
        await { find(label(R.string.quick_play_portrait)) != null }
        click(label(R.string.quick_play_portrait))
        await { model.draft.destination == DestinationChoice.Preset(DestinationCatalog.PLAY_PORTRAIT) }
        assertEquals(before.revision + 1, model.draft.revision)
        val geometry = (model.draft.plans(36).single().geometry as PlannedGeometry.Contained).value
        assertEquals(PixelSize(1683, 2992), geometry.canvas)
        assertEquals(169, geometry.placement.left)
        assertEquals(170, geometry.rightMargin)
        // The optional solid edge matte is an explicit per-image RGB override.
        click(label(R.string.editor_background))
        reveal(label(R.string.quick_background_edge))
        click(label(R.string.quick_background_edge))
        await { model.draft.images.single().overrides.resolve(model.draft.batch).background is BackgroundChoice.CustomRgb }
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
        await { find(label(R.string.quick_flexible_hint)) != null }
        await { find(label(R.string.quick_crop_show_trim)) != null }
        click(label(R.string.quick_crop_show_trim))
        await { find(label(R.string.quick_crop_trim_accessibility)) != null }
        click(label(R.string.quick_crop_show_result))
        capture()
        click(label(R.string.crop_editor_cancel))
        assertEquals(before, model.draft)
        click(label(R.string.quick_expand))
        click(label(R.string.quick_fill_crop))
        click(label(R.string.quick_apply_frame))
        await { model.draft.revision == before.revision + 1 }
        assertEquals(FitMode.FLEXIBLE, model.draft.images.single().overrides.resolve(model.draft.batch).fit)
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
        await { model.draft.images.single().overrides.resolve(model.draft.batch).fit == FitMode.FLEXIBLE }
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
    }    @Test fun platformPickerAndMatchedMattePreserveTheOriginal() = fixture { _, model, label ->
        caseName = "destinations"
        val sample = model.preview
        val before = model.draft
        click(label(R.string.quick_choose_destination))
        reveal("WhatsApp")
        click("WhatsApp")
        reveal(label(R.string.destination_whatsapp_chat))
        capture()
        click(label(R.string.destination_whatsapp_chat))
        await { model.draft.destination == DestinationChoice.Preset(DestinationCatalog.WHATSAPP_CHAT) }
        val plan = model.draft.plans(36).single()
        assertEquals(PixelSize(1344, 2992), (plan.geometry as PlannedGeometry.Contained).value.canvas)
        assertEquals(before.batch, model.draft.batch)
        assertSame(sample, model.preview)
        click(label(R.string.editor_background))
        reveal(label(R.string.quick_background_edge))
        click(label(R.string.quick_background_edge))
        await { model.draft.images.single().overrides.resolve(model.draft.batch).background is BackgroundChoice.CustomRgb }
        assertEquals(before.batch, model.draft.batch)
        assertSame(sample, model.preview)
        caseName = "matched-matte"
        capture()
    }

    @Test fun cropMovesDrawWithoutPublishingOrRecomposingTheParent() = fixture { scenario, model, label ->
        caseName = "crop-performance"
        val source = model.draft.images.single()
        val sample = requireNotNull(model.preview)
        val baseline = model.draft.setBatch(model.draft.batch.copy(fit = FitMode.CROP,
            crop = CropFocus(zoom = 1.2))).plans(36).single()
        val compositions = AtomicInteger()
        val publications = AtomicInteger()
        val durations = Collections.synchronizedList(mutableListOf<Long>())
        val metricThread = HandlerThread("editor-frame-evidence").apply { start() }
        val listener = android.view.Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            durations.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))
        }
        var metricWindow: android.view.Window? = null
        try {
            scenario.onActivity { activity ->
                metricWindow = activity.window
                activity.window.addOnFrameMetricsAvailableListener(listener, Handler(metricThread.looper))
                activity.setContent {
                    var focus by remember { mutableStateOf(baseline.settings.crop) }
                    val plan = remember(focus) { baseline.copy(settings = baseline.settings.copy(crop = focus),
                        geometry = PlannedGeometry.Cropped(CropPlanner.plan(source.orientedSize,
                            baseline.destination.cropRatio(source.orientedSize), focus))) }
                    SideEffect { compositions.incrementAndGet() }
                    FixMyShotTheme(dark = false) {
                        ReviewCanvas(sample, source, plan, true, false,
                            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                            onCrop = { publications.incrementAndGet(); focus = it }, onPosition = {}, onGestureFinished = {})
                    }
                }
            }
            await { find(label(R.string.preview_description)) != null && compositions.get() > 0 }
            settleUi()
            val rectangle = bounds(label(R.string.preview_description))
            fun rasterSignature(): Long {
                val bitmap = requireNotNull(test.uiAutomation.takeScreenshot())
                try {
                    var result = 7L
                    for (y in rectangle.top + 10 until rectangle.bottom - 10 step 11) {
                        result = result * 31 + bitmap.getPixel(rectangle.centerX(), y)
                    }
                    return result
                } finally { bitmap.recycle() }
            }
            val before = rasterSignature()
            val count = compositions.get()
            var down = SystemClock.uptimeMillis()
            fun pointer(action: Int, dy: Float) {
                if (action == MotionEvent.ACTION_DOWN) down = SystemClock.uptimeMillis()
                val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                    rectangle.exactCenterX(), rectangle.exactCenterY() + dy, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try { test.sendPointerSync(event) } finally { event.recycle() }
            }
            pointer(MotionEvent.ACTION_DOWN, 0f)
            repeat(20) { step -> pointer(MotionEvent.ACTION_MOVE, (step + 1) * 4f); SystemClock.sleep(16) }
            test.waitForIdleSync()
            assertEquals("Pointer movement must not publish transient crop values", 0, publications.get())
            assertEquals("Draw-only movement must not recompose the parent", count, compositions.get())
            assertNotEquals("The actual preview must move before pointer-up", before, rasterSignature())
            pointer(MotionEvent.ACTION_UP, 80f)
            await { publications.get() == 1 && compositions.get() > count }
            val committedRaster = rasterSignature()
            pointer(MotionEvent.ACTION_DOWN, 0f)
            repeat(10) { step -> pointer(MotionEvent.ACTION_MOVE, -(step + 1) * 4f); SystemClock.sleep(16) }
            pointer(MotionEvent.ACTION_CANCEL, -40f)
            // The renderer redraw may outlive an idle pointer/semantics queue; still
            // require exact committed pixels with a bounded deadline, not a weaker diff.
            await { rasterSignature() == committedRaster }
            assertEquals("Cancellation must not publish another crop", 1, publications.get())
            assertEquals("Cancellation restores the committed viewport", committedRaster, rasterSignature())
            capture()
        } finally {
            scenario.onActivity { metricWindow?.removeOnFrameMetricsAvailableListener(listener) }
            metricThread.quitSafely()
            metricThread.join(2_000)
            val times = synchronized(durations) { durations.toList().sorted() }
            val report = JSONObject().put("scope", "SYNTHETIC_EMULATOR_DEBUG_NOT_PIXEL_ACCEPTANCE")
                .put("parentCompositions", compositions.get()).put("cropPublications", publications.get())
                .put("frameCount", times.size).put("totalDurationNs", JSONArray(times))
                .put("physicalBaselineAvailable", false)
            if (times.isNotEmpty()) report.put("p95TotalNs", times[((times.size - 1) * .95).toInt()])
            File(app.filesDir, "editor-frame-metrics.json").writeText(report.toString(2))
        }
    }

    @Test fun settingsSheetKeepsAppearanceLanguageAndHelpAccessible() = fixture { _, model, label ->
        caseName = "settings-dark"
        val before = model.draft
        val sample = model.preview
        click(label(R.string.quick_settings))
        try {
            await { find(label(R.string.settings_title)) != null }
            click(label(R.string.theme_dark))
            await { find(label(R.string.settings_language)) != null }
            capture()
            reveal(label(R.string.help_title))
            click(label(R.string.help_title))
            reveal(label(R.string.native_check_title))
            assertNotNull(find(label(R.string.native_check_title)))
            assertEquals(before, model.draft)
            assertSame(sample, model.preview)
        } finally {
            reveal(label(R.string.theme_light))
            click(label(R.string.theme_light))
            click(label(R.string.close_action))
        }
        await { find(label(R.string.preview_description)) != null }
        assertEquals(before, model.draft)
    }

}
