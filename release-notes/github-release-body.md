# What's New (2.5.0)
**Version:** 2.5.0

## Highlights
- Unified settings control center with instant notification, theme, offline, and gamification toggles
- Reimagined Home, Tasks, Progress, and Social tabs with live badges, streak coaching, and responsive micro-interactions
- Calendar sync + upgraded notifications with snooze/mute actions and smarter scheduling safeguards

## Added
- Shared Hilt-backed app architecture, navigation badges, and shared view model powering cross-screen state
- CalendarWorker + settings-managed calendar sync controls, offline queue, and WorkManager observers
- Notification manager with multiple channels, actionable intents, and StudyPlanApplication wiring

## Improved
- Home dashboard surfaces daily goals, offline indicators, smart suggestions, and XP tracking at a glance
- Tasks and Progress screens pick up analytics tabs, skill breakdowns, achievement celebrations, and richer visuals
- Settings UI, loading components, and theming adopt consistent Material 3 styling and haptic-aware feedback

## Fixed
- Navigation state, badge counts, and streak indicators now stay in sync after backgrounding or quick switches
- Notification scheduling, calendar sync recovery, and offline retries prevent silent drops on flaky networks
- Settings persistence, loading overlays, and feedback toasts handle error states without stalling the UI

## Tech
- VersionCode: 42
- VersionName: 2.5.0

---

**Full Changelog**: See CHANGELOG.md
