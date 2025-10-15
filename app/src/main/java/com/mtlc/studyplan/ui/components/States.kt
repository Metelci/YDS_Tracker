package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.R

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String = "Nothing here yet",
    message: String = "Add or start something to see content.",
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(LocalSpacing.current.lg)
            .semantics { contentDescription = "empty_state" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = title)
            Spacer(Modifier.height(LocalSpacing.current.md))
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(LocalSpacing.current.xs))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) {
            Spacer(Modifier.height(LocalSpacing.current.lg))
            action()
        }
    }
}

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    message: String = "Please try again.",
    onRetry: () -> Unit = {},
    onDiagnostics: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(LocalSpacing.current.lg)
            .semantics { contentDescription = "error_state" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.cd_error)
        )
        Spacer(Modifier.height(LocalSpacing.current.md))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(LocalSpacing.current.xs))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(LocalSpacing.current.lg))
        ElevatedButton(onClick = onRetry) { Text("Retry") }
        if (onDiagnostics != null) {
            Spacer(Modifier.height(LocalSpacing.current.xs))
            OutlinedButton(onClick = onDiagnostics) { Text("Diagnostics") }
        }
    }
}
