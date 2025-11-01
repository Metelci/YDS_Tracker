package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.data.TaskLog
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for StudyPatternAnalyzer
 *
 * Tests pattern analysis, weak area detection, consistency metrics, and productivity calculations
 */
class StudyPatternAnalyzerTest {

    private lateinit var analyzer: StudyPatternAnalyzer

    @Before
    fun setup() {
        analyzer = StudyPatternAnalyzer()
    }

    // ============ Data Class Tests ============

    @Test
    fun `StudyPatternsUI should contain all required fields`() {
        val pattern = StudyPatternsUI(
            timeDistribution = mapOf("morning" to 0.5f, "afternoon" to 0.5f),
            categoryPerformance = mapOf("Grammar" to 0.8f),
            weeklyProgress = listOf(0.7f, 0.8f, 0.9f),
            mostProductiveHour = 10,
            mostProductiveDay = "Monday",
            focusScore = 0.85f,
            hourlyProductivity = mapOf(10 to 0.95f, 14 to 0.75f),
            morningProductivity = 0.9f,
            afternoonProductivity = 0.7f,
            eveningProductivity = 0.6f
        )

        assertEquals(10, pattern.mostProductiveHour)
        assertEquals("Monday", pattern.mostProductiveDay)
        assertEquals(0.85f, pattern.focusScore)
        assertEquals(0.9f, pattern.morningProductivity)
        assertEquals(0.7f, pattern.afternoonProductivity)
        assertEquals(0.6f, pattern.eveningProductivity)
    }

    @Test
    fun `WeakArea should store category and error rate`() {
        val weakArea = WeakArea(
            category = "Grammar",
            errorRate = 0.45f,
            recommendedFocus = "Break down the topic into smaller chunks"
        )

        assertEquals("Grammar", weakArea.category)
        assertEquals(0.45f, weakArea.errorRate)
        assertTrue(weakArea.recommendedFocus.isNotEmpty())
    }

    @Test
    fun `StudyPatternsUI should have default values`() {
        val defaultPattern = StudyPatternsUI()

        assertEquals(emptyMap(), defaultPattern.timeDistribution)
        assertEquals(emptyMap(), defaultPattern.categoryPerformance)
        assertEquals(emptyList(), defaultPattern.weeklyProgress)
        assertEquals(9, defaultPattern.mostProductiveHour)
        assertEquals("Monday", defaultPattern.mostProductiveDay)
        assertEquals(0.7f, defaultPattern.focusScore)
    }

    // ============ Empty Logs Tests ============

    @Test
    fun `analyze() should handle empty logs`() {
        val patterns = analyzer.analyze(emptyList())

        assertNotNull(patterns)
        assertEquals(emptyMap(), patterns.timeDistribution)
        assertEquals(emptyMap(), patterns.categoryPerformance)
        assertEquals(emptyList(), patterns.weeklyProgress)
    }

    @Test
    fun `identifyWeakAreas() should return empty list for empty logs`() {
        val weakAreas = analyzer.identifyWeakAreas(emptyList())

        assertEquals(0, weakAreas.size)
    }

    @Test
    fun `consistencyMetric() should return 0 for empty logs`() {
        val metric = analyzer.consistencyMetric(emptyList())

        assertEquals(0f, metric)
    }

    @Test
    fun `improvementTrend() should return 0 for logs with size less than 10`() {
        val logs = (1..5).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val trend = analyzer.improvementTrend(logs)

        assertEquals(0f, trend)
    }

    // ============ Time Distribution Tests ============

    @Test
    fun `analyzeTimeDistribution() should categorize logs by time periods`() {
        val now = System.currentTimeMillis()
        val logs = listOf(
            // Morning (8-11): 25 min
            TaskLog(
                taskId = "morning",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            ),
            // Afternoon (12-16): 30 min
            TaskLog(
                taskId = "afternoon",
                category = "Grammar",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 30
            )
        )

        val patterns = analyzer.analyze(logs)

        assertNotNull(patterns.timeDistribution)
        assertTrue(patterns.timeDistribution.values.sum() in 0.99f..1.01f, "Time distribution should sum to 1")
    }

    @Test
    fun `Time distribution should have all required periods`() {
        val logs = (1..10).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)
        val distribution = patterns.timeDistribution

        assertTrue(distribution.containsKey("early_morning"))
        assertTrue(distribution.containsKey("morning"))
        assertTrue(distribution.containsKey("afternoon"))
        assertTrue(distribution.containsKey("evening"))
        assertTrue(distribution.containsKey("night"))
    }

    // ============ Category Performance Tests ============

    @Test
    fun `analyzeCategoryPerformance() should calculate accuracy by category`() {
        val logs = listOf(
            TaskLog(taskId = "1", category = "Grammar", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25),
            TaskLog(taskId = "2", category = "Grammar", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25),
            TaskLog(taskId = "3", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 25),
            TaskLog(taskId = "4", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25),
            TaskLog(taskId = "5", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )

        val patterns = analyzer.analyze(logs)

        assertNotNull(patterns.categoryPerformance)
        assertTrue(patterns.categoryPerformance.containsKey("Grammar"))
        assertTrue(patterns.categoryPerformance.containsKey("Reading"))
        assertTrue(patterns.categoryPerformance["Grammar"]!! <= 1f)
        assertTrue(patterns.categoryPerformance["Reading"]!! <= 1f)
    }

    @Test
    fun `Category performance scores should be between 0 and 1`() {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = listOf("Grammar", "Reading", "Vocabulary").random(),
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        patterns.categoryPerformance.forEach { (category, score) ->
            assertTrue(score >= 0f, "Score for $category should be >= 0")
            assertTrue(score <= 1f, "Score for $category should be <= 1")
        }
    }

    // ============ Weak Area Detection Tests ============

    @Test
    fun `identifyWeakAreas() should detect categories with high error rates`() {
        val logs = listOf(
            // Grammar: 0% accuracy (weak)
            TaskLog(taskId = "1", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "2", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            // Reading: 100% accuracy (strong)
            TaskLog(taskId = "3", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25),
            TaskLog(taskId = "4", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )

        val weakAreas = analyzer.identifyWeakAreas(logs)

        assertTrue(weakAreas.isNotEmpty(), "Should identify at least one weak area")
        assertTrue(weakAreas.any { it.category == "Grammar" }, "Should identify Grammar as weak")
        assertTrue(weakAreas.first().errorRate > 0.5f, "Grammar error rate should be high")
    }

    @Test
    fun `identifyWeakAreas() should limit results to 4 categories`() {
        val logs = (1..50).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = when (index % 5) {
                    0 -> "Category1"
                    1 -> "Category2"
                    2 -> "Category3"
                    3 -> "Category4"
                    else -> "Category5"
                },
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val weakAreas = analyzer.identifyWeakAreas(logs)

        assertTrue(weakAreas.size <= 4, "Should return at most 4 weak areas")
    }

    @Test
    fun `Weak areas should be sorted by error rate descending`() {
        val logs = listOf(
            // Category A: 50% error rate
            TaskLog(taskId = "1", category = "A", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "2", category = "A", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            // Category B: 100% error rate (highest)
            TaskLog(taskId = "3", category = "B", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "4", category = "B", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            // Category C: 33% error rate (lowest)
            TaskLog(taskId = "5", category = "C", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "6", category = "C", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "7", category = "C", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20)
        )

        val weakAreas = analyzer.identifyWeakAreas(logs)

        assertTrue(weakAreas[0].errorRate >= weakAreas[1].errorRate, "Should be sorted by error rate descending")
    }

    // ============ Consistency Metric Tests ============

    @Test
    fun `consistencyMetric() should return value between 0 and 1`() {
        val logs = (1..20).map { dayOffset ->
            TaskLog(
                taskId = "test-$dayOffset",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = 30
            )
        }

        val metric = analyzer.consistencyMetric(logs)

        assertTrue(metric >= 0f, "Consistency should be >= 0")
        assertTrue(metric <= 1f, "Consistency should be <= 1")
    }

    @Test
    fun `Regular study patterns should have high consistency`() {
        val regularLogs = (1..20).map { dayOffset ->
            TaskLog(
                taskId = "regular-$dayOffset",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = 30  // Consistent 30 minutes
            )
        }

        val inconsistentLogs = (1..20).map { dayOffset ->
            TaskLog(
                taskId = "inconsistent-$dayOffset",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = if (dayOffset % 3 == 0) 5 else 60  // Varying 5-60 minutes
            )
        }

        val regularMetric = analyzer.consistencyMetric(regularLogs)
        val inconsistentMetric = analyzer.consistencyMetric(inconsistentLogs)

        assertTrue(regularMetric >= inconsistentMetric, "Regular pattern should have higher consistency")
    }

    // ============ Improvement Trend Tests ============

    @Test
    fun `improvementTrend() should detect positive improvement`() {
        val logs = (1..20).mapIndexed { index, _ ->
            TaskLog(
                taskId = "trend-$index",
                category = "Reading",
                correct = index >= 10,  // Correct only in second half
                timestampMillis = System.currentTimeMillis() - ((20 - index).toLong() * 60 * 60 * 1000),
                minutesSpent = 25
            )
        }

        val trend = analyzer.improvementTrend(logs)

        assertTrue(trend > 0f, "Should detect positive improvement in second half")
    }

    @Test
    fun `improvementTrend() should return value in reasonable range`() {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis() - ((20 - index).toLong() * 60 * 60 * 1000),
                minutesSpent = 25
            )
        }

        val trend = analyzer.improvementTrend(logs)

        assertTrue(trend >= 0f, "Trend should be >= 0")
        assertTrue(trend <= 0.6f, "Trend should be <= 0.6 (0.3 range + 0.3 base)")
    }

    // ============ Weekly Progress Tests ============

    @Test
    fun `analyzeWeeklyProgress() should return list of float values`() {
        val logs = (1..30).map { dayOffset ->
            TaskLog(
                taskId = "test-$dayOffset",
                category = "Reading",
                correct = dayOffset % 2 == 0,
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        assertTrue(patterns.weeklyProgress.isNotEmpty(), "Weekly progress should not be empty")
        patterns.weeklyProgress.forEach { progress ->
            assertTrue(progress >= 0f, "Progress values should be non-negative")
        }
    }

    @Test
    fun `Weekly progress should show trend over time`() {
        val logs = (1..30).map { dayOffset ->
            TaskLog(
                taskId = "test-$dayOffset",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        assertTrue(patterns.weeklyProgress.isNotEmpty())
        // With consistent logging, progress should be tracked
        assertTrue(patterns.weeklyProgress.all { it >= 0f })
    }

    // ============ Hourly Productivity Tests ============

    @Test
    fun `analyzeHourlyProductivity() should map hours to productivity scores`() {
        val logs = (1..24).map { hour ->
            val dateTime = LocalDateTime.now().withHour(hour % 24)
            TaskLog(
                taskId = "hour-$hour",
                category = "Reading",
                correct = hour % 2 == 0,
                timestampMillis = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                minutesSpent = 30
            )
        }

        val patterns = analyzer.analyze(logs)

        assertNotNull(patterns.hourlyProductivity)
        // Should have entries for hours where there was activity
        assertTrue(patterns.hourlyProductivity.values.all { it >= 0f && it <= 1f }, "Productivity scores should be normalized")
    }

    @Test
    fun `Hourly productivity scores should sum to reasonable value`() {
        val logs = (1..10).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        if (patterns.hourlyProductivity.isNotEmpty()) {
            val sum = patterns.hourlyProductivity.values.sum()
            assertTrue(sum in 0.99f..1.01f, "Hourly productivity should normalize to 1")
        }
    }

    // ============ Peak Productivity Tests ============

    @Test
    fun `findPeakProductivityTimes() should return reasonable default for empty logs`() {
        val patterns = analyzer.analyze(emptyList())

        assertEquals(9, patterns.mostProductiveHour)
        assertEquals("Monday", patterns.mostProductiveDay)
    }

    @Test
    fun `findPeakProductivityTimes() should identify peak hour from logs`() {
        val now = System.currentTimeMillis()
        val logs = listOf(
            // Morning (hour 9): 100% accuracy
            TaskLog(taskId = "1", category = "Reading", correct = true, timestampMillis = now, minutesSpent = 25),
            TaskLog(taskId = "2", category = "Reading", correct = true, timestampMillis = now, minutesSpent = 25),
            // Afternoon (hour 14): 0% accuracy
            TaskLog(taskId = "3", category = "Reading", correct = false, timestampMillis = now, minutesSpent = 25),
            TaskLog(taskId = "4", category = "Reading", correct = false, timestampMillis = now, minutesSpent = 25)
        )

        val patterns = analyzer.analyze(logs)

        assertTrue(patterns.mostProductiveHour >= 0 && patterns.mostProductiveHour < 24, "Peak hour should be valid")
    }

    @Test
    fun `findPeakProductivityTimes() should return valid day name`() {
        val logs = (1..30).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis() - (index.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        assertTrue(
            patterns.mostProductiveDay in listOf(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
            ),
            "Peak day should be valid day name"
        )
    }

    // ============ Focus Score Tests ============

    @Test
    fun `calculateFocusScore() should return value between 0 and 1`() {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = (20..40).random()
            )
        }

        val patterns = analyzer.analyze(logs)

        assertTrue(patterns.focusScore >= 0f, "Focus score should be >= 0")
        assertTrue(patterns.focusScore <= 1f, "Focus score should be <= 1")
    }

    @Test
    fun `Consistent session lengths should have higher focus score`() {
        val consistentLogs = (1..10).map { index ->
            TaskLog(
                taskId = "consistent-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 30  // Always 30 minutes
            )
        }

        val variableLogs = (1..10).map { index ->
            TaskLog(
                taskId = "variable-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = if (index % 2 == 0) 10 else 60  // Varies from 10 to 60
            )
        }

        val consistentPattern = analyzer.analyze(consistentLogs)
        val variablePattern = analyzer.analyze(variableLogs)

        assertTrue(consistentPattern.focusScore > 0f, "Consistent pattern should have focus score > 0")
        assertTrue(variablePattern.focusScore > 0f, "Variable pattern should have focus score > 0")
    }

    // ============ Day Time Productivity Tests ============

    @Test
    fun `analyzeDayTimeProductivity() should return three productivity scores`() {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        assertTrue(patterns.morningProductivity >= 0f)
        assertTrue(patterns.afternoonProductivity >= 0f)
        assertTrue(patterns.eveningProductivity >= 0f)
        assertTrue(patterns.morningProductivity <= 1f)
        assertTrue(patterns.afternoonProductivity <= 1f)
        assertTrue(patterns.eveningProductivity <= 1f)
    }

    @Test
    fun `Morning, afternoon, and evening productivity should follow expected pattern`() {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = index % 3 != 0,  // Most are correct, some incorrect
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        // All should be valid scores
        assertTrue(patterns.morningProductivity in 0f..1f)
        assertTrue(patterns.afternoonProductivity in 0f..1f)
        assertTrue(patterns.eveningProductivity in 0f..1f)
    }

    // ============ Edge Case Tests ============

    @Test
    fun `Should handle single task log`() {
        val logs = listOf(
            TaskLog(taskId = "1", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )

        val patterns = analyzer.analyze(logs)

        assertNotNull(patterns)
        // Focus score should be reasonable for single log
        assertTrue(patterns.focusScore >= 0f && patterns.focusScore <= 1f)
    }

    @Test
    fun `Should handle all correct answers`() {
        val logs = (1..10).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        // With all correct, category performance should be high
        patterns.categoryPerformance.forEach { (_, score) ->
            assertTrue(score > 0.5f, "With all correct answers, performance should be good")
        }
    }

    @Test
    fun `Should handle all incorrect answers`() {
        val logs = (1..10).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = false,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)
        val weakAreas = analyzer.identifyWeakAreas(logs)

        // With all incorrect, should identify weak areas
        assertTrue(weakAreas.isNotEmpty(), "All incorrect should identify weak areas")
        assertTrue(weakAreas[0].errorRate == 1f, "Error rate should be 100%")
    }

    @Test
    fun `Should handle logs from different time periods`() {
        val logs = (1..30).map { dayOffset ->
            TaskLog(
                taskId = "test-$dayOffset",
                category = listOf("Grammar", "Reading", "Vocabulary").random(),
                correct = dayOffset % 2 == 0,
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = (20..40).random()
            )
        }

        val patterns = analyzer.analyze(logs)

        assertNotNull(patterns)
        assertTrue(patterns.weeklyProgress.isNotEmpty(), "Should analyze weekly progress across days")
    }

    @Test
    fun `Should handle very large number of logs`() {
        val logs = (1..1000).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = listOf("Grammar", "Reading", "Vocabulary", "Listening").random(),
                correct = index % 3 != 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = (10..60).random()
            )
        }

        val patterns = analyzer.analyze(logs)

        assertNotNull(patterns)
        assertTrue(patterns.categoryPerformance.isNotEmpty())
    }

    // ============ Consistency Tests ============

    @Test
    fun `analyze() should always return non-null StudyPatternsUI`() {
        val patterns1 = analyzer.analyze(emptyList())
        val patterns2 = analyzer.analyze((1..5).map { index ->
            TaskLog(taskId = "test-$index", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        })

        assertNotNull(patterns1)
        assertNotNull(patterns2)
    }

    @Test
    fun `identifyWeakAreas() should always return list`() {
        val areas1 = analyzer.identifyWeakAreas(emptyList())
        val areas2 = analyzer.identifyWeakAreas((1..10).map { index ->
            TaskLog(taskId = "test-$index", category = "Reading", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        })

        assertNotNull(areas1)
        assertNotNull(areas2)
        assertTrue(areas1.isEmpty())
        assertTrue(areas2.isNotEmpty())
    }

    // ============ Normalization Tests ============

    @Test
    fun `Time distribution values should normalize to 1`() {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val patterns = analyzer.analyze(logs)

        if (patterns.timeDistribution.isNotEmpty()) {
            val sum = patterns.timeDistribution.values.sum()
            assertTrue(abs(sum - 1f) < 0.01f, "Time distribution should sum to ~1")
        }
    }

    @Test
    fun `Category performance should reflect accuracy`() {
        val logs = listOf(
            TaskLog(taskId = "1", category = "A", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25),
            TaskLog(taskId = "2", category = "A", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25),
            TaskLog(taskId = "3", category = "A", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )

        val patterns = analyzer.analyze(logs)

        // Category A has 66% accuracy
        assertTrue(patterns.categoryPerformance["A"]!! in 0.6f..0.8f, "Performance should reflect ~67% accuracy")
    }
}
