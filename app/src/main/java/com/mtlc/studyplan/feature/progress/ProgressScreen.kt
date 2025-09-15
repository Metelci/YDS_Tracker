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
import com.mtlc.studyplan.ui.components.*
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.data.StreakManager
import com.mtlc.studyplan.data.rememberStreakManager
import com.mtlc.studyplan.data.rememberStreakState
import com.mtlc.studyplan.analytics.AnalyticsScreen
import com.mtlc.studyplan.ui.components.StudyHeatmap
import com.mtlc.studyplan.progress.progressByDay
import java.time.LocalDate

@Composable
fun ProgressScreen() {
    val s = LocalSpacing.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Analytics")
    val context = androidx.compose.ui.platform.LocalContext.current
    val ds = (context.applicationContext as android.content.Context).dataStore
    val repo = remember { ProgressRepository(ds) }
    val streakManager = rememberStreakManager(ds)
    val userProgress by repo.userProgressFlow.collectAsState(initial = null)
    val logs by repo.taskLogsFlow.collectAsState(initial = null)
    val streakState by rememberStreakState(streakManager)
    val since = remember { LocalDate.now().minusDays(83) }
    val entries = remember(logs) { logs?.let { progressByDay(it, since) } }

    // Loading states
    val isLoadingProgress = userProgress == null
    val isLoadingLogs = logs == null

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
                if (isLoadingProgress || isLoadingLogs) {
                    // Show skeleton screens while loading
                    TwoPaneScaffold(
                        list = {
                            // Heatmap skeleton
                            HeatmapSkeleton(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(s.sm))
                            ShimmerText(width = 0.4f, height = 24f) // Progress Overview title
                            Spacer(Modifier.height(s.xs))
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(s.xs)
                            ) {
                                items(12) { idx ->
                                    ShimmerCard(
                                        modifier = Modifier.height(72.dp)
                                    )
                                }
                            }
                        },
                        detail = {
                            ShimmerText(width = 0.5f, height = 24f) // Progress Details title
                            Spacer(Modifier.height(s.xs))
                            ShimmerText(width = 0.7f, height = 16f) // Completed tasks
                            Spacer(Modifier.height(4.dp))
                            ShimmerText(width = 0.6f, height = 16f) // Current streak
                            Spacer(Modifier.height(4.dp))
                            ShimmerText(width = 0.8f, height = 16f) // Achievement level
                            Spacer(Modifier.height(s.sm))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .shimmer()
                            )
                            Spacer(Modifier.height(8.dp))
                            ShimmerText(width = 0.5f, height = 16f) // Overall completion
                        }
                    )
                } else {
                    // Original progress overview with data
                    TwoPaneScaffold(
                        list = {
                            // Heatmap for last 84 days
                            entries?.let { entryData ->
                                StudyHeatmap(entries = entryData, onDayClick = { date ->
                                    android.widget.Toast.makeText(context, "Open ${date}", android.widget.Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height(s.sm))
                            Text("Progress Overview", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(s.xs))
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(s.xs)
                            ) {
                                val items = listOf("Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Week 7", "Week 8", "Week 9", "Week 10", "Week 11", "Week 12")
                                itemsIndexed(items) { idx, label ->
                                    ListItem(
                                        headlineContent = { Text(label) },
                                        supportingContent = { Text("Summary metrics…") },
                                        trailingContent = {
                                            userProgress?.let { progress ->
                                                if (idx < progress.completedTasks.size) {
                                                    AssistChip(onClick = {}, label = { Text("Complete") })
                                                }
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        },
                        detail = {
                            userProgress?.let { progress ->
                                Text("Progress Details", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(s.xs))

                                // Enhanced streak counter
                                streakState?.let { state ->
                                    EnhancedStreakCounter(
                                        streakState = state,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(s.sm))

                                    // Streak danger warning if applicable
                                    if (state.isInDanger) {
                                        StreakDangerWarning(
                                            streakState = state,
                                            onTakeAction = { /* Navigate to tasks */ },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(s.sm))
                                    }
                                }

                                Text("Completed tasks: ${progress.completedTasks.size}")
                                Text("Total points earned: ${progress.totalPoints}")
                                Text("Achievement level: ${progress.unlockedAchievements.size} unlocked")
                                Spacer(Modifier.height(s.sm))

                                val progressPercent = progress.completedTasks.size / 100f
                                LinearProgressIndicator(
                                    progress = { progressPercent },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Overall completion: ${(progressPercent * 100).toInt()}%")
                            }
                        }
                    )
                }
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
