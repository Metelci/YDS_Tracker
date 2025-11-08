package com.mtlc.studyplan.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.integration.AppIntegrationManager

@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingHomeScreen(
    appIntegrationManager: AppIntegrationManager,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToWeeklyPlan: () -> Unit = {},
    onNavigateToStudyPlan: () -> Unit = {},
    onNavigateToExamDetails: (String) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val homeState = rememberWorkingHomeState(appIntegrationManager)

    val prussianBlue = Color(0xFF003153)
    val homeCardBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val homeCardBorder = remember(homeCardBorderColor) { BorderStroke(1.dp, homeCardBorderColor) }
    val statusTextStyle = MaterialTheme.typography.labelSmall

    val pastelTasks = Color(0xFFF0FDF4)
    val pastelNavigation = Color(0xFFF3E8FF)
    val pastelGamification = Color(0xFFFFF4E6)
    val pastelPrivacy = Color(0xFFFDF2F8)
    val pastelNotifications = Color(0xFFE8F5E8)
    val pastelWeekly = Color(0xFFFFF8E1)

    val pastelPink = pastelTasks
    val pastelBlue = pastelNavigation
    val pastelMint = pastelGamification
    val pastelLavender = pastelNotifications
    val pastelPeach = pastelWeekly
    val pastelYellow = pastelPrivacy

    val isDarkTheme = false

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen")
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFEFF6FF), Color(0xFFF7FBFF))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE3F2FD)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFE3F2FD),
                                    Color(0xFFFCE4EC)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (homeState.isFirstTimeUser) {
                                stringResource(R.string.welcome_first_time)
                            } else {
                                stringResource(R.string.good_morning)
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                prussianBlue
                            }
                        )
                        Text(
                            text = stringResource(R.string.ready_yds_exam),
                            fontSize = 14.sp,
                            color = if (isDarkTheme) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                prussianBlue.copy(alpha = 0.8f)
                            }
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TodayProgressCard(
                    state = homeState,
                    appearance = HomeCardAppearance(
                        containerColor = pastelPink,
                        border = homeCardBorder,
                        modifier = Modifier.weight(1f)
                    ),
                    onClick = onNavigateToStudyPlan
                    )
                    DaysToExamCard(
                    state = homeState,
                    appearance = HomeCardAppearance(
                        containerColor = pastelBlue,
                        border = homeCardBorder,
                        modifier = Modifier.weight(1f)
                    ),
                    statusTextStyle = statusTextStyle,
                    onNavigateToExamDetails = onNavigateToExamDetails
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PointsTodayCard(
                    state = homeState,
                    appearance = HomeCardAppearance(
                        containerColor = pastelMint,
                        border = homeCardBorder,
                        modifier = Modifier.weight(1f)
                    ),
                    onNavigateToAnalytics = onNavigateToAnalytics
                    )
                    TasksDoneCard(
                    state = homeState,
                    appearance = HomeCardAppearance(
                        containerColor = pastelLavender,
                        border = homeCardBorder,
                        modifier = Modifier.weight(1f)
                    ),
                    onNavigateToTasks = onNavigateToTasks
                    )
                }
            }

            if (homeState.streakInfo.currentStreak > 0) {
                item {
                    StreakCard(
                    state = homeState,
                    appearance = HomeCardAppearance(
                        containerColor = Color.Transparent,
                        border = homeCardBorder,
                        modifier = Modifier.fillMaxWidth()
                    ),
                    onNavigateToAnalytics = onNavigateToAnalytics
                    )
                }
            }

            item {
                WeeklyPlanCard(
                    state = homeState,
                    appearance = HomeCardAppearance(
                        containerColor = pastelPeach,
                        border = homeCardBorder,
                        modifier = Modifier.fillMaxWidth()
                    ),
                    prussianBlue = prussianBlue,
                    onNavigateToWeeklyPlan = onNavigateToWeeklyPlan
                    )
            }

            item {
                ExamCountdownCard(
                    state = homeState,
                    appearance = HomeCardAppearance(
                        containerColor = pastelYellow,
                        border = homeCardBorder,
                        modifier = Modifier.fillMaxWidth()
                    ),
                    prussianBlue = prussianBlue,
                    statusTextStyle = statusTextStyle,
                    onNavigateToExamDetails = onNavigateToExamDetails
                    )
            }

            item {
                AnalyticsCard(
                    appearance = HomeCardAppearance(
                        containerColor = pastelBlue,
                        border = homeCardBorder,
                        modifier = Modifier.fillMaxWidth()
                    ),
                    prussianBlue = prussianBlue,
                    onNavigateToAnalytics = onNavigateToAnalytics
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

