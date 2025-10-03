# StudyPlan Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.9.36] - 2025-10-03 (hotfix)

### Removed
- **Settings Navigation Components**: Cleaned up legacy navigation code
  - Removed NavigationBadgeManager.kt
  - Removed NavigationStateManager.kt
  - Removed StudyPlanNavigationManager.kt
  - Removed SettingsDeepLinkHandler.kt

- **Settings Fragments**: Removed unused XML-based settings fragments
  - BackupSettingsFragment.kt and layout
  - BaseSettingsFragment.kt and layout
  - ConflictResolutionFragment.kt
  - GamificationSettingsFragment.kt
  - NotificationSettingsFragment.kt
  - PrivacySettingsFragment.kt
  - SettingsDetailFragment.kt and layout

### Code Quality
- Streamlined dependency injection in SettingsDependencyInjection.kt
- Updated MainSettingsViewModel and SettingsViewModelFactory
- Migrated settings to Compose-based navigation
- Removed app-release.aab from version control

## [2.9.35] - 2025-10-02 (hotfix)

### Fixed
- **Dark Theme Color Consistency**: Unified color palette across all app pages in dark mode
  - All pages now use the same TASKS palette colors for consistent visual experience
  - Light theme remains completely unchanged
  - Improved dark mode contrast with adjusted alpha transparency (0.12f → 0.4f) for better visibility
  - Enhanced AwardCard rarity colors for better readability in dark mode

### Technical Improvements
- Fixed AndroidManifest warning by removing unnecessary WorkManagerInitializer meta-data
- Added deprecation suppressions for statusBarColor and navigationBarColor (deprecated in API 35+)
- Improved code maintainability with proper theme handling

## [2.9.34] - 2025-10-02 (hotfix)

### Fixed
- **Top Bar Visual Refinement**: Improved FixedTopBar styling for cleaner appearance
  - Replaced shadow elevation with Material 3 tonal elevation for more subtle depth
  - Changed background from transparent to Material 3 surface color for better consistency
  - Removed bottom divider to eliminate unwanted border appearance
  - Enhanced visual hierarchy with proper surface elevation (2.dp)

### Added
- **Localization Enhancements**: Added missing English translations
  - Gamification and achievement notification strings
  - Tooltip messages for achievements, plan customization, and settings
  - Action button labels (back to plan, save, cancel, etc.)
  - Streak expiry warnings and daily goal completion messages

### Technical Improvements
- Material 3 design system compliance with proper tonal elevation
- Improved top bar rendering with optimized shadow/elevation system
- Better accessibility with complete string resources

## [2.9.33] - 2025-10-02 (hotfix)

### Fixed
- **Dark Mode Status Bar Visibility**: Fixed critical issue where notification/status bar was invisible in dark mode
  - Implemented dynamic status bar icon colors that adapt to theme (light icons in dark mode, dark icons in light mode)
  - Added themed status bar colors with 95% opacity for subtle separation
  - Fixed navigation bar to match status bar styling for consistency
  - Updated background gradient to properly support dark mode
  - Added safe initial values to prevent crashes during theme changes

### Removed
- **Plan Management Card**: Removed "Plan Management" card from Tasks page including:
  - "Modify This Week" and "Generate Next Week" buttons
  - "View Planning Analytics" option
  - All related dialogs and handlers
  - Cleaned up orphaned imports and unused code

- **Study Groups Setting**: Removed "Study Groups" option from Settings page including:
  - Study Groups toggle from Social settings
  - All related data structures and handlers
  - Database and repository references

- **Social Page Top Bar Border**: Removed blue border from Social page top bar for cleaner appearance

### Code Quality
- **Orphaned Code Cleanup**: Identified and removed orphaned imports and unused code from:
  - Settings page (unused Group icon import)
  - Tasks page (unused TextButton import)
  - All files verified to have no remaining orphaned code

### Technical Improvements
- Enhanced theme observation with proper StateFlow integration
- Improved system bar handling using WindowCompat API
- Added comprehensive edge case handling for theme changes
- Ensured compatibility across all Android versions and manufacturers

## [2.9.32] - 2025-10-01 (hotfix)

### UI
- Apply pastel color scheme to home cards; extend across app.
- Per-feature pastel palettes with dark/light compositing.
- Reduced green dominance on Tasks; warmer Today, higher-contrast Analytics.

### Dev
- Added `PastelPeach` token and centralized pastel helpers.
- Package→FeatureKey mapper for automatic palette selection.

## [2.9.31] - 2025-10-01

### Changed
- Wrapped the settings category pill grid in a Prussian blue outlined card to mirror the Tasks reference styling.
- Refreshed all Home screen summary cards with a light pastel palette that adapts for dark mode.
