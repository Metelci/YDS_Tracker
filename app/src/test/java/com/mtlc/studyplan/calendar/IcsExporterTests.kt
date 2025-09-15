package com.mtlc.studyplan.calendar

import com.mtlc.studyplan.data.DayPlan
import com.mtlc.studyplan.data.Task
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Unit tests for ICS (iCalendar) export functionality
 */
class IcsExporterTests {

    private lateinit var testStudySlots: List<StudyTimeSlot>
    private lateinit var testDayPlans: List<DayPlan>

    @Before
    fun setup() {
        // Create test study slots
        testStudySlots = listOf(
            StudyTimeSlot(
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(9, 0),
                durationMinutes = 60,
                title = "Study: Grammar Fundamentals",
                description = "Review basic grammar rules and practice exercises",
                studyEventId = "test_session_1"
            ),
            StudyTimeSlot(
                date = LocalDate.now().plusDays(2),
                startTime = LocalTime.of(14, 30),
                durationMinutes = 45,
                title = "Study: Reading Comprehension",
                description = "Practice reading passages and answer questions",
                studyEventId = "test_session_2"
            )
        )

        // Create test day plans
        testDayPlans = listOf(
            DayPlan(
                day = LocalDate.now().plusDays(1).toString(),
                tasks = listOf(
                    Task(id = "task1", desc = "Grammar Review", details = "Focus on verb tenses"),
                    Task(id = "task2", desc = "Vocabulary Practice", details = "Learn 20 new words")
                )
            ),
            DayPlan(
                day = LocalDate.now().plusDays(2).toString(),
                tasks = listOf(
                    Task(id = "task3", desc = "Reading Practice", details = "Read 3 articles"),
                    Task(id = "task4", desc = "Listening Exercise", details = "Listen to podcast")
                )
            )
        )
    }

    /**
     * Test ICS generation for 2 sessions with proper RFC-5545 format
     */
    @Test
    fun testIcsGenerationWithTwoSessions() = runBlocking {
        val icsBytes = IcsExporter.exportStudySlots(testStudySlots)
        val icsContent = String(icsBytes, Charsets.UTF_8)

        // Assert basic structure
        assertTrue("ICS should contain BEGIN:VCALENDAR", icsContent.contains("BEGIN:VCALENDAR"))
        assertTrue("ICS should contain END:VCALENDAR", icsContent.contains("END:VCALENDAR"))
        assertTrue("ICS should contain VERSION", icsContent.contains("VERSION:2.0"))
        assertTrue("ICS should contain PRODID", icsContent.contains("PRODID:"))

        // Count VEVENTs
        val beginEventCount = icsContent.split("BEGIN:VEVENT").size - 1
        val endEventCount = icsContent.split("END:VEVENT").size - 1
        
        assertEquals("Should contain exactly 2 VEVENTs", 2, beginEventCount)
        assertEquals("BEGIN and END VEVENT count should match", beginEventCount, endEventCount)
    }

    /**
     * Test DTSTART and DTEND format validation
     */
    @Test
    fun testDateTimeFormats() = runBlocking {
        val icsBytes = IcsExporter.exportStudySlots(testStudySlots)
        val icsContent = String(icsBytes, Charsets.UTF_8)

        // Extract DTSTART and DTEND lines
        val dtStartLines = icsContent.lines().filter { it.startsWith("DTSTART") }
        val dtEndLines = icsContent.lines().filter { it.startsWith("DTEND") }

        assertEquals("Should have 2 DTSTART entries", 2, dtStartLines.size)
        assertEquals("Should have 2 DTEND entries", 2, dtEndLines.size)

        // Validate format: DTSTART;TZID=timezone:YYYYMMDDTHHmmss
        dtStartLines.forEach { dtStart ->
            assertTrue("DTSTART should contain TZID", dtStart.contains("TZID="))
            assertTrue("DTSTART should contain colon separator", dtStart.contains(":"))
            
            val dateTimePart = dtStart.split(":").last()
            assertTrue("DTSTART should match YYYYMMDDTHHmmss format", 
                      dateTimePart.matches(Regex("\\d{8}T\\d{6}")))
        }

        dtEndLines.forEach { dtEnd ->
            assertTrue("DTEND should contain TZID", dtEnd.contains("TZID="))
            assertTrue("DTEND should contain colon separator", dtEnd.contains(":"))
            
            val dateTimePart = dtEnd.split(":").last()
            assertTrue("DTEND should match YYYYMMDDTHHmmss format", 
                      dateTimePart.matches(Regex("\\d{8}T\\d{6}")))
        }
    }

    /**
     * Test event properties are properly included
     */
    @Test
    fun testEventProperties() = runBlocking {
        val icsBytes = IcsExporter.exportStudySlots(testStudySlots)
        val icsContent = String(icsBytes, Charsets.UTF_8)

        // Check for required properties
        assertTrue("Should contain SUMMARY", icsContent.contains("SUMMARY:"))
        assertTrue("Should contain DESCRIPTION", icsContent.contains("DESCRIPTION:"))
        assertTrue("Should contain UID", icsContent.contains("UID:"))
        assertTrue("Should contain DTSTAMP", icsContent.contains("DTSTAMP:"))

        // Check for study-specific content
        assertTrue("Should contain study title", icsContent.contains("Grammar Fundamentals"))
        assertTrue("Should contain study title", icsContent.contains("Reading Comprehension"))

        // Check for categories
        assertTrue("Should contain study categories", icsContent.contains("CATEGORIES:Study,Education"))
        
        // Check for alarms
        assertTrue("Should contain alarm", icsContent.contains("BEGIN:VALARM"))
        assertTrue("Should contain alarm trigger", icsContent.contains("TRIGGER:-PT15M"))
    }

    /**
     * Test ICS validation utility
     */
    @Test
    fun testIcsValidation() = runBlocking {
        val icsBytes = IcsExporter.exportStudySlots(testStudySlots)
        val icsContent = String(icsBytes, Charsets.UTF_8)

        val validationResult = IcsExporter.validateIcsContent(icsContent)
        
        assertTrue("ICS content should be valid", validationResult.isValid)
        assertTrue("Should have no validation errors", validationResult.errors.isEmpty())
        assertEquals("Should detect 2 events", 2, validationResult.eventCount)
    }

    /**
     * Test invalid ICS content validation
     */
    @Test
    fun testInvalidIcsValidation() {
        val invalidIcs = "BEGIN:VCALENDAR\nVERSION:2.0\nBEGIN:VEVENT\n" // Missing END tags
        
        val validationResult = IcsExporter.validateIcsContent(invalidIcs)
        
        assertFalse("Invalid ICS should fail validation", validationResult.isValid)
        assertFalse("Should have validation errors", validationResult.errors.isEmpty())
        assertTrue("Should detect missing END:VCALENDAR", validationResult.errors.any { it.contains("END:VCALENDAR") })
    }

    /**
     * Test ICS export from day plans
     */
    @Test
    fun testIcsFromDayPlans() = runBlocking {
        val icsBytes = IcsExporter.export(testDayPlans)
        val icsContent = String(icsBytes, Charsets.UTF_8)

        // Should generate valid ICS
        assertTrue("Should contain BEGIN:VCALENDAR", icsContent.contains("BEGIN:VCALENDAR"))
        assertTrue("Should contain events", icsContent.contains("BEGIN:VEVENT"))

        // Should contain task information
        assertTrue("Should reference grammar", icsContent.contains("Grammar") || icsContent.contains("grammar"))
        assertTrue("Should reference reading", icsContent.contains("Reading") || icsContent.contains("reading"))
    }

    /**
     * Test text escaping for ICS format
     */
    @Test
    fun testTextEscaping() = runBlocking {
        val specialTextSlot = StudyTimeSlot(
            date = LocalDate.now().plusDays(1),
            startTime = LocalTime.of(10, 0),
            durationMinutes = 30,
            title = "Study: Test;With,Special\\Characters\nAnd Lines",
            description = "Description with\nNew lines,semicolons;and\\backslashes",
            studyEventId = "special_text_test"
        )

        val icsBytes = IcsExporter.exportStudySlots(listOf(specialTextSlot))
        val icsContent = String(icsBytes, Charsets.UTF_8)

        // Verify escaping
        assertTrue("Should escape semicolons", icsContent.contains("\\;"))
        assertTrue("Should escape commas", icsContent.contains("\\,"))
        assertTrue("Should escape backslashes", icsContent.contains("\\\\"))
        assertTrue("Should escape newlines", icsContent.contains("\\n"))
        
        // Should still be valid ICS
        val validationResult = IcsExporter.validateIcsContent(icsContent)
        assertTrue("Escaped content should still be valid", validationResult.isValid)
    }

    /**
     * Test empty input handling
     */
    @Test
    fun testEmptyInput() = runBlocking {
        val icsBytes = IcsExporter.exportStudySlots(emptyList())
        val icsContent = String(icsBytes, Charsets.UTF_8)

        // Should still be valid ICS structure
        assertTrue("Should contain BEGIN:VCALENDAR", icsContent.contains("BEGIN:VCALENDAR"))
        assertTrue("Should contain END:VCALENDAR", icsContent.contains("END:VCALENDAR"))
        
        val validationResult = IcsExporter.validateIcsContent(icsContent)
        assertTrue("Empty ICS should be valid", validationResult.isValid)
        assertEquals("Should have 0 events", 0, validationResult.eventCount)
    }

    /**
     * Test timezone handling
     */
    @Test
    fun testTimezoneHandling() = runBlocking {
        val icsBytes = IcsExporter.exportStudySlots(testStudySlots)
        val icsContent = String(icsBytes, Charsets.UTF_8)

        // Should contain timezone definition
        assertTrue("Should contain VTIMEZONE", icsContent.contains("BEGIN:VTIMEZONE"))
        assertTrue("Should contain TZID", icsContent.contains("TZID:"))
        assertTrue("Should contain TZOFFSETFROM", icsContent.contains("TZOFFSETFROM:"))
        assertTrue("Should contain TZOFFSETTO", icsContent.contains("TZOFFSETTO:"))
    }

    /**
     * Test filename generation
     */
    @Test
    fun testFilenameGeneration() {
        val filename = IcsExporter.generateIcsFilename("test-schedule")
        
        assertTrue("Should end with .ics", filename.endsWith(".ics"))
        assertTrue("Should contain prefix", filename.contains("test-schedule"))
        assertTrue("Should contain date", filename.matches(Regex(".*\\d{4}-\\d{2}-\\d{2}.*")))
    }

    /**
     * Test duration calculations
     */
    @Test
    fun testDurationCalculations() = runBlocking {
        val slot = StudyTimeSlot(
            date = LocalDate.now().plusDays(1),
            startTime = LocalTime.of(9, 0),
            durationMinutes = 75, // 1 hour 15 minutes
            title = "Extended Study Session",
            description = "Longer session test",
            studyEventId = "duration_test"
        )

        val icsBytes = IcsExporter.exportStudySlots(listOf(slot))
        val icsContent = String(icsBytes, Charsets.UTF_8)

        // Extract DTSTART and DTEND
        val dtStartLine = icsContent.lines().find { it.startsWith("DTSTART") }
        val dtEndLine = icsContent.lines().find { it.startsWith("DTEND") }

        assertNotNull("Should have DTSTART", dtStartLine)
        assertNotNull("Should have DTEND", dtEndLine)

        // Parse times to verify duration
        val startTime = dtStartLine!!.split(":").last()
        val endTime = dtEndLine!!.split(":").last()

        // Start should be 09:00, end should be 10:15 (75 minutes later)
        assertTrue("Start time should be 090000", startTime.endsWith("090000"))
        assertTrue("End time should be 101500", endTime.endsWith("101500"))
    }
}

/**
 * Fake implementation of CalendarProvider for testing
 */
class FakeCalendarProvider : CalendarProvider {
    
    private var hasPermissions = true
    private val calendars = mutableListOf<CalendarInfo>()
    private val events = mutableMapOf<Long, CalendarEventData>()
    private var nextEventId = 1L

    fun setHasPermissions(hasPermissions: Boolean) {
        this.hasPermissions = hasPermissions
    }

    fun addCalendar(calendar: CalendarInfo) {
        calendars.add(calendar)
    }

    fun getStoredEvents(): Map<Long, CalendarEventData> = events.toMap()

    override fun hasCalendarPermissions(): Boolean = hasPermissions

    override fun getAvailableCalendars(): List<CalendarInfo> = calendars.toList()

    override fun insertEvent(calendarId: Long, event: CalendarEventData): Long? {
        if (!hasPermissions) return null
        
        val eventId = nextEventId++
        events[eventId] = event.copy(calendarId = calendarId)
        return eventId
    }

    override fun updateEvent(eventId: Long, event: CalendarEventData): Boolean {
        if (!hasPermissions) return false
        
        return if (events.containsKey(eventId)) {
            events[eventId] = event
            true
        } else false
    }

    override fun deleteEvent(eventId: Long): Boolean {
        if (!hasPermissions) return false
        
        // Safety check: only delete events with our UID prefix
        val event = events[eventId]
        if (event?.uid?.startsWith("studyplan_") != true) {
            return false
        }
        
        return events.remove(eventId) != null
    }

    override fun getEventById(eventId: Long): CalendarEventData? {
        if (!hasPermissions) return null
        return events[eventId]
    }
}

/**
 * Tests for calendar sync with fake provider
 */
class CalendarSyncWithFakeTests {
    
    private lateinit var fakeProvider: FakeCalendarProvider
    private lateinit var calendarSync: CalendarSync
    private lateinit var testDayPlans: List<DayPlan>

    @Before
    fun setup() {
        fakeProvider = FakeCalendarProvider().apply {
            setHasPermissions(true)
            addCalendar(
                CalendarInfo(
                    id = 1L,
                    displayName = "Test Calendar",
                    accountName = "test@example.com",
                    accountType = "com.google",
                    isPrimary = true,
                    accessLevel = 700 // Full access
                )
            )
        }

        // Note: In a real test, you'd inject this via constructor or DI
        // For this example, we'll create a mock settings repository
        // calendarSync = CalendarSync(context, settingsRepository, fakeProvider)
        
        testDayPlans = listOf(
            DayPlan(
                day = LocalDate.now().plusDays(1).toString(),
                tasks = listOf(
                    Task(id = "task1", desc = "Grammar Review"),
                    Task(id = "task2", desc = "Vocabulary Practice")
                )
            )
        )
    }

    @Test
    fun testFakeProviderBasicOperations() {
        // Test calendar listing
        val calendars = fakeProvider.getAvailableCalendars()
        assertEquals("Should have 1 test calendar", 1, calendars.size)
        assertEquals("Should have correct calendar name", "Test Calendar", calendars.first().displayName)

        // Test event creation
        val event = CalendarEventData(
            calendarId = 1L,
            title = "Test Event",
            description = "Test Description",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + 60 * 60 * 1000,
            uid = "studyplan_test"
        )

        val eventId = fakeProvider.insertEvent(1L, event)
        assertNotNull("Should create event successfully", eventId)

        // Test event retrieval
        val retrievedEvent = fakeProvider.getEventById(eventId!!)
        assertNotNull("Should retrieve created event", retrievedEvent)
        assertEquals("Title should match", "Test Event", retrievedEvent!!.title)
    }

    @Test
    fun testFakeProviderPermissionHandling() {
        // Test with permissions
        assertTrue("Should have permissions", fakeProvider.hasCalendarPermissions())
        
        val event = CalendarEventData(
            calendarId = 1L,
            title = "Test Event",
            description = "Test Description",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + 60 * 60 * 1000
        )

        val eventId = fakeProvider.insertEvent(1L, event)
        assertNotNull("Should create event with permissions", eventId)

        // Remove permissions
        fakeProvider.setHasPermissions(false)
        assertFalse("Should not have permissions", fakeProvider.hasCalendarPermissions())
        
        val noPermissionEventId = fakeProvider.insertEvent(1L, event)
        assertNull("Should not create event without permissions", noPermissionEventId)
        
        val calendars = fakeProvider.getAvailableCalendars()
        assertTrue("Should return empty calendar list without permissions", calendars.isEmpty())
    }

    @Test
    fun testFakeProviderSafetyChecks() {
        // Create event with our UID
        val ourEvent = CalendarEventData(
            calendarId = 1L,
            title = "Our Event",
            description = "Our Description",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + 60 * 60 * 1000,
            uid = "studyplan_our_event"
        )

        val ourEventId = fakeProvider.insertEvent(1L, ourEvent)!!

        // Create event without our UID (external event)
        val externalEvent = CalendarEventData(
            calendarId = 1L,
            title = "External Event",
            description = "External Description",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + 60 * 60 * 1000,
            uid = "external_event"
        )

        val externalEventId = fakeProvider.insertEvent(1L, externalEvent)!!

        // Should be able to delete our event
        assertTrue("Should delete our event", fakeProvider.deleteEvent(ourEventId))
        
        // Should not be able to delete external event
        assertFalse("Should not delete external event", fakeProvider.deleteEvent(externalEventId))
        
        // Verify external event still exists
        assertNotNull("External event should still exist", fakeProvider.getEventById(externalEventId))
    }
}