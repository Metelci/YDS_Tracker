package com.mtlc.studyplan.settings.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.gamification.GamificationManager
import com.mtlc.studyplan.settings.data.SettingsKeys
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.SettingsUpdateRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Integration layer between settings and gamification system
 */
class GamificationIntegration(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val gamificationManager: GamificationManager
) {

    data class GamificationState(
        val streakTrackingEnabled: Boolean = true,
        val pointsRewardsEnabled: Boolean = true,
        val celebrationEffectsEnabled: Boolean = true,
        val streakRiskWarningsEnabled: Boolean = true,
        val achievementBadgesEnabled: Boolean = true,
        val levelProgressionEnabled: Boolean = true,
        val dailyChallengesEnabled: Boolean = true,
        val xpMultipliersEnabled: Boolean = true,
        val rewardAnimationsEnabled: Boolean = true
    )

    private val _gamificationState = MutableStateFlow(GamificationState())
    val gamificationState: StateFlow<GamificationState> = _gamificationState.asStateFlow()

    init {
        observeGamificationSettings()
    }

    private fun observeGamificationSettings() {
        settingsRepository.settingsState
            .map { settings ->
                GamificationState(
                    streakTrackingEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.STREAK_TRACKING, true),
                    pointsRewardsEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.POINTS_REWARDS, true),
                    celebrationEffectsEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.CELEBRATION_EFFECTS, true),
                    streakRiskWarningsEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.STREAK_RISK_WARNINGS, true),
                    achievementBadgesEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.ACHIEVEMENT_BADGES, true),
                    levelProgressionEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.LEVEL_PROGRESSION, true),
                    dailyChallengesEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.DAILY_CHALLENGES, true),
                    xpMultipliersEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.XP_MULTIPLIERS, true),
                    rewardAnimationsEnabled = settingsRepository.getBoolean(SettingsKeys.Gamification.REWARD_ANIMATIONS, true)
                )
            }
            .onEach { _gamificationState.value = it }
            .launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))
    }

    /**
     * Check if gamification feature is enabled
     */
    fun isFeatureEnabled(feature: GamificationFeature): Boolean {
        return when (feature) {
            GamificationFeature.STREAK_TRACKING -> _gamificationState.value.streakTrackingEnabled
            GamificationFeature.POINTS_REWARDS -> _gamificationState.value.pointsRewardsEnabled
            GamificationFeature.CELEBRATION_EFFECTS -> _gamificationState.value.celebrationEffectsEnabled
            GamificationFeature.STREAK_RISK_WARNINGS -> _gamificationState.value.streakRiskWarningsEnabled
            GamificationFeature.ACHIEVEMENT_BADGES -> _gamificationState.value.achievementBadgesEnabled
            GamificationFeature.LEVEL_PROGRESSION -> _gamificationState.value.levelProgressionEnabled
            GamificationFeature.DAILY_CHALLENGES -> _gamificationState.value.dailyChallengesEnabled
            GamificationFeature.XP_MULTIPLIERS -> _gamificationState.value.xpMultipliersEnabled
            GamificationFeature.REWARD_ANIMATIONS -> _gamificationState.value.rewardAnimationsEnabled
        }
    }

    /**
     * Complete task with gamification if enabled
     */
    suspend fun completeTaskWithGamification(
        taskId: String,
        taskDescription: String,
        taskDetails: String?,
        minutesSpent: Int,
        isCorrect: Boolean = true
    ) {
        if (!isFeatureEnabled(GamificationFeature.POINTS_REWARDS)) return

        gamificationManager.completeTaskWithGamification(
            taskId = taskId,
            taskDescription = taskDescription,
            taskDetails = taskDetails,
            minutesSpent = minutesSpent,
            isCorrect = isCorrect
        )
    }

    /**
     * Show achievement if enabled
     */
    fun shouldShowAchievement(): Boolean {
        return isFeatureEnabled(GamificationFeature.ACHIEVEMENT_BADGES)
    }

    /**
     * Show level progression if enabled
     */
    fun shouldShowLevelProgression(): Boolean {
        return isFeatureEnabled(GamificationFeature.LEVEL_PROGRESSION)
    }

    /**
     * Show celebration effects if enabled
     */
    fun shouldShowCelebrationEffects(): Boolean {
        return isFeatureEnabled(GamificationFeature.CELEBRATION_EFFECTS) &&
                isFeatureEnabled(GamificationFeature.REWARD_ANIMATIONS)
    }

    /**
     * Get daily challenge if enabled
     */
    suspend fun getDailyChallengeIfEnabled() = if (isFeatureEnabled(GamificationFeature.DAILY_CHALLENGES)) {
        gamificationManager.generateNewDailyChallenge()
    } else null

    /**
     * Update gamification settings
     */
    suspend fun toggleStreakTracking() {
        val current = _gamificationState.value.streakTrackingEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.STREAK_TRACKING, !current))
    }

    suspend fun togglePointsRewards() {
        val current = _gamificationState.value.pointsRewardsEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.POINTS_REWARDS, !current))
    }

    suspend fun toggleCelebrationEffects() {
        val current = _gamificationState.value.celebrationEffectsEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.CELEBRATION_EFFECTS, !current))
    }

    suspend fun toggleStreakRiskWarnings() {
        val current = _gamificationState.value.streakRiskWarningsEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.STREAK_RISK_WARNINGS, !current))
    }

    suspend fun toggleAchievementBadges() {
        val current = _gamificationState.value.achievementBadgesEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.ACHIEVEMENT_BADGES, !current))
    }

    suspend fun toggleLevelProgression() {
        val current = _gamificationState.value.levelProgressionEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.LEVEL_PROGRESSION, !current))
    }

    suspend fun toggleDailyChallenges() {
        val current = _gamificationState.value.dailyChallengesEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.DAILY_CHALLENGES, !current))
    }

    suspend fun toggleXpMultipliers() {
        val current = _gamificationState.value.xpMultipliersEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.XP_MULTIPLIERS, !current))
    }

    suspend fun toggleRewardAnimations() {
        val current = _gamificationState.value.rewardAnimationsEnabled
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(SettingsKeys.Gamification.REWARD_ANIMATIONS, !current))
    }
}

enum class GamificationFeature {
    STREAK_TRACKING,
    POINTS_REWARDS,
    CELEBRATION_EFFECTS,
    STREAK_RISK_WARNINGS,
    ACHIEVEMENT_BADGES,
    LEVEL_PROGRESSION,
    DAILY_CHALLENGES,
    XP_MULTIPLIERS,
    REWARD_ANIMATIONS
}

/**
 * ViewModel for gamification management in UI
 */
class GamificationViewModel(
    private val gamificationIntegration: GamificationIntegration
) : ViewModel() {

    val gamificationState = gamificationIntegration.gamificationState

    fun toggleStreakTracking() {
        viewModelScope.launch {
            gamificationIntegration.toggleStreakTracking()
        }
    }

    fun togglePointsRewards() {
        viewModelScope.launch {
            gamificationIntegration.togglePointsRewards()
        }
    }

    fun toggleCelebrationEffects() {
        viewModelScope.launch {
            gamificationIntegration.toggleCelebrationEffects()
        }
    }

    fun toggleStreakRiskWarnings() {
        viewModelScope.launch {
            gamificationIntegration.toggleStreakRiskWarnings()
        }
    }

    fun toggleAchievementBadges() {
        viewModelScope.launch {
            gamificationIntegration.toggleAchievementBadges()
        }
    }

    fun toggleLevelProgression() {
        viewModelScope.launch {
            gamificationIntegration.toggleLevelProgression()
        }
    }

    fun toggleDailyChallenges() {
        viewModelScope.launch {
            gamificationIntegration.toggleDailyChallenges()
        }
    }

    fun toggleXpMultipliers() {
        viewModelScope.launch {
            gamificationIntegration.toggleXpMultipliers()
        }
    }

    fun toggleRewardAnimations() {
        viewModelScope.launch {
            gamificationIntegration.toggleRewardAnimations()
        }
    }

    fun completeTask(
        taskId: String,
        taskDescription: String,
        taskDetails: String?,
        minutesSpent: Int,
        isCorrect: Boolean = true
    ) {
        viewModelScope.launch {
            gamificationIntegration.completeTaskWithGamification(
                taskId, taskDescription, taskDetails, minutesSpent, isCorrect
            )
        }
    }
}
