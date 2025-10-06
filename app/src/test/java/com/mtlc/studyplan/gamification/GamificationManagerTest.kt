package com.mtlc.studyplan.gamification

import com.mtlc.studyplan.data.AchievementCategory
import com.mtlc.studyplan.data.AchievementTier
import com.mtlc.studyplan.data.TaskCategory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Gamification Manager business logic
 */
class GamificationManagerTest {

    private lateinit var testState: GamificationManager.GamificationState
    private lateinit var testWallet: PointWallet
    private lateinit var testAchievements: List<AdvancedAchievement>

    @Before
    fun setup() {
        testWallet = PointWallet(
            totalLifetimePoints = 10000L,
            currentSpendablePoints = 7000L,
            pointsSpentTotal = 3000L,
            lastUpdated = System.currentTimeMillis()
        )

        testAchievements = listOf(
            AdvancedAchievement(
                id = "grammar_bronze",
                category = AchievementCategory.GRAMMAR_MASTER,
                tier = AchievementTier.BRONZE,
                title = "Grammar Novice",
                description = "Complete 10 grammar tasks",
                targetValue = 10,
                pointsReward = 100,
                currentProgress = 5,
                isUnlocked = false
            ),
            AdvancedAchievement(
                id = "speed_silver",
                category = AchievementCategory.SPEED_DEMON,
                tier = AchievementTier.SILVER,
                title = "Speed Master",
                description = "Complete 50 tasks quickly",
                targetValue = 50,
                pointsReward = 500,
                currentProgress = 50,
                isUnlocked = true,
                unlockedDate = System.currentTimeMillis()
            )
        )

        testState = GamificationManager.GamificationState(
            pointWallet = testWallet,
            achievements = testAchievements,
            transactionHistory = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
    }

    @Test
    fun `gamification state contains point wallet`() {
        assertNotNull(testState.pointWallet)
        assertEquals(10000L, testState.pointWallet.totalLifetimePoints)
    }

    @Test
    fun `gamification state contains achievements`() {
        assertNotNull(testState.achievements)
        assertEquals(2, testState.achievements.size)
    }

    @Test
    fun `gamification state tracks lifetime points correctly`() {
        assertEquals(10000L, testState.pointWallet.totalLifetimePoints)
        assertEquals(7000L, testState.pointWallet.currentSpendablePoints)
        assertEquals(3000L, testState.pointWallet.pointsSpentTotal)
    }

    @Test
    fun `achievements include locked and unlocked`() {
        val locked = testState.achievements.filter { !it.isUnlocked }
        val unlocked = testState.achievements.filter { it.isUnlocked }

        assertEquals(1, locked.size)
        assertEquals(1, unlocked.size)
    }

    @Test
    fun `achievement progress is tracked`() {
        val inProgressAchievement = testState.achievements.find {
            it.id == "grammar_bronze"
        }

        assertNotNull(inProgressAchievement)
        assertEquals(5, inProgressAchievement?.currentProgress)
        assertEquals(10, inProgressAchievement?.targetValue)
        assertEquals(0.5f, inProgressAchievement?.progressPercentage ?: 0f, 0.01f)
    }

    @Test
    fun `unlocked achievement has timestamp`() {
        val unlockedAchievement = testState.achievements.find { it.isUnlocked }

        assertNotNull(unlockedAchievement)
        assertNotNull(unlockedAchievement?.unlockedDate)
        assertTrue((unlockedAchievement?.unlockedDate ?: 0) > 0)
    }

    @Test
    fun `point wallet balance is consistent`() {
        val total = testWallet.totalLifetimePoints
        val spendable = testWallet.currentSpendablePoints
        val spent = testWallet.pointsSpentTotal

        assertEquals(total, spendable + spent)
    }

    @Test
    fun `achievement categories are diverse`() {
        val categories = testAchievements.map { it.category }.distinct()

        assertTrue(categories.contains(AchievementCategory.GRAMMAR_MASTER))
        assertTrue(categories.contains(AchievementCategory.SPEED_DEMON))
        assertEquals(2, categories.size)
    }

    @Test
    fun `achievement tiers vary`() {
        val tiers = testAchievements.map { it.tier }.distinct()

        assertTrue(tiers.contains(AchievementTier.BRONZE))
        assertTrue(tiers.contains(AchievementTier.SILVER))
        assertEquals(2, tiers.size)
    }

    @Test
    fun `point rewards scale with achievement tier`() {
        val bronzeAchievement = testAchievements.find { it.tier == AchievementTier.BRONZE }
        val silverAchievement = testAchievements.find { it.tier == AchievementTier.SILVER }

        assertTrue((silverAchievement?.pointsReward ?: 0) > (bronzeAchievement?.pointsReward ?: 0))
    }

    @Test
    fun `level system is calculated from lifetime points`() {
        val levelSystem = LevelSystemCalculator.calculateLevel(10000L)

        assertTrue(levelSystem.currentLevel > 0)
        assertTrue(levelSystem.currentXP >= 0)
        assertTrue(levelSystem.xpToNextLevel > 0)
    }

    @Test
    fun `level system has proper level title`() {
        val levelSystem = LevelSystemCalculator.calculateLevel(1000L)

        assertNotNull(levelSystem.levelTitle)
        assertFalse(levelSystem.levelTitle.isEmpty())
    }

    @Test
    fun `transaction history can be tracked`() {
        val transactions = listOf(
            PointTransaction(
                type = PointTransactionType.TASK_COMPLETION,
                amount = 100L,
                description = "Task 1"
            ),
            PointTransaction(
                type = PointTransactionType.STREAK_BONUS,
                amount = 50L,
                description = "7-day streak"
            )
        )

        val stateWithTransactions = testState.copy(transactionHistory = transactions)

        assertEquals(2, stateWithTransactions.transactionHistory.size)
    }

    @Test
    fun `gamification state tracks last updated time`() {
        assertTrue(testState.lastUpdated > 0)
        assertTrue(testState.lastUpdated <= System.currentTimeMillis())
    }

    @Test
    fun `achievement full title includes tier`() {
        val achievement = testAchievements[0]
        assertEquals("Bronze Grammar Novice", achievement.fullTitle)
    }

    @Test
    fun `completed achievements show 100 percent progress`() {
        val completedAchievement = testAchievements.find {
            it.currentProgress >= it.targetValue
        }

        assertNotNull(completedAchievement)
        assertEquals(1.0f, completedAchievement?.progressPercentage ?: 0f, 0.01f)
    }

    @Test
    fun `gamification state can handle empty achievements`() {
        val emptyState = testState.copy(achievements = emptyList())

        assertEquals(0, emptyState.achievements.size)
        assertNotNull(emptyState.pointWallet)
    }

    @Test
    fun `point wallet tracks spending correctly`() {
        val spent = testWallet.pointsSpentTotal
        val spendable = testWallet.currentSpendablePoints

        assertTrue(spent >= 0)
        assertTrue(spendable >= 0)
        assertTrue(testWallet.totalLifetimePoints >= spent)
    }

    @Test
    fun `achievement rewards are positive`() {
        testAchievements.forEach { achievement ->
            assertTrue(achievement.pointsReward > 0)
        }
    }

    @Test
    fun `achievement target values are positive`() {
        testAchievements.forEach { achievement ->
            assertTrue(achievement.targetValue > 0)
        }
    }

    @Test
    fun `achievement progress cannot exceed target`() {
        testAchievements.forEach { achievement ->
            assertTrue(achievement.currentProgress <= achievement.targetValue)
        }
    }

    @Test
    fun `gamification state has default values`() {
        val defaultState = GamificationManager.GamificationState()

        assertEquals(defaultState.pointWallet.totalLifetimePoints, 0L)
        assertTrue(defaultState.achievements.isEmpty())
        assertNull(defaultState.dailyChallenge)
        assertNull(defaultState.weeklyChallenge)
    }

    @Test
    fun `category multipliers are properly configured`() {
        val grammarMultiplier = CategoryMultiplier.GRAMMAR_MULTIPLIER
        val readingMultiplier = CategoryMultiplier.READING_MULTIPLIER

        assertEquals(TaskCategory.GRAMMAR, grammarMultiplier.category)
        assertEquals(TaskCategory.READING, readingMultiplier.category)

        assertTrue(grammarMultiplier.baseMultiplier > 0)
        assertTrue(readingMultiplier.baseMultiplier > 0)
    }

    @Test
    fun `achievement descriptions are informative`() {
        testAchievements.forEach { achievement ->
            assertNotNull(achievement.description)
            assertTrue(achievement.description.isNotEmpty())
        }
    }

    @Test
    fun `level system provides next level target`() {
        val level1 = LevelSystemCalculator.calculateLevel(0L)
        val level2 = LevelSystemCalculator.calculateLevel(1000L)

        assertTrue(level1.xpToNextLevel > 0)
        assertTrue(level2.currentLevel >= level1.currentLevel)
    }

    @Test
    fun `achievements can have different categories`() {
        val allCategories = AchievementCategory.values()

        assertTrue(allCategories.contains(AchievementCategory.GRAMMAR_MASTER))
        assertTrue(allCategories.contains(AchievementCategory.SPEED_DEMON))
        assertTrue(allCategories.contains(AchievementCategory.CONSISTENCY_CHAMPION))
        assertTrue(allCategories.contains(AchievementCategory.PROGRESS_PIONEER))
    }

    @Test
    fun `point transactions have unique IDs`() {
        val tx1 = PointTransaction(
            type = PointTransactionType.TASK_COMPLETION,
            amount = 100L,
            description = "Test 1"
        )

        val tx2 = PointTransaction(
            type = PointTransactionType.TASK_COMPLETION,
            amount = 100L,
            description = "Test 2"
        )

        assertNotEquals(tx1.id, tx2.id)
    }
}
