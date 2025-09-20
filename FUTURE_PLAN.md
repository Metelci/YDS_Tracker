# Future Plan

## Vision
Establish a sustainable design-system governance and automation strategy so Material 3 remains the baseline while we prepare for future Compose/Material releases.

## Immediate Objectives (0-3 months)
1. **Stabilize Build**
   - Resolve outstanding compile errors (validation duplicates, missing resources, TaskCard bindings).
   - Ensure `./gradlew clean build` and `./gradlew test` succeed in CI.
2. **Design System Hardening**
   - Promote spacing/shape/color tokens into a documented design-system module (`:core-ui`).
   - Add lint rule to block inline `dp` magic numbers (use `LocalSpacing`).
3. **Guardrails**
   - Enable Detekt rule to forbid `androidx.compose.material.` imports.
   - Add CI job for accessibility lint and screenshot diffing.

## Mid-Term Initiatives (3-6 months)
1. **Automation & Tooling**
   - Configure Dependabot/Renovate for Compose BOM and Kotlin plugin updates.
   - Schedule nightly CI runs with Macrobenchmark smoke tests.
   - Add pre-commit hook to run `ktlint` + `detekt`.
2. **Modularization Roadmap**
   - Break out feature packages into Gradle modules (`:feature-today`, `:feature-tasks`, `:feature-progress`).
   - Extract reusable UI to `:core-ui` with centralized tokens.
3. **Performance Regression Suite**
   - Introduce JankStats collection and Macrobenchmark for TodayScreen & ProgressScreen.
   - Publish performance dashboards (Grafana/Data Studio).

## Long-Term Outlook (6-12 months)
1. **Material 4 Readiness**
   - Track Compose Material roadmap; pilot feature branch once alpha stable.
   - Maintain `mapping.json` as authoritative design token spec.
2. **Cross-Platform Exploration**
   - Evaluate Compose Multiplatform for desktop / iOS prototypes.
   - Share design tokens with web (Figma, CSS variables).
3. **Knowledge Sharing**
   - Organize quarterly workshops (“Living with Material 3”).
   - Keep `MIGRATION_LESSONS.md`, `OPTIMIZATION_REPORT.md` up to date.

## Governance Checklist
- [ ] Lint rules blocking inline dp/color usage.
- [ ] CI jobs: unit, instrumentation, accessibility scan, macrobenchmark.
- [ ] Automated dependency updates enabled.
- [ ] Design-system tokens documented and versioned.

## Open Questions
- Do we maintain validation stubs or integrate with production telemetry service?
- Should TodayScreen pull refresh migrate to Material3 experimental API or remain on Material2 until stable?

## Next Review
Schedule governance review **quarterly** (next: 2025-12-01) to reassess progress, update roadmap, and coordinate with product/design leadership.
