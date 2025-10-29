package com.mtlc.studyplan.database.entities

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ProgressEntity - Tracks daily and cumulative study progress
 * Focus: Progress data structure validation, goal tracking, and achievement tracking
 */
class ProgressEntityTest {

    // ========== HELPER FUNCTIONS ==========

    private fun createTestProgress(
        id: String = "progress-1",
        userId: String = "default_user",
        date: String = "2025-10-21",
        tasksCompleted: Int = 0,
        studyMinutes: Int = 0,
        pointsEarned: Int = 0,
        streak: Int = 0,
        categories: Map<String, Int> = emptyMap(),
        achievements: List<String> = emptyList(),
        dailyGoalMinutes: Int = 120,
        dailyGoalTasks: Int = 5,
        weeklyGoalMinutes: Int = 840,
        goalProgress: Float = 0.0f,
        efficiency: Float = 1.0f,
        focusTime: Int = 0,
        breakTime: Int = 0,
        sessionsCompleted: Int = 0,
        averageSessionLength: Int = 0,
        bestStreak: Int = 0,
        totalPointsEarned: Int = 0,
        rank: String = "Beginner",
        level: Int = 1,
        experiencePoints: Int = 0,
        createdAt: Long = 1L,
        updatedAt: Long = 1L
    ) = ProgressEntity(
        id = id,
        userId = userId,
        date = date,
        tasksCompleted = tasksCompleted,
        studyMinutes = studyMinutes,
        pointsEarned = pointsEarned,
        streak = streak,
        categories = categories,
        achievements = achievements,
        dailyGoalMinutes = dailyGoalMinutes,
        dailyGoalTasks = dailyGoalTasks,
        weeklyGoalMinutes = weeklyGoalMinutes,
        goalProgress = goalProgress,
        efficiency = efficiency,
        focusTime = focusTime,
        breakTime = breakTime,
        sessionsCompleted = sessionsCompleted,
        averageSessionLength = averageSessionLength,
        bestStreak = bestStreak,
        totalPointsEarned = totalPointsEarned,
        rank = rank,
        level = level,
        experiencePoints = experiencePoints,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ========== CONSTRUCTOR AND DEFAULTS TESTS ==========

    @Test
    fun `ProgressEntity creates with required fields`() {
        // Act
        val progress = createTestProgress()

        // Assert
        assertNotNull(progress)
        assertEquals("progress-1", progress.id)
        assertEquals("default_user", progress.userId)
        assertEquals("2025-10-21", progress.date)
    }

    @Test
    fun `ProgressEntity uses default values correctly`() {
        // Arrange & Act
        val progress = ProgressEntity(date = "2025-10-21")

        // Assert
        assertNotNull(progress.id) // Auto-generated UUID
        assertEquals("default_user", progress.userId)
        assertEquals(0, progress.tasksCompleted)
        assertEquals(0, progress.studyMinutes)
        assertEquals(0, progress.pointsEarned)
        assertEquals(0, progress.streak)
        assertEquals(120, progress.dailyGoalMinutes)
        assertEquals(5, progress.dailyGoalTasks)
        assertEquals(840, progress.weeklyGoalMinutes)
        assertEquals(0.0f, progress.goalProgress)
        assertEquals(1.0f, progress.efficiency)
        assertEquals("Beginner", progress.rank)
        assertEquals(1, progress.level)
    }

    // ========== IMMUTABILITY TESTS (data class copy) ==========

    @Test
    fun `ProgressEntity copy creates new instance`() {
        // Arrange
        val original = createTestProgress(id = "progress-1", tasksCompleted = 5)

        // Act
        val copy = original.copy()

        // Assert
        assertEquals(original, copy)
        assertTrue(original === copy == false) // Different instances
    }

    @Test
    fun `ProgressEntity copy with field updates`() {
        // Arrange
        val original = createTestProgress(tasksCompleted = 5, studyMinutes = 60)

        // Act
        val updated = original.copy(
            tasksCompleted = 10,
            studyMinutes = 120,
            streak = 5
        )

        // Assert
        assertEquals(5, original.tasksCompleted) // Original unchanged
        assertEquals(10, updated.tasksCompleted)
        assertEquals(120, updated.studyMinutes)
        assertEquals(5, updated.streak)
    }

    // ========== DATE FORMAT TESTS ==========

    @Test
    fun `ProgressEntity stores date in YYYY-MM-DD format`() {
        // Arrange
        val validDate = "2025-10-21"

        // Act
        val progress = createTestProgress(date = validDate)

        // Assert
        assertEquals(validDate, progress.date)
    }

    @Test
    fun `ProgressEntity handles different dates`() {
        // Arrange
        val dates = listOf(
            "2025-01-01",
            "2025-12-31",
            "2020-02-29", // Leap year
            "1999-12-31"
        )

        // Act & Assert
        dates.forEach { date ->
            val progress = createTestProgress(date = date)
            assertEquals(date, progress.date)
        }
    }

    // ========== USER ID TESTS ==========

    @Test
    fun `ProgressEntity stores user ID`() {
        // Act
        val progress = createTestProgress(userId = "user-123")

        // Assert
        assertEquals("user-123", progress.userId)
    }

    @Test
    fun `ProgressEntity uses default user when not specified`() {
        // Act
        val progress = ProgressEntity(date = "2025-10-21")

        // Assert
        assertEquals("default_user", progress.userId)
    }

    // ========== COMPLETION METRICS TESTS ==========

    @Test
    fun `ProgressEntity tracks tasks completed`() {
        // Act
        val progress = createTestProgress(tasksCompleted = 8)

        // Assert
        assertEquals(8, progress.tasksCompleted)
    }

    @Test
    fun `ProgressEntity tracks study minutes`() {
        // Act
        val progress = createTestProgress(studyMinutes = 180)

        // Assert
        assertEquals(180, progress.studyMinutes)
    }

    @Test
    fun `ProgressEntity tracks points earned`() {
        // Act
        val progress = createTestProgress(pointsEarned = 500)

        // Assert
        assertEquals(500, progress.pointsEarned)
    }

    @Test
    fun `ProgressEntity tracks streak`() {
        // Act
        val progress = createTestProgress(streak = 15)

        // Assert
        assertEquals(15, progress.streak)
    }

    // ========== CATEGORY PERFORMANCE TESTS ==========

    @Test
    fun `ProgressEntity tracks minutes by category`() {
        // Arrange
        val categories = mapOf(
            "GRAMMAR" to 60,
            "VOCABULARY" to 45,
            "READING" to 75
        )

        // Act
        val progress = createTestProgress(categories = categories)

        // Assert
        assertEquals(categories, progress.categories)
        assertEquals(60, progress.categories["GRAMMAR"])
        assertEquals(45, progress.categories["VOCABULARY"])
        assertEquals(75, progress.categories["READING"])
    }

    @Test
    fun `ProgressEntity with empty categories`() {
        // Act
        val progress = createTestProgress(categories = emptyMap())

        // Assert
        assertTrue(progress.categories.isEmpty())
    }

    @Test
    fun `ProgressEntity copy can update category performance`() {
        // Arrange
        val original = createTestProgress(
            categories = mapOf("GRAMMAR" to 60)
        )

        // Act
        val updated = original.copy(
            categories = mapOf(
                "GRAMMAR" to 80,
                "VOCABULARY" to 50
            )
        )

        // Assert
        assertEquals(1, original.categories.size)
        assertEquals(2, updated.categories.size)
        assertEquals(80, updated.categories["GRAMMAR"])
    }

    // ========== ACHIEVEMENTS TESTS ==========

    @Test
    fun `ProgressEntity tracks achievements`() {
        // Arrange
        val achievements = listOf("first_task", "perfect_day", "streak_7")

        // Act
        val progress = createTestProgress(achievements = achievements)

        // Assert
        assertEquals(achievements, progress.achievements)
        assertEquals(3, progress.achievements.size)
    }

    @Test
    fun `ProgressEntity with empty achievements`() {
        // Act
        val progress = createTestProgress(achievements = emptyList())

        // Assert
        assertTrue(progress.achievements.isEmpty())
    }

    @Test
    fun `ProgressEntity copy can add achievements`() {
        // Arrange
        val original = createTestProgress(
            achievements = listOf("first_task")
        )

        // Act
        val updated = original.copy(
            achievements = listOf("first_task", "perfect_day", "streak_7")
        )

        // Assert
        assertEquals(1, original.achievements.size)
        assertEquals(3, updated.achievements.size)
    }

    // ========== GOAL TRACKING TESTS ==========

    @Test
    fun `ProgressEntity tracks daily goals`() {
        // Act
        val progress = createTestProgress(
            dailyGoalMinutes = 120,
            dailyGoalTasks = 5
        )

        // Assert
        assertEquals(120, progress.dailyGoalMinutes)
        assertEquals(5, progress.dailyGoalTasks)
    }

    @Test
    fun `ProgressEntity tracks weekly goal`() {
        // Act
        val progress = createTestProgress(weeklyGoalMinutes = 840)

        // Assert
        assertEquals(840, progress.weeklyGoalMinutes)
    }

    @Test
    fun `ProgressEntity tracks goal progress`() {
        // Act
        val progress = createTestProgress(goalProgress = 0.75f)

        // Assert
        assertEquals(0.75f, progress.goalProgress)
    }

    @Test
    fun `ProgressEntity tracks completion efficiency`() {
        // Act
        val perfectEfficiency = createTestProgress(efficiency = 1.0f)
        val goodEfficiency = createTestProgress(efficiency = 0.8f)
        val slowEfficiency = createTestProgress(efficiency = 1.5f)

        // Assert
        assertEquals(1.0f, perfectEfficiency.efficiency)
        assertEquals(0.8f, goodEfficiency.efficiency)
        assertEquals(1.5f, slowEfficiency.efficiency)
    }

    // ========== FOCUS AND BREAK TRACKING TESTS ==========

    @Test
    fun `ProgressEntity tracks focus time`() {
        // Act
        val progress = createTestProgress(focusTime = 90)

        // Assert
        assertEquals(90, progress.focusTime)
    }

    @Test
    fun `ProgressEntity tracks break time`() {
        // Act
        val progress = createTestProgress(breakTime = 30)

        // Assert
        assertEquals(30, progress.breakTime)
    }

    @Test
    fun `ProgressEntity tracks focus and break balance`() {
        // Act
        val progress = createTestProgress(focusTime = 120, breakTime = 30)

        // Assert
        assertEquals(120, progress.focusTime)
        assertEquals(30, progress.breakTime)
        assertEquals(150, progress.focusTime + progress.breakTime)
    }

    // ========== SESSION TRACKING TESTS ==========

    @Test
    fun `ProgressEntity tracks sessions completed`() {
        // Act
        val progress = createTestProgress(sessionsCompleted = 5)

        // Assert
        assertEquals(5, progress.sessionsCompleted)
    }

    @Test
    fun `ProgressEntity tracks average session length`() {
        // Act
        val progress = createTestProgress(averageSessionLength = 45)

        // Assert
        assertEquals(45, progress.averageSessionLength)
    }

    @Test
    fun `ProgressEntity tracks best streak`() {
        // Act
        val progress = createTestProgress(bestStreak = 30)

        // Assert
        assertEquals(30, progress.bestStreak)
    }

    // ========== CUMULATIVE STATS TESTS ==========

    @Test
    fun `ProgressEntity tracks total points earned`() {
        // Act
        val progress = createTestProgress(totalPointsEarned = 2500)

        // Assert
        assertEquals(2500, progress.totalPointsEarned)
    }

    @Test
    fun `ProgressEntity tracks rank`() {
        // Arrange
        val ranks = listOf("Beginner", "Intermediate", "Advanced", "Expert", "Master")

        // Act & Assert
        ranks.forEach { rank ->
            val progress = createTestProgress(rank = rank)
            assertEquals(rank, progress.rank)
        }
    }

    @Test
    fun `ProgressEntity tracks level`() {
        // Act
        val progress = createTestProgress(level = 15)

        // Assert
        assertEquals(15, progress.level)
    }

    @Test
    fun `ProgressEntity tracks experience points`() {
        // Act
        val progress = createTestProgress(experiencePoints = 5000)

        // Assert
        assertEquals(5000, progress.experiencePoints)
    }

    // ========== TIMESTAMP TESTS ==========

    @Test
    fun `ProgressEntity stores creation and update timestamps`() {
        // Arrange
        val createdAt = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis() + 1000

        // Act
        val progress = createTestProgress(
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // Assert
        assertEquals(createdAt, progress.createdAt)
        assertEquals(updatedAt, progress.updatedAt)
    }

    @Test
    fun `ProgressEntity can be updated with new timestamp`() {
        // Arrange
        val original = createTestProgress(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newUpdateTime = System.currentTimeMillis() + 10000

        // Act
        val updated = original.copy(updatedAt = newUpdateTime)

        // Assert
        assertEquals(original.createdAt, updated.createdAt) // unchanged
        assertNotNull(updated.updatedAt)
        assertTrue(updated.updatedAt > original.updatedAt)
    }

    // ========== REALISTIC PROGRESS SCENARIOS TESTS ==========

    @Test
    fun `ProgressEntity represents productive study day`() {
        // Act
        val progress = createTestProgress(
            tasksCompleted = 8,
            studyMinutes = 240,
            pointsEarned = 800,
            streak = 5,
            focusTime = 200,
            breakTime = 40,
            sessionsCompleted = 6,
            averageSessionLength = 40,
            goalProgress = 1.2f, // Exceeded goal
            efficiency = 0.95f
        )

        // Assert
        assertEquals(8, progress.tasksCompleted)
        assertEquals(240, progress.studyMinutes)
        assertEquals(800, progress.pointsEarned)
        assertTrue(progress.goalProgress >= 1.0f)
        assertEquals(0.95f, progress.efficiency)
    }

    @Test
    fun `ProgressEntity represents minimal study day`() {
        // Act
        val progress = createTestProgress(
            tasksCompleted = 0,
            studyMinutes = 0,
            pointsEarned = 0,
            streak = 0,
            goalProgress = 0.0f
        )

        // Assert
        assertEquals(0, progress.tasksCompleted)
        assertEquals(0, progress.studyMinutes)
        assertEquals(0.0f, progress.goalProgress)
    }

    @Test
    fun `ProgressEntity progression through levels`() {
        // Arrange
        val level1 = createTestProgress(level = 1, experiencePoints = 0)
        val level5 = createTestProgress(level = 5, experiencePoints = 5000)
        val level10 = createTestProgress(level = 10, experiencePoints = 15000)

        // Assert
        assertEquals(1, level1.level)
        assertEquals(5, level5.level)
        assertEquals(10, level10.level)
        assertTrue(level10.experiencePoints > level5.experiencePoints)
        assertTrue(level5.experiencePoints > level1.experiencePoints)
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `ProgressEntity with zero values`() {
        // Act
        val progress = createTestProgress(
            tasksCompleted = 0,
            studyMinutes = 0,
            pointsEarned = 0,
            streak = 0,
            focusTime = 0,
            breakTime = 0,
            sessionsCompleted = 0,
            averageSessionLength = 0,
            goalProgress = 0.0f,
            efficiency = 1.0f
        )

        // Assert
        assertEquals(0, progress.tasksCompleted)
        assertEquals(0, progress.studyMinutes)
        assertEquals(0.0f, progress.goalProgress)
    }

    @Test
    fun `ProgressEntity with maximum values`() {
        // Act
        val progress = createTestProgress(
            tasksCompleted = 999,
            studyMinutes = 9999,
            pointsEarned = 99999,
            streak = 365,
            level = 100,
            experiencePoints = 999999,
            totalPointsEarned = 9999999
        )

        // Assert
        assertEquals(999, progress.tasksCompleted)
        assertEquals(9999, progress.studyMinutes)
        assertEquals(365, progress.streak)
        assertEquals(100, progress.level)
    }

    @Test
    fun `ProgressEntity equality based on all fields`() {
        // Arrange
        val progress1 = createTestProgress(
            id = "progress-1",
            tasksCompleted = 5,
            studyMinutes = 120
        )
        val progress2 = createTestProgress(
            id = "progress-1",
            tasksCompleted = 5,
            studyMinutes = 120
        )
        val progress3 = createTestProgress(
            id = "progress-1",
            tasksCompleted = 10,
            studyMinutes = 120
        )

        // Assert
        assertEquals(progress1, progress2)
        assertTrue(progress1 != progress3)
    }

    @Test
    fun `ProgressEntity hash code consistency`() {
        // Arrange
        val progress1 = createTestProgress(id = "progress-1")
        val progress2 = createTestProgress(id = "progress-1")

        // Assert
        assertEquals(progress1.hashCode(), progress2.hashCode())
    }

    @Test
    fun `ProgressEntity toString representation`() {
        // Arrange
        val progress = createTestProgress(id = "progress-1", date = "2025-10-21")

        // Act
        val stringRepresentation = progress.toString()

        // Assert
        assertNotNull(stringRepresentation)
        assertTrue(stringRepresentation.contains("ProgressEntity"))
    }
}
