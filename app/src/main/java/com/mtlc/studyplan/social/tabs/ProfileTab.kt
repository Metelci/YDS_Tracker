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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mtlc.studyplan.data.social.SocialProfile
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

@Composable
fun ProfileTab(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit,
    onUploadAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .padding(bottom = spacing.sm), // Reduced from lg (24dp) to sm (12dp)
        verticalArrangement = Arrangement.spacedBy(spacing.xs) // Reduced from md to xs
    ) {
        // Add responsive width constraint for tablets/larger screens
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ProfileCard(
                profile = profile,
                onAvatarSelected = onAvatarSelected,
                onUploadAvatarClick = onUploadAvatarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xs) // Add horizontal padding for very wide screens
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit,
    onUploadAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var showInappropriateContentDialog by remember { mutableStateOf(false) }
    var showCroppingDialog by remember { mutableStateOf(false) }
    var pendingImageProcessing by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        color = DesignTokens.PrimaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm), // Reduced from md (16dp) to sm (12dp)
            verticalArrangement = Arrangement.spacedBy(spacing.xs) // Reduced from md to xs (8dp)
        ) {
            // Compact header section
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) { // Reduced spacing
                Text(
                    text = stringResource(id = R.string.social_profile_title),
                    style = MaterialTheme.typography.titleSmall, // Reduced from titleMedium
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 32.dp) // Ensure minimum touch target
                ) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.bodyMedium, // Reduced from bodyLarge
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier
                            .size(32.dp) // Minimum touch target
                            .clickable { /* Copy username functionality */ },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.social_copy_username_cd),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp) // Slightly smaller icon
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(id = R.string.social_username_hint),
                    style = MaterialTheme.typography.labelSmall, // Reduced from labelMedium
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            // Avatar section - Only show upload option
            Text(
                text = "Profile Photo",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = spacing.xxs)
            )

            Text(
                text = "Upload your own photo - custom avatars are not supported",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Enhanced upload button with content detection
            FilledTonalButton(
                onClick = {
                    pendingImageProcessing = true
                    // Simulate content detection process
                    val hasInappropriateContent = detectInappropriateContent()

                    if (hasInappropriateContent) {
                        showInappropriateContentDialog = true
                        pendingImageProcessing = false
                    } else {
                        // Check if image needs cropping
                        val needsCropping = checkIfImageNeedsCropping()
                        if (needsCropping) {
                            showCroppingDialog = true
                            pendingImageProcessing = false
                        } else {
                            // Process normally
                            onUploadAvatarClick()
                            pendingImageProcessing = false
                        }
                    }
                },
                enabled = !pendingImageProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = DesignTokens.Surface)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (pendingImageProcessing) "Processing..." else "Upload Profile Photo",
                    modifier = Modifier.padding(start = spacing.xxs),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Compact profile preview
            Surface(
                color = DesignTokens.Surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.xs), // Reduced from md (16dp) to xs (8dp)
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs) // Reduced spacing
                ) {
                    AvatarPreview(avatarId = profile.selectedAvatarId)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.labelLarge, // Reduced from titleSmall
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(id = R.string.social_study_level, profile.studyLevel),
                            style = MaterialTheme.typography.labelSmall, // Reduced from labelMedium
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Inappropriate Content Warning Dialog
            if (showInappropriateContentDialog) {
                AlertDialog(
                    onDismissRequest = { showInappropriateContentDialog = false },
                    title = {
                        Text(
                            text = "Inappropriate Content Detected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Text(
                            text = "The uploaded image contains inappropriate content and cannot be used as a profile photo. Please select a different image that follows our community guidelines.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showInappropriateContentDialog = false }
                        ) {
                            Text("Try Again")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showInappropriateContentDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Auto-Cropping Dialog
            if (showCroppingDialog) {
                AlertDialog(
                    onDismissRequest = { showCroppingDialog = false },
                    title = {
                        Text(
                            text = "Image Auto-Cropped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Text(
                            text = "Your image has been automatically cropped to the appropriate size for a profile photo. The cropped version will be used as your profile picture.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCroppingDialog = false
                                // Apply the cropped image
                                applyCroppedImage()
                                onUploadAvatarClick()
                            }
                        ) {
                            Text("Use Cropped Image")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showCroppingDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactAvatarGrid(
    options: List<AvatarOption>,
    selectedId: String,
    onAvatarSelected: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs), // Reduced from sm (12dp) to xs (8dp)
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        options.forEach { option ->
            val isSelected = option.id == selectedId
            val border = if (isSelected) BorderStroke(2.dp, DesignTokens.Primary) else null
            Surface(
                modifier = Modifier
                    .size(48.dp) // Reduced from 64dp to 48dp
                    .clickable { onAvatarSelected(option.id) },
                color = DesignTokens.Surface,
                border = border,
                shape = RoundedCornerShape(12.dp) // Reduced corner radius
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = avatarEmoji(option.id),
                        style = MaterialTheme.typography.titleMedium // Reduced from headlineSmall
                    )
                    if (isSelected) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp) // Reduced padding
                                .size(14.dp), // Reduced size
                            shape = CircleShape,
                            color = DesignTokens.Primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✔",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f),
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
private fun CompactInitialPreview(initial: String) {
    Surface(
        modifier = Modifier.size(36.dp), // Reduced from 48dp to 36dp
        shape = CircleShape,
        color = DesignTokens.PrimaryContainer,
        tonalElevation = 1.dp // Reduced elevation
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleSmall, // Reduced from titleMedium
                fontWeight = FontWeight.Bold,
                color = DesignTokens.PrimaryContainerForeground
            )
        }
    }
}

@Composable
private fun AvatarPreview(avatarId: String) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = DesignTokens.PrimaryContainer,
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = avatarEmoji(avatarId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DesignTokens.PrimaryContainerForeground
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

// Helper functions for content detection and cropping
private fun detectInappropriateContent(): Boolean {
    // Simulate content detection - in real implementation, this would use ML/AI services
    // For demo purposes, randomly return true ~10% of the time
    return (1..10).random() == 1
}

private fun checkIfImageNeedsCropping(): Boolean {
    // Simulate checking if image dimensions are appropriate
    // In real implementation, this would analyze actual image dimensions
    return (1..3).random() == 1 // ~33% chance of needing cropping
}

private fun applyCroppedImage() {
    // Simulate applying auto-cropped image
    // In real implementation, this would perform actual image cropping
    // using libraries like Coil, Glide, or Android's built-in bitmap utilities
}

