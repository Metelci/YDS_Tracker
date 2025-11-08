package com.mtlc.studyplan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class NavTab(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun AppBottomBar(
    currentRoute: String,
    tabs: List<NavTab>,
    onTabSelected: (String) -> Unit
) {
    val mappedTabs = tabs.map { Triple(it.route, it.icon, it.label) }
    com.mtlc.studyplan.ui.components.StudyBottomNav(
        currentRoute = currentRoute,
        tabs = mappedTabs,
        onTabSelected = onTabSelected
    )
}
