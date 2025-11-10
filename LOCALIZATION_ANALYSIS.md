# Localization Analysis: "This Week Schedule" Section

## Executive Summary

The "This Week Schedule" section in `WorkingTasksScreen.kt` displays Turkish day names that are hardcoded in the data source. String resources already exist but are not being used for day name display.

---

## Location of Display

**File**: `H:\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\core\WorkingTasksScreen.kt`

### WeeklyScheduleHeader (Lines 1207-1214)
- Title text: Uses `R.string.tasks_week_schedule_title` (localized)
- Translation: "Bu Haftanın Programı" (Turkish)

### WeeklyDayCard Component (Lines 1391-1483)
- Displays day names from `DayPlan.day` property
- **Line 1448**: `Text(day.day, ...)` - NOT using string resources
- Day names: Pazartesi, Salı, Çarşamba, Perşembe, Cuma, Cumartesi, Pazar

---

## Current Day Highlighting Logic

### Detection (Line 981)
```kotlin
val currentDayOfWeek = remember { LocalDate.now().dayOfWeek.value }
```
- Returns 1-7 where 1=Monday, 7=Sunday

### Comparison (Line 1015)
```kotlin
isToday = index + 1 == currentDayOfWeek
```

### Visual Highlighting (Lines 1452-1466)
- Card background: `primaryContainer` color when isToday=true
- Number circle: `primary` color (highlighted)
- "BUGÜN" badge appears next to day name
- Badge text: Uses `R.string.tasks_week_today_badge` (localized)

---

## Hardcoded Day Names Source

**File**: `H:\StudioProjects\StudyPlan\app\src\main\java\com\mtlc\studyplan\data\PlanDataSource.kt`

### All 7 days hardcoded in Turkish:
```
DayPlan("Pazartesi", ...)
DayPlan("Salı", ...)
DayPlan("Çarşamba", ...)
DayPlan("Perşembe", ...)
DayPlan("Cuma", ...)
DayPlan("Cumartesi", ...)
DayPlan("Pazar", ...)
```

### Data Model
```kotlin
data class DayPlan(val day: String, val tasks: List<PlanTask>)
```

---

## Existing String Resources

**File**: `H:\StudioProjects\StudyPlan\app\src\main\res\values-tr\strings.xml` (Lines 73-79)

Already defined but NOT USED:
```xml
<string name="monday">Pazartesi</string>
<string name="tuesday">Salı</string>
<string name="wednesday">Çarşamba</string>
<string name="thursday">Perşembe</string>
<string name="friday">Cuma</string>
<string name="saturday">Cumartesi</string>
<string name="sunday">Pazar</string>
```

---

## Localization Issues

1. **CRITICAL**: Day names hardcoded in DayPlan objects
2. **CRITICAL**: No localization layer between display and data
3. **HIGH**: Cannot support other languages without code changes
4. **HIGH**: 210+ instances across 30 weeks of lesson plans

---

## Key Code Sections

**WeeklyDayCard - Day Name Display (Line 1448)**
```kotlin
Text(
    day.day,  // Hardcoded string - NOT localized
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold
)
```

**Today Badge Display (Lines 1458-1464)**
```kotlin
Text(
    stringResource(R.string.tasks_week_today_badge),  // Properly localized
    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onPrimary
)
```

**Card Highlighting for Today (Lines 1403-1409)**
```kotlin
colors = if (isToday) {
    CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )
} else {
    CardDefaults.cardColors()
}
```

---

## Recommended Solution

Create mapping function to use existing string resources:

```kotlin
fun getLocalizedDayName(turkishDayName: String, context: Context): String {
    return when(turkishDayName) {
        "Pazartesi" -> context.getString(R.string.monday)
        "Salı" -> context.getString(R.string.tuesday)
        "Çarşamba" -> context.getString(R.string.wednesday)
        "Perşembe" -> context.getString(R.string.thursday)
        "Cuma" -> context.getString(R.string.friday)
        "Cumartesi" -> context.getString(R.string.saturday)
        "Pazar" -> context.getString(R.string.sunday)
        else -> turkishDayName
    }
}
```

Apply in WeeklyDayCard:
```kotlin
Text(
    getLocalizedDayName(day.day, LocalContext.current)
)
```

