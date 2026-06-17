# Episode 2 — Jetpack Compose — UI Basics

**Series:** Android Development from Scratch
**Channel:** The Debugger Diary
**Prerequisites:** Episode 1 (Android Studio setup, first app running)

---

## Episode Overview

This episode is a deep dive into the core building blocks of Jetpack Compose UI. You will learn how layout composables work, how to use `Modifier` to style and position elements, and how to apply Material 3 theming. By the end you will have built a profile card component from scratch using `Row`, `Column`, `Box`, `Surface`, `Text`, `Button`, and `Icon`.

---

## Section 1: Composable Function Rules

A composable function is any function annotated with `@Composable`. The Compose compiler transforms these functions into UI instructions at build time. Key rules:

1. **PascalCase naming** — `ProfileCard`, not `profileCard`. This is a Compose convention that helps Android Studio distinguish composables from regular functions.
2. **No return value** — composables emit UI as a side effect rather than building and returning an object.
3. **Can only be called from other composables** (or from `setContent`).
4. **Can be called multiple times** — Compose may re-run (recompose) a function whenever its inputs change. Code inside composables must be side-effect-free unless explicitly using an effect API (covered in Episode 3).

```kotlin
@Composable
fun MyComponent(title: String) {
    // this is fine — pure UI description
    Text(text = title)
}
```

---

## Section 2: Layout Composables

### Column

Stacks children vertically, top to bottom.

```kotlin
Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text("First")
    Text("Second")
    Text("Third")
}
```

`Arrangement.spacedBy(8.dp)` adds equal spacing between all children without needing to add padding to each one individually.

### Row

Places children horizontally, left to right.

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Left")
    Text("Right")
}
```

### Box

Stacks children on top of each other. Children can be aligned to corners or the center using `contentAlignment` or per-child `Modifier.align()`.

```kotlin
Box(
    modifier = Modifier.size(100.dp),
    contentAlignment = Alignment.Center
) {
    // background layer
    Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
    // foreground layer
    Text("Center")
}
```

Use `Box` for overlays, badges, and avatar images with an icon on top.

### Spacer

Inserts blank space between elements.

```kotlin
Row {
    Text("Left")
    Spacer(modifier = Modifier.width(12.dp))
    Text("Right")
}
```

Prefer `Spacer` over asymmetric padding for readability.

---

## Section 3: Modifier

`Modifier` is an ordered chain of decorations applied to a composable. Every composable accepts a `modifier: Modifier = Modifier` parameter by convention.

```kotlin
@Composable
fun StyledBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}
```

### Order matters

```kotlin
// clip THEN background — background is clipped to circle shape
Modifier.clip(CircleShape).background(Color.Blue)

// background THEN clip — square background, then clipped (same visual result here
// but if you add a border after clip, the difference becomes obvious)
Modifier.background(Color.Blue).clip(CircleShape)
```

The rule: modifiers are applied from left to right (outer to inner in the layout pass). Apply `clip` before `background` to constrain the fill to the shape.

### Common modifiers

| Modifier | Purpose |
|----------|---------|
| `padding(all)` / `padding(start, top, end, bottom)` | Inner spacing |
| `fillMaxWidth()` / `fillMaxHeight()` / `fillMaxSize()` | Expand to parent constraint |
| `size(dp)` / `width(dp)` / `height(dp)` | Fixed dimensions |
| `background(color, shape)` | Fill color with optional shape |
| `clip(shape)` | Clip content to shape |
| `border(width, color, shape)` | Draw a border |
| `clickable { }` | Handle tap — adds ripple effect automatically |
| `alpha(float)` | Transparency (0f = invisible, 1f = opaque) |
| `weight(float)` | Inside Row/Column: fraction of remaining space |
| `align(Alignment)` | Inside Box: position within the box |

---

## Section 4: Text

```kotlin
Text(
    text = "Hello, Compose!",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth()
)
```

**`style`** — always use `MaterialTheme.typography.*` rather than hardcoding `fontSize = 24.sp`. Using the theme respects user accessibility settings (font scale) and keeps your app visually consistent.

Material 3 typography scale (common ones):
- `displayLarge` / `displayMedium` / `displaySmall` — hero numbers, large headlines
- `headlineLarge` / `headlineMedium` / `headlineSmall` — screen titles
- `titleLarge` / `titleMedium` / `titleSmall` — card titles, list item primaries
- `bodyLarge` / `bodyMedium` / `bodySmall` — body copy
- `labelLarge` / `labelMedium` / `labelSmall` — buttons, chips, captions

---

## Section 5: Button and TextButton

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(
        onClick = { /* follow action */ },
        modifier = Modifier.weight(1f)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("Follow")
    }

    OutlinedButton(
        onClick = { /* message action */ },
        modifier = Modifier.weight(1f)
    ) {
        Text("Message")
    }
}
```

**Button variants in Material 3:**
- `Button` — filled, high emphasis
- `OutlinedButton` — outlined, medium emphasis
- `TextButton` — no border or fill, low emphasis
- `ElevatedButton` — filled with a shadow
- `FilledTonalButton` — filled with secondary container color

---

## Section 6: Icon

```kotlin
Icon(
    imageVector = Icons.Filled.Person,
    contentDescription = "Profile picture",
    modifier = Modifier.size(32.dp),
    tint = MaterialTheme.colorScheme.primary
)
```

Icons come from the `androidx.compose.material.icons` library. There are three styles: `Filled` (solid), `Outlined` (line), and `Rounded`. Add the extended icons dependency to access the full icon set:

```kotlin
// build.gradle.kts
implementation("androidx.compose.material:material-icons-extended")
```

**`contentDescription`** — a string read aloud by accessibility screen readers (TalkBack). Provide a meaningful description for interactive or meaningful icons. Pass `null` only for purely decorative icons that have adjacent text conveying the same meaning.

---

## Section 7: Surface

`Surface` is the Material 3 container composable. It handles:
- Background color from the theme
- Elevation shadow (using `shadowElevation`)
- Tonal elevation (color shift based on elevation, using `tonalElevation`)
- Shape clipping

```kotlin
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shadowElevation = 2.dp
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Card Title", style = MaterialTheme.typography.titleMedium)
        Text("Card body text", style = MaterialTheme.typography.bodyMedium)
    }
}
```

For clickable cards, use `Card` instead of `Surface` — it wraps `Surface` with a click handler and the correct semantics for accessibility.

---

## Section 8: Material 3 Theming

The generated `ui/theme/Theme.kt` defines your app's theme. To understand it:

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80
    ) else lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

**How to use Material 3 Dynamic Color (Android 12+):**

```kotlin
val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val colorScheme = when {
    dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
    dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

Dynamic color reads the user's wallpaper and generates a matching color palette automatically.

---

## Section 9: Putting It Together — Profile Card

```kotlin
@Composable
fun ProfileCard(
    name: String,
    role: String,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarPlaceholder()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onFollowClick, modifier = Modifier.weight(1f)) {
                    Text("Follow")
                }
                OutlinedButton(onClick = onMessageClick, modifier = Modifier.weight(1f)) {
                    Text("Message")
                }
            }
        }
    }
}

@Composable
private fun AvatarPlaceholder() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }
}
```

---

## Key Takeaways

- `Column`, `Row`, `Box` are the three layout primitives — combine them for any UI
- `Modifier` is an ordered chain — apply `clip` before `background` to constrain fill to shape
- Use `MaterialTheme.typography.*` for text styles — never hardcode `fontSize`
- `Surface` handles elevation, shape, and background color in one composable
- Always pass `contentDescription` to `Icon` for accessibility
- `@Preview` with multiple annotations lets you check light/dark mode without running the app

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `@Composable invocations can only happen from the context of a @Composable function` | Called a composable from a regular function | Ensure the calling function is also `@Composable` |
| Preview not showing in IDE | Compose BOM or ui-tooling-preview dependency missing | Add `debugImplementation("androidx.compose.ui:ui-tooling")` to build.gradle |
| `Unresolved reference: Icons.Filled.X` | Using extended icons without the dependency | Add `material-icons-extended` to build.gradle.kts |
| Text cut off / ellipsized unexpectedly | Parent container width constraint too narrow | Add `Modifier.fillMaxWidth()` to the parent or check `weight()` usage |
| Modifier not applying in expected order | Order of modifier chain is wrong | Remember: modifiers are applied outermost to innermost (left to right) |

---

## Further Reading

- developer.android.com/jetpack/compose/layouts — official layout docs
- developer.android.com/jetpack/compose/modifiers — modifier reference
- m3.material.io — Material 3 design system (color, typography, components)
- Episode 3: State & Lists — `remember`, `mutableStateOf`, `LazyColumn`
