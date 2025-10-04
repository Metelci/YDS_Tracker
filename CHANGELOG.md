# Changelog

All notable changes to this project will be documented in this file.

## 2.9.40-hotfix.1 - 2025-10-04

- Fix malformed `import androidx.compose.ui.graphics.Brush` lines (removed stray literal `n`), in:
  - `app/src/main/java/com/mtlc/studyplan/settings/ui/SettingsScreens.kt`
  - `app/src/main/java/com/mtlc/studyplan/social/tabs/ProfileTab.kt`
  - `app/src/main/java/com/mtlc/studyplan/social/components/LeaderboardRow.kt`
  - `app/src/main/java/com/mtlc/studyplan/social/tabs/FriendsTab.kt`
- Today screen fixes to restore compilation and behavior:
  - Correct `inferredFeaturePastelContainer("com.mtlc.studyplan.feature.today", s.id)` usage.
  - Align `StudyPlanTopBar` call with current API (remove unsupported `onMenuClick`).
  - Replace incompatible `ErrorState/EmptyState` usages with compatible UI.
  - Normalize odd characters in strings to standard middle dots (·).
- Build verified with `./gradlew assembleDebug -x test` (successful).

