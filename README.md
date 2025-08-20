[![Build](https://github.com/qent/sona/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/qent/sona/actions/workflows/build.yml)

# sona

Chat with various language models directly from a side panel in your IDE. The chat
history is persisted in IDE storage so it survives restarts. Connection details are
managed as presets where you choose a provider, model, API endpoint and token. Responses stream
gradually so you can watch them being generated in real time and stop the stream at any moment
to keep the partial reply. Stopping cancels the active LLM request. The interface is available in English, Russian, Chinese, German, and French. Under the hood the core relies on langchain4j `AiService` and
`TokenStream` to deliver partial responses and tool execution events via streaming callbacks.

<!-- Plugin description -->
Chat with Anthropic, OpenAI, Deepseek or Gemini models right inside your IDE. The chat history is stored persistently so
you can resume conversations after restarting the IDE.
<!-- Plugin description end -->

## Installation

Use the IDE plugin manager or download the plugin from releases.

## Testing

Pull requests trigger a GitHub Actions workflow that runs the test suite.

## Nightly builds

A scheduled GitHub Actions workflow runs every night and builds a plugin archive if there were changes in the `main`
branch during the previous day. The resulting archive is uploaded as a workflow artifact available from the run page.

## Roles

The plugin supports multiple system roles. Open the roles screen from the tool
window actions to switch between roles or create new ones. Each role provides a
short description for tools and a detailed system prompt. Roles can be added,
selected and removed, but the default Architect and Code roles cannot be deleted.
The active role can also be changed directly from the chat via the selector
under the message input. The text of the active role is sent as a system message
with every request but is not stored in the chat history. Every request is also
prefixed with a system message summarizing
the current environment (OS, IDE, Java, Python, Node.js versions, project root path, file extension
statistics and build systems). The plugin also reads any `.md` files in `src/main/resources/prompts`.
Projects can provide their own prompts under `.sona/prompts` (for all roles) and `.sona/prompts/{role}`
matching the active role name in lowercase; these files are appended as additional system messages.
A user-specific system prompt can be edited from the toolbar and is included with every request when set.

Models can also switch roles themselves using a tool that selects a role by name.

When starting a new conversation the message field shows a placeholder tailored
to the active role. The Architect role suggests planning and design, the Code
role suggests implementation, and other roles display a generic "Describe your
task..." prompt. Once messages are present, the placeholder becomes
"Type a message...".

## Presets

LLM credentials are stored as presets. API tokens are kept in the IDE password
store rather than `presets.xml`. Each preset defines the provider, model,
API endpoint and token. Manage presets from the Presets screen opened via the
tool window actions. At least one preset must exist for the chat to function.
Supported providers and their models are listed in `core/src/main/resources/providers.json`
so new options can be added without touching the code. A `Custom OpenAI` provider is
available for manual model entry and is defined directly in code.

The chat header shows token usage together with the estimated cost for the last
message and for the entire conversation. It also displays how much of the
model's context window is currently filled based on the active preset.

The settings screen is split into two sections. **Plugin Settings** contains an **Answer in English** toggle that forces the
model to respond in English, the original **Ignore HTTPS errors** option, an **Enable plugin logging** flag that writes
debug messages to the IDE log, a **Use search agent** checkbox that delegates project search to a dedicated agent,
and a field for **LLM API retries** that controls how many times failed requests are
retried with exponential backoff. The new **Anthropic Settings** section adds
checkboxes to cache system prompts and tool descriptions when sending requests
to Anthropic models.

## Model pricing

Token costs are loaded from `providers.json` so conversations can display their
estimated price. The `Custom OpenAI` provider always reports zero cost. The rates
below are in USD per million tokens and are based on the official pricing pages.

| Provider | Model | Input | Output | Source |
| --- | --- | --- | --- | --- |
| Anthropic | Claude Sonnet 4 | $3.00 | $15.00 | [link](https://www.anthropic.com/pricing) |
| Anthropic | Claude 3.7 Sonnet | $3.00 | $15.00 | [link](https://www.anthropic.com/pricing) |
| Anthropic | Claude 3.5 Haiku | $0.80 | $4.00 | [link](https://www.anthropic.com/pricing) |
| OpenAI | o3 | $2.00 | $8.00 | [link](https://platform.openai.com/docs/pricing) |
| OpenAI | GPT-4.1 | $2.00 | $8.00 | [link](https://platform.openai.com/docs/pricing) |
| OpenAI | GPT-4.1 Mini | $0.40 | $1.60 | [link](https://platform.openai.com/docs/pricing) |
| OpenAI | GPT-4o | $2.50 | $10.00 | [link](https://platform.openai.com/docs/pricing) |
| Deepseek | deepseek-chat | $0.27 | $1.10 | [link](https://www.deepseek.com/pricing) |
| Deepseek | deepseek-reasoner | $0.55 | $2.19 | [link](https://api-docs.deepseek.com/quick_start/pricing) |
| Gemini | Gemini 2.5 Pro | $1.25 | $10.00 | [link](https://ai.google.dev/gemini-api/docs/pricing) |
| Gemini | Gemini 2.5 Flash | $0.30 | $2.50 | [link](https://ai.google.dev/gemini-api/docs/pricing) |

## Tools

When the model requests to run a tool, the plugin asks for permission before
executing it. You can allow the action once or choose **Always in this chat** to
skip future confirmations for the same tool within the current conversation.

A 🤘 button next to the send action toggles automatic approval of all tool
requests in the current chat. When enabled, tools run without prompting until
you switch to a different chat.

Tool calls referenced by the model are hidden behind a gear icon in the top‑right corner of each AI message. Clicking the icon reveals the list of requested tools. When a tool starts running, the chat shows a dark terminal‑style bubble with animated dots that are replaced by the tool's output once it finishes.
If the model message contains only a tool request, the chat displays a note like "Sona is calling tool '<tool>'" instead of an empty response.

 The available tools let the model read the focused file, read any file by absolute path and line range, list directory contents, run terminal commands from the project root, read terminal output, apply unified diff patches through `applyPatch`, and switch the active role between Architect and Code. When **Use search agent** is enabled, a dedicated agent can also query the project using tools that search for files, classes and text patterns. Otherwise, the model can call `findFilesByNames`, `findClasses` and `findText` directly through IntelliJ APIs. The search agent runs synchronously and shows tool requests together with their responses in the chat while the final structured answer is not added. Directory listings append "/" to folder names and include the first-level contents of each directory. File access is guarded by a permission system with a whitelist (project root by default) and a blacklist blocking sensitive files such as `.env`. Custom lists can be supplied by creating a `sona.json` file inside `.sona` in the project root:

```
{
    "permissions": {
        "files": {
            "whitelist": [],
            "blacklist": []
        }
    }
}
```

Sona merges updates into this file to preserve any custom entries the user may have added.

The same configuration file can also include an `mcpServers` object keyed by server name specifying Model
Context Protocol servers. Each entry supports `enabled`, `command`, `args`, `env`, `transport`, `url`, `cwd`
and `headers` fields. When `command` is `npx`, Sona resolves the absolute path to the `npx` executable by
checking typical installation locations for the current operating system before launching the server.
Currently `transport` may be `stdio` or `http`. Every server runs in its own coroutine so a failure does
not affect the plugin. Tools provided by MCP servers require the same user confirmation as local tools.

Sona ships with two predefined servers: `@jetbrains/mcp-proxy` and `memory`. The
`memory` server uses `@modelcontextprotocol/server-memory` via `npx` and stores
its state in `.sona/sona_memory.json` at the project root. When this server is enabled,
its usage instructions from `prompts/memory_instructions.md` are appended to the
system messages sent with each request.

The tool window includes a **Servers** action listing all configured MCP servers. Each server is shown as a card
with a coloured status indicator – grey for disabled, red when a connection fails, yellow while connecting and
green once connected and exposing tools. Clicking a card toggles the server on or off. A refresh button above
the list reloads `.sona/sona.json` and reconnects previously enabled servers. A pinned **Редактировать конфигурацию**
button at the bottom opens `.sona/sona.json`, creating it with the current server configuration and file permission
lists when missing. Server enablement is stored in `.sona/sona.json` so only servers marked as enabled start
automatically after restarting the IDE. Connected servers expand to show their tools with a green indicator next
to each one. Clicking the indicator disables the tool, turning it grey and excluding it from future LLM requests.
Disabled tool names persist in `.sona/sona.json` under the server's `disabledTools` array.

```json
{
  "mcpServers": {
    "calc": {
      "enabled": true,
      "command": "calc-mcp",
      "transport": "stdio",
      "disabledTools": ["multiply"]
    },
    "weather": { "enabled": false, "url": "https://example.com/mcp", "transport": "http" }
  }
}
```

## Copying, editing, and deleting messages

When hovering a user message, copy, edit, and delete icons appear beneath its bottom‑right corner.
The clipboard button copies the message text while the pencil icon removes that message and all following messages,
then places the text into the input field for further editing.
The trash icon simply removes the message and subsequent history from both the chat view and persistent history.
AI and tool messages only expose the copy icon. Code blocks additionally show an **Apply patch** button that opens a diff view to review changes; the patch can then be applied through the chat using the `applyPatch` tool.

Chat messages are now selectable so you can highlight and copy any portion of
the text directly. Code blocks render using the IDE editor component, providing
full syntax highlighting.

## Architecture Overview

The project is split into two modules:

* `core` – contains all domain logic and is free from IntelliJ
  dependencies. It exposes a Flow‑based API used by the UI module.
* plugin module – implements the IntelliJ part and hosts the Compose UI.

### Core module

The `core` module defines a small MVI style state container. The main
entry point is a pair of `ChatController` and `ChatStateFlow`. The
`ChatStateFlow` is a `Flow<Chat>` that holds the current conversation,
while `ChatController` orchestrates message exchange with the language
model:

```
data class Chat(
    val chatId: String,
    val tokenUsage: TokenUsageInfo,
    val messages: List<ChatRepositoryMessage> = emptyList(),
    val requestInProgress: Boolean = false,
    val toolRequest: String? = null
)
```

`StateProvider` consumes `ChatStateFlow` and maps it to a higher level
`State` model used by the UI. Both `ChatController` and
`StateProvider` operate purely on Kotlin flows without any IntelliJ types.

`tokenUsage` represents the cumulative input, output and cached tokens
spent for the entire conversation. Every message stored in the chat
(system, user, tool and AI) records its own token usage. Providers that
do not expose cache statistics simply report zero cached tokens.
Removing messages does not affect the total usage stored for the chat.
Token counts are obtained via a pluggable `TokenCounter` interface that
computes tokens for individual messages using provider specific implementations
such as the official Anthropic token counting API or langchain4j estimators for
OpenAI and Gemini models.

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

* `ChatStateFlow` exposes conversation updates as `Flow<Chat>`.
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
