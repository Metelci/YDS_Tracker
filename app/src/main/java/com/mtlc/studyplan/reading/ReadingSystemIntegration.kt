package com.mtlc.studyplan.reading

import android.content.Context
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.VocabularyItem
import com.mtlc.studyplan.data.VocabularyProgress
import com.mtlc.studyplan.data.ReviewDifficulty
import com.mtlc.studyplan.questions.VocabularyManager
import com.mtlc.studyplan.questions.QuestionGenerator
import com.mtlc.studyplan.questions.GeneratedQuestion
import com.mtlc.studyplan.questions.SkillCategory
import com.mtlc.studyplan.storage.room.StudyPlanDatabase
import com.mtlc.studyplan.storage.room.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.UUID

/**
 * Integration layer connecting Reading System with Vocabulary and Question Systems
 * Provides unified interface for reading-based learning activities
 */
class ReadingSystemIntegration(
    private val context: Context,
    private val progressRepository: ProgressRepository,
    private val vocabularyManager: VocabularyManager,
    private val questionGenerator: QuestionGenerator
) {
    private val database = StudyPlanDatabase.get(context)
    private val readingDao = database.readingDao()
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Create a complete reading session with vocabulary and comprehension integration
     */
    suspend fun createIntegratedReadingSession(
        contentId: String,
        availableTime: Int
    ): IntegratedReadingSession? {
        val curator = ContentCuratorFactory.create(context, progressRepository, vocabularyManager)
        val content = curator.recommendReading(availableTime) ?: return null
        
        // Get vocabulary items related to the content
        val vocabularyItems = getRelevantVocabulary(content)
        
        // Generate comprehension questions
        val comprehensionQuestions = generateComprehensionQuestions(content)
        
        // Create reading session
        val sessionId = UUID.randomUUID().toString()
        val session = ReadingSession(
            sessionId = sessionId,
            contentId = content.id,
            startTime = System.currentTimeMillis()
        )
        
        // Save session to database
        readingDao.insertSession(session.toEntity())
        
        return IntegratedReadingSession(
            session = session,
            content = content,
            vocabularyItems = vocabularyItems,
            comprehensionQuestions = comprehensionQuestions,
            preReadingVocabulary = selectPreReadingVocabulary(vocabularyItems),
            postReadingActivities = createPostReadingActivities(content, vocabularyItems)
        )
    }
    
    /**
     * Complete a reading session with vocabulary and comprehension tracking
     */
    suspend fun completeReadingSession(
        sessionId: String,
        comprehensionAnswers: Map<Int, String>,
        vocabularyEncounters: List<VocabularyEncounter>,
        readingSpeedData: ReadingSpeedData
    ): ReadingSessionResult {
        val session = readingDao.getSession(sessionId) ?: return ReadingSessionResult.SessionNotFound
        
        // Update session completion
        val completedSession = session.copy(
            endTime = System.currentTimeMillis(),
            comprehensionAnswersCsv = encodeComprehensionAnswers(comprehensionAnswers),
            vocabularyLookupsCsv = vocabularyEncounters.map { it.word }.joinToString(","),
            isCompleted = true
        )
        readingDao.insertSession(completedSession)
        
        // Save reading speed data
        readingDao.insertSpeedData(readingSpeedData.toEntity())
        
        // Process vocabulary encounters
        processVocabularyEncounters(vocabularyEncounters)
        
        // Calculate comprehension score and update progress
        val comprehensionScore = calculateComprehensionScore(
            comprehensionAnswers, 
            getCorrectAnswers(session.contentId)
        )
        
        // Update reading analytics
        val analytics = ReadingAnalytics(progressRepository)
        analytics.trackComprehensionAccuracy(
            session.contentId, 
            comprehensionScore.correct,
            comprehensionScore.total
        )
        
        // Create task log for reading activity
        val taskLog = TaskLog(
            taskId = "reading_${session.contentId}",
            timestampMillis = System.currentTimeMillis(),
            minutesSpent = ((completedSession.endTime ?: 0) - session.startTime).toInt() / 60000,
            correct = comprehensionScore.accuracy >= 0.7f,
            category = "Reading Comprehension",
            pointsEarned = calculateReadingPoints(comprehensionScore, readingSpeedData)
        )
        progressRepository.addTaskLog(taskLog)
        
        return ReadingSessionResult.Success(
            sessionSummary = createSessionSummary(completedSession, comprehensionScore, vocabularyEncounters),
            vocabularyProgress = vocabularyEncounters.map { it.progressUpdate },
            nextRecommendations = generateNextRecommendations(session.contentId, comprehensionScore)
        )
    }
    
    /**
     * Get vocabulary items relevant to reading content
     */
    private suspend fun getRelevantVocabulary(content: ReadingContent): List<VocabularyItem> {
        if (content.vocabularyFocus.isEmpty()) return emptyList()
        
        return vocabularyManager.findByWords(content.vocabularyFocus)
    }
    
    /**
     * Generate comprehension questions based on content
     */
    private suspend fun generateComprehensionQuestions(content: ReadingContent): List<GeneratedQuestion> {
        // Use existing comprehension questions if available
        if (!content.comprehensionQuestions.isNullOrEmpty()) {
            return content.comprehensionQuestions.mapIndexed { index, question ->
                GeneratedQuestion(
                    id = "${content.id}_comp_$index",
                    category = SkillCategory.READING,
                    prompt = question,
                    options = generateAnswerOptions(question, content),
                    correctIndex = 0, // First option is correct for simplicity
                    difficulty = content.difficulty.numericLevel,
                    explanation = "Based on the reading passage about ${content.title}",
                    sourceTemplateId = content.id
                )
            }
        }
        
        // Generate questions using existing question generator
        return questionGenerator.generateForCategory(
            SkillCategory.READING, 
            content.difficulty.numericLevel, 
            3 // Generate 3 questions
        ).map { question ->
            question.copy(sourceTemplateId = content.id)
        }
    }
    
    /**
     * Select vocabulary items for pre-reading introduction
     */
    private fun selectPreReadingVocabulary(vocabularyItems: List<VocabularyItem>): List<VocabularyItem> {
        return vocabularyItems
            .filter { it.masteryLevel < 0.7f } // Focus on less mastered words
            .sortedBy { it.difficulty }
            .take(5) // Limit to 5 words for pre-reading
    }
    
    /**
     * Create post-reading activities
     */
    private suspend fun createPostReadingActivities(
        content: ReadingContent,
        vocabularyItems: List<VocabularyItem>
    ): List<PostReadingActivity> {
        val activities = mutableListOf<PostReadingActivity>()
        
        // Vocabulary review activity
        if (vocabularyItems.isNotEmpty()) {
            activities.add(
                PostReadingActivity.VocabularyReview(
                    words = vocabularyItems.take(8),
                    activityType = VocabularyActivityType.DEFINITION_MATCHING
                )
            )
        }
        
        // Grammar focus activity based on content
        if (content.grammarPatterns.isNotEmpty()) {
            val grammarQuestions = generateGrammarQuestions(content.grammarPatterns)
            activities.add(
                PostReadingActivity.GrammarPractice(
                    questions = grammarQuestions,
                    focusAreas = content.grammarPatterns
                )
            )
        }
        
        // Discussion questions for deeper understanding
        activities.add(
            PostReadingActivity.DiscussionQuestions(
                questions = generateDiscussionQuestions(content),
                reflectionPrompts = generateReflectionPrompts(content)
            )
        )
        
        return activities
    }
    
    /**
     * Process vocabulary encounters and update progress
     */
    private suspend fun processVocabularyEncounters(encounters: List<VocabularyEncounter>) {
        encounters.forEach { encounter ->
            val difficulty = when (encounter.interactionType) {
                VocabularyInteractionType.LOOKED_UP -> ReviewDifficulty.HARD
                VocabularyInteractionType.GUESSED_CORRECTLY -> ReviewDifficulty.MEDIUM
                VocabularyInteractionType.ALREADY_KNOWN -> ReviewDifficulty.EASY
                VocabularyInteractionType.CONFUSED -> ReviewDifficulty.HARD
            }
            
            val wasCorrect = encounter.interactionType in setOf(
                VocabularyInteractionType.ALREADY_KNOWN,
                VocabularyInteractionType.GUESSED_CORRECTLY
            )
            
            progressRepository.updateVocabularyMastery(
                encounter.word,
                wasCorrect,
                encounter.responseTime,
                difficulty
            )
        }
    }
    
    /**
     * Calculate comprehension score
     */
    private fun calculateComprehensionScore(
        userAnswers: Map<Int, String>,
        correctAnswers: Map<Int, String>
    ): ComprehensionScore {
        val correct = userAnswers.count { (index, answer) ->
            correctAnswers[index]?.equals(answer, ignoreCase = true) == true
        }
        val total = correctAnswers.size
        
        return ComprehensionScore(
            correct = correct,
            total = total,
            accuracy = if (total > 0) correct.toFloat() / total else 0f
        )
    }
    
    /**
     * Get correct answers for comprehension questions
     */
    private fun getCorrectAnswers(contentId: String): Map<Int, String> {
        // In a real implementation, this would retrieve correct answers from the database
        // For now, return empty map as comprehension questions are open-ended
        return emptyMap()
    }
    
    /**
     * Calculate points earned from reading session
     */
    private fun calculateReadingPoints(
        comprehensionScore: ComprehensionScore,
        readingSpeedData: ReadingSpeedData
    ): Int {
        val basePoints = 20
        val comprehensionBonus = (comprehensionScore.accuracy * 15).toInt()
        val speedBonus = if (readingSpeedData.wordsPerMinute > 200) 5 else 0
        
        return basePoints + comprehensionBonus + speedBonus
    }
    
    /**
     * Create session summary
     */
    private fun createSessionSummary(
        session: ReadingSessionEntity,
        comprehensionScore: ComprehensionScore,
        vocabularyEncounters: List<VocabularyEncounter>
    ): ReadingSessionSummary {
        return ReadingSessionSummary(
            sessionId = session.sessionId,
            contentId = session.contentId,
            duration = (session.endTime ?: 0) - session.startTime,
            comprehensionAccuracy = comprehensionScore.accuracy,
            vocabularyWordsEncountered = vocabularyEncounters.size,
            newWordsLearned = vocabularyEncounters.count { 
                it.interactionType == VocabularyInteractionType.LOOKED_UP 
            },
            readingSpeedWPM = calculateWPMFromSession(session),
            pointsEarned = calculateReadingPoints(comprehensionScore, ReadingSpeedData(
                contentId = session.contentId,
                wordsPerMinute = calculateWPMFromSession(session),
                comprehensionAccuracy = comprehensionScore.accuracy,
                completionTime = (session.endTime ?: 0) - session.startTime
            ))
        )
    }
    
    /**
     * Generate next reading recommendations
     */
    private suspend fun generateNextRecommendations(
        completedContentId: String,
        comprehensionScore: ComprehensionScore
    ): List<ReadingRecommendation> {
        val curator = ContentCuratorFactory.create(context, progressRepository, vocabularyManager)
        
        // Adjust difficulty based on performance
        val targetDifficulty = if (comprehensionScore.accuracy >= 0.8f) {
            // High accuracy - can handle slightly harder content
            curator.calculateOptimalDifficulty()
        } else {
            // Lower accuracy - maintain or decrease difficulty
            ReadingLevel.values().find { 
                it.numericLevel <= curator.calculateOptimalDifficulty().numericLevel - 1 
            } ?: curator.calculateOptimalDifficulty()
        }
        
        // Get weak areas for targeted recommendations
        val taskLogs = progressRepository.taskLogsFlow.first()
        val weakAreas = identifyWeakAreas(taskLogs)
        
        val recommendations = mutableListOf<ReadingRecommendation>()
        
        // Add skill-focused recommendations
        weakAreas.forEach { skillCategory ->
            val skillContent = curator.getReadingByWeakArea(skillCategory).take(2)
            skillContent.forEach { content ->
                recommendations.add(
                    ReadingRecommendation(
                        content = content,
                        reason = "Recommended for ${skillCategory.name} improvement",
                        priority = RecommendationPriority.HIGH
                    )
                )
            }
        }
        
        // Add general progression recommendations
        val generalContent = curator.recommendReading(15) // 15-minute session
        generalContent?.let { content ->
            recommendations.add(
                ReadingRecommendation(
                    content = content,
                    reason = "Next in your learning progression",
                    priority = RecommendationPriority.MEDIUM
                )
            )
        }
        
        return recommendations.take(5) // Limit to 5 recommendations
    }
    
    // Helper methods
    
    private fun generateAnswerOptions(question: String, content: ReadingContent): List<String> {
        // Simple implementation - in practice, would use more sophisticated option generation
        return listOf("True", "False", "Not mentioned")
    }
    
    private suspend fun generateGrammarQuestions(grammarPatterns: List<String>): List<GeneratedQuestion> {
        return grammarPatterns.take(3).mapIndexed { index, pattern ->
            questionGenerator.generateForCategory(SkillCategory.GRAMMAR, 3, 1).firstOrNull()
                ?: GeneratedQuestion(
                    id = "grammar_$index",
                    category = SkillCategory.GRAMMAR,
                    prompt = "Identify the $pattern in the passage",
                    options = listOf("Option A", "Option B", "Option C"),
                    correctIndex = 0,
                    difficulty = 3,
                    explanation = "Focus on $pattern usage"
                )
        }
    }
    
    private fun generateDiscussionQuestions(content: ReadingContent): List<String> {
        val questions = mutableListOf<String>()
        
        when {
            content.topics.contains("technology") -> {
                questions.add("How might this technology impact your daily life?")
                questions.add("What are the potential benefits and drawbacks?")
            }
            content.topics.contains("environment") -> {
                questions.add("What actions can individuals take to address this issue?")
                questions.add("How might this affect future generations?")
            }
            content.topics.contains("culture") -> {
                questions.add("How does this compare to traditions in your culture?")
                questions.add("What can we learn from different cultural practices?")
            }
            else -> {
                questions.add("What is your opinion on the main topic?")
                questions.add("How does this relate to your own experiences?")
            }
        }
        
        return questions
    }
    
    private fun generateReflectionPrompts(content: ReadingContent): List<String> {
        return listOf(
            "What was the most interesting information you learned?",
            "Which vocabulary words were new to you?",
            "What questions do you still have about this topic?",
            "How can you apply this information in your life?"
        )
    }
    
    private fun identifyWeakAreas(taskLogs: List<TaskLog>): List<SkillCategory> {
        val categoryPerformance = SkillCategory.values().associateWith { category ->
            val categoryTasks = taskLogs.filter { task ->
                task.category.contains(category.name, ignoreCase = true)
            }
            
            if (categoryTasks.isNotEmpty()) {
                categoryTasks.count { it.correct }.toFloat() / categoryTasks.size
            } else 1f
        }
        
        return categoryPerformance.filter { it.value < 0.7f }.keys.toList()
    }
    
    private fun calculateWPMFromSession(session: ReadingSessionEntity): Int {
        val durationMinutes = ((session.endTime ?: 0) - session.startTime) / 60000
        // Estimate based on position and duration
        return if (durationMinutes > 0) {
            (session.currentPosition / 5 / durationMinutes).toInt() // 5 chars per word estimate
        } else 200 // Default WPM
    }
    
    private fun encodeComprehensionAnswers(answers: Map<Int, String>): String {
        return Json.encodeToString(answers)
    }

    /**
     * Get quick reading content (5-10 minute sessions focused on speed)
     */
    suspend fun getQuickReadingContent(): ReadingContent? {
        val curator = ContentCuratorFactory.create(context, progressRepository, vocabularyManager)
        return curator.recommendReading(7) // 7-minute target for quick reading
    }

    /**
     * Get vocabulary-focused reading content with word learning emphasis
     */
    suspend fun getVocabularyFocusedContent(): ReadingContent? {
        val curator = ContentCuratorFactory.create(context, progressRepository, vocabularyManager)
        val taskLogs = progressRepository.taskLogsFlow.first()
        val weakAreas = identifyWeakAreas(taskLogs)

        // Look for content that emphasizes vocabulary building
        return if (weakAreas.contains(SkillCategory.VOCAB)) {
            curator.getReadingByWeakArea(SkillCategory.VOCAB).firstOrNull()
        } else {
            curator.recommendReading(12) // 12-minute session with vocabulary focus
        }
    }

    /**
     * Get comprehension-focused content with built-in questions
     */
    suspend fun getComprehensionContent(): ReadingContent? {
        val curator = ContentCuratorFactory.create(context, progressRepository, vocabularyManager)
        val content = curator.recommendReading(15) // 15-minute session for comprehension work

        // Ensure content has comprehension questions, add them if missing
        return content?.let { baseContent ->
            if (baseContent.comprehensionQuestions.isNullOrEmpty()) {
                baseContent.copy(
                    comprehensionQuestions = generateBasicComprehensionQuestions(baseContent)
                )
            } else {
                baseContent
            }
        }
    }

    /**
     * Generate basic comprehension questions for content that doesn't have them
     */
    private fun generateBasicComprehensionQuestions(content: ReadingContent): List<String> {
        val questions = mutableListOf<String>()

        // Add topic-based questions
        if (content.topics.isNotEmpty()) {
            questions.add("What is the main topic discussed in this passage?")
            questions.add("According to the passage, what are the key points about ${content.topics.first()}?")
        }

        // Add general comprehension questions
        questions.add("What is the author's main argument or point of view?")
        questions.add("What evidence or examples does the author provide?")
        questions.add("What conclusion can you draw from this passage?")

        return questions.take(5) // Limit to 5 questions
    }
}

// Data classes for integration

data class IntegratedReadingSession(
    val session: ReadingSession,
    val content: ReadingContent,
    val vocabularyItems: List<VocabularyItem>,
    val comprehensionQuestions: List<GeneratedQuestion>,
    val preReadingVocabulary: List<VocabularyItem>,
    val postReadingActivities: List<PostReadingActivity>
)

sealed class ReadingSessionResult {
    object SessionNotFound : ReadingSessionResult()
    
    data class Success(
        val sessionSummary: ReadingSessionSummary,
        val vocabularyProgress: List<VocabularyProgress>,
        val nextRecommendations: List<ReadingRecommendation>
    ) : ReadingSessionResult()
}

data class ReadingSessionSummary(
    val sessionId: String,
    val contentId: String,
    val duration: Long,
    val comprehensionAccuracy: Float,
    val vocabularyWordsEncountered: Int,
    val newWordsLearned: Int,
    val readingSpeedWPM: Int,
    val pointsEarned: Int
)

data class ReadingRecommendation(
    val content: ReadingContent,
    val reason: String,
    val priority: RecommendationPriority
)

enum class RecommendationPriority {
    LOW, MEDIUM, HIGH
}

data class VocabularyEncounter(
    val word: String,
    val interactionType: VocabularyInteractionType,
    val responseTime: Long,
    val progressUpdate: VocabularyProgress
)

enum class VocabularyInteractionType {
    LOOKED_UP,
    GUESSED_CORRECTLY,
    ALREADY_KNOWN,
    CONFUSED
}

sealed class PostReadingActivity {
    data class VocabularyReview(
        val words: List<VocabularyItem>,
        val activityType: VocabularyActivityType
    ) : PostReadingActivity()
    
    data class GrammarPractice(
        val questions: List<GeneratedQuestion>,
        val focusAreas: List<String>
    ) : PostReadingActivity()
    
    data class DiscussionQuestions(
        val questions: List<String>,
        val reflectionPrompts: List<String>
    ) : PostReadingActivity()
}

enum class VocabularyActivityType {
    DEFINITION_MATCHING,
    FILL_IN_BLANKS,
    SYNONYM_ANTONYM,
    CONTEXT_USAGE
}

data class ComprehensionScore(
    val correct: Int,
    val total: Int,
    val accuracy: Float
)

// Extension functions for entity conversion

private fun ReadingSession.toEntity() = com.mtlc.studyplan.storage.room.ReadingSessionEntity(
    sessionId = sessionId,
    contentId = contentId,
    startTime = startTime,
    endTime = endTime,
    currentPosition = currentPosition,
    pauseTimestampsCsv = pauseTimestamps.joinToString(","),
    comprehensionAnswersCsv = "", // Will be updated when completed
    vocabularyLookupsCsv = vocabularyLookups.joinToString(","),
    notes = notes,
    isCompleted = isCompleted
)

private fun ReadingSpeedData.toEntity() = com.mtlc.studyplan.storage.room.ReadingSpeedDataEntity(
    contentId = contentId,
    wordsPerMinute = wordsPerMinute,
    comprehensionAccuracy = comprehensionAccuracy,
    completionTime = completionTime,
    pauseCount = pauseCount,
    rereadSectionsCsv = rereadSections.joinToString(","),
    timestamp = timestamp
)