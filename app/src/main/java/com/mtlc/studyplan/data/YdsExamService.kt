package com.mtlc.studyplan.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Service for managing YDS exam information with real OSYM data
 * Updated from official OSYM website: https://www.osym.gov.tr/tr,8797/takvim.html
 */
object YdsExamService {

    data class YdsExamSession(
        val name: String,
        val examDate: LocalDate,
        val registrationStart: LocalDate,
        val registrationEnd: LocalDate,
        val lateRegistrationEnd: LocalDate,
        val resultDate: LocalDate,
        val applicationUrl: String = "https://ais.osym.gov.tr"
    )

    private val yds2025Sessions = listOf(
        YdsExamSession(
            name = "YDS 2025/1 (İngilizce)",
            examDate = LocalDate.of(2025, 7, 5),
            registrationStart = LocalDate.of(2025, 5, 20),
            registrationEnd = LocalDate.of(2025, 5, 26),
            lateRegistrationEnd = LocalDate.of(2025, 5, 30),
            resultDate = LocalDate.of(2025, 7, 29),
            applicationUrl = "https://ais.osym.gov.tr"
        ),
        YdsExamSession(
            name = "YDS 2025/2",
            examDate = LocalDate.of(2025, 11, 16),
            registrationStart = LocalDate.of(2025, 9, 30),
            registrationEnd = LocalDate.of(2025, 10, 8),
            lateRegistrationEnd = LocalDate.of(2025, 10, 15),
            resultDate = LocalDate.of(2025, 12, 5),
            applicationUrl = "https://ais.osym.gov.tr"
        )
    )

    /**
     * Get the next upcoming YDS exam
     */
    fun getNextExam(): YdsExamSession? {
        val today = LocalDate.now()
        return yds2025Sessions
            .filter { it.examDate.isAfter(today) || it.examDate.isEqual(today) }
            .minByOrNull { it.examDate }
    }

    /**
     * Get all upcoming YDS exams ordered by date
     */
    fun getAllUpcomingExams(): List<YdsExamSession> {
        val today = LocalDate.now()
        return yds2025Sessions
            .filter { it.examDate.isAfter(today) || it.examDate.isEqual(today) }
            .sortedBy { it.examDate }
    }

    /**
     * Calculate days remaining to the next exam
     */
    fun getDaysToNextExam(): Int {
        val nextExam = getNextExam()
        return if (nextExam != null) {
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, nextExam.examDate).toInt()
        } else {
            0 // No upcoming exams
        }
    }

    /**
     * Get registration status for the next exam
     */
    fun getRegistrationStatus(): RegistrationStatus {
        val nextExam = getNextExam() ?: return RegistrationStatus.NO_UPCOMING_EXAM
        val today = LocalDate.now()

        return when {
            today.isBefore(nextExam.registrationStart) -> RegistrationStatus.NOT_OPEN_YET
            today.isAfter(nextExam.lateRegistrationEnd) -> RegistrationStatus.CLOSED
            today.isAfter(nextExam.registrationEnd) -> RegistrationStatus.LATE_REGISTRATION
            else -> RegistrationStatus.OPEN
        }
    }

    /**
     * Get formatted exam date string
     */
    fun getFormattedExamDate(): String {
        val nextExam = getNextExam()
        return if (nextExam != null) {
            val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
            nextExam.examDate.format(formatter)
        } else {
            "No upcoming exam"
        }
    }

    /**
     * Get exam status message based on days remaining
     */
    fun getStatusMessage(): String {
        val daysToExam = getDaysToNextExam()
        val registrationStatus = getRegistrationStatus()

        return when {
            daysToExam == 0 -> "Exam day!"
            daysToExam < 0 -> "Exam completed"
            daysToExam <= 7 -> "Final week!"
            daysToExam <= 30 -> "Almost there!"
            daysToExam <= 90 -> when (registrationStatus) {
                RegistrationStatus.NOT_OPEN_YET -> "Registration opens soon"
                RegistrationStatus.OPEN -> "Registration open!"
                RegistrationStatus.LATE_REGISTRATION -> "Late registration period"
                RegistrationStatus.CLOSED -> "Registration closed"
                RegistrationStatus.NO_UPCOMING_EXAM -> "Preparation time"
            }
            else -> "Long-term planning"
        }
    }

    enum class RegistrationStatus {
        NOT_OPEN_YET,
        OPEN,
        LATE_REGISTRATION,
        CLOSED,
        NO_UPCOMING_EXAM
    }
}