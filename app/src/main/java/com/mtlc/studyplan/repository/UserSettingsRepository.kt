package com.mtlc.studyplan.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import com.mtlc.studyplan.database.dao.UserSettingsDao
import com.mtlc.studyplan.database.entities.UserSettingsEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepository @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) {

    // Reactive data flows
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private val _settingsChanged = MutableStateFlow<SettingsChange?>(null)
    val settingsChanged: StateFlow<SettingsChange?> = _settingsChanged.asStateFlow()

    // Core settings flow
    fun getUserSettings(userId: String = "default_user"): Flow<UserSettingsEntity?> =
        userSettingsDao.getUserSettings(userId)

    val allUserSettings: Flow<List<UserSettingsEntity>> = userSettingsDao.getAllUserSettings()

    // Grouped settings flows
    val notificationSettings: Flow<NotificationSettings> = getUserSettings().map { settings ->
        settings?.let {
            NotificationSettings(
                notificationsEnabled = it.notificationsEnabled,
                dailyReminderEnabled = it.dailyReminderEnabled,
                dailyReminderTime = it.dailyReminderTime,
                streakReminderEnabled = it.streakReminderEnabled,
                achievementNotificationsEnabled = it.achievementNotificationsEnabled,
                weeklyReportEnabled = it.weeklyReportEnabled,
                weeklyReportDay = it.weeklyReportDay
            )
        } ?: NotificationSettings.default()
    }

    val themeSettings: Flow<ThemeSettings> = getUserSettings().map { settings ->
        settings?.let {
            ThemeSettings(
                theme = it.theme,
                accentColor = it.accentColor,
                useDynamicColors = it.useDynamicColors,
                fontSize = it.fontSize,
                reducedAnimations = it.reducedAnimations,
                compactMode = it.compactMode
            )
        } ?: ThemeSettings.default()
    }

    val studySettings: Flow<StudySettings> = getUserSettings().map { settings ->
        settings?.let {
            StudySettings(
                defaultStudySessionLength = it.defaultStudySessionLength,
                defaultBreakLength = it.defaultBreakLength,
                longBreakLength = it.longBreakLength,
                sessionsUntilLongBreak = it.sessionsUntilLongBreak,
                autoStartBreaks = it.autoStartBreaks,
                autoStartSessions = it.autoStartSessions,
                soundEnabled = it.soundEnabled,
                vibrationEnabled = it.vibrationEnabled
            )
        } ?: StudySettings.default()
    }

    val goalSettings: Flow<GoalSettings> = getUserSettings().map { settings ->
        settings?.let {
            GoalSettings(
                dailyStudyGoalMinutes = it.dailyStudyGoalMinutes,
                dailyTaskGoal = it.dailyTaskGoal,
                weeklyStudyGoalMinutes = it.weeklyStudyGoalMinutes,
                weeklyTaskGoal = it.weeklyTaskGoal,
                adaptiveGoals = it.adaptiveGoals,
                goalDifficulty = it.goalDifficulty
            )
        } ?: GoalSettings.default()
    }

    val privacySettings: Flow<PrivacySettings> = getUserSettings().map { settings ->
        settings?.let {
            PrivacySettings(
                profilePublic = it.profilePublic
            )
        } ?: PrivacySettings.default()
    }

    val dataSettings: Flow<DataSettings> = getUserSettings().map { settings ->
        settings?.let {
            DataSettings(
                autoSyncEnabled = it.autoSyncEnabled,
                syncOnlyOnWifi = it.syncOnlyOnWifi,
                dataUsageOptimization = it.dataUsageOptimization,
                offlineMode = it.offlineMode,
                backupEnabled = it.backupEnabled,
                backupFrequency = it.backupFrequency
            )
        } ?: DataSettings.default()
    }

    val accessibilitySettings: Flow<AccessibilitySettings> = getUserSettings().map { settings ->
        settings?.let {
            AccessibilitySettings(
                highContrastMode = it.highContrastMode,
                largeTextMode = it.largeTextMode,
                screenReaderOptimized = it.screenReaderOptimized,
                reducedMotion = it.reducedMotion,
                colorBlindFriendly = it.colorBlindFriendly
            )
        } ?: AccessibilitySettings.default()
    }

    val localizationSettings: Flow<LocalizationSettings> = getUserSettings().map { settings ->
        settings?.let {
            LocalizationSettings(
                language = it.language,
                dateFormat = it.dateFormat,
                timeFormat = it.timeFormat,
                firstDayOfWeek = it.firstDayOfWeek
            )
        } ?: LocalizationSettings.default()
    }

    // Settings operations
    suspend fun getUserSettingsSync(userId: String = "default_user"): UserSettingsEntity? =
        userSettingsDao.getUserSettingsSync(userId)

    suspend fun insertUserSettings(settings: UserSettingsEntity) {
        userSettingsDao.insertUserSettings(settings)
        triggerRefresh()
    }

    suspend fun updateUserSettings(settings: UserSettingsEntity) {
        userSettingsDao.updateUserSettings(settings)
        triggerRefresh()
    }

    suspend fun deleteUserSettings(userId: String = "default_user") {
        userSettingsDao.deleteUserSettingsByUserId(userId)
        triggerRefresh()
    }

    suspend fun deleteAllUserSettings() {
        userSettingsDao.deleteAllUserSettings()
        triggerRefresh()
    }

    // Individual setting operations
    suspend fun updateNotificationsEnabled(enabled: Boolean, userId: String = "default_user") {
        userSettingsDao.updateNotificationsEnabled(enabled, System.currentTimeMillis(), userId)
        emitSettingsChange("notifications", enabled)
        triggerRefresh()
    }

    suspend fun updateDailyReminderEnabled(enabled: Boolean, userId: String = "default_user") {
        userSettingsDao.updateDailyReminderEnabled(enabled, System.currentTimeMillis(), userId)
        emitSettingsChange("dailyReminder", enabled)
        triggerRefresh()
    }

    suspend fun updateDailyReminderTime(time: String, userId: String = "default_user") {
        userSettingsDao.updateDailyReminderTime(time, System.currentTimeMillis(), userId)
        emitSettingsChange("dailyReminderTime", time)
        triggerRefresh()
    }

    suspend fun updateTheme(theme: String, userId: String = "default_user") {
        userSettingsDao.updateTheme(theme, System.currentTimeMillis(), userId)
        emitSettingsChange("theme", theme)
        triggerRefresh()
    }

    suspend fun updateAccentColor(color: String, userId: String = "default_user") {
        userSettingsDao.updateAccentColor(color, System.currentTimeMillis(), userId)
        emitSettingsChange("accentColor", color)
        triggerRefresh()
    }

    suspend fun updateUseDynamicColors(enabled: Boolean, userId: String = "default_user") {
        userSettingsDao.updateUseDynamicColors(enabled, System.currentTimeMillis(), userId)
        emitSettingsChange("dynamicColors", enabled)
        triggerRefresh()
    }

    suspend fun updateFontSize(size: String, userId: String = "default_user") {
        userSettingsDao.updateFontSize(size, System.currentTimeMillis(), userId)
        emitSettingsChange("fontSize", size)
        triggerRefresh()
    }

    suspend fun updateDefaultStudySessionLength(length: Int, userId: String = "default_user") {
        userSettingsDao.updateDefaultStudySessionLength(length, System.currentTimeMillis(), userId)
        emitSettingsChange("studySessionLength", length)
        triggerRefresh()
    }

    suspend fun updateDailyStudyGoalMinutes(goal: Int, userId: String = "default_user") {
        userSettingsDao.updateDailyStudyGoalMinutes(goal, System.currentTimeMillis(), userId)
        emitSettingsChange("dailyStudyGoal", goal)
        triggerRefresh()
    }

    suspend fun updateDailyTaskGoal(goal: Int, userId: String = "default_user") {
        userSettingsDao.updateDailyTaskGoal(goal, System.currentTimeMillis(), userId)
        emitSettingsChange("dailyTaskGoal", goal)
        triggerRefresh()
    }

    suspend fun updateProfilePublic(isPublic: Boolean, userId: String = "default_user") {
        userSettingsDao.updateProfilePublic(isPublic, System.currentTimeMillis(), userId)
        emitSettingsChange("profilePublic", isPublic)
        triggerRefresh()
    }

    suspend fun updateAutoSyncEnabled(enabled: Boolean, userId: String = "default_user") {
        userSettingsDao.updateAutoSyncEnabled(enabled, System.currentTimeMillis(), userId)
        emitSettingsChange("autoSync", enabled)
        triggerRefresh()
    }

    suspend fun updateLanguage(language: String, userId: String = "default_user") {
        userSettingsDao.updateLanguage(language, System.currentTimeMillis(), userId)
        emitSettingsChange("language", language)
        triggerRefresh()
    }

    // Individual setting getters
    suspend fun getNotificationsEnabled(userId: String = "default_user"): Boolean =
        userSettingsDao.getNotificationsEnabled(userId) ?: true

    suspend fun getDailyReminderEnabled(userId: String = "default_user"): Boolean =
        userSettingsDao.getDailyReminderEnabled(userId) ?: true

    suspend fun getDailyReminderTime(userId: String = "default_user"): String =
        userSettingsDao.getDailyReminderTime(userId) ?: "09:00"

    suspend fun getTheme(userId: String = "default_user"): String =
        userSettingsDao.getTheme(userId) ?: "system"

    suspend fun getAccentColor(userId: String = "default_user"): String =
        userSettingsDao.getAccentColor(userId) ?: "#1976D2"

    suspend fun getDailyStudyGoalMinutes(userId: String = "default_user"): Int =
        userSettingsDao.getDailyStudyGoalMinutes(userId) ?: 120

    suspend fun getDailyTaskGoal(userId: String = "default_user"): Int =
        userSettingsDao.getDailyTaskGoal(userId) ?: 5

    suspend fun getAutoSyncEnabled(userId: String = "default_user"): Boolean =
        userSettingsDao.getAutoSyncEnabled(userId) ?: true

    // Transaction operations
    suspend fun getOrCreateUserSettings(userId: String = "default_user"): UserSettingsEntity =
        userSettingsDao.getOrCreateUserSettings(userId)

    suspend fun updateSetting(
        userId: String = "default_user",
        updateFunction: suspend (UserSettingsEntity) -> UserSettingsEntity
    ) {
        userSettingsDao.updateSetting(userId, updateFunction)
        triggerRefresh()
    }

    suspend fun resetToDefaults(userId: String = "default_user") {
        userSettingsDao.resetToDefaults(userId)
        emitSettingsChange("reset", true)
        triggerRefresh()
    }

    // Batch operations
    suspend fun updateNotificationSettings(
        settings: NotificationSettings,
        userId: String = "default_user"
    ) {
        updateSetting(userId) { current ->
            current.copy(
                notificationsEnabled = settings.notificationsEnabled,
                dailyReminderEnabled = settings.dailyReminderEnabled,
                dailyReminderTime = settings.dailyReminderTime,
                streakReminderEnabled = settings.streakReminderEnabled,
                achievementNotificationsEnabled = settings.achievementNotificationsEnabled,
                weeklyReportEnabled = settings.weeklyReportEnabled,
                weeklyReportDay = settings.weeklyReportDay
            )
        }
        emitSettingsChange("notificationSettings", settings)
    }

    suspend fun updateThemeSettings(
        settings: ThemeSettings,
        userId: String = "default_user"
    ) {
        updateSetting(userId) { current ->
            current.copy(
                theme = settings.theme,
                accentColor = settings.accentColor,
                useDynamicColors = settings.useDynamicColors,
                fontSize = settings.fontSize,
                reducedAnimations = settings.reducedAnimations,
                compactMode = settings.compactMode
            )
        }
        emitSettingsChange("themeSettings", settings)
    }

    suspend fun updateStudySettings(
        settings: StudySettings,
        userId: String = "default_user"
    ) {
        updateSetting(userId) { current ->
            current.copy(
                defaultStudySessionLength = settings.defaultStudySessionLength,
                defaultBreakLength = settings.defaultBreakLength,
                longBreakLength = settings.longBreakLength,
                sessionsUntilLongBreak = settings.sessionsUntilLongBreak,
                autoStartBreaks = settings.autoStartBreaks,
                autoStartSessions = settings.autoStartSessions,
                soundEnabled = settings.soundEnabled,
                vibrationEnabled = settings.vibrationEnabled
            )
        }
        emitSettingsChange("studySettings", settings)
    }

    suspend fun updateGoalSettings(
        settings: GoalSettings,
        userId: String = "default_user"
    ) {
        updateSetting(userId) { current ->
            current.copy(
                dailyStudyGoalMinutes = settings.dailyStudyGoalMinutes,
                dailyTaskGoal = settings.dailyTaskGoal,
                weeklyStudyGoalMinutes = settings.weeklyStudyGoalMinutes,
                weeklyTaskGoal = settings.weeklyTaskGoal,
                adaptiveGoals = settings.adaptiveGoals,
                goalDifficulty = settings.goalDifficulty
            )
        }
        emitSettingsChange("goalSettings", settings)
    }

    // Utility operations
    fun clearSettingsChangeNotification() {
        _settingsChanged.value = null
    }

    private fun triggerRefresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    private fun emitSettingsChange(key: String, value: Any) {
        _settingsChanged.value = SettingsChange(key, value, System.currentTimeMillis())
    }

    // Data models
    data class NotificationSettings(
        val notificationsEnabled: Boolean,
        val dailyReminderEnabled: Boolean,
        val dailyReminderTime: String,
        val streakReminderEnabled: Boolean,
        val achievementNotificationsEnabled: Boolean,
        val weeklyReportEnabled: Boolean,
        val weeklyReportDay: String
    ) {
        companion object {
            fun default() = NotificationSettings(
                true, true, "09:00", true, true, true, "Sunday"
            )
        }
    }

    data class ThemeSettings(
        val theme: String,
        val accentColor: String,
        val useDynamicColors: Boolean,
        val fontSize: String,
        val reducedAnimations: Boolean,
        val compactMode: Boolean
    ) {
        companion object {
            fun default() = ThemeSettings(
                "system", "#1976D2", true, "medium", false, false
            )
        }
    }

    data class StudySettings(
        val defaultStudySessionLength: Int,
        val defaultBreakLength: Int,
        val longBreakLength: Int,
        val sessionsUntilLongBreak: Int,
        val autoStartBreaks: Boolean,
        val autoStartSessions: Boolean,
        val soundEnabled: Boolean,
        val vibrationEnabled: Boolean
    ) {
        companion object {
            fun default() = StudySettings(
                25, 5, 15, 4, false, false, true, true
            )
        }
    }

    data class GoalSettings(
        val dailyStudyGoalMinutes: Int,
        val dailyTaskGoal: Int,
        val weeklyStudyGoalMinutes: Int,
        val weeklyTaskGoal: Int,
        val adaptiveGoals: Boolean,
        val goalDifficulty: String
    ) {
        companion object {
            fun default() = GoalSettings(
                120, 5, 840, 35, true, "medium"
            )
        }
    }

    data class PrivacySettings(
        val profilePublic: Boolean
    ) {
        companion object {
            fun default() = PrivacySettings(false)
        }
    }

    data class DataSettings(
        val autoSyncEnabled: Boolean,
        val syncOnlyOnWifi: Boolean,
        val dataUsageOptimization: Boolean,
        val offlineMode: Boolean,
        val backupEnabled: Boolean,
        val backupFrequency: String
    ) {
        companion object {
            fun default() = DataSettings(
                true, false, true, false, true, "weekly"
            )
        }
    }

    data class AccessibilitySettings(
        val highContrastMode: Boolean,
        val largeTextMode: Boolean,
        val screenReaderOptimized: Boolean,
        val reducedMotion: Boolean,
        val colorBlindFriendly: Boolean
    ) {
        companion object {
            fun default() = AccessibilitySettings(
                false, false, false, false, false
            )
        }
    }

    data class LocalizationSettings(
        val language: String,
        val dateFormat: String,
        val timeFormat: String,
        val firstDayOfWeek: String
    ) {
        companion object {
            fun default() = LocalizationSettings(
                "system", "system", "system", "system"
            )
        }
    }

    data class SettingsChange(
        val key: String,
        val value: Any,
        val timestamp: Long
    )
}
