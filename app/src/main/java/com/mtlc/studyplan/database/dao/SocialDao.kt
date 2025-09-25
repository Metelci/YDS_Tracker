package com.mtlc.studyplan.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.mtlc.studyplan.database.entities.SocialActivityEntity

@Dao
interface SocialDao {

    @Query("SELECT * FROM social_activities ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserActivities(userId: String): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE id = :activityId")
    suspend fun getActivityById(activityId: String): SocialActivityEntity?

    @Query("SELECT * FROM social_activities WHERE userId = :userId AND activityType = :type ORDER BY timestamp DESC")
    fun getActivitiesByType(type: String, userId: String): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE isPublic = 1 AND visibility = 'public' ORDER BY timestamp DESC LIMIT :limit")
    fun getPublicActivities(limit: Int = 50): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE isHighlight = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getHighlightActivities(limit: Int = 10): Flow<List<SocialActivityEntity>>

    @Query("""
        SELECT * FROM social_activities
        WHERE userId IN (:friendIds)
        AND isPublic = 1
        AND visibility IN ('public', 'friends')
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun getFriendsActivities(friendIds: List<String>, limit: Int = 100): Flow<List<SocialActivityEntity>>

    @Query("""
        SELECT * FROM social_activities
        WHERE DATE(timestamp/1000, 'unixepoch') = DATE('now')
        AND userId = :userId
        ORDER BY timestamp DESC
    """)
    fun getTodayActivities(userId: String): Flow<List<SocialActivityEntity>>

    @Query("""
        SELECT * FROM social_activities
        WHERE DATE(timestamp/1000, 'unixepoch') BETWEEN DATE('now', '-7 days') AND DATE('now')
        AND userId = :userId
        ORDER BY timestamp DESC
    """)
    fun getWeeklyActivities(userId: String): Flow<List<SocialActivityEntity>>

    @Query("""
        SELECT * FROM social_activities
        WHERE DATE(timestamp/1000, 'unixepoch') BETWEEN DATE('now', 'start of month') AND DATE('now')
        AND userId = :userId
        ORDER BY timestamp DESC
    """)
    fun getMonthlyActivities(userId: String): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE milestone = 1 ORDER BY timestamp DESC")
    fun getMilestoneActivities(): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE perfectDay = 1 ORDER BY timestamp DESC")
    fun getPerfectDayActivities(): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE challenge IS NOT NULL ORDER BY timestamp DESC")
    fun getChallengeActivities(): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE challenge = :challengeName ORDER BY timestamp DESC")
    fun getActivitiesForChallenge(challengeName: String): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE categoryInvolved = :category AND userId = :userId ORDER BY timestamp DESC")
    fun getActivitiesByCategory(category: String, userId: String): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE difficulty = :difficulty AND userId = :userId ORDER BY timestamp DESC")
    fun getActivitiesByDifficulty(difficulty: String, userId: String): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE mood = :mood AND userId = :userId ORDER BY timestamp DESC")
    fun getActivitiesByMood(mood: String, userId: String): Flow<List<SocialActivityEntity>>

    @Query("SELECT * FROM social_activities WHERE location = :location ORDER BY timestamp DESC")
    fun getActivitiesByLocation(location: String): Flow<List<SocialActivityEntity>>

    @Query("""
        SELECT * FROM social_activities
        WHERE pointsEarned >= :minPoints
        AND userId = :userId
        ORDER BY pointsEarned DESC, timestamp DESC
    """)
    fun getHighScoringActivities(minPoints: Int, userId: String): Flow<List<SocialActivityEntity>>

    @Query("""
        SELECT * FROM social_activities
        WHERE streakDay IS NOT NULL
        AND userId = :userId
        ORDER BY streakDay DESC, timestamp DESC
    """)
    fun getStreakActivities(userId: String): Flow<List<SocialActivityEntity>>

    @Query("""
        SELECT * FROM social_activities
        WHERE achievementId IS NOT NULL
        AND userId = :userId
        ORDER BY timestamp DESC
    """)
    fun getAchievementActivities(userId: String): Flow<List<SocialActivityEntity>>

    @Query("""
        SELECT * FROM social_activities
        WHERE taskId IS NOT NULL
        AND userId = :userId
        ORDER BY timestamp DESC
    """)
    fun getTaskRelatedActivities(userId: String): Flow<List<SocialActivityEntity>>

    @Query("SELECT COUNT(*) FROM social_activities WHERE userId = :userId")
    suspend fun getUserActivityCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM social_activities WHERE userId = :userId AND activityType = :type")
    suspend fun getUserActivityCountByType(type: String, userId: String): Int

    @Query("SELECT SUM(pointsEarned) FROM social_activities WHERE userId = :userId")
    suspend fun getUserTotalPoints(userId: String): Int?

    @Query("SELECT SUM(studyMinutes) FROM social_activities WHERE userId = :userId AND studyMinutes IS NOT NULL")
    suspend fun getUserTotalStudyMinutes(userId: String): Int?

    @Query("SELECT SUM(shares) FROM social_activities WHERE userId = :userId")
    suspend fun getUserTotalShares(userId: String): Int?

    @Query("SELECT AVG(pointsEarned) FROM social_activities WHERE userId = :userId AND pointsEarned > 0")
    suspend fun getUserAveragePoints(userId: String): Float?

    @Query("SELECT DISTINCT activityType FROM social_activities WHERE userId = :userId")
    suspend fun getUserActivityTypes(userId: String): List<String>

    @Query("SELECT DISTINCT categoryInvolved FROM social_activities WHERE userId = :userId AND categoryInvolved IS NOT NULL")
    suspend fun getUserActiveCategories(userId: String): List<String>

    @Query("SELECT DISTINCT mood FROM social_activities WHERE userId = :userId AND mood IS NOT NULL")
    suspend fun getUserMoods(userId: String): List<String>

    @Query("SELECT DISTINCT challenge FROM social_activities WHERE challenge IS NOT NULL")
    suspend fun getAllChallenges(): List<String>

    @Query("SELECT DISTINCT location FROM social_activities WHERE location IS NOT NULL")
    suspend fun getAllLocations(): List<String>

    @Query("""
        SELECT * FROM social_activities
        WHERE timestamp >= :startTime
        AND timestamp <= :endTime
        ORDER BY timestamp DESC
    """)
    suspend fun getActivitiesInTimeRange(startTime: Long, endTime: Long): List<SocialActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: SocialActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<SocialActivityEntity>)

    @Update
    suspend fun updateActivity(activity: SocialActivityEntity)

    @Query("""
        UPDATE social_activities
        SET isPublic = :isPublic,
            visibility = :visibility,
            updatedAt = :updatedAt
        WHERE id = :activityId
    """)
    suspend fun updateVisibility(activityId: String, isPublic: Boolean, visibility: String, updatedAt: Long)

    @Query("""
        UPDATE social_activities
        SET isHighlight = :isHighlight,
            updatedAt = :updatedAt
        WHERE id = :activityId
    """)
    suspend fun updateHighlightStatus(activityId: String, isHighlight: Boolean, updatedAt: Long)

    @Query("""
        UPDATE social_activities
        SET shares = shares + 1,
            updatedAt = :updatedAt
        WHERE id = :activityId
    """)
    suspend fun incrementShares(activityId: String, updatedAt: Long)

    @Query("""
        UPDATE social_activities
        SET reactions = :reactions,
            updatedAt = :updatedAt
        WHERE id = :activityId
    """)
    suspend fun updateReactions(activityId: String, reactions: Map<String, Int>, updatedAt: Long)

    @Query("""
        UPDATE social_activities
        SET comments = :comments,
            updatedAt = :updatedAt
        WHERE id = :activityId
    """)
    suspend fun updateComments(activityId: String, comments: List<String>, updatedAt: Long)

    @Query("""
        UPDATE social_activities
        SET tags = :tags,
            updatedAt = :updatedAt
        WHERE id = :activityId
    """)
    suspend fun updateTags(activityId: String, tags: List<String>, updatedAt: Long)

    @Delete
    suspend fun deleteActivity(activity: SocialActivityEntity)

    @Query("DELETE FROM social_activities WHERE id = :activityId")
    suspend fun deleteActivityById(activityId: String)

    @Query("DELETE FROM social_activities WHERE userId = :userId")
    suspend fun deleteUserActivities(userId: String)

    @Query("DELETE FROM social_activities WHERE timestamp < :cutoffTime")
    suspend fun deleteOldActivities(cutoffTime: Long)

    @Query("DELETE FROM social_activities")
    suspend fun deleteAllActivities()

    // Transaction methods
    @Transaction
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
        val activity = SocialActivityEntity(
            userId = userId,
            activityType = "TASK_COMPLETED",
            title = "Task Completed: $taskTitle",
            description = "Completed a $difficulty task in $category",
            pointsEarned = pointsEarned,
            taskId = taskId,
            categoryInvolved = category,
            difficulty = difficulty,
            studyMinutes = studyMinutes,
            isPublic = isPublic
        )
        insertActivity(activity)
    }

    @Transaction
    suspend fun createAchievementActivity(
        userId: String,
        achievementId: String,
        achievementTitle: String,
        achievementDescription: String,
        pointsEarned: Int,
        isPublic: Boolean = true
    ) {
        val activity = SocialActivityEntity(
            userId = userId,
            activityType = "ACHIEVEMENT_UNLOCKED",
            title = "Achievement Unlocked: $achievementTitle",
            description = achievementDescription,
            pointsEarned = pointsEarned,
            achievementId = achievementId,
            isPublic = isPublic,
            isHighlight = true
        )
        insertActivity(activity)
    }

    @Transaction
    suspend fun createStreakMilestoneActivity(
        userId: String,
        streakDay: Int,
        pointsEarned: Int,
        isPublic: Boolean = true
    ) {
        val activity = SocialActivityEntity(
            userId = userId,
            activityType = "STREAK_MILESTONE",
            title = "Streak Milestone Reached!",
            description = "Reached $streakDay day streak!",
            pointsEarned = pointsEarned,
            streakDay = streakDay,
            milestone = true,
            isPublic = isPublic,
            isHighlight = streakDay % 10 == 0 // Highlight every 10th day
        )
        insertActivity(activity)
    }

    @Transaction
    suspend fun createPerfectDayActivity(
        userId: String,
        tasksCompleted: Int,
        studyMinutes: Int,
        pointsEarned: Int,
        isPublic: Boolean = true
    ) {
        val activity = SocialActivityEntity(
            userId = userId,
            activityType = "PERFECT_DAY",
            title = "Perfect Study Day!",
            description = "Completed $tasksCompleted tasks and studied for $studyMinutes minutes",
            pointsEarned = pointsEarned,
            studyMinutes = studyMinutes,
            perfectDay = true,
            isPublic = isPublic,
            isHighlight = true
        )
        insertActivity(activity)
    }
}