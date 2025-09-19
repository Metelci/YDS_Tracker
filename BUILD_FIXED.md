# Build Stabilization Notes (Phase 1)

## Build Command
- Ran `./gradlew.bat :app:assembleDebug` (current stand-in for `buld_cmd`).

## Current Status
- Build still failing at Kotlin compile stage.
- Eliminated the top-level Compose scope error in `WorkingTasksScreen.kt` by wrapping the `AnimatedVisibility` block in a LazyColumn `item { ... }` container.

## Remaining Blockers (Top of Stack)
1. **Data Layer mismatches:** `DataConsistencyManager`, `PlanRepository`, and `ProgressRepository` reference types (`PlanTask`, `PlanDataSource`, `updateStudyStats` helpers) that no longer align with available models.
2. **Event Bus generics:** `eventbus/EventBus.kt` has unresolved generic type inference with `java.lang.Class` vs `kotlin.reflect.KClass`.
3. **Home screen state adapters:** `feature/home/NewHomeScreen.kt` expects keyed state wrappers; generic parameters missing.
4. **Validation utilities:** `validation/SettingsValidationManager.kt` references `UserSettings` and `times` extension utilities that appear removed or renamed.

## Next Fix Candidates
- Reconcile `Task` vs `PlanTask` model split (likely by restoring `PlanTask` definition or updating repository signatures).
- Reintroduce or adapt missing helpers in `DataConsistencyManager.kt` (`updateStudyStats`, `updateStreak`, etc.).
- Convert EventBus API to use `KClass` signatures or provide overload bridging from `Class`.

## Artifacts
- Code change: `app/src/main/java/com/mtlc/studyplan/core/WorkingTasksScreen.kt` (wrap completed section in `item { ... }`).
- Updated `app/src/main/java/com/mtlc/studyplan/data/PlanRepository.kt` to produce `PlanTask` objects throughout, resolving the `PlanTask`/`Task` type mismatch errors during Kotlin compilation.
