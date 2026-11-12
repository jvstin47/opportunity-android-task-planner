package com.opportunity.app.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "opportunity_theme")

@Singleton
class ThemeStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val accentKey = stringPreferencesKey("accent_palette")

    val accent = context.themeDataStore.data.map { prefs ->
        prefs[accentKey]?.let { name -> runCatching { AccentPalette.valueOf(name) }.getOrNull() }
            ?: AccentPalette.AMBER
    }

    suspend fun setAccent(accent: AccentPalette) {
        context.themeDataStore.edit { it[accentKey] = accent.name }
    }
}
