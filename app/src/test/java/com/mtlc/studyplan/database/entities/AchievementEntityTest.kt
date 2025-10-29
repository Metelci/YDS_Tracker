package com.mtlc.studyplan.database.entities

import com.mtlc.studyplan.shared.AchievementCategory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AchievementEntity - Tracks unlockable achievements and badges
 * Focus: Achievement state, progress tracking, and unlock conditions
 */
class AchievementEntityTest {

    private fun createTestAchievement(
        id: String = "achievement-1",
        title: String = "Test Achievement",
        description: String = "Test Description",
        iconRes: String = "ic_achievement",
        category: AchievementCategory = AchievementCategory.TASKS,
        threshold: Int = 10,
        currentProgress: Int = 0,
        isUnlocked: Boolean = false,
        unlockedAt: Long? = null,
        pointsReward: Int = 100,
        isViewed: Boolean = false,
        difficulty: String = "Normal",
        rarity: String = "Common",
        badgeColor: String = "#1976D2",
        requirements: List<String> = emptyList(),
        hint: String? = null,
        isHidden: Boolean = false,
        prerequisiteAchievements: List<String> = emptyList(),
        seasonalEvent: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ) = AchievementEntity(
        id = id,
        title = title,
        description = description,
        iconRes = iconRes,
        category = category,
        threshold = threshold,
        currentProgress = currentProgress,
        isUnlocked = isUnlocked,
        unlockedAt = unlockedAt,
        pointsReward = pointsReward,
        isViewed = isViewed,
        difficulty = difficulty,
        rarity = rarity,
        badgeColor = badgeColor,
        requirements = requirements,
        hint = hint,
        isHidden = isHidden,
        prerequisiteAchievements = prerequisiteAchievements,
        seasonalEvent = seasonalEvent,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    @Test
    fun `AchievementEntity creates with required fields`() {
        val achievement = createTestAchievement()
        assertEquals("achievement-1", achievement.id)
        assertEquals("Test Achievement", achievement.title)
    }

    @Test
    fun `AchievementEntity tracks progress toward threshold`() {
        val achievement = createTestAchievement(threshold = 10, currentProgress = 5)
        assertEquals(10, achievement.threshold)
        assertEquals(5, achievement.currentProgress)
    }

    @Test
    fun `AchievementEntity can be unlocked`() {
        val locked = createTestAchievement(isUnlocked = false)
        val unlockedTime = System.currentTimeMillis()
        val unlocked = locked.copy(isUnlocked = true, unlockedAt = unlockedTime)

        assertFalse(locked.isUnlocked)
        assertTrue(unlocked.isUnlocked)
        assertEquals(unlockedTime, unlocked.unlockedAt)
    }

    @Test
    fun `AchievementEntity supports all achievement categories`() {
        AchievementCategory.values().forEach { category ->
            val achievement = createTestAchievement(category = category)
            assertEquals(category, achievement.category)
        }
    }

    @Test
    fun `AchievementEntity tracks difficulty levels`() {
        val difficulties = listOf("Easy", "Normal", "Hard", "Legendary")
        difficulties.forEach { difficulty ->
            val achievement = createTestAchievement(difficulty = difficulty)
            assertEquals(difficulty, achievement.difficulty)
        }
    }

    @Test
    fun `AchievementEntity tracks rarity levels`() {
        val rarities = listOf("Common", "Rare", "Epic", "Legendary")
        rarities.forEach { rarity ->
            val achievement = createTestAchievement(rarity = rarity)
            assertEquals(rarity, achievement.rarity)
        }
    }

    @Test
    fun `AchievementEntity can be hidden until close to completion`() {
        val hiddenAchievement = createTestAchievement(isHidden = true)
        val visibleAchievement = createTestAchievement(isHidden = false)

        assertTrue(hiddenAchievement.isHidden)
        assertFalse(visibleAchievement.isHidden)
    }

    @Test
    fun `AchievementEntity tracks prerequisite achievements`() {
        val prerequisites = listOf("achievement-0", "achievement-1")
        val achievement = createTestAchievement(prerequisiteAchievements = prerequisites)
        assertEquals(prerequisites, achievement.prerequisiteAchievements)
    }

    @Test
    fun `AchievementEntity tracks seasonal events`() {
        val seasonal = createTestAchievement(seasonalEvent = "Halloween2025")
        assertEquals("Halloween2025", seasonal.seasonalEvent)
    }

    @Test
    fun `AchievementEntity copy allows progress update`() {
        val original = createTestAchievement(currentProgress = 5, threshold = 10)
        val updated = original.copy(currentProgress = 8)

        assertEquals(5, original.currentProgress)
        assertEquals(8, updated.currentProgress)
    }

    @Test
    fun `AchievementEntity tracks points reward`() {
        val achievement = createTestAchievement(pointsReward = 250)
        assertEquals(250, achievement.pointsReward)
    }

    @Test
    fun `AchievementEntity can be marked as viewed`() {
        val newAchievement = createTestAchievement(isViewed = false)
        val viewedAchievement = newAchievement.copy(isViewed = true)

        assertFalse(newAchievement.isViewed)
        assertTrue(viewedAchievement.isViewed)
    }

    @Test
    fun `AchievementEntity stores badge color`() {
        val achievement = createTestAchievement(badgeColor = "#FF6B6B")
        assertEquals("#FF6B6B", achievement.badgeColor)
    }

    @Test
    fun `AchievementEntity can have hint text`() {
        val hintText = "Complete 5 tasks in one day"
        val achievement = createTestAchievement(hint = hintText)
        assertEquals(hintText, achievement.hint)
    }

    @Test
    fun `AchievementEntity equality test`() {
        val createdAt = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        val ach1 = createTestAchievement(
            id = "ach-1",
            createdAt = createdAt,
            updatedAt = updatedAt
        )
        val ach2 = createTestAchievement(
            id = "ach-1",
            createdAt = createdAt,
            updatedAt = updatedAt
        )
        assertEquals(ach1, ach2)
    }
}
