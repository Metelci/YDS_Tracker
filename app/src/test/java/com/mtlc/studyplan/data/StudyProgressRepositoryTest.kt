package com.mtlc.studyplan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for StudyProgressRepository
 * Tests week management, date-based calculations, and curriculum phases
 */
class StudyProgressRepositoryTest {

    private lateinit var fakeDataStore: InMemoryDataStore
    private lateinit var repository: StudyProgressRepository

    @Before
    fun setUp() {
        fakeDataStore = InMemoryDataStore()
        repository = StudyProgressRepository(fakeDataStore, true)
    }

    // ===== Week Management Tests =====

    @Test
    fun `currentWeek should emit default value of 1`() = runTest {
        val week = repository.currentWeek.first()
        assertEquals(1, week)
    }

    @Test
    fun `setCurrentWeek should update stored week`() = runTest {
        repository.setCurrentWeek(5)
        val week = repository.currentWeek.first()
        assertEquals(5, week)
    }

    @Test
    fun `setCurrentWeek should clamp week to 1-30 range upper bound`() = runTest {
        repository.setCurrentWeek(35)
        val week = repository.currentWeek.first()
        assertEquals(30, week)
    }

    @Test
    fun `setCurrentWeek should clamp week to 1-30 range lower bound`() = runTest {
        repository.setCurrentWeek(0)
        val week = repository.currentWeek.first()
        assertEquals(1, week)
    }

    @Test
    fun `manual override should take precedence over stored week`() = runTest {
        repository.setCurrentWeek(5)
        repository.setManualWeekOverride(10)
        val week = repository.currentWeek.first()
        assertEquals(10, week)
    }

    @Test
    fun `setManualWeekOverride with null should remove override`() = runTest {
        repository.setManualWeekOverride(15)
        repository.setManualWeekOverride(null)
        val week = repository.currentWeek.first()
        // Should fall back to default or stored week
        assertTrue(week >= 1 && week <= 30)
    }

    @Test
    fun `advanceToNextWeek should increment week by 1`() = runTest {
        repository.setCurrentWeek(5)
        repository.advanceToNextWeek()
        val week = repository.currentWeek.first()
        assertEquals(6, week)
    }

    @Test
    fun `advanceToNextWeek should cap at week 30`() = runTest {
        repository.setCurrentWeek(30)
        repository.advanceToNextWeek()
        val week = repository.currentWeek.first()
        assertEquals(30, week)
    }

    @Test
    fun `advanceToNextWeek from default should advance to 2`() = runTest {
        repository.advanceToNextWeek()
        val week = repository.currentWeek.first()
        assertEquals(2, week)
    }

    // ===== Curriculum Phase Tests =====

    @Test
    fun `setToRedBookPhase should set week in red phase range`() = runTest {
        repository.setToRedBookPhase(4)
        val week = repository.currentWeek.first()
        assertEquals(4, week)
        assertTrue(week in 1..8)
    }

    @Test
    fun `setToRedBookPhase default should set to week 1`() = runTest {
        repository.setToRedBookPhase()
        val week = repository.currentWeek.first()
        assertEquals(1, week)
    }

    @Test
    fun `setToRedBookPhase should clamp high values to 8`() = runTest {
        repository.setToRedBookPhase(50)
        val week = repository.currentWeek.first()
        assertEquals(8, week)
    }

    @Test
    fun `setToBlueBookPhase should set week in blue phase range`() = runTest {
        repository.setToBlueBookPhase(12)
        val week = repository.currentWeek.first()
        assertEquals(12, week)
        assertTrue(week in 9..18)
    }

    @Test
    fun `setToBlueBookPhase default should set to week 9`() = runTest {
        repository.setToBlueBookPhase()
        val week = repository.currentWeek.first()
        assertEquals(9, week)
    }

    @Test
    fun `setToBlueBookPhase should clamp low values to 9`() = runTest {
        repository.setToBlueBookPhase(5)
        val week = repository.currentWeek.first()
        assertEquals(9, week)
    }

    @Test
    fun `setToGreenBookPhase should set week in green phase range`() = runTest {
        repository.setToGreenBookPhase(22)
        val week = repository.currentWeek.first()
        assertEquals(22, week)
        assertTrue(week in 19..26)
    }

    @Test
    fun `setToGreenBookPhase default should set to week 19`() = runTest {
        repository.setToGreenBookPhase()
        val week = repository.currentWeek.first()
        assertEquals(19, week)
    }

    @Test
    fun `setToGreenBookPhase should clamp low values to 19`() = runTest {
        repository.setToGreenBookPhase(0)
        val week = repository.currentWeek.first()
        assertEquals(19, week)
    }

    @Test
    fun `setToExamCampPhase should set week in exam camp range`() = runTest {
        repository.setToExamCampPhase(28)
        val week = repository.currentWeek.first()
        assertEquals(28, week)
        assertTrue(week in 27..30)
    }

    @Test
    fun `setToExamCampPhase default should set to week 27`() = runTest {
        repository.setToExamCampPhase()
        val week = repository.currentWeek.first()
        assertEquals(27, week)
    }

    @Test
    fun `setToExamCampPhase should clamp high values to 30`() = runTest {
        repository.setToExamCampPhase(100)
        val week = repository.currentWeek.first()
        assertEquals(30, week)
    }

    // ===== Reset Tests =====

    @Test
    fun `resetProgress should clear all stored data`() = runTest {
        repository.setCurrentWeek(15)
        repository.setManualWeekOverride(20)

        repository.resetProgress()

        val week = repository.currentWeek.first()
        assertEquals(1, week)
    }

    @Test
    fun `resetProgress after phase change should reset to week 1`() = runTest {
        repository.setToExamCampPhase(28)

        repository.resetProgress()

        val week = repository.currentWeek.first()
        assertEquals(1, week)
    }

    // ===== getCurrentWeekSync Tests =====

    @Test
    fun `getCurrentWeekSync should return current week synchronously`() = runTest {
        repository.setCurrentWeek(7)
        val week = repository.getCurrentWeekSync()
        assertEquals(7, week)
    }

    @Test
    fun `getCurrentWeekSync should return manual override`() = runTest {
        repository.setCurrentWeek(5)
        repository.setManualWeekOverride(12)
        val week = repository.getCurrentWeekSync()
        assertEquals(12, week)
    }

    @Test
    fun `getCurrentWeekSync should always return valid range`() = runTest {
        repository.setCurrentWeek(25)
        val week = repository.getCurrentWeekSync()
        assertTrue(week in 1..30)
    }

    // ===== Integration Tests =====

    @Test
    fun `complex workflow - set weeks, override, reset`() = runTest {
        repository.setCurrentWeek(3)
        assertEquals(3, repository.currentWeek.first())

        repository.setManualWeekOverride(15)
        assertEquals(15, repository.currentWeek.first())

        repository.setManualWeekOverride(null)
        assertTrue(repository.currentWeek.first() >= 1)

        repository.resetProgress()
        assertEquals(1, repository.currentWeek.first())
    }

    @Test
    fun `multiple phase transitions`() = runTest {
        repository.setToRedBookPhase(5)
        assertEquals(5, repository.currentWeek.first())

        repository.setToBlueBookPhase(15)
        assertEquals(15, repository.currentWeek.first())

        repository.setToGreenBookPhase(20)
        assertEquals(20, repository.currentWeek.first())

        repository.setToExamCampPhase(30)
        assertEquals(30, repository.currentWeek.first())
    }

    @Test
    fun `week advancement respects maximum`() = runTest {
        for (i in 1..35) {
            repository.advanceToNextWeek()
        }
        val week = repository.currentWeek.first()
        assertEquals(30, week)
    }
}

/**
 * Simple in-memory DataStore for testing
 */
private class InMemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences
    ): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
