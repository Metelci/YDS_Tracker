@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.features.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.R
import com.mtlc.studyplan.ui.components.*
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.data.dataStore
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { OnboardingRepository(context.dataStore) }
    val settings = remember { PlanSettingsStore(context.dataStore) }
    val vm: OnboardingViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(repo, settings) as T
        }
    })

    var step by rememberSaveable { mutableStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val isGeneratingPlan by vm.isGeneratingPlan.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_title)) },
                actions = {
                    // Shared progress indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
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
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )

                            if (index < 2) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()), // Apply specific top and bottom padding
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
                    LoadingIndicator(
                        message = "Generating your personalized study plan...",
                        showProgress = true
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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

                if (step == 0) {
                    Spacer(modifier = Modifier.width(1.dp))
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
                    modifier = Modifier.animateContentSize(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingStepDate(vm: OnboardingViewModel) {
    val date by vm.examDate.collectAsState()
    val min = remember { LocalDate.now().plusWeeks(1) }
    Text(stringResource(R.string.exam_date_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    val selectable = object : androidx.compose.material3.SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val d = java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            return !d.isBefore(min)
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        yearRange = IntRange(LocalDate.now().year, LocalDate.now().year + 2),
        selectableDates = selectable
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) { // Wrap in Column
        DatePicker(state = state, title = null, showModeToggle = false)
        Spacer(modifier = Modifier.height(16.dp)) // Add spacer
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { vm.setExamDate(min) }, label = { Text(stringResource(R.string.pick_min_date)) })
            AssistChip(onClick = { vm.setExamDate(min.plusWeeks(4)) }, label = { Text(stringResource(R.string.pick_plus_month)) })
        }
    }
    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { millis ->
            val d = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            if (!d.isBefore(min)) vm.setExamDate(d)
        }
    }
}

@Composable
private fun OnboardingStepAvailability(vm: OnboardingViewModel) {
    val availability by vm.availability.collectAsState()
    Text(stringResource(R.string.availability_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.values().forEach { day ->
            val label = day.name.lowercase().replaceFirstChar { it.uppercase() }
            val value = availability[day] ?: 0
            Text("$label: ${value}min", style = MaterialTheme.typography.labelLarge)
            Slider(value = value.toFloat(), onValueChange = { vm.setAvailability(day, it.toInt()) }, valueRange = 0f..180f)
        }
    }
}

@Composable
private fun OnboardingStepSkills(vm: OnboardingViewModel) {
    val weights by vm.skillWeights.collectAsState()
    Text(stringResource(R.string.skills_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    SkillRow(label = stringResource(R.string.skill_grammar), value = weights.grammar) { vm.setSkillWeights(weights.copy(grammar = it)) }
    SkillRow(label = stringResource(R.string.skill_reading), value = weights.reading) { vm.setSkillWeights(weights.copy(reading = it)) }
    SkillRow(label = stringResource(R.string.skill_listening), value = weights.listening) { vm.setSkillWeights(weights.copy(listening = it)) }
    SkillRow(label = stringResource(R.string.skill_vocab), value = weights.vocab) { vm.setSkillWeights(weights.copy(vocab = it)) }
    Text(stringResource(R.string.weekly_plan_preview, 5), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun SkillRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Text("$label: ${"%.1f".format(value)}x", style = MaterialTheme.typography.labelLarge)
    Slider(value = value, onValueChange = onChange, valueRange = 0.5f..1.5f, steps = 10)
}
