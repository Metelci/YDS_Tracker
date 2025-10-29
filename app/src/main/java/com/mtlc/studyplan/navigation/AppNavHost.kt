
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mtlc.studyplan.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mtlc.studyplan.R
import com.mtlc.studyplan.feature.Routes.WELCOME_ROUTE
import com.mtlc.studyplan.navigation.NavTab
import com.mtlc.studyplan.navigation.AppBottomBar

@Suppress("UNUSED_PARAMETER")
@Composable
fun AppNavHost(
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel,
    mainAppIntegrationManager: com.mtlc.studyplan.integration.AppIntegrationManager,
    studyProgressRepository: com.mtlc.studyplan.data.StudyProgressRepository,
    taskRepository: com.mtlc.studyplan.data.TaskRepository,
    appIntegrationManager: com.mtlc.studyplan.settings.integration.AppIntegrationManager? = null // reserved for future wiring
) {
    val navController = rememberNavController()
    val coordinator = remember { AppNavigationCoordinator() }
    val haptics = LocalHapticFeedback.current
    val settingsIntegration = com.mtlc.studyplan.settings.rememberSettingsIntegration()
    val hapticsEnabled by settingsIntegration.isHapticFeedbackEnabled().collectAsState(initial = true)

    HandleNavigationEvents(sharedViewModel, navController, coordinator)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val resolvedRoute = remember(currentRoute) { coordinator.resolveRoute(currentRoute) }
    val showBottomBar = coordinator.shouldShowBottomBar(currentRoute)

    val tabs = listOf(
        NavTab("home", Icons.Filled.Home, stringResource(R.string.nav_home)),
        NavTab("tasks", Icons.Filled.CheckCircle, stringResource(R.string.nav_tasks)),
        NavTab("settings", Icons.Filled.Settings, stringResource(R.string.nav_settings))
    )

    Scaffold(
        containerColor = Color.Transparent, // Allow gradient background from theme to show through
        bottomBar = {
        if (showBottomBar) {
            AppBottomBar(
                currentRoute = resolvedRoute,
                tabs = tabs,
                onTabSelected = { route ->
                    if (hapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    coordinator.onTabSelected(navController, route)
                }
            )
        }
    }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = WELCOME_ROUTE,
                modifier = Modifier.fillMaxSize()
            ) {
                appNavigationGraph(
                    AppNavigationGraphParams(
                        navController = navController,
                        sharedViewModel = sharedViewModel,
                        mainAppIntegrationManager = mainAppIntegrationManager,
                        studyProgressRepository = studyProgressRepository,
                        taskRepository = taskRepository,
                        hapticFeedback = haptics,
                        hapticsEnabled = hapticsEnabled
                    )
                )
            }
        }
    }
}

@Composable
private fun HandleNavigationEvents(
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel,
    navController: androidx.navigation.NavHostController,
    coordinator: AppNavigationCoordinator
) {
    LaunchedEffect(navController, sharedViewModel) {
        sharedViewModel.navigationEvent.collect { event ->
            coordinator.handleEvent(navController, event)
        }
    }
}
