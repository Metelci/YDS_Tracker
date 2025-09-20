# Legacy UI Audit Report

## Totals by Kind
- **materialtheme-typography**: 439
- **magic-spacing**: 432
- **raw-color**: 287
- **material2-import**: 138
- **legacy-tab**: 5
- **legacy-divider**: 1

## Hotspots (top 10 files)
- app\src\main\java\com\mtlc\studyplan\gamification\EnhancedProgressCelebrations.kt: raw-color: 47, materialtheme-typography: 13, magic-spacing: 9
- app\src\main\java\com\mtlc\studyplan\feature\progress\ProgressScreen.kt: materialtheme-typography: 50, material2-import: 12, legacy-tab: 1
- app\src\main\java\com\mtlc\studyplan\ui\theme\Color.kt: raw-color: 58
- app\src\main\java\com\mtlc\studyplan\gamification\MotivationMechanics.kt: raw-color: 16, materialtheme-typography: 15, magic-spacing: 14
- app\src\main\java\com\mtlc\studyplan\ui\components\WeeklyChallengeSystem.kt: materialtheme-typography: 18, magic-spacing: 16, raw-color: 7, material2-import: 4
- app\src\main\java\com\mtlc\studyplan\feature\tasks\TasksScreen.kt: magic-spacing: 32, material2-import: 5, raw-color: 3, legacy-tab: 2, materialtheme-typography: 2
- app\src\main\java\com\mtlc\studyplan\ui\components\ErrorComponents.kt: magic-spacing: 19, materialtheme-typography: 15, material2-import: 2
- app\src\main\java\com\mtlc\studyplan\settings\ui\SettingsScreens.kt: materialtheme-typography: 16, magic-spacing: 16, material2-import: 3
- app\src\main\java\com\mtlc\studyplan\theme\StudyPlanTheme.kt: raw-color: 31, magic-spacing: 3
- app\src\main\java\com\mtlc\studyplan\ui\components\AchievementUnlockAnimation.kt: magic-spacing: 19, materialtheme-typography: 11, material2-import: 4

## Risks & Notes
- Material2 imports remain widespread; codemods must ensure BOM-managed imports flip to material3 counterparts before adjusting composables.
- Multiple screens rely on BottomNavigation/TabRow patterns; migration should sequence high-traffic screens first to avoid navigation regressions.
- Raw Color and dp literals are pervasive; consider introducing centralized design tokens before bulk refactor to avoid inconsistencies.
- Confirm accompanist usages before removal; ensure pull-to-refresh behaviour is preserved via `pullRefresh`.


