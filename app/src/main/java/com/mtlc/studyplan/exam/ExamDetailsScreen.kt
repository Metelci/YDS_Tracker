package com.mtlc.studyplan.exam

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
        examCountdownManager.forceRefresh()
        // Try best‑effort refresh from ÖSYM; fall back to built‑in data if parsing fails
        runCatching {
            val fetched = OsymExamCalendarClient.fetchYdsExams()
            if (fetched.isNotEmpty()) {
                allUpcomingExams = fetched
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom YDS Exam Information Top Bar (matching Settings topbar design)
        YdsExamGradientTopBar(
            onNavigateBack = onNavigateBack,
            daysRemaining = selectedExam?.let { exam ->
                ChronoUnit.DAYS.between(LocalDate.now(), exam.examDate).toInt()
            } ?: 0
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
                    text = "Upcoming YDS Exams",
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
                            contentDescription = "No Exams",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Upcoming YDS Exams",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Check back later for new exam announcements from ÖSYM",
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
                val daysToExam = ChronoUnit.DAYS.between(LocalDate.now(), exam.examDate).toInt()
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
                                text = "Days Remaining",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getStatusMessageForExam(exam),
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Enhanced Exam Information Card with Apply Button
                ExamInfoCardWithAction(
                    title = "Exam Information",
                    icon = Icons.Filled.Event,
                    examSession = exam,
                    daysRemaining = daysToExam,
                    content = {
                        InfoRow("Exam Name", exam.name.takeIf { it.isNotBlank() } ?: "YDS Exam")
                        InfoRow("Exam Date", formatDate(exam.examDate))
                        InfoRow("Exam Time", "10:15") // Standard YDS time
                        InfoRow("Duration", "3 hours")
                        InfoRow("Question Count", "80 questions")
                    }
                )

                // Enhanced Registration Information Card with Apply Button
                ExamInfoCardWithAction(
                    title = "Registration Information",
                    icon = Icons.Filled.AssignmentInd,
                    examSession = exam,
                    daysRemaining = daysToExam,
                    content = {
                        InfoRow("Registration Status", getRegistrationStatusText(registrationStatus))
                        InfoRow("Application Period", formatDate(exam.registrationStart) + " - " + formatDate(exam.registrationEnd))
                        InfoRow("Late Registration", "Until " + formatDate(exam.lateRegistrationEnd))
                        InfoRow("Results Date", formatDate(exam.resultDate))
                    }
                )
            }


            // Tips and Reminders Card
            ExamInfoCard(
                title = "Important Reminders",
                icon = Icons.Filled.Lightbulb,
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TipItem("📋", "Bring your ID and exam document")
                        TipItem("⏰", "Arrive 30 minutes before exam time")
                        TipItem("✏️", "Bring HB pencil and eraser")
                        TipItem("📱", "Turn off all electronic devices")
                        TipItem("🎯", "Read questions carefully")
                    }
                }
            )
        }
    }
}

@Composable
private fun ExamInfoCardWithAction(
    title: String,
    icon: ImageVector,
    examSession: YdsExamService.YdsExamSession,
    daysRemaining: Int,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val registrationStatus = getRegistrationStatusForExam(examSession)
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
                        colors = if (title == "Exam Information") {
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

                // Apply Button in top-right corner for registration card
                if (title == "Registration Information" &&
                    (registrationStatus == YdsExamService.RegistrationStatus.OPEN ||
                     registrationStatus == YdsExamService.RegistrationStatus.LATE_REGISTRATION)) {

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(examSession.applicationUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle case where no browser app is available
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .width(80.dp)
                    ) {
                        Text(
                            text = "Apply",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
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
                        text = daysRemaining.toString() + " days remaining",
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
                if (title == "Exam Information" &&
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
                            contentDescription = "Apply",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Apply",
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
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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

private fun getStatusMessageForExam(exam: YdsExamService.YdsExamSession): String {
    val daysToExam = ChronoUnit.DAYS.between(LocalDate.now(), exam.examDate).toInt()
    val registrationStatus = getRegistrationStatusForExam(exam)

    return when {
        daysToExam == 0 -> "Exam day!"
        daysToExam < 0 -> "Exam completed"
        daysToExam <= 7 -> "Final week!"
        daysToExam <= 30 -> "Almost there!"
        daysToExam <= 90 -> when (registrationStatus) {
            YdsExamService.RegistrationStatus.NOT_OPEN_YET -> "Registration opens soon"
            YdsExamService.RegistrationStatus.OPEN -> "Registration open!"
            YdsExamService.RegistrationStatus.LATE_REGISTRATION -> "Late registration period"
            YdsExamService.RegistrationStatus.CLOSED -> "Registration closed"
            YdsExamService.RegistrationStatus.NO_UPCOMING_EXAM -> "Preparation time"
        }
        else -> "Long-term planning"
    }
}

private fun formatShortDate(date: LocalDate): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd")
    return date.format(formatter)
}

private fun getRegistrationStatusText(status: YdsExamService.RegistrationStatus): String {
    return when (status) {
        YdsExamService.RegistrationStatus.NOT_OPEN_YET -> "Opens Soon"
        YdsExamService.RegistrationStatus.OPEN -> "Open"
        YdsExamService.RegistrationStatus.LATE_REGISTRATION -> "Late Registration"
        YdsExamService.RegistrationStatus.CLOSED -> "Closed"
        YdsExamService.RegistrationStatus.NO_UPCOMING_EXAM -> "No Upcoming Exam"
    }
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            contentDescription = "Back",
                            tint = Color(0xFF424242), // Same as homepage text color
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "YDS Exam Information",
                            fontSize = 20.sp, // Reduced from 24sp
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF424242) // Same as homepage
                        )
                        Text(
                            text = "Track your exam preparation",
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
                            text = "${daysRemaining}d",
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

