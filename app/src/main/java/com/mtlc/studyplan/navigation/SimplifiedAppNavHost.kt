package com.mtlc.studyplan.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mtlc.studyplan.core.WorkingHomeScreen
import com.mtlc.studyplan.core.WorkingTasksScreen
import com.mtlc.studyplan.integration.AppIntegrationManager
import org.koin.core.context.GlobalContext

@Composable
fun SimplifiedAppNavHost(
    mainAppIntegrationManager: AppIntegrationManager? = null,
    studyProgressRepository: com.mtlc.studyplan.data.StudyProgressRepository? = null,
    taskRepository: com.mtlc.studyplan.data.TaskRepository? = null,
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Use provided dependencies or fallback to Koin with proper lifecycle management
    val resolvedMainAppIntegrationManager = mainAppIntegrationManager ?: remember {
        GlobalContext.get().get<AppIntegrationManager>()
    }

    val resolvedStudyProgressRepository = studyProgressRepository ?: remember {
        com.mtlc.studyplan.data.StudyProgressRepository(context)
    }

    val resolvedTaskRepository = taskRepository ?: remember {
        GlobalContext.get().get<com.mtlc.studyplan.data.TaskRepository>()
    }

    val resolvedSharedViewModel = sharedViewModel ?: remember {
        GlobalContext.get().get<com.mtlc.studyplan.shared.SharedAppViewModel>()
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
                    appIntegrationManager = resolvedMainAppIntegrationManager,
                    onNavigateToTasks = {
                        navController.navigate("tasks")
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
                    appIntegrationManager = resolvedMainAppIntegrationManager,
                    studyProgressRepository = resolvedStudyProgressRepository,
                    taskRepository = resolvedTaskRepository,
                    sharedViewModel = resolvedSharedViewModel
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
