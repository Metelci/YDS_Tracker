@file:Suppress("LongMethod", "LongParameterList")
package com.mtlc.studyplan.gamification

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.mtlc.studyplan.data.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.serializer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Point Economy System - Core of Gamification 2.0
 */

@Serializable
data class PointWallet(
    val totalLifetimePoints: Long = 0L,
    val currentSpendablePoints: Long = 0L,
    val pointsSpentTotal: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class PointTransaction(
    val id: String = generateTransactionId(),
    val type: PointTransactionType,
    val amount: Long,
    val category: TaskCategory? = null,
    val multiplier: Float = 1f,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        private fun generateTransactionId(): String =
            "pt_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

enum class PointTransactionType {
    TASK_COMPLETION,
    STREAK_BONUS,
    DAILY_GOAL_BONUS,
    WEEKLY_GOAL_BONUS,
    MONTHLY_GOAL_BONUS,
    ACHIEVEMENT_UNLOCK,
    CHALLENGE_COMPLETION,
    COMEBACK_BONUS,
    PURCHASE_THEME,
    PURCHASE_BADGE,
    PURCHASE_CELEBRATION,
    REFUND
}

/**
 * Enhanced Category Multipliers
 */
enum class CategoryMultiplier(
    val category: TaskCategory,
    val baseMultiplier: Float,
    val streakBonusMultiplier: Float,
    val displayName: String,
    val description: String
) {
    GRAMMAR_MULTIPLIER(
        TaskCategory.GRAMMAR,
        1.2f,
        0.1f,
        "Grammar Expert",
        "Extra points for mastering language fundamentals"
    ),
    READING_MULTIPLIER(
        TaskCategory.READING,
        1.5f,
        0.15f,
        "Reading Specialist",
        "Higher rewards for comprehension skills"
    ),
    LISTENING_MULTIPLIER(
        TaskCategory.LISTENING,
        1.3f,
        0.12f,
        "Audio Master",
        "Enhanced points for listening practice"
    ),
    VOCABULARY_MULTIPLIER(
        TaskCategory.VOCABULARY,
        1.0f,
        0.08f,
        "Word Builder",
        "Steady rewards for vocabulary expansion"
    ),
    OTHER_MULTIPLIER(
        TaskCategory.OTHER,
        0.8f,
        0.05f,
        "General Study",
        "Base rewards for miscellaneous tasks"
    );

    companion object {
        fun getMultiplierForCategory(category: TaskCategory): CategoryMultiplier {
            return values().find { it.category == category } ?: OTHER_MULTIPLIER
        }
    }
}

/**
 * Streak Multiplier System
 */
enum class StreakMultiplierTier(
    val minDays: Int,
    val multiplier: Float,
    val bonusPercentage: Int,
    val title: String,
    val description: String,
    val color: Color,
    val icon: String
) {
    GETTING_STARTED(0, 1.0f, 0, "Getting Started", "Building your foundation", Color(0xFF607D8B), "🌱"),
    BUILDING_MOMENTUM(7, 2.0f, 100, "Building Momentum", "7-day streak bonus", Color(0xFF4CAF50), "🚀"),
    POWER_STREAK(14, 3.0f, 200, "Power Streak", "Fortnight of dedication", Color(0xFF2196F3), "⚡"),
    MASTER_STREAK(30, 5.0f, 400, "Master Streak", "Month-long commitment", Color(0xFF9C27B0), "👑"),
    LEGENDARY_STREAK(50, 8.0f, 700, "Legendary Streak", "Exceptional persistence", Color(0xFFFF9800), "🏆"),
    GODLIKE_STREAK(100, 12.0f, 1100, "Godlike Streak", "Transcendent dedication", Color(0xFFE91E63), "🔥");

    companion object {
        fun getMultiplierForStreak(streakDays: Int): StreakMultiplierTier {
            return values()
                .filter { it.minDays <= streakDays }
                .maxByOrNull { it.minDays } ?: GETTING_STARTED
        }
    }
}

/**
 * Cosmetic Rewards System
 */
@Serializable
data class CosmeticReward(
    val id: String,
    val type: CosmeticType,
    val name: String,
    val description: String,
    val cost: Long,
    val rarity: CosmeticRarity,
    val unlockRequirement: String? = null,
    val isOwned: Boolean = false,
    val isEquipped: Boolean = false,
    val previewData: String = "", // Color hex, asset path, etc.
    val unlockDate: Long? = null
)

enum class CosmeticType(val displayName: String) {
    THEME("App Theme"),
    CELEBRATION("Celebration Style"),
    BADGE("Profile Badge"),
    STREAK_EFFECT("Streak Effect"),
    PROGRESS_BAR("Progress Bar Style"),
    PARTICLE_EFFECT("Particle Effect")
}

enum class CosmeticRarity(
    val displayName: String,
    val color: Color,
    val baseCostMultiplier: Float
) {
    COMMON("Common", Color(0xFF9E9E9E), 1.0f),
    UNCOMMON("Uncommon", Color(0xFF4CAF50), 2.0f),
    RARE("Rare", Color(0xFF2196F3), 4.0f),
    EPIC("Epic", Color(0xFF9C27B0), 8.0f),
    LEGENDARY("Legendary", Color(0xFFFF9800), 16.0f),
    MYTHIC("Mythic", Color(0xFFE91E63), 32.0f)
}

/**
 * Point Economy Manager
 */
class PointEconomyManager(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) {

    private object Keys {
        val POINT_WALLET = androidx.datastore.preferences.core.stringPreferencesKey("point_wallet")
        val POINT_TRANSACTIONS = androidx.datastore.preferences.core.stringSetPreferencesKey("point_transactions")
        val OWNED_COSMETICS = androidx.datastore.preferences.core.stringSetPreferencesKey("owned_cosmetics")
        val EQUIPPED_COSMETICS = androidx.datastore.preferences.core.stringPreferencesKey("equipped_cosmetics")
    }

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    val pointWalletFlow: Flow<PointWallet> = dataStore.data.map { preferences ->
        preferences[Keys.POINT_WALLET]?.let { walletJson ->
            try {
                json.decodeFromString<PointWallet>(walletJson)
            } catch (e: Exception) {
                PointWallet()
            }
        } ?: PointWallet()
    }

    val transactionHistoryFlow: Flow<List<PointTransaction>> = dataStore.data.map { preferences ->
        preferences[Keys.POINT_TRANSACTIONS]?.mapNotNull { transactionJson ->
            try {
                json.decodeFromString<PointTransaction>(transactionJson)
            } catch (e: Exception) {
                null
            }
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    /**
     * Calculate points for task completion with all multipliers
     */
    suspend fun calculateTaskPoints(
        taskCategory: TaskCategory,
        basePoints: Int,
        streakDays: Int,
        isCorrect: Boolean = true,
        timeBonus: Float = 1.0f
    ): PointCalculationResult {

        val categoryMultiplier = CategoryMultiplier.getMultiplierForCategory(taskCategory)
        val streakMultiplier = StreakMultiplierTier.getMultiplierForStreak(streakDays)

        // Base calculation
        val categoryPoints = (basePoints * categoryMultiplier.baseMultiplier).toLong()
        val streakPoints = (categoryPoints * streakMultiplier.multiplier).toLong()
        val timeBonusPoints = (streakPoints * timeBonus).toLong()

        // Accuracy bonus/penalty
        val accuracyMultiplier = if (isCorrect) 1.0f else 0.5f
        val finalPoints = (timeBonusPoints * accuracyMultiplier).toLong()

        // Streak bonus calculation
        val streakBonusPoints = if (streakDays >= 7) {
            (finalPoints * categoryMultiplier.streakBonusMultiplier * (streakDays / 7f)).toLong()
        } else 0L

        val totalPoints = finalPoints + streakBonusPoints

        return PointCalculationResult(
            basePoints = basePoints.toLong(),
            categoryMultiplier = categoryMultiplier.baseMultiplier,
            streakMultiplier = streakMultiplier.multiplier,
            timeBonus = timeBonus,
            accuracyMultiplier = accuracyMultiplier,
            bonusPoints = streakBonusPoints,
            finalPoints = totalPoints,
            breakdown = PointBreakdown(
                base = basePoints.toLong(),
                category = categoryPoints - basePoints,
                streak = streakPoints - categoryPoints,
                time = timeBonusPoints - streakPoints,
                accuracy = (timeBonusPoints * accuracyMultiplier).toLong() - timeBonusPoints,
                bonus = streakBonusPoints
            )
        )
    }

    /**
     * Award points and create transaction
     */
    suspend fun awardPoints(
        type: PointTransactionType,
        amount: Long,
        description: String,
        category: TaskCategory? = null,
        multiplier: Float = 1f,
        metadata: Map<String, String> = emptyMap()
    ) {
        val transaction = PointTransaction(
            type = type,
            amount = amount,
            category = category,
            multiplier = multiplier,
            description = description,
            metadata = metadata
        )

        dataStore.edit { preferences ->
            // Update wallet
            val currentWallet = preferences[Keys.POINT_WALLET]?.let {
                json.decodeFromString<PointWallet>(it)
            } ?: PointWallet()

            val updatedWallet = currentWallet.copy(
                totalLifetimePoints = currentWallet.totalLifetimePoints + amount,
                currentSpendablePoints = currentWallet.currentSpendablePoints + amount,
                lastUpdated = System.currentTimeMillis()
            )

            preferences[Keys.POINT_WALLET] = json.encodeToString(PointWallet.serializer(), updatedWallet)

            // Add transaction
            val currentTransactions = preferences[Keys.POINT_TRANSACTIONS] ?: emptySet()
            val updatedTransactions = currentTransactions + json.encodeToString(PointTransaction.serializer(), transaction)
            preferences[Keys.POINT_TRANSACTIONS] = updatedTransactions.toList().takeLast(1000).toSet()
        }

    }

    /**
     * Spend points on cosmetic rewards
     */
    suspend fun purchaseCosmetic(cosmetic: CosmeticReward): PurchaseResult {
        val currentWallet = pointWalletFlow.first()

        if (currentWallet.currentSpendablePoints < cosmetic.cost) {
            return PurchaseResult.InsufficientFunds(
                required = cosmetic.cost,
                available = currentWallet.currentSpendablePoints
            )
        }

        // Create purchase transaction
        val purchaseTransaction = PointTransaction(
            type = when (cosmetic.type) {
                CosmeticType.THEME -> PointTransactionType.PURCHASE_THEME
                CosmeticType.BADGE -> PointTransactionType.PURCHASE_BADGE
                CosmeticType.CELEBRATION -> PointTransactionType.PURCHASE_CELEBRATION
                else -> PointTransactionType.PURCHASE_THEME
            },
            amount = -cosmetic.cost,
            description = "Purchased ${cosmetic.name}",
            metadata = mapOf(
                "cosmetic_id" to cosmetic.id,
                "cosmetic_type" to cosmetic.type.name,
                "rarity" to cosmetic.rarity.name
            )
        )

        dataStore.edit { preferences ->
            // Update wallet
            val updatedWallet = currentWallet.copy(
                currentSpendablePoints = currentWallet.currentSpendablePoints - cosmetic.cost,
                pointsSpentTotal = currentWallet.pointsSpentTotal + cosmetic.cost,
                lastUpdated = System.currentTimeMillis()
            )
            preferences[Keys.POINT_WALLET] = json.encodeToString(PointWallet.serializer(), updatedWallet)

            // Add transaction
            val currentTransactions = preferences[Keys.POINT_TRANSACTIONS] ?: emptySet()
            preferences[Keys.POINT_TRANSACTIONS] = currentTransactions + json.encodeToString(PointTransaction.serializer(), purchaseTransaction)

            // Add owned cosmetic
            val currentCosmetics = preferences[Keys.OWNED_COSMETICS] ?: emptySet()
            val ownedCosmetic = cosmetic.copy(
                isOwned = true,
                unlockDate = System.currentTimeMillis()
            )
            preferences[Keys.OWNED_COSMETICS] = currentCosmetics + json.encodeToString(CosmeticReward.serializer(), ownedCosmetic)
        }

        return PurchaseResult.Success(cosmetic)
    }

    /**
     * Get available cosmetic rewards
     */
    fun getAvailableCosmetics(): List<CosmeticReward> {
        return listOf(
            // Themes
            CosmeticReward(
                id = "theme_ocean",
                type = CosmeticType.THEME,
                name = "Ocean Depths",
                description = "Deep blue theme with wave animations",
                cost = 500,
                rarity = CosmeticRarity.COMMON,
                previewData = "#0D47A1"
            ),
            CosmeticReward(
                id = "theme_sunset",
                type = CosmeticType.THEME,
                name = "Golden Sunset",
                description = "Warm orange gradient theme",
                cost = 750,
                rarity = CosmeticRarity.UNCOMMON,
                previewData = "#FF6F00"
            ),
            CosmeticReward(
                id = "theme_forest",
                type = CosmeticType.THEME,
                name = "Enchanted Forest",
                description = "Nature-inspired green theme",
                cost = 1000,
                rarity = CosmeticRarity.RARE,
                previewData = "#2E7D32"
            ),

            // Celebrations
            CosmeticReward(
                id = "celebration_fireworks",
                type = CosmeticType.CELEBRATION,
                name = "Fireworks Display",
                description = "Explosive celebration with colorful bursts",
                cost = 1200,
                rarity = CosmeticRarity.RARE,
                previewData = "fireworks"
            ),
            CosmeticReward(
                id = "celebration_rainbow",
                type = CosmeticType.CELEBRATION,
                name = "Rainbow Cascade",
                description = "Magical rainbow particle effects",
                cost = 2000,
                rarity = CosmeticRarity.EPIC,
                previewData = "rainbow"
            ),

            // Badges
            CosmeticReward(
                id = "badge_scholar",
                type = CosmeticType.BADGE,
                name = "Scholar's Crest",
                description = "For dedicated learners",
                cost = 800,
                rarity = CosmeticRarity.UNCOMMON,
                previewData = "🎓"
            ),
            CosmeticReward(
                id = "badge_lightning",
                type = CosmeticType.BADGE,
                name = "Lightning Strike",
                description = "For speed masters",
                cost = 1500,
                rarity = CosmeticRarity.RARE,
                previewData = "⚡"
            ),

            // Legendary items
            CosmeticReward(
                id = "theme_aurora",
                type = CosmeticType.THEME,
                name = "Aurora Borealis",
                description = "Mystical northern lights theme with animated gradients",
                cost = 5000,
                rarity = CosmeticRarity.LEGENDARY,
                unlockRequirement = "Achieve 100-day streak",
                previewData = "#4A148C"
            ),
            CosmeticReward(
                id = "celebration_phoenix",
                type = CosmeticType.CELEBRATION,
                name = "Phoenix Rising",
                description = "Legendary fire bird celebration",
                cost = 7500,
                rarity = CosmeticRarity.MYTHIC,
                unlockRequirement = "Complete all achievements",
                previewData = "phoenix"
            )
        )
    }
}

/**
 * Supporting Data Classes
 */
data class PointCalculationResult(
    val basePoints: Long,
    val categoryMultiplier: Float,
    val streakMultiplier: Float,
    val timeBonus: Float,
    val accuracyMultiplier: Float,
    val bonusPoints: Long,
    val finalPoints: Long,
    val breakdown: PointBreakdown
)

data class PointBreakdown(
    val base: Long,
    val category: Long,
    val streak: Long,
    val time: Long,
    val accuracy: Long,
    val bonus: Long
)

sealed class PurchaseResult {
    data class Success(val cosmetic: CosmeticReward) : PurchaseResult()
    data class InsufficientFunds(val required: Long, val available: Long) : PurchaseResult()
    data class AlreadyOwned(val cosmetic: CosmeticReward) : PurchaseResult()
    data class RequirementNotMet(val requirement: String) : PurchaseResult()
}

