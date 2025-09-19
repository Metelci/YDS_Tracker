package com.mtlc.studyplan.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorHandler
import com.mtlc.studyplan.core.error.ErrorLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for managing application settings with reactive updates and comprehensive error handling
 */
class SettingsRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val errorHandler = ErrorHandler(ErrorLogger(context))
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Internal state flows for reactive updates
    private val _settingsState = MutableStateFlow(SettingsState(emptyList(), emptyMap()))
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    private val _changeEvents = MutableSharedFlow<SettingChangeEvent>(replay = 0, extraBufferCapacity = 100)
    val changeEvents: SharedFlow<SettingChangeEvent> = _changeEvents.asSharedFlow()

    // Cache for preference values to avoid repeated SharedPreferences access
    private val valueCache = ConcurrentHashMap<String, Any?>()

    private val privacyState = MutableStateFlow(loadPrivacyData())
    private val notificationState = MutableStateFlow(loadNotificationData())
    private val gamificationState = MutableStateFlow(loadGamificationData())

    private val privacyKeys = setOf(
        SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED,
        SettingsKeys.Privacy.PROFILE_VISIBILITY_LEVEL,
        SettingsKeys.Privacy.ANONYMOUS_ANALYTICS,
        SettingsKeys.Privacy.PROGRESS_SHARING
    )

    private val notificationKeys = setOf(
        SettingsKeys.Notifications.PUSH_NOTIFICATIONS,
        SettingsKeys.Notifications.STUDY_REMINDERS,
        SettingsKeys.Notifications.STUDY_REMINDER_TIME,
        SettingsKeys.Notifications.ACHIEVEMENT_ALERTS,
        SettingsKeys.Notifications.EMAIL_SUMMARIES,
        SettingsKeys.Notifications.EMAIL_SUMMARY_FREQUENCY
    )

    private val gamificationKeys = setOf(
        SettingsKeys.Gamification.STREAK_TRACKING,
        SettingsKeys.Gamification.POINTS_REWARDS,
        SettingsKeys.Gamification.CELEBRATION_EFFECTS,
        SettingsKeys.Gamification.STREAK_RISK_WARNINGS
    )

    // Preference change listener for reactive updates
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        coroutineScope.launch {
            key?.let {
                valueCache.remove(it)
                when (it) {
                    in privacyKeys -> privacyState.value = loadPrivacyData()
                    in notificationKeys -> notificationState.value = loadNotificationData()
                    in gamificationKeys -> gamificationState.value = loadGamificationData()
                }
                _changeEvents.emit(
                    SettingChangeEvent(
                        key = it,
                        oldValue = null,
                        newValue = getValueForKey(it)
                    )
                )
                    )
                )
            }
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        initializeDefaultValues()
        loadSettingsState()
    }

    // MARK: - Public API

    /**
     * Get a boolean setting value with default fallback
     */
    fun getBoolean(key: String, defaultValue: Boolean = defaultValues.getBooleanOrDefault(key)): Boolean {
        return getCachedValue(key) as? Boolean ?: run {
            val value = preferences.getBoolean(key, defaultValue)
            valueCache[key] = value
            value
        }
    }

    /**
     * Get an integer setting value with default fallback
     */
    fun getInt(key: String, defaultValue: Int = defaultValues.getIntOrDefault(key)): Int {
        return getCachedValue(key) as? Int ?: run {
            val value = preferences.getInt(key, defaultValue)
            valueCache[key] = value
            value
        }
    }

    /**
     * Get a float setting value with default fallback
     */
    fun getFloat(key: String, defaultValue: Float = defaultValues.getFloatOrDefault(key)): Float {
        return getCachedValue(key) as? Float ?: run {
            val value = preferences.getFloat(key, defaultValue)
            valueCache[key] = value
            value
        }
    }

    /**
     * Get a string setting value with default fallback
     */
    fun getString(key: String, defaultValue: String = defaultValues.getStringOrDefault(key)): String {
        return getCachedValue(key) as? String ?: run {
            val value = preferences.getString(key, defaultValue) ?: defaultValue
            valueCache[key] = value
            value
        }
    }

    /**
     * Get a string set setting value with default fallback
     */
    fun getStringSet(key: String, defaultValue: Set<String> = defaultValues.getStringSetOrDefault(key)): Set<String> {
        return getCachedValue(key) as? Set<String> ?: run {
            val value = preferences.getStringSet(key, defaultValue) ?: defaultValue
            valueCache[key] = value
            value
        }
    }

    /**
     * Observe privacy settings.
     */
    fun getPrivacySettings(): Flow<PrivacyData> = privacyState.asStateFlow()

    /**
     * Observe notification settings.
     */
    fun getNotificationSettings(): Flow<NotificationData> = notificationState.asStateFlow()

    /**
     * Observe gamification settings.
     */
    fun getGamificationSettings(): Flow<GamificationData> = gamificationState.asStateFlow()

    /**
     * Update a setting value with validation and error handling
     */
    suspend fun updateSetting(request: SettingsUpdateRequest): SettingsOperationResult {
        return try {
            when (request) {
                is SettingsUpdateRequest.UpdateBoolean -> {
                    validateAndUpdate(request.key, request.value) {
                        preferences.edit { putBoolean(request.key, request.value) }
                    }
                }
                is SettingsUpdateRequest.UpdateInt -> {
                    validateAndUpdate(request.key, request.value) {
                        preferences.edit { putInt(request.key, request.value) }
                    }
                }
                is SettingsUpdateRequest.UpdateFloat -> {
                    validateAndUpdate(request.key, request.value) {
                        preferences.edit { putFloat(request.key, request.value) }
                    }
                }
                is SettingsUpdateRequest.UpdateString -> {
                    validateAndUpdate(request.key, request.value) {
                        preferences.edit { putString(request.key, request.value) }
                    }
                }
                is SettingsUpdateRequest.UpdateSelection -> {
                    validateAndUpdate(request.key, request.selectedIndex) {
                        preferences.edit { putInt(request.key, request.selectedIndex) }
                    }
                }
                is SettingsUpdateRequest.PerformAction -> {
                    performAction(request.action)
                }
            }
        } catch (e: Exception) {
            errorHandler.logError(AppError.DataError.SaveFailed, "Failed to update setting", e)
            SettingsOperationResult.Error("Failed to update setting: ${e.message}", e)
        }
    }

    private fun handleOperationResult(result: SettingsOperationResult) {
        when (result) {
            is SettingsOperationResult.Success -> Unit
            is SettingsOperationResult.ValidationError -> {
                val message = result.errors.firstOrNull()?.message ?: "Invalid value"
                throw IllegalArgumentException(message)
            }
            is SettingsOperationResult.Error -> throw result.cause ?: RuntimeException(result.message)
        }
    }


    /**
     * Reset all settings to default values
     */
    suspend fun resetAllSettings(): SettingsOperationResult {
        return try {
            preferences.edit { clear() }
            valueCache.clear()
            initializeDefaultValues()
            loadSettingsState()
            SettingsOperationResult.Success
        } catch (e: Exception) {
            errorHandler.logError(AppError.DataError.ResetFailed, "Failed to reset settings", e)
            SettingsOperationResult.Error("Failed to reset settings: ${e.message}", e)
        }
    }

    /**
     * Export settings to JSON string
     */
    suspend fun exportSettings(): String {
        return try {
            val jsonObject = JSONObject()
            val allPrefs = preferences.all

            for ((key, value) in allPrefs) {
                when (value) {
                    is Boolean -> jsonObject.put(key, value)
                    is Int -> jsonObject.put(key, value)
                    is Float -> jsonObject.put(key, value.toDouble()) // JSON doesn't have float
                    is Long -> jsonObject.put(key, value)
                    is String -> jsonObject.put(key, value)
                    is Set<*> -> {
                        val array = JSONArray()
                        value.forEach { array.put(it.toString()) }
                        jsonObject.put(key, array)
                    }
                }
            }

            jsonObject.toString(2)
        } catch (e: Exception) {
            errorHandler.logError(AppError.DataError.ExportFailed, "Failed to export settings", e)
            throw e
        }
    }

    /**
     * Import settings from JSON string
     */
    suspend fun importSettings(jsonString: String): SettingsOperationResult {
        return try {
            val jsonObject = JSONObject(jsonString)
            val editor = preferences.edit()

            jsonObject.keys().forEach { key ->
                val value = jsonObject.get(key)
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is Long -> editor.putLong(key, value)
                    is String -> editor.putString(key, value)
                    is JSONArray -> {
                        val stringSet = mutableSetOf<String>()
                        for (i in 0 until value.length()) {
                            stringSet.add(value.getString(i))
                        }
                        editor.putStringSet(key, stringSet)
                    }
                }
            }

            editor.apply()
            valueCache.clear()
            loadSettingsState()
            SettingsOperationResult.Success
        } catch (e: Exception) {
            errorHandler.logError(AppError.DataError.ImportFailed, "Failed to import settings", e)
            SettingsOperationResult.Error("Failed to import settings: ${e.message}", e)
        }
    }

    /**
     * Get reactive flow for a specific setting
     */
    fun <T> getSettingFlow(key: String, defaultValue: T): Flow<T> {
        return changeEvents
            .filter { it.key == key }
            .map { getValueForKey(key) as? T ?: defaultValue }
            .onStart { emit(getValueForKey(key) as? T ?: defaultValue) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Check if a setting has been modified from its default value
     */
    fun isModifiedFromDefault(key: String): Boolean {
        val currentValue = getValueForKey(key)
        val defaultValue = defaultValues.getValueForKey(key)
        return currentValue != defaultValue
    }

    /**
     * Get all modified settings
     */
    fun getModifiedSettings(): Map<String, Any?> {
        return SettingsKeys.allKeys
            .filter { isModifiedFromDefault(it) }
            .associateWith { getValueForKey(it) }
    }

    // MARK: - Private Implementation

    private fun getCachedValue(key: String): Any? = valueCache[key]

    private fun getValueForKey(key: String): Any? {
        return when (defaultValues.getTypeForKey(key)) {
            Boolean::class -> getBoolean(key)
            Int::class -> getInt(key)
            Float::class -> getFloat(key)
            String::class -> getString(key)
            else -> preferences.all[key]
        }
    }

    private suspend fun validateAndUpdate(key: String, value: Any, updateAction: () -> Unit): SettingsOperationResult {
        // Find the setting item for validation
        val settingItem = _settingsState.value.getSettingById(key)

        // Perform validation if setting item exists
        settingItem?.let { setting ->
            val validationRules = when (setting) {
                is SettingItem.ToggleSetting -> setting.validationRules
                is SettingItem.SelectionSetting<*> -> setting.validationRules
                is SettingItem.RangeSetting -> setting.validationRules
                is SettingItem.TextSetting -> setting.validationRules
                is SettingItem.ActionSetting -> emptyList()
            }

            val validationErrors = validationRules.mapNotNull { rule ->
                val result = rule.validate(value)
                if (result is ValidationResult.Invalid) result else null
            }

            if (validationErrors.isNotEmpty()) {
                return SettingsOperationResult.ValidationError(validationErrors)
            }
        }

        // Perform the update
        updateAction()
        valueCache[key] = value

        return SettingsOperationResult.Success
    }

    private suspend fun performAction(action: SettingAction): SettingsOperationResult {
        return try {
            when (action) {
                is SettingAction.ClearCache -> {
                    context.cacheDir?.deleteRecursively()
                    SettingsOperationResult.Success
                }
                is SettingAction.ResetSettings -> resetAllSettings()
                is SettingAction.ExportData -> {
                    exportSettings()
                    SettingsOperationResult.Success
                }
                is SettingAction.ResetProgress -> {
                    // Implementation depends on your progress data structure
                    SettingsOperationResult.Success
                }
                is SettingAction.SyncData -> {
                    // Implementation depends on your sync mechanism
                    SettingsOperationResult.Success
                }
                else -> SettingsOperationResult.Error("Action not implemented: $action")
            }
        } catch (e: Exception) {
            SettingsOperationResult.Error("Failed to perform action: ${e.message}", e)
        }
    }

    private fun initializeDefaultValues() {
        val editor = preferences.edit()
        var hasChanges = false

        // Initialize default values only if they don't already exist
        defaultValues.getAllDefaults().forEach { (key, defaultValue) ->
            if (!preferences.contains(key)) {
                when (defaultValue) {
                    is Boolean -> editor.putBoolean(key, defaultValue)
                    is Int -> editor.putInt(key, defaultValue)
                    is Float -> editor.putFloat(key, defaultValue)
                    is String -> editor.putString(key, defaultValue)
                    is Set<*> -> editor.putStringSet(key, defaultValue as Set<String>)
                }
                hasChanges = true
            }
        }

        if (hasChanges) {
            editor.apply()
        }
    }

    private fun loadSettingsState() {
        coroutineScope.launch {
            try {
                val categories = SettingsDefaults.getDefaultCategories()
                val sections = SettingsDefaults.getDefaultSections()

                _settingsState.value = SettingsState(
                    categories = categories,
                    sections = sections,
                    isLoading = false,
                    error = null,
                    hasUnsavedChanges = false,
                    lastSyncTime = preferences.getLong(SettingsKeys.LAST_SYNC_TIME, 0L).takeIf { it > 0 }
                )
            } catch (e: Exception) {
                errorHandler.logError(AppError.DataError.LoadFailed, "Failed to load settings state", e)
                _settingsState.value = _settingsState.value.copy(
                    isLoading = false,
                    error = "Failed to load settings: ${e.message}"
                )
            }
        }
    }

    suspend fun updatePrivacySetting(id: String, value: Any) {
        val result = when (id) {
            "profile_visibility_enabled" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for profile visibility enabled")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED,
                        enabled
                    )
                )
            }
            "profile_visibility_level" -> {
                val level = when (value) {
                    is ProfileVisibilityLevel -> value
                    is String -> ProfileVisibilityLevel.valueOf(value)
                    else -> throw IllegalArgumentException("Expected profile visibility level value")
                }
                updateSetting(
                    SettingsUpdateRequest.UpdateString(
                        SettingsKeys.Privacy.PROFILE_VISIBILITY_LEVEL,
                        level.name
                    )
                )
            }
            "anonymous_analytics" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for anonymous analytics")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Privacy.ANONYMOUS_ANALYTICS,
                        enabled
                    )
                )
            }
            "progress_sharing" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for progress sharing")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Privacy.PROGRESS_SHARING,
                        enabled
                    )
                )
            }
            else -> throw IllegalArgumentException("Unknown privacy setting: $id")
        }

        handleOperationResult(result)
        privacyState.value = loadPrivacyData()
    }

    suspend fun updateNotificationSetting(id: String, value: Any) {
        val result = when (id) {
            "push_notifications" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for push notifications")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Notifications.PUSH_NOTIFICATIONS,
                        enabled
                    )
                )
            }
            "study_reminders" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for study reminders")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Notifications.STUDY_REMINDERS,
                        enabled
                    )
                )
            }
            "study_reminder_time" -> {
                val timeValue = when (value) {
                    is TimeValue -> value
                    is String -> parseTimeValue(value)
                    else -> throw IllegalArgumentException("Expected TimeValue for reminder time")
                }
                updateSetting(
                    SettingsUpdateRequest.UpdateString(
                        SettingsKeys.Notifications.STUDY_REMINDER_TIME,
                        serializeTimeValue(timeValue)
                    )
                )
            }
            "achievement_alerts" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for achievement alerts")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Notifications.ACHIEVEMENT_ALERTS,
                        enabled
                    )
                )
            }
            "email_summaries" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for email summaries")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Notifications.EMAIL_SUMMARIES,
                        enabled
                    )
                )
            }
            "email_frequency" -> {
                val frequency = when (value) {
                    is EmailFrequency -> value
                    is String -> EmailFrequency.valueOf(value)
                    else -> throw IllegalArgumentException("Expected EmailFrequency value")
                }
                updateSetting(
                    SettingsUpdateRequest.UpdateString(
                        SettingsKeys.Notifications.EMAIL_SUMMARY_FREQUENCY,
                        frequency.name
                    )
                )
            }
            else -> throw IllegalArgumentException("Unknown notification setting: $id")
        }

        handleOperationResult(result)
        notificationState.value = loadNotificationData()
    }

    suspend fun updateGamificationSetting(id: String, value: Any) {
        val result = when (id) {
            "streak_tracking" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for streak tracking")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Gamification.STREAK_TRACKING,
                        enabled
                    )
                )
            }
            "points_rewards" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for points and rewards")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Gamification.POINTS_REWARDS,
                        enabled
                    )
                )
            }
            "celebration_effects" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for celebration effects")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Gamification.CELEBRATION_EFFECTS,
                        enabled
                    )
                )
            }
            "streak_risk_warnings" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for streak risk warnings")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Gamification.STREAK_RISK_WARNINGS,
                        enabled
                    )
                )
            }
            else -> throw IllegalArgumentException("Unknown gamification setting: $id")
        }

        handleOperationResult(result)
        gamificationState.value = loadGamificationData()
    }

    suspend fun sendTestNotification() {
        delay(300)
    }

    suspend fun exportPersonalData() {
        exportSettings()
    }

    suspend fun clearAllPersonalData() {
        preferences.edit {
            putBoolean(SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED, true)
            putString(SettingsKeys.Privacy.PROFILE_VISIBILITY_LEVEL, ProfileVisibilityLevel.FRIENDS_ONLY.name)
            putBoolean(SettingsKeys.Privacy.ANONYMOUS_ANALYTICS, true)
            putBoolean(SettingsKeys.Privacy.PROGRESS_SHARING, true)
        }

        valueCache.remove(SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED)
        valueCache.remove(SettingsKeys.Privacy.PROFILE_VISIBILITY_LEVEL)
        valueCache.remove(SettingsKeys.Privacy.ANONYMOUS_ANALYTICS)
        valueCache.remove(SettingsKeys.Privacy.PROGRESS_SHARING)

        privacyState.value = loadPrivacyData()
    }

    private fun loadPrivacyData(): PrivacyData {
        return PrivacyData(
            profileVisibilityEnabled = preferences.getBoolean(
                SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED,
                defaultValues.getBooleanOrDefault(SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED)
            ),
            profileVisibilityLevel = preferences.getString(
                SettingsKeys.Privacy.PROFILE_VISIBILITY_LEVEL,
                ProfileVisibilityLevel.FRIENDS_ONLY.name
            )?.let { runCatching { ProfileVisibilityLevel.valueOf(it) }.getOrDefault(ProfileVisibilityLevel.FRIENDS_ONLY) }
                ?: ProfileVisibilityLevel.FRIENDS_ONLY,
            anonymousAnalytics = preferences.getBoolean(
                SettingsKeys.Privacy.ANONYMOUS_ANALYTICS,
                defaultValues.getBooleanOrDefault(SettingsKeys.Privacy.ANONYMOUS_ANALYTICS)
            ),
            progressSharing = preferences.getBoolean(
                SettingsKeys.Privacy.PROGRESS_SHARING,
                defaultValues.getBooleanOrDefault(SettingsKeys.Privacy.PROGRESS_SHARING)
            )
        )
    }

    private fun loadNotificationData(): NotificationData {
        val timeString = preferences.getString(
            SettingsKeys.Notifications.STUDY_REMINDER_TIME,
            "09:00"
        )
        val frequencyString = preferences.getString(
            SettingsKeys.Notifications.EMAIL_SUMMARY_FREQUENCY,
            EmailFrequency.WEEKLY.name
        )

        return NotificationData(
            pushNotifications = preferences.getBoolean(
                SettingsKeys.Notifications.PUSH_NOTIFICATIONS,
                defaultValues.getBooleanOrDefault(SettingsKeys.Notifications.PUSH_NOTIFICATIONS)
            ),
            studyReminders = preferences.getBoolean(
                SettingsKeys.Notifications.STUDY_REMINDERS,
                defaultValues.getBooleanOrDefault(SettingsKeys.Notifications.STUDY_REMINDERS)
            ),
            studyReminderTime = parseTimeValue(timeString),
            achievementAlerts = preferences.getBoolean(
                SettingsKeys.Notifications.ACHIEVEMENT_ALERTS,
                defaultValues.getBooleanOrDefault(SettingsKeys.Notifications.ACHIEVEMENT_ALERTS)
            ),
            emailSummaries = preferences.getBoolean(
                SettingsKeys.Notifications.EMAIL_SUMMARIES,
                defaultValues.getBooleanOrDefault(SettingsKeys.Notifications.EMAIL_SUMMARIES)
            ),
            emailFrequency = runCatching { EmailFrequency.valueOf(frequencyString ?: EmailFrequency.WEEKLY.name) }
                .getOrDefault(EmailFrequency.WEEKLY)
        )
    }

    private fun loadGamificationData(): GamificationData {
        return GamificationData(
            streakTracking = preferences.getBoolean(
                SettingsKeys.Gamification.STREAK_TRACKING,
                defaultValues.getBooleanOrDefault(SettingsKeys.Gamification.STREAK_TRACKING)
            ),
            pointsRewards = preferences.getBoolean(
                SettingsKeys.Gamification.POINTS_REWARDS,
                defaultValues.getBooleanOrDefault(SettingsKeys.Gamification.POINTS_REWARDS)
            ),
            celebrationEffects = preferences.getBoolean(
                SettingsKeys.Gamification.CELEBRATION_EFFECTS,
                defaultValues.getBooleanOrDefault(SettingsKeys.Gamification.CELEBRATION_EFFECTS)
            ),
            streakRiskWarnings = preferences.getBoolean(
                SettingsKeys.Gamification.STREAK_RISK_WARNINGS,
                defaultValues.getBooleanOrDefault(SettingsKeys.Gamification.STREAK_RISK_WARNINGS)
            )
        )
    }

    private fun serializeTimeValue(timeValue: TimeValue): String {
        val hour = timeValue.hour.coerceIn(0, 23)
        val minute = timeValue.minute.coerceIn(0, 59)
        return "%02d:%02d".format(hour, minute)
    }

    private fun parseTimeValue(raw: String?): TimeValue {
        if (raw.isNullOrBlank()) {
            return TimeValue(9, 0)
        }
        val parts = raw.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return TimeValue(hour, minute)
    }

    fun dispose() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        coroutineScope.cancel()
    }

    companion object {
        private const val PREFS_NAME = "study_plan_settings"
    }
}

/**
 * Default values for all settings
 */
private object defaultValues {
    private val defaults = mapOf<String, Any>(
        // Privacy defaults
        SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED to true,
        SettingsKeys.Privacy.PROFILE_VISIBILITY_LEVEL to ProfileVisibilityLevel.FRIENDS_ONLY.name,
        SettingsKeys.Privacy.ANONYMOUS_ANALYTICS to false,
        SettingsKeys.Privacy.PROGRESS_SHARING to true,
        SettingsKeys.Privacy.DATA_COLLECTION_CONSENT to false,
        SettingsKeys.Privacy.CRASH_REPORTING to true,
        SettingsKeys.Privacy.PERFORMANCE_ANALYTICS to false,
        SettingsKeys.Privacy.LOCATION_SHARING to false,
        SettingsKeys.Privacy.CONTACT_SYNC to false,

        // Notification defaults
        SettingsKeys.Notifications.PUSH_NOTIFICATIONS to true,
        SettingsKeys.Notifications.STUDY_REMINDERS to true,
        SettingsKeys.Notifications.STUDY_REMINDER_TIME to "09:00",
        SettingsKeys.Notifications.ACHIEVEMENT_ALERTS to true,
        SettingsKeys.Notifications.EMAIL_SUMMARIES to false,
        SettingsKeys.Notifications.EMAIL_SUMMARY_FREQUENCY to EmailFrequency.WEEKLY.name,
        SettingsKeys.Notifications.WEEKLY_REPORTS to true,
        SettingsKeys.Notifications.STREAK_WARNINGS to true,
        SettingsKeys.Notifications.GOAL_REMINDERS to true,
        SettingsKeys.Notifications.SOCIAL_NOTIFICATIONS to true,
        SettingsKeys.Notifications.QUIET_HOURS_ENABLED to false,
        SettingsKeys.Notifications.QUIET_HOURS_START to "22:00",
        SettingsKeys.Notifications.QUIET_HOURS_END to "08:00",
        SettingsKeys.Notifications.NOTIFICATION_SOUND to "default",
        SettingsKeys.Notifications.VIBRATION_ENABLED to true,

        // Gamification defaults
        SettingsKeys.Gamification.STREAK_TRACKING to true,
        SettingsKeys.Gamification.POINTS_REWARDS to true,
        SettingsKeys.Gamification.CELEBRATION_EFFECTS to true,
        SettingsKeys.Gamification.STREAK_RISK_WARNINGS to true,
        SettingsKeys.Gamification.ACHIEVEMENT_BADGES to true,
        SettingsKeys.Gamification.LEVEL_PROGRESSION to true,
        SettingsKeys.Gamification.DAILY_CHALLENGES to true,
        SettingsKeys.Gamification.LEADERBOARD_ENABLED to true,
        SettingsKeys.Gamification.XP_MULTIPLIERS to true,
        SettingsKeys.Gamification.REWARD_ANIMATIONS to true,

        // Task defaults
        SettingsKeys.Tasks.SMART_SCHEDULING to true,
        SettingsKeys.Tasks.AUTO_DIFFICULTY to false,
        SettingsKeys.Tasks.DAILY_GOAL_REMINDERS to true,
        SettingsKeys.Tasks.WEEKEND_MODE to false,
        SettingsKeys.Tasks.ADAPTIVE_LEARNING to true,
        SettingsKeys.Tasks.BREAK_REMINDERS to true,
        SettingsKeys.Tasks.SESSION_TIMEOUT to 30,
        SettingsKeys.Tasks.AUTO_PAUSE to false,
        SettingsKeys.Tasks.PROGRESS_TRACKING to true,
        SettingsKeys.Tasks.DIFFICULTY_ADJUSTMENT to true,
        SettingsKeys.Tasks.SPACED_REPETITION to true,
        SettingsKeys.Tasks.CUSTOM_GOALS to false,

        // Navigation defaults
        SettingsKeys.Navigation.BOTTOM_NAVIGATION to true,
        SettingsKeys.Navigation.HAPTIC_FEEDBACK to true,
        SettingsKeys.Navigation.GESTURE_NAVIGATION to false,
        SettingsKeys.Navigation.SWIPE_ACTIONS to true,
        SettingsKeys.Navigation.QUICK_ACCESS_TOOLBAR to false,
        SettingsKeys.Navigation.TAB_PERSISTENCE to true,
        SettingsKeys.Navigation.DOUBLE_TAP_EXIT to true,

        // Social defaults
        SettingsKeys.Social.STUDY_BUDDY_MATCHING to false,
        SettingsKeys.Social.SHARE_ACTIVITY to false,
        SettingsKeys.Social.GROUP_NOTIFICATIONS to true,
        SettingsKeys.Social.LEADERBOARD_PARTICIPATION to false,
        SettingsKeys.Social.FRIEND_REQUESTS to true,
        SettingsKeys.Social.STUDY_GROUPS to false,
        SettingsKeys.Social.ACHIEVEMENT_SHARING to true,
        SettingsKeys.Social.PROGRESS_COMPARISON to false,
        SettingsKeys.Social.COLLABORATIVE_LEARNING to false,
        SettingsKeys.Social.PEER_CHALLENGES to false,

        // Study defaults
        SettingsKeys.Study.DEFAULT_SESSION_LENGTH to 25,
        SettingsKeys.Study.PREFERRED_STUDY_TIME to "morning",
        SettingsKeys.Study.LEARNING_STYLE to "visual",
        SettingsKeys.Study.DIFFICULTY_PREFERENCE to "adaptive",
        SettingsKeys.Study.FOCUS_MODE to false,
        SettingsKeys.Study.BACKGROUND_SOUNDS to false,
        SettingsKeys.Study.TIMER_STYLE to "pomodoro",
        SettingsKeys.Study.PROGRESS_INDICATORS to true,
        SettingsKeys.Study.STUDY_STREAKS to true,
        SettingsKeys.Study.REST_INTERVALS to 5,

        // Appearance defaults
        SettingsKeys.Appearance.THEME_MODE to "system",
        SettingsKeys.Appearance.ACCENT_COLOR to "blue",
        SettingsKeys.Appearance.FONT_SIZE to 1.0f,
        SettingsKeys.Appearance.FONT_FAMILY to "default",
        SettingsKeys.Appearance.ANIMATION_SPEED to 1.0f,
        SettingsKeys.Appearance.CARD_STYLE to "rounded",
        SettingsKeys.Appearance.LAYOUT_DENSITY to "comfortable",

        // Audio defaults
        SettingsKeys.Audio.MASTER_VOLUME to 0.7f,
        SettingsKeys.Audio.NOTIFICATION_VOLUME to 0.5f,
        SettingsKeys.Audio.FEEDBACK_SOUNDS to true,
        SettingsKeys.Audio.SUCCESS_SOUND to true,
        SettingsKeys.Audio.ERROR_SOUND to true,
        SettingsKeys.Audio.BACKGROUND_MUSIC to false,
        SettingsKeys.Audio.PRONUNCIATION_AUDIO to true,
        SettingsKeys.Audio.AUDIO_QUALITY to "high",

        // Data defaults
        SettingsKeys.Data.AUTO_SYNC to true,
        SettingsKeys.Data.SYNC_FREQUENCY to 60, // minutes
        SettingsKeys.Data.WIFI_ONLY_SYNC to false,
        SettingsKeys.Data.BACKUP_ENABLED to true,
        SettingsKeys.Data.CACHE_SIZE_LIMIT to 100, // MB
        SettingsKeys.Data.OFFLINE_MODE to false,
        SettingsKeys.Data.DATA_SAVER_MODE to false,
        SettingsKeys.Data.AUTO_CLEANUP to true,
        SettingsKeys.Data.EXPORT_FORMAT to "json",
        SettingsKeys.Data.CLOUD_STORAGE to false
    )

    fun getAllDefaults(): Map<String, Any> = defaults

    fun getBooleanOrDefault(key: String): Boolean = defaults[key] as? Boolean ?: false
    fun getIntOrDefault(key: String): Int = defaults[key] as? Int ?: 0
    fun getFloatOrDefault(key: String): Float = defaults[key] as? Float ?: 0f
    fun getStringOrDefault(key: String): String = defaults[key] as? String ?: ""
    fun getStringSetOrDefault(key: String): Set<String> = defaults[key] as? Set<String> ?: emptySet()

    fun getValueForKey(key: String): Any? = defaults[key]

    fun getTypeForKey(key: String): kotlin.reflect.KClass<*>? = defaults[key]?.let { it::class }
}


