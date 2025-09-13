package com.mtlc.studyplan.social

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.util.*

// Privacy-first social features with minimal data sharing

@Immutable
data class StudyBuddy(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String, // User-chosen display name, not real name
    val avatarEmoji: String = "👤", // Simple emoji avatar
    val studyLevel: StudyLevel,
    val targetExam: String,
    val preferredStudyTimes: List<StudyTimeSlot>,
    val weeklyGoalMinutes: Int,
    val isOnline: Boolean = false,
    val lastSeen: LocalDateTime? = null,
    val compatibilityScore: Float = 0f, // 0.0 to 1.0
    val privacySettings: BuddyPrivacySettings = BuddyPrivacySettings()
) {
    // Anonymized identifier for matching without exposing real user data
    val anonymizedId: String = UUID.nameUUIDFromBytes(id.toByteArray()).toString().take(8)
}

enum class StudyLevel(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    EXAM_PREP("Exam Prep")
}

@Immutable
data class StudyTimeSlot(
    val dayOfWeek: Int, // 1-7 (Monday-Sunday)
    val startHour: Int, // 0-23
    val endHour: Int, // 0-23
    val timezone: String = TimeZone.getDefault().id
)

@Immutable
data class BuddyPrivacySettings(
    val shareProgressStats: Boolean = false, // Share basic progress metrics
    val shareStudyStreak: Boolean = false, // Share current streak count
    val shareWeakAreas: Boolean = false, // Share categories needing improvement
    val allowDirectMessages: Boolean = true, // Allow private messages
    val showOnlineStatus: Boolean = false, // Show when online/offline
    val shareStudyGoals: Boolean = false, // Share weekly/daily goals
    val anonymousMode: Boolean = true // Additional anonymization layer
)

@Immutable
data class ProgressShare(
    val id: String = UUID.randomUUID().toString(),
    val buddyId: String,
    val buddyDisplayName: String,
    val shareType: ShareType,
    val content: ShareContent,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val reactions: Map<String, Int> = emptyMap(), // buddyId -> reactionType
    val isPrivate: Boolean = true // Private to study buddy group only
)

enum class ShareType {
    STREAK_MILESTONE, // "Just hit a 7-day streak!"
    GOAL_ACHIEVED,    // "Completed weekly goal"
    WEAK_AREA_IMPROVED, // "Improved grammar score by 20%"
    STUDY_SESSION_COMPLETED, // "Just completed a 45-min focus session"
    MOTIVATION_BOOST  // Motivational message or tip
}

@Immutable
sealed class ShareContent {
    data class StreakMilestone(val days: Int) : ShareContent()
    data class GoalAchieved(val goalType: String, val progress: String) : ShareContent()
    data class WeakAreaImproved(val category: String, val improvementPercent: Int) : ShareContent()
    data class StudySessionCompleted(val durationMinutes: Int, val category: String) : ShareContent()
    data class MotivationBoost(val message: String, val tips: List<String> = emptyList()) : ShareContent()
}

@Immutable
data class StudyGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val members: List<StudyBuddy> = emptyList(),
    val maxMembers: Int = 8, // Keep groups small for better interaction
    val targetExam: String,
    val studyLevel: StudyLevel,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val activityCount: Int = 0,
    val isPrivate: Boolean = true,
    val joinCode: String = generateJoinCode() // 6-digit code for joining
) {
    val memberCount: Int get() = members.size
    val isActive: Boolean get() = activityCount > 0 && members.any { it.lastSeen?.isAfter(LocalDateTime.now().minusHours(24)) == true }
}

private fun generateJoinCode(): String = (100000..999999).random().toString()

// Privacy-preserving matching algorithm
class StudyBuddyMatcher {

    fun calculateCompatibility(user: StudyBuddy, candidate: StudyBuddy): Float {
        var score = 0f
        var factors = 0

        // Study level compatibility (40% weight)
        score += when {
            user.studyLevel == candidate.studyLevel -> 0.4f
            abs(user.studyLevel.ordinal - candidate.studyLevel.ordinal) <= 1 -> 0.2f
            else -> 0f
        }
        factors++

        // Target exam compatibility (30% weight)
        if (user.targetExam == candidate.targetExam) {
            score += 0.3f
        }
        factors++

        // Study time overlap (20% weight)
        val timeOverlap = calculateTimeOverlap(user.preferredStudyTimes, candidate.preferredStudyTimes)
        score += timeOverlap * 0.2f
        factors++

        // Goal similarity (10% weight)
        val goalSimilarity = calculateGoalSimilarity(user.weeklyGoalMinutes, candidate.weeklyGoalMinutes)
        score += goalSimilarity * 0.1f
        factors++

        return score / factors
    }

    private fun calculateTimeOverlap(userTimes: List<StudyTimeSlot>, candidateTimes: List<StudyTimeSlot>): Float {
        var overlapHours = 0
        var totalHours = 0

        for (userSlot in userTimes) {
            for (candidateSlot in candidateTimes) {
                if (userSlot.dayOfWeek == candidateSlot.dayOfWeek) {
                    val overlapStart = maxOf(userSlot.startHour, candidateSlot.startHour)
                    val overlapEnd = minOf(userSlot.endHour, candidateSlot.endHour)
                    if (overlapStart < overlapEnd) {
                        overlapHours += (overlapEnd - overlapStart)
                    }
                }
                totalHours += (userSlot.endHour - userSlot.startHour)
            }
        }

        return if (totalHours > 0) overlapHours.toFloat() / totalHours else 0f
    }

    private fun calculateGoalSimilarity(userGoal: Int, candidateGoal: Int): Float {
        val difference = kotlin.math.abs(userGoal - candidateGoal)
        val average = (userGoal + candidateGoal) / 2f
        return if (average > 0) 1f - (difference / average).coerceAtMost(1f) else 0f
    }
}

// Mock social repository (in real app, this would be a proper backend)
class SocialRepository {
    private val _studyBuddies = MutableStateFlow<List<StudyBuddy>>(emptyList())
    val studyBuddies: StateFlow<List<StudyBuddy>> = _studyBuddies.asStateFlow()

    private val _studyGroups = MutableStateFlow<List<StudyGroup>>(emptyList())
    val studyGroups: StateFlow<List<StudyGroup>> = _studyGroups.asStateFlow()

    private val _progressShares = MutableStateFlow<List<ProgressShare>>(emptyList())
    val progressShares: StateFlow<List<ProgressShare>> = _progressShares.asStateFlow()

    private val _userProfile = MutableStateFlow<StudyBuddy?>(null)
    val userProfile: StateFlow<StudyBuddy?> = _userProfile.asStateFlow()

    private val matcher = StudyBuddyMatcher()

    // Create user profile with privacy-first approach
    suspend fun createUserProfile(
        displayName: String,
        avatarEmoji: String,
        studyLevel: StudyLevel,
        targetExam: String,
        preferredStudyTimes: List<StudyTimeSlot>,
        weeklyGoalMinutes: Int,
        privacySettings: BuddyPrivacySettings
    ): StudyBuddy {
        val profile = StudyBuddy(
            displayName = displayName,
            avatarEmoji = avatarEmoji,
            studyLevel = studyLevel,
            targetExam = targetExam,
            preferredStudyTimes = preferredStudyTimes,
            weeklyGoalMinutes = weeklyGoalMinutes,
            privacySettings = privacySettings,
            isOnline = true,
            lastSeen = LocalDateTime.now()
        )

        _userProfile.value = profile
        return profile
    }

    // Find compatible study buddies
    suspend fun findCompatibleBuddies(limit: Int = 10): List<StudyBuddy> {
        val userProfile = _userProfile.value ?: return emptyList()

        // In real implementation, this would query a privacy-preserving backend
        // For demo purposes, generate some mock compatible buddies
        return generateMockBuddies(userProfile, limit)
            .map { buddy ->
                buddy.copy(compatibilityScore = matcher.calculateCompatibility(userProfile, buddy))
            }
            .sortedByDescending { it.compatibilityScore }
    }

    // Join or create a study group
    suspend fun joinStudyGroup(groupId: String): Boolean {
        val userProfile = _userProfile.value ?: return false
        val currentGroups = _studyGroups.value.toMutableList()

        val groupIndex = currentGroups.indexOfFirst { it.id == groupId }
        if (groupIndex != -1) {
            val group = currentGroups[groupIndex]
            if (group.members.size < group.maxMembers) {
                val updatedGroup = group.copy(
                    members = group.members + userProfile,
                    activityCount = group.activityCount + 1
                )
                currentGroups[groupIndex] = updatedGroup
                _studyGroups.value = currentGroups
                return true
            }
        }

        return false
    }

    // Share progress with privacy controls
    suspend fun shareProgress(
        shareType: ShareType,
        content: ShareContent,
        isPrivate: Boolean = true
    ): ProgressShare {
        val userProfile = _userProfile.value ?: error("User profile not set")

        val share = ProgressShare(
            buddyId = userProfile.id,
            buddyDisplayName = if (userProfile.privacySettings.anonymousMode)
                "Study Buddy ${userProfile.anonymizedId}"
            else
                userProfile.displayName,
            shareType = shareType,
            content = content,
            isPrivate = isPrivate
        )

        val currentShares = _progressShares.value.toMutableList()
        currentShares.add(0, share) // Add to beginning
        _progressShares.value = currentShares.take(50) // Keep only recent 50 shares

        return share
    }

    // Get study group activity feed
    fun getGroupActivityFeed(groupId: String): Flow<List<ProgressShare>> {
        // Filter shares for specific group members
        return progressShares
    }

    private fun generateMockBuddies(userProfile: StudyBuddy, count: Int): List<StudyBuddy> {
        val mockNames = listOf(
            "StudyNinja", "BookwormBee", "GrammarGuru", "VocabViper", "TestTiger",
            "ReadingRaven", "WritingWizard", "ListeningLion", "SpeakingStar", "LearnLegend"
        )
        val mockEmojis = listOf("🦸", "🐝", "🧙", "🐍", "🐅", "🐦‍⬛", "🧙‍♂️", "🦁", "⭐", "🏆")

        return (1..count).map { i ->
            StudyBuddy(
                displayName = mockNames.random(),
                avatarEmoji = mockEmojis.random(),
                studyLevel = StudyLevel.values().random(),
                targetExam = userProfile.targetExam, // Same exam for better compatibility
                preferredStudyTimes = generateMockStudyTimes(),
                weeklyGoalMinutes = (120..600).random(), // 2-10 hours per week
                isOnline = (0..10).random() > 3, // 70% chance of being online
                lastSeen = if ((0..10).random() > 3) LocalDateTime.now().minusHours((0..24).random().toLong()) else null
            )
        }
    }

    private fun generateMockStudyTimes(): List<StudyTimeSlot> {
        return listOf(
            StudyTimeSlot(dayOfWeek = (1..7).random(), startHour = (9..14).random(), endHour = (15..21).random()),
            StudyTimeSlot(dayOfWeek = (1..7).random(), startHour = (18..19).random(), endHour = (20..22).random())
        )
    }
}