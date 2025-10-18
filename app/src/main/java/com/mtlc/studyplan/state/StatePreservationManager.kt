package com.mtlc.studyplan.state

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatePreservationManager @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_state", Context.MODE_PRIVATE)

    // Screen-specific state preservation
    suspend fun saveScreenState(screenKey: String, state: Any) {
        withContext(Dispatchers.IO) {
            try {
                val stateJson = gson.toJson(state)
                sharedPreferences.edit()
                    .putString("screen_$screenKey", stateJson)
                    .putLong("screen_${screenKey}_timestamp", System.currentTimeMillis())
                    .apply()
            } catch (e: Exception) {
                Log.e("StatePreservation", "Failed to save state for $screenKey", e)
            }
        }
    }

    suspend fun <T> restoreScreenState(screenKey: String, type: Class<T>): T? {
        return withContext(Dispatchers.IO) {
            try {
                val stateJson = sharedPreferences.getString("screen_$screenKey", null)
                val timestamp = sharedPreferences.getLong("screen_${screenKey}_timestamp", 0)

                // Only restore if state is less than 24 hours old
                if (stateJson != null && System.currentTimeMillis() - timestamp < 24 * 60 * 60 * 1000) {
                    gson.fromJson(stateJson, type)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("StatePreservation", "Failed to restore state for $screenKey", e)
                null
            }
        }
    }

    // Navigation state preservation
    fun saveNavigationState(navigationState: NavigationState) {
        try {
            val stateJson = gson.toJson(navigationState)
            sharedPreferences.edit()
                .putString("navigation_state", stateJson)
                .putLong("navigation_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e("StatePreservation", "Failed to save navigation state", e)
        }
    }

    fun restoreNavigationState(): NavigationState? {
        return try {
            val stateJson = sharedPreferences.getString("navigation_state", null)
            val timestamp = sharedPreferences.getLong("navigation_timestamp", 0)

            if (stateJson != null && System.currentTimeMillis() - timestamp < 60 * 60 * 1000) { // 1 hour
                gson.fromJson(stateJson, NavigationState::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StatePreservation", "Failed to restore navigation state", e)
            null
        }
    }

    // Form state preservation
    fun saveFormState(formKey: String, formData: Map<String, Any>) {
        try {
            val dataJson = gson.toJson(formData)
            sharedPreferences.edit()
                .putString("form_$formKey", dataJson)
                .putLong("form_${formKey}_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e("StatePreservation", "Failed to save form state for $formKey", e)
        }
    }

    fun restoreFormState(formKey: String): Map<String, Any>? {
        return try {
            val dataJson = sharedPreferences.getString("form_$formKey", null)
            val timestamp = sharedPreferences.getLong("form_${formKey}_timestamp", 0)

            if (dataJson != null && System.currentTimeMillis() - timestamp < 30 * 60 * 1000) { // 30 minutes
                gson.fromJson(dataJson, object : TypeToken<Map<String, Any>>() {}.type)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StatePreservation", "Failed to restore form state for $formKey", e)
            null
        }
    }

    fun clearExpiredState() {
        val editor = sharedPreferences.edit()
        val currentTime = System.currentTimeMillis()

        sharedPreferences.all.forEach { (key, _) ->
            if (key.endsWith("_timestamp")) {
                val timestamp = sharedPreferences.getLong(key, 0)
                if (currentTime - timestamp > 24 * 60 * 60 * 1000) { // 24 hours
                    val stateKey = key.removeSuffix("_timestamp")
                    editor.remove(stateKey).remove(key)
                }
            }
        }

        editor.apply()
    }

    // Scroll position preservation
    fun saveScrollPosition(screenKey: String, position: Int) {
        sharedPreferences.edit()
            .putInt("scroll_$screenKey", position)
            .putLong("scroll_${screenKey}_timestamp", System.currentTimeMillis())
            .apply()
    }

    fun restoreScrollPosition(screenKey: String): Int {
        val timestamp = sharedPreferences.getLong("scroll_${screenKey}_timestamp", 0)
        return if (System.currentTimeMillis() - timestamp < 60 * 60 * 1000) { // 1 hour
            sharedPreferences.getInt("scroll_$screenKey", 0)
        } else {
            0
        }
    }

    // Search query preservation
    fun saveSearchQuery(screenKey: String, query: String) {
        sharedPreferences.edit()
            .putString("search_$screenKey", query)
            .putLong("search_${screenKey}_timestamp", System.currentTimeMillis())
            .apply()
    }

    fun restoreSearchQuery(screenKey: String): String {
        val timestamp = sharedPreferences.getLong("search_${screenKey}_timestamp", 0)
        return if (System.currentTimeMillis() - timestamp < 30 * 60 * 1000) { // 30 minutes
            sharedPreferences.getString("search_$screenKey", "") ?: ""
        } else {
            ""
        }
    }

    // Filter state preservation
    fun saveFilterState(screenKey: String, filters: Set<String>) {
        try {
            val filtersJson = gson.toJson(filters)
            sharedPreferences.edit()
                .putString("filters_$screenKey", filtersJson)
                .putLong("filters_${screenKey}_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e("StatePreservation", "Failed to save filter state for $screenKey", e)
        }
    }

    fun restoreFilterState(screenKey: String): Set<String> {
        return try {
            val timestamp = sharedPreferences.getLong("filters_${screenKey}_timestamp", 0)
            if (System.currentTimeMillis() - timestamp < 60 * 60 * 1000) { // 1 hour
                val filtersJson = sharedPreferences.getString("filters_$screenKey", null)
                if (filtersJson != null) {
                    gson.fromJson(filtersJson, object : TypeToken<Set<String>>() {}.type)
                } else {
                    emptySet()
                }
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            Log.e("StatePreservation", "Failed to restore filter state for $screenKey", e)
            emptySet()
        }
    }
}

// State data classes
data class NavigationState(
    val currentRoute: String = "home",
    val backStackRoutes: List<String> = listOf("home"),
    val routeArguments: Map<String, String> = emptyMap()
)

data class TasksScreenState(
    val searchQuery: String = "",
    val selectedFilter: String? = null,
    val sortOrder: String = "DUE_DATE",
    val scrollPosition: Int = 0,
    val expandedCategories: Set<String> = emptySet(),
    val selectedTaskIds: Set<String> = emptySet()
)

data class ProgressScreenState(
    val selectedTimeRange: String = "WEEK",
    val selectedChartType: String = "DAILY_PROGRESS",
    val scrollPosition: Int = 0,
    val expandedSections: Set<String> = emptySet()
)

data class SettingsScreenState(
    val expandedSections: Set<String> = emptySet(),
    val scrollPosition: Int = 0,
    val searchQuery: String = ""
)
