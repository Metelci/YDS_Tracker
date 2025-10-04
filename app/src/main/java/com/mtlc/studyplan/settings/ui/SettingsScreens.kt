package com.mtlc.studyplan.settings.ui
import androidx.compose.ui.graphics.Brush
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
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
import com.mtlc.studyplan.ui.theme.FeatureKey
import com.mtlc.studyplan.ui.theme.featurePastelContainer
import com.mtlc.studyplan.ui.theme.inferredFeaturePastelContainer
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.PackageInfoCompat
import com.mtlc.studyplan.settings.data.NavigationSettings
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.data.OnboardingRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Intent

private val PrussianBlue = Color(0xFF003153)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateToCategory: (String) -> Unit = {},
    onBack: () -> Unit = {},
    studyProgressRepository: StudyProgressRepository? = null,
    onboardingRepository: OnboardingRepository? = null
) {
    val s = LocalSpacing.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = inferredFeaturePastelContainer("com.mtlc.studyplan.settings", "settings_categories")),
                border = BorderStroke(1.dp, PrussianBlue)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    maxItemsInEachRow = 3,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategoryPill(label = "Navigation", icon = Icons.AutoMirrored.Outlined.Send, selected = selected=="Navigation") { selected = it; onNavigateToCategory(it) }
                    CategoryPill(label = "Notifications", icon = Icons.Outlined.Notifications, selected = selected=="Notifications") { selected = it; onNavigateToCategory(it) }
                    CategoryPill(label = "Gamification", icon = Icons.Outlined.Celebration, selected = selected=="Gamification") { selected = it; onNavigateToCategory(it) }
                    CategoryPill(label = "Social", icon = Icons.Outlined.Image, selected = selected=="Social") { selected = it; onNavigateToCategory(it) }
                    CategoryPill(label = "Privacy", icon = Icons.Outlined.Lock, selected = selected=="Privacy") { selected = it; onNavigateToCategory(it) }
                    CategoryPill(label = "Tasks", icon = Icons.Outlined.TaskAlt, selected = selected=="Tasks") { selected = it; onNavigateToCategory(it) }
                }
            }

            // Navigation section card
            // read current nav settings
            val ctx = LocalContext.current
            val prefs = remember { SettingsPreferencesManager(ctx) }
            val navSettings by prefs.navigationSettings.collectAsState(initial = NavigationSettings())

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = inferredFeaturePastelContainer("com.mtlc.studyplan.settings", "settings_navigation")),
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
            val packageInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            }.getOrNull()
            val versionName = packageInfo?.versionName?.takeIf { it.isNotBlank() } ?: "Unknown"
            val versionCode = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: -1L

            Text(
                text = buildString {
                    append("StudyPlan YDS Tracker\nVersion $versionName")
                    if (versionCode >= 0) {
                        append(" (Build $versionCode)")
                    }
                },
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
                                scope.launch {
                                    // Clear all study progress
                                    studyProgressRepository?.resetProgress()

                                    // Clear onboarding state
                                    onboardingRepository?.resetOnboarding()

                                    // Clear all settings
                                    context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .clear()
                                        .apply()

                                    // Clear DataStore preferences
                                    context.getSharedPreferences("study_progress", android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .clear()
                                        .apply()

                                    // Restart the app
                                    val packageManager = context.packageManager
                                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)

                                    // Kill the current process to ensure clean restart
                                    android.os.Process.killProcess(android.os.Process.myPid())
                                }
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
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = if (selected) colorScheme.primaryContainer else colorScheme.surface
    val contentColor = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface
    val borderColor = if (selected) colorScheme.primary else colorScheme.outline
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (selected) 0.dp else 2.dp,
        border = BorderStroke(1.dp, borderColor),
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
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = contentColor, fontSize = 14.sp)
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

