@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.onboarding

// removed luminance-based dark theme checks
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.ui.responsive.OnboardingTypography
import com.mtlc.studyplan.ui.responsive.ResponsiveCard
import com.mtlc.studyplan.ui.responsive.ResponsiveChipRow
import com.mtlc.studyplan.ui.responsive.ResponsiveContainer
import com.mtlc.studyplan.ui.responsive.ResponsiveLazyColumn
import com.mtlc.studyplan.ui.responsive.adaptiveComponentSizing
import com.mtlc.studyplan.ui.responsive.adaptiveFontScale
import com.mtlc.studyplan.ui.responsive.adaptiveLayoutConfig
import com.mtlc.studyplan.ui.responsive.isVerySmallScreen
import com.mtlc.studyplan.ui.responsive.performanceConfig
import com.mtlc.studyplan.ui.responsive.rememberDeviceProfile
import com.mtlc.studyplan.ui.responsive.rememberSafeAreaInsets
import com.mtlc.studyplan.ui.responsive.responsiveHeights
import com.mtlc.studyplan.ui.responsive.responsiveOnboardingTypography
import com.mtlc.studyplan.ui.theme.DesignTokens
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    val vm: OnboardingViewModel = koinViewModel()

    var step by rememberSaveable { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val isGeneratingPlan by vm.isGeneratingPlan.collectAsState()

    val colorScheme = MaterialTheme.colorScheme
    val topBarGradientColors = remember(colorScheme) {
        listOf(
            colorScheme.primaryContainer.copy(alpha = 1f),
            colorScheme.secondaryContainer.copy(alpha = 1f),
            colorScheme.tertiaryContainer.copy(alpha = 1f)
        )
    }
    val topBarTitleColor = colorScheme.onPrimaryContainer
    val topBarSubtitleColor = colorScheme.onSurfaceVariant
    val indicatorColor = colorScheme.onPrimaryContainer

    // Enhanced device compatibility
    rememberDeviceProfile()
    val safeAreaInsets = rememberSafeAreaInsets()
    performanceConfig()
    adaptiveLayoutConfig()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // Settings-style topbar with pastel gradient
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
                                colors = topBarGradientColors,
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Personalize Your Plan",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = topBarTitleColor
                            )
                            Text(
                                text = "Set up your study preferences",
                                fontSize = 14.sp,
                                color = topBarSubtitleColor
                            )
                        }

                        // Progress indicator in top right (replacing language switcher)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) { index ->
                                val isActive = index <= step
                                val targetScale = if (isActive) 1f else 0.6f
                                val targetAlpha = if (isActive) 1f else 0.4f

                                val scale by animateFloatAsState(
                                    targetValue = targetScale,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "progress_indicator_scale_$index"
                                )

                                val alpha by animateFloatAsState(
                                    targetValue = targetAlpha,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = FastOutSlowInEasing
                                    ),
                                    label = "progress_indicator_alpha_$index"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(scale)
                                        .graphicsLayer { this.alpha = alpha }
                                        .background(
                                            color = indicatorColor,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Show loading state when generating plan
            if (isGeneratingPlan) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = maxOf(padding.calculateTopPadding(), safeAreaInsets.top),
                            bottom = maxOf(padding.calculateBottomPadding(), safeAreaInsets.bottom),
                            start = safeAreaInsets.start,
                            end = safeAreaInsets.end
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating your personalized study plan...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "This may take a moment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val controlsVerticalPadding = 16.dp
                val controlHeight = 56.dp
                val contentBottomPadding = safeAreaInsets.bottom + controlHeight + (controlsVerticalPadding * 2)
                // Animated content with slide transitions
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            // Moving forward
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(
                                animationSpec = tween(200, easing = FastOutLinearInEasing)
                            )
                        } else {
                            // Moving backward
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(
                                animationSpec = tween(200, easing = FastOutLinearInEasing)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = maxOf(padding.calculateTopPadding(), safeAreaInsets.top),
                            bottom = contentBottomPadding,
                            start = safeAreaInsets.start,
                            end = safeAreaInsets.end
                        ),
                    label = "onboarding_step_transition"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> OnboardingStepDate(vm)
                        1 -> OnboardingStepAvailability(vm)
                        else -> OnboardingStepSkills()
                    }
                }
                // Bottom navigation controls
                val nextIcon = if (step < 2) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.Check
                val nextLabel = if (step < 2) {
                    stringResource(R.string.next_onboarding)
                } else {
                    stringResource(R.string.generate_plan)
                }
                val nextEnabled = !isGeneratingPlan

                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = controlsVerticalPadding),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = {
                                    if (nextEnabled) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        step -= 1
                                    }
                                },
                                enabled = nextEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_onboarding)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.back_onboarding))
                            }
                        }

                        Button(
                            onClick = {
                                if (!nextEnabled) return@Button
                                if (step < 2) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    step += 1
                                } else {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.finish(onDone)
                                }
                            },
                            enabled = nextEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = if (step > 0) Modifier.weight(1f) else Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = nextIcon,
                                contentDescription = nextLabel
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = nextLabel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingStepDate(vm: OnboardingViewModel) {
    val examDate by vm.examDate.collectAsState()
    val startDate by vm.startDate.collectAsState()

    var selectedMode by remember { mutableStateOf("exam") }
    var selectedDuration by remember { mutableStateOf(3) }

    val minStartDate = remember { LocalDate.now().minusDays(1) }
    val typography = responsiveOnboardingTypography()
    val heights = responsiveHeights()
    val isVerySmall = isVerySmallScreen()
    rememberDeviceProfile()
    adaptiveComponentSizing()
    adaptiveLayoutConfig()
    adaptiveFontScale()

    val finalEndDate = remember(startDate, examDate, selectedMode, selectedDuration) {
        if (selectedMode == "duration") {
            startDate.plusMonths(selectedDuration.toLong())
        } else {
            examDate
        }
    }

    val studyPeriod = remember(startDate, finalEndDate) {
        val period = java.time.Period.between(startDate, finalEndDate)
        if (period.isNegative) java.time.Period.ofDays(1) else period
    }

    val totalWeeks = remember(startDate, finalEndDate) {
        maxOf(0L, ChronoUnit.WEEKS.between(startDate, finalEndDate)).toInt()
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM dd") }
    val fullFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    val totalMonths = remember(studyPeriod) { (studyPeriod.years * 12) + studyPeriod.months }
    val durationLabel = remember(totalMonths, studyPeriod) {
        buildString {
            if (totalMonths > 0) {
                append("$totalMonths month")
                if (totalMonths != 1) append('s')
            }
            if (studyPeriod.days > 0) {
                if (isNotEmpty()) append(", ")
                append("${studyPeriod.days} day")
                if (studyPeriod.days != 1) append('s')
            }
            if (isEmpty()) append("Less than a week")
        }
    }
    val weeksLabel = remember(totalWeeks) {
        if (totalWeeks > 0) {
            "$totalWeeks week" + if (totalWeeks != 1) "s" else ""
        } else {
            "Less than a week"
        }
    }

    val suggestion = remember(totalWeeks) { plannerSuggestionForWeeks(totalWeeks) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val updateExamDate: (LocalDate) -> Unit = remember(selectedMode, startDate) {
        { proposed ->
            val coerced = if (proposed.isBefore(startDate.plusWeeks(1))) {
                startDate.plusWeeks(1)
            } else {
                proposed
            }
            vm.setExamDate(coerced)
            if (selectedMode == "duration") {
                val months = ((ChronoUnit.DAYS.between(startDate, coerced) / 30.0).roundToInt()).coerceIn(1, 12)
                selectedDuration = months
            }
        }
    }

    val quickDurationWeeks = listOf(4, 6, 8, 12)

    ResponsiveLazyColumn {
        item {
            ResponsiveContainer {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Plan Your Study Schedule",
                        style = typography.stepTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pick your prep window and we rebalance the workload instantly.",
                        style = typography.cardSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            PlannerGradientCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Planning mode",
                    style = typography.cardTitle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Countdown to an exam date or choose a fixed duration ÔÇö switch anytime.",
                    style = typography.cardSubtitle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedMode == "exam",
                        onClick = { selectedMode = "exam" },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        icon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Target exam date") },
                        label = { Text("Target exam date") }
                    )
                    SegmentedButton(
                        selected = selectedMode == "duration",
                        onClick = {
                            selectedMode = "duration"
                            updateExamDate(startDate.plusMonths(selectedDuration.toLong()))
                        },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        icon = { Icon(Icons.Filled.Schedule, contentDescription = "Study duration") },
                        label = { Text("Study duration") }
                    )
                }
            }
        }

        item {
            PlannerGradientCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                gradient = plannerTonalGradient()
            ) {
                Text(
                    text = "Schedule period",
                    style = typography.cardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Adjust start and end dates without leaving onboarding.",
                    style = typography.cardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PlannerDateField(
                    label = "Start date",
                    value = startDate.format(dateFormatter),
                    typography = typography,
                    icon = Icons.Filled.Flag,
                    onClick = { showStartPicker = true }
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                if (selectedMode == "exam") {
                    PlannerDateField(
                        label = "Exam date",
                        value = examDate.format(dateFormatter),
                        typography = typography,
                        icon = Icons.Filled.Event,
                        supportingText = "Minimum one week after your start date",
                        onClick = { showEndPicker = true }
                    )
                } else {
                    PlannerDateField(
                        label = "End date",
                        value = finalEndDate.format(dateFormatter),
                        typography = typography,
                        icon = Icons.Filled.CheckCircle,
                        supportingText = "$selectedDuration month" + if (selectedDuration != 1) "s" else "",
                        onClick = { showEndPicker = true }
                    )

                    Text(
                        text = "Quick picks",
                        style = typography.cardSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    ResponsiveChipRow {
                        quickDurationWeeks.forEach { weeks ->
                            val isClose = abs(totalWeeks - weeks) <= 1
                            AssistChip(
                                onClick = { updateExamDate(startDate.plusWeeks(weeks.toLong())) },
                                label = { Text("${weeks}w") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = stringResource(R.string.cd_goal),
                                        modifier = Modifier.size(if (isVerySmall) 16.dp else 18.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isClose) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                    },
                                    labelColor = if (isClose) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    leadingIconContentColor = if (isClose) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            )
                        }
                    }

                    Slider(
                        value = selectedDuration.toFloat(),
                        onValueChange = { newValue ->
                            val coerced = newValue.roundToInt().coerceIn(1, 12)
                            if (coerced != selectedDuration) {
                                selectedDuration = coerced
                                updateExamDate(startDate.plusMonths(coerced.toLong()))
                            }
                        },
                        valueRange = 1f..12f,
                        steps = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heights.slider)
                            .padding(top = 12.dp)
                    )
                }
            }
        }

        item {
            PlannerGradientCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                gradient = plannerInfoGradient()
            ) {
                Text(
                    text = "Study plan preview",
                    style = typography.cardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StudyPlanPreviewGrid(
                    startLabel = startDate.format(fullFormatter),
                    endLabel = finalEndDate.format(fullFormatter),
                    durationLabel = durationLabel,
                    weeksLabel = weeksLabel,
                    typography = typography,
                    isCompact = isVerySmall
                )
                PlannerSuggestionCard(
                    suggestion = suggestion,
                    typography = typography,
                    onExtendByWeeks = { extraWeeks ->
                        updateExamDate(finalEndDate.plusWeeks(extraWeeks.toLong()))
                    }
                )
            }
        }
    }

    if (showStartPicker) {
        val startPickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            yearRange = IntRange(LocalDate.now().year, LocalDate.now().year + 1),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = java.time.Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    return !date.isBefore(minStartDate)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startPickerState.selectedDateMillis?.let { millis ->
                            val newStart = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            vm.setStartDate(newStart)
                            if (selectedMode == "duration") {
                                vm.setExamDate(newStart.plusMonths(selectedDuration.toLong()))
                            } else if (examDate.isBefore(newStart.plusWeeks(1))) {
                                vm.setExamDate(newStart.plusWeeks(1))
                            }
                        }
                        showStartPicker = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = startPickerState,
                showModeToggle = false
            )
        }
    }

    if (showEndPicker) {
        val endPickerState = rememberDatePickerState(
            initialSelectedDateMillis = finalEndDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            yearRange = IntRange(LocalDate.now().year, LocalDate.now().year + 2),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = java.time.Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    return !date.isBefore(startDate.plusWeeks(1))
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endPickerState.selectedDateMillis?.let { millis ->
                            val newDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            updateExamDate(newDate)
                        }
                        showEndPicker = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = endPickerState,
                showModeToggle = false
            )
        }
    }

    LaunchedEffect(selectedDuration, startDate, selectedMode) {
        if (selectedMode == "duration") {
            vm.setExamDate(startDate.plusMonths(selectedDuration.toLong()))
        }
    }
}

private val PlannerGradientStart = Color(0xFF67AAC8)
private val PlannerGradientMid = Color(0xFF84AB86)
private val PlannerGradientEnd = Color(0xFFCC8974)

@Composable
private fun plannerPrimaryGradient(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    val colors = listOf(
        lerp(PlannerGradientStart, surface, 0.25f),
        lerp(PlannerGradientMid, surface, 0.3f),
        lerp(PlannerGradientEnd, surface, 0.2f)
    )
    return Brush.linearGradient(colors = colors, start = Offset.Zero, end = Offset.Infinite)
}

@Composable
private fun plannerTonalGradient(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    val colors = listOf(
        lerp(PlannerGradientStart, surface, 0.55f),
        lerp(PlannerGradientMid, surface, 0.6f),
        lerp(PlannerGradientEnd, surface, 0.55f)
    )
    return Brush.linearGradient(colors = colors, start = Offset.Zero, end = Offset.Infinite)
}

@Composable
private fun plannerInfoGradient(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    val colors = listOf(
        lerp(PlannerGradientStart, surface, 0.7f),
        lerp(PlannerGradientMid, surface, 0.75f),
        lerp(PlannerGradientEnd, surface, 0.7f)
    )
    return Brush.linearGradient(colors = colors, start = Offset.Zero, end = Offset.Infinite)
}

@Composable
private fun PlannerGradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush = plannerPrimaryGradient(),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(gradient, shape)
                .border(1.dp, outline, shape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PlannerDateField(
    label: String,
    value: String,
    typography: OnboardingTypography,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: ImageVector = Icons.Filled.CalendarToday
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = typography.cardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = typography.previewValue.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = typography.cardSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyPlanPreviewGrid(
    startLabel: String,
    endLabel: String,
    durationLabel: String,
    weeksLabel: String,
    typography: OnboardingTypography,
    isCompact: Boolean
) {
    if (isCompact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudyPlanPreviewItem("Start date", startLabel, typography)
            StudyPlanPreviewItem("End date", endLabel, typography)
            StudyPlanPreviewItem("Duration", durationLabel, typography)
            StudyPlanPreviewItem("Total weeks", weeksLabel, typography)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StudyPlanPreviewItem(
                    label = "Start date",
                    value = startLabel,
                    typography = typography,
                    modifier = Modifier.weight(1f)
                )
                StudyPlanPreviewItem(
                    label = "End date",
                    value = endLabel,
                    typography = typography,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StudyPlanPreviewItem(
                    label = "Duration",
                    value = durationLabel,
                    typography = typography,
                    modifier = Modifier.weight(1f)
                )
                StudyPlanPreviewItem(
                    label = "Total weeks",
                    value = weeksLabel,
                    typography = typography,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private enum class PlannerSuggestionTone { Warning, Info, Success }

private data class PlannerSuggestion(
    val tone: PlannerSuggestionTone,
    val headline: String,
    val body: String,
    val icon: ImageVector,
    val recommendedWeeks: List<Int>
)

private fun plannerSuggestionForWeeks(weeks: Int): PlannerSuggestion {
    return when {
        weeks <= 4 -> PlannerSuggestion(
            tone = PlannerSuggestionTone.Warning,
            headline = "Four weeks is a sprint",
            body = "Extend your plan by 2ÔÇô4 weeks or schedule intensive weekend sessions to cover all sections.",
            icon = Icons.Filled.WarningAmber,
            recommendedWeeks = listOf(2, 4)
        )
        weeks in 5..7 -> PlannerSuggestion(
            tone = PlannerSuggestionTone.Info,
            headline = "Tight but doable",
            body = "Aim for two deep practice blocks each week. Adding a 2-week buffer keeps revision realistic.",
            icon = Icons.Filled.Lightbulb,
            recommendedWeeks = listOf(2)
        )
        weeks >= 20 -> PlannerSuggestion(
            tone = PlannerSuggestionTone.Success,
            headline = "Great runway",
            body = "Use the extra time for full mocks and spaced repetition. Keep checking weak topics monthly.",
            icon = Icons.Filled.TaskAlt,
            recommendedWeeks = emptyList()
        )
        else -> PlannerSuggestion(
            tone = PlannerSuggestionTone.Info,
            headline = "Balanced schedule",
            body = "Stay consistent with weekly checkpoints. Add a buffer week if you expect travel or busy periods.",
            icon = Icons.Filled.AutoAwesome,
            recommendedWeeks = listOf(1)
        )
    }
}

@Composable
private fun PlannerSuggestionCard(
    suggestion: PlannerSuggestion,
    typography: OnboardingTypography,
    onExtendByWeeks: (Int) -> Unit
) {
    val (containerColor, contentColor) = when (suggestion.tone) {
        PlannerSuggestionTone.Warning -> DesignTokens.Warning.copy(alpha = 0.18f) to DesignTokens.Warning
        PlannerSuggestionTone.Info -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) to MaterialTheme.colorScheme.onPrimaryContainer
        PlannerSuggestionTone.Success -> DesignTokens.Success.copy(alpha = 0.18f) to DesignTokens.Success
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = suggestion.icon,
                    contentDescription = suggestion.headline,
                    tint = contentColor
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = suggestion.headline,
                        style = typography.cardTitle,
                        color = contentColor
                    )
                    Text(
                        text = suggestion.body,
                        style = typography.cardSubtitle,
                        color = contentColor.copy(alpha = 0.9f)
                    )
                }
            }

            if (suggestion.recommendedWeeks.isNotEmpty()) {
                ResponsiveChipRow {
                    suggestion.recommendedWeeks.forEach { weeks ->
                        AssistChip(
                            onClick = { onExtendByWeeks(weeks) },
                            label = { Text("Add ${weeks}w") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.Transparent,
                                labelColor = contentColor,
                                leadingIconContentColor = contentColor
                            ),
                            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyPlanPreviewItem(
    label: String,
    value: String,
    typography: OnboardingTypography,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = typography.cardSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = typography.cardSubtitle.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OnboardingStepAvailability(vm: OnboardingViewModel) {
    val availability by vm.availability.collectAsState()
    val typography = responsiveOnboardingTypography()
    val heights = responsiveHeights()
    val cardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    val sliderHeight = (heights.slider - 16.dp).coerceAtLeast(20.dp)
    val sliderColors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent
    )

    ResponsiveLazyColumn {
        item {
            ResponsiveCard(
                containerColor = cardColor
            ) {
                Text(
                    stringResource(R.string.availability_title),
                    style = typography.cardTitle
                )
                DayOfWeek.entries
                    .sortedBy { it.value }
                    .forEach { day ->
                        val label = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        val minutes = (availability[day] ?: 0).coerceIn(0, 180)
                        AvailabilityRow(
                            label = label,
                            minutes = minutes,
                            typography = typography,
                            sliderColors = sliderColors,
                            sliderHeight = sliderHeight,
                            onMinutesChanged = { snapped ->
                                vm.setAvailability(day, snapped)
                            }
                        )
                    }
            }
        }
    }
}

@Composable
private fun AvailabilityRow(
    label: String,
    minutes: Int,
    typography: OnboardingTypography,
    sliderColors: SliderColors,
    sliderHeight: Dp,
    onMinutesChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = typography.sliderLabel,
            modifier = Modifier.widthIn(min = 32.dp)
        )
        Slider(
            value = minutes.toFloat(),
            onValueChange = { newValue ->
                val snapped = (newValue / 15f).roundToInt().coerceIn(0, 12) * 15
                onMinutesChanged(snapped)
            },
            valueRange = 0f..180f,
            steps = 11,
            colors = sliderColors,
            modifier = Modifier
                .weight(1f)
                .height(sliderHeight)
        )
        Text(
            text = "$minutes min",
            style = typography.sliderLabel,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 56.dp)
        )
    }
}

@Composable
private fun OnboardingStepSkills() {
    val typography = responsiveOnboardingTypography()
    ResponsiveCard(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.skills_title),
                style = typography.cardTitle
            )
            Text(
                stringResource(R.string.skill_weights_placeholder_title),
                style = typography.cardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.skill_weights_placeholder_body),
                style = typography.chipText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
