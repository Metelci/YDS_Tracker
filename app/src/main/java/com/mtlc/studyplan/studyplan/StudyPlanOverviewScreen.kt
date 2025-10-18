package com.mtlc.studyplan.studyplan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
// removed luminance-based dark theme checks
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.integration.AppIntegrationManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanOverviewScreen(
    appIntegrationManager: AppIntegrationManager,
    studyProgressRepository: StudyProgressRepository,
    onNavigateBack: () -> Unit = {},
    initialTab: StudyPlanTab = StudyPlanTab.WEEKLY
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var selectedDay by remember { mutableStateOf<DailyStudyInfo?>(null) }

    // Collect data from AppIntegrationManager
    val taskStats by appIntegrationManager.getTaskStatsFlow().collectAsState(
        initial = TaskStats(0, 0, 0, 0)
    )

    // Get current week from study progress repository
    val currentWeek by studyProgressRepository.currentWeek.collectAsState(initial = 1)

    // Create WeeklyStudyPlan with actual current week
    val weeklyPlan = remember(currentWeek) {
        WeeklyStudyPlan(
            title = "YDS Study Plan",
            currentWeek = currentWeek,
            // Neutralize computed percentage for initial use
            progressPercentage = 0f
        )
    }
    val studySchedule = remember { createStudyScheduleData() }

    Scaffold(
        topBar = {
            // Settings-style topbar with pastel gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFBE9E7), // Light pastel red/pink
                                    Color(0xFFE3F2FD)  // Light pastel blue
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(2.dp, Color(0xFF0066FF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF424242)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Study Plan Overview",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF424242)
                                )
                                Text(
                                    text = "Track your YDS preparation progress",
                                    fontSize = 14.sp,
                                    color = Color(0xFF616161)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val isDarkTheme = false
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = if (isDarkTheme) {
                        // Seamless anthracite to light grey gradient for dark theme
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2C2C2C), // Deep anthracite (top)
                                Color(0xFF3A3A3A), // Medium anthracite
                                Color(0xFF4A4A4A)  // Light anthracite (bottom)
                            )
                        )
                    } else {
                        // Keep original light theme gradient unchanged
                        Brush.verticalGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFF7FBFF)))
                    }
                )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(StudyPlanTab.entries) { tab ->
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
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "tab_background"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                style = MaterialTheme.typography.labelMedium,
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
                taskStats = taskStats
            )
        }

        // Daily Schedule Cards
        item {
            Text(
                text = "This Week's Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(studySchedule.dailySchedules) { dailySchedule ->
            DailyScheduleCard(
                dailySchedule = dailySchedule,
                onClick = {
                    onDayClick(createDailyStudyInfo(dailySchedule, weeklyPlan.currentWeek))
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
private fun CurrentWeekCard(
    taskStats: TaskStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Week Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = {
                            val realProgress = if (taskStats.totalTasks > 0) taskStats.getProgressPercentage() else 0
                            realProgress / 100f
                        },
                        modifier = Modifier.size(44.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Text(
                        text = if (taskStats.totalTasks > 0) "${taskStats.getProgressPercentage()}%" else "0%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Only show metrics when user has real task data
            if (taskStats.totalTasks > 0 && taskStats.completedTasks > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val realProgress = taskStats.getProgressPercentage()
                    ProgressMetric("Completed", "${taskStats.completedTasks}")
                    ProgressMetric("Remaining", "${maxOf(0, taskStats.totalTasks - taskStats.completedTasks)}")
                    ProgressMetric("Progress", "${realProgress}%")
                }
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (dailySchedule.tasks.isEmpty() && dailySchedule.estimatedTime.isEmpty()) {
                        "Ready to plan your study session"
                    } else {
                        "${dailySchedule.tasks.size} tasks • ${dailySchedule.estimatedTime}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Only show progress indicator when there is actual progress data
            if (dailySchedule.completionPercentage > 0) {
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
                        color = if (dailySchedule.completionPercentage >= 100) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${dailySchedule.completionPercentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Data classes and helper functions
enum class StudyPlanTab(val title: String, val icon: ImageVector) {
    WEEKLY("Weekly", Icons.Filled.CalendarToday),
    DAILY("Daily", Icons.Filled.DateRange)
}

data class StudyScheduleData(
    val dailySchedules: List<DailySchedule>,
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

data class StudyGoal(
    val title: String,
    val description: String,
    val progress: Int,
    val isCompleted: Boolean
)

enum class Priority { HIGH, MEDIUM, LOW }

private fun createStudyScheduleData(): StudyScheduleData {
    // Generate current week dates for labels only
    val today = LocalDate.now()
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

    // Neutral schedule with no fabricated subjects, durations, or progress
    val dailySchedules = listOf(
        DailySchedule("Monday", startOfWeek.format(dateFormatter), emptyList(), "", 0, today.dayOfWeek.value == 1),
        DailySchedule("Tuesday", startOfWeek.plusDays(1).format(dateFormatter), emptyList(), "", 0, today.dayOfWeek.value == 2),
        DailySchedule("Wednesday", startOfWeek.plusDays(2).format(dateFormatter), emptyList(), "", 0, today.dayOfWeek.value == 3),
        DailySchedule("Thursday", startOfWeek.plusDays(3).format(dateFormatter), emptyList(), "", 0, today.dayOfWeek.value == 4),
        DailySchedule("Friday", startOfWeek.plusDays(4).format(dateFormatter), emptyList(), "", 0, today.dayOfWeek.value == 5),
        DailySchedule("Saturday", startOfWeek.plusDays(5).format(dateFormatter), emptyList(), "", 0, today.dayOfWeek.value == 6),
        DailySchedule("Sunday", startOfWeek.plusDays(6).format(dateFormatter), emptyList(), "", 0, today.dayOfWeek.value == 7)
    )

    // No pre-filled goals until there is real data
    val weeklyGoals = emptyList<StudyGoal>()

    return StudyScheduleData(dailySchedules, weeklyGoals)
}

// Helper function to create DailyStudyInfo from DailySchedule
private fun createDailyStudyInfo(dailySchedule: DailySchedule, currentWeek: Int = 1): DailyStudyInfo {
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

    // Use the current week from WeeklyStudyPlan parameter

    // Assign books based on the 30-week curriculum progression
    val book = when (currentWeek) {
        in 1..8 -> StudyBook.RED_BOOK      // Weeks 1-8: Red Book Foundation
        in 9..18 -> StudyBook.BLUE_BOOK    // Weeks 9-18: Blue Book Intermediate
        in 19..26 -> StudyBook.GREEN_BOOK  // Weeks 19-26: Green Book Advanced
        else -> StudyBook.RED_BOOK         // Weeks 27-30: Exam Camp (mixed/review - default to Red for now)
    }

    // Create realistic units based on the curriculum and book progression
    val units = when (// Red Book: Units 1-115 across 8 weeks
        book) {
        StudyBook.RED_BOOK -> {
            val baseUnitNumber = (currentWeek - 1) * 14 + dayIndex * 2 + 1 // Roughly 14 units per week
            listOf(
                StudyUnit(
                    title = when (baseUnitNumber % 10) {
                        0 -> "Present Simple and Continuous"
                        1 -> "Past Simple and Continuous"
                        2 -> "Present Perfect"
                        3 -> "Modal Verbs"
                        4 -> "Future Forms"
                        5 -> "Conditionals"
                        6 -> "Passive Voice"
                        7 -> "Reported Speech"
                        8 -> "Gerunds and Infinitives"
                        else -> "Questions and Negatives"
                    },
                    unitNumber = minOf(baseUnitNumber, 115),
                    pages = "${baseUnitNumber * 2}-${baseUnitNumber * 2 + 3}",
                    exercises = listOf("${baseUnitNumber}.1", "${baseUnitNumber}.2"),
                    isCompleted = dailySchedule.completionPercentage > 50
                )
            )
        }

        // Blue Book: Intermediate level topics
        StudyBook.BLUE_BOOK -> {
            val weekInBlueBook = currentWeek - 8 // Blue book starts at week 9
            listOf(
                StudyUnit(
                    title = when (weekInBlueBook) {
                        1 -> "Tenses Review (All Tenses Comparison)"
                        2 -> "Future in Detail (Continuous/Perfect)"
                        3 -> "Modals 1 (Ability, Permission, Advice)"
                        4 -> "Modals 2 (Deduction, Obligation, Regret)"
                        5 -> "Conditionals & Wish (All Types)"
                        6 -> "Passive Voice (All Tenses) & 'have something done'"
                        7 -> "Reported Speech (Questions, Commands, Advanced)"
                        8 -> "Noun Clauses & Relative Clauses"
                        9 -> "Gerunds & Infinitives (Advanced patterns)"
                        10 -> "Conjunctions & Connectors"
                        else -> "Advanced Grammar Review"
                    },
                    unitNumber = weekInBlueBook * 10 + dayIndex,
                    pages = "${weekInBlueBook * 8}-${weekInBlueBook * 8 + 7}",
                    exercises = listOf("${weekInBlueBook}.1", "${weekInBlueBook}.2", "${weekInBlueBook}.3"),
                    isCompleted = dailySchedule.completionPercentage > 50
                )
            )
        }

        // Green Book: Advanced level topics
        StudyBook.GREEN_BOOK -> {
            val weekInGreenBook = currentWeek - 18 // Green book starts at week 19
            listOf(
                StudyUnit(
                    title = when (weekInGreenBook) {
                        1 -> "Advanced Tense Nuances & Narrative Tenses"
                        2 -> "Inversion & Emphasis (Not only, Hardly...)"
                        3 -> "Advanced Modals (Speculation, Hypothetical)"
                        4 -> "Participle Clauses (-ing and -ed clauses)"
                        5 -> "Advanced Connectors & Discourse Markers"
                        6 -> "Hypothetical Meaning & Subjunctives"
                        7 -> "Adjectives & Adverbs (Advanced Uses)"
                        else -> "Prepositions & Phrasal Verbs (Advanced)"
                    },
                    unitNumber = weekInGreenBook * 5 + dayIndex,
                    pages = "${weekInGreenBook * 6}-${weekInGreenBook * 6 + 5}",
                    exercises = listOf("${weekInGreenBook}.1", "${weekInGreenBook}.2"),
                    isCompleted = dailySchedule.completionPercentage > 50
                )
            )
        }
        else -> emptyList()
    }

    // No fabricated tasks - only show real user-created tasks
    val tasks = emptyList<DailyTask>()

    // No fabricated materials - only show real user-added materials
    val materials = emptyList<StudyMaterial>()

    // No fabricated notes - only show real user notes
    val notes = ""

    return DailyStudyInfo(
        weekTitle = "Week $currentWeek - ${book.description}",
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
private fun StudyGoalsCard(goals: List<StudyGoal>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
            tint = if (goal.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        dayInfo.completionPercentage >= 100 -> MaterialTheme.colorScheme.primary
                        dayInfo.completionPercentage > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
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
                color = if (unit.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (unit.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = null,
            tint = if (unit.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = if (task.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = null,
            tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                Priority.HIGH -> MaterialTheme.colorScheme.error
                Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                Priority.LOW -> MaterialTheme.colorScheme.primary
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
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
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
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


