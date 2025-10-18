package com.mtlc.studyplan.data

import com.mtlc.studyplan.network.OsymExamCalendarClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Service for managing YDS exam information.
 *
 * The service keeps a cached copy of the official ÖSYM exam schedule.
 * When network retrieval fails, static fallback data is used so the UI
 * remains functional offline.
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

    private val fallbackSessions = listOf(
        YdsExamSession(
            name = "YDS 2025/1 (İngilizce)",
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
        )
    )

    @Volatile
    private var cachedSessions: List<YdsExamSession> = emptyList()

    @Volatile
    private var lastSuccessfulFetchMillis: Long = 0L

    private val cacheMutex = Mutex()

    private fun activeSessions(): List<YdsExamSession> {
        return cachedSessions.takeIf { it.isNotEmpty() } ?: fallbackSessions
    }

    /**
     * Attempt to refresh sessions from ÖSYM.
     *
     * @return true if a fresh schedule was downloaded and applied.
     */
    suspend fun refreshFromNetwork(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        if (!force && cachedSessions.isNotEmpty()) {
            val ageMs = now - lastSuccessfulFetchMillis
            if (ageMs < TimeUnit.HOURS.toMillis(6)) {
                return false
            }
        }

        return cacheMutex.withLock {
            val stillNeedsFetch =
                force || cachedSessions.isEmpty() ||
                    now - lastSuccessfulFetchMillis >= TimeUnit.HOURS.toMillis(6)
            if (!stillNeedsFetch) {
                return@withLock false
            }

            val fetched = runCatching { OsymExamCalendarClient.fetchYdsExams() }
                .getOrElse { emptyList() }
            val today = LocalDate.now()
            val hasUpcoming = fetched.any { it.examDate.isAfter(today) || it.examDate.isEqual(today) }
            if (hasUpcoming) {
                cachedSessions = fetched
                    .distinctBy { it.examDate }
                    .sortedBy { it.examDate }
                lastSuccessfulFetchMillis = System.currentTimeMillis()
                true
            } else {
                false
            }
    }

    /**
     * Replace the cached sessions with a pre-fetched list.
     */
    suspend fun applySessions(sessions: List<YdsExamSession>) {
        if (sessions.isEmpty()) return
        val today = LocalDate.now()
        if (!sessions.any { it.examDate.isAfter(today) || it.examDate.isEqual(today) }) return
        cacheMutex.withLock {
            cachedSessions = sessions
                .distinctBy { it.examDate }
                .sortedBy { it.examDate }
            lastSuccessfulFetchMillis = System.currentTimeMillis()
        }
    }
    }

    fun getNextExam(): YdsExamSession? {
        val today = LocalDate.now()
        return activeSessions()
            .filter { it.examDate.isAfter(today) || it.examDate.isEqual(today) }
            .minByOrNull { it.examDate }
    }

    fun getAllUpcomingExams(): List<YdsExamSession> {
        val today = LocalDate.now()
        return activeSessions()
            .filter { it.examDate.isAfter(today) || it.examDate.isEqual(today) }
            .sortedBy { it.examDate }
    }

    fun getDaysToNextExam(): Int {
        val nextExam = getNextExam()
        return if (nextExam != null) {
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, nextExam.examDate).toInt()
        } else {
            0
        }
    }

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

    fun getFormattedExamDate(): String {
        val nextExam = getNextExam()
        return if (nextExam != null) {
            val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
            nextExam.examDate.format(formatter)
        } else {
            "No upcoming exam"
        }
    }

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

