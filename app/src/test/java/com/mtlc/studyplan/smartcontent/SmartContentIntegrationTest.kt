package com.mtlc.studyplan.smartcontent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.ai.SmartScheduler
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.data.VocabularyItem
import com.mtlc.studyplan.data.VocabCategory
import com.mtlc.studyplan.questions.GeneratedQuestion
import com.mtlc.studyplan.questions.QuestionGenerator
import com.mtlc.studyplan.questions.SkillCategory
import com.mtlc.studyplan.questions.VocabularyManager
import com.mtlc.studyplan.reading.ReadingContent
import com.mtlc.studyplan.reading.ReadingLevel
import com.mtlc.studyplan.reading.TimeSlot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.ArgumentMatchers.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Comprehensive Integration Test for Unified Smart Content System
 * Tests the complete Phase 4 implementation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SmartContentIntegrationTest {

    @Mock
    private lateinit var mockProgressRepository: ProgressRepository

    @Mock
    private lateinit var mockQuestionGenerator: QuestionGenerator

    @Mock
    private lateinit var mockVocabularyManager: VocabularyManager

    @Mock
    private lateinit var mockSmartScheduler: SmartScheduler

    private lateinit var context: Context
    private lateinit var smartContentManager: SmartContentManager
    private lateinit var intelligenceEngine: ContentIntelligenceEngine

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Setup realistic mock data
        setupMockData()

        // Initialize components
        intelligenceEngine = ContentIntelligenceEngine(mockProgressRepository)
        smartContentManager = SmartContentManager(
            context, mockQuestionGenerator, mockVocabularyManager,
            mockProgressRepository, mockSmartScheduler
        )
    }

    @Test
    fun `test complete unified content system integration`() = runBlocking {
        // Test 1: Generate daily content pack
        val dailyPack = smartContentManager.generateDailyContentPack(30)

        assertNotNull("Should generate daily content pack", dailyPack)
        assertTrue("Should have content", dailyPack.vocabulary.isNotEmpty() || dailyPack.questions.isNotEmpty() || dailyPack.reading != null)
        assertTrue("Should have recommended sequence", dailyPack.recommendedSequence.isNotEmpty())
        assertTrue("Should have focus areas", dailyPack.focusAreas.isNotEmpty())
        assertTrue("Should have estimated time", dailyPack.estimatedTotalTime > 0)
        assertTrue("Should have difficulty level", dailyPack.difficultyLevel in 1f..5f)
        assertTrue("Should have confidence score", dailyPack.confidenceScore in 0f..1f)

        // Test 2: Create personalized study session
        val studySession = smartContentManager.createPersonalizedStudySession(SessionType.FOCUSED_PRACTICE)

        assertNotNull("Should create study session", studySession)
        assertEquals("Should have correct session type", SessionType.FOCUSED_PRACTICE, studySession.sessionType)
        assertTrue("Should have warmup vocabulary", studySession.warmupVocabulary.isNotEmpty())
        assertNotNull("Should have main content", studySession.mainContent)
        assertTrue("Should have reinforcement questions", studySession.reinforcementQuestions.isNotEmpty())
        assertTrue("Should have cooldown vocabulary", studySession.cooldownVocabulary.isNotEmpty())
        assertTrue("Should have session goals", studySession.sessionGoals.isNotEmpty())
        assertTrue("Should have estimated duration", studySession.estimatedDuration > 0)

        // Test 3: Get content recommendations
        val recommendations = smartContentManager.getContentRecommendations()

        assertTrue("Should provide recommendations", recommendations.isNotEmpty())
        recommendations.forEach { rec ->
            assertTrue("Should have valid content type", rec.contentType in ContentType.values())
            assertTrue("Should have title", rec.title.isNotEmpty())
            assertTrue("Should have description", rec.description.isNotEmpty())
            assertTrue("Should have estimated time", rec.estimatedTime > 0)
            assertTrue("Should have difficulty", rec.difficulty in 1f..5f)
            assertTrue("Should have relevance score", rec.relevanceScore in 0f..1f)
            assertTrue("Should have reason", rec.reason.isNotEmpty())
            assertTrue("Should have skill focus", rec.skillFocus.isNotEmpty())
        }

        // Test 4: Update content performance
        val performance = ContentPerformance(
            contentId = "test_content",
            contentType = ContentType.QUESTIONS,
            accuracy = 0.75f,
            timeSpent = 300000L, // 5 minutes
            completionRate = 1.0f,
            userEngagement = 0.8f,
            timestamp = System.currentTimeMillis(),
            difficulty = 3f,
            skillCategory = SkillCategory.GRAMMAR
        )

        smartContentManager.updateContentPerformance("test_content", performance)

        // Verify performance was processed (this would update internal state)
        // In a real test, we'd verify the intelligence engine was called

        // Test 5: Cross-system learning application
        smartContentManager.applyCrossSystemLearning()

        // This should trigger internal learning algorithm updates
        // Verification would require checking internal state changes
    }

    @Test
    fun `test content intelligence engine predictive capabilities`() = runBlocking {
        // Test 1: Build user learning profile
        val userProfile = intelligenceEngine.buildUserLearningProfile("test_user")

        assertNotNull("Should build user profile", userProfile)
        assertEquals("Should have correct user ID", "test_user", userProfile.userId)
        assertTrue("Should have learning speed for all skills", userProfile.learningSpeed.size == SkillCategory.values().size)
        assertTrue("Should have retention rates for all content types", userProfile.retentionRate.size == ContentType.values().size)
        assertTrue("Should have optimal session length", userProfile.optimalSessionLength in 5..60)
        assertTrue("Should have cognitive capacity", userProfile.cognitiveCapacity in 0f..1f)
        assertTrue("Should have motivation level", userProfile.motivationLevel in 0f..1f)

        // Test 2: Predict learning outcomes
        val testQuestion = createTestQuestion()
        val prediction = intelligenceEngine.predictLearningOutcomes(testQuestion, userProfile)

        assertNotNull("Should generate prediction", prediction)
        assertEquals("Should have correct content ID", testQuestion.id, prediction.contentId)
        assertTrue("Should have predicted accuracy", prediction.predictedAccuracy in 0f..1f)
        assertTrue("Should have predicted time", prediction.predictedTime > 0)
        assertTrue("Should have confidence level", prediction.confidenceLevel in 0f..1f)
        assertNotNull("Should have risk factors", prediction.riskFactors)
        assertNotNull("Should have recommended adjustments", prediction.recommendedAdjustments)

        // Test 3: Optimize content mix
        val learningGoals = createTestLearningGoals()
        val optimalMix = intelligenceEngine.optimizeContentMix(30, learningGoals)

        assertNotNull("Should generate optimal mix", optimalMix)
        assertTrue("Should have vocabulary ratio", optimalMix.vocabularyRatio in 0f..1f)
        assertTrue("Should have questions ratio", optimalMix.questionsRatio in 0f..1f)
        assertTrue("Should have reading ratio", optimalMix.readingRatio in 0f..1f)
        assertEquals("Should have correct total time", 30, optimalMix.totalTime)
        assertTrue("Should have difficulty distribution", optimalMix.difficultyDistribution.isNotEmpty())
        assertTrue("Should have skill balance", optimalMix.skillBalance.isNotEmpty())

        // Verify ratios sum to approximately 1.0 (allowing for floating point precision)
        val totalRatio = optimalMix.vocabularyRatio + optimalMix.questionsRatio + optimalMix.readingRatio
        assertTrue("Ratios should sum to approximately 1.0", abs(totalRatio - 1.0f) < 0.01f)

        // Test 4: Identify learning plateaus
        val plateaus = intelligenceEngine.identifyLearningPlateaus()

        // May or may not find plateaus depending on test data
        // If plateaus are found, verify their structure
        plateaus.forEach { plateau ->
            assertTrue("Should have valid skill category", plateau.skillCategory in SkillCategory.values())
            assertTrue("Should have plateau duration", plateau.plateauDuration >= 0)
            assertTrue("Should have current level", plateau.currentLevel in 0f..1f)
            assertTrue("Should have breakthrough strategies", plateau.breakthroughStrategies.isNotEmpty())
            assertTrue("Should have recommended actions", plateau.recommendedActions.isNotEmpty())
        }

        // Test 5: Suggest learning path adjustments
        val adjustments = intelligenceEngine.suggestLearningPathAdjustments()

        adjustments.forEach { adjustment ->
            assertTrue("Should have valid adjustment type", adjustment.adjustmentType in AdjustmentType.values())
            assertTrue("Should have description", adjustment.description.isNotEmpty())
            assertTrue("Should have expected impact", adjustment.expectedImpact in 0f..1f)
            assertTrue("Should have implementation difficulty", adjustment.implementationDifficulty in 0f..1f)
        }
    }

    @Test
    fun `test different study session types generation`() = runBlocking {
        val sessionTypes = listOf(
            SessionType.WARMUP,
            SessionType.FOCUSED_PRACTICE,
            SessionType.COMPREHENSIVE_REVIEW,
            SessionType.EXAM_PREPARATION,
            SessionType.SKILL_BUILDING
        )

        sessionTypes.forEach { sessionType ->
            val session = smartContentManager.createPersonalizedStudySession(sessionType)

            assertEquals("Should have correct session type", sessionType, session.sessionType)
            assertTrue("Should have warmup content", session.warmupVocabulary.isNotEmpty())
            assertNotNull("Should have main content", session.mainContent)
            assertTrue("Should have reinforcement content", session.reinforcementQuestions.isNotEmpty())
            assertTrue("Should have cooldown content", session.cooldownVocabulary.isNotEmpty())
            assertTrue("Should have session goals", session.sessionGoals.isNotEmpty())
            assertTrue("Should have reasonable duration", session.estimatedDuration in 10..45)

            // Verify session goals are appropriate for the type
            when (sessionType) {
                SessionType.WARMUP -> assertTrue("Warmup should mention confidence or momentum",
                    session.sessionGoals.any { it.contains("confidence") || it.contains("momentum") })
                SessionType.FOCUSED_PRACTICE -> assertTrue("Focused practice should mention weakness or mastery",
                    session.sessionGoals.any { it.contains("weak") || it.contains("mastery") })
                SessionType.EXAM_PREPARATION -> assertTrue("Exam prep should mention exam or confidence",
                    session.sessionGoals.any { it.contains("exam") || it.contains("confidence") })
                SessionType.SKILL_BUILDING -> assertTrue("Skill building should mention foundational or progressive",
                    session.sessionGoals.any { it.contains("foundational") || it.contains("progressive") })
                SessionType.COMPREHENSIVE_REVIEW -> assertTrue("Review should mention comprehensive or connect",
                    session.sessionGoals.any { it.contains("comprehensive") || it.contains("connect") })
            }
        }
    }

    @Test
    fun `test content performance feedback loop`() = runBlocking {
        // Create initial content performance
        val initialPerformance = ContentPerformance(
            contentId = "grammar_lesson_1",
            contentType = ContentType.QUESTIONS,
            accuracy = 0.6f,
            timeSpent = 480000L, // 8 minutes
            completionRate = 0.9f,
            userEngagement = 0.7f,
            timestamp = System.currentTimeMillis(),
            difficulty = 3f,
            skillCategory = SkillCategory.GRAMMAR
        )

        // Update performance
        intelligenceEngine.updatePerformanceData(initialPerformance)
        intelligenceEngine.updateContentEffectiveness("grammar_lesson_1", initialPerformance)

        // Create improved performance (user got better)
        val improvedPerformance = initialPerformance.copy(
            accuracy = 0.8f,
            timeSpent = 360000L, // 6 minutes (faster)
            userEngagement = 0.9f,
            timestamp = System.currentTimeMillis() + 86400000L // Next day
        )

        // Update with improved performance
        intelligenceEngine.updatePerformanceData(improvedPerformance)
        intelligenceEngine.updateContentEffectiveness("grammar_lesson_1", improvedPerformance)

        // The system should now have learned from this improvement
        // In a real scenario, this would affect future recommendations

        // Test that the system can generate predictions based on updated data
        val userProfile = intelligenceEngine.buildUserLearningProfile("test_user")
        val testQuestion = createTestQuestion()
        val prediction = intelligenceEngine.predictLearningOutcomes(testQuestion, userProfile)

        assertNotNull("Should still generate predictions after learning", prediction)
        // The prediction should potentially be different due to learned patterns
    }

    @Test
    fun `test adaptive difficulty and personalization`() = runBlocking {
        // Test that the system adapts recommendations based on user performance

        // Simulate a user who struggles with grammar but excels at vocabulary
        val userProfile = createStrugglingUserProfile()

        // Generate content recommendations
        val recommendations = smartContentManager.getContentRecommendations()

        // Should prioritize grammar content due to weakness
        val grammarRecommendations = recommendations.filter { it.skillFocus.contains(SkillCategory.GRAMMAR) }
        val vocabRecommendations = recommendations.filter { it.skillFocus.contains(SkillCategory.VOCAB) }

        // Should have more grammar-focused recommendations due to weakness
        assertTrue("Should prioritize weak areas", grammarRecommendations.size >= vocabRecommendations.size)

        // Test optimal mix for struggling user
        val learningGoals = listOf(
            LearningGoal(SkillCategory.GRAMMAR, 0.8f, 30 * 24 * 60 * 60 * 1000L, 3, 0.5f)
        )
        val optimalMix = intelligenceEngine.optimizeContentMix(25, learningGoals)

        // Should allocate more time to questions (grammar practice)
        assertTrue("Should allocate more time to weak skill", optimalMix.questionsRatio > optimalMix.vocabularyRatio)
    }

    @Test
    fun `test comprehensive system evolution and learning`() = runBlocking {
        // Test that the system can identify plateaus and suggest adjustments

        // Create a scenario with a clear plateau in reading comprehension
        val plateauUserProfile = createPlateauUserProfile()

        // The system should identify the plateau
        val plateaus = intelligenceEngine.identifyLearningPlateaus()

        assertTrue("Should identify reading plateau", plateaus.any { it.skillCategory == SkillCategory.READING })

        // Should suggest path adjustments
        val adjustments = intelligenceEngine.suggestLearningPathAdjustments()

        assertTrue("Should suggest adjustments for plateau", adjustments.isNotEmpty())

        // Should include specific strategies for breaking through plateaus
        val readingAdjustments = adjustments.filter { it.targetSkill == SkillCategory.READING }
        assertTrue("Should have reading-specific adjustments", readingAdjustments.isNotEmpty())

        // Verify adjustment recommendations are actionable
        adjustments.forEach { adjustment ->
            assertTrue("Should have clear description", adjustment.description.length > 10)
            assertTrue("Should have reasonable expected impact", adjustment.expectedImpact > 0f)
            assertTrue("Should have implementation difficulty rating", adjustment.implementationDifficulty >= 0f)
        }
    }

    // Helper methods for creating test data

    private fun setupMockData() = runBlocking {
        // Setup realistic user progress
        val userProgress = UserProgress(
            completedTasks = (1..100).map { "task_$it" }.toSet(),
            streakCount = 12,
            lastCompletionDate = System.currentTimeMillis() - 3600000,
            unlockedAchievements = setOf("first_week", "consistent_learner"),
            totalPoints = 2500,
            currentStreakMultiplier = 2.0f
        )

        // Setup realistic task logs
        val taskLogs = createRealisticTaskLogs()

        // Setup vocabulary data
        val vocabularyItems = createTestVocabularyItems()

        // Setup question generation mock
        val mockQuestions = createMockQuestions()

        `when`(mockProgressRepository.userProgressFlow).thenReturn(flowOf(userProgress))
        `when`(mockProgressRepository.taskLogsFlow).thenReturn(flowOf(taskLogs))
        `when`(mockVocabularyManager.getAll()).thenReturn(vocabularyItems)
        // `when`(mockQuestionGenerator.generateQuestions(any(), any(), any(), any<List<String>>())).thenReturn(mockQuestions)
        // `when`(mockQuestionGenerator.generateQuestions(any(), any(), any())).thenReturn(mockQuestions)
    }

    private fun createRealisticTaskLogs(): List<TaskLog> {
        val now = System.currentTimeMillis()
        return listOf(
            TaskLog("reading_1", now - 86400000, 15, true, "Reading Comprehension", 20),
            TaskLog("vocab_1", now - 86400000, 8, true, "Vocabulary", 12),
            TaskLog("grammar_1", now - 86400000, 12, false, "Grammar", 8),
            TaskLog("reading_2", now - 43200000, 12, true, "Reading Comprehension", 18),
            TaskLog("vocab_2", now - 43200000, 6, true, "Vocabulary", 10),
            TaskLog("grammar_2", now - 43200000, 10, true, "Grammar", 15),
            TaskLog("reading_3", now - 21600000, 18, false, "Reading Comprehension", 12),
            TaskLog("vocab_3", now - 21600000, 7, true, "Vocabulary", 11),
            TaskLog("grammar_3", now - 21600000, 14, true, "Grammar", 18)
        )
    }

    private fun createTestVocabularyItems(): List<VocabularyItem> {
        return listOf(
            VocabularyItem("comprehensive", "Complete and thorough", 4, VocabCategory.ACADEMIC,
                listOf("comprehensive analysis"), listOf("thorough", "complete"), null, 0.7f, 0, 0, 0.0f, 15),
            VocabularyItem("analyze", "Examine in detail", 3, VocabCategory.ACADEMIC,
                listOf("analyze data"), listOf("examine", "study"), null, 0.8f, 0, 0, 0.0f, 12),
            VocabularyItem("significant", "Important or meaningful", 3, VocabCategory.ACADEMIC,
                listOf("significant impact"), listOf("important", "meaningful"), null, 0.6f, 0, 0, 0.0f, 10)
        )
    }

    private fun createMockQuestions(): List<GeneratedQuestion> {
        return listOf(
            GeneratedQuestion(
                id = "q1",
                category = SkillCategory.GRAMMAR,
                prompt = "Choose the correct form: She ___ to the store yesterday.",
                options = listOf("go", "went", "going", "goes"),
                correctIndex = 1,
                difficulty = 2,
                explanation = "Past simple tense is used for completed actions in the past."
            ),
            GeneratedQuestion(
                id = "q2",
                category = SkillCategory.VOCAB,
                prompt = "What does 'comprehensive' mean?",
                options = listOf("Simple", "Complete", "Quick", "Small"),
                correctIndex = 1,
                difficulty = 3,
                explanation = "'Comprehensive' means complete and thorough."
            )
        )
    }

    private fun createTestQuestion(): GeneratedQuestion {
        return GeneratedQuestion(
            id = "test_question",
            category = SkillCategory.GRAMMAR,
            prompt = "Select the correct tense: Yesterday, I ___ to the park.",
            options = listOf("go", "went", "going", "will go"),
            correctIndex = 1,
            difficulty = 2,
            explanation = "Past simple is used for completed actions in the past."
        )
    }

    private fun createTestLearningGoals(): List<LearningGoal> {
        return listOf(
            LearningGoal(SkillCategory.GRAMMAR, 0.8f, 30 * 24 * 60 * 60 * 1000L, 3, 0.6f),
            LearningGoal(SkillCategory.READING, 0.75f, 30 * 24 * 60 * 60 * 1000L, 2, 0.7f),
            LearningGoal(SkillCategory.VOCAB, 0.85f, 30 * 24 * 60 * 60 * 1000L, 1, 0.8f)
        )
    }

    private fun createStrugglingUserProfile(): UserLearningProfile {
        return UserLearningProfile(
            userId = "struggling_user",
            learningSpeed = mapOf(
                SkillCategory.GRAMMAR to 0.4f,
                SkillCategory.READING to 0.6f,
                SkillCategory.LISTENING to 0.5f,
                SkillCategory.VOCAB to 0.8f
            ),
            retentionRate = mapOf(
                ContentType.VOCABULARY to 0.9f,
                ContentType.QUESTIONS to 0.6f,
                ContentType.READING to 0.7f,
                ContentType.MIXED to 0.75f
            ),
            preferredDifficultyCurve = CurveType.GRADUAL,
            optimalSessionLength = 20,
            peakPerformanceTimes = listOf(TimeSlot(10, 12, emptyList(), 15)),
            weaknessPatterns = listOf(
                WeaknessPattern(SkillCategory.GRAMMAR, PatternType.CONSISTENT_LOW_PERFORMANCE, 0.6f, 15, System.currentTimeMillis())
            ),
            interestAreas = listOf("technology", "science"),
            cognitiveCapacity = 0.7f,
            motivationLevel = 0.8f,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun createPlateauUserProfile(): UserLearningProfile {
        return UserLearningProfile(
            userId = "plateau_user",
            learningSpeed = mapOf(
                SkillCategory.GRAMMAR to 0.7f,
                SkillCategory.READING to 0.65f, // Plateau area
                SkillCategory.LISTENING to 0.75f,
                SkillCategory.VOCAB to 0.8f
            ),
            retentionRate = mapOf(
                ContentType.VOCABULARY to 0.85f,
                ContentType.QUESTIONS to 0.8f,
                ContentType.READING to 0.6f, // Lower retention in plateau area
                ContentType.MIXED to 0.75f
            ),
            preferredDifficultyCurve = CurveType.PLATEAU,
            optimalSessionLength = 25,
            peakPerformanceTimes = listOf(TimeSlot(14, 16, emptyList(), 20)),
            weaknessPatterns = listOf(
                WeaknessPattern(SkillCategory.READING, PatternType.CONSISTENT_LOW_PERFORMANCE, 0.35f, 20, System.currentTimeMillis())
            ),
            interestAreas = listOf("business", "culture"),
            cognitiveCapacity = 0.8f,
            motivationLevel = 0.7f,
            lastUpdated = System.currentTimeMillis()
        )
    }
}