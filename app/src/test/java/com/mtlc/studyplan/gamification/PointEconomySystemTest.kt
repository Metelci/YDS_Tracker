package com.mtlc.studyplan.gamification

import com.mtlc.studyplan.data.TaskCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Point Economy System
 */
class PointEconomySystemTest {

    @Test
    fun `PointWallet creation with default values`() {
        val wallet = PointWallet()

        assertEquals(0L, wallet.totalLifetimePoints)
        assertEquals(0L, wallet.currentSpendablePoints)
        assertEquals(0L, wallet.pointsSpentTotal)
        assertTrue(wallet.lastUpdated > 0)
    }

    @Test
    fun `PointWallet creation with custom values`() {
        val wallet = PointWallet(
            totalLifetimePoints = 10000L,
            currentSpendablePoints = 5000L,
            pointsSpentTotal = 5000L,
            lastUpdated = 123456789L
        )

        assertEquals(10000L, wallet.totalLifetimePoints)
        assertEquals(5000L, wallet.currentSpendablePoints)
        assertEquals(5000L, wallet.pointsSpentTotal)
        assertEquals(123456789L, wallet.lastUpdated)
    }

    @Test
    fun `PointTransaction creation with task completion type`() {
        val transaction = PointTransaction(
            type = PointTransactionType.TASK_COMPLETION,
            amount = 100L,
            category = TaskCategory.GRAMMAR,
            multiplier = 1.2f,
            description = "Completed grammar task"
        )

        assertEquals(PointTransactionType.TASK_COMPLETION, transaction.type)
        assertEquals(100L, transaction.amount)
        assertEquals(TaskCategory.GRAMMAR, transaction.category)
        assertEquals(1.2f, transaction.multiplier, 0.01f)
        assertEquals("Completed grammar task", transaction.description)
        assertTrue(transaction.id.startsWith("pt_"))
    }

    @Test
    fun `PointTransaction with metadata`() {
        val metadata = mapOf(
            "taskId" to "task123",
            "difficulty" to "hard",
            "bonus" to "streak"
        )

        val transaction = PointTransaction(
            type = PointTransactionType.STREAK_BONUS,
            amount = 50L,
            description = "7-day streak bonus",
            metadata = metadata
        )

        assertEquals(3, transaction.metadata.size)
        assertEquals("task123", transaction.metadata["taskId"])
        assertEquals("hard", transaction.metadata["difficulty"])
        assertEquals("streak", transaction.metadata["bonus"])
    }

    @Test
    fun `PointTransactionType enum has all expected values`() {
        val types = PointTransactionType.values()

        assertTrue(types.contains(PointTransactionType.TASK_COMPLETION))
        assertTrue(types.contains(PointTransactionType.STREAK_BONUS))
        assertTrue(types.contains(PointTransactionType.DAILY_GOAL_BONUS))
        assertTrue(types.contains(PointTransactionType.WEEKLY_GOAL_BONUS))
        assertTrue(types.contains(PointTransactionType.MONTHLY_GOAL_BONUS))
        assertTrue(types.contains(PointTransactionType.ACHIEVEMENT_UNLOCK))
        assertTrue(types.contains(PointTransactionType.CHALLENGE_COMPLETION))
        assertTrue(types.contains(PointTransactionType.COMEBACK_BONUS))
        assertTrue(types.contains(PointTransactionType.PURCHASE_THEME))
        assertTrue(types.contains(PointTransactionType.PURCHASE_BADGE))
        assertTrue(types.contains(PointTransactionType.PURCHASE_CELEBRATION))
        assertTrue(types.contains(PointTransactionType.REFUND))
    }

    @Test
    fun `CategoryMultiplier for Grammar`() {
        val multiplier = CategoryMultiplier.GRAMMAR_MULTIPLIER

        assertEquals(TaskCategory.GRAMMAR, multiplier.category)
        assertEquals(1.2f, multiplier.baseMultiplier, 0.01f)
        assertEquals(0.1f, multiplier.streakBonusMultiplier, 0.01f)
        assertEquals("Grammar Expert", multiplier.displayName)
        assertFalse(multiplier.description.isEmpty())
    }

    @Test
    fun `CategoryMultiplier for Reading`() {
        val multiplier = CategoryMultiplier.READING_MULTIPLIER

        assertEquals(TaskCategory.READING, multiplier.category)
        assertEquals(1.5f, multiplier.baseMultiplier, 0.01f)
        assertEquals(0.15f, multiplier.streakBonusMultiplier, 0.01f)
        assertEquals("Reading Specialist", multiplier.displayName)
    }

    @Test
    fun `CategoryMultiplier for Listening`() {
        val multiplier = CategoryMultiplier.LISTENING_MULTIPLIER

        assertEquals(TaskCategory.LISTENING, multiplier.category)
        assertEquals(1.3f, multiplier.baseMultiplier, 0.01f)
        assertEquals(0.12f, multiplier.streakBonusMultiplier, 0.01f)
        assertEquals("Audio Master", multiplier.displayName)
    }

    @Test
    fun `CategoryMultiplier for Vocabulary`() {
        val multiplier = CategoryMultiplier.VOCABULARY_MULTIPLIER

        assertEquals(TaskCategory.VOCABULARY, multiplier.category)
        assertEquals(1.0f, multiplier.baseMultiplier, 0.01f)
        assertEquals(0.08f, multiplier.streakBonusMultiplier, 0.01f)
        assertEquals("Word Builder", multiplier.displayName)
    }

    @Test
    fun `PointTransaction ID generation is unique`() {
        val transaction1 = PointTransaction(
            type = PointTransactionType.TASK_COMPLETION,
            amount = 100L,
            description = "Test 1"
        )

        val transaction2 = PointTransaction(
            type = PointTransactionType.TASK_COMPLETION,
            amount = 100L,
            description = "Test 2"
        )

        assertNotEquals(transaction1.id, transaction2.id)
        assertTrue(transaction1.id.startsWith("pt_"))
        assertTrue(transaction2.id.startsWith("pt_"))
    }

    @Test
    fun `PointTransaction with null category for non-task transactions`() {
        val transaction = PointTransaction(
            type = PointTransactionType.PURCHASE_THEME,
            amount = -500L,
            category = null,
            description = "Purchased dark theme"
        )

        assertNull(transaction.category)
        assertTrue(transaction.amount < 0)
    }

    @Test
    fun `PointTransaction with default multiplier`() {
        val transaction = PointTransaction(
            type = PointTransactionType.DAILY_GOAL_BONUS,
            amount = 200L,
            description = "Daily goal reached"
        )

        assertEquals(1.0f, transaction.multiplier, 0.01f)
    }

    @Test
    fun `PointWallet tracks spending correctly`() {
        val wallet = PointWallet(
            totalLifetimePoints = 5000L,
            currentSpendablePoints = 3000L,
            pointsSpentTotal = 2000L
        )

        // Verify the relationship: lifetime = spendable + spent
        assertEquals(
            wallet.totalLifetimePoints,
            wallet.currentSpendablePoints + wallet.pointsSpentTotal
        )
    }

    @Test
    fun `PointTransaction timestamp is generated`() {
        val beforeTime = System.currentTimeMillis()

        val transaction = PointTransaction(
            type = PointTransactionType.TASK_COMPLETION,
            amount = 100L,
            description = "Test"
        )

        val afterTime = System.currentTimeMillis()

        assertTrue(transaction.timestamp >= beforeTime)
        assertTrue(transaction.timestamp <= afterTime)
    }

    @Test
    fun `PointTransaction with high multiplier for special events`() {
        val transaction = PointTransaction(
            type = PointTransactionType.CHALLENGE_COMPLETION,
            amount = 1000L,
            multiplier = 2.5f,
            description = "Weekly challenge completed with 2.5x bonus"
        )

        assertEquals(2.5f, transaction.multiplier, 0.01f)
        assertEquals(1000L, transaction.amount)
    }

    @Test
    fun `PointTransaction refund has negative amount`() {
        val transaction = PointTransaction(
            type = PointTransactionType.REFUND,
            amount = -300L,
            description = "Refund for unused cosmetic"
        )

        assertTrue(transaction.amount < 0)
        assertEquals(PointTransactionType.REFUND, transaction.type)
    }

    @Test
    fun `PointWallet equality check`() {
        val wallet1 = PointWallet(1000L, 500L, 500L, 123L)
        val wallet2 = PointWallet(1000L, 500L, 500L, 123L)
        val wallet3 = PointWallet(2000L, 500L, 500L, 123L)

        assertEquals(wallet1, wallet2)
        assertNotEquals(wallet1, wallet3)
    }

    @Test
    fun `CategoryMultiplier all values are accessible`() {
        val multipliers = CategoryMultiplier.values()

        assertTrue(multipliers.contains(CategoryMultiplier.GRAMMAR_MULTIPLIER))
        assertTrue(multipliers.contains(CategoryMultiplier.READING_MULTIPLIER))
        assertTrue(multipliers.contains(CategoryMultiplier.LISTENING_MULTIPLIER))
        assertTrue(multipliers.contains(CategoryMultiplier.VOCABULARY_MULTIPLIER))
    }
}
