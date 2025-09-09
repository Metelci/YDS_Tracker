# TASK: Smart Notifications + Deep Links


## Goal
Actionable, non‑spammy reminders that deep‑link into tasks.


## Deliverables
- WorkManager workers scheduling inside user window.
- Notification actions: Start Sprint, Snooze 30m, Mute today.
- Deep links: yds://today, yds://mock/start.


## Acceptance
- No alerts in DND; respects quiet hours from `DataStore`.


## Commit msg
`feat(notif): smart reminders with deep links and quiet hours`