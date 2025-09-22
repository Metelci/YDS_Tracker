package com.mtlc.studyplan.studyplan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanOverviewScreen(
    appIntegrationManager: AppIntegrationManager,
    onNavigateBack: () -> Unit = {},
    initialTab: StudyPlanTab = StudyPlanTab.WEEKLY
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) }

    // Collect data from AppIntegrationManager
    val allTasks by appIntegrationManager.getAllTasks().collectAsState(initial = emptyList())
    val taskStats by appIntegrationManager.getTaskStatsFlow().collectAsState(
        initial = TaskStats(0, 0, 0, 0)
    )

    // Sample data for comprehensive view
    val weeklyPlan = remember { WeeklyStudyPlan() }
    val studySchedule = remember { createStudyScheduleData() }

    Scaffold(
        topBar = {
            StudyPlanTopBar(
                title = "Study Plan Overview",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                showLanguageSwitcher = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selector
            StudyPlanTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content based on selected tab
            when (selectedTab) {
                StudyPlanTab.WEEKLY -> WeeklyScheduleView(
                    weeklyPlan = weeklyPlan,
                    studySchedule = studySchedule,
                    taskStats = taskStats,
                    modifier = Modifier.weight(1f)
                )
                StudyPlanTab.PROGRESS -> ProgressOverview(
                    taskStats = taskStats,
                    weeklyPlan = weeklyPlan,
                    modifier = Modifier.weight(1f)
                )
                StudyPlanTab.UPCOMING -> UpcomingTasksView(
                    allTasks = allTasks,
                    studySchedule = studySchedule,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StudyPlanTabRow(
    selectedTab: StudyPlanTab,
    onTabSelected: (StudyPlanTab) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(StudyPlanTab.values()) { tab ->
            StudyPlanTabChip(
                tab = tab,
                isSelected = selectedTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun StudyPlanTabChip(
    tab: StudyPlanTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1976D2) else Color(0xFFF5F5F5),
        animationSpec = tween(200),
        label = "tab_background"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF666666),
        animationSpec = tween(200),
        label = "tab_content"
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = tab.title,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WeeklyScheduleView(
    weeklyPlan: WeeklyStudyPlan,
    studySchedule: StudyScheduleData,
    taskStats: TaskStats,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Current Week Overview
        item {
            CurrentWeekCard(
                weeklyPlan = weeklyPlan,
                taskStats = taskStats
            )
        }

        // Daily Schedule Cards
        item {
            Text(
                text = "This Week's Schedule",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(studySchedule.dailySchedules) { dailySchedule ->
            DailyScheduleCard(
                dailySchedule = dailySchedule,
                onClick = { /* Navigate to daily detail */ }
            )
        }

        // Study Goals
        item {
            StudyGoalsCard(studySchedule.weeklyGoals)
        }
    }
}

@Composable
private fun ProgressOverview(
    taskStats: TaskStats,
    weeklyPlan: WeeklyStudyPlan,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Overall Progress Card
        item {
            OverallProgressCard(taskStats = taskStats)
        }

        // Subject Progress
        item {
            SubjectProgressCard()
        }

        // Weekly Progress Chart
        item {
            WeeklyProgressChart(weeklyPlan = weeklyPlan)
        }

        // Achievement Summary
        item {
            AchievementSummaryCard()
        }
    }
}

@Composable
private fun UpcomingTasksView(
    allTasks: List<Task>,
    studySchedule: StudyScheduleData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Today's Tasks
        item {
            TaskSectionHeader("Today's Tasks")
        }

        items(studySchedule.todayTasks) { task ->
            UpcomingTaskCard(task = task)
        }

        // Tomorrow's Tasks
        item {
            TaskSectionHeader("Tomorrow's Tasks")
        }

        items(studySchedule.tomorrowTasks) { task ->
            UpcomingTaskCard(task = task)
        }

        // This Week's Targets
        item {
            TaskSectionHeader("Week Targets")
        }

        items(studySchedule.weekTargets) { target ->
            WeekTargetCard(target = target)
        }
    }
}

@Composable
private fun CurrentWeekCard(
    weeklyPlan: WeeklyStudyPlan,
    taskStats: TaskStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Week Progress",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = "Week ${weeklyPlan.currentWeek} of ${weeklyPlan.totalWeeks}",
                        fontSize = 14.sp,
                        color = Color(0xFF1976D2).copy(alpha = 0.7f)
                    )
                }

                CircularProgressIndicator(
                    progress = { taskStats.getProgressPercentage() / 100f },
                    modifier = Modifier.size(60.dp),
                    color = Color(0xFF1976D2),
                    strokeWidth = 6.dp,
                    trackColor = Color(0xFF1976D2).copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProgressMetric("Completed", "${taskStats.completedTasks}")
                ProgressMetric("Remaining", "${taskStats.totalTasks - taskStats.completedTasks}")
                ProgressMetric("Progress", "${taskStats.getProgressPercentage()}%")
            }
        }
    }
}

@Composable
private fun ProgressMetric(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF1976D2).copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DailyScheduleCard(
    dailySchedule: DailySchedule,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dailySchedule.dayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${dailySchedule.tasks.size} tasks • ${dailySchedule.estimatedTime}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { dailySchedule.completionPercentage / 100f },
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (dailySchedule.completionPercentage >= 100) Color(0xFF4CAF50) else Color(0xFF2196F3)
                )
                Text(
                    text = "${dailySchedule.completionPercentage}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Data classes and helper functions
enum class StudyPlanTab(val title: String, val icon: ImageVector) {
    WEEKLY("Weekly", Icons.Filled.CalendarToday),
    PROGRESS("Progress", Icons.AutoMirrored.Filled.TrendingUp),
    UPCOMING("Upcoming", Icons.AutoMirrored.Filled.Assignment)
}

data class StudyScheduleData(
    val dailySchedules: List<DailySchedule>,
    val todayTasks: List<UpcomingTask>,
    val tomorrowTasks: List<UpcomingTask>,
    val weekTargets: List<WeekTarget>,
    val weeklyGoals: List<StudyGoal>
)

data class DailySchedule(
    val dayName: String,
    val date: String,
    val tasks: List<String>,
    val estimatedTime: String,
    val completionPercentage: Int,
    val isToday: Boolean = false
)

data class UpcomingTask(
    val title: String,
    val subject: String,
    val dueTime: String,
    val priority: Priority,
    val estimatedDuration: String
)

data class WeekTarget(
    val title: String,
    val progress: Int,
    val target: Int,
    val category: String
)

data class StudyGoal(
    val title: String,
    val description: String,
    val progress: Int,
    val isCompleted: Boolean
)

enum class Priority { HIGH, MEDIUM, LOW }

// Helper functions
private fun createStudyScheduleData(): StudyScheduleData {
    val dailySchedules = listOf(
        DailySchedule("Monday", "Dec 16", listOf("Grammar", "Reading"), "2h 30m", 100, false),
        DailySchedule("Tuesday", "Dec 17", listOf("Vocabulary", "Listening"), "2h", 80, false),
        DailySchedule("Wednesday", "Dec 18", listOf("Reading", "Writing"), "3h", 60, true),
        DailySchedule("Thursday", "Dec 19", listOf("Grammar", "Practice"), "2h 15m", 0, false),
        DailySchedule("Friday", "Dec 20", listOf("Mock Test", "Review"), "3h 30m", 0, false),
        DailySchedule("Saturday", "Dec 21", listOf("Vocabulary Review"), "1h 30m", 0, false),
        DailySchedule("Sunday", "Dec 22", listOf("Free Study"), "2h", 0, false)
    )

    val todayTasks = listOf(
        UpcomingTask("Complete Reading Exercise 15", "Reading", "10:00 AM", Priority.HIGH, "45 min"),
        UpcomingTask("Grammar: Conditionals", "Grammar", "2:00 PM", Priority.MEDIUM, "30 min"),
        UpcomingTask("Vocabulary Review Session", "Vocabulary", "4:00 PM", Priority.LOW, "20 min")
    )

    val tomorrowTasks = listOf(
        UpcomingTask("Practice Test Section A", "Grammar", "9:00 AM", Priority.HIGH, "60 min"),
        UpcomingTask("Reading Comprehension", "Reading", "11:00 AM", Priority.MEDIUM, "40 min")
    )

    val weekTargets = listOf(
        WeekTarget("Reading Exercises", 8, 12, "Reading"),
        WeekTarget("Grammar Units", 5, 8, "Grammar"),
        WeekTarget("Vocabulary Words", 150, 200, "Vocabulary")
    )

    val weeklyGoals = listOf(
        StudyGoal("Complete Week 2 Curriculum", "Finish all assigned reading and grammar exercises", 75, false),
        StudyGoal("Vocabulary Milestone", "Learn 200 new words this week", 150, false),
        StudyGoal("Practice Test", "Complete one full practice test", 0, false)
    )

    return StudyScheduleData(dailySchedules, todayTasks, tomorrowTasks, weekTargets, weeklyGoals)
}

// Additional composables for other sections...
@Composable
private fun TaskSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun UpcomingTaskCard(task: UpcomingTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.subject} • ${task.estimatedDuration}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = task.dueTime,
                fontSize = 12.sp,
                color = when (task.priority) {
                    Priority.HIGH -> Color(0xFFE53935)
                    Priority.MEDIUM -> Color(0xFFFF9800)
                    Priority.LOW -> Color(0xFF4CAF50)
                }
            )
        }
    }
}

@Composable
private fun WeekTargetCard(target: WeekTarget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = target.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${target.progress}/${target.target}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { target.progress.toFloat() / target.target },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun StudyGoalsCard(goals: List<StudyGoal>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Goals",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            goals.forEach { goal ->
                GoalItem(goal = goal)
                if (goal != goals.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GoalItem(goal: StudyGoal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (goal.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (goal.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = goal.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Placeholder composables for other views
@Composable
private fun OverallProgressCard(taskStats: TaskStats) {
    // Implementation for overall progress
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        // Progress content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Overall Progress: ${taskStats.getProgressPercentage()}%")
        }
    }
}

@Composable
private fun SubjectProgressCard() {
    // Implementation for subject progress
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Subject Progress Charts")
        }
    }
}

@Composable
private fun WeeklyProgressChart(weeklyPlan: WeeklyStudyPlan) {
    // Implementation for weekly progress chart
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Weekly Progress Chart")
        }
    }
}

@Composable
private fun AchievementSummaryCard() {
    // Implementation for achievement summary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Achievement Summary")
        }
    }
}