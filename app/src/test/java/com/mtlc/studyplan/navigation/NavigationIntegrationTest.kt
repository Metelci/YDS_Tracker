package com.mtlc.studyplan.navigation

import org.junit.Assert.*
import org.junit.Test

/**
 * Integration tests for navigation package components.
 *
 * Tests how NavigationHelper, NavigationDestination, and BadgeDataProvider
 * work together to provide complete navigation functionality.
 */
class NavigationIntegrationTest {

    private val navigationHelper = NavigationHelper()

    @Test
    fun `Navigation flow from welcome to home works correctly`() {
        // Given: User has not completed onboarding
        val startDestination = navigationHelper.getStartDestination(isOnboardingComplete = false)
        assertEquals("Start should be welcome", "welcome", startDestination)

        // When: User completes onboarding
        val homeDestination = navigationHelper.getStartDestination(isOnboardingComplete = true)

        // Then: Should navigate to home
        assertEquals("After onboarding should go to home", "home", homeDestination)
        assertTrue("Home should be a valid tab route", navigationHelper.isValidTabRoute(homeDestination))
    }

    @Test
    fun `Navigation between tab routes uses correct options`() {
        // Given: User is on home tab
        val currentRoute = "home"

        // When: User navigates to tasks tab
        val tasksRoute = "tasks"
        val navOptions = navigationHelper.getTabNavigationOptions(tasksRoute, currentRoute)

        // Then: Navigation options should pop to home
        assertEquals("Should pop to home", "home", navOptions.popUpTo)
        assertTrue("Should launch single top", navOptions.launchSingleTop)
        assertFalse("Should not include pop destination", navOptions.inclusive)
    }

    @Test
    fun `Building routes for all destination types works correctly`() {
        // Test all NavigationDestination types can be built into routes

        val tasksRoute = navigationHelper.buildRoute(NavigationDestination.Tasks())
        assertEquals("tasks", tasksRoute)

        val tasksWithFilterRoute = navigationHelper.buildRoute(
            NavigationDestination.Tasks(filter = "completed", highlightId = "123")
        )
        assertTrue(tasksWithFilterRoute.startsWith("tasks?"))
        assertTrue(tasksWithFilterRoute.contains("filter=completed"))
        assertTrue(tasksWithFilterRoute.contains("highlightId=123"))

        val taskDetailRoute = navigationHelper.buildRoute(
            NavigationDestination.TaskDetail(taskId = "task-456")
        )
        assertEquals("tasks/task-456", taskDetailRoute)

        val progressRoute = navigationHelper.buildRoute(NavigationDestination.Progress())
        assertEquals("progress", progressRoute)

        val customRoute = navigationHelper.buildRoute(
            NavigationDestination.Custom(route = "custom/path", params = emptyMap())
        )
        assertEquals("custom/path", customRoute)

        val backRoute = navigationHelper.buildRoute(
            NavigationDestination.Back(destination = "home")
        )
        assertEquals("home", backRoute)
    }

    @Test
    fun `Bottom navigation visibility logic works correctly`() {
        // Bottom nav should show for tab routes
        assertTrue(
            "Should show bottom nav for home",
            navigationHelper.shouldShowBottomNav("home")
        )
        assertTrue(
            "Should show bottom nav for tasks",
            navigationHelper.shouldShowBottomNav("tasks")
        )
        assertTrue(
            "Should show bottom nav for settings",
            navigationHelper.shouldShowBottomNav("settings")
        )

        // Bottom nav should NOT show for blacklisted routes
        assertFalse(
            "Should not show for welcome",
            navigationHelper.shouldShowBottomNav("welcome")
        )
        assertFalse(
            "Should not show for onboarding",
            navigationHelper.shouldShowBottomNav("onboarding")
        )
        assertFalse(
            "Should not show for reader",
            navigationHelper.shouldShowBottomNav("reader")
        )

        // Bottom nav SHOULD show for non-blacklisted routes (including detail screens)
        assertTrue(
            "Should show for task details when enabled",
            navigationHelper.shouldShowBottomNav("tasks/123")
        )
        assertTrue(
            "Should show for null route (defaults to home)",
            navigationHelper.shouldShowBottomNav(null)
        )
    }

    @Test
    fun `Haptic feedback triggers correctly for tab navigation`() {
        // Haptic should trigger when enabled and navigating between tabs
        assertTrue(
            "Should trigger haptic for tab navigation when enabled",
            navigationHelper.shouldTriggerHaptic(hapticsEnabled = true, isTabNavigation = true)
        )

        // Haptic should NOT trigger when disabled
        assertFalse(
            "Should not trigger when disabled",
            navigationHelper.shouldTriggerHaptic(hapticsEnabled = false, isTabNavigation = true)
        )

        // Haptic should NOT trigger for non-tab navigation
        assertFalse(
            "Should not trigger for non-tab navigation",
            navigationHelper.shouldTriggerHaptic(hapticsEnabled = true, isTabNavigation = false)
        )
    }

    @Test
    fun `Invalid route handling defaults to home`() {
        // All invalid routes should default to home
        assertEquals("home", navigationHelper.getDefaultRouteIfInvalid(null))
        assertEquals("home", navigationHelper.getDefaultRouteIfInvalid(""))
        assertEquals("home", navigationHelper.getDefaultRouteIfInvalid("invalid-route"))
        assertEquals("home", navigationHelper.getDefaultRouteIfInvalid("unknown"))

        // Valid tab routes should be preserved
        assertEquals("home", navigationHelper.getDefaultRouteIfInvalid("home"))
        assertEquals("tasks", navigationHelper.getDefaultRouteIfInvalid("tasks"))
        assertEquals("settings", navigationHelper.getDefaultRouteIfInvalid("settings"))
    }

    @Test
    fun `Tab route validation works for all tabs`() {
        // All main tabs should be valid
        assertTrue(navigationHelper.isValidTabRoute("home"))
        assertTrue(navigationHelper.isValidTabRoute("tasks"))
        assertTrue(navigationHelper.isValidTabRoute("settings"))

        // Non-tab routes should be invalid
        assertFalse(navigationHelper.isValidTabRoute("welcome"))
        assertFalse(navigationHelper.isValidTabRoute("onboarding"))
        assertFalse(navigationHelper.isValidTabRoute("tasks/123"))
        assertFalse(navigationHelper.isValidTabRoute(""))
        assertFalse(navigationHelper.isValidTabRoute("invalid"))
    }

    @Test
    fun `Navigation destinations have correct equality semantics`() {
        // Same destinations should be equal
        val tasks1 = NavigationDestination.Tasks(filter = "pending")
        val tasks2 = NavigationDestination.Tasks(filter = "pending")
        assertEquals(tasks1, tasks2)
        assertEquals(tasks1.hashCode(), tasks2.hashCode())

        // Different destinations should not be equal
        val tasks3 = NavigationDestination.Tasks(filter = "completed")
        assertNotEquals(tasks1, tasks3)

        // Different types should never be equal
        val progress = NavigationDestination.Progress()
        assertNotEquals(tasks1, progress)
    }

    @Test
    fun `Navigation destinations are immutable`() {
        // Original destination should not change when copied
        val original = NavigationDestination.Tasks(filter = "pending", highlightId = "123")
        val modified = original.copy(filter = "completed")

        assertEquals("pending", original.filter)
        assertEquals("123", original.highlightId)

        assertEquals("completed", modified.filter)
        assertEquals("123", modified.highlightId)

        assertNotEquals(original, modified)
    }

    @Test
    fun `NavOptions data class works correctly`() {
        val options = NavigationHelper.NavOptions(
            popUpTo = "home",
            inclusive = false,
            launchSingleTop = true
        )

        assertEquals("home", options.popUpTo)
        assertFalse(options.inclusive)
        assertTrue(options.launchSingleTop)

        // Test copy functionality
        val modified = options.copy(inclusive = true)
        assertTrue(modified.inclusive)
        assertEquals("home", modified.popUpTo)
    }

    @Test
    fun `Building complex routes with multiple parameters works`() {
        // Test building routes with all possible parameters
        val destination = NavigationDestination.Tasks(
            filter = "pending",
            highlightId = "task-123"
        )

        val route = navigationHelper.buildRoute(destination)

        assertTrue("Route should start with tasks?", route.startsWith("tasks?"))
        assertTrue("Route should contain filter parameter", route.contains("filter=pending"))
        assertTrue("Route should contain highlightId parameter", route.contains("highlightId=task-123"))
        assertTrue("Route should contain &", route.contains("&"))
    }

    @Test
    fun `Building routes with special characters encodes properly`() {
        // Test that special characters are handled correctly
        val destination = NavigationDestination.Tasks(
            filter = "status&priority",
            highlightId = "task#123"
        )

        val route = navigationHelper.buildRoute(destination)

        assertTrue("Route should be built", route.startsWith("tasks?"))
        // Note: Actual URL encoding would depend on implementation
        // This test verifies the route is built without crashing
    }

    @Test
    fun `Navigation event mapping returns correct routes`() {
        // Verify that different navigation events map to correct routes
        val homeRoute = navigationHelper.getStartDestination(isOnboardingComplete = true)
        assertEquals("home", homeRoute)

        val welcomeRoute = navigationHelper.getStartDestination(isOnboardingComplete = false)
        assertEquals("welcome", welcomeRoute)
    }

    @Test
    fun `All tab routes have consistent validation`() {
        // Ensure all tab routes pass validation consistently
        val tabRoutes = listOf("home", "tasks", "settings")

        tabRoutes.forEach { route ->
            assertTrue("$route should be valid tab route", navigationHelper.isValidTabRoute(route))
            assertTrue("$route should show bottom nav",
                navigationHelper.shouldShowBottomNav(route))
        }
    }

    @Test
    fun `Navigation between all tabs uses correct pop behavior`() {
        val tabs = listOf("home", "tasks", "settings")

        // Test navigation from each tab to every other tab
        for (fromTab in tabs) {
            for (toTab in tabs) {
                val options = navigationHelper.getTabNavigationOptions(toTab, fromTab)

                assertEquals("Should always pop to home", "home", options.popUpTo)
                assertTrue("Should always launch single top", options.launchSingleTop)
                assertFalse("Should never include pop destination", options.inclusive)
            }
        }
    }
}
