package com.mtlc.studyplan.exam

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
// removed luminance-based dark theme checks
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.mtlc.studyplan.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.ExamCountdownManager
import com.mtlc.studyplan.data.YdsExamService
import com.mtlc.studyplan.network.OsymExamCalendarClient
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val examCountdownManager = remember { ExamCountdownManager.getInstance(context) }
    val examData by examCountdownManager.examData.collectAsState()
    var allUpcomingExams by remember { mutableStateOf(YdsExamService.getAllUpcomingExams()) }

    // Show the first (nearest) exam by default
    val selectedExam = allUpcomingExams.firstOrNull()

    // Debug: Ensure we have exam data
    LaunchedEffect(allUpcomingExams) {
        if (allUpcomingExams.isEmpty()) {
            // Fallback to static data if dynamic loading failed
            allUpcomingExams = YdsExamService.getAllUpcomingExams()
        }
    }

    // Refresh exam data when screen loads and try fetching official dates
    LaunchedEffect(Unit) {
        val fetched = runCatching { OsymExamCalendarClient.fetchYdsExams() }
            .getOrElse { emptyList() }
        if (fetched.isNotEmpty()) {
            allUpcomingExams = fetched
            examCountdownManager.refreshNow()
        } else {
            YdsExamService.refreshFromNetwork()
            examCountdownManager.refreshNow()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom YDS Exam Information Top Bar (matching Settings topbar design)
        YdsExamGradientTopBar(
            onNavigateBack = onNavigateBack,
            daysRemaining = examData.daysToExam.coerceAtLeast(0)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            if (allUpcomingExams.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.upcoming_yds_exams),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                // Empty state when no upcoming exams
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventBusy,
                            contentDescription = stringResource(id = R.string.no_exams_cd),
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.no_upcoming_exams_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.no_upcoming_exams_message),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@Column
            }

            // Selected Exam Details
            selectedExam?.let { exam ->
                val daysToExam = examData.daysToExam.coerceAtLeast(0)
                val registrationStatus = getRegistrationStatusForExam(exam)

                // Countdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF4FC3F7), // Original light blue
                                        Color(0xFF29B6F6)  // Original blue
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = daysToExam.toString(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(id = R.string.days_remaining),
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = getStatusMessageResForExam(exam)),
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Enhanced Exam Information Card with Apply Button
                ExamInfoCardWithAction(
                    title = stringResource(id = R.string.exam_information),
                    isExamInfoCard = true,
                    icon = Icons.Filled.Event,
                    examSession = exam,
                    daysRemaining = daysToExam,
                    content = {
                        InfoRow(stringResource(id = R.string.exam_name_label), exam.name.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.yds_exam_default))
                        InfoRow(stringResource(id = R.string.exam_date_label), formatDate(exam.examDate))
                        InfoRow(stringResource(id = R.string.exam_time_label), stringResource(id = R.string.exam_time_value))
                        InfoRow(stringResource(id = R.string.exam_duration_label), stringResource(id = R.string.exam_duration_value))
                        InfoRow(stringResource(id = R.string.exam_question_count_label), stringResource(id = R.string.exam_question_count_value))
                    }
                )

                // Enhanced Registration Information Card with Apply Button
                ExamInfoCardWithAction(
                    title = stringResource(id = R.string.registration_information),
                    isExamInfoCard = false,
                    icon = Icons.Filled.AssignmentInd,
                    examSession = exam,
                    daysRemaining = daysToExam,
                    content = {
                        InfoRow(stringResource(id = R.string.registration_status_label), getRegistrationStatusText(registrationStatus))
                        InfoRow(stringResource(id = R.string.application_period_label), formatDate(exam.registrationStart) + " - " + formatDate(exam.registrationEnd))
                        InfoRow(stringResource(id = R.string.late_registration_label), formatDate(exam.lateRegistrationEnd))
                        InfoRow(stringResource(id = R.string.results_date_label), formatDate(exam.resultDate))
                    }
                )
            }


            // Tips and Reminders Card
            ExamInfoCard(
                title = stringResource(id = R.string.important_reminders),
                icon = Icons.Filled.Lightbulb,
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TipItem("📋", stringResource(id = R.string.tip_bring_id))
                        TipItem("⏰", stringResource(id = R.string.tip_arrive_early))
                        TipItem("✏️", stringResource(id = R.string.tip_bring_tools))
                        TipItem("📱", stringResource(id = R.string.tip_turn_off_devices))
                        TipItem("🎯", stringResource(id = R.string.tip_read_carefully))
                    }
                }
            )
        }
    }
}

@Composable
private fun ExamInfoCardWithAction(
    title: String,
    isExamInfoCard: Boolean,
    icon: ImageVector,
    examSession: YdsExamService.YdsExamSession,
    daysRemaining: Int,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val registrationStatus = getRegistrationStatusForExam(examSession)
    val isDarkTheme = false

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isExamInfoCard) {
                            listOf(
                                Color(0xFFF0F8FF), // Light blue pastel
                                Color(0xFFF5F0FF), // Light lavender
                                Color(0xFFFFF8F0)  // Light cream
                            )
                        } else {
                            listOf(
                                Color(0xFFF0FFF0), // Light mint green
                                Color(0xFFFFF0F8), // Light rose
                                Color(0xFFF0F8FF)  // Light sky blue
                            )
                        },
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isDarkTheme) {
                            Color(0xFF64B5F6) // Darker blue for dark theme
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) {
                            Color(0xFFE0E0E0) // Darker white for dark theme
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            content()

            // Bottom section with days remaining for each card
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when {
                        daysRemaining <= 7 -> Color(0xFFE57373).copy(alpha = 0.2f)
                        daysRemaining <= 30 -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                        else -> Color(0xFF81C784).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.days_remaining_chip, daysRemaining),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            daysRemaining <= 7 -> Color(0xFFD32F2F)
                            daysRemaining <= 30 -> Color(0xFFE65100)
                            else -> Color(0xFF388E3C)
                        }
                    )
                }

                // Additional apply button for exam info card if registration is open
                if (isExamInfoCard &&
                    (registrationStatus == YdsExamService.RegistrationStatus.OPEN ||
                     registrationStatus == YdsExamService.RegistrationStatus.LATE_REGISTRATION)) {

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(examSession.applicationUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle case where no browser app is available
                                e.printStackTrace()
                            }
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(id = R.string.apply),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.apply),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ExamInfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    val isDarkTheme = false

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isDarkTheme) {
                Color(0xFF9E9E9E) // Darker gray for dark theme
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkTheme) {
                Color(0xFFE0E0E0) // Darker white for dark theme
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TipItem(
    emoji: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 16.sp
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getRegistrationStatusForExam(exam: YdsExamService.YdsExamSession): YdsExamService.RegistrationStatus {
    val today = LocalDate.now()
    return when {
        today.isBefore(exam.registrationStart) -> YdsExamService.RegistrationStatus.NOT_OPEN_YET
        today.isAfter(exam.lateRegistrationEnd) -> YdsExamService.RegistrationStatus.CLOSED
        today.isAfter(exam.registrationEnd) -> YdsExamService.RegistrationStatus.LATE_REGISTRATION
        else -> YdsExamService.RegistrationStatus.OPEN
    }
}

private fun getStatusMessageResForExam(exam: YdsExamService.YdsExamSession): Int {
    val daysToExam = ChronoUnit.DAYS.between(LocalDate.now(), exam.examDate).toInt()
    val registrationStatus = getRegistrationStatusForExam(exam)

    return when {
        daysToExam == 0 -> R.string.exam_status_exam_day
        daysToExam < 0 -> R.string.exam_status_completed
        daysToExam <= 7 -> R.string.exam_status_final_week
        daysToExam <= 30 -> R.string.exam_status_almost_there
        daysToExam <= 90 -> when (registrationStatus) {
            YdsExamService.RegistrationStatus.NOT_OPEN_YET -> R.string.exam_status_registration_opens_soon
            YdsExamService.RegistrationStatus.OPEN -> R.string.exam_status_registration_open
            YdsExamService.RegistrationStatus.LATE_REGISTRATION -> R.string.exam_status_late_registration
            YdsExamService.RegistrationStatus.CLOSED -> R.string.exam_status_registration_closed
            YdsExamService.RegistrationStatus.NO_UPCOMING_EXAM -> R.string.exam_status_preparation_time
        }
        else -> R.string.exam_status_long_term_planning
    }
}

private fun formatShortDate(date: LocalDate): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd")
    return date.format(formatter)
}

@Composable
private fun getRegistrationStatusText(status: YdsExamService.RegistrationStatus): String {
    val resId = when (status) {
        YdsExamService.RegistrationStatus.NOT_OPEN_YET -> R.string.registration_status_opens_soon
        YdsExamService.RegistrationStatus.OPEN -> R.string.registration_status_open
        YdsExamService.RegistrationStatus.LATE_REGISTRATION -> R.string.registration_status_late
        YdsExamService.RegistrationStatus.CLOSED -> R.string.registration_status_closed
        YdsExamService.RegistrationStatus.NO_UPCOMING_EXAM -> R.string.registration_status_none
    }
    return stringResource(id = resId)
}

private fun formatDate(date: java.time.LocalDate): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
    return date.format(formatter)
}

@Composable
private fun YdsExamGradientTopBar(
    onNavigateBack: () -> Unit,
    daysRemaining: Int
) {
    // Header Section with gradient background - identical to homepage topbar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFBE9E7), // Light pastel red/pink
                            Color(0xFFE3F2FD)  // Light pastel blue
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                            tint = Color(0xFF424242), // Same as homepage text color
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.yds_exam_info_title),
                            fontSize = 20.sp, // Reduced from 24sp
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF424242) // Same as homepage
                        )
                        Text(
                            text = stringResource(id = R.string.yds_exam_info_subtitle),
                            fontSize = 14.sp,
                            color = Color(0xFF616161) // Same as homepage
                        )
                    }
                }

                // Days remaining counter
                if (daysRemaining > 0) {
                    Surface(
                        color = Color(0xFF424242).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = daysRemaining.toString() + stringResource(id = R.string.days_short_suffix),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }
    }
}





