# TASK: Mock Exam — 80Q Shell


## Goal
Create full exam container with section map, countdown, review flags.


## Deliverables
- `MockExamScreen()` with pager of questions, global countdown 180m.
- Section map rail; mark‑for‑review; quick jump.
- Submit flow → `MockResult` summary (correct, time/Q, per‑section).


## Constraints
- Timer survives config change; warns on exit; stores snapshot in `DataStore`.


## Acceptance
- Keyboard navigation support; TalkBack labels for options.


## Commit msg
`feat(mock): 80Q exam shell with countdown and review flags`