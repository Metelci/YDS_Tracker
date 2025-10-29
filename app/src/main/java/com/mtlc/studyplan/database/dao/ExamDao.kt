package com.mtlc.studyplan.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mtlc.studyplan.database.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for exam data
 */
@Dao
interface ExamDao {

    /**
     * Get all exams, ordered by exam date
     */
    @Query("SELECT * FROM exams ORDER BY examDateEpochDay ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    /**
     * Get all upcoming exams (exam date >= today)
     */
    @Query("SELECT * FROM exams WHERE examDateEpochDay >= :todayEpochDay ORDER BY examDateEpochDay ASC")
    fun getUpcomingExams(todayEpochDay: Long): Flow<List<ExamEntity>>

    /**
     * Get exams by type (YDS, YÖKDİL, e-YDS)
     */
    @Query("SELECT * FROM exams WHERE examType = :examType ORDER BY examDateEpochDay ASC")
    fun getExamsByType(examType: String): Flow<List<ExamEntity>>

    /**
     * Get upcoming exams by type
     */
    @Query("SELECT * FROM exams WHERE examType = :examType AND examDateEpochDay >= :todayEpochDay ORDER BY examDateEpochDay ASC")
    fun getUpcomingExamsByType(examType: String, todayEpochDay: Long): Flow<List<ExamEntity>>

    /**
     * Get the next upcoming exam (any type)
     */
    @Query("SELECT * FROM exams WHERE examDateEpochDay >= :todayEpochDay ORDER BY examDateEpochDay ASC LIMIT 1")
    suspend fun getNextExam(todayEpochDay: Long): ExamEntity?

    /**
     * Get exam by ID
     */
    @Query("SELECT * FROM exams WHERE id = :examId")
    suspend fun getExamById(examId: Long): ExamEntity?

    /**
     * Get exam by name (for duplicate checking)
     */
    @Query("SELECT * FROM exams WHERE examName = :examName LIMIT 1")
    suspend fun getExamByName(examName: String): ExamEntity?

    /**
     * Insert a single exam
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    /**
     * Insert multiple exams
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    /**
     * Update an exam
     */
    @Update
    suspend fun updateExam(exam: ExamEntity)

    /**
     * Mark exam as notified
     */
    @Query("UPDATE exams SET notified = 1 WHERE id = :examId")
    suspend fun markAsNotified(examId: Long)

    /**
     * Get exams that haven't been notified yet
     */
    @Query("SELECT * FROM exams WHERE notified = 0 AND examDateEpochDay >= :todayEpochDay ORDER BY examDateEpochDay ASC")
    suspend fun getUnnotifiedExams(todayEpochDay: Long): List<ExamEntity>

    /**
     * Delete old exams (exam date older than cutoff)
     */
    @Query("DELETE FROM exams WHERE examDateEpochDay < :cutoffEpochDay")
    suspend fun deleteOldExams(cutoffEpochDay: Long): Int

    /**
     * Delete all exams (for testing or reset)
     */
    @Query("DELETE FROM exams")
    suspend fun deleteAllExams()

    /**
     * Get count of all exams
     */
    @Query("SELECT COUNT(*) FROM exams")
    suspend fun getExamCount(): Int
}
