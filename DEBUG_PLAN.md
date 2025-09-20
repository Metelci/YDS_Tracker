# YDS_Tracker Debug Plan

**Date:** September 20, 2025
**Objective:** Systematically fix compilation errors and achieve successful build + runtime verification
**Based on:** AUDIT_RESULTS.md findings and recent progress

## Current Status Summary

### ✅ Recent Progress (User Fixes)
- ✅ SharedAppViewModel significantly improved with proper data classes
- ✅ NavigationBadgeManager simplified to remove compilation errors
- ✅ AppNavHost updated to use TodayRoute instead of missing TodayScreen
- ✅ Dependencies updated with navigation-compose library

### ❌ Remaining Critical Issues
1. **Missing Enum Definitions** - `TaskCategory`, `TaskDifficulty` referenced but not defined
2. **Settings Update Issues** - `SettingsUpdateRequest` compilation errors
3. **Navigation Managers** - NavigationStateManager and StudyPlanNavigationManager have unresolved references
4. **Route References** - TodayRoute function may not exist or have correct signature

---

## Phase 1: Core Data Structure Fixes (Priority: 🔴 Critical)

### Task 1.1: Define Missing Enums in SharedAppViewModel
**Target File:** `SharedAppViewModel.kt`
**Issue:** References to `TaskCategory` and `TaskDifficulty` but package path unclear

**Action Steps:**
1. Check if these enums exist elsewhere in codebase
2. If not, define them in SharedAppViewModel or separate file
3. Ensure all references use correct package paths

**Expected Outcome:** Remove compilation errors related to enum types

---

### Task 1.2: Fix SettingsUpdateRequest Issue
**Target File:** `SharedAppViewModel.kt` (lines 211-215)
**Issue:** Using `SettingsUpdateRequest` but may not exist or wrong signature

**Action Steps:**
1. Check if `SettingsUpdateRequest` class exists in settings.data package
2. If not, create it or fix the updateSetting method calls
3. Verify SettingsRepository.updateSetting method signature

**Expected Outcome:** Settings integration code compiles successfully

---

## Phase 2: Navigation Architecture Cleanup (Priority: 🟡 Important)

### Task 2.1: Fix NavigationStateManager
**Target File:** `NavigationStateManager.kt`
**Issue:** References to missing `TaskSortOrder`, `R.id.nav_home`, etc.

**Action Steps:**
1. Define missing `TaskSortOrder` enum or remove references
2. Replace `R.id.nav_*` references with Compose navigation equivalent
3. Consider simplifying or removing if not essential

**Expected Outcome:** Navigation state management compiles or is safely removed

---

### Task 2.2: Fix StudyPlanNavigationManager
**Target File:** `StudyPlanNavigationManager.kt`
**Issue:** Multiple unresolved references including `DeepLinkParams`, `emitNavigationRequested`

**Action Steps:**
1. Define missing classes/methods or remove unused code
2. Align with SharedAppViewModel's NavigationEvent system
3. Consider consolidating navigation logic

**Expected Outcome:** Clean navigation architecture without compilation errors

---

### Task 2.3: Verify TodayRoute Reference
**Target File:** `AppNavHost.kt` (line 437)
**Issue:** TodayRoute function may not exist with expected signature

**Action Steps:**
1. Verify TodayRoute exists in `feature.today` package
2. Check function signature matches usage (parameters)
3. Fix import or function call as needed

**Expected Outcome:** AppNavHost compiles successfully

---

## Phase 3: Build Verification (Priority: 🟢 Validation)

### Task 3.1: Incremental Build Testing
**Process:** Test compilation after each major fix
**Commands:**
```bash
./gradlew :app:compileDebugKotlin
./gradlew build --dry-run
```

**Expected Outcome:** Catch remaining issues early

---

### Task 3.2: Full Build Verification
**Process:** Complete clean build after all fixes
**Commands:**
```bash
./gradlew clean
./gradlew build
```

**Expected Outcome:** BUILD SUCCESS

---

## Phase 4: Runtime Verification (Priority: 🔵 Final)

### Task 4.1: App Launch Test
**Process:** Install and launch app
**Commands:**
```bash
./gradlew installDebug
# Launch app manually or via ADB
```

**Verification Points:**
- App launches without crash
- Main screen renders
- Basic navigation works

---

### Task 4.2: Core Screen Functionality
**Test Scenarios:**
1. **Today Screen:** Verify task list renders
2. **Tasks Screen:** Verify navigation and task display
3. **Progress Screen:** Verify stats display
4. **Settings Screen:** Verify settings load and update

**Expected Outcome:** All core screens functional

---

## Implementation Priority Order

### 🔴 Phase 1 - Critical Fixes (Must Complete First)
1. **Task 1.1:** Define TaskCategory/TaskDifficulty enums
2. **Task 1.2:** Fix SettingsUpdateRequest usage
3. **Task 2.3:** Verify TodayRoute reference

### 🟡 Phase 2 - Navigation Cleanup (Important)
4. **Task 2.1:** Fix NavigationStateManager
5. **Task 2.2:** Fix StudyPlanNavigationManager

### 🟢 Phase 3 & 4 - Verification (Post-Fix)
6. **Task 3.1:** Incremental build testing
7. **Task 3.2:** Full build verification
8. **Task 4.1:** App launch test
9. **Task 4.2:** Core functionality verification

---

## Success Criteria

### ✅ Build Success
- [ ] Zero compilation errors
- [ ] Zero unresolved references
- [ ] Clean Gradle build output

### ✅ Runtime Success
- [ ] App launches successfully
- [ ] No crash on startup
- [ ] Core screens render properly
- [ ] Basic navigation functional

### ✅ Code Quality
- [ ] No TODO/FIXME blocking core functionality
- [ ] Consistent architecture patterns
- [ ] Proper error handling

---

## Risk Assessment

### 🔴 High Risk
- **Unknown Dependencies:** Some classes may have complex dependency chains
- **Architecture Misalignment:** Mixed navigation patterns may require more extensive refactoring

### 🟡 Medium Risk
- **Testing Coverage:** Some fixes may introduce new issues
- **Performance Impact:** Navigation changes may affect app performance

### 🟢 Low Risk
- **Build Environment:** Already verified as stable and compatible
- **Core Logic:** Business logic appears sound, mostly architectural issues

---

## Estimated Timeline

| Phase | Tasks | Estimated Time | Notes |
|-------|-------|----------------|-------|
| **Phase 1** | Critical Fixes | 2-3 hours | Core compilation issues |
| **Phase 2** | Navigation Cleanup | 2-4 hours | May require architectural decisions |
| **Phase 3** | Build Verification | 30 minutes | Quick validation |
| **Phase 4** | Runtime Testing | 1-2 hours | Thorough functionality testing |
| **Total** | | **5.5-9.5 hours** | Conservative estimate |

---

## Next Immediate Actions

1. **START:** Begin with Task 1.1 (Define missing enums)
2. **VERIFY:** Run incremental build after each fix
3. **DOCUMENT:** Track progress and any new issues discovered
4. **ITERATE:** Adjust plan based on findings during implementation

This systematic approach should get the YDS_Tracker app from "cannot compile" to "fully functional" state.