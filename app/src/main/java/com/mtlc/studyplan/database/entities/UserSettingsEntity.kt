package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: String = "default_user_settings",
    val userId: String = "default_user",

    // Notification Settings
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: String = "09:00", // HH:mm format
    val streakReminderEnabled: Boolean = true,
    val achievementNotificationsEnabled: Boolean = true,
    val socialNotificationsEnabled: Boolean = true,
    val weeklyReportEnabled: Boolean = true,
    val weeklyReportDay: String = "Sunday",

    // Theme and UI Settings
    val theme: String = "system", // light, dark, system
    val accentColor: String = "#1976D2",
    val useDynamicColors: Boolean = true,
    val fontSize: String = "medium", // small, medium, large
    val reducedAnimations: Boolean = false,
    val compactMode: Boolean = false,

    // Study Settings
    val defaultStudySessionLength: Int = 25, // Pomodoro default
    val defaultBreakLength: Int = 5,
    val longBreakLength: Int = 15,
    val sessionsUntilLongBreak: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartSessions: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,

    // Goal Settings
    val dailyStudyGoalMinutes: Int = 120,
    val dailyTaskGoal: Int = 5,
    val weeklyStudyGoalMinutes: Int = 840,
    val weeklyTaskGoal: Int = 35,
    val adaptiveGoals: Boolean = true,
    val goalDifficulty: String = "medium", // easy, medium, hard

    // Privacy and Social Settings
    val socialSharingEnabled: Boolean = false,
    val profilePublic: Boolean = false,
    val shareAchievements: Boolean = true,
    val shareStreak: Boolean = true,
    val shareProgress: Boolean = false,
    val allowFriendRequests: Boolean = true,
    val showOnLeaderboards: Boolean = false,

    // Data and Sync Settings
    val autoSyncEnabled: Boolean = true,
    val syncOnlyOnWifi: Boolean = false,
    val dataUsageOptimization: Boolean = true,
    val offlineMode: Boolean = false,
    val backupEnabled: Boolean = true,
    val backupFrequency: String = "weekly", // daily, weekly, monthly

    // Accessibility Settings
    val highContrastMode: Boolean = false,
    val largeTextMode: Boolean = false,
    val screenReaderOptimized: Boolean = false,
    val reducedMotion: Boolean = false,
    val colorBlindFriendly: Boolean = false,

    // Advanced Settings
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val betaFeaturesEnabled: Boolean = false,
    val debugModeEnabled: Boolean = false,
    val experimentalFeaturesEnabled: Boolean = false,

    // Language and Localization
    val language: String = "system", // system, en, tr, etc.
    val dateFormat: String = "system", // system, US, EU, ISO
    val timeFormat: String = "system", // system, 12h, 24h
    val firstDayOfWeek: String = "system", // system, monday, sunday

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)