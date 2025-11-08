package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.ai.SmartScheduler
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for RecommendationGenerator
 *
 * Tests recommendation generation, prioritization, and integration with SmartScheduler and StudyPatternAnalyzer
 */
class RecommendationGeneratorTest {

    private lateinit var generator: RecommendationGenerator
    private lateinit var analyzer: StudyPatternAnalyzer
    private lateinit var scheduler: SmartScheduler

    @Before
    fun setup() {
        analyzer = StudyPatternAnalyzer()
        scheduler = SmartScheduler()
        generator = RecommendationGenerator(analyzer, scheduler)
    }

    // ============ Data Class Tests ============

    @Test
    fun `Recommendation should contain all required fields`() {
        val recommendation = Recommendation(
            id = "test-1",
            title = "Study at Morning",
            description = "You perform best in the morning",
            priority = RecommendationPriority.HIGH,
            actionText = "Schedule",
            category = "timing",
            message = "Peak productivity window detected",
            reasoning = "Based on historical data"
        )

        assertEquals("test-1", recommendation.id)
        assertEquals("Study at Morning", recommendation.title)
        assertEquals(RecommendationPriority.HIGH, recommendation.priority)
        assertEquals("timing", recommendation.category)
    }

    @Test
    fun `RecommendationPriority enum should have required values`() {
        val priorities = RecommendationPriority.values()

        assertTrue(priorities.contains(RecommendationPriority.HIGH))
        assertTrue(priorities.contains(RecommendationPriority.MEDIUM))
        assertTrue(priorities.contains(RecommendationPriority.LOW))
        assertEquals(3, priorities.size)
    }

    @Test
    fun `Recommendation should have default values for optional fields`() {
        val recommendation = Recommendation(
            id = "test-1",
            title = "Test",
            description = "Test description",
            priority = RecommendationPriority.MEDIUM
        )

        assertEquals("Apply", recommendation.actionText)
        assertEquals("general", recommendation.category)
        assertEquals("", recommendation.message)
        assertEquals("", recommendation.reasoning)
    }

    // ============ Empty Logs Tests ============

    @Test
    fun `generate() should return empty list for empty logs`() = runTest {
        val recommendations = generator.generate(emptyList(), UserProgress())

        assertEquals(0, recommendations.size)
    }

    @Test
    fun `generate() should return empty list when userProgress is null with empty logs`() = runTest {
        val recommendations = generator.generate(emptyList(), null)

        assertEquals(0, recommendations.size)
    }

    // ============ Smart Scheduler Integration Tests ============

    @Test
    fun `generate() should include smart scheduler suggestions`() = runTest {
        val logs = (1..30).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = listOf("Reading", "Grammar", "Vocabulary").random(),
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis() - (index.toLong() * 60 * 60 * 1000),
                minutesSpent = (20..45).random()
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // Should have recommendations from multiple sources
        assertTrue(recommendations.isNotEmpty() || logs.size < 10, "Should generate recommendations or have insufficient data")
    }

    @Test
    fun `Smart scheduler suggestions should be limited to 5`() = runTest {
        val logs = (1..50).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = (20..45).random()
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // Count recommendations that look like they came from smart scheduler (based on type patterns)
        val fromScheduler = recommendations.count { it.category == "timing" || it.category == "breaks" }
        assertTrue(fromScheduler <= 5 || fromScheduler == 0, "Smart scheduler should contribute at most 5 recommendations")
    }

    // ============ Performance Recommendations Tests ============

    @Test
    fun `generate() should include performance recommendation for low accuracy`() = runTest {
        val logs = (1..25).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = false,  // 0% accuracy
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // With 0% accuracy, should suggest performance improvement
        assertTrue(
            recommendations.any { it.category == "performance" || it.title.contains("Fundamental", ignoreCase = true) },
            "Should recommend performance improvement for low accuracy"
        )
    }

    @Test
    fun `Performance recommendation should not be generated for high accuracy`() = runTest {
        val logs = (1..25).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,  // 100% accuracy
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // With high accuracy, performance recommendation should not appear
        assertFalse(
            recommendations.any { it.id == "difficulty_reduction" },
            "Should not recommend difficulty reduction for high accuracy"
        )
    }

    @Test
    fun `Performance recommendation should have HIGH priority`() = runTest {
        val logs = (1..25).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = false,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        val performanceRec = recommendations.find { it.id == "difficulty_reduction" }
        if (performanceRec != null) {
            assertEquals(RecommendationPriority.HIGH, performanceRec.priority)
        }
    }

    // ============ Circadian Rhythm Recommendations Tests ============

    @Test
    fun `generate() should include circadian rhythm recommendation`() = runTest {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        assertTrue(
            recommendations.any { it.category == "timing" && it.title.contains("Peak", ignoreCase = true) },
            "Should include circadian rhythm recommendation"
        )
    }

    @Test
    fun `Circadian recommendation should mention productive hour and day`() = runTest {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        val circadianRec = recommendations.find { it.id == "circadian_alignment" }
        if (circadianRec != null) {
            assertTrue(circadianRec.description.contains(":00") || circadianRec.description.contains("hour"), "Should mention hour")
            assertTrue(
                listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    .any { circadianRec.description.contains(it) },
                "Should mention day of week"
            )
        }
    }

    @Test
    fun `Circadian recommendation should have MEDIUM priority`() = runTest {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        val circadianRec = recommendations.find { it.id == "circadian_alignment" }
        if (circadianRec != null) {
            assertEquals(RecommendationPriority.MEDIUM, circadianRec.priority)
        }
    }

    // ============ Weak Area Recommendations Tests ============

    @Test
    fun `generate() should include weak area recommendations`() = runTest {
        val logs = listOf(
            TaskLog(taskId = "1", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "2", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "3", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )

        val recommendations = generator.generate(logs, UserProgress())

        assertTrue(
            recommendations.any { it.id.startsWith("focus_") },
            "Should include weak area focus recommendations"
        )
    }

    @Test
    fun `Weak area recommendation should have HIGH priority`() = runTest {
        val logs = listOf(
            TaskLog(taskId = "1", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "2", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "3", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )

        val recommendations = generator.generate(logs, UserProgress())

        val weakAreaRecs = recommendations.filter { it.id.startsWith("focus_") }
        weakAreaRecs.forEach { rec ->
            assertEquals(RecommendationPriority.HIGH, rec.priority)
        }
    }

    @Test
    fun `Weak area recommendation should mention error rate`() = runTest {
        val logs = listOf(
            TaskLog(taskId = "1", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "2", category = "Grammar", correct = false, timestampMillis = System.currentTimeMillis(), minutesSpent = 20),
            TaskLog(taskId = "3", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )

        val recommendations = generator.generate(logs, UserProgress())

        val grammarRec = recommendations.find { it.id == "focus_grammar" }
        if (grammarRec != null) {
            assertTrue(grammarRec.description.contains("%"), "Should mention error rate percentage")
        }
    }

    @Test
    fun `No weak area recommendations should be generated for strong performance`() = runTest {
        val logs = (1..10).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,  // All correct
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // With all correct answers, should not have weak area recommendations
        assertTrue(
            recommendations.none { it.id.startsWith("focus_") },
            "Should not recommend weak areas when all correct"
        )
    }

    // ============ Consistency Recommendations Tests ============

    @Test
    fun `generate() should include consistency recommendation for inconsistent patterns`() = runTest {
        val logs = (1..20).map { dayOffset ->
            TaskLog(
                taskId = "test-$dayOffset",
                category = "Reading",
                correct = true,
                // Inconsistent gaps (2-3 days between sessions)
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * (if (dayOffset % 3 == 0) 72 else 24) * 60 * 60 * 1000),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // Inconsistent pattern should trigger consistency recommendation
        assertTrue(
            recommendations.any { it.id == "consistency_boost" },
            "Should recommend consistency boost for irregular patterns"
        )
    }

    @Test
    fun `Consistency recommendation should have MEDIUM priority`() = runTest {
        val logs = (1..20).map { dayOffset ->
            TaskLog(
                taskId = "test-$dayOffset",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * (if (dayOffset % 3 == 0) 72 else 24) * 60 * 60 * 1000),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        val consistencyRec = recommendations.find { it.id == "consistency_boost" }
        if (consistencyRec != null) {
            assertEquals(RecommendationPriority.MEDIUM, consistencyRec.priority)
        }
    }

    @Test
    fun `Consistency recommendation should not be generated for consistent patterns`() = runTest {
        val logs = (1..20).map { dayOffset ->
            TaskLog(
                taskId = "test-$dayOffset",
                category = "Reading",
                correct = true,
                // Consistent daily pattern
                timestampMillis = System.currentTimeMillis() - (dayOffset.toLong() * 24 * 60 * 60 * 1000),
                minutesSpent = 30
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // Consistent pattern should not trigger consistency recommendation
        assertFalse(
            recommendations.any { it.id == "consistency_boost" },
            "Should not recommend consistency boost for regular patterns"
        )
    }

    // ============ Recommendation Distinctness Tests ============

    @Test
    fun `generate() should return unique recommendations by ID`() = runTest {
        val logs = (1..50).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = listOf("Reading", "Grammar", "Vocabulary").random(),
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = (20..45).random()
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        val ids = recommendations.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "All recommendation IDs should be unique")
    }

    // ============ Priority Sorting Tests ============

    @Test
    fun `generate() should sort recommendations by priority (HIGH first)`() = runTest {
        val logs = (1..30).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = false,  // Low accuracy to trigger HIGH priority recommendations
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // Verify HIGH priority recommendations appear before MEDIUM
        if (recommendations.size >= 2) {
            var lastHighIndex = -1
            var firstMediumIndex = Int.MAX_VALUE
            recommendations.forEachIndexed { index, rec ->
                if (rec.priority == RecommendationPriority.HIGH) {
                    lastHighIndex = index
                }
                if (rec.priority == RecommendationPriority.MEDIUM) {
                    firstMediumIndex = minOf(firstMediumIndex, index)
                }
            }
            if (lastHighIndex >= 0 && firstMediumIndex < Int.MAX_VALUE) {
                assertTrue(lastHighIndex <= firstMediumIndex, "HIGH priority should appear before MEDIUM")
            }
        }
    }

    // ============ Category Tests ============

    @Test
    fun `Recommendations should have valid categories`() = runTest {
        val logs = (1..30).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = listOf("Reading", "Grammar", "Vocabulary").random(),
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = (20..45).random()
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        val validCategories = listOf(
            "performance", "timing", "improvement", "habits", "breaks", "review", "general"
        )

        recommendations.forEach { rec ->
            assertTrue(
                rec.category in validCategories,
                "Recommendation category '${rec.category}' should be valid"
            )
        }
    }

    // ============ Edge Case Tests ============

    @Test
    fun `Should handle single task log`() = runTest {
        val logs = listOf(
            TaskLog(taskId = "1", category = "Reading", correct = true, timestampMillis = System.currentTimeMillis(), minutesSpent = 25)
        )

        val recommendations = generator.generate(logs, UserProgress())

        assertNotNull(recommendations)
    }

    @Test
    fun `Should handle very large number of logs`() = runTest {
        val logs = (1..1000).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = listOf("Grammar", "Reading", "Vocabulary", "Listening").random(),
                correct = index % 3 != 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = (10..60).random()
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        assertNotNull(recommendations)
        assertTrue(recommendations.all { it.id.isNotEmpty() }, "All recommendations should have non-empty IDs")
    }

    @Test
    fun `Should handle null UserProgress gracefully`() = runTest {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, null)

        assertNotNull(recommendations)
        assertTrue(recommendations.isNotEmpty() || logs.isEmpty())
    }

    // ============ Content Validation Tests ============

    @Test
    fun `All recommendations should have non-empty title and description`() = runTest {
        val logs = (1..30).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = listOf("Reading", "Grammar", "Vocabulary").random(),
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = (20..45).random()
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        recommendations.forEach { rec ->
            assertTrue(rec.title.isNotEmpty(), "Recommendation title should not be empty")
            assertTrue(rec.description.isNotEmpty(), "Recommendation description should not be empty")
            assertTrue(rec.id.isNotEmpty(), "Recommendation ID should not be empty")
        }
    }

    @Test
    fun `Recommendations should have non-empty reasoning for context`() = runTest {
        val logs = (1..30).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = false,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // All recommendations should have reasoning
        recommendations.forEach { rec ->
            assertTrue(rec.reasoning.isNotEmpty(), "Each recommendation should have reasoning for transparency")
        }
    }

    // ============ Integration Tests ============

    @Test
    fun `generate() should integrate multiple recommendation sources`() = runTest {
        val logs = (1..40).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = when (index % 4) {
                    0 -> "Grammar"
                    1 -> "Reading"
                    2 -> "Vocabulary"
                    else -> "Listening"
                },
                correct = index % 3 != 0,
                timestampMillis = System.currentTimeMillis() - (index.toLong() * 12 * 60 * 60 * 1000),
                minutesSpent = (20..45).random()
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // Should have recommendations from multiple sources
        val categories = recommendations.map { it.category }.toSet()
        assertTrue(categories.size >= 2, "Should have recommendations from multiple sources/categories")
    }

    @Test
    fun `Recommendations should provide actionable guidance`() = runTest {
        val logs = (1..20).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = listOf("Reading", "Grammar").random(),
                correct = index % 2 == 0,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        val recommendations = generator.generate(logs, UserProgress())

        // Recommendations should have actionText or be inherently actionable via description
        recommendations.forEach { rec ->
            assertTrue(
                rec.actionText.isNotEmpty() || rec.description.contains("try", ignoreCase = true) || rec.description.contains("schedule", ignoreCase = true),
                "Recommendations should be actionable"
            )
        }
    }

    // ============ Coroutine Handling Tests ============

    @Test
    fun `generate() should execute suspend function without errors`() = runTest {
        val logs = (1..15).map { index ->
            TaskLog(
                taskId = "test-$index",
                category = "Reading",
                correct = true,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = 25
            )
        }

        try {
            val recommendations = generator.generate(logs, UserProgress())
            assertNotNull(recommendations)
            assertTrue(recommendations.isNotEmpty() || logs.isEmpty())
        } catch (e: Exception) {
            throw AssertionError("generate() should not throw exception: ${e.message}")
        }
    }
}
