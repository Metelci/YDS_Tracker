package com.mtlc.studyplan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.LoadingType
import com.mtlc.studyplan.error.ErrorEvent
import com.mtlc.studyplan.error.ErrorType

@Composable
fun StudyPlanLoadingState(
    isLoading: Boolean,
    loadingType: LoadingType = LoadingType.SHIMMER,
    message: String = "Loading...",
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (isLoading) {
            when (loadingType) {
                LoadingType.SHIMMER -> {
                    ShimmerOverlay(modifier = Modifier.fillMaxSize())
                }
                LoadingType.SPINNER -> {
                    SpinnerOverlay(
                        message = message,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LoadingType.PROGRESS_BAR -> {
                    ProgressBarOverlay(
                        progress = progress,
                        message = message,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LoadingType.SKELETON -> {
                    SkeletonLoader(modifier = Modifier.fillMaxSize())
                }
                LoadingType.PULL_TO_REFRESH -> {
                    // Handled by SwipeRefresh
                }
            }
        }
    }
}

@Composable
fun ShimmerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .background(
                Color.Gray.copy(alpha = alpha),
                shape = RoundedCornerShape(8.dp)
            )
    )
}

@Composable
fun SpinnerOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProgressBarOverlay(
    progress: Float,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SkeletonLoader(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            SkeletonTaskItem()
        }
    }
}

@Composable
fun SkeletonTaskItem() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerOverlay(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                ShimmerOverlay(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ShimmerOverlay(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                )
            }

            ShimmerOverlay(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }
    }
}

@Composable
fun ErrorDialog(
    errorEvent: ErrorEvent,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (errorEvent.error.type) {
                    ErrorType.NO_INTERNET -> Icons.Default.CloudOff
                    ErrorType.NETWORK -> Icons.Default.NetworkCheck
                    ErrorType.PERMISSION -> Icons.Default.Security
                    ErrorType.DATABASE -> Icons.Default.Storage
                    ErrorType.VALIDATION -> Icons.Default.Warning
                    else -> Icons.Default.Error
                },
                contentDescription = stringResource(R.string.cd_error),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = errorEvent.error.title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = errorEvent.error.message,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorEvent.error.userAction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (errorEvent.error.isRetryable && onRetry != null) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (errorEvent.error.isRetryable && onRetry != null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun ErrorSnackbar(
    errorEvent: ErrorEvent,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            if (errorEvent.error.isRetryable && onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("RETRY")
                }
            }
        },
        dismissAction = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    ) {
        Text(text = errorEvent.error.message)
    }
}
