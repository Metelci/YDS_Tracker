package com.mtlc.studyplan.settings.data

import kotlinx.serialization.Serializable

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

@Serializable
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val studyRemindersEnabled: Boolean = true,
    val achievementNotificationsEnabled: Boolean = true,
    val dailyGoalRemindersEnabled: Boolean = true,
    val streakWarningsEnabled: Boolean = true,
    val offlineModeEnabled: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val gamificationEnabled: Boolean = true,
    val socialSharingEnabled: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val weekendModeEnabled: Boolean = false,
    val smartSchedulingEnabled: Boolean = true,
    val autoDifficultyEnabled: Boolean = true,
    val studyReminderTime: String = "09:00",
    val dailyStudyGoalMinutes: Int = 60
) {
    companion object {
        fun default(): UserSettings = UserSettings()
    }
}
