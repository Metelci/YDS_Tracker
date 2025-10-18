package com.mtlc.studyplan.navigation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NavigationHelperTest {

    private lateinit var navigationHelper: NavigationHelper

    @Before
    fun setup() {
        navigationHelper = NavigationHelper()
    }

    // ===== Start Destination Tests =====

    @Test
    fun `getStartDestination returns home when onboarding complete`() {
        val result = navigationHelper.getStartDestination(isOnboardingComplete = true)
        assertEquals("home", result)
    }

    @Test
    fun `getStartDestination returns welcome when onboarding not complete`() {
        val result = navigationHelper.getStartDestination(isOnboardingComplete = false)
        assertEquals("welcome", result)
    }

    // ===== Build Route Tests =====

    @Test
    fun `buildRoute for Tasks without parameters`() {
        val destination = NavigationDestination.Tasks()
        val result = navigationHelper.buildRoute(destination)
        assertEquals("tasks", result)
    }

    @Test
    fun `buildRoute for Tasks with filter only`() {
        val destination = NavigationDestination.Tasks(filter = "pending")
        val result = navigationHelper.buildRoute(destination)
        assertEquals("tasks?filter=pending", result)
    }

    @Test
    fun `buildRoute for Tasks with highlightId only`() {
        val destination = NavigationDestination.Tasks(highlightId = "task-123")
        val result = navigationHelper.buildRoute(destination)
        assertEquals("tasks?highlightId=task-123", result)
    }

    @Test
    fun `buildRoute for Tasks with both parameters`() {
        val destination = NavigationDestination.Tasks(filter = "completed", highlightId = "task-456")
        val result = navigationHelper.buildRoute(destination)
        assertTrue(result.startsWith("tasks?"))
        assertTrue(result.contains("filter=completed"))
        assertTrue(result.contains("highlightId=task-456"))
    }

    @Test
    fun `buildRoute for TaskDetail`() {
        val destination = NavigationDestination.TaskDetail(taskId = "task-789")
        val result = navigationHelper.buildRoute(destination)
        assertEquals("tasks/task-789", result)
    }

    @Test
    fun `buildRoute for Progress without parameters`() {
        val destination = NavigationDestination.Progress()
        val result = navigationHelper.buildRoute(destination)
        assertEquals("progress", result)
    }

    @Test
    fun `buildRoute for Progress with timeRange`() {
        val destination = NavigationDestination.Progress(timeRange = "weekly")
        val result = navigationHelper.buildRoute(destination)
        assertEquals("progress?timeRange=weekly", result)
    }

    @Test
    fun `buildRoute for Progress with highlight`() {
        val destination = NavigationDestination.Progress(highlight = "achievement-123")
        val result = navigationHelper.buildRoute(destination)
        assertEquals("progress?highlight=achievement-123", result)
    }

    @Test
    fun `buildRoute for Progress with both parameters`() {
        val destination = NavigationDestination.Progress(timeRange = "monthly", highlight = "ach-456")
        val result = navigationHelper.buildRoute(destination)
        assertTrue(result.startsWith("progress?"))
        assertTrue(result.contains("timeRange=monthly"))
        assertTrue(result.contains("highlight=ach-456"))
    }

    @Test
    fun `buildRoute for Custom without parameters`() {
        val destination = NavigationDestination.Custom(route = "custom/route", params = emptyMap())
        val result = navigationHelper.buildRoute(destination)
        assertEquals("custom/route", result)
    }

    @Test
    fun `buildRoute for Custom with parameters`() {
        val destination = NavigationDestination.Custom(
            route = "custom/route",
            params = mapOf("id" to "123", "mode" to "edit")
        )
        val result = navigationHelper.buildRoute(destination)
        assertTrue(result.startsWith("custom/route?"))
        assertTrue(result.contains("id=123"))
        assertTrue(result.contains("mode=edit"))
    }

    @Test
    fun `buildRoute for Back`() {
        val destination = NavigationDestination.Back(destination = "home")
        val result = navigationHelper.buildRoute(destination)
        assertEquals("home", result)
    }

    // ===== Tab Route Validation Tests =====

    @Test
    fun `isValidTabRoute returns true for known routes`() {
        listOf("home", "tasks", "settings", "progress").forEach { route ->
            assertTrue(navigationHelper.isValidTabRoute(route))
        }
    }

    @Test
    fun `isValidTabRoute returns false for unknown routes`() {
        assertFalse(navigationHelper.isValidTabRoute("community"))
    }

    @Test
    fun `getDefaultRouteIfInvalid returns same route when valid`() {
        assertEquals("tasks", navigationHelper.getDefaultRouteIfInvalid("tasks"))
    }

    @Test
    fun `getDefaultRouteIfInvalid returns home when route invalid`() {
        assertEquals("home", navigationHelper.getDefaultRouteIfInvalid("unknown"))
        assertEquals("home", navigationHelper.getDefaultRouteIfInvalid(null))
    }

    // ===== Haptics Tests =====

    @Test
    fun `shouldTriggerHaptic returns true when enabled and tab navigation`() {
        assertTrue(navigationHelper.shouldTriggerHaptic(hapticsEnabled = true, isTabNavigation = true))
    }

    @Test
    fun `shouldTriggerHaptic returns false otherwise`() {
        assertFalse(navigationHelper.shouldTriggerHaptic(hapticsEnabled = false, isTabNavigation = true))
        assertFalse(navigationHelper.shouldTriggerHaptic(hapticsEnabled = true, isTabNavigation = false))
    }

    // ===== Navigation Options Tests =====

    @Test
    fun `getTabNavigationOptions for navigating to home from other route`() {
        val result = navigationHelper.getTabNavigationOptions(targetRoute = "home", currentRoute = "tasks")

        assertEquals("home", result.popUpTo)
        assertTrue(result.launchSingleTop)
        assertFalse(result.inclusive)
    }

    @Test
    fun `getTabNavigationOptions for navigating between non-home routes`() {
        val result = navigationHelper.getTabNavigationOptions(targetRoute = "tasks", currentRoute = "settings")

        assertEquals("home", result.popUpTo)
        assertTrue(result.launchSingleTop)
        assertFalse(result.inclusive)
    }

    @Test
    fun `NavOptions has correct defaults`() {
        val options = NavigationHelper.NavOptions()

        assertEquals("home", options.popUpTo)
        assertTrue(options.launchSingleTop)
        assertFalse(options.inclusive)
    }

    @Test
    fun `NavOptions can be created with custom values`() {
        val options = NavigationHelper.NavOptions(
            popUpTo = "tasks",
            launchSingleTop = false,
            inclusive = true
        )

        assertEquals("tasks", options.popUpTo)
        assertFalse(options.launchSingleTop)
        assertTrue(options.inclusive)
    }

    @Test
    fun `NavOptions copy works correctly`() {
        val original = NavigationHelper.NavOptions(popUpTo = "tasks")
        val modified = original.copy(inclusive = true)

        assertEquals("tasks", modified.popUpTo)
        assertTrue(modified.launchSingleTop)
        assertTrue(modified.inclusive)
    }

    // ===== Bottom Navigation Visibility Tests =====

    @Test
    fun `shouldShowBottomNav returns true for home when enabled`() {
        val result = navigationHelper.shouldShowBottomNav(route = "home")
        assertTrue(result)
    }

    @Test
    fun `shouldShowBottomNav returns true for tasks when enabled`() {
        val result = navigationHelper.shouldShowBottomNav(route = "tasks")
        assertTrue(result)
    }

    @Test
    fun `shouldShowBottomNav returns false for routes excluded from bottom nav`() {
        assertFalse(navigationHelper.shouldShowBottomNav(route = "welcome"))
        assertFalse(navigationHelper.shouldShowBottomNav(route = "onboarding"))
        assertFalse(navigationHelper.shouldShowBottomNav(route = "reader"))
        assertFalse(navigationHelper.shouldShowBottomNav(route = "reader/123"))
    }

    @Test
    fun `shouldShowBottomNav returns true for null route when enabled`() {
        val result = navigationHelper.shouldShowBottomNav(route = null)
        assertTrue(result)
    }

    // ===== Navigation Event Mapping Tests =====

    private class GoToHome
    private class GoToTasks
    private class GoToSettings
    private class GoToProgress
    private class UnknownEvent

    @Test
    fun `navigationEventToRoute maps known events`() {
        assertEquals("home", navigationHelper.navigationEventToRoute(GoToHome()))
        assertEquals("tasks", navigationHelper.navigationEventToRoute(GoToTasks()))
        assertEquals("settings", navigationHelper.navigationEventToRoute(GoToSettings()))
        assertEquals("progress", navigationHelper.navigationEventToRoute(GoToProgress()))
    }

    @Test
    fun `navigationEventToRoute returns null for unknown event`() {
        val result = navigationHelper.navigationEventToRoute(UnknownEvent())
        assertNull(result)
    }
}
