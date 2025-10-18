package com.mtlc.studyplan.settings.ui
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.settings.data.NavigationSettings
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import com.mtlc.studyplan.ui.components.StudyPlanTopBarStyle
import com.mtlc.studyplan.ui.theme.FeatureKey
import com.mtlc.studyplan.ui.theme.featurePastelContainer
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Navigation", subtitle = "Customize navigation experience", onBack = onBack) {
        NavigationCategoryCard()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Privacy", subtitle = "Data and privacy controls", onBack = onBack) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.SETTINGS, "Privacy"))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryCardHeader("Privacy", Icons.Filled.Shield)
                SettingRowToggle(title = "Secure Storage", description = "Encrypt sensitive data", checked = true) { }
                SettingRowToggle(title = "Data Sharing", description = "Share anonymized data for research", checked = false) { }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Notifications", subtitle = "Manage notifications and alerts", onBack = onBack) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.SETTINGS, "Notifications"))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryCardHeader("Notifications", Icons.Filled.Notifications)
                SettingRowToggle(title = "Push Notifications", description = "Allow notifications from this app", checked = true) { }
                SettingRowToggle(title = "Study Reminders", description = "Daily reminders to study", checked = true) { }
                SettingRowToggle(title = "Achievement Alerts", description = "Get notified when you earn achievements", checked = false) { }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Gamification", subtitle = "Achievements and rewards", onBack = onBack) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.SETTINGS, "Tasks"))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryCardHeader("Gamification", Icons.Filled.Celebration)
                SettingRowToggle(title = "Point System", description = "Earn points for completing tasks", checked = true) { }
                SettingRowToggle(title = "Achievements", description = "Unlock achievements and badges", checked = true) { }
                SettingRowToggle(title = "Streak Counter", description = "Track daily study streaks", checked = true) { }
                SettingRowToggle(title = "Level System", description = "Gain XP and level up", checked = false) { }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSettingsScreen(onBack: () -> Unit) {
    SettingsCategoryScaffold(title = "Tasks", subtitle = "Study planning and scheduling", onBack = onBack) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryCardHeader("Tasks", Icons.AutoMirrored.Filled.Send)
                SettingRowToggle(title = "Daily Reminders", description = "Remind me of my daily study goals", checked = true) { }
                SettingRowToggle(title = "Progress Tracking", description = "Track time spent on tasks", checked = true) { }
            }
        }
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
    val context = LocalContext.current
    val settingsManager = remember { SettingsPreferencesManager(context) }
    val navSettings by settingsManager.navigationSettings.collectAsState(initial = NavigationSettings())

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.SETTINGS, "Navigation"))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryCardHeader("Navigation", Icons.AutoMirrored.Filled.Send)
            SettingRowToggle(
                title = "Haptic Feedback",
                description = "Vibrate on button taps and interactions",
                checked = navSettings.hapticFeedback
            ) {
                settingsManager.updateNavigationSettings(navSettings.copy(hapticFeedback = it))
            }
            // Dark Mode toggle removed
        }
    }
}

@Composable
private fun CategoryCardHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
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



