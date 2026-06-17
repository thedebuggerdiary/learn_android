---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
---

# Local Data with Room
### Episode 6 — Android Development from Scratch
**The Debugger Diary**

---

# What is Room?

Room is an **ORM (Object-Relational Mapping)** library built on top of SQLite.

Instead of writing raw SQL and manually mapping cursor rows to Kotlin objects, Room lets you:
- Define tables as `@Entity` data classes
- Write queries as `@Dao` interface functions
- Get `Flow<List<T>>` back — the UI updates automatically when data changes

---

# Add Dependencies

```kotlin
// build.gradle.kts (app)
plugins {
    id("com.google.devtools.ksp")  // needed for Room's code generation
}

dependencies {
    val roomVersion = "2.6.x"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")  // Flow + coroutine support
    ksp("androidx.room:room-compiler:$roomVersion")
}
```

KSP (Kotlin Symbol Processing) replaces the older `kapt` annotation processor for Room. It's faster.

---

# @Entity — Define a Table

```kotlin
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

Each `@Entity` becomes a table. Each property becomes a column. `@PrimaryKey(autoGenerate = true)` — Room assigns a unique ID on insert.

---

# @Dao — Define Queries

```kotlin
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

---

# Flow from Room

```kotlin
@Query("SELECT * FROM notes ORDER BY created_at DESC")
fun getAllNotes(): Flow<List<Note>>
```

This returns a **live query** — every time any row in the `notes` table changes, Room emits a new list. Connect it to `collectAsState()` in Compose and the UI updates automatically on every insert, update, or delete. No polling needed.

---

# @Database — Wire It Together

```kotlin
@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

---

# Repository Pattern

```kotlin
class NoteRepository(private val dao: NoteDao) {

    val allNotes: Flow<List<Note>> = dao.getAllNotes()

    suspend fun addNote(title: String) {
        dao.insert(Note(title = title))
    }

    suspend fun deleteNote(note: Note) {
        dao.delete(note)
    }
}
```

The repository is the single source of truth. The ViewModel talks to the repository — not the DAO directly. This makes swapping Room for a network source easy in Episode 8.

---

# ViewModel + Room

```kotlin
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addNote(title: String) {
        viewModelScope.launch { repository.addNote(title) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }
}
```

`.stateIn()` converts a `Flow` into a `StateFlow` — ready for `collectAsState()`.

---

# Database Migrations

When you add a column in a future version, bump the `version` and provide a migration:

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

Never use `fallbackToDestructiveMigration()` in production — it deletes all user data.

---

# Key Takeaways

- `@Entity` = table, `@Dao` = queries, `@Database` = the database
- Room generates all SQL boilerplate at compile time — typos are caught at build, not runtime
- `Flow<List<T>>` from a `@Query` is a live query — emits on every table change
- Use `suspend` functions for writes (insert, update, delete)
- Repository wraps the DAO and is the ViewModel's single source of truth
- `.stateIn(viewModelScope, ...)` converts `Flow` to `StateFlow` for Compose

---

# What's Next — Episode 7

**Networking with Retrofit**

- Retrofit + Kotlinx Serialization setup
- Defining API interfaces with `@GET`, `@POST`
- `suspend` functions for network calls
- Loading / error / success UI states
- Coil for async image loading

**The Debugger Diary** — Android Development from Scratch
_"Understand the tools, not just the syntax."_
