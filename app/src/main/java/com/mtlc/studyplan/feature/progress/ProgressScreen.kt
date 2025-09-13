package com.mtlc.studyplan.feature.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.components.TwoPaneScaffold
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.analytics.AnalyticsScreen

@Composable
fun ProgressScreen() {
    val s = LocalSpacing.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Analytics")
    val context = androidx.compose.ui.platform.LocalContext.current
    val ds = (context.applicationContext as android.content.Context).dataStore
    val repo = remember { ProgressRepository(ds) }
    val userProgress by repo.userProgressFlow.collectAsState(initial = UserProgress())

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab selector
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    icon = if (index == 1) {
                        { Icon(Icons.Filled.Analytics, contentDescription = "Analytics") }
                    } else null
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> {
                // Original progress overview
                TwoPaneScaffold(
                    list = {
                        Text("Progress Overview", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(s.xs))
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            val items = listOf("Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Week 7")
                            itemsIndexed(items) { idx, label ->
                                ListItem(
                                    headlineContent = { Text(label) },
                                    supportingContent = { Text("Summary metrics…") },
                                    trailingContent = {
                                        if (idx < userProgress.completedTasks.size) {
                                            AssistChip(onClick = {}, label = { Text("Complete") })
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    },
                    detail = {
                        Text("Progress Details", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(s.xs))
                        Text("Completed tasks: ${userProgress.completedTasks.size}")
                        Text("Current streak: ${userProgress.streakCount} days")
                        Text("Achievement level: ${userProgress.achievementIds.size} unlocked")
                        Spacer(Modifier.height(s.sm))

                        val progressPercent = userProgress.completedTasks.size / 100f
                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Overall completion: ${(progressPercent * 100).toInt()}%")
                    }
                )
            }
            1 -> {
                // Analytics dashboard
                AnalyticsScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Preview(widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun ProgressLargePreview() { ProgressScreen() }

@Preview(widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun ProgressSmallPreview() { ProgressScreen() }
