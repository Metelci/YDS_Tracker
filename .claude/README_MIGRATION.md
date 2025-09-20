# 🚀 YDS_Tracker — Migration Workflow (Material 2 ➜ Material 3)

This repository contains AI-agent-friendly prompts and checklists to guide the **end-to-end migration** of the app UI from legacy Compose Material 2 / Accompanist to **Material 3**.

---

## 📂 Files Overview

### Phase 2 — Legacy UI Audit
- **`phase2_audit_prompt.md`** *(not yet exported, see conversation)*  
  Instructs the agent to scan the repo for legacy UI patterns and produce:  
  - `legacy-usage.csv`  
  - `mapping.json`  
  - `AUDIT_REPORT.md`

### Phase 3 — Codemods & Migration
- **[`phase3_migration_prompt.md`](./phase3_migration_prompt.md)**  
  Tells the agent how to:  
  - Update Gradle dependencies  
  - Rewrite imports (`androidx.compose.material` ➜ `androidx.compose.material3`)  
  - Replace deprecated widgets (`BottomNavigation` ➜ `NavigationBar`, etc.)  
  - Migrate headers to `TopAppBar`  
  - Replace theme/colors with `MaterialTheme.colorScheme`  
  - Add CI guardrails (`detekt.yml`)  
  - Produce `MIGRATION.md`

- **[`MIGRATION.md`](./MIGRATION.md)**  
  Human-readable migration report (auto-filled by the agent).

### Phase 4 — Page & Feature Rewiring
- **[`phase4_rewiring_prompt.md`](./phase4_rewiring_prompt.md)**  
  Guides the agent to rewire full screens (Main, Tasks, Progress, Settings) with Material 3 components, spacing tokens, and accessibility best practices.

- **[`SCREEN_CHECKLIST.md`](./SCREEN_CHECKLIST.md)**  
  Developer checklist for verifying each screen migration is complete.

### Phase 5 — Legacy Removal & Enforcement
- **[`phase5_removal_prompt.md`](./phase5_removal_prompt.md)**  
  Tells the agent how to:  
  - Remove unused legacy files, adapters, resources, and dependencies  
  - Strengthen CI guardrails (Detekt rules, Gradle checks, optional GitHub Actions)  
  - Ensure zero legacy references remain  
  - Produce `REMOVAL_REPORT.md`

- **[`REMOVAL_REPORT.md`](./REMOVAL_REPORT.md)**  
  Final cleanup documentation, listing removed components and confirming guardrails.

---

## ▶️ Execution Order

1. **Phase 2 Audit**  
   - Agent runs `phase2_audit_prompt.md`.  
   - Outputs: `legacy-usage.csv`, `mapping.json`, `AUDIT_REPORT.md`.

2. **Phase 3 Migration (Codemods & Deps)**  
   - Agent runs `phase3_migration_prompt.md`.  
   - Applies codemods, dependency updates, and theming fixes.  
   - Outputs: updated codebase + `MIGRATION.md` + CI guardrails.

3. **Phase 4 Rewiring (Screens)**  
   - Agent runs `phase4_rewiring_prompt.md`.  
   - Migrates each screen to Material 3.  
   - Uses `SCREEN_CHECKLIST.md` for tracking.  
   - Outputs: updated screens, Previews, `audit-snapshots/` images.

4. **Phase 5 Removal & Enforcement**  
   - Agent runs `phase5_removal_prompt.md`.  
   - Deletes legacy code, adapters, and unused resources.  
   - Strengthens guardrails in CI.  
   - Outputs: `REMOVAL_REPORT.md`.

---

## ✅ Definition of Done
- Build passes: `./gradlew build`  
- Tests green: `./gradlew test`  
- No `androidx.compose.material` or `com.google.accompanist` imports remain  
- All screens use Material 3 components  
- `MIGRATION.md` completed and reviewed  
- `SCREEN_CHECKLIST.md` fully checked off  
- `REMOVAL_REPORT.md` confirms cleanup and enforcement

---

## 🔒 Guardrails
- Detekt rules block legacy imports.  
- CI fails if regressions occur.  
- Legacy dependencies cannot be reintroduced.

---

With this structure, your AI agent can auto-discover **Phases 2 → 5** in the correct order by scanning for the `phase*_*.md` files in this directory.
