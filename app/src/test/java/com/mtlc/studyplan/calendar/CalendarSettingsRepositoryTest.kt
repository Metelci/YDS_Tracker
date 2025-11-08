package com.mtlc.studyplan.calendar

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarSettingsRepositoryTest {

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<MutablePreferences>(mutablePreferencesOf())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val result = transform(state.value)
            val updated = result as? MutablePreferences
                ?: throw IllegalStateException("Expected MutablePreferences result")
            state.value = updated
            return updated
        }
    }

    private fun TestScope.createRepository(): CalendarSettingsRepository {
        return CalendarSettingsRepository(InMemoryPreferencesDataStore())
    }

    @Test
    fun calendarPrefsFlowReflectsUpdates() = runTest {
        val repository = createRepository()

        val updated = CalendarPrefs(
            enabled = true,
            targetCalendarId = 42L,
            titleTemplate = "Focus: %s",
            defaultDurationMin = 60,
            remindersMinBefore = 5,
            quietHours = 22..6,
            lastSyncTime = 1_234L,
            syncedEventCount = 7
        )

        repository.updateCalendarPrefs(updated)
        advanceUntilIdle()

        val emitted = repository.calendarPrefsFlow.first()
        assertTrue(emitted.enabled)
        assertEquals(updated.targetCalendarId, emitted.targetCalendarId)
        assertEquals(updated.titleTemplate, emitted.titleTemplate)
        assertEquals(updated.defaultDurationMin, emitted.defaultDurationMin)
        assertEquals(updated.remindersMinBefore, emitted.remindersMinBefore)
        assertEquals(updated.quietHours, emitted.quietHours)
        assertEquals(updated.lastSyncTime, emitted.lastSyncTime)
        assertEquals(updated.syncedEventCount, emitted.syncedEventCount)
    }

    @Test
    fun eventMappingsEmitAddedAndRemovedEntries() = runTest {
        val repository = createRepository()

        repository.addEventMapping("study-1", 100L)
        advanceUntilIdle()

        val mappingsAfterInsert = repository.eventMappingsFlow.first()
        assertEquals(1, mappingsAfterInsert.size)
        assertEquals("study-1", mappingsAfterInsert.first().studyEventId)
        assertEquals(100L, mappingsAfterInsert.first().calendarEventId)

        repository.removeEventMapping("study-1")
        advanceUntilIdle()

        val mappingsAfterRemoval = repository.eventMappingsFlow.first()
        assertTrue(mappingsAfterRemoval.isEmpty())
    }

    @Test
    fun setCalendarEnabledAndSyncStatsPersist() = runTest {
        val repository = createRepository()

        repository.setCalendarEnabled(true)
        repository.setTargetCalendar(77L)
        repository.updateSyncStats(syncTime = 5_000L, eventCount = 3)
        advanceUntilIdle()

        val emitted = repository.calendarPrefsFlow.first()
        assertTrue(emitted.enabled)
        assertEquals(77L, emitted.targetCalendarId)
        assertEquals(5_000L, emitted.lastSyncTime)
        assertEquals(3, emitted.syncedEventCount)

        repository.setCalendarEnabled(false)
        advanceUntilIdle()

        val disabled = repository.calendarPrefsFlow.first()
        assertFalse(disabled.enabled)
    }
}
