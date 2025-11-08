package com.mtlc.studyplan.settings.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsRepositoryNotificationsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        try { stopKoin() } catch (e: Exception) { }
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("study_plan_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun updateNotificationSettingUpdatesFlow() = runTest {
        val repository = SettingsRepository(context, backgroundScope)

        val initial = repository.getNotificationSettings().first()
        assertTrue(initial.pushNotifications)
        assertEquals(TimeValue(9, 0), initial.studyReminderTime)

        repository.updateNotificationSetting("push_notifications", false)
        repository.updateNotificationSetting("study_reminder_time", TimeValue(7, 30))
        advanceUntilIdle()

        val updated = repository.getNotificationSettings().first()
        assertFalse(updated.pushNotifications)
        assertEquals(TimeValue(7, 30), updated.studyReminderTime)
    }

    @Test
    fun updateNotificationSettingRejectsInvalidValues() = runTest {
        val repository = SettingsRepository(context, backgroundScope)

        val result = runCatching {
            repository.updateNotificationSetting("push_notifications", "invalid")
        }

        assertTrue(result.isFailure)
    }
}
