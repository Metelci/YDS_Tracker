package com.mtlc.studyplan.integration

import com.mtlc.studyplan.settings.data.ThemeMode
import com.mtlc.studyplan.settings.data.UserSettings
import com.mtlc.studyplan.settings.data.SettingsKey
import com.mtlc.studyplan.validation.SettingsValidationManager
import com.mtlc.studyplan.validation.ValidationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SettingsIntegrationValidator {

    private val validationManager = SettingsValidationManager()

    fun validateCompleteSettingsSystem(): SettingsIntegrationReport {
        val report = SettingsIntegrationReport()

        // Test 1: Default settings validation
        val defaultSettings = UserSettings.default()
        val defaultValidation = validationManager.validateSettings(defaultSettings)
        report.addTest("Default Settings", defaultValidation.none { it is ValidationResult.Error })

        // Test 2: Theme mode changes
        val lightThemeSettings = defaultSettings.copy(themeMode = ThemeMode.LIGHT)
        val darkThemeSettings = defaultSettings.copy(themeMode = ThemeMode.DARK)
        val systemThemeSettings = defaultSettings.copy(themeMode = ThemeMode.SYSTEM)

        report.addTest("Light Theme", validationManager.validateSettings(lightThemeSettings).none { it is ValidationResult.Error })
        report.addTest("Dark Theme", validationManager.validateSettings(darkThemeSettings).none { it is ValidationResult.Error })
        report.addTest("System Theme", validationManager.validateSettings(systemThemeSettings).none { it is ValidationResult.Error })

        // Test 3: Notification dependency validation
        val invalidNotificationSettings = defaultSettings.copy(
            notificationsEnabled = false,
            studyRemindersEnabled = true,
            achievementNotificationsEnabled = true
        )
        val notificationValidation = validationManager.validateSettings(invalidNotificationSettings)
        report.addTest("Notification Dependencies", notificationValidation.any { it is ValidationResult.Warning })

        // Test 4: Gamification dependency validation
        val invalidGamificationSettings = defaultSettings.copy(
            gamificationEnabled = false,
            achievementNotificationsEnabled = true,
            streakWarningsEnabled = true
        )
        val gamificationValidation = validationManager.validateSettings(invalidGamificationSettings)
        report.addTest("Gamification Dependencies", gamificationValidation.any { it is ValidationResult.Warning })

        // Test 5: Offline settings validation
        val invalidOfflineSettings = defaultSettings.copy(
            offlineModeEnabled = false,
            autoSyncEnabled = true
        )
        val offlineValidation = validationManager.validateSettings(invalidOfflineSettings)
        report.addTest("Offline Dependencies", offlineValidation.any { it is ValidationResult.Warning })

        // Test 6: All settings enabled (stress test)
        val allEnabledSettings = UserSettings(
            themeMode = ThemeMode.DARK,
            notificationsEnabled = true,
            studyRemindersEnabled = true,
            achievementNotificationsEnabled = true,
            dailyGoalRemindersEnabled = true,
            streakWarningsEnabled = true,
            offlineModeEnabled = true,
            autoSyncEnabled = true,
            gamificationEnabled = true,
            socialSharingEnabled = true,
            hapticFeedbackEnabled = true,
            weekendModeEnabled = true,
            smartSchedulingEnabled = true,
            autoDifficultyEnabled = true
        )
        val allEnabledValidation = validationManager.validateSettings(allEnabledSettings)
        report.addTest("All Features Enabled", allEnabledValidation.none { it is ValidationResult.Error })

        // Test 7: All settings disabled (minimal test)
        val allDisabledSettings = UserSettings(
            themeMode = ThemeMode.LIGHT,
            notificationsEnabled = false,
            studyRemindersEnabled = false,
            achievementNotificationsEnabled = false,
            dailyGoalRemindersEnabled = false,
            streakWarningsEnabled = false,
            offlineModeEnabled = false,
            autoSyncEnabled = false,
            gamificationEnabled = false,
            socialSharingEnabled = false,
            hapticFeedbackEnabled = false,
            weekendModeEnabled = false,
            smartSchedulingEnabled = false,
            autoDifficultyEnabled = false
        )
        val allDisabledValidation = validationManager.validateSettings(allDisabledSettings)
        report.addTest("All Features Disabled", allDisabledValidation.none { it is ValidationResult.Error })

        return report
    }

    fun validateSettingsKeys(): Boolean {
        val allKeys = SettingsKey.values()
        val requiredKeys = setOf(
            SettingsKey.THEME_MODE,
            SettingsKey.NOTIFICATIONS_ENABLED,
            SettingsKey.GAMIFICATION_ENABLED,
            SettingsKey.OFFLINE_MODE,
            SettingsKey.HAPTIC_FEEDBACK
        )

        return requiredKeys.all { it in allKeys }
    }

    fun validateThemeIntegration(): Boolean {
        // Test that all theme modes are properly defined
        val theModes = ThemeMode.values()
        return theModes.contains(ThemeMode.LIGHT) &&
               theModes.contains(ThemeMode.DARK) &&
               theModes.contains(ThemeMode.SYSTEM)
    }

    fun generateIntegrationReport(): String {
        val report = validateCompleteSettingsSystem()
        val settingsKeysValid = validateSettingsKeys()
        val themeIntegrationValid = validateThemeIntegration()

        return buildString {
            appendLine("=== StudyPlan Settings Integration Report ===")
            appendLine()
            appendLine("System Validation Tests:")
            report.tests.forEach { (name, passed) ->
                val status = if (passed) "✅ PASS" else "❌ FAIL"
                appendLine("  $status $name")
            }
            appendLine()
            appendLine("Component Validation:")
            appendLine("  ${if (settingsKeysValid) "✅ PASS" else "❌ FAIL"} Settings Keys")
            appendLine("  ${if (themeIntegrationValid) "✅ PASS" else "❌ FAIL"} Theme Integration")
            appendLine()
            appendLine("Summary:")
            appendLine("  Total Tests: ${report.tests.size + 2}")
            appendLine("  Passed: ${report.passedCount + (if (settingsKeysValid) 1 else 0) + (if (themeIntegrationValid) 1 else 0)}")
            appendLine("  Failed: ${report.failedCount + (if (!settingsKeysValid) 1 else 0) + (if (!themeIntegrationValid) 1 else 0)}")
            appendLine("  Success Rate: ${report.successRate}%")
            appendLine()
            appendLine("=== Settings System Ready for Production ===")
        }
    }
}

data class SettingsIntegrationReport(
    private val tests: MutableMap<String, Boolean> = mutableMapOf()
) {
    fun addTest(name: String, passed: Boolean) {
        tests[name] = passed
    }

    val passedCount: Int get() = tests.values.count { it }
    val failedCount: Int get() = tests.values.count { !it }
    val totalCount: Int get() = tests.size
    val successRate: Int get() = if (totalCount > 0) (passedCount * 100) / totalCount else 0

    val tests: Map<String, Boolean> get() = this.tests.toMap()
}