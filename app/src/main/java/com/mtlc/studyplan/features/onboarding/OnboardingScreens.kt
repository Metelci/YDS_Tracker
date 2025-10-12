@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.features.onboarding

// removed luminance-based dark theme checks
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.mtlc.studyplan.ui.responsive.ScreenSize
import com.mtlc.studyplan.ui.responsive.adaptiveComponentSizing
import com.mtlc.studyplan.ui.responsive.adaptiveFontScale
import com.mtlc.studyplan.ui.responsive.adaptiveLayoutConfig
import com.mtlc.studyplan.ui.responsive.isVerySmallScreen
import com.mtlc.studyplan.ui.responsive.performanceConfig
import com.mtlc.studyplan.ui.responsive.rememberDeviceProfile
import com.mtlc.studyplan.ui.responsive.rememberSafeAreaInsets
import com.mtlc.studyplan.ui.responsive.rememberScreenSize
import com.mtlc.studyplan.ui.responsive.rememberScreenWidth
import com.mtlc.studyplan.ui.responsive.responsiveHeights
import com.mtlc.studyplan.ui.responsive.responsiveOnboardingTypography
import com.mtlc.studyplan.ui.responsive.touchTargetSize
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale


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
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    top = maxOf(padding.calculateTopPadding(), safeAreaInsets.top),
                    bottom = maxOf(padding.calculateBottomPadding(), safeAreaInsets.bottom),
                    start = safeAreaInsets.start,
                    end = safeAreaInsets.end
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show loading state when generating plan
            if (isGeneratingPlan) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                    modifier = Modifier.weight(1f),
                    label = "onboarding_step_transition"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> OnboardingStepDate(vm)
                        1 -> OnboardingStepAvailability(vm)
                        else -> OnboardingStepSkills(vm)
                    }
                }
            }

            // Navigation buttons with animations
            ResponsiveContainer {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                AnimatedVisibility(
                    visible = step > 0,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut()
                ) {
                    TextButton(onClick = { step -= 1 }) {
                        Text(stringResource(R.string.back_onboarding))
                    }
                }

                Button(
                    onClick = {
                        if (step < 2) {
                            // Light feedback for next step
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            step += 1
                        } else {
                            // Special feedback for completing onboarding
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.finish(onDone)
                        }
                    },
                    enabled = !isGeneratingPlan,
                    modifier = Modifier
                        .height(touchTargetSize())
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                ) {
                    Text(
                        if (step < 2) stringResource(R.string.next_onboarding)
                        else stringResource(R.string.generate_plan)
                    )
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
    var selectedMode by remember { mutableStateOf("exam") } // "exam" or "duration"
    var selectedDuration by remember { mutableStateOf(3) } // months

    remember { LocalDate.now().plusWeeks(1) }
    val minStartDate = remember { LocalDate.now().minusDays(1) } // Allow starting yesterday

    // Enhanced responsive utilities
    val typography = responsiveOnboardingTypography()
    val heights = responsiveHeights()
    val isVerySmall = isVerySmallScreen()
    val deviceProfile = rememberDeviceProfile()
    val adaptiveSizing = adaptiveComponentSizing()
    adaptiveLayoutConfig()
    adaptiveFontScale()

    // Calculate study period
    val studyPeriod = remember(startDate, examDate, selectedMode, selectedDuration) {
        val period = if (selectedMode == "exam") {
            java.time.Period.between(startDate, examDate)
        } else {
            java.time.Period.ofMonths(selectedDuration)
        }
        // Ensure period is not negative
        if (period.isNegative) java.time.Period.ofDays(1) else period
    }

    // Calculate end date for duration mode
    val calculatedEndDate = remember(startDate, selectedDuration, selectedMode) {
        if (selectedMode == "duration") {
            startDate.plusMonths(selectedDuration.toLong())
        } else {
            examDate
        }
    }

    val finalEndDate = if (selectedMode == "duration") calculatedEndDate else examDate

    val datePickerHeight = remember(deviceProfile, adaptiveSizing) {
        if (deviceProfile.isLandscape) {
            (adaptiveSizing.datePicker + 80.dp).coerceAtLeast(340.dp)
        } else {
            (adaptiveSizing.datePicker + 80.dp).coerceAtLeast(440.dp)
        }
    }

    val screenSize = rememberScreenSize()
    val screenWidth = rememberScreenWidth()
    340.dp
    remember(screenSize, screenWidth) {
        when (screenSize) {
            ScreenSize.Mobile -> (screenWidth - 32.dp).coerceAtLeast(340.dp)
            ScreenSize.Tablet -> maxOf(screenWidth * 0.75f, 520.dp)
            ScreenSize.Desktop -> 720.dp
        }
    }

    ResponsiveLazyColumn {
        item {
            ResponsiveContainer {
                Text(
                    text = "Plan Your Study Schedule",
                    style = typography.stepTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Mode Selection
        item {
            ResponsiveCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Text(
                    text = "Study Plan Mode",
                    style = typography.cardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ResponsiveChipRow {
                    AssistChip(
                        onClick = { selectedMode = "exam" },
                        label = {
                            Text(
                                "Target Exam Date",
                                style = typography.chipText
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(if (isVerySmall) 16.dp else 18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedMode == "exam")
                                com.mtlc.studyplan.ui.theme.DesignTokens.PrimaryContainer
                            else MaterialTheme.colorScheme.surface,
                            labelColor = if (selectedMode == "exam")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            leadingIconContentColor = if (selectedMode == "exam")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    AssistChip(
                        onClick = {
                            selectedMode = "duration"
                            // Immediately update exam date when switching to duration mode
                            vm.setExamDate(startDate.plusMonths(selectedDuration.toLong()))
                        },
                        label = {
                            Text(
                                "Study Duration",
                                style = typography.chipText
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(if (isVerySmall) 16.dp else 18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedMode == "duration")
                                com.mtlc.studyplan.ui.theme.DesignTokens.PrimaryContainer
                            else MaterialTheme.colorScheme.surface,
                            labelColor = if (selectedMode == "duration")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            leadingIconContentColor = if (selectedMode == "duration")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // Start Date Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.mtlc.studyplan.ui.theme.DesignTokens.SecondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Start Date",
                        style = typography.cardTitle,
                        color = com.mtlc.studyplan.ui.theme.DesignTokens.SecondaryContainerForeground,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val startDateState = rememberDatePickerState(
                        initialSelectedDateMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        yearRange = IntRange(LocalDate.now().year, LocalDate.now().year + 1)
                    )

                    // Calendar with minimal padding to show all days
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                    ) {
                        DatePicker(
                            state = startDateState,
                            title = null,
                            showModeToggle = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(datePickerHeight)
                        )
                    }

                    LaunchedEffect(startDateState.selectedDateMillis) {
                        startDateState.selectedDateMillis?.let { millis ->
                            val newStartDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            if (!newStartDate.isBefore(minStartDate)) {
                                vm.setStartDate(newStartDate)
                            }
                        }
                    }
                }
            }
        }

        // End Date or Duration Selection
        item {
            if (selectedMode == "exam") {
                // Exam Date Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = com.mtlc.studyplan.ui.theme.DesignTokens.TertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "YDS Exam Date",
                            style = typography.cardTitle,
                            color = com.mtlc.studyplan.ui.theme.DesignTokens.TertiaryContainerForeground,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val examDateState = rememberDatePickerState(
                            initialSelectedDateMillis = examDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            yearRange = IntRange(LocalDate.now().year, LocalDate.now().year + 2),
                            selectableDates = object : SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                    val date = java.time.Instant.ofEpochMilli(utcTimeMillis)
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                                    return !date.isBefore(startDate.plusWeeks(1))
                                }
                            }
                        )

                        // Calendar with minimal padding to show all days
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp)
                        ) {
                            DatePicker(
                                state = examDateState,
                                title = null,
                                showModeToggle = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(datePickerHeight)
                            )
                        }

                        LaunchedEffect(examDateState.selectedDateMillis) {
                            examDateState.selectedDateMillis?.let { millis ->
                                val newExamDate = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                                if (!newExamDate.isBefore(startDate.plusWeeks(1)) && selectedMode == "exam") {
                                    vm.setExamDate(newExamDate)
                                }
                            }
                        }
                    }
                }
            } else {
                // Duration Selection
                ResponsiveCard(
                    containerColor = com.mtlc.studyplan.ui.theme.DesignTokens.TertiaryContainer
                ) {
                    Text(
                        text = "Study Duration",
                        style = typography.cardTitle,
                        color = com.mtlc.studyplan.ui.theme.DesignTokens.TertiaryContainerForeground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "$selectedDuration month${if (selectedDuration > 1) "s" else ""}",
                        style = typography.previewValue,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Slider(
                        value = selectedDuration.toFloat(),
                        onValueChange = { newValue ->
                            selectedDuration = newValue.toInt()
                            // Immediately update exam date when slider changes
                            if (selectedMode == "duration") {
                                vm.setExamDate(startDate.plusMonths(newValue.toLong()))
                            }
                        },
                        valueRange = 1f..6f,
                        steps = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heights.slider)
                    )

                    ResponsiveChipRow {
                        listOf(1, 3, 6).forEach { months ->
                            AssistChip(
                                onClick = {
                                    selectedDuration = months
                                    // Immediately update exam date when duration changes
                                    if (selectedMode == "duration") {
                                        vm.setExamDate(startDate.plusMonths(months.toLong()))
                                    }
                                },
                                label = {
                                    Text(
                                        "${months}mo",
                                        style = typography.chipText
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = if (selectedDuration == months)
                                        com.mtlc.studyplan.ui.theme.DesignTokens.PrimaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    // Update exam date for duration mode
                    LaunchedEffect(selectedDuration, startDate, selectedMode) {
                        if (selectedMode == "duration") {
                            vm.setExamDate(startDate.plusMonths(selectedDuration.toLong()))
                        }
                    }
                }
            }
        }

        // Study Plan Preview
        item {
            ResponsiveCard(
                containerColor = com.mtlc.studyplan.ui.theme.DesignTokens.SuccessContainer
            ) {
                Text(
                    text = "Study Plan Preview",
                    style = typography.cardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isVerySmall) {
                    // Stack items vertically on very small screens
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudyPlanPreviewItem(
                            label = "Start Date",
                            value = startDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            typography = typography
                        )
                        StudyPlanPreviewItem(
                            label = "End Date",
                            value = finalEndDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            typography = typography
                        )
                        StudyPlanPreviewItem(
                            label = "Duration",
                            value = "${studyPeriod.months} months, ${studyPeriod.days} days",
                            typography = typography
                        )
                        StudyPlanPreviewItem(
                            label = "Total Weeks",
                            value = "${(studyPeriod.toTotalMonths() * 4.3).toInt()} weeks",
                            typography = typography
                        )
                    }
                } else {
                    // Use rows on larger screens
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StudyPlanPreviewItem(
                                label = "Start Date",
                                value = startDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                typography = typography
                            )
                            StudyPlanPreviewItem(
                                label = "End Date",
                                value = finalEndDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                typography = typography
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StudyPlanPreviewItem(
                                label = "Duration",
                                value = "${studyPeriod.months} months, ${studyPeriod.days} days",
                                typography = typography
                            )
                            StudyPlanPreviewItem(
                                label = "Total Weeks",
                                value = "${(studyPeriod.toTotalMonths() * 4.3).toInt()} weeks",
                                typography = typography
                            )
                        }
                    }
                }

                // Validation warnings
                when {
                    studyPeriod.toTotalMonths() < 1 -> {
                        Text(
                            text = "⚠️ Very short study period. Consider extending for better preparation.",
                            style = typography.cardSubtitle,
                            color = com.mtlc.studyplan.ui.theme.DesignTokens.Warning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    studyPeriod.toTotalMonths() > 12 -> {
                        Text(
                            text = "📚 Long study period. Great for thorough preparation!",
                            style = typography.cardSubtitle,
                            color = com.mtlc.studyplan.ui.theme.DesignTokens.Success,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    studyPeriod.isNegative -> {
                        Text(
                            text = "❌ Invalid date range. Please check your start and end dates.",
                            style = typography.cardSubtitle,
                            color = com.mtlc.studyplan.ui.theme.DesignTokens.Error,
                            modifier = Modifier.fillMaxWidth()
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
    typography: OnboardingTypography
) {
    Column {
        Text(
            text = label,
            style = typography.cardSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = typography.cardSubtitle.copy(
                fontWeight = FontWeight.Medium
            ),
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
                        val label = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        val value = (availability[day] ?: 0).coerceIn(0, 180)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = typography.sliderLabel
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    text = "$value min",
                                    style = typography.sliderLabel,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Slider(
                                value = value.toFloat(),
                                onValueChange = { newValue ->
                                    vm.setAvailability(day, newValue.toInt())
                                },
                                valueRange = 0f..180f,
                                colors = sliderColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(sliderHeight)
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun OnboardingStepSkills(vm: OnboardingViewModel) {
    val weights by vm.skillWeights.collectAsState()
    val typography = responsiveOnboardingTypography()
    val heights = responsiveHeights()
    val cardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    val sliderHeight = (heights.slider - 8.dp).coerceAtLeast(24.dp)

    ResponsiveCard(
        containerColor = cardColor
    ) {
        Text(
            stringResource(R.string.skills_title),
            style = typography.cardTitle
        )
        SkillRow(
            label = stringResource(R.string.skill_grammar),
            value = weights.grammar,
            onChange = { vm.setSkillWeights(weights.copy(grammar = it)) },
            typography = typography,
            sliderHeight = sliderHeight
        )
        SkillRow(
            label = stringResource(R.string.skill_reading),
            value = weights.reading,
            onChange = { vm.setSkillWeights(weights.copy(reading = it)) },
            typography = typography,
            sliderHeight = sliderHeight
        )
        SkillRow(
            label = stringResource(R.string.skill_listening),
            value = weights.listening,
            onChange = { vm.setSkillWeights(weights.copy(listening = it)) },
            typography = typography,
            sliderHeight = sliderHeight
        )
        SkillRow(
            label = stringResource(R.string.skill_vocab),
            value = weights.vocab,
            onChange = { vm.setSkillWeights(weights.copy(vocab = it)) },
            typography = typography,
            sliderHeight = sliderHeight
        )
        Text(
            stringResource(R.string.weekly_plan_preview, 5),
            style = typography.cardSubtitle
        )
    }
}

@Composable
private fun SkillRow(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    typography: OnboardingTypography,
    sliderHeight: Dp
) {
    Text(
        "$label: ${"%.1f".format(value)}x",
        style = typography.sliderLabel
    )
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = 0.5f..1.5f,
        steps = 10,
        modifier = Modifier.height(sliderHeight)
    )
}

