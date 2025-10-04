package com.mtlc.studyplan.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import com.mtlc.studyplan.ui.theme.FeatureKey
import com.mtlc.studyplan.ui.theme.featurePastelContainer
import com.mtlc.studyplan.ui.theme.inferredFeaturePastelContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import com.mtlc.studyplan.ui.components.StudyPlanTopBarStyle
import com.mtlc.studyplan.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDaily: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Initial study plan data for first-time users
    val weeks = remember {
        listOf(
            WeeklyStudyPlan(
                title = "Getting Started",
                description = "Week 1 - Build Your Foundation",
                currentWeek = 1,
                totalWeeks = 4,
                progressPercentage = 0.0f,
                days = listOf(
                    WeekDay("Mon", 0, false),
                    WeekDay("Tue", 0, false),
                    WeekDay("Wed", 0, false),
                    WeekDay("Thu", 0, false),
                    WeekDay("Fri", 0, false),
                    WeekDay("Sat", 0, false),
                    WeekDay("Sun", 0, false)
                )
            ),
            WeeklyStudyPlan(
                title = "Core Skills",
                description = "Week 2 - Develop Your Abilities",
                currentWeek = 2,
                totalWeeks = 4,
                progressPercentage = 0.0f,
                days = listOf(
                    WeekDay("Mon", 0, false),
                    WeekDay("Tue", 0, false),
                    WeekDay("Wed", 0, false),
                    WeekDay("Thu", 0, false),
                    WeekDay("Fri", 0, false),
                    WeekDay("Sat", 0, false),
                    WeekDay("Sun", 0, false)
                )
            ),
            WeeklyStudyPlan(
                title = "Practice & Apply",
                description = "Week 3 - Test Your Knowledge",
                currentWeek = 3,
                totalWeeks = 4,
                progressPercentage = 0.0f,
                days = listOf(
                    WeekDay("Mon", 0, false),
                    WeekDay("Tue", 0, false),
                    WeekDay("Wed", 0, false),
                    WeekDay("Thu", 0, false),
                    WeekDay("Fri", 0, false),
                    WeekDay("Sat", 0, false),
                    WeekDay("Sun", 0, false)
                )
            ),
            WeeklyStudyPlan(
                title = "Final Preparation",
                description = "Week 4 - Ready for Success",
                currentWeek = 4,
                totalWeeks = 4,
                progressPercentage = 0.0f,
                days = listOf(
                    WeekDay("Mon", 0, false),
                    WeekDay("Tue", 0, false),
                    WeekDay("Wed", 0, false),
                    WeekDay("Thu", 0, false),
                    WeekDay("Fri", 0, false),
                    WeekDay("Sat", 0, false),
                    WeekDay("Sun", 0, false)
                )
            )
        )
    }

    Scaffold(
        topBar = {
            // Settings-style topbar with pastel gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFBE9E7), // Light pastel red/pink
                                        Color(0xFFE3F2FD)  // Light pastel blue
                                    )
                                },
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
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Weekly Study Plan",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Track your progress week by week",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
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
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Week Cards
            items(weeks) { week ->
                WeekPlanCard(
                    week = week,
                    onDayClick = onNavigateToDaily,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun WeekPlanCard(
    week: WeeklyStudyPlan,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = inferredFeaturePastelContainer("com.mtlc.studyplan.core", week.title)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Week Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Week ${week.currentWeek}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = week.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = week.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Surface(
                    color = when {
                        week.progressPercentage >= 1.0f -> Color(0xFF4CAF50)
                        week.progressPercentage > 0f -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(week.progressPercentage * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { week.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    week.progressPercentage >= 1.0f -> Color(0xFF4CAF50)
                    week.progressPercentage > 0f -> Color(0xFFFF9800)
                    else -> Color(0xFF9E9E9E)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Days Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(week.days) { day ->
                    WeekDayCard(
                        day = day,
                        onClick = { onDayClick("${week.title} - ${day.dayName}") },
                        modifier = Modifier.width(70.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Week Status
            Text(
                text = "${week.days.count { it.isCompleted }} of ${week.days.size} days completed",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WeekDayCard(
    day: WeekDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                day.isCompleted -> Color(0xFF4CAF50)
                day.completionPercentage > 0 -> Color(0xFFFFEB3B).copy(alpha = 0.8f)
                else -> Color(0xFFE0E0E0)
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.dayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    day.isCompleted -> Color.White
                    day.completionPercentage > 0 -> Color.Black
                    else -> Color(0xFF757575)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = day.displayText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    day.isCompleted -> Color.White
                    day.completionPercentage > 0 -> Color.Black
                    else -> Color(0xFF757575)
                }
            )
        }
    }
}

