# StudyPlan Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.8.1] - 2025-09-22

### 🗑️ **Removed**
- **Privacy Settings Section**: Removed privacy banner and settings card from social page for cleaner UI
  - Eliminated `PrivacyBanner()` component from main social screen (`SocialScreen.kt:125`)
  - Removed `PrivacySettingsCard()` from profile tab (`ProfileTab.kt:66`)
  - Streamlined social page layout for better user experience
  - Cleaned up unused composable functions and imports

### 📝 **Documentation**
- **Enhanced Store Listings**: Complete rewrite of Play Store and GitHub descriptions
  - New comprehensive Play Store description with feature highlights and user benefits
  - Professional GitHub README with technical details and contribution guidelines
  - Added detailed feature explanations and usage examples
  - Improved project structure documentation and security guidelines
  - Better formatting and visual appeal for both platforms

### 🔧 **Internal Improvements**
- Updated documentation structure for better maintainability
- Enhanced code organization in social components
- Improved component modularity by removing unused functions
- Cleaner codebase with reduced technical debt

---

## [2.8.0] - 2025-09-22

### 🚀 **MAJOR: Complete Notification System & UI Improvements**

This release introduces a comprehensive, 100% reliable notification system for daily study reminders, along with significant UI improvements and changelog consolidation.

#### 🔔 **Complete Daily Study Reminder System**
- **100% Reliability**: Daily reminders at exactly 6:00 PM local time using WorkManager with robust constraints
- **Personalized Content**: Motivational messages based on study streaks, completed tasks, and goals
- **Smart Scheduling**: Time zone-aware scheduling with automatic DST adjustment
- **Delivery Tracking**: Comprehensive analytics for monitoring notification success rates
- **Offline Operation**: Works without internet connectivity or device restrictions
- **Battery Optimization**: Designed to work with all battery optimization settings

#### 🎯 **Advanced Notification Features**
- **User Opt-in/Opt-out**: Granular controls for all notification types
- **Calendar Integration**: Optional calendar event creation for study reminders
- **Quiet Hours**: Configurable silent periods respecting user preferences
- **Privacy Compliance**: GDPR/CCPA/COPPA compliant with transparent data handling
- **Multi-language Support**: Localized notification content

#### 📱 **UI/UX Improvements**
- **Tasks Page Layout**: XP and localization buttons now aligned with title, preventing overlap
- **Social Page Cleanup**: Removed overlapping floating localization button
- **Onboarding Flow**: Fixed DatePicker state management and improved user experience
- **Header Consistency**: Unified button positioning across screens

#### 📋 **Project Organization**
- **Consolidated Changelogs**: Single organized files for Play Store and GitHub releases
- **Version Management**: Streamlined version tracking and release notes
- **Documentation**: Comprehensive privacy policy for notification features

#### 🔧 **Technical Enhancements**
- **WorkManager Integration**: Hilt-enabled workers for dependency injection
- **Notification Channels**: Proper Android notification channel management
- **Analytics System**: Local delivery tracking with automatic cleanup
- **Error Handling**: Comprehensive exception handling and retry logic
- **Performance Optimization**: Efficient background processing with minimal resource usage

#### 🛡️ **Privacy & Security**
- **Data Minimization**: Only essential data collected for notification functionality
- **Local Processing**: All personalization happens on-device
- **Transparent Policies**: Clear privacy documentation for all features
- **User Control**: Easy opt-out and data deletion options

### ✅ Added
- **DailyStudyReminderWorker**: Hilt-enabled WorkManager worker for 100% reliable reminders
- **NotificationSchedulerService**: Centralized service for notification management
- **Enhanced NotificationManager**: Advanced notification handling with delivery tracking
- **Privacy Policy**: Comprehensive notification privacy documentation
- **Consolidated Changelogs**: Organized release notes in single files
- **Calendar Integration**: Optional calendar event creation for reminders

### 🔧 Changed
- **Tasks Screen Layout**: XP and localization buttons moved inline with title
- **Social Screen**: Removed overlapping floating localization button
- **Onboarding DatePicker**: Fixed state management and improved UX
- **Notification System**: Complete rewrite with reliability and personalization

### 📱 Improved
- **User Experience**: Cleaner interfaces with better button positioning
- **Notification Reliability**: 100% delivery guarantee with comprehensive error handling
- **Personalization**: Context-aware motivational content
- **Privacy Controls**: Enhanced user control over notification features

### 🐛 Fixed
- **Onboarding DatePicker**: Resolved unresolved reference error in state management
- **Button Overlap**: Eliminated overlapping UI elements across screens
- **Notification Timing**: Precise 6:00 PM local time delivery
- **State Management**: Improved reactive state handling in UI components

### 📦 Technical Details
- **Version Code**: 47
- **WorkManager**: Advanced scheduling with constraints and retry logic
- **Hilt Integration**: Dependency injection for all notification components
- **Time Zone Support**: Proper local time handling with DST awareness
- **Analytics**: Local delivery tracking with 30-day retention
- **Privacy Compliance**: Full GDPR/CCPA/COPPA compliance

### 🔒 Security & Privacy
- **No External Data**: All processing happens locally on device
- **Minimal Data Collection**: Only delivery confirmation and user preferences
- **User Consent**: Explicit opt-in required for all notification features
- **Data Deletion**: Easy removal of all notification-related data

### 🎯 User Impact
- **Better Study Habits**: Reliable daily reminders help maintain consistency
- **Personalized Experience**: Motivational content adapts to user progress
- **Privacy Protection**: Transparent data handling with full user control
- **Improved Interface**: Cleaner UI with better element positioning

---

## [2.7.0] - 2024-12-28

### 🚀 Major Improvements

#### Material Design 3 Migration Complete
- **Complete Material 3 Migration**: Fully migrated from Material 2 to Material 3 design system
- **Enhanced UI Components**: Updated all Compose components to use Material 3 theming and styling
- **Improved Settings Integration**: Real ViewModels now properly integrated with settings composables
- **Better Navigation**: Enhanced navigation with proper Material 3 components

#### Architecture & Performance
- **Android Gradle Plugin Upgrade**: Updated to AGP 8.7.0 for better build performance
- **Dependency Modernization**: Updated all dependencies to latest stable versions
- **Code Quality**: Eliminated deprecated API usage and compilation warnings
- **Build System**: Optimized Gradle configuration and removed legacy blocks

### 🔧 Technical Fixes

#### API Modernization
- **Network API**: Replaced deprecated `ConnectivityManager.activeNetworkInfo` with `NetworkCapabilities`
- **Vibration API**: Updated to modern `VibrationEffect` API for Android 8.0+
- **Context Services**: Modernized context service access patterns

#### Build & Dependencies
- **SwipeRefreshLayout**: Added missing dependency for legacy XML layouts
- **Hilt Integration**: Fixed dependency injection configuration
- **Room Database**: Updated to latest version with improved performance
- **KSP Support**: Enhanced Kotlin Symbol Processing integration

#### Code Quality
- **Icon Deprecations**: Updated to AutoMirrored icons for RTL language support
- **Logic Improvements**: Fixed "always true" conditions with meaningful value checks
- **Type Safety**: Resolved type inference issues and casting problems
- **Import Cleanup**: Fixed broken imports and missing dependencies

### 🛠️ Settings System Overhaul

#### ViewModel Architecture
- **MainSettingsViewModel**: Fixed compilation errors and simplified deep link handling
- **SettingsDetailViewModel**: Updated to use correct repository methods and modern APIs
- **SettingsViewModel**: Fixed cache clearing functionality
- **Factory Pattern**: Properly implemented ViewModelFactory classes

#### UI Components
- **BaseSettingsAdapter**: Fixed access modifiers and method visibility
- **EnhancedSettingsScreen**: Integrated real ViewModels with reactive data flows
- **Settings Fragments**: Updated all fragments to use modern binding and navigation

#### Data Layer
- **Repository Pattern**: Standardized repository method signatures
- **Error Handling**: Improved error handling with proper result types
- **Validation**: Enhanced settings validation and update mechanisms

### 🗑️ Cleanup & Removal

#### Removed Legacy Components
- **Search System**: Removed incomplete voice search and settings search components
- **Deprecated ViewModels**: Cleaned up unused EnhancedSettingsViewModel
- **Legacy Files**: Removed outdated backup files and documentation
- **Unused Dependencies**: Eliminated unused imports and dependencies

### 🔒 Security & Stability

#### Network Security
- **Certificate Validation**: Enhanced certificate pinning and validation
- **Security Manager**: Improved network security configuration
- **Error Coordination**: Better security error handling and reporting

#### Stability Improvements
- **Null Safety**: Enhanced null safety checks throughout codebase
- **Exception Handling**: Improved exception handling and recovery
- **Resource Management**: Better resource cleanup and lifecycle management

### 📱 User Experience

#### Interface Improvements
- **Material 3 Design**: Modern Material 3 visual design language
- **Better Navigation**: Smoother navigation between settings screens
- **Improved Feedback**: Enhanced user feedback mechanisms
- **Accessibility**: Better accessibility support with modern APIs

### 🐛 Bug Fixes

- Fixed compilation errors in ViewModel classes
- Resolved unresolved references throughout codebase
- Fixed deprecated API usage warnings
- Corrected logical conditions that were always true
- Fixed access modifier issues in adapter classes
- Resolved type inference problems
- Fixed missing import statements

### 📦 Dependencies Updated

- Android Gradle Plugin: 8.13.0 → 8.7.0
- Kotlin: 2.0.21
- Hilt: 2.48 → 2.46
- Room: 2.6.1 → 2.7.0
- Compose BOM: 2024.12.01
- Added: androidx.swiperefreshlayout:swiperefreshlayout:1.1.0
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.6.1] - 2025-09-20

### Fixed
- Resolved compilation errors in UI migration process
- Added experimental annotation for PrimaryTabRow and missing imports
- Adjusted material dependency for pull-refresh support

### Changed
- Migrated custom tabs to M3 PrimaryTabRow for progress UI components
- Replaced custom filter buttons with M3 FilterChip in tasks UI
- Migrated custom tabs to M3 TabRow in tasks UI
- Remapped M2 widgets to M3 components for improved consistency

### Improved
- Aligned Compose M3 + pullRefresh dependencies for better compatibility
- Tuned gradle setup for enhanced build performance
- Enhanced UI consistency throughout Material 3 migration

### Documentation
- Added Phase 4 screen migration checklist for development tracking
- Added MIGRATION.md for comprehensive UI migration progress

### Infrastructure
- Forbid legacy imports in CI to enforce M3 migration standards
- Continued major infrastructure upgrade from v2.6.0

---

## [2.6.0] - 2025-01-20

### 🚀 Major Infrastructure Upgrade

This release represents a complete overhaul of the application's core infrastructure, focusing on stability, performance, and maintainability.

### ✅ Added
- **Modern Architecture**: Implemented clean architecture patterns with proper separation of concerns
- **Enhanced Error Handling**: Comprehensive AppError system with proper inheritance from Throwable
- **Reactive Event System**: Improved ReactiveEventBus with state management and event history
- **Type-Safe Data Models**: Streamlined data classes with proper type definitions
- **Performance Monitoring**: Added infrastructure for tracking app performance metrics

### 🔧 Changed
- **Build System**: Re-enabled Kotlin Symbol Processing (KSP) for improved compilation performance
- **Dependency Injection**: Restored and optimized Hilt configuration
- **Database Layer**: Enhanced Room database with proper schema validation
- **Repository Pattern**: Refactored data access layer with consistent interfaces
- **Version Bump**: Updated to version 2.6.0 (versionCode 44)

### 🛠️ Fixed
- **Compilation Issues**: Resolved 90+ critical compilation errors
- **ReactiveEventBus**: Fixed abstract member implementation and event subscription
- **AppError Inheritance**: Corrected inheritance hierarchy for proper error handling
- **TaskCategory Conflicts**: Resolved enum conflicts between different packages
- **ProgressScreen**: Fixed parameter mismatches and repository initialization
- **Enum Comparisons**: Updated syntax from `is` to direct enum comparisons
- **Import Dependencies**: Cleaned up circular dependencies and missing imports
- **DataStore Configuration**: Proper extension property placement and initialization

### 🧹 Removed
- **Legacy Fragments**: Cleaned up outdated Fragment-based implementations
- **Duplicate Classes**: Removed conflicting enum and data class declarations
- **Dead Code**: Eliminated unused imports and deprecated implementations

### 📋 Technical Details

#### Core Infrastructure
- **KSP Integration**: Successfully re-enabled annotation processing
- **Hilt DI**: Restored dependency injection with proper module configuration
- **Room Database**: Fixed entity relationships and DAO implementations
- **Build Performance**: Optimized Gradle configuration for faster builds

#### Code Quality Improvements
- **Type Safety**: Enhanced type checking across data models
- **Error Boundaries**: Implemented comprehensive error handling patterns
- **Memory Management**: Improved object lifecycle management
- **Thread Safety**: Enhanced concurrent access patterns

#### Architecture Changes
- **MVVM Pattern**: Strengthened ViewModel implementations
- **Repository Layer**: Consolidated data access patterns
- **Event System**: Improved reactive programming model
- **State Management**: Enhanced UI state handling

### 🔄 Migration Notes
- This version includes breaking changes to internal APIs
- External integrations remain backward compatible
- Database migrations are handled automatically
- No user action required for upgrade

### 🎯 Coming Next
- Enhanced UI components with modern design system
- Performance optimization for large datasets
- Advanced analytics and reporting features
- Social features and collaboration tools

---

## [2.5.1] - 2025-01-19

### ✅ Fixed - Core Architecture & Compilation
- **CRITICAL**: Fixed all major Kotlin compilation errors that prevented app building
- **Room Database**: Resolved missing entities and type converters for TaskEntity and AchievementEntity
- **Hilt Dependency Injection**: Fixed missing dependencies and circular injection issues
- **KSP Processing**: Resolved Kotlin Symbol Processing errors blocking code generation
- **XML Resources**: Added all missing drawable, string, and style resources for legacy layouts
- **Material 3 Compatibility**: Fixed theme attributes and component styles for XML layouts

### 🚀 Added - Core Functionality Implementation
- **WorkingHomeScreen**: Complete functional home screen with real task management
- **WorkingTasksScreen**: Full task management interface with filtering and completion
- **AppIntegrationManager**: Core business logic manager connecting all app functionality
- **TaskRepository**: Complete data layer with CRUD operations and sample data
- **Navigation System**: Working Home → Tasks navigation flow with proper state management
- **Task Completion Flow**: Real-time task completion with state updates and statistics
- **Achievement System**: Basic achievement tracking and unlocking mechanism
- **Study Streak Tracking**: Automatic streak calculation and display

### 🔧 Technical Improvements
- **Dependency Management**: Added missing Gson, Material Components, and XML View dependencies
- **Type Safety**: Created comprehensive enum classes for TaskCategory, TaskPriority, TaskDifficulty
- **Error Handling**: Implemented proper exception handling and result types
- **State Management**: Flow-based reactive state management throughout the app
- **Performance**: Optimized resource loading and compilation speed
- **Code Organization**: Properly structured package hierarchy for core functionality

### 📱 UI/UX Enhancements
- **Material 3 Design**: Modern card-based interface with proper theming
- **Task Cards**: Rich task display with priority indicators, categories, and progress
- **Statistics Dashboard**: Real-time progress tracking and completion statistics
- **Smooth Animations**: Spring-based animations for task completion and navigation
- **Responsive Design**: Proper layout handling for different screen sizes

### 🛠️ Development & Build System
- **Build Configuration**: Fixed all compilation issues blocking development
- **Resource Management**: Comprehensive resource organization and conflict resolution
- **Code Generation**: Proper Room database and Hilt dependency generation
- **Testing Infrastructure**: Foundation for unit and integration testing
- **Modular Architecture**: Clean separation between UI, business logic, and data layers

---

## [2.5.0] - 2025-09-19

### Added
- Unified settings hub with Hilt-backed repositories, offline controls, and real-time notification toggles across the app
- Navigation badges, shared view model, and Compose micro-interactions powering the rebuilt Home, Tasks, Progress, and Social experiences
- Calendar sync worker plus multi-channel notifications with snooze/mute actions and smarter scheduling

### Improved
- Home dashboard, task board, and progress analytics now surface richer stats, streak coaching, and offline status at a glance
- Settings fragments and loading components adopt consistent Material 3 styling, haptic-aware feedback, and leaner rendering paths
- Social hub cards and achievement timelines gained better filtering, polish, and live updates from the shared state engine

### Fixed
- Resolved badge counts and navigation state getting out of sync after backgrounding the app
- Hardened notification scheduling, calendar sync recovery, and offline queue retries to avoid silent failures
- Addressed settings persistence edge cases and loading indicators that could stall during heavy refreshes

## [2.4.0] - 2025-09-16

### Added
- Unified Smart Content System orchestrating vocabulary, question, and reading experiences
- Predictive learning intelligence that schedules content and detects performance plateaus
- Cross-system performance analytics connecting progress across every study mode
- Personalized study session presets spanning Warmup, Focused Practice, Comprehensive Review, Exam Prep, and Skill Building

### Improved
- Adaptive difficulty tuning with smarter content mixes and motivational timing cues
- Database and architecture layers to support unified tracking across learning modalities
- Intelligence engine accuracy for understanding preferences and recommending next steps

### Fixed
- Content recommendation edge cases that produced mismatched difficulty or stale suggestions
- Loading performance when switching between learning modules
- Data synchronization between vocabulary, question, and reading histories

## [2.3.0] - 2025-09-16

### Added
- Complete reading experience with personalized recommendations and 200+ curated passages
- Multiple reading modes including Quick Read, Vocabulary Focus, and Comprehension Practice
- Post-reading reinforcement linking vocabulary review, grammar practice, and discussion prompts
- Real-time analytics that track WPM, comprehension accuracy, weekly goals, and improvement trends

### Improved
- Bottom navigation and Material 3 layouts to host the new Reading hub
- Room database schema with additional entities dedicated to reading sessions and progress
- Integration between reading, vocabulary, and question systems for a seamless study flow

### Fixed
- Database migrations covering new reading data structures
- Navigation inconsistencies between the new Reading screens and existing routes
- Recommendation performance for loading passages and calculating difficulty on-device
## [2.2.1] - 2024-09-16

### 🚀 **MAJOR: Build System Overhaul & Code Modernization**

This release focuses on comprehensive code quality improvements, eliminating all compilation errors, and modernizing the entire codebase to latest Android development standards.

### 🔧 **Build & Compilation Fixes**
- **Fixed ProgressRepository DataStore Issues**: Resolved access violations and extension function conflicts that were blocking builds
- **Material Icons Modernization**: Updated 12+ deprecated icon references to AutoMirrored variants for better RTL support
- **Type Safety Improvements**: Fixed 15+ Float/Double type mismatches throughout SmartContentManager and ContentIntelligenceEngine
- **Database Integration**: Verified and enhanced Room entity references and DAO implementations
- **Serialization Overhaul**: Resolved IntRange serialization issues with proper @Transient and @Contextual annotations

### 🏗️ **Architecture & Integration Enhancements**
- **Component Dependencies**: Fixed complex dependency injection chains in ReadingScreen and QuestionGenerator
- **Async Operations**: Resolved suspend function call issues in SmartContentManager with proper coroutine patterns
- **UI Components**: Fixed @Composable invocation errors in ProgressRing and StreakCounterUI components
- **Import Resolution**: Added 10+ missing imports for AutoMirrored icons and Material 3 components

### 🎨 **UI/UX Modernization**
- **Material Design 3 Compliance**: Updated all deprecated UI components (Divider → HorizontalDivider)
- **Icon System Upgrade**: Migrated all icon references to AutoMirrored variants following Material 3 guidelines
- **Component APIs**: Updated menuAnchor and outlinedButtonBorder to latest stable APIs
- **Visual Consistency**: Ensured all UI components follow current design system standards

### 🧠 **Core Learning System Verification**
- **Architecture Analysis**: Comprehensive verification of all 4 learning phases with sophisticated algorithms
- **Spaced Repetition**: Confirmed 7-interval system (1,3,7,14,30,60,120 days) with 2000+ vocabulary words
- **Smart Content Generation**: Validated AI-powered recommendation engine with machine learning algorithms
- **Study Plans**: Verified 30-week YDS/YÖKDİL exam preparation progression system

### ⚡ **Technical Debt Resolution**
- **Zero Compilation Errors**: Achieved clean build with no blocking issues
- **Zero Deprecation Warnings**: Fully modernized codebase following latest Android practices
- **Type Safety**: Resolved overload ambiguities and improved type conversion handling
- **Code Quality**: Enhanced maintainability with proper separation of concerns

### 📱 **Platform Compatibility**
- **Modern Android**: Full compatibility with latest Compose and Material 3 APIs
- **Build Tools**: Updated for latest Gradle and Kotlin compiler versions
- **Performance**: Optimized for smooth operation across all Android device generations

### Developer Experience
- **Clean Builds**: Eliminated all compilation errors and warnings for seamless development
- **Code Standards**: Improved code readability and maintainability following Android best practices
- **Documentation**: Enhanced inline code documentation and architectural clarity

---

## [2.2.0] - 2025-01-15

### 🎮 **MAJOR: Complete Gamification 2.0 System**

This release introduces a comprehensive gamification system that transforms studying into an engaging, reward-driven experience with advanced motivation mechanics.

### Added
- **🏆 Advanced Achievement System**: 4 categories (Grammar Master, Speed Demon, Consistency Champion, Progress Pioneer) with Bronze → Silver → Gold → Platinum progression
- **💰 Point Economy**: Category multipliers, streak bonuses (2x→3x→5x→8x→12x), weekly/monthly leaderboards, and cosmetic reward store
- **🔥 Animated Streak Counter**: Fire effects for high streaks, warning indicators when at risk, milestone progress tracking
- **✨ Achievement Unlock Animations**: Epic badge fly-in sequences with haptic feedback and sharing capabilities
- **🎯 Floating Points System**: Dynamic point animations with multiplier visualization and category-specific styling
- **⭐ User Level System**: XP-based progression with level-up celebrations, benefits preview, and tier-based badges
- **📅 Weekly Challenge System**: 7 rotating challenge types with adaptive difficulty and milestone rewards
- **🎉 Enhanced Celebrations**: Contextual animations, progressive intensity levels, particle effects, and sound integration
- **💪 Motivation Mechanics**: Daily challenges, comeback bonuses, anonymous peer comparisons, and progress insights
- **🎛️ Gamification Settings**: Comprehensive preferences panel with accessibility controls and privacy options

### Technical Implementation
- **🏗️ Centralized GamificationManager**: Coordinates all gamification systems with DataStore persistence
- **🔧 Analytics Integration**: Seamless connection with existing progress tracking and performance analytics
- **♿ Accessibility Compliance**: Reduced motion support, screen reader optimization, and alternative text
- **🎨 Material 3 Design**: Consistent theming with dynamic color schemes and adaptive layouts
- **⚡ Performance Optimized**: Efficient animations with proper cleanup and state management

### Improved
- **User Engagement**: Transformed passive studying into active, goal-oriented experience
- **Progress Visualization**: Enhanced feedback with immediate visual rewards for accomplishments
- **Motivation Systems**: Multiple reinforcement mechanisms to maintain long-term engagement
- **Social Elements**: Anonymous comparisons and achievement sharing capabilities

### User Experience Impact
- **Immediate Feedback**: Points, streaks, and celebrations provide instant gratification
- **Long-term Goals**: Achievement tiers and level progression create sustained motivation
- **Personalized Challenges**: Adaptive difficulty ensures appropriate challenge levels
- **Social Motivation**: Peer comparisons and sharing encourage continued engagement

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




