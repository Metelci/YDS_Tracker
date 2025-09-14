package com.mtlc.studyplan.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

// Route constants
object Routes {
    const val TODAY_ROUTE = "today"
    const val PLAN_ROUTE = "plan"
    const val WELCOME_ROUTE = "welcome"
    const val ONBOARDING_ROUTE = "onboarding"
    const val PRACTICE_ROUTE = "practice"
    const val PROGRESS_ROUTE = "progress"
    const val READER_ROUTE = "reader"
    const val REVIEW_ROUTE = "review"
    const val MOCK_EXAM_ROUTE = "mock_exam"
}

// Placeholder composable for missing screens
@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This screen is under development",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Intentionally no todayGraph() here to avoid conflicts with the real one
// in com.mtlc.studyplan.feature.today.todayGraph(navController).

// Feature screens
@Composable
fun WelcomeRoute(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Tap continue to start", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDone) { Text("Continue") }
    }
}
