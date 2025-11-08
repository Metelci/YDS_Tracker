package com.mtlc.studyplan.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.PlanOverridesStore
import com.mtlc.studyplan.data.PlanRepository
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.data.PlanTask
import com.mtlc.studyplan.data.PlanTaskLocalizer
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import com.mtlc.studyplan.ui.components.StudyPlanTopBarStyle
import com.mtlc.studyplan.utils.settingsDataStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPlanScreen(
    weekIndex: Int,
    dayIndex: Int,
    onNavigateBack: () -> Unit,
    sharedViewModel: SharedAppViewModel? = null,
    taskRepository: TaskRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsStore = remember { PlanSettingsStore(context.settingsDataStore) }
    val overridesStore = remember { PlanOverridesStore(context.settingsDataStore) }
    val planRepository = remember(context) { PlanRepository(context, overridesStore, settingsStore) }
    val resolvedTaskRepository = taskRepository ?: get()

    val planFlow: Flow<List<com.mtlc.studyplan.data.WeekPlan>> =
        sharedViewModel?.planFlow ?: planRepository.planFlow
    val plan by planFlow.collectAsState(initial = emptyList())
    val settings by settingsStore.settingsFlow.collectAsState(initial = com.mtlc.studyplan.data.PlanDurationSettings())
    val allTasks by resolvedTaskRepository.getAllTasks().collectAsState(initial = emptyList())
    val completedTaskIds = remember(allTasks) {
        allTasks.filter { it.isCompleted }.map { it.id }.toSet()
    }

    val week = plan.getOrNull(weekIndex)
    val dayPlan = week?.days?.getOrNull(dayIndex)

    val startDate = remember(settings.startEpochDay) { LocalDate.ofEpochDay(settings.startEpochDay) }
    val scheduledDate = startDate.plusDays((weekIndex * 7L) + dayIndex)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val localizedDayName = dayPlan?.day?.let { PlanTaskLocalizer.localizeDayName(it, context) }
        ?: "Study Plan"

    val gradientBrush = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFF7FBFF)))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                StudyPlanTopBar(
                    title = localizedDayName,
                    subtitle = "Week ${week?.week ?: weekIndex + 1}, Day ${dayIndex + 1}",
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onNavigateBack,
                    style = StudyPlanTopBarStyle.Home
                )
            }
        ) { padding ->
            if (dayPlan == null) {
                EmptyDailyPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                )
            } else {
                val totalTasks = dayPlan.tasks.size
                val completedTasks = dayPlan.tasks.count { completedTaskIds.contains(it.id) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DailySummaryCard(
                            week = week,
                            dayName = localizedDayName,
                            scheduledDate = scheduledDate.format(dateFormatter),
                            totalTasks = totalTasks,
                            completedTasks = completedTasks
                        )
                    }
                    if (dayPlan.tasks.isEmpty()) {
                        item {
                            EmptyDailyTasks()
                        }
                    } else {
                        itemsIndexed(dayPlan.tasks) { index, task ->
                            val isCompleted = completedTaskIds.contains(task.id)
                            DailyTaskCard(
                                position = index + 1,
                                task = task,
                                isCompleted = isCompleted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(
    week: com.mtlc.studyplan.data.WeekPlan?,
    dayName: String,
    scheduledDate: String,
    totalTasks: Int,
    completedTasks: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$dayName – $scheduledDate",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = week?.title ?: "Personalized Raymond Murphy plan",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$completedTasks of $totalTasks tasks completed",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DailyTaskCard(
    position: Int,
    task: PlanTask,
    isCompleted: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCompleted) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(6.dp)
                            .height(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task $position",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = task.desc,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            task.details?.takeIf { it.isNotBlank() }?.let { detail ->
                Text(
                    text = detail,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reference: ${task.id}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmptyDailyPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "No study plan available for this day",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Generate your personalized schedule from the onboarding planner to unlock daily Raymond Murphy units and exercises.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyDailyTasks() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Rest day scheduled",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "There are no Raymond Murphy tasks assigned today. Use this time to review previous units or rest.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

