# Optimization Report

## Context
- Project: YDS_Tracker Material 3 migration
- Phase: 9 (Post-Migration Optimization)
- Date: 2025-09-20

## Current Build Status
- `./gradlew clean build` **fails** (see DEPLOYMENT_REPORT.md). Optimization work is gated on restoring a green build.

## Observed Performance & UX Issues
1. **Compose compile failures** prevent runtime profiling (e.g., invalid resource IDs, duplicate validation classes).
2. **TaskCard composable** has broken bindings (`desc`, `details`). Needs refactor before measuring UI performance.
3. **Validation module** duplicates `PerformanceMonitor`, inflating memory footprint once compiled.

## Accessibility Assessment
- Automated scans not run due to build failure.
- Manual inspection: TodayScreen uses 48dp touch targets (`largeTouchTarget()`), but other screens require verification.

## Optimization Backlog
| Priority | Item | Area | Notes |
| --- | --- | --- | --- |
| P0 | Fix validation suite duplicates (`PerformanceMonitor`, `PerformanceMetrics`) | validation/ | Blocks build & monitoring |
| P0 | Restore missing icons referenced in `DataFormatters.kt` (`ic_trophy`, `ic_warning`, etc.) | utils/resources | Required for UI rendering |
| P0 | Repair `TaskCard` composable props (`desc`, `details`) | ui/components | Prevents list rendering |
| P1 | Align TodayScreen swipe-to-dismiss with Material3 | feature/today | Currently pulls in Material2 API |
| P2 | Benchmark startup & frame rendering once build passes | global | Use Macrobenchmark or Compose Tracing |
| P2 | Run Accessibility Scanner on top tasks/progress screens | feature/tasks, feature/progress | Ensure semantics coverage |

## Continuous Improvement Loop
- Pending: cannot schedule performance regressions until compile errors resolved.
- Recommendation: after P0 fixes, add simple Macrobenchmark entry point and automated accessibility lint.

## Next Steps
1. Land P0 fixes to restore `./gradlew clean build`.
2. Execute baseline profiling on physical device (startup, JankStats).
3. Integrate results into CI (benchmarks, lint rules).

## Status
- **Phase 9 BLOCKED** – awaiting functional build before true optimization can begin.
