package com.mtlc.studyplan.settings

import android.content.Context
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Integration layer between settings and other app components
 */
class SettingsIntegration(context: Context) {

    private val preferencesManager = SettingsPreferencesManager(context)

    /**
     * Get haptic feedback setting for use in navigation and interactions
     */
    fun isHapticFeedbackEnabled(): Flow<Boolean> {
        return preferencesManager.navigationSettings.map { it.hapticFeedback }
    }

    /**
     * Get bottom navigation setting for UI layout decisions
     */
    fun isBottomNavigationEnabled(): Flow<Boolean> {
        return preferencesManager.navigationSettings.map { it.bottomNavigation }
    }

    /**
     * Get push notifications setting for notification scheduling
     */
    fun arePushNotificationsEnabled(): Flow<Boolean> {
        return preferencesManager.notificationSettings.map { it.pushNotifications }
    }

    /**
     * Get study reminders setting for reminder scheduling
     */
    fun areStudyRemindersEnabled(): Flow<Boolean> {
        return preferencesManager.notificationSettings.map { it.studyReminders }
    }

    /**
     * Get smart scheduling setting for AI recommendations
     */
    fun isSmartSchedulingEnabled(): Flow<Boolean> {
        return preferencesManager.taskSettings.map { it.smartScheduling }
    }

    /**
     * Get gamification preferences for UI features
     */
    fun getGamificationSettings() = preferencesManager.gamificationSettings

    /**
     * Get all privacy settings for data handling
     */
    fun getPrivacySettings() = preferencesManager.privacySettings

    companion object {
        @Volatile
        private var INSTANCE: SettingsIntegration? = null

        fun getInstance(context: Context): SettingsIntegration {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsIntegration(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Extension functions for easy integration with existing components
 */

/**
 * Quick access to settings from any composable with LocalContext
 */
@androidx.compose.runtime.Composable
fun rememberSettingsIntegration(): SettingsIntegration {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember { SettingsIntegration.getInstance(context) }
}