package com.mtlc.studyplan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mtlc.studyplan.integration.EnhancedAppIntegrationManager
import com.mtlc.studyplan.repository.AchievementRepository
import com.mtlc.studyplan.database.entities.AchievementEntity
import com.mtlc.studyplan.shared.AchievementCategory
import com.mtlc.studyplan.eventbus.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Achievement ViewModel that integrates with the enhanced app integration manager
 */
@HiltViewModel
class IntegratedAchievementViewModel @Inject constructor(
    private val integrationManager: EnhancedAppIntegrationManager,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    // Achievement data flows
    val allAchievements = achievementRepository.allAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unlockedAchievements = achievementRepository.unlockedAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableAchievements = achievementRepository.availableAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newlyUnlockedAchievements = achievementRepository.newlyUnlockedAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nearlyUnlockedAchievements = achievementRepository.nearlyUnlockedAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressingAchievements = achievementRepository.progressingAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Achievement statistics
    val achievementStats = integrationManager.achievementStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            AchievementRepository.AchievementStats(0, 0, 0f, 0, 0))

    val categoryStats = achievementRepository.categoryStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val rarityStats = achievementRepository.rarityStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val difficultyStats = achievementRepository.difficultyStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Achievement events
    val achievementEvents = integrationManager.achievementEvents
        .filterIsInstance<AchievementEvent>()

    // UI state
    private val _uiState = MutableStateFlow(AchievementUiState())
    val uiState = _uiState.asStateFlow()

    // Filter state
    private val _filterState = MutableStateFlow(AchievementFilterState())
    val filterState = _filterState.asStateFlow()

    // Filtered achievements
    val filteredAchievements = combine(
        allAchievements,
        _filterState
    ) { achievements, filter ->
        filterAchievements(achievements, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Achievement breakdown for display
    data class AchievementBreakdown(
        val totalCount: Int,
        val unlockedCount: Int,
        val completionPercentage: Float,
        val totalPoints: Int,
        val newUnlockedCount: Int,
        val nearCompletionCount: Int
    )

    val achievementBreakdown = combine(
        achievementStats,
        nearlyUnlockedAchievements,
        newlyUnlockedAchievements
    ) { stats, nearCompletion, newUnlocked ->
        AchievementBreakdown(
            totalCount = stats.totalAchievements,
            unlockedCount = stats.unlockedCount,
            completionPercentage = stats.completionRate,
            totalPoints = stats.totalPointsEarned,
            newUnlockedCount = stats.newUnlockedCount,
            nearCompletionCount = nearCompletion.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        AchievementBreakdown(0, 0, 0f, 0, 0, 0))

    init {
        observeAchievementEvents()
    }

    // Business logic methods

    /**
     * Mark achievement as viewed
     */
    fun markAchievementAsViewed(achievementId: String) {
        viewModelScope.launch {
            try {
                achievementRepository.markAsViewed(achievementId)
            } catch (e: Exception) {
                showError("Failed to mark achievement as viewed: ${e.message}")
            }
        }
    }

    /**
     * Mark all achievements as viewed
     */
    fun markAllAchievementsAsViewed() {
        viewModelScope.launch {
            try {
                achievementRepository.markAllAsViewed()
                showSuccess("All achievements marked as viewed")
            } catch (e: Exception) {
                showError("Failed to mark achievements as viewed: ${e.message}")
            }
        }
    }

    /**
     * Get achievements by category
     */
    fun getAchievementsByCategory(category: AchievementCategory): Flow<List<AchievementEntity>> {
        return achievementRepository.getAchievementsByCategory(category)
    }

    /**
     * Get achievements by difficulty
     */
    fun getAchievementsByDifficulty(difficulty: String): Flow<List<AchievementEntity>> {
        return achievementRepository.getAchievementsByDifficulty(difficulty)
    }

    /**
     * Get achievements by rarity
     */
    fun getAchievementsByRarity(rarity: String): Flow<List<AchievementEntity>> {
        return achievementRepository.getAchievementsByRarity(rarity)
    }

    /**
     * Update filter
     */
    fun updateFilter(filter: AchievementFilterState) {
        _filterState.value = filter
    }

    /**
     * Clear filters
     */
    fun clearFilters() {
        _filterState.value = AchievementFilterState()
    }

    /**
     * Refresh achievements
     */
    fun refreshAchievements() {
        viewModelScope.launch {
            integrationManager.eventBus.publish(
                UIEvent.RefreshRequested(
                    component = "achievements",
                    reason = "user_request"
                )
            )
        }
    }

    /**
     * Share achievement
     */
    fun shareAchievement(achievementId: String) {
        viewModelScope.launch {
            try {
                val achievement = achievementRepository.getAchievementById(achievementId)
                if (achievement != null && achievement.isUnlocked) {
                    // Create social activity for sharing
                    integrationManager.eventBus.publish(
                        SocialEvent.ActivityShared(
                            activityId = "achievement_$achievementId",
                            shareCount = 1
                        )
                    )

                    showSuccess("Achievement shared!")

                    // Track analytics
                    integrationManager.eventBus.publish(
                        AnalyticsEvent.UserActionTracked(
                            action = "achievement_shared",
                            screen = "achievements",
                            properties = mapOf(
                                "achievement_id" to achievementId,
                                "achievement_title" to achievement.title
                            )
                        )
                    )
                } else {
                    showError("Cannot share locked achievement")
                }
            } catch (e: Exception) {
                showError("Failed to share achievement: ${e.message}")
            }
        }
    }

    /**
     * Get achievement progress summary
     */
    fun getProgressSummary(): Flow<ProgressSummary> {
        return combine(
            achievementStats,
            progressingAchievements
        ) { stats, progressing ->
            ProgressSummary(
                totalProgress = stats.completionRate,
                progressingCount = progressing.size,
                nextMilestone = calculateNextMilestone(stats.unlockedCount),
                pointsToNextReward = calculatePointsToNextReward(progressing)
            )
        }
    }

    // Helper methods

    private fun observeAchievementEvents() {
        achievementEvents
            .onEach { event ->
                when (event) {
                    is AchievementEvent.AchievementUnlocked -> {
                        showSuccess("🏆 Achievement Unlocked: ${event.achievementTitle}")

                        // Auto-scroll to new achievement or show special UI
                        _uiState.value = _uiState.value.copy(
                            newlyUnlockedId = event.achievementId
                        )
                    }
                    is AchievementEvent.CategoryCompleted -> {
                        showSuccess("🎉 Category '${event.category}' completed!")
                    }
                    is AchievementEvent.RarityMilestone -> {
                        showSuccess("✨ Unlocked ${event.unlockedCount} ${event.rarity} achievements!")
                    }
                    else -> {
                        // Handle other achievement events as needed
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun filterAchievements(
        achievements: List<AchievementEntity>,
        filter: AchievementFilterState
    ): List<AchievementEntity> {
        var filtered = achievements

        // Apply category filter
        if (filter.category != null) {
            filtered = filtered.filter { it.category == filter.category }
        }

        // Apply difficulty filter
        if (filter.difficulty != null) {
            filtered = filtered.filter { it.difficulty == filter.difficulty }
        }

        // Apply rarity filter
        if (filter.rarity != null) {
            filtered = filtered.filter { it.rarity == filter.rarity }
        }

        // Apply completion status filter
        when (filter.completionStatus) {
            CompletionStatus.UNLOCKED -> filtered = filtered.filter { it.isUnlocked }
            CompletionStatus.LOCKED -> filtered = filtered.filter { !it.isUnlocked }
            CompletionStatus.IN_PROGRESS -> filtered = filtered.filter {
                !it.isUnlocked && it.currentProgress > 0
            }
            CompletionStatus.NEAR_COMPLETION -> filtered = filtered.filter {
                !it.isUnlocked && it.currentProgress >= (it.threshold * 0.8)
            }
            CompletionStatus.ALL -> { /* No filter */ }
        }

        // Apply sorting
        return when (filter.sortBy) {
            AchievementSortBy.UNLOCK_DATE -> filtered.sortedByDescending { it.unlockedAt ?: 0L }
            AchievementSortBy.PROGRESS -> filtered.sortedByDescending {
                if (it.isUnlocked) 1f else it.currentProgress.toFloat() / it.threshold
            }
            AchievementSortBy.DIFFICULTY -> filtered.sortedBy { it.difficulty }
            AchievementSortBy.RARITY -> filtered.sortedBy { it.rarity }
            AchievementSortBy.POINTS -> filtered.sortedByDescending { it.pointsReward }
            AchievementSortBy.ALPHABETICAL -> filtered.sortedBy { it.title }
        }
    }

    private fun calculateNextMilestone(unlockedCount: Int): Int {
        val milestones = listOf(5, 10, 25, 50, 100, 200)
        return milestones.find { it > unlockedCount } ?: (unlockedCount + 50)
    }

    private fun calculatePointsToNextReward(progressing: List<AchievementRepository.ProgressingAchievement>): Int {
        return progressing.minByOrNull { it.remainingProgress }?.achievement?.pointsReward ?: 0
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
        integrationManager.eventBus.let { eventBus ->
            viewModelScope.launch {
                eventBus.publish(
                    UIEvent.LoadingStateChanged("achievements", isLoading)
                )
            }
        }
    }

    private fun showSuccess(message: String) {
        _uiState.value = _uiState.value.copy(
            successMessage = message,
            errorMessage = null
        )
        integrationManager.eventBus.let { eventBus ->
            viewModelScope.launch {
                eventBus.publish(
                    UIEvent.SnackbarRequested(message, duration = "SHORT")
                )
            }
        }
    }

    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            successMessage = null
        )
        integrationManager.eventBus.let { eventBus ->
            viewModelScope.launch {
                eventBus.publish(
                    UIEvent.ErrorOccurred("AchievementViewModel", message, isCritical = false)
                )
            }
        }
    }

    // Data classes and enums
    data class AchievementUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val newlyUnlockedId: String? = null
    )

    data class AchievementFilterState(
        val category: AchievementCategory? = null,
        val difficulty: String? = null,
        val rarity: String? = null,
        val completionStatus: CompletionStatus = CompletionStatus.ALL,
        val sortBy: AchievementSortBy = AchievementSortBy.PROGRESS
    )

    data class ProgressSummary(
        val totalProgress: Float,
        val progressingCount: Int,
        val nextMilestone: Int,
        val pointsToNextReward: Int
    )

    enum class CompletionStatus {
        ALL, UNLOCKED, LOCKED, IN_PROGRESS, NEAR_COMPLETION
    }

    enum class AchievementSortBy {
        UNLOCK_DATE, PROGRESS, DIFFICULTY, RARITY, POINTS, ALPHABETICAL
    }
}