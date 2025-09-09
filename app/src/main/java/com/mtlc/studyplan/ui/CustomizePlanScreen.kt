package com.mtlc.studyplan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.UserPlanOverrides
import com.mtlc.studyplan.data.WeekPlan
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import com.mtlc.studyplan.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizePlanScreen(
    plan: List<WeekPlan>,
    overrides: UserPlanOverrides,
    startEpochDay: Long,
    onBack: () -> Unit,
    onToggleHidden: (taskId: String, hidden: Boolean) -> Unit,
    onRequestEdit: (taskId: String, currentDesc: String?, currentDetails: String?) -> Unit,
    onAddTask: (week: Int, dayIndex: Int) -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.customize_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = stringResource(id = R.string.info))
                    }
                    TextButton(onClick = onBack) { Text(text = stringResource(id = R.string.cancel)) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(plan) { _, week ->
                WeekCard(
                    week = week,
                    overrides = overrides,
                    startEpochDay = startEpochDay,
                    onToggleHidden = onToggleHidden,
                    onRequestEdit = onRequestEdit,
                    onAddTask = onAddTask
                )
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(text = stringResource(id = R.string.customize_info_title)) },
            text = { Text(text = stringResource(id = R.string.customize_info_message)) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun WeekCard(
    week: WeekPlan,
    overrides: UserPlanOverrides,
    startEpochDay: Long,
    onToggleHidden: (taskId: String, hidden: Boolean) -> Unit,
    onRequestEdit: (taskId: String, currentDesc: String?, currentDetails: String?) -> Unit,
    onAddTask: (week: Int, dayIndex: Int) -> Unit,
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(week.title, style = MaterialTheme.typography.titleMedium)
            val weekStart = remember(startEpochDay, week.week) { LocalDate.ofEpochDay(startEpochDay).plusDays(((week.week - 1) * 7L)) }
            val weekEnd = remember(weekStart) { weekStart.plusDays(6) }
            val fmtShort = remember { DateTimeFormatter.ofPattern("MMM d") }
            val fmtFull = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
            val weekRange = remember(weekStart, weekEnd) {
                if (weekStart.year == weekEnd.year) {
                    if (weekStart.month == weekEnd.month) "${weekStart.format(fmtShort)} – ${weekEnd.dayOfMonth}" else "${weekStart.format(fmtShort)} – ${weekEnd.format(fmtShort)}"
                } else "${weekStart.format(fmtFull)} – ${weekEnd.format(fmtFull)}"
            }
            Text(weekRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            week.days.forEachIndexed { dayIndex, day ->
                val dayDate = remember(weekStart, dayIndex) { weekStart.plusDays(dayIndex.toLong()) }
                val dayLabel = "${day.day} (${dayDate.format(fmtShort)})"
                Text(dayLabel, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                day.tasks.forEach { task ->
                    TaskRow(
                        task = task,
                        hidden = overrides.taskOverrides.firstOrNull { it.taskId == task.id }?.hidden == true,
                        onToggleHidden = { hidden -> onToggleHidden(task.id, hidden) },
                        onRequestEdit = { onRequestEdit(task.id, task.desc, task.details) }
                    )
                }
                HorizontalDivider()
                TextButton(onClick = { onAddTask(week.week, dayIndex) }) {
                    Text("Add Task")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    hidden: Boolean,
    onToggleHidden: (Boolean) -> Unit,
    onRequestEdit: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(task.desc, style = MaterialTheme.typography.bodyMedium)
            val details = task.details
            if (!details.isNullOrBlank()) {
                Text(details, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row {
            IconButton(onClick = { onToggleHidden(!hidden) }) {
                Icon(
                    imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                )
            }
            IconButton(onClick = onRequestEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null)
            }
        }
    }
}
