# StudyPlan Changelog

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
