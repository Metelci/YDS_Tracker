package com.mtlc.studyplan.feature.today

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val TODAY_ROUTE = "today"

fun NavGraphBuilder.todayGraph() {
    composable(TODAY_ROUTE) { TodayRoute() }
}
