package com.mtlc.studyplan.settings.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val settingsCache = ConcurrentHashMap<String, Any?>()
    private val settingsFlows = ConcurrentHashMap<String, MutableStateFlow<Any?>>()
    private val mutex = Mutex()

    init {
        // Pre-populate cache with existing settings
        loadAllSettingsToCache()

        // Listen for preference changes
        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if (key != null) {
                updateCacheAndFlow(key)
            }
        }
    }

    override suspend fun <T> getSetting(key: String, defaultValue: T): T {
        return mutex.withLock {
            @Suppress("UNCHECKED_CAST")
            (settingsCache[key] as? T) ?: run {
                val value = getSettingFromPreferences(key, defaultValue)
                settingsCache[key] = value
                value
            }
        }
    }

    override suspend fun <T> setSetting(key: String, value: T) {
        mutex.withLock {
            saveSettingToPreferences(key, value)
            settingsCache[key] = value
            updateFlow(key, value)
        }
    }

    override fun <T> observeSetting(key: String, defaultValue: T): StateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return settingsFlows.getOrPut(key) {
            MutableStateFlow(settingsCache[key] ?: defaultValue)
        } as StateFlow<T>
    }

    override suspend fun hasSetting(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    override suspend fun removeSetting(key: String) {
        mutex.withLock {
            sharedPreferences.edit().remove(key).apply()
            settingsCache.remove(key)
            settingsFlows.remove(key)
        }
    }

    override suspend fun getAllSettings(): Map<String, Any?> {
        return mutex.withLock {
            settingsCache.toMap()
        }
    }

    override suspend fun getAllSettingsKeys(): Set<String> {
        return sharedPreferences.all.keys
    }

    override suspend fun clearAllSettings() {
        mutex.withLock {
            sharedPreferences.edit().clear().apply()
            settingsCache.clear()
            settingsFlows.clear()
        }
    }

    override suspend fun importSettings(settings: Map<String, Any?>) {
        mutex.withLock {
            val editor = sharedPreferences.edit()

            settings.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, value as Set<String>)
                    }
                    null -> editor.remove(key)
                }

                settingsCache[key] = value
                updateFlow(key, value)
            }

            editor.apply()
        }
    }

    override suspend fun exportSettings(): Map<String, Any?> {
        return getAllSettings()
    }

    override suspend fun getSettingsByCategory(category: String): Map<String, Any?> {
        return mutex.withLock {
            settingsCache.filterKeys { it.startsWith("${category}_") }
        }
    }

    override suspend fun searchSettings(query: String): List<SettingItem> {
        val allSettings = getAllSettings()
        val searchResults = mutableListOf<SettingItem>()

        allSettings.forEach { (key, value) ->
            val settingInfo = getSettingMetadata(key)
            val title = settingInfo.title
            val description = settingInfo.description

            if (title.contains(query, ignoreCase = true) ||
                description.contains(query, ignoreCase = true) ||
                key.contains(query, ignoreCase = true) ||
                value.toString().contains(query, ignoreCase = true)
            ) {
                searchResults.add(
                    SettingItem(
                        key = key,
                        title = title,
                        description = description,
                        value = value,
                        type = getSettingType(value),
                        category = getCategoryFromKey(key)
                    )
                )
            }
        }

        return searchResults.sortedBy { it.title }
    }

    override suspend fun getSettingMetadata(key: String): SettingMetadata {
        // This would typically come from a configuration file or database
        // For now, we'll provide some basic metadata based on key patterns
        return when {
            key.contains("notification") -> SettingMetadata(
                key = key,
                title = formatKeyToTitle(key),
                description = "Controls notification behavior",
                category = "notifications",
                type = getSettingType(settingsCache[key]),
                isAdvanced = false,
                dependencies = emptyList(),
                constraints = emptyList()
            )
            key.contains("privacy") -> SettingMetadata(
                key = key,
                title = formatKeyToTitle(key),
                description = "Privacy and security setting",
                category = "privacy",
                type = getSettingType(settingsCache[key]),
                isAdvanced = false,
                dependencies = emptyList(),
                constraints = emptyList()
            )
            key.contains("accessibility") -> SettingMetadata(
                key = key,
                title = formatKeyToTitle(key),
                description = "Accessibility enhancement",
                category = "accessibility",
                type = getSettingType(settingsCache[key]),
                isAdvanced = false,
                dependencies = emptyList(),
                constraints = emptyList()
            )
            key.contains("advanced") -> SettingMetadata(
                key = key,
                title = formatKeyToTitle(key),
                description = "Advanced configuration option",
                category = "advanced",
                type = getSettingType(settingsCache[key]),
                isAdvanced = true,
                dependencies = emptyList(),
                constraints = emptyList()
            )
            else -> SettingMetadata(
                key = key,
                title = formatKeyToTitle(key),
                description = "Application setting",
                category = "general",
                type = getSettingType(settingsCache[key]),
                isAdvanced = false,
                dependencies = emptyList(),
                constraints = emptyList()
            )
        }
    }

    override suspend fun validateSetting(key: String, value: Any?): SettingValidationResult {
        val metadata = getSettingMetadata(key)

        // Check type compatibility
        if (!isTypeCompatible(value, metadata.type)) {
            return SettingValidationResult(
                isValid = false,
                errors = listOf("Invalid type for setting $key"),
                warnings = emptyList()
            )
        }

        // Check constraints
        val constraintViolations = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        metadata.constraints.forEach { constraint ->
            when (constraint.type) {
                ConstraintType.RANGE -> {
                    if (value is Number && constraint.minValue != null && constraint.maxValue != null) {
                        val numValue = value.toDouble()
                        if (numValue < constraint.minValue || numValue > constraint.maxValue) {
                            constraintViolations.add("Value must be between ${constraint.minValue} and ${constraint.maxValue}")
                        }
                    }
                }
                ConstraintType.MIN_LENGTH -> {
                    if (value is String && constraint.minValue != null) {
                        if (value.length < constraint.minValue.toInt()) {
                            constraintViolations.add("Text must be at least ${constraint.minValue.toInt()} characters")
                        }
                    }
                }
                ConstraintType.MAX_LENGTH -> {
                    if (value is String && constraint.maxValue != null) {
                        if (value.length > constraint.maxValue.toInt()) {
                            constraintViolations.add("Text must be at most ${constraint.maxValue.toInt()} characters")
                        }
                    }
                }
                ConstraintType.REGEX -> {
                    if (value is String && constraint.pattern != null) {
                        if (!value.matches(Regex(constraint.pattern))) {
                            constraintViolations.add("Invalid format for $key")
                        }
                    }
                }
            }
        }

        // Check dependencies
        metadata.dependencies.forEach { dependency ->
            val dependencyValue = getSetting(dependency.key, dependency.defaultValue)
            if (!dependency.validator(dependencyValue)) {
                warnings.add("Setting ${dependency.key} should be configured first")
            }
        }

        return SettingValidationResult(
            isValid = constraintViolations.isEmpty(),
            errors = constraintViolations,
            warnings = warnings
        )
    }

    private fun loadAllSettingsToCache() {
        sharedPreferences.all.forEach { (key, value) ->
            settingsCache[key] = value
        }
    }

    private fun updateCacheAndFlow(key: String) {
        val value = sharedPreferences.all[key]
        settingsCache[key] = value
        updateFlow(key, value)
    }

    private fun updateFlow(key: String, value: Any?) {
        settingsFlows[key]?.value = value
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getSettingFromPreferences(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> sharedPreferences.getString(key, defaultValue) as T
            is Int -> sharedPreferences.getInt(key, defaultValue) as T
            is Long -> sharedPreferences.getLong(key, defaultValue) as T
            is Float -> sharedPreferences.getFloat(key, defaultValue) as T
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
            is Set<*> -> sharedPreferences.getStringSet(key, defaultValue as Set<String>) as T
            else -> defaultValue
        }
    }

    private fun <T> saveSettingToPreferences(key: String, value: T) {
        val editor = sharedPreferences.edit()

        when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                editor.putStringSet(key, value as Set<String>)
            }
            null -> editor.remove(key)
        }

        editor.apply()
    }

    private fun getSettingType(value: Any?): SettingType {
        return when (value) {
            is String -> SettingType.STRING
            is Int -> SettingType.INTEGER
            is Long -> SettingType.LONG
            is Float -> SettingType.FLOAT
            is Boolean -> SettingType.BOOLEAN
            is Set<*> -> SettingType.STRING_SET
            null -> SettingType.STRING
            else -> SettingType.STRING
        }
    }

    private fun isTypeCompatible(value: Any?, expectedType: SettingType): Boolean {
        return when (expectedType) {
            SettingType.STRING -> value is String || value == null
            SettingType.INTEGER -> value is Int
            SettingType.LONG -> value is Long
            SettingType.FLOAT -> value is Float
            SettingType.BOOLEAN -> value is Boolean
            SettingType.STRING_SET -> value is Set<*>
        }
    }

    private fun formatKeyToTitle(key: String): String {
        return key.split("_")
            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
    }

    private fun getCategoryFromKey(key: String): String {
        return when {
            key.startsWith("notification_") -> "notifications"
            key.startsWith("privacy_") -> "privacy"
            key.startsWith("accessibility_") -> "accessibility"
            key.startsWith("advanced_") -> "advanced"
            key.startsWith("backup_") -> "backup"
            key.startsWith("sync_") -> "sync"
            else -> "general"
        }
    }
}