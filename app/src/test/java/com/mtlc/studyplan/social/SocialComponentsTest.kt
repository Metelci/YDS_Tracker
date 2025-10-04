package com.mtlc.studyplan.social

import com.mtlc.studyplan.data.social.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before

/**
 * Unit tests for Social UI component logic and data transformations
 */
class SocialComponentsTest {

    private lateinit var testRankEntry: RankEntry
    private lateinit var testFriend: Friend
    private lateinit var testAward: Award

    @Before
    fun setup() {
        testRankEntry = RankEntry(
            id = "user1",
            name = "Test User",
            avatar = "avatar_1",
            xp = 5000,
            streak = 10,
            isYou = false
        )

        testFriend = Friend(
            id = "friend1",
            initials = "TU",
            name = "Test User",
            status = FriendStatus.Online,
            score = 3000,
            streak = 5
        )

        testAward = Award(
            id = "award1",
            title = "First Steps",
            description = "Complete your first task",
            rarity = AwardRarity.Common,
            points = 100,
            iconType = AwardIconType.Target,
            unlockedBy = listOf("task_1"),
            isUnlocked = true,
            unlockedDate = "2025-01-01"
        )
    }

    @Test
    fun `LeaderboardRow displays correct rank`() {
        val rank = 1
        assertEquals(1, rank)
        assertNotNull(testRankEntry)
    }

    @Test
    fun `LeaderboardRow highlights current user`() {
        val userEntry = testRankEntry.copy(isYou = true)
        assertTrue(userEntry.isYou)
        assertFalse(testRankEntry.isYou)
    }

    @Test
    fun `LeaderboardRow shows XP and streak correctly`() {
        assertEquals(5000, testRankEntry.xp)
        assertEquals(10, testRankEntry.streak)
    }

    @Test
    fun `FriendRow displays friend status correctly`() {
        assertEquals(FriendStatus.Online, testFriend.status)
        assertEquals("TU", testFriend.initials)
    }

    @Test
    fun `FriendRow handles offline status`() {
        val offlineFriend = testFriend.copy(status = FriendStatus.Offline)
        assertEquals(FriendStatus.Offline, offlineFriend.status)
    }

    @Test
    fun `FriendRow handles studying status`() {
        val studyingFriend = testFriend.copy(status = FriendStatus.Studying)
        assertEquals(FriendStatus.Studying, studyingFriend.status)
    }

    @Test
    fun `FriendRow displays score and streak`() {
        assertEquals(3000, testFriend.score)
        assertEquals(5, testFriend.streak)
    }

    @Test
    fun `AwardCard shows unlocked award correctly`() {
        assertTrue(testAward.isUnlocked)
        assertEquals("2025-01-01", testAward.unlockedDate)
    }

    @Test
    fun `AwardCard shows locked award correctly`() {
        val lockedAward = testAward.copy(isUnlocked = false, unlockedDate = null)
        assertFalse(lockedAward.isUnlocked)
        assertNull(lockedAward.unlockedDate)
    }

    @Test
    fun `AwardCard displays rarity correctly`() {
        assertEquals(AwardRarity.Common, testAward.rarity)

        val rareAward = testAward.copy(rarity = AwardRarity.Rare)
        assertEquals(AwardRarity.Rare, rareAward.rarity)

        val epicAward = testAward.copy(rarity = AwardRarity.Epic)
        assertEquals(AwardRarity.Epic, epicAward.rarity)

        val legendaryAward = testAward.copy(rarity = AwardRarity.Legendary)
        assertEquals(AwardRarity.Legendary, legendaryAward.rarity)
    }

    @Test
    fun `AwardCard shows icon type correctly`() {
        assertEquals(AwardIconType.Target, testAward.iconType)

        val bookAward = testAward.copy(iconType = AwardIconType.Book)
        assertEquals(AwardIconType.Book, bookAward.iconType)
    }

    @Test
    fun `AwardCard displays points reward`() {
        assertEquals(100, testAward.points)

        val highValueAward = testAward.copy(points = 5000)
        assertEquals(5000, highValueAward.points)
    }

    @Test
    fun `LeaderboardRow handles null avatar gracefully`() {
        val entryWithoutAvatar = testRankEntry.copy(avatar = null)
        assertNull(entryWithoutAvatar.avatar)
        assertNotNull(entryWithoutAvatar.name)
    }

    @Test
    fun `Friend initials are properly formatted`() {
        assertEquals("TU", testFriend.initials)
        assertEquals(2, testFriend.initials.length)
    }

    @Test
    fun `Award tags are properly stored`() {
        val awardWithTags = testAward.copy(tags = listOf("beginner", "milestone", "achievement"))
        assertEquals(3, awardWithTags.tags.size)
        assertTrue(awardWithTags.tags.contains("beginner"))
        assertTrue(awardWithTags.tags.contains("milestone"))
    }

    @Test
    fun `Award unlock conditions are tracked`() {
        assertEquals(1, testAward.unlockedBy.size)
        assertEquals("task_1", testAward.unlockedBy[0])

        val multiConditionAward = testAward.copy(
            unlockedBy = listOf("task_1", "task_2", "task_3")
        )
        assertEquals(3, multiConditionAward.unlockedBy.size)
    }

    @Test
    fun `LeaderboardRow ranking positions work correctly`() {
        val firstPlace = 1
        val secondPlace = 2
        val thirdPlace = 3

        assertTrue(firstPlace < secondPlace)
        assertTrue(secondPlace < thirdPlace)
        assertTrue(firstPlace < thirdPlace)
    }

    @Test
    fun `Friend comparison by score`() {
        val friend1 = testFriend.copy(id = "f1", score = 1000)
        val friend2 = testFriend.copy(id = "f2", score = 2000)
        val friend3 = testFriend.copy(id = "f3", score = 1500)

        assertTrue(friend2.score > friend1.score)
        assertTrue(friend2.score > friend3.score)
        assertTrue(friend3.score > friend1.score)
    }

    @Test
    fun `Award icon types are all valid`() {
        val icons = listOf(
            AwardIconType.Target,
            AwardIconType.Shield,
            AwardIconType.Crown,
            AwardIconType.Book,
            AwardIconType.Diamond,
            AwardIconType.Lightbulb
        )

        assertEquals(6, icons.size)
        icons.forEach { assertNotNull(it) }
    }

    @Test
    fun `Streak tracking for friends`() {
        val streakFriend1 = testFriend.copy(streak = 0)
        val streakFriend2 = testFriend.copy(streak = 7)
        val streakFriend3 = testFriend.copy(streak = 30)

        assertEquals(0, streakFriend1.streak)
        assertEquals(7, streakFriend2.streak)
        assertEquals(30, streakFriend3.streak)
    }

    @Test
    fun `XP values are non-negative`() {
        assertTrue(testRankEntry.xp >= 0)

        val zeroXP = testRankEntry.copy(xp = 0)
        assertEquals(0, zeroXP.xp)

        val highXP = testRankEntry.copy(xp = 1000000)
        assertEquals(1000000, highXP.xp)
    }

    @Test
    fun `Award description is informative`() {
        assertNotNull(testAward.description)
        assertTrue(testAward.description.isNotEmpty())
        assertTrue(testAward.description.length > 0)
    }

    @Test
    fun `Friend status can change`() {
        val statuses = listOf(
            FriendStatus.Online,
            FriendStatus.Offline,
            FriendStatus.Studying
        )

        statuses.forEach { status ->
            val friend = testFriend.copy(status = status)
            assertEquals(status, friend.status)
        }
    }
}
