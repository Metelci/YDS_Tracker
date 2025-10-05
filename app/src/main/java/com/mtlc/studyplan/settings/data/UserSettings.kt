package com.mtlc.studyplan.settings.data

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
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
    val autoDifficultyEnabled: Boolean = true,
    val studyReminderTime: String = "09:00",
    val dailyStudyGoalMinutes: Int = 60
) {
    companion object {
        fun default(): UserSettings = UserSettings()
    }
}

enum class SettingsKey {
    NOTIFICATIONS_ENABLED,
    PUSH_NOTIFICATIONS_ENABLED,
    STUDY_REMINDERS,
    ACHIEVEMENT_NOTIFICATIONS,
    DAILY_GOAL_REMINDERS,
    STREAK_WARNINGS,
    OFFLINE_MODE,
    AUTO_SYNC,
    GAMIFICATION_ENABLED,
    SOCIAL_SHARING,
    HAPTIC_FEEDBACK,
    WEEKEND_MODE,
    AUTO_DIFFICULTY
}
