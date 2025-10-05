package com.mtlc.studyplan.localization

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtlc.studyplan.ui.components.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * Language management for the entire application
 * Handles language persistence and locale changes
 */
class LanguageManager(private val context: Context) {

    companion object {
        private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_settings")
        private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

        @Volatile
        private var INSTANCE: LanguageManager? = null

        fun getInstance(context: Context): LanguageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanguageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val dataStore = context.languageDataStore

    /**
     * Flow of current selected language
     */
    val currentLanguage: Flow<Language> = dataStore.data.map { preferences ->
        val languageCode = preferences[LANGUAGE_KEY] ?: Language.ENGLISH.code
        Language.entries.find { it.code == languageCode } ?: Language.ENGLISH
    }

    /**
     * Save selected language to DataStore and restart activity
     */
    suspend fun setLanguage(language: Language, activity: Activity? = null) {
        // Save to DataStore
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }

        // Also save to SharedPreferences for synchronous access
        try {
            val sharedPrefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("language_code", language.code).apply()
        } catch (_: Exception) {
            // Ignore SharedPreferences errors
        }

        // Restart the activity so locale is applied in attachBaseContext
        activity?.recreate()
    }

    /**
     * Get current language synchronously (for initial setup)
     * This checks saved preference first, then falls back to system locale
     */
    fun getCurrentLanguageSync(): Language {
        return try {
            val sharedPrefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            val savedLanguageCode = sharedPrefs.getString("language_code", null)

            if (savedLanguageCode != null) {
                Language.entries.find { it.code == savedLanguageCode } ?: Language.ENGLISH
            } else {
                val locale = Locale.getDefault()
                when (locale.language) {
                    "tr" -> Language.TURKISH
                    else -> Language.ENGLISH
                }
            }
        } catch (_: Exception) {
            Language.ENGLISH
        }
    }
}

/**
 * Composable function to manage language state across the app
 */
@Composable
fun rememberLanguageManager(context: Context): LanguageManagerState {
    val manager = remember { LanguageManager.getInstance(context) }
    val currentLanguage by manager.currentLanguage.collectAsState(
        initial = manager.getCurrentLanguageSync()
    )

    return remember(manager, currentLanguage) {
        LanguageManagerState(manager, currentLanguage)
    }
}

/**
 * State holder for language management
 */
class LanguageManagerState(
    private val manager: LanguageManager,
    val currentLanguage: Language
) {
    /**
     * Change the application language
     */
    suspend fun changeLanguage(language: Language, activity: Activity? = null) {
        manager.setLanguage(language, activity)
    }
}

