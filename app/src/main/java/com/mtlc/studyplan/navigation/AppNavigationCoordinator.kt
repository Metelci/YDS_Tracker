package com.mtlc.studyplan.navigation

import androidx.navigation.NavHostController
import androidx.navigation.navOptions
import com.mtlc.studyplan.feature.Routes.WELCOME_ROUTE
import com.mtlc.studyplan.shared.NavigationEvent

class AppNavigationCoordinator(
    private val navigationHelper: NavigationHelper = NavigationHelper()
) {

    fun resolveRoute(route: String?): String =
        navigationHelper.getDefaultRouteIfInvalid(route ?: WELCOME_ROUTE)

    fun shouldShowBottomBar(route: String?): Boolean =
        navigationHelper.shouldShowBottomNav(route ?: WELCOME_ROUTE)

    fun handleEvent(navController: NavHostController, event: NavigationEvent) {
        when (event) {
            is NavigationEvent.GoToHome -> navController.navigate("home", navOptions {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            })

            is NavigationEvent.GoToTasks -> navController.navigate("tasks", navOptions {
                launchSingleTop = true
            })

            is NavigationEvent.GoToSettings -> navController.navigate("settings", navOptions {
                launchSingleTop = true
            })
        }
    }

    fun onTabSelected(navController: NavHostController, route: String) {
        navController.navigate(route) {
            popUpTo("home")
            launchSingleTop = true
        }
    }
}
