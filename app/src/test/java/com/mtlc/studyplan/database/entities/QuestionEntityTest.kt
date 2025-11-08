package com.mtlc.studyplan.database.entities

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for QuestionEntity - Stores exam questions for study
 */
class QuestionEntityTest {

    private fun createTestQuestion(
        id: String = "question-1",
        examType: ExamType = ExamType.YDS,
        questionType: QuestionType = QuestionType.GRAMMAR,
        difficulty: Difficulty = Difficulty.B1,
        questionText: String = "What is the correct answer?",
        options: List<String> = listOf("A", "B", "C", "D"),
        correctAnswer: Int = 0,
        explanation: String = "This is the correct answer because...",
        tags: List<String> = emptyList(),
        isDownloaded: Boolean = false,
        lastAccessed: Long = System.currentTimeMillis()
    ) = QuestionEntity(
        id = id,
        examType = examType,
        questionType = questionType,
        difficulty = difficulty,
        questionText = questionText,
        options = options,
        correctAnswer = correctAnswer,
        explanation = explanation,
        tags = tags,
        isDownloaded = isDownloaded,
        lastAccessed = lastAccessed
    )

    @Test
    fun `QuestionEntity creates with required fields`() {
        val question = createTestQuestion()
        assertEquals("question-1", question.id)
        assertEquals("What is the correct answer?", question.questionText)
    }

    @Test
    fun `QuestionEntity supports all exam types`() {
        ExamType.values().forEach { exam ->
            val question = createTestQuestion(examType = exam)
            assertEquals(exam, question.examType)
        }
    }

    @Test
    fun `QuestionEntity supports all question types`() {
        QuestionType.values().forEach { type ->
            val question = createTestQuestion(questionType = type)
            assertEquals(type, question.questionType)
        }
    }

    @Test
    fun `QuestionEntity supports all difficulty levels`() {
        Difficulty.values().forEach { difficulty ->
            val question = createTestQuestion(difficulty = difficulty)
            assertEquals(difficulty, question.difficulty)
        }
    }

    @Test
    fun `QuestionEntity stores multiple choice options`() {
        val options = listOf(
            "The cat is sleeping",
            "The cat sleep",
            "The cat are sleeping",
            "The cat were sleeping"
        )
        val question = createTestQuestion(options = options)
        assertEquals(4, question.options.size)
        assertEquals(options, question.options)
    }

    @Test
    fun `QuestionEntity tracks correct answer index`() {
        val question = createTestQuestion(correctAnswer = 2)
        assertEquals(2, question.correctAnswer)
    }

    @Test
    fun `QuestionEntity stores explanation`() {
        val explanation = "This is correct because the subject is singular (cat)"
        val question = createTestQuestion(explanation = explanation)
        assertEquals(explanation, question.explanation)
    }

    @Test
    fun `QuestionEntity can be tagged`() {
        val tags = listOf("verb_tense", "present_continuous", "grammar")
        val question = createTestQuestion(tags = tags)
        assertEquals(tags, question.tags)
    }

    @Test
    fun `QuestionEntity tracks download status`() {
        val downloaded = createTestQuestion(isDownloaded = true)
        val notDownloaded = createTestQuestion(isDownloaded = false)

        assertTrue(downloaded.isDownloaded)
        assertFalse(notDownloaded.isDownloaded)
    }

    @Test
    fun `QuestionEntity tracks last access time`() {
        val accessTime = System.currentTimeMillis()
        val question = createTestQuestion(lastAccessed = accessTime)
        assertEquals(accessTime, question.lastAccessed)
    }

    @Test
    fun `QuestionEntity copy with updated difficulty`() {
        val original = createTestQuestion(difficulty = Difficulty.A2)
        val harder = original.copy(difficulty = Difficulty.B2)

        assertEquals(Difficulty.A2, original.difficulty)
        assertEquals(Difficulty.B2, harder.difficulty)
    }

    @Test
    fun `QuestionEntity equality test`() {
        val q1 = createTestQuestion(id = "q1")
        val q2 = createTestQuestion(id = "q1")
        assertEquals(q1, q2)
    }
}
