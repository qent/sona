# sona

Chat with Anthropic models directly from a side panel in your IDE. The chat
history is persisted in IDE storage so it survives restarts. Configure API
token, endpoint and model in the settings. Responses stream gradually so you
can watch them being generated in real time.

<!-- Plugin description -->
Chat with an Anthropic language model right inside your IDE. The chat history is stored persistently so you can resume conversations after restarting the IDE.
<!-- Plugin description end -->

## Installation
Use the IDE plugin manager or download the plugin from releases.

## Nightly builds
A scheduled GitHub Actions workflow runs every night and builds a plugin archive if there were changes in the `main` branch during the previous day. The resulting archive is uploaded as a workflow artifact available from the run page.

## Roles
The plugin supports multiple system roles. Open the roles screen from the tool
window actions to switch between roles or create new ones. Each role has its own
prompt text. Roles can be added, selected and removed (except the last role).

## Architecture Overview

The project is split into two modules:

* `core` – contains all domain logic and is free from IntelliJ
  dependencies. It exposes a Flow‑based API used by the UI module.
* plugin module – implements the IntelliJ part and hosts the Compose UI.

### Core module

The `core` module defines a small MVI style state container. The main
entry point is `ChatFlow`, a `Flow<Chat>` backed by a `MutableStateFlow`.
It orchestrates message exchange with the language model and exposes the
current conversation state:

```
data class Chat(
    val chatId: String,
    val tokenUsage: TokenUsage,
    val messages: List<ChatRepositoryMessage> = emptyList(),
    val requestInProgress: Boolean = false
)
```

`StateProvider` consumes `ChatFlow` and maps it to a higher level
`State` model used by the UI. Both `ChatFlow` and `StateProvider`
operate purely on Kotlin flows without any IntelliJ types.

Repositories for settings and chat history are declared as interfaces in
`core` so that the UI module can provide IDE specific implementations.

### Plugin module

`PluginStateFlow` is registered as a project service and bridges the
`StateProvider` to IntelliJ. It also supplies implementations of the
repositories and of helper tools such as retrieving the currently focused
file contents. The Compose‑based UI in `ui/PluginPanel.kt` collects the
`StateFlow` from `PluginStateFlow` to render either the chat screen or the
chat history list.

Chat history is persisted via `PluginChatRepository`, which uses IntelliJ's
`PersistentStateComponent` to store chats in `chat_history.xml`.

The UI is written entirely with Compose (via Jewel Compose) in accordance
with the development guidelines.

### Styling

UI components should be wrapped in `SonaTheme`, which supplies color palettes
and markdown typography. `ThemeService` tracks IDE theme changes so the plugin
automatically switches between light and dark palettes.

### Flow usage

All state propagation relies on Kotlin `Flow`/`StateFlow`:

* `ChatFlow` implements `Flow<Chat>` and exposes conversation updates.
* `StateProvider` provides a `StateFlow<State>` for the UI.
* `PluginStateFlow` exposes the same `StateFlow` as a project level
  service.

This approach keeps the logic reactive and allows the UI to simply
collect the latest state.

## Building

Run `./gradlew build` to assemble the plugin. The build script uses the
IntelliJ Platform Gradle Plugin and downloads the required IDE
distribution automatically.

During development you can launch the IDE with the plugin by executing
`./gradlew runIde`.
