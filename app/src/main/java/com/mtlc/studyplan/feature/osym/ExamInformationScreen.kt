@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.osym

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.YdsExamService
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * YDS Exam Information Screen - Displays detailed exam information
 * Matches the design from the provided screenshots exactly
 */
@Composable
fun ExamInformationScreen(
    exam: YdsExamService.YdsExamSession,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    // Calculate days remaining
    val today = java.time.LocalDate.now()
    val daysRemaining = ChronoUnit.DAYS.between(today, exam.examDate).toInt()

    // Get registration status
    val registrationStatus = YdsExamService.getRegistrationStatus()
    val statusText = when (registrationStatus) {
        YdsExamService.RegistrationStatus.NOT_OPEN_YET -> stringResource(R.string.registration_status_opens_soon)
        YdsExamService.RegistrationStatus.OPEN -> stringResource(R.string.registration_status_open)
        YdsExamService.RegistrationStatus.LATE_REGISTRATION -> stringResource(R.string.registration_status_late)
        YdsExamService.RegistrationStatus.CLOSED -> stringResource(R.string.registration_status_closed)
        YdsExamService.RegistrationStatus.NO_UPCOMING_EXAM -> stringResource(R.string.registration_status_none)
    }

    val prussianBlue = Color(0xFF003153)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFFEFF6FF), Color(0xFFF7FBFF))
                )
            )
    ) {
        // Header Section - identical to home page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE3F2FD) // Light blue solid color as fallback
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFE3F2FD), // Light blue
                                    Color(0xFFFCE4EC)  // Light peach/pink
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back button
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = prussianBlue
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.yds_exam_info_title),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = prussianBlue
                                )
                                Text(
                                    text = stringResource(R.string.yds_exam_info_subtitle),
                                    fontSize = 13.sp,
                                    color = prussianBlue.copy(alpha = 0.8f)
                                )
                            }
                        }

                    }
                }
            }
        }

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "Upcoming YDS Exams" header
            item {
                Text(
                    text = stringResource(R.string.upcoming_yds_exams),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )
            }

            // Days Remaining Card (Blue card)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFB3E5FC), // Light pastel blue
                                        Color(0xFFCE93D8)  // Light pastel purple
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                        Text(
                            text = daysRemaining.toString(),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.days_remaining),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.exam_status_almost_there),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

            // Exam Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFF9C4), // Light pastel yellow
                                        Color(0xFFFFE082)  // Slightly deeper pastel yellow
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                        // Header with icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "📅",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.exam_information),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F1F1F)
                            )
                        }

                        // Exam Name
                        InfoRow(
                            label = stringResource(R.string.exam_name_label),
                            value = exam.name
                        )

                        // Exam Date
                        InfoRow(
                            label = stringResource(R.string.exam_date_label),
                            value = exam.examDate.format(dateFormatter)
                        )

                        // Exam Time
                        InfoRow(
                            label = stringResource(R.string.exam_time_label),
                            value = stringResource(R.string.exam_time_value)
                        )

                        // Duration
                        InfoRow(
                            label = stringResource(R.string.exam_duration_label),
                            value = stringResource(R.string.exam_duration_value)
                        )

                        // Question Count
                        InfoRow(
                            label = stringResource(R.string.exam_question_count_label),
                            value = stringResource(R.string.exam_question_count_value)
                        )

                        // Days remaining badge
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFE0B2),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = stringResource(R.string.exam_info_days_remaining, daysRemaining),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

            // Registration Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFE8F5E9), // Light pastel green
                                        Color(0xFFC8E6C9)  // Slightly deeper pastel green
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                        // Header with icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "👤",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.registration_information),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F1F1F)
                            )
                        }

                        // Registration Status
                        InfoRow(
                            label = stringResource(R.string.registration_status_label),
                            value = statusText
                        )

                        // Application Period
                        InfoRow(
                            label = stringResource(R.string.application_period_label),
                            value = "${exam.registrationStart.format(dateFormatter)} - ${exam.registrationEnd.format(dateFormatter)}"
                        )

                        // Late Registration
                        InfoRow(
                            label = stringResource(R.string.late_registration_label),
                            value = exam.lateRegistrationEnd.format(dateFormatter)
                        )

                        // Results Date
                        InfoRow(
                            label = stringResource(R.string.results_date_label),
                            value = exam.resultDate.format(dateFormatter)
                        )

                        // Days remaining badge
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFE0B2),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = stringResource(R.string.exam_info_days_remaining, daysRemaining),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

            // Important Reminders Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFCE4EC), // Light pastel pink
                                        Color(0xFFF8BBD0)  // Slightly deeper pastel pink
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                        // Header with icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.important_reminders),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F1F1F)
                            )
                        }

                        // Reminders list
                        ReminderItem(
                            icon = "📋",
                            text = stringResource(R.string.tip_bring_id)
                        )
                        ReminderItem(
                            icon = "⏰",
                            text = stringResource(R.string.tip_arrive_early)
                        )
                        ReminderItem(
                            icon = "✏️",
                            text = stringResource(R.string.tip_bring_tools)
                        )
                        ReminderItem(
                            icon = "📱",
                            text = stringResource(R.string.tip_turn_off_devices)
                        )
                        ReminderItem(
                            icon = "🎯",
                            text = stringResource(R.string.tip_read_carefully),
                            isLast = true
                        )
                    }
                }
            }
        }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF757575)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F1F1F)
        )
    }
}

@Composable
private fun ReminderItem(
    icon: String,
    text: String,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF424242),
            modifier = Modifier.weight(1f)
        )
    }
    if (!isLast) {
        Spacer(modifier = Modifier.height(4.dp))
    }
}
