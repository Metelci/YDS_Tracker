@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.data.Task as DataTask
import com.mtlc.studyplan.data.DayPlan as DataDayPlan
import com.mtlc.studyplan.data.WeekPlan as DataWeekPlan
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val settingsStore = remember { PlanSettingsStore(appContext.dataStore) }
    val overridesStore = remember { PlanOverridesStore(appContext.dataStore) }
    val planRepo = remember { PlanRepository(overridesStore, settingsStore) }
    // Progress repository removed

    val plan by planRepo.planFlow.collectAsState(initial = emptyList())
    // Progress tracking removed - using empty state
    val progress = UserProgress()

    val today = remember { LocalDate.now() }
    val settings by settingsStore.settingsFlow.collectAsState(initial = PlanDurationSettings())

    val totalTasks = remember(plan) { plan.flatMap { it.days }.flatMap { it.tasks }.size }
    val completedInPlan = remember(plan, progress) {
        val ids = plan.flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()
        progress.completedTasks.count { it in ids }
    }
    val progressRatio = if (totalTasks > 0) completedInPlan.toFloat() / totalTasks else 0f

    val absIndex = remember(settings.startEpochDay, today) {
        val start = LocalDate.ofEpochDay(settings.startEpochDay)
        val diff = ChronoUnit.DAYS.between(start, today).toInt()
        diff.coerceAtLeast(0)
    }
    val todayTasks = remember(plan, absIndex) {
        var idx = absIndex
        for (week in plan) {
            if (idx < week.days.size) {
                return@remember week.week to week.days[idx].tasks
            } else idx -= week.days.size
        }
        null
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Home", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress ring for today's plan
            item {
                val tasks = todayTasks?.second.orEmpty()
                val plannedMinutes = remember(tasks) {
                    tasks.sumOf { com.mtlc.studyplan.ui.components.estimateTaskMinutes(it.desc, it.details) }
                }
                val completedMinutes = remember(tasks, progress.completedTasks) {
                    tasks.filter { it.id in progress.completedTasks }
                        .sumOf { com.mtlc.studyplan.ui.components.estimateTaskMinutes(it.desc, it.details) }
                }
                val ratio = remember(plannedMinutes, completedMinutes) {
                    if (plannedMinutes > 0) (completedMinutes.toFloat() / plannedMinutes).coerceIn(0f, 1f) else 0f
                }
                // Progress ring removed with progress functionality
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Today's Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tasks.size} tasks planned",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                // Exam countdown
                val nextExam = ExamCalendarDataSource.getNextExam()
                if (nextExam != null) {
                    val daysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Exam: ${nextExam.name}", style = MaterialTheme.typography.titleSmall)
                            Text("Date: ${nextExam.examDate}", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                            progress = { ((-daysToExam).coerceAtMost(0) / -1f).coerceIn(0f,1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(top = 8.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                            Text("Days left: ${daysToExam}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                // Streak and progress
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Streak: ${progress.streakCount} days")
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        Text("Overall: ${completedInPlan} / ${totalTasks}")
                    }
                }
            }
            item {
                Text("Today’s Tasks", style = MaterialTheme.typography.titleMedium)
            }
            if (todayTasks != null) {
                val (weekNum, tasks) = todayTasks
                items(tasks, key = { it.id }) { t ->
                    val isDone = progress.completedTasks.contains(t.id)
                    ListItem(
                        headlineContent = { Text(t.desc) },
                        supportingContent = { if (t.details != null) Text(t.details!!) },
                        leadingContent = {
                            Checkbox(checked = isDone, onCheckedChange = {
                                // Task completion tracking removed with progress functionality
                            })
                        }
                    )
                    HorizontalDivider()
                }
            } else {
                item { Text("No tasks scheduled for today.") }
            }
        }
    }
}
