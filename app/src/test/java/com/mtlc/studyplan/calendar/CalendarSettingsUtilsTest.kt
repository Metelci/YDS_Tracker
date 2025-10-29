package com.mtlc.studyplan.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalendarSettingsUtilsTest {

    @Test
    fun `isWithinQuietHours detects normal ranges`() {
        val quietHours = 20..22

        assertTrue(CalendarSettingsUtils.isWithinQuietHours(LocalTime.of(21, 0), quietHours))
        assertFalse(CalendarSettingsUtils.isWithinQuietHours(LocalTime.of(10, 0), quietHours))
    }

    @Test
    fun `adjustForQuietHours moves start time to end of quiet window`() {
        val adjusted = CalendarSettingsUtils.adjustForQuietHours(
            time = LocalTime.of(21, 30),
            quietHours = 20..22
        )

        assertEquals(LocalTime.of(23, 0), adjusted)
    }

    @Test
    fun `generateTitle formats template and truncates task description`() {
        val longTask = "L".repeat(80)
        val titleWithPlaceholder = CalendarSettingsUtils.generateTitle("Study %s", longTask)
        assertTrue(titleWithPlaceholder.startsWith("Study "))
        assertEquals(56, titleWithPlaceholder.length) // "Study " + 50 chars of content

        val titleWithoutPlaceholder = CalendarSettingsUtils.generateTitle("Session", "Vocabulary")
        assertEquals("Session Vocabulary", titleWithoutPlaceholder)
    }

    @Test
    fun `calculateOptimalTimeSlot returns slot within availability`() {
        val slot = CalendarSettingsUtils.calculateOptimalTimeSlot(
            date = LocalDate.of(2025, 1, 1),
            availabilityStart = LocalTime.of(9, 0),
            availabilityEnd = LocalTime.of(11, 0),
            quietHours = 20..22,
            durationMinutes = 60
        )

        requireNotNull(slot)
        assertEquals(LocalTime.of(9, 0), slot.startTime)
        assertEquals(LocalTime.of(10, 0), slot.endTime)
    }

    @Test
    fun `calculateOptimalTimeSlot returns null when window too small`() {
        val slot = CalendarSettingsUtils.calculateOptimalTimeSlot(
            date = LocalDate.now(),
            availabilityStart = LocalTime.of(9, 0),
            availabilityEnd = LocalTime.of(9, 15),
            durationMinutes = 30
        )

        assertNull(slot)
    }
}
