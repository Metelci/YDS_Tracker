package com.mtlc.studyplan.reading

import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Reading Analytics System for Performance Tracking and Optimization
 * Implements adaptive content delivery based on learning patterns
 */
class ReadingAnalytics(
    private val progressRepository: ProgressRepository
) {
    
    /**
     * Analyze reading speed and comprehension from a reading session
     */
    fun analyzeReadingSpeed(
        content: ReadingContent,
        completionTime: Long
    ): ReadingSpeedData {
        val timeInMinutes = TimeUnit.MILLISECONDS.toMinutes(completionTime).toInt()
        val wordsPerMinute = if (timeInMinutes > 0) {
            (content.wordCount / timeInMinutes.toFloat()).roundToInt()
        } else {
            content.wordCount // Assume 1 minute if completion was very fast
        }
        
        return ReadingSpeedData(
            contentId = content.id,
            wordsPerMinute = wordsPerMinute,
            comprehensionAccuracy = 0.0f, // To be updated when comprehension is tested
            completionTime = completionTime,
            pauseCount = 0, // Will be tracked by reading UI
            rereadSections = emptyList(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Track comprehension accuracy for a specific content piece
     */
    suspend fun trackComprehensionAccuracy(
        contentId: String,
        questionsCorrect: Int,
        totalQuestions: Int
    ) {
        if (totalQuestions == 0) return
        
        val accuracy = questionsCorrect.toFloat() / totalQuestions.toFloat()
        
        // Create a task log entry for this comprehension test
        val taskLog = TaskLog(
            taskId = "reading_comprehension_$contentId",
            timestampMillis = System.currentTimeMillis(),
            minutesSpent = 5, // Estimate for comprehension questions
            correct = accuracy >= 0.7f, // Consider 70%+ as correct
            category = "Reading Comprehension",
            pointsEarned = (accuracy * 20).roundToInt() // Scale points based on accuracy
        )
        
        progressRepository.addTaskLog(taskLog)
    }
    
    /**
     * Identify optimal reading times based on historical performance
     */
    suspend fun identifyOptimalReadingTimes(): List<TimeSlot> {
        val taskLogs = progressRepository.taskLogsFlow.first()
        val readingLogs = taskLogs.filter { it.category.contains("reading", ignoreCase = true) }
        
        if (readingLogs.isEmpty()) {
            return getDefaultOptimalTimes()
        }
        
        // Group by hour of day and calculate average performance
        val performanceByHour = readingLogs.groupBy { log ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = log.timestampMillis
            }
            calendar.get(Calendar.HOUR_OF_DAY)
        }.mapValues { (_, logs) ->
            logs.count { it.correct }.toFloat() / logs.size
        }
        
        // Find hours with above-average performance
        val averagePerformance = performanceByHour.values.average().toFloat()
        val optimalHours = performanceByHour.filter { it.value > averagePerformance }
            .keys.sorted()
        
        return createTimeSlots(optimalHours)
    }
    
    /**
     * Calculate optimal reading endurance (maximum productive session length)
     */
    suspend fun calculateReadingEndurance(): Int {
        val taskLogs = progressRepository.taskLogsFlow.first()
        val readingLogs = taskLogs.filter { 
            it.category.contains("reading", ignoreCase = true) 
        }.sortedBy { it.timestampMillis }
        
        if (readingLogs.isEmpty()) return 15 // Default 15 minutes
        
        // Analyze performance degradation over time within sessions
        val sessionPerformance = analyzeSessionPerformance(readingLogs)
        
        // Find the point where performance significantly drops
        return findOptimalSessionLength(sessionPerformance)
    }
    
    /**
     * Generate comprehensive reading performance metrics
     */
    suspend fun generatePerformanceMetrics(userId: String): ReadingPerformanceMetrics {
        val taskLogs = progressRepository.taskLogsFlow.first()
        val readingLogs = taskLogs.filter { it.category.contains("reading", ignoreCase = true) }
        
        if (readingLogs.isEmpty()) {
            return getDefaultPerformanceMetrics(userId)
        }
        
        val averageWPM = calculateAverageWPM(readingLogs)
        val averageComprehension = readingLogs.count { it.correct }.toFloat() / readingLogs.size
        val preferredLength = calculatePreferredReadingLength(readingLogs)
        val strongTopics = identifyStrongTopics(readingLogs)
        val challengingTopics = identifyChallengingTopics(readingLogs)
        val optimalTimes = identifyOptimalReadingTimes()
        val endurance = calculateReadingEndurance()
        
        return ReadingPerformanceMetrics(
            userId = userId,
            averageWPM = averageWPM,
            averageComprehension = averageComprehension,
            preferredReadingLength = preferredLength,
            strongTopics = strongTopics,
            challengingTopics = challengingTopics,
            optimalReadingTimes = optimalTimes,
            readingEndurance = endurance
        )
    }
    
    /**
     * Analyze content effectiveness based on user engagement and learning outcomes
     */
    suspend fun analyzeContentEffectiveness(contentId: String): ContentEffectiveness {
        val taskLogs = progressRepository.taskLogsFlow.first()
        val contentLogs = taskLogs.filter { 
            it.taskId.contains(contentId) || it.taskId.contains("reading") 
        }
        
        if (contentLogs.isEmpty()) {
            return getDefaultContentEffectiveness(contentId)
        }
        
        val completionRate = contentLogs.count { it.correct }.toFloat() / contentLogs.size
        val engagementScore = calculateEngagementScore(contentLogs)
        val learningOutcome = calculateLearningOutcome(contentLogs)
        val comprehensionScore = contentLogs.filter { it.correct }.size.toFloat() / contentLogs.size
        
        return ContentEffectiveness(
            contentId = contentId,
            engagementScore = engagementScore,
            learningOutcome = learningOutcome,
            userRating = null, // Would come from user feedback
            completionRate = completionRate,
            averageComprehensionScore = comprehensionScore,
            vocabularyRetentionRate = estimateVocabularyRetention(contentId),
            lastEvaluated = System.currentTimeMillis()
        )
    }
    
    /**
     * Predict optimal content difficulty based on performance trends
     */
    suspend fun predictOptimalDifficulty(baseLevel: ReadingLevel): ReadingLevel {
        val taskLogs = progressRepository.taskLogsFlow.first()
        val recentReadingLogs = taskLogs
            .filter { it.category.contains("reading", ignoreCase = true) }
            .sortedByDescending { it.timestampMillis }
            .take(10)
        
        if (recentReadingLogs.isEmpty()) return baseLevel
        
        val recentAccuracy = recentReadingLogs.count { it.correct }.toFloat() / recentReadingLogs.size
        val trend = calculatePerformanceTrend(recentReadingLogs)
        
        return when {
            recentAccuracy > 0.9f && trend > 0.1f -> {
                // High accuracy and positive trend - increase difficulty
                ReadingLevel.values().find { it.numericLevel == baseLevel.numericLevel + 1 } ?: baseLevel
            }
            recentAccuracy < 0.6f || trend < -0.2f -> {
                // Low accuracy or negative trend - decrease difficulty
                ReadingLevel.values().find { it.numericLevel == baseLevel.numericLevel - 1 } ?: baseLevel
            }
            else -> baseLevel
        }
    }
    
    /**
     * Track reading session interruptions and patterns
     */
    fun trackReadingSession(session: ReadingSession): ReadingSpeedData {
        val effectiveWPM = if (session.effectiveReadingTime > 0) {
            // Estimate word count from position (simplified)
            val estimatedWords = session.currentPosition / 5 // Rough estimate: 5 chars per word
            val effectiveMinutes = TimeUnit.MILLISECONDS.toMinutes(session.effectiveReadingTime)
            if (effectiveMinutes > 0) (estimatedWords / effectiveMinutes).toInt() else 0
        } else 0
        
        return ReadingSpeedData(
            contentId = session.contentId,
            wordsPerMinute = effectiveWPM,
            comprehensionAccuracy = 0.0f, // Updated separately
            completionTime = session.duration,
            pauseCount = session.pauseTimestamps.size / 2,
            rereadSections = extractRereadSections(session),
            timestamp = session.startTime
        )
    }
    
    // Private helper methods
    
    private fun getDefaultOptimalTimes(): List<TimeSlot> {
        return listOf(
            TimeSlot(9, 11, listOf(ContentSourceType.ACADEMIC_TEXT), 15),
            TimeSlot(14, 16, listOf(ContentSourceType.NEWS_ARTICLE), 10),
            TimeSlot(19, 21, listOf(ContentSourceType.CULTURAL_TEXT), 12)
        )
    }
    
    private fun createTimeSlots(optimalHours: List<Int>): List<TimeSlot> {
        return optimalHours.windowed(2).map { (start, end) ->
            TimeSlot(
                startHour = start,
                endHour = end,
                preferredContentTypes = determinePreferredContentForTime(start),
                optimalReadingLength = determineOptimalLengthForTime(start)
            )
        }
    }
    
    private fun determinePreferredContentForTime(hour: Int): List<ContentSourceType> {
        return when (hour) {
            in 6..11 -> listOf(ContentSourceType.NEWS_ARTICLE, ContentSourceType.ACADEMIC_TEXT)
            in 12..17 -> listOf(ContentSourceType.BUSINESS_COMMUNICATION, ContentSourceType.EXAM_PASSAGE)
            else -> listOf(ContentSourceType.CULTURAL_TEXT, ContentSourceType.CURATED)
        }
    }
    
    private fun determineOptimalLengthForTime(hour: Int): Int {
        return when (hour) {
            in 6..11 -> 15 // Morning: longer focus
            in 12..14 -> 8  // Lunch: shorter sessions
            in 15..18 -> 12 // Afternoon: moderate
            else -> 10      // Evening: shorter
        }
    }
    
    private fun analyzeSessionPerformance(readingLogs: List<TaskLog>): Map<Int, Float> {
        // Group logs by session (simplified: group by day)
        val sessionGroups = readingLogs.groupBy { log ->
            TimeUnit.MILLISECONDS.toDays(log.timestampMillis)
        }
        
        return sessionGroups.mapKeys { (key, _) ->
            key.toInt()
        }.mapValues { (_, logs) ->
            logs.count { it.correct }.toFloat() / logs.size
        }
    }
    
    private fun findOptimalSessionLength(sessionPerformance: Map<Int, Float>): Int {
        // Simplified: look for consistent performance
        val consistentSessions = sessionPerformance.filter { it.value >= 0.7f }
        return if (consistentSessions.isNotEmpty()) {
            15 // Base optimal length
        } else {
            10 // Shorter if struggling
        }
    }
    
    private fun getDefaultPerformanceMetrics(userId: String): ReadingPerformanceMetrics {
        return ReadingPerformanceMetrics(
            userId = userId,
            averageWPM = 200,
            averageComprehension = 0.75f,
            preferredReadingLength = 12,
            strongTopics = listOf("technology", "news"),
            challengingTopics = listOf("academic", "literature"),
            optimalReadingTimes = getDefaultOptimalTimes(),
            readingEndurance = 15
        )
    }
    
    private fun calculateAverageWPM(readingLogs: List<TaskLog>): Int {
        // Simplified calculation based on time spent
        val averageMinutes = readingLogs.map { it.minutesSpent }.average()
        // Estimate: average reading speed based on time patterns
        return when {
            averageMinutes < 3 -> 300 // Fast reading
            averageMinutes > 15 -> 150 // Slow, careful reading
            else -> 200 // Standard reading speed
        }
    }
    
    private fun calculatePreferredReadingLength(readingLogs: List<TaskLog>): Int {
        val averageTime = readingLogs.map { it.minutesSpent }.average()
        return averageTime.roundToInt().coerceIn(5, 25)
    }
    
    private fun identifyStrongTopics(readingLogs: List<TaskLog>): List<String> {
        val topicPerformance = readingLogs.groupBy { it.category }
            .mapValues { (_, logs) ->
                logs.count { it.correct }.toFloat() / logs.size
            }
        
        return topicPerformance.filter { it.value > 0.8f }
            .keys.map { it.lowercase() }
    }
    
    private fun identifyChallengingTopics(readingLogs: List<TaskLog>): List<String> {
        val topicPerformance = readingLogs.groupBy { it.category }
            .mapValues { (_, logs) ->
                logs.count { it.correct }.toFloat() / logs.size
            }
        
        return topicPerformance.filter { it.value < 0.6f }
            .keys.map { it.lowercase() }
    }
    
    private fun getDefaultContentEffectiveness(contentId: String): ContentEffectiveness {
        return ContentEffectiveness(
            contentId = contentId,
            engagementScore = 0.7f,
            learningOutcome = 0.7f,
            userRating = null,
            completionRate = 0.8f,
            averageComprehensionScore = 0.75f,
            vocabularyRetentionRate = 0.6f
        )
    }
    
    private fun calculateEngagementScore(contentLogs: List<TaskLog>): Float {
        // Based on time spent and completion rates
        val averageTime = contentLogs.map { it.minutesSpent }.average()
        val completionRate = contentLogs.count { it.correct }.toFloat() / contentLogs.size
        
        // Engagement = combination of time investment and success
        return ((averageTime / 15.0f) * 0.3f + completionRate * 0.7f).toFloat().coerceIn(0f, 1f)
    }
    
    private fun calculateLearningOutcome(contentLogs: List<TaskLog>): Float {
        // Measure improvement over time for this content type
        if (contentLogs.size < 3) return 0.7f // Default for insufficient data
        
        val sortedLogs = contentLogs.sortedBy { it.timestampMillis }
        val firstHalf = sortedLogs.take(sortedLogs.size / 2)
        val secondHalf = sortedLogs.drop(sortedLogs.size / 2)
        
        val firstPerformance = firstHalf.count { it.correct }.toFloat() / firstHalf.size
        val secondPerformance = secondHalf.count { it.correct }.toFloat() / secondHalf.size
        
        return (secondPerformance - firstPerformance + 1f) / 2f // Normalize improvement to 0-1
    }
    
    private fun estimateVocabularyRetention(contentId: String): Float {
        // Simplified estimation - would integrate with vocabulary tracking in real implementation
        return 0.65f // Default estimate
    }
    
    private fun calculatePerformanceTrend(recentLogs: List<TaskLog>): Float {
        if (recentLogs.size < 5) return 0f
        
        val sortedLogs = recentLogs.sortedBy { it.timestampMillis }
        val firstHalf = sortedLogs.take(sortedLogs.size / 2)
        val secondHalf = sortedLogs.drop(sortedLogs.size / 2)
        
        val firstPerformance = firstHalf.count { it.correct }.toFloat() / firstHalf.size
        val secondPerformance = secondHalf.count { it.correct }.toFloat() / secondHalf.size
        
        return secondPerformance - firstPerformance
    }
    
    private fun extractRereadSections(session: ReadingSession): List<String> {
        // Simplified implementation - would track actual reread sections in UI
        return if (session.pauseTimestamps.size > 4) {
            listOf("middle_section") // Indicate potential reread areas
        } else emptyList()
    }
}

/**
 * Content complexity analyzer for reading difficulty assessment
 */
class ContentComplexityAnalyzer {
    
    /**
     * Calculate comprehensive complexity score for reading content
     */
    fun calculateComplexityScore(content: ReadingContent): Float {
        var complexity = 0f
        
        // Sentence length factor (0-0.3)
        val sentenceLengthFactor = (content.averageSentenceLength / 25f).coerceIn(0f, 0.3f)
        complexity += sentenceLengthFactor
        
        // Word count factor (0-0.2)
        val wordCountFactor = (content.wordCount / 1000f).coerceIn(0f, 0.2f)
        complexity += wordCountFactor
        
        // Vocabulary complexity (0-0.3)
        val vocabComplexity = analyzeVocabularyComplexity(content.vocabularyFocus)
        complexity += vocabComplexity
        
        // Grammar pattern complexity (0-0.2)
        val grammarComplexity = analyzeGrammarComplexity(content.grammarPatterns)
        complexity += grammarComplexity
        
        return complexity.coerceIn(0f, 1f)
    }
    
    /**
     * Analyze vocabulary difficulty based on word frequency and complexity
     */
    private fun analyzeVocabularyComplexity(vocabularyFocus: List<String>): Float {
        if (vocabularyFocus.isEmpty()) return 0.1f
        
        val complexWords = vocabularyFocus.count { word ->
            word.length > 8 || word.contains(Regex("[A-Z]")) // Long words or proper nouns
        }
        
        return (complexWords.toFloat() / vocabularyFocus.size * 0.3f).coerceIn(0f, 0.3f)
    }
    
    /**
     * Analyze grammar pattern complexity
     */
    private fun analyzeGrammarComplexity(grammarPatterns: List<String>): Float {
        if (grammarPatterns.isEmpty()) return 0.1f
        
        val complexPatterns = setOf(
            "subjunctive", "conditional_perfect", "passive_voice",
            "relative_clauses", "gerund_infinitive", "modal_perfects"
        )
        
        val complexCount = grammarPatterns.count { pattern ->
            complexPatterns.any { complex -> pattern.contains(complex, ignoreCase = true) }
        }
        
        return (complexCount.toFloat() / grammarPatterns.size * 0.2f).coerceIn(0f, 0.2f)
    }
}