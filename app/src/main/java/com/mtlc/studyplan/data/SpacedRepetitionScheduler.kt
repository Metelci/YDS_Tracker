package com.mtlc.studyplan.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import java.util.concurrent.TimeUnit

class SpacedRepetitionScheduler {

    private val baseIntervals = listOf(1, 3, 7, 14, 30, 60, 120) // days

    fun calculateNextReview(
        item: VocabularyItem,
        wasCorrect: Boolean,
        responseTime: Long
    ): Long {
        val currentInterval = getCurrentInterval(item)
        val newInterval = calculateNewInterval(currentInterval, wasCorrect, responseTime, item.masteryLevel)

        return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(newInterval.toLong())
    }

    fun updateMasteryLevel(
        wordId: String,
        wasCorrect: Boolean,
        difficulty: ReviewDifficulty
    ): Float {
        // This will be used in conjunction with VocabularyManager
        val baseChange = when (difficulty) {
            ReviewDifficulty.EASY -> if (wasCorrect) 0.2f else -0.05f
            ReviewDifficulty.MEDIUM -> if (wasCorrect) 0.15f else -0.1f
            ReviewDifficulty.HARD -> if (wasCorrect) 0.1f else -0.15f
        }

        return baseChange
    }

    fun getDueVocabulary(
        vocabularyItems: List<VocabularyItem>,
        maxCount: Int
    ): List<VocabularyItem> {
        val now = System.currentTimeMillis()

        return vocabularyItems.filter { item ->
            isDue(item, now)
        }.sortedWith(compareBy<VocabularyItem> { getPriority(it, now) }
            .thenBy { it.masteryLevel } // Lower mastery first
            .thenByDescending { getOverdueDays(it, now) } // More overdue first
        ).take(maxCount)
    }

    fun scheduleBatchReview(
        vocabularyItems: List<VocabularyItem>,
        availableTimeMinutes: Int
    ): ReviewSession {
        val estimatedTimePerWord = 2 // minutes per vocabulary review
        val maxWords = (availableTimeMinutes / estimatedTimePerWord).coerceAtLeast(1)

        val dueWords = getDueVocabulary(vocabularyItems, maxWords)
        val reviewSequence = optimizeReviewSequence(dueWords)

        return ReviewSession(
            vocabularyItems = reviewSequence,
            estimatedDuration = reviewSequence.size * estimatedTimePerWord,
            sessionType = determineSessionType(reviewSequence),
            difficultyProgression = createDifficultyProgression(reviewSequence)
        )
    }

    private fun getCurrentInterval(item: VocabularyItem): Int {
        if (item.lastEncountered == 0L) return baseIntervals[0]

        val daysSinceLastReview = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - item.lastEncountered
        ).toInt()

        // Find the interval that best matches the time since last review
        return baseIntervals.find { it >= daysSinceLastReview } ?: baseIntervals.last()
    }

    private fun calculateNewInterval(
        currentInterval: Int,
        wasCorrect: Boolean,
        responseTime: Long,
        masteryLevel: Float
    ): Int {
        val currentIndex = baseIntervals.indexOf(currentInterval).takeIf { it >= 0 } ?: 0

        val newIndex = when {
            wasCorrect -> {
                // Move to next interval, with bonus for high mastery
                val masteryBonus = if (masteryLevel > 0.8f) 1 else 0
                min(currentIndex + 1 + masteryBonus, baseIntervals.size - 1)
            }
            else -> {
                // Go back, but not to the very beginning unless mastery is very low
                val penalty = if (masteryLevel < 0.3f) 2 else 1
                max(currentIndex - penalty, 0)
            }
        }

        // Adjust based on response time
        val responseTimeAdjustment = calculateResponseTimeAdjustment(responseTime)
        val finalIndex = (newIndex + responseTimeAdjustment).coerceIn(0, baseIntervals.size - 1)

        return baseIntervals[finalIndex]
    }

    private fun calculateResponseTimeAdjustment(responseTime: Long): Int {
        val responseTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(responseTime)

        return when {
            responseTimeSeconds < 3 -> 1 // Very fast response, increase interval
            responseTimeSeconds < 8 -> 0 // Normal response time
            responseTimeSeconds < 15 -> -1 // Slow response, decrease interval
            else -> -2 // Very slow response, significantly decrease interval
        }
    }

    private fun isDue(item: VocabularyItem, currentTime: Long): Boolean {
        if (item.lastEncountered == 0L) return true // Never encountered

        val currentInterval = getCurrentInterval(item)
        val nextReviewTime = item.lastEncountered + TimeUnit.DAYS.toMillis(currentInterval.toLong())

        return currentTime >= nextReviewTime
    }

    private fun getPriority(item: VocabularyItem, currentTime: Long): Double {
        val overdueDays = getOverdueDays(item, currentTime)
        val masteryFactor = 1.0 - item.masteryLevel // Lower mastery = higher priority
        val errorFactor = min(item.errorCount * 0.1, 1.0) // More errors = higher priority

        // Combine factors: overdue time, mastery level, and error count
        return -(overdueDays * 0.5 + masteryFactor * 0.3 + errorFactor * 0.2)
    }

    private fun getOverdueDays(item: VocabularyItem, currentTime: Long): Long {
        if (item.lastEncountered == 0L) return Long.MAX_VALUE

        val currentInterval = getCurrentInterval(item)
        val nextReviewTime = item.lastEncountered + TimeUnit.DAYS.toMillis(currentInterval.toLong())

        return max(0, TimeUnit.MILLISECONDS.toDays(currentTime - nextReviewTime))
    }

    private fun optimizeReviewSequence(vocabularyItems: List<VocabularyItem>): List<VocabularyItem> {
        // Implement interleaving strategy: mix difficulties and categories
        val grouped = vocabularyItems.groupBy { it.difficulty }
        val result = mutableListOf<VocabularyItem>()

        val iterators = grouped.values.map { it.iterator() }

        // Interleave words from different difficulty levels
        while (iterators.any { it.hasNext() }) {
            iterators.forEach { iterator ->
                if (iterator.hasNext()) {
                    result.add(iterator.next())
                }
            }
        }

        return result
    }

    private fun determineSessionType(vocabularyItems: List<VocabularyItem>): String {
        val newWords = vocabularyItems.count { it.masteryLevel < 0.3f }
        val learningWords = vocabularyItems.count { it.masteryLevel in 0.3f..0.7f }
        val reviewWords = vocabularyItems.count { it.masteryLevel > 0.7f }

        return when {
            newWords > vocabularyItems.size * 0.6 -> "new_words"
            learningWords > vocabularyItems.size * 0.6 -> "learning"
            reviewWords > vocabularyItems.size * 0.6 -> "review"
            else -> "mixed"
        }
    }

    private fun createDifficultyProgression(vocabularyItems: List<VocabularyItem>): List<ReviewDifficulty> {
        // Start with easier words and gradually increase difficulty
        return vocabularyItems.mapIndexed { index, item ->
            val progressRatio = index.toFloat() / vocabularyItems.size.toFloat()

            when {
                progressRatio < 0.3f || item.masteryLevel < 0.3f -> ReviewDifficulty.EASY
                progressRatio < 0.7f || item.masteryLevel < 0.7f -> ReviewDifficulty.MEDIUM
                else -> ReviewDifficulty.HARD
            }
        }
    }

    fun getOptimalStudyLoad(
        userPerformance: List<TaskLog>,
        currentStreak: Int
    ): StudyLoadRecommendation {
        val recentPerformance = userPerformance.takeLast(10)
        val averageAccuracy = if (recentPerformance.isNotEmpty()) {
            recentPerformance.count { it.correct }.toFloat() / recentPerformance.size
        } else 0.5f

        val baseLoad = when {
            currentStreak > 14 && averageAccuracy > 0.8f -> 20 // High performer
            currentStreak > 7 && averageAccuracy > 0.7f -> 15 // Good performer
            averageAccuracy > 0.6f -> 12 // Average performer
            else -> 8 // Struggling, reduce load
        }

        return StudyLoadRecommendation(
            newWordsPerDay = (baseLoad * 0.3).toInt(),
            reviewWordsPerDay = (baseLoad * 0.7).toInt(),
            maxDailyWords = baseLoad,
            recommendedSessionLength = when {
                baseLoad > 15 -> 25 // minutes
                baseLoad > 10 -> 20
                else -> 15
            }
        )
    }
}

data class ReviewSession(
    val vocabularyItems: List<VocabularyItem>,
    val estimatedDuration: Int, // minutes
    val sessionType: String, // "new_words", "learning", "review", "mixed"
    val difficultyProgression: List<ReviewDifficulty>
)

data class StudyLoadRecommendation(
    val newWordsPerDay: Int,
    val reviewWordsPerDay: Int,
    val maxDailyWords: Int,
    val recommendedSessionLength: Int // minutes
)