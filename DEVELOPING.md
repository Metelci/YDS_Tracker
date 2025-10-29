# 🛠️ Developer Guide — Material 3 Development

This guide explains how to develop and extend the YDS Tracker app using **Material 3** components and the established design system.

## Dependencies

This project uses Gradle as the build system. Dependencies are managed in the `build.gradle.kts` files and the `libs.versions.toml` file.

For a comprehensive overview of all project dependencies and their purposes, see [DEPENDENCIES.md](./DEPENDENCIES.md).

### Dependency Verification
To check the current dependency tree for conflicts or updates:
- Run `./gradlew app:dependencies` to see the dependency tree
- Run `./gradlew dependencyInsight --dependency <dependency-name>` for specific dependency details
- Generated dependency reports are stored in the `deps.txt`, `deps2.txt`, `deps3.txt` files (for archiving/debugging purposes)

---

## 📋 Table of Contents
- [Quick Start](#quick-start)
- [Material 3 Components](#material-3-components)
- [Design System](#design-system)
- [Code Examples](#code-examples)
- [Do Not Use](#do-not-use)
- [Testing Guidelines](#testing-guidelines)
- [Common Patterns](#common-patterns)

---

## 🚀 Quick Start

### Prerequisites
- Android Studio (latest stable)
- Understanding of Jetpack Compose
- Familiarity with Material 3 design principles

### Setting Up
1. Clone the repository and open in Android Studio
2. Sync Gradle and ensure all dependencies are resolved
3. Run the app to see Material 3 components in action
4. Read this guide before making UI changes

### Analytics & Data Layer Modules (October 2025)
- AnalyticsEngine delegates to focused services (StudyPatternAnalyzer, AnalyticsMetricsCalculator, ProductivityInsightCalculator, RecommendationGenerator, AchievementTracker). Extend these helpers when adding new analytics instead of growing a monolith.
- PlanRepository now receives an Android Context via Koin. Avoid reviving the old PlanDataSource.initialize path; rely on DI in production code and swap the module in tests when needed.
- Register new analytics or data dependencies through the existing Koin modules (AppModule, RepositoryModule) to keep the graph consistent across app and tests.

### Key Files to Know
```
app/src/main/java/com/mtlc/studyplan/ui/theme/
├── DesignTokens.kt          # Color and design tokens
├── ShapeTokens.kt           # Shape radius constants
├── LocalSpacing.kt          # Spacing system
├── Theme.kt                 # Material 3 theme setup
└── Typography.kt            # Typography definitions
```

---

## 🎨 Material 3 Components

### Always Use These Material 3 Components

#### Navigation
```kotlin
// ✅ Material 3 NavigationBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem

NavigationBar {
    NavigationBarItem(
        selected = selected,
        onClick = { /* navigate */ },
        icon = { Icon(Icons.Default.Home, contentDescription = null) },
        label = { Text("Home") }
    )
}
```

#### Tab Navigation
```kotlin
// ✅ Material 3 PrimaryTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab

PrimaryTabRow(
    selectedTabIndex = selectedIndex
) {
    Tab(
        selected = selected,
        onClick = { /* switch tab */ },
        text = { Text("Tab Title") }
    )
}
```

#### App Bars
```kotlin
// ✅ Material 3 TopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

TopAppBar(
    title = { Text("Screen Title") },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    actions = {
        IconButton(onClick = { /* action */ }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
)
```

#### Cards and Surfaces
```kotlin
// ✅ Material 3 Card
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    shape = RoundedCornerShape(ShapeTokens.RadiusLg)
) {
    // Card content
}
```

#### Buttons
```kotlin
// ✅ Material 3 Buttons
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton

// Primary actions
Button(
    onClick = { /* action */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    )
) {
    Text("Primary Action")
}

// Secondary actions
FilledTonalButton(onClick = { /* action */ }) {
    Text("Secondary Action")
}

// Tertiary actions
OutlinedButton(onClick = { /* action */ }) {
    Text("Tertiary Action")
}
```

---

## 🎨 Design System

### Colors
**Always use MaterialTheme.colorScheme for colors:**

```kotlin
// ✅ Correct - Material 3 dynamic colors
Text(
    text = "Hello World",
    color = MaterialTheme.colorScheme.onSurface
)

Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
) { /* content */ }

// ✅ Custom design tokens when needed
Surface(
    color = DesignTokens.Primary,
    contentColor = DesignTokens.PrimaryForeground
) { /* content */ }
```

### Spacing
**Use LocalSpacing.current for consistent spacing:**

```kotlin
@Composable
fun MyScreen() {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier.padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        // ✅ Consistent spacing throughout the app
    }
}
```

Available spacing values:
- `spacing.xs` (4.dp)
- `spacing.sm` (8.dp)
- `spacing.md` (16.dp)
- `spacing.lg` (24.dp)
- `spacing.xl` (32.dp)

### Shapes
**Use ShapeTokens for consistent corner radius:**

```kotlin
Surface(
    shape = RoundedCornerShape(ShapeTokens.RadiusLg),
    modifier = Modifier.clip(RoundedCornerShape(ShapeTokens.RadiusMd))
) { /* content */ }
```

Available shape tokens:
- `ShapeTokens.RadiusSm` (8.dp)
- `ShapeTokens.RadiusMd` (12.dp)
- `ShapeTokens.RadiusMdPlus` (18.dp)
- `ShapeTokens.RadiusLg` (20.dp)
- `ShapeTokens.RadiusXl` (24.dp)
- `ShapeTokens.RadiusXxl` (28.dp)

### Typography
**Use MaterialTheme.typography for text styles:**

```kotlin
Text(
    text = "Heading",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onSurface
)

Text(
    text = "Body text",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

---

## 💻 Code Examples

### Complete Screen Example
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleScreen() {
    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Example Screen") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            items(items) { item ->
                ExampleCard(item = item)
            }
        }
    }
}

@Composable
private fun ExampleCard(item: Item) {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(ShapeTokens.RadiusLg)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Custom Component with Material 3
```kotlin
@Composable
fun CustomProgressCard(
    title: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DesignTokens.PrimaryContainer
        ),
        shape = RoundedCornerShape(ShapeTokens.RadiusLg)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DesignTokens.PrimaryContainerForeground
            )

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = DesignTokens.Primary,
                trackColor = DesignTokens.Surface
            )
        }
    }
}
```

---

## ❌ Do Not Use

### Forbidden Material 2 Imports
```kotlin
// ❌ NEVER use Material 2
import androidx.compose.material.* // Forbidden
import com.google.accompanist.swiperefresh.* // Forbidden

// ❌ Old components
BottomNavigation { } // Use NavigationBar instead
TabRow { } // Use PrimaryTabRow instead
```

### Forbidden Practices
```kotlin
// ❌ Hard-coded colors
Text(color = Color.Blue) // Use MaterialTheme.colorScheme instead

// ❌ Hard-coded spacing
Modifier.padding(16.dp) // Use LocalSpacing.current.md instead

// ❌ Magic numbers for shapes
RoundedCornerShape(12.dp) // Use ShapeTokens.RadiusMd instead

// ❌ Raw color values
Color(0xFF1976D2) // Use design tokens or theme colors
```

### Legacy Components to Avoid
- `BottomNavigation` → Use `NavigationBar`
- `TabRow` → Use `PrimaryTabRow`
- `TopAppBar` (Material 2) → Use `TopAppBar` (Material 3)
- `Scaffold` (Material 2) → Use `Scaffold` (Material 3)
- Any `androidx.compose.material.*` import → Use `androidx.compose.material3.*`

---

## 🧪 Testing Guidelines

### Compose UI Testing
```kotlin
@Test
fun testMaterial3Component() {
    composeTestRule.setContent {
        MaterialTheme {
            ExampleScreen()
        }
    }

    // Test Material 3 components
    composeTestRule
        .onNodeWithText("Example Screen")
        .assertIsDisplayed()
}
```

### Theme Testing
```kotlin
@Test
fun testLightAndDarkTheme() {
    // Test light theme
    composeTestRule.setContent {
        StudyPlanTheme(darkTheme = false) {
            ExampleCard(item = sampleItem)
        }
    }

    // App uses light theme only
}
```

---

## 🔄 Common Patterns

### Screen with Tabs
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Tab 1", "Tab 2", "Tab 3")

    Column {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> Tab1Content()
            1 -> Tab2Content()
            2 -> Tab3Content()
        }
    }
}
```

### Pull-to-Refresh (Material 3)
```kotlin
// Note: Material 3 pull-to-refresh implementation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent() {
    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            // Perform refresh
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
    ) {
        // Content
    }
}
```

### Navigation with Material 3
```kotlin
@Composable
fun MainNavigationBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        listOf(
            NavigationItem("Home", Icons.Default.Home),
            NavigationItem("Tasks", Icons.Default.List),
            NavigationItem("Progress", Icons.Default.Analytics),
            NavigationItem("Settings", Icons.Default.Settings)
        ).forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}
```

---

## 🔍 Code Review Checklist

Before submitting a PR with UI changes, ensure:

- [ ] **No Material 2 imports** (`androidx.compose.material.*`)
- [ ] **Material 3 components used** (`androidx.compose.material3.*`)
- [ ] **Colors from theme** (`MaterialTheme.colorScheme.*` or `DesignTokens.*`)
- [ ] **Consistent spacing** (`LocalSpacing.current.*`)
- [ ] **Shape tokens used** (`ShapeTokens.*`)
- [ ] **Typography from theme** (`MaterialTheme.typography.*`)
- [ ] **Light theme tested**
- [ ] **Accessibility content descriptions added**
- [ ] **No hard-coded values** (colors, spacing, shapes)

---

This guide ensures consistent Material 3 development across the YDS Tracker app. For questions or clarifications, refer to the migration documentation in `.claude/` or create an issue.
