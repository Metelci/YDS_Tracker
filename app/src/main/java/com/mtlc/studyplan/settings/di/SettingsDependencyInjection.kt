package com.mtlc.studyplan.settings.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.accessibility.FontScalingManager
import com.mtlc.studyplan.accessibility.FocusIndicatorManager
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.deeplink.SettingsDeepLinkHandler
import com.mtlc.studyplan.settings.security.SettingsEncryption
import com.mtlc.studyplan.settings.performance.SettingsPerformanceMonitor
import com.mtlc.studyplan.ui.animations.SettingsAnimationCoordinator
import com.mtlc.studyplan.settings.viewmodel.*

class SettingsDependencyInjection private constructor(
    private val context: Context
) {

    // Core dependencies
    private val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    private val settingsEncryption: SettingsEncryption by lazy {
        SettingsEncryption(context)
    }

    // Accessibility dependencies
    private val accessibilityManager: AccessibilityManager by lazy {
        AccessibilityManager(context)
    }

    private val fontScalingManager: FontScalingManager by lazy {
        FontScalingManager(context)
    }

    private val focusIndicatorManager: FocusIndicatorManager by lazy {
        FocusIndicatorManager(context)
    }

    // Search removed from settings page

    // Backup dependencies
    private val settingsBackupManager: SettingsBackupManager by lazy {
        SettingsBackupManager(context, settingsRepository)
    }

    // Deep linking dependencies
    // Note: SettingsDeepLinkHandler requires FragmentActivity, will be created when needed
    fun createDeepLinkHandler(activity: androidx.fragment.app.FragmentActivity): SettingsDeepLinkHandler {
        return SettingsDeepLinkHandler(activity)
    }

    // Performance dependencies
    private val settingsPerformanceMonitor: SettingsPerformanceMonitor by lazy {
        SettingsPerformanceMonitor(context)
    }

    // Animation dependencies
    private val settingsAnimationCoordinator: SettingsAnimationCoordinator by lazy {
        SettingsAnimationCoordinator(context, accessibilityManager)
    }

    // ViewModelFactory
    fun createCompositeSettingsViewModelFactory(activity: androidx.fragment.app.FragmentActivity): CompositeSettingsViewModelFactory {
        return CompositeSettingsViewModelFactory(
            settingsRepository = settingsRepository,
            backupManager = settingsBackupManager,
            deepLinkHandler = createDeepLinkHandler(activity),
            accessibilityManager = accessibilityManager,
            performanceMonitor = settingsPerformanceMonitor,
            animationCoordinator = settingsAnimationCoordinator
        )
    }

    // Public accessors - removed duplicate getters to avoid JVM signature clashes
    // Use properties directly instead

    fun getViewModelFactory(activity: androidx.fragment.app.FragmentActivity): ViewModelProvider.Factory = createCompositeSettingsViewModelFactory(activity)

    // Search ViewModel factory removed

    fun getBackupViewModelFactory(): CompositeSettingsBackupViewModelFactory {
        return CompositeSettingsBackupViewModelFactory(
            backupManager = settingsBackupManager,
            accessibilityManager = accessibilityManager,
            animationCoordinator = settingsAnimationCoordinator
        )
    }

    fun getConflictResolutionViewModelFactory(): ConflictResolutionViewModelFactory {
        return ConflictResolutionViewModelFactory(context)
    }

    fun getAdvancedToggleViewModelFactory(): AdvancedToggleViewModelFactory {
        return AdvancedToggleViewModelFactory(
            settingsRepository,
            accessibilityManager,
            settingsAnimationCoordinator
        )
    }

    fun getAccessibilityViewModelFactory(): AccessibilityViewModelFactory {
        return AccessibilityViewModelFactory(
            accessibilityManager,
            settingsRepository
        )
    }

    fun getPerformanceViewModelFactory(): PerformanceViewModelFactory {
        return PerformanceViewModelFactory(
            settingsPerformanceMonitor,
            settingsRepository,
            accessibilityManager
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsDependencyInjection? = null

        fun getInstance(context: Context): SettingsDependencyInjection {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsDependencyInjection(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}

/**
 * Extension function to get dependencies from context
 */
fun Context.getSettingsDependencies(): SettingsDependencyInjection {
    return SettingsDependencyInjection.getInstance(this)
}

