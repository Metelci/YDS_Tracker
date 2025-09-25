package com.mtlc.studyplan.navigation

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mtlc.studyplan.shared.TaskFilter
import com.mtlc.studyplan.shared.TaskSortOrder
import java.time.LocalDateTime

class NavigationStateManager(private val context: Context) {

    private val statePreferences: SharedPreferences =
        context.getSharedPreferences("navigation_state", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val DEFAULT_TAB_ID = 0
        private const val KEY_NAVIGATION_STATE = "navigation_state"
        private const val KEY_TASKS_STATE = "tasks_state"
        private const val KEY_PROGRESS_STATE = "progress_state"
        private const val KEY_SOCIAL_STATE = "social_state"
        private const val KEY_HOME_STATE = "home_state"
        private const val KEY_SETTINGS_STATE = "settings_state"
    }

    data class NavigationState(
        val currentTab: Int = DEFAULT_TAB_ID,
        val lastActiveTime: Long = System.currentTimeMillis(),
        val sessionId: String = "",
        val isFirstLaunch: Boolean = true
    )

    data class TasksScreenState(
        val selectedFilter: TaskFilter? = null,
        val searchQuery: String = "",
        val sortOrder: TaskSortOrder = TaskSortOrder.DUE_DATE,
        val scrollPosition: Int = 0,
        val expandedCategories: Set<String> = emptySet(),
        val selectedTaskId: String? = null,
        val showCompletedTasks: Boolean = false,
        val filterByCategory: String? = null,
        val filterByPriority: String? = null,
        val groupByDate: Boolean = true,
        val lastRefreshTime: Long = 0,
        val viewMode: TaskViewMode = TaskViewMode.LIST
    )

    data class ProgressScreenState(
        val selectedTimeRange: TimeRange = TimeRange.WEEK,
        val selectedChartType: ChartType = ChartType.DAILY_PROGRESS,
        val scrollPosition: Int = 0,
        val expandedSections: Set<String> = emptySet(),
        val selectedGoalId: String? = null,
        val showDetailedStats: Boolean = false,
        val selectedMetric: ProgressMetric = ProgressMetric.STUDY_TIME,
        val comparisonMode: ComparisonMode = ComparisonMode.NONE
    )

    data class SocialScreenState(
        val selectedTab: SocialTab = SocialTab.FEED,
        val feedScrollPosition: Int = 0,
        val leaderboardScrollPosition: Int = 0,
        val selectedFriendId: String? = null,
        val searchQuery: String = "",
        val selectedLeaderboardType: LeaderboardType = LeaderboardType.WEEKLY,
        val showOnlineFriendsOnly: Boolean = false,
        val selectedChallengeId: String? = null,
        val notificationFilters: Set<String> = emptySet()
    )

    data class HomeScreenState(
        val expandedSections: Set<String> = emptySet(),
        val selectedQuickAction: String? = null,
        val lastRefreshTime: Long = 0,
        val showTodayFocus: Boolean = true,
        val selectedWidget: String? = null,
        val dashboardLayout: DashboardLayout = DashboardLayout.DEFAULT,
        val hiddenWidgets: Set<String> = emptySet()
    )

    data class SettingsScreenState(
        val searchQuery: String = "",
        val expandedCategories: Set<String> = emptySet(),
        val scrollPosition: Int = 0,
        val selectedSettingId: String? = null,
        val showAdvancedSettings: Boolean = false,
        val pendingChanges: Map<String, Any> = emptyMap()
    )

    // Save navigation state
    fun saveNavigationState(state: NavigationState) {
        statePreferences.edit()
            .putString(KEY_NAVIGATION_STATE, gson.toJson(state))
            .apply()
    }

    fun restoreNavigationState(): NavigationState {
        val stateJson = statePreferences.getString(KEY_NAVIGATION_STATE, null)
        return if (stateJson != null) {
            try {
                gson.fromJson(stateJson, NavigationState::class.java)
            } catch (e: Exception) {
                NavigationState() // Return default if parsing fails
            }
        } else {
            NavigationState()
        }
    }

    // Tasks state management
    fun saveTasksState(state: TasksScreenState) {
        statePreferences.edit()
            .putString(KEY_TASKS_STATE, gson.toJson(state))
            .apply()
    }

    fun restoreTasksState(): TasksScreenState {
        val stateJson = statePreferences.getString(KEY_TASKS_STATE, null)
        return if (stateJson != null) {
            try {
                gson.fromJson(stateJson, TasksScreenState::class.java)
            } catch (e: Exception) {
                TasksScreenState()
            }
        } else {
            TasksScreenState()
        }
    }

    // Progress state management
    fun saveProgressState(state: ProgressScreenState) {
        statePreferences.edit()
            .putString(KEY_PROGRESS_STATE, gson.toJson(state))
            .apply()
    }

    fun restoreProgressState(): ProgressScreenState {
        val stateJson = statePreferences.getString(KEY_PROGRESS_STATE, null)
        return if (stateJson != null) {
            try {
                gson.fromJson(stateJson, ProgressScreenState::class.java)
            } catch (e: Exception) {
                ProgressScreenState()
            }
        } else {
            ProgressScreenState()
        }
    }

    // Social state management
    fun saveSocialState(state: SocialScreenState) {
        statePreferences.edit()
            .putString(KEY_SOCIAL_STATE, gson.toJson(state))
            .apply()
    }

    fun restoreSocialState(): SocialScreenState {
        val stateJson = statePreferences.getString(KEY_SOCIAL_STATE, null)
        return if (stateJson != null) {
            try {
                gson.fromJson(stateJson, SocialScreenState::class.java)
            } catch (e: Exception) {
                SocialScreenState()
            }
        } else {
            SocialScreenState()
        }
    }

    // Home state management
    fun saveHomeState(state: HomeScreenState) {
        statePreferences.edit()
            .putString(KEY_HOME_STATE, gson.toJson(state))
            .apply()
    }

    fun restoreHomeState(): HomeScreenState {
        val stateJson = statePreferences.getString(KEY_HOME_STATE, null)
        return if (stateJson != null) {
            try {
                gson.fromJson(stateJson, HomeScreenState::class.java)
            } catch (e: Exception) {
                HomeScreenState()
            }
        } else {
            HomeScreenState()
        }
    }

    // Settings state management
    fun saveSettingsState(state: SettingsScreenState) {
        statePreferences.edit()
            .putString(KEY_SETTINGS_STATE, gson.toJson(state))
            .apply()
    }

    fun restoreSettingsState(): SettingsScreenState {
        val stateJson = statePreferences.getString(KEY_SETTINGS_STATE, null)
        return if (stateJson != null) {
            try {
                gson.fromJson(stateJson, SettingsScreenState::class.java)
            } catch (e: Exception) {
                SettingsScreenState()
            }
        } else {
            SettingsScreenState()
        }
    }

    // Utility methods
    fun clearNavigationState() {
        statePreferences.edit().clear().apply()
    }

    fun clearSpecificState(stateType: StateType) {
        val key = when (stateType) {
            StateType.NAVIGATION -> KEY_NAVIGATION_STATE
            StateType.TASKS -> KEY_TASKS_STATE
            StateType.PROGRESS -> KEY_PROGRESS_STATE
            StateType.SOCIAL -> KEY_SOCIAL_STATE
            StateType.HOME -> KEY_HOME_STATE
            StateType.SETTINGS -> KEY_SETTINGS_STATE
        }
        statePreferences.edit().remove(key).apply()
    }

    fun hasValidState(): Boolean {
        val navigationState = restoreNavigationState()
        val timeDiff = System.currentTimeMillis() - navigationState.lastActiveTime
        // Consider state valid if less than 24 hours old
        return timeDiff < 24 * 60 * 60 * 1000
    }

    fun getStateAge(): Long {
        val navigationState = restoreNavigationState()
        return System.currentTimeMillis() - navigationState.lastActiveTime
    }

    fun updateLastActiveTime() {
        val currentState = restoreNavigationState()
        saveNavigationState(currentState.copy(lastActiveTime = System.currentTimeMillis()))
    }

    // Complex state operations
    fun saveCompleteAppState(
        navigationState: NavigationState,
        tasksState: TasksScreenState?,
        progressState: ProgressScreenState?,
        socialState: SocialScreenState?,
        homeState: HomeScreenState?,
        settingsState: SettingsScreenState?
    ) {
        statePreferences.edit().apply {
            putString(KEY_NAVIGATION_STATE, gson.toJson(navigationState))
            tasksState?.let { putString(KEY_TASKS_STATE, gson.toJson(it)) }
            progressState?.let { putString(KEY_PROGRESS_STATE, gson.toJson(it)) }
            socialState?.let { putString(KEY_SOCIAL_STATE, gson.toJson(it)) }
            homeState?.let { putString(KEY_HOME_STATE, gson.toJson(it)) }
            settingsState?.let { putString(KEY_SETTINGS_STATE, gson.toJson(it)) }
            apply()
        }
    }

    enum class StateType {
        NAVIGATION, TASKS, PROGRESS, SOCIAL, HOME, SETTINGS
    }

    enum class TaskViewMode {
        LIST, GRID, CALENDAR
    }

    enum class TimeRange {
        DAY, WEEK, MONTH, QUARTER, YEAR, CUSTOM
    }

    enum class ChartType {
        DAILY_PROGRESS, WEEKLY_SUMMARY, MONTHLY_OVERVIEW, CATEGORY_BREAKDOWN, STREAK_ANALYSIS
    }

    enum class ProgressMetric {
        STUDY_TIME, TASKS_COMPLETED, STREAK_LENGTH, ACHIEVEMENT_POINTS, CATEGORY_PROGRESS
    }

    enum class ComparisonMode {
        NONE, PREVIOUS_PERIOD, SAME_PERIOD_LAST_YEAR, FRIENDS_AVERAGE
    }

    enum class SocialTab {
        FEED, LEADERBOARD, FRIENDS, CHALLENGES, NOTIFICATIONS
    }

    enum class LeaderboardType {
        DAILY, WEEKLY, MONTHLY, ALL_TIME, FRIENDS_ONLY
    }

    enum class DashboardLayout {
        DEFAULT, COMPACT, DETAILED, CUSTOM
    }
}
