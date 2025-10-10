package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.mtlc.studyplan.data.TaskRepositoryImpl
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

/**
 * Minimal MainActivity as the app's entry point.
 * Uses AppNavHost for proper navigation with bottom bar.
 */
import com.mtlc.studyplan.ui.base.LocaleAwareActivity

class MinimalMainActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize PlanDataSource before any repository usage
        com.mtlc.studyplan.data.PlanDataSource.initialize(this)

        setContent {
            val context = LocalContext.current
            val view = LocalView.current
            val application = context.applicationContext as android.app.Application
            val database = remember { StudyPlanDatabase.getDatabase(context) }
            val databaseTaskRepo = remember { com.mtlc.studyplan.repository.TaskRepository(database.taskDao()) }
            val taskRepository = remember { TaskRepositoryImpl(databaseTaskRepo) }
            val mainAppIntegrationManager = remember { AppIntegrationManager(taskRepository) }
            val sharedViewModel = remember { SharedAppViewModel(application) }

            StudyPlanTheme {
                // Apply robust system bar styling on supported devices
                val window = (this@MinimalMainActivity).window
                val useDarkIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f
                SideEffect {
                    // Draw behind system bars and control icon appearance explicitly
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    @Suppress("DEPRECATION")
                    window.statusBarColor = Color.Transparent.toArgb()
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.isAppearanceLightStatusBars = useDarkIcons
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        mainAppIntegrationManager = mainAppIntegrationManager,
                        sharedViewModel = sharedViewModel,
                        taskRepository = taskRepository
                    )
                }
            }
        }
    }
}
