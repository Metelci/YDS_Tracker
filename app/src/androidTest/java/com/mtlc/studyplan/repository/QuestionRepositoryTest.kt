package com.mtlc.studyplan.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Question Repository
 * Tests question database operations with real Room database
 */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class QuestionRepositoryTest {

    private lateinit var database: StudyPlanDatabase
    private lateinit var repository: QuestionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            StudyPlanDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = QuestionRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveQuestion() = runBlocking {
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
    fun getQuestionsByExamType() = runBlocking {
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
    fun getQuestionsByDifficulty() = runBlocking {
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
    fun updateDownloadStatus() = runBlocking {
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
    fun getQuestionStats() = runBlocking {
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
    fun searchQuestions() = runBlocking {
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
