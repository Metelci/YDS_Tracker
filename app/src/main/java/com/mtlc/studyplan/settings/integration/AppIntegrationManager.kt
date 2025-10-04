package com.mtlc.studyplan.settings.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.gamification.GamificationManager
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.settings.data.SettingsRepository
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Master integration manager that coordinates all app subsystems with settings
 */
class AppIntegrationManager(
    private val context: Context,
    settingsRepository: SettingsRepository,
    gamificationManager: GamificationManager
) {

    // Integration components
    val themeIntegration = ThemeIntegration(context, settingsRepository)
    private val notificationManager: NotificationManager by lazy {
        resolveNotificationManager(context)
    }
    val notificationIntegration = NotificationIntegration(context, settingsRepository, notificationManager)
    val gamificationIntegration = GamificationIntegration(context, settingsRepository, gamificationManager)

    // Feedback and migration
    private val feedbackManager = com.mtlc.studyplan.settings.feedback.SettingsFeedbackManager(context)
    val migrationIntegration = MigrationIntegration(context, settingsRepository, feedbackManager)

    // Edge case handling
    data class AppState(
        val themeState: ThemeIntegration.ThemeState,
        val notificationState: NotificationIntegration.NotificationState,
        val gamificationState: GamificationIntegration.GamificationState,
        val migrationState: MigrationIntegration.MigrationState,
        val isInitialized: Boolean = false
    )

    private val _appState = MutableStateFlow(AppState(
        themeState = ThemeIntegration.ThemeState(),
        notificationState = NotificationIntegration.NotificationState(),
        gamificationState = GamificationIntegration.GamificationState(),
        migrationState = MigrationIntegration.MigrationState()
    ))
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        initializeIntegrations()
    }

    private fun initializeIntegrations() {
        // Combine all integration states
        combine(
            themeIntegration.themeState,
            notificationIntegration.notificationState,
            gamificationIntegration.gamificationState,
            migrationIntegration.migrationState
        ) { themeState, notificationState, gamificationState, migrationState ->
            AppState(
                themeState = themeState,
                notificationState = notificationState,
                gamificationState = gamificationState,
                migrationState = migrationState,
                isInitialized = true
            )
        }
        .onEach { _appState.value = it }
        .launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))
    }

    /**
     * Initialize app and run migrations on startup
     */
    suspend fun initializeApp(): Boolean {
        return migrationIntegration.checkAndRunMigrationsOnStartup()
    }

    /**
     * Handle task completion with all integrations
     */
    suspend fun handleTaskCompletion(
        taskId: String,
        taskDescription: String,
        taskDetails: String?,
        minutesSpent: Int,
        isCorrect: Boolean = true
    ) {
        // Complete task with gamification
        gamificationIntegration.completeTaskWithGamification(
            taskId, taskDescription, taskDetails, minutesSpent, isCorrect
        )

        // Show achievement notification if enabled
        if (gamificationIntegration.shouldShowAchievement()) {
            notificationIntegration.showAchievementNotificationIfEnabled(
                "Achievement Unlocked! 🏆",
                "Great job completing: $taskDescription"
            )
        }
    }

    /**
     * Handle streak warning
     */
    fun handleStreakWarning(streakDays: Int) {
        if (gamificationIntegration.isFeatureEnabled(GamificationFeature.STREAK_RISK_WARNINGS)) {
            notificationIntegration.showStreakWarningIfEnabled(streakDays)
        }
    }

    /**
     * Handle goal reminder
     */
    fun handleGoalReminder(goalDescription: String) {
        notificationIntegration.showGoalReminderIfEnabled(goalDescription)
    }

    /**
     * Handle daily study reminder
     */
    fun handleDailyStudyReminder() {
        notificationIntegration.showStudyReminderIfEnabled()
    }

    /**
     * Check if feature is enabled across integrations
     */
    fun isFeatureEnabled(feature: AppFeature): Boolean {
        return when (feature) {
            AppFeature.DARK_THEME -> _appState.value.themeState.darkTheme
            AppFeature.DYNAMIC_COLORS -> _appState.value.themeState.dynamicColor
            AppFeature.REDUCED_MOTION -> _appState.value.themeState.reducedMotion
            AppFeature.HIGH_CONTRAST -> _appState.value.themeState.highContrast
            AppFeature.PUSH_NOTIFICATIONS -> _appState.value.notificationState.pushNotificationsEnabled
            AppFeature.STUDY_REMINDERS -> _appState.value.notificationState.studyRemindersEnabled
            AppFeature.ACHIEVEMENT_ALERTS -> _appState.value.notificationState.achievementAlertsEnabled
            AppFeature.QUIET_HOURS -> _appState.value.notificationState.quietHoursEnabled
            AppFeature.STREAK_TRACKING -> _appState.value.gamificationState.streakTrackingEnabled
            AppFeature.POINTS_REWARDS -> _appState.value.gamificationState.pointsRewardsEnabled
            AppFeature.CELEBRATION_EFFECTS -> _appState.value.gamificationState.celebrationEffectsEnabled
            AppFeature.ACHIEVEMENT_BADGES -> _appState.value.gamificationState.achievementBadgesEnabled
            AppFeature.DAILY_CHALLENGES -> _appState.value.gamificationState.dailyChallengesEnabled
            AppFeature.LEADERBOARD -> _appState.value.gamificationState.leaderboardEnabled
        }
    }

    /**
     * Get current theme configuration
     */
    fun getCurrentThemeConfig(): ThemeConfig {
        val themeState = _appState.value.themeState
        return ThemeConfig(
            isDarkTheme = themeState.darkTheme,
            useDynamicColors = themeState.dynamicColor,
            fontScale = themeState.fontSize,
            animationScale = themeState.animationSpeed,
            useReducedMotion = themeState.reducedMotion,
            useHighContrast = themeState.highContrast
        )
    }

    /**
     * Get notification configuration
     */
    fun getNotificationConfig(): NotificationConfig {
        val notificationState = _appState.value.notificationState
        return NotificationConfig(
            areNotificationsEnabled = notificationState.pushNotificationsEnabled,
            allowStudyReminders = notificationState.studyRemindersEnabled,
            allowAchievementAlerts = notificationState.achievementAlertsEnabled,
            allowStreakWarnings = notificationState.streakWarningsEnabled,
            allowGoalReminders = notificationState.goalRemindersEnabled,
            useQuietHours = notificationState.quietHoursEnabled,
            quietHoursStart = notificationState.quietHoursStart,
            quietHoursEnd = notificationState.quietHoursEnd,
            useVibration = notificationState.vibrationEnabled
        )
    }

    /**
     * Get gamification configuration
     */
    fun getGamificationConfig(): GamificationConfig {
        val gamificationState = _appState.value.gamificationState
        return GamificationConfig(
            enableStreakTracking = gamificationState.streakTrackingEnabled,
            enablePointsRewards = gamificationState.pointsRewardsEnabled,
            enableCelebrationEffects = gamificationState.celebrationEffectsEnabled,
            enableAchievementBadges = gamificationState.achievementBadgesEnabled,
            enableLevelProgression = gamificationState.levelProgressionEnabled,
            enableDailyChallenges = gamificationState.dailyChallengesEnabled,
            enableLeaderboard = gamificationState.leaderboardEnabled,
            enableRewardAnimations = gamificationState.rewardAnimationsEnabled
        )
    }
}

enum class AppFeature {
    DARK_THEME,
    DYNAMIC_COLORS,
    REDUCED_MOTION,
    HIGH_CONTRAST,
    PUSH_NOTIFICATIONS,
    STUDY_REMINDERS,
    ACHIEVEMENT_ALERTS,
    QUIET_HOURS,
    STREAK_TRACKING,
    POINTS_REWARDS,
    CELEBRATION_EFFECTS,
    ACHIEVEMENT_BADGES,
    DAILY_CHALLENGES,
    LEADERBOARD
}

data class ThemeConfig(
    val isDarkTheme: Boolean,
    val useDynamicColors: Boolean,
    val fontScale: Float,
    val animationScale: Float,
    val useReducedMotion: Boolean,
    val useHighContrast: Boolean
)

data class NotificationConfig(
    val areNotificationsEnabled: Boolean,
    val allowStudyReminders: Boolean,
    val allowAchievementAlerts: Boolean,
    val allowStreakWarnings: Boolean,
    val allowGoalReminders: Boolean,
    val useQuietHours: Boolean,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val useVibration: Boolean
)

data class GamificationConfig(
    val enableStreakTracking: Boolean,
    val enablePointsRewards: Boolean,
    val enableCelebrationEffects: Boolean,
    val enableAchievementBadges: Boolean,
    val enableLevelProgression: Boolean,
    val enableDailyChallenges: Boolean,
    val enableLeaderboard: Boolean,
    val enableRewardAnimations: Boolean
)

/**
 * ViewModel for app-wide integration management
 */
private fun resolveNotificationManager(context: Context): NotificationManager {
    return GlobalContext.get().get()
}

class AppIntegrationViewModel(
    private val appIntegrationManager: AppIntegrationManager
) : ViewModel() {

    val appState = appIntegrationManager.appState

    val themeIntegration = appIntegrationManager.themeIntegration
    val notificationIntegration = appIntegrationManager.notificationIntegration
    val gamificationIntegration = appIntegrationManager.gamificationIntegration

    fun handleTaskCompletion(
        taskId: String,
        taskDescription: String,
        taskDetails: String?,
        minutesSpent: Int,
        isCorrect: Boolean = true
    ) {
        viewModelScope.launch {
            appIntegrationManager.handleTaskCompletion(
                taskId, taskDescription, taskDetails, minutesSpent, isCorrect
            )
        }
    }

    fun handleStreakWarning(streakDays: Int) {
        appIntegrationManager.handleStreakWarning(streakDays)
    }

    fun handleGoalReminder(goalDescription: String) {
        appIntegrationManager.handleGoalReminder(goalDescription)
    }

    fun handleDailyStudyReminder() {
        appIntegrationManager.handleDailyStudyReminder()
    }

    fun isFeatureEnabled(feature: AppFeature): Boolean {
        return appIntegrationManager.isFeatureEnabled(feature)
    }

    fun getCurrentThemeConfig(): ThemeConfig {
        return appIntegrationManager.getCurrentThemeConfig()
    }

    fun getNotificationConfig(): NotificationConfig {
        return appIntegrationManager.getNotificationConfig()
    }

    fun getGamificationConfig(): GamificationConfig {
        return appIntegrationManager.getGamificationConfig()
    }
}
