package com.mtlc.studyplan.settings.repository

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {

    suspend fun <T> getSetting(key: String, defaultValue: T): T

    suspend fun <T> setSetting(key: String, value: T)

    fun <T> observeSetting(key: String, defaultValue: T): StateFlow<T>

    suspend fun hasSetting(key: String): Boolean

    suspend fun removeSetting(key: String)

    suspend fun getAllSettings(): Map<String, Any?>

    suspend fun getAllSettingsKeys(): Set<String>

    suspend fun clearAllSettings()

    suspend fun importSettings(settings: Map<String, Any?>)

    suspend fun exportSettings(): Map<String, Any?>

    suspend fun getSettingsByCategory(category: String): Map<String, Any?>

    suspend fun searchSettings(query: String): List<SettingItem>

    suspend fun getSettingMetadata(key: String): SettingMetadata

    suspend fun validateSetting(key: String, value: Any?): SettingValidationResult
}

data class SettingItem(
    val key: String,
    val title: String,
    val description: String,
    val value: Any?,
    val type: SettingType,
    val category: String
)

data class SettingMetadata(
    val key: String,
    val title: String,
    val description: String,
    val category: String,
    val type: SettingType,
    val isAdvanced: Boolean,
    val dependencies: List<SettingDependency>,
    val constraints: List<SettingConstraint>
)

data class SettingDependency(
    val key: String,
    val defaultValue: Any?,
    val validator: (Any?) -> Boolean
)

data class SettingConstraint(
    val type: ConstraintType,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val pattern: String? = null,
    val allowedValues: Set<Any>? = null
)

data class SettingValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

enum class SettingType {
    STRING,
    INTEGER,
    LONG,
    FLOAT,
    BOOLEAN,
    STRING_SET
}

enum class ConstraintType {
    RANGE,
    MIN_LENGTH,
    MAX_LENGTH,
    REGEX,
    ALLOWED_VALUES
}