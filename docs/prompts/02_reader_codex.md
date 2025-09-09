# TASK: Reader — Controls, WPM Bar, Glossary


## Context
Reading is core. Implement a comfortable reader with in‑place controls and metrics.


## Deliverables
- `ReaderScreen(passage: PassageUi)` with:
- Font size/line height/theme toggles (bottom sheet or top actions).
- **WPM bar** and time‑on‑passage indicator.
- Long‑press word → definition sheet; add to vocab list.
- Connector highlights toggle (however simple at first).


## UX
- Default to bodyLarge; min/max sizes accessible.
- Persist preferences via `DataStore`.
- Night/sepia themes; follow system dark.


## Tech
- Compose M3; avoid blocking recompositions on timer (use `LaunchedEffect`).
- Provide fake dictionary hookup with interface `GlossaryRepo`.


## Acceptance
- Scroll perf smooth on long text.
- Controls discoverable yet out of the way; no overlap with system bars.


## Commit msg
`feat(reader): typography controls, WPM bar, inline glossary`