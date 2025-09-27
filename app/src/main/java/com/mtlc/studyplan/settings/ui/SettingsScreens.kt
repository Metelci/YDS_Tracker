package com.mtlc.studyplan.settings.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.mtlc.studyplan.settings.data.NavigationSettings
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateToCategory: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val s = LocalSpacing.current
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = DesignTokens.Foreground)
            Text(
                "Customize your study experience",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category grid (2 rows x 3)
            var selected by remember { mutableStateOf("Navigation") }
            var showResetDialog by remember { mutableStateOf(false) }
            FlowRow(maxItemsInEachRow = 3, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryPill(label = "Navigation", icon = Icons.AutoMirrored.Outlined.Send, selected = selected=="Navigation") { selected = it; onNavigateToCategory(it) }
                CategoryPill(label = "Notifications", icon = Icons.Outlined.Notifications, selected = selected=="Notifications") { selected = it; onNavigateToCategory(it) }
                CategoryPill(label = "Gamification", icon = Icons.Outlined.Celebration, selected = selected=="Gamification") { selected = it; onNavigateToCategory(it) }
                CategoryPill(label = "Social", icon = Icons.Outlined.Image, selected = selected=="Social") { selected = it; onNavigateToCategory(it) }
                CategoryPill(label = "Privacy", icon = Icons.Outlined.Lock, selected = selected=="Privacy") { selected = it; onNavigateToCategory(it) }
                CategoryPill(label = "Tasks", icon = Icons.Outlined.TaskAlt, selected = selected=="Tasks") { selected = it; onNavigateToCategory(it) }
            }

            // Navigation section card
            // read current nav settings
            val ctx = LocalContext.current
            val prefs = remember { SettingsPreferencesManager(ctx) }
            val navSettings by prefs.navigationSettings.collectAsState(initial = NavigationSettings())

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DesignTokens.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, tint = DesignTokens.Primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Navigation", fontWeight = FontWeight.SemiBold, color = DesignTokens.Foreground)
                    }

                    SettingToggleRow(
                        icon = Icons.Outlined.Settings,
                        title = "Bottom Navigation",
                        description = "Show navigation at bottom of screen",
                        checked = navSettings.bottomNavigation,
                        onCheckedChange = { checked ->
                            prefs.updateNavigationSettings(navSettings.copy(bottomNavigation = checked))
                        }
                    )
                    SettingToggleRow(
                        icon = Icons.Outlined.Image,
                        title = "Haptic Feedback",
                        description = "Vibrate on button taps and interactions",
                        checked = navSettings.hapticFeedback,
                        onCheckedChange = { checked ->
                            prefs.updateNavigationSettings(navSettings.copy(hapticFeedback = checked))
                        }
                    )
                }
            }

            // Removed neutral action: Reset All Notifications per requirement

            // Danger action
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignTokens.Error),
                border = BorderStroke(1.dp, DesignTokens.Error)
            ) {
                Icon(Icons.Outlined.Celebration, contentDescription = null, tint = DesignTokens.Error)
                Spacer(Modifier.width(8.dp))
                Text("Reset Progress (Danger)")
            }

            // Footer
            Spacer(Modifier.width(4.dp))
            val context = LocalContext.current
            val versionName = remember(context) {
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    packageInfo.versionName ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
            }

            Text(
                text = "StudyPlan YDS Tracker\nVersion $versionName",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Reset Progress Confirmation Dialog
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Reset All Progress") },
                    text = {
                        Text("This will permanently delete all your study progress, achievements, and statistics. You will return to the welcome screen to start fresh. This action cannot be undone.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showResetDialog = false
                                // Reset functionality would go here
                            }
                        ) {
                            Text("Reset Everything", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: (String) -> Unit,
) {
    val bg = if (selected) DesignTokens.PrimaryContainer else DesignTokens.Surface
    val fg = if (selected) DesignTokens.PrimaryContainerForeground else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, if (selected) DesignTokens.Primary.copy(alpha = 0.25f) else DesignTokens.Border),
        modifier = Modifier
            .padding(vertical = 6.dp)
            .width(110.dp)
            .clickable { onClick(label) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = fg, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        color = DesignTokens.SurfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DesignTokens.Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DesignTokens.Primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, color = DesignTokens.Foreground)
                    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
