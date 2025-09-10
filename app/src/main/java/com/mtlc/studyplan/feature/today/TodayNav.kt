package com.mtlc.studyplan.feature.today

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val TODAY_ROUTE = "today"
const val PLAN_ROUTE = "plan"
const val LESSON_ROUTE = "lesson/{id}"
fun lessonRoute(id: String) = "lesson/$id"

fun NavGraphBuilder.todayGraph(navController: NavController) {
    composable(TODAY_ROUTE) {
        TodayRoute(
            onNavigateToPlan = {
                navController.navigate(PLAN_ROUTE)
            },
            onNavigateToLesson = { id ->
                navController.navigate(lessonRoute(id))
            }
        )
    }
    composable(LESSON_ROUTE) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id").orEmpty()
        LessonScreen(sessionId = id, onBack = { navController.popBackStack() })
    }
}
