package com.mtlc.studyplan.social.tabs

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.SocialProfile
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
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
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
            modifier = Modifier.fillMaxWidth()
        )
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
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val headlineColor = MaterialTheme.colorScheme.onPrimaryContainer
    val subtitleColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = stringResource(id = R.string.social_profile_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.Top
            ) {
                AvatarDisplay(
                    profile = profile,
                    size = 72.dp
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { /* Copy username */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.social_copy_username_cd),
                                tint = subtitleColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(id = R.string.social_study_level, profile.studyLevel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(id = R.string.social_username_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = borderColor,
                thickness = 1.dp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.social_profile_photo_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = headlineColor
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
                                tint = if (canUndo && !isAvatarBusy) MaterialTheme.colorScheme.primary else subtitleColor
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
                                tint = if (canRedo && !isAvatarBusy) MaterialTheme.colorScheme.primary else subtitleColor
                            )
                        }
                    }
                }

                AvatarSelectionGrid(
                    profile = profile,
                    onAvatarSelected = onAvatarSelected,
                    isBusy = isAvatarBusy
                )

                FilledTonalButton(
                    onClick = onUploadAvatarClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    enabled = !isAvatarBusy
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text(text = stringResource(id = R.string.social_upload_avatar))
                }
            }
        }
    }
}

@Composable
private fun AvatarSelectionGrid(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val availableAvatars = listOf("target", "rocket", "star", "flame", "diamond", "trophy", "puzzle", "sun")

    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        availableAvatars.forEach { avatarId ->
            val isSelected = profile.selectedAvatarId == avatarId
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .aspectRatio(1f)
                    .clickable(enabled = !isBusy) { onAvatarSelected(avatarId) },
                shape = CircleShape,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null,
                tonalElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = avatarEmoji(avatarId),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarDisplay(
    profile: SocialProfile,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
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
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = avatarEmoji(profile.selectedAvatarId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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














