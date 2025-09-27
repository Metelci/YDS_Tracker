package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.core.ViewModelFactory
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.deeplink.SettingsDeepLinkHandler
import com.mtlc.studyplan.settings.performance.SettingsPerformanceMonitor
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.ui.animations.SettingsAnimationCoordinator

class CompositeSettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val backupManager: SettingsBackupManager,
    private val deepLinkHandler: SettingsDeepLinkHandler,
    private val accessibilityManager: AccessibilityManager,
    private val performanceMonitor: SettingsPerformanceMonitor,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelProvider.Factory {

    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainSettingsViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                MainSettingsViewModel(
                    settingsRepository,
                    deepLinkHandler,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            modelClass.isAssignableFrom(SettingsBackupViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                SettingsBackupViewModel(
                    backupManager,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            modelClass.isAssignableFrom(AdvancedToggleViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AdvancedToggleViewModel(
                    settingsRepository,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            modelClass.isAssignableFrom(AccessibilityViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AccessibilityViewModel(
                    accessibilityManager,
                    settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(PerformanceViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                PerformanceViewModel(
                    performanceMonitor,
                    settingsRepository,
                    accessibilityManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

// Search ViewModel factory removed (search feature not present)

class CompositeSettingsBackupViewModelFactory(
    private val backupManager: SettingsBackupManager,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelFactory<SettingsBackupViewModel>({
    SettingsBackupViewModel(backupManager, accessibilityManager, animationCoordinator)
})

class ConflictResolutionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class AdvancedToggleViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelFactory<AdvancedToggleViewModel>({
    AdvancedToggleViewModel(settingsRepository, accessibilityManager, animationCoordinator)
})

class AccessibilityViewModelFactory(
    private val accessibilityManager: AccessibilityManager,
    private val settingsRepository: SettingsRepository
) : ViewModelFactory<AccessibilityViewModel>({
    AccessibilityViewModel(accessibilityManager, settingsRepository)
})

class PerformanceViewModelFactory(
    private val performanceMonitor: SettingsPerformanceMonitor,
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager
) : ViewModelFactory<PerformanceViewModel>({
    PerformanceViewModel(performanceMonitor, settingsRepository, accessibilityManager)
})

class GamificationSettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModelFactory<GamificationSettingsViewModel>({
    GamificationSettingsViewModel(settingsRepository, context)
})

class NotificationSettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModelFactory<NotificationSettingsViewModel>({
    NotificationSettingsViewModel(settingsRepository, context)
})

class PrivacySettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModelFactory<PrivacySettingsViewModel>({
    PrivacySettingsViewModel(settingsRepository, context)
})
