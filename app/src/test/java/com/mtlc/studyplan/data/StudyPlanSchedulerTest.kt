package com.mtlc.studyplan.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Unit tests for StudyPlanScheduler.
 *
 * These tests verify the core scheduling algorithms work correctly without
 * any Android dependencies.
 */
class StudyPlanSchedulerTest {

    private lateinit var scheduler: StudyPlanScheduler

    // Test data: A simplified 4-week base plan
    private val testBasePlan = listOf(
        WeekPlan(
            week = 1, month = 1, title = "Week 1: Foundation",
            days = listOf(
                DayPlan("Monday", listOf(PlanTask("w1-t1", "Grammar Units 1-4", "Red Book")), DayOfWeek.MONDAY),
                DayPlan("Tuesday", listOf(PlanTask("w1-t2", "Reading Practice", null)), DayOfWeek.TUESDAY),
                DayPlan("Wednesday", listOf(PlanTask("w1-t3", "Vocabulary", "50 words")), DayOfWeek.WEDNESDAY),
                DayPlan("Thursday", listOf(PlanTask("w1-t4", "Grammar Units 5-8", "Red Book")), DayOfWeek.THURSDAY),
                DayPlan("Friday", listOf(PlanTask("w1-t5", "Listening", "Podcast")), DayOfWeek.FRIDAY),
                DayPlan("Saturday", listOf(PlanTask("w1-t6", "Full Exam Practice", "180 dakika deneme")), DayOfWeek.SATURDAY),
                DayPlan("Sunday", listOf(PlanTask("w1-t7", "Review", null)), DayOfWeek.SUNDAY)
            )
        ),
        WeekPlan(
            week = 2, month = 1, title = "Week 2: Foundation",
            days = listOf(
                DayPlan("Monday", listOf(PlanTask("w2-t1", "Grammar Units 9-12", "Red Book")), DayOfWeek.MONDAY),
                DayPlan("Tuesday", listOf(PlanTask("w2-t2", "Reading Advanced", null)), DayOfWeek.TUESDAY)
            )
        ),
        WeekPlan(
            week = 3, month = 1, title = "Week 3: Development",
            days = listOf(
                DayPlan("Monday", listOf(PlanTask("w3-t1", "Blue Book Ch 1", null)), DayOfWeek.MONDAY)
            )
        ),
        WeekPlan(
            week = 4, month = 1, title = "Week 4: Development",
            days = listOf(
                DayPlan("Monday", listOf(PlanTask("w4-t1", "Blue Book Ch 2", null)), DayOfWeek.MONDAY)
            )
        )
    )

    @Before
    fun setup() {
        scheduler = StudyPlanScheduler()
    }

    // --- adaptPlanDuration tests ---

    @Test
    fun `adaptPlanDuration with same duration returns same number of weeks`() {
        val result = scheduler.adaptPlanDuration(testBasePlan, 4)

        assertEquals(4, result.size)
        assertEquals(1, result[0].weekNumber)
        assertEquals(4, result[3].weekNumber)
    }

    @Test
    fun `adaptPlanDuration stretches plan correctly`() {
        // Stretch 4-week plan to 8 weeks
        val result = scheduler.adaptPlanDuration(testBasePlan, 8)

        assertEquals(8, result.size)
        // First and last weeks should map to original first and last
        assertEquals(1, result[0].weekNumber)
        assertEquals(8, result[7].weekNumber)

        // Check that tasks are remapped with new week IDs
        val firstTask = result[0].days.firstOrNull()?.tasks?.firstOrNull()
        assertTrue(firstTask?.id?.startsWith("w1-") == true)

        val lastTask = result[7].days.firstOrNull()?.tasks?.firstOrNull()
        assertTrue(lastTask?.id?.startsWith("w8-") == true)
    }

    @Test
    fun `adaptPlanDuration compresses plan correctly`() {
        // Compress 4-week plan to 2 weeks
        val result = scheduler.adaptPlanDuration(testBasePlan, 2)

        assertEquals(2, result.size)
        assertEquals(1, result[0].weekNumber)
        assertEquals(2, result[1].weekNumber)
    }

    @Test
    fun `adaptPlanDuration handles single week correctly`() {
        val result = scheduler.adaptPlanDuration(testBasePlan, 1)

        assertEquals(1, result.size)
        assertEquals(1, result[0].weekNumber)
    }

    @Test
    fun `adaptPlanDuration handles empty base plan`() {
        val result = scheduler.adaptPlanDuration(emptyList(), 10)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `adaptPlanDuration assigns correct phases`() {
        // Use a 30-week plan to test phase progression
        val result = scheduler.adaptPlanDuration(PlanDataSource.planData, 30)

        // First 8 weeks should be FOUNDATION
        assertEquals(StudyPlanScheduler.Phase.FOUNDATION, result[0].phase)
        assertEquals(StudyPlanScheduler.Phase.FOUNDATION, result[7].phase)

        // Weeks 9-18 should be DEVELOPMENT
        assertEquals(StudyPlanScheduler.Phase.DEVELOPMENT, result[8].phase)
        assertEquals(StudyPlanScheduler.Phase.DEVELOPMENT, result[17].phase)

        // Weeks 19-26 should be ADVANCED
        assertEquals(StudyPlanScheduler.Phase.ADVANCED, result[18].phase)
        assertEquals(StudyPlanScheduler.Phase.ADVANCED, result[25].phase)

        // Weeks 27+ should be EXAM_CAMP
        assertEquals(StudyPlanScheduler.Phase.EXAM_CAMP, result[26].phase)
        assertEquals(StudyPlanScheduler.Phase.EXAM_CAMP, result[29].phase)
    }

    // --- alignStartWeekday tests ---

    @Test
    fun `alignStartWeekday does not modify plan when starting on Monday`() {
        val adapted = scheduler.adaptPlanDuration(testBasePlan, 4)
        val monday = LocalDate.of(2025, 1, 6) // A Monday

        val result = scheduler.alignStartWeekday(adapted, monday)

        assertEquals(adapted.size, result.size)
        assertEquals(adapted[0].days.size, result[0].days.size)
    }

    @Test
    fun `alignStartWeekday drops early days when starting on Wednesday`() {
        val adapted = scheduler.adaptPlanDuration(testBasePlan, 4)
        val wednesday = LocalDate.of(2025, 1, 8) // A Wednesday

        val result = scheduler.alignStartWeekday(adapted, wednesday)

        assertEquals(4, result.size)
        // First week should have 2 fewer days (Mon, Tue dropped)
        assertEquals(adapted[0].days.size - 2, result[0].days.size)
        // First day should be Wednesday
        assertEquals(DayOfWeek.WEDNESDAY, result[0].days.firstOrNull()?.dayOfWeek)
    }

    @Test
    fun `alignStartWeekday handles empty plan`() {
        val result = scheduler.alignStartWeekday(emptyList(), LocalDate.now())

        assertTrue(result.isEmpty())
    }

    // --- alignToEndDate tests ---

    @Test
    fun `alignToEndDate returns unchanged plan when no end date`() {
        val adapted = scheduler.adaptPlanDuration(testBasePlan, 4)
        val startDate = LocalDate.of(2025, 1, 6)

        val result = scheduler.alignToEndDate(adapted, startDate, null)

        assertEquals(adapted.size, result.size)
    }

    @Test
    fun `alignToEndDate trims plan to specified days`() {
        val adapted = scheduler.adaptPlanDuration(testBasePlan, 4)
        val startDate = LocalDate.of(2025, 1, 6)
        val endDate = LocalDate.of(2025, 1, 10) // 5 days total

        val result = scheduler.alignToEndDate(adapted, startDate, endDate)

        val totalDays = result.sumOf { it.days.size }
        assertEquals(5, totalDays)
    }

    @Test
    fun `alignToEndDate returns empty plan when end is before start`() {
        val adapted = scheduler.adaptPlanDuration(testBasePlan, 4)
        val startDate = LocalDate.of(2025, 1, 10)
        val endDate = LocalDate.of(2025, 1, 5)

        val result = scheduler.alignToEndDate(adapted, startDate, endDate)

        assertEquals(adapted.size, result.size) // Should return unchanged
    }

    // --- packWeek tests ---

    @Test
    fun `packWeek respects daily time budget`() {
        val adapted = scheduler.adaptPlanDuration(testBasePlan, 1)[0]
        val availability = StudyPlanScheduler.DailyAvailability(
            monday = 60,    // Only 60 minutes on Monday
            tuesday = 60,
            wednesday = 60,
            thursday = 60,
            friday = 60,
            saturday = 0,   // No time on Saturday (should drop full exam)
            sunday = 60
        )

        val result = scheduler.packWeek(adapted, availability)

        // Saturday has 0 budget, so should have no tasks
        val saturdayTasks = result.days.find { it.dayOfWeek == DayOfWeek.SATURDAY }?.tasks
        assertTrue(saturdayTasks?.isEmpty() == true)
    }

    @Test
    fun `packWeek handles zero availability on all days`() {
        val adapted = scheduler.adaptPlanDuration(testBasePlan, 1)[0]
        val availability = StudyPlanScheduler.DailyAvailability(
            monday = 0, tuesday = 0, wednesday = 0,
            thursday = 0, friday = 0, saturday = 0, sunday = 0
        )

        val result = scheduler.packWeek(adapted, availability)

        result.days.forEach { day ->
            assertTrue("Day ${day.dayOfWeek} should have no tasks", day.tasks.isEmpty())
        }
    }

    @Test
    fun `packWeek handles empty week`() {
        val emptyWeek = StudyPlanScheduler.AdaptedWeekPlan(
            weekNumber = 1,
            monthNumber = 1,
            phase = StudyPlanScheduler.Phase.FOUNDATION,
            days = emptyList()
        )
        val availability = StudyPlanScheduler.DailyAvailability(
            monday = 120, tuesday = 120, wednesday = 120,
            thursday = 120, friday = 120, saturday = 120, sunday = 120
        )

        val result = scheduler.packWeek(emptyWeek, availability)

        assertTrue(result.days.isEmpty())
    }

    // --- Helper function tests ---

    @Test
    fun `taskPriority returns high priority for reading tasks`() {
        val readingTask = PlanTask("t1", "Reading Practice", "Some reading details")
        val priority = scheduler.taskPriority(readingTask)

        assertEquals(3, priority)
    }

    @Test
    fun `taskPriority returns high priority for exam tasks`() {
        val examTask = PlanTask("t1", "Mini Exam", "deneme sınavı")
        val priority = scheduler.taskPriority(examTask)

        assertEquals(3, priority)
    }

    @Test
    fun `taskPriority returns medium priority for analysis tasks`() {
        val analysisTask = PlanTask("t1", "Mistake Analysis", "Hata analizi")
        val priority = scheduler.taskPriority(analysisTask)

        assertEquals(2, priority)
    }

    @Test
    fun `taskPriority returns low priority for other tasks`() {
        val otherTask = PlanTask("t1", "General Task", "Some general task")
        val priority = scheduler.taskPriority(otherTask)

        assertEquals(1, priority)
    }

    @Test
    fun `estimateDurationMinutes parses explicit duration`() {
        val task = PlanTask("t1", "Full Exam", "180 dakika deneme")
        val duration = scheduler.estimateDurationMinutes(task)

        assertEquals(180, duration)
    }

    @Test
    fun `estimateDurationMinutes parses duration range`() {
        val task = PlanTask("t1", "Mini Exam", "60-75 dk sınav")
        val duration = scheduler.estimateDurationMinutes(task)

        assertEquals(67, duration) // Average of 60 and 75
    }

    @Test
    fun `estimateDurationMinutes uses heuristic for reading`() {
        val task = PlanTask("t1", "Reading Practice", null)
        val duration = scheduler.estimateDurationMinutes(task)

        assertEquals(45, duration)
    }

    @Test
    fun `estimateDurationMinutes uses heuristic for grammar`() {
        val task = PlanTask("t1", "Grammar Study", null)
        val duration = scheduler.estimateDurationMinutes(task)

        assertEquals(40, duration)
    }

    @Test
    fun `estimateDurationMinutes returns default for unknown tasks`() {
        val task = PlanTask("t1", "Unknown Task Type", null)
        val duration = scheduler.estimateDurationMinutes(task)

        assertEquals(30, duration)
    }

    // --- DailyAvailability tests ---

    @Test
    fun `DailyAvailability fromSettings creates correct availability`() {
        val settings = PlanDurationSettings(
            monMinutes = 60,
            tueMinutes = 90,
            wedMinutes = 120,
            thuMinutes = 60,
            friMinutes = 90,
            satMinutes = 180,
            sunMinutes = 200
        )

        val availability = StudyPlanScheduler.DailyAvailability.fromSettings(settings)

        assertEquals(60, availability.getMinutes(DayOfWeek.MONDAY))
        assertEquals(90, availability.getMinutes(DayOfWeek.TUESDAY))
        assertEquals(120, availability.getMinutes(DayOfWeek.WEDNESDAY))
        assertEquals(60, availability.getMinutes(DayOfWeek.THURSDAY))
        assertEquals(90, availability.getMinutes(DayOfWeek.FRIDAY))
        assertEquals(180, availability.getMinutes(DayOfWeek.SATURDAY))
        assertEquals(200, availability.getMinutes(DayOfWeek.SUNDAY))
    }

    // --- Phase determination tests ---

    @Test
    fun `phaseForIndex returns FOUNDATION for weeks 0-7`() {
        for (i in 0 until 8) {
            assertEquals("Week index $i should be FOUNDATION",
                StudyPlanScheduler.Phase.FOUNDATION,
                scheduler.phaseForIndex(i)
            )
        }
    }

    @Test
    fun `phaseForIndex returns DEVELOPMENT for weeks 8-17`() {
        for (i in 8 until 18) {
            assertEquals("Week index $i should be DEVELOPMENT",
                StudyPlanScheduler.Phase.DEVELOPMENT,
                scheduler.phaseForIndex(i)
            )
        }
    }

    @Test
    fun `phaseForIndex returns ADVANCED for weeks 18-25`() {
        for (i in 18 until 26) {
            assertEquals("Week index $i should be ADVANCED",
                StudyPlanScheduler.Phase.ADVANCED,
                scheduler.phaseForIndex(i)
            )
        }
    }

    @Test
    fun `phaseForIndex returns EXAM_CAMP for weeks 26+`() {
        for (i in 26 until 35) {
            assertEquals("Week index $i should be EXAM_CAMP",
                StudyPlanScheduler.Phase.EXAM_CAMP,
                scheduler.phaseForIndex(i)
            )
        }
    }
}
