package com.mtlc.studyplan.settings.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import com.mtlc.studyplan.ui.components.StudyPlanTopBarStyle
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Settings", subtitle = "Customize your study experience", onBack = onBack) {
        NavigationCategoryCard()
        Spacer(Modifier.height(12.dp))
        ActionButtons()
        Footer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Privacy", subtitle = "Data and privacy controls", onBack = onBack) {
        // Placeholder card
        CategoryCardHeader("Privacy", Icons.Filled.Shield)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Notifications", subtitle = "Manage notifications and alerts", onBack = onBack) {
        CategoryCardHeader("Notifications", Icons.Filled.Notifications)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Gamification", subtitle = "Achievements and rewards", onBack = onBack) {
        CategoryCardHeader("Gamification", Icons.Filled.Celebration)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Social", subtitle = "Connect with other learners", onBack = onBack) {
        CategoryCardHeader("Social", Icons.Filled.People)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Tasks", subtitle = "Study planning and scheduling", onBack = onBack) {
        CategoryCardHeader("Tasks", Icons.AutoMirrored.Filled.Send)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategoryScaffold(title: String, subtitle: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            StudyPlanTopBar(
                title = title,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
                style = StudyPlanTopBarStyle.Settings
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun NavigationCategoryCard() {
    val bottomNav = remember { mutableStateOf(true) }
    val haptics = remember { mutableStateOf(true) }

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryCardHeader("Navigation", Icons.AutoMirrored.Filled.Send)
            SettingRowToggle(title = "Bottom Navigation", description = "Show navigation at bottom of screen", checked = bottomNav.value) { bottomNav.value = it }
            SettingRowToggle(title = "Haptic Feedback", description = "Vibrate on button taps and interactions", checked = haptics.value) { haptics.value = it }
        }
    }
}

@Composable
private fun CategoryCardHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingRowToggle(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ActionButtons() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    // Removed: Reset All Notifications button per requirement

    // Reset progress danger
    OutlinedButton(
        onClick = { showResetDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.Filled.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(8.dp))
        Text("Reset Progress (Danger)", color = MaterialTheme.colorScheme.error)
    }

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
                        coroutineScope.launch {
                            try {
                                // Reset all progress data
                                val progressRepo = StudyProgressRepository(context)
                                val onboardingRepo = OnboardingRepository(context.dataStore)

                                progressRepo.resetProgress()
                                onboardingRepo.resetOnboarding()

                                // Restart the app by finishing the current activity and navigating to onboarding
                                if (context is Activity) {
                                    context.finishAffinity()
                                    // The app will restart and show onboarding due to resetOnboarding()
                                }
                            } catch (_: Exception) {
                                // Handle error silently - user will see the reset didn't work
                            }
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

@Composable
private fun Footer() {
    val context = LocalContext.current
    val versionName = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("StudyPlan YDS Tracker", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Version $versionName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

