package org.cajapersonal.fixmyshot.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Modern Android uses the actual Activity's platform service; legacy storage stays in AppCompat. */
internal object AppLanguages {
    fun selected(context: Context): String = if (Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(LocaleManager::class.java).applicationLocales[0]?.language.orEmpty()
    } else AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()

    fun select(context: Context, tag: String) {
        require(tag in setOf("", "en", "es", "ca"))
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }
}
