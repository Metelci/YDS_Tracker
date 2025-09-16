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

    // EXACT Primary Colors from CODE_SPECIFICATIONS.MD
    val Primary = hsl(199, 92, 73)  // #81D4FA - Light Blue (Main Brand)
    val PrimaryContainer = hsl(199, 70, 85)  // #B3E5FC - Light Blue Container
    val PrimaryContainerForeground = hsl(199, 100, 20)
    val PrimaryForeground = hsl(0, 0, 100)  // White text on primary

    // EXACT Secondary Colors from specifications
    val Secondary = hsl(122, 39, 69)  // #A5D6A7 - Light Green
    val SecondaryContainer = hsl(122, 39, 82)  // #C8E6C9 - Light Green Container
    val SecondaryContainerForeground = hsl(122, 100, 15)
    val SecondaryForeground = hsl(0, 0, 100)

    // EXACT Tertiary Colors from specifications
    val Tertiary = hsl(14, 100, 78)  // #FFAB91 - Soft Coral
    val TertiaryContainer = hsl(14, 100, 88)
    val TertiaryContainerForeground = hsl(14, 100, 25)

    // EXACT Surface System from specifications
    val Background = hsl(0, 0, 98)  // #FAFAFA
    val Foreground = hsl(0, 0, 13)  // #212121
    val Card = hsl(0, 0, 100)
    val CardForeground = hsl(0, 0, 13)
    val Popover = hsl(0, 0, 100)
    val PopoverForeground = hsl(0, 0, 13)
    val Surface = hsl(0, 0, 100)
    val SurfaceVariant = hsl(0, 0, 96)
    val SurfaceContainer = hsl(0, 0, 94)
    val SurfaceContainerHigh = hsl(0, 0, 92)

    // EXACT Borders & Outlines from specifications
    val Border = hsl(0, 0, 89)
    val Input = hsl(0, 0, 89)
    val Ring = hsl(199, 92, 73)

    // EXACT Text Colors from specifications
    val Muted = hsl(0, 0, 96)
    val MutedForeground = hsl(0, 0, 46)
    val Accent = hsl(0, 0, 96)
    val AccentForeground = hsl(0, 0, 13)

    // EXACT Semantic Colors from specifications
    val Destructive = hsl(0, 65, 51)  // Error Red
    val DestructiveForeground = hsl(0, 0, 100)
    val Success = hsl(122, 39, 49)  // Success Green
    val SuccessContainer = hsl(122, 39, 82)
    val Warning = hsl(45, 100, 51)  // Warning Orange

    // Gamification Colors (using design tokens)
    val StreakFire = TertiaryContainer  // Use tertiary container for streak
    val PointsGreen = SecondaryContainer  // Use secondary container for points
    val TasksDone = TertiaryContainer  // Use tertiary container for tasks done
    val ExamBlue = PrimaryContainer  // Use primary container for exam card

    // Task category colors (using design tokens)
    val TaskGrammar = PrimaryContainer  // Light blue for grammar
    val TaskReading = SecondaryContainer  // Light green for reading
    val TaskVocabulary = SecondaryContainer  // Light green for vocabulary

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