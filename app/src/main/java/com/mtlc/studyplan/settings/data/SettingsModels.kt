package com.mtlc.studyplan.settings.data

import androidx.annotation.DrawableRes
import kotlinx.coroutines.flow.Flow

/**
 * Represents a category of settings in the settings screen
 */
data class SettingsCategory(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
) {
    companion object {
        const val PRIVACY_ID = "privacy"
        const val NOTIFICATIONS_ID = "notifications"
        const val GAMIFICATION_ID = "gamification"
        const val TASKS_ID = "tasks"
        const val NAVIGATION_ID = "navigation"
        const val SOCIAL_ID = "social"
        const val ACCESSIBILITY_ID = "accessibility"
        const val DATA_ID = "data"
    }
}

/**
 * Sealed class representing different types of settings
 */
sealed class SettingItem {
    abstract val id: String
    abstract val title: String
    abstract val description: String
    abstract val isEnabled: Boolean
    abstract val category: String
    abstract val sortOrder: Int

    data class ToggleSetting(
        override val id: String,
        override val title: String,
        override val description: String,
        override val isEnabled: Boolean = true,
        override val category: String,
        override val sortOrder: Int = 0,
        val key: String,
        val defaultValue: Boolean = false,
        val requiresRestart: Boolean = false,
        val validationRules: List<ValidationRule> = emptyList()
    ) : SettingItem()

    data class SelectionSetting<T>(
        override val id: String,
        override val title: String,
        override val description: String,
        override val isEnabled: Boolean = true,
        override val category: String,
        override val sortOrder: Int = 0,
        val key: String,
        val options: List<SelectionOption<T>>,
        val selectedIndex: Int = 0,
        val requiresRestart: Boolean = false,
        val validationRules: List<ValidationRule> = emptyList()
    ) : SettingItem() {
        val selectedOption: SelectionOption<T>?
            get() = options.getOrNull(selectedIndex)

        val selectedValue: T?
            get() = selectedOption?.value
    }

    data class ActionSetting(
        override val id: String,
        override val title: String,
        override val description: String,
        override val isEnabled: Boolean = true,
        override val category: String,
        override val sortOrder: Int = 0,
        val action: SettingAction,
        val confirmationRequired: Boolean = false,
        val confirmationMessage: String? = null
    ) : SettingItem()

    data class RangeSetting(
        override val id: String,
        override val title: String,
        override val description: String,
        override val isEnabled: Boolean = true,
        override val category: String,
        override val sortOrder: Int = 0,
        val key: String,
        val currentValue: Float,
        val minValue: Float,
        val maxValue: Float,
        val step: Float = 1f,
        val unit: String = "",
        val formatPattern: String = "%.0f",
        val requiresRestart: Boolean = false,
        val validationRules: List<ValidationRule> = emptyList()
    ) : SettingItem()

    data class TextSetting(
        override val id: String,
        override val title: String,
        override val description: String,
        override val isEnabled: Boolean = true,
        override val category: String,
        override val sortOrder: Int = 0,
        val key: String,
        val currentValue: String,
        val placeholder: String = "",
        val inputType: TextInputType = TextInputType.TEXT,
        val maxLength: Int = -1,
        val requiresRestart: Boolean = false,
        val validationRules: List<ValidationRule> = emptyList()
    ) : SettingItem()
}

/**
 * Represents an option in a selection setting
 */
data class SelectionOption<T>(
    val label: String,
    val value: T,
    val description: String? = null,
    @DrawableRes val iconRes: Int? = null,
    val isEnabled: Boolean = true
)

/**
 * Represents an action that can be performed from settings
 */
sealed class SettingAction {
    object ClearCache : SettingAction()
    object ExportData : SettingAction()
    object ImportData : SettingAction()
    object ResetSettings : SettingAction()
    object ContactSupport : SettingAction()
    object ShowAbout : SettingAction()
    object OpenPrivacyPolicy : SettingAction()
    object OpenTermsOfService : SettingAction()
    object ResetProgress : SettingAction()
    object SyncData : SettingAction()
    data class OpenUrl(val url: String) : SettingAction()
    data class Custom(val actionId: String, val data: Map<String, Any> = emptyMap()) : SettingAction()
}

/**
 * Input types for text settings
 */
enum class TextInputType {
    TEXT,
    EMAIL,
    PASSWORD,
    NUMBER,
    PHONE,
    URL,
    MULTILINE
}

/**
 * Validation rules for settings
 */
sealed class ValidationRule {
    abstract fun validate(value: Any?): ValidationResult

    object Required : ValidationRule() {
        override fun validate(value: Any?): ValidationResult {
            return if (value != null && value.toString().isNotBlank()) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("This field is required")
            }
        }
    }

    data class MinLength(val min: Int) : ValidationRule() {
        override fun validate(value: Any?): ValidationResult {
            val text = value?.toString() ?: ""
            return if (text.length >= min) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("Must be at least $min characters")
            }
        }
    }

    data class MaxLength(val max: Int) : ValidationRule() {
        override fun validate(value: Any?): ValidationResult {
            val text = value?.toString() ?: ""
            return if (text.length <= max) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("Must be at most $max characters")
            }
        }
    }

    data class Range(val min: Number, val max: Number) : ValidationRule() {
        override fun validate(value: Any?): ValidationResult {
            return when (value) {
                is Number -> {
                    val doubleValue = value.toDouble()
                    if (doubleValue >= min.toDouble() && doubleValue <= max.toDouble()) {
                        ValidationResult.Valid
                    } else {
                        ValidationResult.Invalid("Value must be between $min and $max")
                    }
                }
                else -> ValidationResult.Invalid("Value must be a number")
            }
        }
    }

    data class Email(val message: String = "Invalid email format") : ValidationRule() {
        private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        override fun validate(value: Any?): ValidationResult {
            val email = value?.toString() ?: ""
            return if (email.matches(emailRegex)) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(message)
            }
        }
    }

    data class Custom(
        val validator: (Any?) -> Boolean,
        val message: String
    ) : ValidationRule() {
        override fun validate(value: Any?): ValidationResult {
            return if (validator(value)) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(message)
            }
        }
    }
}

/**
 * Result of a validation operation
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid
}

/**
 * Settings change event
 */
data class SettingChangeEvent(
    val key: String,
    val oldValue: Any?,
    val newValue: Any?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Settings section for organizing settings
 */
data class SettingsSection(
    val id: String,
    val title: String,
    val description: String? = null,
    val items: List<SettingItem>,
    val isCollapsible: Boolean = false,
    val isExpanded: Boolean = true,
    val sortOrder: Int = 0
)

/**
 * Complete settings state
 */
data class SettingsState(
    val categories: List<SettingsCategory>,
    val sections: Map<String, List<SettingsSection>>,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val lastSyncTime: Long? = null
) {
    fun getSectionsForCategory(categoryId: String): List<SettingsSection> {
        return sections[categoryId] ?: emptyList()
    }

    fun getAllSettings(): List<SettingItem> {
        return sections.values.flatten().flatMap { it.items }
    }

    fun getSettingById(id: String): SettingItem? {
        return getAllSettings().find { it.id == id }
    }
}

/**
 * Settings update request
 */
sealed class SettingsUpdateRequest {
    data class UpdateBoolean(val key: String, val value: Boolean) : SettingsUpdateRequest()
    data class UpdateInt(val key: String, val value: Int) : SettingsUpdateRequest()
    data class UpdateFloat(val key: String, val value: Float) : SettingsUpdateRequest()
    data class UpdateString(val key: String, val value: String) : SettingsUpdateRequest()
    data class UpdateSelection(val key: String, val selectedIndex: Int) : SettingsUpdateRequest()
    data class PerformAction(val action: SettingAction) : SettingsUpdateRequest()
}

/**
 * Settings operation result
 */
sealed class SettingsOperationResult {
    object Success : SettingsOperationResult()
    data class Error(val message: String, val cause: Throwable? = null) : SettingsOperationResult()
    data class ValidationError(val errors: List<ValidationResult.Invalid>) : SettingsOperationResult()
}