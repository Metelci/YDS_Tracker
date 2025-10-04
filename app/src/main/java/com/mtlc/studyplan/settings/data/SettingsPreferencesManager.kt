package com.mtlc.studyplan.settings.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages all settings-related SharedPreferences with type-safe access
 */
class SettingsPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // State flows for reactive UI updates
    private val _privacySettings = MutableStateFlow(getPrivacySettings())
    val privacySettings: Flow<PrivacySettings> = _privacySettings.asStateFlow()

    private val _notificationSettings = MutableStateFlow(getNotificationSettings())
    val notificationSettings: Flow<NotificationSettings> = _notificationSettings.asStateFlow()

    private val _taskSettings = MutableStateFlow(getTaskSettings())
    val taskSettings: Flow<TaskSettings> = _taskSettings.asStateFlow()

    private val _navigationSettings = MutableStateFlow(getNavigationSettings())
    val navigationSettings: Flow<NavigationSettings> = _navigationSettings.asStateFlow()

    private val _gamificationSettings = MutableStateFlow(getGamificationSettings())
    val gamificationSettings: Flow<GamificationSettings> = _gamificationSettings.asStateFlow()

    private val _socialSettings = MutableStateFlow(getSocialSettings())
    val socialSettings: Flow<SocialSettings> = _socialSettings.asStateFlow()

    // Privacy Settings
    fun updatePrivacySettings(settings: PrivacySettings) {
        with(prefs.edit()) {
            putString(KEY_PROFILE_VISIBILITY, settings.profileVisibility.name)
            putBoolean(KEY_PROGRESS_SHARING, settings.progressSharing)
            apply()
        }
        _privacySettings.value = settings
    }

    private fun getPrivacySettings(): PrivacySettings {
        return PrivacySettings(
            profileVisibility = ProfileVisibility.valueOf(
                prefs.getString(KEY_PROFILE_VISIBILITY, ProfileVisibility.FRIENDS_ONLY.name) ?: ProfileVisibility.FRIENDS_ONLY.name
            ),
            progressSharing = prefs.getBoolean(KEY_PROGRESS_SHARING, true)
        )
    }

    // Notification Settings
    fun updateNotificationSettings(settings: NotificationSettings) {
        with(prefs.edit()) {
            putBoolean(KEY_PUSH_NOTIFICATIONS, settings.pushNotifications)
            putBoolean(KEY_STUDY_REMINDERS, settings.studyReminders)
            putBoolean(KEY_ACHIEVEMENT_ALERTS, settings.achievementAlerts)
            putBoolean(KEY_EMAIL_SUMMARIES, settings.emailSummaries)
            apply()
        }
        _notificationSettings.value = settings
    }

    private fun getNotificationSettings(): NotificationSettings {
        return NotificationSettings(
            pushNotifications = prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true),
            studyReminders = prefs.getBoolean(KEY_STUDY_REMINDERS, true),
            achievementAlerts = prefs.getBoolean(KEY_ACHIEVEMENT_ALERTS, true),
            emailSummaries = prefs.getBoolean(KEY_EMAIL_SUMMARIES, false)
        )
    }

    // Task Settings
    fun updateTaskSettings(settings: TaskSettings) {
        with(prefs.edit()) {
            putBoolean(KEY_SMART_SCHEDULING, settings.smartScheduling)
            putBoolean(KEY_AUTO_DIFFICULTY, settings.autoDifficultyAdjustment)
            putBoolean(KEY_DAILY_GOAL_REMINDERS, settings.dailyGoalReminders)
            putBoolean(KEY_WEEKEND_MODE, settings.weekendMode)
            apply()
        }
        _taskSettings.value = settings
    }

    private fun getTaskSettings(): TaskSettings {
        return TaskSettings(
            smartScheduling = prefs.getBoolean(KEY_SMART_SCHEDULING, true),
            autoDifficultyAdjustment = prefs.getBoolean(KEY_AUTO_DIFFICULTY, true),
            dailyGoalReminders = prefs.getBoolean(KEY_DAILY_GOAL_REMINDERS, true),
            weekendMode = prefs.getBoolean(KEY_WEEKEND_MODE, false)
        )
    }

    // Navigation Settings
    fun updateNavigationSettings(settings: NavigationSettings) {
        with(prefs.edit()) {
            putBoolean(KEY_BOTTOM_NAVIGATION, settings.bottomNavigation)
            putBoolean(KEY_HAPTIC_FEEDBACK, settings.hapticFeedback)
            putBoolean(KEY_DARK_MODE, settings.darkMode)
            apply()
        }
        _navigationSettings.value = settings
    }

    private fun getNavigationSettings(): NavigationSettings {
        return NavigationSettings(
            bottomNavigation = prefs.getBoolean(KEY_BOTTOM_NAVIGATION, true),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true),
            darkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        )
    }

    // Gamification Settings
    fun updateGamificationSettings(settings: GamificationSettings) {
        with(prefs.edit()) {
            putBoolean(KEY_STREAK_TRACKING, settings.streakTracking)
            putBoolean(KEY_POINTS_REWARDS, settings.pointsAndRewards)
            putBoolean(KEY_CELEBRATION_EFFECTS, settings.celebrationEffects)
            putBoolean(KEY_STREAK_RISK_WARNINGS, settings.streakRiskWarnings)
            apply()
        }
        _gamificationSettings.value = settings
    }

    private fun getGamificationSettings(): GamificationSettings {
        return GamificationSettings(
            streakTracking = prefs.getBoolean(KEY_STREAK_TRACKING, true),
            pointsAndRewards = prefs.getBoolean(KEY_POINTS_REWARDS, true),
            celebrationEffects = prefs.getBoolean(KEY_CELEBRATION_EFFECTS, true),
            streakRiskWarnings = prefs.getBoolean(KEY_STREAK_RISK_WARNINGS, true)
        )
    }

    // Social Settings
    fun updateSocialSettings(settings: SocialSettings) {
        with(prefs.edit()) {
            putBoolean(KEY_SOCIAL_FEATURES, settings.socialFeatures)
            putBoolean(KEY_LEADERBOARDS, settings.leaderboards)
            apply()
        }
        _socialSettings.value = settings
    }

    private fun getSocialSettings(): SocialSettings {
        return SocialSettings(
            socialFeatures = prefs.getBoolean(KEY_SOCIAL_FEATURES, true),
            leaderboards = prefs.getBoolean(KEY_LEADERBOARDS, true)
        )
    }

    companion object {
        private const val PREFS_NAME = "study_plan_settings"

        // Privacy keys
        private const val KEY_PROFILE_VISIBILITY = "profile_visibility"
        private const val KEY_PROGRESS_SHARING = "progress_sharing"

        // Notification keys
        private const val KEY_PUSH_NOTIFICATIONS = "push_notifications"
        private const val KEY_STUDY_REMINDERS = "study_reminders"
        private const val KEY_ACHIEVEMENT_ALERTS = "achievement_alerts"
        private const val KEY_EMAIL_SUMMARIES = "email_summaries"

        // Task keys
        private const val KEY_SMART_SCHEDULING = "smart_scheduling"
        private const val KEY_AUTO_DIFFICULTY = "auto_difficulty"
        private const val KEY_DAILY_GOAL_REMINDERS = "daily_goal_reminders"
        private const val KEY_WEEKEND_MODE = "weekend_mode"

        // Navigation keys
        private const val KEY_BOTTOM_NAVIGATION = "bottom_navigation"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_DARK_MODE = "dark_mode"

        // Gamification keys
        private const val KEY_STREAK_TRACKING = "streak_tracking"
        private const val KEY_POINTS_REWARDS = "points_rewards"
        private const val KEY_CELEBRATION_EFFECTS = "celebration_effects"
        private const val KEY_STREAK_RISK_WARNINGS = "streak_risk_warnings"

        // Social keys
        private const val KEY_SOCIAL_FEATURES = "social_features"
        private const val KEY_LEADERBOARDS = "leaderboards"
    }
}

// Data classes for settings
data class PrivacySettings(
    val profileVisibility: ProfileVisibility = ProfileVisibility.FRIENDS_ONLY,
    val progressSharing: Boolean = true
)

data class NotificationSettings(
    val pushNotifications: Boolean = true,
    val studyReminders: Boolean = true,
    val achievementAlerts: Boolean = true,
    val emailSummaries: Boolean = false
)

data class TaskSettings(
    val smartScheduling: Boolean = true,
    val autoDifficultyAdjustment: Boolean = true,
    val dailyGoalReminders: Boolean = true,
    val weekendMode: Boolean = false
)

data class NavigationSettings(
    val bottomNavigation: Boolean = true,
    val hapticFeedback: Boolean = true,
    val darkMode: Boolean = false
)

data class GamificationSettings(
    val streakTracking: Boolean = true,
    val pointsAndRewards: Boolean = true,
    val celebrationEffects: Boolean = true,
    val streakRiskWarnings: Boolean = true
)

data class SocialSettings(
    val socialFeatures: Boolean = true,
    val leaderboards: Boolean = true
)

enum class ProfileVisibility {
    PUBLIC, FRIENDS_ONLY, PRIVATE
}