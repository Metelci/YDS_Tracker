package com.mtlc.studyplan.feature.today

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val TODAY_ROUTE = "today"
const val PLAN_ROUTE = "plan"
const val LESSON_ROUTE = "lesson/{id}"
const val FOCUS_ROUTE = "focus/{id}"
fun lessonRoute(id: String) = "lesson/$id"
fun focusRoute(id: String) = "focus/$id"

fun NavGraphBuilder.todayGraph(navController: NavController) {
    composable(TODAY_ROUTE) {
        todayRoute(
            onNavigateToFocus = { id ->
                navController.navigate(focusRoute(id))
            }
        )
    }
    composable(LESSON_ROUTE) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id").orEmpty()
        LessonScreen(sessionId = id, onBack = { navController.popBackStack() })
    }
    composable(FOCUS_ROUTE) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id").orEmpty()
        com.mtlc.studyplan.feature.focus.FocusModeScreen(
            taskId = id,
            taskTitle = "Study Session", // Could be enhanced to pass actual task title
            onExit = { navController.popBackStack() }
        )
    }
}


