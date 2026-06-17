---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
---

# Networking with Retrofit
### Episode 7 — Android Development from Scratch
**The Debugger Diary**

---

# What You'll Build

A GitHub repository browser — type a username, fetch their public repos from the GitHub API

```
┌────────────────────────────┐
│  GitHub: [torvalds    ] 🔍 │
├────────────────────────────┤
│  linux          ★ 180k     │
│  The Linux kernel          │
├────────────────────────────┤
│  subsurface      ★ 2k      │
│  Dive log program          │
└────────────────────────────┘
```

---

# Add Dependencies

```kotlin
// build.gradle.kts (app)
dependencies {
    // Retrofit + Kotlinx Serialization converter
    implementation("com.squareup.retrofit2:retrofit:2.x.x")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.x.x")

    // Kotlinx Serialization JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.x.x")

    // OkHttp for HTTP client (Retrofit uses it underneath)
    implementation("com.squareup.okhttp3:okhttp:4.x.x")
    implementation("com.squareup.okhttp3:logging-interceptor:4.x.x")

    // Coil for async image loading in Compose
    implementation("io.coil-kt:coil-compose:2.x.x")
}
```

Also add `plugins { id("org.jetbrains.kotlin.plugin.serialization") }`.

---

# @Serializable — Model the Response

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRepo(
    val id: Long,
    val name: String,
    val description: String?,

    @SerialName("stargazers_count")
    val stars: Int,

    @SerialName("html_url")
    val htmlUrl: String,

    val owner: Owner
)

@Serializable
data class Owner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String
)
```

---

# Retrofit Interface

```kotlin
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubApi {

    @GET("users/{username}/repos")
    suspend fun getRepos(
        @Path("username") username: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30
    ): List<GithubRepo>
}
```

Retrofit generates the HTTP client from this interface at runtime. `suspend` makes the call coroutine-friendly — Retrofit handles threading automatically.

---

# Build the Retrofit Instance

```kotlin
object RetrofitInstance {
    private val json = Json { ignoreUnknownKeys = true }

    val api: GithubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build()
            )
            .build()
            .create(GithubApi::class.java)
    }
}
```

`ignoreUnknownKeys = true` — the GitHub API returns many fields. This prevents crashes when the API adds new fields your model doesn't have.

---

# UiState for Network Requests

```kotlin
sealed interface GithubUiState {
    data object Idle : GithubUiState
    data object Loading : GithubUiState
    data class Success(val repos: List<GithubRepo>) : GithubUiState
    data class Error(val message: String) : GithubUiState
}
```

A sealed interface forces you to handle every possible state in the UI — the compiler will warn you if you miss a case in a `when` expression.

---

# ViewModel

```kotlin
class GithubViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<GithubUiState>(GithubUiState.Idle)
    val uiState: StateFlow<GithubUiState> = _uiState.asStateFlow()

    fun searchRepos(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            _uiState.value = GithubUiState.Loading
            try {
                val repos = RetrofitInstance.api.getRepos(username)
                _uiState.value = GithubUiState.Success(repos)
            } catch (e: Exception) {
                _uiState.value = GithubUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

---

# Handling States in the UI

```kotlin
@Composable
fun GithubScreen(viewModel: GithubViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        SearchBar(onSearch = { viewModel.searchRepos(it) })

        when (val state = uiState) {
            is GithubUiState.Idle    -> IdleHint()
            is GithubUiState.Loading -> LoadingSpinner()
            is GithubUiState.Error   -> ErrorMessage(state.message)
            is GithubUiState.Success -> RepoList(repos = state.repos)
        }
    }
}
```

---

# Coil — Async Image Loading

```kotlin
import coil.compose.AsyncImage

AsyncImage(
    model = repo.owner.avatarUrl,
    contentDescription = "${repo.owner.login}'s avatar",
    modifier = Modifier
        .size(48.dp)
        .clip(CircleShape),
    contentScale = ContentScale.Crop
)
```

`AsyncImage` handles loading, caching, and error states automatically. No callbacks needed — it's fully declarative.

---

# Key Takeaways

- Retrofit turns an annotated interface into a working HTTP client — no manual HTTP code
- `suspend` in the interface means Retrofit calls are coroutine-friendly; call from `viewModelScope`
- `@Serializable` + `ignoreUnknownKeys = true` — safe deserialization even when APIs add fields
- Use a sealed `UiState` interface — the compiler enforces exhaustive handling of all states
- Wrap network calls in `try/catch` inside `viewModelScope.launch` — update state on error
- `AsyncImage` from Coil — async image loading in one composable, caching included

---

# What's Next — Episode 8

**Building a Complete App**

- MVVM + Room + Retrofit wired together
- Repository as single source of truth (offline-first)
- Hilt dependency injection — clean, no manual factories
- End-to-end architecture review

**The Debugger Diary** — Android Development from Scratch
_"Understand the tools, not just the syntax."_
