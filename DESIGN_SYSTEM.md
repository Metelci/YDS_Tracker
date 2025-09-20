# 🎨 Design System Documentation — Material 3

This document describes the complete design system for the YDS Tracker app, including Material 3 integration, design tokens, and component mapping.

---

## 📋 Table of Contents
- [Overview](#overview)
- [Material 3 Integration](#material-3-integration)
- [Design Tokens](#design-tokens)
- [Component Mapping](#component-mapping)
- [Color System](#color-system)
- [Typography System](#typography-system)
- [Spacing System](#spacing-system)
- [Shape System](#shape-system)
- [Usage Guidelines](#usage-guidelines)

---

## 🌟 Overview

The YDS Tracker app uses a design system built on **Material 3** principles with custom design tokens for consistency and maintainability. The system provides:

- **Consistent visual language** across all screens
- **Accessibility compliance** with proper contrast ratios
- **Dark mode support** with dynamic theming
- **Scalable component library** for future development
- **Design token system** for easy customization

---

## 🎨 Material 3 Integration

### Migration Status
This app has been fully migrated from Material 2 to Material 3:

| Component Category | Status | Details |
|-------------------|--------|---------|
| **Navigation** | ✅ Complete | NavigationBar, PrimaryTabRow implemented |
| **App Bars** | ✅ Complete | TopAppBar (Material 3) |
| **Buttons** | ✅ Complete | Button, FilledTonalButton, OutlinedButton |
| **Cards & Surfaces** | ✅ Complete | Card, Surface with Material 3 styling |
| **Colors** | ✅ Complete | MaterialTheme.colorScheme.* |
| **Typography** | ✅ Complete | MaterialTheme.typography.* |
| **Pull-to-Refresh** | ⚠️ In Progress | Migrating from Material 2 to Material 3 |

### Core Material 3 Components Used
```kotlin
// Navigation
NavigationBar { NavigationBarItem(...) }
PrimaryTabRow { Tab(...) }

// App Structure
TopAppBar(...)
Scaffold(...)

// Content
Card(...)
Surface(...)
LinearProgressIndicator(...)

// Actions
Button(...)
FilledTonalButton(...)
OutlinedButton(...)
IconButton(...)
```

---

## 🎯 Design Tokens

Design tokens are located in `app/src/main/java/com/mtlc/studyplan/ui/theme/DesignTokens.kt`:

### Primary Colors
```kotlin
object DesignTokens {
    // Primary Brand Colors
    val Primary = Color(0xFF1976D2)
    val PrimaryContainer = Color(0xFFE3F2FD)
    val PrimaryForeground = Color(0xFFFFFFFF)
    val PrimaryContainerForeground = Color(0xFF0D47A1)

    // Secondary Colors
    val Secondary = Color(0xFF388E3C)
    val SecondaryContainer = Color(0xFFE8F5E8)
    val SecondaryForeground = Color(0xFFFFFFFF)
    val SecondaryContainerForeground = Color(0xFF1B5E20)

    // Tertiary Colors
    val Tertiary = Color(0xFFE65100)
    val TertiaryContainer = Color(0xFFFFF3E0)
    val TertiaryContainerForeground = Color(0xFFBF360C)

    // Status Colors
    val Success = Color(0xFF4CAF50)
    val SuccessContainer = Color(0xFFE8F5E8)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)

    // Surface Colors
    val Surface = Color(0xFFFFFBFF)
    val SurfaceContainer = Color(0xFFF7F2FA)
    val SurfaceContainerHigh = Color(0xFFF1ECF4)
    val SurfaceVariant = Color(0xFFE7E0EC)

    // Content Colors
    val CardForeground = Color(0xFF1C1B1F)
}
```

### Usage in Components
```kotlin
// ✅ Using design tokens
Card(
    colors = CardDefaults.cardColors(
        containerColor = DesignTokens.PrimaryContainer,
        contentColor = DesignTokens.PrimaryContainerForeground
    )
)

// ✅ Using Material 3 theme colors
Text(
    text = "Hello World",
    color = MaterialTheme.colorScheme.onSurface
)
```

---

## 🔄 Component Mapping

Complete mapping from Material 2 to Material 3 components:

### Navigation Components
| Material 2 | Material 3 | Status |
|------------|------------|--------|
| `BottomNavigation` | `NavigationBar` | ✅ Migrated |
| `BottomNavigationItem` | `NavigationBarItem` | ✅ Migrated |
| `TabRow` | `PrimaryTabRow` | ✅ Migrated |
| `Tab` | `Tab` (Material 3) | ✅ Migrated |

### App Structure
| Material 2 | Material 3 | Status |
|------------|------------|--------|
| `TopAppBar` | `TopAppBar` (Material 3) | ✅ Migrated |
| `Scaffold` | `Scaffold` (Material 3) | ✅ Migrated |

### Content Components
| Material 2 | Material 3 | Status |
|------------|------------|--------|
| `Card` | `Card` (Material 3) | ✅ Migrated |
| `Surface` | `Surface` (Material 3) | ✅ Migrated |

### Actions
| Material 2 | Material 3 | Status |
|------------|------------|--------|
| `Button` | `Button` (Material 3) | ✅ Migrated |
| `OutlinedButton` | `OutlinedButton` (Material 3) | ✅ Migrated |
| N/A | `FilledTonalButton` | ✅ New Component |

### Pull-to-Refresh
| Material 2 / Accompanist | Material 3 | Status |
|--------------------------|------------|--------|
| `com.google.accompanist.swiperefresh.*` | Material 3 pull-to-refresh | ⚠️ In Progress |

---

## 🎨 Color System

### Material 3 Color Scheme
The app uses Material 3's dynamic color system with proper light/dark mode support:

```kotlin
// Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = DesignTokens.Primary,
    onPrimary = DesignTokens.PrimaryForeground,
    primaryContainer = DesignTokens.PrimaryContainer,
    onPrimaryContainer = DesignTokens.PrimaryContainerForeground,
    secondary = DesignTokens.Secondary,
    // ... full color scheme
)

// Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    // ... full dark color scheme
)
```

### Color Usage Guidelines
```kotlin
// ✅ Correct - Use theme colors
Text(color = MaterialTheme.colorScheme.onSurface)
Card(colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer
))

// ✅ Correct - Use design tokens for custom colors
Surface(color = DesignTokens.SuccessContainer)

// ❌ Wrong - Hard-coded colors
Text(color = Color.Blue)
Card(colors = CardDefaults.cardColors(
    containerColor = Color(0xFFE3F2FD)
))
```

---

## 📝 Typography System

Typography follows Material 3 type scale:

```kotlin
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    // ... complete type scale
)
```

### Typography Usage
```kotlin
// ✅ Headlines
Text(
    text = "Screen Title",
    style = MaterialTheme.typography.headlineMedium
)

// ✅ Body text
Text(
    text = "Content text",
    style = MaterialTheme.typography.bodyMedium
)

// ✅ Labels
Text(
    text = "Label",
    style = MaterialTheme.typography.labelMedium
)
```

---

## 📏 Spacing System

Consistent spacing system in `app/src/main/java/com/mtlc/studyplan/ui/theme/LocalSpacing.kt`:

```kotlin
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp
)

val LocalSpacing = compositionLocalOf { Spacing() }
```

### Spacing Usage
```kotlin
@Composable
fun MyComponent() {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier.padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        // Content with consistent spacing
    }
}
```

---

## 🔳 Shape System

Shape tokens in `app/src/main/java/com/mtlc/studyplan/ui/theme/ShapeTokens.kt`:

```kotlin
object ShapeTokens {
    val RadiusSm: Dp = 8.dp      // Small components
    val RadiusMd: Dp = 12.dp     // Medium components
    val RadiusMdPlus: Dp = 18.dp // Medium+ components
    val RadiusLg: Dp = 20.dp     // Large components
    val RadiusXl: Dp = 24.dp     // Extra large components
    val RadiusXxl: Dp = 28.dp    // Extra extra large components
}
```

### Shape Usage
```kotlin
// ✅ Cards
Card(
    shape = RoundedCornerShape(ShapeTokens.RadiusLg)
)

// ✅ Buttons
Button(
    shape = RoundedCornerShape(ShapeTokens.RadiusMd)
)

// ✅ Small surfaces
Surface(
    shape = RoundedCornerShape(ShapeTokens.RadiusSm)
)
```

---

## 📖 Usage Guidelines

### Component Guidelines

#### Navigation
```kotlin
// ✅ Main navigation
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface
) {
    items.forEach { item ->
        NavigationBarItem(
            selected = currentRoute == item.route,
            onClick = { navigate(item.route) },
            icon = { Icon(item.icon, contentDescription = item.label) },
            label = { Text(item.label) }
        )
    }
}

// ✅ Tab navigation
PrimaryTabRow(selectedTabIndex = selectedTab) {
    tabs.forEachIndexed { index, title ->
        Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = { Text(title) }
        )
    }
}
```

#### Cards and Content
```kotlin
// ✅ Standard card
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    shape = RoundedCornerShape(ShapeTokens.RadiusLg)
) {
    // Card content
}

// ✅ Highlighted card
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = DesignTokens.PrimaryContainer,
        contentColor = DesignTokens.PrimaryContainerForeground
    ),
    shape = RoundedCornerShape(ShapeTokens.RadiusLg)
) {
    // Highlighted content
}
```

#### Buttons
```kotlin
// ✅ Primary action
Button(
    onClick = { /* primary action */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    )
) {
    Text("Primary Action")
}

// ✅ Secondary action
FilledTonalButton(onClick = { /* secondary action */ }) {
    Text("Secondary Action")
}

// ✅ Tertiary action
OutlinedButton(onClick = { /* tertiary action */ }) {
    Text("Tertiary Action")
}
```

### Accessibility Guidelines

```kotlin
// ✅ Content descriptions
Icon(
    imageVector = Icons.Default.Settings,
    contentDescription = "Open Settings"
)

// ✅ Semantic labels
Button(
    onClick = { /* action */ },
    modifier = Modifier.semantics {
        contentDescription = "Save progress and continue"
    }
) {
    Text("Save")
}
```

### Dark Mode Support

```kotlin
// ✅ Theme-aware colors
Text(
    text = "Content",
    color = MaterialTheme.colorScheme.onSurface // Adapts to light/dark
)

// ✅ Custom theme-aware colors
val backgroundColor = if (isSystemInDarkTheme()) {
    DesignTokens.SurfaceContainer
} else {
    MaterialTheme.colorScheme.surface
}
```

---

## 🔗 Related Files

### Core Theme Files
- `app/src/main/java/com/mtlc/studyplan/ui/theme/Theme.kt` - Main theme configuration
- `app/src/main/java/com/mtlc/studyplan/ui/theme/DesignTokens.kt` - Custom design tokens
- `app/src/main/java/com/mtlc/studyplan/ui/theme/ShapeTokens.kt` - Shape system
- `app/src/main/java/com/mtlc/studyplan/ui/theme/LocalSpacing.kt` - Spacing system
- `app/src/main/java/com/mtlc/studyplan/ui/theme/Typography.kt` - Typography definitions

### Migration Documentation
- `.claude/mapping.json` - Complete component mapping
- `.claude/MIGRATION.md` - Migration process documentation
- `.claude/VALIDATION_REPORT.md` - Current validation status

---

This design system ensures consistent, accessible, and maintainable Material 3 UI across the YDS Tracker app.