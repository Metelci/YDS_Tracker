package com.mtlc.studyplan.social.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.SocialProfile
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

@Composable
fun ProfileTab(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit,
    onUploadAvatarClick: () -> Unit,
    onUndoAvatar: () -> Unit,
    onRedoAvatar: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    isAvatarBusy: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier.padding(bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ProfileCard(
                profile = profile,
                onAvatarSelected = onAvatarSelected,
                onUploadAvatarClick = onUploadAvatarClick,
                onUndoAvatar = onUndoAvatar,
                onRedoAvatar = onRedoAvatar,
                canUndo = canUndo,
                canRedo = canRedo,
                isAvatarBusy = isAvatarBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xs)
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit,
    onUploadAvatarClick: () -> Unit,
    onUndoAvatar: () -> Unit,
    onRedoAvatar: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    isAvatarBusy: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val prussianBlue = Color(0xFF003153)
    val isDarkTheme = isSystemInDarkTheme()

    Surface(
        modifier = modifier
            .border(BorderStroke(1.dp, prussianBlue), RoundedCornerShape(12.dp)),
        color = DesignTokens.PrimaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = stringResource(id = R.string.social_profile_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 32.dp)
                ) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { /* Copy username functionality */ },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.social_copy_username_cd),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(id = R.string.social_username_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xxs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.social_profile_photo_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    IconButton(
                        onClick = onUndoAvatar,
                        enabled = canUndo && !isAvatarBusy
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = stringResource(id = R.string.social_avatar_undo_cd)
                        )
                    }
                    IconButton(
                        onClick = onRedoAvatar,
                        enabled = canRedo && !isAvatarBusy
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = stringResource(id = R.string.social_avatar_redo_cd)
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.social_avatar_supported_formats_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            FilledTonalButton(
                onClick = onUploadAvatarClick,
                enabled = !isAvatarBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DesignTokens.Primary,
                    contentColor = DesignTokens.PrimaryForeground
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = stringResource(id = R.string.social_avatar_upload_icon_cd),
                    modifier = Modifier.size(16.dp),
                    tint = DesignTokens.PrimaryForeground
                )
                Text(
                    text = if (isAvatarBusy) {
                        stringResource(id = R.string.social_avatar_action_in_progress)
                    } else {
                        stringResource(id = R.string.social_upload_avatar_button)
                    },
                    modifier = Modifier.padding(start = spacing.xxs),
                    style = MaterialTheme.typography.labelMedium,
                    color = DesignTokens.PrimaryForeground
                )
            }

                    if (profile.availableAvatars.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    profile.availableAvatars.forEach { option ->
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(enabled = !isAvatarBusy) { onAvatarSelected(option.id) },
                            shape = CircleShape,
                            color = if (option.id == profile.selectedAvatarId) {
                                DesignTokens.PrimaryContainer
                            } else {
                                DesignTokens.Surface
                            },
                            tonalElevation = if (option.id == profile.selectedAvatarId) 2.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = avatarEmoji(option.id),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (option.id == profile.selectedAvatarId) {
                                        DesignTokens.PrimaryContainerForeground
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }

            val infoCardBackground = if (isDarkTheme) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                DesignTokens.Surface
            }

            Surface(
                color = infoCardBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, prussianBlue)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    AvatarDisplay(
                        profile = profile,
                        size = 36.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(id = R.string.social_study_level, profile.studyLevel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarDisplay(
    profile: SocialProfile,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    if (profile.selectedAvatarId == "custom" && !profile.customAvatarUri.isNullOrEmpty()) {
        Image(
            painter = rememberAsyncImagePainter(model = Uri.parse(profile.customAvatarUri)),
            contentDescription = stringResource(id = R.string.social_avatar_custom_cd),
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier.size(size),
            shape = CircleShape,
            color = DesignTokens.PrimaryContainer,
            tonalElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = avatarEmoji(profile.selectedAvatarId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.PrimaryContainerForeground
                )
            }
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
    "sun" -> "☀️"
    "custom" -> "🖼️"
    else -> "🙂"
}

@Preview(showBackground = true)
@Composable
private fun ProfileTabPreview() {
    StudyPlanTheme {
        ProfileTab(
            profile = SocialProfile(
                username = "sample_user",
                selectedAvatarId = "target",
                availableAvatars = emptyList(),
                studyLevel = "Intermediate",
                weeklyGoalHours = 10,
                goalRange = 5..25,
                privacyEnabled = true
            ),
            onAvatarSelected = {},
            onUploadAvatarClick = {},
            onUndoAvatar = {},
            onRedoAvatar = {},
            canUndo = false,
            canRedo = false,
            isAvatarBusy = false
        )
    }
}

