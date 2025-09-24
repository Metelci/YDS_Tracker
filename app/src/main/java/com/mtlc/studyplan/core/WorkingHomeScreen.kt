package com.mtlc.studyplan.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.ExamCountdownManager
import com.mtlc.studyplan.data.StreakInfo
import com.mtlc.studyplan.data.TaskStats
import com.mtlc.studyplan.data.TodayStats
import com.mtlc.studyplan.data.WeekDay
import com.mtlc.studyplan.data.WeeklyStudyPlan
import com.mtlc.studyplan.integration.AppIntegrationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingHomeScreen(
    appIntegrationManager: AppIntegrationManager,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToWeeklyPlan: () -> Unit = {},
    onNavigateToDaily: (String) -> Unit = {},
    onNavigateToExamDetails: () -> Unit = {},
    onNavigateToStudyPlan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Collect data from AppIntegrationManager
    val allTasks by appIntegrationManager.getAllTasks().collectAsState(initial = emptyList())
    val studyStreak by appIntegrationManager.studyStreak.collectAsState()

    // Calculate stats
    var taskStats by remember { mutableStateOf(TaskStats()) }
    val todayTasks = remember(allTasks) {
        val today = java.time.LocalDate.now()
        allTasks.filter { task ->
            task.dueDate?.let { dueDateTimestamp ->
                val taskDate = java.time.Instant.ofEpochMilli(dueDateTimestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                taskDate == today
            } ?: false
        }
    }
    val completedTodayTasks = todayTasks.filter { it.isCompleted }
    val todayProgress = if (todayTasks.isNotEmpty()) {
        (completedTodayTasks.size.toFloat() / todayTasks.size.toFloat() * 100).toInt()
    } else 0

    // Enhanced first-use detection: user needs proper initial state if they have minimal or no meaningful data
    val isFirstTimeUser = remember(taskStats, studyStreak, allTasks) {
        // Consider user as first-time if they have no tasks OR no completed tasks AND no streak
        (allTasks.isEmpty()) ||
        (taskStats.completedTasks == 0 && studyStreak.currentStreak == 0)
    }

    // Use ExamCountdownManager for real-time exam data
    val examCountdownManager = remember { ExamCountdownManager.getInstance(context) }
    val examTracker by examCountdownManager.examData.collectAsState()
    val weeklyPlan = remember { WeeklyStudyPlan() }
    remember(completedTodayTasks, todayTasks, isFirstTimeUser) {
        if (isFirstTimeUser) {
            // For first-time users, show welcoming empty state
            TodayStats(
                progressPercentage = 0,
                pointsEarned = 0,
                completedTasks = 0,
                totalTasks = 0
            )
        } else {
            // Calculate points based on completed today tasks only
            val pointsFromTodayTasks = completedTodayTasks.size * 10
            TodayStats(
                progressPercentage = todayProgress,
                pointsEarned = pointsFromTodayTasks,
                completedTasks = completedTodayTasks.size,
                totalTasks = maxOf(todayTasks.size, 1)
            )
        }
    }
    val streakInfo = remember(studyStreak, isFirstTimeUser) {
        if (isFirstTimeUser) {
            // For first-time users, start with no streak
            StreakInfo(currentStreak = 0)
        } else {
            StreakInfo(currentStreak = studyStreak.currentStreak)
        }
    }

    LaunchedEffect(allTasks) {
        taskStats = appIntegrationManager.getTaskStats()
    }

    // Refresh exam data when screen loads
    LaunchedEffect(Unit) {
        examCountdownManager.forceRefresh()
    }

    // Language manager setup identical to Settings page

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Section with gradient background
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
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isFirstTimeUser) "Welcome! 👋" else "Good morning! 👋",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF424242)
                    )
                    Text(
                        text = "Ready to ace your YDS exam?",
                        fontSize = 14.sp,
                        color = Color(0xFF616161)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

        // Top Progress Cards Row - Compact and aligned
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Today Progress Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(104.dp) // Reduced by 20% (130 * 0.8 = 104)
                        .clickable { onNavigateToStudyPlan() },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE6E3FF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp), // Slightly reduced padding
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isFirstTimeUser) {
                            Text(
                                text = "Start",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C)
                            )
                            Text(
                                text = "Your Journey",
                                fontSize = 12.sp,
                                color = Color(0xFF424242),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Start studying",
                                modifier = Modifier.size(28.dp),
                                tint = Color(0xFF2C2C2C)
                            )
                        } else {
                            Text(
                                text = "${todayProgress}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C)
                            )
                            Text(
                                text = "Today",
                                fontSize = 12.sp,
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            CircularProgressIndicator(
                                progress = { todayProgress / 100f },
                                modifier = Modifier.size(28.dp),
                                color = Color(0xFF2C2C2C),
                                trackColor = Color(0xFF424242).copy(alpha = 0.3f),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }

                // Days to Exam Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(104.dp) // Reduced by 20% (130 * 0.8 = 104)
                        .clickable { onNavigateToExamDetails() },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0F8FF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp), // Slightly reduced padding
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${examTracker.daysToExam}",
                            fontSize = 24.sp, // Reduced font size
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C2C2C)
                        )
                        Text(
                            text = "Days to YDS",
                            fontSize = 12.sp, // Reduced font size
                            color = Color(0xFF424242),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(1.dp)) // Reduced spacing
                        Text(
                            text = examTracker.statusMessage,
                            fontSize = 9.sp, // Reduced font size
                            color = Color(0xFF616161),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Stats Row - Compact and aligned
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Points Today Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp) // Reduced by 20% (100 * 0.8 = 80)
                        .clickable { /* Progress feature removed */ },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp), // Slightly reduced padding
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isFirstTimeUser) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Earn points",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF2C2C2C)
                            )
                            Text(
                                text = "Earn Points",
                                fontSize = 11.sp,
                                color = Color(0xFF424242),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val pointsFromToday = completedTodayTasks.size * 10
                            Text(
                                text = "$pointsFromToday",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C)
                            )
                            Text(
                                text = "Points Today",
                                fontSize = 11.sp,
                                color = Color(0xFF424242),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Tasks Done Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp) // Reduced by 20% (100 * 0.8 = 80)
                        .clickable { onNavigateToTasks() },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFE5CC)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp), // Slightly reduced padding
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isFirstTimeUser) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Create tasks",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF2C2C2C)
                            )
                            Text(
                                text = "Create Tasks",
                                fontSize = 11.sp,
                                color = Color(0xFF424242),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "${completedTodayTasks.size}/${todayTasks.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C)
                            )
                            Text(
                                text = "Tasks Done",
                                fontSize = 11.sp,
                                color = Color(0xFF424242),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }


        // Streak Card
        if (streakInfo.currentStreak > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Progress feature removed */ },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFF8A65),
                                        Color(0xFFFFAB91)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFE0E0E0),
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
                                    text = streakInfo.displayText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C2C2C)
                                )
                                Text(
                                    text = streakInfo.motivationText,
                                    fontSize = 14.sp,
                                    color = Color(0xFF424242)
                                )
                            }

                            if (streakInfo.showMultiplierBadge) {
                                Surface(
                                    color = Color(0xFF2C2C2C),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = streakInfo.multiplier,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF8A65)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weekly Study Plan Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToWeeklyPlan() },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0FFF0)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 Weekly Study Plan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${(weeklyPlan.progressPercentage * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C)
                            )
                        }
                    }

                    Text(
                        text = weeklyPlan.title,
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { weeklyPlan.progressPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Week Progress • ${weeklyPlan.weekProgressText}",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32).copy(alpha = 0.7f)
                    )
                }
            }
        }

        // This Week Section
        item {
            Column {
                Text(
                    text = "This Week",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(weeklyPlan.days) { day ->
                        DayProgressCard(
                            day = day,
                            onClick = { onNavigateToDaily(day.dayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onNavigateToWeeklyPlan
                    ) {
                        Text(stringResource(R.string.view_full_week))
                    }
                    TextButton(
                        onClick = onNavigateToWeeklyPlan
                    ) {
                        Text(stringResource(R.string.modify_plan))
                    }
                }
            }
        }

        // YDS Exam 2024 Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToExamDetails() },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F8FF)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF2196F3).copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "📅",
                                fontSize = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YDS Exam 2024",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = examTracker.statusMessage,
                            fontSize = 14.sp,
                            color = Color(0xFF1976D2).copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { examTracker.currentPreparationLevel },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF2196F3),
                            trackColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "🕒 ${examTracker.examDateFormatted}",
                            fontSize = 12.sp,
                            color = Color(0xFF1976D2).copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "${examTracker.progressPercentage}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }
            }
        }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun DayProgressCard(
    day: WeekDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (day.isCompleted) Color(0xFFB8E6B8) else Color(0xFFF0FFF0)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.dayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (day.isCompleted) Color(0xFF2C2C2C) else Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = day.displayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (day.isCompleted) Color(0xFF2C2C2C) else Color(0xFF1B5E20)
            )
        }
    }
}

