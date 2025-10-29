package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.ai.SmartScheduler
import com.mtlc.studyplan.ai.SmartSuggestion
import com.mtlc.studyplan.ai.SuggestionType
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class RecommendationGenerator(
    private val patternAnalyzer: StudyPatternAnalyzer,
    private val smartScheduler: SmartScheduler = SmartScheduler()
) {

    suspend fun generate(logs: List<TaskLog>, userProgress: UserProgress?): List<Recommendation> =
        withContext(Dispatchers.Default) {
            if (logs.isEmpty()) return@withContext emptyList<Recommendation>()

            val recommendations = mutableListOf<Recommendation>()

            // Smart scheduler suggestions
            recommendations += generateSmartSchedulerSuggestions(logs, userProgress)

            // Performance-based suggestions
            recommendations += performanceRecommendations(logs)

            // Circadian rhythm suggestions
            recommendations += circadianRecommendations(logs)

            // Weak area focus
            recommendations += weakAreaRecommendations(logs)

            // Consistency boost if needed
            recommendations += consistencyRecommendations(logs)

            recommendations.distinctBy { it.id }.sortedBy { it.priority.ordinal }
        }

    private suspend fun generateSmartSchedulerSuggestions(
        logs: List<TaskLog>,
        userProgress: UserProgress?
    ): List<Recommendation> = runCatching {
        val userPattern = smartScheduler.analyzeUserPatterns(logs, userProgress ?: UserProgress())
        smartScheduler.generateSuggestions(userPattern, logs, userProgress ?: UserProgress())
            .take(5)
            .map { convertSmartSuggestionToRecommendation(it) }
    }.getOrDefault(emptyList())

    private fun performanceRecommendations(logs: List<TaskLog>): List<Recommendation> {
        if (logs.isEmpty()) return emptyList()
        val recentAccuracy = logs.takeLast(20).map { if (it.correct) 1f else 0f }.average().toFloat()
        if (recentAccuracy >= 0.7f) return emptyList()
        val accuracyPercentage = (recentAccuracy * 100).roundToInt()
        return listOf(
            Recommendation(
                id = "difficulty_reduction",
                title = "Focus on Fundamentals",
                description = "Your recent accuracy is $accuracyPercentage%. Consider reviewing easier topics to rebuild confidence.",
                priority = RecommendationPriority.HIGH,
                category = "performance",
                reasoning = "Low accuracy pattern detected using performance trend analysis"
            )
        )
    }

    private fun circadianRecommendations(logs: List<TaskLog>): List<Recommendation> {
        if (logs.isEmpty()) return emptyList()
        val (hour, day) = patternAnalyzer.analyze(logs).let { it.mostProductiveHour to it.mostProductiveDay }
        return listOf(
            Recommendation(
                id = "circadian_alignment",
                title = "Study at Your Peak Time",
                description = "You perform best around $hour:00 on $day. Schedule important sessions during this window.",
                priority = RecommendationPriority.MEDIUM,
                category = "timing",
                reasoning = "Detected peak productivity window using circadian rhythm analysis"
            )
        )
    }

    private fun weakAreaRecommendations(logs: List<TaskLog>): List<Recommendation> {
        val weakAreas = patternAnalyzer.identifyWeakAreas(logs)
        if (weakAreas.isEmpty()) return emptyList()
        return weakAreas.map { area ->
            Recommendation(
                id = "focus_${area.category.lowercase()}",
                title = "Strengthen ${area.category}",
                description = "Error rate in ${area.category}: ${(area.errorRate * 100).roundToInt()}%. Suggested focused practice: ${area.recommendedFocus}",
                priority = RecommendationPriority.HIGH,
                category = "improvement",
                reasoning = "Identified through error frequency and difficulty correlation analysis"
            )
        }
    }

    private fun consistencyRecommendations(logs: List<TaskLog>): List<Recommendation> {
        val consistencyScore = patternAnalyzer.consistencyMetric(logs)
        if (consistencyScore >= 0.6f) return emptyList()
        return listOf(
            Recommendation(
                id = "consistency_boost",
                title = "Build Study Momentum",
                description = "Your consistency score is ${(consistencyScore * 100).roundToInt()}%. Try shorter, daily sessions to maintain engagement.",
                priority = RecommendationPriority.MEDIUM,
                category = "habits",
                reasoning = "Low consistency detected using temporal pattern analysis"
            )
        )
    }

    private fun convertSmartSuggestionToRecommendation(suggestion: SmartSuggestion): Recommendation {
        val priority = when (suggestion.priority) {
            1 -> RecommendationPriority.HIGH
            2 -> RecommendationPriority.MEDIUM
            else -> RecommendationPriority.LOW
        }
        val category = when (suggestion.type) {
            SuggestionType.OPTIMAL_TIME -> "timing"
            SuggestionType.BREAK_REMINDER -> "breaks"
            SuggestionType.WEAK_AREA_FOCUS -> "improvement"
            SuggestionType.CONSISTENCY_BOOST -> "habits"
            SuggestionType.DIFFICULTY_ADJUSTMENT -> "performance"
            SuggestionType.REVIEW_SESSION -> "review"
        }
        val confidence = (suggestion.confidence * 100).roundToInt()
        return Recommendation(
            id = suggestion.id,
            title = suggestion.title,
            description = suggestion.description,
            priority = priority,
            category = category,
            reasoning = "AI-powered recommendation based on study patterns and performance analysis (confidence: $confidence%)"
        )
    }
}
