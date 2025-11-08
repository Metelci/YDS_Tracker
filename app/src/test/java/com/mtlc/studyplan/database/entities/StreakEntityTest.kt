package com.mtlc.studyplan.database.entities

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for StreakEntity - Tracks user study streaks and consecutive study days
 */
class StreakEntityTest {

    private fun createTestStreak(
        id: String = "streak-1",
        userId: String = "default_user",
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        lastActivityDate: String = "2025-10-21",
        streakStartDate: String? = null,
        streakType: String = "daily",
        isActive: Boolean = true,
        milestones: List<Int> = emptyList(),
        freezeCount: Int = 0,
        maxFreezes: Int = 3,
        streakGoal: Int = 30,
        perfectDays: Int = 0,
        almostPerfectDays: Int = 0,
        riskDays: Int = 0,
        recoveryDays: Int = 0,
        totalDaysStudied: Int = 0,
        averageTasksPerDay: Float = 0.0f,
        averageMinutesPerDay: Float = 0.0f,
        bestDay: String? = null,
        bestDayScore: Int = 0,
        motivation: String = "Keep going!"
    ) = StreakEntity(
        id = id,
        userId = userId,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastActivityDate = lastActivityDate,
        streakStartDate = streakStartDate,
        streakType = streakType,
        isActive = isActive,
        milestones = milestones,
        freezeCount = freezeCount,
        maxFreezes = maxFreezes,
        streakGoal = streakGoal,
        perfectDays = perfectDays,
        almostPerfectDays = almostPerfectDays,
        riskDays = riskDays,
        recoveryDays = recoveryDays,
        totalDaysStudied = totalDaysStudied,
        averageTasksPerDay = averageTasksPerDay,
        averageMinutesPerDay = averageMinutesPerDay,
        bestDay = bestDay,
        bestDayScore = bestDayScore,
        motivation = motivation
    )

    @Test
    fun `StreakEntity creates with required fields`() {
        val streak = createTestStreak()
        assertNotNull(streak.id)
        assertEquals("2025-10-21", streak.lastActivityDate)
    }

    @Test
    fun `StreakEntity tracks current and longest streaks`() {
        val streak = createTestStreak(currentStreak = 15, longestStreak = 30)
        assertEquals(15, streak.currentStreak)
        assertEquals(30, streak.longestStreak)
    }

    @Test
    fun `StreakEntity can be broken`() {
        val active = createTestStreak(isActive = true, currentStreak = 10)
        val broken = active.copy(isActive = false, currentStreak = 0)

        assertTrue(active.isActive)
        assertEquals(10, active.currentStreak)
        assertFalse(broken.isActive)
        assertEquals(0, broken.currentStreak)
    }

    @Test
    fun `StreakEntity tracks freeze usage`() {
        val streak = createTestStreak(freezeCount = 1, maxFreezes = 3)
        assertEquals(1, streak.freezeCount)
        assertEquals(3, streak.maxFreezes)
        assertTrue(streak.freezeCount < streak.maxFreezes)
    }

    @Test
    fun `StreakEntity supports different streak types`() {
        val types = listOf("daily", "weekly", "monthly")
        types.forEach { type ->
            val streak = createTestStreak(streakType = type)
            assertEquals(type, streak.streakType)
        }
    }

    @Test
    fun `StreakEntity tracks milestones`() {
        val milestones = listOf(7, 14, 30, 90)
        val streak = createTestStreak(milestones = milestones)
        assertEquals(milestones, streak.milestones)
    }

    @Test
    fun `StreakEntity tracks day quality metrics`() {
        val streak = createTestStreak(
            perfectDays = 10,
            almostPerfectDays = 5,
            riskDays = 2,
            recoveryDays = 1
        )
        assertEquals(10, streak.perfectDays)
        assertEquals(5, streak.almostPerfectDays)
        assertEquals(2, streak.riskDays)
        assertEquals(1, streak.recoveryDays)
    }

    @Test
    fun `StreakEntity tracks average performance`() {
        val streak = createTestStreak(
            averageTasksPerDay = 5.5f,
            averageMinutesPerDay = 120.5f,
            totalDaysStudied = 45
        )
        assertEquals(5.5f, streak.averageTasksPerDay)
        assertEquals(120.5f, streak.averageMinutesPerDay)
        assertEquals(45, streak.totalDaysStudied)
    }

    @Test
    fun `StreakEntity tracks best day performance`() {
        val bestDayDate = "2025-10-15"
        val streak = createTestStreak(
            bestDay = bestDayDate,
            bestDayScore = 950
        )
        assertEquals(bestDayDate, streak.bestDay)
        assertEquals(950, streak.bestDayScore)
    }

    @Test
    fun `StreakEntity stores motivation message`() {
        val motivation = "Amazing work! Keep the momentum going!"
        val streak = createTestStreak(motivation = motivation)
        assertEquals(motivation, streak.motivation)
    }

    @Test
    fun `StreakEntity copy updates progress`() {
        val original = createTestStreak(currentStreak = 5)
        val updated = original.copy(currentStreak = 6, longestStreak = 6)

        assertEquals(5, original.currentStreak)
        assertEquals(6, updated.currentStreak)
        assertEquals(6, updated.longestStreak)
    }

    @Test
    fun `StreakEntity default values`() {
        val streak = StreakEntity(lastActivityDate = "2025-10-21")
        assertEquals(0, streak.currentStreak)
        assertEquals(0, streak.longestStreak)
        assertEquals("daily", streak.streakType)
        assertTrue(streak.isActive)
        assertEquals(3, streak.maxFreezes)
        assertEquals(30, streak.streakGoal)
    }
}
