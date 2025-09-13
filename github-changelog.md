# StudyPlan Changelog

## v1.9.1 (September 2025)

### Improvements
- Today: Pull-to-refresh now binds to the actual loading state for accurate feedback.
- Today: Added a “Planned vs. Budget” header powered by Plan Settings, showing planned minutes, daily budget, delta, and a compact progress bar.

### Developer Notes
- Reads daily minutes from `PlanSettingsStore.settingsFlow` based on current weekday.
- `TodayViewModel` toggles `isLoading` while loading to drive the refresh indicator.

## v1.7.0 (September 2025)

### Features
- Flexible plan duration: Choose a start date and total weeks, or provide an end date or total months to auto‑calculate weeks. Week cards and day rows now display calendar dates.
- Lesson screen: Tapping "Start" in Today opens a dedicated lesson view with back navigation and a brief loading state.

### Improvements
- Customize Plan editor: Added Back and Cancel actions, plus an Info dialog explaining hide/edit/add. Date labels added to weeks/days.
- Navigation polish: Today’s Start action both gives snackbar feedback and navigates into the lesson.
- About availability: Info button on the main plan view reliably opens the About sheet (not restricted to the editor view).
- UI cleanup: Replaced deprecated `Divider` with `HorizontalDivider` and used `Icons.AutoMirrored.Filled.ArrowBack` for RTL support.

### Fixes
- Start buttons unresponsive: Start now triggers navigation and shows feedback.
- Info icon unresponsive on main screen: About sheet is shown from anywhere in the plan screen.
- Localization & lint: Added missing Turkish strings and proper escaping; lint passes without MissingTranslation errors.

### Developer Notes
- Added `PlanSettingsStore` (DataStore) and integrated with `PlanRepository`; base plan is proportionally remapped to any target length while preserving pacing.
- Task IDs are remapped to the new week numbers; week titles recompute with localized phase labels.
- Lesson screen is a placeholder scaffold for future interactive content integration.

### Migration considerations
- Changing total duration remaps week numbers/IDs. Existing per‑task overrides remain, but if you drastically change duration, some overrides may no longer align as expected. Consider resetting/adjusting overrides after large schedule changes.

## v1.6.1 (September 2025)

### Architecture Improvements
- Implemented Navigation Compose framework for improved screen navigation
- Created centralized AppNavHost for navigation management
- Moved navigation logic out of activities into composable-based navigation

### Bug Fixes & Technical Improvements
- Removed deprecated manifest package attribute in favor of Gradle namespace
- Fixed Activity configuration to use proper themes
- Made MainActivity the primary launcher activity
- Enhanced app stability and performance

## v1.6.0 (Previous version)

[Previous changelog content...]
## v1.8.0

Date: 2025-09-11

Highlights
- Accessibility: content descriptions, larger touch targets, reduced motion support (system setting aware). Added Accessibility Test Rule.
- Metrics: lightweight in-app analytics wrapper (no PII, no network in release). Events: app_open, today_open, session_start/complete/skip, mock_start/submit, reader_pref_change.
- UI Polish: Material 3 style pass with centralized spacing tokens and standardized card radius/elevations across Today, Reader, Mock, Review, Practice, Progress, and shared components.

Details
- a11y: Provided `LocalReducedMotion`, disabled nonessential animations, semantics improvements.
- metrics: `Analytics.track()` via `WorkManager`; logs only in debug; resilient to process death.
- theme: Added `Spacing` tokens and `Elevations`. Replaced ad-hoc paddings with 8-pt grid values.
- dialogs: Tokenized paddings for EditTaskDialog and PlanSettingsDialog.
## v1.8.1

Date: 2025-09-11

Highlights
- New: First‑run Welcome screen with concise onboarding and a discreet Material icon.
- Fix: Progress screen crash due to multiple DataStores for the same file (now uses application context).
- Polish: Tokenized dialog paddings and refined welcome copy and visuals.
## 1.9.0 — Time-aware scheduling, Home dashboard, boosters

Highlights
- Time-aware plan: align to start weekday and exact exam date; plan is trimmed to the precise number of days.
- Flexible study slots: set minutes per day; scheduler distributes tasks into those budgets, prioritizing high‑yield items.
- Home tab: unified dashboard with exam countdown, today’s tasks, streak, and overall progress.
- Persistent Quick Actions: global FAB for “Start Session” and “Add Quick Note/Flashcard”.
- Progress now uses the effective (post‑compression) plan for accurate percentages.
- Auto-fill End Date from next exam; date format presets and locale support.
- Log time + correctness after tasks; booster suggestions for weak categories with one‑tap add.

Changes
- Added PlanSettings fields for per‑day minutes and custom date format.
- Plan pipeline: start weekday alignment → end date trimming → availability‑based weekly packing → overrides merge.
- UI: dates shown beside day names; presets for date formats.
- Navigation: bottom tabs (Home, Tasks, Progress, Settings).
- New screens: Home dashboard, Quick Note, Settings (with Plan Settings launcher).
- Today screen FAB is opt‑in to avoid overlapping with global FAB.

Internal
- DataStore: lightweight task logs (time, correctness, category) with simple encoding.
- Weakness detection computes incorrect rates per category.
