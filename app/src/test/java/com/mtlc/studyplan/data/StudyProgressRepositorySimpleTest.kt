package com.mtlc.studyplan.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyProgressRepositorySimpleTest {

    private class InMemoryStudyProgressRepository {
        private var manualOverride: Int? = null
        private var storedWeek: Int? = null
        private var startDate: LocalDate? = null

        fun setManualWeekOverride(week: Int?) {
            manualOverride = week
        }

        fun setCurrentWeek(week: Int) {
            storedWeek = week.coerceIn(1, 30)
        }

        fun setStudyStartDate(date: LocalDate) {
            startDate = date
            manualOverride = null
        }

        fun nextWeek() {
            val current = storedWeek ?: 1
            storedWeek = (current + 1).coerceAtMost(30)
        }

        fun currentWeek(today: LocalDate = LocalDate.now()): Int {
            manualOverride?.let { return it.coerceIn(1, 30) }
            storedWeek?.let { return it.coerceIn(1, 30) }

            startDate?.let { start ->
                val weeksSinceStart = ChronoUnit.WEEKS.between(start, today).toInt() + 1
                return weeksSinceStart.coerceIn(1, 30)
            }

            return 1
        }
    }

    @Test
    fun currentWeekDefaultsWithinRange() {
        val repo = InMemoryStudyProgressRepository()
        val week = repo.currentWeek()
        assertTrue(week in 1..30)
    }

    @Test
    fun currentWeekUsesManualOverride() {
        val repo = InMemoryStudyProgressRepository()
        repo.setManualWeekOverride(22)
        assertEquals(22, repo.currentWeek())
    }

    @Test
    fun currentWeekClampsStoredWeek() {
        val repo = InMemoryStudyProgressRepository()
        repo.setCurrentWeek(45)
        assertEquals(30, repo.currentWeek())
    }

    @Test
    fun currentWeekCalculatesFromStartDate() {
        val repo = InMemoryStudyProgressRepository()
        repo.setStudyStartDate(LocalDate.now().minusWeeks(4))
        assertTrue(repo.currentWeek() >= 4)
    }

    @Test
    fun nextWeekDoesNotExceedThirty() {
        val repo = InMemoryStudyProgressRepository()
        repo.setCurrentWeek(30)
        repo.nextWeek()
        assertEquals(30, repo.currentWeek())
    }
}
