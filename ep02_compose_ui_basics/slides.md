---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
---

# Jetpack Compose — UI Basics
### Episode 2 — Android Development from Scratch
**The Debugger Diary**

---

# What You'll Build

A profile card — a common UI pattern that uses every concept in this episode

```
┌────────────────────────────┐
│  ┌────┐  Himanshu Keshri   │
│  │    │  Android Developer │
│  └────┘                    │
│  [  Follow  ] [  Message ] │
└────────────────────────────┘
```

---

# The Composable Function

```kotlin
@Composable
fun ProfileCard(name: String, role: String) {
    // describes UI — does NOT return a View
}
```

Rules:
- Must be annotated with `@Composable`
- Name starts with an uppercase letter (PascalCase)
- No return value — it **emits** UI
- Can call other `@Composable` functions
- Can NOT be called from non-composable code (except `setContent`)

---

# Layout: Column, Row, Box

```kotlin
Column { /* children stacked vertically */ }
Row    { /* children placed horizontally */ }
Box    { /* children stacked on top of each other */ }
```

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Box(modifier = Modifier.size(64.dp).background(Color.Gray))
    Column(modifier = Modifier.padding(start = 12.dp)) {
        Text("Himanshu Keshri")
        Text("Android Developer")
    }
}
```

---

# Modifier — The Swiss Army Knife

```kotlin
Modifier
    .padding(16.dp)          // space inside/outside
    .fillMaxWidth()          // take full available width
    .size(64.dp)             // fixed width and height
    .background(Color.Blue)  // fill color
    .clip(CircleShape)       // clip to shape
    .clickable { onClick() } // handle taps
    .border(1.dp, Color.Gray, CircleShape)
```

Order matters — `clip` before `background` vs `background` before `clip` produce different results.

---

# Text

```kotlin
Text(
    text = "Himanshu Keshri",
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onSurface,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
)
```

Always use `MaterialTheme.typography.*` instead of hardcoded sizes — it respects the user's system font scale setting.

---

# Button & TextButton

```kotlin
Button(
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Follow")
}

TextButton(onClick = { /* action */ }) {
    Text("Message")
}
```

Button slots: `Button { }` takes a composable lambda — you can put an Icon + Text inside for an icon button.

---

# Icon

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person

Icon(
    imageVector = Icons.Filled.Person,
    contentDescription = "Profile picture placeholder",
    modifier = Modifier.size(40.dp),
    tint = MaterialTheme.colorScheme.onPrimary
)
```

Always provide `contentDescription` for accessibility screen readers. Use `null` only if the icon is purely decorative and nearby text already describes it.

---

# Surface

```kotlin
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    shape = RoundedCornerShape(12.dp),
    shadowElevation = 4.dp,
    color = MaterialTheme.colorScheme.surface
) {
    // content inside the card
}
```

`Surface` handles elevation, shape, and color in one composable. Use it as the root of card-like components.

---

# Material 3 Theming

```kotlin
MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Color(0xFF6650A4),
        secondary = Color(0xFF625B71)
    ),
    typography = Typography()
) {
    // everything inside uses these colors and fonts
}
```

Access theme values anywhere:
```kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.typography.headlineMedium
MaterialTheme.shapes.medium
```

---

# The Full Profile Card

```kotlin
@Composable
fun ProfileCard(name: String, role: String) {
    Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 4.dp) {
        Row(modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AvatarPlaceholder()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(role, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

---

# Preview — Multiple Variants

```kotlin
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true,
    uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
fun ProfileCardPreview() {
    AppTheme {
        ProfileCard(
            name = "Himanshu Keshri",
            role = "Android Developer"
        )
    }
}
```

---

# Key Takeaways

- `Column` / `Row` / `Box` are the three layout primitives
- `Modifier` controls spacing, size, shape, and interaction — order matters
- `Text`, `Button`, `Icon`, `Surface` cover 80% of typical UIs
- Always use `MaterialTheme.typography` and `MaterialTheme.colorScheme`
- `@Preview` with multiple annotations = instant design QA

---

# What's Next — Episode 3

**State & Lists**

- `remember` and `mutableStateOf` — making UI reactive
- What triggers recomposition
- State hoisting — keeping composables testable
- `LazyColumn` / `LazyRow` — efficient scrolling lists
- Building an interactive to-do list

**The Debugger Diary** — Android Development from Scratch
_"Understand the tools, not just the syntax."_
