package com.mtlc.studyplan.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens tuned to exact HEX values from DESIGN_SYSTEM.md
 */
object DesignTokens {

    // Primary Brand Colors
    val Primary = Color(0xFF1976D2)
    val PrimaryContainer = Color(0xFFE3F2FD)
    val PrimaryForeground = Color(0xFFFFFFFF)
    val PrimaryContainerForeground = Color(0xFF0D47A1)

    // Secondary Colors
    val Secondary = Color(0xFF388E3C)
    val SecondaryContainer = Color(0xFFE8F5E8)
    val SecondaryForeground = Color(0xFFFFFFFF)
    val SecondaryContainerForeground = Color(0xFF1B5E20)

    // Tertiary Colors
    val Tertiary = Color(0xFFE65100)
    val TertiaryContainer = Color(0xFFFFF3E0)
    val TertiaryContainerForeground = Color(0xFFBF360C)

    // Status Colors
    val Success = Color(0xFF4CAF50)
    val SuccessContainer = Color(0xFFE8F5E8)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Destructive = Error
    val DestructiveForeground = Color(0xFFFFFFFF)

    // Surface & Content Colors
    val Background = Color(0xFFFFFBFF)
    val Foreground = Color(0xFF1C1B1F)
    val Card = Color(0xFFFFFFFF)
    val CardForeground = Color(0xFF1C1B1F)
    val Popover = Color(0xFFFFFFFF)
    val PopoverForeground = Color(0xFF1C1B1F)
    val Surface = Color(0xFFFFFBFF)
    val SurfaceVariant = Color(0xFFE7E0EC)
    val SurfaceContainer = Color(0xFFF7F2FA)
    val SurfaceContainerHigh = Color(0xFFF1ECF4)

    // Borders & Outlines
    val Border = Color(0xFFE0E0E0)
    val Input = Color(0xFFE0E0E0)
    val Ring = Primary

    // Text & Accents
    val Muted = Color(0xFFF5F5F5)
    val MutedForeground = Color(0xFF757575)
    val Accent = Muted
    val AccentForeground = Foreground

    // Gamification Colors (mapped to containers)
    val StreakFire = TertiaryContainer
    val PointsGreen = SecondaryContainer
    val TasksDone = TertiaryContainer
    val ExamBlue = PrimaryContainer

    // Task category colors (mapped)
    val TaskGrammar = PrimaryContainer
    val TaskReading = SecondaryContainer
    val TaskVocabulary = SecondaryContainer

    // Achievement Colors (compatibility)
    val AchievementBronze = Color(0xFFCD7F32)
    val AchievementSilver = Color(0xFFC0C0C0)
    val AchievementGold = Color(0xFFFFD700)
    val AchievementPlatinum = Color(0xFFE5E4E2)

    // Segmented control specific tokens (rail/pill/border)
    val SegmentedRail = Color(0xFFF2F6FB)
    val SegmentedBorder = Color(0xFFE2E8F0)
    val SegmentedPill = Color(0xFFFFFFFF)

    // Bottom navigation highlight tokens
    val BottomNavItemSelected = PrimaryContainer // tile background
    val BottomNavIconHighlight = Color(0xFFB3E5FC) // circle behind selected icon

    // Pastel palette for cards (light + dark aware usage)
    // These are low-chroma, high-luma backgrounds to keep contrast with text tokens.
    val PastelLightGray = Color(0xFFF2F2F2)
    val PastelLavender = Color(0xFFEDE7F6) // light lavender
    val PastelPrussia = Color(0xFFE3F2FD) // light prussian blue tint (soft blue)
    val PastelMint = Color(0xFFE8F5E9) // light mint
    val PastelYellow = Color(0xFFFFF9C4) // light yellow
    val PastelRed = Color(0xFFFFEBEE) // light red/rose
    val PastelPeach = Color(0xFFFFE5D4) // soft peach

    // For dark theme, we will slightly deepen via alpha overlays in usage sites.

    // Dark mode variants (approximate mapping)
    object Dark {
        val Background = Color(0xFF121212)
        val Foreground = Color(0xFFEDEDED)
        val Card = Color(0xFF1E1E1E)
        val CardForeground = Color(0xFFEDEDED)
        val Primary = Color(0xFF90CAF9)
        val PrimaryForeground = Color(0xFF0D47A1)
        val Secondary = Color(0xFFA5D6A7)
        val SecondaryForeground = Color(0xFF1B5E20)
        val Muted = Color(0xFF2A2A2A)
        val MutedForeground = Color(0xFF9E9E9E)
        val Border = Color(0xFF2A2A2A)
        val SurfaceVariant = Color(0xFF2C2C2C)
    }
}

// Extension (kept for compatibility)
val Color.Companion.StudyPlan: DesignTokens get() = DesignTokens
