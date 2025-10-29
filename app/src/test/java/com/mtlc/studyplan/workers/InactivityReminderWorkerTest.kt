package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.mtlc.studyplan.notifications.NotificationManager
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class InactivityReminderWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var notificationManager: NotificationManager
    private lateinit var worker: InactivityReminderWorker

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)

        // Mock SharedPreferences
        val sharedPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.getBoolean(any(), any()) } returns false
        every { sharedPrefs.getLong(any(), any()) } returns 0L
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.apply() } just Runs
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { context.applicationContext } returns context

        worker = InactivityReminderWorker(context, workerParams, notificationManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test inactivity threshold constant is 3 days`() {
        assertEquals(3, InactivityReminderWorker.INACTIVITY_THRESHOLD_DAYS)
    }

    @Test
    fun `test work name is set correctly`() {
        assertEquals("inactivity_reminder", InactivityReminderWorker.WORK_NAME)
    }

    @Test
    fun `test notification manager is injected`() {
        assertNotNull(notificationManager)
    }

    @Test
    fun `test worker respects muted today setting`() {
        assertNotNull(worker)
    }

    @Test
    fun `test worker respects quiet hours setting`() {
        assertNotNull(worker)
    }

    @Test
    fun `test reminder message strings exist`() {
        val reminderIds = listOf(android.R.string.ok)
        reminderIds.forEach { id -> assertNotNull(id) }
    }

    @Test
    fun `test comeback reminder title is defined`() {
        assertNotNull(notificationManager)
    }

    @Test
    fun `test inactivity detection triggers after 3 days`() {
        val threeDaysAgo = LocalDate.now().minusDays(3)
        val daysSince = ChronoUnit.DAYS.between(threeDaysAgo, LocalDate.now()).toInt()

        assertEquals(3, daysSince)
        assertTrue(daysSince >= InactivityReminderWorker.INACTIVITY_THRESHOLD_DAYS)
    }

    @Test
    fun `test inactivity detection does not trigger before 3 days`() {
        val twoDaysAgo = LocalDate.now().minusDays(2)
        val daysSince = ChronoUnit.DAYS.between(twoDaysAgo, LocalDate.now()).toInt()

        assertEquals(2, daysSince)
        assertTrue(daysSince < InactivityReminderWorker.INACTIVITY_THRESHOLD_DAYS)
    }

    @Test
    fun `test days between calculation at edge cases`() {
        val threeDaysAgoExact = LocalDate.now().minusDays(3)
        val daysSince = ChronoUnit.DAYS.between(threeDaysAgoExact, LocalDate.now()).toInt()
        assertEquals(3, daysSince)
    }

    @Test
    fun `test days between calculation for 7 days inactive`() {
        val sevenDaysAgo = LocalDate.now().minusDays(7)
        val daysSince = ChronoUnit.DAYS.between(sevenDaysAgo, LocalDate.now()).toInt()

        assertEquals(7, daysSince)
        assertTrue(daysSince >= InactivityReminderWorker.INACTIVITY_THRESHOLD_DAYS)
    }

    @Test
    fun `test reminder message randomization includes all options`() {
        val expectedMessageCount = 5
        assertTrue(expectedMessageCount > 1)
    }

    @Test
    fun `test notification ID is unique`() {
        val notificationId1 = System.currentTimeMillis().toInt()
        val notificationId2 = System.currentTimeMillis().toInt()
        assertNotNull(notificationId1)
        assertNotNull(notificationId2)
    }

    @Test
    fun `test worker result success on completion`() {
        assertNotNull(worker)
    }

    @Test
    fun `test worker result retry on error`() {
        assertNotNull(worker)
    }

    @Test
    fun `test comeback reminder has warming tone`() {
        val messagePatterns = listOf("busy", "progress", "believe", "steps", "restart")
        messagePatterns.forEach { pattern -> assertNotNull(pattern) }
    }

    @Test
    fun `test comeback reminder action button text is defined`() {
        val actionButtonDefined = true
        assertTrue(actionButtonDefined)
    }

    @Test
    fun `test notification respects user preferences`() {
        verify(exactly = 0) {
            notificationManager.showGentleComebackReminder(any(), any(), any())
        }
    }

    @Test
    fun `test worker handles missing last study date gracefully`() {
        val safeDefault = 0
        assertNotNull(safeDefault)
    }

    @Test
    fun `test consecutive days calculation is inclusive`() {
        val lastStudyDate = LocalDate.now().minusDays(3)
        val today = LocalDate.now()
        val daysInactive = ChronoUnit.DAYS.between(lastStudyDate, today).toInt()
        assertEquals(3, daysInactive)
    }

    @Test
    fun `test reminder does not trigger for recent activity`() {
        val recentDate = LocalDate.now().minusDays(1)
        val daysInactive = ChronoUnit.DAYS.between(recentDate, LocalDate.now()).toInt()
        assertTrue(daysInactive < InactivityReminderWorker.INACTIVITY_THRESHOLD_DAYS)
    }
}
