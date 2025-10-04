package com.mtlc.studyplan.gamification

import com.mtlc.studyplan.data.AchievementCategory
import com.mtlc.studyplan.data.AchievementTier
import com.mtlc.studyplan.data.TaskCategory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Gamification UI component logic
 */
class GamificationComponentsTest {

    private lateinit var testWallet: PointWallet
    private lateinit var testTransaction: PointTransaction
    private lateinit var testAchievement: AdvancedAchievement

    @Before
    fun setup() {
        testWallet = PointWallet(
            totalLifetimePoints = 10000L,
            currentSpendablePoints = 7000L,
            pointsSpentTotal = 3000L,
            lastUpdated = System.currentTimeMillis()
        )

        testTransaction = PointTransaction(
            type = PointTransactionType.TASK_COMPLETION,
            amount = 100L,
            category = TaskCategory.GRAMMAR,
            multiplier = 1.2f,
            description = "Completed grammar task"
        )

        testAchievement = AdvancedAchievement(
            id = "test_achievement",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Grammar Novice",
            description = "Complete 10 grammar tasks",
            targetValue = 10,
            pointsReward = 100,
            currentProgress = 5
        )
    }

    @Test
    fun `PointWallet displays balance correctly`() {
        assertEquals(7000L, testWallet.currentSpendablePoints)
        assertEquals(10000L, testWallet.totalLifetimePoints)
        assertEquals(3000L, testWallet.pointsSpentTotal)
    }

    @Test
    fun `PointWallet tracks spending relationship`() {
        val wallet = PointWallet(
            totalLifetimePoints = 5000L,
            currentSpendablePoints = 3000L,
            pointsSpentTotal = 2000L
        )

        assertEquals(
            wallet.totalLifetimePoints,
            wallet.currentSpendablePoints + wallet.pointsSpentTotal
        )
    }

    @Test
    fun `Transaction shows correct type`() {
        assertEquals(PointTransactionType.TASK_COMPLETION, testTransaction.type)
    }

    @Test
    fun `Transaction displays amount correctly`() {
        assertEquals(100L, testTransaction.amount)
    }

    @Test
    fun `Transaction shows multiplier effect`() {
        assertEquals(1.2f, testTransaction.multiplier, 0.01f)

        val effectivePoints = testTransaction.amount * testTransaction.multiplier
        assertEquals(120f, effectivePoints, 0.01f)
    }

    @Test
    fun `Transaction categories are properly linked`() {
        assertEquals(TaskCategory.GRAMMAR, testTransaction.category)

        val readingTransaction = testTransaction.copy(category = TaskCategory.READING)
        assertEquals(TaskCategory.READING, readingTransaction.category)
    }

    @Test
    fun `Achievement progress is calculated correctly`() {
        assertEquals(0.5f, testAchievement.progressPercentage, 0.01f)
    }

    @Test
    fun `Achievement tier is displayed correctly`() {
        assertEquals(AchievementTier.BRONZE, testAchievement.tier)
        assertEquals("Bronze Grammar Novice", testAchievement.fullTitle)
    }

    @Test
    fun `Achievement shows unlock status`() {
        assertFalse(testAchievement.isUnlocked)

        val unlockedAchievement = testAchievement.copy(
            isUnlocked = true,
            currentProgress = 10
        )
        assertTrue(unlockedAchievement.isUnlocked)
        assertEquals(1.0f, unlockedAchievement.progressPercentage, 0.01f)
    }

    @Test
    fun `Achievement rarity affects rewards`() {
        val commonAchievement = testAchievement.copy(rarity = AchievementRarity.COMMON)
        assertEquals(1.0f, commonAchievement.rarity.pointsMultiplier, 0.01f)

        val rareAchievement = testAchievement.copy(rarity = AchievementRarity.RARE)
        assertEquals(2.0f, rareAchievement.rarity.pointsMultiplier, 0.01f)

        val legendaryAchievement = testAchievement.copy(rarity = AchievementRarity.LEGENDARY)
        assertEquals(5.0f, legendaryAchievement.rarity.pointsMultiplier, 0.01f)
    }

    @Test
    fun `CategoryMultiplier bonuses are applied correctly`() {
        assertEquals(1.2f, CategoryMultiplier.GRAMMAR_MULTIPLIER.baseMultiplier, 0.01f)
        assertEquals(1.5f, CategoryMultiplier.READING_MULTIPLIER.baseMultiplier, 0.01f)
        assertEquals(1.3f, CategoryMultiplier.LISTENING_MULTIPLIER.baseMultiplier, 0.01f)
        assertEquals(1.0f, CategoryMultiplier.VOCABULARY_MULTIPLIER.baseMultiplier, 0.01f)
    }

    @Test
    fun `Streak bonuses increase with category`() {
        assertEquals(0.1f, CategoryMultiplier.GRAMMAR_MULTIPLIER.streakBonusMultiplier, 0.01f)
        assertEquals(0.15f, CategoryMultiplier.READING_MULTIPLIER.streakBonusMultiplier, 0.01f)
        assertEquals(0.12f, CategoryMultiplier.LISTENING_MULTIPLIER.streakBonusMultiplier, 0.01f)
        assertEquals(0.08f, CategoryMultiplier.VOCABULARY_MULTIPLIER.streakBonusMultiplier, 0.01f)
    }

    @Test
    fun `Transaction types cover all gamification events`() {
        val types = listOf(
            PointTransactionType.TASK_COMPLETION,
            PointTransactionType.STREAK_BONUS,
            PointTransactionType.DAILY_GOAL_BONUS,
            PointTransactionType.WEEKLY_GOAL_BONUS,
            PointTransactionType.ACHIEVEMENT_UNLOCK,
            PointTransactionType.CHALLENGE_COMPLETION,
            PointTransactionType.COMEBACK_BONUS
        )

        types.forEach { type ->
            val transaction = testTransaction.copy(type = type)
            assertEquals(type, transaction.type)
        }
    }

    @Test
    fun `Purchase transactions have negative amounts`() {
        val purchase = testTransaction.copy(
            type = PointTransactionType.PURCHASE_THEME,
            amount = -500L
        )

        assertTrue(purchase.amount < 0)
        assertEquals(PointTransactionType.PURCHASE_THEME, purchase.type)
    }

    @Test
    fun `Achievement visibility rules work correctly`() {
        // Regular achievement is visible
        assertTrue(testAchievement.isVisible)

        // Hidden achievement with no progress is not visible
        val hiddenAchievement = testAchievement.copy(
            isHidden = true,
            currentProgress = 0
        )
        assertFalse(hiddenAchievement.isVisible)

        // Hidden achievement with progress becomes visible
        val revealedAchievement = hiddenAchievement.copy(currentProgress = 1)
        assertTrue(revealedAchievement.isVisible)

        // Unlocked hidden achievement is visible
        val unlockedHidden = hiddenAchievement.copy(isUnlocked = true)
        assertTrue(unlockedHidden.isVisible)
    }

    @Test
    fun `Special rewards are properly attached to achievements`() {
        val specialReward = SpecialReward(
            type = SpecialReward.SpecialRewardType.COSMETIC_UNLOCK,
            value = "premium_theme",
            description = "Unlock premium theme"
        )

        val specialAchievement = testAchievement.copy(specialReward = specialReward)
        assertNotNull(specialAchievement.specialReward)
        assertEquals("premium_theme", specialAchievement.specialReward?.value)
    }

    @Test
    fun `Wallet can be empty`() {
        val emptyWallet = PointWallet(
            totalLifetimePoints = 0L,
            currentSpendablePoints = 0L,
            pointsSpentTotal = 0L
        )

        assertEquals(0L, emptyWallet.totalLifetimePoints)
        assertEquals(0L, emptyWallet.currentSpendablePoints)
    }

    @Test
    fun `High-value transactions are supported`() {
        val bigTransaction = testTransaction.copy(
            amount = 1000000L,
            type = PointTransactionType.ACHIEVEMENT_UNLOCK,
            description = "Unlocked legendary achievement"
        )

        assertEquals(1000000L, bigTransaction.amount)
    }

    @Test
    fun `Achievement prerequisites can be checked`() {
        val prereqAchievement = testAchievement.copy(
            prerequisites = listOf("ach_1", "ach_2", "ach_3")
        )

        assertEquals(3, prereqAchievement.prerequisites.size)
        assertTrue(prereqAchievement.prerequisites.contains("ach_1"))
    }

    @Test
    fun `Rarity glow effects are correct`() {
        assertFalse(AchievementRarity.COMMON.glowEffect)
        assertFalse(AchievementRarity.UNCOMMON.glowEffect)
        assertTrue(AchievementRarity.RARE.glowEffect)
        assertTrue(AchievementRarity.EPIC.glowEffect)
        assertTrue(AchievementRarity.LEGENDARY.glowEffect)
        assertTrue(AchievementRarity.MYTHIC.glowEffect)
    }

    @Test
    fun `Transaction metadata stores additional context`() {
        val metadata = mapOf(
            "taskId" to "task_123",
            "difficulty" to "hard",
            "timeSpent" to "300"
        )

        val detailedTransaction = testTransaction.copy(metadata = metadata)
        assertEquals(3, detailedTransaction.metadata.size)
        assertEquals("task_123", detailedTransaction.metadata["taskId"])
        assertEquals("hard", detailedTransaction.metadata["difficulty"])
        assertEquals("300", detailedTransaction.metadata["timeSpent"])
    }

    @Test
    fun `Achievement categories have proper display names`() {
        assertEquals("Grammar Master", AchievementCategory.GRAMMAR_MASTER.title)
        assertEquals("Speed Demon", AchievementCategory.SPEED_DEMON.title)
        assertEquals("Consistency Champion", AchievementCategory.CONSISTENCY_CHAMPION.title)
        assertEquals("Progress Pioneer", AchievementCategory.PROGRESS_PIONEER.title)
    }

    @Test
    fun `Achievement tiers have proper titles`() {
        assertEquals("Bronze", AchievementTier.BRONZE.title)
        assertEquals("Silver", AchievementTier.SILVER.title)
        assertEquals("Gold", AchievementTier.GOLD.title)
        assertEquals("Platinum", AchievementTier.PLATINUM.title)
    }

    @Test
    fun `Achievement tier multipliers increase with rank`() {
        assertEquals(1, AchievementTier.BRONZE.multiplier)
        assertEquals(2, AchievementTier.SILVER.multiplier)
        assertEquals(3, AchievementTier.GOLD.multiplier)
        assertEquals(5, AchievementTier.PLATINUM.multiplier)
    }

    @Test
    fun `Point balance cannot go negative in wallet`() {
        val wallet = PointWallet(
            totalLifetimePoints = 1000L,
            currentSpendablePoints = 0L,
            pointsSpentTotal = 1000L
        )

        assertTrue(wallet.currentSpendablePoints >= 0)
        assertTrue(wallet.totalLifetimePoints >= 0)
    }
}
