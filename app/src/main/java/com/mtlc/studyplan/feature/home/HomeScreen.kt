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
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.mtlc.studyplan.utils.settingsDataStore
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit


@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val settingsStore = remember { PlanSettingsStore(appContext.settingsDataStore) }
    val overridesStore = remember { PlanOverridesStore(appContext.settingsDataStore) }
    val planRepo = remember { PlanRepository(overridesStore, settingsStore) }
    // Progress repository removed

    val plan by planRepo.planFlow.collectAsState(initial = emptyList())
    // TODO: Connect to real progress tracking when available
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
                        if (plannedMinutes > 0) {
                            Text(
                                text = "$completedMinutes / $plannedMinutes minutes completed",
                                style = MaterialTheme.typography.bodySmall
                            )
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(top = 8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
            item {
                // Real exam countdown using YdsExamService
                val nextExam = YdsExamService.getNextExam()
                if (nextExam != null) {
                    val daysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate)
                    val registrationStatus = YdsExamService.getRegistrationStatus()
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Next Exam: ${nextExam.name}", style = MaterialTheme.typography.titleSmall)
                            Text("Date: ${nextExam.examDate}", style = MaterialTheme.typography.bodySmall)
                            Text("Status: ${YdsExamService.getStatusMessage()}", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { if (daysToExam > 0) (100 - daysToExam.coerceAtMost(100)) / 100f else 1f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(top = 8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            Text("Days remaining: ${daysToExam}", style = MaterialTheme.typography.bodySmall)

                            // Show registration info if relevant
                            when (registrationStatus) {
                                YdsExamService.RegistrationStatus.OPEN -> {
                                    Text("Registration period: ${nextExam.registrationStart} - ${nextExam.registrationEnd}",
                                         style = MaterialTheme.typography.bodySmall,
                                         color = MaterialTheme.colorScheme.primary)
                                }
                                YdsExamService.RegistrationStatus.LATE_REGISTRATION -> {
                                    Text("Late registration until: ${nextExam.lateRegistrationEnd}",
                                         style = MaterialTheme.typography.bodySmall,
                                         color = MaterialTheme.colorScheme.error)
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
            item {
                // Study statistics - only show if there's real data
                if (totalTasks > 0) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Study Progress", style = MaterialTheme.typography.titleSmall)
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            Text("Overall: ${completedInPlan} / ${totalTasks} tasks completed")
                            // Only show streak if it's greater than 0
                            if (progress.streakCount > 0) {
                                Text("Current streak: ${progress.streakCount} days")
                            }
                        }
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
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = null // Disabled until progress tracking is connected
                            )
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
