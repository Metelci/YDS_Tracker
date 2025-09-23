package com.mtlc.studyplan.localization

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtlc.studyplan.ui.components.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

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
        Language.values().find { it.code == languageCode } ?: Language.ENGLISH
    }

    /**
     * Save selected language to DataStore
     */
    suspend fun setLanguage(language: Language) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }

        // Apply locale change
        applyLocale(language)
    }

    /**
     * Apply locale to the application context
     */
    private fun applyLocale(language: Language) {
        val locale = when (language) {
            Language.ENGLISH -> Locale.ENGLISH
            Language.TURKISH -> Locale("tr", "TR")
        }

        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        // Create new context with updated configuration (modern approach for API 30+)
        context.createConfigurationContext(config)
    }

    /**
     * Get current language synchronously (for initial setup)
     */
    fun getCurrentLanguageSync(): Language {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "tr" -> Language.TURKISH
            else -> Language.ENGLISH
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
    suspend fun changeLanguage(language: Language) {
        manager.setLanguage(language)
    }
}

/**
 * Localized strings for the application
 * This would typically be loaded from string resources
 */
object LocalizedStrings {

    fun getString(key: String, language: Language): String {
        return when (language) {
            Language.ENGLISH -> getEnglishString(key)
            Language.TURKISH -> getTurkishString(key)
        }
    }

    private fun getEnglishString(key: String): String {
        return when (key) {
            "good_morning" -> "Good morning! 👋"
            "ready_ace_yds" -> "Ready to ace your YDS exam?"
            "today" -> "Today"
            "days_to_yds" -> "Days to YDS"
            "points_today" -> "Points Today"
            "tasks_done" -> "Tasks Done"
            "weekly_study_plan" -> "📅 Weekly Study Plan"
            "this_week" -> "This Week"
            "view_full_week" -> "View Full Week"
            "modify_plan" -> "▶ Modify Plan"
            "yds_exam_2024" -> "YDS Exam 2024"
            "exam_day" -> "Exam day!"
            "settings" -> "Settings"
            "home" -> "Home"
            "tasks" -> "Tasks"
            "social" -> "Social"
            "switch_to_turkish" -> "Switch to Turkish"
            "switch_to_english" -> "Switch to English"
            else -> key
        }
    }

    private fun getTurkishString(key: String): String {
        return when (key) {
            "good_morning" -> "Günaydın! 👋"
            "ready_ace_yds" -> "YDS sınavında başarılı olmaya hazır mısın?"
            "today" -> "Bugün"
            "days_to_yds" -> "YDS'ye Kalan"
            "points_today" -> "Bugünkü Puan"
            "tasks_done" -> "Tamamlanan"
            "weekly_study_plan" -> "📅 Haftalık Çalışma Planı"
            "this_week" -> "Bu Hafta"
            "view_full_week" -> "Tüm Haftayı Gör"
            "modify_plan" -> "▶ Planı Değiştir"
            "yds_exam_2024" -> "YDS Sınavı 2024"
            "exam_day" -> "Sınav günü!"
            "settings" -> "Ayarlar"
            "home" -> "Ana Sayfa"
            "tasks" -> "Görevler"
            "social" -> "Sosyal"
            "switch_to_turkish" -> "Türkçe'ye geç"
            "switch_to_english" -> "İngilizce'ye geç"
            else -> key
        }
    }
}

/**
 * Composable function to get localized string
 */
@Composable
fun localizedString(key: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val languageManager = rememberLanguageManager(context)
    return LocalizedStrings.getString(key, languageManager.currentLanguage)
}