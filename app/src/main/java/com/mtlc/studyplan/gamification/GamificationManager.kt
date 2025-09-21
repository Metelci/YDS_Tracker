package com.mtlc.studyplan.gamification

import androidx.compose.runtime.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.*
// Celebration imports removed with progress functionality
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Centralized Gamification Manager - Coordinates all gamification systems
 */
class GamificationManager(
    private val dataStore: DataStore<Preferences>
) {

    // Component managers
    private val pointEconomyManager = PointEconomyManager(dataStore)
    private val advancedAchievementTracker = AdvancedAchievementTracker(dataStore)

    /**
     * Comprehensive gamification state
     */
    data class GamificationState(
        val pointWallet: PointWallet = PointWallet(),
        val achievements: List<AdvancedAchievement> = emptyList(),
        val dailyChallenge: DailyChallenge? = null,
        val weeklyChallenge: WeeklyChallenge? = null,
        val activeComebackBonus: ComebackBonus? = null,
        val studyBuddyComparison: StudyBuddyComparison? = null,
        val levelSystem: LevelSystem = LevelSystem(1, 0, 1000, 0, "Newcomer", "Learner", emptyList()),
        val availableCosmetics: List<CosmeticReward> = emptyList(),
        val ownedCosmetics: List<CosmeticReward> = emptyList(),
        val equippedCosmetics: Map<CosmeticType, CosmeticReward> = emptyMap(),
        val transactionHistory: List<PointTransaction> = emptyList(),
        val celebrationPreferences: CelebrationPreferences = CelebrationPreferences(),
        val lastUpdated: Long = System.currentTimeMillis()
    )

    /**
     * Main gamification state flow
     */
    val gamificationStateFlow: StateFlow<GamificationState> = combine(
        pointEconomyManager.pointWalletFlow,
        advancedAchievementTracker.advancedAchievementFlow,
        pointEconomyManager.transactionHistoryFlow
    ) { pointWallet, achievements, transactions ->

        val dailyChallenge = getCurrentDailyChallenge(userProgress, transactions)
        val comebackBonus = checkActiveComebackBonus(userProgress)
        val studyBuddyComparison = StudyBuddySystem.generateComparison(userProgress)
        val levelSystem = LevelSystemCalculator.calculateLevel(pointWallet.totalLifetimePoints)
        val availableCosmetics = pointEconomyManager.getAvailableCosmetics()

        GamificationState(
            pointWallet = pointWallet,
            achievements = achievements,
            dailyChallenge = dailyChallenge,
            activeComebackBonus = comebackBonus,
            studyBuddyComparison = studyBuddyComparison,
            levelSystem = levelSystem,
            availableCosmetics = availableCosmetics,
            transactionHistory = transactions.take(50) // Recent transactions
        )
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GamificationState()
    )

    /**
     * Enhanced task completion with full gamification integration
     */
    suspend fun completeTaskWithGamification(
        taskId: String,
        taskDescription: String,
        taskDetails: String?,
        minutesSpent: Int,
        isCorrect: Boolean = true
    ): GamificationTaskResult {

        // User progress tracking removed
        val taskCategory = TaskCategory.fromString(taskDescription)

        // Calculate comprehensive points with all bonuses
        val pointCalculation = pointEconomyManager.calculateTaskPoints(
            taskCategory = taskCategory,
            basePoints = taskCategory.basePoints,
            streakDays = userProgress.streakCount,
            isCorrect = isCorrect,
            timeBonus = calculateTimeBonus(minutesSpent, taskCategory)
        )

        // Check for active comeback bonus
        val activeComebackBonus = checkActiveComebackBonus(userProgress)
        val finalPoints = if (activeComebackBonus != null) {
            (pointCalculation.finalPoints * activeComebackBonus.multiplier).toLong()
        } else {
            pointCalculation.finalPoints
        }

        // Award points
        pointEconomyManager.awardPoints(
            type = PointTransactionType.TASK_COMPLETION,
            amount = finalPoints,
            description = "Completed: $taskDescription",
            category = taskCategory,
            multiplier = pointCalculation.streakMultiplier,
            metadata = mapOf(
                "task_id" to taskId,
                "minutes_spent" to minutesSpent.toString(),
                "is_correct" to isCorrect.toString(),
                "comeback_bonus" to (activeComebackBonus?.multiplier?.toString() ?: "none")
            )
        )

        // Complete the task in the progress system
        // Task completion tracking removed

        // Check for new achievement unlocks
        val newAchievements = checkForNewAchievementUnlocks()

        // Update challenge progress
        updateChallengeProgress(taskCategory, isCorrect, minutesSpent)

        // Check for level up
        val newLevel = LevelSystemCalculator.calculateLevel(
            pointEconomyManager.pointWalletFlow.first().totalLifetimePoints + finalPoints
        )
        val previousLevel = LevelSystemCalculator.calculateLevel(
            pointEconomyManager.pointWalletFlow.first().totalLifetimePoints
        )
        val leveledUp = newLevel.currentLevel > previousLevel.currentLevel

        return GamificationTaskResult(
            pointsEarned = finalPoints,
            pointBreakdown = pointCalculation.breakdown,
            newAchievements = newAchievements,
            challengeProgress = getCurrentDailyChallengeProgress(),
            levelUp = if (leveledUp) newLevel else null,
            comebackBonusApplied = activeComebackBonus,
            celebrationEvents = generateCelebrationEvents(
                pointsEarned = finalPoints,
                newAchievements = newAchievements,
                levelUp = if (leveledUp) newLevel else null,
                taskCategory = taskCategory
            )
        )
    }

    /**
     * Purchase cosmetic with enhanced feedback
     */
    suspend fun purchaseCosmetic(cosmetic: CosmeticReward): GamificationPurchaseResult {
        val purchaseResult = pointEconomyManager.purchaseCosmetic(cosmetic)

        return when (purchaseResult) {
            is PurchaseResult.Success -> {
                val celebrationEvent = CelebrationEvent(
                    type = CelebrationType.MilestoneReward(
                        milestoneType = MilestoneType.WEEK_COMPLETION, // Using as cosmetic unlock
                        value = cosmetic.cost.toInt(),
                        reward = "Unlocked ${cosmetic.name}!",
                        points = 0
                    )
                )

                GamificationPurchaseResult.Success(
                    cosmetic = cosmetic,
                    celebrationEvent = celebrationEvent
                )
            }
            is PurchaseResult.InsufficientFunds -> GamificationPurchaseResult.InsufficientFunds(
                purchaseResult.required,
                purchaseResult.available
            )
            is PurchaseResult.AlreadyOwned -> GamificationPurchaseResult.AlreadyOwned(cosmetic)
            is PurchaseResult.RequirementNotMet -> GamificationPurchaseResult.RequirementNotMet(
                purchaseResult.requirement
            )
        }
    }

    /**
     * Generate daily challenge
     */
    suspend fun generateNewDailyChallenge(): DailyChallenge {
        // User progress tracking removed
        // Task logs removed

        return ChallengeGenerator.generateDailyChallenge(userProgress, taskLogs)
    }

    /**
     * Get achievement predictions
     */
    suspend fun getAchievementPredictions(): List<AchievementPrediction> {
        val achievements = advancedAchievementTracker.advancedAchievementFlow.first()
        return advancedAchievementTracker.generatePredictions(achievements)
    }

    /**
     * Get motivational insights
     */
    suspend fun getMotivationalInsights(): MotivationalInsights {
        val state = gamificationStateFlow.first()
        // User progress tracking removed

        return MotivationalInsights(
            todayProgress = calculateTodayProgress(),
            weekProgress = calculateWeekProgress(),
            streakAnalysis = analyzeStreak(userProgress),
            nextMilestone = findNextMilestone(state.achievements),
            encouragementMessage = generateEncouragementMessage(userProgress, state),
            recommendedActions = generateRecommendedActions(state)
        )
    }

    // Private helper methods

    private suspend fun getCurrentDailyChallenge(
        userProgress: UserProgress,
        transactions: List<PointTransaction>
    ): DailyChallenge? {
        val today = java.time.LocalDate.now().toString()

        // Check if we have a challenge for today
        val existingChallenge = loadDailyChallengeFromStorage(today)

        return existingChallenge ?: run {
            // Task logs removed
            val newChallenge = ChallengeGenerator.generateDailyChallenge(userProgress, taskLogs)
            saveDailyChallengeToStorage(newChallenge)
            newChallenge
        }
    }

    private suspend fun checkActiveComebackBonus(userProgress: UserProgress): ComebackBonus? {
        val daysSinceLastActivity = calculateDaysSinceLastActivity(userProgress)
        if (daysSinceLastActivity <= 0) return null

        return ComebackSystem.checkForComebackBonus(
            currentStreak = userProgress.streakCount,
            previousBestStreak = calculatePreviousBestStreak(userProgress),
            daysSinceLastActivity = daysSinceLastActivity
        )
    }

    private suspend fun checkForNewAchievementUnlocks(): List<AdvancedAchievement> {
        val currentAchievements = advancedAchievementTracker.advancedAchievementFlow.first()
        return currentAchievements.filter { it.isUnlocked && it.unlockedDate != null &&
            System.currentTimeMillis() - it.unlockedDate!! < 60000 } // Unlocked in last minute
    }

    private fun calculateTimeBonus(minutesSpent: Int, category: TaskCategory): Float {
        val estimatedTime = when (category) {
            TaskCategory.GRAMMAR -> 15
            TaskCategory.READING -> 20
            TaskCategory.LISTENING -> 18
            TaskCategory.VOCABULARY -> 12
            TaskCategory.PRACTICE_EXAM -> 120
            TaskCategory.OTHER -> 15
        }

        return if (minutesSpent < estimatedTime) {
            1f + (estimatedTime - minutesSpent) / estimatedTime.toFloat() * 0.5f
        } else {
            1f
        }
    }

    private suspend fun updateChallengeProgress(
        category: TaskCategory,
        isCorrect: Boolean,
        minutesSpent: Int
    ) {
        // Update daily challenge progress based on task completion
        // This would modify the stored challenge state
    }

    private suspend fun getCurrentDailyChallengeProgress(): DailyChallenge? {
        return gamificationStateFlow.first().dailyChallenge
    }

    private fun generateCelebrationEvents(
        pointsEarned: Long,
        newAchievements: List<AdvancedAchievement>,
        levelUp: LevelSystem?,
        taskCategory: TaskCategory
    ): List<CelebrationEvent> {
        val events = mutableListOf<CelebrationEvent>()

        // Task completion celebration
        events.add(
            CelebrationEvent(
                type = CelebrationType.TaskCompletion(
                    taskId = "task_${System.currentTimeMillis()}",
                    points = pointsEarned.toInt(),
                    taskCategory = taskCategory
                )
            )
        )

        // Achievement unlock celebrations
        newAchievements.forEach { achievement ->
            events.add(
                CelebrationEvent(
                    type = CelebrationType.LevelUp(
                        achievement = CategorizedAchievement(
                            id = achievement.id,
                            category = achievement.category,
                            tier = achievement.tier,
                            title = achievement.title,
                            description = achievement.description,
                            targetValue = achievement.targetValue,
                            pointsReward = achievement.pointsReward
                        ),
                        newLevel = achievement.tier,
                        totalPoints = pointsEarned.toInt()
                    )
                )
            )
        }

        // Level up celebration
        levelUp?.let { level ->
            events.add(
                CelebrationEvent(
                    type = CelebrationType.MilestoneReward(
                        milestoneType = MilestoneType.WEEK_COMPLETION,
                        value = level.currentLevel,
                        reward = "Level ${level.currentLevel}: ${level.levelTitle}",
                        points = (level.currentLevel * 100)
                    )
                )
            )
        }

        return events
    }

    // Storage helper methods (would be implemented with actual DataStore)
    private suspend fun loadDailyChallengeFromStorage(date: String): DailyChallenge? {
        // Implementation would load from DataStore
        return null
    }

    private suspend fun saveDailyChallengeToStorage(challenge: DailyChallenge) {
        // Implementation would save to DataStore
    }

    private fun calculateDaysSinceLastActivity(userProgress: UserProgress): Int {
        if (userProgress.lastCompletionDate == 0L) return 0

        val daysSince = (System.currentTimeMillis() - userProgress.lastCompletionDate) / (24 * 60 * 60 * 1000)
        return daysSince.toInt()
    }

    private fun calculatePreviousBestStreak(userProgress: UserProgress): Int {
        // This would need historical data - simplified
        return userProgress.streakCount + 10
    }

    private suspend fun calculateTodayProgress(): Float {
        // Task logs removed
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val todayTasks = taskLogs.filter { it.timestampMillis >= todayStart }

        return minOf(todayTasks.size / 5f, 1f) // Assume 5 tasks per day goal
    }

    private suspend fun calculateWeekProgress(): Float {
        // Task logs removed
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val weekTasks = taskLogs.filter { it.timestampMillis >= weekStart }

        return minOf(weekTasks.size / 35f, 1f) // Assume 35 tasks per week goal
    }

    private fun analyzeStreak(userProgress: UserProgress): StreakAnalysis {
        val tier = StreakMultiplierTier.getMultiplierForStreak(userProgress.streakCount)
        return StreakAnalysis(
            currentStreak = userProgress.streakCount,
            tier = tier,
            nextMilestone = when {
                userProgress.streakCount < 7 -> 7
                userProgress.streakCount < 14 -> 14
                userProgress.streakCount < 30 -> 30
                userProgress.streakCount < 50 -> 50
                userProgress.streakCount < 100 -> 100
                else -> ((userProgress.streakCount / 50) + 1) * 50
            },
            progressToNext = userProgress.streakCount.toFloat() / when {
                userProgress.streakCount < 7 -> 7f
                userProgress.streakCount < 14 -> 14f
                userProgress.streakCount < 30 -> 30f
                userProgress.streakCount < 50 -> 50f
                userProgress.streakCount < 100 -> 100f
                else -> (((userProgress.streakCount / 50) + 1) * 50).toFloat()
            }
        )
    }

    private fun findNextMilestone(achievements: List<AdvancedAchievement>): NextMilestone? {
        val nextAchievement = achievements
            .filter { !it.isUnlocked && it.isVisible }
            .maxByOrNull { it.progressPercentage }

        return nextAchievement?.let { achievement ->
            NextMilestone(
                type = MilestoneType.WEEK_COMPLETION,
                title = achievement.title,
                progress = achievement.progressPercentage,
                estimatedDays = when (val est = achievement.estimatedTimeToUnlock) {
                    is EstimatedTime.DAYS -> est.days
                    is EstimatedTime.WEEKS -> est.weeks * 7
                    is EstimatedTime.MONTHS -> est.months * 30
                    EstimatedTime.READY_TO_UNLOCK -> 0
                    null -> -1
                }
            )
        }
    }

    private fun generateEncouragementMessage(
        userProgress: UserProgress,
        state: GamificationState
    ): String {
        return when {
            userProgress.streakCount >= 100 -> "🔥 You're absolutely legendary! ${userProgress.streakCount} days of pure dedication!"
            userProgress.streakCount >= 50 -> "🏆 Incredible commitment! You're in the elite tier of learners!"
            userProgress.streakCount >= 30 -> "⭐ Outstanding consistency! You're building something truly special!"
            userProgress.streakCount >= 14 -> "🚀 Great momentum! You're developing excellent study habits!"
            userProgress.streakCount >= 7 -> "💪 Solid week! Keep this energy going!"
            state.activeComebackBonus != null -> ComebackSystem.getComebackEncouragement(
                calculateDaysSinceLastActivity(userProgress)
            )
            else -> "🌱 Every journey begins with a single step. You're building something amazing!"
        }
    }

    private fun generateRecommendedActions(state: GamificationState): List<String> {
        val actions = mutableListOf<String>()

        // Daily challenge recommendation
        state.dailyChallenge?.let { challenge ->
            if (!challenge.isCompleted) {
                actions.add("Complete today's ${challenge.type.displayName.lowercase()}: ${challenge.title}")
            }
        }

        // Achievement recommendations
        val nextAchievement = state.achievements
            .filter { !it.isUnlocked && it.progressPercentage > 0.5f }
            .maxByOrNull { it.progressPercentage }

        nextAchievement?.let { achievement ->
            actions.add("You're close to unlocking '${achievement.title}' - ${(achievement.progressPercentage * 100).toInt()}% complete!")
        }

        // Cosmetic recommendations
        val affordableCosmetics = state.availableCosmetics.filter {
            it.cost <= state.pointWallet.currentSpendablePoints
        }
        if (affordableCosmetics.isNotEmpty()) {
            val cheapest = affordableCosmetics.minByOrNull { it.cost }
            cheapest?.let { cosmetic ->
                actions.add("Consider purchasing '${cosmetic.name}' to customize your experience!")
            }
        }

        return actions
    }
}

/**
 * Supporting data classes for gamification results
 */
data class GamificationTaskResult(
    val pointsEarned: Long,
    val pointBreakdown: PointBreakdown,
    val newAchievements: List<AdvancedAchievement>,
    val challengeProgress: DailyChallenge?,
    val levelUp: LevelSystem?,
    val comebackBonusApplied: ComebackBonus?,
    val celebrationEvents: List<CelebrationEvent>
)

sealed class GamificationPurchaseResult {
    data class Success(
        val cosmetic: CosmeticReward,
        val celebrationEvent: CelebrationEvent
    ) : GamificationPurchaseResult()

    data class InsufficientFunds(val required: Long, val available: Long) : GamificationPurchaseResult()
    data class AlreadyOwned(val cosmetic: CosmeticReward) : GamificationPurchaseResult()
    data class RequirementNotMet(val requirement: String) : GamificationPurchaseResult()
}

data class MotivationalInsights(
    val todayProgress: Float,
    val weekProgress: Float,
    val streakAnalysis: StreakAnalysis,
    val nextMilestone: NextMilestone?,
    val encouragementMessage: String,
    val recommendedActions: List<String>
)

data class StreakAnalysis(
    val currentStreak: Int,
    val tier: StreakMultiplierTier,
    val nextMilestone: Int,
    val progressToNext: Float
)

data class NextMilestone(
    val type: MilestoneType,
    val title: String,
    val progress: Float,
    val estimatedDays: Int
)

/**
 * ViewModel for gamification UI integration
 */
class GamificationViewModel(
    private val gamificationManager: GamificationManager
) : ViewModel() {

    val gamificationState = gamificationManager.gamificationStateFlow

    private val _motivationalInsights = MutableStateFlow<MotivationalInsights?>(null)
    val motivationalInsights: StateFlow<MotivationalInsights?> = _motivationalInsights

    private val _achievementPredictions = MutableStateFlow<List<AchievementPrediction>>(emptyList())
    val achievementPredictions: StateFlow<List<AchievementPrediction>> = _achievementPredictions

    init {
        // Load initial insights
        viewModelScope.launch {
            _motivationalInsights.value = gamificationManager.getMotivationalInsights()
            _achievementPredictions.value = gamificationManager.getAchievementPredictions()
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
            gamificationManager.completeTaskWithGamification(
                taskId, taskDescription, taskDetails, minutesSpent, isCorrect
            )

            // Refresh insights
            _motivationalInsights.value = gamificationManager.getMotivationalInsights()
            _achievementPredictions.value = gamificationManager.getAchievementPredictions()
        }
    }

    fun purchaseCosmetic(cosmetic: CosmeticReward) {
        viewModelScope.launch {
            gamificationManager.purchaseCosmetic(cosmetic)
        }
    }

    fun generateNewDailyChallenge() {
        viewModelScope.launch {
            gamificationManager.generateNewDailyChallenge()
        }
    }

    fun refreshInsights() {
        viewModelScope.launch {
            _motivationalInsights.value = gamificationManager.getMotivationalInsights()
            _achievementPredictions.value = gamificationManager.getAchievementPredictions()
        }
    }
}