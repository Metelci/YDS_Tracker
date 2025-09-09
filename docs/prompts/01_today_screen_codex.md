# TASK: Today Screen — Session Cards & Focus Mode


## Context
App: Road to YDS (Compose M3). We need a sleek **Today** screen with actionable session cards and a distraction‑free Focus Mode.


## Deliverables
- `TodayScreen()` composable with:
- **SessionCard** list: title, section chip (Reading/Grammar/Vocab/Translation), est. time, difficulty, streak badge.
- Actions: Start, Skip, Reschedule (overflow).
- Pull‑to‑refresh and swipe‑to‑complete (left=complete, right=reschedule).
- `FocusModeScreen()` with large timer, pause/resume, completion confetti (subtle), and exit confirmation.
- ViewModel: `TodayViewModel` (MVI) with intents: Load, StartSession(id), Complete(id), Skip(id), Reschedule(id, date/time).


## Non‑goals
- No server calls. No new DB schema. Use existing repositories & sample data if needed.


## UX requirements
- Minimal chrome; one FAB "Start next" if list not empty.
- Show **time left today** and a tiny **adherence** pill.
- Respect DND and power saver: tone down haptics.


## Tech constraints
- Kotlin, Compose M3, Hilt.
- State with `MutableStateFlow` + reducer; UI subscribes via `collectAsStateWithLifecycle()`.
- Accessibility: contentDescr, focus order, hit targets ≥ 48dp.


## Data Contract (stub)
```kotlin
data class SessionUi(
val id: String,
val title: String,
val section: String, // Reading, Grammar, Vocab, Translation
val estMinutes: Int,
val difficulty: Int, // 1..5
val isCompleted: Boolean,
)