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
    fun `buildRoute for Social without parameters`() {
        val destination = NavigationDestination.Social()
        val result = navigationHelper.buildRoute(destination)
        assertEquals("social", result)
    }

    @Test
    fun `buildRoute for Social with tab`() {
        val destination = NavigationDestination.Social(tab = "friends")
        val result = navigationHelper.buildRoute(destination)
        assertEquals("social?tab=friends", result)
    }

    @Test
    fun `buildRoute for Social with achievementId`() {
        val destination = NavigationDestination.Social(achievementId = "ach-789")
        val result = navigationHelper.buildRoute(destination)
        assertEquals("social?achievementId=ach-789", result)
    }

    @Test
    fun `buildRoute for Social with both parameters`() {
        val destination = NavigationDestination.Social(tab = "leaderboard", achievementId = "ach-999")
        val result = navigationHelper.buildRoute(destination)
        assertTrue(result.startsWith("social?"))
        assertTrue(result.contains("tab=leaderboard"))
        assertTrue(result.contains("achievementId=ach-999"))
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

    // ===== Valid Tab Route Tests =====

    @Test
    fun `isValidTabRoute returns true for home`() {
        assertTrue(navigationHelper.isValidTabRoute("home"))
    }

    @Test
    fun `isValidTabRoute returns true for tasks`() {
        assertTrue(navigationHelper.isValidTabRoute("tasks"))
    }

    @Test
    fun `isValidTabRoute returns true for social`() {
        assertTrue(navigationHelper.isValidTabRoute("social"))
    }

    @Test
    fun `isValidTabRoute returns true for settings`() {
        assertTrue(navigationHelper.isValidTabRoute("settings"))
    }

    @Test
    fun `isValidTabRoute returns true for progress`() {
        assertTrue(navigationHelper.isValidTabRoute("progress"))
    }

    @Test
    fun `isValidTabRoute returns false for invalid route`() {
        assertFalse(navigationHelper.isValidTabRoute("invalid"))
    }

    @Test
    fun `isValidTabRoute returns false for empty string`() {
        assertFalse(navigationHelper.isValidTabRoute(""))
    }

    @Test
    fun `isValidTabRoute returns false for reader route`() {
        assertFalse(navigationHelper.isValidTabRoute("reader"))
    }

    // ===== Default Route Tests =====

    @Test
    fun `getDefaultRouteIfInvalid returns route if valid`() {
        val result = navigationHelper.getDefaultRouteIfInvalid("tasks")
        assertEquals("tasks", result)
    }

    @Test
    fun `getDefaultRouteIfInvalid returns home if invalid`() {
        val result = navigationHelper.getDefaultRouteIfInvalid("invalid")
        assertEquals("home", result)
    }

    @Test
    fun `getDefaultRouteIfInvalid returns home if null`() {
        val result = navigationHelper.getDefaultRouteIfInvalid(null)
        assertEquals("home", result)
    }

    @Test
    fun `getDefaultRouteIfInvalid returns home if empty`() {
        val result = navigationHelper.getDefaultRouteIfInvalid("")
        assertEquals("home", result)
    }

    // ===== Haptic Feedback Tests =====

    @Test
    fun `shouldTriggerHaptic returns true when enabled and tab navigation`() {
        val result = navigationHelper.shouldTriggerHaptic(hapticsEnabled = true, isTabNavigation = true)
        assertTrue(result)
    }

    @Test
    fun `shouldTriggerHaptic returns false when disabled`() {
        val result = navigationHelper.shouldTriggerHaptic(hapticsEnabled = false, isTabNavigation = true)
        assertFalse(result)
    }

    @Test
    fun `shouldTriggerHaptic returns false when not tab navigation`() {
        val result = navigationHelper.shouldTriggerHaptic(hapticsEnabled = true, isTabNavigation = false)
        assertFalse(result)
    }

    @Test
    fun `shouldTriggerHaptic returns false when both disabled`() {
        val result = navigationHelper.shouldTriggerHaptic(hapticsEnabled = false, isTabNavigation = false)
        assertFalse(result)
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
    fun `getTabNavigationOptions for navigating within non-home routes`() {
        val result = navigationHelper.getTabNavigationOptions(targetRoute = "tasks", currentRoute = "social")

        assertEquals("home", result.popUpTo)
        assertTrue(result.launchSingleTop)
        assertFalse(result.inclusive)
    }

    @Test
    fun `getTabNavigationOptions for staying on home`() {
        val result = navigationHelper.getTabNavigationOptions(targetRoute = "home", currentRoute = "home")

        assertEquals("home", result.popUpTo)
        assertTrue(result.launchSingleTop)
        assertFalse(result.inclusive)
    }

    // ===== Bottom Navigation Visibility Tests =====

    @Test
    fun `shouldShowBottomNav returns false when bottom nav disabled`() {
        val result = navigationHelper.shouldShowBottomNav(route = "home", bottomNavEnabled = false)
        assertFalse(result)
    }

    @Test
    fun `shouldShowBottomNav returns true for home when enabled`() {
        val result = navigationHelper.shouldShowBottomNav(route = "home", bottomNavEnabled = true)
        assertTrue(result)
    }

    @Test
    fun `shouldShowBottomNav returns true for tasks when enabled`() {
        val result = navigationHelper.shouldShowBottomNav(route = "tasks", bottomNavEnabled = true)
        assertTrue(result)
    }

    @Test
    fun `shouldShowBottomNav returns false for welcome route`() {
        val result = navigationHelper.shouldShowBottomNav(route = "welcome", bottomNavEnabled = true)
        assertFalse(result)
    }

    @Test
    fun `shouldShowBottomNav returns false for onboarding route`() {
        val result = navigationHelper.shouldShowBottomNav(route = "onboarding", bottomNavEnabled = true)
        assertFalse(result)
    }

    @Test
    fun `shouldShowBottomNav returns false for reader route`() {
        val result = navigationHelper.shouldShowBottomNav(route = "reader", bottomNavEnabled = true)
        assertFalse(result)
    }

    @Test
    fun `shouldShowBottomNav returns true for null route when enabled`() {
        val result = navigationHelper.shouldShowBottomNav(route = null, bottomNavEnabled = true)
        assertTrue(result)
    }

    @Test
    fun `shouldShowBottomNav handles reader with path`() {
        val result = navigationHelper.shouldShowBottomNav(route = "reader/123", bottomNavEnabled = true)
        assertFalse(result)
    }

    // ===== Navigation Event Mapping Tests =====

    @Test
    fun `navigationEventToRoute returns null for unknown event`() {
        val event = object {
            val name = "UnknownEvent"
        }
        val result = navigationHelper.navigationEventToRoute(event)
        assertNull(result)
    }

    // ===== NavOptions Data Class Tests =====

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
        val original = NavigationHelper.NavOptions(popUpTo = "social")
        val modified = original.copy(inclusive = true)

        assertEquals("social", modified.popUpTo)
        assertTrue(modified.launchSingleTop)
        assertTrue(modified.inclusive)
    }
}
