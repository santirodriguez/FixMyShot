package org.cajapersonal.fixmyshot.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal enum class AppTheme { SYSTEM, LIGHT, DARK }
private val Context.preferences by preferencesDataStore(name = "appearance")

/** Presentation preferences only; format and metadata opt-out are never remembered defaults. */
internal class AppPreferences(context: Context) {
    private val store = context.applicationContext.preferences
    private val templateKey = stringPreferencesKey("last_template")
    private val themeKey = stringPreferencesKey("theme")
    val theme = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { preferences ->
        AppTheme.entries.firstOrNull { it.name == preferences[themeKey] } ?: AppTheme.SYSTEM
    }

    val lastTemplate = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { org.cajapersonal.fixmyshot.core.QuickTemplates.remembered(it[templateKey])?.id }

    suspend fun setTemplate(id: String) {
        require(org.cajapersonal.fixmyshot.core.QuickTemplates.remembered(id) != null)
        store.edit { it[templateKey] = id }
    }

    suspend fun setTheme(value: AppTheme) {
        store.edit { it[themeKey] = value.name }
    }
}
