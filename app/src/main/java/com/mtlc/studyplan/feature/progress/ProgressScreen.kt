package com.mtlc.studyplan.feature.progress

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.AchievementState
import com.mtlc.studyplan.data.AchievementTracker
import com.mtlc.studyplan.data.CategorizedAchievementDataSource
import com.mtlc.studyplan.data.StreakManager
import com.mtlc.studyplan.data.StreakMultiplier
import com.mtlc.studyplan.data.StreakState
import com.mtlc.studyplan.data.TaskCategory
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.feature.progress.viewmodel.ProgressViewModel
import com.mtlc.studyplan.feature.progress.viewmodel.ProgressViewModelFactory
import com.mtlc.studyplan.ui.components.*
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.realtime.RealTimeUpdateManager
import com.mtlc.studyplan.realtime.ProgressUpdateType
import com.mtlc.studyplan.navigation.StudyPlanNavigationManager
import com.mtlc.studyplan.navigation.TimeRange
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.flow.collect
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private val turkishLocale = Locale("tr", "TR")

private enum class ProgressTab(val title: String) {
    OVERVIEW("Genel Bakış"),
    SKILLS("Beceriler"),
    AWARDS("Ödüller"),
    ANALYTICS("YZ Analitiği")
}

private enum class AnalyticsRange(val label: String, val description: String, val days: Long?) {
    LAST_7_DAYS("7G", "Son 7 günlük performans", 7),
    LAST_30_DAYS("30G", "Son 30 gün", 30),
    ALL_TIME("Tümü", "Tüm zamanların özeti", null),
    PERFORMANCE("Perf", "Performans eğilimleri", 14),
    AI_INSIGHTS("YZ", "YZ içgörüleri", 7)
}

private data class ProgressUiState(
    val overallPercent: Int,
    val totalPoints: Int,
    val awardsCount: Int,
    val overview: OverviewData,
    val skills: List<SkillCardData>,
    val awards: List<AwardCardData>,
    val analytics: Map<AnalyticsRange, AnalyticsSnapshot>
)

private data class OverviewData(
    val summaryCards: List<OverviewSummary>,
    val weeklyProgress: List<WeeklyDayProgress>,
    val studyTime: StudyTimeStats
)

private data class OverviewSummary(
    val title: String,
    val value: String,
    val color: Color,
    val contentColor: Color
)

private data class WeeklyDayProgress(
    val dayLabel: String,
    val percent: Float,
    val percentLabel: String,
    val isToday: Boolean
)

private data class StudyTimeStats(
    val totalMinutes: Int,
    val averageMinutes: Int
)

private data class SkillCardData(
    val type: SkillType,
    val pointsLabel: String,
    val percent: Float,
    val percentLabel: String,
    val level: SkillLevelInfo
)

private data class SkillLevelInfo(
    val label: String,
    val background: Color,
    val foreground: Color
)

private data class AwardCardData(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val earnedDate: LocalDate?,
    val icon: ImageVector
)

private data class AnalyticsSnapshot(
    val avgScore: Int,
    val tasksDone: Int,
    val skillChanges: List<SkillDelta>,
    val summary: String
)

private data class SkillDelta(
    val skill: SkillType,
    val deltaPercent: Float
)

private data class WeeklyMetrics(
    val days: List<WeeklyDayProgress>,
    val totalMinutes: Int,
    val averageMinutes: Int
)

private enum class SkillType(
    val displayName: String,
    val icon: ImageVector,
    val targetPoints: Int,
    val containerColor: Color,
    val badgeColor: Color,
    val badgeContentColor: Color,
    val taskCategory: TaskCategory
) {
    GRAMMAR(
        displayName = "Dilbilgisi",
        icon = Icons.Outlined.Article,
        targetPoints = 1500,
        containerColor = DesignTokens.PrimaryContainer.copy(alpha = 0.55f),
        badgeColor = DesignTokens.Primary,
        badgeContentColor = DesignTokens.PrimaryForeground,
        taskCategory = TaskCategory.GRAMMAR
    ),
    READING(
        displayName = "Okuma",
        icon = Icons.Outlined.MenuBook,
        targetPoints = 1350,
        containerColor = DesignTokens.SecondaryContainer.copy(alpha = 0.6f),
        badgeColor = DesignTokens.Secondary,
        badgeContentColor = DesignTokens.SecondaryForeground,
        taskCategory = TaskCategory.READING
    ),
    LISTENING(
        displayName = "Dinleme",
        icon = Icons.Outlined.Headset,
        targetPoints = 1300,
        containerColor = DesignTokens.TertiaryContainer.copy(alpha = 0.6f),
        badgeColor = DesignTokens.Tertiary,
        badgeContentColor = DesignTokens.TertiaryContainerForeground,
        taskCategory = TaskCategory.LISTENING
    ),
    VOCABULARY(
        displayName = "Kelime",
        icon = Icons.Outlined.Translate,
        targetPoints = 1550,
        containerColor = DesignTokens.SurfaceContainer,
        badgeColor = DesignTokens.Success,
        badgeContentColor = DesignTokens.PrimaryForeground,
        taskCategory = TaskCategory.VOCABULARY
    )
}

@Composable
fun ProgressScreen(
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel? = null,
    realTimeUpdateManager: RealTimeUpdateManager? = null,
    navigationManager: StudyPlanNavigationManager? = null
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext as Context }
    val dataStore = remember(appContext) { appContext.dataStore }
    val repository = remember(dataStore) { ProgressRepository(dataStore) }
    val streakManager = remember(repository) { StreakManager(repository) }
    val achievementTracker = remember(dataStore, repository) { AchievementTracker(dataStore, repository) }

    // Use ViewModel with error handling
    val viewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModelFactory(
            progressRepository = repository,
            planRepository = repository, // Assuming repository implements both interfaces
            achievementTracker = achievementTracker,
            streakManager = streakManager,
            context = appContext
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val globalError by viewModel.globalError.collectAsState()

    // Collect SharedViewModel state for real-time updates
    val sharedProgress by if (sharedViewModel != null) {
        sharedViewModel.userProgress.collectAsState()
    } else {
        remember { mutableStateOf(com.mtlc.studyplan.data.UserProgress()) }
    }

    val sharedStudyStats by if (sharedViewModel != null) {
        sharedViewModel.studyStats.collectAsState()
    } else {
        remember { mutableStateOf(com.mtlc.studyplan.shared.StudyStats()) }
    }

    val sharedCurrentStreak by if (sharedViewModel != null) {
        sharedViewModel.currentStreak.collectAsState()
    } else {
        remember { mutableStateOf(0) }
    }

    val sharedAchievements by if (sharedViewModel != null) {
        sharedViewModel.achievements.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<com.mtlc.studyplan.data.Achievement>()) }
    }

    // Real-time update states
    var animatingProgress by remember { mutableStateOf(false) }
    var animatingPoints by remember { mutableStateOf(false) }
    var animatingStreak by remember { mutableStateOf(false) }
    var animatingAchievements by remember { mutableStateOf(false) }
    val deepLinkParams by navigationManager?.deepLinkParams?.collectAsState() ?: remember { mutableStateOf(null) }

    // Handle deep link parameters for navigation
    LaunchedEffect(deepLinkParams) {
        deepLinkParams?.let { params ->
            params.progressTimeRange?.let { timeRange ->
                // Handle navigation to specific progress view
                // Could scroll to specific chart or highlight element
            }
            params.highlightElement?.let { element ->
                // Highlight specific UI element
                when (element) {
                    "daily_progress" -> animatingProgress = true
                    "streak_chart" -> animatingStreak = true
                    "today_summary" -> animatingProgress = true
                }
            }
        }
    }

    // Real-time progress updates
    LaunchedEffect(realTimeUpdateManager) {
        realTimeUpdateManager?.progressUpdates?.collect { update ->
            when (update.type) {
                ProgressUpdateType.TASK_COMPLETED -> {
                    animatingProgress = true
                    animatingPoints = true
                    kotlinx.coroutines.delay(1500)
                    animatingProgress = false
                    animatingPoints = false
                }
                ProgressUpdateType.DAILY_SUMMARY -> {
                    animatingProgress = true
                    kotlinx.coroutines.delay(1000)
                    animatingProgress = false
                }
                ProgressUpdateType.MILESTONE_REACHED -> {
                    animatingProgress = true
                    animatingAchievements = true
                    kotlinx.coroutines.delay(2000)
                    animatingProgress = false
                    animatingAchievements = false
                }
                else -> {}
            }
        }
    }

    // Real-time streak updates
    LaunchedEffect(realTimeUpdateManager) {
        realTimeUpdateManager?.streakUpdates?.collect { update ->
            if (update.isExtension) {
                animatingStreak = true
                kotlinx.coroutines.delay(1500)
                animatingStreak = false
            }
        }
    }

    // Real-time achievement updates
    LaunchedEffect(realTimeUpdateManager) {
        realTimeUpdateManager?.achievementUpdates?.collect { update ->
            animatingAchievements = true
            kotlinx.coroutines.delay(2000)
            animatingAchievements = false
        }
    }

    // Real-time points updates
    LaunchedEffect(realTimeUpdateManager) {
        realTimeUpdateManager?.pointsUpdates?.collect { update ->
            animatingPoints = true
            kotlinx.coroutines.delay(1000)
            animatingPoints = false
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
                androidx.compose.material3.CircularProgressIndicator()
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
                emptyTitle = "Henüz İlerleme Yok",
                emptyMessage = "Çalışmaya başlayarak ilerleme kaydetmeye başla",
                onRetry = if (uiState.error != null) { { viewModel.retry() } } else null,
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            // Use SharedViewModel data if available, fallback to local ViewModel
            val legacyUiState = if (sharedViewModel != null) {
                convertSharedDataToLegacyUiState(sharedStudyStats, sharedProgress, sharedAchievements, sharedCurrentStreak)
            } else {
                convertToLegacyUiState(uiState)
            }

            ProgressScreenContent(
                uiState = legacyUiState,
                streakData = uiState.streakData,
                sharedStudyStats = sharedStudyStats,
                sharedCurrentStreak = sharedCurrentStreak,
                animatingProgress = animatingProgress,
                animatingPoints = animatingPoints,
                animatingStreak = animatingStreak,
                animatingAchievements = animatingAchievements,
                navigationManager = navigationManager,
                onRefresh = { viewModel.refresh() },
                onResetProgress = { viewModel.resetProgress() },
                onExportProgress = { format -> viewModel.exportProgress(format) },
                onRecalculateStats = { viewModel.recalculateStats() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.md)
            )
        }
    }
}

@Composable
private fun ProgressScreenContent(
    uiState: ProgressUiState,
    streakData: com.mtlc.studyplan.feature.progress.viewmodel.StreakData?,
    sharedStudyStats: com.mtlc.studyplan.shared.StudyStats,
    sharedCurrentStreak: Int,
    animatingProgress: Boolean,
    animatingPoints: Boolean,
    animatingStreak: Boolean,
    animatingAchievements: Boolean,
    navigationManager: StudyPlanNavigationManager?,
    onRefresh: () -> Unit,
    onResetProgress: () -> Unit,
    onExportProgress: (String) -> Unit,
    onRecalculateStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val scrollState = rememberScrollState()
    var selectedTab by rememberSaveable { mutableStateOf(ProgressTab.OVERVIEW) }
    var selectedRange by rememberSaveable { mutableStateOf(AnalyticsRange.LAST_7_DAYS) }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        ProgressHeader(uiState)
        ProgressTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        when (selectedTab) {
            ProgressTab.OVERVIEW -> OverviewTabContent(
                overview = uiState.overview,
                animatingProgress = animatingProgress,
                animatingPoints = animatingPoints,
                navigationManager = navigationManager
            )
            ProgressTab.SKILLS -> SkillsTabContent(
                skills = uiState.skills,
                animatingProgress = animatingProgress
            )
            ProgressTab.AWARDS -> AwardsTabContent(
                awards = uiState.awards,
                animatingAchievements = animatingAchievements,
                navigationManager = navigationManager
            )
            ProgressTab.ANALYTICS -> AnalyticsTabContent(
                selectedRange = selectedRange,
                onRangeSelected = { selectedRange = it },
                analytics = uiState.analytics,
                animatingStreak = animatingStreak,
                streakState = streakData?.let { data ->
                    StreakState(
                        currentStreak = data.currentStreak,
                        multiplier = StreakMultiplier.getMultiplierForStreak(data.currentStreak),
                        isInDanger = data.isInDanger,
                        hoursUntilBreak = data.hoursUntilBreak,
                        lastActivityDate = data.lastActivityTimestamp
                    )
                }
            )
        }
    }
}

@Composable
private fun ProgressHeader(uiState: ProgressUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "İlerlemen",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(LocalSpacing.current.xs))
        Text(
            text = "YDS hazırlığını yakından takip et",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProgressTabRow(
    selectedTab: ProgressTab,
    onTabSelected: (ProgressTab) -> Unit
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignTokens.Surface,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            ProgressTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val background by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.PrimaryContainer else Color.Transparent,
                    animationSpec = tween(200),
                    label = "tab_background"
                )
                val contentColor = if (isSelected) DesignTokens.PrimaryContainerForeground else MaterialTheme.colorScheme.onSurface

                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(24.dp),
                    color = background,
                    contentColor = contentColor
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = spacing.md, vertical = spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    overview: OverviewData,
    animatingProgress: Boolean,
    animatingPoints: Boolean,
    navigationManager: StudyPlanNavigationManager?
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        AnimatedOverviewSummaryRow(
            summaries = overview.summaryCards,
            animatingPoints = animatingPoints,
            navigationManager = navigationManager
        )
        AnimatedWeeklyProgressCard(
            days = overview.weeklyProgress,
            animatingProgress = animatingProgress,
            navigationManager = navigationManager
        )
        StudyTimeCard(overview.studyTime)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewSummaryRow(summaries: List<OverviewSummary>) {
    val spacing = LocalSpacing.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        summaries.forEach { summary ->
            Surface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                color = summary.color,
                contentColor = summary.contentColor,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = summary.contentColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = summary.value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyProgressCard(days: List<WeeklyDayProgress>) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignTokens.Surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.TrendingUp,
                    contentDescription = null,
                    tint = DesignTokens.Primary
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = "Haftalık İlerleme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                days.forEach { day ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.dayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = day.percentLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DesignTokens.SurfaceContainerHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(day.percent.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(DesignTokens.Primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyTimeCard(studyTime: StudyTimeStats) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignTokens.PrimaryContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = null,
                    tint = DesignTokens.PrimaryContainerForeground
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = "Çalışma Süresi",
                    color = DesignTokens.PrimaryContainerForeground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Bu hafta",
                color = DesignTokens.PrimaryContainerForeground.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Toplam",
                        color = DesignTokens.PrimaryContainerForeground.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = formatMinutes(studyTime.totalMinutes),
                        color = DesignTokens.PrimaryContainerForeground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Günlük Ort.",
                        color = DesignTokens.PrimaryContainerForeground.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = formatMinutes(studyTime.averageMinutes),
                        color = DesignTokens.PrimaryContainerForeground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillsTabContent(
    skills: List<SkillCardData>,
    animatingProgress: Boolean
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        skills.forEach { skill ->
            AnimatedSkillProgressCard(
                skill = skill,
                isAnimating = animatingProgress
            )
        }
    }
}

@Composable
private fun SkillProgressCard(skill: SkillCardData) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = skill.type.containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = DesignTokens.Surface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(
                            imageVector = skill.type.icon,
                            contentDescription = null,
                            tint = DesignTokens.Primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = skill.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = skill.pointsLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = skill.level.background,
                    contentColor = skill.level.foreground
                ) {
                    Text(
                        text = skill.level.label,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DesignTokens.Surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(skill.percent.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(DesignTokens.Primary)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = skill.percentLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = skill.level.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AwardsTabContent(
    awards: List<AwardCardData>,
    animatingAchievements: Boolean,
    navigationManager: StudyPlanNavigationManager?
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        awards.forEach { award ->
            AnimatedAwardCard(
                award = award,
                isAnimating = animatingAchievements && award.isUnlocked,
                onClick = {
                    navigationManager?.navigateToSocial(
                        tab = com.mtlc.studyplan.navigation.SocialTab.ACHIEVEMENTS,
                        achievementId = award.id,
                        fromScreen = "progress"
                    )
                }
            )
        }
    }
}

@Composable
private fun AwardCard(award: AwardCardData) {
    val spacing = LocalSpacing.current
    val surfaceColor = if (award.isUnlocked) DesignTokens.SuccessContainer else DesignTokens.Surface
    val iconTint = if (award.isUnlocked) DesignTokens.Success else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (award.isUnlocked) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = DesignTokens.Surface,
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        imageVector = award.icon,
                        contentDescription = null,
                        tint = iconTint
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = award.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = award.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                val dateText = award.earnedDate?.let { formatDate(it) } ?: "Henüz kilit açılmadı"
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            androidx.compose.material3.Icon(
                imageVector = if (award.isUnlocked) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

@Composable
private fun AnalyticsTabContent(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit,
    analytics: Map<AnalyticsRange, AnalyticsSnapshot>,
    animatingStreak: Boolean,
    streakState: StreakState?
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        AnalyticsRangeRow(selected = selectedRange, onRangeSelected = onRangeSelected)
        val snapshot = analytics[selectedRange] ?: analytics.values.firstOrNull()
        if (snapshot != null) {
            AnalyticsSummaryCard(range = selectedRange, snapshot = snapshot)
        }
        AnimatedWeeklyStreakCard(
            streakState = streakState,
            isAnimating = animatingStreak
        )
    }
}

@Composable
private fun AnalyticsRangeRow(
    selected: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignTokens.Surface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            AnalyticsRange.values().forEach { range ->
                val isSelected = range == selected
                val background by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.PrimaryContainer else Color.Transparent,
                    animationSpec = tween(200),
                    label = "analytics_range"
                )
                val contentColor = if (isSelected) DesignTokens.PrimaryContainerForeground else MaterialTheme.colorScheme.onSurface
                Surface(
                    onClick = { onRangeSelected(range) },
                    shape = RoundedCornerShape(18.dp),
                    color = background,
                    contentColor = contentColor
                ) {
                    Text(
                        text = range.label,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsSummaryCard(
    range: AnalyticsRange,
    snapshot: AnalyticsSnapshot
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignTokens.PrimaryContainer.copy(alpha = 0.65f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Insights,
                    contentDescription = null,
                    tint = DesignTokens.PrimaryContainerForeground
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Column {
                    Text(
                        text = when (range) {
                            AnalyticsRange.LAST_7_DAYS -> "Son 7 Gün Analizi"
                            AnalyticsRange.LAST_30_DAYS -> "30 Günlük Analiz"
                            AnalyticsRange.ALL_TIME -> "Tüm Zamanlar"
                            AnalyticsRange.PERFORMANCE -> "Performans Trendleri"
                            AnalyticsRange.AI_INSIGHTS -> "YZ İçgörüleri"
                        },
                        color = DesignTokens.PrimaryContainerForeground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = range.description,
                        color = DesignTokens.PrimaryContainerForeground.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Ortalama Skor",
                        color = DesignTokens.PrimaryContainerForeground.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${snapshot.avgScore}%",
                        color = DesignTokens.PrimaryContainerForeground,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tamamlanan Görev",
                        color = DesignTokens.PrimaryContainerForeground.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = NumberFormat.getIntegerInstance(turkishLocale).format(snapshot.tasksDone),
                        color = DesignTokens.PrimaryContainerForeground,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                snapshot.skillChanges.forEach { change ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = change.skill.displayName,
                            color = DesignTokens.PrimaryContainerForeground,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        val sign = if (change.deltaPercent >= 0f) "+" else ""
                        val tint = if (change.deltaPercent >= 0f) DesignTokens.Success else DesignTokens.Warning
                        Text(
                            text = "$sign${change.deltaPercent.roundToInt()}%",
                            color = tint,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DesignTokens.Surface)
                    ) {
                        val width = change.deltaPercent.coerceIn(-100f, 100f) / 100f
                        val progressWidth = width.coerceIn(-1f, 1f)
                        val barColor = if (progressWidth >= 0f) DesignTokens.Success else DesignTokens.Warning
                        val barFraction = kotlin.math.abs(progressWidth)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .fillMaxHeight()
                                .background(barColor)
                        )
                    }
                }
            }
            Text(
                text = snapshot.summary,
                color = DesignTokens.PrimaryContainerForeground,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun WeeklyStreakCard(streakState: StreakState?) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DesignTokens.SuccessContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = DesignTokens.Surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Whatshot,
                        contentDescription = null,
                        tint = DesignTokens.Success
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Haftalık Seri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val streakValue = streakState?.currentStreak ?: 0
                val message = if (streakValue > 0) {
                    "Harikasın! ${streakValue} gündür serin sürüyor."
                } else {
                    "Serini başlatmak için bugün bir görev tamamla."
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }
    }
}
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ProgressScreenPreview() {
    val sampleState = sampleProgressUiState()
    val streak = StreakState(
        currentStreak = 7,
        multiplier = StreakMultiplier.getMultiplierForStreak(7),
        isInDanger = false,
        hoursUntilBreak = 12,
        lastActivityDate = Instant.now().toEpochMilli()
    )
    ProgressScreenContent(uiState = sampleState, streakState = streak)
}

private fun sampleProgressUiState(): ProgressUiState {
    val overview = OverviewData(
        summaryCards = listOf(
            OverviewSummary("Genel", "79%", DesignTokens.PrimaryContainer, DesignTokens.PrimaryContainerForeground),
            OverviewSummary("Puan", "4.540", DesignTokens.SecondaryContainer, DesignTokens.SecondaryContainerForeground),
            OverviewSummary("Ödül", "2", DesignTokens.TertiaryContainer, DesignTokens.TertiaryContainerForeground)
        ),
        weeklyProgress = listOf(0.85f, 0.92f, 0.78f, 0.95f, 0.88f, 0.4f, 0f).mapIndexed { index, value ->
            WeeklyDayProgress(
                dayLabel = listOf("Pzt", "Sal", "Çrş", "Prş", "Cum", "Cmt", "Paz")[index],
                percent = value,
                percentLabel = "${(value * 100).roundToInt()}%",
                isToday = index == 4
            )
        },
        studyTime = StudyTimeStats(totalMinutes = 750, averageMinutes = 107)
    )

    val skills = SkillType.values().mapIndexed { idx, type ->
        val percent = listOf(0.83f, 0.72f, 0.68f, 0.91f)[idx]
        SkillCardData(
            type = type,
            pointsLabel = "${(percent * type.targetPoints).roundToInt()}/${type.targetPoints} puan",
            percent = percent,
            percentLabel = "${(percent * 100).roundToInt()}%",
            level = levelForPercent(percent, type)
        )
    }

    val awards = listOf(
        AwardCardData(
            id = "first_steps",
            title = "İlk Adım",
            description = "İlk görevi tamamladın",
            isUnlocked = true,
            earnedDate = LocalDate.now().minusDays(3),
            icon = Icons.Outlined.EmojiEvents
        ),
        AwardCardData(
            id = "week_warrior",
            title = "Hafta Savaşçısı",
            description = "7 günlük seri",
            isUnlocked = true,
            earnedDate = LocalDate.now().minusDays(1),
            icon = Icons.Outlined.EmojiEvents
        ),
        AwardCardData(
            id = "grammar_master",
            title = "Dilbilgisi Ustası",
            description = "%90 başarıya ulaş",
            isUnlocked = false,
            earnedDate = null,
            icon = Icons.Outlined.EmojiEvents
        ),
        AwardCardData(
            id = "speed_reader",
            title = "Hızlı Okur",
            description = "50 makale oku",
            isUnlocked = false,
            earnedDate = null,
            icon = Icons.Outlined.EmojiEvents
        )
    )

    val analytics = AnalyticsRange.values().associateWith { range ->
        AnalyticsSnapshot(
            avgScore = 87,
            tasksDone = 42,
            skillChanges = SkillType.values().mapIndexed { index, type ->
                SkillDelta(type, listOf(12f, 8f, 15f, 6f)[index])
            },
            summary = "Analiz özeti (${range.label})."
        )
    }

    return ProgressUiState(
        overallPercent = 79,
        totalPoints = 4540,
        awardsCount = 2,
        overview = overview,
        skills = skills,
        awards = awards,
        analytics = analytics
    )
}

private fun buildProgressUiState(
    userProgress: UserProgress,
    taskLogs: List<TaskLog>,
    streakState: StreakState?,
    achievementState: AchievementState?
): ProgressUiState {
    val overallPercent = computeOverallPercent(userProgress, achievementState)
    val totalPoints = userProgress.totalPoints
    val awardsCount = achievementState?.unlockedAchievements?.size ?: userProgress.unlockedAchievements.size
    val weeklyMetrics = computeWeeklyMetrics(taskLogs)

    val overview = OverviewData(
        summaryCards = listOf(
            OverviewSummary(
                "Genel",
                "${overallPercent}%",
                DesignTokens.PrimaryContainer,
                DesignTokens.PrimaryContainerForeground
            ),
            OverviewSummary(
                "Puan",
                NumberFormat.getIntegerInstance(turkishLocale).format(totalPoints),
                DesignTokens.SecondaryContainer,
                DesignTokens.SecondaryContainerForeground
            ),
            OverviewSummary(
                "Ödül",
                NumberFormat.getIntegerInstance(turkishLocale).format(awardsCount),
                DesignTokens.TertiaryContainer,
                DesignTokens.TertiaryContainerForeground
            )
        ),
        weeklyProgress = weeklyMetrics.days,
        studyTime = StudyTimeStats(
            totalMinutes = weeklyMetrics.totalMinutes,
            averageMinutes = weeklyMetrics.averageMinutes
        )
    )

    val skills = computeSkillCards(taskLogs)
    val awards = buildAwardCards(achievementState, userProgress, streakState)
    val analytics = buildAnalyticsSnapshots(taskLogs)

    return ProgressUiState(
        overallPercent = overallPercent,
        totalPoints = totalPoints,
        awardsCount = awardsCount,
        overview = overview,
        skills = skills,
        awards = awards,
        analytics = analytics
    )
}

private fun computeOverallPercent(
    userProgress: UserProgress,
    achievementState: AchievementState?
): Int {
    val fromAchievements = achievementState?.categoryProgress?.values
        ?.maxOfOrNull { (it.completionPercentage * 100).roundToInt() }
    if (fromAchievements != null) {
        return fromAchievements.coerceIn(0, 100)
    }
    val estimatedTotalTasks = 600f
    if (estimatedTotalTasks <= 0f) return 0
    return ((userProgress.completedTasks.size / estimatedTotalTasks) * 100f).roundToInt().coerceIn(0, 100)
}

private fun computeWeeklyMetrics(taskLogs: List<TaskLog>): WeeklyMetrics {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val days = (0 until 7).map { offset ->
        val date = today.minusDays((6 - offset).toLong())
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val logsForDay = taskLogs.filter { it.timestampMillis in start until end }
        val minutes = logsForDay.sumOf { it.minutesSpent.coerceAtLeast(20) }
        val percent = (minutes / 120f).coerceIn(0f, 1f)
        WeeklyDayProgress(
            dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, turkishLocale),
            percent = percent,
            percentLabel = "${(percent * 100).roundToInt()}%",
            isToday = date == today
        )
    }
    val totalMinutes = days.sumOf { (it.percent * 120).roundToInt() }
    val average = if (days.isNotEmpty()) totalMinutes / days.size else 0
    return WeeklyMetrics(days, totalMinutes, average)
}

private fun computeSkillCards(taskLogs: List<TaskLog>): List<SkillCardData> {
    return SkillType.values().map { skill ->
        val logs = taskLogs.filter { TaskCategory.fromString(it.category) == skill.taskCategory }
        val points = logs.sumOf { it.effectivePoints(skill.taskCategory) }
        val percent = if (skill.targetPoints > 0) (points / skill.targetPoints.toFloat()).coerceIn(0f, 1f) else 0f
        val percentLabel = "${(percent * 100).roundToInt()}%"
        val pointsLabel = "${NumberFormat.getIntegerInstance(turkishLocale).format(points)}/${NumberFormat.getIntegerInstance(turkishLocale).format(skill.targetPoints)} puan"
        SkillCardData(
            type = skill,
            pointsLabel = pointsLabel,
            percent = percent,
            percentLabel = percentLabel,
            level = levelForPercent(percent, skill)
        )
    }
}

private fun levelForPercent(percent: Float, skill: SkillType): SkillLevelInfo {
    return when {
        percent >= 0.9f -> SkillLevelInfo(
            label = "Uzman",
            background = DesignTokens.Success,
            foreground = DesignTokens.PrimaryForeground
        )
        percent >= 0.75f -> SkillLevelInfo(
            label = "İleri",
            background = skill.badgeColor,
            foreground = skill.badgeContentColor
        )
        percent >= 0.5f -> SkillLevelInfo(
            label = "Orta",
            background = DesignTokens.Tertiary,
            foreground = DesignTokens.PrimaryForeground
        )
        else -> SkillLevelInfo(
            label = "Başlangıç",
            background = DesignTokens.SurfaceVariant,
            foreground = DesignTokens.CardForeground
        )
    }
}

private fun buildAwardCards(
    achievementState: AchievementState?,
    userProgress: UserProgress,
    streakState: StreakState?
): List<AwardCardData> {
    val unlocked = achievementState?.unlockedAchievements ?: userProgress.unlockedAchievements
    val achievements = achievementState?.categoryProgress?.values
        ?.flatMap { it.achievements }
        ?.distinctBy { it.id }
        ?: CategorizedAchievementDataSource.allCategorizedAchievements

    val mostRecentDate = streakState?.lastActivityDate?.takeIf { it > 0 }
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

    return achievements
        .sortedBy { it.sortOrder }
        .take(8)
        .map { achievement ->
            val isUnlocked = unlocked.contains(achievement.id)
            AwardCardData(
                id = achievement.id,
                title = achievement.title,
                description = achievement.description,
                isUnlocked = isUnlocked,
                earnedDate = if (isUnlocked) mostRecentDate else null,
                icon = Icons.Outlined.EmojiEvents
            )
        }
}

private fun buildAnalyticsSnapshots(taskLogs: List<TaskLog>): Map<AnalyticsRange, AnalyticsSnapshot> {
    fun logsForRange(days: Long?): List<TaskLog> {
        if (days == null) return taskLogs
        val cutoff = Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli()
        return taskLogs.filter { it.timestampMillis >= cutoff }
    }

    return AnalyticsRange.values().associateWith { range ->
        val currentLogs = logsForRange(range.days)
        val avgScore = if (currentLogs.isNotEmpty()) {
            val correct = currentLogs.count { it.correct }
            ((correct.toFloat() / currentLogs.size.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else 0
        val tasksDone = currentLogs.size

        val skillDeltas = SkillType.values().map { skill ->
            val currentPoints = currentLogs
                .filter { TaskCategory.fromString(it.category) == skill.taskCategory }
                .sumOf { it.effectivePoints(skill.taskCategory) }
            val previousPoints = range.days?.let { days ->
                val end = Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli()
                val start = Instant.now().minus(days * 2, ChronoUnit.DAYS).toEpochMilli()
                taskLogs
                    .filter { it.timestampMillis in start until end }
                    .filter { TaskCategory.fromString(it.category) == skill.taskCategory }
                    .sumOf { it.effectivePoints(skill.taskCategory) }
            } ?: 0

            val delta = when {
                range.days == null -> 0f
                previousPoints == 0 -> if (currentPoints > 0) 100f else 0f
                else -> ((currentPoints - previousPoints) / previousPoints.toFloat()) * 100f
            }
            SkillDelta(skill, delta)
        }

        val summary = when (range) {
            AnalyticsRange.LAST_7_DAYS -> "Son 7 günde dengeli bir performans sergiledin."
            AnalyticsRange.LAST_30_DAYS -> "30 günlük trendinde istikrarlı bir yükseliş var."
            AnalyticsRange.ALL_TIME -> "Program boyunca topladığın tüm veriler burada."
            AnalyticsRange.PERFORMANCE -> "Performans hızın artıyor, aynen devam!"
            AnalyticsRange.AI_INSIGHTS -> "YZ analizine göre güçlü alanlarını koruyorsun."
        }

        AnalyticsSnapshot(
            avgScore = avgScore,
            tasksDone = tasksDone,
            skillChanges = skillDeltas,
            summary = summary
        )
    }
}

private fun TaskLog.effectivePoints(category: TaskCategory): Int {
    return if (pointsEarned > 0) pointsEarned else category.basePoints
}

private fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0 sa"
    val hours = minutes / 60
    val mins = minutes % 60
    return buildString {
        if (hours > 0) append("${hours} sa ")
        if (mins > 0) append("${mins} dk")
    }.trim()
}

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", turkishLocale)
    return date.format(formatter)
}

// Convert ViewModel UI state to legacy format for existing UI components
private fun convertToLegacyUiState(
    viewModelUiState: com.mtlc.studyplan.feature.progress.viewmodel.ProgressUiState
): ProgressUiState {
    val userProgress = viewModelUiState.userProgress ?: UserProgress()

    // Build overview data
    val overview = OverviewData(
        summaryCards = listOf(
            OverviewSummary(
                "Genel",
                "${(viewModelUiState.completionRate * 100).toInt()}%",
                DesignTokens.PrimaryContainer,
                DesignTokens.PrimaryContainerForeground
            ),
            OverviewSummary(
                "Puan",
                NumberFormat.getIntegerInstance(turkishLocale).format(viewModelUiState.xpEarned),
                DesignTokens.SecondaryContainer,
                DesignTokens.SecondaryContainerForeground
            ),
            OverviewSummary(
                "Ödül",
                NumberFormat.getIntegerInstance(turkishLocale).format(viewModelUiState.achievements.size),
                DesignTokens.TertiaryContainer,
                DesignTokens.TertiaryContainerForeground
            )
        ),
        weeklyProgress = emptyList(), // Would need weekly stats conversion
        studyTime = StudyTimeStats(
            totalMinutes = viewModelUiState.studyTimeThisWeek,
            averageMinutes = viewModelUiState.studyTimeThisWeek / 7
        )
    )

    // Convert achievements to award cards
    val awards = viewModelUiState.achievements.map { achievement ->
        AwardCardData(
            id = achievement.id,
            title = achievement.title,
            description = achievement.description,
            isUnlocked = achievement.isUnlocked,
            earnedDate = achievement.unlockedDate,
            icon = Icons.Outlined.EmojiEvents
        )
    }

    return ProgressUiState(
        overallPercent = (viewModelUiState.completionRate * 100).toInt(),
        totalPoints = viewModelUiState.xpEarned,
        awardsCount = viewModelUiState.achievements.count { it.isUnlocked },
        overview = overview,
        skills = emptyList(), // Would need skill conversion
        awards = awards,
        analytics = emptyMap() // Would need analytics conversion
    )
}

// Convert SharedViewModel data to legacy format for real-time updates
private fun convertSharedDataToLegacyUiState(
    sharedStudyStats: com.mtlc.studyplan.shared.StudyStats,
    sharedProgress: com.mtlc.studyplan.data.UserProgress,
    sharedAchievements: List<com.mtlc.studyplan.data.Achievement>,
    sharedCurrentStreak: Int
): ProgressUiState {
    // Build overview data using SharedViewModel stats
    val overview = OverviewData(
        summaryCards = listOf(
            OverviewSummary(
                "Genel",
                "${((sharedStudyStats.totalTasksCompleted / 100.0) * 100).toInt()}%",
                DesignTokens.PrimaryContainer,
                DesignTokens.PrimaryContainerForeground
            ),
            OverviewSummary(
                "Puan",
                NumberFormat.getIntegerInstance(turkishLocale).format(sharedStudyStats.totalXP),
                DesignTokens.SecondaryContainer,
                DesignTokens.SecondaryContainerForeground
            ),
            OverviewSummary(
                "Ödül",
                NumberFormat.getIntegerInstance(turkishLocale).format(sharedAchievements.size),
                DesignTokens.TertiaryContainer,
                DesignTokens.TertiaryContainerForeground
            )
        ),
        weeklyProgress = emptyList(), // Could build from SharedViewModel weekly data
        studyTime = StudyTimeStats(
            totalMinutes = sharedStudyStats.thisWeekStudyTime,
            averageMinutes = sharedStudyStats.averageSessionTime
        )
    )

    // Convert achievements to award cards using shared data
    val awards = sharedAchievements.map { achievement ->
        AwardCardData(
            id = achievement.id,
            title = achievement.title,
            description = achievement.description,
            isUnlocked = achievement.isUnlocked,
            earnedDate = achievement.unlockedDate,
            icon = Icons.Outlined.EmojiEvents
        )
    }

    // Build basic skills from shared data
    val skills = SkillType.values().map { skillType ->
        val progress = 0.5f // Default progress, could calculate from shared stats
        SkillCardData(
            type = skillType,
            pointsLabel = "${(progress * skillType.targetPoints).toInt()}/${skillType.targetPoints} puan",
            percent = progress,
            percentLabel = "${(progress * 100).toInt()}%",
            level = levelForPercent(progress, skillType)
        )
    }

    return ProgressUiState(
        overallPercent = ((sharedStudyStats.totalTasksCompleted / 100.0) * 100).toInt().coerceIn(0, 100),
        totalPoints = sharedStudyStats.totalXP,
        awardsCount = sharedAchievements.size,
        overview = overview,
        skills = skills,
        awards = awards,
        analytics = emptyMap() // Could build analytics from shared stats
    )
}

// Animated components for real-time updates
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimatedOverviewSummaryRow(
    summaries: List<OverviewSummary>,
    animatingPoints: Boolean,
    navigationManager: StudyPlanNavigationManager?
) {
    val spacing = LocalSpacing.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        summaries.forEachIndexed { index, summary ->
            val isPointsCard = summary.title == "Puan"
            val scale by animateFloatAsState(
                targetValue = if (animatingPoints && isPointsCard) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )

            Surface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .scale(scale)
                    .clickable {
                        navigationManager?.navigateToProgress(
                            timeRange = when (summary.title) {
                                "Genel" -> TimeRange.TODAY
                                "Puan" -> TimeRange.WEEK
                                else -> TimeRange.ALL_TIME
                            },
                            highlightElement = summary.title.lowercase(),
                            fromScreen = "progress"
                        )
                    },
                color = summary.color,
                contentColor = summary.contentColor,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = if (animatingPoints && isPointsCard) 8.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = summary.contentColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = summary.value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (animatingPoints && isPointsCard)
                            MaterialTheme.colorScheme.primary
                        else summary.contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedWeeklyProgressCard(
    days: List<WeeklyDayProgress>,
    animatingProgress: Boolean,
    navigationManager: StudyPlanNavigationManager?
) {
    val spacing = LocalSpacing.current
    val scale by animateFloatAsState(
        targetValue = if (animatingProgress) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                navigationManager?.navigateToProgress(
                    timeRange = TimeRange.WEEK,
                    highlightElement = "weekly_chart",
                    fromScreen = "progress"
                )
            },
        color = DesignTokens.Surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (animatingProgress) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.TrendingUp,
                    contentDescription = null,
                    tint = if (animatingProgress) MaterialTheme.colorScheme.primary else DesignTokens.Primary
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = "Haftalık İlerleme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (animatingProgress) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                days.forEach { day ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.dayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = day.percentLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DesignTokens.SurfaceContainerHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(day.percent.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(
                                        if (animatingProgress && day.isToday)
                                            MaterialTheme.colorScheme.primary
                                        else DesignTokens.Primary
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSkillProgressCard(
    skill: SkillCardData,
    isAnimating: Boolean
) {
    val spacing = LocalSpacing.current
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        color = skill.type.containerColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (isAnimating) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = DesignTokens.Surface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(
                            imageVector = skill.type.icon,
                            contentDescription = null,
                            tint = if (isAnimating) MaterialTheme.colorScheme.primary else DesignTokens.Primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = skill.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = skill.pointsLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = skill.level.background,
                    contentColor = skill.level.foreground
                ) {
                    Text(
                        text = skill.level.label,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DesignTokens.Surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(skill.percent.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            if (isAnimating) MaterialTheme.colorScheme.primary else DesignTokens.Primary
                        )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = skill.percentLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isAnimating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = skill.level.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AnimatedAwardCard(
    award: AwardCardData,
    isAnimating: Boolean,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    val surfaceColor = if (award.isUnlocked) DesignTokens.SuccessContainer else DesignTokens.Surface
    val iconTint = if (award.isUnlocked) DesignTokens.Success else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() },
        color = surfaceColor,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (award.isUnlocked) 2.dp else 0.dp,
        shadowElevation = if (isAnimating) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = DesignTokens.Surface,
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        imageVector = award.icon,
                        contentDescription = null,
                        tint = if (isAnimating) MaterialTheme.colorScheme.primary else iconTint
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = award.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isAnimating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = award.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                val dateText = award.earnedDate?.let { formatDate(it) } ?: "Henüz kilit açılmadı"
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            androidx.compose.material3.Icon(
                imageVector = if (award.isUnlocked) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (isAnimating && award.isUnlocked) MaterialTheme.colorScheme.primary else iconTint
            )
        }
    }
}

@Composable
private fun AnimatedWeeklyStreakCard(
    streakState: StreakState?,
    isAnimating: Boolean
) {
    val spacing = LocalSpacing.current
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        color = DesignTokens.SuccessContainer,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (isAnimating) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = DesignTokens.Surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Whatshot,
                        contentDescription = null,
                        tint = if (isAnimating) MaterialTheme.colorScheme.primary else DesignTokens.Success
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Haftalık Seri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAnimating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                val streakValue = streakState?.currentStreak ?: 0
                val message = if (streakValue > 0) {
                    "Harikasın! ${streakValue} gündür serin sürüyor."
                } else {
                    "Serini başlatmak için bugün bir görev tamamla."
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
            if (isAnimating) {
                Text(
                    text = "🔥",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
