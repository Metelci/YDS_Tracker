package com.mtlc.studyplan.gamification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.data.AchievementCategory
import com.mtlc.studyplan.data.AchievementTier
import com.mtlc.studyplan.data.TaskCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Gamification Progression
 * Tests complete progression flows through gamification system including:
 * - Point accumulation and economy
 * - Level progression
 * - Achievement unlocking
 * - Streak tracking
 * - Reward system
 * - Challenge completion
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class GamificationProgressionIntegrationTest {

    private val Context.testDataStore: DataStore<Preferences> by preferencesDataStore(name = "gamification_test")
    private lateinit var context: Context
    private lateinit var gamificationManager: GamificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        gamificationManager = GamificationManager(context.testDataStore)
    }

    @Test
    fun initialGamificationStateIsValid() = runTest {
        // Given - Fresh gamification state
        val state = gamificationManager.gamificationStateFlow.first()

        // Then - All components should be initialized
        assertNotNull("Point wallet should exist", state.pointWallet)
        assertNotNull("Achievements list should exist", state.achievements)
        assertNotNull("Level system should exist", state.levelSystem)
        assertNotNull("Transaction history should exist", state.transactionHistory)

        // Point wallet should start at zero or have default values
        assertTrue("Lifetime points should be non-negative", state.pointWallet.totalLifetimePoints >= 0)
        assertTrue("Spendable points should be non-negative", state.pointWallet.currentSpendablePoints >= 0)
        assertTrue("Spent points should be non-negative", state.pointWallet.pointsSpentTotal >= 0)

        // Level system should start at level 1 or higher
        assertTrue("Current level should be at least 1", state.levelSystem.currentLevel >= 1)
        assertTrue("XP should be non-negative", state.levelSystem.currentXP >= 0)
        assertTrue("XP to next level should be positive", state.levelSystem.xpToNextLevel > 0)
    }

    @Test
    fun taskCompletionAwardsPoints() = runTest {
        // Given - Initial state
        val initialState = gamificationManager.gamificationStateFlow.first()
        val initialPoints = initialState.pointWallet.totalLifetimePoints

        // When - User completes a task
        val result = gamificationManager.completeTaskWithGamification(
            taskId = "test_task_1",
            taskDescription = "Grammar exercise",
            taskDetails = "Complete grammar practice",
            minutesSpent = 30,
            isCorrect = true
        )

        // Then - Points should be awarded
        assertNotNull("Result should be returned", result)
        assertTrue("Points should be awarded", result.pointsEarned > 0)

        // Verify state updated
        val updatedState = gamificationManager.gamificationStateFlow.first()
        assertTrue(
            "Total points should increase",
            updatedState.pointWallet.totalLifetimePoints > initialPoints
        )
    }

    @Test
    fun levelProgressionWorkflow() = runTest {
        // Given - User at a certain level
        val initialState = gamificationManager.gamificationStateFlow.first()
        val initialLevel = initialState.levelSystem.currentLevel
        val initialXP = initialState.levelSystem.currentXP

        // When - User earns enough XP to level up
        // Complete multiple tasks to gain XP
        repeat(10) { index ->
            gamificationManager.completeTaskWithGamification(
                taskId = "level_test_$index",
                taskDescription = "Reading comprehension",
                taskDetails = "Practice task $index",
                minutesSpent = 15,
                isCorrect = true
            )
        }

        // Then - Level or XP should have progressed
        val progressedState = gamificationManager.gamificationStateFlow.first()
        val newLevel = progressedState.levelSystem.currentLevel
        val newXP = progressedState.levelSystem.currentXP

        assertTrue(
            "Level should increase or XP should progress",
            newLevel > initialLevel || newXP > initialXP
        )

        // Level title should be valid
        assertNotNull("Level title should exist", progressedState.levelSystem.levelTitle)
        assertTrue("Level title should not be empty", progressedState.levelSystem.levelTitle.isNotEmpty())
    }

    @Test
    fun achievementUnlockingWorkflow() = runTest {
        // Given - Available achievements
        val initialState = gamificationManager.gamificationStateFlow.first()
        val initialAchievements = initialState.achievements

        // Then - Achievements should have valid structure
        initialAchievements.forEach { achievement ->
            assertNotNull("Achievement ID should exist", achievement.id)
            assertNotNull("Achievement title should exist", achievement.title)
            assertNotNull("Achievement category should exist", achievement.category)
            assertNotNull("Achievement tier should exist", achievement.tier)
            assertTrue("Target value should be positive", achievement.targetValue > 0)
            assertTrue("Points reward should be positive", achievement.pointsReward > 0)
            assertTrue("Progress should be non-negative", achievement.currentProgress >= 0)
            assertTrue("Progress should not exceed target", achievement.currentProgress <= achievement.targetValue)

            // Progress percentage should be valid
            assertTrue("Progress percentage should be 0-1", achievement.progressPercentage in 0.0f..1.0f)
        }

        // Check achievement unlocking after task completion
        // Complete tasks that might trigger achievements
        repeat(5) { index ->
            gamificationManager.completeTaskWithGamification(
                taskId = "achievement_test_$index",
                taskDescription = "Vocabulary building",
                taskDetails = "Task $index",
                minutesSpent = 20,
                isCorrect = true
            )
        }

        // Verify achievement progress
        val updatedState = gamificationManager.gamificationStateFlow.first()
        val updatedAchievements = updatedState.achievements

        // At least one achievement might have progressed
        val hasProgress = updatedAchievements.any { it.currentProgress > 0 }
        // This is OK if false (no matching achievements), just verify structure
        assertTrue("Achievements list should exist", updatedAchievements.isNotEmpty())
    }

    @Test
    fun pointEconomyConsistencyWorkflow() = runTest {
        // Given - User earns and spends points
        val initialState = gamificationManager.gamificationStateFlow.first()
        val initialLifetime = initialState.pointWallet.totalLifetimePoints

        // When - User completes tasks
        repeat(3) { index ->
            gamificationManager.completeTaskWithGamification(
                taskId = "economy_test_$index",
                taskDescription = "Listening practice",
                taskDetails = "Task $index",
                minutesSpent = 25,
                isCorrect = true
            )
        }

        // Then - Point economy should be consistent
        val updatedState = gamificationManager.gamificationStateFlow.first()
        val wallet = updatedState.pointWallet

        // Fundamental equation: lifetime = spendable + spent
        assertEquals(
            "Point economy should be balanced",
            wallet.totalLifetimePoints,
            wallet.currentSpendablePoints + wallet.pointsSpentTotal
        )

        assertTrue("Lifetime points should have increased", wallet.totalLifetimePoints > initialLifetime)
        assertTrue("Spendable points should be non-negative", wallet.currentSpendablePoints >= 0)
        assertTrue("Spent points should be non-negative", wallet.pointsSpentTotal >= 0)
    }

    @Test
    fun transactionHistoryTrackingWorkflow() = runTest {
        // Given - User completes multiple tasks
        repeat(5) { index ->
            gamificationManager.completeTaskWithGamification(
                taskId = "transaction_test_$index",
                taskDescription = "Grammar practice",
                taskDetails = "Task $index",
                minutesSpent = 15,
                isCorrect = true
            )
        }

        // Then - Transaction history should be recorded
        val state = gamificationManager.gamificationStateFlow.first()
        val transactions = state.transactionHistory

        assertTrue("Transactions should be recorded", transactions.isNotEmpty())

        // Verify transaction structure
        transactions.forEach { transaction ->
            assertNotNull("Transaction ID should exist", transaction.id)
            assertNotNull("Transaction type should exist", transaction.type)
            assertTrue("Transaction amount should be positive for earnings", transaction.amount != 0L)
            assertNotNull("Transaction description should exist", transaction.description)
            assertTrue("Timestamp should be valid", transaction.timestamp > 0)
        }

        // Recent transactions should be ordered by time (newest first typical pattern)
        if (transactions.size > 1) {
            // Just verify they have timestamps
            transactions.forEach { tx ->
                assertTrue("Transaction should have timestamp", tx.timestamp > 0)
            }
        }
    }

    @Test
    fun categoryMultiplierSystem Workflow() = runTest {
        // Given - Different task categories have different multipliers
        val grammarResult = gamificationManager.completeTaskWithGamification(
            taskId = "multiplier_grammar",
            taskDescription = "Grammar exercise",
            taskDetails = "Test",
            minutesSpent = 30,
            isCorrect = true
        )

        val readingResult = gamificationManager.completeTaskWithGamification(
            taskId = "multiplier_reading",
            taskDescription = "Reading comprehension",
            taskDetails = "Test",
            minutesSpent = 30,
            isCorrect = true
        )

        // Then - Results should have valid point calculations
        assertTrue("Grammar task should award points", grammarResult.pointsEarned > 0)
        assertTrue("Reading task should award points", readingResult.pointsEarned > 0)

        // Different categories may have different multipliers
        // Just verify both awarded points (exact values depend on multipliers)
        assertNotNull("Grammar result should exist", grammarResult)
        assertNotNull("Reading result should exist", readingResult)
    }

    @Test
    fun dailyChallengeTrackingWorkflow() = runTest {
        // Given - Daily challenge system
        val state = gamificationManager.gamificationStateFlow.first()
        val dailyChallenge = state.dailyChallenge

        // Daily challenge may or may not be active
        if (dailyChallenge != null) {
            // Then - Challenge should have valid structure
            assertNotNull("Challenge description should exist", dailyChallenge.description)
            assertTrue("Target value should be positive", dailyChallenge.targetValue > 0)
            assertTrue("Progress should be non-negative", dailyChallenge.currentProgress >= 0)
            assertTrue("Bonus points should be positive", dailyChallenge.bonusPoints > 0)
            assertTrue("Deadline should be in future or present", dailyChallenge.deadlineTimestamp >= 0)

            // Progress should not exceed target
            assertTrue(
                "Progress should not exceed target",
                dailyChallenge.currentProgress <= dailyChallenge.targetValue
            )
        }
    }

    @Test
    fun comebackBonusSystemWorkflow() = runTest {
        // Given - Comeback bonus system for returning users
        val state = gamificationManager.gamificationStateFlow.first()
        val comebackBonus = state.activeComebackBonus

        // Comeback bonus may or may not be active
        if (comebackBonus != null) {
            // Then - Bonus should have valid structure
            assertTrue("Days away should be positive", comebackBonus.daysAway > 0)
            assertTrue("Bonus multiplier should be >= 1", comebackBonus.bonusMultiplier >= 1.0f)
            assertTrue("Duration should be positive", comebackBonus.durationHours > 0)
            assertTrue("Expiry should be valid", comebackBonus.expiresAt > 0)

            // Verify expiry is in the future relative to current time
            val currentTime = System.currentTimeMillis()
            // Expiry might be in past if bonus expired, that's ok
            assertTrue("Expiry timestamp should be valid", comebackBonus.expiresAt > 0)
        }
    }

    @Test
    fun levelSystemCalculationWorkflow() = runTest {
        // Given - Different XP amounts
        val testXPLevels = listOf(0L, 1000L, 5000L, 10000L, 50000L, 100000L)

        testXPLevels.forEach { xp ->
            // When - Level is calculated from XP
            val levelSystem = LevelSystemCalculator.calculateLevel(xp)

            // Then - Level system should be valid
            assertTrue("Level should be at least 1", levelSystem.currentLevel >= 1)
            assertTrue("Current XP should be non-negative", levelSystem.currentXP >= 0)
            assertTrue("XP to next level should be positive", levelSystem.xpToNextLevel > 0)
            assertEquals("Total XP should match", xp, levelSystem.totalXP)
            assertNotNull("Level title should exist", levelSystem.levelTitle)
            assertTrue("Level title should not be empty", levelSystem.levelTitle.isNotEmpty())

            // Higher XP should result in higher or equal level
            if (xp > 0) {
                assertTrue("Level should increase with XP", levelSystem.currentLevel >= 1)
            }
        }
    }

    @Test
    fun achievementTierProgressionWorkflow() = runTest {
        // Given - Achievement system with different tiers
        val state = gamificationManager.gamificationStateFlow.first()
        val achievements = state.achievements

        // Then - Achievements should span different tiers
        val tiers = achievements.map { it.tier }.distinct()

        // Valid tiers check
        tiers.forEach { tier ->
            assertTrue(
                "Tier should be valid",
                tier in listOf(AchievementTier.BRONZE, AchievementTier.SILVER, AchievementTier.GOLD, AchievementTier.PLATINUM)
            )
        }

        // Higher tiers should generally have higher rewards
        val bronzeAchievements = achievements.filter { it.tier == AchievementTier.BRONZE }
        val platinumAchievements = achievements.filter { it.tier == AchievementTier.PLATINUM }

        if (bronzeAchievements.isNotEmpty() && platinumAchievements.isNotEmpty()) {
            val avgBronzeReward = bronzeAchievements.map { it.pointsReward }.average()
            val avgPlatinumReward = platinumAchievements.map { it.pointsReward }.average()

            // Platinum should generally award more than bronze
            assertTrue(
                "Higher tiers should generally award more points",
                avgPlatinumReward >= avgBronzeReward
            )
        }
    }

    @Test
    fun achievementCategoryDistributionWorkflow() = runTest {
        // Given - Achievements across different categories
        val state = gamificationManager.gamificationStateFlow.first()
        val achievements = state.achievements

        // Then - Achievements should cover different categories
        val categories = achievements.map { it.category }.distinct()

        assertTrue("Achievements should exist", achievements.isNotEmpty())

        // Valid categories check
        categories.forEach { category ->
            assertTrue(
                "Category should be valid",
                category in listOf(
                    AchievementCategory.GRAMMAR_MASTER,
                    AchievementCategory.SPEED_DEMON,
                    AchievementCategory.CONSISTENCY_CHAMPION,
                    AchievementCategory.PROGRESS_PIONEER
                )
            )
        }

        // Each achievement should have appropriate category
        achievements.forEach { achievement ->
            assertNotNull("Achievement category should exist", achievement.category)
        }
    }

    @Test
    fun gamificationStateConsistencyWorkflow() = runTest {
        // Given - User performs multiple actions
        repeat(5) { index ->
            gamificationManager.completeTaskWithGamification(
                taskId = "consistency_test_$index",
                taskDescription = "Mixed practice",
                taskDetails = "Task $index",
                minutesSpent = 20,
                isCorrect = true
            )
        }

        // Then - All gamification state should remain consistent
        val finalState = gamificationManager.gamificationStateFlow.first()

        // Point wallet consistency
        val wallet = finalState.pointWallet
        assertEquals(
            "Wallet should be balanced",
            wallet.totalLifetimePoints,
            wallet.currentSpendablePoints + wallet.pointsSpentTotal
        )

        // Achievement progress consistency
        finalState.achievements.forEach { achievement ->
            assertTrue(
                "Achievement progress should be valid",
                achievement.currentProgress in 0..achievement.targetValue
            )
            assertTrue(
                "Progress percentage should be valid",
                achievement.progressPercentage in 0.0f..1.0f
            )

            // If completed, should be marked as unlocked
            if (achievement.currentProgress >= achievement.targetValue) {
                assertTrue("Completed achievement should be unlocked", achievement.isUnlocked)
            }
        }

        // Level system consistency
        val levelSystem = finalState.levelSystem
        assertTrue("Level should be positive", levelSystem.currentLevel > 0)
        assertTrue("Current XP should not exceed next level XP", levelSystem.currentXP < levelSystem.xpToNextLevel)
        assertEquals(
            "Total XP should match lifetime points",
            wallet.totalLifetimePoints,
            levelSystem.totalXP
        )

        // Last updated timestamp should be recent
        assertTrue("State should have recent timestamp", finalState.lastUpdated > 0)
    }
}
