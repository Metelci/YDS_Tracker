package com.mtlc.studyplan.reading

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.data.VocabularyItem
import com.mtlc.studyplan.data.VocabCategory
import com.mtlc.studyplan.questions.SkillCategory
import com.mtlc.studyplan.questions.VocabularyManager
import com.mtlc.studyplan.questions.QuestionGenerator
import com.mtlc.studyplan.storage.room.StudyPlanDatabase
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

/**
 * Integration tests for the Reading System
 * Tests all components working together with existing systems
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReadingSystemIntegrationTest {

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
        
        // Setup mock data
        setupMockData()
        
        // Initialize components
        readingIntegration = ReadingSystemIntegration(
            context, mockProgressRepository, mockVocabularyManager, mockQuestionGenerator
        )
        
        readingAnalytics = ReadingAnalytics(mockProgressRepository)
    }

    @Test
    fun `test reading content loading from JSON`() = runBlocking {
        // Mock context to return test JSON
        val testReadingContent = listOf(
            ReadingContent(
                id = "test_001",
                title = "Test Article",
                content = "This is a test article for reading comprehension.",
                difficulty = ReadingLevel.B1,
                estimatedTime = 5,
                topics = listOf("technology"),
                vocabularyFocus = listOf("comprehension", "article"),
                grammarPatterns = listOf("present simple"),
                wordCount = 50,
                averageSentenceLength = 10.0f,
                complexityScore = 0.5f,
                weekAppropriate = 8..15
            )
        )
        
        // Create mock context with asset manager
        val mockContext = mock(Context::class.java)
        val mockAssetManager = mock(android.content.res.AssetManager::class.java)
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        
        val jsonContent = Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(ReadingContent.serializer()),
            testReadingContent
        )
        `when`(mockAssetManager.open("reading_materials.json"))
            .thenReturn(ByteArrayInputStream(jsonContent.toByteArray()))

        // Test loading
        val curator = ContentCurator(mockContext, testReadingContent, mockProgressRepository, mockVocabularyManager)
        val recommendedContent = curator.recommendReading(10)
        
        assertNotNull("Should recommend content", recommendedContent)
        assertEquals("Should match test content", "test_001", recommendedContent?.id)
    }

    @Test
    fun `test content curation with vocabulary integration`() = runBlocking {
        val testContent = createTestReadingContent()
        val testVocabulary = createTestVocabulary()
        
        `when`(mockVocabularyManager.findByWords(any())).thenReturn(testVocabulary)
        `when`(mockVocabularyManager.getAll()).thenReturn(testVocabulary)
        
        val curator = ContentCurator(context, listOf(testContent), mockProgressRepository, mockVocabularyManager)
        
        // Test vocabulary alignment in recommendations
        val recommendation = curator.recommendReading(10)
        assertNotNull("Should provide recommendation", recommendation)
        assertTrue("Should have vocabulary focus", recommendation!!.vocabularyFocus.isNotEmpty())
    }

    @Test
    fun `test reading analytics with progress tracking`() = runBlocking {
        val testTaskLogs = createTestTaskLogs()
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(testTaskLogs))
        
        val analytics = ReadingAnalytics(mockProgressRepository)
        
        // Test optimal reading times identification
        val optimalTimes = analytics.identifyOptimalReadingTimes()
        assertFalse("Should identify optimal times", optimalTimes.isEmpty())
        
        // Test reading endurance calculation
        val endurance = analytics.calculateReadingEndurance()
        assertTrue("Should calculate realistic endurance", endurance > 0 && endurance <= 60)
        
        // Test performance metrics generation
        val metrics = analytics.generatePerformanceMetrics("test_user")
        assertEquals("Should set correct user ID", "test_user", metrics.userId)
        assertTrue("Should have positive WPM", metrics.averageWPM > 0)
        assertTrue("Should have valid comprehension", metrics.averageComprehension >= 0f && metrics.averageComprehension <= 1f)
    }

    @Test
    fun `test reading session integration`() = runBlocking {
        val testContent = createTestReadingContent()
        val testVocabulary = createTestVocabulary()
        
        `when`(mockVocabularyManager.findByWords(any())).thenReturn(testVocabulary)
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(createTestUserProgress()))
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(createTestTaskLogs()))
        
        // Test integrated session creation
        val session = readingIntegration.createIntegratedReadingSession("test_001", 10)
        
        assertNotNull("Should create integrated session", session)
        assertNotNull("Session should have content", session?.content)
        assertNotNull("Session should have vocabulary items", session?.vocabularyItems)
        assertNotNull("Session should have comprehension questions", session?.comprehensionQuestions)
        assertNotNull("Session should have post-reading activities", session?.postReadingActivities)
    }

    @Test
    fun `test vocabulary progress integration`() = runBlocking {
        val testVocabularyEncounters = listOf(
            VocabularyEncounter(
                word = "comprehension",
                interactionType = VocabularyInteractionType.LOOKED_UP,
                responseTime = 5000L,
                progressUpdate = createTestVocabularyProgress("comprehension")
            )
        )
        
        // Test vocabulary encounter processing
        val readingSpeedData = ReadingSpeedData(
            contentId = "test_001",
            wordsPerMinute = 200,
            comprehensionAccuracy = 0.8f,
            completionTime = 300000L // 5 minutes
        )
        
        // This would normally update the vocabulary progress
        val result = readingIntegration.completeReadingSession(
            "test_session",
            mapOf(0 to "correct_answer"),
            testVocabularyEncounters,
            readingSpeedData
        )
        
        // Verify vocabulary progress was called
        verify(mockProgressRepository, atLeastOnce()).updateVocabularyMastery(
            eq("comprehension"),
            eq(false), // looked up = incorrect initially
            eq(5000L),
            any()
        )
    }

    @Test
    fun `test content complexity analysis`() {
        val analyzer = ContentComplexityAnalyzer()
        val testContent = createTestReadingContent()
        
        val complexity = analyzer.calculateComplexityScore(testContent)
        
        assertTrue("Complexity should be between 0 and 1", complexity >= 0f && complexity <= 1f)
    }

    @Test
    fun `test reading level progression`() {
        // Test reading level calculation based on week
        assertEquals("Week 3 should be A2", ReadingLevel.A2, ReadingLevel.fromWeek(3))
        assertEquals("Week 8 should be B1", ReadingLevel.B1, ReadingLevel.fromWeek(8))
        assertEquals("Week 20 should be B2", ReadingLevel.B2, ReadingLevel.fromWeek(20))
        assertEquals("Week 30 should be C1", ReadingLevel.C1, ReadingLevel.fromWeek(30))
    }

    @Test
    fun `test recommendation priority system`() = runBlocking {
        val testContents = listOf(
            createTestReadingContent().copy(
                id = "high_priority",
                difficulty = ReadingLevel.B1,
                topics = listOf("grammar") // Assuming user has grammar weakness
            ),
            createTestReadingContent().copy(
                id = "medium_priority",
                difficulty = ReadingLevel.B1_PLUS,
                topics = listOf("technology")
            )
        )
        
        // Mock weak areas (grammar)
        val taskLogs = listOf(
            TaskLog("grammar_1", System.currentTimeMillis(), 10, false, "Grammar", 5),
            TaskLog("grammar_2", System.currentTimeMillis(), 12, false, "Grammar", 5)
        )
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(taskLogs))
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(createTestUserProgress()))
        
        val curator = ContentCurator(context, testContents, mockProgressRepository, mockVocabularyManager)
        val weakAreaContent = curator.getReadingByWeakArea(SkillCategory.GRAMMAR)
        
        assertFalse("Should return content for weak areas", weakAreaContent.isEmpty())
    }

    @Test
    fun `test topic rotation algorithm`() = runBlocking {
        val taskLogs = createTestTaskLogsWithTopics()
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(taskLogs))
        
        val testContents = listOf(
            createTestReadingContent().copy(topics = listOf("technology")),
            createTestReadingContent().copy(topics = listOf("health")),
            createTestReadingContent().copy(topics = listOf("culture"))
        )
        
        val curator = ContentCurator(context, testContents, mockProgressRepository, mockVocabularyManager)
        val recommendedTopic = curator.getTopicRotationRecommendation()
        
        assertNotNull("Should recommend a topic", recommendedTopic)
        assertFalse("Topic should not be empty", recommendedTopic.isBlank())
    }

    // Helper methods for creating test data

    private fun createTestReadingContent(): ReadingContent {
        return ReadingContent(
            id = "test_001",
            title = "Test Reading Content",
            content = "This is a test article for comprehensive reading practice. It contains various vocabulary words and grammar structures to help students improve their English skills.",
            difficulty = ReadingLevel.B1,
            estimatedTime = 10,
            topics = listOf("education", "language_learning"),
            vocabularyFocus = listOf("comprehensive", "vocabulary", "grammar", "structures"),
            grammarPatterns = listOf("present simple", "infinitive forms"),
            wordCount = 150,
            averageSentenceLength = 12.5f,
            complexityScore = 0.55f,
            weekAppropriate = 8..15,
            comprehensionQuestions = listOf(
                "What is the main topic of this article?",
                "Name two things that help students improve English.",
                "What type of practice does this article provide?"
            )
        )
    }

    private fun createTestVocabulary(): List<VocabularyItem> {
        return listOf(
            VocabularyItem(
                word = "comprehensive",
                definition = "Complete and including everything that is necessary",
                difficulty = 3,
                category = VocabCategory.ACADEMIC,
                contexts = listOf("comprehensive study", "comprehensive analysis"),
                relatedWords = listOf("complete", "thorough", "extensive"),
                grammarPattern = "adjective",
                masteryLevel = 0.6f,
                weekIntroduced = 10
            ),
            VocabularyItem(
                word = "vocabulary",
                definition = "All the words known and used by a person",
                difficulty = 2,
                category = VocabCategory.ACADEMIC,
                contexts = listOf("vocabulary words", "expand vocabulary"),
                relatedWords = listOf("words", "lexicon", "terminology"),
                grammarPattern = "noun",
                masteryLevel = 0.8f,
                weekIntroduced = 5
            )
        )
    }

    private fun createTestTaskLogs(): List<TaskLog> {
        return listOf(
            TaskLog("reading_1", System.currentTimeMillis() - 86400000, 15, true, "Reading Comprehension", 25),
            TaskLog("reading_2", System.currentTimeMillis() - 172800000, 12, true, "Reading Comprehension", 20),
            TaskLog("reading_3", System.currentTimeMillis() - 259200000, 18, false, "Reading Comprehension", 10),
            TaskLog("grammar_1", System.currentTimeMillis() - 86400000, 8, false, "Grammar", 5),
            TaskLog("vocab_1", System.currentTimeMillis() - 86400000, 10, true, "Vocabulary", 15)
        )
    }

    private fun createTestTaskLogsWithTopics(): List<TaskLog> {
        return listOf(
            TaskLog("tech_1", System.currentTimeMillis() - 86400000, 10, true, "technology", 15),
            TaskLog("tech_2", System.currentTimeMillis() - 172800000, 12, true, "technology", 18),
            TaskLog("health_1", System.currentTimeMillis() - 604800000, 8, true, "health", 12) // 1 week ago
        )
    }

    private fun createTestUserProgress(): UserProgress {
        return UserProgress(
            completedTasks = setOf("task1", "task2", "task3", "task4", "task5"),
            streakCount = 5,
            lastCompletionDate = System.currentTimeMillis() - 86400000,
            unlockedAchievements = setOf("reading_novice"),
            totalPoints = 150,
            currentStreakMultiplier = 2.0f
        )
    }

    private fun createTestVocabularyProgress(word: String): com.mtlc.studyplan.data.VocabularyProgress {
        return com.mtlc.studyplan.data.VocabularyProgress(
            wordId = word,
            masteryLevel = 0.4f,
            lastReviewDate = System.currentTimeMillis(),
            nextReviewDate = System.currentTimeMillis() + 259200000, // 3 days
            reviewCount = 2,
            successCount = 1,
            errorCount = 1,
            currentInterval = 3
        )
    }

    private fun setupMockData() {
        // Setup common mock returns
        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(createTestUserProgress()))
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(createTestTaskLogs()))
        `when`(mockVocabularyManager.getAll()).thenReturn(emptyList())
    }
}