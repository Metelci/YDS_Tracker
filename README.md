# Road to YDS (StudyPlan)

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/Metelci/YDS_Tracker/actions)
[![Version](https://img.shields.io/badge/version-2.5.0-blue.svg)](https://github.com/Metelci/YDS_Tracker/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-30%2B-orange.svg)](https://android-arsenal.com/api?level=30)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org/)

Road to YDS is a 30‑week English study plan app to prepare for YDS/YÖKDİL and similar proficiency exams. It delivers structured, daily tasks across progressive phases (Red/Blue/Green Book approaches) with reminders, streaks, analytics, and strong on‑device security.

## Table of Contents
- [Getting Started](#getting-started)
  - [Quick Setup](#quick-setup)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Overview](#overview)
- [Features](#features)
- [Security](#security)
- [Tech Stack](#tech-stack)
- [App Id & Entry Point](#app-id--entry-point)
- [Build & Run](#build--run)
  - [IDE Setup](#ide-setup)
  - [CLI Setup](#cli-setup)
  - [Running Tests](#running-tests)
- [Troubleshooting](#troubleshooting)
- [Release](#release)
- [Project Structure](#project-structure)
  - [Extending Features](#extending-features)
- [Metrics & Analytics](#metrics--analytics)
- [Privacy](#privacy)
- [Contributing](#contributing)

## Getting Started

### Quick Setup

Get up and running in less than 5 minutes:

```bash
# Clone the repository
git clone https://github.com/Metelci/YDS_Tracker.git
cd YDS_Tracker

# Quick build and install (Linux/Mac)
./gradlew :app:installDebug && adb shell am start -n com.mtlc.studyplan/.MainActivity

# Quick build and install (Windows)
gradlew.bat :app:installDebug && adb shell am start -n com.mtlc.studyplan/.MainActivity
```

### Prerequisites

Before you begin, ensure you have the following installed:

- **Android Studio** (Giraffe 2022.3.1 or later)
- **Android SDK** (API level 30 or higher)
- **JDK 11** (configured automatically via Gradle toolchain)
- **Android device or emulator** running API 30+
- **Git** for version control

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Metelci/YDS_Tracker.git
   cd YDS_Tracker
   ```

2. **Open in Android Studio:**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory and select it
   - Wait for Gradle sync to complete

3. **Run the app:**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green play icon) in Android Studio
   - The app will build, install, and launch automatically

## Overview

### Core Features
- Phased program: Foundation → B1–B2 → C1 → Exam Camp
- Daily lesson tracking, streaks, achievements, and exam countdowns
- Works offline; sync‑free local storage by design
- Strong security: encryption at rest, biometric/PIN access, network hardening

See `google-play-store-description.md` and `CHANGELOG.md` for product details and release notes.

## Features

### Core Features
- **30-Week Plan:** Guided curriculum with daily tasks and progress.
- **Customizable Plans:** Hide/edit tasks and add your own per day. Opens from the top bar "Customize" action; changes persist on device and apply across the app.
- **Gamification:** Study streaks and achievement badges.
- **Reminders:** Exam date countdowns and smart notifications.
- **Analytics:** Progress stats and weak-area insights.
- **Offline First:** Full functionality without network.

### Screenshots
- Customize entry: `docs/screenshots/topbar_customize.png`
- Customize editor: `docs/screenshots/customize_editor.png`

Place your screenshots at the above paths to render inline in GitHub.

## Security

This project places privacy and security first. Highlights:

- **Encryption:** AES‑256‑GCM for sensitive data; hashing with SHA‑256 and PBKDF2.
- **Secure Storage:** Encrypted SharedPreferences and secure key handling.
- **Authentication:** BiometricPrompt, PIN/password, secure session management.
- **Network:** HTTPS only, certificate pinning, strict `NetworkSecurityConfig`.
- **Hardening:** Input validation/sanitization, secure memory wipe, safe logging.

### Security Documentation
- `SECURITY_USAGE_GUIDE.md` — How to use security utilities in app code.
- `SECURITY_INTEGRATION_GUIDE.md` — Integrating auth, storage, and network security.
- `SECURITY_POLICY.md` — Principles, standards, and operational policies.

Key implementation files include `app/src/main/java/com/mtlc/studyplan/security/` and `app/src/main/res/xml/network_security_config.xml`.

## Tech Stack

| Category | Libraries/Tools |
|----------|-----------------|
| **Language/UI** | Kotlin, Jetpack Compose, **Material 3** |
| **AndroidX** | Lifecycle, DataStore Preferences, WorkManager |
| **Security/Net** | AndroidX Security Crypto, OkHttp (+ logging), Biometric, kotlinx.serialization |
| **SDK** | Min SDK 30, Target SDK 36, Compile SDK 36 |

### Material 3 Migration Status
This app has been migrated from Material 2 to **Material 3** for modern UI consistency:
- ✅ **UI Components**: All screens use Material 3 components (NavigationBar, PrimaryTabRow, Material 3 color schemes)
- ✅ **Design System**: Consistent spacing tokens, shape tokens, and color schemes
- ✅ **Theming**: Dynamic Material 3 theming with proper light/dark mode support
- ⚠️ **Build Status**: Some compilation issues remain in legacy components (see `.claude/VALIDATION_REPORT.md`)

For detailed migration information, see the [Migration Documentation](#migration-documentation) section.

## App Id & Entry Point

- **Application Id:** `com.mtlc.studyplan` (`app/build.gradle.kts`)
- **Launcher Activity:** `.MainActivity` (`app/src/main/AndroidManifest.xml`)

## Build & Run

### IDE Setup
1. Open the project in Android Studio
2. Ensure "Compose" is enabled in the project (Gradle sync should handle this)
3. Select a device/emulator with API level 30 or higher
4. Click **Run** (green play button) to build and install

### CLI Setup
```bash
# Windows
gradlew.bat :app:installDebug
adb shell am start -n com.mtlc.studyplan/.MainActivity

# Linux/Mac
./gradlew :app:installDebug
adb shell am start -n com.mtlc.studyplan/.MainActivity
```

### Running Tests
```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumentation tests (requires device/emulator)
./gradlew :app:connectedDebugAndroidTest
```

### Code Quality & Guardrails
```bash
# Static analysis (when configured)
./gradlew detekt

# Code formatting check
./gradlew ktlintCheck

# Lint analysis
./gradlew lint
```

**Guardrails in place:**
- **Detekt rules**: Block legacy Material 2 imports (`androidx.compose.material`, `com.google.accompanist.swiperefresh`)
- **Build checks**: Prevent reintroduction of deprecated dependencies
- **Migration validation**: See `.claude/VALIDATION_REPORT.md` for current status

## Troubleshooting

### Common Build Issues

#### Gradle Sync Failures
- **Problem:** Gradle sync fails or takes too long
- **Solution:**
  ```bash
  ./gradlew clean
  # or on Windows:
  gradlew.bat clean
  ```
  Then restart Android Studio and try again.

#### JDK Version Issues
- **Problem:** Build fails with JDK-related errors
- **Solution:** Ensure JDK 11 is installed and configured. Android Studio usually handles this automatically via Gradle toolchain.

#### Android SDK Issues
- **Problem:** "SDK not found" or API level errors
- **Solution:**
  - Open Android Studio → SDK Manager
  - Install Android SDK Platform 30+ and Build Tools 35.0.0+
  - Set `ANDROID_HOME` environment variable if needed

#### Memory Issues
- **Problem:** Build fails due to out of memory errors
- **Solution:** Add to `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
  ```

#### Device/Emulator Connection
- **Problem:** ADB cannot find device or installation fails
- **Solutions:**
  - Enable USB debugging on device
  - Try `adb kill-server && adb start-server`
  - For emulators, ensure hardware acceleration is enabled

### Security-Related Issues

#### Certificate Pinning in Debug
- **Problem:** Network requests fail in debug builds
- **Solution:**
  1. Uncomment `CertificatePinRetriever.getCertificatePins()` in `MainActivity.kt`
  2. Run the app in debug mode
  3. Copy logged SHA-256 hashes to `network_security_config.xml` and `NetworkSecurityManager.kt`

#### BiometricPrompt Issues
- **Problem:** Biometric authentication not working
- **Solution:**
  - Ensure device has biometric hardware
  - Set up fingerprint/face unlock in device settings
  - Check that app has necessary permissions

### Performance Issues

#### Slow Build Times
- **Solutions:**
  - Enable Gradle build cache: `--build-cache`
  - Use parallel builds: `--parallel`
  - Consider using Gradle daemon: `--daemon`

#### App Crashes on Startup
- **Solutions:**
  - Check Logcat for crash details: `adb logcat | grep StudyPlan`
  - Verify minimum SDK version (API 30+)
  - Clear app data: `adb shell pm clear com.mtlc.studyplan`

## Release

- Versioning follows SemVer; see `CHANGELOG.md`.
- Current app module version: `versionName` and `versionCode` in `app/build.gradle.kts`.
- Release build uses ProGuard/R8 with minification enabled (`proguard-rules.pro`).

## Project Structure

- `app/src/main/java/com/mtlc/studyplan/` — App code (Compose UI, security, utils)
- `app/src/main/res/` — Resources (themes, XML, icons, network security)
- `app/src/main/AndroidManifest.xml` — App manifest
- `SECURITY_*.md` — Security usage, integration, and policy docs
- `google-play-store-description.md` — Store listing description
- `CHANGELOG.md` — Changelog

### Extending Features
- To add custom weeks in code, modify `PlanDataSource.planData` in `PlanDataSource.kt` (append new `WeekPlan`s).
- For end users, use the in-app "Customize" action (top bar) to hide/edit tasks or add custom tasks per day. Overrides are persisted in `DataStore` and merged at runtime.

Suggested future features:
- **Progress Export**: Implement progress export via `ProgressRepository` for PDF generation. Add a new composable in `MainActivity.kt` that queries the repository and uses a PDF library like iText or Android's PdfDocument.
- **Backup/Sync**: Encrypted cloud backup of progress (end-to-end encryption) for multi-device continuity.

## Privacy

- No third‑party data sharing; data stays on device.
- Encrypted at rest and protected by biometric/PIN.
- Network calls use TLS with certificate pinning when enabled.

## Migration Documentation

This project underwent a comprehensive migration from Material 2 to Material 3. All migration artifacts are preserved for reference:

### Migration Artifacts
- **`.claude/AUDIT_REPORT.md`** - Initial legacy UI audit and mapping
- **`.claude/MIGRATION.md`** - Detailed migration process and changes made
- **`.claude/REMOVAL_REPORT.md`** - Legacy code removal and cleanup documentation
- **`.claude/VALIDATION_REPORT.md`** - Final validation results and current status
- **`.claude/mapping.json`** - Component mapping from Material 2 to Material 3
- **`legacy-usage.csv`** - Complete inventory of legacy components found

### Design System
- **Design Tokens**: `app/src/main/java/com/mtlc/studyplan/ui/theme/DesignTokens.kt`
- **Shape Tokens**: `app/src/main/java/com/mtlc/studyplan/ui/theme/ShapeTokens.kt`
- **Spacing System**: `app/src/main/java/com/mtlc/studyplan/ui/theme/LocalSpacing.kt`

## Contributing

See [Repository Guidelines](AGENTS.md) for contributor onboarding, coding conventions, and review expectations.

### Code Style
- Follow Kotlin and Compose conventions (reference ktlint).
- Use consistent naming: camelCase for variables/functions, PascalCase for classes.
- Add comprehensive KDoc for public functions.
- **Material 3 Only**: Use Material 3 components exclusively - no Material 2 imports allowed.

### Material 3 Development Guidelines
- **Components**: Use `androidx.compose.material3.*` components only
- **Colors**: Use `MaterialTheme.colorScheme.*` for all colors
- **Typography**: Use `MaterialTheme.typography.*` for text styles
- **Spacing**: Use `LocalSpacing.current.*` for consistent spacing
- **Shapes**: Use `ShapeTokens.*` for consistent corner radiuses

### Testing
- All new features require unit tests (target 80% coverage).
- Use Compose UI testing for new screens.
- Test both light and dark Material 3 themes.

### Security Reviews
- Mandate lint scans for PRs affecting auth, storage, or networking.
- Security changes require review by project maintainer.

### Migration Guardrails
- **Forbidden imports**: `androidx.compose.material` (Material 2), `com.google.accompanist.swiperefresh`
- **Required reviews**: Any changes to UI components must use Material 3 equivalents
- **Build validation**: Detekt rules prevent legacy component reintroduction

Open issues and PRs are welcome. Please follow the security guidance in `SECURITY_POLICY.md` and reference `SECURITY_INTEGRATION_GUIDE.md` for any feature touching authentication, storage, or networking.

## Metrics & Analytics

This app implements lightweight, privacy-respecting analytics for internal instrumentation. There is no third-party SDK and no network transmission in release builds.

- Design: `Analytics.track(name, props)` enqueues a `WorkManager` one-off task handled by `AnalyticsWorker`.
- Privacy: No PII; small key/value props only. In release (non-debug) builds, `AnalyticsWorker` no-ops.
- Resilience: Using WorkManager ensures events persist across process death and run when the app resumes.

Events
- `app_open`
- `today_open`
- `session_start` / `session_complete` / `session_skip` (props: `id`)
- `mock_start` / `mock_submit` (props: `correct`, `total`, `avg_sec_per_q`)
- `reader_pref_change` (props: `font_sp`, `line_height`, `theme`)

Key files
- `app/src/main/java/com/mtlc/studyplan/metrics/Analytics.kt`
- `app/src/main/java/com/mtlc/studyplan/metrics/AnalyticsWorker.kt`
- Wiring examples in Today, Mock, and Reader screens.

Debugging
- In debug builds, events are logged with tag `Analytics`. Filter Logcat for `Analytics`.
- In release builds, events are disabled by design.

Testing
- Instrumentation: `./gradlew :app:connectedDebugAndroidTest` (requires a device/emulator). A basic accessibility test exists and analytics logs appear in Logcat during interactions.

