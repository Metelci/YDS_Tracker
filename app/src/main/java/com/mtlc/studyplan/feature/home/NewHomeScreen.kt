@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.data.Task as DataTask
import com.mtlc.studyplan.data.dataStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.navigation.StudyPlanNavigationManager
import com.mtlc.studyplan.navigation.TaskFilter
import com.mtlc.studyplan.navigation.TimeRange
import com.mtlc.studyplan.eventbus.AppEvent
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.derivedStateOf

@Composable
fun NewHomeScreen(
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel? = null,
    navigationManager: StudyPlanNavigationManager? = null,
    settingsManager: com.mtlc.studyplan.settings.manager.SettingsManager? = null,
    offlineManager: com.mtlc.studyplan.offline.OfflineManager? = null
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val settingsStore = remember { PlanSettingsStore(appContext.dataStore) }
    val overridesStore = remember { PlanOverridesStore(appContext.dataStore) }
    val planRepo = remember { PlanRepository(overridesStore, settingsStore) }
    val progressRepo = remember { ProgressRepository(appContext.dataStore) }

    // Get current settings if available
    val userSettings by if (settingsManager != null) {
        settingsManager.currentSettings.collectAsState()
    } else {
        remember { mutableStateOf(com.mtlc.studyplan.settings.data.UserSettings.default()) }
    }

    // Get offline status if available
    val isOperatingOffline by if (offlineManager != null) {
        remember { derivedStateOf { offlineManager.isOperatingOffline() } }
    } else {
        remember { mutableStateOf(false) }
    }

    val pendingActionsCount by if (offlineManager != null) {
        offlineManager.pendingActions.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<com.mtlc.studyplan.offline.OfflineAction>()) }
    }

    // Use SharedViewModel data when available, fallback to local data
    val plan by if (sharedViewModel != null) {
        sharedViewModel.planFlow.collectAsState(initial = emptyList())
    } else {
        planRepo.planFlow.collectAsState(initial = emptyList())
    }

    val progress by if (sharedViewModel != null) {
        sharedViewModel.userProgress.collectAsState(initial = UserProgress())
    } else {
        progressRepo.userProgressFlow.collectAsState(initial = UserProgress())
    }

    val todayTasks by if (sharedViewModel != null) {
        sharedViewModel.todayTasks.collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<com.mtlc.studyplan.shared.AppTask>()) }
    }

    val studyStats by if (sharedViewModel != null) {
        sharedViewModel.studyStats.collectAsState(initial = com.mtlc.studyplan.shared.StudyStats())
    } else {
        remember { mutableStateOf(com.mtlc.studyplan.shared.StudyStats()) }
    }

    val uiState by if (sharedViewModel != null) {
        sharedViewModel.uiState.collectAsState()
    } else {
        remember { mutableStateOf(com.mtlc.studyplan.shared.AppUiState()) }
    }

    val today = remember { LocalDate.now() }
    val settings by settingsStore.settingsFlow.collectAsState(initial = PlanDurationSettings())

    // Calculate today's progress
    val absIndex = remember(settings.startEpochDay, today) {
        val start = LocalDate.ofEpochDay(settings.startEpochDay)
        val diff = ChronoUnit.DAYS.between(start, today).toInt()
        diff.coerceAtLeast(0)
    }

    val todayTasks = remember(plan, absIndex) {
        var idx = absIndex
        for (week in plan) {
            if (idx < week.days.size) {
                return@remember week.days[idx].tasks
            } else idx -= week.days.size
        }
        emptyList<DataTask>()
    }

    val todayProgress = remember(todayTasks, progress.completedTasks) {
        if (todayTasks.isEmpty()) 0f
        else todayTasks.count { it.id in progress.completedTasks }.toFloat() / todayTasks.size
    }

    val todayPoints: Int = remember(todayTasks, progress.completedTasks) {
        todayTasks.filter { it.id in progress.completedTasks }.size * 10 // 10 points per task
    }

    val tasksCompleted: Int = remember(todayTasks, progress.completedTasks) {
        todayTasks.count { it.id in progress.completedTasks }
    }

    val coroutineScope = rememberCoroutineScope()

    // Real-time event handling
    LaunchedEffect(sharedViewModel) {
        sharedViewModel?.appEventBus?.events?.collect { event ->
            when (event) {
                is AppEvent.TaskCompleted -> {
                    // Show completion celebration
                    // Auto-navigate to progress if milestone reached
                    if (event.result.triggeredMilestone) {
                        navigationManager?.navigateToProgress(
                            highlightElement = "daily_progress",
                            fromScreen = "home"
                        )
                    }
                }
                else -> {}
            }
        }
    }

    // Calculate days to exam
    val nextExam = ExamCalendarDataSource.getNextExam()
    val daysToExam = nextExam?.let { ChronoUnit.DAYS.between(today, it.examDate) } ?: 274

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignTokens.Background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Offline indicator
        if (isOperatingOffline) {
            item {
                OfflineIndicatorCard(
                    pendingActionsCount = pendingActionsCount.size,
                    onSyncClick = {
                        coroutineScope.launch {
                            offlineManager?.syncPendingActions()
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        // Header with greeting
        item {
            Column(
                modifier = Modifier.padding(top = if (isOperatingOffline) 8.dp else 24.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Good morning! 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Foreground
                )
                Text(
                    text = "Ready to ace your YDS exam?",
                    fontSize = 16.sp,
                    color = DesignTokens.MutedForeground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Main progress card - matching screenshot exactly
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DesignTokens.ExamBlue
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left side - 65% Today
                        Column {
                            Text(
                                text = "65%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Foreground
                            )
                            Text(
                                text = "Today",
                                fontSize = 14.sp,
                                color = DesignTokens.MutedForeground
                            )
                        }

                        // Right side - Days to YDS with navigation
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.clickable {
                                navigationManager?.navigateToProgress(
                                    timeRange = TimeRange.WEEK,
                                    highlightElement = "streak_chart",
                                    fromScreen = "home"
                                )
                            }
                        ) {
                            Text(
                                text = daysToExam.toString(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Foreground
                            )
                            Text(
                                text = "Days to YDS",
                                fontSize = 14.sp,
                                color = DesignTokens.MutedForeground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { 0.65f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = DesignTokens.Success,
                        trackColor = DesignTokens.Surface.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Exam Preparation",
                        fontSize = 12.sp,
                        color = DesignTokens.MutedForeground
                    )
                }
            }
        }

        // Stats row (Points and Tasks) - matching screenshot colors
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Points card - softer green with navigation
                Card(
                    modifier = Modifier.weight(1f).clickable {
                        navigationManager?.navigateToProgress(
                            timeRange = TimeRange.TODAY,
                            highlightElement = "today_summary",
                            fromScreen = "home"
                        )
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = DesignTokens.PointsGreen
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (sharedViewModel != null) studyStats.totalXP.toString() else "40",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Foreground
                        )
                        Text(
                            text = "Points Today",
                            fontSize = 12.sp,
                            color = DesignTokens.MutedForeground,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Tasks done card - softer coral with navigation
                Card(
                    modifier = Modifier.weight(1f).clickable {
                        navigationManager?.navigateToTasks(
                            filter = TaskFilter.TODAY,
                            fromScreen = "home"
                        )
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = DesignTokens.TasksDone
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "1/3",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Foreground
                        )
                        Text(
                            text = "Tasks Done",
                            fontSize = 12.sp,
                            color = DesignTokens.MutedForeground,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Streak card - matching screenshot exactly with navigation
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    navigationManager?.navigateToProgress(
                        timeRange = TimeRange.WEEK,
                        highlightElement = "streak_chart",
                        fromScreen = "home"
                    )
                },
                colors = CardDefaults.cardColors(
                    containerColor = DesignTokens.StreakFire
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fire emoji with background circle
                    Surface(
                        color = DesignTokens.Surface.copy(alpha = 0.3f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "12 days streak",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "You're on fire! 🔥",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    // 2x multiplier badge
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "2x",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // YDS Exam 2024 card - matching screenshot exactly
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DesignTokens.ExamBlue
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YDS Exam 2024",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Text(
                        text = "Exam day!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progress",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "100%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF2196F3),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "December 15, 2024",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Smart Suggestion card - matching screenshot exactly
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DesignTokens.ExamBlue
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF2196F3),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Smart Suggestion",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "AI-powered recommendation",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Based on your progress, focus on reading comprehension today. You're 85% ready for grammar but could improve reading speed by 15%.",
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            navigationManager?.navigateToTasks(
                                filter = TaskFilter.CREATE_NEW,
                                fromScreen = "home"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Try Recommended Task",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Today's Tasks section with navigation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Tasks",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row {
                    if (navigationManager != null) {
                        IconButton(
                            onClick = {
                                navigationManager.navigateToTasks(
                                    filter = TaskFilter.CREATE_NEW,
                                    fromScreen = "home"
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Task",
                                tint = Color(0xFF2196F3)
                            )
                        }
                    }

                    if (sharedViewModel != null || navigationManager != null) {
                        TextButton(
                            onClick = {
                                if (navigationManager != null) {
                                    navigationManager.navigateToTasks(
                                        filter = TaskFilter.TODAY,
                                        fromScreen = "home"
                                    )
                                } else {
                                    sharedViewModel?.navigateToTasks()
                                }
                            }
                        ) {
                            Text(
                                text = "View All",
                                color = Color(0xFF2196F3),
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Real task items from SharedViewModel
        if (sharedViewModel != null && todayTasks.isNotEmpty()) {
            items(todayTasks) { task ->
                TaskItemIntegrated(
                    task = task,
                    onToggleComplete = {
                        sharedViewModel.completeTask(task.id)
                    },
                    onTaskClick = {
                        if (navigationManager != null) {
                            navigationManager.navigateToTaskDetail(
                                taskId = task.id,
                                fromScreen = "home"
                            )
                        } else {
                            sharedViewModel.navigateToTasks(
                                com.mtlc.studyplan.shared.TaskFilter.ById(task.id)
                            )
                        }
                    }
                )
            }
        } else {
            // Fallback static tasks when SharedViewModel not available
            item {
                TaskItemScreenshot(
                    category = "Grammar",
                    title = "Complete Grammar Practice Set",
                    time = "15min",
                    points = "50 points",
                    isCompleted = false,
                    categoryColor = DesignTokens.TaskGrammar,
                    borderColor = Color(0xFF2196F3),
                    onToggleComplete = { },
                    onTaskClick = {
                        navigationManager?.navigateToTasks(
                            filter = TaskFilter.TODAY,
                            fromScreen = "home"
                        )
                    }
                )
            }

            item {
                TaskItemScreenshot(
                    category = "Reading",
                    title = "Reading Comprehension - Science Articles",
                    time = "20min",
                    points = "75 points",
                    isCompleted = false,
                    categoryColor = DesignTokens.TaskReading,
                    borderColor = Color(0xFF4CAF50),
                    onToggleComplete = { },
                    onTaskClick = {
                        navigationManager?.navigateToTasks(
                            filter = TaskFilter.TODAY,
                            fromScreen = "home"
                        )
                    }
                )
            }

            item {
                TaskItemScreenshot(
                    category = "Vocabulary",
                    title = "Vocabulary Builder - Advanced Words",
                    time = "10min",
                    points = "25 points",
                    isCompleted = true,
                    categoryColor = DesignTokens.TaskVocabulary,
                    borderColor = Color(0xFF8BC34A),
                    onToggleComplete = { },
                    onTaskClick = {
                        navigationManager?.navigateToTasks(
                            filter = TaskFilter.TODAY,
                            fromScreen = "home"
                        )
                    }
                )
            }
        }

        // Bottom spacing for navigation
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun TaskItemScreenshot(
    category: String,
    title: String,
    time: String,
    points: String,
    isCompleted: Boolean,
    categoryColor: Color,
    borderColor: Color,
    onToggleComplete: () -> Unit,
    onTaskClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 4.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onTaskClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) categoryColor else categoryColor.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = time,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "⭐ $points",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Complete button or checkmark - exactly like screenshot
            if (isCompleted) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Surface(
                    color = Color.Transparent,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(32.dp)
                        .border(2.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                ) {
                    // Empty circle for incomplete tasks
                }
            }
        }
    }
}

@Composable
fun TaskItemIntegrated(
    task: com.mtlc.studyplan.shared.AppTask,
    onToggleComplete: () -> Unit,
    onTaskClick: () -> Unit
) {
    val categoryColor = when (task.category) {
        com.mtlc.studyplan.shared.TaskCategory.GRAMMAR -> DesignTokens.TaskGrammar
        com.mtlc.studyplan.shared.TaskCategory.READING -> DesignTokens.TaskReading
        com.mtlc.studyplan.shared.TaskCategory.VOCABULARY -> DesignTokens.TaskVocabulary
        com.mtlc.studyplan.shared.TaskCategory.LISTENING -> Color(0xFFFF9800)
        com.mtlc.studyplan.shared.TaskCategory.PRACTICE_EXAM -> Color(0xFF9C27B0)
        com.mtlc.studyplan.shared.TaskCategory.OTHER -> Color(0xFF607D8B)
    }

    val borderColor = when (task.category) {
        com.mtlc.studyplan.shared.TaskCategory.GRAMMAR -> Color(0xFF2196F3)
        com.mtlc.studyplan.shared.TaskCategory.READING -> Color(0xFF4CAF50)
        com.mtlc.studyplan.shared.TaskCategory.VOCABULARY -> Color(0xFF8BC34A)
        com.mtlc.studyplan.shared.TaskCategory.LISTENING -> Color(0xFFFF9800)
        com.mtlc.studyplan.shared.TaskCategory.PRACTICE_EXAM -> Color(0xFF9C27B0)
        com.mtlc.studyplan.shared.TaskCategory.OTHER -> Color(0xFF607D8B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onTaskClick() },
        colors = CardDefaults.cardColors(
            containerColor = categoryColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = borderColor,
                    uncheckedColor = Color.White,
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Task content
            Column(modifier = Modifier.weight(1f)) {
                // Category tag
                Surface(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = task.category.name,
                        fontSize = 10.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Task title
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time and points
                Row {
                    Text(
                        text = "${task.estimatedMinutes}min",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${task.xpReward} points",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Completion indicator
            if (task.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun OfflineIndicatorCard(
    pendingActionsCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFC107)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Offline Mode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "$pendingActionsCount actions pending sync",
                        fontSize = 12.sp,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }

            if (pendingActionsCount > 0) {
                com.mtlc.studyplan.ui.SettingsAwareTextButton(
                    onClick = onSyncClick,
                    hapticType = com.mtlc.studyplan.utils.HapticType.LIGHT_CLICK
                ) {
                    Text(
                        text = "Sync",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}