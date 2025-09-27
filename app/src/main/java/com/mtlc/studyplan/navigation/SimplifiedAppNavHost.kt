package com.mtlc.studyplan.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.annotation.SuppressLint
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mtlc.studyplan.core.WorkingHomeScreen
import com.mtlc.studyplan.core.WorkingTasksScreen
import com.mtlc.studyplan.integration.AppIntegrationManager
import org.koin.core.context.GlobalContext

@Composable
fun SimplifiedAppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Resolve integration manager via Koin
    val mainAppIntegrationManager = remember {
        GlobalContext.get().get<AppIntegrationManager>()
    }

    // Create StudyProgressRepository for week progression tracking
    val studyProgressRepository = remember {
        com.mtlc.studyplan.data.StudyProgressRepository(context)
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
                    studyProgressRepository = studyProgressRepository,
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
