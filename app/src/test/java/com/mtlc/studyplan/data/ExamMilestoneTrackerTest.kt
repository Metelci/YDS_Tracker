package com.mtlc.studyplan.data

import com.mtlc.studyplan.R
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

/**
 * Comprehensive tests for ExamMilestoneTracker data models
 * Validates milestone days, study phases, and string resource mappings
 */
class ExamMilestoneTrackerTest {

    @Test
    fun `milestone days list contains all five milestones`() {
        val expectedDays = listOf(90, 60, 30, 14, 7)
        assertEquals(expectedDays, ExamMilestone.MILESTONE_DAYS)
        assertEquals(5, ExamMilestone.MILESTONE_DAYS.size)
    }

    @Test
    fun `fromDaysRemaining returns correct milestone for 90 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(90, "YDS 2025/1", examDate)

        assertNotNull(milestone)
        assertEquals(90, milestone!!.daysUntil)
        assertEquals("YDS 2025/1", milestone.examName)
        assertEquals(examDate, milestone.examDate)
        assertEquals(StudyPhase.FOUNDATION, milestone.studyPhase)
    }

    @Test
    fun `fromDaysRemaining returns correct milestone for 60 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(60, "YDS 2025/1", examDate)

        assertNotNull(milestone)
        assertEquals(60, milestone!!.daysUntil)
        assertEquals("YDS 2025/1", milestone.examName)
        assertEquals(examDate, milestone.examDate)
        assertEquals(StudyPhase.INTERMEDIATE, milestone.studyPhase)
    }

    @Test
    fun `fromDaysRemaining returns correct milestone for 30 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(30, "YDS 2025/1", examDate)

        assertNotNull(milestone)
        assertEquals(30, milestone!!.daysUntil)
        assertEquals("YDS 2025/1", milestone.examName)
        assertEquals(examDate, milestone.examDate)
        assertEquals(StudyPhase.ADVANCED, milestone.studyPhase)
    }

    @Test
    fun `fromDaysRemaining returns correct milestone for 14 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(14, "YDS 2025/1", examDate)

        assertNotNull(milestone)
        assertEquals(14, milestone!!.daysUntil)
        assertEquals("YDS 2025/1", milestone.examName)
        assertEquals(examDate, milestone.examDate)
        assertEquals(StudyPhase.FINAL_PREP, milestone.studyPhase)
    }

    @Test
    fun `fromDaysRemaining returns correct milestone for 7 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(7, "YDS 2025/1", examDate)

        assertNotNull(milestone)
        assertEquals(7, milestone!!.daysUntil)
        assertEquals("YDS 2025/1", milestone.examName)
        assertEquals(examDate, milestone.examDate)
        assertEquals(StudyPhase.LAST_WEEK, milestone.studyPhase)
    }

    @Test
    fun `fromDaysRemaining returns null for non-milestone days`() {
        val examDate = LocalDate.of(2025, 7, 5)

        assertNull(ExamMilestone.fromDaysRemaining(89, "YDS 2025/1", examDate))
        assertNull(ExamMilestone.fromDaysRemaining(59, "YDS 2025/1", examDate))
        assertNull(ExamMilestone.fromDaysRemaining(45, "YDS 2025/1", examDate))
        assertNull(ExamMilestone.fromDaysRemaining(15, "YDS 2025/1", examDate))
        assertNull(ExamMilestone.fromDaysRemaining(8, "YDS 2025/1", examDate))
        assertNull(ExamMilestone.fromDaysRemaining(1, "YDS 2025/1", examDate))
    }

    @Test
    fun `study phase is FOUNDATION for 90 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(90, "YDS 2025/1", examDate)

        assertEquals(StudyPhase.FOUNDATION, milestone!!.studyPhase)
    }

    @Test
    fun `study phase is INTERMEDIATE for 60 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(60, "YDS 2025/1", examDate)

        assertEquals(StudyPhase.INTERMEDIATE, milestone!!.studyPhase)
    }

    @Test
    fun `study phase is ADVANCED for 30 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(30, "YDS 2025/1", examDate)

        assertEquals(StudyPhase.ADVANCED, milestone!!.studyPhase)
    }

    @Test
    fun `study phase is FINAL_PREP for 14 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(14, "YDS 2025/1", examDate)

        assertEquals(StudyPhase.FINAL_PREP, milestone!!.studyPhase)
    }

    @Test
    fun `study phase is LAST_WEEK for 7 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(7, "YDS 2025/1", examDate)

        assertEquals(StudyPhase.LAST_WEEK, milestone!!.studyPhase)
    }

    @Test
    fun `milestone has correct string resource IDs for 90 days`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val milestone = ExamMilestone.fromDaysRemaining(90, "YDS 2025/1", examDate)

        assertEquals(R.string.exam_milestone_90_title, milestone!!.titleResId)
        assertEquals(R.string.exam_milestone_90_message, milestone.messageResId)
        assertEquals(R.string.exam_milestone_90_phase, milestone.phaseResId)
        assertEquals(R.string.exam_milestone_90_action, milestone.actionResId)
    }

    @Test
    fun `milestone includes exam name and date`() {
        val examDate = LocalDate.of(2025, 7, 5)
        val examName = "YDS 2025/1 (İngilizce)"
        val milestone = ExamMilestone.fromDaysRemaining(60, examName, examDate)

        assertNotNull(milestone)
        assertEquals(examName, milestone!!.examName)
        assertEquals(examDate, milestone.examDate)
    }

    @Test
    fun `all milestone days are positive integers`() {
        ExamMilestone.MILESTONE_DAYS.forEach { days ->
            assertTrue("Milestone day $days should be positive", days > 0)
        }
    }
}
