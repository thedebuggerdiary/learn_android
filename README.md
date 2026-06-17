# Android Development from Scratch

A YouTube video series by **The Debugger Diary** that teaches Android development for developers who already know Kotlin and want to build real Android apps using modern tools.

You will learn Jetpack Compose, navigation, state management with ViewModel and StateFlow, local persistence with Room, networking with Retrofit, and finish by building a complete app that wires everything together.

---

## Who This Is For

- Kotlin developers who want to start building Android apps
- Developers who completed the *Kotlin from Scratch* series and want the next step
- Android developers still using XML layouts who want to move to Jetpack Compose
- Anyone who wants to understand the modern Android architecture stack (MVVM, Room, Retrofit)

Kotlin knowledge is assumed. If you are not comfortable with data classes, coroutines, and lambdas, watch [Kotlin from Scratch](../learn_kotlin/) first.

---

## Episodes

| # | Folder | Title |
|---|--------|-------|
| 1 | [ep01_setup_and_project_structure](ep01_setup_and_project_structure/) | Setup & Project Structure |
| 2 | [ep02_compose_ui_basics](ep02_compose_ui_basics/) | Jetpack Compose — UI Basics |
| 3 | [ep03_state_and_lists](ep03_state_and_lists/) | State, Recomposition & Lists |
| 4 | [ep04_navigation](ep04_navigation/) | Navigation & Multi-screen Apps |
| 5 | [ep05_viewmodel_and_mvvm](ep05_viewmodel_and_mvvm/) | ViewModel & MVVM Architecture |
| 6 | [ep06_room_database](ep06_room_database/) | Local Data with Room |
| 7 | [ep07_networking_retrofit](ep07_networking_retrofit/) | Networking with Retrofit |
| 8 | [ep08_complete_app](ep08_complete_app/) | Building a Complete App |

---

## How to Use This Repo

Each episode folder contains:

- **`slides.md`** — Marp Markdown presentation. Open in VS Code with the [Marp for VS Code](https://marketplace.visualstudio.com/items?itemName=marp-team.marp-vscode) extension for a live slide preview. Export to PDF/HTML for screen recording.
- **`notes.md`** — Complete reference document with full explanations, all source code, talking points, and further reading.
- **`code/`** — A runnable Android Studio project (or standalone Kotlin files) for the episode.

---

## Setup

### Install Android Studio

Download **Android Studio** (latest stable) from [developer.android.com/studio](https://developer.android.com/studio). It bundles:

- The Android SDK
- An AVD (Android Virtual Device) manager for emulators
- Gradle build tooling
- Kotlin plugin

Verify by opening Android Studio and creating a new **Empty Activity** project with Kotlin + Compose.

### Run on an emulator

1. Open **Device Manager** (toolbar or Tools → Device Manager)
2. Create a virtual device — Pixel 6, API 34 is a good default
3. Press the green **Run** button (`Shift+F10`)

### Run on a physical device

1. Enable **Developer Options** on your phone (Settings → About → tap Build Number 7 times)
2. Enable **USB Debugging**
3. Connect via USB — Android Studio will detect the device automatically

### Project structure overview

```
MyApp/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml     ← app entry point & permissions
│   │   ├── java/.../               ← Kotlin source files
│   │   └── res/                    ← drawables, strings, themes
│   └── build.gradle.kts            ← app-level dependencies
├── build.gradle.kts                ← project-level build config
└── settings.gradle.kts
```

### Marp (for slides)

Install the **Marp for VS Code** extension, or use the CLI:

```bash
npm install -g @marp-team/marp-cli
marp ep01_setup_and_project_structure/slides.md --pdf
```

---

## See Also

- `VIDEO_SERIES_PLAN.md` — full episode breakdown with topics, demos, and duration estimates
- [Kotlin from Scratch](../learn_kotlin/) — prerequisite series covering the Kotlin language
