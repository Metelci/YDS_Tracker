package com.mtlc.studyplan.navigation

/**
 * Helper class for navigation logic that can be unit tested.
 * Extracts navigation decisions and route building from Composable functions.
 */
class NavigationHelper {

    /**
     * Determines the start destination based on onboarding completion status.
     */
    fun getStartDestination(isOnboardingComplete: Boolean): String {
        return if (isOnboardingComplete) "home" else "welcome"
    }

    /**
     * Builds a navigation route with optional parameters.
     */
    fun buildRoute(destination: NavigationDestination): String {
        return when (destination) {
            is NavigationDestination.Tasks -> buildTasksRoute(destination)
            is NavigationDestination.TaskDetail -> buildTaskDetailRoute(destination)
            is NavigationDestination.Progress -> buildProgressRoute(destination)
            is NavigationDestination.Custom -> buildCustomRoute(destination)
            is NavigationDestination.Back -> buildBackRoute(destination)
        }
    }

    private fun buildTasksRoute(destination: NavigationDestination.Tasks): String {
        val params = mutableListOf<String>()
        destination.filter?.let { params.add("filter=$it") }
        destination.highlightId?.let { params.add("highlightId=$it") }
        return if (params.isEmpty()) "tasks" else "tasks?${params.joinToString("&")}"
    }

    private fun buildTaskDetailRoute(destination: NavigationDestination.TaskDetail): String {
        return "tasks/${destination.taskId}"
    }

    private fun buildProgressRoute(destination: NavigationDestination.Progress): String {
        val params = mutableListOf<String>()
        destination.timeRange?.let { params.add("timeRange=$it") }
        destination.highlight?.let { params.add("highlight=$it") }
        return if (params.isEmpty()) "progress" else "progress?${params.joinToString("&")}"
    }

    private fun buildCustomRoute(destination: NavigationDestination.Custom): String {
        val params = destination.params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return if (params.isEmpty()) destination.route else "${destination.route}?$params"
    }

    private fun buildBackRoute(destination: NavigationDestination.Back): String {
        return destination.destination
    }

    /**
     * Validates if a route is a valid tab destination.
     */
    fun isValidTabRoute(route: String): Boolean {
        return route in listOf("home", "tasks", "settings", "progress")
    }

    /**
     * Gets the default route if the provided route is invalid.
     */
    fun getDefaultRouteIfInvalid(route: String?): String {
        return if (route != null && isValidTabRoute(route)) route else "home"
    }

    /**
     * Determines if haptic feedback should be triggered for navigation.
     */
    fun shouldTriggerHaptic(hapticsEnabled: Boolean, isTabNavigation: Boolean): Boolean {
        return hapticsEnabled && isTabNavigation
    }

    /**
     * Builds navigation options for tab navigation.
     */
    data class NavOptions(
        val popUpTo: String = "home",
        val launchSingleTop: Boolean = true,
        val inclusive: Boolean = false
    )

    fun getTabNavigationOptions(targetRoute: String, currentRoute: String): NavOptions {
        // If navigating to home from anywhere, pop everything
        return if (targetRoute == "home" && currentRoute != "home") {
            NavOptions(popUpTo = "home", launchSingleTop = true, inclusive = false)
        } else {
            NavOptions(popUpTo = "home", launchSingleTop = true, inclusive = false)
        }
    }

    /**
     * Determines if bottom navigation should be visible for a given route.
     */
    fun shouldShowBottomNav(route: String?): Boolean {
        val routesWithoutBottomNav = listOf("welcome", "onboarding", "reader")
        val currentRoute = route ?: "home"

        return !routesWithoutBottomNav.any { currentRoute.startsWith(it) }
    }

    /**
     * Maps navigation event to route string.
     */
    fun navigationEventToRoute(event: Any): String? {
        return when (event::class.simpleName) {
            "GoToHome" -> "home"
            "GoToTasks" -> "tasks"
            "GoToSettings" -> "settings"
            "GoToProgress" -> "progress"
            else -> null
        }
    }
}
