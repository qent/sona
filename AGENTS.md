# Development Guidelines

- Follow a simple MVI architecture using `Flow<PluginState>` from the logic module.
- The logic module must not depend on IntelliJ SDK; only the UI module may use it.
- Use Compose for all UI components.
- Settings are stored using a repository interface in the logic module with an IntelliJ implementation in the UI module.
- Chat history is persisted via a repository implementation (`PluginChatRepository`) that stores chats across IDE sessions.
- System prompts (roles) are stored in `RolesRepository` and can be managed from
  the Roles screen. Each role has a name and text and the last role cannot be
  deleted.
- Run `./gradlew build` before committing any changes.
- After completing a task, make sure `AGENTS.md` and `README.md` reflect the latest behavior.

## Current structure

- **core module** – contains `ChatFlow` and `StateProvider`. `ChatFlow` is a
  `MutableStateFlow` based `Flow` that manages the conversation with the model
  and keeps track of token usage. `StateProvider` maps that flow to a higher
  level `State` used by the UI.
- **Plugin module** – provides IntelliJ implementations of the repositories,
  the Compose UI and `PluginStateFlow`, a project service exposing
  `StateFlow<State>` from `StateProvider`.

`Tools` are injected into `ChatFlow` through an interface so that IDE specific
helpers can be implemented in the plugin module.

Whenever you extend the logic make sure the flow of state remains unidirectional
and that the core module stays free from IntelliJ SDK imports.
