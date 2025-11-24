@file:Suppress("LongMethod", "CyclomaticComplexMethod")
@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.ui.theme.FeatureKey
import com.mtlc.studyplan.ui.theme.featurePastelContainer
import com.mtlc.studyplan.ui.theme.inferredFeaturePastelContainer

// Data classes are defined in AnalyticsModels.kt

@Composable
fun TimeDistributionCard(
    patterns: StudyPatternsUI,
    modifier: Modifier = Modifier
) {
    val timeDistributionGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFD0E8E8),  // Soft teal-cyan
            Color(0xFFF0FFFE)   // Very light cyan white
        )
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(timeDistributionGradient)
        ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Study Time Distribution",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (patterns.timeDistribution.isEmpty()) {
                // Show empty state message for first-time users
                val timeDistributionEmptyGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFDCD8),  // Soft coral-peach
                        Color(0xFFFFF0ED)   // Very light salmon white
                    )
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(timeDistributionEmptyGradient)
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Track Your Study Times",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = stringResource(R.string.cd_time_focus),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete study sessions throughout the day to see when you're most productive. This chart will show your study time distribution across morning, afternoon, and evening periods.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact pie chart
                    TimeDistributionChart(
                        distribution = patterns.timeDistribution,
                        modifier = Modifier.size(100.dp)
                    )

                    // Legend beside chart
                    TimeDistributionLegend(
                        distribution = patterns.timeDistribution,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
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
            val displayName = when (category) {
                "early_morning" -> "Early Morning"
                "morning" -> "Morning"
                "afternoon" -> "Afternoon"
                "evening" -> "Evening"
                "night" -> "Night"
                else -> category.replace("_", " ").split(" ").joinToString(" ") {
                    it.replaceFirstChar { c -> c.uppercase() }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors[index % colors.size])
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayName,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp
                )
                Text(
                    text = "${(value * 100).toInt()}%",
                    fontSize = 12.sp,
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
    val productivityGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF0D8E8),  // Soft pink-mauve
            Color(0xFFFFF5FB)   // Very light rose white
        )
    )

    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(productivityGradient)
        ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Productivity Insights",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (insights.hourlyProductivity.isEmpty()) {
                // Show empty state message for first-time users
                val productivityEmptyGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE8D4F8),  // Soft purple-lavender
                        Color(0xFFFDF5FF)   // Very light purple white
                    )
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(productivityEmptyGradient)
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Discover Your Peak Hours",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.CenterFocusWeak,
                                contentDescription = stringResource(R.string.cd_time_focus),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Build your study history to identify your most productive hours, peak performance days, and focus patterns. This insight helps you schedule important tasks when you're at your best.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                    }
                }
            } else {
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

                Spacer(modifier = Modifier.height(10.dp))

                // Productivity trend
                ProductivityTrendChart(
                    hourlyData = insights.hourlyProductivity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        }
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
    val bestTimesGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFD8E8D0),  // Soft sage-green
            Color(0xFFF5FFF0)   // Very light mint white
        )
    )

    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bestTimesGradient)
        ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Best Study Times",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (patterns.hourlyProductivity.isEmpty()) {
                // Show empty state message for first-time users
                val bestTimesEmptyGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD0F0D8),  // Soft mint-green
                        Color(0xFFF0FFF5)   // Very light mint white
                    )
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bestTimesEmptyGradient)
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Find Your Optimal Study Windows",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = stringResource(R.string.cd_time_focus),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete study sessions during different times of day to discover when you perform best. We'll analyze your morning, afternoon, and evening productivity to recommend ideal study times.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudyTimeRecommendation(
                        time = "Morning",
                        score = patterns.morningProductivity
                    )
                    StudyTimeRecommendation(
                        time = "Afternoon",
                        score = patterns.afternoonProductivity
                    )
                    StudyTimeRecommendation(
                        time = "Evening",
                        score = patterns.eveningProductivity
                    )
                }
            }
        }
        }
    }
}

@Composable
fun StudyTimeRecommendation(
    time: String,
    score: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        // Progress bar
        LinearProgressIndicator(
            progress = { score.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(2f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                score >= 0.7f -> MaterialTheme.colorScheme.primary
                score >= 0.4f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outline
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Score percentage
        Text(
            text = "${(score * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun StudyTimeRecommendationLegacy(
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
                    contentDescription = stringResource(R.string.cd_info),
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

            if (recommendations.isEmpty()) {
                // Show default welcome message when no data
                val recommendationsEmptyGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE0D4F8),  // Soft violet-lavender
                        Color(0xFFF8F5FF)   // Very light violet white
                    )
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(recommendationsEmptyGradient)
                    ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Welcome to StudyPlan!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.EmojiObjects,
                                contentDescription = stringResource(R.string.cd_info),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start completing study sessions to receive personalized AI recommendations based on your learning patterns, performance, and study habits.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                    }
                }
            } else {
                recommendations.take(5).forEach { recommendation ->
                    RecommendationItem(
                        recommendation = recommendation,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
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
                    contentDescription = stringResource(R.string.cd_info),
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
                    if (recommendation.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recommendation.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
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
                    value = String.format(java.util.Locale.US, "%.1f", data.averageSessionsPerDay),
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

            // Weekly goal progress (real user goal)
            val goalMinutes = data.weeklyGoalMinutesTarget
            val goalHoursText = if (goalMinutes > 0) {
                val h = goalMinutes / 60
                val m = goalMinutes % 60
                if (m > 0) "${h}h ${m}m" else "${h}h"
            } else "—"
            GoalProgressItem(
                title = "Weekly Study Goal",
                progress = data.weeklyGoalProgress,
                target = goalHoursText,
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

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Accuracy",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Speed",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Multi-line chart for accuracy and speed
            MultiLineChart(
                accuracyData = weeklyData.map { it.averageAccuracy },
                speedData = weeklyData.map { it.averageSpeed },
                labels = weeklyData.map { "W${it.weekNumber}" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
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
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        if (accuracyData.isEmpty()) return@Canvas

        val padding = 40.dp.toPx()
        val stepX = if (accuracyData.size > 1) {
            (size.width - 2 * padding) / (accuracyData.size - 1)
        } else {
            size.width / 2
        }

        // Normalize data
        val maxAccuracy = accuracyData.maxOrNull() ?: 1f
        val minAccuracy = accuracyData.minOrNull() ?: 0f
        val maxSpeed = speedData.maxOrNull() ?: 1f
        val minSpeed = speedData.minOrNull() ?: 0f

        // Draw grid lines
        val gridLineColor = surfaceVariant.copy(alpha = 0.3f)
        for (i in 0..4) {
            val y = padding + (size.height - 2 * padding) * i / 4
            drawLine(
                color = gridLineColor,
                start = Offset(padding, y),
                end = Offset(size.width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw accuracy line/points
        val accuracyPoints = accuracyData.mapIndexed { index, value ->
            val x = if (accuracyData.size == 1) size.width / 2 else padding + stepX * index
            val normalizedValue = (value - minAccuracy) / (maxAccuracy - minAccuracy).coerceAtLeast(0.001f)
            val y = size.height - padding - normalizedValue * (size.height - 2 * padding)
            Offset(x, y)
        }

        // Draw lines only if multiple points
        if (accuracyPoints.size > 1) {
            for (i in 0 until accuracyPoints.size - 1) {
                drawLine(
                    color = primaryColor,
                    start = accuracyPoints[i],
                    end = accuracyPoints[i + 1],
                    strokeWidth = 4.dp.toPx()
                )
            }
        }

        // Always draw points
        accuracyPoints.forEach { point ->
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx(),
                center = point
            )
        }

        // Draw speed line/points (inverted since lower is better)
        if (speedData.isNotEmpty()) {
            val speedPoints = speedData.mapIndexed { index, value ->
                val x = if (speedData.size == 1) size.width / 2 else padding + stepX * index
                val normalizedValue = 1f - ((value - minSpeed) / (maxSpeed - minSpeed).coerceAtLeast(0.001f))
                val y = size.height - padding - normalizedValue * (size.height - 2 * padding)
                Offset(x, y)
            }

            // Draw lines only if multiple points
            if (speedPoints.size > 1) {
                for (i in 0 until speedPoints.size - 1) {
                    drawLine(
                        color = secondaryColor,
                        start = speedPoints[i],
                        end = speedPoints[i + 1],
                        strokeWidth = 4.dp.toPx()
                    )
                }
            }

            // Always draw points
            speedPoints.forEach { point ->
                drawCircle(
                    color = secondaryColor,
                    radius = 6.dp.toPx(),
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
    val achievementsGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFE8D0),  // Soft peach-orange
            Color(0xFFFFF9F0)   // Very light cream
        )
    )

    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(achievementsGradient)
        ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = stringResource(R.string.cd_achievement),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.analytics_recent_achievements_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (achievements.isEmpty()) {
                Text(
                    text = "Build a 7-day streak, reach 95% accuracy, or study 500+ minutes to earn achievements!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            } else {
                achievements.take(3).forEach { achievement ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(R.string.cd_achievement),
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
    }
}

@Composable
fun QuickStatsCard(
    data: AnalyticsData,
    modifier: Modifier = Modifier
) {
    val hasData = data.completedTasks > 0 || data.totalStudyMinutes > 0

    val quickStatsGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE0BFE0),
            Color(0xFFF5E6F5)
        )
    )

    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(quickStatsGradient)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Stats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (!hasData) {
                    // First-use informational content
                    val quickStatsEmptyGradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFDD5C8),  // Soft rose-orange
                            Color(0xFFFFF2ED)   // Very light coral white
                        )
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(quickStatsEmptyGradient)
                        ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Start Your Journey",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Dashboard,
                                    contentDescription = stringResource(R.string.cd_chart),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your daily stats, weekly progress, study streaks, and total study days will appear here as you complete tasks. Start by creating and completing your first task!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        }
                        }
                    }
                } else {
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
