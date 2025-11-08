package com.mtlc.studyplan.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Room entity for storing exam data scraped from ÖSYM website
 */
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Type of exam (YDS, YÖKDİL, e-YDS)
     */
    val examType: String,

    /**
     * Full exam name (e.g., "YDS 2025/1", "YÖKDİL 2025/2")
     */
    val examName: String,

    /**
     * Exam date in epoch days (LocalDate serialized)
     */
    val examDateEpochDay: Long,

    /**
     * Registration start date in epoch days (null if not available)
     */
    val registrationStartEpochDay: Long?,

    /**
     * Registration end date in epoch days (null if not available)
     */
    val registrationEndEpochDay: Long?,

    /**
     * Late registration end date in epoch days (null if not available)
     */
    val lateRegistrationEndEpochDay: Long?,

    /**
     * Result announcement date in epoch days (null if not available)
     */
    val resultDateEpochDay: Long?,

    /**
     * ÖSYM application URL for this exam
     */
    val applicationUrl: String,

    /**
     * Timestamp when this data was scraped/updated
     */
    val scrapedAtMillis: Long = System.currentTimeMillis(),

    /**
     * Whether user has been notified about this exam
     */
    val notified: Boolean = false,
) {
    /**
     * Convert exam date from epoch days to LocalDate
     */
    fun getExamDate(): LocalDate = LocalDate.ofEpochDay(examDateEpochDay)

    /**
     * Convert registration start from epoch days to LocalDate
     */
    fun getRegistrationStart(): LocalDate? = registrationStartEpochDay?.let { LocalDate.ofEpochDay(it) }

    /**
     * Convert registration end from epoch days to LocalDate
     */
    fun getRegistrationEnd(): LocalDate? = registrationEndEpochDay?.let { LocalDate.ofEpochDay(it) }

    /**
     * Convert late registration end from epoch days to LocalDate
     */
    fun getLateRegistrationEnd(): LocalDate? = lateRegistrationEndEpochDay?.let { LocalDate.ofEpochDay(it) }

    /**
     * Convert result date from epoch days to LocalDate
     */
    fun getResultDate(): LocalDate? = resultDateEpochDay?.let { LocalDate.ofEpochDay(it) }
}
