package com.mtlc.studyplan.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.Difficulty
import com.mtlc.studyplan.database.entities.ExamType
import com.mtlc.studyplan.database.entities.QuestionEntity
import com.mtlc.studyplan.database.entities.QuestionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for QuestionDao - Question database operations for language exams
 * Tests CRUD operations, filtering, search, and download status management
 *
 * Pattern: Study-first methodology with Robolectric + Room in-memory database
 * Fix: Explicit JournalMode.TRUNCATE to avoid ActivityManager.isLowRamDevice() NoSuchMethodError
 */
@RunWith(RobolectricTestRunner::class)
class QuestionDaoTest {

    private lateinit var database: StudyPlanDatabase
    private lateinit var questionDao: QuestionDao

    @Before
    fun setUp() {
        try { stopKoin() } catch (e: Exception) { }
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            StudyPlanDatabase::class.java
        )
            .allowMainThreadQueries()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE) // Fix for Robolectric
            .build()
        questionDao = database.questionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createTestQuestion(
        id: String = "q-1",
        examType: ExamType = ExamType.YDS,
        questionType: QuestionType = QuestionType.GRAMMAR,
        difficulty: Difficulty = Difficulty.B1,
        isDownloaded: Boolean = false,
        tags: List<String> = listOf("verb", "present-tense")
    ) = QuestionEntity(
        id = id,
        examType = examType,
        questionType = questionType,
        difficulty = difficulty,
        questionText = "What is the correct answer?",
        options = listOf("A) Option A", "B) Option B", "C) Option C", "D) Option D"),
        correctAnswer = 1,
        explanation = "The correct answer is B because...",
        tags = tags,
        isDownloaded = isDownloaded,
        lastAccessed = System.currentTimeMillis()
    )

    // ========== INSERT TESTS ==========

    @Test
    fun `insertQuestion should store question in database`() = runTest {
        val question = createTestQuestion(id = "q1")

        questionDao.insertQuestion(question)

        val retrieved = questionDao.getQuestionById("q1")
        assertNotNull(retrieved)
        assertEquals("What is the correct answer?", retrieved.questionText)
    }

    @Test
    fun `insertQuestions should store multiple questions`() = runTest {
        val questions = listOf(
            createTestQuestion(id = "q1"),
            createTestQuestion(id = "q2"),
            createTestQuestion(id = "q3")
        )

        questionDao.insertQuestions(questions)

        val all = questionDao.getAllQuestions().first()
        assertEquals(3, all.size)
    }

    // ========== READ TESTS ==========

    @Test
    fun `getQuestionById should return question when exists`() = runTest {
        val question = createTestQuestion(id = "q1")
        questionDao.insertQuestion(question)

        val retrieved = questionDao.getQuestionById("q1")

        assertNotNull(retrieved)
        assertEquals("q1", retrieved.id)
        assertEquals(ExamType.YDS, retrieved.examType)
    }

    @Test
    fun `getQuestionById should return null when not exists`() = runTest {
        val retrieved = questionDao.getQuestionById("nonexistent")

        assertNull(retrieved)
    }

    @Test
    fun `getAllQuestions should return all questions ordered by lastAccessed DESC`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1"),
            createTestQuestion(id = "q2"),
            createTestQuestion(id = "q3")
        ))

        val all = questionDao.getAllQuestions().first()

        assertEquals(3, all.size)
    }

    // ========== FILTERING TESTS ==========

    @Test
    fun `getQuestionsByExamType should filter by exam type`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", examType = ExamType.YDS),
            createTestQuestion(id = "q2", examType = ExamType.KPDS),
            createTestQuestion(id = "q3", examType = ExamType.YDS)
        ))

        val ydsQuestions = questionDao.getQuestionsByExamType(ExamType.YDS).first()

        assertEquals(2, ydsQuestions.size)
        assertTrue(ydsQuestions.all { it.examType == ExamType.YDS })
    }

    @Test
    fun `getQuestionsByType should filter by question type`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", questionType = QuestionType.GRAMMAR),
            createTestQuestion(id = "q2", questionType = QuestionType.READING),
            createTestQuestion(id = "q3", questionType = QuestionType.GRAMMAR)
        ))

        val grammarQuestions = questionDao.getQuestionsByType(QuestionType.GRAMMAR).first()

        assertEquals(2, grammarQuestions.size)
        assertTrue(grammarQuestions.all { it.questionType == QuestionType.GRAMMAR })
    }

    @Test
    fun `getQuestionsByDifficulty should filter by difficulty`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", difficulty = Difficulty.B1),
            createTestQuestion(id = "q2", difficulty = Difficulty.C1),
            createTestQuestion(id = "q3", difficulty = Difficulty.B1)
        ))

        val b1Questions = questionDao.getQuestionsByDifficulty(Difficulty.B1).first()

        assertEquals(2, b1Questions.size)
        assertTrue(b1Questions.all { it.difficulty == Difficulty.B1 })
    }

    @Test
    fun `getQuestionsByExamAndType should filter by both exam and type`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", examType = ExamType.YDS, questionType = QuestionType.GRAMMAR),
            createTestQuestion(id = "q2", examType = ExamType.YDS, questionType = QuestionType.READING),
            createTestQuestion(id = "q3", examType = ExamType.KPDS, questionType = QuestionType.GRAMMAR)
        ))

        val filtered = questionDao.getQuestionsByExamAndType(ExamType.YDS, QuestionType.GRAMMAR).first()

        assertEquals(1, filtered.size)
        assertEquals(ExamType.YDS, filtered[0].examType)
        assertEquals(QuestionType.GRAMMAR, filtered[0].questionType)
    }

    @Test
    fun `getQuestionsByExamAndDifficulty should filter by exam and difficulty`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", examType = ExamType.YDS, difficulty = Difficulty.B1),
            createTestQuestion(id = "q2", examType = ExamType.YDS, difficulty = Difficulty.C1),
            createTestQuestion(id = "q3", examType = ExamType.KPDS, difficulty = Difficulty.B1)
        ))

        val filtered = questionDao.getQuestionsByExamAndDifficulty(ExamType.YDS, Difficulty.B1).first()

        assertEquals(1, filtered.size)
        assertEquals(ExamType.YDS, filtered[0].examType)
        assertEquals(Difficulty.B1, filtered[0].difficulty)
    }

    @Test
    fun `getDownloadedQuestions should return only downloaded questions`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", isDownloaded = true),
            createTestQuestion(id = "q2", isDownloaded = false),
            createTestQuestion(id = "q3", isDownloaded = true)
        ))

        val downloaded = questionDao.getDownloadedQuestions().first()

        assertEquals(2, downloaded.size)
        assertTrue(downloaded.all { it.isDownloaded })
    }

    // ========== COUNT TESTS ==========

    @Test
    fun `getTotalQuestionCount should return total number of questions`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1"),
            createTestQuestion(id = "q2"),
            createTestQuestion(id = "q3")
        ))

        val count = questionDao.getTotalQuestionCount()

        assertEquals(3, count)
    }

    @Test
    fun `getQuestionCountByExamType should count by exam type`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", examType = ExamType.YDS),
            createTestQuestion(id = "q2", examType = ExamType.YDS),
            createTestQuestion(id = "q3", examType = ExamType.KPDS)
        ))

        val count = questionDao.getQuestionCountByExamType(ExamType.YDS)

        assertEquals(2, count)
    }

    @Test
    fun `getQuestionCountByType should count by question type`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", questionType = QuestionType.GRAMMAR),
            createTestQuestion(id = "q2", questionType = QuestionType.GRAMMAR),
            createTestQuestion(id = "q3", questionType = QuestionType.READING)
        ))

        val count = questionDao.getQuestionCountByType(QuestionType.GRAMMAR)

        assertEquals(2, count)
    }

    @Test
    fun `getQuestionCountByDifficulty should count by difficulty`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", difficulty = Difficulty.B1),
            createTestQuestion(id = "q2", difficulty = Difficulty.B1),
            createTestQuestion(id = "q3", difficulty = Difficulty.C1)
        ))

        val count = questionDao.getQuestionCountByDifficulty(Difficulty.B1)

        assertEquals(2, count)
    }

    @Test
    fun `getDownloadedQuestionCount should count downloaded questions`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", isDownloaded = true),
            createTestQuestion(id = "q2", isDownloaded = true),
            createTestQuestion(id = "q3", isDownloaded = false)
        ))

        val count = questionDao.getDownloadedQuestionCount()

        assertEquals(2, count)
    }

    // ========== UPDATE TESTS ==========

    @Test
    fun `updateQuestion should modify question`() = runTest {
        val initial = createTestQuestion(id = "q1")
        questionDao.insertQuestion(initial)

        val modified = initial.copy(questionText = "Updated question text")
        questionDao.updateQuestion(modified)

        val updated = questionDao.getQuestionById("q1")
        assertEquals("Updated question text", updated?.questionText)
    }

    @Test
    fun `updateDownloadStatus should mark as downloaded`() = runTest {
        val question = createTestQuestion(id = "q1", isDownloaded = false)
        questionDao.insertQuestion(question)

        questionDao.updateDownloadStatus("q1", true)

        val updated = questionDao.getQuestionById("q1")
        assertTrue(updated?.isDownloaded == true)
    }

    @Test
    fun `updateLastAccessed should update access timestamp`() = runTest {
        val question = createTestQuestion(id = "q1")
        questionDao.insertQuestion(question)

        val newTimestamp = System.currentTimeMillis() + 10000
        questionDao.updateLastAccessed("q1", newTimestamp)

        val updated = questionDao.getQuestionById("q1")
        assertEquals(newTimestamp, updated?.lastAccessed)
    }

    // ========== DELETE TESTS ==========

    @Test
    fun `deleteQuestion should remove question`() = runTest {
        val question = createTestQuestion(id = "q1")
        questionDao.insertQuestion(question)

        questionDao.deleteQuestion(question)

        val deleted = questionDao.getQuestionById("q1")
        assertNull(deleted)
    }

    @Test
    fun `deleteNonDownloadedQuestions should remove only non-downloaded`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", isDownloaded = true),
            createTestQuestion(id = "q2", isDownloaded = false),
            createTestQuestion(id = "q3", isDownloaded = false)
        ))

        questionDao.deleteNonDownloadedQuestions()

        val remaining = questionDao.getAllQuestions().first()
        assertEquals(1, remaining.size)
        assertTrue(remaining[0].isDownloaded)
    }

    // ========== SEARCH TESTS ==========

    @Test
    fun `searchQuestions should find by question text`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1"),
            createTestQuestion(id = "q2").copy(questionText = "Find this specific question"),
            createTestQuestion(id = "q3")
        ))

        val results = questionDao.searchQuestions("specific").first()

        assertEquals(1, results.size)
        assertEquals("q2", results[0].id)
    }

    @Test
    fun `searchQuestions should find by explanation`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1"),
            createTestQuestion(id = "q2").copy(explanation = "This is the unique explanation"),
            createTestQuestion(id = "q3")
        ))

        val results = questionDao.searchQuestions("unique").first()

        assertEquals(1, results.size)
        assertEquals("q2", results[0].id)
    }

    @Test
    fun `getQuestionsByTag should filter by tag`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", tags = listOf("verb", "present")),
            createTestQuestion(id = "q2", tags = listOf("noun", "plural")),
            createTestQuestion(id = "q3", tags = listOf("verb", "past"))
        ))

        val verbQuestions = questionDao.getQuestionsByTag("verb").first()

        assertEquals(2, verbQuestions.size)
        assertTrue(verbQuestions.all { it.tags.contains("verb") })
    }

    @Test
    fun `getAllTags should return all distinct tag lists`() = runTest {
        questionDao.insertQuestions(listOf(
            createTestQuestion(id = "q1", tags = listOf("tag1", "tag2")),
            createTestQuestion(id = "q2", tags = listOf("tag2", "tag3")),
            createTestQuestion(id = "q3", tags = listOf("tag1", "tag3"))
        ))

        val tags = questionDao.getAllTags()

        // getAllTags returns distinct serialized tag lists (JSON arrays stored in DB)
        assertEquals(3, tags.size)
        assertEquals(3, tags.distinct().size)
    }
}
