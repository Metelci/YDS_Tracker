# 🧪 Phase 6 — Validation & Final QA

**Repo:** `Metelci/YDS_Tracker`  
**Prereqs:** Phases 2–5 completed (audit, codemods, rewiring, legacy removal).  
**Mission:** Validate the entire app build, test coverage, UI, and developer experience. Document results in a `VALIDATION_REPORT.md`.

---

## 6.0 Goals
- Confirm build + tests pass reliably.  
- Run E2E/smoke flows on all major screens.  
- Verify no regressions: theming, accessibility, internationalization.  
- Ensure guardrails (Detekt, Gradle checks, CI) are blocking legacy imports.  
- Document results in a report.

---

## 6.1 Validation steps

### A) Build & test
```bash
./gradlew clean build
./gradlew test
./gradlew connectedAndroidTest   # if instrumented tests exist
```

### B) Static analysis
```bash
./gradlew detekt
./gradlew ktlintCheck
```
- Confirm forbidden imports cause build failure if reintroduced.

### C) UI verification
- Run Compose Previews for main screens (Main, Tasks, Progress, Settings).  
- Optional: run screenshot tests (e.g., Paparazzi, Shot).  
- Compare against snapshots in `audit-snapshots/`.

### D) Smoke flows
Check user flows manually or with automation:
- Add new task  
- Mark task complete  
- View progress screen  
- Toggle a setting  

### E) Accessibility & i18n
- Verify `contentDescription` on icons.  
- Text scaling with `fontScale=1.5` works.  
- Dark mode verified.  
- RTL layout support (if applicable).

---

## 6.2 Deliverables
- **`VALIDATION_REPORT.md`** including:  
  - Build status  
  - Test results summary  
  - Static analysis summary  
  - Screens verified  
  - Accessibility/i18n results  
- Optional: screenshots in `audit-snapshots/validation/`.

---

## 6.3 Definition of Done
- ✅ Build & tests green  
- ✅ No forbidden imports/resources  
- ✅ All screens render correctly in M3  
- ✅ Accessibility & dark mode verified  
- ✅ Validation documented in `VALIDATION_REPORT.md`
