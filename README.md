# sona

Chat with various language models directly from a side panel in your IDE. The chat
history is persisted in IDE storage so it survives restarts. Connection details are
managed as presets where you choose a provider, model, API endpoint and token. Responses stream
gradually so you can watch them being generated in real time and stop the stream at any moment
to keep the partial reply. Under the hood the core relies on langchain4j `AiService` and
`TokenStream` to deliver partial responses and tool execution events via streaming callbacks.

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
chat history. Every request is also prefixed with a system message summarizing
the current environment (OS, IDE, Java, Python, Node.js versions, file extension
statistics and build systems).

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

The chat header shows token usage together with the estimated cost for the last
message and for the entire conversation. It also displays how much of the
model's context window is currently filled based on the active preset.

The settings screen contains only a single option: **Ignore HTTPS errors**. Enable
it to trust all HTTPS certificates when connecting to custom endpoints.

## Model pricing

Token costs are embedded for each supported model so conversations can display
their estimated price. The rates below are in USD per million tokens and are
based on the official pricing pages.

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

The available tools let the model read the focused file, read any file by absolute path, and switch the active role between Architect and Code. File access is guarded by a permission system with a whitelist (project root by default) and a blacklist blocking sensitive files such as `.env`. Custom lists can be supplied by creating a `sona.json` file in the project root:

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

The same configuration file can also include an `mcpServers` array specifying Model Context Protocol
servers. Each entry supports `name`, `command`, `args`, `env`, `transport`, `url`, `cwd` and `headers` fields.
When `command` is `npx`, Sona resolves the absolute path to the `npx` executable by checking typical
installation locations for the current operating system before launching the server. Currently
`transport` may be `stdio` or `http`. Every server runs in its own coroutine so a failure does
not affect the plugin. Tools provided by MCP servers require the same user confirmation as local tools.

The tool window includes a **Servers** action listing all configured MCP servers. Each server is shown as a card
with a coloured status indicator – grey for disabled, red when a connection fails, yellow while connecting and
green once connected and exposing tools. Clicking a card toggles the server on or off. A refresh button above
the list reloads `sona.json` and reconnects previously enabled servers. Server enablement is persisted so only
servers that were on previously start automatically after restarting the IDE.

```json
{
  "mcpServers": [
    { "name": "calc", "command": "calc-mcp", "transport": "stdio" },
    { "name": "weather", "url": "https://example.com/mcp", "transport": "http" }
  ]
}
```

## Copying and deleting messages

When hovering a message, copy and delete icons appear beneath its bottom‑right corner.
The clipboard button copies the entire message text while the trash icon removes
that message and all following messages from both the chat view and persistent
history.

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
entry point is `ChatFlow`, a `Flow<Chat>` backed by a `MutableStateFlow`.
It orchestrates message exchange with the language model and exposes the
current conversation state:

```
data class Chat(
    val chatId: String,
    val tokenUsage: TokenUsageInfo,
    val messages: List<ChatRepositoryMessage> = emptyList(),
    val requestInProgress: Boolean = false,
    val toolRequest: String? = null
)
```

`StateProvider` consumes `ChatFlow` and maps it to a higher level
`State` model used by the UI. Both `ChatFlow` and `StateProvider`
operate purely on Kotlin flows without any IntelliJ types.

`tokenUsage` represents the cumulative input, output and cached tokens
spent for all AI responses in the chat. Each AI message also
stores its own token usage including cached tokens while user messages
always record zeros. Providers that do not expose cache statistics
simply report zero cached tokens. Removing messages does not affect the
total usage stored for the chat.

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
