@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.ui.LoadingType
import com.mtlc.studyplan.ui.components.ShimmerOverlay
import com.mtlc.studyplan.ui.components.StudyPlanLoadingState
import kotlinx.coroutines.launch

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier
) {
    val analyticsEngine = remember { AnalyticsEngine() }
    val viewModel: AnalyticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        AnalyticsViewModel(analyticsEngine)
    }
    val analyticsData by viewModel.analyticsData.collectAsState()
    val weeklyData by viewModel.weeklyData.collectAsState()
    val performanceData by viewModel.performanceData.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    var selectedTimeframe by remember { mutableStateOf(AnalyticsTimeframe.LAST_30_DAYS) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTimeframe) {
        scope.launch {
            viewModel.loadAnalytics(selectedTimeframe)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with timeframe selector
        TimeframeSelector(
            selectedTimeframe = selectedTimeframe,
            onTimeframeChanged = { selectedTimeframe = it },
            modifier = Modifier.padding(16.dp)
        )

        // Tab selection
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth(),
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            AnalyticsTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = tab.displayName,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.displayName
                        )
                    }
                )
            }
        }

        // Content based on selected tab
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            when (selectedTab) {
                AnalyticsTab.OVERVIEW -> {
                    item {
                        OverviewSection(
                            data = analyticsData,
                            weeklyData = weeklyData
                        )
                    }
                }
                AnalyticsTab.PERFORMANCE -> {
                    item {
                        PerformanceSection(
                            data = performanceData,
                            weeklyData = weeklyData
                        )
                    }
                }
                AnalyticsTab.PATTERNS -> {
                    item {
                        PatternsSection(data = analyticsData)
                    }
                }
                AnalyticsTab.INSIGHTS -> {
                    item {
                        InsightsSection(data = analyticsData)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeframeSelector(
    selectedTimeframe: AnalyticsTimeframe,
    onTimeframeChanged: (AnalyticsTimeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFiltering by remember { mutableStateOf(false) }

    // Handle filtering delay
    LaunchedEffect(selectedTimeframe) {
        if (isFiltering) {
            kotlinx.coroutines.delay(800)
            isFiltering = false
        }
    }

    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AnalyticsTimeframe.values()) { timeframe ->
                FilterChip(
                    onClick = {
                        isFiltering = true
                        onTimeframeChanged(timeframe)
                    },
                    label = { Text(timeframe.displayName) },
                    selected = selectedTimeframe == timeframe,
                    leadingIcon = if (selectedTimeframe == timeframe) {
                        { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else null
                )
            }
        }

        // Show filtering loading state
        AnimatedVisibility(
            visible = isFiltering,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SearchLoadingState(
                query = selectedTimeframe.displayName,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun OverviewSection(
    data: AnalyticsData?,
    weeklyData: List<WeeklyAnalyticsData>,
    modifier: Modifier = Modifier
) {
    if (data == null) {
        // Enhanced loading skeleton
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics grid skeleton
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(4) {
                    MetricCardSkeleton()
                }
            }

            // Chart skeleton
            ChartSkeleton(modifier = Modifier.height(200.dp))

            // Additional skeleton cards
            ShimmerCard(modifier = Modifier.height(120.dp))
            ShimmerCard(modifier = Modifier.height(80.dp))
        }
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Key Metrics Cards
        MetricsGrid(data = data)

        // Study Progress Chart
        StudyProgressChart(
            weeklyData = weeklyData,
            modifier = Modifier.height(200.dp)
        )

        // Recent Achievements
        if (data.recentAchievements.isNotEmpty()) {
            RecentAchievementsCard(achievements = data.recentAchievements)
        }

        // Quick Stats
        QuickStatsCard(data = data)
    }
}

@Composable
fun MetricsGrid(
    data: AnalyticsData,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            MetricCard(
                title = "Study Streak",
                value = "${data.studyStreak.currentStreak}",
                subtitle = "days",
                icon = Icons.Filled.LocalFireDepartment,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            MetricCard(
                title = "Total Time",
                value = "${data.totalStudyMinutes / 60}h ${data.totalStudyMinutes % 60}m",
                subtitle = "studied",
                icon = Icons.Filled.AccessTime,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            MetricCard(
                title = "Tasks Done",
                value = "${data.completedTasks}",
                subtitle = "completed",
                icon = Icons.Filled.CheckCircle,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        item {
            MetricCard(
                title = "Avg Score",
                value = "${(data.averagePerformance * 100).toInt()}%",
                subtitle = "accuracy",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = if (data.averagePerformance > 0.8)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.width(140.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudyProgressChart(
    weeklyData: List<WeeklyAnalyticsData>,
    modifier: Modifier = Modifier
) {
    if (weeklyData.isEmpty()) {
        EmptyStateCard(
            title = "No Data Yet",
            description = "Complete some study sessions to see your progress chart",
            icon = Icons.AutoMirrored.Filled.ShowChart
        )
        return
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Study Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LineChart(
                data = weeklyData.map { it.totalMinutes.toFloat() },
                labels = weeklyData.map { "W${it.weekNumber}" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }
    }
}

@Composable
fun LineChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    animateEntry: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // Animation for staggered data entry
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(data) {
        if (animateEntry) {
            animationProgress = 0f
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            ) { value, _ ->
                animationProgress = value
            }
        } else {
            animationProgress = 1f
        }
    }

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxValue = data.maxOrNull() ?: 1f
        val minValue = data.minOrNull() ?: 0f
        val range = maxValue - minValue

        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val padding = 40.dp.toPx()

        // Draw grid lines
        val gridLineColor = surfaceVariant.copy(alpha = 0.5f)
        for (i in 0..4) {
            val y = padding + (size.height - 2 * padding) * i / 4
            drawLine(
                color = gridLineColor,
                start = Offset(padding, y),
                end = Offset(size.width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw data line with staggered animation
        if (data.size > 1) {
            val path = Path()
            val animatedData = data.mapIndexed { index, value ->
                val delay = index * 0.1f // Stagger each point by 0.1 seconds
                val pointProgress = ((animationProgress - delay) / (1f - delay)).coerceIn(0f, 1f)
                value * pointProgress
            }

            val points = animatedData.mapIndexed { index, value ->
                val x = padding + stepX * index
                val y = size.height - padding - ((value - minValue) / range.coerceAtLeast(1f)) * (size.height - 2 * padding)
                Offset(x, y)
            }

            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            // Draw gradient fill
            val gradientColors = listOf(
                primaryColor.copy(alpha = 0.3f),
                Color.Transparent
            )
            val gradientPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, size.height - padding)
                lineTo(points.first().x, size.height - padding)
                close()
            }

            drawPath(
                path = gradientPath,
                brush = Brush.verticalGradient(gradientColors)
            )

            // Draw line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw points
            points.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
        }

        // Draw labels
        labels.forEachIndexed { index, label ->
            val x = padding + stepX * index
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                size.height - 10.dp.toPx(),
                android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                    color = surfaceVariant.toArgb()
                }
            )
        }
    }
}

@Composable
fun PerformanceSection(
    data: PerformanceData?,
    weeklyData: List<WeeklyAnalyticsData>,
    modifier: Modifier = Modifier
) {
    if (data == null) {
        // Performance section skeleton
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Performance metrics skeleton
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ShimmerText(width = 0.5f, height = 18f)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(3) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ShimmerText(width = 0.8f, height = 24f)
                                Spacer(modifier = Modifier.height(4.dp))
                                ShimmerText(width = 0.6f, height = 12f)
                            }
                        }
                    }
                }
            }

            // Weak areas skeleton
            ShimmerCard(modifier = Modifier.height(150.dp))

            // Performance trends skeleton
            ChartSkeleton(modifier = Modifier.height(200.dp))
        }
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Performance Metrics
        PerformanceMetricsCard(data = data)

        // Weak Areas
        if (data.weakAreas.isNotEmpty()) {
            WeakAreasCard(weakAreas = data.weakAreas)
        }

        // Performance Trends
        PerformanceTrendsChart(weeklyData = weeklyData)
    }
}

@Composable
fun PerformanceMetricsCard(
    data: PerformanceData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance Metrics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PerformanceMetric(
                    label = "Accuracy",
                    value = "${(data.averageAccuracy * 100).toInt()}%",
                    color = getPerformanceColor(data.averageAccuracy)
                )
                PerformanceMetric(
                    label = "Speed",
                    value = "${data.averageSpeed.toInt()}s/task",
                    color = MaterialTheme.colorScheme.secondary
                )
                PerformanceMetric(
                    label = "Consistency",
                    value = "${(data.consistencyScore * 100).toInt()}%",
                    color = getPerformanceColor(data.consistencyScore)
                )
            }
        }
    }
}

@Composable
fun PerformanceMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WeakAreasCard(
    weakAreas: List<WeakArea>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Areas for Improvement",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            weakAreas.take(5).forEach { weakArea ->
                WeakAreaItem(
                    weakArea = weakArea,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WeakAreaItem(
    weakArea: WeakArea,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = weakArea.category,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = "${(weakArea.accuracy * 100).toInt()}% accuracy",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${weakArea.taskCount} tasks",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PatternsSection(
    data: AnalyticsData?,
    modifier: Modifier = Modifier
) {
    if (data == null) {
        LoadingCard()
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Distribution
        TimeDistributionCard(patterns = data.studyPatterns)

        // Productivity Insights
        ProductivityInsightsCard(insights = data.productivityInsights)

        // Best Study Times
        BestStudyTimesCard(patterns = data.studyPatterns)
    }
}

@Composable
fun InsightsSection(
    data: AnalyticsData?,
    modifier: Modifier = Modifier
) {
    if (data == null) {
        LoadingCard()
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Recommendations
        RecommendationsCard(recommendations = data.recommendations)

        // Study Habits Analysis
        StudyHabitsCard(data = data)

        // Goal Progress
        GoalProgressCard(data = data)
    }
}

@Composable
fun LoadingCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun getPerformanceColor(score: Float): Color {
    return when {
        score >= 0.9f -> MaterialTheme.colorScheme.primary
        score >= 0.7f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun SearchLoadingState(query: String, modifier: Modifier = Modifier) {
    StudyPlanLoadingState(
        isLoading = true,
        loadingType = LoadingType.SPINNER,
        message = stringResource(id = R.string.analytics_loading_results, query),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MetricCardSkeleton(modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerText(width = 0.45f, height = 14f)
            ShimmerText(width = 0.65f, height = 20f)
            ShimmerText(width = 0.35f, height = 12f)
        }
    }
}

@Composable
private fun ChartSkeleton(modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        ShimmerOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ShimmerCard(modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        ShimmerOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ShimmerText(width: Float, height: Float) {
    ShimmerOverlay(
        modifier = Modifier
            .fillMaxWidth(width.coerceIn(0f, 1f))
            .height(height.dp)
            .clip(RoundedCornerShape(6.dp))
    )
}

// Additional specialized cards would be implemented here...
// TimeDistributionCard, ProductivityInsightsCard, RecommendationsCard, etc.
