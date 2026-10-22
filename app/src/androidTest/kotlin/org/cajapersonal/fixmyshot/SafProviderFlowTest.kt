package org.cajapersonal.fixmyshot

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.provider.DocumentsContract
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.review.ExportUiPhase
import org.cajapersonal.fixmyshot.review.ReviewViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Dedicated disposable-emulator exercise; the shell runs its phases in separate processes. */
@RunWith(AndroidJUnit4::class)
class SafProviderFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext
    private val args get() = InstrumentationRegistry.getArguments()
    private val receipt get() = File(app.filesDir, "saf-provider-smoke.json")
    private val folder get() = "FixMyShotSaf_" + requireNotNull(args.getString("safToken"))
    private val tree get() = DocumentsContract.buildTreeDocumentUri(
        "com.android.externalstorage.documents", "primary:Download/$folder")

    private fun guard() {
        assumeTrue("Use the dedicated emulator script", args.getString("safSmoke") == "true")
        check(Build.VERSION.SDK_INT in setOf(26, 28))
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        check(args.getString("safToken")?.matches(Regex("[0-9a-f]{32}")) == true)
        check(args.getString("sourceSha")?.matches(Regex("[0-9a-f]{40}")) == true)
    }

    private fun await(description: String, timeout: Long = 30_000, test: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            if (test()) return
            Thread.sleep(100)
        }
        fail("Timed out: $description")
    }

    private fun launch(): Pair<MainActivity, ReviewViewModel> {
        val activity = instrumentation.startActivitySync(Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        lateinit var model: ReviewViewModel
        instrumentation.runOnMainSync { model = ViewModelProvider(activity)[ReviewViewModel::class.java] }
        await("previous-output check") { model.exportState.phase != ExportUiPhase.CHECKING_HISTORY }
        return activity to model
    }

    private fun hasGrant() = app.contentResolver.persistedUriPermissions.any {
        it.uri == tree && it.isReadPermission && it.isWritePermission
    }

    /** The hint navigates DocumentsUI; it does not inject a returned URI or grant permission. */
    @Suppress("UNCHECKED_CAST")
    private fun openActualPicker(activity: MainActivity, model: ReviewViewModel) {
        val field = MainActivity::class.java.getDeclaredField("folderPicker").apply { isAccessible = true }
        val launcher = field.get(activity) as ActivityResultLauncher<Uri?>
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        instrumentation.runOnMainSync {
            assertTrue(model.prepareExport())
            launcher.launch(DocumentsContract.buildDocumentUri(
                tree.authority!!, DocumentsContract.getTreeDocumentId(tree)))
        }
        await("actual DocumentsUI at the isolated fixture folder") {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            root != null && root.packageName?.toString()?.endsWith("documentsui") == true &&
                root.findAccessibilityNodeInfosByText(folder).any { it.text?.toString() == folder }
        }
    }

    private fun confirmFolder() {
        await("DocumentsUI Select button") {
            val root = instrumentation.uiAutomation.rootInActiveWindow ?: return@await false
            root.findAccessibilityNodeInfosByViewId("android:id/button1").any {
                it.isVisibleToUser && it.isEnabled && it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    private fun hash(file: File) = file.inputStream().use { stream ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    private fun stage(jpeg: Boolean): Pair<DraftImage, File> {
        val id = UUID.randomUUID().toString().replace("-", "")
        val root = File(app.cacheDir, "review-sources").apply { mkdirs() }
        val file = File(root, "$id.source")
        if (jpeg) {
            instrumentation.context.assets.open("orientation6_solid_36x60.jpg").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        } else {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            try {
                bitmap.setHasAlpha(false)
                bitmap.eraseColor(Color.BLUE)
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
        }
        return DraftImage(id, "synthetic-$id", if (jpeg) PixelSize(60, 36) else PixelSize(64, 64),
            false, contentSha256 = hash(file)) to file
    }

    private fun children(): Set<String> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree))
        return app.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null, null, null)!!.use { cursor -> buildSet {
            while (cursor.moveToNext()) add(cursor.getString(0))
        } }
    }

    private fun checkOutput(item: JSONObject) {
        val uri = Uri.parse(item.getString("uri"))
        assertEquals(tree.authority, uri.authority)
        assertEquals(DocumentsContract.getTreeDocumentId(tree), DocumentsContract.getTreeDocumentId(uri))
        val bytes = app.contentResolver.openInputStream(uri)!!.use { input ->
            val data = input.readBytes()
            assertTrue("Tiny synthetic output exceeded limit", data.size in 1..1_048_576)
            data
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals(item.getString("sha256"), digest)
        assertEquals(item.getString("mime"), app.contentResolver.getType(uri))
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        try {
            assertEquals(item.getInt("side"), bitmap.width)
            assertEquals(item.getInt("side"), bitmap.height)
        } finally { bitmap.recycle() }
    }

    @Test fun selectCancelAndPublishThroughActualFolderGrant() {
        guard()
        assertFalse("Fixture must start without a grant", hasGrant())
        val (activity, model) = launch()
        val (png, pngFile) = stage(false)
        val (jpeg, jpegFile) = stage(true)
        val sources = listOf(png to pngFile, jpeg to jpegFile)
        val outputs = JSONArray()
        val beforeJobs = File(app.filesDir, "export-jobs").listFiles()?.map { it.name }.orEmpty().toSet()
        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
            batch = EditSettings(background = BackgroundChoice.White)).addImages(listOf(png, jpeg))
        try {
            instrumentation.runOnMainSync { model.draft = draft; model.selectedId = png.id }
            openActualPicker(activity, model)
            repeat(8) {
                if (model.exportState.phase == ExportUiPhase.CHOOSING_FOLDER) {
                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                    Thread.sleep(150)
                }
            }
            await("picker cancellation") { model.exportState.phase == ExportUiPhase.IDLE }
            assertFalse(hasGrant())
            assertEquals(draft, model.draft)
            assertEquals(beforeJobs, File(app.filesDir, "export-jobs").listFiles()
                ?.map { it.name }.orEmpty().toSet())

            for (format in listOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
                if (format != OutputFormat.PNG) {
                    instrumentation.runOnMainSync { model.acknowledgePreviousExport(app) }
                    await("explicit acknowledgement") { model.exportState.phase == ExportUiPhase.IDLE }
                    instrumentation.runOnMainSync {
                        model.draft = model.draft.setBatch(model.draft.batch.copy(format = format))
                        model.onDraftEdited()
                    }
                }
                openActualPicker(activity, model)
                confirmFolder()
                await("two synthetic outputs", 90_000) {
                    model.exportState.phase !in setOf(ExportUiPhase.CHOOSING_FOLDER, ExportUiPhase.RUNNING)
                }
                assertEquals(ExportUiPhase.COMPLETED, model.exportState.phase)
                assertEquals(2, model.exportState.published)
                assertTrue(hasGrant())
                for (item in model.exportState.items) {
                    val uri = Uri.parse(requireNotNull(item.uri))
                    val bytes = app.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                        .joinToString("") { "%02x".format(it.toInt() and 255) }
                    val entry = JSONObject().put("uri", uri.toString()).put("sha256", digest)
                        .put("side", if (item.itemId == png.id) 64 else 60)
                        .put("mime", if (format == OutputFormat.PNG) "image/png" else "image/webp")
                    checkOutput(entry)
                    outputs.put(entry)
                }
            }
            assertEquals(4, children().size)
            sources.forEach { (image, file) -> assertEquals(image.contentSha256, hash(file)) }
            val report = JSONObject().put("schema", 1).put("api", Build.VERSION.SDK_INT)
                .put("source_sha", args.getString("sourceSha")).put("token", args.getString("safToken"))
                .put("status", "PHASE1_PASS").put("phase1_pid", Process.myPid())
                .put("picker_cancelled", true).put("grant_read", true).put("grant_write", true)
                .put("provider", tree.authority).put("outputs", outputs)
                .put("sources", JSONArray(sources.map { (image, _) -> JSONObject()
                    .put("id", image.id).put("sha256", image.contentSha256) }))
                .put("jobs", JSONArray(File(app.filesDir, "export-jobs").listFiles()
                    ?.map { it.name }?.filter { it !in beforeJobs }.orEmpty()))
            receipt.writeText(report.toString())
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }

    @Test fun afterForceStopPersistedGrantIsUsableAndRevocationDeniesReads() {
        guard()
        val report = JSONObject(receipt.readText())
        assertEquals("PHASE1_PASS", report.getString("status"))
        assertEquals(args.getString("safToken"), report.getString("token"))
        assertEquals(args.getString("sourceSha"), report.getString("source_sha"))
        assertNotEquals("Shell must force-stop between phases", report.getInt("phase1_pid"), Process.myPid())
        assertTrue(hasGrant())
        val outputs = report.getJSONArray("outputs")
        assertEquals(4, outputs.length())
        val (activity, model) = launch()
        try {
            assertEquals(ExportUiPhase.NEEDS_REVIEW, model.exportState.phase)
            assertEquals(2, model.exportState.published)
            for (index in 0 until outputs.length()) checkOutput(outputs.getJSONObject(index))
            assertEquals(4, children().size)
            val sources = report.getJSONArray("sources")
            for (index in 0 until sources.length()) {
                val source = sources.getJSONObject(index)
                assertEquals(source.getString("sha256"), hash(File(app.cacheDir,
                    "review-sources/${source.getString("id")}.source")))
            }
            app.contentResolver.releasePersistableUriPermission(tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            assertFalse(hasGrant())
            for (index in 0 until outputs.length()) {
                var denied = false
                try {
                    app.contentResolver.openInputStream(Uri.parse(outputs.getJSONObject(index)
                        .getString("uri")))?.use { it.read() }
                } catch (_: SecurityException) { denied = true }
                assertTrue("Existing output must require its revoked tree grant", denied)
            }
            report.put("status", "PASS_REAL_SAF_EMULATOR").put("phase2_pid", Process.myPid())
                .put("restart_readable", true).put("source_hashes_unchanged", true)
                .put("duplicate_free", true).put("revoked_reads_denied", true)
            receipt.writeText(report.toString())
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
