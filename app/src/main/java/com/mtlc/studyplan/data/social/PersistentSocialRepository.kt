package com.mtlc.studyplan.data.social

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.AvatarEntity
import com.mtlc.studyplan.utils.ImageProcessingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Persistent implementation of SocialRepository using DataStore for username storage
 * and Room database for avatar storage with proper file management.
 */
class PersistentSocialRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val database: StudyPlanDatabase
) : SocialRepository {

    companion object {
        private val USERNAME_KEY = stringPreferencesKey("social_username")
        private val SELECTED_AVATAR_KEY = stringPreferencesKey("selected_avatar")
        private val CUSTOM_AVATAR_URI_KEY = stringPreferencesKey("custom_avatar_uri")
    }

    private val currentUserId = "default_user" // In a real app, this would come from authentication

    private val profileState = MutableStateFlow(
        SocialProfile(
            username = "",
            selectedAvatarId = "target",
            availableAvatars = listOf(
                AvatarOption("target", "Target"),
                AvatarOption("rocket", "Rocket"),
                AvatarOption("star", "Star"),
                AvatarOption("flame", "Flame"),
                AvatarOption("diamond", "Diamond"),
                AvatarOption("trophy", "Trophy"),
                AvatarOption("puzzle", "Puzzle"),
                AvatarOption("sun", "Sun")
            ),
            studyLevel = "Intermediate",
            weeklyGoalHours = 15,
            goalRange = 3..35,
            privacyEnabled = true
        )
    )

    private val ranksState = MutableStateFlow(emptyList<RankEntry>())

    private val groupsState = MutableStateFlow(
        listOf(
            Group(
                id = "g1",
                name = "YDS Warriors",
                members = 24,
                tags = listOf("General YDS", "High Activity"),
                activity = "Daily practice sessions",
                description = "Daily practice sessions",
                joined = true
            ),
            Group(
                id = "g2",
                name = "Vocabulary Masters",
                members = 18,
                tags = listOf("Vocabulary", "Medium Activity"),
                activity = "Focus on word building",
                description = "Focus on word building"
            ),
            Group(
                id = "g3",
                name = "Grammar Experts",
                members = 31,
                tags = listOf("Grammar", "High Activity"),
                activity = "Advanced grammar practice",
                description = "Advanced grammar practice"
            )
        )
    )

    private val friendsState = MutableStateFlow(emptyList<Friend>())

    private val awardsState = MutableStateFlow(
        listOf(
            // Legendary Awards
            Award(
                id = "a1",
                title = "Study Streak Master",
                description = "Maintained 30-day streak",
                rarity = AwardRarity.Legendary,
                tags = listOf("Streak", "Dedication"),
                unlockedBy = listOf("Alex Chen", "Sarah Kim"),
                isUnlocked = false
            ),
            Award(
                id = "a2",
                title = "YDS Champion",
                description = "Scored 80+ in YDS exam",
                rarity = AwardRarity.Legendary,
                tags = listOf("YDS", "Achievement"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a3",
                title = "Perfect Week",
                description = "Completed all study goals for 7 consecutive days",
                rarity = AwardRarity.Legendary,
                tags = listOf("Streak", "Discipline"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a16",
                title = "YDS Legend",
                description = "Scored 85+ in YDS exam - Top 1% achievement",
                rarity = AwardRarity.Legendary,
                tags = listOf("YDS", "Top Score"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a17",
                title = "Academic Excellence",
                description = "Maintained 90%+ accuracy in all practice tests",
                rarity = AwardRarity.Legendary,
                tags = listOf("Accuracy", "Consistency"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),

            // Epic Awards
            Award(
                id = "a4",
                title = "Vocabulary Virtuoso",
                description = "Learned 1000+ words",
                rarity = AwardRarity.Epic,
                tags = listOf("Vocabulary", "Milestone"),
                unlockedBy = listOf("Mike Jones", "Emma Davis"),
                isUnlocked = false
            ),
            Award(
                id = "a5",
                title = "Grammar Guardian",
                description = "Mastered all grammar sections",
                rarity = AwardRarity.Epic,
                tags = listOf("Grammar", "Completion"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a6",
                title = "Reading Rocket",
                description = "Completed 50 reading comprehension exercises",
                rarity = AwardRarity.Epic,
                tags = listOf("Reading", "Speed"),
                unlockedBy = listOf("Sarah Kim"),
                isUnlocked = false
            ),
            Award(
                id = "a7",
                title = "Consistent Learner",
                description = "Studied for 14 consecutive days",
                rarity = AwardRarity.Epic,
                tags = listOf("Streak", "Consistency"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a18",
                title = "YDS Scholar",
                description = "Completed 100 YDS practice questions",
                rarity = AwardRarity.Epic,
                tags = listOf("YDS", "Practice"),
                unlockedBy = listOf("Alex Chen"),
                isUnlocked = false
            ),
            Award(
                id = "a19",
                title = "Listening Master",
                description = "Achieved 85%+ in 10 listening exercises",
                rarity = AwardRarity.Epic,
                tags = listOf("Listening", "Accuracy"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a20",
                title = "Academic Vocabulary Expert",
                description = "Mastered 500+ academic English words",
                rarity = AwardRarity.Epic,
                tags = listOf("Vocabulary", "Academic"),
                unlockedBy = listOf("Sarah Kim"),
                isUnlocked = false
            ),
            Award(
                id = "a21",
                title = "Writing Wizard",
                description = "Achieved 85%+ in 5 writing exercises",
                rarity = AwardRarity.Epic,
                tags = listOf("Writing", "Mastery"),
                unlockedBy = listOf("Lisa Wang"),
                isUnlocked = false
            ),
            Award(
                id = "a22",
                title = "Time Management Pro",
                description = "Completed 3 practice tests within time limit",
                rarity = AwardRarity.Epic,
                tags = listOf("Planning", "Focus"),
                unlockedBy = listOf("Mike Jones"),
                isUnlocked = false
            ),

            // Rare Awards
            Award(
                id = "a8",
                title = "Speed Demon",
                description = "Completed 100 questions in 30min",
                rarity = AwardRarity.Rare,
                tags = listOf("Speed", "Accuracy"),
                unlockedBy = listOf("Sarah Kim", "Lisa Wang"),
                isUnlocked = false
            ),
            Award(
                id = "a9",
                title = "Early Bird",
                description = "Studied before 8 AM for 5 days",
                rarity = AwardRarity.Rare,
                tags = listOf("Routine", "Discipline"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a10",
                title = "Night Owl",
                description = "Studied after 10 PM for 5 days",
                rarity = AwardRarity.Rare,
                tags = listOf("Routine", "Persistence"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a11",
                title = "Word Hunter",
                description = "Learned 100 new words",
                rarity = AwardRarity.Rare,
                tags = listOf("Vocabulary", "Milestone"),
                unlockedBy = listOf("Mike Jones", "Emma Davis"),
                isUnlocked = false
            ),
            Award(
                id = "a12",
                title = "Practice Pioneer",
                description = "Completed 25 practice tests",
                rarity = AwardRarity.Rare,
                tags = listOf("Practice", "Endurance"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a13",
                title = "First Steps",
                description = "Completed your first study session",
                rarity = AwardRarity.Rare,
                tags = listOf("Getting Started", "Milestone"),
                unlockedBy = listOf("Alex Chen", "Sarah Kim", "Mike Jones", "Emma Davis"),
                isUnlocked = false
            ),
            Award(
                id = "a14",
                title = "Social Butterfly",
                description = "Joined your first study group",
                rarity = AwardRarity.Rare,
                tags = listOf("Community", "Collaboration"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a15",
                title = "Goal Setter",
                description = "Set your weekly study goal",
                rarity = AwardRarity.Rare,
                tags = listOf("Planning", "Motivation"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a23",
                title = "YDS Starter",
                description = "Completed your first YDS practice test",
                rarity = AwardRarity.Rare,
                tags = listOf("YDS", "Milestone"),
                unlockedBy = listOf("Alex Chen"),
                isUnlocked = false
            ),
            Award(
                id = "a24",
                title = "Vocabulary Builder",
                description = "Learned 50 new words in one week",
                rarity = AwardRarity.Rare,
                tags = listOf("Vocabulary", "Weekly Goal"),
                unlockedBy = listOf("Emma Davis"),
                isUnlocked = false
            ),
            Award(
                id = "a25",
                title = "Reading Enthusiast",
                description = "Completed 10 reading comprehension exercises",
                rarity = AwardRarity.Rare,
                tags = listOf("Reading", "Practice"),
                unlockedBy = listOf("Lisa Wang", "Sarah Kim"),
                isUnlocked = false
            ),
            Award(
                id = "a26",
                title = "Grammar Rookie",
                description = "Scored 80%+ in grammar section",
                rarity = AwardRarity.Rare,
                tags = listOf("Grammar", "Achievement"),
                unlockedBy = listOf("Mike Jones"),
                isUnlocked = false
            ),
            Award(
                id = "a27",
                title = "Weekly Warrior",
                description = "Studied every day for one week",
                rarity = AwardRarity.Rare,
                tags = listOf("Streak", "Commitment"),
                unlockedBy = listOf("Tom Wilson"),
                isUnlocked = false
            ),
            Award(
                id = "a28",
                title = "Question Master",
                description = "Answered 500 practice questions correctly",
                rarity = AwardRarity.Rare,
                tags = listOf("Practice", "Accuracy"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a29",
                title = "Motivation Keeper",
                description = "Maintained study streak during exam stress period",
                rarity = AwardRarity.Rare,
                tags = listOf("Streak", "Resilience"),
                unlockedBy = listOf("Alex Chen"),
                isUnlocked = false
            ),
            Award(
                id = "a30",
                title = "Error Hunter",
                description = "Identified and corrected 100 mistakes",
                rarity = AwardRarity.Rare,
                tags = listOf("Review", "Focus"),
                unlockedBy = listOf("Sarah Kim", "Emma Davis"),
                isUnlocked = false
            ),
            Award(
                id = "a31",
                title = "Study Buddy",
                description = "Helped 3 friends with their YDS preparation",
                rarity = AwardRarity.Rare,
                tags = listOf("Community", "Support"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "a32",
                title = "Progress Tracker",
                description = "Logged study progress for 30 days",
                rarity = AwardRarity.Rare,
                tags = listOf("Habits", "Tracking"),
                unlockedBy = listOf(),
                isUnlocked = false
            )
        )
    )

    override val profile: StateFlow<SocialProfile> = profileState
    override val ranks: StateFlow<List<RankEntry>> = ranksState
    override val groups: StateFlow<List<Group>> = groupsState
    override val friends: StateFlow<List<Friend>> = friendsState
    override val awards: StateFlow<List<Award>> = awardsState

    init {
        // Load persistent data on initialization
        loadPersistedData()
    }

    private fun loadPersistedData() {
        // Load username and avatar from DataStore and database
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = dataStore.data.first()
            val savedUsername = preferences[USERNAME_KEY] ?: ""
            val savedAvatar = preferences[SELECTED_AVATAR_KEY] ?: "target"

            // Load active avatar from database
            val activeAvatar = database.avatarDao().getActiveAvatar(currentUserId).first()
            val customAvatarUri = activeAvatar?.let { "file://${it.filePath}" }

            profileState.update { currentProfile ->
                currentProfile.copy(
                    username = savedUsername,
                    selectedAvatarId = if (activeAvatar != null) "custom" else savedAvatar,
                    customAvatarUri = customAvatarUri
                )
            }
        }
    }

    override suspend fun toggleGroupMembership(groupId: String) {
        groupsState.update { groups ->
            groups.map { group ->
                if (group.id == groupId) group.copy(joined = !group.joined) else group
            }
        }
    }

    override suspend fun shareGroup(groupId: String) {
        // Stub: keeping structure for future analytics/logging.
        delay(120)
    }

    override suspend fun selectAvatar(avatarId: String) {
        // Update state
        profileState.update { profile -> profile.copy(selectedAvatarId = avatarId) }

        // Persist to DataStore
        dataStore.edit { preferences ->
            preferences[SELECTED_AVATAR_KEY] = avatarId
        }
    }

    override suspend fun updateWeeklyGoal(hours: Int) {
        profileState.update { profile ->
            profile.copy(weeklyGoalHours = hours.coerceIn(profile.goalRange))
        }
    }

    override suspend fun updateUsername(username: String) {
        val sanitized = username.trim()

        // Update state
        profileState.update { it.copy(username = sanitized) }

        // Persist to DataStore
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = sanitized
        }
    }

    override suspend fun unlockAward(awardId: String): Award? {
        var unlockedAward: Award? = null
        awardsState.update { awards ->
            awards.map { award ->
                if (award.id == awardId && !award.isUnlocked) {
                    val updated = award.copy(
                        isUnlocked = true,
                        unlockedDate = getCurrentDate(),
                        unlockedBy = award.unlockedBy + "You"
                    )
                    unlockedAward = updated
                    updated
                } else {
                    award
                }
            }
        }
        return unlockedAward
    }

    override suspend fun uploadCustomAvatar(uri: String) {
        try {
            val sourceUri = Uri.parse(uri)

            // Process and save image using ImageProcessingUtils
            val result = ImageProcessingUtils.processAndSaveImage(context, sourceUri, currentUserId)

            result.fold(
                onSuccess = { processedImage ->
                    // Create avatar entity
                    val avatarEntity = AvatarEntity(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        fileName = processedImage.file.name,
                        filePath = processedImage.file.absolutePath,
                        originalUri = uri,
                        fileSize = processedImage.fileSize,
                        width = processedImage.width,
                        height = processedImage.height,
                        mimeType = processedImage.mimeType,
                        uploadedAt = System.currentTimeMillis(),
                        isActive = true
                    )

                    // Deactivate all previous avatars
                    database.avatarDao().deactivateAllAvatars(currentUserId)

                    // Insert new avatar
                    database.avatarDao().insertAvatar(avatarEntity)

                    // Update profile state
                    profileState.update { profile ->
                        profile.copy(
                            customAvatarUri = "file://${processedImage.file.absolutePath}",
                            selectedAvatarId = "custom"
                        )
                    }

                    // Update DataStore
                    dataStore.edit { preferences ->
                        preferences[SELECTED_AVATAR_KEY] = "custom"
                        preferences.remove(CUSTOM_AVATAR_URI_KEY) // Remove old URI storage
                    }

                    // Cleanup old files
                    ImageProcessingUtils.cleanupOldAvatars(context, currentUserId, keepCount = 3)
                },
                onFailure = { exception ->
                    // Handle error - in a real app, you might want to throw this or handle it differently
                    throw exception
                }
            )
        } catch (e: Exception) {
            throw e
        }
    }

    private fun getCurrentDate(): String {
        // Simple date formatting for demo purposes
        val now = java.time.LocalDate.now()
        return "${now.year}-${now.monthValue.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
    }
}
