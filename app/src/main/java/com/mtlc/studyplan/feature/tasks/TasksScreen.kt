@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.feature.tasks.viewmodel.TasksViewModel
import com.mtlc.studyplan.feature.tasks.viewmodel.TasksViewModelFactory
import com.mtlc.studyplan.ui.components.*
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.navigation.StudyPlanNavigationManager
import com.mtlc.studyplan.navigation.TaskFilter
import com.mtlc.studyplan.navigation.TimeRange
import kotlinx.coroutines.flow.collect
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tasks_data")

data class TaskItem(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val difficulty: TaskDifficulty,
    val duration: Int, // minutes
    val xp: Int,
    val isCompleted: Boolean = false
)

enum class TaskCategory(val displayName: String, val icon: ImageVector, val color: Color) {
    VOCABULARY("Vocabulary", Icons.AutoMirrored.Filled.MenuBook, DesignTokens.Success),
    GRAMMAR("Grammar", Icons.Filled.School, DesignTokens.Primary),
    READING("Reading", Icons.AutoMirrored.Filled.Article, DesignTokens.Tertiary),
    LISTENING("Listening", Icons.Filled.Headphones, DesignTokens.Warning),
    PRACTICE_EXAM("Practice Exam", Icons.Filled.Assignment, DesignTokens.Error),
    OTHER("Other", Icons.Filled.Assignment, DesignTokens.Secondary)
}

enum class TaskDifficulty(val displayName: String, val color: Color, val dots: Int) {
    EASY("easy", DesignTokens.Success, 1),
    MEDIUM("medium", DesignTokens.Warning, 3),
    HARD("hard", DesignTokens.Destructive, 5),
    EXPERT("expert", DesignTokens.Primary, 5)
}


enum class TabType(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    CUSTOM("Custom")
}

enum class FilterType(val displayName: String, val icon: ImageVector?) {
    ALL("All Tasks", null),
    VOCABULARY("Vocabulary", Icons.AutoMirrored.Filled.MenuBook),
    GRAMMAR("Grammar", Icons.Filled.School)
}

data class SkillProgress(
    val category: TaskCategory,
    val current: Int,
    val total: Int
) {
    val progress: Float = if (total > 0) current.toFloat() / total else 0f
}

@Composable
fun TasksScreen(
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel? = null,
    navigationManager: StudyPlanNavigationManager? = null
) {
    val context = LocalContext.current

    // Initialize ViewModel with error handling
    val dataStore = remember(context) { context.dataStore }
    val progressRepository = remember(dataStore) { ProgressRepository(dataStore) }
    val planRepository = remember(dataStore) {
        PlanRepository(
            store = PlanOverridesStore(dataStore),
            settings = PlanSettingsStore(dataStore)
        )
    }

    val viewModel: TasksViewModel = viewModel(
        factory = TasksViewModelFactory(
            planRepository = planRepository,
            progressRepository = progressRepository,
            context = context
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val globalError by viewModel.globalError.collectAsState()

    // Collect SharedViewModel state for real-time updates
    val sharedTasks by if (sharedViewModel != null) {
        sharedViewModel.allTasks.collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<com.mtlc.studyplan.shared.AppTask>()) }
    }

    val sharedStudyStats by if (sharedViewModel != null) {
        sharedViewModel.studyStats.collectAsState()
    } else {
        remember { mutableStateOf(com.mtlc.studyplan.shared.StudyStats()) }
    }

    val deepLinkParams by navigationManager?.deepLinkParams?.collectAsState() ?: remember { mutableStateOf(null) }

    var currentFilter by remember { mutableStateOf<TaskFilter?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var highlightedTaskId by remember { mutableStateOf<String?>(null) }

    // Handle deep link parameters
    LaunchedEffect(deepLinkParams) {
        deepLinkParams?.let { params ->
            // Apply filter from navigation
            params.taskFilter?.let { filter ->
                currentFilter = filter

                // Handle specific filter types
                when (filter) {
                    TaskFilter.CREATE_NEW -> {
                        // Show create task dialog or navigate to creation
                        // This would open a task creation interface
                    }
                    TaskFilter.PENDING -> {
                        // Focus on pending tasks
                        currentFilter = TaskFilter.PENDING
                    }
                    TaskFilter.TODAY -> {
                        currentFilter = TaskFilter.TODAY
                    }
                    else -> {
                        currentFilter = filter
                    }
                }
            }

            // Highlight specific task
            params.taskId?.let { taskId ->
                highlightedTaskId = taskId
                // Auto-scroll to task would be implemented here
            }
        }
    }

    // Handle global errors
    globalError?.let { error ->
        ErrorDialog(
            error = error,
            onDismiss = { viewModel.clearError() },
            onRetry = { viewModel.retry() }
        )
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.isError -> {
            LoadingErrorState(
                error = uiState.error!!,
                onRetry = { viewModel.retry() },
                modifier = Modifier.fillMaxSize()
            )
        }
        uiState.isEmpty -> {
            EmptyStateWithError(
                error = uiState.error,
                emptyTitle = "Henüz Görev Yok",
                emptyMessage = "Çalışma planınız hazırlandığında görevler burada görünecek",
                onRetry = if (uiState.error != null) { { viewModel.retry() } } else null,
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            TasksScreenContent(
                uiState = uiState,
                sharedTasks = sharedTasks,
                sharedStudyStats = sharedStudyStats,
                currentFilter = currentFilter,
                highlightedTaskId = highlightedTaskId,
                navigationManager = navigationManager,
                onTaskComplete = { taskId ->
                    // Use SharedViewModel if available, fallback to local ViewModel
                    if (sharedViewModel != null) {
                        sharedViewModel.completeTask(taskId)
                    } else {
                        viewModel.completeTask(taskId)
                    }
                },
                onTaskUncomplete = { taskId -> viewModel.uncompleteTask(taskId) },
                onCategoryFilter = { category -> viewModel.filterByCategory(category) },
                onDifficultyFilter = { difficulty -> viewModel.filterByDifficulty(difficulty) },
                onClearFilters = { viewModel.clearFilters() },
                onRefresh = { viewModel.refresh() },
                onTaskClick = { task ->
                    navigationManager?.navigateToTaskDetail(
                        taskId = task.id,
                        fromScreen = "tasks"
                    )
                }
            )
        }
    }
}

@Composable
private fun TasksScreenContent(
    uiState: com.mtlc.studyplan.feature.tasks.viewmodel.TasksUiState,
    sharedTasks: List<com.mtlc.studyplan.shared.AppTask>,
    sharedStudyStats: com.mtlc.studyplan.shared.StudyStats,
    currentFilter: TaskFilter?,
    highlightedTaskId: String?,
    navigationManager: StudyPlanNavigationManager?,
    onTaskComplete: (String) -> Unit,
    onTaskUncomplete: (String) -> Unit,
    onCategoryFilter: (com.mtlc.studyplan.data.TaskCategory?) -> Unit,
    onDifficultyFilter: (com.mtlc.studyplan.shared.TaskDifficulty?) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onTaskClick: (TaskItem) -> Unit
) {
    var selectedTab by remember { mutableStateOf(TabType.DAILY) }
    var selectedFilter by remember { mutableStateOf(FilterType.ALL) }

    // Use SharedViewModel data if available, fallback to local ViewModel
    val legacyTasks = if (sharedTasks.isNotEmpty()) {
        // Convert SharedViewModel data to legacy format
        sharedTasks.map { task ->
            TaskItem(
                id = task.id,
                title = task.title,
                category = convertSharedTaskCategory(task.category),
                difficulty = convertSharedTaskDifficulty(task.difficulty),
                duration = task.estimatedMinutes,
                xp = task.xpReward,
                isCompleted = task.isCompleted
            )
        }
    } else {
        // Convert local ViewModel data to legacy format
        uiState.filteredTasks.map { task ->
            TaskItem(
                id = task.id,
                title = task.title,
                category = task.category,
                difficulty = task.difficulty,
                duration = task.duration,
                xp = task.xp,
                isCompleted = task.isCompleted
            )
        }
    }

    // Use SharedViewModel XP if available, fallback to local ViewModel
    val totalXp = if (sharedTasks.isNotEmpty()) sharedStudyStats.totalXP else uiState.totalXp

    val skillsProgress = listOf(
        SkillProgress(TaskCategory.VOCABULARY, totalXp / 4, 1000),
        SkillProgress(TaskCategory.GRAMMAR, totalXp / 3, 1000),
        SkillProgress(TaskCategory.READING, totalXp / 5, 1000),
        SkillProgress(TaskCategory.LISTENING, totalXp / 6, 1000)
    )

    // Apply navigation filter first, then UI filter
    val navigationFilteredTasks = when (currentFilter) {
        TaskFilter.TODAY -> legacyTasks.filter { /* Add today logic */ true }
        TaskFilter.PENDING -> legacyTasks.filter { !it.isCompleted }
        TaskFilter.COMPLETED -> legacyTasks.filter { it.isCompleted }
        else -> legacyTasks
    }

    val filteredTasks = when (selectedFilter) {
        FilterType.ALL -> navigationFilteredTasks
        FilterType.VOCABULARY -> navigationFilteredTasks.filter { it.category == TaskCategory.VOCABULARY }
        FilterType.GRAMMAR -> navigationFilteredTasks.filter { it.category == TaskCategory.GRAMMAR }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignTokens.Background)
    ) {
        // Header with Tasks title and XP badge
        TasksHeader(
            xpPoints = totalXp,
            onAddTaskClick = {
                navigationManager?.navigateToTasks(
                    filter = TaskFilter.CREATE_NEW,
                    fromScreen = "tasks"
                )
            }
        )

        // Tab navigation
        TabNavigation(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // Content based on selected tab
        when (selectedTab) {
            TabType.DAILY -> {
                DailyTabContent(
                    skillsProgress = skillsProgress,
                    tasks = filteredTasks,
                    selectedFilter = selectedFilter,
                    highlightedTaskId = highlightedTaskId,
                    onFilterSelected = { selectedFilter = it },
                    onTaskComplete = onTaskComplete,
                    onTaskUncomplete = onTaskUncomplete,
                    onTaskClick = onTaskClick
                )
            }
            TabType.WEEKLY -> {
                WeeklyTabContent()
            }
            TabType.CUSTOM -> {
                CustomTabContent()
            }
        }
    }
}

@Composable
private fun TasksHeader(
    xpPoints: Int,
    onAddTaskClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tasks",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DesignTokens.Foreground
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add Task Button
            IconButton(
                onClick = onAddTaskClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = DesignTokens.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // XP Badge
            Surface(
                color = DesignTokens.Success,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = DesignTokens.PrimaryForeground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$xpPoints XP",
                        color = DesignTokens.PrimaryForeground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TabNavigation(
    selectedTab: TabType,
    onTabSelected: (TabType) -> Unit
) {
    val selectedTabIndex = TabType.values().indexOf(selectedTab)

    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        TabType.values().forEachIndexed { index, tab ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.displayName,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

@Composable
private fun TodaysProgressSection(skillsProgress: List<SkillProgress>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DesignTokens.PrimaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.TrackChanges,
                    contentDescription = null,
                    tint = DesignTokens.Foreground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Today's Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DesignTokens.Foreground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            skillsProgress.forEach { skill ->
                SkillProgressBar(skill = skill)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SkillProgressBar(skill: SkillProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = skill.category.icon,
            contentDescription = null,
            tint = skill.category.color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = skill.category.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DesignTokens.Foreground
                )
                Text(
                    text = "${skill.current}/${skill.total}",
                    fontSize = 14.sp,
                    color = DesignTokens.MutedForeground
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { skill.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = skill.category.color,
                trackColor = DesignTokens.Surface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TaskFilterSection(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Navigation arrow (left)
        IconButton(
            onClick = { /* Handle left navigation */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = DesignTokens.MutedForeground
            )
        }

        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterType.values()) { filter ->
                FilterButton(
                    filter = filter,
                    isSelected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) }
                )
            }
        }

        // Navigation arrow (right)
        IconButton(
            onClick = { /* Handle right navigation */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DesignTokens.MutedForeground
            )
        }
    }
}

@Composable
private fun FilterButton(
    filter: FilterType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = filter.displayName,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = filter.icon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = "${filter.displayName} filter",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        }
    )
}

@Composable
private fun TaskCard(
    task: TaskItem,
    onStartTask: () -> Unit,
    onTaskToggle: () -> Unit,
    onTaskClick: () -> Unit = {},
    isHighlighted: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick() }
            .then(
                if (isHighlighted) Modifier.border(
                    width = 2.dp,
                    color = DesignTokens.Primary,
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) DesignTokens.PrimaryContainer else DesignTokens.Surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top row: Category tag and completion status/difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left side: Category tag and duration (stacked vertically)
                Column {
                    // Difficulty tag
                    Surface(
                        color = task.difficulty.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.difficulty.displayName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = task.difficulty.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${task.duration} min",
                            color = DesignTokens.MutedForeground,
                            fontSize = 12.sp
                        )
                    }
                }

                // Right side: Completion status or difficulty dots
                if (task.isCompleted) {
                    Surface(
                        color = DesignTokens.Success,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = DesignTokens.PrimaryForeground,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }
                } else {
                    DifficultyIndicator(difficulty = task.difficulty)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task title
            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DesignTokens.Foreground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom row: XP and action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = DesignTokens.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${task.xp} XP",
                        color = DesignTokens.MutedForeground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (task.isCompleted) {
                    Text(
                        text = "Completed",
                        color = DesignTokens.Success,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Button(
                        onClick = onStartTask,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DesignTokens.Primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Start Task",
                            color = DesignTokens.PrimaryForeground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyIndicator(difficulty: TaskDifficulty) {
    Column(
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < difficulty.dots) difficulty.color
                            else DesignTokens.Border
                        )
                )
                if (index < 4) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    }
}

// Tab Content Composables

@Composable
private fun DailyTabContent(
    skillsProgress: List<SkillProgress>,
    tasks: List<TaskItem>,
    selectedFilter: FilterType,
    highlightedTaskId: String?,
    onFilterSelected: (FilterType) -> Unit,
    onTaskComplete: (String) -> Unit,
    onTaskUncomplete: (String) -> Unit,
    onTaskClick: (TaskItem) -> Unit
) {
    Column {
        // Today's Progress section
        TodaysProgressSection(skillsProgress = skillsProgress)

        // Filter buttons
        TaskFilterSection(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )

        // Task list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks) { task ->
                TaskCard(
                    task = task,
                    isHighlighted = highlightedTaskId == task.id,
                    onStartTask = {
                        // Start task - could navigate to task details or begin timer
                        onTaskComplete(task.id)
                    },
                    onTaskToggle = {
                        if (task.isCompleted) {
                            onTaskUncomplete(task.id)
                        } else {
                            onTaskComplete(task.id)
                        }
                    },
                    onTaskClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun CustomTabContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CustomPracticeSection()
        }
        item {
            QuickPracticeSection()
        }
    }
}

@Composable
private fun WeeklyTabContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WeeklyGoalsSection()
        }
        item {
            WeeklyStatsSection()
        }
    }
}

@Composable
private fun CustomPracticeSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DesignTokens.PrimaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Custom Practice",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DesignTokens.Foreground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val customPracticeItems = listOf(
                Triple("Create Vocabulary Set", Icons.AutoMirrored.Filled.MenuBook, DesignTokens.Primary),
                Triple("Grammar Focus Session", Icons.Filled.LocalOffer, DesignTokens.Success),
                Triple("Timed Reading Practice", Icons.Filled.Timer, DesignTokens.Tertiary),
                Triple("Listening Comprehension", Icons.Filled.Headphones, DesignTokens.Warning)
            )

            customPracticeItems.forEachIndexed { index, (title, icon, iconColor) ->
                CustomPracticeItem(
                    title = title,
                    icon = icon,
                    iconColor = iconColor,
                    onClick = { /* Handle click */ }
                )
                if (index < customPracticeItems.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomPracticeItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = DesignTokens.Surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = DesignTokens.Foreground
            )
        }
    }
}

@Composable
private fun QuickPracticeSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DesignTokens.PrimaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Practice",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DesignTokens.Foreground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val quickPracticeItems = listOf(
                Triple("5-Minute Vocabulary Boost", 25, DesignTokens.Success),
                Triple("Grammar Speed Test", 30, DesignTokens.Success),
                Triple("Word Association Game", 20, DesignTokens.Success)
            )

            quickPracticeItems.forEachIndexed { index, (title, xp, badgeColor) ->
                QuickPracticeItem(
                    title = title,
                    xp = xp,
                    badgeColor = badgeColor,
                    onClick = { /* Handle click */ }
                )
                if (index < quickPracticeItems.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickPracticeItem(
    title: String,
    xp: Int,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = DesignTokens.Surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = DesignTokens.Foreground
            )

            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$xp XP",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = DesignTokens.PrimaryForeground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WeeklyGoalsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DesignTokens.PrimaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = DesignTokens.Foreground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DesignTokens.Foreground
                )
            }

            WeeklyGoalItem(
                title = "Complete 50 Vocabulary Tasks",
                current = 34,
                total = 50,
                reward = "Reward: 200 XP + Badge",
                progressColor = DesignTokens.Primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeeklyGoalItem(
                title = "Maintain 7-Day Streak",
                current = 5,
                total = 7,
                reward = "Reward: Streak Master Badge",
                progressColor = DesignTokens.Success
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeeklyGoalItem(
                title = "Score 80%+ on All Quizzes",
                current = 9,
                total = 20,
                reward = "Reward: Perfectionist Title",
                progressColor = DesignTokens.Primary
            )
        }
    }
}

@Composable
private fun WeeklyGoalItem(
    title: String,
    current: Int,
    total: Int,
    reward: String,
    progressColor: Color
) {
    val progress = current.toFloat() / total
    val percentage = (progress * 100).toInt()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DesignTokens.Foreground
            )
            Text(
                text = "$current/$total",
                fontSize = 14.sp,
                color = DesignTokens.MutedForeground
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = reward,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = DesignTokens.Surface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$percentage% Complete",
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun WeeklyStatsSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WeeklyStatCard(
            title = "85%",
            subtitle = "Avg. Weekly Score",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            modifier = Modifier.weight(1f)
        )

        WeeklyStatCard(
            title = "12",
            subtitle = "Goals Completed",
            icon = Icons.Filled.EmojiEvents,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WeeklyStatCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = DesignTokens.PrimaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DesignTokens.Primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DesignTokens.Foreground
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = DesignTokens.MutedForeground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// Conversion functions between ViewModel and Legacy UI data classes
private fun convertTaskCategory(
    viewModelCategory: com.mtlc.studyplan.data.TaskCategory
): TaskCategory {
    return when (viewModelCategory) {
        com.mtlc.studyplan.data.TaskCategory.VOCABULARY -> TaskCategory.VOCABULARY
        com.mtlc.studyplan.data.TaskCategory.GRAMMAR -> TaskCategory.GRAMMAR
        com.mtlc.studyplan.data.TaskCategory.READING -> TaskCategory.READING
        com.mtlc.studyplan.data.TaskCategory.LISTENING -> TaskCategory.LISTENING
        com.mtlc.studyplan.data.TaskCategory.PRACTICE_EXAM -> TaskCategory.OTHER
        com.mtlc.studyplan.data.TaskCategory.OTHER -> TaskCategory.OTHER
    }
}

private fun convertTaskDifficulty(
    viewModelDifficulty: com.mtlc.studyplan.shared.TaskDifficulty
): TaskDifficulty {
    return when (viewModelDifficulty) {
        com.mtlc.studyplan.shared.TaskDifficulty.EASY -> TaskDifficulty.EASY
        com.mtlc.studyplan.shared.TaskDifficulty.MEDIUM -> TaskDifficulty.MEDIUM
        com.mtlc.studyplan.shared.TaskDifficulty.HARD -> TaskDifficulty.HARD
        com.mtlc.studyplan.shared.TaskDifficulty.EXPERT -> TaskDifficulty.EXPERT
    }
}

// Conversion functions for SharedViewModel data classes
private fun convertSharedTaskCategory(
    sharedCategory: com.mtlc.studyplan.shared.TaskCategory
): TaskCategory {
    return when (sharedCategory) {
        com.mtlc.studyplan.shared.TaskCategory.VOCABULARY -> TaskCategory.VOCABULARY
        com.mtlc.studyplan.shared.TaskCategory.GRAMMAR -> TaskCategory.GRAMMAR
        com.mtlc.studyplan.shared.TaskCategory.READING -> TaskCategory.READING
        com.mtlc.studyplan.shared.TaskCategory.LISTENING -> TaskCategory.LISTENING
        com.mtlc.studyplan.shared.TaskCategory.PRACTICE_EXAM -> TaskCategory.PRACTICE_EXAM
        com.mtlc.studyplan.shared.TaskCategory.OTHER -> TaskCategory.OTHER
    }
}

private fun convertSharedTaskDifficulty(
    sharedDifficulty: com.mtlc.studyplan.shared.TaskDifficulty
): TaskDifficulty {
    return when (sharedDifficulty) {
        com.mtlc.studyplan.shared.TaskDifficulty.EASY -> TaskDifficulty.EASY
        com.mtlc.studyplan.shared.TaskDifficulty.MEDIUM -> TaskDifficulty.MEDIUM
        com.mtlc.studyplan.shared.TaskDifficulty.HARD -> TaskDifficulty.HARD
        com.mtlc.studyplan.shared.TaskDifficulty.EXPERT -> TaskDifficulty.EXPERT
    }
}
