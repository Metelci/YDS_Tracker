
# StudyPlan – Grandmaster Prompt Pack (Android • Kotlin • Jetpack Compose • Material3)

These are copy‑paste “job cards” for your **VS Code Codex / GPT‑5 High** agent.  
Each card has a *Goal → Steps → Deliverables → Acceptance Criteria*.  
Run them in order for a clean, reviewable migration to the new Lovable-style UI you showed in the 19 screenshots.

> Assumptions: Android, Kotlin, Jetpack Compose, Material 3, Turkish locale. Do **not** change package names or break public APIs. Keep commits small and runnable.

---

## CARD 0 — Project Audit & Safety Rails (run first)
**Goal**: Prepare a safe workspace and baseline previews.
**Steps**
1) Create branch `feat/lovable-ui`.
2) List current Compose/Material/Accompanist/Lottie versions; upgrade to latest stable if trivial.
3) Generate *before* screenshots via Compose Previews for Home, Tasks, Progress, Social, Settings.
4) Set up ktlint + detekt if missing; enable `-Xexplicit-api=strict` only for new modules.
**Deliverables**
- Branch + `docs/CHANGELOG_LovableUI.md` (seeded).
- `preview/` Previews for 5 top-level screens.
**Acceptance**
- App builds; previews compile; CI/lint pass.

---

## CARD 1 — Crosswalk the Lovable Design Tokens → Compose Theme
**Goal**: Port Lovable’s semantic tokens (HSL palette, surfaces, radii, typography) into Compose.
**Steps**
1) Create `ui/theme/Tokens.kt` with helpers:
```kotlin
fun hsl(h: Int, s: Int, l: Int): Color =
    Color.hsl(h.toFloat(), s / 100f, l / 100f)
```
2) Add **ColorTokens** object (primary, secondary, tertiary, surfaces, success/warning/error, ring, muted, etc.).
3) Map tokens to `lightColorScheme`/`darkColorScheme` in `ColorSchemes.kt`.
4) Define `Shapes.kt` with default corner radius **12.dp** (cards/buttons 16.dp, chips 20.dp).
5) Create `Typography.kt` matching display/headline/title/body/label sizes. Use **Google Sans Display** fallback or system font if missing.
6) Implement `StudyPlanTheme` with parameters `(darkTheme: Boolean, dynamicColor: Boolean=false)`; expose semantic extension colors when needed (e.g., `success`, `warning`).
**Deliverables**
- `ui/theme/*` files + theme previews.
**Acceptance**
- No hard-coded hex colors; only `hsl()` or Material tokens.
- Light/Dark parity; text contrast >= 4.5 for body text.


### Design Tokens — Color Palette (from `CODE_SPECIFICATIONS.MD`)
Token | Format | Value
---|---|---
`primary` | raw | 199 92% 73%
`primary-container` | raw | 199 70% 85%
`secondary` | raw | 0 0% 15%
`secondary-container` | raw | 122 39% 82%
`tertiary` | raw | 14 100% 78%
`tertiary-container` | raw | 14 100% 88%
`background` | raw | 0 0% 7%
`surface` | raw | 0 0% 100%
`surface-variant` | raw | 0 0% 96%
`border` | raw | 0 0% 15%
`input` | raw | 0 0% 15%
`ring` | raw | 199 92% 73%
`muted` | raw | 0 0% 15%
`accent` | raw | 0 0% 15%
`success` | raw | 122 39% 49%
`success-container` | raw | 122 39% 82%
`warning` | raw | 45 100% 51%
`streak-fire` | raw | 14 100% 57%
`achievement-bronze` | raw | 30 67% 47%
`achievement-silver` | raw | 0 0% 75%
`achievement-gold` | raw | 51 100% 50%
`achievement-platinum` | raw | 240 12% 85%
`accent-foreground` | raw | 0 0% 98%
`card` | raw | 0 0% 10%
`card-foreground` | raw | 0 0% 98%
`destructive` | raw | 0 63% 31%
`destructive-foreground` | raw | 0 0% 98%
`foreground` | raw | 0 0% 98%
`muted-foreground` | raw | 0 0% 64%
`popover` | raw | 0 0% 10%
`popover-foreground` | raw | 0 0% 98%
`primary-container-foreground` | raw | 199 100% 20%
`primary-foreground` | raw | 0 0% 7%
`radius` | raw | 0.75rem
`secondary-container-foreground` | raw | 122 100% 15%
`secondary-foreground` | raw | 0 0% 98%
`surface-container` | raw | 0 0% 94%
`surface-container-high` | raw | 0 0% 92%
`tertiary-container-foreground` | raw | 14 100% 25%

---

## CARD 2 — Foundations: Spacing, Elevation, Motion
**Goal**: Recreate spacing scale, elevations, and micro‑interactions from design.
**Steps**
1) Add `Dimens.kt` with 4/8/12/16/24/32 spacing.
2) Create motion constants and helper modifiers:
   - `breathing()` (idle) using infinite `animateFloatAsState` on scale.
   - `celebrate()` (success) bounce keyframes.
   - `fireEffect()` (streak) subtle hue/scale oscillation.
3) Elevation set: 0, 1, 3, 6 for surface/raised/elevated/overlay.
**Deliverables**
- `ui/foundation/Motion.kt`, `Dimens.kt`, `Elevation.kt`.
**Acceptance**
- Animations are subtle and `prefersReducedMotion` respected (use `isSystemInDarkTheme()` + user setting flag).

---

## CARD 3 — Component Kit (shadcn‑style in Compose)
**Goal**: Build reusable components matching screenshots.
**Steps**
1) **AppCard**: Elevated container with optional header/subtitle and progress slot.
2) **StatChip**: Filled/outlined variants (e.g., “85%”, “2x”). Rounded 20.dp; small icon support.
3) **BadgeXP**: Small badge for points (⭐ 50 XP).
4) **SegmentedTabs**: 3‑state tabs for Daily/Weekly/Custom and Overview/Skills/Awards/AI.
5) **LinearProgress**: Primary track with muted background; label option.
6) **RingProgress**: Canvas‑based circular progress with center text; supports `celebrate` at 100%.
7) **ToggleRow**: Settings row with title, subtitle, and `Switch` on the right.
8) **BottomNavBar**: Icon+label, floating highlight for selected tab.
**Deliverables**
- `ui/components/*` with previews for each.
**Acceptance**
- No per‑screen one‑off styles; components are reusable and themed.

---

## CARD 4 — Navigation (Bottom Tabs)
**Goal**: Five‑tab bottom navigation (Ana Sayfa, Görevler, İlerleme, Sosyal, Ayarlar).
**Steps**
- Implement `StudyBottomNav` using `NavigationBar` with icons & Turkish labels.
- Provide `ariaLabel`/`contentDescription` for accessibility.
**Acceptance**
- Active tab has primary tint & rounded backdrop; min touch target 48dp.

---

## CARD 5 — Home Screen
**Goal**: Match the dashboard: greeting, today %, days to YDS, streak, exam card, AI suggestion, today’s tasks list.
**Steps**
- Layout with scrollable column; cards: Today %, Countdown, Streak, Exam Day, Smart Suggestion, Tasks.
- Use `RingProgress`, `StatChip`, `LinearProgress`, and `TaskItem`.
- Task item supports completion toggle animation; points shown.
**Acceptance**
- Visual parity with screenshot: pastel cards, streak chip with “🔥 12 days”, exam bar at 100%, suggestion CTA button.

---

## CARD 6 — Tasks Screen (Daily / Weekly / Custom)
**Goal**: Recreate 3 tabs + category bars + task list items with difficulty chips and XP.
**Steps**
- `SegmentedTabs` for mode.
- Top “Today’s Progress” card with 4 skill bars.
- List items: title, difficulty chip (Kolay/Orta/Zor), time, XP, Start/Completed CTA.
- Weekly tab shows weekly goals + metrics.
- Custom tab shows quick practice shortcuts.
**Acceptance**
- Swipe/press feedback; completed items greyed with checkmark; all strings in Turkish.

---

## CARD 7 — Progress Screen (Overview / Skills / Awards / AI Analytics)
**Goal**: Mirror analytics pages.
**Steps**
- Overview: weekly bars, study time card.
- Skills: 4 skill cards with level labels (Beginner→Expert) and percentages.
- Awards: earned vs locked states with rarity colors.
- AI Analytics: 7D/30D/All/Perf/AI sub‑tabs with simple charts (use Canvas or a lightweight chart lib only if already present).
**Acceptance**
- 60 fps scroll; cards consistent with component kit.

---

## CARD 8 — Social Screen (Profile / Ranks / Groups / Friends / Awards)
**Goal**: Implement profile customization & leaderboard.
**Steps**
- Privacy banner at top; profile editor: username field, avatar grid (8 options), custom upload stub.
- Weekly study goal slider (3–35h) with preview chip.
- Ranks: weekly leaderboard with the user highlighted.
- Groups: 3 example groups with activity chips + Join button.
**Acceptance**
- All interactive controls keyboard and TalkBack accessible.

---

## CARD 9 — Settings (Navigation • Notifications • Gamification • Social • Privacy • Tasks)
**Goal**: Sectioned settings with toggle rows and danger action.
**Steps**
- Header segmented buttons (6).
- Section cards using `ToggleRow` for each setting in screenshots.
- “Reset All Notifications” (neutral) and “Reset Progress (Danger)” (destructive) buttons.
**Acceptance**
- Switch states persist in local `DataStore` (proto or prefs schema).

---

## CARD 10 — Turkish Localization & Formatting
**Goal**: Ensure Turkish language everywhere.
**Steps**
- `strings.xml` (tr) for all UI text; format dates with `Locale("tr","TR")`.
- Number formatting via `NumberFormat.getInstance(trLocale)`.
**Acceptance**
- No hard-coded English; a11y labels in Turkish.

---

## CARD 11 — Theming QA & Dark Mode
**Goal**: Visual parity in both modes.
**Steps**
- Dark color scheme mapping; add theme toggle in Settings.
- Snapshot previews for Light/Dark of each component.
**Acceptance**
- Contrast OK; ripple/indication visible in dark mode.

---

## CARD 12 — Final Polish, Lint, and Changelog
**Goal**: Ship the UI revamp cleanly.
**Steps**
- Replace ad‑hoc UI on screens with component kit.
- Remove dead code/styles.
- Update `CHANGELOG_LovableUI.md` with screenshots/gifs (if available).
**Acceptance**
- Single PR with readable commits and passing checks.


---

## CARD 13 — **No Hard‑coded Colors** Guard (Detekt + Lint)
**Goal**: Enforce token usage forever.
**Steps**
1) Add a custom **Detekt** rule `NoHardcodedColorsRule` that flags usages of `Color(#...)`, `Color(red=..., green=..., blue=...)`, or `Color(...)` outside theme files.
2) Add an Android **Lint** check that scans Compose modifiers for `.background(Color...)` / `.border(Color...)` / `.color(Color...)` and suggests `StudyPlanTheme.colorScheme.*` or `hsl()`.
3) Wire both to CI; fail build on violations.
**Deliverables**
- `quality/detekt-rules/src/.../NoHardcodedColorsRule.kt`
- `quality/lint/src/.../NoHardcodedColorsDetector.kt`
**Acceptance**
- Running `./gradlew detekt lint` fails on any direct color usage outside `ui/theme`.

---

## CARD 14 — Token Bridge: CSS (Lovable) → Compose
**Goal**: Keep Lovable’s HSL tokens as the single source of truth.
**Steps**
1) Create script `tools/tokens_css_to_compose.kt` that parses `--token: h s% l%` from a CSS file (or CODE_SPECIFICATIONS.MD) and generates:
   - `ui/theme/GeneratedTokens.kt` (Color constants + mapping to `ColorScheme`).
   - `ui/theme/GeneratedSemantics.kt` (success/warning/achievement colors).
2) Add Gradle task `:app:generateComposeTokens` and wire to `preBuild`.
**Acceptance**
- Deleting a token from CSS and regenerating updates Compose colors in one command.

---

## CARD 15 — Visual Regression: Roborazzi Snapshots
**Goal**: Prevent UI drift vs the 19 screenshots.
**Steps**
1) Add **Roborazzi** + **Robolectric** test setup.
2) Create golden tests for: Home, Tasks (Daily/Weekly/Custom), Progress (4 tabs), Social (5 tabs), Settings (6 tabs).
3) Store goldens under `snapshots/golden/` and compare on PRs.
**Acceptance**
- `./gradlew connectedCheck roborazziRecord` creates images; `robora zz iCompare` passes on unchanged UI.

---

## CARD 16 — Accessibility & Dynamic Type Audit
**Goal**: WCAG‑AA parity in TR locale.
**Steps**
1) Add a11y labels/contentDescriptions for all interactive elements.
2) Verify keyboard focus order; add `semantics { }` where needed.
3) Respect **fontScale up to 1.3x** without clipping.
4) Add a “Reduced Motion” toggle that disables nonessential animations (read system animator scale and app flag).
**Acceptance**
- Accessibility Scanner shows no critical issues; TalkBack reads all controls correctly.

---

## CARD 17 — Baseline Profiles & Scroll Performance
**Goal**: Smooth startup and lists.
**Steps**
1) Add **Macrobenchmark** module; generate **Baseline Profiles** for cold/warm start and heavy lists (Tasks, Leaderboard).
2) Ensure LazyColumn items have stable keys and `remember` where appropriate.
**Acceptance**
- Jank < 3% during 60‑second scroll test; start time improves by ≥15% with profiles.

---

## CARD 18 — DataStore Persistence Schemas
**Goal**: Persist Settings and lightweight UI state.
**Steps**
1) Define `UserPrefs.proto` for theme, toggles, language, reducedMotion, weeklyStudyGoal.
2) Add repository + ViewModel integration; migrate Settings screen to real persistence.
**Acceptance**
- Toggling switches survives app restarts; proto migration path covered in tests.

---

## CARD 19 — Turkish i18n Deep‑Dive + RTL Readiness
**Goal**: Perfect TR text & formatting; future RTL‑proofing.
**Steps**
1) Move strings to `res/values-tr/strings.xml` (default TR) and `res/values/strings.xml` (EN fallback).
2) All dates via `DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale("tr","TR"))`.
3) Add `layoutDirection = LayoutDirection.Ltr` for now; verify no hardcoded paddings block RTL.
**Acceptance**
- No English remnants; switching device language shows correct numerals and month names.

---

## CARD 20 — Feature Add‑On: **Progress PDF Reports**
**Goal**: Export shareable one‑page reports (teachers/tutors).
**Steps**
1) Create `report/ReportComposer.kt` rendering Compose to **PDF** via `PdfDocument` (A4, vector).
2) Include sections: Weekly summary, Skills, Awards, AI insights, streak.
3) Add share sheet + “Export to Files” options.
**Acceptance**
- A4 PDF ≤ 400KB with selectable text; matches app branding.

---

## CARD 21 — Feature Add‑On: **Calendar Integration**
**Goal**: Sync study sessions with Google Calendar.
**Steps**
1) Add optional OAuth flow; or use Calendar Provider if on-device account is permitted.
2) Create events for “Study Session: <Category>” with reminders; keep idempotent updates.
3) Settings toggle to enable/disable sync.
**Acceptance**
- Sessions appear in Calendar; deleting in app removes events (with confirmation).

---

## CARD 22 — Feature Add‑On: **Performance Prediction (ML)**
**Goal**: Forecast YDS score trajectory.
**Steps**
1) Create `ml/` module with simple **on‑device** regression baseline: inputs (study time, task completion ratio, recent accuracy, streak length). Use `Smile` or `ONNX Runtime` if available, else heuristic model.
2) Provide API to compute next‑30‑day expected score and confidence band.
3) UI: In Progress → AI Analytics, show predicted curve with “What improves this?” tips.
**Acceptance**
- Deterministic output given same inputs; model weights are versioned and can be swapped with server model later.

---

## CARD 23 — PR Hygiene: Templates, CODEOWNERS, CI
**Goal**: Keep changes reviewable and safe.
**Steps**
1) Add `.github/pull_request_template.md` with checklist (tokens, a11y, dark mode, TR text, tests).
2) Add `CODEOWNERS` mapping `ui/theme/**` and `ui/components/**` to reviewers.
3) CI: run `build`, `detekt`, `lint`, `test`, `roborazzi` on pull requests.
**Acceptance**
- Every PR shows snapshots and passes quality gates before merge.

---

## CARD 24 — React → Compose Mapping Note (for Lovable Specs)
**Goal**: Avoid drift between Lovable (React/Tailwind) spec and Android Compose.
**Steps**
1) In `docs/DesignParity.md` include a **token mapping table**: CSS var → `StudyPlanTheme.colorScheme.*` or semantic extension.
2) Document component parity: shadcn `Card`, `Badge`, `Tabs`, `Switch` ↔ Compose `AppCard`, `StatChip`, `SegmentedTabs`, `ToggleRow`.
3) Keep HSL as the palette space; never store hex in Kotlin files.
**Acceptance**
- Designers can cross‑reference tokens 1:1 between platforms.

