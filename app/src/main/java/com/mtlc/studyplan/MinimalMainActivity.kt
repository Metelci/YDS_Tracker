package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.data.PlanDataSource
import com.mtlc.studyplan.ui.base.LocaleAwareActivity

class MinimalMainActivity : LocaleAwareActivity() {

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