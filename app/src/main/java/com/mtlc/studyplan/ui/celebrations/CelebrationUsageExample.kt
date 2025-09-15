package com.mtlc.studyplan.ui.celebrations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.*

/**
 * Complete example of how to integrate the celebration system with StudyPlan
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanWithCelebrations(
    progressRepository: ProgressRepository,
    achievementTracker: AchievementTracker,
    modifier: Modifier = Modifier
) {
    val celebrationState = rememberCelebrationState()
    var soundEnabled by remember { mutableStateOf(true) }

    // Integration layer
    CelebrationSystemIntegration(
        progressRepository = progressRepository,
        achievementTracker = achievementTracker,
        soundEnabled = soundEnabled,
        modifier = modifier
    ) {
        Column {
            // App bar with sound toggle
            TopAppBar(
                title = { Text("StudyPlan with Celebrations") },
                actions = {
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }
            )

            // Main content with celebration integration
            StudyContent(
                progressRepository = progressRepository,
                celebrationState = celebrationState
            )
        }
    }
}

/**
 * Main study content with integrated celebrations
 */
@Composable
private fun StudyContent(
    progressRepository: ProgressRepository,
    celebrationState: CelebrationState
) {
    val userProgress by progressRepository.userProgressFlow.collectAsState(initial = UserProgress())
    val taskLogs by progressRepository.taskLogsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress overview with celebration triggers
        ProgressOverviewCard(
            userProgress = userProgress,
            taskLogs = taskLogs,
            celebrationState = celebrationState
        )

        // Daily tasks with celebration integration
        DailyTasksSection(
            progressRepository = progressRepository,
            celebrationState = celebrationState
        )

        // Achievement progress
        AchievementProgressSection(
            userProgress = userProgress
        )
    }
}

/**
 * Progress overview card with celebration integration
 */
@Composable
private fun ProgressOverviewCard(
    userProgress: UserProgress,
    taskLogs: List<TaskLog>,
    celebrationState: CelebrationState
) {
    val todayTasks = remember(taskLogs) {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        taskLogs.filter { it.timestampMillis >= todayStart }
    }

    val dailyProgress = todayTasks.size / 5f // Assuming 5 tasks per day
    val todayPoints = todayTasks.sumOf { it.pointsEarned }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Today's Progress",
                style = MaterialTheme.typography.titleLarge
            )

            // Progress bar with celebration integration
            CelebrationProgressBar(
                progress = dailyProgress,
                celebrationState = celebrationState,
                tasksCompleted = todayTasks.size,
                totalTasks = 5,
                pointsEarned = todayPoints,
                streakCount = userProgress.streakCount
            )

            // Streak display with milestone celebrations
            StreakCard(
                streakCount = userProgress.streakCount,
                celebrationState = celebrationState
            )

            // Points display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Points: ${userProgress.totalPoints}")
                Text("Today: +$todayPoints pts")
            }
        }
    }
}

/**
 * Streak card with milestone celebration triggers
 */
@Composable
private fun StreakCard(
    streakCount: Int,
    celebrationState: CelebrationState
) {
    // Auto-trigger streak milestone celebrations
    StreakMilestoneCelebration(
        streakDays = streakCount,
        celebrationState = celebrationState
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (streakCount >= 50) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (streakCount >= 50) "🔥" else "📅",
                style = MaterialTheme.typography.headlineMedium
            )

            Column {
                Text(
                    text = "$streakCount Day Streak",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when {
                        streakCount >= 100 -> "LEGENDARY STREAK!"
                        streakCount >= 50 -> "Fire Streak!"
                        streakCount >= 30 -> "Master Streak"
                        streakCount >= 14 -> "Power Streak"
                        streakCount >= 7 -> "Building Streak"
                        else -> "Keep going!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Daily tasks section with celebration integration
 */
@Composable
private fun DailyTasksSection(
    progressRepository: ProgressRepository,
    celebrationState: CelebrationState
) {
    // Sample daily tasks - in real app, this would come from PlanDataSource
    val sampleTasks = listOf(
        "Grammar Practice" to "Complete 20 grammar exercises",
        "Reading Comprehension" to "Read 2 passages and answer questions",
        "Vocabulary Building" to "Learn 15 new words",
        "Listening Practice" to "Complete listening exercises",
        "Practice Exam" to "Take a full practice test"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Today's Tasks",
            style = MaterialTheme.typography.titleLarge
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleTasks) { (title, description) ->
                CelebrationAwareTaskCard(
                    taskTitle = title,
                    taskDescription = description,
                    isCompleted = false,
                    onTaskComplete = {
                        // Trigger task completion with celebration
                        celebrationState.triggerCelebration(
                            CelebrationType.TaskCompletion(
                                taskId = title,
                                points = TaskCategory.fromString(description).basePoints,
                                taskCategory = TaskCategory.fromString(description)
                            )
                        )
                    },
                    celebrationState = celebrationState
                )
            }
        }
    }
}

/**
 * Achievement progress section
 */
@Composable
private fun AchievementProgressSection(
    userProgress: UserProgress
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Achievement Progress",
                style = MaterialTheme.typography.titleLarge
            )

            // Sample achievement progress
            AchievementCategory.values().forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = category.icon)
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Sample progress - in real app, this would come from AchievementTracker
                    Text(
                        text = "2/4",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { 0.5f }, // Sample progress
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = androidx.compose.ui.graphics.Color(category.color)
                )
            }
        }
    }
}

/**
 * Example of how to trigger celebrations manually for testing
 */
@Composable
fun CelebrationTestButtons(
    celebrationState: CelebrationState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Test Celebrations",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = {
                celebrationState.triggerCelebration(
                    CelebrationType.TaskCompletion(
                        taskId = "test_task",
                        points = 15,
                        taskCategory = TaskCategory.GRAMMAR
                    )
                )
            }
        ) {
            Text("Test Task Completion")
        }

        Button(
            onClick = {
                celebrationState.triggerCelebration(
                    CelebrationType.DailyGoalAchieved(
                        tasksCompleted = 5,
                        totalTasks = 5,
                        streakCount = 10,
                        pointsEarned = 75
                    )
                )
            }
        ) {
            Text("Test Daily Goal")
        }

        Button(
            onClick = {
                celebrationState.triggerCelebration(
                    CelebrationType.LevelUp(
                        achievement = CategorizedAchievementDataSource.allCategorizedAchievements.first(),
                        newLevel = AchievementTier.GOLD,
                        totalPoints = 1500
                    )
                )
            }
        ) {
            Text("Test Level Up")
        }

        Button(
            onClick = {
                celebrationState.triggerCelebration(
                    CelebrationType.MilestoneReward(
                        milestoneType = MilestoneType.STREAK_MILESTONE,
                        value = 30,
                        reward = "Fire Streak Badge!",
                        points = 1500
                    )
                )
            }
        ) {
            Text("Test Milestone")
        }
    }
}

/**
 * Settings for celebration system
 */
@Composable
fun CelebrationSettings(
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Celebration Settings",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Effects")
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = onSoundToggle
                )
            }

            Text(
                text = "Celebrations help you stay motivated by recognizing your achievements and milestones!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}