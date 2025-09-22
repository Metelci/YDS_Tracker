package com.mtlc.studyplan.exam

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val examCountdownManager = remember { ExamCountdownManager.getInstance(context) }
    val examData by examCountdownManager.examData.collectAsState()
    val nextExam = YdsExamService.getNextExam()
    val registrationStatus = YdsExamService.getRegistrationStatus()

    // Refresh exam data when screen loads
    LaunchedEffect(Unit) {
        examCountdownManager.forceRefresh()
    }

    Scaffold(
        topBar = {
            StudyPlanTopBar(
                title = nextExam?.name ?: "YDS Exam Details",
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
                            text = "${examData.daysToExam}",
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
                            text = examData.statusMessage,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Exam Information Card
            nextExam?.let { exam ->
                ExamInfoCard(
                    title = "Exam Information",
                    icon = Icons.Filled.Event,
                    content = {
                        InfoRow("Exam Name", exam.name)
                        InfoRow("Exam Date", examData.examDateFormatted)
                        InfoRow("Exam Time", "10:15") // Standard YDS time
                        InfoRow("Duration", "3 hours")
                        InfoRow("Question Count", "80 questions")
                    }
                )
            }

            // Registration Information Card
            nextExam?.let { exam ->
                ExamInfoCard(
                    title = "Registration Information",
                    icon = Icons.Filled.AssignmentInd,
                    content = {
                        InfoRow("Registration Status", getRegistrationStatusText(registrationStatus))
                        InfoRow("Registration Start", formatDate(exam.registrationStart))
                        InfoRow("Registration End", formatDate(exam.registrationEnd))
                        InfoRow("Late Registration", formatDate(exam.lateRegistrationEnd))
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
                                text = "${examData.progressPercentage}%",
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
                                text = "Target Score: ${examData.targetScore}",
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
