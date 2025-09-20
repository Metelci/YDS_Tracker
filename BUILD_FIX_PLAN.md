# Build Fix Plan

## Bucket 1 – SettingsBackupManager failures
- Reintroduce/define `ErrorType` enum or adjust manager to use existing `AppError` factory helpers.
- Replace calls to missing synchronous repository helpers with available `SettingsRepository` APIs (likely flow-based; use cached state or add temporary helper).
- Ensure serialization/export path uses accessible data (`getSectionsForCategory`, etc.).

## Bucket 2 – Settings ViewModel factories
- Consolidate factory definitions to a single, valid `ViewModelProvider.Factory` implementation.
- Provide dedicated factories (or lambdas) for legacy fragments referencing `GamificationSettingsViewModelFactory`, etc., or adjust fragments to use the shared factory.
- Import `ViewModelProvider` and required dependencies.

## Bucket 3 – Settings system integration clean-up
- Fix `memoryUsage` handling: respect actual type from `SettingsPerformanceMonitor.PerformanceState`.
- Wrap suspend search call inside coroutine or expose non-suspend helper returning `Flow`.
- Cross-check other flow exposures for correctness.

## Bucket 4 – Legacy fragments compatibility
- For fragments still using old factories, either restore lightweight compatibility wrappers or migrate them to new factory API without touching business logic.

## Execution Checklist
1. Address BackupManager compile errors.
2. Normalize `SettingsViewModelFactory` and dependent fragments.
3. Patch `SettingsSystemIntegration` suspend usage & types.
4. Re-run `./gradlew :app:compileDebugKotlin --stacktrace`.
5. Iterate on any new compiler output; capture logs where needed.
6. Once clean, run full `./gradlew clean build --stacktrace` and `./gradlew test`.
