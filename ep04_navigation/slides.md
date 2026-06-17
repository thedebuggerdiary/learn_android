---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
---

# Navigation & Multi-screen Apps
### Episode 4 — Android Development from Scratch
**The Debugger Diary**

---

# What You'll Build

A two-screen task app: a list screen that navigates to a detail screen

```
List Screen          Detail Screen
┌──────────────┐    ┌──────────────┐
│ Tasks        │    │ ← Back       │
│              │    │              │
│ • Buy milk ──────►│ Buy milk     │
│ • Read book  │    │              │
│ • Call mom   │    │ Task #1      │
└──────────────┘    └──────────────┘
```

---

# Add the Dependency

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("androidx.navigation:navigation-compose:2.7.x")
}
```

Three main classes you'll use:
- `NavController` — remembers the back stack, drives navigation
- `NavHost` — the container that swaps composables based on route
- `composable()` — registers a composable at a route string

---

# Define Routes as Constants

```kotlin
object Routes {
    const val LIST   = "list"
    const val DETAIL = "detail/{taskId}"

    fun detail(taskId: Int) = "detail/$taskId"
}
```

Routes are just strings. Define them as constants to avoid typos.
For routes with arguments, use `{paramName}` placeholders.

---

# NavHost — The Screen Container

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIST
    ) {
        composable(Routes.LIST) {
            ListScreen(onTaskClick = { id ->
                navController.navigate(Routes.detail(id))
            })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.IntType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
            DetailScreen(taskId = taskId, onBack = { navController.popBackStack() })
        }
    }
}
```

---

# navigate() and popBackStack()

```kotlin
// Push a new screen onto the back stack
navController.navigate(Routes.detail(42))

// Pop the current screen — equivalent to pressing Back
navController.popBackStack()

// Navigate and clear everything below up to a destination
navController.navigate(Routes.LIST) {
    popUpTo(Routes.LIST) { inclusive = true }
}
```

`popUpTo` with `inclusive = true` removes the destination itself too — useful for "logout → back to login" flows.

---

# Passing Arguments

```kotlin
// Route definition with type info
composable(
    route = "detail/{taskId}",
    arguments = listOf(
        navArgument("taskId") { type = NavType.IntType }
    )
) { backStackEntry ->
    val taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
    DetailScreen(taskId = taskId)
}
```

Supported types: `IntType`, `StringType`, `BoolType`, `FloatType`, `LongType`.

For complex objects — pass an ID and let the detail screen fetch the data from a shared source (ViewModel, database, or repository).

---

# List Screen

```kotlin
@Composable
fun ListScreen(onTaskClick: (Int) -> Unit) {
    val tasks = listOf(
        Task(1, "Buy milk"),
        Task(2, "Read book"),
        Task(3, "Call mom")
    )
    LazyColumn {
        items(tasks, key = { it.id }) { task ->
            ListItem(
                headlineContent = { Text(task.title) },
                modifier = Modifier.clickable { onTaskClick(task.id) }
            )
            HorizontalDivider()
        }
    }
}
```

---

# Detail Screen

```kotlin
@Composable
fun DetailScreen(taskId: Int, onBack: () -> Unit) {
    val task = remember(taskId) {
        listOf(
            Task(1, "Buy milk"),
            Task(2, "Read book"),
            Task(3, "Call mom")
        ).find { it.id == taskId }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
        task?.let {
            Text(it.title, style = MaterialTheme.typography.headlineMedium)
            Text("Task #${it.id}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

---

# Bottom Navigation Bar

```kotlin
NavigationBar {
    NavigationBarItem(
        selected = currentRoute == Routes.LIST,
        onClick = { navController.navigate(Routes.LIST) },
        icon = { Icon(Icons.Filled.List, contentDescription = "Tasks") },
        label = { Text("Tasks") }
    )
    NavigationBarItem(
        selected = currentRoute == Routes.PROFILE,
        onClick = { navController.navigate(Routes.PROFILE) },
        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
        label = { Text("Profile") }
    )
}
```

---

# Key Takeaways

- `NavController` owns the back stack; `NavHost` displays the current screen
- Routes are strings — define them as constants to avoid typos
- Use `{paramName}` in routes and `navArgument()` for typed arguments
- Pass IDs between screens, not full objects — let the destination fetch data
- `popBackStack()` navigates back; `popUpTo()` clears the stack to a destination
- `NavigationBar` + `NavigationBarItem` builds a bottom nav bar

---

# What's Next — Episode 5

**ViewModel & MVVM Architecture**

- Why MVVM — separating UI from logic
- `ViewModel` and `viewModelScope`
- `StateFlow` + `collectAsState()` in Compose
- `UiState` sealed class pattern
- State that survives screen rotation

**The Debugger Diary** — Android Development from Scratch
_"Understand the tools, not just the syntax."_
