# StudyPlan Settings Architecture

## 📱 Overview
Comprehensive Android settings architecture implementing Material Design 3 with Jetpack Compose, following MVVM pattern and using reactive state management.

## 🏗️ Architecture Components

### 1. **Data Models** (`models/SettingsModels.kt`)
- `SettingsCategory` - Main settings grid categories
- `SettingItem` - Sealed class for different setting types:
  - `Toggle` - Switch settings
  - `Clickable` - Navigation settings
  - `Selection` - Dropdown settings
  - `Header` - Section headers
  - `Divider` - Visual separators
- `SettingsUiState` - UI state management

### 2. **Data Layer** (`data/SettingsPreferencesManager.kt`)
- Type-safe SharedPreferences wrapper
- Reactive Flow-based state updates
- Settings categories:
  - **Privacy**: Profile visibility, progress sharing
  - **Notifications**: Push notifications, study reminders, achievement alerts, email summaries
  - **Tasks**: Smart scheduling, auto-difficulty, daily goals, weekend mode
  - **Navigation**: Bottom navigation, haptic feedback
  - **Gamification**: Streak tracking, points/rewards, celebrations, risk warnings
  - **Social**: Social features, leaderboards, study groups

### 3. **ViewModels** (`viewmodel/SettingsViewModel.kt`)
- Separate ViewModels for each settings category
- Reactive state management with StateFlow
- Business logic separation
- ViewModelFactory for dependency injection

### 4. **UI Layer** (`ui/SettingsScreens.kt`)
- Material Design 3 components
- Smooth animations and transitions
- Reusable composable components
- Icon-based settings items

### 5. **Integration Layer** (`SettingsIntegration.kt`)
- Singleton pattern for app-wide access
- Helper functions for easy integration
- Reactive settings consumption

## 🎨 UI Features

### Main Settings Screen
- Grid layout with 6 categories
- Material Design 3 styling
- Reset actions section
- Version information with branding

### Individual Category Screens
- **Privacy Settings**:
  - Profile Visibility (dropdown: Public, Friends Only, Private)
  - Progress Sharing (toggle)

- **Tasks Settings**:
  - Smart Scheduling (toggle)
  - Auto Difficulty Adjustment (toggle)
  - Daily Goal Reminders (toggle)
  - Weekend Mode (toggle)

- **Navigation Settings**:
  - Bottom Navigation (toggle)
  - Haptic Feedback (toggle)

- **Notifications Settings**:
  - Push Notifications (toggle)
  - Study Reminders (toggle)
  - Achievement Alerts (toggle)
  - Email Summaries (toggle)

- **Gamification Settings**:
  - Streak Tracking (toggle)
  - Points & Rewards (toggle)
  - Celebration Effects (toggle)
  - Streak Risk Warnings (toggle)

- **Social Settings**:
  - Social Features (toggle)
  - Leaderboards (toggle)
  - Study Groups (toggle)

## 🔄 Navigation Integration

### Routes
- Main: `"settings"`
- Categories: `"settings/{category}"` where category is:
  - `privacy`
  - `notifications`
  - `tasks`
  - `navigation`
  - `gamification`
  - `social`

### AppNavHost.kt Integration
```kotlin
// Main settings screen
composable("settings") {
    SettingsScreen(
        onNavigateToCategory = { route -> navController.navigate(route) },
        onBack = { navController.popBackStack() }
    )
}

// Category screens
composable("settings/privacy") {
    PrivacySettingsScreen(onBack = { navController.popBackStack() })
}
// ... other categories
```

## 🎯 Key Features

### Reactive State Management
- All settings changes are persisted immediately
- UI updates reactively to state changes
- Type-safe preference access

### Material Design 3
- Proper theming and color schemes
- Rounded corners and modern styling
- Smooth animations and transitions
- Proper spacing and typography

### Icon System
- Context-aware icons for each setting
- Consistent visual language
- Material Design iconography

### Reset Functions
- Reset Progress (Danger) action
- Proper error handling and confirmation

## 🚀 Usage

### Basic Integration
```kotlin
// Get settings integration instance
val settings = rememberSettingsIntegration()

// Listen to specific settings
val isHapticEnabled by settings.isHapticFeedbackEnabled().collectAsState(initial = true)

// Use in UI
if (isHapticEnabled) {
    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}
```

### ViewModel Usage
```kotlin
// In a settings screen
val viewModel: PrivacySettingsViewModel = viewModel(
    factory = SettingsViewModelFactory(preferencesManager)
)
val uiState by viewModel.uiState.collectAsState()
```

## 📱 Design Compliance

The implementation matches the provided screenshots exactly:
- ✅ Privacy screen with Profile Visibility dropdown set to "Friends Only"
- ✅ Tasks screen with all 4 toggles (Smart Scheduling, Auto Difficulty, Daily Goals, Weekend Mode)
- ✅ Navigation screen with Bottom Navigation and Haptic Feedback toggles
- ✅ Notifications screen with 4 notification settings
- ✅ Gamification screen with streak, points, celebration, and warning settings
- ✅ Main screen with Reset buttons and version info
- ✅ Proper icons and Material Design 3 styling
- ✅ Rounded corners and consistent spacing

## 🔧 Extensibility

Adding new settings:
1. Add to `SettingsPreferencesManager` data class
2. Create ViewModel methods
3. Add UI items in ViewModel's build method
4. Settings automatically persist and sync

The architecture is designed for easy extension and maintenance while following Android best practices.
