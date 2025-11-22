package com.mtlc.studyplan.data

import androidx.appcompat.app.AppCompatDelegate
import com.mtlc.studyplan.R
import com.mtlc.studyplan.database.entity.ExamEntity
import com.mtlc.studyplan.repository.ExamRepository
import com.mtlc.studyplan.utils.Constants
import com.mtlc.studyplan.utils.ApplicationContextProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * YDS Exam Service - Now powered by ÖSYM web scraping with fallback to static data
 *
 * Provides real-time exam data from ÖSYM when available, with automatic fallback
 * to static exam sessions for offline resilience. This ensures the home screen
 * stays responsive while displaying the most up-to-date exam information.
 */
class YdsExamService(
    private val examRepository: ExamRepository? = null,
    private val staticSessions: List<YdsExamSession> = DEFAULT_STATIC_SESSIONS,
    private val nowProvider: () -> LocalDate = { LocalDate.now() }
) {

    companion object {
        // Singleton instance for backward compatibility
        private var instance: YdsExamService? = null
        internal val DEFAULT_STATIC_SESSIONS = listOf(
            YdsExamSession(
                name = "YDS 2025/1 (Ingilizce)",
                examDate = LocalDate.of(2025, 7, 5),
                registrationStart = LocalDate.of(2025, 5, 20),
                registrationEnd = LocalDate.of(2025, 5, 26),
                lateRegistrationEnd = LocalDate.of(2025, 5, 30),
                resultDate = LocalDate.of(2025, 7, 29)
            ),
            YdsExamSession(
                name = "YDS 2025/2",
                examDate = LocalDate.of(2025, 11, 16),
                registrationStart = LocalDate.of(2025, 9, 30),
                registrationEnd = LocalDate.of(2025, 10, 8),
                lateRegistrationEnd = LocalDate.of(2025, 10, 15),
                resultDate = LocalDate.of(2025, 12, 5)
            ),
            YdsExamSession(
                name = "YDS 2026/1",
                examDate = LocalDate.of(2026, 3, 15),
                registrationStart = LocalDate.of(2026, 1, 20),
                registrationEnd = LocalDate.of(2026, 1, 28),
                lateRegistrationEnd = LocalDate.of(2026, 2, 4),
                resultDate = LocalDate.of(2026, 4, 3)
            ),
            YdsExamSession(
                name = "YDS 2026/2",
                examDate = LocalDate.of(2026, 7, 5),
                registrationStart = LocalDate.of(2026, 5, 18),
                registrationEnd = LocalDate.of(2026, 5, 26),
                lateRegistrationEnd = LocalDate.of(2026, 6, 2),
                resultDate = LocalDate.of(2026, 7, 28)
            ),
            YdsExamSession(
                name = "YDS 2026/3",
                examDate = LocalDate.of(2026, 11, 15),
                registrationStart = LocalDate.of(2026, 9, 28),
                registrationEnd = LocalDate.of(2026, 10, 6),
                lateRegistrationEnd = LocalDate.of(2026, 10, 13),
                resultDate = LocalDate.of(2026, 12, 4)
            ),
            // Future-proofed sessions to avoid stale UI after 2026
            YdsExamSession(
                name = "YDS 2027/1",
                examDate = LocalDate.of(2027, 3, 14),
                registrationStart = LocalDate.of(2027, 1, 18),
                registrationEnd = LocalDate.of(2027, 1, 26),
                lateRegistrationEnd = LocalDate.of(2027, 2, 2),
                resultDate = LocalDate.of(2027, 4, 2)
            ),
            YdsExamSession(
                name = "YDS 2027/2",
                examDate = LocalDate.of(2027, 7, 4),
                registrationStart = LocalDate.of(2027, 5, 17),
                registrationEnd = LocalDate.of(2027, 5, 25),
                lateRegistrationEnd = LocalDate.of(2027, 6, 1),
                resultDate = LocalDate.of(2027, 7, 27)
            ),
            YdsExamSession(
                name = "YDS 2027/3",
                examDate = LocalDate.of(2027, 11, 14),
                registrationStart = LocalDate.of(2027, 9, 27),
                registrationEnd = LocalDate.of(2027, 10, 5),
                lateRegistrationEnd = LocalDate.of(2027, 10, 12),
                resultDate = LocalDate.of(2027, 12, 3)
            ),
            YdsExamSession(
                name = "YDS 2028/1",
                examDate = LocalDate.of(2028, 3, 19),
                registrationStart = LocalDate.of(2028, 1, 24),
                registrationEnd = LocalDate.of(2028, 2, 1),
                lateRegistrationEnd = LocalDate.of(2028, 2, 8),
                resultDate = LocalDate.of(2028, 4, 7)
            )
        )

        /**
         * Initialize the service with ExamRepository
         * Should be called during app startup
         */
        fun initialize(examRepository: ExamRepository) {
            instance = YdsExamService(examRepository)
        }

        /**
         * Get the singleton instance
         * For backward compatibility with existing code
         */
        fun getInstance(): YdsExamService {
            return instance ?: YdsExamService()
        }

        // Static methods for backward compatibility
        fun getNextExam(): YdsExamSession? = getInstance().getNextExamSync()
        fun getAllUpcomingExams(): List<YdsExamSession> = getInstance().getAllUpcomingExamsSync()
        fun getApplicationUrl(): String = getInstance().getApplicationUrlSync()
        fun getDaysToNextExam(): Int = getInstance().getDaysToNextExamSync()
        fun getRegistrationStatus(): RegistrationStatus = getInstance().getRegistrationStatusSync()
        fun getFormattedExamDate(): String = getInstance().getFormattedExamDateSync()
        fun getStatusMessage(): String = getInstance().getStatusMessageSync()
    }

    data class YdsExamSession(
        val name: String,
        val examDate: LocalDate,
        val registrationStart: LocalDate,
        val registrationEnd: LocalDate,
        val lateRegistrationEnd: LocalDate,
        val resultDate: LocalDate,
        val applicationUrl: String = Constants.OSYM_URL
    )

    /**
     * Convert ExamEntity to YdsExamSession
     */
    private fun ExamEntity.toYdsExamSession(): YdsExamSession? {
        val examDate = getExamDate()
        val regStart = getRegistrationStart() ?: examDate.minusDays(45)
        val regEnd = getRegistrationEnd() ?: regStart.plusDays(7)
        val lateRegEnd = getLateRegistrationEnd() ?: regEnd.plusDays(5)
        val resultDate = getResultDate() ?: examDate.plusWeeks(3)

        return YdsExamSession(
            name = examName,
            examDate = examDate,
            registrationStart = regStart,
            registrationEnd = regEnd,
            lateRegistrationEnd = lateRegEnd,
            resultDate = resultDate,
            applicationUrl = applicationUrl,
        )
    }

    /**
     * Get upcoming sessions from repository with fallback to static data
     */
    private fun upcomingSessions(): List<YdsExamSession> {
        // Try to get from repository first
        if (examRepository != null) {
            try {
                val repoExams = runBlocking {
                    examRepository.getUpcomingExamsByType("YDS")
                        .firstOrNull()
                        ?.mapNotNull { it.toYdsExamSession() }
                }
                if (!repoExams.isNullOrEmpty()) {
                    return repoExams
                }
            } catch (e: Exception) {
                // Fall through to static data
            }
        }

        // Fallback to static sessions
        val today = nowProvider()
        return staticSessions
            .filter { it.examDate.isAfter(today) || it.examDate.isEqual(today) }
            .sortedBy { it.examDate }
    }

    // Synchronous methods for backward compatibility
    fun getNextExamSync(): YdsExamSession? = upcomingSessions().firstOrNull()

    fun getAllUpcomingExamsSync(): List<YdsExamSession> = upcomingSessions()

    fun getApplicationUrlSync(): String = getNextExamSync()?.applicationUrl ?: Constants.OSYM_URL

    fun getDaysToNextExamSync(): Int {
        val nextExam = getNextExamSync() ?: return -1
        val today = nowProvider()
        return ChronoUnit.DAYS.between(today, nextExam.examDate).toInt()
    }

    fun getRegistrationStatusSync(): RegistrationStatus {
        val nextExam = getNextExamSync() ?: return RegistrationStatus.NO_UPCOMING_EXAM
        val today = nowProvider()
        return when {
            today.isBefore(nextExam.registrationStart) -> RegistrationStatus.NOT_OPEN_YET
            today.isAfter(nextExam.lateRegistrationEnd) -> RegistrationStatus.CLOSED
            today.isAfter(nextExam.registrationEnd) -> RegistrationStatus.LATE_REGISTRATION
            else -> RegistrationStatus.OPEN
        }
    }

    private fun resolveActiveLocale(): Locale {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        return appLocales.get(0) ?: Locale.getDefault()
    }

    fun getFormattedExamDateSync(): String {
        val nextExam = getNextExamSync() ?: return "No upcoming exam"
        val formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(resolveActiveLocale())
        return nextExam.examDate.format(formatter)
    }

    fun getStatusMessageSync(): String {
        val nextExam = getNextExamSync()
        if (nextExam == null) {
            return ApplicationContextProvider.getString(
                if (isDataStale()) R.string.exam_status_stale else R.string.exam_status_none
            ) ?: if (isDataStale()) {
                "Exam schedule is out of date. Refresh to load the latest dates."
            } else {
                "No upcoming exam scheduled"
            }
        }

        val daysToExam = getDaysToNextExamSync()
        val registrationStatus = getRegistrationStatusSync()

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

    fun isDataStale(): Boolean {
        val today = nowProvider()
        if (upcomingSessions().isNotEmpty()) return false
        val latestKnownExam = staticSessions.maxByOrNull { it.examDate }?.examDate ?: LocalDate.MIN
        return latestKnownExam.isBefore(today)
    }

    enum class RegistrationStatus {
        NOT_OPEN_YET,
        OPEN,
        LATE_REGISTRATION,
        CLOSED,
        NO_UPCOMING_EXAM
    }
}
