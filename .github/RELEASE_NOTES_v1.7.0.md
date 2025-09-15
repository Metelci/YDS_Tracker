# StudyPlan v1.7.0

Highlights
- Flexible plan duration: Set a start date and weeks, or use an end date or total months to auto‑calculate. Week/day rows now show calendar dates.
- New lesson screen: “Start” on Today opens a dedicated lesson view with a back button and loading state.

Improvements
- Customize Plan: Back/Cancel actions, Info help dialog, and date labels for weeks and days.
- Navigation: Start both shows a snackbar and navigates to the lesson. About button reliably opens the sheet.
- UI cleanup: Replaced deprecated components; better RTL back arrow.

Fixes
- Unresponsive Start and Info buttons.
- Localization gaps; lint now passes.

Dev Notes
- Added PlanSettingsStore and proportional plan scaling with remapped week IDs.
- Lesson screen scaffold for future interactive content.

Upgrade Notes
- Changing total duration remaps week numbers/IDs; review any custom overrides after large schedule changes.

