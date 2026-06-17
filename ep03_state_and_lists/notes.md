# Episode 3 — State & Lists

**Series:** Android Development from Scratch
**Channel:** The Debugger Diary
**Prerequisites:** Episodes 1–2 (setup, Compose UI basics)

---

## Episode Overview

This episode covers the most important concept in Jetpack Compose: state. You will learn why local variables don't work for UI state, how `remember` and `mutableStateOf` make state survive recomposition, what actually triggers recomposition, the state hoisting pattern, and how to build efficient scrollable lists with `LazyColumn`. The demo is a fully interactive to-do list where you can add and remove items.

---

## Section 1: Why Compose Needs Explicit State

In the old View system, a `TextView` held its own text internally — you called `setText("hello")` and the view remembered it. Compose works differently. A composable function is just a function: every time Compose runs it (recomposition), all local variables are re-initialized.

```kotlin
// BROKEN — count resets to 0 on every recomposition
@Composable
fun BrokenCounter() {
    var count = 0
    Button(onClick = { count++ }) {
        Text("Count: $count")  // always shows "Count: 0"
    }
}
```

The click increments `count`, which causes Compose to recompose `BrokenCounter`, which re-declares `count = 0`. The increment is lost.

The fix is `remember` + `mutableStateOf`.

---

## Section 2: `remember` and `mutableStateOf`

```kotlin
@Composable
fun Counter() {
    // mutableStateOf creates a State<Int> that Compose observes
    // remember stores it across recompositions
    var count by remember { mutableStateOf(0) }

    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

**`mutableStateOf(initialValue)`** — creates a `MutableState<T>` object. When you write to it, Compose is notified and schedules a recomposition of any composable that read it.

**`remember { }`** — runs the lambda once (on first composition) and caches the result. On subsequent recompositions, `remember` returns the cached value instead of re-running the lambda. The value is discarded when the composable leaves the composition (e.g., screen is navigated away from).

**`by` delegate** — `var count by remember { mutableStateOf(0) }` uses Kotlin property delegation so you can write `count` and `count = 5` instead of `count.value` and `count.value = 5`. Import `getValue` and `setValue` from `androidx.compose.runtime`.

### `rememberSaveable` — surviving rotation

`remember` is lost on configuration changes (screen rotation, language change). Use `rememberSaveable` to persist state across those:

```kotlin
var count by rememberSaveable { mutableStateOf(0) }
```

`rememberSaveable` saves to the saved instance state bundle automatically for primitives and `Parcelable` types. For custom types, provide a `Saver`.

---

## Section 3: What Triggers Recomposition

Compose tracks which `State` objects a composable reads during its execution. When a `State` object changes, Compose marks all composables that read it as invalid and schedules them for recomposition.

Key insight: only composables that **read** a changed state are recomposed. Siblings that don't read it are not touched.

```kotlin
@Composable
fun ParentScreen() {
    var name by remember { mutableStateOf("Alice") }
    var count by remember { mutableStateOf(0) }

    // Only recomposes when `name` changes
    NameDisplay(name = name)

    // Only recomposes when `count` changes
    CountDisplay(count = count)
}
```

This is why breaking UI into small, focused composables is a performance optimization — each recomposes independently.

**What does NOT trigger recomposition:**
- Reading a `State` after composition (e.g., inside a click handler lambda)
- Reading regular Kotlin variables (not `State`)
- Passing the same value — if `name` was "Alice" and you set it to "Alice" again, no recomposition

---

## Section 4: State Hoisting

State hoisting is the pattern of moving state up from a composable to its caller, making the composable stateless.

**Before hoisting (stateful):**
```kotlin
@Composable
fun SearchBar() {
    var query by remember { mutableStateOf("") }
    OutlinedTextField(value = query, onValueChange = { query = it })
}
```

This is hard to test and hard to connect to other components (how do you observe `query` from a parent?).

**After hoisting:**
```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(value = query, onValueChange = onQueryChange)
}

@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    SearchBar(query = query, onQueryChange = { query = it })
    // now query is accessible here — can filter a list, pass to a ViewModel, etc.
}
```

**Rule:** Hoist state to the lowest common ancestor of all composables that need to read or write it.

A composable that takes value + callback instead of owning state is called **stateless**. Stateless composables are:
- Easier to preview (just pass test values)
- Easier to test (no internal state to set up)
- Reusable in more contexts

---

## Section 5: Observable Lists — `mutableStateListOf`

For a list of items, you need Compose to observe not just whether the list reference changed, but whether the list's contents changed (items added, removed, moved).

```kotlin
// WRONG — adding an item does NOT trigger recomposition
val items = remember { mutableStateOf(mutableListOf("a", "b")) }
items.value.add("c")  // list mutated, but State reference unchanged

// CORRECT — mutableStateListOf is Compose-aware
val items = remember { mutableStateListOf("a", "b") }
items.add("c")   // ✅ Compose is notified, UI updates
items.remove("b") // ✅
items[0] = "z"   // ✅
```

`mutableStateListOf` wraps a `SnapshotStateList` which notifies Compose of any structural change.

---

## Section 6: `LazyColumn` and `LazyRow`

`Column` composes all its children at once — fine for small, fixed lists. For any list that could be long or dynamic, use `LazyColumn`:

```kotlin
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    // Static header item
    item {
        Text("My Tasks", style = MaterialTheme.typography.headlineSmall)
    }

    // Dynamic items from a list
    items(
        items = todoItems,
        key = { item -> item.id }  // stable unique key
    ) { item ->
        TodoRow(item = item, onRemove = { todoItems.remove(item) })
    }

    // Static footer
    item {
        Text("${todoItems.size} tasks total", style = MaterialTheme.typography.labelSmall)
    }
}
```

**`key`** — tells Compose how to identify items when the list changes. Without it, Compose recomposes every visible item on any change. With a stable key (an ID, not the index), Compose can animate insertions and deletions and only recompose affected items.

**`LazyRow`** — same API, but scrolls horizontally.

**`LazyVerticalGrid`** — grid layout with a configurable number of columns:
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2)
) {
    items(items) { item -> GridCell(item) }
}
```

---

## Section 7: TextField

Compose `TextField` is a controlled input — you manage the state and pass it back.

```kotlin
var text by remember { mutableStateOf("") }

OutlinedTextField(
    value = text,
    onValueChange = { newValue -> text = newValue },
    label = { Text("Task name") },
    placeholder = { Text("Enter a task...") },
    singleLine = true,
    trailingIcon = {
        if (text.isNotEmpty()) {
            IconButton(onClick = { text = "" }) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear")
            }
        }
    },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = {
        if (text.isNotBlank()) {
            // submit
        }
    }),
    modifier = Modifier.fillMaxWidth()
)
```

**`keyboardOptions`** and **`keyboardActions`** let you control the IME (on-screen keyboard) action button and respond to it — crucial for submit-on-Enter behavior.

---

## Section 8: Full To-Do Screen

```kotlin
@Composable
fun TodoScreen(modifier: Modifier = Modifier) {
    val items = remember { mutableStateListOf<String>() }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("New task") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        items.add(inputText.trim())
                        inputText = ""
                    }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No tasks yet. Add one above!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = items, key = { it }) { item ->
                    TodoItem(
                        text = item,
                        onRemove = { items.remove(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoItem(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove $text",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

---

## Key Takeaways

- Local variables reset on recomposition — use `remember { mutableStateOf(...) }` for persistent state
- `by` delegate (`var x by remember { mutableStateOf(...) }`) avoids `.value` boilerplate
- Compose recomposes only the composables that read changed state — break UI into small functions
- State hoisting: move state up, pass `value` + `onValueChange` down to keep composables stateless and reusable
- Use `mutableStateListOf` for observable lists — regular `mutableListOf` won't trigger recomposition
- `LazyColumn` is lazy — use it for any list that might scroll; always provide a stable `key`
- `rememberSaveable` survives configuration changes (rotation); plain `remember` does not

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| UI doesn't update when list is modified | Using `mutableListOf` instead of `mutableStateListOf` | Replace with `remember { mutableStateListOf(...) }` |
| State resets on screen rotation | Using `remember` instead of `rememberSaveable` | Switch to `rememberSaveable` for state that should survive rotation |
| `LazyColumn` items flash/reorder on insert | No `key` parameter provided | Add `key = { item -> item.id }` to the `items()` call |
| TextField doesn't update | Forgetting `onValueChange = { text = it }` | Compose TextField is controlled — you must update state in `onValueChange` |
| State not shared between two composables | State owned by a child | Hoist state to the common ancestor of the two composables |

---

## Further Reading

- developer.android.com/jetpack/compose/state — official state docs
- developer.android.com/jetpack/compose/lists — LazyColumn reference
- developer.android.com/jetpack/compose/state-hoisting — hoisting pattern
- Episode 4: Navigation & Multi-screen Apps — NavController, NavHost, routes
