package com.mtlc.studyplan.data

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class VocabularyManager(
    private val context: Context,
    private val progressRepository: ProgressRepository,
    taskLogs: List<TaskLog>? = null,
    vocabularyDatabase: List<VocabularyItem>? = null
) {
    private var vocabularyDatabase: List<VocabularyItem> = vocabularyDatabase ?: emptyList()
    private val taskLogs: List<TaskLog> = taskLogs ?: emptyList()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun initialize() {
        loadVocabularyDatabase()
    }

    private fun loadVocabularyDatabase() {
        try {
            val jsonString = context.assets.open("vocabulary_database.json").bufferedReader().use { it.readText() }
            vocabularyDatabase = json.decodeFromString<List<VocabularyItem>>(jsonString)
        } catch (e: Exception) {
            // For now, use empty list if file doesn't exist
            vocabularyDatabase = emptyList()
        }
    }

    suspend fun getPersonalizedVocabulary(maxCount: Int = 10): List<VocabularyItem> {
        if (vocabularyDatabase.isEmpty()) return emptyList()

        val currentTaskLogs = if (taskLogs.isNotEmpty()) taskLogs else progressRepository.taskLogsFlow.first()
        val userProgress = progressRepository.userProgressFlow.first()
        val weakAreas = analyzeWeakAreas(currentTaskLogs)
        val currentWeek = calculateCurrentWeek(userProgress)

        // Weight selection: 70% weak areas, 30% general progression
        val weakAreaCount = (maxCount * 0.7).toInt()
        val generalCount = maxCount - weakAreaCount

        val weakAreaVocab = getVocabularyForWeakAreas(weakAreas, weakAreaCount, currentWeek)
        val generalVocab = getGeneralProgression(generalCount, currentWeek, weakAreaVocab.map { it.word }.toSet())

        return (weakAreaVocab + generalVocab).take(maxCount)
    }

    suspend fun getDueVocabulary(maxCount: Int = 20): List<VocabularyItem> {
        val now = System.currentTimeMillis()
        val userProgress = progressRepository.userProgressFlow.first()

        return vocabularyDatabase.filter { vocab ->
            // Check if vocabulary is due for review based on spaced repetition
            val daysSinceLastEncounter = if (vocab.lastEncountered > 0) {
                TimeUnit.MILLISECONDS.toDays(now - vocab.lastEncountered)
            } else {
                Long.MAX_VALUE // Never encountered, always due
            }

            val intervalDays = calculateSpacedRepetitionInterval(vocab.masteryLevel, vocab.errorCount)
            daysSinceLastEncounter >= intervalDays
        }.sortedBy { vocab ->
            // Prioritize by combination of due time and mastery level
            val daysSinceLastEncounter = if (vocab.lastEncountered > 0) {
                TimeUnit.MILLISECONDS.toDays(now - vocab.lastEncountered)
            } else {
                Long.MAX_VALUE
            }
            val intervalDays = calculateSpacedRepetitionInterval(vocab.masteryLevel, vocab.errorCount)
            val overdueDays = daysSinceLastEncounter - intervalDays

            // Lower mastery and more overdue = higher priority (lower sort value)
            -(overdueDays * (1.0 - vocab.masteryLevel))
        }.take(maxCount)
    }

    suspend fun getVocabularyByCategory(category: VocabCategory, maxCount: Int = 15): List<VocabularyItem> {
        val currentWeek = calculateCurrentWeek(progressRepository.userProgressFlow.first())

        return vocabularyDatabase.filter { vocab ->
            vocab.category == category && vocab.weekIntroduced <= currentWeek
        }.sortedBy { vocab ->
            // Sort by week introduced and mastery level
            vocab.weekIntroduced * 100 + (vocab.masteryLevel * 100).toInt()
        }.take(maxCount)
    }

    suspend fun updateVocabularyProgress(
        word: String,
        wasCorrect: Boolean,
        responseTime: Long,
        difficulty: ReviewDifficulty
    ): VocabularyItem? {
        val vocabIndex = vocabularyDatabase.indexOfFirst { it.word == word }
        if (vocabIndex == -1) return null

        val currentVocab = vocabularyDatabase[vocabIndex]
        val newMasteryLevel = calculateNewMasteryLevel(currentVocab, wasCorrect, difficulty)
        val newErrorCount = if (wasCorrect) currentVocab.errorCount else currentVocab.errorCount + 1
        val newSuccessRate = calculateSuccessRate(currentVocab, wasCorrect)

        val updatedVocab = currentVocab.copy(
            masteryLevel = newMasteryLevel,
            lastEncountered = System.currentTimeMillis(),
            errorCount = newErrorCount,
            successRate = newSuccessRate
        )

        // Update in memory database
        vocabularyDatabase = vocabularyDatabase.toMutableList().apply {
            set(vocabIndex, updatedVocab)
        }

        return updatedVocab
    }

    private fun analyzeWeakAreas(taskLogs: List<TaskLog>): Map<String, Double> {
        val categoryStats = taskLogs.groupBy { it.category }.mapValues { (_, logs) ->
            val total = logs.size
            val incorrect = logs.count { !it.correct }
            if (total > 0) incorrect.toDouble() / total.toDouble() else 0.0
        }

        // Focus on categories with >30% error rate
        return categoryStats.filter { it.value > 0.3 }
    }

    private fun getVocabularyForWeakAreas(
        weakAreas: Map<String, Double>,
        count: Int,
        currentWeek: Int
    ): List<VocabularyItem> {
        if (weakAreas.isEmpty()) return emptyList()

        val vocabForWeakAreas = mutableListOf<VocabularyItem>()

        weakAreas.forEach { (category, errorRate) ->
            val targetCategory = mapTaskCategoryToVocabCategory(category)
            val categoryVocab = vocabularyDatabase.filter { vocab ->
                vocab.category == targetCategory &&
                vocab.weekIntroduced <= currentWeek &&
                vocab.masteryLevel < 0.8 // Focus on non-mastered words
            }.sortedBy { it.masteryLevel } // Prioritize lowest mastery

            val categoryCount = ceil(count * errorRate / weakAreas.values.sum()).toInt()
            vocabForWeakAreas.addAll(categoryVocab.take(categoryCount))
        }

        return vocabForWeakAreas.take(count)
    }

    private fun getGeneralProgression(
        count: Int,
        currentWeek: Int,
        excludeWords: Set<String>
    ): List<VocabularyItem> {
        return vocabularyDatabase.filter { vocab ->
            vocab.weekIntroduced <= currentWeek &&
            vocab.word !in excludeWords &&
            vocab.masteryLevel < 1.0 // Not fully mastered
        }.sortedWith(compareBy<VocabularyItem> { it.weekIntroduced }
            .thenBy { it.difficulty }
            .thenBy { it.masteryLevel })
        .take(count)
    }

    private fun calculateCurrentWeek(userProgress: UserProgress): Int {
        // Estimate current week based on completion progress
        // This is a simplified calculation - could be enhanced with actual start date tracking
        return min(max(userProgress.completedTasks.size / 10, 1), 30)
    }

    private fun calculateSpacedRepetitionInterval(masteryLevel: Float, errorCount: Int): Long {
        val baseIntervals = listOf(1L, 3L, 7L, 14L, 30L, 60L, 120L) // days

        val masteryIndex = (masteryLevel * (baseIntervals.size - 1)).toInt()
        val baseInterval = baseIntervals.getOrElse(masteryIndex) { baseIntervals.last() }

        // Reduce interval for words with high error count
        val errorPenalty = max(1.0 - (errorCount * 0.1), 0.3)

        return (baseInterval * errorPenalty).toLong()
    }

    private fun calculateNewMasteryLevel(
        vocab: VocabularyItem,
        wasCorrect: Boolean,
        difficulty: ReviewDifficulty
    ): Float {
        val baseChange = when (difficulty) {
            ReviewDifficulty.EASY -> if (wasCorrect) 0.15f else -0.05f
            ReviewDifficulty.MEDIUM -> if (wasCorrect) 0.10f else -0.10f
            ReviewDifficulty.HARD -> if (wasCorrect) 0.05f else -0.15f
        }

        // Apply diminishing returns for high mastery levels
        val adjustedChange = if (wasCorrect && vocab.masteryLevel > 0.7f) {
            baseChange * (1.0f - vocab.masteryLevel)
        } else {
            baseChange
        }

        return (vocab.masteryLevel + adjustedChange).coerceIn(0.0f, 1.0f)
    }

    private fun calculateSuccessRate(vocab: VocabularyItem, wasCorrect: Boolean): Float {
        // Simple moving average approach
        val weight = 0.1f // How much the current attempt affects the rate
        return if (wasCorrect) {
            vocab.successRate + weight * (1.0f - vocab.successRate)
        } else {
            vocab.successRate * (1.0f - weight)
        }
    }

    private fun mapTaskCategoryToVocabCategory(taskCategory: String): VocabCategory {
        return when (taskCategory.lowercase()) {
            "grammar", "gramer" -> VocabCategory.GRAMMAR_FOCUSED
            "reading", "okuma" -> VocabCategory.ACADEMIC
            "listening", "dinleme" -> VocabCategory.EVERYDAY
            "vocabulary", "vocab", "kelime" -> VocabCategory.EXAM_SPECIFIC
            "exam", "practice", "mock" -> VocabCategory.EXAM_SPECIFIC
            else -> VocabCategory.ACADEMIC
        }
    }

    fun getVocabularyStats(): VocabularyStats {
        val totalWords = vocabularyDatabase.size
        val masteredWords = vocabularyDatabase.count { it.masteryLevel >= 0.8f }
        val learningWords = vocabularyDatabase.count { it.masteryLevel in 0.3f..0.8f }
        val newWords = vocabularyDatabase.count { it.masteryLevel < 0.3f }

        val categoryDistribution = vocabularyDatabase.groupBy { it.category }
            .mapValues { it.value.size }

        return VocabularyStats(
            totalWords = totalWords,
            masteredWords = masteredWords,
            learningWords = learningWords,
            newWords = newWords,
            categoryDistribution = categoryDistribution,
            averageMastery = if (totalWords > 0) vocabularyDatabase.map { it.masteryLevel }.average().toFloat() else 0.0f
        )
    }
}

data class VocabularyStats(
    val totalWords: Int,
    val masteredWords: Int,
    val learningWords: Int,
    val newWords: Int,
    val categoryDistribution: Map<VocabCategory, Int>,
    val averageMastery: Float
)