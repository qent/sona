package io.qent.sona.core.state

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.chat.Chat
import io.qent.sona.core.chat.ChatRepositoryMessage
import io.qent.sona.core.chat.ChatSummary
import io.qent.sona.core.model.TokenUsageInfo
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.mcp.McpServerStatus
import kotlinx.coroutines.flow.StateFlow

class StateFactory {
    fun createChatState(
        chat: Chat,
        roles: Roles,
        presets: Presets,
        onSelectRole: (Int) -> Unit,
        onSelectPreset: (Int) -> Unit,
        onSendMessage: (String) -> Unit,
        onStop: () -> Unit,
        onDeleteFrom: (Int) -> Unit,
        onToggleAutoApprove: () -> Unit,
        onAllowTool: () -> Unit,
        onAlwaysAllowTool: () -> Unit,
        onDenyTool: () -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenRoles: () -> Unit,
        onOpenPresets: () -> Unit,
        onOpenServers: () -> Unit,
    ): State.ChatState {
        val lastAi = chat.messages.lastOrNull { it.message is AiMessage }
        val lastUsage = lastAi?.tokenUsage ?: TokenUsageInfo()
        return State.ChatState(
            messages = chat.messages.mapNotNull { it.toUiMessage() },
            totalTokenUsage = chat.tokenUsage,
            lastTokenUsage = lastUsage,
            isSending = chat.requestInProgress,
            roles = roles.roles.map { it.name },
            activeRole = roles.active,
            onSelectRole = onSelectRole,
            presets = presets,
            onSelectPreset = onSelectPreset,
            onSendMessage = onSendMessage,
            onStop = onStop,
            onDeleteFrom = onDeleteFrom,
            toolRequest = chat.toolRequest != null,
            autoApproveTools = chat.autoApproveTools,
            onToggleAutoApprove = onToggleAutoApprove,
            onAllowTool = onAllowTool,
            onAlwaysAllowTool = onAlwaysAllowTool,
            onDenyTool = onDenyTool,
            onNewChat = onNewChat,
            onOpenHistory = onOpenHistory,
            onOpenRoles = onOpenRoles,
            onOpenPresets = onOpenPresets,
            onOpenServers = onOpenServers,
        )
    }

    fun createChatListState(
        chats: List<ChatSummary>,
        onOpenChat: (String) -> Unit,
        onDeleteChat: (String) -> Unit,
        onNewChat: () -> Unit,
        onOpenRoles: () -> Unit,
        onOpenPresets: () -> Unit,
        onOpenServers: () -> Unit,
    ) = State.ChatListState(
        chats = chats.filter { it.messages != 0 }.map { it.toUi() },
        onOpenChat = onOpenChat,
        onDeleteChat = onDeleteChat,
        onNewChat = onNewChat,
        onOpenRoles = onOpenRoles,
        onOpenPresets = onOpenPresets,
        onOpenServers = onOpenServers,
    )

    fun createRolesState(
        roles: Roles,
        creatingRole: Boolean,
        text: String,
        onSelectRole: (Int) -> Unit,
        onStartCreateRole: () -> Unit,
        onAddRole: (String, String) -> Unit,
        onDeleteRole: () -> Unit,
        onSave: (String) -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenPresets: () -> Unit,
        onOpenServers: () -> Unit,
    ) = State.RolesState(
        roles = roles.roles.map { it.name },
        currentIndex = roles.active,
        creating = creatingRole,
        text = text,
        onSelectRole = onSelectRole,
        onStartCreateRole = onStartCreateRole,
        onAddRole = onAddRole,
        onDeleteRole = onDeleteRole,
        onSave = onSave,
        onNewChat = onNewChat,
        onOpenHistory = onOpenHistory,
        onOpenRoles = {},
        onOpenPresets = onOpenPresets,
        onOpenServers = onOpenServers,
    )

    fun createPresetsState(
        presets: Presets,
        creatingPreset: Boolean,
        preset: Preset,
        onSelectPreset: (Int) -> Unit,
        onStartCreatePreset: () -> Unit,
        onAddPreset: (Preset) -> Unit,
        onDeletePreset: () -> Unit,
        onSave: (Preset) -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenRoles: () -> Unit,
        onOpenServers: () -> Unit,
    ) = State.PresetsState(
        presets = presets.presets.map { it.name },
        currentIndex = presets.active,
        creating = creatingPreset,
        preset = preset,
        onSelectPreset = onSelectPreset,
        onStartCreatePreset = onStartCreatePreset,
        onAddPreset = onAddPreset,
        onDeletePreset = onDeletePreset,
        onSave = onSave,
        onNewChat = onNewChat,
        onOpenHistory = onOpenHistory,
        onOpenRoles = onOpenRoles,
        onOpenServers = onOpenServers,
    )

    fun createServersState(
        servers: StateFlow<List<McpServerStatus>>,
        onToggleServer: (String) -> Unit,
        onToggleTool: (String, String) -> Unit,
        onReload: () -> Unit,
        onEditConfig: () -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenRoles: () -> Unit,
        onOpenPresets: () -> Unit,
    ) = State.ServersState(
        servers = servers,
        onToggleServer = onToggleServer,
        onToggleTool = onToggleTool,
        onReload = onReload,
        onEditConfig = onEditConfig,
        onNewChat = onNewChat,
        onOpenHistory = onOpenHistory,
        onOpenRoles = onOpenRoles,
        onOpenPresets = onOpenPresets,
    )
}

private fun ChatRepositoryMessage.toUiMessage(): UiMessage? = when (val m = message) {
    is AiMessage -> UiMessage.Ai(m.text().orEmpty(), m.toolExecutionRequests())
    is UserMessage -> UiMessage.User(m.singleText().trim())
    is ToolExecutionResultMessage -> UiMessage.Tool(m.text())
    else -> null
}

private fun ChatSummary.toUi(): UiChatSummary =
    UiChatSummary(id, firstMessage, messages, createdAt)

