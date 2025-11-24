@file:Suppress("TooManyFunctions", "CyclomaticComplexMethod")
package com.mtlc.studyplan.repository

import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val database: StudyPlanDatabase
) {
    private val questionDao = database.questionDao()

    // Basic CRUD operations
    suspend fun insertQuestion(question: QuestionEntity) {
        questionDao.insertQuestion(question)
    }

    suspend fun insertQuestions(questions: List<QuestionEntity>) {
        questionDao.insertQuestions(questions)
    }

    suspend fun updateQuestion(question: QuestionEntity) {
        questionDao.updateQuestion(question)
    }

    suspend fun deleteQuestion(question: QuestionEntity) {
        questionDao.deleteQuestion(question)
    }

    suspend fun getQuestionById(questionId: String): QuestionEntity? {
        return questionDao.getQuestionById(questionId)
    }

    // Flow-based queries for reactive UI updates
    fun getAllQuestions(): Flow<List<QuestionEntity>> {
        return questionDao.getAllQuestions()
    }

    fun getQuestionsByExamType(examType: ExamType): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsByExamType(examType)
    }

    fun getQuestionsByType(questionType: QuestionType): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsByType(questionType)
    }

    fun getQuestionsByDifficulty(difficulty: Difficulty): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsByDifficulty(difficulty)
    }

    fun getQuestionsByExamAndType(examType: ExamType, questionType: QuestionType): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsByExamAndType(examType, questionType)
    }

    fun getQuestionsByExamAndDifficulty(examType: ExamType, difficulty: Difficulty): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsByExamAndDifficulty(examType, difficulty)
    }

    fun getDownloadedQuestions(): Flow<List<QuestionEntity>> {
        return questionDao.getDownloadedQuestions()
    }

    // Analytics and statistics
    suspend fun getTotalQuestionCount(): Int {
        return questionDao.getTotalQuestionCount()
    }

    suspend fun getQuestionCountByExamType(examType: ExamType): Int {
        return questionDao.getQuestionCountByExamType(examType)
    }

    suspend fun getQuestionCountByType(questionType: QuestionType): Int {
        return questionDao.getQuestionCountByType(questionType)
    }

    suspend fun getQuestionCountByDifficulty(difficulty: Difficulty): Int {
        return questionDao.getQuestionCountByDifficulty(difficulty)
    }

    suspend fun getDownloadedQuestionCount(): Int {
        return questionDao.getDownloadedQuestionCount()
    }

    // Download management
    suspend fun markQuestionAsDownloaded(questionId: String) {
        questionDao.updateDownloadStatus(questionId, true)
        questionDao.updateLastAccessed(questionId, System.currentTimeMillis())
    }

    suspend fun markQuestionAsNotDownloaded(questionId: String) {
        questionDao.updateDownloadStatus(questionId, false)
    }

    suspend fun updateLastAccessed(questionId: String) {
        questionDao.updateLastAccessed(questionId, System.currentTimeMillis())
    }

    // Bulk operations
    suspend fun deleteNonDownloadedQuestions() {
        questionDao.deleteNonDownloadedQuestions()
    }

    // Search functionality
    fun searchQuestions(searchTerm: String): Flow<List<QuestionEntity>> {
        return questionDao.searchQuestions(searchTerm)
    }

    // Tag management
    suspend fun getAllTags(): List<String> {
        return questionDao.getAllTags()
    }

    fun getQuestionsByTag(tag: String): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsByTag(tag)
    }

    // Advanced filtering with multiple criteria
    fun getFilteredQuestions(
        examType: ExamType? = null,
        questionType: QuestionType? = null,
        difficulty: Difficulty? = null,
        downloadedOnly: Boolean = false,
        tag: String? = null
    ): Flow<List<QuestionEntity>> {
        val baseFlow = when {
            examType != null && questionType != null -> getQuestionsByExamAndType(examType, questionType)
            examType != null && difficulty != null -> getQuestionsByExamAndDifficulty(examType, difficulty)
            examType != null -> getQuestionsByExamType(examType)
            questionType != null -> getQuestionsByType(questionType)
            difficulty != null -> getQuestionsByDifficulty(difficulty)
            tag != null -> getQuestionsByTag(tag)
            downloadedOnly -> getDownloadedQuestions()
            else -> getAllQuestions()
        }

        return baseFlow.map { questions ->
            questions.filter { question ->
                // Apply additional filters if needed
                val examFilter = examType == null || question.examType == examType
                val typeFilter = questionType == null || question.questionType == questionType
                val difficultyFilter = difficulty == null || question.difficulty == difficulty
                val downloadFilter = !downloadedOnly || question.isDownloaded
                val tagFilter = tag == null || question.tags.contains(tag)

                examFilter && typeFilter && difficultyFilter && downloadFilter && tagFilter
            }
        }
    }

    // Utility methods for UI
    suspend fun getQuestionStats(): QuestionStats {
        val total = getTotalQuestionCount()
        val downloaded = getDownloadedQuestionCount()
        val ydsCount = getQuestionCountByExamType(ExamType.YDS)
        val yokdilCount = getQuestionCountByExamType(ExamType.YOKDIL)

        return QuestionStats(
            totalQuestions = total,
            downloadedQuestions = downloaded,
            ydsQuestions = ydsCount,
            yokdilQuestions = yokdilCount
        )
    }
}

data class QuestionStats(
    val totalQuestions: Int,
    val downloadedQuestions: Int,
    val ydsQuestions: Int,
    val yokdilQuestions: Int
)