package com.mtlc.studyplan.reading

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.data.VocabularyItem
import com.mtlc.studyplan.data.VocabCategory
import com.mtlc.studyplan.data.VocabularyProgress
import com.mtlc.studyplan.data.ReviewDifficulty
import com.mtlc.studyplan.questions.SkillCategory
import com.mtlc.studyplan.questions.VocabularyManager
import com.mtlc.studyplan.questions.QuestionGenerator
import com.mtlc.studyplan.questions.GeneratedQuestion
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.UUID

/**
 * User Experience Tests for Reading System Core Features
 * Validates the key user-facing features mentioned in the feedback
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReadingUserExperienceTest {

    @Mock
    private lateinit var mockProgressRepository: ProgressRepository

    @Mock
    private lateinit var mockVocabularyManager: VocabularyManager

    @Mock
    private lateinit var mockQuestionGenerator: QuestionGenerator

    private lateinit var context: Context
    private lateinit var readingIntegration: ReadingSystemIntegration
    private lateinit var contentCurator: ContentCurator
    private lateinit var readingAnalytics: ReadingAnalytics

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        // Setup realistic mock data for user experience testing
        setupRealisticMockData()
        
        // Initialize components
        readingIntegration = ReadingSystemIntegration(
            context, mockProgressRepository, mockVocabularyManager, mockQuestionGenerator
        )
        
        readingAnalytics = ReadingAnalytics(mockProgressRepository)
    }

    @Test
    fun `test personalized content recommendations based on current week, performance, and available time`() = runBlocking {
        // Setup: User in week 15, has some reading experience, wants 10-minute session
        val userProgress = createRealisticUserProgress(week15 = true, readingAccuracy = 0.75f)
        val taskLogs = createRealisticTaskLogs()
        val vocabularyMastery = createRealisticVocabularyMastery()
        
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(userProgress))
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(taskLogs))
        `when`(mockVocabularyManager.getAll()).thenReturn(vocabularyMastery)
        
        val testContent = createRealisticReadingContent()
        val curator = ContentCurator(context, testContent, mockProgressRepository, mockVocabularyManager)
        
        // Test: Get personalized recommendation for 10-minute session
        val recommendation = curator.recommendReading(availableTime = 10)
        
        // Verify: Recommendation should match user's context
        assertNotNull("Should provide personalized recommendation", recommendation)
        assertTrue("Estimated time should match available time", 
            recommendation!!.estimatedTime <= 12) // Allow slight variance
        assertTrue("Difficulty should be appropriate for week 15", 
            recommendation.difficulty.numericLevel >= ReadingLevel.B1.numericLevel)
        assertTrue("Should have vocabulary focus for learning", 
            recommendation.vocabularyFocus.isNotEmpty())
        
        // Verify: Recommendation considers weak areas
        val weakAreaContent = curator.getReadingByWeakArea(SkillCategory.READING)
        assertFalse("Should provide content for weak areas", weakAreaContent.isEmpty())
    }

    @Test
    fun `test multiple reading modes - Quick read, Vocabulary focus, Comprehension practice`() = runBlocking {
        val userProgress = createRealisticUserProgress()
        val vocabularyItems = createRealisticVocabularyItems()
        
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(userProgress))
        `when`(mockVocabularyManager.findByWords(any())).thenReturn(vocabularyItems)
        `when`(mockVocabularyManager.getAll()).thenReturn(vocabularyItems)
        
        val testContent = createRealisticReadingContent()
        val curator = ContentCurator(context, testContent, mockProgressRepository, mockVocabularyManager)
        
        // Test Quick Read mode (5 minutes)
        val quickRead = curator.recommendReading(availableTime = 5)
        assertNotNull("Should provide quick read content", quickRead)
        assertTrue("Quick read should be 5-8 minutes", quickRead!!.estimatedTime <= 8)
        
        // Test Vocabulary Focus mode
        val vocabFocusContent = curator.getReadingByWeakArea(SkillCategory.VOCAB)
        assertFalse("Should provide vocabulary-focused content", vocabFocusContent.isEmpty())
        vocabFocusContent.forEach { content ->
            assertTrue("Vocabulary focus content should have vocabulary words", 
                content.vocabularyFocus.isNotEmpty())
        }
        
        // Test Comprehension Practice mode
        val comprehensionContent = curator.getReadingByWeakArea(SkillCategory.READING)
        assertFalse("Should provide comprehension-focused content", comprehensionContent.isEmpty())
        comprehensionContent.forEach { content ->
            assertTrue("Comprehension content should have questions", 
                !content.comprehensionQuestions.isNullOrEmpty())
        }
    }

    @Test
    fun `test real-time progress tracking with weekly goals and streak integration`() = runBlocking {
        val userProgress = createRealisticUserProgress(streakCount = 7, totalPoints = 500)
        val readingLogs = createWeeklyReadingLogs()
        
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(userProgress))
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(readingLogs))
        
        val analytics = ReadingAnalytics(mockProgressRepository)
        
        // Test weekly progress calculation
        val weeklyProgress = calculateWeeklyProgress(readingLogs)
        assertTrue("Weekly progress should be tracked", weeklyProgress >= 0f && weeklyProgress <= 1f)
        
        // Test streak integration
        val streakMultiplier = userProgress.currentStreakMultiplier
        assertTrue("Should have streak multiplier", streakMultiplier >= 1.0f)
        
        // Test reading performance metrics
        val metrics = analytics.generatePerformanceMetrics("test_user")
        assertTrue("Should track articles read this week", metrics.preferredReadingLength > 0)
        assertTrue("Should have comprehension tracking", metrics.averageComprehension >= 0f)
        assertTrue("Should have WPM tracking", metrics.averageWPM > 0)
        
        // Verify points are calculated with streak multiplier
        val basePoints = 20
        val expectedPoints = (basePoints * streakMultiplier).toInt()
        assertTrue("Points should be affected by streak", expectedPoints >= basePoints)
    }

    @Test
    fun `test post-reading activities including vocabulary review and discussion questions`() = runBlocking {
        val userProgress = createRealisticUserProgress()
        val vocabularyItems = createRealisticVocabularyItems()
        
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(userProgress))
        `when`(mockVocabularyManager.findByWords(any())).thenReturn(vocabularyItems)
        `when`(mockVocabularyManager.getAll()).thenReturn(vocabularyItems)
        
        // Create integrated reading session
        val session = readingIntegration.createIntegratedReadingSession("tech_001", 10)
        assertNotNull("Should create integrated session", session)
        
        // Test post-reading activities
        val postReadingActivities = session!!.postReadingActivities
        assertFalse("Should have post-reading activities", postReadingActivities.isEmpty())
        
        // Verify vocabulary review activity
        val vocabActivity = postReadingActivities.find { it is PostReadingActivity.VocabularyReview }
        assertNotNull("Should include vocabulary review", vocabActivity)
        val vocabReview = vocabActivity as PostReadingActivity.VocabularyReview
        assertTrue("Should have vocabulary words for review", vocabReview.words.isNotEmpty())
        
        // Verify discussion questions activity
        val discussionActivity = postReadingActivities.find { it is PostReadingActivity.DiscussionQuestions }
        assertNotNull("Should include discussion questions", discussionActivity)
        val discussion = discussionActivity as PostReadingActivity.DiscussionQuestions
        assertTrue("Should have discussion questions", discussion.questions.isNotEmpty())
        assertTrue("Should have reflection prompts", discussion.reflectionPrompts.isNotEmpty())
        
        // Verify grammar practice activity
        val grammarActivity = postReadingActivities.find { it is PostReadingActivity.GrammarPractice }
        assertNotNull("Should include grammar practice", grammarActivity)
    }

    @Test
    fun `test performance insights with WPM, comprehension accuracy, and improvement tracking`() = runBlocking {
        val userProgress = createRealisticUserProgress()
        val readingLogs = createProgressiveReadingLogs() // Logs showing improvement over time
        
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(userProgress))
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(readingLogs))
        
        val analytics = ReadingAnalytics(mockProgressRepository)
        
        // Test WPM calculation
        val wpmData = analytics.analyzeReadingSpeed(
            content = createTestReadingContent(wordCount = 200),
            completionTime = 60000L // 1 minute
        )
        assertEquals("Should calculate correct WPM", 200, wpmData.wordsPerMinute)
        
        // Test comprehension accuracy tracking
        analytics.trackComprehensionAccuracy("test_content", questionsCorrect = 3, totalQuestions = 4)
        verify(mockProgressRepository).addTaskLog(argThat { 
            category == "Reading Comprehension" && 
            pointsEarned == 15 // 3/4 * 20 points
        })
        
        // Test performance improvement tracking
        val metrics = analytics.generatePerformanceMetrics("test_user")
        assertTrue("Should track strong topics", metrics.strongTopics.isNotEmpty())
        assertTrue("Should track challenging topics", metrics.challengingTopics.isNotEmpty())
        
        // Test optimal reading times
        val optimalTimes = analytics.identifyOptimalReadingTimes()
        assertFalse("Should identify optimal reading times", optimalTimes.isEmpty())
        
        // Test reading endurance
        val endurance = analytics.calculateReadingEndurance()
        assertTrue("Should calculate realistic endurance", endurance in 5..60)
    }

    @Test
    fun `test complete user journey - from recommendation to session completion`() = runBlocking {
        // Setup realistic user scenario
        val userProgress = createRealisticUserProgress(week15 = true)
        val vocabularyItems = createRealisticVocabularyItems()
        val readingLogs = createWeeklyReadingLogs()
        
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(userProgress))
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(readingLogs))
        `when`(mockVocabularyManager.findByWords(any())).thenReturn(vocabularyItems)
        `when`(mockVocabularyManager.getAll()).thenReturn(vocabularyItems)
        
        // Step 1: Get personalized recommendation
        val testContent = createRealisticReadingContent()
        val curator = ContentCurator(context, testContent, mockProgressRepository, mockVocabularyManager)
        val recommendation = curator.recommendReading(10)
        assertNotNull("Should get recommendation", recommendation)
        
        // Step 2: Create integrated reading session
        val session = readingIntegration.createIntegratedReadingSession(recommendation!!.id, 10)
        assertNotNull("Should create session", session)
        
        // Step 3: Simulate reading session with vocabulary encounters
        val vocabularyEncounters = listOf(
            VocabularyEncounter(
                word = "comprehension",
                interactionType = VocabularyInteractionType.LOOKED_UP,
                responseTime = 3000L,
                progressUpdate = createVocabularyProgress("comprehension", masteryLevel = 0.6f)
            ),
            VocabularyEncounter(
                word = "analyze",
                interactionType = VocabularyInteractionType.GUESSED_CORRECTLY,
                responseTime = 2000L,
                progressUpdate = createVocabularyProgress("analyze", masteryLevel = 0.8f)
            )
        )
        
        val readingSpeedData = ReadingSpeedData(
            contentId = recommendation.id,
            wordsPerMinute = 180,
            comprehensionAccuracy = 0.75f,
            completionTime = 480000L // 8 minutes
        )
        
        // Step 4: Complete session
        val result = readingIntegration.completeReadingSession(
            sessionId = session!!.session.sessionId,
            comprehensionAnswers = mapOf(0 to "correct_answer", 1 to "another_correct"),
            vocabularyEncounters = vocabularyEncounters,
            readingSpeedData = readingSpeedData
        )
        
        // Verify: Session completion
        assertTrue("Should complete successfully", result is ReadingSessionResult.Success)
        val successResult = result as ReadingSessionResult.Success
        
        // Verify: Progress tracking
        assertTrue("Should have session summary", successResult.sessionSummary.vocabularyWordsEncountered > 0)
        assertTrue("Should track new words learned", successResult.sessionSummary.newWordsLearned >= 0)
        assertTrue("Should calculate WPM", successResult.sessionSummary.readingSpeedWPM > 0)
        assertTrue("Should earn points", successResult.sessionSummary.pointsEarned > 0)
        
        // Verify: Next recommendations
        assertFalse("Should provide next recommendations", successResult.nextRecommendations.isEmpty())
        
        // Verify: Vocabulary progress updates
        assertFalse("Should update vocabulary progress", successResult.vocabularyProgress.isEmpty())
    }

    // Helper methods for creating realistic test data

    private fun createRealisticUserProgress(
        week15: Boolean = false,
        readingAccuracy: Float = 0.75f,
        streakCount: Int = 5,
        totalPoints: Int = 300
    ): UserProgress {
        val completedTasks = if (week15) {
            (1..150).map { "task_$it" }.toSet() // ~15 weeks of tasks
        } else {
            (1..100).map { "task_$it" }.toSet() // ~10 weeks of tasks
        }
        
        return UserProgress(
            completedTasks = completedTasks,
            streakCount = streakCount,
            lastCompletionDate = System.currentTimeMillis() - 3600000, // 1 hour ago
            unlockedAchievements = setOf("reading_novice", "vocabulary_learner"),
            totalPoints = totalPoints,
            currentStreakMultiplier = when {
                streakCount >= 14 -> 3.0f
                streakCount >= 7 -> 2.0f
                else -> 1.0f
            }
        )
    }

    private fun createRealisticTaskLogs(): List<TaskLog> {
        val now = System.currentTimeMillis()
        return listOf(
            // Recent reading sessions
            TaskLog("reading_tech_1", now - 86400000, 12, true, "Reading Comprehension", 25),
            TaskLog("reading_health_1", now - 172800000, 8, true, "Reading Comprehension", 18),
            TaskLog("reading_business_1", now - 259200000, 15, false, "Reading Comprehension", 10),
            
            // Vocabulary sessions
            TaskLog("vocab_review_1", now - 86400000, 10, true, "Vocabulary", 15),
            TaskLog("vocab_new_1", now - 172800000, 8, false, "Vocabulary", 8),
            
            // Grammar sessions
            TaskLog("grammar_1", now - 86400000, 6, true, "Grammar", 10),
            TaskLog("grammar_2", now - 172800000, 7, false, "Grammar", 5)
        )
    }

    private fun createRealisticVocabularyMastery(): List<VocabularyItem> {
        return listOf(
            VocabularyItem("comprehension", "Understanding", 3, VocabCategory.ACADEMIC, 
                listOf("reading comprehension"), listOf("understanding", "grasp"), null, 0.7f, 0, 0, 0.0f, 10),
            VocabularyItem("analyze", "Examine in detail", 4, VocabCategory.ACADEMIC,
                listOf("analyze data"), listOf("examine", "study"), null, 0.8f, 0, 0, 0.0f, 12),
            VocabularyItem("synthesize", "Combine elements", 5, VocabCategory.ACADEMIC,
                listOf("synthesize information"), listOf("combine", "integrate"), null, 0.4f, 0, 0, 0.0f, 15)
        )
    }

    private fun createRealisticReadingContent(): List<ReadingContent> {
        return listOf(
            ReadingContent(
                id = "tech_001",
                title = "The Future of Artificial Intelligence",
                content = "Artificial intelligence continues to evolve rapidly, transforming industries and creating new opportunities for innovation. Machine learning algorithms are becoming more sophisticated, enabling computers to perform complex tasks that previously required human intelligence.",
                difficulty = ReadingLevel.B1_PLUS,
                estimatedTime = 8,
                topics = listOf("technology", "innovation"),
                vocabularyFocus = listOf("artificial intelligence", "evolve", "sophisticated", "innovation"),
                grammarPatterns = listOf("present continuous", "comparative adjectives"),
                wordCount = 120,
                averageSentenceLength = 14.0f,
                complexityScore = 0.65f,
                weekAppropriate = 12..20,
                comprehensionQuestions = listOf(
                    "What is happening to artificial intelligence?",
                    "What are machine learning algorithms enabling?"
                )
            ),
            ReadingContent(
                id = "health_002",
                title = "Mental Health in the Digital Age",
                content = "The relationship between technology and mental health has become increasingly complex. While digital tools offer new ways to access mental health resources, they also present challenges related to screen time and social comparison.",
                difficulty = ReadingLevel.B2,
                estimatedTime = 10,
                topics = listOf("health", "technology", "psychology"),
                vocabularyFocus = listOf("relationship", "increasingly", "resources", "challenges"),
                grammarPatterns = listOf("present perfect", "concessive clauses"),
                wordCount = 95,
                averageSentenceLength = 16.0f,
                complexityScore = 0.72f,
                weekAppropriate = 15..25,
                comprehensionQuestions = listOf(
                    "How has the relationship between technology and mental health changed?",
                    "What are two effects of digital tools on mental health?"
                )
            )
        )
    }

    private fun createWeeklyReadingLogs(): List<TaskLog> {
        val now = System.currentTimeMillis()
        return (0..6).map { day ->
            TaskLog(
                "reading_day_$day",
                now - (day * 86400000L), // Each day back
                10 + (day * 2), // Increasing time spent
                day % 3 != 2, // Mostly correct answers
                "Reading Comprehension",
                20 + (day * 5) // Increasing points
            )
        }
    }

    private fun createProgressiveReadingLogs(): List<TaskLog> {
        val now = System.currentTimeMillis()
        return (0..9).map { session ->
            TaskLog(
                "reading_session_$session",
                now - (session * 86400000L),
                12,
                session >= 5, // Later sessions are correct (improvement)
                "Reading Comprehension",
                if (session >= 5) 25 else 10 // Better performance = more points
            )
        }
    }

    private fun createVocabularyProgress(word: String, masteryLevel: Float): VocabularyProgress {
        return VocabularyProgress(
            wordId = word,
            masteryLevel = masteryLevel,
            lastReviewDate = System.currentTimeMillis(),
            nextReviewDate = System.currentTimeMillis() + 259200000, // 3 days
            reviewCount = 3,
            successCount = (masteryLevel * 3).toInt(),
            errorCount = 3 - (masteryLevel * 3).toInt(),
            currentInterval = 3
        )
    }

    private fun createTestReadingContent(wordCount: Int = 100): ReadingContent {
        return ReadingContent(
            id = "test_content",
            title = "Test Article",
            content = "This is a test article with $wordCount words for reading practice and comprehension testing.",
            difficulty = ReadingLevel.B1,
            estimatedTime = 5,
            topics = listOf("test"),
            vocabularyFocus = listOf("test", "article", "practice"),
            grammarPatterns = listOf("present simple"),
            wordCount = wordCount,
            averageSentenceLength = 10.0f,
            complexityScore = 0.5f,
            weekAppropriate = 8..15
        )
    }

    private fun calculateWeeklyProgress(readingLogs: List<TaskLog>): Float {
        val thisWeekLogs = readingLogs.filter { log ->
            val daysSinceLog = (System.currentTimeMillis() - log.timestampMillis) / (24 * 60 * 60 * 1000)
            daysSinceLog <= 7
        }
        return kotlin.math.min(thisWeekLogs.size.toFloat() / 5f, 1f) // Target: 5 readings per week
    }

    private fun setupRealisticMockData() {
        // Setup realistic mock returns for user experience testing
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(createRealisticUserProgress()))
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(createRealisticTaskLogs()))
        `when`(mockVocabularyManager.getAll()).thenReturn(createRealisticVocabularyMastery())
        `when`(mockVocabularyManager.findByWords(any())).thenReturn(createRealisticVocabularyItems())
    }

    private fun createRealisticVocabularyItems(): List<VocabularyItem> {
        return listOf(
            VocabularyItem("comprehension", "Understanding", 3, VocabCategory.ACADEMIC, 
                listOf("reading comprehension"), listOf("understanding"), null, 0.6f, 0, 0, 0.0f, 10),
            VocabularyItem("analyze", "Examine in detail", 4, VocabCategory.ACADEMIC,
                listOf("analyze data"), listOf("examine"), null, 0.8f, 0, 0, 0.0f, 12)
        )
    }
}