package com.mtlc.studyplan.data

import com.mtlc.studyplan.repository.ExamRepository
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YdsExamServiceTest {

    @Test
    fun `static fallback provides future sessions and is not stale`() {
        val today = LocalDate.of(2025, 5, 1)
        val service = YdsExamService(
            examRepository = null,
            staticSessions = YdsExamService.DEFAULT_STATIC_SESSIONS,
            nowProvider = { today }
        )

        val nextExam = service.getNextExamSync()

        assertNotNull(nextExam)
        assertTrue(nextExam!!.examDate.isAfter(today) || nextExam.examDate.isEqual(today))
        assertFalse(service.isDataStale())
    }

    @Test
    fun `stale state surfaces when no upcoming data exists`() {
        val staleDate = LocalDate.of(2028, 1, 1)
        val staleStatic = listOf(
            YdsExamService.YdsExamSession(
                name = "Legacy YDS 2023/1",
                examDate = LocalDate.of(2023, 3, 1),
                registrationStart = LocalDate.of(2022, 12, 15),
                registrationEnd = LocalDate.of(2023, 1, 5),
                lateRegistrationEnd = LocalDate.of(2023, 1, 12),
                resultDate = LocalDate.of(2023, 3, 30)
            )
        )
        val examRepository = mockk<ExamRepository>()
        every { examRepository.getUpcomingExamsByType(any()) } returns flowOf(emptyList())

        val service = YdsExamService(
            examRepository = examRepository,
            staticSessions = staleStatic,
            nowProvider = { staleDate }
        )

        assertTrue(service.getAllUpcomingExamsSync().isEmpty())
        assertTrue(service.isDataStale())
        assertEquals(
            "Exam schedule is out of date. Refresh to load the latest dates.",
            service.getStatusMessageSync()
        )
    }
}
