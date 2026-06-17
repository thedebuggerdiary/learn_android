# Episode 5 — ViewModel & MVVM Architecture

**Series:** Android Development from Scratch
**Channel:** The Debugger Diary
**Prerequisites:** Episodes 1–4 (setup, Compose UI, state, navigation)

---

## Episode Overview

This episode introduces the ViewModel and the MVVM (Model-View-ViewModel) architecture pattern. You will learn why composable-owned state breaks on rotation, how ViewModel survives the screen lifecycle, how `StateFlow` connects the ViewModel to Compose via `collectAsState()`, and how to structure screen state with a `UiState` data class. The demo refactors the notes list from Episode 3 to use a proper ViewModel.

---

## Section 1: The Rotation Problem

When the user rotates their device, Android destroys and recreates the Activity. This also discards all `remember` state in your composables.

```kotlin
// This state is LOST on rotation
@Composable
fun NotesScreen() {
    val notes = remember { mutableStateListOf<String>() }
    // all notes disappear when screen rotates
}
```

For ephemeral UI state (which tab is selected, whether a dialog is open) `remember` is fine. For meaningful app data (the list of notes the user wrote), you need state that outlives the Activity.

---

## Section 2: ViewModel Lifecycle

`ViewModel` is an Android architecture component that lives as long as the screen is in scope — it survives rotation, theme changes, and any other configuration change. It is only cleared when the user navigates away from the screen permanently (or the app process is killed).

```
Activity lifecycle:   onCreate → onDestroy (rotation) → onCreate → onDestroy (user leaves)
ViewModel lifecycle:  created                                       cleared
```

The `ViewModel` is stored in a `ViewModelStore` owned by the Activity/Fragment, not by the composable. When the Activity is recreated after rotation, it reconnects to the same `ViewModelStore` and gets the same `ViewModel` instance back.

Add the dependency:
```kotlin
// build.gradle.kts (app)
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.x")
```

---

## Section 3: Creating a ViewModel

```kotlin
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotesViewModel : ViewModel() {

    // Private mutable state — only this ViewModel can write
    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    // Public read-only state — UI observes this
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    fun addNote(title: String) {
        if (title.isBlank()) return
        val newNote = Note(
            id = System.currentTimeMillis(),
            title = title.trim()
        )
        _notes.update { currentList -> currentList + newNote }
    }

    fun deleteNote(id: Long) {
        _notes.update { currentList -> currentList.filter { it.id != id } }
    }
}
```

**Notice:** `NotesViewModel` has zero Android imports. It imports only Kotlin and Jetpack lifecycle/coroutines classes. This is intentional — keeping the ViewModel free of Android framework dependencies makes it unit testable without an emulator or robolectric.

---

## Section 4: StateFlow

`StateFlow` is a Kotlin coroutines `Flow` with two extra guarantees:
1. It always has a current value (no waiting for the first emission)
2. It only emits when the value actually changes (it deduplicates identical values)

```kotlin
val flow = MutableStateFlow(0)

// Reading the current value synchronously
println(flow.value)   // 0

// Writing
flow.value = 5
flow.update { it + 1 }  // atomic read-modify-write

// Collecting (in a coroutine)
flow.collect { value -> println(value) }
```

**`update { }`** is preferred over direct `.value = ` assignment because it is atomic — safe if multiple coroutines are updating the same `StateFlow` concurrently.

---

## Section 5: collectAsState() in Compose

To use a `StateFlow` in a composable, call `.collectAsState()`. This subscribes to the flow and returns a Compose `State<T>`, triggering recomposition whenever the flow emits a new value.

```kotlin
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = viewModel()  // get or create ViewModel
) {
    val notes by viewModel.notes.collectAsState()
    // `notes` is now a plain List<Note> — use it like any other value
}
```

`viewModel()` (from `androidx.lifecycle:lifecycle-viewmodel-compose`) retrieves the existing `ViewModel` instance for this screen or creates one if it doesn't exist yet. It is scoped to the nearest `ViewModelStoreOwner` in the composition tree (usually the current `NavBackStackEntry` or `Activity`).

---

## Section 6: UiState Pattern

As screens get more complex, you end up with multiple pieces of state: the data, a loading flag, an error message. Instead of three separate `StateFlow` objects, consolidate into a single `UiState`:

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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.getAllNotes()
                _uiState.update { it.copy(notes = result, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}
```

In the composable:
```kotlin
val uiState by viewModel.uiState.collectAsState()

when {
    uiState.isLoading -> CircularProgressIndicator()
    uiState.errorMessage != null -> ErrorText(uiState.errorMessage!!)
    else -> NotesList(notes = uiState.notes)
}
```

---

## Section 7: viewModelScope

`viewModelScope` is a `CoroutineScope` provided by the `ViewModel` base class. Coroutines launched in it are automatically cancelled when the ViewModel is cleared.

```kotlin
class NotesViewModel : ViewModel() {
    fun addNoteAsync(title: String) {
        viewModelScope.launch {
            delay(100)  // simulate async work
            _notes.update { it + Note(id = System.currentTimeMillis(), title = title) }
        }
    }

    fun loadFromNetwork() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = api.fetchNotes()  // network call on IO thread
            _notes.update { result }       // StateFlow update — thread-safe
        }
    }
}
```

Never launch coroutines in composables directly for business logic — put them in the ViewModel so they are lifecycle-aware.

---

## Section 8: Full Notes ViewModel Demo

```kotlin
data class Note(val id: Long, val title: String)

class NotesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun addNote(title: String) {
        if (title.isBlank()) return
        _notes().add(Note(id = System.currentTimeMillis(), title = title.trim()))
        _uiState.update { it.copy(notes = _notes().toList()) }
    }

    fun deleteNote(id: Long) {
        _uiState.update { it.copy(notes = it.notes.filter { n -> n.id != id }) }
    }

    private fun _notes() = _uiState.value.notes.toMutableList()
}

// Simpler direct approach with just a list StateFlow:
class NotesViewModelSimple : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    fun addNote(title: String) {
        if (title.isBlank()) return
        _notes.update { it + Note(id = System.currentTimeMillis(), title = title.trim()) }
    }

    fun deleteNote(id: Long) {
        _notes.update { it.filter { note -> note.id != id } }
    }
}
```

---

## Key Takeaways

- `ViewModel` survives screen rotation — move state here for anything that should persist across configuration changes
- `MutableStateFlow` (private, writable) + `StateFlow` (public, read-only) is the standard state exposure pattern
- `collectAsState()` connects a `StateFlow` to Compose — any new emission triggers recomposition
- `UiState` data class consolidates loading, error, and data into one observable object
- `viewModelScope.launch { }` — coroutines auto-cancelled when ViewModel is cleared, no leaks
- ViewModel should have no Android framework imports — keeps it unit testable

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `Cannot create instance of ViewModel` (in test) | ViewModel has constructor params without a Factory | Use `ViewModelProvider.Factory` or inject with Hilt (ep8) |
| State lost on rotation despite using ViewModel | State stored in `remember` inside a composable, not the ViewModel | Move `mutableStateOf` / `mutableStateListOf` into the ViewModel |
| UI doesn't update after ViewModel state change | Using `LiveData` without `.observeAsState()`, or `Flow` without `.collectAsState()` | Use `StateFlow` + `.collectAsState()` |
| `viewModel()` creates a new instance each recomposition | Using `viewModel()` inside a nested composable that is recreated frequently | Hoist `viewModel()` call to the screen-level composable and pass the instance down |

---

## Further Reading

- developer.android.com/topic/architecture/ui-layer — official UI layer guide
- developer.android.com/kotlin/coroutines/coroutines-best-practices — coroutines + ViewModel
- kotlinlang.org/docs/flow — Kotlin Flow documentation
- Episode 6: Local Data with Room — persist ViewModel data to SQLite
