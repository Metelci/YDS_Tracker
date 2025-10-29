package com.mtlc.studyplan.database.entities

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for UserSettingsEntity - Stores user preferences and settings
 */
class UserSettingsEntityTest {

    private fun createTestSettings(
        id: String = "default_user_settings",
        userId: String = "default_user",
        notificationsEnabled: Boolean = true,
        theme: String = "system",
        accentColor: String = "#1976D2",
        dailyStudyGoalMinutes: Int = 120,
        dailyTaskGoal: Int = 5
    ) = UserSettingsEntity(
        id = id,
        userId = userId,
        notificationsEnabled = notificationsEnabled,
        theme = theme,
        accentColor = accentColor,
        dailyStudyGoalMinutes = dailyStudyGoalMinutes,
        dailyTaskGoal = dailyTaskGoal
    )

    @Test
    fun `UserSettingsEntity creates with defaults`() {
        val settings = UserSettingsEntity(id = "settings-1")
        assertEquals("settings-1", settings.id)
        assertTrue(settings.notificationsEnabled)
        assertEquals("system", settings.theme)
        assertEquals(120, settings.dailyStudyGoalMinutes)
    }

    @Test
    fun `UserSettingsEntity notification settings`() {
        val settings = UserSettingsEntity(
            id = "settings-1",
            notificationsEnabled = true,
            dailyReminderEnabled = true,
            streakReminderEnabled = false
        )
        assertTrue(settings.notificationsEnabled)
        assertTrue(settings.dailyReminderEnabled)
        assertFalse(settings.streakReminderEnabled)
    }

    @Test
    fun `UserSettingsEntity theme settings`() {
        val themes = listOf("light", "dark", "system")
        themes.forEach { theme ->
            val settings = createTestSettings(theme = theme)
            assertEquals(theme, settings.theme)
        }
    }

    @Test
    fun `UserSettingsEntity accent color settings`() {
        val settings = createTestSettings(accentColor = "#FF6B6B")
        assertEquals("#FF6B6B", settings.accentColor)
    }

    @Test
    fun `UserSettingsEntity study goal settings`() {
        val settings = createTestSettings(
            dailyStudyGoalMinutes = 180,
            dailyTaskGoal = 10
        )
        assertEquals(180, settings.dailyStudyGoalMinutes)
        assertEquals(10, settings.dailyTaskGoal)
    }

    @Test
    fun `UserSettingsEntity pomodoro settings`() {
        val settings = UserSettingsEntity(
            id = "settings-1",
            defaultStudySessionLength = 30,
            defaultBreakLength = 10,
            longBreakLength = 20,
            sessionsUntilLongBreak = 4
        )
        assertEquals(30, settings.defaultStudySessionLength)
        assertEquals(10, settings.defaultBreakLength)
        assertEquals(20, settings.longBreakLength)
        assertEquals(4, settings.sessionsUntilLongBreak)
    }

    @Test
    fun `UserSettingsEntity sound and vibration settings`() {
        val settings = UserSettingsEntity(
            id = "settings-1",
            soundEnabled = false,
            vibrationEnabled = true
        )
        assertFalse(settings.soundEnabled)
        assertTrue(settings.vibrationEnabled)
    }

    @Test
    fun `UserSettingsEntity privacy settings`() {
        val privateSettings = UserSettingsEntity(id = "s1", profilePublic = false)
        val publicSettings = UserSettingsEntity(id = "s2", profilePublic = true)

        assertFalse(privateSettings.profilePublic)
        assertTrue(publicSettings.profilePublic)
    }

    @Test
    fun `UserSettingsEntity sync settings`() {
        val settings = UserSettingsEntity(
            id = "settings-1",
            autoSyncEnabled = true,
            syncOnlyOnWifi = true,
            backupEnabled = true
        )
        assertTrue(settings.autoSyncEnabled)
        assertTrue(settings.syncOnlyOnWifi)
        assertTrue(settings.backupEnabled)
    }

    @Test
    fun `UserSettingsEntity accessibility settings`() {
        val settings = UserSettingsEntity(
            id = "settings-1",
            highContrastMode = true,
            screenReaderOptimized = true,
            reducedMotion = true
        )
        assertTrue(settings.highContrastMode)
        assertTrue(settings.screenReaderOptimized)
        assertTrue(settings.reducedMotion)
    }

    @Test
    fun `UserSettingsEntity language settings`() {
        val settings = UserSettingsEntity(
            id = "settings-1",
            language = "tr",
            dateFormat = "EU",
            timeFormat = "24h"
        )
        assertEquals("tr", settings.language)
        assertEquals("EU", settings.dateFormat)
        assertEquals("24h", settings.timeFormat)
    }

    @Test
    fun `UserSettingsEntity default values`() {
        val settings = UserSettingsEntity()
        assertEquals(true, settings.notificationsEnabled)
        assertEquals(true, settings.dailyReminderEnabled)
        assertEquals("09:00", settings.dailyReminderTime)
        assertEquals("system", settings.theme)
        assertEquals(25, settings.defaultStudySessionLength)
        assertEquals(5, settings.defaultBreakLength)
        assertEquals(15, settings.longBreakLength)
        assertEquals(4, settings.sessionsUntilLongBreak)
        assertEquals(120, settings.dailyStudyGoalMinutes)
        assertEquals(5, settings.dailyTaskGoal)
        assertEquals(840, settings.weeklyStudyGoalMinutes)
        assertEquals(35, settings.weeklyTaskGoal)
    }

    @Test
    fun `UserSettingsEntity copy with updates`() {
        val original = UserSettingsEntity(
            id = "s1",
            theme = "light",
            dailyStudyGoalMinutes = 120
        )
        val updated = original.copy(
            theme = "dark",
            dailyStudyGoalMinutes = 180
        )

        assertEquals("light", original.theme)
        assertEquals(120, original.dailyStudyGoalMinutes)
        assertEquals("dark", updated.theme)
        assertEquals(180, updated.dailyStudyGoalMinutes)
    }

    @Test
    fun `UserSettingsEntity analytical settings`() {
        val settings = UserSettingsEntity(
            id = "s1",
            analyticsEnabled = true,
            crashReportingEnabled = true,
            debugModeEnabled = false
        )
        assertTrue(settings.analyticsEnabled)
        assertTrue(settings.crashReportingEnabled)
        assertFalse(settings.debugModeEnabled)
    }

    @Test
    fun `UserSettingsEntity equality`() {
        val s1 = UserSettingsEntity(id = "settings-1", theme = "dark")
        val s2 = UserSettingsEntity(id = "settings-1", theme = "dark")
        assertEquals(s1, s2)
    }
}
