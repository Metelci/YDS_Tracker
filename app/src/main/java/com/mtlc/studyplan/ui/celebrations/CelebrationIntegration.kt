package com.mtlc.studyplan.ui.celebrations

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.ui.celebrations.rememberCelebrationState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Integration layer between celebrations and existing progress/achievement systems
 */
class CelebrationIntegrationViewModel(
    private val progressRepository: ProgressRepository,
    private val achievementTracker: AchievementTracker
) : ViewModel() {

    private val _celebrationEvents = MutableStateFlow<List<CelebrationEvent>>(emptyList())
    val celebrationEvents: StateFlow<List<CelebrationEvent>> = _celebrationEvents.asStateFlow()

    /**
     * Monitor progress changes and trigger appropriate celebrations
     */
    fun observeProgressForCelebrations() {
        // Monitor task completions
        viewModelScope.launch {
            progressRepository.userProgressFlow
                .distinctUntilChanged { old, new -> old.completedTasks.size == new.completedTasks.size }
                .collect { userProgress ->
                    handleTaskProgressUpdate(userProgress)
                }
        }

        // Monitor achievement unlocks
        viewModelScope.launch {
            achievementTracker.achievementStateFlow
                .distinctUntilChanged { old, new -> old.unlockedAchievements.size == new.unlockedAchievements.size }
                .collect { achievementState ->
                    handleAchievementUpdate(achievementState)
                }
        }

        // Monitor streak changes
        viewModelScope.launch {
            progressRepository.userProgressFlow
                .distinctUntilChanged { old, new -> old.streakCount == new.streakCount }
                .collect { userProgress ->
                    handleStreakUpdate(userProgress)
                }
        }
    }

    /**
     * Handle task progress updates
     */
    private suspend fun handleTaskProgressUpdate(userProgress: UserProgress) {
        val taskLogs = progressRepository.taskLogsFlow.first()
        val todayLogs = getTodayTaskLogs(taskLogs)

        // Check for daily goal achievement
        if (isDailyGoalAchieved(todayLogs)) {
            triggerDailyGoalCelebration(todayLogs, userProgress)
        }

        // Check for milestone completions
        checkWeeklyMilestones(userProgress, taskLogs)
        checkMonthlyMilestones(userProgress, taskLogs)
    }

    /**
     * Handle achievement unlocks
     */
    private fun handleAchievementUpdate(achievementState: AchievementState) {
        // This would typically be called when new achievements are unlocked
        // The actual achievement unlock celebrations are handled in the AchievementTracker
        // This method serves as an additional integration point if needed
    }

    /**
     * Handle streak milestone updates
     */
    private fun handleStreakUpdate(userProgress: UserProgress) {
        val streakCount = userProgress.streakCount

        // Trigger streak milestone celebrations
        when (streakCount) {
            7, 14, 30, 50, 100 -> {
                triggerStreakMilestoneCelebration(streakCount)
            }
            else -> {
                // Check for other significant streak milestones (every 50 days after 100)
                if (streakCount > 100 && streakCount % 50 == 0) {
                    triggerStreakMilestoneCelebration(streakCount)
                }
            }
        }
    }

    /**
     * Trigger task completion celebration
     */
    fun triggerTaskCompletionCelebration(
        taskId: String,
        taskDescription: String,
        minutesSpent: Int
    ) {
        val category = TaskCategory.fromString(taskDescription)
        val points = category.basePoints

        val celebration = CelebrationEvent(
            type = CelebrationType.TaskCompletion(
                taskId = taskId,
                points = points,
                taskCategory = category
            )
        )

        addCelebration(celebration)
    }

    /**
     * Trigger daily goal celebration
     */
    private fun triggerDailyGoalCelebration(
        todayLogs: List<TaskLog>,
        userProgress: UserProgress
    ) {
        val tasksCompleted = todayLogs.size
        val totalTasks = getExpectedDailyTasks() // This would come from your daily planning system
        val pointsEarned = todayLogs.sumOf { it.pointsEarned }

        val celebration = CelebrationEvent(
            type = CelebrationType.DailyGoalAchieved(
                tasksCompleted = tasksCompleted,
                totalTasks = totalTasks,
                streakCount = userProgress.streakCount,
                pointsEarned = pointsEarned
            )
        )

        addCelebration(celebration)
    }

    /**
     * Trigger achievement level up celebration
     */
    fun triggerAchievementCelebration(achievementUnlock: AchievementUnlock) {
        if (achievementUnlock.isNewTier) {
            val celebration = CelebrationEvent(
                type = CelebrationType.LevelUp(
                    achievement = achievementUnlock.achievement,
                    newLevel = achievementUnlock.achievement.tier,
                    totalPoints = achievementUnlock.totalCategoryPoints
                )
            )

            addCelebration(celebration)
        }
    }

    /**
     * Trigger streak milestone celebration
     */
    private fun triggerStreakMilestoneCelebration(streakDays: Int) {
        val points = calculateStreakMilestonePoints(streakDays)

        val celebration = CelebrationEvent(
            type = CelebrationType.MilestoneReward(
                milestoneType = MilestoneType.STREAK_MILESTONE,
                value = streakDays,
                reward = "Streak Fire Badge Earned!",
                points = points
            )
        )

        addCelebration(celebration)
    }

    /**
     * Check weekly milestones
     */
    private suspend fun checkWeeklyMilestones(userProgress: UserProgress, taskLogs: List<TaskLog>) {
        // This would integrate with your weekly planning system
        // For now, using a simplified check based on task count
        val weeklyTasks = getWeeklyTaskLogs(taskLogs)
        val expectedWeeklyTasks = 35 // 5 tasks per day * 7 days

        if (weeklyTasks.size >= expectedWeeklyTasks) {
            val currentWeek = getCurrentWeek() // This would come from your planning system

            val celebration = CelebrationEvent(
                type = CelebrationType.MilestoneReward(
                    milestoneType = MilestoneType.WEEK_COMPLETION,
                    value = currentWeek,
                    reward = "Week $currentWeek Badge Unlocked!",
                    points = 500 + (currentWeek * 50)
                )
            )

            addCelebration(celebration)
        }
    }

    /**
     * Check monthly milestones
     */
    private suspend fun checkMonthlyMilestones(userProgress: UserProgress, taskLogs: List<TaskLog>) {
        val monthlyTasks = getMonthlyTaskLogs(taskLogs)
        val expectedMonthlyTasks = 150 // Approximate monthly goal

        if (monthlyTasks.size >= expectedMonthlyTasks) {
            val currentMonth = getCurrentMonth()

            val celebration = CelebrationEvent(
                type = CelebrationType.MilestoneReward(
                    milestoneType = MilestoneType.MONTH_COMPLETION,
                    value = currentMonth,
                    reward = "Monthly Champion Trophy Earned!",
                    points = 2000 + (currentMonth * 200)
                )
            )

            addCelebration(celebration)
        }
    }

    /**
     * Add celebration to the queue
     */
    private fun addCelebration(celebration: CelebrationEvent) {
        _celebrationEvents.value = _celebrationEvents.value + celebration
    }

    /**
     * Remove completed celebration
     */
    fun completeCelebration(celebrationId: String) {
        _celebrationEvents.value = _celebrationEvents.value.filter { it.id != celebrationId }
    }

    /**
     * Clear all celebrations
     */
    fun clearAllCelebrations() {
        _celebrationEvents.value = emptyList()
    }

    // Helper functions (these would integrate with your actual planning system)

    private fun getTodayTaskLogs(taskLogs: List<TaskLog>): List<TaskLog> {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return taskLogs.filter { it.timestampMillis >= todayStart }
    }

    private fun getWeeklyTaskLogs(taskLogs: List<TaskLog>): List<TaskLog> {
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return taskLogs.filter { it.timestampMillis >= weekStart }
    }

    private fun getMonthlyTaskLogs(taskLogs: List<TaskLog>): List<TaskLog> {
        val monthStart = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        return taskLogs.filter { it.timestampMillis >= monthStart }
    }

    private fun isDailyGoalAchieved(todayLogs: List<TaskLog>): Boolean {
        // This would integrate with your daily planning logic
        val expectedTasks = getExpectedDailyTasks()
        return todayLogs.size >= expectedTasks
    }

    private fun getExpectedDailyTasks(): Int {
        // This would come from your planning system
        return 5 // Default expectation
    }

    private fun getCurrentWeek(): Int {
        // This would integrate with your week tracking system
        return ((System.currentTimeMillis() / (7 * 24 * 60 * 60 * 1000)) % 52).toInt() + 1
    }

    private fun getCurrentMonth(): Int {
        // This would integrate with your month tracking system
        return java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
    }

    private fun calculateStreakMilestonePoints(streakDays: Int): Int {
        return when (streakDays) {
            7 -> 300
            14 -> 600
            30 -> 1500
            50 -> 3000
            100 -> 5000
            else -> if (streakDays % 50 == 0) streakDays * 50 else 1000
        }
    }
}

/**
 * Main integration composable for the celebration system
 */
@Composable
fun CelebrationSystemIntegration(
    progressRepository: ProgressRepository,
    achievementTracker: AchievementTracker,
    soundEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val integrationViewModel = remember {
        CelebrationIntegrationViewModel(progressRepository, achievementTracker)
    }

    val celebrationEvents by integrationViewModel.celebrationEvents.collectAsState()

    // Start monitoring progress
    LaunchedEffect(Unit) {
        integrationViewModel.observeProgressForCelebrations()
    }

    Box(modifier = modifier) {
        // Main app content
        content()

        // Celebration overlay
        CelebrationManagerWithSound(
            celebrations = celebrationEvents,
            onCelebrationComplete = integrationViewModel::completeCelebration,
            soundEnabled = soundEnabled
        )
    }
}

/**
 * Enhanced task completion with celebration integration
 */
@Composable
fun CelebrationIntegratedTaskCompletion(
    taskId: String,
    taskDescription: String,
    taskDetails: String?,
    minutesSpent: Int,
    onComplete: () -> Unit,
    celebrationIntegration: CelebrationIntegrationViewModel,
    progressRepository: ProgressRepository
) {
    LaunchedEffect(Unit) {
        // Complete the task with points and celebration
        val transaction = progressRepository.completeTaskWithPoints(
            taskId = taskId,
            taskDescription = taskDescription,
            taskDetails = taskDetails,
            minutesSpent = minutesSpent
        )

        // Trigger celebration
        celebrationIntegration.triggerTaskCompletionCelebration(
            taskId = taskId,
            taskDescription = taskDescription,
            minutesSpent = minutesSpent
        )

        // Check for achievement unlocks
        val achievementUnlocks = progressRepository.checkAchievementUnlocks()
        achievementUnlocks.forEach { unlock ->
            celebrationIntegration.triggerAchievementCelebration(unlock)
        }

        onComplete()
    }
}

/**
 * Celebration-aware progress tracking
 */
@Composable
fun CelebrationProgressTracker(
    progressRepository: ProgressRepository,
    achievementTracker: AchievementTracker,
    celebrationState: CelebrationState,
    content: @Composable (userProgress: UserProgress, achievementState: AchievementState) -> Unit
) {
    val userProgress by progressRepository.userProgressFlow.collectAsState(initial = UserProgress())
    val achievementState by achievementTracker.achievementStateFlow.collectAsState(
        initial = AchievementState(
            categoryProgress = emptyMap(),
            unlockedAchievements = emptySet(),
            totalAchievements = 0,
            totalPoints = 0
        )
    )

    content(userProgress, achievementState)
}

/**
 * Helper composable for integrating celebrations with existing UI components
 */
@Composable
fun WithCelebrations(
    celebrationState: CelebrationState = rememberCelebrationState(),
    soundEnabled: Boolean = true,
    content: @Composable (CelebrationState) -> Unit
) {
    Box {
        content(celebrationState)

        CelebrationManagerWithSound(
            celebrations = celebrationState.celebrations,
            onCelebrationComplete = celebrationState::completeCelebration,
            soundEnabled = soundEnabled
        )
    }
}
