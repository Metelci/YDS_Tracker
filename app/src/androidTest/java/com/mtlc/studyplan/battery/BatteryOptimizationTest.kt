package com.mtlc.studyplan.battery

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.mtlc.studyplan.calendar.CalendarWorker
import com.mtlc.studyplan.calendar.CalendarWorkerManager
import com.mtlc.studyplan.power.PowerStateChecker
import com.mtlc.studyplan.workers.DailyStudyReminderWorker
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatteryOptimizationTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        // Ensure no forced constraints by default
        PowerStateChecker.overrideForTests(null)
    }

    @After
    fun tearDown() {
        PowerStateChecker.overrideForTests(null)
    }

    @Test
    fun dailyReminder_isScheduledWithBatteryNotLow() {
        // Enqueue scheduling
        DailyStudyReminderWorker.schedule(context)

        // Verify constraints of the unique periodic work
        val wm = WorkManager.getInstance(context)
        val infos = wm.getWorkInfosForUniqueWork(DailyStudyReminderWorker.WORK_NAME).get()
        assertTrue(infos.isNotEmpty())
        val request = infos.first()
        assertTrue(request.state == WorkInfo.State.ENQUEUED || request.state == WorkInfo.State.RUNNING)
        val requiresBatteryNotLow = request.constraints.requiresBatteryNotLow()
        assertTrue("Daily reminder should require battery not low", requiresBatteryNotLow)
    }

    @Test
    fun powerReceiver_triggersCatchupSyncOnBatteryOkay() {
        // Send ACTION_BATTERY_OKAY broadcast to trigger catch-up
        val intent = Intent(Intent.ACTION_BATTERY_OKAY)
        context.sendBroadcast(intent)

        // Verify a calendar sync is enqueued (by tag)
        val wm = WorkManager.getInstance(context)
        val infos = wm.getWorkInfosByTag(CalendarWorker.TAG_CALENDAR_SYNC).get()
        assertTrue("Calendar sync should be enqueued on BATTERY_OKAY", infos.isNotEmpty())
    }
}

