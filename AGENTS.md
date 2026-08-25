# AGENTS.md

Guidance for AI agents working in this repository.

## Project

**Ballooner** is a simple Android app for creating short comics using images
in the device. It is also a learning project for working with agents and skills,
so prefer clear, conventional code over clever abstractions, and explain non-obvious
decisions in short commit messages.

## Tech stack

- **Language:** Kotlin (JVM target 17)
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM with a unidirectional data flow (UI state exposed as
  `StateFlow` from a `ViewModel`)
- **Persistence:** Room
- **Dependency injection:** Hilt
- **Async:** Kotlin Coroutines + Flow
- **Build:** Gradle (Kotlin DSL, `.gradle.kts`)
- **Tests:** JUnit + Turbine (Flow), Compose UI tests, Room in-memory DB tests

## Module / package layout

```
app/
  src/main/java/com/ballooner/
    data/            # Room entities, DAOs, repositories
    domain/          # Models + use cases (plain Kotlin, no Android deps)
    ui/
      <feature>/     # Composable screen + ViewModel + UI state per feature
      theme/         # Material 3 theme
    di/              # Hilt modules
```

## Conventions

- Each screen has its own package under `ui/<feature>/` containing:
  - `<Feature>Screen.kt` — stateless Composable that takes state + callbacks
  - `<Feature>ViewModel.kt` — exposes `StateFlow<<Feature>UiState>`
  - `<Feature>UiState.kt` — immutable data class describing the screen
- ViewModels never reference Composables or Android `Context`. Inject
  repositories, not DAOs, into ViewModels.
- `domain/` must not import anything from `androidx` or `android.*`.
- Prefer `sealed interface` for UI state that has distinct modes
  (e.g. `Loading`, `Empty`, `Content`).
- Name Room migrations `MIGRATION_<from>_<to>` and always bump the DB `version`.
- Keep functions small and pure where possible; push side effects to the edges.

## Definition of done

Before claiming a task is complete:

1. Code compiles: `./gradlew assembleDebug`
2. Unit tests pass: `./gradlew testDebugUnitTest`
3. Lint is clean: `./gradlew lintDebug`
4. New logic has at least one test.

## What to ask before doing

- Adding a new third-party dependency.
- Changing the database schema (requires a migration + version bump).
- Introducing a new architectural pattern not described above.

## Skills

Local skills live in `.agents/skills/mine/`. Relevant ones for this project:

- **add-compose-screen** — scaffold a new MVVM screen (Screen + ViewModel + UiState).
- **add-room-entity** — add a Room entity, DAO, repository, and migration.

Other general engineering skills live in `.agents/skills/`
