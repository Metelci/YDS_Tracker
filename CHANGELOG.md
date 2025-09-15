# StudyPlan Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.1] - 2025-01-15

### Fixed
- **Tooltip System**: Fixed repeated tooltip popups during menu navigation
- **Progress Tracking**: Extended Progress overview from 8 to 12 weeks for better long-term visibility
- **Study Heatmap**: Adjusted heatmap timeline to display 12-week period (84 days) instead of 8 weeks
- **Onboarding Flow**: Improved tooltip triggering logic to prevent interruptions during navigation

### Improved
- **User Experience**: Smoother navigation without disruptive tooltip re-appearances
- **Progress Visualization**: Enhanced long-term progress tracking with extended timeline
- **UI Stability**: More stable onboarding and tooltip system

## [2.1.0] - 2025-09-15

### Added
- Onboarding Wizard: personalize Day‑1 with exam date, weekly availability (Mon–Sun minutes), and skill priorities
- Study Heatmap: GitHub‑style calendar (last 8 weeks) with task intensity per day and tap‑to‑detail
- Progress Ring + Confetti: animated sweep; confetti plays once per calendar day when reaching 100%
- Color‑coded Task Cards: skill strip (Grammar/Reading/Listening/Vocab), estimated minutes, and checkbox

### Improved
- Accessibility & visual polish: modernized icons, improved labels, reduced‑motion support

### Fixed
- Stability: minor fixes across progress calculations and UI

## [2.0.0] - 2024-12-15

### 🤖 **MAJOR: AI-Powered Analytics System**

This release introduces a revolutionary AI analytics engine that transforms the StudyPlan experience with genuine artificial intelligence.

### Added
- **🧠 Real AI Analytics Engine**: Complete replacement of mock data with sophisticated AI algorithms
- **📊 Intelligent Pattern Recognition**: Statistical clustering for time distribution and study behavior analysis
- **🎯 Smart Recommendations**: AI-powered suggestions with confidence scoring and personalized insights
- **⏰ Circadian Rhythm Analysis**: AI detects optimal study times based on performance correlation
- **🔥 Burnout Risk Assessment**: Advanced algorithms monitor workload and performance indicators
- **📈 Performance Prediction Models**: ML-like scoring with weighted accuracy and improvement trend analysis
- **🎨 Focus Score Calculation**: Session continuity and accuracy correlation analysis
- **💡 Weak Area Intelligence**: Error pattern analysis with automated improvement suggestions
- **📅 SmartScheduler Integration**: Unified AI system combining analytics with intelligent scheduling

### AI Features
- **Statistical Clustering** for time distribution analysis
- **Exponential Smoothing** for weekly progress tracking
- **Coefficient of Variation** for consistency measurement
- **Performance Correlation Analysis** for peak productivity detection
- **Error Frequency Analysis** for personalized weak area identification
- **Temporal Pattern Analysis** for habit formation recommendations
- **Efficiency Scoring** using time-to-completion analysis
- **Productivity Trends** with comprehensive time series analysis

### Improved
- **Analytics/Insights Menu**: Now displays genuine AI-powered recommendations instead of placeholder content
- **Recommendation System**: 15+ AI analysis functions working together for personalized suggestions
- **Performance Metrics**: Real-time calculation based on actual study data with advanced algorithms
- **User Experience**: Intelligent insights adapt to individual study patterns and preferences

### Technical Implementation
- **AI Algorithm Integration**: 20+ sophisticated mathematical functions for pattern analysis
- **SmartScheduler Fusion**: Seamless integration between analytics engine and scheduling AI
- **Real-time Processing**: All analytics computed from live TaskLog data using advanced statistical methods
- **Intelligent Prioritization**: AI-determined recommendation priority with confidence scoring
- **Comprehensive Data Models**: Enhanced analytics data structures supporting complex AI insights

### Changed
- **Version Jump to 2.0.0**: Reflects the major architectural shift from mock data to genuine AI
- **Analytics Engine**: Complete rewrite with real artificial intelligence replacing static mock responses
- **Recommendation Confidence**: All suggestions now include AI confidence percentages
- **Performance Analysis**: Sophisticated weighted calculations considering recency, difficulty, and improvement trends

### For Users
This update transforms your study analytics from basic tracking to intelligent coaching. The AI now:
- Learns your optimal study times and suggests when to focus on challenging topics
- Identifies patterns in your mistakes and provides targeted improvement strategies
- Predicts burnout risk and recommends appropriate break timing
- Analyzes your consistency and suggests habit formation techniques
- Provides personalized scheduling recommendations based on your unique performance patterns

The Analytics/Insights section is now a powerful AI coach that understands your study habits and helps optimize your learning journey.

## [1.9.3] - 2024-09-15

### Added
- **Comprehensive analytics system** with advanced study patterns tracking and insights
- **Full-featured data models** for performance analytics, productivity insights, and study recommendations
- **Enhanced UI components** for analytics visualization including progress rings, heatmaps, and task cards
- **Robust error handling** and data validation throughout the analytics pipeline

### Improved
- **Build system stability** with dependency compatibility fixes and version alignment
- **Code architecture** with proper data separation and modular analytics components
- **Type safety** across analytics data models with comprehensive property definitions
- **Navigation system** with enhanced parameter handling and screen transitions

### Fixed
- **Critical compilation errors** in analytics engine and components (100+ errors resolved)
- **Data model inconsistencies** with proper TaskLog timestamp property usage
- **Missing dependencies** and unresolved references throughout the analytics system
- **Build configuration issues** with updated Kotlin, Compose, and AndroidX versions

### Technical
- **Updated dependencies**: Kotlin 2.0.21, Compose BOM 2024.12.01, AGP 8.7.3
- **Analytics data classes**: StudyPatternsUI, AnalyticsData, WeeklyAnalyticsData, ProductivityInsights
- **Enhanced type system** with proper nullable handling and default values
- **Improved build performance** with optimized dependency resolution

## [1.9.2] - 2024-09-14

### Added
- **Comprehensive troubleshooting guide** in README.md with solutions for common build issues
- **Professional badges** in README.md showing build status, version, license, and platform support
- **Play Store changelog** generation system for consistent release notes
- **Enhanced Google Play Store description** with detailed feature comparisons and technical specifications

### Improved
- **Complete security documentation translation** from Turkish to English for better accessibility
- **README.md structure** with dedicated "Getting Started" section before technical details
- **Changelog organization** with clear separation of user-facing vs technical changes
- **Version management consistency** across all project documentation
- **Google Play Store presentation** with competitor comparisons and unique selling points

### Fixed
- **Inconsistent version numbering** in changelog entries
- **Mixed date formats** across documentation files
- **Developer notes mixed with release notes** - now properly separated
- **Missing repository URLs** in quick start commands

### Changed
- **Security documentation language** from Turkish to English for international accessibility
- **Changelog format** following Keep a Changelog standards
- **Documentation structure** prioritizing user onboarding experience

## [1.9.1] - 2024-09-14

### Added
- "Planned vs. Budget" header in Today screen showing planned minutes, daily budget, delta, and progress bar
- Real-time loading state binding for pull-to-refresh functionality

### Improved
- Today screen pull-to-refresh now provides accurate feedback based on actual loading state
- Better visual feedback for time budget vs actual progress

## [1.9.0] - 2024-09-12

### Added
- **Time-aware scheduling**: Plan alignment to start weekday and exact exam date
- **Home dashboard**: Unified view with exam countdown, today's tasks, streak, and overall progress
- **Flexible study slots**: Set minutes per day with intelligent task distribution
- **Persistent Quick Actions**: Global FAB for "Start Session" and "Add Quick Note/Flashcard"
- **Booster suggestions**: Automatic recommendations for weak categories with one-tap add
- **Auto-fill End Date**: Populate from next exam with date format presets and locale support
- **Task logging**: Track time spent and correctness after completing tasks

### Improved
- Progress calculations now use effective (post-compression) plan for accurate percentages
- Navigation redesigned with bottom tabs (Home, Tasks, Progress, Settings)
- Date display beside day names for better context
- Plan pipeline with start weekday alignment, end date trimming, and availability-based packing

### Changed
- Today screen FAB is now opt-in to avoid overlapping with global FAB
- Plan is automatically trimmed to precise number of days until exam

## [1.8.1] - 2024-09-11

### Added
- **First-run Welcome screen** with concise onboarding and Material Design icon
- Tokenized dialog paddings for consistent UI spacing

### Fixed
- **Progress screen crash** caused by multiple DataStore instances for the same file
- DataStore now uses application context for proper singleton behavior

### Improved
- Refined welcome screen copy and visuals
- Better dialog padding consistency across the app

## [1.8.0] - 2024-09-11

### Added
- **Accessibility improvements**: Content descriptions, larger touch targets, reduced motion support
- **Lightweight analytics**: In-app metrics wrapper with no PII and no network in release builds
- **Accessibility Test Rule** for automated accessibility testing
- **Material 3 design system**: Centralized spacing tokens and standardized elevations

### Improved
- UI polish with Material 3 styling across Today, Reader, Mock, Review, Practice, and Progress screens
- Reduced motion support based on system settings
- Semantic improvements for screen readers
- Card radius and elevation standardization

### Analytics Events Added
- `app_open`, `today_open`
- `session_start`, `session_complete`, `session_skip`
- `mock_start`, `mock_submit`
- `reader_pref_change`

## [1.7.0] - 2024-09-10

### Added
- **Flexible plan duration**: Choose start date and total weeks, or provide end date/months for auto-calculation
- **Dedicated lesson screen**: Tapping "Start" in Today opens focused lesson view with navigation
- **Calendar date display**: Week cards and day rows now show actual calendar dates
- **Plan customization improvements**: Back/Cancel actions and Info dialog in Customize Plan editor

### Improved
- Navigation polish with snackbar feedback when starting lessons
- About sheet accessibility - Info button works from main plan view
- UI cleanup with `HorizontalDivider` replacing deprecated `Divider`
- RTL support with `Icons.AutoMirrored.Filled.ArrowBack`

### Fixed
- Unresponsive Start buttons now properly trigger navigation
- Info icon unresponsive on main screen
- Missing Turkish localization strings and lint issues

## [1.6.1] - 2024-09-05

### Added
- **Navigation Compose framework** for improved screen navigation
- **Centralized AppNavHost** for navigation management

### Improved
- Enhanced app stability and performance
- Better Activity configuration with proper themes

### Fixed
- Removed deprecated manifest package attribute in favor of Gradle namespace
- MainActivity properly set as primary launcher activity

### Changed
- Navigation logic moved from activities into composable-based navigation

## [1.6.0] - 2024-09-01

### Added
- **Customizable Plans**: Hide/edit tasks and add your own per day
- Plan customization accessible from top bar "Customize" action
- Changes persist on device and apply across the app

### Improved
- Enhanced plan flexibility and personalization options

---

## Developer Notes

### Technical Implementation Details

#### v1.9.1 Technical Changes
- `TodayViewModel` toggles `isLoading` while loading to drive refresh indicator
- Daily minutes read from `PlanSettingsStore.settingsFlow` based on current weekday

#### v1.9.0 Technical Changes
- Added `PlanSettingsStore` (DataStore) with per-day minutes and custom date format fields
- Plan pipeline: start weekday alignment → end date trimming → availability-based weekly packing → overrides merge
- DataStore: lightweight task logs (time, correctness, category) with simple encoding
- Weakness detection computes incorrect rates per category
- Task IDs remapped to new week numbers; week titles recomputed with localized phase labels

#### v1.8.0 Technical Changes
- `LocalReducedMotion` provider with disabled non-essential animations
- `Analytics.track()` via `WorkManager`; logs only in debug builds; resilient to process death
- Added `Spacing` tokens and `Elevations` for consistent Material 3 theming
- Replaced ad-hoc paddings with 8-pt grid system

#### v1.7.0 Technical Changes
- `PlanSettingsStore` (DataStore) integrated with `PlanRepository`
- Base plan proportionally remapped to target length while preserving pacing
- Lesson screen implemented as placeholder scaffold for future interactive content

#### Migration Considerations
- **v1.7.0**: Changing total duration remaps week numbers/IDs. Existing per-task overrides remain, but dramatic duration changes may misalign overrides. Consider resetting overrides after large schedule changes.

### Development Guidelines
- All technical implementation details are documented here for developers
- User-facing changes are documented in the main changelog above
- Follow semantic versioning for all releases
- Maintain consistent date format (YYYY-MM-DD) for all entries
