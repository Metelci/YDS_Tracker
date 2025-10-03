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
import com.mtlc.studyplan.core.WorkingHomeScreen
import com.mtlc.studyplan.data.TaskRepositoryImpl
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

/**
 * Minimal MainActivity as the app's entry point.
 * Displays the working home screen using Jetpack Compose.
 */
class MinimalMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            val database = remember { StudyPlanDatabase.getDatabase(context) }
            val databaseTaskRepo = remember { com.mtlc.studyplan.repository.TaskRepository(database.taskDao()) }
            val taskRepository = remember { TaskRepositoryImpl(databaseTaskRepo) }
            val appIntegrationManager = remember { AppIntegrationManager(taskRepository) }
            
            StudyPlanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkingHomeScreen(
                        appIntegrationManager = appIntegrationManager
                    )
                }
            }
        }
    }
}
