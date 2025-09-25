package com.mtlc.studyplan.gamification

// Celebration imports removed with progress functionality
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.TaskCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

        val dailyChallenge = getCurrentDailyChallenge(transactions)
        val comebackBonus = checkActiveComebackBonus()
        val studyBuddyComparison = null // Progress tracking removed
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
            streakDays = 0, // Progress tracking removed
            isCorrect = isCorrect,
            timeBonus = calculateTimeBonus(minutesSpent, taskCategory)
        )

        // Check for active comeback bonus
        val activeComebackBonus = checkActiveComebackBonus()
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
                    type = "cosmetic_unlock",
                    title = "Unlocked ${cosmetic.name}!",
                    description = "You've unlocked a new cosmetic item!",
                    intensity = "MODERATE"
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
    suspend fun generateNewDailyChallenge(): DailyChallenge? {
        // User progress tracking removed
        // Task logs removed

        return null // Progress tracking removed
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
            streakAnalysis = analyzeStreak(),
            nextMilestone = findNextMilestone(state.achievements),
            encouragementMessage = generateEncouragementMessage(state),
            recommendedActions = generateRecommendedActions(state)
        )
    }

    // Private helper methods

    private suspend fun getCurrentDailyChallenge(
        transactions: List<PointTransaction>
    ): DailyChallenge? {
        val today = java.time.LocalDate.now().toString()

        // Check if we have a challenge for today
        val existingChallenge = loadDailyChallengeFromStorage(today)

        return existingChallenge // Progress tracking removed
    }

    private suspend fun checkActiveComebackBonus(): ComebackBonus? {
        // Progress tracking removed - no comeback bonus functionality
        return null
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
                type = "task_completion",
                title = "Task Completed!",
                description = "You earned ${pointsEarned} points",
                intensity = "MODERATE"
            )
        )

        // Achievement unlock celebrations
        newAchievements.forEach { achievement ->
            events.add(
                CelebrationEvent(
                    type = "achievement_unlock",
                    title = achievement.title,
                    description = achievement.description,
                    intensity = "HIGH"
                )
            )
        }

        // Level up celebration
        levelUp?.let { level ->
            events.add(
                CelebrationEvent(
                    type = "level_up",
                    title = "Level Up!",
                    description = "Level ${level.currentLevel}: ${level.levelTitle}",
                    intensity = "HIGH"
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

    // Progress tracking methods removed

    private suspend fun calculateTodayProgress(): Float {
        // Task logs removed
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        // Task logs removed - return default progress

        return 0f
    }

    private suspend fun calculateWeekProgress(): Float {
        // Task logs removed
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        // Task logs removed - return default progress

        return 0f
    }

    private fun analyzeStreak(): StreakAnalysis {
        // Progress tracking removed - return default streak analysis
        val streakCount = 0
        val tier = StreakMultiplierTier.getMultiplierForStreak(streakCount)
        return StreakAnalysis(
            currentStreak = streakCount,
            tier = tier,
            nextMilestone = 7,
            progressToNext = 0f
        )
    }

    private fun findNextMilestone(achievements: List<AdvancedAchievement>): NextMilestone? {
        val nextAchievement = achievements
            .filter { !it.isUnlocked && it.isVisible }
            .maxByOrNull { it.progressPercentage }

        return nextAchievement?.let { achievement ->
            NextMilestone(
                type = "achievement",
                title = achievement.title,
                progress = achievement.progressPercentage,
                estimatedDays = 0 // Simplified without EstimatedTime
            )
        }
    }

    private fun generateEncouragementMessage(
        state: GamificationState
    ): String {
        return "🌱 Every journey begins with a single step. You're building something amazing!"
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

// Simplified CelebrationEvent to replace deleted celebration system
data class CelebrationEvent(
    val type: String = "achievement",
    val title: String = "",
    val description: String = "",
    val intensity: String = "MODERATE"
)
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
    val type: String,
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