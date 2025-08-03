# sona

Chat with various language models directly from a side panel in your IDE. The chat
history is persisted in IDE storage so it survives restarts. Connection details are
managed as presets where you choose a provider, model, API endpoint and token. Responses stream
gradually so you can watch them being generated in real time and stop the stream at any moment
to keep the partial reply.

<!-- Plugin description -->
Chat with Anthropic, OpenAI, Deepseek or Gemini models right inside your IDE. The chat history is stored persistently so
you can resume conversations after restarting the IDE.
<!-- Plugin description end -->

## Installation

Use the IDE plugin manager or download the plugin from releases.

## Nightly builds

A scheduled GitHub Actions workflow runs every night and builds a plugin archive if there were changes in the `main`
branch during the previous day. The resulting archive is uploaded as a workflow artifact available from the run page.

## Roles

The plugin supports multiple system roles. Open the roles screen from the tool
window actions to switch between roles or create new ones. Each role has its own
prompt text. Roles can be added, selected and removed, but the default Architect
and Code roles cannot be deleted. The active role can also be changed directly
from the chat via the selector under the message input. The text of the active
role is sent as a system message with every request but is not stored in the
chat history.

Models can also switch roles themselves using tools to toggle between Architect and Code.

When starting a new conversation the message field shows a placeholder tailored
to the active role. The Architect role suggests planning and design, the Code
role suggests implementation, and other roles display a generic "Describe your
task..." prompt. Once messages are present, the placeholder becomes
"Type a message...".

## Presets

LLM credentials are stored as presets. Each preset defines the provider, model,
API endpoint and token. Manage presets from the Presets screen opened via the
tool window actions. At least one preset must exist for the chat to function.

The settings screen contains only a single option: **Ignore HTTPS errors**. Enable
it to trust all HTTPS certificates when connecting to custom endpoints.

## Tools

When the model requests to run a tool, the plugin asks for permission before
executing it. You can allow the action once or choose **Always in this chat** to
skip future confirmations for the same tool within the current conversation.

The available tools let the model read the focused file and switch the active role between Architect and Code.

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
    val requestInProgress: Boolean = false,
    val toolRequest: String? = null
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
