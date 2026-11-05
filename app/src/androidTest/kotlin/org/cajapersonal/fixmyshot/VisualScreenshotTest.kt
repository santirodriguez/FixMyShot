package org.cajapersonal.fixmyshot

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import org.cajapersonal.fixmyshot.core.OutputFormat
import org.cajapersonal.fixmyshot.export.BatchItemPhase
import org.cajapersonal.fixmyshot.review.ExportUiPhase
import org.cajapersonal.fixmyshot.review.ReviewViewModel
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Original UI pixels from the installed app, with a genuine imported test JPEG. */
@RunWith(AndroidJUnit4::class)
class VisualScreenshotTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext

    private fun root(): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= 34) instrumentation.uiAutomation.clearCache()
        return instrumentation.uiAutomation.rootInActiveWindow
    }

    private fun find(label: String, node: AccessibilityNodeInfo? = root()): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isVisibleToUser &&
            (node.text?.toString() == label || node.contentDescription?.toString() == label)) return node
        for (i in 0 until node.childCount) find(label, node.getChild(i))?.let { return it }
        return null
    }

    private fun scrollable(node: AccessibilityNodeInfo? = root()): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) scrollable(node.getChild(i))?.let { return it }
        return null
    }

    private fun await(timeoutMs: Long = 20_000L, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) return
            SystemClock.sleep(100)
        }
        fail("Timed out waiting for visual capture state")
    }

    private fun reveal(label: String) {
        if (find(label) != null) return
        repeat(24) {
            val view = scrollable()
            if (view != null && view.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD }) {
                view.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(90)
            if (find(label) != null) return
        }
        fail("Missing visible capture control: $label")
    }

    private fun click(label: String) {
        reveal(label)
        var node = requireNotNull(find(label))
        while (!node.isClickable && node.parent != null) node = node.parent
        assertTrue("Capture control is not clickable: $label",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        SystemClock.sleep(250)
    }

    private fun capture(role: String) {
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(180, 4_000)
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "Android UI screenshot unavailable: $role"
        }
        try {
            val file = File(app.filesDir, "visual-$role.png")
            file.outputStream().use { out ->
                assertTrue("Could not write real UI capture", screenshot.compress(Bitmap.CompressFormat.PNG, 100, out))
            }
            assertTrue("Empty captured PNG: $role", file.length() > 1024)
            val bytes = file.readBytes()
            assertArrayEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10),
                bytes.copyOfRange(0, 8))
        } finally { screenshot.recycle() }
    }

    @Test fun sixAuthenticStoreCandidateScreensFromImportedCafe() {
        assumeTrue("Phone screenshot run requires owned MediaStore output", Build.VERSION.SDK_INT >= 29)
        val photo = instrumentation.context.assets.open("FixMyShot-visual-sample-cafe-hq.jpg").use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(photo).joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
        assertEquals("50ff45c57d4c8e93570e5e5e3df99f7a863d625537fd7e9381963d785f968051", digest)
        assertEquals(387379, photo.size)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(photo, 0, photo.size, bounds)
        assertEquals(941, bounds.outWidth)
        assertEquals(1672, bounds.outHeight)

        val resolver = app.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "FixMyShot_visual_reference_cafe.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FixMyShotVisualTest")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val input = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        var output: Uri? = null
        try {
            resolver.openOutputStream(input, "w")!!.use { it.write(photo) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            assertEquals(1, resolver.update(input, values, null, null))

            // Capture a fresh start screen before importing. Then open the real
            // ACTION_SEND entry point as a separate activity; onNewIntent is protected.
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var startModel: ReviewViewModel
                scenario.onActivity { activity ->
                    activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    startModel = ViewModelProvider(activity)[ReviewViewModel::class.java]
                }
                await { startModel.quickPreferencesReady && startModel.exportState.phase == ExportUiPhase.IDLE }
                await { find(app.getString(R.string.choose_images)) != null }
                capture("01-start")
            }

            // Exercise the actual Android share-intent importer; no draft or preview is fabricated.
            val shared = Intent(app, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, input)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ActivityScenario.launch<MainActivity>(shared).use { scenario ->
                lateinit var model: ReviewViewModel
                scenario.onActivity { activity ->
                    activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    model = ViewModelProvider(activity)[ReviewViewModel::class.java]
                }
                await(40_000L) { model.quickPreferencesReady && model.draft.images.size == 1 &&
                    !model.isBusy && model.preview != null && model.latestImport?.added == 1 }
                assertEquals(941, model.draft.images.single().orientedSize.width)
                assertEquals(1672, model.draft.images.single().orientedSize.height)
                scenario.onActivity { model.clearImportFeedback() }
                await { model.canPrepareExport && find(app.getString(R.string.preview_description)) != null }

                // Select the actual Instagram Stories contract through its offline picker.
                click(app.getString(R.string.quick_choose_destination))
                click("Instagram")
                click(app.getString(R.string.destination_instagram_story))
                await { model.canPrepareExport }
                capture("02-editor")
                click(app.getString(R.string.quick_choose_destination))
                await { find(app.getString(R.string.destination_instagram_story)) != null }
                capture("03-destinations")
                click(app.getString(R.string.quick_done))

                click(app.getString(R.string.quick_customize))
                click(app.getString(R.string.editor_framing))
                // Advanced Framing uses Preserve content/Crop (not the Quick Keep all/Fill buttons).
                click(app.getString(R.string.crop_mode))
                await { model.draft.batch.fit == org.cajapersonal.fixmyshot.core.FitMode.CROP }
                capture("04-framing")
                click(app.getString(R.string.quick_automatic))

                click(app.getString(R.string.quick_settings))
                click(app.getString(R.string.theme_dark))
                reveal(app.getString(R.string.ux_about))
                click(app.getString(R.string.ux_about))
                reveal(app.getString(R.string.about_version, "1.0.0"))
                await { find(app.getString(R.string.about_product_name)) != null }
                if (app.getString(R.string.app_name) != app.getString(R.string.about_product_name)) {
                    assertNull("About should not show the debug launcher label",
                        find(app.getString(R.string.app_name)))
                }
                capture("06-settings")
                click(app.getString(R.string.close_action))
                await { model.canPrepareExport }
                assertEquals(OutputFormat.PNG, model.draft.batch.format)

                // The ordinary UI export click must publish and verify a real MediaStore PNG.
                click(app.getString(R.string.quick_export_format, app.getString(R.string.format_png), 1))
                await(120_000L) { model.exportState.phase == ExportUiPhase.COMPLETED &&
                    model.exportState.published == 1 && model.canFinishSession }
                assertEquals(BatchItemPhase.PUBLISHED, model.exportState.items.single().phase)
                output = Uri.parse(requireNotNull(model.exportState.items.single().uri))
                assertEquals(0x89, resolver.openInputStream(requireNotNull(output))!!.use { it.read() })
                await { find(app.getString(R.string.quick_saved_title)) != null }
                capture("05-saved")
                assertTrue(model.canFinishSession)
                scenario.onActivity { model.acknowledgePreviousExport(app) }
                await(25_000L) {
                    model.exportState.phase == ExportUiPhase.IDLE &&
                        model.draft.images.isEmpty() && !model.exportState.outputError
                }
                assertEquals(0x89, resolver.openInputStream(requireNotNull(output))!!.use { it.read() })
            }
        } finally {
            output?.let { runCatching { resolver.delete(it, null, null) } }
            runCatching { resolver.delete(input, null, null) }
        }
    }
}
