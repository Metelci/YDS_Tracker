package com.mtlc.studyplan.data.social

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Persistent implementation of SocialRepository using DataStore for username storage.
 * Provides the same interface as FakeSocialRepository but persists username across app restarts.
 */
class PersistentSocialRepository(
    private val dataStore: DataStore<Preferences>
) : SocialRepository {

    companion object {
        private val USERNAME_KEY = stringPreferencesKey("social_username")
        private val SELECTED_AVATAR_KEY = stringPreferencesKey("selected_avatar")
    }

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

    private val ranksState = MutableStateFlow(
        listOf(
            RankEntry("1", "Alex Chen", "AC", xp = 2850, streak = 25),
            RankEntry("2", "Sarah Kim", "SK", xp = 2720, streak = 18),
            RankEntry("3", "You", "YU", xp = 2650, streak = 15, isYou = true),
            RankEntry("4", "Mike Jones", "MJ", xp = 2580, streak = 12),
            RankEntry("5", "Emma Davis", "ED", xp = 2450, streak = 20)
        )
    )

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

    private val friendsState = MutableStateFlow(
        listOf(
            Friend("f1", "JL", "Jessica Liu", FriendStatus.Online, score = 85, streak = 12),
            Friend("f2", "DP", "David Park", FriendStatus.Offline, score = 92, streak = 8),
            Friend("f3", "LW", "Lisa Wang", FriendStatus.Studying, score = 78, streak = 22),
            Friend("f4", "TW", "Tom Wilson", FriendStatus.Online, score = 88, streak = 5)
        )
    )

    private val awardsState = MutableStateFlow(
        listOf(
            Award(
                id = "a1",
                title = "Study Streak Master",
                description = "Maintained 30-day streak",
                rarity = AwardRarity.Legendary,
                unlockedBy = listOf("Alex Chen", "Sarah Kim")
            ),
            Award(
                id = "a2",
                title = "Vocabulary Virtuoso",
                description = "Learned 1000+ words",
                rarity = AwardRarity.Epic,
                unlockedBy = listOf("You", "Mike Jones", "Emma Davis")
            ),
            Award(
                id = "a3",
                title = "Speed Demon",
                description = "Completed 100 questions in 30min",
                rarity = AwardRarity.Rare,
                unlockedBy = listOf("Sarah Kim", "Lisa Wang")
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
        // Load username and avatar from DataStore
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = dataStore.data.first()
            val savedUsername = preferences[USERNAME_KEY] ?: ""
            val savedAvatar = preferences[SELECTED_AVATAR_KEY] ?: "target"

            profileState.update { currentProfile ->
                currentProfile.copy(
                    username = savedUsername,
                    selectedAvatarId = savedAvatar
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
}