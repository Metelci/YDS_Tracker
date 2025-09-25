## Copilot / AI agent quick instructions for YDS_Tracker

Purpose: give an AI coding agent the precise, immediately-useful facts about this repo so suggested code and PRs are correct and low-risk.

- Repo shape: single Gradle Android app module `:app` (Kotlin + Jetpack Compose, MVVM + Repository). Top-level app code: `app/src/main/java/com/mtlc/studyplan`.
- UI: Compose-only (Material 3). Look under `ui/screens` (feature screens) and `ui/components` (shared widgets).
- State & DI: state is hoisted to `viewmodels/`, dependency injection is Hilt. Data layer uses Room (`database/`), Repositories (`repositories/`) and DataStore preferences.
- Background & integration: WorkManager used for scheduled/background tasks. Network and security code is in `security/` and `network/` packages.

Key files and patterns to reference in changes
- `app/src/main/java/com/mtlc/studyplan/ui/` — prefer composable, stateless functions; move state into corresponding ViewModel.
- `app/src/main/java/com/mtlc/studyplan/security/SecureStorageManager.kt` and `GetCertificateHash.kt`/`.java` — used for encryption and certificate pinning. Any change touching security/network requires explicit security review and tests.
- `app/src/main/res/network_security_config.xml` (or network config under `res`) — follow existing pinning / strict HTTPS patterns.
- `build.gradle.kts` and `app/build.gradle.kts` — add dependencies via Gradle Kotlin DSL; follow existing versioning in `gradle/libs.versions.toml`.

Developer workflows & commands (copy-pasteable)
- Build debug APK (Linux/macOS): `./gradlew :app:assembleDebug`  — Windows: `gradlew.bat :app:assembleDebug`
- Install to device (Windows): `gradlew.bat :app:installDebug`
- Launch app on device: `adb shell am start -n com.mtlc.studyplan/.MainActivity`
- Run unit tests: `./gradlew :app:testDebugUnitTest`  (Windows: `gradlew.bat ...`)
- Run instrumentation/UI tests (device/emulator): `./gradlew :app:connectedDebugAndroidTest`
- Lint/compose metrics: `./gradlew :app:lintDebug`

Conventions and rules that matter for code changes
- Formatting: 4-space indentation; follow project Kotlin style and run ktlint/Android Studio formatter before committing.
- Commits: Conventional Commit messages (feat:, fix:, chore:, etc.). PRs should include which Gradle tasks were run and screenshots for UI changes.
- Compose: use Material 3 components only. Keep composables pure; side-effects and long-running work must live in ViewModels or use WorkManager.
- Tests: unit tests in `app/src/test`, instrumentation tests in `app/src/androidTest`. New logic should include at least one unit test; aim to preserve >=80% coverage for touched packages.

Integration & security checklist for PRs touching security/network
- Do not commit keys or secrets. Use `local.properties`/CI variables for secrets.
- If adding or rotating certificate pins, update `GetCertificateHash.kt`/`.java` and follow `SECURITY_INTEGRATION_GUIDE.md`.
- Add or update unit/instrumentation tests that validate authentication/encryption flow when touching `security/` or `network/`.

Examples of safe edits
- UI change: add a new stateless composable under `ui/components/`, add a ViewModel in `viewmodels/` to expose state, write a unit test for the ViewModel.
- New repository: create under `data/repositories/`, wire into Hilt modules (search for existing `@Module` bindings), add Room DAO in `database/` and migrations if schema changes.

What not to change without approval
- Anything under `security/`, `network/` or certificate pinning assets. These require security review.
- Release signing, `local.properties`, keystore files, Play store metadata.

If you need more context
- Read `AGENTS.md` and `README.md` (root) for architecture and dev setup notes.
- Search for `@HiltAndroidApp` to find the DI entrypoint and `WorkManager` usages to find background task boundaries.

When opening a PR created or modified by an AI
- Use a focused branch and Conventional Commit title. In the PR description: list run Gradle tasks, attach screenshots, list files changed, and call out any security-impacting changes.

Questions or unclear areas: ask the maintainer for intended UX/flow before changing study-plan algorithm logic or exam scheduling heuristics.
