package com.mtlc.studyplan.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.mtlc.studyplan.database.entities.UserSettingsEntity

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    fun getUserSettings(userId: String = "default_user"): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    suspend fun getUserSettingsSync(userId: String = "default_user"): UserSettingsEntity?

    @Query("SELECT * FROM user_settings ORDER BY updatedAt DESC")
    fun getAllUserSettings(): Flow<List<UserSettingsEntity>>

    // Notification Settings Queries
    @Query("SELECT notificationsEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getNotificationsEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT dailyReminderEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getDailyReminderEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT dailyReminderTime FROM user_settings WHERE userId = :userId")
    suspend fun getDailyReminderTime(userId: String = "default_user"): String?

    @Query("SELECT streakReminderEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getStreakReminderEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT achievementNotificationsEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getAchievementNotificationsEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT socialNotificationsEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getSocialNotificationsEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT weeklyReportEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getWeeklyReportEnabled(userId: String = "default_user"): Boolean?

    // Theme and UI Settings Queries
    @Query("SELECT theme FROM user_settings WHERE userId = :userId")
    suspend fun getTheme(userId: String = "default_user"): String?

    @Query("SELECT accentColor FROM user_settings WHERE userId = :userId")
    suspend fun getAccentColor(userId: String = "default_user"): String?

    @Query("SELECT useDynamicColors FROM user_settings WHERE userId = :userId")
    suspend fun getUseDynamicColors(userId: String = "default_user"): Boolean?

    @Query("SELECT fontSize FROM user_settings WHERE userId = :userId")
    suspend fun getFontSize(userId: String = "default_user"): String?

    @Query("SELECT reducedAnimations FROM user_settings WHERE userId = :userId")
    suspend fun getReducedAnimations(userId: String = "default_user"): Boolean?

    @Query("SELECT compactMode FROM user_settings WHERE userId = :userId")
    suspend fun getCompactMode(userId: String = "default_user"): Boolean?

    // Study Settings Queries
    @Query("SELECT defaultStudySessionLength FROM user_settings WHERE userId = :userId")
    suspend fun getDefaultStudySessionLength(userId: String = "default_user"): Int?

    @Query("SELECT defaultBreakLength FROM user_settings WHERE userId = :userId")
    suspend fun getDefaultBreakLength(userId: String = "default_user"): Int?

    @Query("SELECT longBreakLength FROM user_settings WHERE userId = :userId")
    suspend fun getLongBreakLength(userId: String = "default_user"): Int?

    @Query("SELECT autoStartBreaks FROM user_settings WHERE userId = :userId")
    suspend fun getAutoStartBreaks(userId: String = "default_user"): Boolean?

    @Query("SELECT soundEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getSoundEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT vibrationEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getVibrationEnabled(userId: String = "default_user"): Boolean?

    // Goal Settings Queries
    @Query("SELECT dailyStudyGoalMinutes FROM user_settings WHERE userId = :userId")
    suspend fun getDailyStudyGoalMinutes(userId: String = "default_user"): Int?

    @Query("SELECT dailyTaskGoal FROM user_settings WHERE userId = :userId")
    suspend fun getDailyTaskGoal(userId: String = "default_user"): Int?

    @Query("SELECT weeklyStudyGoalMinutes FROM user_settings WHERE userId = :userId")
    suspend fun getWeeklyStudyGoalMinutes(userId: String = "default_user"): Int?

    @Query("SELECT weeklyTaskGoal FROM user_settings WHERE userId = :userId")
    suspend fun getWeeklyTaskGoal(userId: String = "default_user"): Int?

    @Query("SELECT adaptiveGoals FROM user_settings WHERE userId = :userId")
    suspend fun getAdaptiveGoals(userId: String = "default_user"): Boolean?

    @Query("SELECT goalDifficulty FROM user_settings WHERE userId = :userId")
    suspend fun getGoalDifficulty(userId: String = "default_user"): String?

    // Privacy and Social Settings Queries
    @Query("SELECT socialSharingEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getSocialSharingEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT profilePublic FROM user_settings WHERE userId = :userId")
    suspend fun getProfilePublic(userId: String = "default_user"): Boolean?

    @Query("SELECT shareAchievements FROM user_settings WHERE userId = :userId")
    suspend fun getShareAchievements(userId: String = "default_user"): Boolean?

    @Query("SELECT shareStreak FROM user_settings WHERE userId = :userId")
    suspend fun getShareStreak(userId: String = "default_user"): Boolean?

    @Query("SELECT allowFriendRequests FROM user_settings WHERE userId = :userId")
    suspend fun getAllowFriendRequests(userId: String = "default_user"): Boolean?

    @Query("SELECT showOnLeaderboards FROM user_settings WHERE userId = :userId")
    suspend fun getShowOnLeaderboards(userId: String = "default_user"): Boolean?

    // Data and Sync Settings Queries
    @Query("SELECT autoSyncEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getAutoSyncEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT syncOnlyOnWifi FROM user_settings WHERE userId = :userId")
    suspend fun getSyncOnlyOnWifi(userId: String = "default_user"): Boolean?

    @Query("SELECT offlineMode FROM user_settings WHERE userId = :userId")
    suspend fun getOfflineMode(userId: String = "default_user"): Boolean?

    @Query("SELECT backupEnabled FROM user_settings WHERE userId = :userId")
    suspend fun getBackupEnabled(userId: String = "default_user"): Boolean?

    @Query("SELECT backupFrequency FROM user_settings WHERE userId = :userId")
    suspend fun getBackupFrequency(userId: String = "default_user"): String?

    // Accessibility Settings Queries
    @Query("SELECT highContrastMode FROM user_settings WHERE userId = :userId")
    suspend fun getHighContrastMode(userId: String = "default_user"): Boolean?

    @Query("SELECT largeTextMode FROM user_settings WHERE userId = :userId")
    suspend fun getLargeTextMode(userId: String = "default_user"): Boolean?

    @Query("SELECT screenReaderOptimized FROM user_settings WHERE userId = :userId")
    suspend fun getScreenReaderOptimized(userId: String = "default_user"): Boolean?

    @Query("SELECT reducedMotion FROM user_settings WHERE userId = :userId")
    suspend fun getReducedMotion(userId: String = "default_user"): Boolean?

    @Query("SELECT colorBlindFriendly FROM user_settings WHERE userId = :userId")
    suspend fun getColorBlindFriendly(userId: String = "default_user"): Boolean?

    // Language and Localization Queries
    @Query("SELECT language FROM user_settings WHERE userId = :userId")
    suspend fun getLanguage(userId: String = "default_user"): String?

    @Query("SELECT dateFormat FROM user_settings WHERE userId = :userId")
    suspend fun getDateFormat(userId: String = "default_user"): String?

    @Query("SELECT timeFormat FROM user_settings WHERE userId = :userId")
    suspend fun getTimeFormat(userId: String = "default_user"): String?

    @Query("SELECT firstDayOfWeek FROM user_settings WHERE userId = :userId")
    suspend fun getFirstDayOfWeek(userId: String = "default_user"): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettingsEntity)

    @Update
    suspend fun updateUserSettings(settings: UserSettingsEntity)

    // Individual Setting Updates
    @Query("UPDATE user_settings SET notificationsEnabled = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateNotificationsEnabled(enabled: Boolean, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET dailyReminderEnabled = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDailyReminderEnabled(enabled: Boolean, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET dailyReminderTime = :time, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDailyReminderTime(time: String, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET theme = :theme, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateTheme(theme: String, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET accentColor = :color, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateAccentColor(color: String, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET useDynamicColors = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateUseDynamicColors(enabled: Boolean, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET fontSize = :size, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateFontSize(size: String, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET defaultStudySessionLength = :length, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDefaultStudySessionLength(length: Int, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET dailyStudyGoalMinutes = :goal, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDailyStudyGoalMinutes(goal: Int, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET dailyTaskGoal = :goal, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDailyTaskGoal(goal: Int, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET socialSharingEnabled = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateSocialSharingEnabled(enabled: Boolean, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET profilePublic = :isPublic, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateProfilePublic(isPublic: Boolean, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET autoSyncEnabled = :enabled, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateAutoSyncEnabled(enabled: Boolean, updatedAt: Long, userId: String = "default_user")

    @Query("UPDATE user_settings SET language = :language, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateLanguage(language: String, updatedAt: Long, userId: String = "default_user")

    @Delete
    suspend fun deleteUserSettings(settings: UserSettingsEntity)

    @Query("DELETE FROM user_settings WHERE userId = :userId")
    suspend fun deleteUserSettingsByUserId(userId: String = "default_user")

    @Query("DELETE FROM user_settings")
    suspend fun deleteAllUserSettings()

    // Transaction methods
    @Transaction
    suspend fun getOrCreateUserSettings(userId: String = "default_user"): UserSettingsEntity {
        val existing = getUserSettingsSync(userId)
        return if (existing != null) {
            existing
        } else {
            val defaultSettings = UserSettingsEntity(userId = userId)
            insertUserSettings(defaultSettings)
            defaultSettings
        }
    }

    @Transaction
    suspend fun updateSetting(
        userId: String = "default_user",
        updateFunction: suspend (UserSettingsEntity) -> UserSettingsEntity
    ) {
        val settings = getOrCreateUserSettings(userId)
        val updatedSettings = updateFunction(settings).copy(updatedAt = System.currentTimeMillis())
        updateUserSettings(updatedSettings)
    }

    @Transaction
    suspend fun resetToDefaults(userId: String = "default_user") {
        deleteUserSettingsByUserId(userId)
        insertUserSettings(UserSettingsEntity(userId = userId))
    }
}