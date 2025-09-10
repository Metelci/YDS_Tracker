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
    }
}
