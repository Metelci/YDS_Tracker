package com.mtlc.studyplan.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mtlc.studyplan.core.WorkingHomeScreen
import com.mtlc.studyplan.core.WorkingTasksScreen
import com.mtlc.studyplan.integration.AppIntegrationManager

@Composable
fun SimplifiedAppNavHost() {
    val navController = rememberNavController()
    
    // Create main AppIntegrationManager for core functionality
    val mainAppIntegrationManager = remember {
        AppIntegrationManager(
            taskRepository = com.mtlc.studyplan.data.TaskRepositoryImpl()
        )
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            AnimatedContent(
                targetState = "home",
                label = "home_animation"
            ) { _ ->
                WorkingHomeScreen(
                    appIntegrationManager = mainAppIntegrationManager,
                    onNavigateToTasks = {
                        navController.navigate("tasks")
                    },
                    onNavigateToProgress = {
                        // Navigate to progress screen when implemented
                    }
                )
            }
        }

        composable("tasks") {
            AnimatedContent(
                targetState = "tasks",
                label = "tasks_animation"
            ) { _ ->
                WorkingTasksScreen(
                    appIntegrationManager = mainAppIntegrationManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                    )
            }
        }
        
        // Add a simple placeholder for other routes
        composable("{other}") { 
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Screen not implemented")
            }
        }
    }
}