package com.mtlc.studyplan.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyPlanNavigationManager @Inject constructor() {

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _deepLinkParams = MutableStateFlow<DeepLinkParams?>(null)
    val deepLinkParams: StateFlow<DeepLinkParams?> = _deepLinkParams.asStateFlow()

    data class NavigationState(
        val currentDestination: String = "home",
        val previousDestination: String? = null,
        val navigationStack: List<String> = listOf("home"),
        val pendingNavigation: PendingNavigation? = null
    )

    data class PendingNavigation(
        val destination: String,
        val params: Map<String, Any> = emptyMap(),
        val clearBackStack: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class DeepLinkParams(
        val taskFilter: TaskFilter? = null,
        val taskId: String? = null,
        val progressTimeRange: TimeRange? = null,
        val achievementId: String? = null,
        val socialTab: SocialTab? = null,
        val highlightElement: String? = null
    )

    // Navigation Methods with Context
    fun navigateToTasks(
        filter: TaskFilter? = null,
        highlightTaskId: String? = null,
        fromScreen: String = getCurrentDestination()
    ) {
        val params = DeepLinkParams(
            taskFilter = filter,
            taskId = highlightTaskId
        )

        performNavigation(
            destination = "tasks",
            params = params,
            fromScreen = fromScreen
        )
    }

    fun navigateToTaskDetail(
        taskId: String,
        fromScreen: String = getCurrentDestination()
    ) {
        val params = DeepLinkParams(taskId = taskId)

        performNavigation(
            destination = "task_detail",
            params = params,
            fromScreen = fromScreen
        )
    }

    fun navigateToProgress(
        timeRange: TimeRange? = null,
        highlightElement: String? = null,
        fromScreen: String = getCurrentDestination()
    ) {
        val params = DeepLinkParams(
            progressTimeRange = timeRange,
            highlightElement = highlightElement
        )

        performNavigation(
            destination = "progress",
            params = params,
            fromScreen = fromScreen
        )
    }

    fun navigateToSocial(
        tab: SocialTab? = null,
        achievementId: String? = null,
        fromScreen: String = getCurrentDestination()
    ) {
        val params = DeepLinkParams(
            socialTab = tab,
            achievementId = achievementId
        )

        performNavigation(
            destination = "social",
            params = params,
            fromScreen = fromScreen
        )
    }

    private fun performNavigation(
        destination: String,
        params: DeepLinkParams,
        fromScreen: String
    ) {
        // Update navigation state
        val currentState = _navigationState.value
        val newState = currentState.copy(
            currentDestination = destination,
            previousDestination = fromScreen,
            navigationStack = currentState.navigationStack + destination
        )
        _navigationState.value = newState

        // Set deep link parameters
        _deepLinkParams.value = params


        // Clear params after brief delay (allows destination to consume them)
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            _deepLinkParams.value = null
        }
    }

    fun getCurrentDestination(): String = _navigationState.value.currentDestination

    fun canGoBack(): Boolean = _navigationState.value.navigationStack.size > 1

    fun goBack() {
        val currentState = _navigationState.value
        if (currentState.navigationStack.size > 1) {
            val newStack = currentState.navigationStack.dropLast(1)
            val previousDestination = newStack.last()
            _navigationState.value = currentState.copy(
                currentDestination = previousDestination,
                navigationStack = newStack
            )

        }
    }
}

// Enum classes for navigation parameters
enum class TaskFilter {
    ALL,
    TODAY,
    PENDING,
    COMPLETED,
    CREATE_NEW;

    companion object {
        val Pending = PENDING
        val Today = TODAY
        val CreateNew = CREATE_NEW
    }
}

enum class TimeRange {
    TODAY,
    WEEK,
    MONTH,
    ALL_TIME;

    companion object {
        val WEEK = TimeRange.WEEK
        val TODAY = TimeRange.TODAY
    }
}

enum class SocialTab {
    FEED,
    ACHIEVEMENTS,
    LEADERBOARD;

    val displayName: String
        get() = when (this) {
            FEED -> "Feed"
            ACHIEVEMENTS -> "Achievements"
            LEADERBOARD -> "Leaderboard"
        }

    companion object {
        val ACHIEVEMENTS = SocialTab.ACHIEVEMENTS
    }
}

// Helper extension function
private fun StudyPlanNavigationManager.DeepLinkParams.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    taskFilter?.let { map["taskFilter"] = it }
    taskId?.let { map["taskId"] = it }
    progressTimeRange?.let { map["progressTimeRange"] = it }
    achievementId?.let { map["achievementId"] = it }
    socialTab?.let { map["socialTab"] = it }
    highlightElement?.let { map["highlightElement"] = it }
    return map
}
