# Episode 6 — Local Data with Room

**Series:** Android Development from Scratch
**Channel:** The Debugger Diary
**Prerequisites:** Episodes 1–5 (setup, Compose UI, state, navigation, ViewModel)

---

## Episode Overview

This episode adds persistent local storage to the notes app from Episode 5. You will learn how Room maps Kotlin data classes to SQLite tables, how to write DAO queries that return `Flow` for live UI updates, how to build a database singleton, and how the repository pattern cleanly separates the ViewModel from storage details.

---

## Section 1: Why Room?

Android apps can use SQLite directly, but the raw API is verbose and error-prone — you write strings for SQL, manually map `Cursor` rows to objects, and typos only surface at runtime. Room solves all of this:

- Tables are Kotlin `@Entity` data classes — plain Kotlin, no SQL boilerplate
- Queries are annotated functions on `@Dao` interfaces — SQL is validated at compile time
- `Flow<List<T>>` return type — the UI reacts automatically to data changes
- Coroutine support built in — `suspend` functions for all write operations

### Add dependencies

```kotlin
// build.gradle.kts (app)
plugins {
    id("com.google.devtools.ksp") version "1.9.x-1.0.x"
}

dependencies {
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
```

KSP (Kotlin Symbol Processing) is Room's annotation processor. It generates the implementation of your DAO and Database classes at build time.

---

## Section 2: @Entity — Defining a Table

Each class annotated with `@Entity` becomes a SQLite table. Properties become columns.

```kotlin
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

**`@PrimaryKey(autoGenerate = true)`** — Room assigns a unique integer ID on every insert. The default value `id = 0` is the sentinel that tells Room to generate a new ID (never reuse 0).

**`@ColumnInfo(name = ...)`** — renames the column in SQL. If omitted, the property name is used as the column name. Use it to follow SQL naming conventions (`snake_case`) without changing your Kotlin property names.

**`tableName`** — optional. If omitted, the class name is used. Always specify it explicitly so a Kotlin refactor (class rename) doesn't silently break your schema.

---

## Section 3: @Dao — Defining Queries

The DAO (Data Access Object) is an interface that declares all the database operations for a table.

```kotlin
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // Flow<List<Note>> — emits a new list every time the notes table changes
    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<Note>>

    // Suspend — must be called from a coroutine
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long  // returns the new row ID

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Note?
}
```

**`@Query`** — Room validates the SQL at compile time. If the table or column doesn't exist, the build fails with a helpful error.

**`OnConflictStrategy.REPLACE`** — if you insert a note with an existing ID, Room deletes the old row and inserts the new one. Useful for upsert patterns.

**Why `Flow` for reads but `suspend` for writes?**
- Reads: you want a long-lived subscription that emits whenever the data changes
- Writes: you want a one-shot operation that completes and returns

---

## Section 4: @Database — Wiring It Together

```kotlin
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = false  // set to true in production to track schema history
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**`@Volatile`** — ensures the `INSTANCE` variable is always read from main memory, not a thread-local cache. Combined with `synchronized`, this is the standard thread-safe singleton pattern in Kotlin.

**`context.applicationContext`** — always use the application context (not an Activity context) for the database. Activity contexts get garbage collected; the application context lives as long as the app process.

---

## Section 5: Repository Pattern

The repository is the single source of truth for data. It abstracts where data comes from (Room, network, cache) so the ViewModel doesn't need to know.

```kotlin
class NoteRepository(private val dao: NoteDao) {

    val allNotes: Flow<List<Note>> = dao.getAllNotes()

    suspend fun addNote(title: String) {
        dao.insert(Note(title = title))
    }

    suspend fun updateNote(note: Note) {
        dao.update(note)
    }

    suspend fun deleteNote(note: Note) {
        dao.delete(note)
    }
}
```

In Episode 8, you will add a network source to this repository. The ViewModel won't need any changes — only the repository changes.

---

## Section 6: ViewModel with Room

```kotlin
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    // Convert Flow to StateFlow for Compose — stays active while there are active collectors
    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addNote(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addNote(title)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}
```

**`SharingStarted.WhileSubscribed(5_000)`** — the upstream `Flow` (Room's live query) is active only while the `StateFlow` has subscribers. The 5-second grace period means the Room subscription survives brief pauses (like switching apps) without restarting.

### ViewModel Factory

Because `NotesViewModel` takes a constructor parameter (`repository`), you need a factory:

```kotlin
class NotesViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// In the composable
@Composable
fun NotesScreen() {
    val context = LocalContext.current
    val db = NoteDatabase.getInstance(context)
    val repository = NoteRepository(db.noteDao())
    val viewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(repository))
    // ...
}
```

Episode 8 replaces this manual wiring with Hilt dependency injection.

---

## Section 7: Database Migrations

Every time you change the schema (add a column, rename a table), you must increment `version` in `@Database` and provide a migration:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN is_done INTEGER NOT NULL DEFAULT 0")
    }
}

Room.databaseBuilder(context, NoteDatabase::class.java, "note_database")
    .addMigrations(MIGRATION_1_2)
    .build()
```

During development, it's common to use `fallbackToDestructiveMigration()` which drops and recreates the database on version mismatch. **Never use this in production** — it deletes all user data.

---

## Key Takeaways

- `@Entity` = table definition, `@Dao` = query methods, `@Database` = the database class
- Room validates SQL at compile time — typos in queries are build errors, not runtime crashes
- `Flow<List<T>>` from a `@Query` is a live query — Room emits a new list on every table change
- Use `suspend` for write operations (insert, update, delete)
- Repository wraps the DAO and is the ViewModel's single point of contact for data
- `.stateIn(viewModelScope, ...)` converts a `Flow` to a `StateFlow` for `collectAsState()`
- Always use `context.applicationContext` when building the Room database

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `Cannot access database on the main thread` | Calling a DAO suspend function outside a coroutine, or using a blocking call | Wrap in `viewModelScope.launch { }` or add `.allowMainThreadQueries()` (dev only) |
| `Room schema changed but no migration provided` | Incremented `version` without a `Migration` | Add a `Migration` or use `fallbackToDestructiveMigration()` during dev |
| `error: Cannot find implementation for ... Database` | KSP not set up, or build cache stale | Add KSP plugin and `ksp(room-compiler)` dependency; do a clean build |
| `Flow` not emitting after insert | DAO function returns `Flow` but caller uses `.first()` only once | Use `collect { }` or `collectAsState()` for a live subscription |

---

## Further Reading

- developer.android.com/training/data-storage/room — official Room guide
- developer.android.com/reference/kotlin/androidx/room — Room API reference
- developer.android.com/topic/architecture/data-layer — data layer architecture
- Episode 7: Networking with Retrofit — fetch data from the internet
