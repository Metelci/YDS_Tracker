package com.mtlc.studyplan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementOverviewScreen(
    onNavigateBack: () -> Unit,
    onCategoryClick: (AchievementCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AchievementViewModel = viewModel()
    val achievementState by viewModel.achievementState.collectAsState()
    val pendingUnlocks by viewModel.pendingUnlocks.collectAsState()

    // Handle pending achievement unlocks
    AchievementNotificationOverlay(
        unlocks = pendingUnlocks,
        onDismissUnlock = viewModel::dismissUnlock
    )

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Achievement stats header
            item {
                AchievementStatsHeader(achievementState)
            }

            // Category cards
            items(AchievementCategory.values()) { category ->
                val categoryProgress = achievementState.categoryProgress[category]
                if (categoryProgress != null) {
                    AchievementCategoryCard(
                        categoryProgress = categoryProgress,
                        onCategoryClick = onCategoryClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementStatsHeader(achievementState: AchievementState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Achievement Progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${achievementState.unlockedAchievements.size}/${achievementState.totalAchievements} unlocked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${achievementState.totalPoints}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Progress bar
            val progressPercentage = if (achievementState.totalAchievements > 0) {
                achievementState.unlockedAchievements.size.toFloat() / achievementState.totalAchievements.toFloat()
            } else 0f

            LinearProgressIndicator(
                progress = { progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )

            // Category stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AchievementCategory.values().forEach { category ->
                    val categoryProgress = achievementState.categoryProgress[category]
                    if (categoryProgress != null) {
                        CategoryMiniStat(
                            category = category,
                            progress = categoryProgress,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryMiniStat(
    category: AchievementCategory,
    progress: CategoryProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = category.icon,
            fontSize = 24.sp
        )
        Text(
            text = "${progress.unlockedCount}/${progress.totalCount}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(category.color)
        )
        Text(
            text = category.title.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

/**
 * Category Detail Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: AchievementCategory,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AchievementViewModel = viewModel()
    val achievementState by viewModel.achievementState.collectAsState()
    val categoryProgress = achievementState.categoryProgress[category]

    if (categoryProgress == null) {
        // Loading or error state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = category.icon, fontSize = 24.sp)
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(category.color).copy(alpha = 0.1f)
            )
        )

        // Get achievement progress for this category
        val achievementProgressMap = remember {
            categoryProgress.achievements.associate { achievement ->
                achievement.id to (if (achievement.isUnlocked) 1f else {
                    // Calculate progress from AchievementTracker
                    // This would need to be connected to the actual progress calculation
                    0.5f // Placeholder
                })
            }
        }

        CategoryAchievementsList(
            categoryProgress = categoryProgress,
            achievementProgressMap = achievementProgressMap,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * ViewModel for achievement system
 */
class AchievementViewModel : androidx.lifecycle.ViewModel() {
    private val _achievementState = MutableStateFlow(
        AchievementState(
            categoryProgress = emptyMap(),
            unlockedAchievements = emptySet(),
            totalAchievements = 0,
            totalPoints = 0
        )
    )
    val achievementState: StateFlow<AchievementState> = _achievementState

    private val _pendingUnlocks = MutableStateFlow<List<AchievementUnlock>>(emptyList())
    val pendingUnlocks: StateFlow<List<AchievementUnlock>> = _pendingUnlocks

    init {
        // Initialize with sample data or connect to repository
        loadAchievementState()
    }

    private fun loadAchievementState() {
        // This would connect to ProgressRepository.getAchievementState()
        // For now, creating sample state
        val sampleCategories = AchievementCategory.values().associateWith { category ->
            val achievements = CategorizedAchievementDataSource.getAchievementsByCategory(category)
            CategoryProgress(
                category = category,
                achievements = achievements,
                unlockedCount = achievements.take(2).size, // Sample: first 2 unlocked
                totalCount = achievements.size,
                categoryPoints = achievements.take(2).sumOf { it.pointsReward },
                nextAchievement = achievements.getOrNull(2),
                completionPercentage = 0.4f
            )
        }

        _achievementState.value = AchievementState(
            categoryProgress = sampleCategories,
            unlockedAchievements = sampleCategories.values
                .flatMap { it.achievements.take(2) }
                .map { it.id }
                .toSet(),
            totalAchievements = CategorizedAchievementDataSource.allCategorizedAchievements.size,
            totalPoints = sampleCategories.values.sumOf { it.categoryPoints }
        )
    }

    fun dismissUnlock(unlock: AchievementUnlock) {
        _pendingUnlocks.value = _pendingUnlocks.value - unlock
    }

    fun addPendingUnlock(unlock: AchievementUnlock) {
        _pendingUnlocks.value = _pendingUnlocks.value + unlock
    }
}