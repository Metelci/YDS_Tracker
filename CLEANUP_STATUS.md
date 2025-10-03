# Code Cleanup Status Report

## ✅ Successfully Removed (12 files)

### Backup Files
- ✅ `social/SocialScreen.kt.bak`
- ✅ `ui/animations/AppAnimations.kt.bak`

### Legacy Fragment Files
- ✅ `settings/ui/BackupSettingsFragment.kt`
- ✅ `settings/ui/BaseSettingsFragment.kt`
- ✅ `settings/ui/ConflictResolutionFragment.kt`
- ✅ `settings/ui/GamificationSettingsFragment.kt`
- ✅ `settings/ui/NotificationSettingsFragment.kt`
- ✅ `settings/ui/PrivacySettingsFragment.kt`
- ✅ `settings/ui/SettingsDetailFragment.kt`

### Duplicate Navigation System Files
- ✅ `navigation/NavigationBadgeManager.kt`
- ✅ `navigation/NavigationStateManager.kt`
- ✅ `navigation/StudyPlanNavigationManager.kt`

### Duplicate MainActivity
- ✅ `MinimalMainActivity.kt`

### Related XML Layouts
- ✅ `res/layout/fragment_backup_settings.xml`
- ✅ `res/layout/fragment_base_settings.xml`
- ✅ `res/layout/fragment_settings_detail.xml`

**Total Removed: 15 files**

## ⚠️ Compilation Errors Introduced

The removed files had dependencies that need to be fixed:

### 1. StudyPlanNavigationManager References
**Files affected:**
- `feature/home/NewHomeScreen.kt:17` - imports removed class
- `social/SocialScreen.kt:79, 174` - uses removed class

**Fix needed:** Remove or replace with SimplifiedAppNavHost logic

### 2. MinimalMainActivity References  
**Files affected:**
- `notifications/NotificationManager.kt:12, 458, 471, 485, 521` - references removed activity

**Fix needed:** Replace all `MinimalMainActivity` with `MainActivitySimple`

### 3. Fragment References
**Files affected:**
- `settings/deeplink/SettingsDeepLinkHandler.kt:130-132, 172, 183` - references removed Fragments
- `settings/ui/SettingsActivity.kt:241` - references removed Fragment

**Fix needed:** 
- Update deep link handler to use Compose screens
- Update SettingsActivity to use Compose navigation

### 4. Navigation Types
**Files affected:**
- `navigation/NavigationDestination.kt:8, 10, 11` - references removed types (TaskFilter, TimeRange, SocialTab)

**Fix needed:** Define these types or simplify NavigationDestination

### 5. SocialScreen Deep Link Issue
**Files affected:**
- `social/SocialScreen.kt:92, 201` - references removed `SocialTab` and `deepLinkParams`

**Fix needed:** Already partially fixed, but needs complete cleanup of deep link logic

## 🔧 Required Fixes

### Priority 1: Fix Notification References
```kotlin
// In notifications/NotificationManager.kt
// Replace all: MinimalMainActivity -> MainActivitySimple
import com.mtlc.studyplan.MainActivitySimple

// Fix PendingIntent creation (lines 458, 471, 485, 521)
val intent = Intent(context, MainActivitySimple::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
}
```

### Priority 2: Fix or Remove Deep Link Handler
**Option A:** Remove if not actively used
```powershell
Remove-Item "app/src/main/java/com/mtlc/studyplan/settings/deeplink/SettingsDeepLinkHandler.kt"
```

**Option B:** Update to use Compose screens instead of Fragments

### Priority 3: Fix NavigationDestination.kt
Remove or simplify the deep link parameter types since the navigation manager is removed.

### Priority 4: Clean SocialScreen.kt
Remove all references to deep link params and StudyPlanNavigationManager.

## 📋 Recommended Next Steps

### Step 1: Fix Immediate Blockers (Est: 15 min)
```powershell
# Fix notifications to use MainActivitySimple
# This is straightforward find-replace
```

### Step 2: Remove or Update Deep Link System (Est: 10 min)
```powershell
# Either remove deep link handler entirely
# Or update it to work with Compose navigation
```

### Step 3: Verify Build (Est: 5 min)
```powershell
.\gradlew.bat assembleDebug
```

### Step 4: Test App (Est: 10 min)
```powershell
.\gradlew.bat installDebug
adb shell am start -n com.mtlc.studyplan/.MainActivitySimple
```

## Alternative: Rollback and Selective Cleanup

If fixing all dependencies takes too long, you can:

1. **Restore removed navigation files** (keep only one type of cleanup at a time)
2. **Focus on removing just .bak files and unused Fragments first**
3. **Deal with navigation consolidation in a separate task**

### Rollback Command
```bash
git checkout HEAD -- app/src/main/java/com/mtlc/studyplan/navigation/
```

## Summary

**What worked:** Successfully removed 15 legacy/duplicate files
**What broke:** 6 files have broken references to removed code
**Effort to fix:** ~40 minutes of focused work
**Alternative:** Rollback navigation changes, keep Fragment/backup removals

## Decision Point

**Choose one:**

A. ✅ **Fix all errors** - Complete the cleanup (recommended)
   - Pros: Clean codebase, single navigation system
   - Cons: 40 min of work to fix dependencies
   
B. ⏮️ **Partial rollback** - Restore navigation files, keep other cleanups
   - Pros: Quick fix (5 min), build works immediately  
   - Cons: Navigation duplication remains

C. 🔄 **Full rollback** - Restore everything, start fresh with better planning
   - Pros: Back to working state
   - Cons: No progress on cleanup goal

**Recommendation:** Choose option A and fix the errors. The cleanup is 80% done, fixing the remaining issues will complete the task properly.
