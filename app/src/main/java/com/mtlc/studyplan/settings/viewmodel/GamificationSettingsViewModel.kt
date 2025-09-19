package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorType
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.ui.BaseSettingsUiState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing gamification settings
 */
class GamificationSettingsViewModel(
    private val repository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    data class GamificationUiState(
        override val isLoading: Boolean = false,
        override val isError: Boolean = false,
        override val isSuccess: Boolean = false,
        val settings: List<SettingItem> = emptyList(),
        override val error: AppError? = null,
        val streakTracking: Boolean = true,
        val pointsRewards: Boolean = true,
        val celebrationEffects: Boolean = true,
        val streakRiskWarnings: Boolean = true
    ) : BaseSettingsUiState

    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    init {
        loadGamificationSettings()
    }

    /**
     * Load gamification settings
     */
    private fun loadGamificationSettings() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isError = false
        )

        viewModelScope.launch(exceptionHandler) {
            try {
                repository.getGamificationSettings()
                    .catch { exception ->
                        handleError(exception)
                    }
                    .collectLatest { gamificationData ->
                        val settings = buildGamificationSettingsList(gamificationData)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isError = false,
                            isSuccess = true,
                            settings = settings,
                            streakTracking = gamificationData.streakTracking,
                            pointsRewards = gamificationData.pointsRewards,
                            celebrationEffects = gamificationData.celebrationEffects,
                            streakRiskWarnings = gamificationData.streakRiskWarnings,
                            currentStreak = gamificationData.currentStreak,
                            totalPoints = gamificationData.totalPoints,
                            error = null
                        )
                    }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Build gamification settings list
     */
    private fun buildGamificationSettingsList(gamificationData: GamificationData): List<SettingItem> {
        return listOf(
            // Streak Tracking Toggle
            ToggleSetting(
                id = "streak_tracking",
                title = "Streak Tracking",
                description = "Track daily study streaks (Current: ${gamificationData.currentStreak} days)",
                value = gamificationData.streakTracking,
                key = SettingsKeys.Gamification.STREAK_TRACKING,
                defaultValue = true,
                isEnabled = true,
                category = "gamification",
                sortOrder = 1
            ),

            // Points & Rewards Toggle
            ToggleSetting(
                id = "points_rewards",
                title = "Points & Rewards",
                description = "Earn points for completed tasks (Total: ${gamificationData.totalPoints} points)",
                value = gamificationData.pointsRewards,
                key = SettingsKeys.Gamification.POINTS_REWARDS,
                defaultValue = true,
                isEnabled = true,
                category = "gamification",
                sortOrder = 2
            ),

            // Celebration Effects Toggle
            ToggleSetting(
                id = "celebration_effects",
                title = "Celebration Effects",
                description = "Show confetti and animations when achieving milestones",
                value = gamificationData.celebrationEffects,
                key = SettingsKeys.Gamification.CELEBRATION_EFFECTS,
                defaultValue = true,
                isEnabled = true,
                category = "gamification",
                sortOrder = 3
            ),

            // Streak Risk Warnings Toggle
            ToggleSetting(
                id = "streak_risk_warnings",
                title = "Streak Risk Warnings",
                description = "Get notified when your streak is at risk of breaking",
                value = gamificationData.streakRiskWarnings,
                key = SettingsKeys.Gamification.STREAK_RISK_WARNINGS,
                defaultValue = true,
                isEnabled = gamificationData.streakTracking,
                category = "gamification",
                sortOrder = 4
            ),

            // Reset Streak Action (only show if streak tracking is enabled and streak > 0)
            *if (gamificationData.streakTracking && gamificationData.currentStreak > 0) {
                arrayOf(
                    ActionSetting(
                        id = "reset_streak",
                        title = "Reset Current Streak",
                        description = "Reset your ${gamificationData.currentStreak}-day streak to 0",
                        buttonText = "Reset",
                        actionType = ActionSetting.ActionType.DESTRUCTIVE,
                        isEnabled = true,
                        category = "gamification",
                        sortOrder = 5
                    )
                )
            } else emptyArray(),

            // Reset Points Action (only show if points system is enabled and points > 0)
            *if (gamificationData.pointsRewards && gamificationData.totalPoints > 0) {
                arrayOf(
                    ActionSetting(
                        id = "reset_points",
                        title = "Reset All Points",
                        description = "Reset your ${gamificationData.totalPoints} points to 0",
                        buttonText = "Reset",
                        actionType = ActionSetting.ActionType.DESTRUCTIVE,
                        isEnabled = true,
                        category = "gamification",
                        sortOrder = 6
                    )
                )
            } else emptyArray()
        )
    }

    /**
     * Update streak tracking setting
     */
    fun updateStreakTracking(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateGamificationSetting("streak_tracking", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update points & rewards setting
     */
    fun updatePointsRewards(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateGamificationSetting("points_rewards", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update celebration effects setting
     */
    fun updateCelebrationEffects(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateGamificationSetting("celebration_effects", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update streak risk warnings setting
     */
    fun updateStreakRiskWarnings(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateGamificationSetting("streak_risk_warnings", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Reset current streak
     */
    fun resetStreak() {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.resetUserStreak()
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Reset all points
     */
    fun resetPoints() {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.resetUserPoints()
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Refresh settings
     */
    fun refresh() {
        loadGamificationSettings()
    }

    /**
     * Retry loading after error
     */
    fun retry() {
        loadGamificationSettings()
    }

    private fun handleError(exception: Throwable) {
        val appError = when (exception) {
            is AppError -> exception
            is IllegalArgumentException -> AppError(
                type = ErrorType.VALIDATION,
                message = exception.message ?: "Invalid gamification setting",
                cause = exception
            )
            else -> AppError(
                type = ErrorType.UNKNOWN,
                message = exception.message ?: "An unexpected error occurred",
                cause = exception
            )
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isError = true,
            isSuccess = false,
            error = appError
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup is handled by repository dispose
    }
}

/**
 * Data classes for gamification settings
 */
