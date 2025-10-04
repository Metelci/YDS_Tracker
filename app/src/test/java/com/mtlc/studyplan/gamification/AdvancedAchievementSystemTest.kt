package com.mtlc.studyplan.gamification

import androidx.compose.ui.graphics.Color
import com.mtlc.studyplan.data.AchievementCategory
import com.mtlc.studyplan.data.AchievementTier
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Advanced Achievement System
 */
class AdvancedAchievementSystemTest {

    @Test
    fun `AdvancedAchievement creation with basic properties`() {
        val achievement = AdvancedAchievement(
            id = "ach_001",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Grammar Beginner",
            description = "Complete 10 grammar tasks",
            targetValue = 10,
            pointsReward = 100
        )

        assertEquals("ach_001", achievement.id)
        assertEquals(AchievementCategory.GRAMMAR_MASTER, achievement.category)
        assertEquals(AchievementTier.BRONZE, achievement.tier)
        assertEquals("Grammar Beginner", achievement.title)
        assertEquals(10, achievement.targetValue)
        assertEquals(100, achievement.pointsReward)
        assertFalse(achievement.isUnlocked)
        assertEquals(0, achievement.currentProgress)
    }

    @Test
    fun `AdvancedAchievement progress percentage calculation`() {
        val achievement = AdvancedAchievement(
            id = "ach_002",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.SILVER,
            title = "Speed Master",
            description = "Complete tasks quickly",
            targetValue = 100,
            pointsReward = 500,
            currentProgress = 50
        )

        assertEquals(0.5f, achievement.progressPercentage, 0.01f)
    }

    @Test
    fun `AdvancedAchievement progress percentage when exceeds target`() {
        val achievement = AdvancedAchievement(
            id = "ach_003",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.GOLD,
            title = "Consistent",
            description = "Maintain streak",
            targetValue = 50,
            pointsReward = 1000,
            currentProgress = 75
        )

        assertEquals(1.0f, achievement.progressPercentage, 0.01f)
    }

    @Test
    fun `AdvancedAchievement full title includes tier`() {
        val achievement = AdvancedAchievement(
            id = "ach_004",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.PLATINUM,
            title = "Pioneer",
            description = "Make progress",
            targetValue = 200,
            pointsReward = 2000
        )

        assertEquals("Platinum Pioneer", achievement.fullTitle)
    }

    @Test
    fun `Hidden achievement is not visible when locked`() {
        val achievement = AdvancedAchievement(
            id = "ach_005",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Secret Master",
            description = "???",
            targetValue = 1000,
            pointsReward = 5000,
            isHidden = true,
            hiddenHint = "Complete something extraordinary"
        )

        assertFalse(achievement.isVisible)
        assertTrue(achievement.isHidden)
        assertEquals("Complete something extraordinary", achievement.hiddenHint)
    }

    @Test
    fun `Hidden achievement becomes visible with progress`() {
        val achievement = AdvancedAchievement(
            id = "ach_006",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.GOLD,
            title = "Secret Speed",
            description = "???",
            targetValue = 500,
            pointsReward = 3000,
            isHidden = true,
            currentProgress = 10
        )

        assertTrue(achievement.isVisible)
    }

    @Test
    fun `Hidden achievement becomes visible when unlocked`() {
        val achievement = AdvancedAchievement(
            id = "ach_007",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.PLATINUM,
            title = "Hidden Champion",
            description = "Master of consistency",
            targetValue = 365,
            pointsReward = 10000,
            isHidden = true,
            isUnlocked = true,
            unlockedDate = 1704067200000L
        )

        assertTrue(achievement.isVisible)
        assertTrue(achievement.isUnlocked)
        assertNotNull(achievement.unlockedDate)
    }

    @Test
    fun `Achievement with prerequisites`() {
        val prerequisites = listOf("ach_001", "ach_002", "ach_003")
        val achievement = AdvancedAchievement(
            id = "ach_008",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.PLATINUM,
            title = "Ultimate Master",
            description = "Requires multiple achievements",
            targetValue = 1,
            pointsReward = 5000,
            prerequisites = prerequisites
        )

        assertEquals(3, achievement.prerequisites.size)
        assertTrue(achievement.prerequisites.contains("ach_001"))
        assertTrue(achievement.prerequisites.contains("ach_002"))
        assertTrue(achievement.prerequisites.contains("ach_003"))
    }

    @Test
    fun `Achievement rarity affects display`() {
        val achievement = AdvancedAchievement(
            id = "ach_009",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.PLATINUM,
            title = "Legendary Pioneer",
            description = "Achieve legendary status",
            targetValue = 5000,
            pointsReward = 50000,
            rarity = AchievementRarity.LEGENDARY
        )

        assertEquals(AchievementRarity.LEGENDARY, achievement.rarity)
    }

    @Test
    fun `AchievementRarity enum values and properties`() {
        assertEquals(AchievementRarity.COMMON.displayName, "Common")
        assertEquals(AchievementRarity.UNCOMMON.displayName, "Uncommon")
        assertEquals(AchievementRarity.RARE.displayName, "Rare")
        assertEquals(AchievementRarity.EPIC.displayName, "Epic")
        assertEquals(AchievementRarity.LEGENDARY.displayName, "Legendary")
        assertEquals(AchievementRarity.MYTHIC.displayName, "Mythic")
    }

    @Test
    fun `AchievementRarity point multipliers`() {
        assertEquals(1.0f, AchievementRarity.COMMON.pointsMultiplier, 0.01f)
        assertEquals(1.5f, AchievementRarity.UNCOMMON.pointsMultiplier, 0.01f)
        assertEquals(2.0f, AchievementRarity.RARE.pointsMultiplier, 0.01f)
        assertEquals(3.0f, AchievementRarity.EPIC.pointsMultiplier, 0.01f)
        assertEquals(5.0f, AchievementRarity.LEGENDARY.pointsMultiplier, 0.01f)
        assertEquals(10.0f, AchievementRarity.MYTHIC.pointsMultiplier, 0.01f)
    }

    @Test
    fun `AchievementRarity glow effects`() {
        assertFalse(AchievementRarity.COMMON.glowEffect)
        assertFalse(AchievementRarity.UNCOMMON.glowEffect)
        assertTrue(AchievementRarity.RARE.glowEffect)
        assertTrue(AchievementRarity.EPIC.glowEffect)
        assertTrue(AchievementRarity.LEGENDARY.glowEffect)
        assertTrue(AchievementRarity.MYTHIC.glowEffect)
    }

    @Test
    fun `SpecialReward creation with cosmetic unlock`() {
        val reward = SpecialReward(
            type = SpecialReward.SpecialRewardType.COSMETIC_UNLOCK,
            value = "theme_dark_premium",
            description = "Unlocks premium dark theme"
        )

        assertEquals(SpecialReward.SpecialRewardType.COSMETIC_UNLOCK, reward.type)
        assertEquals("theme_dark_premium", reward.value)
        assertEquals("Unlocks premium dark theme", reward.description)
    }

    @Test
    fun `SpecialReward with title unlock`() {
        val reward = SpecialReward(
            type = SpecialReward.SpecialRewardType.TITLE_UNLOCK,
            value = "Master Scholar",
            description = "Exclusive title for top achievers"
        )

        assertEquals(SpecialReward.SpecialRewardType.TITLE_UNLOCK, reward.type)
    }

    @Test
    fun `Achievement with special reward`() {
        val specialReward = SpecialReward(
            type = SpecialReward.SpecialRewardType.BONUS_MULTIPLIER,
            value = "2.0",
            description = "2x point multiplier for 24 hours"
        )

        val achievement = AdvancedAchievement(
            id = "ach_special",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.PLATINUM,
            title = "Speed God",
            description = "Ultimate speed achievement",
            targetValue = 10000,
            pointsReward = 100000,
            specialReward = specialReward
        )

        assertNotNull(achievement.specialReward)
        assertEquals("2.0", achievement.specialReward?.value)
    }

    @Test
    fun `EstimatedTime READY_TO_UNLOCK type`() {
        val time = EstimatedTime.READY_TO_UNLOCK
        assertNotNull(time)
    }

    @Test
    fun `EstimatedTime DAYS calculation`() {
        val time = EstimatedTime.estimateByDailyRate(remaining = 14, dailyRate = 2)
        assertTrue(time is EstimatedTime.DAYS)
        assertEquals(7, (time as EstimatedTime.DAYS).days)
    }

    @Test
    fun `EstimatedTime WEEKS calculation`() {
        val time = EstimatedTime.estimateByDailyRate(remaining = 30, dailyRate = 2)
        assertTrue(time is EstimatedTime.WEEKS)
        assertTrue((time as EstimatedTime.WEEKS).weeks > 0)
    }

    @Test
    fun `EstimatedTime MONTHS calculation`() {
        val time = EstimatedTime.estimateByDailyRate(remaining = 300, dailyRate = 2)
        assertTrue(time is EstimatedTime.MONTHS)
        assertTrue((time as EstimatedTime.MONTHS).months > 0)
    }

    @Test
    fun `EstimatedTime by weekly rate calculation`() {
        val time = EstimatedTime.estimateByWeeklyRate(remaining = 50, weeklyRate = 10)
        assertNotNull(time)
    }

    @Test
    fun `Achievement progress percentage is zero when target is zero`() {
        val achievement = AdvancedAchievement(
            id = "ach_zero",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Test",
            description = "Test",
            targetValue = 0,
            pointsReward = 0,
            currentProgress = 5
        )

        assertEquals(0f, achievement.progressPercentage, 0.01f)
    }

    @Test
    fun `Unlocked achievement has no estimated time`() {
        val achievement = AdvancedAchievement(
            id = "ach_unlocked",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.GOLD,
            title = "Champion",
            description = "Unlocked",
            targetValue = 100,
            pointsReward = 1000,
            isUnlocked = true,
            currentProgress = 100
        )

        assertNull(achievement.estimatedTimeToUnlock)
    }

    @Test
    fun `SpecialRewardType enum has all values`() {
        val types = SpecialReward.SpecialRewardType.values()

        assertTrue(types.contains(SpecialReward.SpecialRewardType.COSMETIC_UNLOCK))
        assertTrue(types.contains(SpecialReward.SpecialRewardType.TITLE_UNLOCK))
        assertTrue(types.contains(SpecialReward.SpecialRewardType.FEATURE_UNLOCK))
        assertTrue(types.contains(SpecialReward.SpecialRewardType.BONUS_MULTIPLIER))
        assertTrue(types.contains(SpecialReward.SpecialRewardType.EXCLUSIVE_CELEBRATION))
    }

    @Test
    fun `Achievement equality check`() {
        val ach1 = AdvancedAchievement(
            id = "same",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Test",
            description = "Test",
            targetValue = 10,
            pointsReward = 100
        )

        val ach2 = AdvancedAchievement(
            id = "same",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Test",
            description = "Test",
            targetValue = 10,
            pointsReward = 100
        )

        assertEquals(ach1, ach2)
    }
}
