package com.mtlc.studyplan.data.social

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Data models used by the Social Hub feature. They intentionally avoid any
 * direct UI dependencies so repositories can be re-used by previews/tests.
 */

data class RankEntry(
    val id: String,
    val name: String,
    val avatar: String?,
    val xp: Int,
    val streak: Int,
    val isYou: Boolean = false
)

data class Group(
    val id: String,
    val name: String,
    val members: Int,
    val tags: List<String>,
    val activity: String,
    val description: String,
    val joined: Boolean = false
)

enum class FriendStatus { Online, Offline, Studying }

data class Friend(
    val id: String,
    val initials: String,
    val name: String,
    val status: FriendStatus,
    val score: Int,
    val streak: Int
)

enum class AwardRarity { Common, Rare, Epic, Legendary }

data class Award(
    val id: String,
    val title: String,
    val description: String,
    val rarity: AwardRarity,
    val points: Int,
    val iconType: AwardIconType,
    val tags: List<String> = emptyList(),
    val unlockedBy: List<String>,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null
)

enum class AwardIconType {
    Target, Shield, Crown, Book, Diamond, Lightbulb
}

data class AvatarOption(
    val id: String,
    val label: String
)

data class SocialProfile(
    val username: String,
    val selectedAvatarId: String,
    val availableAvatars: List<AvatarOption>,
    val studyLevel: String,
    val weeklyGoalHours: Int,
    val goalRange: IntRange,
    val privacyEnabled: Boolean,
    val customAvatarUri: String? = null
)

/**
 * Banner copy used across the hub. Stored once for previews/tests.
 */
const val PRIVACY_BANNER = "Privacy First: Your real identity is never shared. Only your chosen username and avatar are visible to others."
