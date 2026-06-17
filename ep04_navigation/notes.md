# Episode 4 — Navigation & Multi-screen Apps

**Series:** Android Development from Scratch
**Channel:** The Debugger Diary
**Prerequisites:** Episodes 1–3 (setup, Compose UI, state and lists)

---

## Episode Overview

This episode adds navigation to your apps. You will learn how Jetpack Navigation for Compose works, how to define routes, pass typed arguments between screens, manage the back stack, and build a bottom navigation bar. The demo is a task list app where tapping a task navigates to a detail screen.

---

## Section 1: Why a Navigation Library?

Android has always had a back stack — the system tracks which screens the user visited and presses the hardware/gesture back button to go back. In the View system this was managed via `FragmentManager`. Compose has no fragments; instead, Jetpack Navigation for Compose manages a `NavController` that maps route strings to composables and handles the back stack.

Benefits over DIY state-based navigation:
- Back gesture/button handled automatically
- Deep link support (open a specific screen from a URL or notification)
- Type-safe argument passing
- Compatible with the system back gesture animation

Add the dependency:
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("androidx.navigation:navigation-compose:2.7.x")
}
```

---

## Section 2: NavController and NavHost

**`NavController`** is the navigator. It maintains the back stack and exposes `navigate()`, `popBackStack()`, and the current state.

**`NavHost`** is a composable that renders whichever composable the `NavController` is currently pointing to. It swaps composables (screens) in and out as you navigate.

```kotlin
@Composable
fun AppNavigation() {
    // rememberNavController ties the NavController to the composition lifecycle
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "list"  // shown on first launch
    ) {
        // register each screen with composable()
        composable("list") {
            ListScreen(navController = navController)
        }
        composable("detail") {
            DetailScreen(navController = navController)
        }
    }
}
```

Call `AppNavigation()` from `MainActivity`'s `setContent`.

---

## Section 3: Defining Routes

Routes are URL-like strings. Keep them in one place to avoid typos:

```kotlin
object Routes {
    const val LIST   = "list"
    const val DETAIL = "detail/{taskId}"

    // Helper to build the concrete URL for navigation
    fun detail(taskId: Int) = "detail/$taskId"
}
```

The `{taskId}` placeholder is a named argument. You fill it in when navigating: `navController.navigate(Routes.detail(42))` produces the string `"detail/42"`.

---

## Section 4: Passing Arguments

When registering a route with arguments, declare the argument types using `navArgument`:

```kotlin
NavHost(navController, startDestination = Routes.LIST) {
    composable(Routes.LIST) {
        ListScreen(onTaskClick = { id ->
            navController.navigate(Routes.detail(id))
        })
    }

    composable(
        route = Routes.DETAIL,
        arguments = listOf(
            navArgument("taskId") {
                type = NavType.IntType
                defaultValue = 0  // optional fallback
            }
        )
    ) { backStackEntry ->
        val taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
        DetailScreen(
            taskId = taskId,
            onBack = { navController.popBackStack() }
        )
    }
}
```

**Supported NavType values:** `NavType.IntType`, `NavType.StringType`, `NavType.BoolType`, `NavType.FloatType`, `NavType.LongType`.

**For complex objects:** Don't try to pass a serialized object through the route string. Pass an ID instead and let the destination look it up from a shared ViewModel, repository, or database. This is cleaner and handles process death correctly.

---

## Section 5: Navigate and Back Stack

```kotlin
// Push DetailScreen onto the back stack
navController.navigate(Routes.detail(42))

// Pop the current screen (equivalent to hardware back)
navController.popBackStack()

// Navigate and pop everything back to a given destination (inclusive = pop that one too)
navController.navigate(Routes.LIST) {
    popUpTo(Routes.LIST) { inclusive = true }
}

// Navigate without adding to back stack (replace current screen)
navController.navigate(Routes.PROFILE) {
    launchSingleTop = true  // avoid duplicate screens on rapid taps
}
```

**`launchSingleTop = true`** — if the destination is already at the top of the stack, don't push another copy. Use this for bottom navigation items.

---

## Section 6: Keeping NavController out of Composables

Passing `NavController` directly into leaf composables makes them hard to test. Instead, pass callback lambdas:

```kotlin
// Good — composable only knows about events, not the navigator
@Composable
fun ListScreen(
    tasks: List<Task>,
    onTaskClick: (Int) -> Unit
)

// Bad — composable is coupled to NavController
@Composable
fun ListScreen(navController: NavController)
```

The `NavHost` lambda is the only place that should reference `navController` directly. Everything else gets a callback.

---

## Section 7: Bottom Navigation

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    // Track which route is currently shown
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.LIST,
                    onClick = {
                        navController.navigate(Routes.LIST) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Tasks") }
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.PROFILE,
                    onClick = {
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LIST) { ListScreen(onTaskClick = { navController.navigate(Routes.detail(it)) }) }
            composable(Routes.PROFILE) { ProfileScreen() }
            composable(Routes.DETAIL, arguments = listOf(navArgument("taskId") { type = NavType.IntType })) {
                DetailScreen(
                    taskId = it.arguments?.getInt("taskId") ?: 0,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

The `saveState = true` and `restoreState = true` options preserve the scroll position and state of each tab when you switch between them.

---

## Section 8: Full Demo — List and Detail Screens

```kotlin
data class Task(val id: Int, val title: String, val description: String)

val sampleTasks = listOf(
    Task(1, "Buy milk", "Get 2% from the corner store"),
    Task(2, "Read book", "Finish chapter 5 of Clean Code"),
    Task(3, "Call mom", "Sunday evening works best")
)

@Composable
fun ListScreen(onTaskClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Text(
                "My Tasks",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(sampleTasks, key = { it.id }) { task ->
            ListItem(
                headlineContent = { Text(task.title) },
                supportingContent = { Text(task.description) },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                },
                modifier = Modifier.clickable { onTaskClick(task.id) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun DetailScreen(
    taskId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task = remember(taskId) { sampleTasks.find { it.id == taskId } }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Task Detail", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider()
        if (task != null) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(task.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(task.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("ID: ${task.id}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Task not found")
            }
        }
    }
}
```

---

## Key Takeaways

- `rememberNavController()` creates and remembers a `NavController` tied to the composition
- `NavHost` registers all screens via `composable(route) { }` — only one screen is shown at a time
- Pass typed arguments in the route string using `{paramName}` and `navArgument()`
- Pass IDs, not objects, between screens — let the destination look up data
- Keep `NavController` out of leaf composables — use callbacks instead
- `launchSingleTop = true` prevents duplicate back stack entries from bottom nav tabs

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `IllegalArgumentException: Navigation destination not found` | Route string doesn't match a registered `composable()` | Check `Routes` constants match exactly what's in `NavHost` |
| Arguments come back as null | Missing `navArgument()` declaration | Add `arguments = listOf(navArgument("name") { type = ... })` to `composable()` |
| Back press exits the app instead of going back | `popBackStack()` on the start destination | Check if `navController.previousBackStackEntry != null` before calling |
| Bottom nav state lost on tab switch | Missing `saveState`/`restoreState` in navigate options | Add `saveState = true` and `restoreState = true` to the `navigate { }` block |

---

## Further Reading

- developer.android.com/jetpack/compose/navigation — official navigation docs
- developer.android.com/guide/navigation/backstack — back stack management
- Episode 5: ViewModel & MVVM Architecture — state that survives rotation
