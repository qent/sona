# Development Guidelines

- Follow a simple MVI architecture using `Flow<PluginState>` from the logic module.
- The logic module must not depend on IntelliJ SDK; only the UI module may use it.
- Use Compose for all UI components.
- Retrieve all user-facing text from the global `Strings` object with entries in `messages/Strings*.properties` (English, Russian, Chinese, German, and French).
- Settings are stored using a repository interface in the logic module with an IntelliJ implementation in the UI module.
- Chat history is persisted via a repository implementation (`PluginChatRepository`) that stores chats across IDE
  sessions.
- User messages offer copy, edit, and delete icons. Edit copies the message text into the input field and removes it and all following messages, letting the user modify and resend manually. AI and tool messages only provide copy, and deleting a user message truncates the chat history from that point.
  - Code blocks display Copy and Apply Patch icons. The latter opens a diff view to review and apply the patch.
  - System prompts (roles) are stored in `RolesRepository` and can be managed from
    the Roles screen. Each role has a name, a short description for tool usage,
    and a detailed system prompt. The default Architect and Code roles cannot be
    deleted.
- LLM connection details are organised as presets via `PresetsRepository`. API keys are stored via the IDE password
  store and never written to `presets.xml`. At least one preset must exist for the chat
  to work and they are managed from the Presets screen.
- Supported LLM providers and models are defined in `core/src/main/resources/providers.json` so new options can be
  added without modifying the code. A `Custom OpenAI` provider is registered in code for manual model entry and
  always reports zero token cost.
- Plugin settings contain "Answer in English", "Ignore HTTPS errors" and "Enable plugin logging" flags
  and an "Anthropic Settings" section to cache system prompts and tool descriptions
  in requests.
- Plugin settings also provide a global "LLM API retries" field controlling how
  many times failed requests are retried with exponential backoff.
- Each chat tracks tools approved by the user so that previously allowed tools run without asking again.
- A 🤘 button next to the send action can temporarily auto-approve all tool
  requests in the current chat. The setting resets when switching chats and is
  not persisted.
- A tool is available to switch the active role by name using the roles' short
  descriptions.
- A tool lists files and directories for a given path, appending "/" to directory names and
  including the first-level contents of each directory.
- Patches are applied in a single step: `applyPatch` takes a unified diff and applies it. Users can preview the diff before applying.
- Two tools manage a dedicated "Sona" terminal: one sends commands for execution and another reads its output. Commands run from the project root by default.
- A tool returns class and object dependencies for a specified file, including paths to files
  defining those dependencies.
- A dedicated search agent can query the project using tools that find files by name,
  locate classes or search for text patterns. The agent's intermediate messages are shown in
  the chat but are not stored; only the final results are returned.
- The UI passes a list of additional `SystemMessage` values to the core. The first message describes the current
  environment (OS, IDE, Java, Python, Node.js, project root path, file extension statistics, build systems) and is prepended to every LLM request.
  - Any `.md` files in `src/main/resources/prompts` are bundled. Projects may provide `.md` files in `.sona/prompts` that apply to all roles and in `.sona/prompts/{role}` for role-specific messages; all are appended as additional system messages.
- ChatController leverages langchain4j `AiService` and `TokenStream` to emit partial responses and tool events via streaming callbacks.
- When preparing messages for the `AiService`, append a `ToolExecutionResultMessage` with localized text `Strings.connectionError` after any `AiMessage` that requests a tool but lacks a following result message and persist it to the chat repository.
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

- **core module** – contains `ChatController`, `ChatStateFlow`, a thin `StateProvider` facade, `StateFactory` and domain interactors (`ChatStateInteractor`, `RolesListStateInteractor`, `EditRoleStateInteractor`, `PresetsListStateInteractor`, `EditPresetStateInteractor`, `ServersStateInteractor`). `ChatStateFlow` emits the current chat while `ChatController` manages the conversation with the model and keeps track of token usage while the interactors handle domain logic.
- **Plugin module** – provides IntelliJ implementations of the repositories,
  the Compose UI and `PluginStateFlow`, a project service exposing
  `StateFlow<State>` from `StateProvider`.

`ExternalTools` are implemented in the plugin module using the IntelliJ
API while `InternalTools` live in the core module for plugin interactions like
switching roles. `ToolsInfoDecorator` combines them and routes file responses through a
permission manager that checks absolute paths against a whitelist (project root by default)
and a blacklist of sensitive files before exposing file contents to the model.
File permissions can also be adjusted by adding a `sona.json` file under `.sona` in the project root with
`permissions.files.whitelist` and `blacklist` arrays of regex patterns.
`sona.json` may additionally define an `mcpServers` object keyed by server name with Model
Context Protocol server configurations including `enabled`, `command`, `args`, environment
variables, transport, URL, working directory, request headers and an optional
`disabledTools` array listing tool names to ignore. Supported transports are `stdio` and
`http` and each server runs in its own coroutine so failures are isolated. Tools exposed by
these servers require the same user permission prompts as local tools. Server enablement and
disabled tools are stored in `sona.json`; only servers marked as enabled reconnect
automatically on restart.
The plugin preconfigures two servers: `@jetbrains/mcp-proxy` and `memory`. The
`memory` server runs `@modelcontextprotocol/server-memory` via `npx` and stores
its data in `.sona/sona_memory.json` inside the project root.
When updating the configuration, merge new values into `.sona/sona.json` to preserve any
user-provided fields instead of overwriting the file.
`ChatController` receives a `Tools` decorator from `StateProvider` via its injected `ChatAgentFactory`.

Whenever you extend the logic make sure the flow of state remains unidirectional
and that the core module stays free from IntelliJ SDK imports.
