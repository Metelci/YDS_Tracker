package com.mtlc.studyplan.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
    // Reset notifications
    OutlinedButton(onClick = { /* TODO: wire to ViewModel */ }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Settings, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Reset All Notifications")
    }
    // Reset progress danger
    OutlinedButton(
        onClick = { /* TODO */ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.Filled.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(8.dp))
        Text("Reset Progress (Danger)", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun Footer() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("StudyPlan YDS Tracker", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Version 1.0.0 • Made with ❤️", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

