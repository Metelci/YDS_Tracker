package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.deeplink.SettingsDeepLinkHandler
import com.mtlc.studyplan.settings.performance.SettingsPerformanceMonitor
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.search.SettingsSearchEngine
import com.mtlc.studyplan.ui.animations.SettingsAnimationCoordinator

class CompositeSettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val searchEngine: SettingsSearchEngine,
    private val backupManager: SettingsBackupManager,
    private val deepLinkHandler: SettingsDeepLinkHandler,
    private val accessibilityManager: AccessibilityManager,
    private val performanceMonitor: SettingsPerformanceMonitor,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainSettingsViewModel::class.java) -> {
                MainSettingsViewModel(
                    settingsRepository,
                    searchEngine,
                    deepLinkHandler,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            modelClass.isAssignableFrom(SettingsBackupViewModel::class.java) -> {
                SettingsBackupViewModel(
                    backupManager,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            modelClass.isAssignableFrom(AdvancedToggleViewModel::class.java) -> {
                AdvancedToggleViewModel(
                    settingsRepository,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            modelClass.isAssignableFrom(AccessibilityViewModel::class.java) -> {
                AccessibilityViewModel(
                    accessibilityManager,
                    settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(PerformanceViewModel::class.java) -> {
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

class CompositeSettingsSearchViewModelFactory(
    private val context: Context,
    private val repository: SettingsRepository,
    private val searchEngine: SettingsSearchEngine
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsSearchViewModel::class.java)) {
            return SettingsSearchViewModel(context, repository, searchEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class CompositeSettingsBackupViewModelFactory(
    private val backupManager: SettingsBackupManager,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsBackupViewModel::class.java)) {
            return SettingsBackupViewModel(backupManager, accessibilityManager, animationCoordinator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class ConflictResolutionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class AdvancedToggleViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdvancedToggleViewModel::class.java)) {
            return AdvancedToggleViewModel(
                settingsRepository,
                accessibilityManager,
                animationCoordinator
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class AccessibilityViewModelFactory(
    private val accessibilityManager: AccessibilityManager,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccessibilityViewModel::class.java)) {
            return AccessibilityViewModel(
                accessibilityManager,
                settingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class PerformanceViewModelFactory(
    private val performanceMonitor: SettingsPerformanceMonitor,
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerformanceViewModel::class.java)) {
            return PerformanceViewModel(
                performanceMonitor,
                settingsRepository,
                accessibilityManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
