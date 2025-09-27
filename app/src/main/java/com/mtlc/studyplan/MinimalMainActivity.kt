package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.data.PlanDataSource
import com.mtlc.studyplan.ui.base.LocaleAwareActivity
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

class MinimalMainActivity : LocaleAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Make status bar content dark in light mode for better visibility
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        // Initialize PlanDataSource before creating ViewModels
        PlanDataSource.initialize(this)

        setContent {
            StudyPlanTheme {
                GradientBackground {
                    val sharedViewModel: SharedAppViewModel = viewModel()
                    AppNavHost(
                        sharedViewModel = sharedViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F5F5), // Light grey at top
                        Color(0xFFFFFFFF)  // White at bottom
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        content()
    }
}