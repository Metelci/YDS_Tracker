package com.mtlc.studyplan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.mtlc.studyplan.features.onboarding.SkillWeights
import com.mtlc.studyplan.ui.components.TooltipData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingRepository(private val dataStore: DataStore<Preferences>) {

    private val shownTooltipsKey = stringSetPreferencesKey("shown_tooltips")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val examEpochDayKey = longPreferencesKey("onboarding_exam_epoch_day")
    private val weightGrammarKey = intPreferencesKey("onboarding_weight_grammar_i1000")
    private val weightReadingKey = intPreferencesKey("onboarding_weight_reading_i1000")
    private val weightListeningKey = intPreferencesKey("onboarding_weight_listening_i1000")
    private val weightVocabKey = intPreferencesKey("onboarding_weight_vocab_i1000")

    val shownTooltips: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[shownTooltipsKey] ?: emptySet()
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[onboardingCompletedKey] ?: false
    }

    suspend fun markTooltipShown(tooltipId: String) {
        dataStore.edit { preferences ->
            val currentShown = preferences[shownTooltipsKey] ?: emptySet()
            preferences[shownTooltipsKey] = currentShown + tooltipId
        }
    }

    suspend fun markOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = true
        }
    }

    suspend fun resetOnboarding() {
        dataStore.edit { preferences ->
            preferences.remove(shownTooltipsKey)
            preferences.remove(onboardingCompletedKey)
            preferences.remove(examEpochDayKey)
            preferences.remove(weightGrammarKey)
            preferences.remove(weightReadingKey)
            preferences.remove(weightListeningKey)
            preferences.remove(weightVocabKey)
        }
    }

    suspend fun saveProfile(examEpochDay: Long, weights: SkillWeights) {
        dataStore.edit { prefs ->
            prefs[examEpochDayKey] = examEpochDay
            prefs[weightGrammarKey] = (weights.grammar * 1000).toInt()
            prefs[weightReadingKey] = (weights.reading * 1000).toInt()
            prefs[weightListeningKey] = (weights.listening * 1000).toInt()
            prefs[weightVocabKey] = (weights.vocab * 1000).toInt()
        }
    }

    companion object {
        val availableTooltips = listOf(
            TooltipData(
                id = "customize_plan",
                title = "Customize Your Plan",
                message = "Tap the calendar icon to hide, edit, or add your own tasks. All changes are saved on your device.",
                isImportant = true
            ),
            TooltipData(
                id = "plan_settings",
                title = "Plan Settings",
                message = "Adjust your study schedule, start date, and daily time allocations to match your availability.",
                isImportant = true
            ),
            TooltipData(
                id = "achievements",
                title = "Track Your Progress",
                message = "Tap the achievements badge to see your study milestones and maintain motivation.",
                isImportant = false
            ),
            TooltipData(
                id = "language_switch",
                title = "Language Support",
                message = "Switch between Turkish and English by tapping TR/EN in the header.",
                isImportant = false
            ),
            TooltipData(
                id = "expand_week",
                title = "Week Details",
                message = "Tap any week to expand and see daily tasks. Tap again to collapse.",
                isImportant = false
            ),
            TooltipData(
                id = "task_details",
                title = "Task Information",
                message = "Tap any task to see detailed instructions and estimated time.",
                isImportant = false
            )
        )

        val onboardingFlow = listOf(
            "customize_plan",
            "plan_settings",
            "achievements",
            "expand_week"
        )
    }
}
