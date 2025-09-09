# Road to YDS - Version History

All notable changes to the Road to YDS application will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6.0] - 2025-09-09
### Added
- Configurable Study Plans: hide/show built‑in tasks, edit task titles/details, and add custom tasks per day.
- “Customize” action in the top app bar to quickly open the editor.
- Edit dialog for updating task text inline.

### Changed
- Main plan view now merges and reflects user overrides instantly.
- Overrides persist on device using DataStore (JSON) and survive app restarts.
- Internal: Added DataStore‑backed overrides models, merge repository, and simple editor UI.

### Fixed
- Minor UI polish and stability improvements in plan rendering.

## [1.5.4] - 2025-08-24
### Added
- Enhanced security logging with improved sanitization
- Additional input validation for API communications
- Comprehensive testing suite for security features

### Changed
- Improved localization accuracy for both Turkish and English versions
- Optimized memory usage during study plan rendering
- Updated security dependencies to latest versions
- Enhanced UI responsiveness on smaller screen sizes
- Refined progress tracking algorithm for better accuracy

### Fixed
- Minor UI alignment issues on various screen sizes
- Notification timing accuracy for exam reminders
- Data persistence improvements for progress tracking
- Memory leak in notification service
- Occasional crashes during app startup
- Progress tracking accuracy improvements

## [1.5.3] - 2025-08-24
### Added
- Enhanced security logging with improved sanitization
- Additional input validation for API communications

### Changed
- Improved localization accuracy for both Turkish and English versions
- Optimized memory usage during study plan rendering
- Updated security dependencies to latest versions

### Fixed
- Minor UI alignment issues on various screen sizes
- Notification timing accuracy for exam reminders
- Data persistence improvements for progress tracking

## [1.5.0] - 2025-07-15
### Added
- Complete localization support for English language
- Enhanced exam calendar with upcoming YDS/YÖKDİL dates for 2026
- New achievement badges for milestone completions
- Dark mode support for comfortable nighttime studying

### Changed
- Redesigned user interface with improved Material Design 3 components
- Enhanced performance of progress tracking algorithms
- Updated study plan content based on latest exam patterns

### Fixed
- Biometric authentication reliability improvements
- Data synchronization issues in offline mode
- Crash fixes related to notification handling

## [1.4.2] - 2025-05-22
### Added
- Enhanced data encryption for user progress
- Certificate pinning for API security
- Detailed security audit logging

### Changed
- Improved AES-256-GCM encryption implementation
- Optimized database queries for faster loading
- Updated network security configuration

### Fixed
- Memory leak in notification service
- Occasional crashes during app startup
- Progress tracking accuracy improvements

## [1.4.1] - 2025-04-10
### Added
- Biometric authentication for app access
- Secure storage for sensitive user data
- Advanced network security features

### Changed
- Refactored security architecture for better protection
- Improved session management
- Enhanced API communication security

### Fixed
- Login state persistence issues
- Data corruption in offline mode
- Security vulnerability in data transmission

## [1.4.0] - 2025-03-05
### Added
- Comprehensive security framework implementation
- AES-256 encryption for all stored data
- Secure SharedPreferences implementation
- Input sanitization for all user data

### Changed
- Complete security overhaul for data protection
- Improved error handling in critical functions
- Enhanced data validation procedures

### Fixed
- Data privacy compliance issues
- Storage security vulnerabilities
- Authentication mechanism improvements

## [1.3.1] - 2025-01-20
### Added
- Exam application deadline notifications
- OSYM website integration for official information
- Weekly progress summary emails (optional)

### Changed
- Improved notification system reliability
- Enhanced exam calendar accuracy
- Updated study plan content for 2025 exams

### Fixed
- Notification delivery timing issues
- Calendar synchronization problems
- Minor UI display inconsistencies

## [1.3.0] - 2024-12-05
### Added
- Exam camp phase with full practice tests
- Detailed exam analysis features
- Performance comparison with previous attempts
- Weak point identification system

### Changed
- Restructured study plan for final 4 weeks
- Enhanced progress tracking algorithms
- Improved user interface for test sections

### Fixed
- Progress calculation accuracy
- Data persistence during app updates
- UI responsiveness on lower-end devices

## [1.2.2] - 2024-10-18
### Added
- Advanced grammar topics for C1 level
- Additional vocabulary building exercises
- Progress comparison with weekly goals

### Changed
- Expanded Green Book study phase to 8 weeks
- Improved task descriptions with more details
- Enhanced gamification elements

### Fixed
- Achievement unlocking mechanism
- Streak counting accuracy
- Data backup reliability

## [1.2.1] - 2024-09-02
### Added
- B1-B2 development phase with Blue Book approach
- Intermediate level grammar exercises
- Pronunciation practice modules

### Changed
- Extended development phase to 10 weeks
- Improved daily task variety
- Enhanced progress visualization

### Fixed
- Task completion synchronization
- Calendar display issues
- Notification scheduling accuracy

## [1.2.0] - 2024-07-15
### Added
- Multi-language support (Turkish/English)
- Enhanced gamification with achievements
- Detailed progress statistics
- Social sharing features

### Changed
- Complete UI redesign with Material Design
- Improved performance and loading times
- Enhanced data persistence

### Fixed
- Various bug fixes and stability improvements
- Memory management optimizations
- User experience enhancements

## [1.1.1] - 2024-05-30
### Added
- Study streak tracking
- Daily reminder notifications
- Progress percentage visualization

### Changed
- Improved task completion tracking
- Enhanced user interface responsiveness
- Optimized battery usage

### Fixed
- Occasional crashes on older devices
- Data synchronization issues
- Notification delivery problems

## [1.1.0] - 2024-04-12
### Added
- 30-week comprehensive study plan
- Daily task tracking system
- Basic progress monitoring
- Simple gamification elements

### Changed
- Improved user onboarding experience
- Enhanced data storage reliability
- Better offline functionality

### Fixed
- Initial stability issues
- UI display problems on various screen sizes
- Minor performance improvements

## [1.0.0] - 2024-03-01
### Added
- Initial release of Road to YDS
- 8-week foundational grammar program
- Basic task tracking functionality
- Simple progress visualization
- Core study plan implementation

[1.5.4]: https://github.com/mtlc/studyplan/compare/v1.5.3...v1.5.4
[1.5.3]: https://github.com/mtlc/studyplan/compare/v1.5.0...v1.5.3
[1.5.0]: https://github.com/mtlc/studyplan/compare/v1.4.2...v1.5.0
[1.4.2]: https://github.com/mtlc/studyplan/compare/v1.4.1...v1.4.2
[1.4.1]: https://github.com/mtlc/studyplan/compare/v1.4.0...v1.4.1
[1.4.0]: https://github.com/mtlc/studyplan/compare/v1.3.1...v1.4.0
[1.3.1]: https://github.com/mtlc/studyplan/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/mtlc/studyplan/compare/v1.2.2...v1.3.0
[1.2.2]: https://github.com/mtlc/studyplan/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/mtlc/studyplan/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/mtlc/studyplan/compare/v1.1.1...v1.2.0
[1.1.1]: https://github.com/mtlc/studyplan/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/mtlc/studyplan/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/mtlc/studyplan/releases/tag/v1.0.0
