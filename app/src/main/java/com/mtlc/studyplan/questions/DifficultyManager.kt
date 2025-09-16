package com.mtlc.studyplan.questions

import com.mtlc.studyplan.analytics.AnalyticsEngine
import com.mtlc.studyplan.data.TaskLog

class DifficultyManager(
    private val analyticsEngine: AnalyticsEngine
) {
    /**
     * Calculate optimal difficulty 1..5 for a skill based on recent accuracy trend
     */
    fun calculateOptimalDifficulty(
        category: SkillCategory,
        recentPerformance: List<TaskLog>
    ): Int {
        if (recentPerformance.isEmpty()) return 3
        val categoryName = category.name.lowercase()
        val acc = recentPerformance
            .filter { it.category.equals(categoryName, ignoreCase = true) || it.category.contains(categoryName, true) }
            .map { if (it.correct) 1f else 0f }
            .average()
            .toFloat()

        return when {
            acc >= 0.9f -> 5
            acc >= 0.8f -> 4
            acc >= 0.7f -> 3
            acc >= 0.6f -> 2
            else -> 1
        }
    }

    /**
     * Adjust the question template difficulty by nudging its difficulty toward the user level.
     * For now, return the same template but the generator will pick templates within +/- 1 range.
     */
    fun adjustQuestionComplexity(
        template: QuestionTemplate,
        userLevel: Int
    ): QuestionTemplate {
        // If template is far from user level, softly clamp by projecting into a nearby band.
        val newDiff = template.difficulty.coerceIn((userLevel - 1).coerceAtLeast(1), (userLevel + 1).coerceAtMost(5))
        return if (newDiff == template.difficulty) template else template.copy(difficulty = newDiff)
    }

    /**
     * Create a target difficulty progression (e.g., 2,3,3,4,4,5) based on a start.
     */
    fun buildProgression(startDifficulty: Int, targetCount: Int): List<Int> {
        val base = startDifficulty.coerceIn(1, 5)
        if (targetCount <= 0) return emptyList()
        val steps = mutableListOf<Int>()
        var cur = base
        repeat(targetCount) {
            steps.add(cur)
            // Increase gradually, but slower near top
            if (cur < 5 && (it % 2 == 1 || cur < 3)) cur++
        }
        return steps
    }
}

