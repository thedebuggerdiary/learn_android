# Episode 7 — Networking with Retrofit

**Series:** Android Development from Scratch
**Channel:** The Debugger Diary
**Prerequisites:** Episodes 1–6 (setup, Compose UI, state, navigation, ViewModel, Room)

---

## Episode Overview

This episode adds networking to your app. You will learn how to use Retrofit to make HTTP requests to a REST API, how to model JSON responses with `@Serializable` data classes, how to structure loading/error/success states in the ViewModel, and how to display remote images with Coil. The demo calls the real GitHub public API to fetch a user's repositories.

---

## Section 1: Android Manifest — Internet Permission

Before any network call, declare the internet permission in `AndroidManifest.xml`:

```xml
<manifest>
    <uses-permission android:name="android.permission.INTERNET" />
    ...
</manifest>
```

This is a normal permission (not a dangerous permission) — the user isn't prompted. But the app will silently fail all network calls without it.

---

## Section 2: Add Dependencies

```kotlin
// build.gradle.kts (app)
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.x"
}

dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.x")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.6.0")
}
```

---

## Section 3: Modeling the API Response

Kotlinx Serialization maps JSON keys to Kotlin properties. When JSON keys use `snake_case` but your Kotlin uses `camelCase`, use `@SerialName`:

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRepo(
    val id: Long,
    val name: String,
    val description: String?,       // nullable — GitHub returns null if no description

    @SerialName("stargazers_count")
    val stars: Int,

    @SerialName("html_url")
    val htmlUrl: String,

    @SerialName("full_name")
    val fullName: String,

    val owner: Owner,

    @SerialName("private")
    val isPrivate: Boolean = false
)

@Serializable
data class Owner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String
)
```

**`description: String?`** — the GitHub API omits or nulls this field for repos with no description. Using a nullable type prevents `NullPointerException` on deserialization.

---

## Section 4: Retrofit Interface

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

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubRepo
}
```

**`@Path`** — replaces `{username}` in the URL with the parameter value.
**`@Query`** — appends `?sort=updated&per_page=30` to the URL.
**`suspend`** — Retrofit with OkHttp supports coroutines natively. The call runs on the IO dispatcher and resumes the coroutine when complete.

---

## Section 5: Building the Retrofit Instance

```kotlin
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object RetrofitInstance {

    private val json = Json {
        ignoreUnknownKeys = true    // don't crash if API adds new fields
        coerceInputValues = true    // use default values for missing/null keys
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // log full request/response in debug
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val api: GithubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(client)
            .build()
            .create(GithubApi::class.java)
    }
}
```

**`ignoreUnknownKeys = true`** — the GitHub API returns 30+ fields per repo. Without this, adding a single unexpected key to the response would crash your app. Always set this to `true` for real APIs.

**`HttpLoggingInterceptor`** — logs all HTTP traffic to Logcat. Remove or disable this in release builds — it prints sensitive headers and tokens.

---

## Section 6: UiState with Sealed Interface

```kotlin
sealed interface GithubUiState {
    data object Idle : GithubUiState
    data object Loading : GithubUiState
    data class Success(val repos: List<GithubRepo>) : GithubUiState
    data class Error(val message: String) : GithubUiState
}
```

Using a `sealed interface` instead of a `data class` with nullable fields has a key benefit: the compiler enforces exhaustive `when` expressions. If you add a new state (e.g., `Empty`) and forget to handle it in the UI, the build fails — not a runtime crash.

```kotlin
// Compiler error if any case is missing
when (val state = uiState) {
    is GithubUiState.Idle    -> { /* show hint */ }
    is GithubUiState.Loading -> { /* show spinner */ }
    is GithubUiState.Success -> { /* show list */ }
    is GithubUiState.Error   -> { /* show error */ }
}
```

---

## Section 7: ViewModel

```kotlin
class GithubViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GithubUiState>(GithubUiState.Idle)
    val uiState: StateFlow<GithubUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun searchRepos() {
        val username = _searchQuery.value.trim()
        if (username.isBlank()) return

        viewModelScope.launch {
            _uiState.value = GithubUiState.Loading
            try {
                val repos = RetrofitInstance.api.getRepos(username)
                _uiState.value = if (repos.isEmpty()) {
                    GithubUiState.Error("No repositories found for '$username'")
                } else {
                    GithubUiState.Success(repos)
                }
            } catch (e: retrofit2.HttpException) {
                val errorMessage = when (e.code()) {
                    404  -> "User '$username' not found"
                    403  -> "API rate limit exceeded. Try again later."
                    else -> "Server error: ${e.code()}"
                }
                _uiState.value = GithubUiState.Error(errorMessage)
            } catch (e: java.io.IOException) {
                _uiState.value = GithubUiState.Error("Network error. Check your connection.")
            }
        }
    }
}
```

**Distinguishing error types:**
- `HttpException` — server responded with a non-2xx status code (404, 429, 500, etc.)
- `IOException` — network failure (no internet, timeout, DNS error)

---

## Section 8: Compose UI

```kotlin
@Composable
fun GithubScreen(
    viewModel: GithubViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onQueryChange,
                label = { Text("GitHub username") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchRepos() })
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = viewModel::searchRepos) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }

        // Content area
        when (val state = uiState) {
            is GithubUiState.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search for a GitHub user", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is GithubUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GithubUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GithubUiState.Success -> {
                RepoList(repos = state.repos)
            }
        }
    }
}

@Composable
fun RepoList(repos: List<GithubRepo>) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(repos, key = { it.id }) { repo ->
            RepoItem(repo = repo)
            HorizontalDivider()
        }
    }
}

@Composable
fun RepoItem(repo: GithubRepo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = repo.owner.avatarUrl,
            contentDescription = "${repo.owner.login}'s avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(repo.name, style = MaterialTheme.typography.titleSmall)
            repo.description?.let { desc ->
                Text(desc, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = "Stars",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(4.dp))
            Text(formatStars(repo.stars), style = MaterialTheme.typography.labelMedium)
        }
    }
}

fun formatStars(count: Int): String = when {
    count >= 1_000 -> "${count / 1_000}k"
    else -> count.toString()
}
```

---

## Key Takeaways

- Add `INTERNET` permission to `AndroidManifest.xml` — without it all calls silently fail
- Retrofit turns an annotated Kotlin interface into a complete HTTP client
- `@Serializable` with `ignoreUnknownKeys = true` — safe JSON mapping that won't crash on API changes
- Use `sealed interface` for `UiState` — compiler enforces all states are handled
- Catch `HttpException` for server errors (status codes) and `IOException` for network failures
- `AsyncImage` from Coil — async image loading in one line, caching and transitions included
- Keep `HttpLoggingInterceptor` at `BODY` level only in debug builds

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `NetworkOnMainThreadException` | Calling Retrofit from the main thread without `suspend` | Add `suspend` to the interface function; call from `viewModelScope.launch` |
| `cleartext traffic not permitted` | HTTP (not HTTPS) URL blocked by Android 9+ | Use HTTPS, or add `android:usesCleartextTraffic="true"` to manifest for dev |
| `SerializationException: Unknown field` | API returned a field your data class doesn't have | Add `ignoreUnknownKeys = true` to the `Json { }` builder |
| 403 / rate limit on GitHub API | Too many unauthenticated requests | Add a GitHub personal access token as a Bearer header in OkHttp interceptor |

---

## Further Reading

- square.github.io/retrofit — Retrofit documentation
- github.com/Kotlin/kotlinx.serialization — Kotlinx Serialization guide
- coil-kt.github.io/coil — Coil documentation
- Episode 8: Building a Complete App — wiring MVVM + Room + Retrofit + Hilt together
