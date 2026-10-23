package org.cajapersonal.fixmyshot

import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cajapersonal.fixmyshot.importing.IncomingImages
import org.cajapersonal.fixmyshot.review.ReviewSources
import org.cajapersonal.fixmyshot.review.ReviewViewModel
import org.cajapersonal.fixmyshot.settings.AppPreferences
import org.cajapersonal.fixmyshot.settings.AppTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** One functional block: share entry, bounded imports, recreation and persistent appearance. */
@RunWith(AndroidJUnit4::class)
class ImportSettingsFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext

    @Test fun streamAndClipDataAreDeduplicatedAndUnsafePathsAreRejected() {
        val first = Uri.parse("content://synthetic/first")
        val second = Uri.parse("content://synthetic/second")
        val unsafe = Uri.parse("file:///sdcard/private.png")
        val incoming = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(first, unsafe))
            clipData = ClipData.newRawUri("synthetic", first).also {
                it.addItem(ClipData.Item(second))
            }
        }
        val selection = IncomingImages.parse(incoming)
        assertEquals(listOf(first, second), selection.uris)
        assertEquals(1, selection.rejected)
        assertFalse(selection.overLimit)
        assertTrue(IncomingImages.parse(Intent(Intent.ACTION_VIEW, first)).uris.isEmpty())
    }

    @Test fun oversizedAndMalformedOffersCannotSilentlyImportAPartialSelection() {
        val incoming = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                ArrayList((0..100).map { Uri.parse("content://synthetic/$it") }))
        }
        assertTrue(IncomingImages.parse(incoming).overLimit)
        assertTrue(IncomingImages.parse(incoming).uris.isEmpty())
        val malformed = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, "wrong-type")
        assertTrue(IncomingImages.parse(malformed).uris.isEmpty())
        assertEquals(1, IncomingImages.parse(malformed).rejected)
    }

    @Test fun themeChoiceSurvivesANewPreferencesReader() = runBlocking {
        val preferences = AppPreferences(app)
        val previous = preferences.theme.first()
        try {
            preferences.setTheme(AppTheme.DARK)
            assertEquals(AppTheme.DARK, AppPreferences(app).theme.first())
            preferences.setTheme(AppTheme.SYSTEM)
            assertEquals(AppTheme.SYSTEM, AppPreferences(app).theme.first())
        } finally { preferences.setTheme(previous) }
    }

    @Test fun incomingImageAndDraftSurviveRecreationAndLocaleChanges() {
        assumeTrue("Uses an owned permission-free MediaStore fixture", Build.VERSION.SDK_INT >= 29)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "FixMyShot_import_test_${System.nanoTime()}.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FixMyShot")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        val oldLocales = AppCompatDelegate.getApplicationLocales()
        val stagedIds = mutableSetOf<String>()
        var scenario: ActivityScenario<MainActivity>? = null
        try {
            val bitmap = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.BLUE)
                app.contentResolver.openOutputStream(uri)!!.use {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally { bitmap.recycle() }
            values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            app.contentResolver.update(uri, values, null, null)
            val incoming = Intent(app, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("synthetic", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            scenario = ActivityScenario.launch(incoming)
            lateinit var retained: ReviewViewModel
            scenario.onActivity { retained = ViewModelProvider(it)[ReviewViewModel::class.java] }
            scenario.recreate()
            val deadline = System.currentTimeMillis() + 20_000L
            while (System.currentTimeMillis() < deadline && retained.draft.images.isEmpty()) {
                instrumentation.waitForIdleSync()
                Thread.sleep(50)
            }
            assertEquals(1, retained.draft.images.size)
            val image = retained.draft.images.single()
            stagedIds += image.id
            assertEquals(32, image.orientedSize.width)
            assertEquals(48, image.orientedSize.height)
            val originalDraft = retained.draft
            for (tag in listOf("es", "ca", "")) {
                instrumentation.runOnMainSync {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                }
                // LocaleManager schedules a configuration change asynchronously; idle on one
                // looper turn does not prove that ActivityScenario has the updated Activity yet.
                val systemLocales = Resources.getSystem().configuration.locales
                val expected = if (tag.isNotEmpty()) tag else
                    (0 until systemLocales.size()).map { systemLocales[it].language }
                        .firstOrNull { it in setOf("en", "es", "ca") } ?: "en"
                var applied = false
                val localeDeadline = System.currentTimeMillis() + 10_000L
                while (!applied && System.currentTimeMillis() < localeDeadline) {
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { applied = it.resources.configuration.locales[0].language == expected }
                    if (!applied) Thread.sleep(50)
                }
                assertTrue("Requested locale was not applied before the deadline: $expected", applied)
                scenario.onActivity { activity ->
                    assertSame(retained, ViewModelProvider(activity)[ReviewViewModel::class.java])
                    assertEquals(originalDraft, retained.draft)
                    if (tag.isNotEmpty()) {
                        assertEquals(tag, activity.resources.configuration.locales[0].language)
                        assertEquals(if (tag == "es") "Ajustes y ayuda" else "Configuració i ajuda",
                            activity.getString(R.string.settings_title))
                    }
                }
            }
            assertTrue(ReviewSources(app).matchesFingerprint(image.id, requireNotNull(image.contentSha256)))
        } finally {
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(oldLocales) }
            scenario?.close()
            stagedIds.forEach { File(app.cacheDir, "review-sources/$it.source").delete() }
            app.contentResolver.delete(uri, null, null)
        }
    }
}
