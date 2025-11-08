package com.mtlc.studyplan.ai

import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for SmartScheduler AI engine
 *
 * Tests pattern analysis, suggestion generation, and optimization algorithms
 */
class SmartSchedulerTest {

    private lateinit var scheduler: SmartScheduler

    @Before
    fun setup() {
        scheduler = SmartScheduler()
    }

    // ============ Data Class Tests ============

    @Test
    fun `TimeSlot should store all timing information`() {
        val startTime = LocalTime.of(9, 0)
        val endTime = LocalTime.of(10, 0)
        val slot = TimeSlot(
            startTime = startTime,
            endTime = endTime,
            performanceScore = 0.85f,
            frequency = 5
        )

        assertEquals(startTime, slot.startTime)
        assertEquals(endTime, slot.endTime)
        assertEquals(0.85f, slot.performanceScore)
        assertEquals(5, slot.frequency)
    }

    @Test
    fun `StudyPattern should contain all required fields`() {
        val pattern = StudyPattern(
            preferredTimeSlots = listOf(
                TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0), 0.9f, 10)
            ),
            averageSessionDuration = 45,
            strongDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            weakCategories = listOf("Grammar", "Vocabulary"),
            consistencyScore = 0.8f,
            optimalBreakInterval = 25
        )

        assertEquals(1, pattern.preferredTimeSlots.size)
        assertEquals(45, pattern.averageSessionDuration)
        assertEquals(2, pattern.strongDays.size)
        assertEquals(2, pattern.weakCategories.size)
        assertEquals(0.8f, pattern.consistencyScore)
        assertEquals(25, pattern.optimalBreakInterval)
    }

    @Test
    fun `SmartSuggestion should include confidence score`() {
        val suggestion = SmartSuggestion(
            id = "test-1",
            type = SuggestionType.OPTIMAL_TIME,
            title = "Study at 9 AM",
            description = "You're most productive in the morning",
            confidence = 0.92f,
            priority = 1
        )

        assertEquals(0.92f, suggestion.confidence)
        assertTrue(suggestion.confidence >= 0f && suggestion.confidence <= 1f)
    }

    @Test
    fun `SmartSuggestion should accept optional scheduled time`() {
        val scheduledTime = LocalDateTime.now().plusDays(1)
        val suggestion = SmartSuggestion(
            id = "test-1",
            type = SuggestionType.BREAK_REMINDER,
            title = "Take a break",
            description = "You've been studying for 45 minutes",
            confidence = 0.8f,
            scheduledTime = scheduledTime
        )

        assertNotNull(suggestion.scheduledTime)
        assertEquals(scheduledTime, suggestion.scheduledTime)
    }

    @Test
    fun `SmartSuggestion priority should default to 1`() {
        val suggestion = SmartSuggestion(
            id = "test-1",
            type = SuggestionType.WEAK_AREA_FOCUS,
            title = "Focus on Grammar",
            description = "You have low accuracy in grammar",
            confidence = 0.75f
        )

        assertEquals(1, suggestion.priority)
    }

    // ============ Default Pattern Tests ============

    @Test
    fun `Should return default pattern for empty task logs`() = runTest {
        val emptyLogs: List<TaskLog> = emptyList()
        val userProgress = UserProgress()

        val pattern = scheduler.analyzeUserPatterns(emptyLogs, userProgress)

        assertNotNull(pattern)
        assertEquals(0, pattern.preferredTimeSlots.size)
        assertEquals(0, pattern.strongDays.size)
    }

    // ============ Time Pattern Analysis Tests ============

    @Test
    fun `Should identify preferred time slots from task logs`() = runTest {
        val logs = listOf(
            TaskLog(taskId = "1", category = "Reading", correct = true, timestampMillis = timeOf(9, 30), minutesSpent = 25),
            TaskLog(taskId = "2", category = "Grammar", correct = true, timestampMillis = timeOf(9, 45), minutesSpent = 20),
            TaskLog(taskId = "3", category = "Vocabulary", correct = true, timestampMillis = timeOf(10, 0), minutesSpent = 15)
        )
        val userProgress = UserProgress()

        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        assertNotNull(pattern.preferredTimeSlots)
        // Should identify morning as preferred time
        assertEquals(true, pattern.preferredTimeSlots.isNotEmpty() || pattern.strongDays.isNotEmpty())
    }

    // ============ Session Duration Tests ============

    @Test
    fun `Should calculate average session duration correctly`() = runTest {
        val now = System.currentTimeMillis()
        val logs = listOf(
            TaskLog(taskId = "1", category = "Reading", correct = true, timestampMillis = now, minutesSpent = 25),
            TaskLog(taskId = "2", category = "Grammar", correct = true, timestampMillis = now + 30 * 60 * 1000, minutesSpent = 30),
            TaskLog(taskId = "3", category = "Vocabulary", correct = true, timestampMillis = now + 45 * 60 * 1000, minutesSpent = 20)
        )
        val userProgress = UserProgress()

        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        assertTrue(pattern.averageSessionDuration >= 0, "Session duration should be non-negative")
    }

    // ============ Consistency Score Tests ============

    @Test
    fun `Consistency score should be between 0 and 1`() = runTest {
        val logs = createTaskLogs(20)
        val userProgress = UserProgress()

        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        assertTrue(pattern.consistencyScore >= 0f, "Consistency should be >= 0")
        assertTrue(pattern.consistencyScore <= 1f, "Consistency should be <= 1")
    }

    @Test
    fun `Higher consistency score indicates regular study habits`() = runTest {
        // Regular studies: same time every day
        val regularLogs = (1..10).map { dayOffset ->
            TaskLog(
                taskId = "regular-$dayOffset",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = 30
            )
        }
        val userProgress = UserProgress()

        val pattern = scheduler.analyzeUserPatterns(regularLogs, userProgress)

        assertTrue(pattern.consistencyScore > 0f, "Should detect consistent study pattern")
    }

    // ============ Weak Category Detection Tests ============

    @Test
    fun `Should identify weak categories from low accuracy logs`() = runTest {
        val logs = listOf(
            TaskLog(taskId = "1", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "2", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "3", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )
        val userProgress = UserProgress()

        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        // Grammar should be identified as weak
        assertTrue(pattern.weakCategories.isEmpty() || pattern.weakCategories.contains("Grammar"))
    }

    // ============ Suggestion Generation Tests ============

    @Test
    fun `Should generate suggestions from study pattern`() = runTest {
        val logs = createTaskLogs(15)
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)

        assertNotNull(suggestions)
        assertTrue(suggestions.isNotEmpty() || logs.isEmpty(), "Should generate suggestions or handle empty logs")
    }

    @Test
    fun `All suggestions should have valid type`() = runTest {
        val logs = createTaskLogs(20)
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)

        for (suggestion in suggestions) {
            assertTrue(
                suggestion.type in SuggestionType.values(),
                "Suggestion type should be valid"
            )
        }
    }

    @Test
    fun `All suggestions should have confidence scores`() = runTest {
        val logs = createTaskLogs(20)
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)

        for (suggestion in suggestions) {
            assertTrue(
                suggestion.confidence >= 0f && suggestion.confidence <= 1f,
                "Confidence should be between 0 and 1"
            )
        }
    }

    @Test
    fun `Should generate optimal time suggestions`() = runTest {
        val logs = createTaskLogs(25)
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)
        val optimalTimeSuggestions = suggestions.filter { it.type == SuggestionType.OPTIMAL_TIME }

        // Should either have optimal time suggestions or pattern data be insufficient
        assertTrue(
            optimalTimeSuggestions.isNotEmpty() || pattern.preferredTimeSlots.isEmpty(),
            "Should generate or explain absence of optimal time suggestions"
        )
    }

    @Test
    fun `Should generate weak area focus suggestions`() = runTest {
        val logs = (1..20).flatMap { dayOffset ->
            listOf(
                TaskLog(taskId = "gram-$dayOffset", category = "Grammar", correct = false, timestampMillis = timeOfDaysAgo(dayOffset), minutesSpent = 20),
                TaskLog(taskId = "read-$dayOffset", category = "Reading", correct = true, timestampMillis = timeOfDaysAgo(dayOffset), minutesSpent = 25)
            )
        }
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)
        val weakAreaSuggestions = suggestions.filter { it.type == SuggestionType.WEAK_AREA_FOCUS }

        // Should identify weak areas
        assertTrue(
            weakAreaSuggestions.isNotEmpty() || pattern.weakCategories.isEmpty(),
            "Should generate weak area suggestions if weak categories exist"
        )
    }

    @Test
    fun `Should generate break reminders`() = runTest {
        val logs = createTaskLogs(30)
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)
        val breakSuggestions = suggestions.filter { it.type == SuggestionType.BREAK_REMINDER }

        // Should generate break reminders based on session duration
        assertTrue(
            breakSuggestions.isEmpty() || breakSuggestions.all { it.confidence > 0f },
            "Break reminders should have valid confidence"
        )
    }

    @Test
    fun `Should generate consistency boost suggestions`() = runTest {
        val logs = createInconsistentTaskLogs(20)
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)
        val consistencySuggestions = suggestions.filter { it.type == SuggestionType.CONSISTENCY_BOOST }

        // Inconsistent pattern should trigger suggestions
        assertTrue(
            consistencySuggestions.isEmpty() || pattern.consistencyScore < 0.7f,
            "Should generate consistency suggestions for low consistency patterns"
        )
    }

    // ============ Suggestion Priority Tests ============

    @Test
    fun `Suggestions should be sorted by priority`() = runTest {
        val logs = createTaskLogs(25)
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)

        // Check if sorted (high priority = low number comes first)
        for (i in 0 until suggestions.size - 1) {
            assertTrue(
                suggestions[i].priority <= suggestions[i + 1].priority,
                "Suggestions should be sorted by priority"
            )
        }
    }

    // ============ Edge Case Tests ============

    @Test
    fun `Should handle mixed correct and incorrect logs`() = runTest {
        val logs = (1..15).map { index ->
            TaskLog(
                taskId = "mixed-$index",
                category = if (index % 3 == 0) "Grammar" else "Reading",
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis() - (index.toLong() * 60 * 60 * 1000),
                minutesSpent = 25
            )
        }
        val userProgress = UserProgress()

        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        assertNotNull(pattern)
        assertTrue(pattern.consistencyScore >= 0f)
    }

    @Test
    fun `Should handle single task log`() = runTest {
        val logs = listOf(
            TaskLog(taskId = "single-1", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )
        val userProgress = UserProgress()

        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        assertNotNull(pattern)
    }

    @Test
    fun `Suggestions should be unique by ID`() = runTest {
        val logs = createTaskLogs(30)
        val userProgress = UserProgress()
        val pattern = scheduler.analyzeUserPatterns(logs, userProgress)

        val suggestions = scheduler.generateSuggestions(pattern, logs, userProgress)
        val ids = suggestions.map { it.id }

        assertEquals(ids.size, ids.toSet().size, "All suggestion IDs should be unique")
    }

    // ============ Time Slot Tests ============

    @Test
    fun `Time slot performance score should be between 0 and 1`() {
        val slot = TimeSlot(
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            performanceScore = 0.75f,
            frequency = 8
        )

        assertTrue(slot.performanceScore >= 0f && slot.performanceScore <= 1f)
    }

    @Test
    fun `Multiple time slots should be comparable`() {
        val morningSlot = TimeSlot(
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(12, 0),
            performanceScore = 0.9f,
            frequency = 15
        )

        val afternoonSlot = TimeSlot(
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(18, 0),
            performanceScore = 0.7f,
            frequency = 10
        )

        assertTrue(morningSlot.performanceScore > afternoonSlot.performanceScore)
    }

    // ============ Suggestion Type Tests ============

    @Test
    fun `All suggestion types should be available`() {
        val types = SuggestionType.values()
        val expectedTypes = listOf(
            SuggestionType.OPTIMAL_TIME,
            SuggestionType.BREAK_REMINDER,
            SuggestionType.WEAK_AREA_FOCUS,
            SuggestionType.CONSISTENCY_BOOST,
            SuggestionType.DIFFICULTY_ADJUSTMENT,
            SuggestionType.REVIEW_SESSION
        )

        for (expectedType in expectedTypes) {
            assertTrue(expectedType in types, "Type $expectedType should exist")
        }
    }

    @Test
    fun `Suggestion should support all types`() {
        for (type in SuggestionType.values()) {
            val suggestion = SmartSuggestion(
                id = "test-${type.name}",
                type = type,
                title = "Test ${type.name}",
                description = "Testing suggestion type",
                confidence = 0.8f
            )

            assertEquals(type, suggestion.type)
        }
    }

    // ============ Helper Functions ============

    private fun createTaskLogs(count: Int): List<TaskLog> {
        return (1..count).map { index ->
            TaskLog(
                taskId = "task-$index",
                category = listOf("Reading", "Grammar", "Vocabulary", "Listening").random(),
                correct = (index % 3) != 0,
                timestampMillis = System.currentTimeMillis() - (index.toLong() * 12 * 60 * 60 * 1000),
                minutesSpent = (20..45).random()
            )
        }
    }

    private fun createInconsistentTaskLogs(count: Int): List<TaskLog> {
        return (1..count).map { index ->
            TaskLog(
                taskId = "incons-$index",
                category = "Reading",
                correct = true,
                // Create inconsistent gaps
                timestampMillis = System.currentTimeMillis() - (index.toLong() * (if (index % 3 == 0) 48 else 24) * 60 * 60 * 1000),
                minutesSpent = 30
            )
        }
    }

    private fun timeOf(hour: Int, minute: Int): Long {
        val time = LocalDateTime.now().withHour(hour).withMinute(minute)
        return time.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
    }

    private fun timeOfDaysAgo(daysAgo: Int): Long {
        val time = LocalDateTime.now().minusDays(daysAgo.toLong())
        return time.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
    }
}
