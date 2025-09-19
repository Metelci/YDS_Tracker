package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.mtlc.studyplan.core.WorkingHomeScreen
import com.mtlc.studyplan.core.WorkingTasksScreen
import com.mtlc.studyplan.data.TaskRepositoryImpl
import com.mtlc.studyplan.integration.AppIntegrationManager
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivitySimple : ComponentActivity() {

    private lateinit var appIntegrationManager: AppIntegrationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize our working AppIntegrationManager
        appIntegrationManager = AppIntegrationManager(
            taskRepository = TaskRepositoryImpl()
        )

        setContent {
            MaterialTheme {
                SimpleAppNavigation()
            }
        }
    }

    @Composable
    private fun SimpleAppNavigation() {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                WorkingHomeScreen(
                    appIntegrationManager = appIntegrationManager,
                    onNavigateToTasks = {
                        navController.navigate("tasks")
                    },
                    onNavigateToProgress = {
                        // Navigate to progress screen when implemented
                    }
                )
            }

            composable("tasks") {
                WorkingTasksScreen(
                    appIntegrationManager = appIntegrationManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}