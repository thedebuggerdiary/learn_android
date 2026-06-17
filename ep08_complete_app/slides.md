---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
---

# Building a Complete App
### Episode 8 — Android Development from Scratch
**The Debugger Diary**

---

# What You'll Build

A news reader app — everything from this series in one app

```
┌────────────────────────────┐
│ Top Headlines              │
├────────────────────────────┤
│ [img] Article title     ↗  │
│       Short description    │
│       CNN • 2h ago         │
├────────────────────────────┤
│ [img] Another headline  ↗  │
│       Description here     │
│       BBC • 4h ago         │
└────────────────────────────┘
```

Fetches from API → caches to Room → displays in Compose

---

# The Architecture

```
┌────────────────────────────────────────┐
│   UI Layer                             │
│   NewsScreen + ArticleViewModel        │
└──────────────┬────────────▲────────────┘
               │ events     │ UiState
┌──────────────▼────────────┴────────────┐
│   Domain/ViewModel Layer               │
│   ArticleViewModel (Hilt injected)     │
└──────────────┬─────────────────────────┘
               │
┌──────────────▼─────────────────────────┐
│   Data Layer — ArticleRepository       │
│   Single source of truth               │
│   API fetch → save to Room → emit Flow │
└───────────┬────────────┬───────────────┘
            │            │
   ┌────────▼───┐  ┌─────▼──────┐
   │ NewsApi    │  │ ArticleDao │
   │ (Retrofit) │  │ (Room)     │
   └────────────┘  └────────────┘
```

---

# Hilt — Dependency Injection

Without Hilt, every ViewModel needs a Factory and every screen needs to manually construct the dependency chain.

With Hilt:

```kotlin
@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() { ... }

// In the screen — Hilt provides the ViewModel automatically
@Composable
fun NewsScreen(viewModel: ArticleViewModel = hiltViewModel()) { ... }
```

No Factory classes. No manual wiring. Hilt validates the dependency graph at compile time.

---

# Setting Up Hilt

```kotlin
// build.gradle.kts (project)
plugins { id("com.google.dagger.hilt.android") version "2.x.x" apply false }

// build.gradle.kts (app)
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.x.x")
    ksp("com.google.dagger:hilt-android-compiler:2.x.x")
    implementation("androidx.hilt:hilt-navigation-compose:1.x.x")
}
```

```kotlin
@HiltAndroidApp
class NewsApp : Application()
// add android:name=".NewsApp" to <application> in AndroidManifest
```

---

# Providing Dependencies — @Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArticleDatabase =
        Room.databaseBuilder(context, ArticleDatabase::class.java, "articles_db").build()

    @Provides
    fun provideArticleDao(db: ArticleDatabase): ArticleDao = db.articleDao()

    @Provides @Singleton
    fun provideNewsApi(): NewsApi = RetrofitInstance.api

    @Provides @Singleton
    fun provideRepository(api: NewsApi, dao: ArticleDao): ArticleRepository =
        ArticleRepository(api, dao)
}
```

---

# The Repository — Offline First

```kotlin
class ArticleRepository @Inject constructor(
    private val api: NewsApi,
    private val dao: ArticleDao
) {
    val articles: Flow<List<Article>> = dao.getAllArticles()

    suspend fun refresh() {
        try {
            val remote = api.getTopHeadlines()
            dao.clearAll()
            dao.insertAll(remote.map { it.toEntity() })
        } catch (e: IOException) {
            // network unavailable — cached data stays visible
        }
    }
}
```

**Offline-first pattern:**
1. UI always observes Room (`Flow<List<Article>>`)
2. On load/pull-to-refresh, fetch from network and update Room
3. Room emits the new list — UI updates automatically
4. If network fails, cached articles remain visible

---

# Article — Dual Role (@Entity + @Serializable)

```kotlin
@Entity(tableName = "articles")
data class Article(
    @PrimaryKey val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val source: String,
    val publishedAt: String
)

@Serializable
data class ArticleDto(
    val title: String,
    val description: String?,
    @SerialName("urlToImage") val imageUrl: String?,
    val url: String,
    val source: SourceDto,
    val publishedAt: String
)

fun ArticleDto.toEntity() = Article(
    url = url, title = title, description = description,
    imageUrl = imageUrl, source = source.name, publishedAt = publishedAt
)
```

---

# ArticleViewModel

```kotlin
@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    val articles: StateFlow<List<Article>> = repository.articles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refresh()
            _isRefreshing.value = false
        }
    }
}
```

---

# NewsScreen

```kotlin
@Composable
fun NewsScreen(viewModel: ArticleViewModel = hiltViewModel()) {
    val articles by viewModel.articles.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh
    ) {
        if (articles.isEmpty() && !isRefreshing) {
            EmptyState()
        } else {
            LazyColumn {
                items(articles, key = { it.url }) { article ->
                    ArticleItem(article = article)
                    HorizontalDivider()
                }
            }
        }
    }
}
```

---

# Key Takeaways

- Repository is the single source of truth — UI observes Room, network updates Room
- Offline-first: cache saves, network refreshes, failure is silent
- Hilt removes all manual wiring — `@HiltViewModel` + `hiltViewModel()` is all you need in Compose
- `@Module` + `@Provides` teaches Hilt how to build dependencies it can't construct itself
- Separate DTO (network model) from Entity (database model) — map between them in the repository
- `init { refresh() }` auto-loads data when the ViewModel is created

---

# You Made It — Series Complete!

You now know how to build real Android apps with:
- **Jetpack Compose** for declarative UI
- **Navigation** for multi-screen apps
- **ViewModel + StateFlow** for state that survives rotation
- **Room** for local persistence
- **Retrofit** for networking
- **Hilt** for dependency injection
- **Repository pattern** for clean data flow

**The Debugger Diary** — Android Development from Scratch
_"Understand the tools, not just the syntax."_
