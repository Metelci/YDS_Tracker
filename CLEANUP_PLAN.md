# Code Cleanup Plan - Remove Duplicate Implementations

## Current Status
- ✅ Build: **SUCCESS** with MainActivitySimple + Compose Navigation
- ⚠️ Issue: Multiple duplicate implementations causing confusion

## Files to Remove

### 1. Backup Files (.bak)
```
app/src/main/java/com/mtlc/studyplan/social/SocialScreen.kt.bak
app/src/main/java/com/mtlc/studyplan/ui/animations/AppAnimations.kt.bak
```

### 2. Legacy Fragment-based UI (Unused with Compose approach)
```
app/src/main/java/com/mtlc/studyplan/settings/ui/BackupSettingsFragment.kt
app/src/main/java/com/mtlc/studyplan/settings/ui/BaseSettingsFragment.kt
app/src/main/java/com/mtlc/studyplan/settings/ui/ConflictResolutionFragment.kt
app/src/main/java/com/mtlc/studyplan/settings/ui/GamificationSettingsFragment.kt
app/src/main/java/com/mtlc/studyplan/settings/ui/NotificationSettingsFragment.kt
app/src/main/java/com/mtlc/studyplan/settings/ui/PrivacySettingsFragment.kt
app/src/main/java/com/mtlc/studyplan/settings/ui/SettingsDetailFragment.kt
```

### 3. Duplicate Navigation Systems
**Keep:** AppNavHost.kt, SimplifiedAppNavHost.kt (active in MainActivitySimple)
**Remove:**
```
app/src/main/java/com/mtlc/studyplan/navigation/NavigationBadgeManager.kt (references unresolved nav_* constants)
app/src/main/java/com/mtlc/studyplan/navigation/NavigationStateManager.kt (references unresolved TaskSortOrder)
app/src/main/java/com/mtlc/studyplan/navigation/StudyPlanNavigationManager.kt (references unresolved DeepLinkParams)
```

### 4. Duplicate MainActivity variants
**Keep:** MainActivitySimple.kt (active launcher)
**Remove:**
```
app/src/main/java/com/mtlc/studyplan/MinimalMainActivity.kt (duplicate, simpler version)
```

### 5. Related XML Layouts (if they exist for removed Fragments)
Will check and remove unused layouts in res/layout/

## Execution Steps

### Step 1: Remove .bak files
```powershell
Remove-Item "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\social\SocialScreen.kt.bak"
Remove-Item "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\ui\animations\AppAnimations.kt.bak"
```

### Step 2: Remove Fragment files
```powershell
$fragmentFiles = @(
    "BackupSettingsFragment.kt",
    "BaseSettingsFragment.kt",
    "ConflictResolutionFragment.kt",
    "GamificationSettingsFragment.kt",
    "NotificationSettingsFragment.kt",
    "PrivacySettingsFragment.kt",
    "SettingsDetailFragment.kt"
)
foreach ($file in $fragmentFiles) {
    Remove-Item "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\settings\ui\$file"
}
```

### Step 3: Remove duplicate navigation files
```powershell
Remove-Item "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\navigation\NavigationBadgeManager.kt"
Remove-Item "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\navigation\NavigationStateManager.kt"
Remove-Item "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\navigation\StudyPlanNavigationManager.kt"
```

### Step 4: Remove duplicate MainActivity
```powershell
Remove-Item "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\MinimalMainActivity.kt"
```

### Step 5: Check for related XML layouts
```powershell
Get-ChildItem -Path "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\res\layout" -Filter "*fragment*.xml"
Get-ChildItem -Path "C:\Users\Metelci\StudioProjects\StudyPlan\app\src\main\res\layout" -Filter "*settings*.xml"
```

### Step 6: Verify build still works
```powershell
.\gradlew.bat assembleDebug
```

## What Stays (Active Codebase)

### ✅ Entry Point
- MainActivitySimple.kt

### ✅ Navigation
- AppNavHost.kt (comprehensive)
- SimplifiedAppNavHost.kt (used by MainActivitySimple)
- NavigationDestination.kt (data models)
- BadgeDataProvider.kt (if still needed)

### ✅ UI Screens (Compose)
- core/WorkingHomeScreen.kt
- core/WorkingTasksScreen.kt
- All other Compose screens in feature/*, social/*, etc.

### ✅ Settings (Compose-based)
- settings/ui/EnhancedSettingsScreen.kt
- settings/ui/EnhancedSettingsComponents.kt
- settings/ui/SettingsScreens.kt
- settings/ui/CategoryScreens.kt

## Post-Cleanup Verification

1. ✅ Build succeeds
2. ✅ No broken imports
3. ✅ App launches on device
4. ✅ Navigation works
5. ✅ Settings accessible

## Rollback Plan

If anything breaks:
```bash
git checkout HEAD -- <problematic-file>
```

Or restore from this commit before cleanup.
