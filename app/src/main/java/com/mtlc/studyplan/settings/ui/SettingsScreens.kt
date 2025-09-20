package com.mtlc.studyplan.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.settings.data.SettingsCategory
import com.mtlc.studyplan.settings.viewmodel.SettingsViewModel.SettingsUiState
import com.mtlc.studyplan.settings.viewmodel.SettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.SettingsViewModelFactory

/**
 * Main settings screen with category grid
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategory: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = remember { SettingsPreferencesManager(context) }

    val categories = remember {
        listOf(
            SettingsCategory(
                id = "navigation",
                title = "Navigation",
                description = "Customize your study experience",
                icon = Icons.Filled.Send,
                route = "settings/navigation"
            ),
            SettingsCategory(
                id = "notifications",
                title = "Notifications",
                description = "Manage notifications and alerts",
                icon = Icons.Filled.Notifications,
                route = "settings/notifications"
            ),
            SettingsCategory(
                id = "gamification",
                title = "Gamification",
                description = "Achievements and rewards",
                icon = Icons.Filled.EmojiEvents,
                route = "settings/gamification"
            ),
            SettingsCategory(
                id = "social",
                title = "Social",
                description = "Connect with other learners",
                icon = Icons.Filled.People,
                route = "settings/social"
            ),
            SettingsCategory(
                id = "privacy",
                title = "Privacy",
                description = "Data and privacy controls",
                icon = Icons.Filled.Shield,
                route = "settings/privacy"
            ),
            SettingsCategory(
                id = "tasks",
                title = "Tasks",
                description = "Study planning and scheduling",
                icon = Icons.Filled.CheckCircle,
                route = "settings/tasks"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Customize your study experience",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { category ->
                    SettingsCategoryCard(
                        category = category,
                        onClick = { onNavigateToCategory(category.route) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Reset Actions
            OutlinedButton(
                onClick = { /* Handle reset notifications */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    Icons.Filled.NotificationsOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Notifications")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Danger Zone
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Handle reset progress */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Progress (Danger)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Version Info
            Text(
                text = "StudyPlan YDS Tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Version 1.0.0 • Made with ❤️",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Settings category card component
 */
@Composable
fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = category.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Generic settings list screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsListScreen(
    title: String,
    uiState: SettingsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.items) { item ->
                    SettingItemComponent(item = item)
                }
            }
        }
    }
}

/**
 * Individual setting item component
 */
@Composable
fun SettingItemComponent(
    item: SettingItem,
    modifier: Modifier = Modifier
) {
    when (item) {
        is SettingItem.Header -> {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier.padding(vertical = 16.dp, horizontal = 4.dp)
            )
        }

        is SettingItem.Divider -> {
            HorizontalDivider(
                modifier = modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }

        is SettingItem.Toggle -> {
            ToggleSettingItem(
                title = item.title,
                description = item.description,
                isEnabled = item.isEnabled,
                onToggle = item.onToggle,
                modifier = modifier
            )
        }

        is SettingItem.Clickable -> {
            ClickableSettingItem(
                title = item.title,
                description = item.description,
                value = item.value,
                onClick = item.onClick,
                modifier = modifier
            )
        }

        is SettingItem.Selection -> {
            SelectionSettingItem(
                title = item.title,
                description = item.description,
                selectedValue = item.selectedValue,
                options = item.options,
                onSelectionChange = item.onSelectionChange,
                modifier = modifier
            )
        }
    }
}

/**
 * Toggle switch setting item
 */
@Composable
fun ToggleSettingItem(
    title: String,
    description: String?,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!isEnabled) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on setting type
            val icon = when {
                title.contains("Analytics", ignoreCase = true) -> Icons.Filled.Analytics
                title.contains("Progress", ignoreCase = true) -> Icons.Filled.TrendingUp
                title.contains("Smart", ignoreCase = true) -> Icons.Filled.Psychology
                title.contains("Difficulty", ignoreCase = true) -> Icons.Filled.Tune
                title.contains("Goal", ignoreCase = true) -> Icons.Filled.Flag
                title.contains("Weekend", ignoreCase = true) -> Icons.Filled.Weekend
                title.contains("Bottom", ignoreCase = true) -> Icons.Filled.ViewList
                title.contains("Haptic", ignoreCase = true) -> Icons.Filled.TouchApp
                title.contains("Push", ignoreCase = true) -> Icons.Filled.Notifications
                title.contains("Study Reminders", ignoreCase = true) -> Icons.Filled.Schedule
                title.contains("Achievement", ignoreCase = true) -> Icons.Filled.EmojiEvents
                title.contains("Email", ignoreCase = true) -> Icons.Filled.Email
                title.contains("Streak Tracking", ignoreCase = true) -> Icons.Filled.LocalFireDepartment
                title.contains("Points", ignoreCase = true) -> Icons.Filled.Stars
                title.contains("Celebration", ignoreCase = true) -> Icons.Filled.Celebration
                title.contains("Risk", ignoreCase = true) -> Icons.Filled.Warning
                title.contains("Social", ignoreCase = true) -> Icons.Filled.People
                title.contains("Leaderboard", ignoreCase = true) -> Icons.Filled.EmojiEvents
                title.contains("Study Groups", ignoreCase = true) -> Icons.Filled.Group
                else -> Icons.Filled.Settings
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

/**
 * Clickable setting item
 */
@Composable
fun ClickableSettingItem(
    title: String,
    description: String?,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Selection setting item with dropdown
 */
@Composable
fun SelectionSettingItem(
    title: String,
    description: String?,
    selectedValue: String,
    options: List<com.mtlc.studyplan.settings.models.SelectionOption>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.value == selectedValue }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon for visibility setting
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    selectedOption?.let { option ->
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectionChange(option.value)
                                    expanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = option.value == selectedValue,
                                onClick = {
                                    onSelectionChange(option.value)
                                    expanded = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (option.description != null) {
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Individual category screens
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { SettingsPreferencesManager(context) }
    val viewModel: SettingsViewModel.PrivacySettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(preferencesManager)
    )
    val uiState by viewModel.uiState.collectAsState()

    SettingsListScreen(
        title = "Privacy",
        uiState = uiState,
        onBack = onBack
    )
}

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { SettingsPreferencesManager(context) }
    val viewModel: SettingsViewModel.NotificationSettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(preferencesManager)
    )
    val uiState by viewModel.uiState.collectAsState()

    SettingsListScreen(
        title = "Notifications",
        uiState = uiState,
        onBack = onBack
    )
}

@Composable
fun TaskSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { SettingsPreferencesManager(context) }
    val viewModel: SettingsViewModel.TaskSettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(preferencesManager)
    )
    val uiState by viewModel.uiState.collectAsState()

    SettingsListScreen(
        title = "Tasks",
        uiState = uiState,
        onBack = onBack
    )
}

@Composable
fun NavigationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { SettingsPreferencesManager(context) }
    val viewModel: SettingsViewModel.NavigationSettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(preferencesManager)
    )
    val uiState by viewModel.uiState.collectAsState()

    SettingsListScreen(
        title = "Navigation",
        uiState = uiState,
        onBack = onBack
    )
}

@Composable
fun GamificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { SettingsPreferencesManager(context) }
    val viewModel: SettingsViewModel.GamificationSettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(preferencesManager)
    )
    val uiState by viewModel.uiState.collectAsState()

    SettingsListScreen(
        title = "Gamification",
        uiState = uiState,
        onBack = onBack
    )
}

@Composable
fun SocialSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { SettingsPreferencesManager(context) }
    val viewModel: SettingsViewModel.SocialSettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(preferencesManager)
    )
    val uiState by viewModel.uiState.collectAsState()

    SettingsListScreen(
        title = "Social",
        uiState = uiState,
        onBack = onBack
    )
}