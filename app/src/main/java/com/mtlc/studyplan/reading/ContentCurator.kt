package com.mtlc.studyplan.reading

import android.content.Context
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.questions.SkillCategory
import com.mtlc.studyplan.questions.VocabularyManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

/**
 * Content Curation System for StudyPlan Reading Materials
 * Provides intelligent reading recommendations based on progress and performance
 */
class ContentCurator(
    private val context: Context,
    private val readingDatabase: List<ReadingContent>,
    private val progressTracker: ProgressRepository,
    private val vocabularyManager: VocabularyManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var topicRotationState = TopicRotationState()
    
    /**
     * Recommend reading content based on available time and user context
     */
    suspend fun recommendReading(availableTime: Int): ReadingContent? {
        if (readingDatabase.isEmpty()) return null
        
        val userProgress = progressTracker.userProgressFlow.first()
        val taskLogs = progressTracker.taskLogsFlow.first()
        val currentWeek = calculateCurrentWeek(userProgress)
        val optimalDifficulty = calculateOptimalDifficulty()
        
        val context = ReadingRecommendationContext(
            availableTime = availableTime,
            currentWeek = currentWeek,
            weakAreas = identifyWeakAreas(taskLogs),
            vocabularyMastery = getVocabularyMasteryMap(),
            recentTopics = getRecentTopics(taskLogs),
            preferredDifficulty = optimalDifficulty,
            timeOfDay = getCurrentHour(),
            sessionType = determineOptimalSessionType(availableTime, taskLogs)
        )
        
        return selectOptimalContent(context)
    }
    
    /**
     * Get reading content focused on weak skill areas
     */
    suspend fun getReadingByWeakArea(category: SkillCategory): List<ReadingContent> {
        val userProgress = progressTracker.userProgressFlow.first()
        val currentWeek = calculateCurrentWeek(userProgress)
        
        val relevantTopics = when (category) {
            SkillCategory.GRAMMAR -> listOf("grammar", "language_structure", "syntax")
            SkillCategory.READING -> listOf("comprehension", "analysis", "academic")
            SkillCategory.LISTENING -> listOf("conversation", "everyday", "communication")
            SkillCategory.VOCAB -> listOf("vocabulary", "academic", "business")
        }
        
        return readingDatabase.filter { content ->
            content.weekAppropriate.contains(currentWeek) &&
            content.topics.any { it in relevantTopics } &&
            content.difficulty <= ReadingLevel.fromWeek(currentWeek + 2) // Allow slightly advanced content
        }.sortedBy { 
            // Prioritize by complexity and vocabulary overlap
            calculateContentRelevanceScore(it, category)
        }.take(10)
    }
    
    /**
     * Calculate optimal reading difficulty based on current performance
     */
    suspend fun calculateOptimalDifficulty(): ReadingLevel {
        val userProgress = progressTracker.userProgressFlow.first()
        val taskLogs = progressTracker.taskLogsFlow.first()
        val currentWeek = calculateCurrentWeek(userProgress)
        
        // Base difficulty on current week
        val baseDifficulty = ReadingLevel.fromWeek(currentWeek)
        
        // Adjust based on recent performance
        val recentPerformance = analyzeRecentReadingPerformance(taskLogs)
        val accuracyAdjustment = when {
            recentPerformance > 0.85f -> 1 // Increase difficulty
            recentPerformance < 0.65f -> -1 // Decrease difficulty
            else -> 0 // Maintain current level
        }
        
        val adjustedLevel = baseDifficulty.numericLevel + accuracyAdjustment
        return ReadingLevel.values().find { it.numericLevel == adjustedLevel.coerceIn(1, 6) }
            ?: baseDifficulty
    }
    
    /**
     * Get topic recommendation using rotation algorithm
     */
    suspend fun getTopicRotationRecommendation(): String {
        val taskLogs = progressTracker.taskLogsFlow.first()
        updateTopicRotationState(taskLogs)
        
        val availableTopics = getAllAvailableTopics()
        val currentTime = System.currentTimeMillis()
        
        // Calculate weights for all topics
        val topicWeights = availableTopics.associateWith { topic ->
            topicRotationState.getTopicWeight(topic, currentTime)
        }
        
        // Use weighted random selection
        return weightedTopicSelection(topicWeights)
    }
    
    /**
     * Select optimal content based on recommendation context
     */
    private suspend fun selectOptimalContent(context: ReadingRecommendationContext): ReadingContent? {
        val eligibleContent = readingDatabase.filter { content ->
            content.weekAppropriate.contains(context.currentWeek) &&
            content.estimatedTime <= context.availableTime &&
            (context.preferredDifficulty == null || content.difficulty == context.preferredDifficulty)
        }
        
        if (eligibleContent.isEmpty()) {
            // Fallback to any appropriate content
            return readingDatabase.filter { it.weekAppropriate.contains(context.currentWeek) }
                .minByOrNull { kotlin.math.abs(it.estimatedTime - context.availableTime) }
        }
        
        // Score each content piece
        val scoredContent = eligibleContent.map { content ->
            content to calculateContentScore(content, context)
        }.sortedByDescending { it.second }
        
        // Use weighted selection from top candidates
        val topCandidates = scoredContent.take(min(5, scoredContent.size))
        return weightedContentSelection(topCandidates)
    }
    
    /**
     * Calculate comprehensive content score based on multiple factors
     */
    private suspend fun calculateContentScore(content: ReadingContent, context: ReadingRecommendationContext): Float {
        var score = 0f
        
        // Time match bonus (0-1)
        val timeMatch = 1f - (kotlin.math.abs(content.estimatedTime - context.availableTime) / context.availableTime.toFloat())
        score += timeMatch * 0.25f
        
        // Difficulty appropriateness (0-1)
        val difficultyMatch = if (context.preferredDifficulty?.let { it == content.difficulty } == true) 1f else 0.5f
        score += difficultyMatch * 0.2f
        
        // Weak area focus bonus (0-0.3)
        val weakAreaBonus = context.weakAreas.sumOf { weakArea ->
            calculateContentRelevanceScore(content, weakArea)
        }.coerceAtMost(0.3f)
        score += weakAreaBonus
        
        // Topic variety bonus (0-0.15)
        val topicVarietyBonus = content.topics.sumOf { topic ->
            topicRotationState.getTopicWeight(topic)
        }.average().toFloat() * 0.15f
        score += topicVarietyBonus
        
        // Vocabulary alignment bonus (0-0.1)
        val vocabularyBonus = calculateVocabularyAlignmentScore(content, context.vocabularyMastery)
        score += vocabularyBonus * 0.1f
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Calculate how well content aligns with current vocabulary mastery
     */
    private suspend fun calculateVocabularyAlignmentScore(
        content: ReadingContent, 
        vocabularyMastery: Map<String, Float>
    ): Float {
        if (content.vocabularyFocus.isEmpty()) return 0.5f
        
        val knownWords = content.vocabularyFocus.count { word ->
            vocabularyMastery[word]?.let { it > 0.7f } ?: false
        }
        val unknownWords = content.vocabularyFocus.size - knownWords
        
        // Prefer content with some known words but also new challenges
        return when {
            unknownWords == 0 -> 0.3f // All known, less learning potential
            unknownWords > content.vocabularyFocus.size * 0.7f -> 0.2f // Too challenging
            else -> 1.0f // Good balance
        }
    }
    
    /**
     * Calculate content relevance to specific skill category
     */
    private fun calculateContentRelevanceScore(content: ReadingContent, category: SkillCategory): Float {
        val categoryKeywords = when (category) {
            SkillCategory.GRAMMAR -> setOf("grammar", "structure", "syntax", "tense", "clause")
            SkillCategory.READING -> setOf("comprehension", "analysis", "inference", "main_idea", "detail")
            SkillCategory.LISTENING -> setOf("conversation", "dialogue", "spoken", "audio", "communication")
            SkillCategory.VOCAB -> setOf("vocabulary", "word", "meaning", "definition", "synonym")
        }
        
        val topicMatches = content.topics.count { topic ->
            categoryKeywords.any { keyword -> topic.contains(keyword, ignoreCase = true) }
        }
        
        val grammarMatches = content.grammarPatterns.count { pattern ->
            categoryKeywords.any { keyword -> pattern.contains(keyword, ignoreCase = true) }
        }
        
        val totalMatches = topicMatches + grammarMatches
        val maxPossibleMatches = content.topics.size + content.grammarPatterns.size
        
        return if (maxPossibleMatches > 0) {
            totalMatches.toFloat() / maxPossibleMatches.toFloat()
        } else 0f
    }
    
    /**
     * Determine optimal session type based on available time and recent performance
     */
    private fun determineOptimalSessionType(availableTime: Int, taskLogs: List<TaskLog>): ReadingSessionType {
        return when {
            availableTime < 5 -> ReadingSessionType.SPEED_READING
            availableTime > 20 -> ReadingSessionType.COMPREHENSION_FOCUS
            hasVocabularyWeakness(taskLogs) -> ReadingSessionType.VOCABULARY_FOCUS
            else -> ReadingSessionType.GENERAL_PRACTICE
        }
    }
    
    /**
     * Analyze recent reading performance from task logs
     */
    private fun analyzeRecentReadingPerformance(taskLogs: List<TaskLog>): Float {
        val recentReadingTasks = taskLogs
            .filter { it.category.contains("reading", ignoreCase = true) }
            .sortedByDescending { it.timestampMillis }
            .take(10)
        
        return if (recentReadingTasks.isNotEmpty()) {
            recentReadingTasks.count { it.correct }.toFloat() / recentReadingTasks.size
        } else 0.75f // Default assumption
    }
    
    /**
     * Identify weak skill areas from task logs
     */
    private fun identifyWeakAreas(taskLogs: List<TaskLog>): List<SkillCategory> {
        val categoryPerformance = SkillCategory.values().associateWith { category ->
            val categoryTasks = taskLogs.filter { task ->
                task.category.contains(category.name, ignoreCase = true)
            }
            
            if (categoryTasks.isNotEmpty()) {
                categoryTasks.count { it.correct }.toFloat() / categoryTasks.size
            } else 1f
        }
        
        // Return categories with performance below 70%
        return categoryPerformance.filter { it.value < 0.7f }.keys.toList()
    }
    
    /**
     * Get vocabulary mastery map from VocabularyManager
     */
    private suspend fun getVocabularyMasteryMap(): Map<String, Float> {
        return try {
            val vocabulary = vocabularyManager.getAll()
            vocabulary.associate { it.word to it.masteryLevel }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Get recently covered topics from task logs
     */
    private fun getRecentTopics(taskLogs: List<TaskLog>): List<String> {
        val recentTasks = taskLogs
            .sortedByDescending { it.timestampMillis }
            .take(20)
        
        // Extract topics from task categories (simplified)
        return recentTasks.map { it.category.lowercase() }.distinct()
    }
    
    /**
     * Calculate current week from user progress
     */
    private fun calculateCurrentWeek(userProgress: UserProgress): Int {
        // Simple calculation based on completed tasks
        // In a real implementation, this might use actual start date
        return min(max(userProgress.completedTasks.size / 10, 1), 30)
    }
    
    /**
     * Update topic rotation state based on recent activity
     */
    private fun updateTopicRotationState(taskLogs: List<TaskLog>) {
        val currentTime = System.currentTimeMillis()
        val recentTopicCounts = taskLogs
            .filter { currentTime - it.timestampMillis < 7 * 24 * 60 * 60 * 1000 } // Last 7 days
            .groupBy { it.category.lowercase() }
            .mapValues { it.value.size }
        
        topicRotationState = topicRotationState.copy(
            recentTopics = recentTopicCounts.mapValues { currentTime },
            topicFrequency = recentTopicCounts,
            lastRotationUpdate = currentTime
        )
    }
    
    /**
     * Get all available topics from reading database
     */
    private fun getAllAvailableTopics(): List<String> {
        return readingDatabase.flatMap { it.topics }.distinct()
    }
    
    /**
     * Weighted topic selection using rotation weights
     */
    private fun weightedTopicSelection(topicWeights: Map<String, Float>): String {
        val totalWeight = topicWeights.values.sum()
        val random = Random.nextFloat() * totalWeight
        var currentWeight = 0f
        
        for ((topic, weight) in topicWeights) {
            currentWeight += weight
            if (random <= currentWeight) {
                return topic
            }
        }
        
        return topicWeights.keys.firstOrNull() ?: "general"
    }
    
    /**
     * Weighted content selection from scored candidates
     */
    private fun weightedContentSelection(scoredContent: List<Pair<ReadingContent, Float>>): ReadingContent? {
        if (scoredContent.isEmpty()) return null
        
        val totalScore = scoredContent.sumOf { it.second.toDouble() }.toFloat()
        val random = Random.nextFloat() * totalScore
        var currentScore = 0f
        
        for ((content, score) in scoredContent) {
            currentScore += score
            if (random <= currentScore) {
                return content
            }
        }
        
        return scoredContent.firstOrNull()?.first
    }
    
    /**
     * Check if there's a vocabulary weakness pattern
     */
    private fun hasVocabularyWeakness(taskLogs: List<TaskLog>): Boolean {
        val vocabTasks = taskLogs.filter { 
            it.category.contains("vocab", ignoreCase = true) ||
            it.category.contains("vocabulary", ignoreCase = true)
        }.take(10)
        
        return if (vocabTasks.isNotEmpty()) {
            val accuracy = vocabTasks.count { it.correct }.toFloat() / vocabTasks.size
            accuracy < 0.7f
        } else false
    }
    
    /**
     * Get current hour for time-based recommendations
     */
    private fun getCurrentHour(): Int {
        return java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }
}

/**
 * Factory class for creating ContentCurator with proper initialization
 */
object ContentCuratorFactory {
    suspend fun create(
        context: Context,
        progressRepository: ProgressRepository,
        vocabularyManager: VocabularyManager
    ): ContentCurator {
        val readingDatabase = loadReadingDatabase(context)
        return ContentCurator(context, readingDatabase, progressRepository, vocabularyManager)
    }
    
    private fun loadReadingDatabase(context: Context): List<ReadingContent> {
        return try {
            val jsonString = context.assets.open("reading_materials.json")
                .bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<List<ReadingContent>>(jsonString)
        } catch (e: Exception) {
            // For now, return empty list if file doesn't exist
            // In production, should have fallback content
            emptyList()
        }
    }
}