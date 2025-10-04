package com.mtlc.studyplan.social

import com.mtlc.studyplan.data.social.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Social data models
 */
class SocialModelsTest {

    @Test
    fun `RankEntry creation with all properties`() {
        val rankEntry = RankEntry(
            id = "user123",
            name = "John Doe",
            avatar = "avatar_1",
            xp = 5000,
            streak = 15,
            isYou = true
        )

        assertEquals("user123", rankEntry.id)
        assertEquals("John Doe", rankEntry.name)
        assertEquals("avatar_1", rankEntry.avatar)
        assertEquals(5000, rankEntry.xp)
        assertEquals(15, rankEntry.streak)
        assertTrue(rankEntry.isYou)
    }

    @Test
    fun `RankEntry creation with null avatar`() {
        val rankEntry = RankEntry(
            id = "user456",
            name = "Jane Smith",
            avatar = null,
            xp = 2000,
            streak = 7,
            isYou = false
        )

        assertNull(rankEntry.avatar)
        assertFalse(rankEntry.isYou)
    }

    @Test
    fun `Friend status enum values`() {
        val online = FriendStatus.Online
        val offline = FriendStatus.Offline
        val studying = FriendStatus.Studying

        assertNotNull(online)
        assertNotNull(offline)
        assertNotNull(studying)
    }

    @Test
    fun `Friend creation with all properties`() {
        val friend = Friend(
            id = "friend123",
            initials = "JD",
            name = "John Doe",
            status = FriendStatus.Studying,
            score = 1500,
            streak = 10
        )

        assertEquals("friend123", friend.id)
        assertEquals("JD", friend.initials)
        assertEquals("John Doe", friend.name)
        assertEquals(FriendStatus.Studying, friend.status)
        assertEquals(1500, friend.score)
        assertEquals(10, friend.streak)
    }

    @Test
    fun `Award rarity enum values`() {
        val common = AwardRarity.Common
        val rare = AwardRarity.Rare
        val epic = AwardRarity.Epic
        val legendary = AwardRarity.Legendary

        assertNotNull(common)
        assertNotNull(rare)
        assertNotNull(epic)
        assertNotNull(legendary)
    }

    @Test
    fun `Award creation with unlocked status`() {
        val award = Award(
            id = "award1",
            title = "First Steps",
            description = "Complete your first task",
            rarity = AwardRarity.Common,
            points = 100,
            iconType = AwardIconType.Target,
            tags = listOf("beginner", "milestone"),
            unlockedBy = listOf("task_completion"),
            isUnlocked = true,
            unlockedDate = "2025-01-01"
        )

        assertTrue(award.isUnlocked)
        assertEquals("2025-01-01", award.unlockedDate)
        assertEquals(2, award.tags.size)
        assertEquals("beginner", award.tags[0])
    }

    @Test
    fun `Award creation with locked status`() {
        val award = Award(
            id = "award2",
            title = "Master Achiever",
            description = "Complete 1000 tasks",
            rarity = AwardRarity.Legendary,
            points = 10000,
            iconType = AwardIconType.Crown,
            unlockedBy = listOf("task_1000"),
            isUnlocked = false
        )

        assertFalse(award.isUnlocked)
        assertNull(award.unlockedDate)
        assertTrue(award.tags.isEmpty())
    }

    @Test
    fun `AwardIconType enum values`() {
        val types = listOf(
            AwardIconType.Target,
            AwardIconType.Shield,
            AwardIconType.Crown,
            AwardIconType.Book,
            AwardIconType.Diamond,
            AwardIconType.Lightbulb
        )

        assertEquals(6, types.size)
        types.forEach { assertNotNull(it) }
    }

    @Test
    fun `AvatarOption creation`() {
        val avatar = AvatarOption(
            id = "avatar_1",
            label = "Ninja"
        )

        assertEquals("avatar_1", avatar.id)
        assertEquals("Ninja", avatar.label)
    }

    @Test
    fun `SocialProfile creation with custom avatar`() {
        val profile = SocialProfile(
            username = "StudyNinja",
            selectedAvatarId = "avatar_custom",
            availableAvatars = listOf(
                AvatarOption("avatar_1", "Ninja"),
                AvatarOption("avatar_2", "Wizard")
            ),
            studyLevel = "Intermediate",
            weeklyGoalHours = 20,
            goalRange = 10..30,
            privacyEnabled = true,
            customAvatarUri = "content://custom/avatar.png"
        )

        assertEquals("StudyNinja", profile.username)
        assertEquals("avatar_custom", profile.selectedAvatarId)
        assertEquals(2, profile.availableAvatars.size)
        assertEquals("Intermediate", profile.studyLevel)
        assertEquals(20, profile.weeklyGoalHours)
        assertEquals(10..30, profile.goalRange)
        assertTrue(profile.privacyEnabled)
        assertEquals("content://custom/avatar.png", profile.customAvatarUri)
    }

    @Test
    fun `SocialProfile creation without custom avatar`() {
        val profile = SocialProfile(
            username = "Learner",
            selectedAvatarId = "avatar_1",
            availableAvatars = emptyList(),
            studyLevel = "Beginner",
            weeklyGoalHours = 10,
            goalRange = 5..15,
            privacyEnabled = false,
            customAvatarUri = null
        )

        assertNull(profile.customAvatarUri)
        assertFalse(profile.privacyEnabled)
        assertTrue(profile.availableAvatars.isEmpty())
    }

    @Test
    fun `privacy banner constant exists`() {
        assertTrue(PRIVACY_BANNER.isNotEmpty())
        assertTrue(PRIVACY_BANNER.contains("Privacy"))
    }

    @Test
    fun `RankEntry equality comparison`() {
        val rank1 = RankEntry("1", "User", "av1", 100, 5, false)
        val rank2 = RankEntry("1", "User", "av1", 100, 5, false)
        val rank3 = RankEntry("2", "User", "av1", 100, 5, false)

        assertEquals(rank1, rank2)
        assertNotEquals(rank1, rank3)
    }

    @Test
    fun `Friend with different statuses`() {
        val onlineFriend = Friend("1", "AB", "Alice", FriendStatus.Online, 100, 1)
        val offlineFriend = Friend("2", "CD", "Charlie", FriendStatus.Offline, 200, 2)
        val studyingFriend = Friend("3", "EF", "Eve", FriendStatus.Studying, 300, 3)

        assertEquals(FriendStatus.Online, onlineFriend.status)
        assertEquals(FriendStatus.Offline, offlineFriend.status)
        assertEquals(FriendStatus.Studying, studyingFriend.status)
    }

    @Test
    fun `Award with multiple unlock conditions`() {
        val award = Award(
            id = "multi_award",
            title = "Multi Master",
            description = "Master multiple categories",
            rarity = AwardRarity.Epic,
            points = 5000,
            iconType = AwardIconType.Diamond,
            unlockedBy = listOf("grammar_100", "vocab_100", "reading_100"),
            isUnlocked = false
        )

        assertEquals(3, award.unlockedBy.size)
        assertTrue(award.unlockedBy.contains("grammar_100"))
        assertTrue(award.unlockedBy.contains("vocab_100"))
        assertTrue(award.unlockedBy.contains("reading_100"))
    }

    @Test
    fun `SocialProfile goal range validation`() {
        val profile = SocialProfile(
            username = "Test",
            selectedAvatarId = "av1",
            availableAvatars = emptyList(),
            studyLevel = "Advanced",
            weeklyGoalHours = 25,
            goalRange = 20..40,
            privacyEnabled = true
        )

        assertTrue(profile.goalRange.contains(profile.weeklyGoalHours))
        assertTrue(profile.goalRange.contains(20))
        assertTrue(profile.goalRange.contains(40))
        assertFalse(profile.goalRange.contains(19))
        assertFalse(profile.goalRange.contains(41))
    }
}
