package com.mtlc.studyplan.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.deeplink.SettingsDeepLinkHandler
import com.mtlc.studyplan.settings.performance.SettingsPerformanceMonitor
import com.mtlc.studyplan.settings.repository.SettingsRepository
import com.mtlc.studyplan.settings.search.SettingsSearchEngine
import com.mtlc.studyplan.settings.search.VoiceSearchManager
import com.mtlc.studyplan.ui.animations.SettingsAnimationCoordinator

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val searchEngine: SettingsSearchEngine,
    private val backupManager: SettingsBackupManager,
    private val deepLinkHandler: SettingsDeepLinkHandler,
    private val accessibilityManager: AccessibilityManager,
    private val performanceMonitor: SettingsPerformanceMonitor,
    private val animationCoordinator: SettingsAnimationCoordinator,
    private val voiceSearchManager: VoiceSearchManager
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
            modelClass.isAssignableFrom(SettingsSearchViewModel::class.java) -> {
                SettingsSearchViewModel(
                    searchEngine,
                    voiceSearchManager,
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
            modelClass.isAssignableFrom(ConflictResolutionViewModel::class.java) -> {
                ConflictResolutionViewModel(
                    backupManager,
                    accessibilityManager
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

class SettingsSearchViewModelFactory(
    private val searchEngine: SettingsSearchEngine,
    private val voiceSearchManager: VoiceSearchManager,
    private val searchResultHighlighter: com.mtlc.studyplan.settings.search.SearchResultHighlighter,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SettingsSearchViewModel::class.java) -> {
                SettingsSearchViewModel(
                    searchEngine,
                    voiceSearchManager,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class SettingsBackupViewModelFactory(
    private val backupManager: SettingsBackupManager,
    private val encryption: com.mtlc.studyplan.settings.security.SettingsEncryption,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SettingsBackupViewModel::class.java) -> {
                SettingsBackupViewModel(
                    backupManager,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class ConflictResolutionViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ConflictResolutionViewModel::class.java) -> {
                val dependencies = com.mtlc.studyplan.settings.di.SettingsDependencyInjection.getInstance(context)
                ConflictResolutionViewModel(
                    dependencies.getBackupManager(),
                    dependencies.getAccessibilityManager()
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class AdvancedToggleViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AdvancedToggleViewModel::class.java) -> {
                AdvancedToggleViewModel(
                    settingsRepository,
                    accessibilityManager,
                    animationCoordinator
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class AccessibilityViewModelFactory(
    private val accessibilityManager: AccessibilityManager,
    private val fontScalingManager: com.mtlc.studyplan.accessibility.FontScalingManager,
    private val focusIndicatorManager: com.mtlc.studyplan.accessibility.FocusIndicatorManager,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AccessibilityViewModel::class.java) -> {
                AccessibilityViewModel(
                    accessibilityManager,
                    settingsRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class PerformanceViewModelFactory(
    private val performanceMonitor: SettingsPerformanceMonitor,
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
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