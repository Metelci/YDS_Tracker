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
            // Awards ready for initial use - all locked by default
            Award(
                id = "first_steps",
                title = "First Steps",
                description = "Complete your first task",
                rarity = AwardRarity.Common,
                points = 50,
                iconType = AwardIconType.Target,
                tags = listOf("Getting Started"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "week_warrior",
                title = "Week Warrior",
                description = "Maintain 7-day streak",
                rarity = AwardRarity.Rare,
                points = 150,
                iconType = AwardIconType.Shield,
                tags = listOf("Streak"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "grammar_master",
                title = "Grammar Master",
                description = "Score 90% in grammar",
                rarity = AwardRarity.Epic,
                points = 300,
                iconType = AwardIconType.Crown,
                tags = listOf("Grammar"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "speed_reader",
                title = "Speed Reader",
                description = "Read 50 articles",
                rarity = AwardRarity.Rare,
                points = 200,
                iconType = AwardIconType.Book,
                tags = listOf("Reading"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "perfect_score_legend",
                title = "Perfect Score Legend",
                description = "Get 100% on 5 consecutive tests",
                rarity = AwardRarity.Legendary,
                points = 500,
                iconType = AwardIconType.Diamond,
                tags = listOf("Perfect Score"),
                unlockedBy = listOf(),
                isUnlocked = false
            ),
            Award(
                id = "study_streak_champion",
                title = "Study Streak Champion",
                description = "Maintain 30-day study streak",
                rarity = AwardRarity.Epic,
                points = 400,
                iconType = AwardIconType.Lightbulb,
                tags = listOf("Streak"),
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

    override suspend fun updateCustomAvatar(imagePath: String) {
        // This method updates an existing custom avatar with a new image path
        // It's similar to uploadCustomAvatar but assumes the image is already processed
        try {
            val timestamp = System.currentTimeMillis()
            val file = java.io.File(imagePath)

            // Create avatar entity with all required parameters
            val avatarEntity = AvatarEntity(
                id = "custom_avatar_${currentUserId}_$timestamp",
                userId = currentUserId,
                fileName = file.name,
                filePath = imagePath,
                originalUri = "file://$imagePath",
                fileSize = if (file.exists()) file.length() else 0L,
                width = 150, // Default size for processed avatars
                height = 150,
                mimeType = "image/jpeg", // Assume processed images are JPEG
                uploadedAt = timestamp,
                isActive = true
            )

            // Deactivate existing avatars and insert new one
            database.avatarDao().deactivateAllAvatars(currentUserId)
            database.avatarDao().insertAvatar(avatarEntity)

            // Update profile state
            profileState.update { profile ->
                profile.copy(
                    customAvatarUri = "file://$imagePath",
                    selectedAvatarId = "custom"
                )
            }

            // Update DataStore
            dataStore.edit { preferences ->
                preferences[SELECTED_AVATAR_KEY] = "custom"
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun getCurrentDate(): String {
        // Simple date formatting for demo purposes
        val now = java.time.LocalDate.now()
        return "${now.dayOfMonth.toString().padStart(2, '0')}/${now.monthValue.toString().padStart(2, '0')}/${now.year}"
    }

}
