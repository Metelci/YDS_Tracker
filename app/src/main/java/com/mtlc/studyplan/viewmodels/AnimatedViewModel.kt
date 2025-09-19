package com.mtlc.studyplan.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.shared.Achievement
import com.mtlc.studyplan.shared.AppTask
import kotlinx.coroutines.launch

abstract class AnimatedViewModel : ViewModel() {

    private val _animationTriggers = MutableLiveData<AnimationTrigger>()
    val animationTriggers: LiveData<AnimationTrigger> = _animationTriggers

    private val _uiAnimationState = MutableLiveData<UiAnimationState>()
    val uiAnimationState: LiveData<UiAnimationState> = _uiAnimationState

    protected fun triggerAnimation(trigger: AnimationTrigger) {
        _animationTriggers.value = trigger
    }

    protected fun setUiAnimationState(state: UiAnimationState) {
        _uiAnimationState.value = state
    }

    // Task completion with animation
    fun completeTaskWithAnimation(taskId: String) {
        viewModelScope.launch {
            try {
                // Start completion animation
                triggerAnimation(AnimationTrigger.TaskCompletionStart(taskId))
                setUiAnimationState(UiAnimationState.TaskCompleting(taskId))

                // Perform actual task completion
                val result = completeTask(taskId)

                // Trigger success animation
                triggerAnimation(AnimationTrigger.TaskCompletionSuccess(taskId, result))
                setUiAnimationState(UiAnimationState.TaskCompleted(taskId))

                // Check for streak/achievement animations
                if (result.streakIncreased) {
                    triggerAnimation(AnimationTrigger.StreakIncrease(result.newStreak))
                }

                if (result.achievementUnlocked.isNotEmpty()) {
                    result.achievementUnlocked.forEach { achievement ->
                        triggerAnimation(AnimationTrigger.AchievementUnlock(achievement))
                    }
                }

                // Reset animation state
                setUiAnimationState(UiAnimationState.Idle)

            } catch (e: Exception) {
                triggerAnimation(AnimationTrigger.TaskCompletionError(taskId, e.message))
                setUiAnimationState(UiAnimationState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    // Progress update with animation
    fun updateProgressWithAnimation(fromValue: Int, toValue: Int, duration: Long = 500L) {
        triggerAnimation(AnimationTrigger.ProgressUpdate(fromValue, toValue, duration))
        setUiAnimationState(UiAnimationState.ProgressUpdating(fromValue, toValue))
    }

    // Data refresh with animation
    fun refreshDataWithAnimation() {
        viewModelScope.launch {
            try {
                triggerAnimation(AnimationTrigger.DataRefreshStart)
                setUiAnimationState(UiAnimationState.DataRefreshing)

                // Perform data refresh
                refreshData()

                triggerAnimation(AnimationTrigger.DataRefreshComplete)
                setUiAnimationState(UiAnimationState.DataRefreshed)

                // Reset state after animation
                viewModelScope.launch {
                    kotlinx.coroutines.delay(1000)
                    setUiAnimationState(UiAnimationState.Idle)
                }

            } catch (e: Exception) {
                setUiAnimationState(UiAnimationState.Error(e.message ?: "Refresh failed"))
            }
        }
    }

    // Badge update with animation
    fun updateBadgeWithAnimation(badgeType: BadgeType, newCount: Int) {
        triggerAnimation(AnimationTrigger.BadgeUpdate(badgeType, newCount))
    }

    // Navigation with animation
    fun navigateWithAnimation(fromTab: String, toTab: String) {
        val direction = if (getTabIndex(toTab) > getTabIndex(fromTab)) {
            AnimationTrigger.TabSwitch.Direction.LEFT_TO_RIGHT
        } else {
            AnimationTrigger.TabSwitch.Direction.RIGHT_TO_LEFT
        }

        triggerAnimation(AnimationTrigger.TabSwitch(fromTab, toTab, direction))
        setUiAnimationState(UiAnimationState.NavigationAnimating(fromTab, toTab))
    }

    // Card reveal animation
    fun revealCardsWithAnimation(cardIds: List<String>) {
        triggerAnimation(AnimationTrigger.CardReveal(cardIds))
        setUiAnimationState(UiAnimationState.CardsRevealing(cardIds))
    }

    // Number counter animation
    fun animateNumberChange(
        counterId: String,
        fromValue: Int,
        toValue: Int,
        duration: Long = 300L
    ) {
        triggerAnimation(
            AnimationTrigger.NumberChange(counterId, fromValue, toValue, duration)
        )
    }

    // Achievement celebration
    fun celebrateAchievement(achievement: Achievement) {
        triggerAnimation(AnimationTrigger.AchievementUnlock(achievement))
        setUiAnimationState(UiAnimationState.AchievementCelebrating(achievement))
    }

    // Streak milestone celebration
    fun celebrateStreakMilestone(newStreak: Int) {
        triggerAnimation(AnimationTrigger.StreakMilestone(newStreak))
        setUiAnimationState(UiAnimationState.StreakCelebrating(newStreak))
    }

    // Loading animation
    fun startLoadingAnimation(message: String = "Loading...") {
        triggerAnimation(AnimationTrigger.LoadingStart(message))
        setUiAnimationState(UiAnimationState.Loading(message))
    }

    fun stopLoadingAnimation() {
        triggerAnimation(AnimationTrigger.LoadingEnd)
        setUiAnimationState(UiAnimationState.Idle)
    }

    // Error animation
    fun showErrorWithAnimation(error: String) {
        triggerAnimation(AnimationTrigger.ErrorDisplay(error))
        setUiAnimationState(UiAnimationState.Error(error))
    }

    // Success animation
    fun showSuccessWithAnimation(message: String) {
        triggerAnimation(AnimationTrigger.SuccessDisplay(message))
        setUiAnimationState(UiAnimationState.Success(message))
    }

    // Abstract methods for subclasses to implement
    protected abstract suspend fun completeTask(taskId: String): TaskCompletionResult
    protected abstract suspend fun refreshData()

    private fun getTabIndex(tabId: String): Int {
        return when (tabId) {
            "home" -> 0
            "tasks" -> 1
            "progress" -> 2
            "social" -> 3
            "settings" -> 4
            else -> 0
        }
    }

    // Animation queue management
    private val animationQueue = mutableListOf<AnimationTrigger>()
    private var isProcessingQueue = false

    protected fun queueAnimation(trigger: AnimationTrigger) {
        animationQueue.add(trigger)
        processAnimationQueue()
    }

    private fun processAnimationQueue() {
        if (isProcessingQueue || animationQueue.isEmpty()) return

        isProcessingQueue = true
        viewModelScope.launch {
            while (animationQueue.isNotEmpty()) {
                val animation = animationQueue.removeAt(0)
                triggerAnimation(animation)

                // Wait for animation to complete
                kotlinx.coroutines.delay(animation.duration)
            }
            isProcessingQueue = false
        }
    }

    // Animation state management
    fun isAnimationInProgress(): Boolean {
        return when (_uiAnimationState.value) {
            is UiAnimationState.Idle -> false
            is UiAnimationState.Error -> false
            else -> true
        }
    }

    fun clearAnimationState() {
        setUiAnimationState(UiAnimationState.Idle)
    }
}

// Animation trigger types
sealed class AnimationTrigger(val duration: Long = 300L) {
    data class TaskCompletionStart(val taskId: String) : AnimationTrigger(150L)
    data class TaskCompletionSuccess(val taskId: String, val result: TaskCompletionResult) : AnimationTrigger(500L)
    data class TaskCompletionError(val taskId: String, val error: String?) : AnimationTrigger(300L)
    data class StreakIncrease(val newStreak: Int) : AnimationTrigger(750L)
    data class StreakMilestone(val streak: Int) : AnimationTrigger(1000L)
    data class AchievementUnlock(val achievement: Achievement) : AnimationTrigger(1500L)
    data class ProgressUpdate(val fromValue: Int, val toValue: Int, val animationDuration: Long = 500L) : AnimationTrigger(animationDuration)
    data class BadgeUpdate(val badgeType: BadgeType, val newCount: Int) : AnimationTrigger(300L)
    object DataRefreshStart : AnimationTrigger(200L)
    object DataRefreshComplete : AnimationTrigger(400L)
    data class TabSwitch(val fromTab: String, val toTab: String, val direction: Direction) : AnimationTrigger(300L) {
        enum class Direction { LEFT_TO_RIGHT, RIGHT_TO_LEFT }
    }
    data class CardReveal(val cardIds: List<String>) : AnimationTrigger(600L)
    data class NumberChange(val counterId: String, val fromValue: Int, val toValue: Int, val animationDuration: Long) : AnimationTrigger(animationDuration)
    data class LoadingStart(val message: String) : AnimationTrigger(200L)
    object LoadingEnd : AnimationTrigger(200L)
    data class ErrorDisplay(val error: String) : AnimationTrigger(400L)
    data class SuccessDisplay(val message: String) : AnimationTrigger(600L)
}

// UI animation states
sealed class UiAnimationState {
    object Idle : UiAnimationState()
    data class Loading(val message: String) : UiAnimationState()
    data class TaskCompleting(val taskId: String) : UiAnimationState()
    data class TaskCompleted(val taskId: String) : UiAnimationState()
    data class ProgressUpdating(val fromValue: Int, val toValue: Int) : UiAnimationState()
    object DataRefreshing : UiAnimationState()
    object DataRefreshed : UiAnimationState()
    data class NavigationAnimating(val fromTab: String, val toTab: String) : UiAnimationState()
    data class CardsRevealing(val cardIds: List<String>) : UiAnimationState()
    data class AchievementCelebrating(val achievement: Achievement) : UiAnimationState()
    data class StreakCelebrating(val streak: Int) : UiAnimationState()
    data class Error(val message: String) : UiAnimationState()
    data class Success(val message: String) : UiAnimationState()
}

// Supporting data classes
data class TaskCompletionResult(
    val task: AppTask,
    val pointsEarned: Int,
    val streakIncreased: Boolean,
    val newStreak: Int,
    val achievementUnlocked: List<Achievement>
)

enum class BadgeType {
    TASKS, SOCIAL, PROGRESS, SETTINGS, STREAK_WARNING
}