# Road to YDS – StudyPlan

Road to YDS is a modern Android app that guides learners through a 30-week preparation journey for the YDS and YÖKDİL English exams. It combines a curated curriculum, live exam tracking, and progress analytics to keep students on schedule from day one until exam day.

## Overview

- **Latest release:** v2.9.70 (November 21, 2025)
- **Target exams:** YDS, YÖKDİL (English)
- **Supported locales:** English, Turkish
- **Minimum Android version:** API 30

The app ships with an offline-first plan repository, localized UI, and a unified settings experience. Recent work focused on polish: a floating navigation bar, refreshed settings cards with gradient backgrounds, localized exam countdowns, fully functional analytics navigation from home screen cards, and weekly plan screens that now render real Raymond Murphy curriculum data.

## Core Features

### Structured Study Program
- 30-week curriculum organised into Foundation, B-level development, C1 mastery, and final exam camp.
- Weekly and daily study plans sourced from real Raymond Murphy course material.
- Custom task support for adding personal study items or editing existing sessions.

### Exam Readiness Tools
- Live countdown cards for upcoming YDS sessions with localized dates and registration windows.
- Calendar export (ICS) for weekly plans and exam reminders.
- Weekly goal tracking with visualised progress and streak support.

### Progress & Motivation
- Achievement system with refreshed awards tab and celebratory animations.
- Analytics dashboard accessible via home screen cards showing real-time stats, streaks, performance metrics, and productivity insights.
- Interactive analytics with 4 tabs: Overview (streak & points), Performance (accuracy & consistency), Patterns (time distribution), and Insights (AI recommendations).
- Gamified XP, points, and daily milestones to keep learners engaged.
- Clickable Points Today and Streak cards on home screen for instant access to detailed progress tracking.

### Personalisation & Settings
- Redesigned settings hub with tabbed categories and gradient toggle cards.
- Quiet hours, haptic control, celebratory effects, and privacy preferences in one place.
- Full localisation for English and Turkish text, including accessibility content.

### Privacy & Security
- AES-256 encrypted storage for sensitive data via AndroidX Security.
- Biometric authentication (fingerprint/face) and PIN fallback.
- Certificate pinning and strict network policies; app runs fully offline after initial setup.

## Recent Highlights (v2.9.52 - v2.9.70)

- **v2.9.70:** Weekly schedule day names now respect your device language (no more hardcoded Turkish labels), and the exam countdown shows a stale-data warning if OSYM dates can't be refreshed.
- **v2.9.70:** Added 2027-2028 YDS fallback sessions, enabled OWASP Dependency-Check by default, encrypted FCM analytics prefs, and muted Koin logs for release builds.
- **v2.9.66:** Resource Library screen now uses the shared StudyPlan top bar with matching gradient capsule, so it feels identical to the Home, Tasks, and Overview headers.
- **v2.9.66:** Updated Resource Library WebView dialog to use auto-mirrored icons and Material 3's `HorizontalDivider`, removing remaining warnings and improving RTL readability.
- **v2.9.65:** Analytics Performance tab UI polish - fixed tab title truncation, scaled text to fit in one line, enhanced chart with legend and grid lines.
- **v2.9.65:** Performance Trends chart now properly displays single data points for new users - no more empty space.
- **v2.9.65:** Verified all analytics display real user data - production-ready with proper empty states for first-time users.
- **v2.9.63:** Analytics navigation fully wired - Points Today and Streak cards now clickable and navigate to comprehensive analytics dashboard.
- **v2.9.63:** Settings cards spacing optimized between tabs and content for consistent visual hierarchy.
- **v2.9.63:** All settings cards now use gradient backgrounds instead of solid colors for enhanced visual appeal.
- Floating, frosted bottom navigation bar that respects system insets.
- Weekly Study Plan and Study Plan Overview headers now share a consistent top bar.
- Settings cards rebuilt with gradient backgrounds and solid toggle containers for clarity.
- Achievement roster expanded with hidden milestones; awards tab styling refreshed.
- Exam countdown stabilised with guarded ÖSYM refresh logic and locale-aware date output.
- CI/CD hardening: Detekt clean, GitHub Actions artifact uploads fixed, Kotlin 2.0.21 support.

## Screens at a Glance

- **Home:** Daily task ring, XP summary, exam countdown, weekly plan, clickable Points Today and Streak cards for quick analytics access.
- **Analytics:** 4-tab dashboard (Overview, Performance, Patterns, Insights) with real-time progress tracking, streak visualization, and AI-powered study recommendations.
- **Weekly Plan:** Actual Raymond Murphy units per day, completion tracking, goal slider.
- **Tasks & Awards:** Priority queues, achievement badges, gradient award cards.
- **Settings:** Category tabs (Navigation, Notifications, Gamification, Privacy, Tasks) with gradient backgrounds and per-feature toggles.

## Tech Stack

| Layer              | Technology |
|--------------------|------------|
| Language           | Kotlin 2.0 |
| UI                 | Jetpack Compose, Material 3 |
| Architecture       | MVVM, Repository pattern |
| DI                 | Koin |
| Persistence        | Room Database, DataStore |
| Background Work    | WorkManager |
| Serialization      | Kotlinx Serialization |
| Testing            | JUnit, Espresso, Compose UI tests |

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.1.1) or newer  
- JDK 17  
- Android SDK 30+  
- Git and Android Debug Bridge (ADB)

### Setup

```bash
git clone https://github.com/Metelci/YDS_Tracker.git
cd YDS_Tracker

# Assemble debug build
./gradlew :app:assembleDebug

# Install on a connected device (Linux/macOS)
./gradlew :app:installDebug

# Install on Windows
gradlew.bat :app:installDebug

# Launch activity
adb shell am start -n com.mtlc.studyplan/.MainActivity
```

Data is stored locally; deleting app data resets the study plan and achievements.

## Testing

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented Compose/UI tests
./gradlew :app:connectedDebugAndroidTest

# Generate lint and Detekt reports
./gradlew lintDebug detekt
```

Artifacts are exported to `app/build/reports`.

## Contributing

1. Fork the repository and create a feature branch.
2. Follow Kotlin coding conventions and Material 3 guidelines.
3. Keep security features intact; never weaken encryption or pinning.
4. Add tests for new logic or UI flows.
5. Submit a pull request with a summary of changes and screenshots where relevant.

Refer to `CONTRIBUTING.md` and `SECURITY_POLICY.md` for full details.

## Play Store Listing (Draft)

**Short Description**  
Structured 30-week YDS study planner with live exam countdowns, achievements, and offline security.

**Full Description**  
Road to YDS helps you prepare for the YDS and YÖKDİL English exams with confidence. Follow a guided 30-week plan built on Raymond Murphy methodology, stay motivated with streaks and achievements, and track the next exam without leaving the app.

Key features:
- Weekly and daily study plans that adapt to your progress and let you add custom tasks.
- Live countdown cards for upcoming YDS sessions, complete with registration window reminders.
- Achievement system, XP tracking, and refreshed awards hub to celebrate milestones.
- Unified settings with quick access to notifications, quiet hours, haptics, and privacy controls.
- Offline-first design with AES-256 encrypted storage, biometric login, and strict network security.

Download Road to YDS and keep your preparation on track all the way to exam day.

### Play Store What's New (v2.9.70)

- Weekly schedule day names are now fully localized using string resources.
- YDS fallback schedule now covers 2027-2028 with a stale-data warning if refresh fails.
- Dependency vulnerability scanning runs by default; FCM analytics prefs are stored encrypted and Koin logs stay quiet in release.

## License

This project is licensed under the Apache License 2.0. See `LICENSE` for details.

