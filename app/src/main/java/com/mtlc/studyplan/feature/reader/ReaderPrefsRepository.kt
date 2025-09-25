package com.mtlc.studyplan.feature.reader

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.readerPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "yds_reader_prefs")

class ReaderPrefsRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val FONT_SCALE = floatPreferencesKey("reader_font_scale")
        val LINE_HEIGHT = floatPreferencesKey("reader_line_height")
        val THEME = intPreferencesKey("reader_theme")
    }

    val prefsFlow: Flow<ReaderPrefs> = dataStore.data.map { p ->
        ReaderPrefs(
            fontScaleSp = p[Keys.FONT_SCALE] ?: 18f,
            lineHeightMult = p[Keys.LINE_HEIGHT] ?: 1.4f,
            theme = p[Keys.THEME]?.let { ReaderTheme.entries[it] } ?: ReaderTheme.System
        )
    }

    suspend fun save(prefs: ReaderPrefs) {
        dataStore.edit { e ->
            e[Keys.FONT_SCALE] = prefs.fontScaleSp
            e[Keys.LINE_HEIGHT] = prefs.lineHeightMult
            e[Keys.THEME] = prefs.theme.ordinal
        }
    }
}

