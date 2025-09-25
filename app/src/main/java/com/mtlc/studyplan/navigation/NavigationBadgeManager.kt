package com.mtlc.studyplan.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mtlc.studyplan.shared.SharedAppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * NavigationBadgeManager - Manages badges for navigation items in Compose-based navigation
 * Supports gamification features like achievement notifications, streak warnings, etc.
 */
class NavigationBadgeManager(
    private val sharedViewModel: SharedAppViewModel,
    private val lifecycleOwner: LifecycleOwner
) {

    // Badge data for each navigation route
    private val _badgeData = MutableStateFlow<Map<String, BadgeInfo>>(emptyMap())
    val badgeData: StateFlow<Map<String, BadgeInfo>> = _badgeData.asStateFlow()

    // Navigation routes that support badges
    private val supportedRoutes = setOf("home", "tasks", "social", "settings")

    data class BadgeInfo(
        val count: Int,
        val style: BadgeStyle = BadgeStyle.DEFAULT,
        val contentDescription: String = ""
    )

    enum class BadgeStyle {
        DEFAULT,     // Standard notification badge
        WARNING,     // Orange warning badge (e.g., streak at risk)
        ERROR,       // Red error badge
        SUCCESS,     // Green success badge (e.g., achievement unlocked)
        CELEBRATION  // Special celebration badge with animation
    }

    init {
        setupBadgeObservers()
        initializeBadges()
    }

    /**
     * Initialize badge observers for gamification events
     */
    private fun setupBadgeObservers() {
        // Observe streak changes
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.currentStreak.collect { streak ->
                    updateStreakBadge(streak)
                }
            }
        }

        // Observe achievements
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.achievements.collect { achievements ->
                    updateAchievementBadge(achievements)
                }
            }
        }

        // Observe pending tasks or notifications
        // Additional observers can be added here for other gamification features
    }

    /**
     * Initialize default badge states
     */
    private fun initializeBadges() {
        val initialBadges = mutableMapOf<String, BadgeInfo>()

        // Initialize badges for all supported routes
        supportedRoutes.forEach { route ->
            initialBadges[route] = BadgeInfo(count = 0)
        }

        _badgeData.value = initialBadges
    }

    /**
     * Update badge for a specific navigation route
     */
    fun updateBadge(route: String, count: Int, style: BadgeStyle = BadgeStyle.DEFAULT, contentDescription: String = "") {
        if (route !in supportedRoutes) return

        val currentBadges = _badgeData.value.toMutableMap()
        currentBadges[route] = BadgeInfo(count, style, contentDescription)
        _badgeData.value = currentBadges
    }

    /**
     * Update badge for a specific navigation item by index (legacy support)
     */
    fun updateBadge(itemId: Int, count: Int, contentDescription: String = "") {
        // Map item IDs to routes (this is a simplified mapping)
        val route = when (itemId) {
            0 -> "home"
            1 -> "tasks"
            2 -> "social"
            3 -> "settings"
            else -> return
        }
        updateBadge(route, count, BadgeStyle.DEFAULT, contentDescription)
    }

    /**
     * Increment badge count for a route
     */
    fun incrementBadge(route: String, style: BadgeStyle = BadgeStyle.DEFAULT) {
        val currentBadge = _badgeData.value[route] ?: BadgeInfo(0)
        updateBadge(route, currentBadge.count + 1, style, currentBadge.contentDescription)
    }

    /**
     * Decrement badge count for a route
     */
    fun decrementBadge(route: String) {
        val currentBadge = _badgeData.value[route] ?: BadgeInfo(0)
        val newCount = maxOf(0, currentBadge.count - 1)
        updateBadge(route, newCount, currentBadge.style, currentBadge.contentDescription)
    }

    /**
     * Clear badge for a specific route
     */
    fun clearBadge(route: String) {
        updateBadge(route, 0, BadgeStyle.DEFAULT, "")
    }

    /**
     * Clear all badges
     */
    fun clearAllBadges() {
        val clearedBadges = mutableMapOf<String, BadgeInfo>()
        supportedRoutes.forEach { route ->
            clearedBadges[route] = BadgeInfo(count = 0)
        }
        _badgeData.value = clearedBadges
    }

    /**
     * Get badge info for a specific route
     */
    fun getBadgeInfo(route: String): BadgeInfo? {
        return _badgeData.value[route]
    }

    /**
     * Update streak-related badge based on current streak
     */
    private fun updateStreakBadge(streak: Int) {
        when {
            streak == 0 -> {
                // No streak, show motivational badge
                updateBadge("home", 1, BadgeStyle.WARNING, "Start your study streak!")
            }
            streak >= 7 -> {
                // Good streak, show celebration
                updateBadge("home", 1, BadgeStyle.SUCCESS, "$streak day streak!")
            }
            else -> {
                // Clear streak badge
                clearBadge("home")
            }
        }
    }

    /**
     * Update achievement-related badge
     */
    private fun updateAchievementBadge(achievements: List<String>) {
        val newAchievements = achievements.size
        if (newAchievements > 0) {
            updateBadge("social", newAchievements, BadgeStyle.CELEBRATION, "$newAchievements new achievements!")
        } else {
            clearBadge("social")
        }
    }

    /**
     * Check if a route has an active badge
     */
    fun hasBadge(route: String): Boolean {
        return (_badgeData.value[route]?.count ?: 0) > 0
    }

    /**
     * Get total badge count across all routes
     */
    fun getTotalBadgeCount(): Int {
        return _badgeData.value.values.sumOf { it.count }
    }
}