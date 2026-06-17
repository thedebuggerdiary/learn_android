# Episode 8 — Building a Complete App

**Series:** Android Development from Scratch
**Channel:** The Debugger Diary
**Prerequisites:** Episodes 1–7 (all previous episodes)

---

## Episode Overview

This is the capstone episode. You will wire together every concept from the series — Jetpack Compose, Navigation, ViewModel, Room, and Retrofit — into a single working news reader app. You will also learn Hilt dependency injection, which replaces all the manual factory and singleton wiring from previous episodes. The result is a clean, offline-first app that fetches articles from an API, caches them locally, and displays them in a scrollable list with pull-to-refresh.

---

## Section 1: Architecture Overview

The app follows the recommended Android architecture:

```
┌──────────────────────────────────────────┐
│  UI Layer                                │
│  NewsScreen (Compose) + ArticleViewModel │
└────────────────┬─────────────▲───────────┘
                 │ events      │ UiState / Flow
┌────────────────▼─────────────┴───────────┐
│  Data Layer — ArticleRepository          │
│  Single source of truth                  │
│  Network → Room → Flow<List<Article>>    │
└─────────────┬─────────────┬──────────────┘
              │             │
   ┌──────────▼──┐  ┌───────▼────────┐
   │  NewsApi    │  │  ArticleDao    │
   │  (Retrofit) │  │  (Room)        │
   └─────────────┘  └────────────────┘
```

**Key principle — offline first:**
- The UI always observes Room via a `Flow`
- On launch and pull-to-refresh, the repository fetches from the network and writes to Room
- Room emits the new list to the UI automatically
- If the network is unavailable, the cached data is shown without any error state disrupting the user

---

## Section 2: Hilt — Dependency Injection

Every ViewModel in the previous episodes either had no dependencies or required a manual Factory. Hilt automates this. It:
1. Reads your `@Inject` annotations at compile time
2. Generates the code to construct and inject dependencies
3. Validates the entire dependency graph — a missing `@Provides` is a build error, not a runtime crash

### Setup

```kotlin
// build.gradle.kts (project-level)
plugins {
    id("com.google.dagger.hilt.android") version "2.51.x" apply false
}

// build.gradle.kts (app)
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.51.x")
    ksp("com.google.dagger:hilt-android-compiler:2.51.x")
    // Hilt + Compose + Navigation integration
    implementation("androidx.hilt:hilt-navigation-compose:1.2.x")
}
```

Annotate your `Application` class:

```kotlin
@HiltAndroidApp
class NewsApp : Application()
```

Add to `AndroidManifest.xml`:
```xml
<application android:name=".NewsApp" ...>
```

Annotate `MainActivity`:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

---

## Section 3: Article — The Data Model

The article has two representations: a DTO (Data Transfer Object) for the network and an Entity for the database. Keep them separate and map between them in the repository.

```kotlin
// Database entity
@Entity(tableName = "articles")
data class Article(
    @PrimaryKey
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val source: String,
    val publishedAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)

// Network DTO — mirrors the NewsAPI JSON structure
@Serializable
data class ArticleDto(
    val title: String,
    val description: String?,
    @SerialName("urlToImage") val imageUrl: String?,
    val url: String,
    val source: SourceDto,
    val publishedAt: String
)

@Serializable
data class SourceDto(val name: String)

@Serializable
data class HeadlinesResponse(
    val articles: List<ArticleDto>
)

// Mapping function — keep in the same file or a separate mapper file
fun ArticleDto.toEntity() = Article(
    url = url,
    title = title,
    description = description,
    imageUrl = imageUrl,
    source = source.name,
    publishedAt = publishedAt
)
```

**Why separate models?** The network model may change (the API adds fields, renames keys). The database model should be stable — you control its schema. A mapping function at the boundary means a network change only requires updating the DTO and the mapping, not the Room schema.

---

## Section 4: ArticleDao and ArticleDatabase

```kotlin
@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY published_at DESC")
    fun getAllArticles(): Flow<List<Article>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<Article>)

    @Query("DELETE FROM articles")
    suspend fun clearAll()

    @Query("SELECT * FROM articles WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): Article?
}

@Database(entities = [Article::class], version = 1, exportSchema = false)
abstract class ArticleDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}
```

---

## Section 5: NewsApi with Retrofit

```kotlin
interface NewsApi {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "us",
        @Query("apiKey") apiKey: String = BuildConfig.NEWS_API_KEY
    ): HeadlinesResponse
}

object RetrofitInstance {
    private val json = Json { ignoreUnknownKeys = true }

    val api: NewsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://newsapi.org/v2/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NewsApi::class.java)
    }
}
```

Store the API key in `local.properties` and expose it via `BuildConfig`:
```
// local.properties (never commit this file)
NEWS_API_KEY=your_key_here

// build.gradle.kts
android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "NEWS_API_KEY", "\"${properties["NEWS_API_KEY"]}\"")
    }
}
```

---

## Section 6: ArticleRepository — Offline First

```kotlin
class ArticleRepository @Inject constructor(
    private val api: NewsApi,
    private val dao: ArticleDao
) {
    // Always emit from Room — UI observes this
    val articles: Flow<List<Article>> = dao.getAllArticles()

    suspend fun refresh() {
        try {
            val response = api.getTopHeadlines()
            val entities = response.articles
                .filter { it.url.isNotBlank() && it.title != "[Removed]" }
                .map { it.toEntity() }
            dao.clearAll()
            dao.insertAll(entities)
            // Room emits the new list automatically — no manual notification needed
        } catch (e: IOException) {
            // Network unavailable — cached data stays visible, no error thrown
        } catch (e: retrofit2.HttpException) {
            // API error — log it, but don't crash the UI
        }
    }
}
```

The offline-first contract:
1. `articles` is always a live Room query
2. `refresh()` fetches, writes to Room, and silently handles failures
3. The UI never calls the API directly — only the repository does

---

## Section 7: Hilt Module — Providing Dependencies

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideArticleDatabase(@ApplicationContext context: Context): ArticleDatabase =
        Room.databaseBuilder(
            context,
            ArticleDatabase::class.java,
            "articles_db"
        ).build()

    @Provides
    fun provideArticleDao(db: ArticleDatabase): ArticleDao = db.articleDao()

    @Provides
    @Singleton
    fun provideNewsApi(): NewsApi = RetrofitInstance.api

    @Provides
    @Singleton
    fun provideArticleRepository(api: NewsApi, dao: ArticleDao): ArticleRepository =
        ArticleRepository(api, dao)
}
```

**`@Singleton`** — Hilt creates one instance for the entire app lifetime. Use it for the database, API, and repository. Do NOT use it for ViewModels (they have their own scope).

**`@InstallIn(SingletonComponent::class)`** — binds the module to the application scope. Alternatives: `ActivityComponent`, `ViewModelComponent`, `FragmentComponent`.

---

## Section 8: ArticleViewModel with Hilt

```kotlin
@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    val articles: StateFlow<List<Article>> = repository.articles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()  // auto-load on first open
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            repository.refresh()
            _isRefreshing.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
```

`@HiltViewModel` + `@Inject constructor` is all Hilt needs. In the composable, replace `viewModel()` with `hiltViewModel()` from `androidx.hilt:hilt-navigation-compose`.

---

## Section 9: NewsScreen — The Complete UI

```kotlin
@Composable
fun NewsScreen(
    viewModel: ArticleViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val articles by viewModel.articles.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Top Headlines") })
        },
        modifier = modifier
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(innerPadding)
        ) {
            if (articles.isEmpty() && !isRefreshing) {
                EmptyState(onRetry = viewModel::refresh)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(articles, key = { it.url }) { article ->
                        ArticleItem(article = article)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleItem(article: Article, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        article.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            article.description?.let { desc ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${article.source} • ${formatRelativeTime(article.publishedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun EmptyState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No articles loaded", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
```

---

## Section 10: Series Recap

You now have all the tools to build production Android apps:

| Episode | Concept | What You Built |
|---------|---------|---------------|
| 1 | Android Studio + Compose basics | Hello World app |
| 2 | Compose UI — layouts and modifiers | Profile card |
| 3 | State and lists | Interactive to-do list |
| 4 | Navigation | Multi-screen task app |
| 5 | ViewModel + MVVM | Rotation-safe notes app |
| 6 | Room database | Persistent notes |
| 7 | Retrofit networking | GitHub repo browser |
| 8 | Full app + Hilt | News reader — offline first |

**What to explore next:**
- WorkManager — background sync and scheduled tasks
- DataStore — key-value preferences (replaces SharedPreferences)
- Paging 3 — paginated lists from network + database
- Compose animations — `AnimatedVisibility`, `animateContentSize`, shared element transitions
- Testing — JUnit for ViewModels, Compose `ComposeTestRule` for UI tests

---

## Key Takeaways

- Hilt removes all manual dependency wiring — `@HiltViewModel` + `hiltViewModel()` replaces every Factory class
- Separate DTO (network) from Entity (database) — map between them in the repository
- Offline-first: UI observes Room, network updates Room, failures are silent
- `@Module` with `@Provides` teaches Hilt how to construct things it can't construct itself
- `@Singleton` on `@Provides` ensures one instance per app — use it for db, api, and repository
- `init { refresh() }` auto-loads data without needing the UI to call it

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `Hilt component ... is not set up` | Missing `@HiltAndroidApp` on Application or `@AndroidEntryPoint` on Activity | Add the annotations and clean-rebuild |
| `[Dagger/MissingBinding]` at build time | A dependency has no `@Provides` or `@Inject` in its constructor | Add a `@Provides` function in a `@Module` |
| `hiltViewModel()` not found | Missing `hilt-navigation-compose` dependency | Add `androidx.hilt:hilt-navigation-compose` |
| Pull-to-refresh not available | Using an older Compose BOM without `PullToRefreshBox` | Update Compose BOM to 2024.x or use the older `SwipeRefresh` from Accompanist |
| API key exposed in version control | `local.properties` committed | Add `local.properties` to `.gitignore` immediately |

---

## Further Reading

- developer.android.com/training/dependency-injection/hilt-android — Hilt guide
- developer.android.com/topic/architecture — official architecture guide
- developer.android.com/topic/architecture/data-layer/offline-first — offline-first patterns
- developer.android.com/jetpack/compose/testing — Compose testing guide
