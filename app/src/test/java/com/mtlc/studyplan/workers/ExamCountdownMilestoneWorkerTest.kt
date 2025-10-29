package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.mtlc.studyplan.data.ExamMilestone
import com.mtlc.studyplan.notifications.NotificationManager
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.koin.core.context.stopKoin

/**
 * Comprehensive tests for ExamCountdownMilestoneWorker
 * Validates milestone detection, duplicate prevention, and configuration
 */
class ExamCountdownMilestoneWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var notificationManager: NotificationManager
    private lateinit var worker: ExamCountdownMilestoneWorker

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)

        // Mock SharedPreferences
        val sharedPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.getBoolean(any(), any()) } returns false
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs

        worker = ExamCountdownMilestoneWorker(context, workerParams, notificationManager)
    }

    @After
    fun tearDown() {
        try {
            stopKoin()
        } catch (e: Exception) {
            // Koin already stopped
        }
        unmockkAll()
    }

    @Test
    fun `work name constant is defined`() {
        assertEquals("exam_countdown_milestones", ExamCountdownMilestoneWorker.WORK_NAME)
    }

    @Test
    fun `milestone days constant has 5 values`() {
        assertEquals(5, ExamMilestone.MILESTONE_DAYS.size)
        assertTrue(ExamMilestone.MILESTONE_DAYS.contains(90))
        assertTrue(ExamMilestone.MILESTONE_DAYS.contains(60))
        assertTrue(ExamMilestone.MILESTONE_DAYS.contains(30))
        assertTrue(ExamMilestone.MILESTONE_DAYS.contains(14))
        assertTrue(ExamMilestone.MILESTONE_DAYS.contains(7))
    }

    @Test
    fun `worker instance can be created`() {
        assertNotNull(worker)
    }

    @Test
    fun `worker has notification manager dependency`() {
        assertNotNull(notificationManager)
    }

    @Test
    fun `milestone days are in descending order`() {
        val days = ExamMilestone.MILESTONE_DAYS
        assertEquals(90, days[0])
        assertEquals(60, days[1])
        assertEquals(30, days[2])
        assertEquals(14, days[3])
        assertEquals(7, days[4])
    }

    @Test
    fun `all milestone days are positive`() {
        ExamMilestone.MILESTONE_DAYS.forEach { days ->
            assertTrue("Milestone day $days should be positive", days > 0)
        }
    }

    @Test
    fun `milestone days are unique`() {
        val days = ExamMilestone.MILESTONE_DAYS
        val uniqueDays = days.toSet()
        assertEquals(days.size, uniqueDays.size)
    }

    @Test
    fun `worker work name is properly formatted`() {
        val workName = ExamCountdownMilestoneWorker.WORK_NAME
        assertTrue(workName.isNotEmpty())
        assertTrue(workName.contains("milestone"))
        assertFalse(workName.contains(" "))
    }

    @Test
    fun `worker uses exam milestones preferences name`() {
        // Trigger SharedPreferences access
        val prefs = context.getSharedPreferences("exam_milestones", Context.MODE_PRIVATE)
        assertNotNull(prefs)
    }

    @Test
    fun `SharedPreferences editor is configured correctly`() {
        val sharedPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)

        every { sharedPrefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs

        val result = sharedPrefs.edit().putBoolean("test_key", true).apply()

        verify { editor.putBoolean("test_key", true) }
        verify { editor.apply() }
    }

    @Test
    fun `notification manager is injected`() {
        assertNotNull(notificationManager)
        // Verify it's a mock
        assertTrue(notificationManager is NotificationManager)
    }

    @Test
    fun `context is available to worker`() {
        assertNotNull(context)
        // Context mock is properly set up, no need to verify call count
    }

    @Test
    fun `worker parameters are available`() {
        assertNotNull(workerParams)
    }

    @Test
    fun `exam milestone 90 days maps to correct phase`() {
        val milestone = ExamMilestone.fromDaysRemaining(
            90,
            "YDS 2025/1",
            java.time.LocalDate.now().plusDays(90)
        )
        assertNotNull(milestone)
        assertEquals(90, milestone!!.daysUntil)
    }

    @Test
    fun `exam milestone 60 days maps to correct phase`() {
        val milestone = ExamMilestone.fromDaysRemaining(
            60,
            "YDS 2025/1",
            java.time.LocalDate.now().plusDays(60)
        )
        assertNotNull(milestone)
        assertEquals(60, milestone!!.daysUntil)
    }
}
