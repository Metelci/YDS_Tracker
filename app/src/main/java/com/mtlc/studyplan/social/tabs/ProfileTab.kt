package com.mtlc.studyplan.social.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.AvatarOption
import com.mtlc.studyplan.data.social.FakeSocialRepository
import com.mtlc.studyplan.data.social.SocialProfile
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

@Composable
fun ProfileTab(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit,
    onSaveGoal: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .padding(bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        ProfileCard(profile = profile, onAvatarSelected = onAvatarSelected)
        WeeklyGoalCard(profile = profile, onSaveGoal = onSaveGoal)
        PrivacySettingsCard()
    }
}

@Composable
private fun ProfileCard(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    Surface(
        color = DesignTokens.PrimaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.social_profile_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(id = R.string.social_copy_username_cd),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = stringResource(id = R.string.social_username_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            Text(
                text = stringResource(id = R.string.social_choose_avatar),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            AvatarGrid(
                options = profile.availableAvatars,
                selectedId = profile.selectedAvatarId,
                onAvatarSelected = onAvatarSelected
            )

            FilledTonalButton(
                onClick = { /* Upload custom avatar stub */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = DesignTokens.Surface)
            ) {
                Icon(imageVector = Icons.Outlined.Image, contentDescription = null)
                Text(
                    text = stringResource(id = R.string.social_upload_avatar),
                    modifier = Modifier.padding(start = spacing.xs)
                )
            }

            Surface(
                color = DesignTokens.Surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    InitialPreview(initial = profile.selectedAvatarId.take(2).uppercase())
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(id = R.string.social_study_level, profile.studyLevel),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvatarGrid(
    options: List<AvatarOption>,
    selectedId: String,
    onAvatarSelected: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        options.forEach { option ->
            val isSelected = option.id == selectedId
            val border = if (isSelected) BorderStroke(2.dp, DesignTokens.Primary) else null
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .clickable { onAvatarSelected(option.id) },
                color = DesignTokens.Surface,
                border = border,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = avatarEmoji(option.id),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (isSelected) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(18.dp),
                            shape = CircleShape,
                            color = DesignTokens.Primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✔",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DesignTokens.PrimaryForeground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialPreview(initial: String) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = DesignTokens.PrimaryContainer,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DesignTokens.PrimaryContainerForeground
            )
        }
    }
}

@Composable
private fun WeeklyGoalCard(
    profile: SocialProfile,
    onSaveGoal: (Int) -> Unit
) {
    val spacing = LocalSpacing.current
    var sliderValue by rememberSaveable(profile.weeklyGoalHours, profile.goalRange) {
        mutableStateOf(profile.weeklyGoalHours.toFloat())
    }
    Surface(
        color = DesignTokens.TertiaryContainer.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = stringResource(id = R.string.social_weekly_goal_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.social_goal_hours_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = stringResource(id = R.string.social_goal_hours_value, sliderValue.toInt()),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DesignTokens.Primary
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = profile.goalRange.first.toFloat()..profile.goalRange.last.toFloat(),
                    steps = (profile.goalRange.last - profile.goalRange.first) - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = DesignTokens.Success,
                        activeTrackColor = DesignTokens.Success,
                        inactiveTrackColor = DesignTokens.SurfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.social_goal_label_casual, profile.goalRange.first))
                    Text(stringResource(id = R.string.social_goal_label_balanced, profile.weeklyGoalHours))
                    Text(stringResource(id = R.string.social_goal_label_intensive, profile.goalRange.last))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.social_goal_daily_target, minutesPerDay(sliderValue.toInt())),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = stringResource(id = R.string.social_goal_monthly_target, hoursPerMonth(sliderValue.toInt())),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                OutlinedButton(
                    onClick = { sliderValue = profile.weeklyGoalHours.toFloat() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.social_goal_cancel))
                }
                FilledTonalButton(
                    onClick = { onSaveGoal(sliderValue.toInt()) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = DesignTokens.Primary)
                ) {
                    Text(
                        text = stringResource(id = R.string.social_goal_save),
                        color = DesignTokens.PrimaryForeground
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacySettingsCard() {
    val spacing = LocalSpacing.current
    Surface(
        color = DesignTokens.Surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = stringResource(id = R.string.social_privacy_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = R.string.social_privacy_settings_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

private fun avatarEmoji(id: String): String = when (id) {
    "target" -> "🎯"
    "rocket" -> "🚀"
    "star" -> "⭐"
    "flame" -> "🔥"
    "diamond" -> "💎"
    "trophy" -> "🏆"
    "puzzle" -> "🧩"
    "sun" -> "🌞"
    else -> "🙂"
}

private fun minutesPerDay(hours: Int): Int = (hours * 60) / 7
private fun hoursPerMonth(hours: Int): Int = hours * 4

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ProfileTabPreview() {
    val repo = FakeSocialRepository()
    StudyPlanTheme {
        val profile = repo.profile.collectAsState()
        ProfileTab(
            profile = profile.value,
            onAvatarSelected = {},
            onSaveGoal = {}
        )
    }
}
