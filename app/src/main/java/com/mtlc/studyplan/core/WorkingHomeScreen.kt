package com.mtlc.studyplan.core

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.integration.AppIntegrationManager
import androidx.compose.ui.platform.LocalContext
import com.mtlc.studyplan.ui.components.FloatingLanguageSwitcher
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Collect data from AppIntegrationManager
    val allTasks by appIntegrationManager.getAllTasks().collectAsState(initial = emptyList())
    val studyStreak by appIntegrationManager.studyStreak.collectAsState()
    val achievements by appIntegrationManager.achievements.collectAsState()

    // Calculate stats
    var taskStats by remember { mutableStateOf(TaskStats()) }

    // Enhanced first-use detection: user needs proper initial state if they have minimal or no meaningful data
    val isFirstTimeUser = remember(taskStats, studyStreak) {
        // Consider user as first-time if they have no tasks OR no completed tasks AND no streak
        (taskStats.totalTasks == 0 && taskStats.completedTasks == 0) ||
        (taskStats.completedTasks == 0 && studyStreak.currentStreak == 0)
    }

    // Use ExamCountdownManager for real-time exam data
    val examCountdownManager = remember { ExamCountdownManager.getInstance(context) }
    val examTracker by examCountdownManager.examData.collectAsState()
    val weeklyPlan = remember { WeeklyStudyPlan() }
    val todayStats = remember(taskStats, isFirstTimeUser) {
        if (isFirstTimeUser) {
            // For first-time users, show welcoming empty state
            TodayStats(
                progressPercentage = 0,
                pointsEarned = 0,
                completedTasks = 0,
                totalTasks = 0
            )
        } else {
            TodayStats(
                progressPercentage = taskStats.getProgressPercentage(),
                pointsEarned = taskStats.completedTasks * 10,
                completedTasks = taskStats.completedTasks,
                totalTasks = maxOf(taskStats.totalTasks, 1)
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Header Section
        item {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    text = if (isFirstTimeUser) "Welcome! 👋" else "Good morning! 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isFirstTimeUser)
                        "Let's start your YDS study journey!"
                    else
                        "Ready to ace your YDS exam?",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

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
                        containerColor = Color(0xFF26C6DA)
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
                                color = Color.White
                            )
                            Text(
                                text = "Your Journey",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Start studying",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                        } else {
                            Text(
                                text = "${todayStats.progressPercentage}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Today",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            CircularProgressIndicator(
                                progress = { todayStats.progressPercentage / 100f },
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f),
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
                        containerColor = Color(0xFF4FC3F7)
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
                            color = Color.White
                        )
                        Text(
                            text = "Days to YDS",
                            fontSize = 12.sp, // Reduced font size
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(1.dp)) // Reduced spacing
                        Text(
                            text = examTracker.statusMessage,
                            fontSize = 9.sp, // Reduced font size
                            color = Color.White.copy(alpha = 0.8f),
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
                        containerColor = Color(0xFF66BB6A)
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
                                tint = Color.White
                            )
                            Text(
                                text = "Earn Points",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "${todayStats.pointsEarned}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Points Today",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
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
                        containerColor = Color(0xFFFF8A65)
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
                                tint = Color.White
                            )
                            Text(
                                text = "Create Tasks",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = todayStats.tasksProgressText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tasks Done",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
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
                                color = Color.White.copy(alpha = 0.2f),
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
                                    color = Color.White
                                )
                                Text(
                                    text = streakInfo.motivationText,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            if (streakInfo.showMultiplierBadge) {
                                Surface(
                                    color = Color.White,
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
                    containerColor = Color(0xFFF1F8E9)
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
                                color = Color.White
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
                        Text("View Full Week")
                    }
                    TextButton(
                        onClick = onNavigateToWeeklyPlan
                    ) {
                        Text("▶ Modify Plan")
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
                    containerColor = Color(0xFFE3F2FD)
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

        // Floating Language Switcher in top-right corner
        FloatingLanguageSwitcher(
            modifier = Modifier.align(Alignment.TopEnd)
        )
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
            containerColor = if (day.isCompleted) Color(0xFF4CAF50) else Color(0xFFE8F5E8)
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
                color = if (day.isCompleted) Color.White else Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = day.displayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (day.isCompleted) Color.White else Color(0xFF1B5E20)
            )
        }
    }
}

