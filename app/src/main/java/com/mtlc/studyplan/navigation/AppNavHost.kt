package com.mtlc.studyplan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mtlc.studyplan.feature.today.PLAN_ROUTE
import com.mtlc.studyplan.feature.today.TODAY_ROUTE
import com.mtlc.studyplan.feature.today.todayGraph
import com.mtlc.studyplan.feature.reader.PassageUi
import com.mtlc.studyplan.feature.reader.ReaderScreen
import com.mtlc.studyplan.feature.mock.MockExamRoute
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TODAY_ROUTE
    ) {
        todayGraph(navController)  // pass the navController to todayGraph
        
        // Add route for the regular study plan screen
        composable(PLAN_ROUTE) {
            // Use the existing PlanScreen composable from MainActivity
            com.mtlc.studyplan.PlanScreen()
        }

        // Lightweight demo route to open reader with a sample passage
        composable("readerDemo") {
            ReaderScreen(
                passage = PassageUi(
                    id = "demo",
                    title = "Reading Demo",
                    body = List(50) { idx ->
                        "Paragraph ${'$'}idx: Reading is core to YDS prep. Control comfort, track time, and learn words in context."
                    }.joinToString("\n\n")
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Mock exam start route
        composable("mock/start") {
            MockExamRoute(onSubmit = { result ->
                navController.navigate("mock/result/${'$'}{result.correct}/${'$'}{result.total}/${'$'}{result.avgSecPerQ}")
            })
        }
        composable("mock/result/{correct}/{total}/{avg}") { backStackEntry ->
            val correct = backStackEntry.arguments?.getString("correct")?.toIntOrNull() ?: 0
            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
            val avg = backStackEntry.arguments?.getString("avg")?.toIntOrNull() ?: 0
            androidx.compose.material3.Scaffold(topBar = {
                androidx.compose.material3.TopAppBar(title = { androidx.compose.material3.Text("Exam Result") })
            }) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Text("Correct: ${'$'}correct / ${'$'}total")
                    androidx.compose.material3.Text("Avg sec per Q: ${'$'}avg")
                    androidx.compose.material3.Button(onClick = { navController.popBackStack(TODAY_ROUTE, inclusive = false) }) {
                        androidx.compose.material3.Text("Back to Today")
                    }
                }
            }
        }
    }
}
