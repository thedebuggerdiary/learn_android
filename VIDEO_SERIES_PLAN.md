# Android Development from Scratch
**Channel:** The Debugger Diary

## Series Goal

This series teaches Android development using modern tools — Jetpack Compose, ViewModel, Room, and Retrofit — targeting developers who already know Kotlin and want to build production-quality Android apps. You will learn how Android projects are structured, how to build reactive UIs with Compose, how to manage state cleanly with MVVM architecture, and how to connect to a real API and persist data locally. The series ends with a capstone episode that assembles all the pieces into a working app. Each episode is standalone; you can watch them in order or jump to the topic you need.

---

## Episode Plan

| # | Title | Key Topics | Demo | Duration Est. |
|---|-------|-----------|------|--------------|
| 1 | Setup & Project Structure | Android Studio install, project anatomy (manifest, Gradle, `res/`), `MainActivity`, `setContent`, running on emulator and device | Hello World Compose app on emulator | 12–15 min |
| 2 | Jetpack Compose — UI Basics | `@Composable`, `Text`, `Button`, `Image`, `Icon`, `Column`, `Row`, `Box`, `Modifier`, `Surface`, Material 3 theming | Profile card UI | 18–22 min |
| 3 | State & Lists | `remember`, `mutableStateOf`, recomposition, state hoisting, `LazyColumn`, `LazyRow`, `key` parameter | Interactive to-do list (add/remove items) | 15–18 min |
| 4 | Navigation & Multi-screen Apps | `NavController`, `NavHost`, `composable` routes, `navigate()`, passing arguments, back stack management | Two-screen app: list → detail | 15–18 min |
| 5 | ViewModel & MVVM Architecture | Why MVVM, `ViewModel`, `viewModelScope`, `StateFlow`, `collectAsState()`, unidirectional data flow, separating UI from logic | Counter app refactored to MVVM; note list with ViewModel | 18–22 min |
| 6 | Local Data with Room | `@Entity`, `@Dao`, `@Database`, CRUD operations, `Flow` from Room, coroutines, migrations | Notes app with persistent storage | 18–22 min |
| 7 | Networking with Retrofit | Retrofit setup, `@GET`/`@POST`, Kotlin Serialization (or Moshi), coroutines + `suspend`, error handling, loading/error states in UI | GitHub repo browser (real API) | 18–22 min |
| 8 | Building a Complete App | MVVM + Room + Retrofit wired together, dependency injection with Hilt (intro), repository pattern, end-to-end feature walk | News reader app: fetch from API, cache to Room, show in Compose | 25–30 min |

**Total series:** ~139–169 minutes across 8 episodes

---

## Prerequisites

### Knowledge
- Kotlin: data classes, coroutines (`suspend`, `launch`, `Flow`), lambdas, extension functions
- Basic understanding of what an Android app is (what a screen and an activity are)
- For Episodes 6–8: familiarity with SQL concepts (SELECT, INSERT) helps but is not required

### Tools to Install

**Android Studio (latest stable)**
- Download from developer.android.com/studio
- Bundles: Android SDK, Gradle, Kotlin plugin, AVD manager
- Verify by creating a new Empty Activity (Compose) project

**JDK 17+**
- Bundled with Android Studio — no separate install needed
- Verify in Android Studio: File → Project Structure → SDK Location

**A device or emulator**
- Emulator: Device Manager → Create Device → Pixel 6, API 34
- Physical device: enable Developer Options + USB Debugging

---

## Key Libraries Used

| Library | Episodes | Purpose |
|---------|----------|---------|
| Jetpack Compose (BOM) | 2–8 | Declarative UI |
| Navigation Compose | 4–8 | Screen routing |
| Lifecycle ViewModel | 5–8 | State management |
| Room | 6, 8 | Local SQLite ORM |
| Retrofit | 7, 8 | HTTP client |
| Kotlinx Serialization | 7, 8 | JSON parsing |
| Hilt | 8 | Dependency injection |
| Coil Compose | 7, 8 | Async image loading |

---

## Repository Structure

```
learn_android/
├── VIDEO_SERIES_PLAN.md          ← You are here
├── README.md
├── ep01_setup_and_project_structure/
│   ├── slides.md                 ← Marp presentation
│   ├── notes.md                  ← Full reference + source code
│   └── code/
│       └── HelloWorldApp/        ← Android Studio project
├── ep02_compose_ui_basics/
│   ├── slides.md
│   ├── notes.md
│   └── code/
│       └── ProfileCardApp/
├── ep03_state_and_lists/
│   ├── slides.md
│   ├── notes.md
│   └── code/
│       └── TodoApp/
├── ep04_navigation/
│   ├── slides.md
│   ├── notes.md
│   └── code/
│       └── ListDetailApp/
├── ep05_viewmodel_and_mvvm/
│   ├── slides.md
│   ├── notes.md
│   └── code/
│       └── NotesViewModelApp/
├── ep06_room_database/
│   ├── slides.md
│   ├── notes.md
│   └── code/
│       └── NotesPersistApp/
├── ep07_networking_retrofit/
│   ├── slides.md
│   ├── notes.md
│   └── code/
│       └── GithubBrowserApp/
└── ep08_complete_app/
    ├── slides.md
    ├── notes.md
    └── code/
        └── NewsReaderApp/
```

---

## How to Use This Repo

- **Slides** (`slides.md`): Open in VS Code with the Marp extension for a live preview. Export to PDF or HTML for recording.
- **Notes** (`notes.md`): Full script/reference. Read before recording. Contains all source code, explanations, and talking points.
- **Code** (`code/`): Each episode's `code/` folder is a self-contained Android Studio project. Open the project folder in Android Studio directly (`File → Open`), let Gradle sync, then run.
