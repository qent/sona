# Development Guidelines

- Follow a simple MVI architecture using `Flow<PluginState>` from the logic module.
- The logic module must not depend on IntelliJ SDK; only the UI module may use it.
- Use Compose for all UI components.
- Settings are stored using a repository interface in the logic module with an IntelliJ implementation in the UI module.
- Chat history is persisted via a repository implementation (`PluginChatRepository`) that stores chats across IDE
  sessions.
  - Individual messages can be copied via a clipboard icon or removed via a trash icon. Deleting a message truncates the chat history from that point.
  - System prompts (roles) are stored in `RolesRepository` and can be managed from
    the Roles screen. Each role has a name and text. The default Architect and
    Code roles cannot be deleted.
- LLM connection details are organised as presets via `PresetsRepository`. At least one preset must exist for the chat
  to work and they are managed from the Presets screen.
- Plugin settings contain only the "Ignore HTTPS errors" flag which, when enabled, trusts all HTTPS certificates.
- Each chat tracks tools approved by the user so that previously allowed tools run without asking again.
- A tool is available to switch the active role between Architect and Code.
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

- **core module** – contains `ChatFlow` and `StateProvider`. `ChatFlow` is a
  `MutableStateFlow` based `Flow` that manages the conversation with the model
  and keeps track of token usage. `StateProvider` maps that flow to a higher
  level `State` used by the UI.
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
`sona.json` may additionally define an `mcpServers` array with Model Context Protocol server
configurations including name, command, arguments, environment variables, transport, URL,
working directory and request headers.
`ChatFlow` depends only on the `Tools` interface and receives the decorator from `StateProvider`.

Whenever you extend the logic make sure the flow of state remains unidirectional
and that the core module stays free from IntelliJ SDK imports.
