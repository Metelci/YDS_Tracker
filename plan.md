# Material 3 Migration – Phase 0 Plan

- **Toolchain snapshot**
  - Gradle 8.13 via wrapper; Android Gradle Plugin 8.13.0; Kotlin 2.0.21 with Compose compiler 1.7.5.
  - Using Compose BOM 2024.12.01 (includes Material 3) but legacy Material 2 dependency (`androidx.compose.material:material`) still present.
  - Single Gradle module `:app`; Kotlin sources in `app/src/main/java/com/mtlc/studyplan`, XML resources in `app/src/main/res`.
  - KSP (2.0.21-1.0.25) and Hilt (2.48) enabled; Room and extensive custom managers/settings modules.

- **High-level migration game plan**
  1. Stabilise build with `AUDIT_AND_FIX_PROMPT.md`: resolve existing Kotlin/Settings compile errors and flaky temp dir issue so CI is green before UI work.
  2. Complete Phase 1 diagnostics (tooling commands + clean build log capture) to baseline current failures.
  3. Perform Phase 2 legacy UI audit to enumerate Material 2, Accompanist, and ad-hoc theming usage (produce `legacy-usage.csv`, `mapping.json`, `AUDIT_REPORT.md`).
  4. Iteratively execute Phases 3-5: upgrade dependencies, run codemods, migrate headers/theme/spacing, replace SwipeRefresh, and enforce guardrails (Detekt rule).
  5. Rework feature screens (Phase 4) in small commits, ensuring previews/tests, followed by cleanup (Phase 5) and validation (Phase 6).
  6. Close out with documentation, deployment, optimisation, and future-proofing artefacts (Phases 7-10).

- **Risks & watchpoints**
  - **Build breakage**: current `SettingsSystemIntegration` and related classes fail compilation (missing sync helpers, suspend misuse, backup manager API drift). Must be fixed pre-migration.
  - **Dependency drift**: coexistence of Material 3 BOM with explicit Material 2 artifact may mask theme conflicts; ensure BOM-managed versions align when removing M2.
  - **KSP/Hilt interplay**: code mods touching settings/offline packages may trigger KSP rebuilds; validate generated sources after large refactors.
  - **Resource conflicts**: manual colors/dp constants likely scattered; need central token strategy to avoid regressions.
  - **Testing debt**: migration touches UI extensively; ensure snapshot tests or previews are updated, and run Robolectric/UI tests where available.
