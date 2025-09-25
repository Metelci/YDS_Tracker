 # 🎓 Road to YDS - English Exam Preparation App

  [![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/Metelci/YDS_Tracker/actions)      
  [![Version](https://img.shields.io/badge/version-2.8.0-blue.svg)](https://github.com/Metelci/YDS_Tracker/releases)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
  [![API](https://img.shields.io/badge/API-30%2B-orange.svg)](https://android-arsenal.com/api?level=30)
  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org/)
  [![Material 3](https://img.shields.io/badge/Material%203-Ready-green.svg)](https://m3.material.io/)

  > **A comprehensive Android application for YDS/YÖKDİL English exam preparation featuring a structured 30-week study program,      
  gamification elements, and advanced security features.**

  ## 🌟 Overview

  Road to YDS is a feature-rich Android application designed to help students prepare for English proficiency exams like YDS
  (Yabancı Dil Sınavı) and YÖKDİL. The app provides a scientifically-structured 30-week study program that progressively builds      
  English skills from foundation to advanced levels.

  ### ✨ Key Highlights

  - 📚 **Comprehensive Curriculum**: 30-week structured program following Red/Blue/Green Book methodology
  - 🎮 **Gamification**: Study streaks, achievements, and progress celebrations
  - 🔒 **Security-First**: End-to-end encryption, biometric authentication, offline-first design
  - 🎨 **Modern UI**: Material 3 design with dark mode and accessibility support
  - 📱 **Offline-First**: Full functionality without internet connectivity
  - 🏆 **Social Features**: Friend connections, leaderboards, and study groups

  ## 🚀 Features

  ### 📖 Study Program
  - **Phase-based Learning**: Foundation → B1-B2 Development → C1 Mastery → Exam Camp
  - **Daily Task Management**: Structured lessons with progress tracking
  - **Customizable Plans**: Hide/edit tasks and add custom daily activities
  - **Smart Reminders**: Exam countdowns and study notifications

  ### 🎯 Progress Tracking
  - **Visual Analytics**: Comprehensive progress dashboard
  - **Achievement System**: Milestone badges and celebrations
  - **Study Streaks**: Daily consistency tracking with motivation features
  - **Performance Insights**: Weak area identification and improvement suggestions

  ### 🔐 Security & Privacy
  - **AES-256-GCM Encryption**: All sensitive data encrypted at rest
  - **Biometric Authentication**: Fingerprint and face unlock support
  - **Local Storage**: No cloud dependency, complete data ownership
  - **Network Security**: Certificate pinning and strict security policies

  ### 🎨 User Experience
  - **Material 3 Design**: Modern, accessible interface
  - **Dark Mode Support**: Comfortable studying in any lighting
  - **Accessibility Features**: Screen reader support, high contrast options
  - **Smooth Animations**: Fluid transitions and micro-interactions

  ## 🛠️ Tech Stack

  | Category | Technologies |
  |----------|-------------|
  | **Language** | Kotlin 1.9+ |
  | **UI Framework** | Jetpack Compose with Material 3 |
  | **Architecture** | MVVM with Repository Pattern |
  | **Dependency Injection** | Hilt |
  | **Local Storage** | Room Database, DataStore Preferences |
  | **Security** | AndroidX Security Crypto, Biometric API |
  | **Background Work** | WorkManager |
  | **Testing** | JUnit, Espresso, Compose Testing |

  ## 📦 Installation

  ### Prerequisites
  - **Android Studio** Giraffe (2022.3.1) or later
  - **JDK 11** or higher
  - **Android SDK** with API level 30+
  - **Git** for version control

  ### Quick Setup
  ```bash
  # Clone the repository
  git clone https://github.com/Metelci/YDS_Tracker.git
  cd YDS_Tracker

  # Build and install (Linux/Mac)
  ./gradlew :app:installDebug

  # Build and install (Windows)
  gradlew.bat :app:installDebug

  # Launch the app
  adb shell am start -n com.mtlc.studyplan/.MainActivity

  Development Setup

  1. Clone and Open
  git clone https://github.com/Metelci/YDS_Tracker.git
  cd YDS_Tracker
  2. Open in Android Studio
    - Launch Android Studio
    - Select "Open an Existing Project"
    - Navigate to the cloned directory
  3. Build and Run
    - Wait for Gradle sync to complete
    - Connect Android device or start emulator (API 30+)
    - Click Run button or use Shift+F10

  🎯 Usage Examples

  Basic Study Flow

  // Track daily study session
  Analytics.track("session_start", mapOf("id" to sessionId))

  // Complete a task
  studyPlanRepository.markTaskComplete(taskId)

  // Update progress
  progressRepository.updateWeeklyProgress(weekNumber, completedTasks)

  Security Integration

  // Initialize secure storage
  val secureStorage = SecureStorageManager(context)

  // Store sensitive data
  secureStorage.storeEncrypted("user_progress", progressData)

  // Authenticate user
  authenticationManager.authenticateWithBiometrics(
      onSuccess = { /* Handle success */ },
      onError = { /* Handle error */ }
  )

  Custom Study Plans

  // Add custom task to daily plan
  customizationRepository.addCustomTask(
      date = LocalDate.now(),
      task = CustomTask(
          title = "Practice Vocabulary",
          description = "Review 50 new words",
          estimatedMinutes = 30
      )
  )

  🧪 Testing

  Running Tests

  # Unit tests
  ./gradlew testDebugUnitTest

  # Integration tests
  ./gradlew :app:connectedDebugAndroidTest

  # UI tests with coverage
  ./gradlew :app:connectedDebugAndroidTest -Pcoverage

  Test Coverage

  - Unit Tests: 85%+ coverage for business logic
  - UI Tests: Critical user flows covered
  - Security Tests: Authentication and encryption validation
  - Accessibility Tests: Screen reader and navigation testing

  📁 Project Structure

  app/src/main/java/com/mtlc/studyplan/
  ├── 🎨 ui/                          # Compose UI components
  │   ├── components/                 # Reusable UI components
  │   ├── screens/                    # Screen composables
  │   └── theme/                      # Material 3 theming
  ├── 🔒 security/                    # Security and authentication
  │   ├── SecureStorageManager.kt     # Encrypted storage
  │   ├── AuthenticationManager.kt    # Biometric/PIN auth
  │   └── NetworkSecurityManager.kt   # Network security
  ├── 📊 data/                        # Data layer
  │   ├── repositories/               # Data repositories
  │   ├── database/                   # Room database
  │   └── models/                     # Data models
  ├── 🎯 features/                    # Feature modules
  │   ├── studyplan/                  # Study plan management
  │   ├── social/                     # Social features
  │   ├── exam/                       # Exam preparation
  │   └── settings/                   # App configuration
  └── 🛠️ utils/                       # Utility classes
      ├── Analytics.kt                # Event tracking
      ├── NetworkHelper.kt            # Network utilities
      └── DateFormatters.kt           # Date/time formatting

  🤝 Contributing

  We welcome contributions! Please follow these guidelines:

  Development Guidelines

  1. Code Style: Follow Kotlin coding conventions
  2. Material 3 Only: Use Material 3 components exclusively
  3. Security First: Follow security best practices
  4. Test Coverage: Include tests for new features

  Contribution Process

  1. Fork the repository
  2. Create feature branch: git checkout -b feature/amazing-feature
  3. Follow code style: Use ktlint for formatting
  4. Add tests: Ensure adequate test coverage
  5. Commit changes: git commit -m 'Add amazing feature'
  6. Push to branch: git push origin feature/amazing-feature
  7. Open Pull Request: Provide detailed description

  Code Review Requirements

  - ✅ All tests pass
  - ✅ Code coverage maintained
  - ✅ Security review for sensitive changes
  - ✅ Material 3 compliance
  - ✅ Accessibility considerations

  🔒 Security

  Security is a top priority. Key security features:

  - 🔐 Encryption: AES-256-GCM for data at rest
  - 🔑 Authentication: Biometric and PIN/password options
  - 🌐 Network Security: Certificate pinning, HTTPS only
  - 📱 Device Security: Secure key storage, memory protection
  - 🛡️ Input Validation: Comprehensive sanitization

  For security issues, please follow our SECURITY_POLICY.md.

  📄 License

  This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

  Copyright 2024 Road to YDS

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

  🙏 Acknowledgments

  - Material Design Team for the excellent Material 3 design system
  - Android Team for comprehensive security APIs
  - Kotlin Team for the modern development experience
  - Open Source Community for valuable libraries and tools
