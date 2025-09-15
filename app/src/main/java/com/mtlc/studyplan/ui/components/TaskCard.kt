package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mtlc.studyplan.R
import com.mtlc.studyplan.ui.animations.StudyPlanMicroInteractions
import com.mtlc.studyplan.ui.animations.GestureEnhancements
import com.mtlc.studyplan.ui.animations.StateAnimations
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import com.mtlc.studyplan.Task
import com.mtlc.studyplan.ui.theme.*
import com.mtlc.studyplan.ui.components.estimateTaskMinutes
import com.mtlc.studyplan.data.TaskCategory
import com.mtlc.studyplan.data.StreakMultiplier

enum class Skill { GRAMMAR, READING, LISTENING, VOCAB, OTHER }

fun detectSkill(desc: String, details: String? = null): Skill {
    val s = (desc + " " + (details ?: "")).lowercase()
    return when {
        listOf("grammar", "gramer").any { s.contains(it) } -> Skill.GRAMMAR
        listOf("reading", "okuma").any { s.contains(it) } -> Skill.READING
        listOf("listening", "dinleme").any { s.contains(it) } -> Skill.LISTENING
        listOf("vocab", "kelime", "vocabulary").any { s.contains(it) } -> Skill.VOCAB
        else -> Skill.OTHER
    }
}

@Composable
fun skillColor(skill: Skill): Color = when (skill) {
    Skill.GRAMMAR -> SkillGrammar
    Skill.READING -> SkillReading
    Skill.LISTENING -> SkillListening
    Skill.VOCAB -> SkillVocab
    Skill.OTHER -> MaterialTheme.colorScheme.outline
}

@Composable
fun TaskCard(
    task: Task,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showExpandedDetails: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
    enableSwipeToComplete: Boolean = false,
    currentStreak: Int = 0, // New parameter for streak information
    showPointsInfo: Boolean = true // New parameter to show/hide points
) {
    val skill = remember(task) { detectSkill(task.desc, task.details) }
    val minutes = remember(task) { estimateTaskMinutes(task.desc, task.details) }
    val cd = remember(task, skill) { "${skill.name.lowercase()} ${task.desc}" }

    // Calculate points information
    val taskCategory = remember(task) { TaskCategory.fromString(task.desc + " " + (task.details ?: "")) }
    val streakMultiplier = remember(currentStreak) { StreakMultiplier.getMultiplierForStreak(currentStreak) }
    val basePoints = taskCategory.basePoints
    val totalPoints = (basePoints * streakMultiplier.multiplier).toInt()

    // Enhanced micro-interactions with comprehensive task completion animation
    val completionAnimation = StudyPlanMicroInteractions.enhancedTaskCompletionAnimation(
        isCompleted = checked,
        onProgressUpdate = { progress ->
            // Update any global progress indicators here
        },
        onAnimationComplete = {
            // Additional completion effects could go here
        }
    )

    var isLoadingDetails by remember { mutableStateOf(false) }
    var previousExpanded by remember { mutableStateOf(showExpandedDetails) }

    // Trigger loading animation when expanding details
    LaunchedEffect(showExpandedDetails) {
        if (showExpandedDetails != previousExpanded && showExpandedDetails) {
            isLoadingDetails = true
            // Simulate loading delay for progressive disclosure
            delay(500)
            isLoadingDetails = false
        }
        previousExpanded = showExpandedDetails
    }

    // Enhanced animation values using micro-interaction system
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.02f else 1f,
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        label = "task_completion_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (checked) 0.7f else 1f,
        animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
            normalSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ),
        label = "task_completion_alpha"
    )

    // Conditional swipe-to-complete wrapper
    if (enableSwipeToComplete) {
        GestureEnhancements.SwipeToCompleteCard(
            onComplete = { onCheckedChange(true) },
            modifier = modifier
                .fillMaxWidth()
                .scale(scale)
                .graphicsLayer { this.alpha = alpha }
                .animateContentSize(
                    animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                        normalSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                )
                .semantics { contentDescription = cd }
        ) { swipeProgress ->
            TaskCardContent(
                task = task,
                checked = checked,
                onCheckedChange = onCheckedChange,
                skill = skill,
                minutes = minutes,
                showExpandedDetails = showExpandedDetails,
                onExpandToggle = onExpandToggle,
                isLoadingDetails = isLoadingDetails,
                swipeProgress = swipeProgress,
                basePoints = basePoints,
                multiplier = streakMultiplier.multiplier,
                showPointsInfo = showPointsInfo,
                streakMultiplier = streakMultiplier
            )
        }
    } else {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale)
                .graphicsLayer { this.alpha = alpha }
                .animateContentSize(
                    animationSpec = StudyPlanMicroInteractions.adaptiveAnimationSpec(
                        normalSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                )
                .semantics { contentDescription = cd }
                .pressAnimation(
                    hapticFeedback = HapticFeedbackType.TextHandleMove
                ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (checked) 2.dp else 6.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            TaskCardContent(
                task = task,
                checked = checked,
                onCheckedChange = onCheckedChange,
                skill = skill,
                minutes = minutes,
                showExpandedDetails = showExpandedDetails,
                onExpandToggle = onExpandToggle,
                isLoadingDetails = isLoadingDetails,
                swipeProgress = 0f,
                basePoints = basePoints,
                multiplier = streakMultiplier.multiplier,
                showPointsInfo = showPointsInfo,
                streakMultiplier = streakMultiplier
            )
        }
    }
}

@Composable
private fun TaskCardContent(
    task: Task,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    skill: Skill,
    minutes: Int,
    showExpandedDetails: Boolean,
    onExpandToggle: (() -> Unit)?,
    isLoadingDetails: Boolean,
    swipeProgress: Float,
    basePoints: Int,
    multiplier: Float,
    showPointsInfo: Boolean,
    streakMultiplier: StreakMultiplier
) {
    // Visual feedback for swipe progress
    val backgroundColor by animateColorAsState(
        targetValue = androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer,
            swipeProgress
        ),
        label = "swipe_background"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable {
                if (onExpandToggle != null) {
                    onExpandToggle()
                } else {
                    onCheckedChange(!checked)
                }
            }
            .padding(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left colored strip with per-skill tonal mapping for current theme
            run {
                val isDark = isSystemInDarkTheme()
                val (lightA, darkA) = when (skill) {
                    Skill.GRAMMAR -> 0.14f to 0.30f  // blue: moderate tone change
                    Skill.READING -> 0.10f to 0.25f  // green: slightly subtler
                    Skill.LISTENING -> 0.08f to 0.40f // orange: stronger lift in dark
                    Skill.VOCAB -> 0.12f to 0.33f    // purple: balanced
                    Skill.OTHER -> 0.08f to 0.20f
                }
                val stripColor = skillStripTone(skillColor(skill), isDark, lightA, darkA)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .heightIn(min = 56.dp)
                        .background(stripColor)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(text = task.desc, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                task.details?.let {
                    if (it.isNotBlank()) Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.task_minutes, minutes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (showPointsInfo) {
                        InlinePointsDisplay(
                            basePoints = basePoints,
                            multiplier = multiplier,
                            compact = true
                        )
                    }
                }

                // Streak multiplier badge for significant multipliers
                if (showPointsInfo && multiplier > 1f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = if (streakMultiplier.isFireStreak) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (streakMultiplier.isFireStreak) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (streakMultiplier.isFireStreak) "🔥 ${streakMultiplier.title}" else streakMultiplier.title,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier
                        .padding(12.dp)
                        .graphicsLayer {
                            scaleX = completionAnimation.scale
                            scaleY = completionAnimation.scale
                            rotationZ = completionAnimation.rotation
                        },
                    colors = CheckboxDefaults.colors(
                        checkedColor = androidx.compose.ui.graphics.lerp(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                            completionAnimation.colorProgress
                        ),
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            // Expanded details section with progressive disclosure
            AnimatedVisibility(
                visible = showExpandedDetails,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    if (isLoadingDetails) {
                        // Show loading state
                        ProgressiveDisclosureLoading(
                            modifier = Modifier.padding(vertical = 8.dp),
                            isExpanding = true
                        )
                    } else {
                        // Show expanded content
                        Text(
                            text = "Task Details",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        task.details?.let { details ->
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Additional task metadata
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Skill: ${skill.name.lowercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = skillColor(skill)
                            )
                            Text(
                                text = "Est. $minutes min",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { /* Add reminder */ },
                                label = { Text("Set Reminder") }
                            )
                            AssistChip(
                                onClick = { /* View resources */ },
                                label = { Text("Resources") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskCardPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
        TaskCard(Task("t1", "Grammar Drills", "Tenses and modals"), checked = false, onCheckedChange = {})
        TaskCard(Task("t2", "Reading Practice", "News articles and summary"), checked = true, onCheckedChange = {})
        TaskCard(Task("t3", "Listening Session", "Podcast episode"), checked = false, onCheckedChange = {})
        TaskCard(Task("t4", "Vocab Review", "50 new words"), checked = false, onCheckedChange = {})
    }
}
