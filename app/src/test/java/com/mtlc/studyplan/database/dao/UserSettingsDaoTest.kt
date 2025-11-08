package com.mtlc.studyplan.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.UserSettingsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserSettingsDao - User preferences and settings database operations
 * Tests CRUD operations, individual setting updates, and settings transactions
 *
 * Pattern: Study-first methodology with Robolectric + Room in-memory database
 * Fix: Explicit JournalMode.TRUNCATE to avoid ActivityManager.isLowRamDevice() NoSuchMethodError
 */
@RunWith(RobolectricTestRunner::class)
class UserSettingsDaoTest {

    private lateinit var database: StudyPlanDatabase
    private lateinit var userSettingsDao: UserSettingsDao

    @Before
    fun setUp() {
        try { stopKoin() } catch (e: Exception) { }
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            StudyPlanDatabase::class.java
        )
            .allowMainThreadQueries()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE) // Fix for Robolectric
            .build()
        userSettingsDao = database.settingsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createTestSettings(
        userId: String = "default_user",
        theme: String = "light",
        notificationsEnabled: Boolean = true,
        dailyStudyGoalMinutes: Int = 120
    ) = UserSettingsEntity(
        userId = userId,
        theme = theme,
        notificationsEnabled = notificationsEnabled,
        dailyStudyGoalMinutes = dailyStudyGoalMinutes
    )

    // ========== INSERT/READ TESTS ==========

    @Test
    fun `insertUserSettings should store settings in database`() = runTest {
        val settings = createTestSettings(userId = "user1", theme = "dark")

        userSettingsDao.insertUserSettings(settings)

        val retrieved = userSettingsDao.getUserSettingsSync("user1")
        assertNotNull(retrieved)
        assertEquals("dark", retrieved.theme)
        assertTrue(retrieved.notificationsEnabled)
    }

    @Test
    fun `getUserSettings should return Flow of settings`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        val flowSettings = userSettingsDao.getUserSettings("user1").first()

        assertNotNull(flowSettings)
        assertEquals("user1", flowSettings?.userId)
    }

    @Test
    fun `getUserSettingsSync should return null when not exists`() = runTest {
        val retrieved = userSettingsDao.getUserSettingsSync("nonexistent_user")

        assertNull(retrieved)
    }

    @Test
    fun `getAllUserSettings should return all settings ordered by updatedAt DESC`() = runTest {
        // Each insert with unique ID stores separate entry
        val settings1 = createTestSettings(userId = "user1").copy(id = "settings_1")
        val settings2 = createTestSettings(userId = "user2").copy(id = "settings_2")
        val settings3 = createTestSettings(userId = "user3").copy(id = "settings_3")

        userSettingsDao.insertUserSettings(settings1)
        userSettingsDao.insertUserSettings(settings2)
        userSettingsDao.insertUserSettings(settings3)

        val allSettings = userSettingsDao.getAllUserSettings().first()

        assertEquals(3, allSettings.size)
    }

    // ========== NOTIFICATION SETTINGS TESTS ==========

    @Test
    fun `getNotificationsEnabled should return setting value`() = runTest {
        val settings = createTestSettings(userId = "user1", notificationsEnabled = false)
        userSettingsDao.insertUserSettings(settings)

        val enabled = userSettingsDao.getNotificationsEnabled("user1")

        assertFalse(enabled == true)
    }

    @Test
    fun `updateNotificationsEnabled should modify setting`() = runTest {
        val settings = createTestSettings(userId = "user1", notificationsEnabled = true)
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateNotificationsEnabled(false, System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getNotificationsEnabled("user1")
        assertFalse(updated == true)
    }

    @Test
    fun `getDailyReminderEnabled should return setting value`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        val enabled = userSettingsDao.getDailyReminderEnabled("user1")

        assertTrue(enabled == true)
    }

    @Test
    fun `getDailyReminderTime should return time in proper format`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(dailyReminderTime = "14:30")
        userSettingsDao.insertUserSettings(settings)

        val time = userSettingsDao.getDailyReminderTime("user1")

        assertEquals("14:30", time)
    }

    @Test
    fun `updateDailyReminderTime should modify reminder time`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateDailyReminderTime("18:00", System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getDailyReminderTime("user1")
        assertEquals("18:00", updated)
    }

    // ========== THEME AND UI SETTINGS TESTS ==========

    @Test
    fun `getTheme should return theme preference`() = runTest {
        val settings = createTestSettings(userId = "user1", theme = "dark")
        userSettingsDao.insertUserSettings(settings)

        val theme = userSettingsDao.getTheme("user1")

        assertEquals("dark", theme)
    }

    @Test
    fun `updateTheme should modify theme setting`() = runTest {
        val settings = createTestSettings(userId = "user1", theme = "light")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateTheme("dark", System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getTheme("user1")
        assertEquals("dark", updated)
    }

    @Test
    fun `getAccentColor should return color value`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(accentColor = "#FF5722")
        userSettingsDao.insertUserSettings(settings)

        val color = userSettingsDao.getAccentColor("user1")

        assertEquals("#FF5722", color)
    }

    @Test
    fun `updateAccentColor should modify color`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateAccentColor("#4CAF50", System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getAccentColor("user1")
        assertEquals("#4CAF50", updated)
    }

    @Test
    fun `getUseDynamicColors should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(useDynamicColors = true)
        userSettingsDao.insertUserSettings(settings)

        val dynamic = userSettingsDao.getUseDynamicColors("user1")

        assertTrue(dynamic == true)
    }

    @Test
    fun `getFontSize should return font size setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(fontSize = "large")
        userSettingsDao.insertUserSettings(settings)

        val size = userSettingsDao.getFontSize("user1")

        assertEquals("large", size)
    }

    @Test
    fun `updateFontSize should modify font size`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateFontSize("small", System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getFontSize("user1")
        assertEquals("small", updated)
    }

    @Test
    fun `getReducedAnimations should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(reducedAnimations = true)
        userSettingsDao.insertUserSettings(settings)

        val reduced = userSettingsDao.getReducedAnimations("user1")

        assertTrue(reduced == true)
    }

    // ========== STUDY SETTINGS TESTS ==========

    @Test
    fun `getDefaultStudySessionLength should return session duration`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(defaultStudySessionLength = 45)
        userSettingsDao.insertUserSettings(settings)

        val length = userSettingsDao.getDefaultStudySessionLength("user1")

        assertEquals(45, length)
    }

    @Test
    fun `updateDefaultStudySessionLength should modify duration`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateDefaultStudySessionLength(30, System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getDefaultStudySessionLength("user1")
        assertEquals(30, updated)
    }

    @Test
    fun `getDefaultBreakLength should return break duration`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(defaultBreakLength = 10)
        userSettingsDao.insertUserSettings(settings)

        val length = userSettingsDao.getDefaultBreakLength("user1")

        assertEquals(10, length)
    }

    @Test
    fun `getLongBreakLength should return long break duration`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(longBreakLength = 30)
        userSettingsDao.insertUserSettings(settings)

        val length = userSettingsDao.getLongBreakLength("user1")

        assertEquals(30, length)
    }

    @Test
    fun `getAutoStartBreaks should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(autoStartBreaks = true)
        userSettingsDao.insertUserSettings(settings)

        val autoStart = userSettingsDao.getAutoStartBreaks("user1")

        assertTrue(autoStart == true)
    }

    @Test
    fun `getSoundEnabled should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(soundEnabled = false)
        userSettingsDao.insertUserSettings(settings)

        val soundEnabled = userSettingsDao.getSoundEnabled("user1")

        assertFalse(soundEnabled == true)
    }

    // ========== GOAL SETTINGS TESTS ==========

    @Test
    fun `getDailyStudyGoalMinutes should return goal value`() = runTest {
        val settings = createTestSettings(userId = "user1", dailyStudyGoalMinutes = 180)
        userSettingsDao.insertUserSettings(settings)

        val goal = userSettingsDao.getDailyStudyGoalMinutes("user1")

        assertEquals(180, goal)
    }

    @Test
    fun `updateDailyStudyGoalMinutes should modify goal`() = runTest {
        val settings = createTestSettings(userId = "user1", dailyStudyGoalMinutes = 120)
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateDailyStudyGoalMinutes(240, System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getDailyStudyGoalMinutes("user1")
        assertEquals(240, updated)
    }

    @Test
    fun `getDailyTaskGoal should return task goal`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(dailyTaskGoal = 10)
        userSettingsDao.insertUserSettings(settings)

        val goal = userSettingsDao.getDailyTaskGoal("user1")

        assertEquals(10, goal)
    }

    @Test
    fun `updateDailyTaskGoal should modify task goal`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateDailyTaskGoal(8, System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getDailyTaskGoal("user1")
        assertEquals(8, updated)
    }

    @Test
    fun `getWeeklyStudyGoalMinutes should return weekly goal`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(weeklyStudyGoalMinutes = 1200)
        userSettingsDao.insertUserSettings(settings)

        val goal = userSettingsDao.getWeeklyStudyGoalMinutes("user1")

        assertEquals(1200, goal)
    }

    @Test
    fun `getWeeklyTaskGoal should return weekly task goal`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(weeklyTaskGoal = 50)
        userSettingsDao.insertUserSettings(settings)

        val goal = userSettingsDao.getWeeklyTaskGoal("user1")

        assertEquals(50, goal)
    }

    @Test
    fun `getAdaptiveGoals should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(adaptiveGoals = false)
        userSettingsDao.insertUserSettings(settings)

        val adaptive = userSettingsDao.getAdaptiveGoals("user1")

        assertFalse(adaptive == true)
    }

    @Test
    fun `getGoalDifficulty should return difficulty level`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(goalDifficulty = "hard")
        userSettingsDao.insertUserSettings(settings)

        val difficulty = userSettingsDao.getGoalDifficulty("user1")

        assertEquals("hard", difficulty)
    }

    // ========== PRIVACY AND SYNC SETTINGS TESTS ==========

    @Test
    fun `getProfilePublic should return privacy setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(profilePublic = true)
        userSettingsDao.insertUserSettings(settings)

        val isPublic = userSettingsDao.getProfilePublic("user1")

        assertTrue(isPublic == true)
    }

    @Test
    fun `updateProfilePublic should modify privacy setting`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateProfilePublic(true, System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getProfilePublic("user1")
        assertTrue(updated == true)
    }

    @Test
    fun `getAutoSyncEnabled should return sync setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(autoSyncEnabled = false)
        userSettingsDao.insertUserSettings(settings)

        val autoSync = userSettingsDao.getAutoSyncEnabled("user1")

        assertFalse(autoSync == true)
    }

    @Test
    fun `updateAutoSyncEnabled should modify sync setting`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateAutoSyncEnabled(false, System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getAutoSyncEnabled("user1")
        assertFalse(updated == true)
    }

    @Test
    fun `getSyncOnlyOnWifi should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(syncOnlyOnWifi = true)
        userSettingsDao.insertUserSettings(settings)

        val wifiOnly = userSettingsDao.getSyncOnlyOnWifi("user1")

        assertTrue(wifiOnly == true)
    }

    @Test
    fun `getOfflineMode should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(offlineMode = true)
        userSettingsDao.insertUserSettings(settings)

        val offline = userSettingsDao.getOfflineMode("user1")

        assertTrue(offline == true)
    }

    @Test
    fun `getBackupEnabled should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(backupEnabled = false)
        userSettingsDao.insertUserSettings(settings)

        val backup = userSettingsDao.getBackupEnabled("user1")

        assertFalse(backup == true)
    }

    @Test
    fun `getBackupFrequency should return backup frequency`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(backupFrequency = "daily")
        userSettingsDao.insertUserSettings(settings)

        val frequency = userSettingsDao.getBackupFrequency("user1")

        assertEquals("daily", frequency)
    }

    // ========== LANGUAGE AND ACCESSIBILITY TESTS ==========

    @Test
    fun `getLanguage should return language setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(language = "tr")
        userSettingsDao.insertUserSettings(settings)

        val language = userSettingsDao.getLanguage("user1")

        assertEquals("tr", language)
    }

    @Test
    fun `updateLanguage should modify language setting`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.updateLanguage("fr", System.currentTimeMillis(), "user1")

        val updated = userSettingsDao.getLanguage("user1")
        assertEquals("fr", updated)
    }

    @Test
    fun `getDateFormat should return date format`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(dateFormat = "EU")
        userSettingsDao.insertUserSettings(settings)

        val format = userSettingsDao.getDateFormat("user1")

        assertEquals("EU", format)
    }

    @Test
    fun `getTimeFormat should return time format`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(timeFormat = "24h")
        userSettingsDao.insertUserSettings(settings)

        val format = userSettingsDao.getTimeFormat("user1")

        assertEquals("24h", format)
    }

    @Test
    fun `getHighContrastMode should return setting`() = runTest {
        val settings = createTestSettings(userId = "user1").copy(highContrastMode = true)
        userSettingsDao.insertUserSettings(settings)

        val highContrast = userSettingsDao.getHighContrastMode("user1")

        assertTrue(highContrast == true)
    }

    // ========== UPDATE AND DELETE TESTS ==========

    @Test
    fun `updateUserSettings should modify all settings`() = runTest {
        val initial = createTestSettings(userId = "user1", theme = "light")
        userSettingsDao.insertUserSettings(initial)

        val modified = initial.copy(theme = "dark", dailyStudyGoalMinutes = 240)
        userSettingsDao.updateUserSettings(modified)

        val updated = userSettingsDao.getUserSettingsSync("user1")
        assertEquals("dark", updated?.theme)
        assertEquals(240, updated?.dailyStudyGoalMinutes)
    }

    @Test
    fun `deleteUserSettingsByUserId should remove user settings`() = runTest {
        val settings = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(settings)

        userSettingsDao.deleteUserSettingsByUserId("user1")

        val deleted = userSettingsDao.getUserSettingsSync("user1")
        assertNull(deleted)
    }

    @Test
    fun `deleteAllUserSettings should remove all settings`() = runTest {
        userSettingsDao.insertUserSettings(createTestSettings(userId = "user1"))
        userSettingsDao.insertUserSettings(createTestSettings(userId = "user2"))

        userSettingsDao.deleteAllUserSettings()

        val all = userSettingsDao.getAllUserSettings().first()
        assertEquals(0, all.size)
    }

    // ========== TRANSACTION TESTS ==========

    @Test
    fun `getOrCreateUserSettings should create if not exists`() = runTest {
        val settings = userSettingsDao.getOrCreateUserSettings("user1")

        assertNotNull(settings)
        assertEquals("user1", settings.userId)
        assertEquals(120, settings.dailyStudyGoalMinutes) // Default value
    }

    @Test
    fun `getOrCreateUserSettings should return existing if exists`() = runTest {
        val initial = createTestSettings(userId = "user1", dailyStudyGoalMinutes = 240)
        userSettingsDao.insertUserSettings(initial)

        val retrieved = userSettingsDao.getOrCreateUserSettings("user1")

        assertEquals(240, retrieved.dailyStudyGoalMinutes)
    }

    @Test
    fun `updateSetting transaction should update all settings atomically`() = runTest {
        val initial = createTestSettings(userId = "user1")
        userSettingsDao.insertUserSettings(initial)

        userSettingsDao.updateSetting("user1") { settings ->
            settings.copy(
                theme = "dark",
                dailyStudyGoalMinutes = 180,
                notificationsEnabled = false
            )
        }

        val updated = userSettingsDao.getUserSettingsSync("user1")
        assertEquals("dark", updated?.theme)
        assertEquals(180, updated?.dailyStudyGoalMinutes)
        assertFalse(updated?.notificationsEnabled == true)
    }

    @Test
    fun `resetToDefaults should restore default settings`() = runTest {
        val initial = createTestSettings(userId = "user1", theme = "dark", dailyStudyGoalMinutes = 240)
        userSettingsDao.insertUserSettings(initial)

        userSettingsDao.resetToDefaults("user1")

        val reset = userSettingsDao.getUserSettingsSync("user1")
        assertNotNull(reset)
        assertEquals("system", reset.theme) // Default theme
        assertEquals(120, reset.dailyStudyGoalMinutes) // Default goal
    }
}
