package com.mtlc.studyplan.database.dao

import androidx.room.*
import com.mtlc.studyplan.database.entities.Difficulty
import com.mtlc.studyplan.database.entities.ExamType
import com.mtlc.studyplan.database.entities.QuestionEntity
import com.mtlc.studyplan.database.entities.QuestionType
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    @Query("SELECT * FROM questions ORDER BY lastAccessed DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE examType = :examType")
    fun getQuestionsByExamType(examType: ExamType): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE questionType = :questionType")
    fun getQuestionsByType(questionType: QuestionType): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE difficulty = :difficulty")
    fun getQuestionsByDifficulty(difficulty: Difficulty): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE examType = :examType AND questionType = :questionType")
    fun getQuestionsByExamAndType(examType: ExamType, questionType: QuestionType): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE examType = :examType AND difficulty = :difficulty")
    fun getQuestionsByExamAndDifficulty(examType: ExamType, difficulty: Difficulty): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE isDownloaded = 1")
    fun getDownloadedQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalQuestionCount(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE examType = :examType")
    suspend fun getQuestionCountByExamType(examType: ExamType): Int

    @Query("SELECT COUNT(*) FROM questions WHERE questionType = :questionType")
    suspend fun getQuestionCountByType(questionType: QuestionType): Int

    @Query("SELECT COUNT(*) FROM questions WHERE difficulty = :difficulty")
    suspend fun getQuestionCountByDifficulty(difficulty: Difficulty): Int

    @Query("SELECT COUNT(*) FROM questions WHERE isDownloaded = 1")
    suspend fun getDownloadedQuestionCount(): Int

    @Query("UPDATE questions SET isDownloaded = :downloaded WHERE id = :questionId")
    suspend fun updateDownloadStatus(questionId: String, downloaded: Boolean)

    @Query("UPDATE questions SET lastAccessed = :timestamp WHERE id = :questionId")
    suspend fun updateLastAccessed(questionId: String, timestamp: Long)

    @Query("DELETE FROM questions WHERE isDownloaded = 0")
    suspend fun deleteNonDownloadedQuestions()

    @Query("SELECT * FROM questions WHERE questionText LIKE '%' || :searchTerm || '%' OR explanation LIKE '%' || :searchTerm || '%'")
    fun searchQuestions(searchTerm: String): Flow<List<QuestionEntity>>

    @Query("SELECT DISTINCT tags FROM questions")
    suspend fun getAllTags(): List<String>

    @Query("SELECT * FROM questions WHERE tags LIKE '%' || :tag || '%'")
    fun getQuestionsByTag(tag: String): Flow<List<QuestionEntity>>
}