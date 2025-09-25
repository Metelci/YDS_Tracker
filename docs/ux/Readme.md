# Road to YDS — UI/UX Task Pack


**Purpose**: Implement a sleek, fast, low‑distraction exam prep experience. This pack defines principles, tokens, IA, and focused build tasks.


**Order of execution**
1) Principles & tokens → 2) Navigation → 3) Today → 4) Reader → 5) Mock Exam → 6) Review → 7) Notifications → 8) Empty/Error → 9) Large‑screen → 10) Accessibility.


**Coding rules**
- Material 3 with dynamic color. Avoid custom colors unless necessary.
- Keep components stateless; ViewModels handle logic (MVI). Use `StateFlow`.
- Minimize recompositions. Stable keys, `remember`, `derivedStateOf`.
- Respect privacy: no PII in logs; keep analytics event names generic.