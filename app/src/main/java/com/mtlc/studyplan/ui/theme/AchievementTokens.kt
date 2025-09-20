package com.mtlc.studyplan.ui.theme

import androidx.compose.ui.graphics.Color
import com.mtlc.studyplan.data.AchievementCategory
import com.mtlc.studyplan.data.AchievementTier

/**
 * Central palette helpers for achievement-related color usage.
 */
object AchievementPalette {

    private val categoryColors = mapOf(
        AchievementCategory.GRAMMAR_MASTER to DesignTokens.Primary,
        AchievementCategory.SPEED_DEMON to DesignTokens.Warning,
        AchievementCategory.CONSISTENCY_CHAMPION to DesignTokens.Success,
        AchievementCategory.PROGRESS_PIONEER to DesignTokens.Tertiary
    )

    private val tierColors = mapOf(
        AchievementTier.BRONZE to DesignTokens.AchievementBronze,
        AchievementTier.SILVER to DesignTokens.AchievementSilver,
        AchievementTier.GOLD to DesignTokens.AchievementGold,
        AchievementTier.PLATINUM to DesignTokens.AchievementPlatinum
    )

    fun categoryColor(category: AchievementCategory): Color =
        categoryColors[category] ?: DesignTokens.Primary

    fun tierColor(tier: AchievementTier): Color =
        tierColors[tier] ?: DesignTokens.AchievementBronze
}

val AchievementCategory.primaryColor: Color
    get() = AchievementPalette.categoryColor(this)

val AchievementTier.primaryColor: Color
    get() = AchievementPalette.tierColor(this)
