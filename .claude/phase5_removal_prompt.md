# 🗑️ Phase 5 — Legacy Removal & Enforcement

**Repo:** `Metelci/YDS_Tracker`  
**Prereqs:** Phase 2–4 completed (audit, codemods, rewiring).  
**Mission:** Delete leftover legacy files, styles, or dependencies once adoption of Material 3 is ≥95%, and lock the door behind us with strict guardrails.

---

## 5.0 Goals
- Remove **all legacy code** (Material 2 components, Accompanist artifacts, custom headers, hardcoded colors/spacings).  
- Delete unused resources/styles/icons tied to old design.  
- Enforce **permanent bans** on reintroduction via lint/detekt rules + CI.  
- Produce a short `REMOVAL_REPORT.md`.

---

## 5.1 Removal steps
1. **Run a final scan**  
   - Reuse the Phase 2 search commands.  
   - Confirm zero hits for:  
     - `androidx.compose.material`  
     - `com.google.accompanist.*`  
     - Legacy headers (`Surface+Row` for app bars)  
     - `MaterialTheme.colors`  
     - Raw `Color(...)` where a token exists  
     - Magic dp spacings  

2. **Delete old resources**  
   - Remove `accompanist-swiperefresh` from Gradle if still present.  
   - Delete any legacy XML styles, drawables, or colors no longer referenced.  
   - Clean up unused `ui/legacy/` or equivalent directories.

3. **Deprecate adapters**  
   - If Phase 3 created temporary adapter components, remove them now.  
   - Replace calls with proper Material 3 APIs.

---

## 5.2 Enforcement guardrails
- Strengthen **Detekt rules** (or Ktlint custom rules):  
  ```yaml
  style:
    ForbiddenImport:
      active: true
      imports:
        - "androidx.compose.material"
        - "com.google.accompanist.swiperefresh"
  ```  
- Add **Gradle build checks** to fail if legacy deps are declared.  
- Optional: add a GitHub Action / CI step that greps for forbidden imports and fails.

---

## 5.3 Verification
- Run `./gradlew build` → must pass.  
- Run `./gradlew test` → must be green.  
- Re-run scanners → **zero hits**.  
- Check `SCREEN_CHECKLIST.md` → all screens migrated.  

---

## 5.4 Deliverables
- **`REMOVAL_REPORT.md`** with:  
  - Files deleted  
  - Deps removed  
  - Adapters removed  
  - Confirmation of guardrails in place  
- Clean PR: `refactor: remove legacy code and enforce guardrails`

---

✅ **Definition of Done**  
- No legacy imports/resources/adapters remain.  
- Repo builds/tests pass.  
- CI guardrails prevent regressions.  
- `REMOVAL_REPORT.md` documents cleanup.
