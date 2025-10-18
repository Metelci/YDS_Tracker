package com.mtlc.studyplan.navigation

import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.settings.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeDataProvider @Inject constructor(
    private val appIntegrationManager: AppIntegrationManager,
    private val settingsManager: SettingsManager
) {

    private val _badgeStates = MutableStateFlow<Map<BadgeType, BadgeState>>(emptyMap())
    val badgeStates: StateFlow<Map<BadgeType, BadgeState>> = _badgeStates.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        observeDataChanges()
    }

    private fun observeDataChanges() {
        // Observe pending tasks for Tasks badge
        scope.launch {
            appIntegrationManager.getPendingTasksCount().collect { count ->
                updateBadge(BadgeType.TASKS, count)
            }
        }

        // Observe new achievements for Progress badge
        scope.launch {
            appIntegrationManager.getNewAchievementsCount().collect { count ->
                updateBadge(BadgeType.PROGRESS, count)
            }
        }

        // Observe streak risk for Home badge
        scope.launch {
            appIntegrationManager.getStreakRiskStatus().collect { atRisk ->
                val settings = settingsManager.currentSettings.value
                if (atRisk && settings.streakWarningsEnabled) {
                    updateBadge(BadgeType.HOME, 1, BadgeStyle.WARNING)
                } else {
                    updateBadge(BadgeType.HOME, 0)
                }
            }
        }

        // Observe settings updates for Settings badge
        scope.launch {
            appIntegrationManager.getSettingsUpdateCount().collect { count ->
                updateBadge(BadgeType.SETTINGS, count)
            }
        }
    }

    private fun updateBadge(
        type: BadgeType,
        count: Int,
        style: BadgeStyle = BadgeStyle.DEFAULT
    ) {
        val currentStates = _badgeStates.value.toMutableMap()

        if (count > 0) {
            currentStates[type] = BadgeState(
                count = count,
                style = style,
                isVisible = true,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            currentStates[type] = BadgeState(
                count = 0,
                style = style,
                isVisible = false,
                lastUpdated = System.currentTimeMillis()
            )
        }

        _badgeStates.value = currentStates
    }

    fun getBadgeState(type: BadgeType): BadgeState {
        return _badgeStates.value[type] ?: BadgeState()
    }

    fun clearBadge(type: BadgeType) {
        updateBadge(type, 0)
    }

    fun markAsViewed(type: BadgeType) {
        when (type) {
            BadgeType.PROGRESS -> {
                scope.launch {
                    appIntegrationManager.markAchievementsAsViewed()
                }
            }
            BadgeType.SETTINGS -> {
                scope.launch {
                    appIntegrationManager.markSettingsUpdatesAsViewed()
                }
            }
            else -> {
                // Some badges clear automatically
            }
        }
    }
}

data class BadgeState(
    val count: Int = 0,
    val style: BadgeStyle = BadgeStyle.DEFAULT,
    val isVisible: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class BadgeType {
    HOME,
    TASKS,
    PROGRESS,
    SETTINGS
}

enum class BadgeStyle {
    DEFAULT,    // Blue/Primary color
    WARNING,    // Orange/Yellow for warnings
    ERROR,      // Red for errors
    SUCCESS     // Green for positive actions
}
