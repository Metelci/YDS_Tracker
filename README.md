# Road to YDS (StudyPlan) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Road to YDS is a 30‑week English study plan app to prepare for YDS/YÖKDİL and similar proficiency exams. It delivers structured, daily tasks across progressive phases (Red/Blue/Green Book approaches) with reminders, streaks, analytics, and strong on‑device security.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Security](#security)
- [Tech Stack](#tech-stack)
- [App Id & Entry Point](#app-id--entry-point)
- [Build & Run](#build--run)
  - [Prerequisites](#prerequisites)
  - [IDE Setup](#ide-setup)
  - [CLI Setup](#cli-setup)
  - [Quick Start](#quick-start)
  - [Running Tests](#running-tests)
- [Release](#release)
- [Project Structure](#project-structure)
  - [Extending Features](#extending-features)
- [Privacy](#privacy)
- [Contributing](#contributing)

## Overview

### Core Features
- Phased program: Foundation → B1–B2 → C1 → Exam Camp
- Daily lesson tracking, streaks, achievements, and exam countdowns
- Works offline; sync‑free local storage by design
- Strong security: encryption at rest, biometric/PIN access, network hardening

See `google-play-store-description.md` and `github-changelog.md` for product details and release notes.

## Features

### Core Features
- **30‑Week Plan:** Guided curriculum with daily tasks and progress.
- **Gamification:** Study streaks and achievement badges.
- **Reminders:** Exam date countdowns and smart notifications.
- **Analytics:** Progress stats and weak‑area insights.
- **Offline First:** Full functionality without network.

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
| **Language/UI** | Kotlin, Jetpack Compose, Material 3 |
| **AndroidX** | Lifecycle, DataStore Preferences, WorkManager |
| **Security/Net** | AndroidX Security Crypto, OkHttp (+ logging), Biometric, kotlinx.serialization |
| **SDK** | Min SDK 30, Target SDK 35, Compile SDK 36 |

## App Id & Entry Point

- **Application Id:** `com.mtlc.studyplan` (`app/build.gradle.kts`)
- **Launcher Activity:** `.MainActivity` (`app/src/main/AndroidManifest.xml`)

## App Id & Entry Point

- **Application Id:** `com.mtlc.studyplan` (`app/build.gradle.kts`)
- **Launcher Activity:** `.MainActivity` (`app/src/main/AndroidManifest.xml`)

## Build & Run

### Prerequisites
- Android Studio (Giraffe+ or later)
- Android SDK (API 30+)
- JDK 11 (toolchain configured in Gradle)
- Android device or emulator (API 30+)

### IDE Setup
1. Open the project in Android Studio.
2. Ensure "Compose" is enabled in the project (Gradle sync should handle this).
3. Select a device/emulator with API level 30 or higher.
4. Click **Run** (green play button) to build and install.

### CLI Setup
```bash
# Windows
gradlew.bat :app:installDebug
adb shell am start -n com.mtlc.studyplan/.MainActivity

# Linux/Mac
./gradlew :app:installDebug
adb shell am start -n com.mtlc.studyplan/.MainActivity
```

### Quick Start
```bash
# Clone and run in one command (Linux/Mac)
git clone [repo-url] && cd StudyPlan && ./gradlew :app:installDebug && adb shell am start -n com.mtlc.studyplan/.MainActivity

# Windows
git clone [repo-url] && cd StudyPlan && gradlew.bat :app:installDebug && adb shell am start -n com.mtlc.studyplan/.MainActivity
```

### Running Tests
```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumentation tests (requires device/emulator)
./gradlew :app:connectedDebugAndroidTest
```

**Troubleshooting**: If Gradle sync fails, run `./gradlew clean` and verify JDK 11 is installed. For security testing, enable debug mode and use the `CertificatePinRetriever` tool to generate real certificate pins.

### Security Setup
For production builds, generate real certificate pins using the `CertificatePinRetriever` tool in debug mode:
1. Uncomment `CertificatePinRetriever.getCertificatePins()` in `MainActivity.kt`.
2. Run the app in debug mode.
3. Copy the logged SHA-256 hashes and replace placeholders in `network_security_config.xml` and `NetworkSecurityManager.kt`.

## Testing

- Unit tests: `gradlew.bat testDebugUnitTest`
- Instrumentation tests (device/emulator): `gradlew.bat :app:connectedDebugAndroidTest`

Refer to `github-changelog.md` for test‑related updates and coverage improvements.

## Release

- Versioning follows SemVer; see `github-changelog.md`.
- Current app module version: `versionName` and `versionCode` in `app/build.gradle.kts`.
- Release build uses ProGuard/R8 with minification enabled (`proguard-rules.pro`).

## Project Structure

- `app/src/main/java/com/mtlc/studyplan/` — App code (Compose UI, security, utils)
- `app/src/main/res/` — Resources (themes, XML, icons, network security)
- `app/src/main/AndroidManifest.xml` — App manifest
- `SECURITY_*.md` — Security usage, integration, and policy docs
- `google-play-store-description.md` — Store listing description
- `github-changelog.md` — Changelog

### Extending Features
To add custom weeks, modify `PlanDataSource.planData` in `PlanDataSource.kt`. For example, append new `WeekPlan` objects to the list in the `planData` property.

Suggested new features:
- **Progress Export**: Implement progress export via `ProgressRepository` for PDF generation. Add a new composable in `MainActivity.kt` that queries the repository and uses a PDF library like iText or Android's PdfDocument.
- **Customizable Plans**: Allow users to modify study plans dynamically through a settings screen.

Extensibility roadmap:
- **Future**: User-customizable plans, encrypted cloud sync for progress backup (using Firebase or similar with end-to-end encryption).

## Privacy

- No third‑party data sharing; data stays on device.
- Encrypted at rest and protected by biometric/PIN.
- Network calls use TLS with certificate pinning when enabled.

## Contributing

### Code Style
- Follow Kotlin and Compose conventions (reference ktlint).
- Use consistent naming: camelCase for variables/functions, PascalCase for classes.
- Add comprehensive KDoc for public functions.

### Testing
- All new features require unit tests (target 80% coverage).
- Use Compose UI testing for new screens.

### Security Reviews
- Mandate lint scans for PRs affecting auth, storage, or networking.
- Security changes require review by project maintainer.

Open issues and PRs are welcome. Please follow the security guidance in `SECURITY_POLICY.md` and reference `SECURITY_INTEGRATION_GUIDE.md` for any feature touching authentication, storage, or networking.

