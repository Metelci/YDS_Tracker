package com.mtlc.studyplan.exam

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
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

    Scaffold(
        topBar = {
            StudyPlanTopBar(
                title = "YDS Exam Information",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                showLanguageSwitcher = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                // Debug: Log exam data to ensure it's loaded
                println("DEBUG: Selected exam: ${exam.name}, date: ${exam.examDate}")
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
                                        Color(0xFF4FC3F7),
                                        Color(0xFF29B6F6)
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

            // Preparation Progress Card
            ExamInfoCard(
                title = "Preparation Progress",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overall Progress",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = examData.progressPercentage.toString() + "%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { examData.currentPreparationLevel },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Target Score: " + examData.targetScore,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    examData.progressPercentage < 30 -> "Just started"
                                    examData.progressPercentage < 60 -> "Making progress"
                                    examData.progressPercentage < 80 -> "Well prepared"
                                    else -> "Exam ready!"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
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
