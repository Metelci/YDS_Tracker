# Build Failure Summary (Ranked)

1. **Settings backup API regressions** – `SettingsBackupManager.kt` cannot resolve `ErrorType`, direct `AppError` construction, or synchronous repository helpers (`getAllCategoriesSync`, `getCategorySettingsSync`, `id` usage). These unresolved references block both debug and release Kotlin compilation.
2. **Settings view model wiring collapsed** – `SettingsViewModelFactory.kt` redeclares factory classes and references missing dependencies (`ViewModelProvider`, `settingsRepository`, `accessibilityManager`, `animationCoordinator`, etc.), causing downstream XML fragment factories (e.g., `GamificationSettingsFragment`, `NotificationSettingsFragment`, `PrivacySettingsFragment`) to fail to compile.
3. **Settings system integration defects** – `SettingsSystemIntegration.kt` still calls `memoryUsage.toLong()` (type mismatch) and invokes suspend search APIs from non-suspend contexts, leading to additional compiler errors.

Secondary fallout includes dozens of cascading unresolved-symbol errors across settings UI fragments and managers once the factory/backup layers fail. No dependency resolution or resource merge issues were observed; failures are confined to Kotlin compilation.
