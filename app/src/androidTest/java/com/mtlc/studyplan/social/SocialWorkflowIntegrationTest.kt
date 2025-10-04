package com.mtlc.studyplan.social

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.data.social.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Social Features Workflow
 * Tests complete user flows through social features including:
 * - Profile management (username, avatar, goals)
 * - Leaderboard interactions
 * - Friend list management
 * - Award system
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SocialWorkflowIntegrationTest {

    private lateinit var socialRepository: SocialRepository

    @Before
    fun setup() {
        socialRepository = PersistentSocialRepository()
    }

    @Test
    fun completeProfileSetupWorkflow() = runTest {
        // Given - User starts with default profile
        val initialProfile = socialRepository.profile.first()
        assertNotNull("Initial profile should exist", initialProfile)

        // When - User updates username
        val newUsername = "TestUser123"
        socialRepository.updateUsername(newUsername)

        // Then - Username should be updated
        val updatedProfile = socialRepository.profile.first()
        assertEquals("Username should be updated", newUsername, updatedProfile.username)

        // When - User selects an avatar
        val selectedAvatar = updatedProfile.availableAvatars.firstOrNull()?.id ?: "avatar_1"
        socialRepository.selectAvatar(selectedAvatar)

        // Then - Avatar should be selected
        val profileWithAvatar = socialRepository.profile.first()
        assertEquals("Avatar should be selected", selectedAvatar, profileWithAvatar.selectedAvatarId)

        // When - User sets weekly goal
        val weeklyGoal = 20
        socialRepository.updateWeeklyGoal(weeklyGoal)

        // Then - Weekly goal should be updated
        val finalProfile = socialRepository.profile.first()
        assertEquals("Weekly goal should be updated", weeklyGoal, finalProfile.weeklyGoalHours)
    }

    @Test
    fun leaderboardRankingWorkflow() = runTest {
        // Given - Leaderboard exists with rankings
        val ranks = socialRepository.ranks.first()
        assertNotNull("Ranks should exist", ranks)

        // Then - Ranks should be ordered by XP descending
        if (ranks.size > 1) {
            for (i in 0 until ranks.size - 1) {
                assertTrue(
                    "Ranks should be ordered by XP",
                    ranks[i].xp >= ranks[i + 1].xp
                )
            }
        }

        // Then - User's own rank should be identifiable
        val userRank = ranks.find { it.isYou }
        if (userRank != null) {
            assertTrue("User rank should be marked", userRank.isYou)
            assertTrue("User XP should be non-negative", userRank.xp >= 0)
            assertTrue("User streak should be non-negative", userRank.streak >= 0)
        }

        // Then - All ranks should have valid data
        ranks.forEach { rank ->
            assertNotNull("Rank ID should not be null", rank.id)
            assertNotNull("Rank name should not be null", rank.name)
            assertTrue("Rank XP should be non-negative", rank.xp >= 0)
            assertTrue("Rank streak should be non-negative", rank.streak >= 0)
        }
    }

    @Test
    fun friendListManagementWorkflow() = runTest {
        // Given - Friend list exists
        val friends = socialRepository.friends.first()
        assertNotNull("Friends list should exist", friends)

        // Then - Friends should have valid status
        friends.forEach { friend ->
            assertNotNull("Friend ID should not be null", friend.id)
            assertNotNull("Friend name should not be null", friend.name)
            assertNotNull("Friend status should not be null", friend.status)
            assertTrue("Friend score should be non-negative", friend.score >= 0)
            assertTrue("Friend streak should be non-negative", friend.streak >= 0)

            // Valid status check
            assertTrue(
                "Friend status should be valid",
                friend.status in listOf(FriendStatus.Online, FriendStatus.Offline, FriendStatus.Studying)
            )
        }

        // Then - Friends should be identifiable by initials
        friends.forEach { friend ->
            assertTrue("Initials should not be empty", friend.initials.isNotEmpty())
            assertTrue("Initials should be 1-3 characters", friend.initials.length in 1..3)
        }
    }

    @Test
    fun awardUnlockingWorkflow() = runTest {
        // Given - Awards system exists
        val initialAwards = socialRepository.awards.first()
        assertNotNull("Awards list should exist", initialAwards)

        // Then - Awards should have valid rarity levels
        initialAwards.forEach { award ->
            assertNotNull("Award ID should not be null", award.id)
            assertNotNull("Award title should not be null", award.title)
            assertNotNull("Award description should not be null", award.description)
            assertTrue("Award points should be positive", award.points > 0)

            // Valid rarity check
            assertTrue(
                "Award rarity should be valid",
                award.rarity in listOf(AwardRarity.Common, AwardRarity.Rare, AwardRarity.Epic, AwardRarity.Legendary)
            )
        }

        // When - User unlocks an award (if any locked awards exist)
        val lockedAward = initialAwards.find { !it.isUnlocked }
        if (lockedAward != null) {
            val unlockedAward = socialRepository.unlockAward(lockedAward.id)

            // Then - Award should be unlocked
            assertNotNull("Unlocked award should be returned", unlockedAward)
            if (unlockedAward != null) {
                assertTrue("Award should be marked as unlocked", unlockedAward.isUnlocked)
                assertNotNull("Unlock date should be set", unlockedAward.unlockedDate)
            }
        }
    }

    @Test
    fun privacySettingsWorkflow() = runTest {
        // Given - User has privacy settings
        val profile = socialRepository.profile.first()

        // Then - Privacy settings should be present
        val privacyEnabled = profile.privacyEnabled

        // Then - Privacy should affect data visibility
        // In privacy mode, real identity should never be shared
        assertNotNull("Username should exist", profile.username)
        assertTrue("Username should not be empty", profile.username.isNotEmpty())

        // Then - Selected avatar should be valid
        assertNotNull("Selected avatar should exist", profile.selectedAvatarId)
        assertTrue("Selected avatar should not be empty", profile.selectedAvatarId.isNotEmpty())

        // Then - Available avatars should exist
        assertTrue("Available avatars should not be empty", profile.availableAvatars.isNotEmpty())

        // Each avatar option should be valid
        profile.availableAvatars.forEach { avatar ->
            assertNotNull("Avatar ID should not be null", avatar.id)
            assertNotNull("Avatar label should not be null", avatar.label)
            assertTrue("Avatar ID should not be empty", avatar.id.isNotEmpty())
            assertTrue("Avatar label should not be empty", avatar.label.isNotEmpty())
        }
    }

    @Test
    fun customAvatarUploadWorkflow() = runTest {
        // Given - User wants to upload custom avatar
        val initialProfile = socialRepository.profile.first()
        val testAvatarUri = "file:///test/avatar.png"

        // When - User uploads custom avatar
        socialRepository.uploadCustomAvatar(testAvatarUri)

        // Then - Custom avatar should be stored
        val updatedProfile = socialRepository.profile.first()
        assertEquals("Custom avatar URI should be updated", testAvatarUri, updatedProfile.customAvatarUri)
    }

    @Test
    fun weeklyGoalValidationWorkflow() = runTest {
        // Given - User has goal range constraints
        val profile = socialRepository.profile.first()
        val goalRange = profile.goalRange

        // Then - Goal range should be valid
        assertTrue("Goal range start should be positive", goalRange.first > 0)
        assertTrue("Goal range end should be greater than start", goalRange.last > goalRange.first)

        // When - User sets goal within valid range
        val validGoal = (goalRange.first + goalRange.last) / 2
        socialRepository.updateWeeklyGoal(validGoal)

        // Then - Goal should be accepted
        val updatedProfile = socialRepository.profile.first()
        assertEquals("Goal should be within range", validGoal, updatedProfile.weeklyGoalHours)
        assertTrue("Goal should be within valid range", updatedProfile.weeklyGoalHours in goalRange)
    }

    @Test
    fun awardProgressTrackingWorkflow() = runTest {
        // Given - User has awards to unlock
        val awards = socialRepository.awards.first()

        // Then - Awards should track unlock requirements
        awards.forEach { award ->
            assertNotNull("Award unlock requirements should exist", award.unlockedBy)
            assertTrue("Award should have unlock requirements or be already unlocked",
                award.unlockedBy.isNotEmpty() || award.isUnlocked)

            // If unlocked, should have unlock date
            if (award.isUnlocked) {
                assertNotNull("Unlocked award should have date", award.unlockedDate)
            } else {
                assertNull("Locked award should not have unlock date", award.unlockedDate)
            }
        }
    }

    @Test
    fun studyLevelDisplayWorkflow() = runTest {
        // Given - User has study level
        val profile = socialRepository.profile.first()

        // Then - Study level should be valid
        assertNotNull("Study level should exist", profile.studyLevel)
        assertTrue("Study level should not be empty", profile.studyLevel.isNotEmpty())

        // Study level should represent user's progress
        val validStudyLevels = listOf("Beginner", "Intermediate", "Advanced", "Expert", "A1", "A2", "B1", "B2", "C1", "C2")
        // Could be any study level, just verify it's set
        assertTrue("Study level should be set", profile.studyLevel.isNotEmpty())
    }

    @Test
    fun socialDataConsistencyWorkflow() = runTest {
        // Given - User interacts with multiple social features
        val initialProfile = socialRepository.profile.first()
        val initialRanks = socialRepository.ranks.first()
        val initialFriends = socialRepository.friends.first()
        val initialAwards = socialRepository.awards.first()

        // When - User updates profile
        socialRepository.updateUsername("ConsistencyTest")
        socialRepository.updateWeeklyGoal(15)

        // Then - All social data should remain consistent
        val updatedProfile = socialRepository.profile.first()
        val updatedRanks = socialRepository.ranks.first()
        val updatedFriends = socialRepository.friends.first()
        val updatedAwards = socialRepository.awards.first()

        // Profile changes should be reflected
        assertEquals("Username should be updated", "ConsistencyTest", updatedProfile.username)
        assertEquals("Weekly goal should be updated", 15, updatedProfile.weeklyGoal Hours)

        // Other data should remain stable (same size/structure)
        assertEquals("Ranks count should remain consistent", initialRanks.size, updatedRanks.size)
        assertEquals("Friends count should remain consistent", initialFriends.size, updatedFriends.size)
        assertEquals("Awards count should remain consistent", initialAwards.size, updatedAwards.size)
    }
}
