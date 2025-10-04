package com.mtlc.studyplan.settings.data

data class PrivacyData(
    val profileVisibilityEnabled: Boolean = true,
    val profileVisibilityLevel: ProfileVisibilityLevel = ProfileVisibilityLevel.FRIENDS_ONLY,
    val progressSharing: Boolean = true
)

enum class ProfileVisibilityLevel {
    PUBLIC, FRIENDS_ONLY, PRIVATE
}

data class NotificationData(
    val pushNotifications: Boolean = true,
    val studyReminders: Boolean = true,
    val studyReminderTime: TimeValue = TimeValue(9, 0),
    val achievementAlerts: Boolean = true,
    val emailSummaries: Boolean = false,
    val emailFrequency: EmailFrequency = EmailFrequency.WEEKLY
)

data class GamificationData(
    val streakTracking: Boolean = true,
    val pointsRewards: Boolean = true,
    val celebrationEffects: Boolean = true,
    val streakRiskWarnings: Boolean = true
)
