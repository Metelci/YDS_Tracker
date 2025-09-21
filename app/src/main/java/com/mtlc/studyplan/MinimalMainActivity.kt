package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.data.PlanDataSource
import androidx.lifecycle.viewmodel.compose.viewModel

class MinimalMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize PlanDataSource before creating ViewModels
        PlanDataSource.initialize(this)

        setContent {
            MaterialTheme {
                val sharedViewModel: SharedAppViewModel = viewModel()
                AppNavHost(
                    sharedViewModel = sharedViewModel
                )
            }
        }
    }
}