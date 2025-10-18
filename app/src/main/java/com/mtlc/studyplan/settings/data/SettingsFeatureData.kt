package com.mtlc.studyplan.settings.data

data class PrivacyData(
    val crashReporting: Boolean = true
)

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

