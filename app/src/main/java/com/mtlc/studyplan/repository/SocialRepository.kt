package com.mtlc.studyplan.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.mtlc.studyplan.database.dao.SocialDao
import com.mtlc.studyplan.database.entities.SocialActivityEntity
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val socialDao: SocialDao
) {

    // Reactive data flows
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private val _newActivityCreated = MutableStateFlow<SocialActivityEntity?>(null)
    val newActivityCreated: StateFlow<SocialActivityEntity?> = _newActivityCreated.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Core activity flows
    val allActivities: Flow<List<SocialActivityEntity>> = socialDao.getAllActivities()
    val publicActivities: Flow<List<SocialActivityEntity>> = socialDao.getPublicActivities()
    val highlightActivities: Flow<List<SocialActivityEntity>> = socialDao.getHighlightActivities()
    val milestoneActivities: Flow<List<SocialActivityEntity>> = socialDao.getMilestoneActivities()
    val perfectDayActivities: Flow<List<SocialActivityEntity>> = socialDao.getPerfectDayActivities()
    val challengeActivities: Flow<List<SocialActivityEntity>> = socialDao.getChallengeActivities()

    // User-specific flows
    fun getUserActivities(userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getUserActivities(userId)

    fun getTodayActivities(userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getTodayActivities(userId)

    fun getWeeklyActivities(userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getWeeklyActivities(userId)

    fun getMonthlyActivities(userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getMonthlyActivities(userId)

    // Friends and social flows
    fun getFriendsActivities(friendIds: List<String>, limit: Int = 100): Flow<List<SocialActivityEntity>> =
        socialDao.getFriendsActivities(friendIds, limit)

    // Analytics flows
    fun getUserStats(userId: String): Flow<UserSocialStats> = combine(
        getUserActivities(userId),
        getTodayActivities(userId),
        getWeeklyActivities(userId)
    ) { allActivities, todayActivities, weeklyActivities ->
        calculateUserSocialStats(allActivities, todayActivities, weeklyActivities)
    }

    val globalStats: Flow<GlobalSocialStats> = combine(
        allActivities,
        publicActivities,
        highlightActivities
    ) { all, public, highlights ->
        calculateGlobalSocialStats(all, public, highlights)
    }

    // Activity type flows
    fun getActivitiesByType(type: String, userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getActivitiesByType(type, userId)

    fun getActivitiesByCategory(category: String, userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getActivitiesByCategory(category, userId)

    fun getActivitiesByDifficulty(difficulty: String, userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getActivitiesByDifficulty(difficulty, userId)

    fun getActivitiesByMood(mood: String, userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getActivitiesByMood(mood, userId)

    fun getActivitiesByLocation(location: String): Flow<List<SocialActivityEntity>> =
        socialDao.getActivitiesByLocation(location)

    // Filtered flows
    fun getHighScoringActivities(minPoints: Int, userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getHighScoringActivities(minPoints, userId)

    fun getStreakActivities(userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getStreakActivities(userId)

    fun getAchievementActivities(userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getAchievementActivities(userId)

    fun getTaskRelatedActivities(userId: String): Flow<List<SocialActivityEntity>> =
        socialDao.getTaskRelatedActivities(userId)

    fun getActivitiesForChallenge(challengeName: String): Flow<List<SocialActivityEntity>> =
        socialDao.getActivitiesForChallenge(challengeName)

    // Activity operations
    suspend fun getActivityById(activityId: String): SocialActivityEntity? =
        socialDao.getActivityById(activityId)

    suspend fun insertActivity(activity: SocialActivityEntity) {
        socialDao.insertActivity(activity)
        _newActivityCreated.value = activity
        triggerRefresh()
    }

    suspend fun insertActivities(activities: List<SocialActivityEntity>) {
        socialDao.insertActivities(activities)
        triggerRefresh()
    }

    suspend fun updateActivity(activity: SocialActivityEntity) {
        socialDao.updateActivity(activity)
        triggerRefresh()
    }

    suspend fun updateVisibility(activityId: String, isPublic: Boolean, visibility: String) {
        socialDao.updateVisibility(activityId, isPublic, visibility, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun updateHighlightStatus(activityId: String, isHighlight: Boolean) {
        socialDao.updateHighlightStatus(activityId, isHighlight, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun incrementShares(activityId: String) {
        socialDao.incrementShares(activityId, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun updateReactions(activityId: String, reactions: Map<String, Int>) {
        socialDao.updateReactions(activityId, reactions, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun updateComments(activityId: String, comments: List<String>) {
        socialDao.updateComments(activityId, comments, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun updateTags(activityId: String, tags: List<String>) {
        socialDao.updateTags(activityId, tags, System.currentTimeMillis())
        triggerRefresh()
    }

    suspend fun deleteActivity(activityId: String) {
        socialDao.deleteActivityById(activityId)
        triggerRefresh()
    }

    suspend fun deleteUserActivities(userId: String) {
        socialDao.deleteUserActivities(userId)
        triggerRefresh()
    }

    suspend fun deleteOldActivities(daysOld: Int = 365) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        socialDao.deleteOldActivities(cutoffTime)
        triggerRefresh()
    }

    suspend fun deleteAllActivities() {
        socialDao.deleteAllActivities()
        triggerRefresh()
    }

    // Analytics operations
    suspend fun getUserActivityCount(userId: String): Int = socialDao.getUserActivityCount(userId)

    suspend fun getUserActivityCountByType(type: String, userId: String): Int =
        socialDao.getUserActivityCountByType(type, userId)

    suspend fun getUserTotalPoints(userId: String): Int = socialDao.getUserTotalPoints(userId) ?: 0

    suspend fun getUserTotalStudyMinutes(userId: String): Int = socialDao.getUserTotalStudyMinutes(userId) ?: 0

    suspend fun getUserTotalShares(userId: String): Int = socialDao.getUserTotalShares(userId) ?: 0

    suspend fun getUserAveragePoints(userId: String): Float = socialDao.getUserAveragePoints(userId) ?: 0f

    suspend fun getUserActivityTypes(userId: String): List<String> = socialDao.getUserActivityTypes(userId)

    suspend fun getUserActiveCategories(userId: String): List<String> = socialDao.getUserActiveCategories(userId)

    suspend fun getUserMoods(userId: String): List<String> = socialDao.getUserMoods(userId)

    suspend fun getAllChallenges(): List<String> = socialDao.getAllChallenges()

    suspend fun getAllLocations(): List<String> = socialDao.getAllLocations()

    suspend fun getActivitiesInTimeRange(startTime: Long, endTime: Long): List<SocialActivityEntity> =
        socialDao.getActivitiesInTimeRange(startTime, endTime)

    // Convenience creation methods
    suspend fun createTaskCompletionActivity(
        userId: String,
        taskId: String,
        taskTitle: String,
        category: String,
        difficulty: String,
        pointsEarned: Int,
        studyMinutes: Int,
        isPublic: Boolean = true
    ) {
        socialDao.createTaskCompletionActivity(
            userId, taskId, taskTitle, category, difficulty, pointsEarned, studyMinutes, isPublic
        )
        triggerRefresh()
    }

    suspend fun createAchievementActivity(
        userId: String,
        achievementId: String,
        achievementTitle: String,
        achievementDescription: String,
        pointsEarned: Int,
        isPublic: Boolean = true
    ) {
        socialDao.createAchievementActivity(
            userId, achievementId, achievementTitle, achievementDescription, pointsEarned, isPublic
        )
        triggerRefresh()
    }

    suspend fun createStreakMilestoneActivity(
        userId: String,
        streakDay: Int,
        pointsEarned: Int,
        isPublic: Boolean = true
    ) {
        socialDao.createStreakMilestoneActivity(userId, streakDay, pointsEarned, isPublic)
        triggerRefresh()
    }

    suspend fun createPerfectDayActivity(
        userId: String,
        tasksCompleted: Int,
        studyMinutes: Int,
        pointsEarned: Int,
        isPublic: Boolean = true
    ) {
        socialDao.createPerfectDayActivity(userId, tasksCompleted, studyMinutes, pointsEarned, isPublic)
        triggerRefresh()
    }

    // Custom activity creation
    suspend fun createCustomActivity(
        userId: String,
        activityType: String,
        title: String,
        description: String,
        content: String? = null,
        pointsEarned: Int = 0,
        categoryInvolved: String? = null,
        difficulty: String? = null,
        studyMinutes: Int? = null,
        mood: String? = null,
        location: String? = null,
        tags: List<String> = emptyList(),
        challenge: String? = null,
        isPublic: Boolean = true,
        isHighlight: Boolean = false
    ) {
        val activity = SocialActivityEntity(
            userId = userId,
            activityType = activityType,
            title = title,
            description = description,
            content = content,
            pointsEarned = pointsEarned,
            categoryInvolved = categoryInvolved,
            difficulty = difficulty,
            studyMinutes = studyMinutes,
            mood = mood,
            location = location,
            tags = tags,
            challenge = challenge,
            isPublic = isPublic,
            isHighlight = isHighlight
        )
        insertActivity(activity)
    }

    // Reaction and engagement operations
    suspend fun addReaction(activityId: String, emoji: String) {
        val activity = getActivityById(activityId)
        activity?.let {
            val currentReactions = it.reactions.toMutableMap()
            currentReactions[emoji] = (currentReactions[emoji] ?: 0) + 1
            updateReactions(activityId, currentReactions)
        }
    }

    suspend fun removeReaction(activityId: String, emoji: String) {
        val activity = getActivityById(activityId)
        activity?.let {
            val currentReactions = it.reactions.toMutableMap()
            val currentCount = currentReactions[emoji] ?: 0
            if (currentCount > 1) {
                currentReactions[emoji] = currentCount - 1
            } else {
                currentReactions.remove(emoji)
            }
            updateReactions(activityId, currentReactions)
        }
    }

    suspend fun addComment(activityId: String, comment: String) {
        val activity = getActivityById(activityId)
        activity?.let {
            val currentComments = it.comments.toMutableList()
            currentComments.add(comment)
            updateComments(activityId, currentComments)
        }
    }

    suspend fun removeComment(activityId: String, commentIndex: Int) {
        val activity = getActivityById(activityId)
        activity?.let {
            val currentComments = it.comments.toMutableList()
            if (commentIndex in currentComments.indices) {
                currentComments.removeAt(commentIndex)
                updateComments(activityId, currentComments)
            }
        }
    }

    suspend fun addTag(activityId: String, tag: String) {
        val activity = getActivityById(activityId)
        activity?.let {
            val currentTags = it.tags.toMutableList()
            if (!currentTags.contains(tag)) {
                currentTags.add(tag)
                updateTags(activityId, currentTags)
            }
        }
    }

    suspend fun removeTag(activityId: String, tag: String) {
        val activity = getActivityById(activityId)
        activity?.let {
            val currentTags = it.tags.toMutableList()
            currentTags.remove(tag)
            updateTags(activityId, currentTags)
        }
    }

    // Utility operations
    fun clearNewActivityNotification() {
        _newActivityCreated.value = null
    }

    private fun triggerRefresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // Calculation helpers
    private fun calculateUserSocialStats(
        allActivities: List<SocialActivityEntity>,
        todayActivities: List<SocialActivityEntity>,
        weeklyActivities: List<SocialActivityEntity>
    ): UserSocialStats {
        return UserSocialStats(
            totalActivities = allActivities.size,
            todayActivities = todayActivities.size,
            weeklyActivities = weeklyActivities.size,
            totalPoints = allActivities.sumOf { it.pointsEarned },
            totalShares = allActivities.sumOf { it.shares },
            totalReactions = allActivities.sumOf { it.reactions.values.sum() },
            totalComments = allActivities.sumOf { it.comments.size },
            highlightActivities = allActivities.count { it.isHighlight },
            milestoneActivities = allActivities.count { it.milestone },
            perfectDayActivities = allActivities.count { it.perfectDay },
            averagePointsPerActivity = if (allActivities.isNotEmpty()) {
                allActivities.map { it.pointsEarned }.average().toFloat()
            } else 0f,
            mostUsedMood = allActivities.mapNotNull { it.mood }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "",
            favoriteCategory = allActivities.mapNotNull { it.categoryInvolved }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: ""
        )
    }

    private fun calculateGlobalSocialStats(
        allActivities: List<SocialActivityEntity>,
        publicActivities: List<SocialActivityEntity>,
        highlightActivities: List<SocialActivityEntity>
    ): GlobalSocialStats {
        return GlobalSocialStats(
            totalActivities = allActivities.size,
            publicActivities = publicActivities.size,
            highlightActivities = highlightActivities.size,
            totalEngagement = allActivities.sumOf { it.shares + it.reactions.values.sum() },
            averageEngagement = if (allActivities.isNotEmpty()) {
                allActivities.map { it.shares + it.reactions.values.sum() }.average().toFloat()
            } else 0f,
            mostPopularActivityType = allActivities.groupingBy { it.activityType }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "",
            mostUsedEmoji = allActivities.flatMap { it.reactions.keys }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "",
            totalChallenges = allActivities.mapNotNull { it.challenge }.distinct().size
        )
    }

    // Data models
    data class UserSocialStats(
        val totalActivities: Int,
        val todayActivities: Int,
        val weeklyActivities: Int,
        val totalPoints: Int,
        val totalShares: Int,
        val totalReactions: Int,
        val totalComments: Int,
        val highlightActivities: Int,
        val milestoneActivities: Int,
        val perfectDayActivities: Int,
        val averagePointsPerActivity: Float,
        val mostUsedMood: String,
        val favoriteCategory: String
    )

    data class GlobalSocialStats(
        val totalActivities: Int,
        val publicActivities: Int,
        val highlightActivities: Int,
        val totalEngagement: Int,
        val averageEngagement: Float,
        val mostPopularActivityType: String,
        val mostUsedEmoji: String,
        val totalChallenges: Int
    )

    // Activity filtering
    fun getFilteredActivities(
        userId: String? = null,
        activityType: String? = null,
        category: String? = null,
        difficulty: String? = null,
        mood: String? = null,
        isPublic: Boolean? = null,
        isHighlight: Boolean? = null,
        milestone: Boolean? = null,
        perfectDay: Boolean? = null,
        fromDate: Long? = null,
        toDate: Long? = null
    ): Flow<List<SocialActivityEntity>> = allActivities.map { activities ->
        activities.filter { activity ->
            (userId == null || activity.userId == userId) &&
            (activityType == null || activity.activityType == activityType) &&
            (category == null || activity.categoryInvolved == category) &&
            (difficulty == null || activity.difficulty == difficulty) &&
            (mood == null || activity.mood == mood) &&
            (isPublic == null || activity.isPublic == isPublic) &&
            (isHighlight == null || activity.isHighlight == isHighlight) &&
            (milestone == null || activity.milestone == milestone) &&
            (perfectDay == null || activity.perfectDay == perfectDay) &&
            (fromDate == null || activity.timestamp >= fromDate) &&
            (toDate == null || activity.timestamp <= toDate)
        }
    }

    // Activity sorting
    fun getSortedActivities(
        sortBy: SocialSortBy = SocialSortBy.TIMESTAMP,
        ascending: Boolean = false
    ): Flow<List<SocialActivityEntity>> = allActivities.map { activities ->
        when (sortBy) {
            SocialSortBy.TIMESTAMP -> activities.sortedWith(
                if (ascending) compareBy { it.timestamp }
                else compareByDescending { it.timestamp }
            )
            SocialSortBy.POINTS -> activities.sortedWith(
                if (ascending) compareBy { it.pointsEarned }
                else compareByDescending { it.pointsEarned }
            )
            SocialSortBy.ENGAGEMENT -> activities.sortedWith(
                if (ascending) compareBy { it.shares + it.reactions.values.sum() }
                else compareByDescending { it.shares + it.reactions.values.sum() }
            )
            SocialSortBy.TITLE -> activities.sortedWith(
                if (ascending) compareBy { it.title }
                else compareByDescending { it.title }
            )
        }
    }

    enum class SocialSortBy {
        TIMESTAMP, POINTS, ENGAGEMENT, TITLE
    }
}