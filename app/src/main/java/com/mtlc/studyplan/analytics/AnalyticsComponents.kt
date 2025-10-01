@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import com.mtlc.studyplan.ui.theme.FeatureKey
import com.mtlc.studyplan.ui.theme.featurePastelContainer
import com.mtlc.studyplan.ui.theme.inferredFeaturePastelContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

import com.mtlc.studyplan.analytics.StudyStreak

// Data classes are now defined in AnalyticsEngine.kt

@Composable
fun TimeDistributionCard(
    patterns: StudyPatternsUI,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = inferredFeaturePastelContainer("com.mtlc.studyplan.analytics", "Study Time Distribution"))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Study Time Distribution",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Pie chart for time distribution
            TimeDistributionChart(
                distribution = patterns.timeDistribution,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            TimeDistributionLegend(distribution = patterns.timeDistribution)
        }
    }
}

@Composable
fun TimeDistributionChart(
    distribution: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        if (distribution.isEmpty()) return@Canvas

        val total = distribution.values.sum()
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) / 3

        var startAngle = -90f
        distribution.entries.forEachIndexed { index, (_, value) ->
            val sweepAngle = (value / total) * 360f
            val color = colors[index % colors.size]

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            startAngle += sweepAngle
        }

        // Draw center circle for donut effect
        drawCircle(
            color = surfaceColor,
            radius = radius * 0.5f,
            center = center
        )
    }
}

@Composable
fun TimeDistributionLegend(
    distribution: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )

    Column(modifier = modifier) {
        distribution.entries.forEachIndexed { index, (category, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors[index % colors.size])
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp
                )
                Text(
                    text = "${value.toInt()}min",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ProductivityInsightsCard(
    insights: ProductivityInsights,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.ANALYTICS, "AnalyticsCard"))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Productivity Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProductivityMetric(
                    title = "Best Hour",
                    value = "${insights.mostProductiveHour}:00",
                    icon = Icons.Filled.AccessTime
                )
                ProductivityMetric(
                    title = "Peak Day",
                    value = insights.mostProductiveDay,
                    icon = Icons.Filled.CalendarToday
                )
                ProductivityMetric(
                    title = "Focus Score",
                    value = "${(insights.focusScore * 100).toInt()}%",
                    icon = Icons.Filled.CenterFocusWeak
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Productivity trend
            ProductivityTrendChart(
                hourlyData = insights.hourlyProductivity,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }
}

@Composable
fun ProductivityMetric(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProductivityTrendChart(
    hourlyData: Map<Int, Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val hours = (0..23).toList()
    val data = hours.map { hourlyData[it] ?: 0f }

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxValue = data.maxOrNull() ?: 1f
        val stepX = size.width / (hours.size - 1)
        val padding = 20.dp.toPx()

        // Draw bars
        data.forEachIndexed { index, value ->
            val barHeight = (value / maxValue) * (size.height - padding)
            val x = stepX * index
            val barWidth = stepX * 0.6f

            drawRect(
                color = primaryColor.copy(alpha = 0.7f),
                topLeft = Offset(x - barWidth / 2, size.height - padding - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun BestStudyTimesCard(
    patterns: StudyPatternsUI,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.ANALYTICS, "AnalyticsCard"))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Best Study Times",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyTimeRecommendation(
                    time = "Morning (8-10 AM)",
                    score = patterns.morningProductivity,
                    reason = "High focus and energy levels"
                )
                StudyTimeRecommendation(
                    time = "Afternoon (2-4 PM)",
                    score = patterns.afternoonProductivity,
                    reason = "Post-lunch concentration boost"
                )
                StudyTimeRecommendation(
                    time = "Evening (7-9 PM)",
                    score = patterns.eveningProductivity,
                    reason = "Quiet time for reflection"
                )
            }
        }
    }
}

@Composable
fun StudyTimeRecommendation(
    time: String,
    score: Float,
    reason: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = time,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = reason,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Score indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when {
                        score >= 0.8f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        score >= 0.6f -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${(score * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    score >= 0.8f -> MaterialTheme.colorScheme.primary
                    score >= 0.6f -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
fun RecommendationsCard(
    recommendations: List<Recommendation>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.ANALYTICS, "AnalyticsCard"))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Recommendations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            recommendations.take(5).forEach { recommendation ->
                RecommendationItem(
                    recommendation = recommendation,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RecommendationItem(
    recommendation: Recommendation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (recommendation.priority) {
                RecommendationPriority.HIGH -> MaterialTheme.colorScheme.errorContainer
                RecommendationPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                RecommendationPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = when (recommendation.category) {
                        "schedule" -> Icons.Filled.Schedule
                        "performance" -> Icons.AutoMirrored.Filled.TrendingUp
                        "focus" -> Icons.Filled.CenterFocusWeak
                        else -> Icons.Filled.Lightbulb
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (recommendation.priority) {
                        RecommendationPriority.HIGH -> MaterialTheme.colorScheme.error
                        RecommendationPriority.MEDIUM -> MaterialTheme.colorScheme.secondary
                        RecommendationPriority.LOW -> MaterialTheme.colorScheme.outline
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.message,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (recommendation.reasoning.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recommendation.reasoning,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Priority badge
                Text(
                    text = when (recommendation.priority) {
                        RecommendationPriority.HIGH -> "!"
                        RecommendationPriority.MEDIUM -> "•"
                        RecommendationPriority.LOW -> "·"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (recommendation.priority) {
                        RecommendationPriority.HIGH -> MaterialTheme.colorScheme.error
                        RecommendationPriority.MEDIUM -> MaterialTheme.colorScheme.secondary
                        RecommendationPriority.LOW -> MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

@Composable
fun StudyHabitsCard(
    data: AnalyticsData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.ANALYTICS, "AnalyticsCard"))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Study Habits Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HabitMetric(
                    title = "Consistency",
                    value = "${(data.consistencyScore * 100).toInt()}%",
                    description = "Daily study habits"
                )
                HabitMetric(
                    title = "Avg Session",
                    value = "${data.averageSessionMinutes}min",
                    description = "Session duration"
                )
                HabitMetric(
                    title = "Sessions/Day",
                    value = String.format("%.1f", data.averageSessionsPerDay),
                    description = "Study frequency"
                )
            }
        }
    }
}

@Composable
fun HabitMetric(
    title: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GoalProgressCard(
    data: AnalyticsData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.ANALYTICS, "AnalyticsCard"))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Goal Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Weekly goal progress
            GoalProgressItem(
                title = "Weekly Study Goal",
                progress = data.weeklyGoalProgress,
                target = "7 hours",
                current = "${data.thisWeekMinutes / 60}h ${data.thisWeekMinutes % 60}m"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Task completion goal
            GoalProgressItem(
                title = "Task Completion",
                progress = data.taskCompletionRate,
                target = "90% completion",
                current = "${(data.taskCompletionRate * 100).toInt()}%"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Streak goal
            GoalProgressItem(
                title = "Study Streak",
                progress = minOf(data.studyStreak.currentStreak / 30f, 1f),
                target = "30 days",
                current = "${data.studyStreak.currentStreak} days"
            )
        }
    }
}

@Composable
fun GoalProgressItem(
    title: String,
    progress: Float,
    target: String,
    current: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$current / $target",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                progress >= 1f -> MaterialTheme.colorScheme.primary
                progress >= 0.7f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outline
            }
        )
    }
}

@Composable
fun PerformanceTrendsChart(
    weeklyData: List<WeeklyAnalyticsData>,
    modifier: Modifier = Modifier
) {
    if (weeklyData.isEmpty()) {
        EmptyStateCard(
            title = "No Performance Data",
            description = "Complete some tasks to see your performance trends",
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        return
    }

    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.ANALYTICS, "AnalyticsCard"))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance Trends",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Multi-line chart for accuracy and speed
            MultiLineChart(
                accuracyData = weeklyData.map { it.averageAccuracy },
                speedData = weeklyData.map { it.averageSpeed },
                labels = weeklyData.map { "W${it.weekNumber}" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }
    }
}

@Composable
fun MultiLineChart(
    accuracyData: List<Float>,
    speedData: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        if (accuracyData.isEmpty()) return@Canvas

        val padding = 40.dp.toPx()
        val stepX = (size.width - 2 * padding) / (accuracyData.size - 1).coerceAtLeast(1)

        // Normalize data
        val maxAccuracy = accuracyData.maxOrNull() ?: 1f
        val minAccuracy = accuracyData.minOrNull() ?: 0f
        val maxSpeed = speedData.maxOrNull() ?: 1f
        val minSpeed = speedData.minOrNull() ?: 0f

        // Draw accuracy line
        if (accuracyData.size > 1) {
            val accuracyPoints = accuracyData.mapIndexed { index, value ->
                val x = padding + stepX * index
                val normalizedValue = (value - minAccuracy) / (maxAccuracy - minAccuracy).coerceAtLeast(0.001f)
                val y = size.height - padding - normalizedValue * (size.height - 2 * padding)
                Offset(x, y)
            }

            for (i in 0 until accuracyPoints.size - 1) {
                drawLine(
                    color = primaryColor,
                    start = accuracyPoints[i],
                    end = accuracyPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }

            accuracyPoints.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }

        // Draw speed line (inverted since lower is better)
        if (speedData.size > 1) {
            val speedPoints = speedData.mapIndexed { index, value ->
                val x = padding + stepX * index
                val normalizedValue = 1f - ((value - minSpeed) / (maxSpeed - minSpeed).coerceAtLeast(0.001f))
                val y = size.height - padding - normalizedValue * (size.height - 2 * padding)
                Offset(x, y)
            }

            for (i in 0 until speedPoints.size - 1) {
                drawLine(
                    color = secondaryColor,
                    start = speedPoints[i],
                    end = speedPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }

            speedPoints.forEach { point ->
                drawCircle(
                    color = secondaryColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
fun RecentAchievementsCard(
    achievements: List<String>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.ANALYTICS, "AnalyticsCard"))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            achievements.take(3).forEach { achievement ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = achievement,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickStatsCard(
    data: AnalyticsData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.ANALYTICS, "AnalyticsCard"))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Stats",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat(
                    label = "Today",
                    value = "${data.todayMinutes}min"
                )
                QuickStat(
                    label = "This Week",
                    value = "${data.thisWeekMinutes}min"
                )
                QuickStat(
                    label = "Best Streak",
                    value = "${data.studyStreak.longestStreak}"
                )
                QuickStat(
                    label = "Total Days",
                    value = "${data.totalStudyDays}"
                )
            }
        }
    }
}

@Composable
fun QuickStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
