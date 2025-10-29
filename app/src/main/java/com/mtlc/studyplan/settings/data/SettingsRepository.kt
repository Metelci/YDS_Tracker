package com.mtlc.studyplan.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorHandler
import com.mtlc.studyplan.core.error.ErrorLogger
import com.mtlc.studyplan.utils.settingsDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

import com.mtlc.studyplan.core.error.ErrorType

/**
 * Repository for managing application settings with reactive updates and comprehensive error handling
 */
class SettingsRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val errorHandler = ErrorHandler(ErrorLogger(context))
    private val preferences: SharedPreferences = DataStoreBackedPreferences(context.settingsDataStore)

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
        SettingsKeys.Privacy.CRASH_REPORTING
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
            }
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        initializeDefaultValues()
        loadSettingsState()
    }

    // MARK: - Public API

    fun getAllCategories(): Flow<List<SettingsCategory>> = settingsState.map { it.categories }.distinctUntilChanged()

    fun getAllCategoriesSync(): List<SettingsCategory> = settingsState.value.categories

    fun getCategorySettingsSnapshot(categoryId: String): Map<String, Any?> {
        val sections = settingsState.value.getSectionsForCategory(categoryId)
        val snapshot = mutableMapOf<String, Any?>()
        sections.forEach { section ->
            section.items.forEach { item ->
                val entry = readSettingEntry(item)
                if (entry != null && entry.second != null) {
                    snapshot[entry.first] = entry.second
                }
            }
        }
        return snapshot
    }

    fun getCategorySettingItems(categoryId: String): List<SettingItem> {
        return settingsState.value.getSectionsForCategory(categoryId).flatMap { it.items }
    }

    fun getCategorySections(categoryId: String): List<SettingsSection> {
        return settingsState.value.getSectionsForCategory(categoryId)
    }


    fun getLastBackupDate(): String? = null

    suspend fun exportAllSettings(): String = exportSettings()

    suspend fun importSettings(): SettingsOperationResult = SettingsOperationResult.Success

    suspend fun resetAllToDefaults(): SettingsOperationResult = resetAllSettings()

    suspend fun replaceCategorySettings(categoryId: String, data: Any?): Int = applyCategoryData(data)

    suspend fun mergeCategorySettings(categoryId: String, data: Any?, conflicts: MutableList<*>): Int = applyCategoryData(data)

    suspend fun importOnlyNewSettings(categoryId: String, data: Any?): Int {
        val entries = data as? Map<*, *> ?: return 0
        var applied = 0
        for ((key, value) in entries) {
            val preferenceKey = key as? String ?: continue
            if (!preferences.contains(preferenceKey) && applyPreferenceValue(preferenceKey, value)) {
                applied++
            }
        }
        return applied
    }

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
        return when (val cached = getCachedValue(key)) {
            is Set<*> -> cached.filterIsInstance<String>().toSet().takeIf { it.size == cached.size } ?: defaultValue
            null -> {
                val value = preferences.getStringSet(key, defaultValue) ?: defaultValue
                valueCache[key] = value
                value
            }
            else -> defaultValue
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
            errorHandler.logger.logError(AppError(type = ErrorType.DATA, message = "Failed to update setting", cause = e))
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
            errorHandler.logger.logError(AppError(type = ErrorType.DATA, message = "Failed to reset settings", cause = e))
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
            errorHandler.logger.logError(AppError(type = ErrorType.DATA, message = "Failed to export settings", cause = e))
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
            errorHandler.logger.logError(AppError(type = ErrorType.DATA, message = "Failed to import settings", cause = e))
            SettingsOperationResult.Error("Failed to import settings: ${e.message}", e)
        }
    }

    /**
     * Get reactive flow for Boolean settings
     */
    fun getBooleanSettingFlow(key: String, defaultValue: Boolean): Flow<Boolean> {
        return changeEvents
            .filter { it.key == key }
            .map { safeCastToBoolean(getValueForKey(key), defaultValue) }
            .onStart { emit(safeCastToBoolean(getValueForKey(key), defaultValue)) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Get reactive flow for String settings
     */
    fun getStringSettingFlow(key: String, defaultValue: String): Flow<String> {
        return changeEvents
            .filter { it.key == key }
            .map { safeCastToString(getValueForKey(key), defaultValue) }
            .onStart { emit(safeCastToString(getValueForKey(key), defaultValue)) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Get reactive flow for Int settings
     */
    fun getIntSettingFlow(key: String, defaultValue: Int): Flow<Int> {
        return changeEvents
            .filter { it.key == key }
            .map { safeCastToInt(getValueForKey(key), defaultValue) }
            .onStart { emit(safeCastToInt(getValueForKey(key), defaultValue)) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Get reactive flow for Float settings
     */
    fun getFloatSettingFlow(key: String, defaultValue: Float): Flow<Float> {
        return changeEvents
            .filter { it.key == key }
            .map { safeCastToFloat(getValueForKey(key), defaultValue) }
            .onStart { emit(safeCastToFloat(getValueForKey(key), defaultValue)) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Get reactive flow for StringSet settings
     */
    fun getStringSetSettingFlow(key: String, defaultValue: Set<String>): Flow<Set<String>> {
        return changeEvents
            .filter { it.key == key }
            .map { safeCastToStringSet(getValueForKey(key), defaultValue) }
            .onStart { emit(safeCastToStringSet(getValueForKey(key), defaultValue)) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Generic fallback for backwards compatibility
     */
    @Suppress("UNCHECKED_CAST") // Generic fallback - prefer type-specific methods above
    fun <T> getSettingFlow(key: String, defaultValue: T): Flow<T> {
        return when (defaultValue) {
            is Boolean -> getBooleanSettingFlow(key, defaultValue) as Flow<T>
            is String -> getStringSettingFlow(key, defaultValue) as Flow<T>
            is Int -> getIntSettingFlow(key, defaultValue) as Flow<T>
            is Float -> getFloatSettingFlow(key, defaultValue) as Flow<T>
            is Set<*> -> getStringSetSettingFlow(key, defaultValue as Set<String>) as Flow<T>
            else -> flowOf(defaultValue) // Unsupported type - return default
        }
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


    private fun readSettingEntry(item: SettingItem): Pair<String, Any?>? = when (item) {
        is SettingItem.ToggleSetting -> {
            val key = item.key ?: item.id
            key to getBoolean(key, item.defaultValue)
        }
        is SettingItem.SelectionSetting<*> -> {
            val key = item.key ?: item.id
            key to item.currentValue
        }
        is SettingItem.TimeSetting -> {
            val key = item.key ?: item.id
            key to mapOf("hour" to item.currentTime.hour, "minute" to item.currentTime.minute)
        }
        is SettingItem.ActionSetting -> null
        is SettingItem.RangeSetting -> item.key to getFloat(item.key, item.currentValue)
        is SettingItem.TextSetting -> item.key to getString(item.key, item.currentValue)
    }

    private suspend fun applyCategoryData(rawData: Any?): Int {
        val entries = rawData as? Map<*, *> ?: return 0
        var applied = 0
        for ((key, value) in entries) {
            val preferenceKey = key as? String ?: continue
            if (applyPreferenceValue(preferenceKey, value)) {
                applied++
            }
        }
        return applied
    }

    private suspend fun applyPreferenceValue(key: String, value: Any?): Boolean {
        val result = when (value) {
            null -> SettingsOperationResult.Error("No value provided for $key")
            is Boolean -> updateSetting(SettingsUpdateRequest.UpdateBoolean(key, value))
            is Number -> {
                val doubleValue = value.toDouble()
                if (doubleValue % 1.0 == 0.0) {
                    updateSetting(SettingsUpdateRequest.UpdateInt(key, doubleValue.toInt()))
                } else {
                    updateSetting(SettingsUpdateRequest.UpdateFloat(key, doubleValue.toFloat()))
                }
            }
            is String -> updateSetting(SettingsUpdateRequest.UpdateString(key, value))
            is Collection<*> -> {
                val stringSet = value.filterIsInstance<String>().toSet()
                preferences.edit { putStringSet(key, stringSet) }
                valueCache[key] = stringSet
                SettingsOperationResult.Success
            }
            is Map<*, *> -> {
                val hour = (value.get("hour") as? Number)?.toInt()
                val minute = (value.get("minute") as? Number)?.toInt()
                if (hour != null && minute != null) {
                    val timeValue = TimeValue(hour, minute)
                    updateSetting(
                        SettingsUpdateRequest.UpdateString(
                            key,
                            serializeTimeValue(timeValue)
                        )
                    )
                } else {
                    SettingsOperationResult.Error("Unsupported map structure for $key")
                }
            }
            else -> SettingsOperationResult.Error("Unsupported value type for $key")
        }

        return result is SettingsOperationResult.Success
    }

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

    /**
     * Type-safe settings value wrapper using sealed class pattern
     */
    sealed class SettingValue<out T> {
        data class BooleanValue(val value: Boolean) : SettingValue<Boolean>()
        data class IntValue(val value: Int) : SettingValue<Int>()
        data class FloatValue(val value: Float) : SettingValue<Float>()
        data class StringValue(val value: String) : SettingValue<String>()
        data class StringSetValue(val value: Set<String>) : SettingValue<Set<String>>()

        companion object {
            fun from(value: Any?): SettingValue<*>? {
                return when (value) {
                    is Boolean -> BooleanValue(value)
                    is Int -> IntValue(value)
                    is Float -> FloatValue(value)
                    is String -> StringValue(value)
                    is Set<*> -> StringSetValue(value.filterIsInstance<String>().toSet())
                    else -> null
                }
            }
        }

        fun extractBoolean(defaultValue: Boolean): Boolean {
            return if (this is BooleanValue) value else defaultValue
        }

        fun extractInt(defaultValue: Int): Int {
            return if (this is IntValue) value else defaultValue
        }

        fun extractFloat(defaultValue: Float): Float {
            return if (this is FloatValue) value else defaultValue
        }

        fun extractString(defaultValue: String): String {
            return if (this is StringValue) value else defaultValue
        }

        fun extractStringSet(defaultValue: Set<String>): Set<String> {
            return if (this is StringSetValue) value else defaultValue
        }
    }

    /**
     * Type-safe Boolean value extraction
     */
    private fun safeCastToBoolean(value: Any?, defaultValue: Boolean): Boolean {
        return try {
            val settingValue = SettingValue.from(value)
            settingValue?.extractBoolean(defaultValue) ?: defaultValue
        } catch (e: Exception) {
            logCastError(value, "Boolean", e)
            defaultValue
        }
    }

    /**
     * Type-safe String value extraction
     */
    private fun safeCastToString(value: Any?, defaultValue: String): String {
        return try {
            val settingValue = SettingValue.from(value)
            settingValue?.extractString(defaultValue) ?: defaultValue
        } catch (e: Exception) {
            logCastError(value, "String", e)
            defaultValue
        }
    }

    /**
     * Type-safe Int value extraction
     */
    private fun safeCastToInt(value: Any?, defaultValue: Int): Int {
        return try {
            val settingValue = SettingValue.from(value)
            settingValue?.extractInt(defaultValue) ?: defaultValue
        } catch (e: Exception) {
            logCastError(value, "Int", e)
            defaultValue
        }
    }

    /**
     * Type-safe Float value extraction
     */
    private fun safeCastToFloat(value: Any?, defaultValue: Float): Float {
        return try {
            val settingValue = SettingValue.from(value)
            settingValue?.extractFloat(defaultValue) ?: defaultValue
        } catch (e: Exception) {
            logCastError(value, "Float", e)
            defaultValue
        }
    }

    /**
     * Type-safe StringSet value extraction
     */
    private fun safeCastToStringSet(value: Any?, defaultValue: Set<String>): Set<String> {
        return try {
            val settingValue = SettingValue.from(value)
            settingValue?.extractStringSet(defaultValue) ?: defaultValue
        } catch (e: Exception) {
            logCastError(value, "Set<String>", e)
            defaultValue
        }
    }

    private fun logCastError(value: Any?, expectedType: String, exception: Exception) {
        errorHandler.logger.logError(
            AppError(
                type = ErrorType.DATA,
                message = "Type casting failed for value: $value, expected type: $expectedType",
                cause = exception
            )
        )
    }

    private suspend fun validateAndUpdate(key: String, value: Any, updateAction: () -> Unit): SettingsOperationResult {
        // Find the setting item for validation
        val settingItem = _settingsState.value.getSettingById(key)

        // Perform validation if setting item exists
        settingItem?.let { setting ->
            val validationRules = when (setting) {
                is SettingItem.ToggleSetting -> setting.validationRules
                is SettingItem.SelectionSetting<*> -> setting.validationRules
                is SettingItem.TimeSetting -> setting.validationRules
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
                    is Set<*> -> {
                        val stringSet = defaultValue.mapNotNull { element ->
                            when (element) {
                                null -> null
                                is String -> element
                                else -> element.toString()
                            }
                        }.toSet()
                        editor.putStringSet(key, stringSet)
                    }
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
                errorHandler.logger.logError(AppError(type = ErrorType.DATA, message = "Failed to load settings state", cause = e))
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
                    ?: throw IllegalArgumentException("Expected boolean for profile visibility")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED,
                        enabled
                    )
                )
            }
            "crash_reporting" -> {
                val enabled = value as? Boolean
                    ?: throw IllegalArgumentException("Expected boolean for crash reporting")
                updateSetting(
                    SettingsUpdateRequest.UpdateBoolean(
                        SettingsKeys.Privacy.CRASH_REPORTING,
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
            putBoolean(SettingsKeys.Privacy.CRASH_REPORTING, true)
        }

        valueCache.remove(SettingsKeys.Privacy.CRASH_REPORTING)

        privacyState.value = loadPrivacyData()
    }

    private fun loadPrivacyData(): PrivacyData {
        return PrivacyData(
            crashReporting = preferences.getBoolean(
                SettingsKeys.Privacy.CRASH_REPORTING,
                defaultValues.getBooleanOrDefault(SettingsKeys.Privacy.CRASH_REPORTING)
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

    /**
     * Get user settings as a flow
     */
    fun getUserSettings(): Flow<UserSettings> {
        return settingsState.map { _ ->
            UserSettings(
                notificationsEnabled = getBoolean(SettingsKeys.Notifications.PUSH_NOTIFICATIONS, true),
                studyRemindersEnabled = getBoolean(SettingsKeys.Notifications.STUDY_REMINDERS, true),
                achievementNotificationsEnabled = getBoolean(SettingsKeys.Notifications.ACHIEVEMENT_ALERTS, true),
                dailyGoalRemindersEnabled = getBoolean(SettingsKeys.Notifications.GOAL_REMINDERS, true),
                streakWarningsEnabled = getBoolean(SettingsKeys.Notifications.STREAK_WARNINGS, true),
                offlineModeEnabled = getBoolean(SettingsKeys.Data.OFFLINE_MODE, false),
                autoSyncEnabled = getBoolean(SettingsKeys.Data.AUTO_SYNC, true),
                gamificationEnabled = getBoolean(SettingsKeys.Gamification.STREAK_TRACKING, true),
                hapticFeedbackEnabled = getBoolean(SettingsKeys.Navigation.HAPTIC_FEEDBACK, true),
                weekendModeEnabled = getBoolean(SettingsKeys.Tasks.WEEKEND_MODE, false),
                autoDifficultyEnabled = getBoolean(SettingsKeys.Tasks.AUTO_DIFFICULTY, true),
                studyReminderTime = getString(SettingsKeys.Notifications.STUDY_REMINDER_TIME, "09:00"),
                dailyStudyGoalMinutes = 60 // Fixed: DAILY_GOAL_REMINDERS is a Boolean, not an Int
            )
        }
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
    private val defaults: Map<String, Any> = buildMap {

        // Privacy defaults
        put(SettingsKeys.Privacy.DATA_COLLECTION_CONSENT, false)
        put(SettingsKeys.Privacy.CRASH_REPORTING, true)

        // Notification defaults
        put(SettingsKeys.Notifications.PUSH_NOTIFICATIONS, true)
        put(SettingsKeys.Notifications.STUDY_REMINDERS, true)
        put(SettingsKeys.Notifications.STUDY_REMINDER_TIME, "09:00")
        put(SettingsKeys.Notifications.ACHIEVEMENT_ALERTS, true)
        put(SettingsKeys.Notifications.EMAIL_SUMMARIES, false)
        put(SettingsKeys.Notifications.EMAIL_SUMMARY_FREQUENCY, EmailFrequency.WEEKLY.name)
        put(SettingsKeys.Notifications.WEEKLY_REPORTS, true)
        put(SettingsKeys.Notifications.STREAK_WARNINGS, true)
        put(SettingsKeys.Notifications.GOAL_REMINDERS, true)
        put(SettingsKeys.Notifications.QUIET_HOURS_ENABLED, false)
        put(SettingsKeys.Notifications.QUIET_HOURS_START, "22:00")
        put(SettingsKeys.Notifications.QUIET_HOURS_END, "08:00")
        put(SettingsKeys.Notifications.NOTIFICATION_SOUND, "default")
        put(SettingsKeys.Notifications.VIBRATION_ENABLED, true)

        // Gamification defaults
        put(SettingsKeys.Gamification.STREAK_TRACKING, true)
        put(SettingsKeys.Gamification.POINTS_REWARDS, true)
        put(SettingsKeys.Gamification.CELEBRATION_EFFECTS, true)
        put(SettingsKeys.Gamification.STREAK_RISK_WARNINGS, true)
        put(SettingsKeys.Gamification.ACHIEVEMENT_BADGES, true)
        put(SettingsKeys.Gamification.LEVEL_PROGRESSION, true)
        put(SettingsKeys.Gamification.DAILY_CHALLENGES, true)
        put(SettingsKeys.Gamification.XP_MULTIPLIERS, true)
        put(SettingsKeys.Gamification.REWARD_ANIMATIONS, true)

        // Task defaults
        put(SettingsKeys.Tasks.AUTO_DIFFICULTY, false)
        put(SettingsKeys.Tasks.DAILY_GOAL_REMINDERS, true)
        put(SettingsKeys.Tasks.WEEKEND_MODE, false)
        put(SettingsKeys.Tasks.ADAPTIVE_LEARNING, true)
        put(SettingsKeys.Tasks.BREAK_REMINDERS, true)
        put(SettingsKeys.Tasks.SESSION_TIMEOUT, 30)
        put(SettingsKeys.Tasks.AUTO_PAUSE, false)
        put(SettingsKeys.Tasks.PROGRESS_TRACKING, true)
        put(SettingsKeys.Tasks.DIFFICULTY_ADJUSTMENT, true)
        put(SettingsKeys.Tasks.SPACED_REPETITION, true)
        put(SettingsKeys.Tasks.CUSTOM_GOALS, false)

        // Navigation defaults
        put(SettingsKeys.Navigation.HAPTIC_FEEDBACK, true)
        put(SettingsKeys.Navigation.GESTURE_NAVIGATION, false)
        put(SettingsKeys.Navigation.SWIPE_ACTIONS, true)
        put(SettingsKeys.Navigation.QUICK_ACCESS_TOOLBAR, false)
        put(SettingsKeys.Navigation.TAB_PERSISTENCE, true)
        put(SettingsKeys.Navigation.DOUBLE_TAP_EXIT, true)

        // Study defaults
        put(SettingsKeys.Study.DEFAULT_SESSION_LENGTH, 25)
        put(SettingsKeys.Study.PREFERRED_STUDY_TIME, "morning")
        put(SettingsKeys.Study.LEARNING_STYLE, "visual")
        put(SettingsKeys.Study.DIFFICULTY_PREFERENCE, "adaptive")
        put(SettingsKeys.Study.FOCUS_MODE, false)
        put(SettingsKeys.Study.BACKGROUND_SOUNDS, false)
        put(SettingsKeys.Study.TIMER_STYLE, "pomodoro")
        put(SettingsKeys.Study.PROGRESS_INDICATORS, true)
        put(SettingsKeys.Study.STUDY_STREAKS, true)
        put(SettingsKeys.Study.REST_INTERVALS, 5)

        // Appearance defaults
        // Theme mode removed; light theme only
        put(SettingsKeys.Appearance.ACCENT_COLOR, "blue")
        put(SettingsKeys.Appearance.FONT_SIZE, "normal")
        put(SettingsKeys.Appearance.FONT_FAMILY, "default")
        put(SettingsKeys.Appearance.ANIMATION_SPEED, "normal")
        put(SettingsKeys.Appearance.CARD_STYLE, "rounded")
        put(SettingsKeys.Appearance.LAYOUT_DENSITY, "comfortable")

        // Audio defaults
        put(SettingsKeys.Audio.MASTER_VOLUME, 0.7f)
        put(SettingsKeys.Audio.NOTIFICATION_VOLUME, 0.5f)
        put(SettingsKeys.Audio.FEEDBACK_SOUNDS, true)
        put(SettingsKeys.Audio.SUCCESS_SOUND, true)
        put(SettingsKeys.Audio.ERROR_SOUND, true)
        put(SettingsKeys.Audio.BACKGROUND_MUSIC, false)
        put(SettingsKeys.Audio.PRONUNCIATION_AUDIO, true)
        put(SettingsKeys.Audio.AUDIO_QUALITY, "high")

        // Data defaults
        put(SettingsKeys.Data.AUTO_SYNC, true)
        put(SettingsKeys.Data.SYNC_FREQUENCY, 60) // minutes
        put(SettingsKeys.Data.WIFI_ONLY_SYNC, false)
        put(SettingsKeys.Data.BACKUP_ENABLED, true)
        put(SettingsKeys.Data.CACHE_SIZE_LIMIT, 100) // MB
        put(SettingsKeys.Data.OFFLINE_MODE, false)
        put(SettingsKeys.Data.DATA_SAVER_MODE, false)
        put(SettingsKeys.Data.AUTO_CLEANUP, true)
        put(SettingsKeys.Data.EXPORT_FORMAT, "json")
    }

    fun getAllDefaults(): Map<String, Any> = defaults

    fun getBooleanOrDefault(key: String): Boolean = safeCastValue(defaults[key], false)
    fun getIntOrDefault(key: String): Int = safeCastValue(defaults[key], 0)
    fun getFloatOrDefault(key: String): Float = safeCastValue(defaults[key], 0f)
    fun getStringOrDefault(key: String): String = safeCastValue(defaults[key], "")
    fun getStringSetOrDefault(key: String): Set<String> = safeCastValue(defaults[key], emptySet<String>())

    fun getValueForKey(key: String): Any? = defaults[key]

    fun getTypeForKey(key: String): kotlin.reflect.KClass<*>? = defaults[key]?.let { it::class }

    /**
     * Safely cast default values with proper error handling
     */
    private inline fun <reified T> safeCastValue(value: Any?, defaultValue: T): T {
        return try {
            when (T::class) {
                Boolean::class -> (value as? Boolean ?: defaultValue) as T
                Int::class -> (value as? Int ?: defaultValue) as T
                Float::class -> (value as? Float ?: defaultValue) as T
                String::class -> (value as? String ?: defaultValue) as T
                Set::class -> @Suppress("UNCHECKED_CAST") (value as? Set<String> ?: defaultValue as Set<String>) as T
                else -> defaultValue
            }
        } catch (e: ClassCastException) {
            // Log error and return default
            println("Warning: Failed to cast default value for key, using default: $defaultValue")
            defaultValue
        }
    }
}
















