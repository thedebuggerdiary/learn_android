---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
---

# Setup & Project Structure
### Episode 1 — Android Development from Scratch
**The Debugger Diary**

---

# Why Android Development?

- **3 billion+ active Android devices** worldwide
- Kotlin is the **official language** for Android since 2019
- **Jetpack Compose** — modern declarative UI (no XML required)
- You already know Kotlin — you're 80% of the way there

---

# What You'll Build This Episode

A Hello World app running on an emulator using Jetpack Compose

```
┌─────────────────────┐
│                     │
│   Hello, Android!   │
│   Welcome to        │
│   The Debugger      │
│   Diary             │
│                     │
└─────────────────────┘
```

---

# Step 1: Install Android Studio

Download the latest stable from **developer.android.com/studio**

It bundles everything you need:
- Android SDK
- Kotlin plugin
- Gradle build system
- AVD (Android Virtual Device) manager
- Layout / Compose preview

---

# Step 2: Create Your First Project

1. **New Project → Empty Activity**
2. Name: `HelloAndroid`
3. Package: `com.debuggerdiary.ep01`
4. Language: **Kotlin**
5. Minimum SDK: **API 26** (Android 8.0 — covers ~95% of devices)
6. Build configuration: **Kotlin DSL**

---

# Project Structure

```
HelloAndroid/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml   ← app entry point & permissions
│   │   ├── java/com/.../
│   │   │   └── MainActivity.kt   ← your code lives here
│   │   └── res/
│   │       ├── values/strings.xml
│   │       └── drawable/
│   └── build.gradle.kts          ← app dependencies
├── build.gradle.kts              ← project-level config
└── settings.gradle.kts
```

---

# AndroidManifest.xml

```xml
<manifest>
    <application
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

`MAIN` + `LAUNCHER` = this is the screen that launches when the user taps the app icon.

---

# MainActivity — the Entry Point

```kotlin
class MainActivity : AppCompatActivity() {
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

`setContent { }` hands the screen to Compose — everything inside is declarative UI.

---

# Your First Composable

```kotlin
@Composable
fun Greeting(name: String) {
    Text(
        text = "Hello, $name!",
        modifier = Modifier.padding(16.dp)
    )
}
```

- `@Composable` — marks a function that describes UI
- No `return` — you call other composables, not build views manually
- `Modifier` — how the element sizes, spaces, and decorates itself

---

# Previewing Without Running

```kotlin
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HelloAndroidTheme {
        Greeting("Debugger")
    }
}
```

Click **Split** in Android Studio to see the preview panel update as you type. No emulator needed for design iteration.

---

# Step 3: Run on an Emulator

1. **Device Manager** (toolbar) → **Create Device**
2. Pick **Pixel 6**, select **API 34** system image
3. Download the image if prompted
4. Press the green **Run** button (`Shift+F10`)

First launch takes ~2 minutes to cold-boot. Subsequent runs are fast.

---

# Step 4: Run on a Physical Device

1. Settings → About Phone → tap **Build Number** 7 times
2. Settings → Developer Options → enable **USB Debugging**
3. Connect via USB — accept the prompt on the phone
4. Your device appears in the device dropdown in Android Studio
5. Press **Run**

Physical device is always faster than the emulator for testing.

---

# Key Takeaways

- Android Studio bundles everything — no separate SDK installs
- `MainActivity` is the entry point; `setContent { }` starts Compose
- `@Composable` functions describe UI — they don't return Views
- `@Preview` lets you design without running the emulator
- Manifest declares the app's entry point and permissions

---

# What's Next — Episode 2

**Jetpack Compose — UI Basics**

- `@Composable` function anatomy in depth
- `Text`, `Button`, `Image`, `Icon` components
- `Column`, `Row`, `Box` — layout building blocks
- `Modifier` — padding, size, background, clickable
- Material 3 theming

**The Debugger Diary** — Android Development from Scratch
_"Understand the tools, not just the syntax."_
