package com.mtlc.studyplan.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.YdsExamService

data class HomeCardAppearance(
    val containerColor: Color,
    val border: BorderStroke,
    val modifier: Modifier = Modifier
)

@Suppress("LongMethod")
@Composable
fun TodayProgressCard(
    state: HomeState,
    appearance: HomeCardAppearance,
    onClick: () -> Unit
) {
    val cardModifier = appearance.modifier
        .height(104.dp)
        .clickable { onClick() }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(16.dp),
        border = appearance.border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.isFirstTimeUser) {
                Text(
                    text = stringResource(R.string.start),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2C2C)
                )
                Text(
                    text = stringResource(R.string.your_journey),
                    fontSize = 12.sp,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.start_studying),
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF2C2C2C)
                )
            } else {
                Text(
                    text = "${state.todayProgress}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2C2C)
                )
                Text(
                    text = stringResource(R.string.today),
                    fontSize = 12.sp,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.height(3.dp))
                CircularProgressIndicator(
                    progress = { state.todayProgress / 100f },
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF2C2C2C),
                    trackColor = Color(0xFF2C2C2C).copy(alpha = 0.3f),
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
fun DaysToExamCard(
    state: HomeState,
    appearance: HomeCardAppearance,
    statusTextStyle: TextStyle,
    onNavigateToExamDetails: (String) -> Unit
) {
    val cardModifier = appearance.modifier
        .height(104.dp)
        .clickable {
            val exam = YdsExamService.getNextExam()
            if (exam != null) {
                onNavigateToExamDetails(exam.examDate.toString())
            }
        }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(16.dp),
        border = appearance.border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${state.examTracker.daysToExam}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2C2C)
            )
            Text(
                text = stringResource(R.string.days_to_yds),
                fontSize = 12.sp,
                color = Color(0xFF424242),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.dp))
            val statusResId = remember(state.examTracker.daysToExam) {
                val days = state.examTracker.daysToExam
                val status = YdsExamService.getRegistrationStatus()
                when {
                    days == 0 -> R.string.exam_status_exam_day
                    days < 0 -> R.string.exam_status_completed
                    days <= 7 -> R.string.exam_status_final_week
                    days <= 30 -> R.string.exam_status_almost_there
                    days <= 90 -> when (status) {
                        YdsExamService.RegistrationStatus.NOT_OPEN_YET -> R.string.exam_status_registration_opens_soon
                        YdsExamService.RegistrationStatus.OPEN -> R.string.exam_status_registration_open
                        YdsExamService.RegistrationStatus.LATE_REGISTRATION -> R.string.exam_status_late_registration
                        YdsExamService.RegistrationStatus.CLOSED -> R.string.exam_status_registration_closed
                        YdsExamService.RegistrationStatus.NO_UPCOMING_EXAM -> R.string.exam_status_preparation_time
                    }
                    else -> R.string.exam_status_long_term_planning
                }
            }
            Text(
                text = stringResource(statusResId),
                style = statusTextStyle,
                color = Color(0xFF424242).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PointsTodayCard(
    state: HomeState,
    appearance: HomeCardAppearance,
    onNavigateToAnalytics: () -> Unit
) {
    val cardModifier = appearance.modifier
        .height(80.dp)
        .clickable { onNavigateToAnalytics() }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(16.dp),
        border = appearance.border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.isFirstTimeUser) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.earn_points),
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF2C2C2C)
                )
                Text(
                    text = stringResource(R.string.earn_points),
                    fontSize = 11.sp,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "${state.pointsToday}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2C2C)
                )
                Text(
                    text = stringResource(R.string.points_today),
                    fontSize = 11.sp,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TasksDoneCard(
    state: HomeState,
    appearance: HomeCardAppearance,
    onNavigateToTasks: () -> Unit
) {
    val cardModifier = appearance.modifier
        .height(80.dp)
        .clickable { onNavigateToTasks() }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(16.dp),
        border = appearance.border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.isFirstTimeUser) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.create_tasks),
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF2C2C2C)
                )
                Text(
                    text = stringResource(R.string.create_tasks),
                    fontSize = 11.sp,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "${state.completedTodayTaskCount}/${state.todayTaskTotal}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2C2C)
                )
                Text(
                    text = stringResource(R.string.tasks_done),
                    fontSize = 11.sp,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
fun StreakCard(
    state: HomeState,
    appearance: HomeCardAppearance,
    onNavigateToAnalytics: () -> Unit
) {
    val cardModifier = appearance.modifier
        .fillMaxWidth()
        .clickable { onNavigateToAnalytics() }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(16.dp),
        border = appearance.border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF8965),
                            Color(0xFFFFAC91)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFFE0E0E0),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
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
                    text = state.streakInfo.displayText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2C2C)
                )
                Text(
                    text = state.streakInfo.motivationText,
                    fontSize = 14.sp,
                    color = Color(0xFF424242)
                )
            }

            if (state.streakInfo.showMultiplierBadge) {
                Surface(
                    color = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = state.streakInfo.multiplier,
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

@Suppress("LongMethod")
@Composable
fun WeeklyPlanCard(
    state: HomeState,
    appearance: HomeCardAppearance,
    prussianBlue: Color,
    onNavigateToWeeklyPlan: () -> Unit
) {
    val cardModifier = appearance.modifier
        .fillMaxWidth()
        .clickable { onNavigateToWeeklyPlan() }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(16.dp),
        border = appearance.border
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
                    text = "📅 " + stringResource(R.string.weekly_study_plan),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C2C2C)
                )
                Surface(
                    color = prussianBlue,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(state.weeklyPlan.progressPercentage * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = state.weeklyPlan.title,
                fontSize = 14.sp,
                color = Color(0xFF424242),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { state.weeklyPlan.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = prussianBlue,
                trackColor = prussianBlue.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.week_progress) + " \u0007 ${state.weeklyPlan.weekProgressText}",
                fontSize = 12.sp,
                color = Color(0xFF424242).copy(alpha = 0.7f)
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
fun ExamCountdownCard(
    state: HomeState,
    appearance: HomeCardAppearance,
    prussianBlue: Color,
    statusTextStyle: TextStyle,
    onNavigateToExamDetails: (String) -> Unit
) {
    val nextExam = remember { YdsExamService.getNextExam() }
    Card(
        modifier = appearance.modifier
            .fillMaxWidth()
            .clickable { nextExam?.let { onNavigateToExamDetails(it.examDate.toString()) } },
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(16.dp),
        border = appearance.border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = prussianBlue.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📘",
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.examTracker.examName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2C2C)
                )
                val statusText = remember(state.examTracker.daysToExam) {
                    val days = state.examTracker.daysToExam
                    val status = YdsExamService.getRegistrationStatus()
                    val resId = when {
                        days == 0 -> R.string.exam_status_exam_day
                        days < 0 -> R.string.exam_status_completed
                        days <= 7 -> R.string.exam_status_final_week
                        days <= 30 -> R.string.exam_status_almost_there
                        days <= 90 -> when (status) {
                            YdsExamService.RegistrationStatus.NOT_OPEN_YET -> R.string.exam_status_registration_opens_soon
                            YdsExamService.RegistrationStatus.OPEN -> R.string.exam_status_registration_open
                            YdsExamService.RegistrationStatus.LATE_REGISTRATION -> R.string.exam_status_late_registration
                            YdsExamService.RegistrationStatus.CLOSED -> R.string.exam_status_registration_closed
                            YdsExamService.RegistrationStatus.NO_UPCOMING_EXAM -> R.string.exam_status_preparation_time
                        }
                        else -> R.string.exam_status_long_term_planning
                    }
                    resId
                }
                Text(
                    text = stringResource(statusText),
                    style = statusTextStyle,
                    color = Color(0xFF424242).copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "📅 ${state.examTracker.examDateFormatted}",
                    fontSize = 12.sp,
                    color = Color(0xFF424242).copy(alpha = 0.7f)
                )
            }

            Text(
                text = state.examTracker.daysToExam.toString() + stringResource(id = R.string.days_short_suffix),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2C2C)
            )
        }
    }
}

@Composable
fun AnalyticsCard(
    appearance: HomeCardAppearance,
    prussianBlue: Color,
    onNavigateToAnalytics: () -> Unit
) {
    val cardModifier = appearance.modifier
        .fillMaxWidth()
        .clickable { onNavigateToAnalytics() }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(16.dp),
        border = appearance.border
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
                    text = "📊 " + stringResource(R.string.analytics),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C2C2C)
                )
                Surface(
                    color = prussianBlue,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.view_stats),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = stringResource(R.string.track_your_progress),
                fontSize = 14.sp,
                color = Color(0xFF424242),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

