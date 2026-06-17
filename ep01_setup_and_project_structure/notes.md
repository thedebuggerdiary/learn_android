# Episode 1 — Setup & Project Structure

**Series:** Android Development from Scratch
**Channel:** The Debugger Diary
**Prerequisites:** Kotlin basics (variables, functions, classes) — complete the *Kotlin from Scratch* series first

---

## Episode Overview

This episode gets you from zero to a running Android app. You will install Android Studio, understand how an Android project is laid out, learn what `MainActivity` and `setContent` do, write your first Composable function, and run the app on an emulator and a physical device. By the end you will have a foundation to build on in every episode that follows.

---

## Section 1: Why Jetpack Compose?

Before 2021, Android UIs were built with XML layout files. You described what a screen looked like in XML, then wrote Kotlin/Java to find views by ID and mutate them imperatively. This split brain — XML for structure, Kotlin for logic — caused fragile code and difficult state management.

**Jetpack Compose** replaces this with a declarative model: you write Kotlin functions that describe what the UI should look like given the current state. When state changes, Compose re-runs the relevant functions and updates only what changed. This is the same model React and SwiftUI use.

Benefits for you as a Kotlin developer:
- UI code is just Kotlin — no XML, no `findViewById`, no view binding
- State management is explicit and composable
- Hot reload via the Preview panel in Android Studio
- First-class coroutine integration

---

## Section 2: Install Android Studio

Download **Android Studio** (latest stable) from `developer.android.com/studio`.

The installer bundles:
- **Android SDK** — the libraries and tools for building Android apps
- **Kotlin plugin** — first-class Kotlin editing, refactoring, and navigation
- **Gradle** — the build system used for all Android projects
- **AVD Manager** — creates and manages emulated Android devices
- **Layout and Compose preview** — renders your UI without running the app

After installation, open Android Studio and let it download any missing SDK components it prompts for. Accept the default SDK location.

### First-run SDK setup

On first launch, the Setup Wizard installs the default Android SDK and a system image for an emulator. Let this complete before creating a project — it downloads several gigabytes.

---

## Section 3: Create the Project

1. Open Android Studio → **New Project**
2. Select **Empty Activity** (this gives you a Compose-ready scaffold)
3. Fill in the form:
   - **Name:** `HelloAndroid`
   - **Package name:** `com.debuggerdiary.ep01`
   - **Save location:** anywhere convenient
   - **Language:** Kotlin
   - **Minimum SDK:** API 26 (Android 8.0 Oreo) — this covers roughly 95% of active devices
   - **Build configuration language:** Kotlin DSL
4. Click **Finish**

Android Studio creates the project and runs an initial Gradle sync. This downloads dependencies and can take a minute on first run.

---

## Section 4: Project Structure

Understanding where things live prevents confusion when the project grows.

```
HelloAndroid/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/
│   │       │   └── com/debuggerdiary/ep01/
│   │       │       ├── MainActivity.kt
│   │       │       └── ui/theme/
│   │       │           ├── Color.kt
│   │       │           ├── Theme.kt
│   │       │           └── Type.kt
│   │       └── res/
│   │           ├── values/
│   │           │   ├── strings.xml
│   │           │   └── themes.xml
│   │           └── drawable/
│   ├── build.gradle.kts        ← app-level: dependencies, compileSdk, minSdk
│   └── proguard-rules.pro
├── build.gradle.kts            ← project-level: plugin versions
├── settings.gradle.kts         ← module list, repository sources
└── gradle/
    └── libs.versions.toml      ← version catalog (centralized dependency versions)
```

### `AndroidManifest.xml`

Every Android app has exactly one manifest. It declares:
- The package name (unique app identifier on Google Play)
- All `Activity`, `Service`, `BroadcastReceiver`, and `ContentProvider` components
- Permissions the app requests (camera, internet, etc.)
- The launcher activity — the screen that opens when the user taps the app icon

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.HelloAndroid">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

The `intent-filter` with `MAIN` and `LAUNCHER` marks `MainActivity` as the app's entry point. Only one activity should have this filter.

### `res/` — Resources

The `res/` directory holds non-code assets:
- `res/values/strings.xml` — string constants used in the app (supports localization)
- `res/drawable/` — images and vector icons
- `res/mipmap/` — launcher icons at different densities

With Compose you will mostly work in Kotlin, but `strings.xml` is still the right place for user-visible text to support multiple languages.

### `build.gradle.kts` (app level)

This file declares the app's dependencies. After you create the project it already includes the Compose BOM (Bill of Materials) which pins all Compose library versions together:

```kotlin
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.x.x")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.x")
}
```

The BOM means you never have to specify individual Compose library versions — they are all kept in sync automatically.

---

## Section 5: MainActivity

Open `MainActivity.kt`. The generated code looks like this:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelloAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}
```

**`ComponentActivity`** — the base class for all modern Android activities. An Activity is the Android framework's concept of a screen with a lifecycle (created, resumed, paused, destroyed). You override `onCreate` to set up the screen.

**`setContent { }`** — this is the bridge between the Android framework and Jetpack Compose. Everything inside this block is rendered by the Compose runtime. It replaces the old `setContentView(R.layout.activity_main)` approach.

**`HelloAndroidTheme { }`** — generated by Android Studio; it applies your app's Material 3 color scheme, typography, and shapes to all composables inside it.

**`Surface`** — a Material 3 composable that draws a background color. `Modifier.fillMaxSize()` makes it take up the entire screen.

---

## Section 6: Your First Composable

```kotlin
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello, $name!",
        modifier = modifier.padding(16.dp)
    )
}
```

**`@Composable`** — this annotation marks a function as a composable. A composable function:
- Can call other composables
- Cannot be called from regular (non-composable) Kotlin code outside `setContent`
- Does not return a value — it emits UI as a side effect
- Can be called many times (recomposition) when state changes

**`Text`** — the basic composable for displaying text. Maps roughly to a `TextView` in the old View system.

**`Modifier`** — an ordered, immutable list of decorations applied to a composable. `Modifier.padding(16.dp)` adds 16 density-independent pixels of space around the text. You chain modifiers: `Modifier.padding(16.dp).fillMaxWidth().background(Color.Red)`.

**`dp`** — density-independent pixels. Use `dp` for sizes and spacing so your UI scales correctly on high and low-density screens. Use `sp` for font sizes.

---

## Section 7: Previewing in Android Studio

```kotlin
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HelloAndroidTheme {
        Greeting("Debugger Diary")
    }
}
```

The `@Preview` annotation tells Android Studio to render this composable in the preview panel — no emulator, no build, instant visual feedback. Click **Split** (top-right of the editor) to see the preview alongside the code.

Multiple previews are common — use them to check light/dark mode, different text sizes, or different screen sizes without running the app each time:

```kotlin
@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun GreetingPreview() {
    HelloAndroidTheme {
        Greeting("Debugger Diary")
    }
}
```

---

## Section 8: Run on an Emulator

1. Open **Device Manager** (toolbar icon or Tools → Device Manager)
2. Click **Create Device**
3. Select **Pixel 6** (or any Phone hardware profile)
4. Select a system image — **API 34 (Android 14)** is a good default; download it if not already present
5. Click **Finish**
6. With the emulator selected in the device dropdown, press **Shift+F10** (Run)

First cold boot takes 1–2 minutes. After that, use **hot reload** (`Ctrl+S` while the app is running) for instant updates to Compose code without a full rebuild.

---

## Section 9: Run on a Physical Device

Physical devices are faster than emulators and better represent real-world performance.

1. On your phone: **Settings → About Phone → tap Build Number 7 times** — this enables Developer Options
2. **Settings → Developer Options → enable USB Debugging**
3. Connect the phone to your computer via USB
4. Accept the "Allow USB Debugging?" prompt on the phone
5. Your device appears in Android Studio's device dropdown
6. Press **Run**

For wireless debugging (Android 11+): in Developer Options enable **Wireless debugging**, then in Android Studio use **Pair Device with Wi-Fi**.

---

## Key Takeaways

- Android Studio bundles the SDK, Kotlin plugin, Gradle, and emulator — one install covers everything
- The manifest declares the app's components and the launcher activity
- `MainActivity.onCreate` calls `setContent { }` to hand the screen to Compose
- `@Composable` functions emit UI; they don't return views
- `@Preview` renders composables in the IDE without running the app
- Use `dp` for sizes/spacing, `sp` for font sizes
- Physical devices give better performance feedback than emulators

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `Gradle sync failed: Could not resolve` | Missing internet or wrong repository URL | Check internet connection; File → Invalidate Caches |
| `Cannot resolve symbol 'Composable'` | Compose dependency not added | Check `build.gradle.kts` has Compose BOM and `ui` dependency |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | Emulator disk full | Wipe emulator data: Device Manager → ▾ → Wipe Data |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Old version of app already installed with different signature | Uninstall the app from the device, then run again |
| Preview panel shows "Build & refresh" | No successful build yet | Run the project once (Shift+F10) then previews will render |

---

## Further Reading

- developer.android.com/jetpack/compose/documentation — official Compose docs
- developer.android.com/training/basics/firstapp — official first app guide
- developer.android.com/studio/intro — Android Studio overview
- Episode 2: Jetpack Compose — UI Basics — Text, Button, Column, Row, Modifier
