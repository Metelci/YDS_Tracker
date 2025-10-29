package com.mtlc.studyplan.repository

import com.mtlc.studyplan.database.dao.ExamDao
import com.mtlc.studyplan.database.entity.ExamEntity
import com.mtlc.studyplan.network.OsymExamScraper
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing exam data
 * Combines local database storage with web scraping from ÖSYM
 */
@Singleton
class ExamRepository @Inject constructor(
    private val examDao: ExamDao,
    private val osymScraper: OsymExamScraper,
) {

    /**
     * Get all exams from database
     */
    fun getAllExams(): Flow<List<ExamEntity>> = examDao.getAllExams()

    /**
     * Get upcoming exams (exam date >= today)
     */
    fun getUpcomingExams(): Flow<List<ExamEntity>> {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return examDao.getUpcomingExams(todayEpochDay)
    }

    /**
     * Get exams by type (YDS, YÖKDİL, e-YDS)
     */
    fun getExamsByType(examType: String): Flow<List<ExamEntity>> =
        examDao.getExamsByType(examType)

    /**
     * Get upcoming exams by type
     */
    fun getUpcomingExamsByType(examType: String): Flow<List<ExamEntity>> {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return examDao.getUpcomingExamsByType(examType, todayEpochDay)
    }

    /**
     * Get the next upcoming exam (any type)
     */
    suspend fun getNextExam(): ExamEntity? {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return examDao.getNextExam(todayEpochDay)
    }

    /**
     * Get exam by ID
     */
    suspend fun getExamById(examId: Long): ExamEntity? =
        examDao.getExamById(examId)

    /**
     * Insert a single exam
     */
    suspend fun insertExam(exam: ExamEntity): Long =
        examDao.insertExam(exam)

    /**
     * Insert multiple exams
     */
    suspend fun insertExams(exams: List<ExamEntity>) =
        examDao.insertExams(exams)

    /**
     * Update an exam
     */
    suspend fun updateExam(exam: ExamEntity) =
        examDao.updateExam(exam)

    /**
     * Mark exam as notified
     */
    suspend fun markAsNotified(examId: Long) =
        examDao.markAsNotified(examId)

    /**
     * Get exams that haven't been notified yet
     */
    suspend fun getUnnotifiedExams(): List<ExamEntity> {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return examDao.getUnnotifiedExams(todayEpochDay)
    }

    /**
     * Sync exams from ÖSYM website
     * Fetches latest exam data and updates database
     *
     * @return Result indicating success/failure with number of exams synced
     */
    suspend fun syncExamsFromOsym(): Result<Int> {
        return try {
            // Fetch exams from ÖSYM website
            val scrapedExams = osymScraper.fetchAllExams().getOrThrow()

            // Convert ScrapedExamData to ExamEntity
            val examEntities = scrapedExams.map { scraped ->
                ExamEntity(
                    examType = scraped.examType,
                    examName = scraped.examName,
                    examDateEpochDay = scraped.examDate.toEpochDay(),
                    registrationStartEpochDay = scraped.registrationStart?.toEpochDay(),
                    registrationEndEpochDay = scraped.registrationEnd?.toEpochDay(),
                    lateRegistrationEndEpochDay = scraped.lateRegistrationEnd?.toEpochDay(),
                    resultDateEpochDay = scraped.resultDate?.toEpochDay(),
                    applicationUrl = scraped.applicationUrl,
                    scrapedAtMillis = scraped.scrapedAt,
                    notified = false,
                )
            }

            // Insert exams into database (will replace if exists)
            examDao.insertExams(examEntities)

            // Clean up old exams (older than 30 days ago)
            val cutoffDate = LocalDate.now().minusDays(30)
            examDao.deleteOldExams(cutoffDate.toEpochDay())

            Result.success(examEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if ÖSYM scraper is accessible (network connectivity test)
     */
    suspend fun testConnection(): Boolean =
        osymScraper.testConnection()

    /**
     * Get exam count
     */
    suspend fun getExamCount(): Int =
        examDao.getExamCount()

    /**
     * Delete all exams (for testing or reset)
     */
    suspend fun deleteAllExams() =
        examDao.deleteAllExams()
}
