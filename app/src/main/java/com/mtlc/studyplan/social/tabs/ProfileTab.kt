package com.mtlc.studyplan.social.tabs


import androidx.compose.ui.graphics.Brush


import androidx.compose.foundation.Image


import androidx.compose.foundation.border


import androidx.compose.foundation.BorderStroke




import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.Arrangement


import androidx.compose.foundation.layout.Box


import androidx.compose.foundation.layout.Column


import androidx.compose.foundation.layout.Row


import androidx.compose.foundation.layout.Spacer


import androidx.compose.foundation.layout.aspectRatio


import androidx.compose.foundation.layout.fillMaxWidth


import androidx.compose.foundation.layout.heightIn


import androidx.compose.foundation.layout.padding


import androidx.compose.foundation.layout.size


import androidx.compose.foundation.layout.width


import androidx.compose.foundation.shape.CircleShape


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.outlined.ContentCopy


import androidx.compose.material.icons.outlined.Image


import androidx.compose.material.icons.automirrored.outlined.Redo


import androidx.compose.material.icons.automirrored.outlined.Undo


import androidx.compose.material3.ButtonDefaults


import androidx.compose.material3.FilledTonalButton


import androidx.compose.material3.HorizontalDivider


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

import androidx.compose.ui.graphics.luminance



import androidx.compose.ui.layout.ContentScale


import androidx.compose.ui.res.stringResource


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.style.TextAlign


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
    val isDarkTheme = false

    Surface(
        modifier = modifier
            .border(BorderStroke(1.dp, prussianBlue), RoundedCornerShape(16.dp)),
        color = DesignTokens.PrimaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Header with title
            Text(
                text = stringResource(id = R.string.social_profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = prussianBlue
            )

            // Main profile section with avatar and info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.Top
            ) {
                // Large avatar display
                AvatarDisplay(
                    profile = profile,
                    size = 80.dp
                )

                // User info column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    // Username with copy button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = prussianBlue,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { /* Copy username functionality */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.social_copy_username_cd),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Study level
                    Text(
                        text = stringResource(id = R.string.social_study_level, profile.studyLevel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    // Bio/description placeholder
                    Text(
                        text = stringResource(id = R.string.social_username_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Divider
            HorizontalDivider(
                color = prussianBlue.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Avatar selection section
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                // Section header with undo/redo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.social_profile_photo_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = prussianBlue
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onUndoAvatar,
                            enabled = canUndo && !isAvatarBusy,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = stringResource(id = R.string.social_avatar_undo_cd),
                                tint = if (canUndo && !isAvatarBusy) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onRedoAvatar,
                            enabled = canRedo && !isAvatarBusy,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Redo,
                                contentDescription = stringResource(id = R.string.social_avatar_redo_cd),
                                tint = if (canRedo && !isAvatarBusy) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Avatar grid with responsive wrapping
                if (profile.availableAvatars.isNotEmpty()) {
                    // Create rows with maximum 4 items per row
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        profile.availableAvatars.chunked(4).forEach { rowAvatars ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                rowAvatars.forEach { option ->
                                    val isSelected = option.id == profile.selectedAvatarId
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable(enabled = !isAvatarBusy) {
                                                onAvatarSelected(option.id)
                                            },
                                        shape = CircleShape,
                                        color = if (isSelected) {
                                            DesignTokens.PrimaryContainer
                                        } else {
                                            DesignTokens.Surface
                                        },
                                        border = if (isSelected) {
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                        },
                                        tonalElevation = if (isSelected) 4.dp else 1.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = avatarEmoji(option.id),
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = if (isSelected) {
                                                    DesignTokens.PrimaryContainerForeground
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                                // Fill empty spaces to maintain grid alignment
                                repeat(4 - rowAvatars.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Upload button
                FilledTonalButton(
                    onClick = onUploadAvatarClick,
                    enabled = !isAvatarBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DesignTokens.Primary,
                        contentColor = DesignTokens.PrimaryForeground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = stringResource(id = R.string.social_avatar_upload_icon_cd),
                        modifier = Modifier.size(20.dp),
                        tint = DesignTokens.PrimaryForeground
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text(
                        text = if (isAvatarBusy) {
                            stringResource(id = R.string.social_avatar_action_in_progress)
                        } else {
                            stringResource(id = R.string.social_upload_avatar_button)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = DesignTokens.PrimaryForeground
                    )
                }

                // Format hint
                Text(
                    text = stringResource(id = R.string.social_avatar_supported_formats_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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










