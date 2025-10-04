package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mtlc.studyplan.data.TaskRepositoryImpl
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.theme.ThemeManager
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

/**
 * Minimal MainActivity as the app's entry point.
 * Uses AppNavHost for proper navigation with bottom bar.
 */
class MinimalMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize PlanDataSource before any repository usage
        com.mtlc.studyplan.data.PlanDataSource.initialize(this)

        setContent {
            val context = LocalContext.current
            val application = context.applicationContext as android.app.Application
            val database = remember { StudyPlanDatabase.getDatabase(context) }
            val databaseTaskRepo = remember { com.mtlc.studyplan.repository.TaskRepository(database.taskDao()) }
            val taskRepository = remember { TaskRepositoryImpl(databaseTaskRepo) }
            val mainAppIntegrationManager = remember { AppIntegrationManager(taskRepository) }
            val themeManager = remember { ThemeManager(context) }
            val sharedViewModel = remember { SharedAppViewModel(application) }

            StudyPlanTheme(themeManager) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        mainAppIntegrationManager = mainAppIntegrationManager,
                        themeManager = themeManager,
                        sharedViewModel = sharedViewModel,
                        taskRepository = taskRepository
                    )
                }
            }
        }
    }
}
