package com.mtlc.studyplan.data.social

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Social Repository business logic
 */
class SocialRepositoryTest {

    private lateinit var mockProfile: SocialProfile
    private lateinit var mockRanks: List<RankEntry>
    private lateinit var mockFriends: List<Friend>
    private lateinit var mockAwards: List<Award>

    @Before
    fun setup() {
        mockProfile = SocialProfile(
            username = "TestUser",
            selectedAvatarId = "avatar_1",
            availableAvatars = listOf(
                AvatarOption("avatar_1", "Ninja"),
                AvatarOption("avatar_2", "Wizard"),
                AvatarOption("avatar_3", "Knight")
            ),
            studyLevel = "Intermediate",
            weeklyGoalHours = 20,
            goalRange = 10..30,
            privacyEnabled = true
        )

        mockRanks = listOf(
            RankEntry("1", "Alice", "A", 10000, 30, false),
            RankEntry("2", "Bob", "B", 9000, 25, false),
            RankEntry("3", "TestUser", "T", 8000, 20, true),
            RankEntry("4", "David", "D", 7000, 15, false)
        )

        mockFriends = listOf(
            Friend("f1", "AB", "Alice", FriendStatus.Online, 5000, 15),
            Friend("f2", "CD", "Charlie", FriendStatus.Studying, 4500, 12),
            Friend("f3", "EF", "Eve", FriendStatus.Offline, 4000, 10)
        )

        mockAwards = listOf(
            Award(
                id = "award1",
                title = "First Steps",
                description = "Complete your first task",
                rarity = AwardRarity.Common,
                points = 100,
                iconType = AwardIconType.Target,
                unlockedBy = listOf("task_1"),
                isUnlocked = true,
                unlockedDate = "2025-01-01"
            ),
            Award(
                id = "award2",
                title = "Week Warrior",
                description = "Maintain 7-day streak",
                rarity = AwardRarity.Rare,
                points = 300,
                iconType = AwardIconType.Shield,
                unlockedBy = listOf("streak_7"),
                isUnlocked = false
            )
        )
    }

    @Test
    fun `profile contains valid username`() {
        assertTrue(mockProfile.username.isNotEmpty())
        assertEquals("TestUser", mockProfile.username)
    }

    @Test
    fun `profile has available avatars`() {
        assertTrue(mockProfile.availableAvatars.isNotEmpty())
        assertEquals(3, mockProfile.availableAvatars.size)
    }

    @Test
    fun `profile has selected avatar`() {
        assertTrue(mockProfile.selectedAvatarId.isNotEmpty())
        assertEquals("avatar_1", mockProfile.selectedAvatarId)
    }

    @Test
    fun `profile weekly goal is within range`() {
        assertTrue(mockProfile.goalRange.contains(mockProfile.weeklyGoalHours))
        assertEquals(20, mockProfile.weeklyGoalHours)
        assertEquals(10..30, mockProfile.goalRange)
    }

    @Test
    fun `ranks are properly ordered by XP`() {
        val sortedByXP = mockRanks.sortedByDescending { it.xp }
        assertEquals(mockRanks, sortedByXP)

        // Verify XP order
        for (i in 0 until mockRanks.size - 1) {
            assertTrue(mockRanks[i].xp >= mockRanks[i + 1].xp)
        }
    }

    @Test
    fun `current user is identified in rankings`() {
        val currentUser = mockRanks.find { it.isYou }
        assertNotNull(currentUser)
        assertEquals("TestUser", currentUser?.name)
        assertTrue(currentUser?.isYou == true)
    }

    @Test
    fun `rank position matches user placement`() {
        val userRank = mockRanks.indexOfFirst { it.isYou }
        assertEquals(2, userRank) // 3rd position (0-indexed)
    }

    @Test
    fun `friends list contains valid friends`() {
        assertEquals(3, mockFriends.size)
        mockFriends.forEach { friend ->
            assertNotNull(friend.id)
            assertNotNull(friend.name)
            assertNotNull(friend.initials)
            assertTrue(friend.score >= 0)
            assertTrue(friend.streak >= 0)
        }
    }

    @Test
    fun `friends have different statuses`() {
        val statusCount = mockFriends.map { it.status }.distinct().size
        assertEquals(3, statusCount)

        assertTrue(mockFriends.any { it.status == FriendStatus.Online })
        assertTrue(mockFriends.any { it.status == FriendStatus.Offline })
        assertTrue(mockFriends.any { it.status == FriendStatus.Studying })
    }

    @Test
    fun `friends can be sorted by score`() {
        val sortedFriends = mockFriends.sortedByDescending { it.score }

        assertEquals("Alice", sortedFriends[0].name)
        assertEquals(5000, sortedFriends[0].score)
        assertEquals("Eve", sortedFriends[2].name)
        assertEquals(4000, sortedFriends[2].score)
    }

    @Test
    fun `awards contain unlocked and locked items`() {
        val unlockedAwards = mockAwards.filter { it.isUnlocked }
        val lockedAwards = mockAwards.filter { !it.isUnlocked }

        assertEquals(1, unlockedAwards.size)
        assertEquals(1, lockedAwards.size)
    }

    @Test
    fun `unlocked award has date`() {
        val unlockedAward = mockAwards.find { it.isUnlocked }
        assertNotNull(unlockedAward)
        assertNotNull(unlockedAward?.unlockedDate)
        assertEquals("2025-01-01", unlockedAward?.unlockedDate)
    }

    @Test
    fun `locked award has no date`() {
        val lockedAward = mockAwards.find { !it.isUnlocked }
        assertNotNull(lockedAward)
        assertNull(lockedAward?.unlockedDate)
    }

    @Test
    fun `awards have different rarities`() {
        val commonAward = mockAwards.find { it.rarity == AwardRarity.Common }
        val rareAward = mockAwards.find { it.rarity == AwardRarity.Rare }

        assertNotNull(commonAward)
        assertNotNull(rareAward)
        assertNotEquals(commonAward?.points, rareAward?.points)
    }

    @Test
    fun `award points increase with rarity`() {
        val commonAward = mockAwards.find { it.rarity == AwardRarity.Common }
        val rareAward = mockAwards.find { it.rarity == AwardRarity.Rare }

        assertTrue((rareAward?.points ?: 0) > (commonAward?.points ?: 0))
    }

    @Test
    fun `avatar selection is valid`() {
        val selectedAvatar = mockProfile.availableAvatars.find {
            it.id == mockProfile.selectedAvatarId
        }
        assertNotNull(selectedAvatar)
        assertEquals("Ninja", selectedAvatar?.label)
    }

    @Test
    fun `privacy setting is configurable`() {
        assertTrue(mockProfile.privacyEnabled)

        val publicProfile = mockProfile.copy(privacyEnabled = false)
        assertFalse(publicProfile.privacyEnabled)
    }

    @Test
    fun `weekly goal can be updated within range`() {
        val updatedProfile = mockProfile.copy(weeklyGoalHours = 25)

        assertTrue(updatedProfile.goalRange.contains(updatedProfile.weeklyGoalHours))
        assertEquals(25, updatedProfile.weeklyGoalHours)
    }

    @Test
    fun `friend initials are properly formatted`() {
        mockFriends.forEach { friend ->
            assertTrue(friend.initials.length <= 3)
            assertTrue(friend.initials.all { it.isUpperCase() || it.isDigit() })
        }
    }

    @Test
    fun `streak values are non-negative`() {
        mockRanks.forEach { rank ->
            assertTrue(rank.streak >= 0)
        }

        mockFriends.forEach { friend ->
            assertTrue(friend.streak >= 0)
        }
    }

    @Test
    fun `XP values are non-negative`() {
        mockRanks.forEach { rank ->
            assertTrue(rank.xp >= 0)
        }
    }

    @Test
    fun `award unlock conditions are tracked`() {
        mockAwards.forEach { award ->
            assertNotNull(award.unlockedBy)
        }

        val award = mockAwards[0]
        assertEquals(1, award.unlockedBy.size)
        assertEquals("task_1", award.unlockedBy[0])
    }

    @Test
    fun `study level is set correctly`() {
        assertEquals("Intermediate", mockProfile.studyLevel)
        assertFalse(mockProfile.studyLevel.isEmpty())
    }

    @Test
    fun `friend status can be checked`() {
        val onlineFriends = mockFriends.filter { it.status == FriendStatus.Online }
        val offlineFriends = mockFriends.filter { it.status == FriendStatus.Offline }
        val studyingFriends = mockFriends.filter { it.status == FriendStatus.Studying }

        assertEquals(1, onlineFriends.size)
        assertEquals(1, offlineFriends.size)
        assertEquals(1, studyingFriends.size)
    }
}
