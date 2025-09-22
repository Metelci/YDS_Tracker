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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
    var selectedDay by remember { mutableStateOf<DailyStudyInfo?>(null) }

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
                showLanguageSwitcher = false
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
                    onDayClick = { dayInfo ->
                        selectedDay = dayInfo
                        selectedTab = StudyPlanTab.DAILY
                    },
                    modifier = Modifier.weight(1f)
                )
                StudyPlanTab.DAILY -> DailyScheduleView(
                    selectedDay = selectedDay,
                    onBackToWeekly = { selectedTab = StudyPlanTab.WEEKLY },
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
    onDayClick: (DailyStudyInfo) -> Unit = {},
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
                onClick = {
                    onDayClick(createDailyStudyInfo(dailySchedule))
                }
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
    DAILY("Daily", Icons.Filled.DateRange),
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

// Helper function to create DailyStudyInfo from DailySchedule
private fun createDailyStudyInfo(dailySchedule: DailySchedule): DailyStudyInfo {
    val dayIndex = when (dailySchedule.dayName.lowercase()) {
        "monday" -> 0
        "tuesday" -> 1
        "wednesday" -> 2
        "thursday" -> 3
        "friday" -> 4
        "saturday" -> 5
        "sunday" -> 6
        else -> 0
    }

    // Assign books based on day of week
    val book = when (dayIndex % 3) {
        0 -> StudyBook.RED_BOOK
        1 -> StudyBook.BLUE_BOOK
        else -> StudyBook.GREEN_BOOK
    }

    // Create sample units for the day
    val units = listOf(
        StudyUnit(
            title = "Present Tense Structures",
            unitNumber = 10 + dayIndex,
            pages = "${25 + dayIndex * 3}-${28 + dayIndex * 3}",
            exercises = listOf("10.1", "10.2", "10.3"),
            isCompleted = dailySchedule.completionPercentage > 50
        ),
        StudyUnit(
            title = "Question Formation",
            unitNumber = 11 + dayIndex,
            pages = "${29 + dayIndex * 3}-${32 + dayIndex * 3}",
            exercises = listOf("11.1", "11.2"),
            isCompleted = dailySchedule.completionPercentage >= 100
        )
    )

    // Create sample tasks
    val tasks = listOf(
        DailyTask(
            title = "Complete Grammar Exercises",
            description = "Work through the exercises in today's units",
            estimatedDuration = "45 minutes",
            priority = Priority.HIGH,
            isCompleted = dailySchedule.completionPercentage >= 100
        ),
        DailyTask(
            title = "Review Vocabulary",
            description = "Study 20 new vocabulary words",
            estimatedDuration = "30 minutes",
            priority = Priority.MEDIUM,
            isCompleted = dailySchedule.completionPercentage > 75
        ),
        DailyTask(
            title = "Practice Listening",
            description = "Complete listening comprehension exercise",
            estimatedDuration = "20 minutes",
            priority = Priority.LOW,
            isCompleted = dailySchedule.completionPercentage >= 100
        )
    ).take(if (dailySchedule.tasks.isNotEmpty()) dailySchedule.tasks.size else 2)

    // Create sample materials
    val materials = listOf(
        StudyMaterial(
            title = "Grammar Video Lesson",
            type = MaterialType.VIDEO,
            description = "Interactive video explaining today's grammar concepts"
        ),
        StudyMaterial(
            title = "Pronunciation Audio",
            type = MaterialType.AUDIO,
            description = "Audio examples for correct pronunciation"
        ),
        StudyMaterial(
            title = "Practice Worksheet",
            type = MaterialType.PDF,
            description = "Additional practice exercises"
        ),
        StudyMaterial(
            title = "Interactive Quiz",
            type = MaterialType.QUIZ,
            description = "Test your understanding of today's lesson"
        )
    ).take(2 + dayIndex % 3) // Vary the number of materials

    // Create notes based on the day
    val notes = when (dayIndex) {
        0 -> "Focus on understanding the fundamental concepts. Take your time with the exercises."
        1 -> "Pay special attention to irregular verbs. Practice pronunciation with the audio materials."
        2 -> "This unit builds on previous lessons. Review Unit 9 if needed."
        3 -> "Challenge day! Try to complete all exercises without looking at answers first."
        4 -> "Review week: Good time to consolidate what you've learned this week."
        5 -> "Weekend practice: Take a more relaxed approach and focus on areas you found difficult."
        6 -> "Preparation day: Review the upcoming week's material and plan your study schedule."
        else -> ""
    }

    return DailyStudyInfo(
        weekTitle = "Week ${(dayIndex / 7) + 1} - ${book.description}",
        dayName = dailySchedule.dayName,
        date = dailySchedule.date,
        book = book,
        units = units,
        tasks = tasks,
        materials = materials,
        estimatedTime = dailySchedule.estimatedTime,
        completionPercentage = dailySchedule.completionPercentage,
        notes = notes
    )
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

// Daily Study Info data class
data class DailyStudyInfo(
    val weekTitle: String,
    val dayName: String,
    val date: String,
    val book: StudyBook,
    val units: List<StudyUnit>,
    val tasks: List<DailyTask>,
    val materials: List<StudyMaterial>,
    val estimatedTime: String,
    val completionPercentage: Int,
    val notes: String = ""
)

data class StudyBook(
    val name: String,
    val color: Color,
    val description: String
) {
    companion object {
        val RED_BOOK = StudyBook("Red Book - Essential Grammar in Use", Color(0xFFE53935), "Foundation Level Grammar")
        val BLUE_BOOK = StudyBook("Blue Book - English Grammar in Use", Color(0xFF1976D2), "Intermediate Level Grammar")
        val GREEN_BOOK = StudyBook("Green Book - Advanced Grammar in Use", Color(0xFF388E3C), "Advanced Level Grammar")
    }
}

data class StudyUnit(
    val title: String,
    val unitNumber: Int,
    val pages: String,
    val exercises: List<String>,
    val isCompleted: Boolean = false
)

data class DailyTask(
    val title: String,
    val description: String,
    val estimatedDuration: String,
    val priority: Priority,
    val isCompleted: Boolean = false
)

data class StudyMaterial(
    val title: String,
    val type: MaterialType,
    val url: String = "",
    val description: String = ""
)

enum class MaterialType {
    VIDEO, AUDIO, PDF, EXERCISE, QUIZ, READING
}

@Composable
private fun DailyScheduleView(
    selectedDay: DailyStudyInfo?,
    onBackToWeekly: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDay) {
        if (selectedDay != null) {
            isLoading = true
            // Simulate loading delay
            kotlinx.coroutines.delay(800)
            isLoading = false
        }
    }

    if (selectedDay == null) {
        // No day selected - show placeholder
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select a Day from the Weekly Plan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Click on any day in the Weekly tab to view detailed study materials and tasks",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Button(
                    onClick = onBackToWeekly,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Go to Weekly Plan")
                }
            }
        }
        return
    }

    if (isLoading) {
        // Loading state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading ${selectedDay.dayName} Study Plan...",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Daily content
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header with day info
        item {
            DailyStudyHeader(
                dayInfo = selectedDay,
                onBackToWeekly = onBackToWeekly
            )
        }

        // Study Book Section
        item {
            StudyBookCard(selectedDay.book, selectedDay.units)
        }

        // Daily Tasks Section
        item {
            DailyTasksSection(selectedDay.tasks)
        }

        // Study Materials Section
        item {
            StudyMaterialsSection(selectedDay.materials)
        }

        // Notes Section
        if (selectedDay.notes.isNotEmpty()) {
            item {
                NotesSection(selectedDay.notes)
            }
        }
    }
}

@Composable
private fun DailyStudyHeader(
    dayInfo: DailyStudyInfo,
    onBackToWeekly: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = dayInfo.book.color.copy(alpha = 0.1f)),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayInfo.dayName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = dayInfo.book.color
                    )
                    Text(
                        text = dayInfo.date,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dayInfo.weekTitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                TextButton(onClick = onBackToWeekly) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Weekly")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated Time: ${dayInfo.estimatedTime}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = when {
                        dayInfo.completionPercentage >= 100 -> Color(0xFF4CAF50)
                        dayInfo.completionPercentage > 0 -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${dayInfo.completionPercentage}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyBookCard(book: StudyBook, units: List<StudyUnit>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = book.color,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = book.name.first().toString(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = book.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (units.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Units for Today",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                units.forEach { unit ->
                    StudyUnitItem(unit)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyUnitItem(unit: StudyUnit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (unit.isCompleted) Color(0xFFE8F5E8) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (unit.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = null,
            tint = if (unit.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Unit ${unit.unitNumber}: ${unit.title}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pages ${unit.pages}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (unit.exercises.isNotEmpty()) {
                Text(
                    text = "Exercises: ${unit.exercises.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyTasksSection(tasks: List<DailyTask>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily Tasks",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = "No specific tasks for today. Focus on your study units!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                tasks.forEach { task ->
                    DailyTaskItem(task)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyTaskItem(task: DailyTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (task.isCompleted) Color(0xFFE8F5E8) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = null,
            tint = if (task.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = task.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Duration: ${task.estimatedDuration}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = when (task.priority) {
                Priority.HIGH -> Color(0xFFE53935)
                Priority.MEDIUM -> Color(0xFFFF9800)
                Priority.LOW -> Color(0xFF4CAF50)
            },
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = task.priority.name,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun StudyMaterialsSection(materials: List<StudyMaterial>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Study Materials",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (materials.isEmpty()) {
                Text(
                    text = "No additional materials for today. Focus on your textbook units!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                materials.forEach { material ->
                    StudyMaterialItem(material)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyMaterialItem(material: StudyMaterial) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { /* Handle material click */ }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (material.type) {
                MaterialType.VIDEO -> Icons.Filled.PlayArrow
                MaterialType.AUDIO -> Icons.AutoMirrored.Filled.VolumeUp
                MaterialType.PDF -> Icons.Filled.PictureAsPdf
                MaterialType.EXERCISE -> Icons.AutoMirrored.Filled.Assignment
                MaterialType.QUIZ -> Icons.Filled.Quiz
                MaterialType.READING -> Icons.AutoMirrored.Filled.MenuBook
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = material.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (material.description.isNotEmpty()) {
                Text(
                    text = material.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = material.type.name,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NotesSection(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                    contentDescription = null,
                    tint = Color(0xFFFF8F00),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Study Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notes,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}