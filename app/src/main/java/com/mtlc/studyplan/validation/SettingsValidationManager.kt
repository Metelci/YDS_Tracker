package com.mtlc.studyplan.validation

import com.mtlc.studyplan.settings.data.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsValidationManager {

    private val _validationResults = MutableStateFlow<List<ValidationResult>>(emptyList())
    val validationResults: StateFlow<List<ValidationResult>> = _validationResults.asStateFlow()

    fun validateSettings(settings: UserSettings): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        // Theme validation removed (light-only)

        // Validate notification settings
        results.addAll(validateNotificationSettings(settings))

        // Validate offline settings
        results.addAll(validateOfflineSettings(settings))

        // Validate gamification settings
        results.addAll(validateGamificationSettings(settings))

        // Validate general settings consistency
        results.addAll(validateSettingsConsistency(settings))

        _validationResults.value = results
        return results
    }

    // Theme validation removed

    private fun validateNotificationSettings(settings: UserSettings): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        // Master notification toggle validation
        if (!settings.notificationsEnabled) {
            val dependentNotifications = listOf(
                settings.studyRemindersEnabled,
                settings.achievementNotificationsEnabled,
                settings.dailyGoalRemindersEnabled,
                settings.streakWarningsEnabled
            )

            if (dependentNotifications.any { it }) {
                results.add(
                    ValidationResult.Warning(
                        "Some notification types are enabled while master notifications are disabled. " +
                        "They will be automatically disabled."
                    )
                )
            } else {
                results.add(
                    ValidationResult.Success(
                        "Notification settings are consistent"
                    )
                )
            }
        }

        // Streak warnings validation
        if (settings.streakWarningsEnabled && !settings.gamificationEnabled) {
            results.add(
                ValidationResult.Warning(
                    "Streak warnings are enabled but gamification is disabled. Streak warnings will not function."
                )
            )
        }

        // Achievement notifications validation
        if (settings.achievementNotificationsEnabled && !settings.gamificationEnabled) {
            results.add(
                ValidationResult.Warning(
                    "Achievement notifications are enabled but gamification is disabled. " +
                    "No achievements will be unlocked."
                )
            )
        }

        return results
    }

    private fun validateOfflineSettings(settings: UserSettings): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        // Auto-sync validation
        if (settings.autoSyncEnabled && !settings.offlineModeEnabled) {
            results.add(
                ValidationResult.Warning(
                    "Auto-sync is enabled but offline mode is disabled. Auto-sync will not function."
                )
            )
        }

        // Offline mode validation
        if (settings.offlineModeEnabled) {
            results.add(
                ValidationResult.Info(
                    "Offline mode is enabled. Data will be cached locally and synced when online."
                )
            )
        }

        return results
    }

    private fun validateGamificationSettings(settings: UserSettings): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        if (!settings.gamificationEnabled) {
            val gamificationDependentFeatures = listOf(
                "achievementNotificationsEnabled" to settings.achievementNotificationsEnabled,
                "streakWarningsEnabled" to settings.streakWarningsEnabled
            )

            val enabledDependentFeatures = gamificationDependentFeatures.filter { it.second }

            if (enabledDependentFeatures.isNotEmpty()) {
                results.add(
                    ValidationResult.Warning(
                        "Gamification is disabled but dependent features are enabled: " +
                        enabledDependentFeatures.joinToString { it.first }
                    )
                )
            }
        }

        return results
    }

    private fun validateSettingsConsistency(settings: UserSettings): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        // Weekend mode validation
        if (settings.weekendModeEnabled) {
            results.add(
                ValidationResult.Info(
                    "Weekend mode is enabled. Study schedules will be adjusted for weekends."
                )
            )
        }

        // Smart scheduling removed; no validation needed here

        // Haptic feedback validation
        if (settings.hapticFeedbackEnabled) {
            results.add(
                ValidationResult.Info(
                    "Haptic feedback is enabled. You'll feel vibrations for interactions."
                )
            )
        }

        return results
    }

    fun hasErrors(): Boolean {
        return _validationResults.value.any { it is ValidationResult.Error }
    }

    fun hasWarnings(): Boolean {
        return _validationResults.value.any { it is ValidationResult.Warning }
    }

    fun getErrorCount(): Int {
        return _validationResults.value.count { it is ValidationResult.Error }
    }

    fun getWarningCount(): Int {
        return _validationResults.value.count { it is ValidationResult.Warning }
    }
}

sealed class ValidationResult(val message: String) {
    class Success(message: String) : ValidationResult(message)
    class Info(message: String) : ValidationResult(message)
    class Warning(message: String) : ValidationResult(message)
    class Error(message: String) : ValidationResult(message)
}

data class SettingsPerformanceMetrics(
    val settingsLoadTime: Long = 0L,
    val themeChangeTime: Long = 0L,
    val notificationSetupTime: Long = 0L,
    val offlineSyncTime: Long = 0L,
    val memoryUsage: Long = 0L,
    val cacheHitRate: Double = 0.0
)

class SettingsPerformanceMonitor {
    private val metrics = MutableStateFlow(SettingsPerformanceMetrics())

    fun recordSettingsLoadTime(time: Long) {
        metrics.value = metrics.value.copy(settingsLoadTime = time)
    }

    fun recordThemeChangeTime(time: Long) {
        metrics.value = metrics.value.copy(themeChangeTime = time)
    }

    fun recordNotificationSetupTime(time: Long) {
        metrics.value = metrics.value.copy(notificationSetupTime = time)
    }

    fun recordOfflineSyncTime(time: Long) {
        metrics.value = metrics.value.copy(offlineSyncTime = time)
    }

    fun getMetrics(): StateFlow<SettingsPerformanceMetrics> = metrics.asStateFlow()

    fun logPerformanceReport() {
        val current = metrics.value
        println("=== Settings System Performance Report ===")
        println("Settings Load Time: ${current.settingsLoadTime}ms")
        println("Theme Change Time: ${current.themeChangeTime}ms")
        println("Notification Setup Time: ${current.notificationSetupTime}ms")
        println("Offline Sync Time: ${current.offlineSyncTime}ms")
        println("Memory Usage: ${current.memoryUsage}MB")
        println("Cache Hit Rate: ${(current.cacheHitRate * 100).toInt()}%")
        println("==========================================")
    }
}
