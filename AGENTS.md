# Repository Guidelines

## Project Structure & Module Organization
The project ships as a single Gradle module `:app`. Kotlin sources live in `app/src/main/java/com/mtlc/studyplan`, with Compose UI split by feature under `ui/screens`, shared widgets in `ui/components`, and security-critical code in `security/` and `network/`. XML resources, theming, and navigation live in `app/src/main/res`. Ancillary material: release notes in `release-notes/`, Play assets in `store-listing/`, extensive security manuals in `SECURITY_*.md`, and reference docs or screenshots in `docs/`.

## Build, Test, and Development Commands
Run Gradle via the provided wrappers from the repo root:
- `./gradlew :app:assembleDebug` (or `gradlew.bat` on Windows) compiles a debug APK.
- `./gradlew :app:installDebug` deploys to the active device or emulator; launch via `adb shell am start -n com.mtlc.studyplan/.MainActivity`.
- `./gradlew :app:lintDebug` runs Android lint and Compose metrics.
- `./gradlew clean` resets build artifacts before generating release bundles.

## Coding Style & Naming Conventions
Use 4-space indentation and Kotlin idioms (`val` by default, expression bodies where clear). Compose composables stay pure and stateless; hoist state to `viewmodels/`. Name classes or composables with PascalCase, functions or variables camelCase, constants ALL_CAPS. Keep packages feature-scoped (for example `today`, `analytics`, `settings`). Format code with Android Studio's Kotlin style or ktlint before pushing, and verify Compose previews inside the relevant screen files.

## Testing Guidelines
Unit specs belong in `app/src/test` and should cover new logic with Robolectric or coroutine test utilities (target >=80% coverage for touched packages). UI and instrumentation tests live in `app/src/androidTest`; mirror screen names (`TodayScreenTest`, `ProgressSummaryTest`). Run `./gradlew :app:testDebugUnitTest` prior to every PR, and execute `./gradlew :app:connectedDebugAndroidTest` whenever UI flows, Compose navigation, or WorkManager logic change (requires an emulator or device).

## Commit & Pull Request Guidelines
Follow the Conventional Commit style used in history (`feat:`, `fix:`, `chore:`, `release:`), optionally adding scopes (`feat(progress):`). Keep commits focused and include doc or resource updates alongside code. Pull requests must link issues, summarize user-visible changes, state which Gradle tasks were run, and attach screenshots or recordings for UI adjustments. Request security review whenever touching `security/`, `network/`, or certificate pinning assets.

## Security & Configuration Tips
Never commit signing keys or API secrets; source them from `local.properties` or CI variables. When rotating pins, follow `SECURITY_INTEGRATION_GUIDE.md` and regenerate hashes with `GetCertificateHash.kt` or `GetCertificateHash.java`. Retain R8, strict `network_security_config.xml`, and biometrics in release builds. If Gradle hits out-of-memory errors, add `org.gradle.jvmargs=-Xmx4g` to `gradle.properties` instead of downgrading features.
