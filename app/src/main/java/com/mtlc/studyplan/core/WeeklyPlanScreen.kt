package com.mtlc.studyplan.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDaily: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Sample data for multiple weeks
    val weeks = remember {
        listOf(
            WeeklyStudyPlan(
                title = "Reading Comprehension Focus",
                description = "Week 1 - Foundation Building",
                currentWeek = 1,
                totalWeeks = 4,
                progressPercentage = 1.0f,
                days = listOf(
                    WeekDay("Mon", 100, true),
                    WeekDay("Tue", 100, true),
                    WeekDay("Wed", 100, true),
                    WeekDay("Thu", 100, true),
                    WeekDay("Fri", 100, true),
                    WeekDay("Sat", 100, true),
                    WeekDay("Sun", 100, true)
                )
            ),
            WeeklyStudyPlan(
                title = "Grammar & Vocabulary",
                description = "Week 2 - Skill Development",
                currentWeek = 2,
                totalWeeks = 4,
                progressPercentage = 0.65f,
                days = listOf(
                    WeekDay("Mon", 100, true),
                    WeekDay("Tue", 100, true),
                    WeekDay("Wed", 70, false),
                    WeekDay("Thu", 0, false),
                    WeekDay("Fri", 0, false),
                    WeekDay("Sat", 0, false),
                    WeekDay("Sun", 0, false)
                )
            ),
            WeeklyStudyPlan(
                title = "Practice Tests",
                description = "Week 3 - Mock Exams",
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
                title = "Final Review",
                description = "Week 4 - Exam Preparation",
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
            TopAppBar(
                title = {
                    Text(
                        "Weekly Study Plan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "Study Plan Overview",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track your progress week by week",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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