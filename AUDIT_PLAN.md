# Audit Plan

## Toolchain Snapshot
- Gradle wrapper: 8.13 (`gradle-wrapper.properties`)
- Android Gradle Plugin: 8.13.0 (via Version Catalog)
- Kotlin: 2.0.21 with Compose compiler extension 1.7.5
- Compose BOM: 2024.12.01
- JDK: 17 (Gradle output)

## Build Attempt Summary
- Command: `./gradlew clean build --no-daemon --stacktrace`
- Output log: `logs/build-initial.txt`

## Failure Buckets & Risks
1. **Settings backup module regressions** (Critical)
   - `SettingsBackupManager.kt` references `ErrorType`, synchronous repository helpers, and `AppError` constructors that do not exist or are inaccessible.
   - Blocks Kotlin compilation for both debug and release variants.
2. **Settings ViewModel factory collapse** (Critical)
   - `SettingsViewModelFactory.kt` redeclares factory classes and references missing dependencies; XML-based settings fragments now fail to find their factories.
   - Causes a cascade of unresolved symbol errors across settings UI packages.
3. **Settings system integration inconsistencies** (High)
   - `SettingsSystemIntegration.kt` misuses `memoryUsage` (calling `toLong()` on non-numeric type) and calls suspend function `searchSettings` from non-suspend context.
   - Additional clean-up required to align flows with actual API surface.
4. **Legacy state management artifacts** (Medium)
   - Some older settings fragments (e.g., `GamificationSettingsFragment`) still expect factories that no longer exist; must update or provide compatibility wrappers to unblock build.
5. **General dependency alignment** (Low)
   - Compose BOM and Material3 available, but lingering Material2 references and accompanist imports will be addressed later (not blocking current build).

## Immediate Objectives
- Restore compilation by fixing items 1–4 with minimal, targeted changes.
- Re-run `./gradlew :app:compileDebugKotlin` after each fix batch.
- Once green, confirm full `./gradlew clean build --stacktrace` and capture final logs.
