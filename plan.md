# Build-and-Migrate Plan (Phase 0)

## Stack Summary
- **Platform:** Android mobile app (Gradle module `:app`).
- **Language:** Kotlin (Jetpack Compose UI + legacy XML interop).
- **Build System:** Gradle 8.13 wrapper, Android Gradle Plugin 8.13.0.
- **Kotlin Toolchain:** Kotlin 2.0.21, JVM target 11 via Gradle toolchains.
- **Dependency Injection:** Hilt (Dagger 2.48).
- **Persistence:** Room 2.6.1, DataStore Preferences.
- **Testing:** JUnit4, Robolectric 4.10.3, Espresso 3.6.1, Compose testing libs.
- **Design Systems:** Legacy UI uses yet-to-be-identified `OLD_LIBS_AND_SELECTORS`; target system `NEW_DESIGN_LIB` (specific packages/selectors to confirm in Phase 2).

## Risks & Unknowns
1. **Legacy UI Scope Unknown:** Need inventory to map actual legacy components/selectors; risk of hidden usages in fragments/XML.
2. **Compose + XML Mix:** Migration may expose state management or theme regressions when swapping components.
3. **Large Vocabulary Assets:** Build times and APK size considerations; ensure no accidental asset removal.
4. **Security Constraints:** Certificate pinning & biometric flows could break during refactors; require targeted regression tests.
5. **Test Coverage:** Some features may lack automated tests; manual validation plan needed.

## Tool Versions & Environment Targets
- Gradle Wrapper: `./gradlew` (Gradle 8.13).
- Android Studio reference: Giraffe 2022.3.1+ (for manual validation).
- Java/JDK: Managed via toolchain (JDK 11).
- Kotlin Compiler Extension: Compose 1.5.15.
- Hilt KSP: `com.google.devtools.ksp` 2.0.21-1.0.25.

## Phase Roadmap & Time Boxes
1. **Phase 1 – Build Triage & Stabilization (0.5 day):**
   - Sync Gradle, ensure spotless build via `./gradlew :app:assembleDebug`.
   - Capture fixes in `BUILD_FIXED.md`; record canonical `BUILD_CMD`.
2. **Phase 2 – Legacy UI Audit (0.5 day):**
   - Identify legacy imports/selectors; produce `legacy-usage.csv` & `mapping.json`.
3. **Phase 3 – Migration Mechanics (1 day):**
   - Create adapters, codemods, ESLint-equivalent checks (Kotlin lint strategy TBD).
4. **Phase 4 – Feature/Page Rewrites (multi-iteration, 2–3 days):**
   - Tackle high-traffic screens first; snapshot diffs in `audit-snapshots/`.
5. **Phase 5 – Remove Legacy (0.5 day):**
   - Delete deprecated components after ≥95% adoption metric.
6. **Phase 6 – Validation (0.5 day):**
   - Run unit, instrumentation, lint; confirm zero legacy references.
7. **Phase 7 – Deliverables (0.5 day, parallelized):**
   - Compile `AUDIT_REPORT.md`, `MIGRATION.md`, update CI config, prep PRs A/B/C.

## Immediate Next Steps
- Validate build health via `./gradlew :app:assembleDebug`.
- Start inventory of legacy design libraries (Phase 2 prep).
- Decide lint/guard strategy for Kotlin (detekt custom rule vs. static analysis script).
\nPhase 3 prep: Added tokens generator script at tools/tokens_css_to_compose.kt (defaults to .claude/CODE_SPECIFICATIONS.MD). Run via Kotlin CLI (requires kotlin runtime) with optional --input/--output.
