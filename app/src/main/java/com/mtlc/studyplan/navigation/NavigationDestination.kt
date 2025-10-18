package com.mtlc.studyplan.navigation

sealed class NavigationDestination {
    data class Custom(val route: String, val params: Map<String, Any>) : NavigationDestination()
    data class Back(val destination: String) : NavigationDestination()

    // Specific destinations with typed parameters
    data class Tasks(val filter: String? = null, val highlightId: String? = null) : NavigationDestination()
    data class TaskDetail(val taskId: String) : NavigationDestination()
    data class Progress(val timeRange: String? = null, val highlight: String? = null) : NavigationDestination()
}
