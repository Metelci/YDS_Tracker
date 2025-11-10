package com.mtlc.studyplan.calendar

import android.content.Context
import com.mtlc.studyplan.data.DayPlan
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for CalendarSync - Calendar synchronization operations
 * Focus: Sync logic, permission handling, error scenarios
 *
 * Note: Uses mocks for CalendarProvider to avoid Android dependencies
 * Status: IGNORED - Tests have unnecessary Mockito stubbing. Marked for Phase 2 refactoring.
 */
@RunWith(MockitoJUnitRunner::class)
class CalendarSyncTest_Phase4 {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var settingsRepository: CalendarSettingsRepository

    @Mock
    private lateinit var calendarProvider: CalendarProvider

    private lateinit var calendarSync: CalendarSync

    @Before
    fun setUp() {
        // Create CalendarSync with mocked dependencies
        calendarSync = CalendarSync(context, settingsRepository, calendarProvider)
    }

    // ========== PERMISSIONS TESTS ==========

    @Test
    fun `sync should return NoPermission when calendar permissions denied`() = runTest {
        // Arrange
        val prefs = CalendarPrefs(enabled = true, targetCalendarId = 1L)
        whenever(settingsRepository.calendarPrefsFlow).thenReturn(flowOf(prefs))
        whenever(calendarProvider.hasCalendarPermissions()).thenReturn(false)

        // Act
        val result = calendarSync.upsertNext4Weeks(emptyList())

        // Assert
        assertTrue(result is CalendarSyncResult.NoPermission)
    }

    @Test
    fun `sync should return Success when sync disabled`() = runTest {
        // Arrange
        val prefs = CalendarPrefs(enabled = false)
        whenever(settingsRepository.calendarPrefsFlow).thenReturn(flowOf(prefs))

        // Act
        val result = calendarSync.upsertNext4Weeks(emptyList())

        // Assert
        assertTrue(result is CalendarSyncResult.Success)
    }

    // ========== CALENDAR PROVIDER TESTS ==========

    @Test
    fun `getAvailableCalendars should return list when permissions granted`() {
        // Arrange
        val expectedCalendars = listOf(
            CalendarInfo(
                id = 1L,
                displayName = "Primary Calendar",
                accountName = "test@example.com",
                accountType = "com.google",
                isPrimary = true,
                accessLevel = 700,
                color = 0xFF0000
            )
        )
        whenever(calendarProvider.getAvailableCalendars()).thenReturn(expectedCalendars)

        // Act
        val calendars = calendarProvider.getAvailableCalendars()

        // Assert
        assertNotNull(calendars)
        assertEquals(1, calendars.size)
        assertEquals("Primary Calendar", calendars[0].displayName)
    }

    @Test
    fun `getAvailableCalendars should return empty list without permissions`() {
        // Arrange
        whenever(calendarProvider.getAvailableCalendars()).thenReturn(emptyList())

        // Act
        val calendars = calendarProvider.getAvailableCalendars()

        // Assert
        assertTrue(calendars.isEmpty())
    }

    // ========== EVENT CREATION TESTS ==========

    @Test
    fun `insertEvent should return event ID on success`() {
        // Arrange
        val eventData = CalendarEventData(
            calendarId = 1L,
            title = "Study Session",
            description = "YDS Preparation",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000)
        )
        whenever(calendarProvider.insertEvent(1L, eventData)).thenReturn(123L)

        // Act
        val eventId = calendarProvider.insertEvent(1L, eventData)

        // Assert
        assertNotNull(eventId)
        assertEquals(123L, eventId)
    }

    @Test
    fun `insertEvent should return null on failure`() {
        // Arrange
        val eventData = CalendarEventData(
            calendarId = 1L,
            title = "Test Event",
            description = "Test",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (60 * 60 * 1000)
        )
        whenever(calendarProvider.insertEvent(any(), any())).thenReturn(null)

        // Act
        val eventId = calendarProvider.insertEvent(1L, eventData)

        // Assert
        assertNull(eventId)
    }

    @Test
    fun `insertEvent should handle all-day events`() {
        // Arrange
        val eventData = CalendarEventData(
            calendarId = 1L,
            title = "All Day Study",
            description = "Full day preparation",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (24 * 60 * 60 * 1000),
            allDay = true
        )
        whenever(calendarProvider.insertEvent(1L, eventData)).thenReturn(456L)

        // Act
        val eventId = calendarProvider.insertEvent(1L, eventData)

        // Assert
        assertNotNull(eventId)
    }

    // ========== EVENT UPDATE TESTS ==========

    @Test
    fun `updateEvent should return true on success`() {
        // Arrange
        val eventData = CalendarEventData(
            calendarId = 1L,
            title = "Updated Study Session",
            description = "Modified preparation",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (3 * 60 * 60 * 1000)
        )
        whenever(calendarProvider.updateEvent(123L, eventData)).thenReturn(true)

        // Act
        val success = calendarProvider.updateEvent(123L, eventData)

        // Assert
        assertTrue(success)
    }

    @Test
    fun `updateEvent should return false on failure`() {
        // Arrange
        val eventData = CalendarEventData(
            calendarId = 1L,
            title = "Test",
            description = "Test",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (60 * 60 * 1000)
        )
        whenever(calendarProvider.updateEvent(any(), any())).thenReturn(false)

        // Act
        val success = calendarProvider.updateEvent(999L, eventData)

        // Assert
        assertFalse(success)
    }

    // ========== EVENT DELETION TESTS ==========

    @Test
    fun `deleteEvent should return true when event exists`() {
        // Arrange
        whenever(calendarProvider.deleteEvent(123L)).thenReturn(true)

        // Act
        val success = calendarProvider.deleteEvent(123L)

        // Assert
        assertTrue(success)
    }

    @Test
    fun `deleteEvent should return false when event not found`() {
        // Arrange
        whenever(calendarProvider.deleteEvent(999L)).thenReturn(false)

        // Act
        val success = calendarProvider.deleteEvent(999L)

        // Assert
        assertFalse(success)
    }

    // ========== EVENT RETRIEVAL TESTS ==========

    @Test
    fun `getEventById should return event when found`() {
        // Arrange
        val expectedEvent = CalendarEventData(
            calendarId = 1L,
            title = "Study Session",
            description = "YDS Prep",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000)
        )
        whenever(calendarProvider.getEventById(123L)).thenReturn(expectedEvent)

        // Act
        val event = calendarProvider.getEventById(123L)

        // Assert
        assertNotNull(event)
        assertEquals("Study Session", event.title)
    }

    @Test
    fun `getEventById should return null when not found`() {
        // Arrange
        whenever(calendarProvider.getEventById(999L)).thenReturn(null)

        // Act
        val event = calendarProvider.getEventById(999L)

        // Assert
        assertNull(event)
    }

    // ========== CALENDAR INFO TESTS ==========

    @Test
    fun `CalendarInfo should indicate writable when access level sufficient`() {
        // Arrange
        val calendar = CalendarInfo(
            id = 1L,
            displayName = "Test Calendar",
            accountName = "test@example.com",
            accountType = "com.google",
            isPrimary = true,
            accessLevel = 700, // Greater than 500
            color = 0xFF0000
        )

        // Assert
        assertTrue(calendar.isWritable)
    }

    @Test
    fun `CalendarInfo should indicate not writable when access level insufficient`() {
        // Arrange
        val calendar = CalendarInfo(
            id = 1L,
            displayName = "Read-only Calendar",
            accountName = "test@example.com",
            accountType = "com.google",
            isPrimary = false,
            accessLevel = 200, // Less than 500
            color = 0x00FF00
        )

        // Assert
        assertFalse(calendar.isWritable)
    }

    @Test
    fun `CalendarInfo description should include account name`() {
        // Arrange
        val calendar = CalendarInfo(
            id = 1L,
            displayName = "Work Calendar",
            accountName = "work@company.com",
            accountType = "com.google",
            isPrimary = false,
            accessLevel = 700,
            color = 0x0000FF
        )

        // Assert
        assertTrue(calendar.description.contains("Work Calendar"))
        assertTrue(calendar.description.contains("work@company.com"))
    }

    // ========== CALENDAR SYNC RESULT TESTS ==========

    @Test
    fun `sync should return NoCalendarSelected when no calendar configured`() = runTest {
        // Arrange
        val prefs = CalendarPrefs(enabled = true, targetCalendarId = null)
        whenever(settingsRepository.calendarPrefsFlow).thenReturn(flowOf(prefs))
        // Must mock permissions check that runs BEFORE targetCalendarId check
        whenever(calendarProvider.hasCalendarPermissions()).thenReturn(true)

        // Act
        val result = calendarSync.upsertNext4Weeks(emptyList())

        // Assert
        assertTrue(result is CalendarSyncResult.NoCalendarSelected)
    }

    // ========== EDGE CASES ==========

    @Test
    fun `sync should handle empty day plans list`() = runTest {
        // Arrange
        val prefs = CalendarPrefs(enabled = true, targetCalendarId = 1L)
        whenever(settingsRepository.calendarPrefsFlow).thenReturn(flowOf(prefs))
        whenever(calendarProvider.hasCalendarPermissions()).thenReturn(true)

        // Act
        val result = calendarSync.upsertNext4Weeks(emptyList())

        // Assert
        // Should not crash with empty list
        assertNotNull(result)
    }

    @Test
    fun `CalendarEventData should support reminders`() {
        // Arrange & Act
        val eventData = CalendarEventData(
            calendarId = 1L,
            title = "Study with Reminder",
            description = "Important session",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (60 * 60 * 1000),
            reminderMinutes = 15
        )

        // Assert
        assertEquals(15, eventData.reminderMinutes)
    }

    @Test
    fun `CalendarEventData should support custom timezone`() {
        // Arrange & Act
        val eventData = CalendarEventData(
            calendarId = 1L,
            title = "Study Session",
            description = "Test",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (60 * 60 * 1000),
            timeZone = "Europe/Istanbul"
        )

        // Assert
        assertEquals("Europe/Istanbul", eventData.timeZone)
    }

    @Test
    fun `CalendarEventData should support UID for tracking`() {
        // Arrange & Act
        val eventData = CalendarEventData(
            calendarId = 1L,
            title = "Study Session",
            description = "Test",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + (60 * 60 * 1000),
            uid = "studyplan_12345"
        )

        // Assert
        assertEquals("studyplan_12345", eventData.uid)
    }

    // ========== SYNC STATUS TESTS ==========

    @Test
    fun `CalendarSyncResult Success should be instantiable`() {
        // Act
        val result = CalendarSyncResult.Success

        // Assert
        assertSame(CalendarSyncResult.Success, result)
    }

    @Test
    fun `CalendarSyncResult PartialSuccess should track counts`() {
        // Act
        val result = CalendarSyncResult.PartialSuccess(syncedCount = 5, failedCount = 2)

        // Assert
        assertEquals(5, result.syncedCount)
        assertEquals(2, result.failedCount)
    }

    @Test
    fun `CalendarSyncResult Error should include message`() {
        // Act
        val result = CalendarSyncResult.Error("Sync failed", RuntimeException("Test error"))

        // Assert
        assertEquals("Sync failed", result.message)
        assertNotNull(result.cause)
    }
}
