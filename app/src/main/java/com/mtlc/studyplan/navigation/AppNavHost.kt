package com.mtlc.studyplan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mtlc.studyplan.feature.today.TODAY_ROUTE
import com.mtlc.studyplan.feature.today.todayGraph

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TODAY_ROUTE
    ) {
        todayGraph()  // adds the Today screen route
    }
}