---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
---

# ViewModel & MVVM Architecture
### Episode 5 — Android Development from Scratch
**The Debugger Diary**

---

# The Problem

Your composable currently does everything:
- Owns UI state
- Runs business logic
- Formats data for display

When the screen rotates, the Activity is **recreated** — all `remember` state is lost.

---

# MVVM Architecture

```
┌────────────────────────────────────────────┐
│  UI Layer                                  │
│  Composable reads state, sends events      │
└──────────────┬─────────────────▲───────────┘
               │ events          │ UiState
┌──────────────▼─────────────────┴───────────┐
│  ViewModel                                 │
│  Holds state, runs logic, survives rotation│
└────────────────────────────────────────────┘
```

- **Composable** — dumb UI. Reads `UiState`, calls event handlers.
- **ViewModel** — owns state. Survives rotation. No Android imports.

---

# ViewModel

```kotlin
class NotesViewModel : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    fun addNote(title: String) {
        _notes.update { currentList ->
            currentList + Note(id = System.currentTimeMillis(), title = title)
        }
    }

    fun deleteNote(id: Long) {
        _notes.update { it.filter { note -> note.id != id } }
    }
}
```

`ViewModel` is tied to the screen lifecycle — it survives rotation and only clears when the screen is permanently gone.

---

# MutableStateFlow vs StateFlow

```kotlin
// Private mutable — only ViewModel can write
private val _notes = MutableStateFlow<List<Note>>(emptyList())

// Public read-only — UI can only read
val notes: StateFlow<List<Note>> = _notes.asStateFlow()
```

This pattern prevents the UI from directly mutating ViewModel state — it must call a function on the ViewModel instead.

---

# collectAsState() — Connecting to Compose

```kotlin
@Composable
fun NotesScreen(viewModel: NotesViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsState()

    // `notes` is a regular List<Note> here
    // Compose recomposes whenever the StateFlow emits a new value
    LazyColumn {
        items(notes, key = { it.id }) { note ->
            NoteItem(note = note, onDelete = { viewModel.deleteNote(note.id) })
        }
    }
}
```

`viewModel()` — the Compose integration function that retrieves or creates the ViewModel, scoped to the current screen.

---

# UiState Pattern

Instead of separate flows for every piece of state, use a single `UiState`:

```kotlin
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class NotesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun loadNotes() {
        _uiState.update { it.copy(isLoading = true) }
        // ... fetch notes ...
        _uiState.update { it.copy(isLoading = false, notes = result) }
    }
}
```

---

# Unidirectional Data Flow

```
User taps "Add"
       │
       ▼
viewModel.addNote(title)
       │
       ▼
_notes.update { ... }
       │
       ▼
StateFlow emits new list
       │
       ▼
collectAsState() triggers recomposition
       │
       ▼
LazyColumn shows updated list
```

Data flows in one direction only — easier to reason about and debug.

---

# viewModelScope for Coroutines

```kotlin
class NotesViewModel : ViewModel() {
    fun loadFromNetwork() {
        viewModelScope.launch {
            val result = api.fetchNotes()  // suspend function
            _notes.update { result }
        }
    }
}
```

`viewModelScope` is a `CoroutineScope` that is automatically cancelled when the ViewModel is cleared — no memory leaks, no manually managing coroutine lifecycles.

---

# Key Takeaways

- `ViewModel` survives screen rotation — move state out of composables for anything important
- `MutableStateFlow` (private) + `StateFlow` (public) = safe state exposure pattern
- `collectAsState()` bridges `StateFlow` to Compose recomposition
- `UiState` data class consolidates all screen state into one observable object
- `viewModelScope.launch { }` runs coroutines tied to the ViewModel's lifecycle
- Never import Android framework classes in ViewModel — keep it testable

---

# What's Next — Episode 6

**Local Data with Room**

- `@Entity`, `@Dao`, `@Database`
- CRUD operations with coroutines
- `Flow<List<T>>` from Room — live database queries
- Repository pattern

**The Debugger Diary** — Android Development from Scratch
_"Understand the tools, not just the syntax."_
