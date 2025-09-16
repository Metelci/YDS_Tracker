package com.mtlc.studyplan.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * HSL-based design tokens matching the target design
 * Following the Lovable design system specifications
 */

// HSL Helper Function
fun hsl(h: Int, s: Int, l: Int): Color =
    Color.hsl(h.toFloat(), s / 100f, l / 100f)

object DesignTokens {

    // Primary System Colors (Light Blue theme from screenshot)
    val Primary = hsl(199, 92, 73)  // Light blue for main CTA
    val PrimaryContainer = hsl(199, 70, 85)  // Lighter blue background
    val PrimaryContainerForeground = hsl(199, 100, 20)
    val PrimaryForeground = hsl(0, 0, 100)  // White text on primary

    // Secondary Colors
    val Secondary = hsl(122, 39, 69)  // Light green
    val SecondaryContainer = hsl(122, 39, 82)  // Light green container
    val SecondaryContainerForeground = hsl(122, 100, 15)
    val SecondaryForeground = hsl(0, 0, 100)

    // Tertiary Colors (Coral/Orange for accent)
    val Tertiary = hsl(14, 100, 78)  // Soft coral
    val TertiaryContainer = hsl(14, 100, 88)
    val TertiaryContainerForeground = hsl(14, 100, 25)

    // Surface System
    val Background = hsl(0, 0, 98)  // Very light gray
    val Foreground = hsl(0, 0, 13)  // Very dark gray for text
    val Card = hsl(0, 0, 100)  // Pure white for cards
    val CardForeground = hsl(0, 0, 13)
    val Surface = hsl(0, 0, 100)
    val SurfaceVariant = hsl(0, 0, 96)
    val SurfaceContainer = hsl(0, 0, 94)
    val SurfaceContainerHigh = hsl(0, 0, 92)

    // Border & Input
    val Border = hsl(0, 0, 89)
    val Input = hsl(0, 0, 89)
    val Ring = hsl(199, 92, 73)  // Same as primary

    // Text Colors
    val Muted = hsl(0, 0, 96)
    val MutedForeground = hsl(0, 0, 46)
    val Accent = hsl(0, 0, 96)
    val AccentForeground = hsl(0, 0, 13)

    // Semantic Colors
    val Success = hsl(122, 39, 49)  // Green for success states
    val SuccessContainer = hsl(122, 39, 82)
    val Warning = hsl(45, 100, 51)  // Orange for warnings
    val Destructive = hsl(0, 65, 51)  // Red for destructive actions
    val DestructiveForeground = hsl(0, 0, 100)

    // Gamification Colors (matching screenshot elements)
    val StreakFire = hsl(14, 70, 85)  // Softer coral for streak card
    val PointsGreen = hsl(122, 50, 85)  // Softer green for points
    val TasksDone = hsl(14, 70, 85)  // Softer coral for tasks done
    val ExamBlue = hsl(199, 60, 88)  // Much lighter blue for exam card

    // Task category colors (softer versions)
    val TaskGrammar = hsl(199, 60, 88)  // Light blue for grammar
    val TaskReading = hsl(122, 50, 85)  // Light green for reading
    val TaskVocabulary = hsl(122, 40, 90)  // Very light green for vocabulary

    // Achievement Colors
    val AchievementBronze = hsl(30, 67, 47)
    val AchievementSilver = hsl(0, 0, 75)
    val AchievementGold = hsl(51, 100, 50)
    val AchievementPlatinum = hsl(240, 12, 85)

    // Dark mode variants
    object Dark {
        val Background = hsl(0, 0, 7)
        val Foreground = hsl(0, 0, 98)
        val Card = hsl(0, 0, 10)
        val CardForeground = hsl(0, 0, 98)
        val Primary = hsl(199, 92, 73)
        val PrimaryForeground = hsl(0, 0, 7)
        val Secondary = hsl(0, 0, 15)
        val SecondaryForeground = hsl(0, 0, 98)
        val Muted = hsl(0, 0, 15)
        val MutedForeground = hsl(0, 0, 64)
        val Border = hsl(0, 0, 15)
        val SurfaceVariant = hsl(0, 0, 15)
    }
}

// Extension properties for easier access in composables
val Color.Companion.StudyPlan: DesignTokens get() = DesignTokens