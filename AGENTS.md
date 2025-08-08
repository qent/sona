# Development Guidelines

- Follow a simple MVI architecture using `Flow<PluginState>` from the logic module.
- The logic module must not depend on IntelliJ SDK; only the UI module may use it.
- Use Compose for all UI components.
- Retrieve all user-facing text from the global `Strings` object with entries in `messages/Strings*.properties` (English, Russian, Chinese, German, and French).
- Settings are stored using a repository interface in the logic module with an IntelliJ implementation in the UI module.
- Chat history is persisted via a repository implementation (`PluginChatRepository`) that stores chats across IDE
  sessions.
  - Individual messages can be copied via a clipboard icon or removed via a trash icon. Deleting a message truncates the chat history from that point.
  - System prompts (roles) are stored in `RolesRepository` and can be managed from
    the Roles screen. Each role has a name and text. The default Architect and
    Code roles cannot be deleted.
- LLM connection details are organised as presets via `PresetsRepository`. At least one preset must exist for the chat
  to work and they are managed from the Presets screen.
- Supported LLM providers and models are defined in `core/src/main/resources/providers.json` so new options can be
  added without modifying the code. A `Custom OpenAI` provider is registered in code for manual model entry and
  always reports zero token cost.
- Plugin settings contain an "Ignore HTTPS errors" flag and an "Anthropic Settings"
  section to cache system prompts and tool descriptions in requests.
- Each chat tracks tools approved by the user so that previously allowed tools run without asking again.
- A 🤘 button next to the send action can temporarily auto-approve all tool
  requests in the current chat. The setting resets when switching chats and is
  not persisted.
- A tool is available to switch the active role between Architect and Code.
- The UI passes a list of additional `SystemMessage` values to the core. The first message describes the current
  environment (OS, IDE, Java, Python, Node.js, file extension statistics, build systems) and is prepended to every LLM request.
- ChatFlow leverages langchain4j `AiService` and `TokenStream` to emit partial responses and tool events via streaming callbacks.
- AI messages show a gear icon that toggles visibility of requested tools. Tool outputs stream into a terminal-style bubble with animated dots until completion.
- Run `./gradlew build` before committing any changes.
- After completing a task, make sure `AGENTS.md` and `README.md` reflect the latest behavior.

## Styling

Use `SonaTheme` from `ui/Theme.kt` for all colors and markdown typography. Colors are available via `SonaTheme.colors`
and markdown styling via `SonaTheme.markdownColors` and `SonaTheme.markdownTypography`. New composables should rely on
these values instead of hard coded colors to keep the look consistent.
`ThemeService` listens to IDE Look&Feel changes and exposes a `StateFlow<Boolean>` to indicate if the IDE is in dark
mode. Wrap top level `JewelComposePanel` contents in `SonaTheme(dark)` using this value so the plugin adapts when the
IDE theme changes.

## Current structure

- **core module** – contains `ChatFlow`, a thin `StateProvider` facade, `StateFactory` and domain interactors (`ChatStateInteractor`, `RolesStateInteractor`, `PresetsStateInteractor`, `ServersStateInteractor`). `ChatFlow` is a
  `MutableStateFlow` based `Flow` that manages the conversation with the model
  and keeps track of token usage while the interactors handle domain logic.
- **Plugin module** – provides IntelliJ implementations of the repositories,
  the Compose UI and `PluginStateFlow`, a project service exposing
  `StateFlow<State>` from `StateProvider`.

`ExternalTools` are implemented in the plugin module using the IntelliJ
API while `InternalTools` live in the core module for plugin interactions like
switching roles. `ToolsInfoDecorator` combines them and routes file responses through a
permission manager that checks absolute paths against a whitelist (project root by default)
and a blacklist of sensitive files before exposing file contents to the model.
File permissions can also be adjusted by adding a `sona.json` file at the project root with
`permissions.files.whitelist` and `blacklist` arrays of regex patterns.
`sona.json` may additionally define an `mcpServers` object keyed by server name with Model
Context Protocol server configurations including `enabled`, `command`, `args`, environment
variables, transport, URL, working directory, request headers and an optional
`disabledTools` array listing tool names to ignore. Supported transports are `stdio` and
`http` and each server runs in its own coroutine so failures are isolated. Tools exposed by
these servers require the same user permission prompts as local tools. Server enablement and
disabled tools are stored in `sona.json`; only servers marked as enabled reconnect
automatically on restart.
`ChatFlow` depends only on the `Tools` interface and receives the decorator from `StateProvider`.

Whenever you extend the logic make sure the flow of state remains unidirectional
and that the core module stays free from IntelliJ SDK imports.
