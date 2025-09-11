@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShowChart
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mtlc.studyplan.feature.today.PLAN_ROUTE
import com.mtlc.studyplan.feature.today.TODAY_ROUTE
import com.mtlc.studyplan.feature.today.todayGraph
import com.mtlc.studyplan.feature.reader.PassageUi
import com.mtlc.studyplan.feature.reader.ReaderScreen
import com.mtlc.studyplan.feature.mock.MockExamRoute
import com.mtlc.studyplan.feature.review.MockResultUi
import com.mtlc.studyplan.feature.review.ReviewScreen
import com.mtlc.studyplan.feature.practice.PracticeScreen
import com.mtlc.studyplan.feature.progress.ProgressScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val tabs = listOf(
        Triple(TODAY_ROUTE, Icons.Filled.Today, "Today"),
        Triple("practice", Icons.Filled.School, "Practice"),
        Triple("progress", Icons.Filled.ShowChart, "Progress"),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val route = navController.currentBackStackEntry?.destination?.route ?: TODAY_ROUTE
                tabs.forEach { (r, icon, label) ->
                    NavigationBarItem(
                        selected = route.startsWith(r),
                        onClick = {
                            navController.navigate(r) {
                                popUpTo(TODAY_ROUTE) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TODAY_ROUTE,
            modifier = androidx.compose.ui.Modifier.padding(padding)
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

        // Large-screen two-pane demos
        composable("practice") { PracticeScreen() }
        composable("progress") { ProgressScreen() }
        // Mock exam start route
        composable("mock/start") {
            MockExamRoute(onSubmit = { result ->
                val json = java.net.URLEncoder.encode(kotlinx.serialization.json.Json.encodeToString(com.mtlc.studyplan.feature.mock.MockResult.serializer(), result), "UTF-8")
                navController.navigate("mock/result/${'$'}json")
            })
        }
        composable("mock/result/{data}") { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("data").orEmpty()
            val json = java.net.URLDecoder.decode(encoded, "UTF-8")
            val mock = kotlinx.serialization.json.Json.decodeFromString<com.mtlc.studyplan.feature.mock.MockResult>(json)
            val review = MockResultUi(
                correct = mock.correct,
                total = mock.total,
                avgSecPerQ = mock.avgSecPerQ,
                perSection = mock.perSection.map { (sec, pair) -> com.mtlc.studyplan.feature.review.SectionStatUi(sec, pair.first, pair.second, if (mock.total>0) mock.avgSecPerQ else 0) },
                wrongIds = mock.wrongIds
            )
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
                    androidx.compose.material3.Text("Correct: ${'$'}{review.correct} / ${'$'}{review.total}")
                    androidx.compose.material3.Text("Avg sec per Q: ${'$'}{review.avgSecPerQ}")
                    androidx.compose.material3.Button(onClick = {
                        val data = java.net.URLEncoder.encode(kotlinx.serialization.json.Json.encodeToString(MockResultUi.serializer(), review), "UTF-8")
                        navController.navigate("mock/review/${'$'}data")
                    }) { androidx.compose.material3.Text("Open Insights") }
                    androidx.compose.material3.Button(onClick = { navController.popBackStack(TODAY_ROUTE, inclusive = false) }) {
                        androidx.compose.material3.Text("Back to Today")
                    }
                }
            }
        }
        composable("mock/review/{data}") { backStackEntry ->
            val enc = backStackEntry.arguments?.getString("data").orEmpty()
            val json2 = java.net.URLDecoder.decode(enc, "UTF-8")
            val ui = kotlinx.serialization.json.Json.decodeFromString<MockResultUi>(json2)
            ReviewScreen(result = ui, onRetrySet = { ids -> navController.navigate("mock/start") }, onBack = { navController.popBackStack() })
        }
        }
    }
}
