---
name: add-compose-screen
description: Scaffold a new Jetpack Compose screen following the app's MVVM conventions (Screen + ViewModel + UiState). Use when the user asks to add a new screen, page, or feature UI to the Ballooner Android app.
---

# Add Compose Screen

Use this skill to add a new screen to the Ballooner app. Every screen follows
the same MVVM shape so the codebase stays predictable.

## Inputs to gather

Before generating code, confirm:

1. **Feature name** in PascalCase (e.g. `ComicList`, `PanelEditor`, `BalloonEditor`).
2. **What the screen shows** and which repository/data it needs.
3. **User actions** on the screen (pick image, add/drag a balloon, edit text).

If any are unclear, ask one short round of questions before writing code.

## Workflow

1. Create the package `app/src/main/java/com/ballooner/ui/<feature>/`.
2. Create three files (see templates below), substituting `<Feature>`:
   - `<Feature>UiState.kt`
   - `<Feature>ViewModel.kt`
   - `<Feature>Screen.kt`
3. Register the screen in the app's navigation graph (`ui/navigation`), adding a
   route constant and a `composable(...)` entry.
4. Verify with `./gradlew assembleDebug`.
5. Add at least one ViewModel test using Turbine to assert the emitted state.

## Rules

- The `Screen` Composable is **stateless**: it receives `uiState` and lambdas,
  and must be previewable with a fake state (add a `@Preview`).
- The `ViewModel` exposes exactly one `StateFlow<<Feature>UiState>` named
  `uiState` and injects **repository interfaces**, never DAOs. Depending on the
  interface lets tests supply a hand-written fake.
- Use `sealed interface` for `UiState` when the screen has distinct modes
  (Loading / Empty / Content / Error).
- No `android.*` or `Context` references inside the ViewModel.

## Templates

### `<Feature>UiState.kt`

```kotlin
package com.ballooner.ui.<feature>

sealed interface <Feature>UiState {
    data object Loading : <Feature>UiState
    data class Content(val items: List<Any>) : <Feature>UiState
    data object Empty : <Feature>UiState
}
```

### `<Feature>ViewModel.kt`

```kotlin
package com.ballooner.ui.<feature>

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class <Feature>ViewModel @Inject constructor(
    // inject repositories here
) : ViewModel() {

    private val _uiState = MutableStateFlow<<Feature>UiState>(<Feature>UiState.Loading)
    val uiState: StateFlow<<Feature>UiState> = _uiState.asStateFlow()

    // TODO: collect from repository in an init { } block via viewModelScope
}
```

### `<Feature>Screen.kt`

```kotlin
package com.ballooner.ui.<feature>

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun <Feature>Route(viewModel: <Feature>ViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    <Feature>Screen(uiState = uiState)
}

@Composable
fun <Feature>Screen(uiState: <Feature>UiState) {
    // render based on uiState
}

@Preview
@Composable
private fun <Feature>ScreenPreview() {
    <Feature>Screen(uiState = <Feature>UiState.Empty)
}
```
