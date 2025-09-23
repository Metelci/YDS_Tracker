package com.mtlc.studyplan.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class QuestionRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: StudyPlanDatabase
    private lateinit var repository: QuestionRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StudyPlanDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = QuestionRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveQuestion() = runTest {
        // Given
        val question = QuestionEntity(
            id = "test_001",
            examType = ExamType.YDS,
            questionType = QuestionType.GRAMMAR,
            difficulty = Difficulty.B1,
            questionText = "Test question?",
            options = listOf("A", "B", "C", "D"),
            correctAnswer = 0,
            explanation = "Test explanation",
            tags = listOf("test", "grammar")
        )

        // When
        repository.insertQuestion(question)
        val retrieved = repository.getQuestionById("test_001")

        // Then
        assertNotNull(retrieved)
        assertEquals(question.id, retrieved?.id)
        assertEquals(question.questionText, retrieved?.questionText)
        assertEquals(question.correctAnswer, retrieved?.correctAnswer)
    }

    @Test
    fun getQuestionsByExamType() = runTest {
        // Given
        val ydsQuestion = createTestQuestion("yds_001", ExamType.YDS)
        val yokdilQuestion = createTestQuestion("yokdil_001", ExamType.YOKDIL)

        repository.insertQuestions(listOf(ydsQuestion, yokdilQuestion))

        // When
        val ydsQuestions = repository.getQuestionsByExamType(ExamType.YDS).first()

        // Then
        assertEquals(1, ydsQuestions.size)
        assertEquals(ExamType.YDS, ydsQuestions[0].examType)
    }

    @Test
    fun getQuestionsByDifficulty() = runTest {
        // Given
        val b1Question = createTestQuestion("b1_001", ExamType.YDS, Difficulty.B1)
        val b2Question = createTestQuestion("b2_001", ExamType.YDS, Difficulty.B2)

        repository.insertQuestions(listOf(b1Question, b2Question))

        // When
        val b1Questions = repository.getQuestionsByDifficulty(Difficulty.B1).first()

        // Then
        assertEquals(1, b1Questions.size)
        assertEquals(Difficulty.B1, b1Questions[0].difficulty)
    }

    @Test
    fun updateDownloadStatus() = runTest {
        // Given
        val question = createTestQuestion("download_test")
        repository.insertQuestion(question)

        // When
        repository.markQuestionAsDownloaded("download_test")
        val updated = repository.getQuestionById("download_test")

        // Then
        assertNotNull(updated)
        assertTrue(updated?.isDownloaded == true)
    }

    @Test
    fun getQuestionStats() = runTest {
        // Given
        val ydsQuestion = createTestQuestion("stats_yds", ExamType.YDS)
        val yokdilQuestion = createTestQuestion("stats_yokdil", ExamType.YOKDIL)

        repository.insertQuestions(listOf(ydsQuestion, yokdilQuestion))
        repository.markQuestionAsDownloaded("stats_yds")

        // When
        val stats = repository.getQuestionStats()

        // Then
        assertEquals(2, stats.totalQuestions)
        assertEquals(1, stats.downloadedQuestions)
        assertEquals(1, stats.ydsQuestions)
        assertEquals(1, stats.yokdilQuestions)
    }

    @Test
    fun searchQuestions() = runTest {
        // Given
        val question1 = createTestQuestion("search_001").copy(
            questionText = "What is photosynthesis?",
            explanation = "Photosynthesis is the process..."
        )
        val question2 = createTestQuestion("search_002").copy(
            questionText = "How does gravity work?",
            explanation = "Gravity is a fundamental force..."
        )

        repository.insertQuestions(listOf(question1, question2))

        // When
        val results = repository.searchQuestions("photosynthesis").first()

        // Then
        assertEquals(1, results.size)
        assertEquals("search_001", results[0].id)
    }

    private fun createTestQuestion(
        id: String,
        examType: ExamType = ExamType.YDS,
        difficulty: Difficulty = Difficulty.B1
    ) = QuestionEntity(
        id = id,
        examType = examType,
        questionType = QuestionType.GRAMMAR,
        difficulty = difficulty,
        questionText = "Test question $id?",
        options = listOf("A", "B", "C", "D"),
        correctAnswer = 0,
        explanation = "Test explanation for $id",
        tags = listOf("test", id)
    )
}