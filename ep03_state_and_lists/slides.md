---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
---

# State & Lists
### Episode 3 — Android Development from Scratch
**The Debugger Diary**

---

# What You'll Build

An interactive to-do list — add items with a text field, remove them with a button

```
┌────────────────────────────┐
│  [  Enter a task...  ] [+] │
├────────────────────────────┤
│  Buy groceries        [✕]  │
│  Write unit tests     [✕]  │
│  Push to GitHub       [✕]  │
└────────────────────────────┘
```

---

# The Problem Without State

```kotlin
@Composable
fun Counter() {
    var count = 0  // ❌ resets to 0 on every recomposition
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

Compose re-runs composable functions when inputs change. A local variable is discarded and recreated each time — it has no memory.

---

# `remember` + `mutableStateOf`

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Count: $count")  // ✅ updates when count changes
    }
}
```

- `mutableStateOf(0)` — creates a `State<Int>` that Compose observes
- `remember { }` — survives recomposition (but NOT rotation/process death)
- `by` delegate — reads/writes `count` directly instead of `count.value`

---

# What Triggers Recomposition?

Compose **only** recomposes when a `State` it reads changes.

```kotlin
var nameState by remember { mutableStateOf("Alice") }
var ageState  by remember { mutableStateOf(30) }

// This composable only recomposes when nameState changes
@Composable
fun NameLabel() { Text(nameState) }

// This composable only recomposes when ageState changes
@Composable
fun AgeLabel() { Text("$ageState") }
```

This is why breaking UI into small composables improves performance — each one recomposes independently.

---

# State Hoisting

Move state **up** to the caller so composables are stateless and reusable.

```kotlin
// Stateless — takes value and event callback
@Composable
fun CounterButton(count: Int, onIncrement: () -> Unit) {
    Button(onClick = onIncrement) { Text("Count: $count") }
}

// Stateful parent owns the state
@Composable
fun CounterScreen() {
    var count by remember { mutableStateOf(0) }
    CounterButton(count = count, onIncrement = { count++ })
}
```

**Rule:** Hoist state to the **lowest common ancestor** of composables that need it.

---

# `mutableStateListOf`

For lists, use `mutableStateListOf` — a Compose-aware list that triggers recomposition on any change.

```kotlin
val items = remember { mutableStateListOf("Buy groceries", "Write tests") }

// add
items.add("New task")

// remove
items.remove("Buy groceries")

// Compose observes the list — UI updates automatically
```

Regular `mutableListOf` wrapped in `mutableStateOf` will NOT trigger recomposition on `.add()` or `.remove()`.

---

# `LazyColumn`

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(
        items = todoItems,
        key = { item -> item }  // stable key prevents flicker on reorder
    ) { item ->
        TodoItem(text = item, onRemove = { todoItems.remove(item) })
    }
}
```

`LazyColumn` only composes visible items — essential for long lists. The `key` parameter lets Compose identify items on reorder/insert.

---

# TextField

```kotlin
var text by remember { mutableStateOf("") }

OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Enter a task") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
```

Compose `TextField` is **controlled** — you own the state and pass it back in `value`. Never store the value inside the `TextField` itself.

---

# The Full To-Do Screen

```kotlin
@Composable
fun TodoScreen() {
    val items = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (input.isNotBlank()) { items.add(input); input = "" }
            }) { Text("+") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn { items(items, key = { it }) { item ->
            TodoItem(text = item, onRemove = { items.remove(item) })
        }}
    }
}
```

---

# Key Takeaways

- Local variables in composables reset on every recomposition — use `remember`
- `mutableStateOf` creates observable state; Compose recomposes when it changes
- State hoisting = move state up, pass value + callback down
- Use `mutableStateListOf` for observable lists
- `LazyColumn` composes only visible items — use it for any list that might scroll
- Provide a stable `key` to `LazyColumn` items to prevent unnecessary recomposition

---

# What's Next — Episode 4

**Navigation & Multi-screen Apps**

- `NavController` and `NavHost`
- Defining routes with `composable()`
- `navigate()` and `popBackStack()`
- Passing arguments between screens
- Bottom navigation bar

**The Debugger Diary** — Android Development from Scratch
_"Understand the tools, not just the syntax."_
