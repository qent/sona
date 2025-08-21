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
import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.mcp.McpServerStatus

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
        onOpenUserPrompt: () -> Unit,
    ): State.ChatState {
        val lastAi = chat.messages.lastOrNull { it.message is AiMessage }
        val lastUsage = lastAi?.tokenUsage ?: TokenUsageInfo()

        val uiMessages = mutableListOf<UiMessage>()


        chat.messages.forEachIndexed { index, message ->
            val m = message.message
            val ts = if (message.timestamp != 0L) message.timestamp else index.toLong()

            val lastUiMessage = uiMessages.lastOrNull()
            val lastUiMessageIndex = uiMessages.size - 1

            when (m) {
                is AiMessage -> {
                    uiMessages.add(UiMessage.Ai(m.text().orEmpty(), ts, m.toolExecutionRequests()))
                }
                is UserMessage -> {
                    uiMessages.add(UiMessage.User(m.singleText().trim(), ts))
                }
                is ToolExecutionResultMessage -> {
                    when (lastUiMessage) {
                        is UiMessage.AiMessageWithTools -> {
                            uiMessages[lastUiMessageIndex] = lastUiMessage.copy(
                                toolResponse = lastUiMessage.toolResponse.toMutableList().apply {
                                    add(m.text())
                                }
                            )
                        }
                        is UiMessage.Ai -> {
                            uiMessages[lastUiMessageIndex] = UiMessage.AiMessageWithTools(
                                lastUiMessage.text,
                                lastUiMessage.timestamp,
                                lastUiMessage.toolRequests,
                                listOf(m.text())
                            )
                        }
                        else -> {
                            uiMessages += UiMessage.AiMessageWithTools(
                                "",
                                ts,
                                emptyList(),
                                listOf(m.text())
                            )
                        }
                    }
                }
            }
        }

        return State.ChatState(
            messages = uiMessages,
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
            toolRequest = chat.toolRequest,
            pendingPatch = chat.pendingPatch,
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
            onOpenUserPrompt = onOpenUserPrompt,
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
        onOpenUserPrompt: () -> Unit,
    ) = State.ChatListState(
        chats = chats.filter { it.messages != 0 }.map { it.toUi() },
        onOpenChat = onOpenChat,
        onDeleteChat = onDeleteChat,
        onNewChat = onNewChat,
        onOpenRoles = onOpenRoles,
        onOpenPresets = onOpenPresets,
        onOpenServers = onOpenServers,
        onOpenUserPrompt = onOpenUserPrompt,
    )

    fun createRolesListState(
        roles: Roles,
        onSelectRole: (Int) -> Unit,
        onAddRole: () -> Unit,
        onEditRole: (Int) -> Unit,
        onDeleteRole: (Int) -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenPresets: () -> Unit,
        onOpenServers: () -> Unit,
        onOpenUserPrompt: () -> Unit,
    ) = State.RolesListState(
        roles = roles.roles,
        currentIndex = roles.active,
        onSelectRole = onSelectRole,
        onAddRole = onAddRole,
        onEditRole = onEditRole,
        onDeleteRole = onDeleteRole,
        onNewChat = onNewChat,
        onOpenHistory = onOpenHistory,
        onOpenPresets = onOpenPresets,
        onOpenServers = onOpenServers,
        onOpenUserPrompt = onOpenUserPrompt,
    )

    fun createEditRoleState(
        role: Role,
        onSave: (Role) -> Unit,
        onCancel: () -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenPresets: () -> Unit,
        onOpenServers: () -> Unit,
        onOpenUserPrompt: () -> Unit,
    ) = State.EditRoleState(
        role = role,
        onSave = onSave,
        onCancel = onCancel,
        onNewChat = onNewChat,
        onOpenHistory = onOpenHistory,
        onOpenPresets = onOpenPresets,
        onOpenServers = onOpenServers,
        onOpenUserPrompt = onOpenUserPrompt,
    )

    fun createPresetsListState(
        presets: Presets,
        onSelectPreset: (Int) -> Unit,
        onAddPreset: () -> Unit,
        onEditPreset: (Int) -> Unit,
        onDeletePreset: (Int) -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenRoles: () -> Unit,
        onOpenServers: () -> Unit,
        onOpenUserPrompt: () -> Unit,
    ) = State.PresetsListState(
        presets = presets.presets,
        currentIndex = presets.active,
        onSelectPreset = onSelectPreset,
        onAddPreset = onAddPreset,
        onEditPreset = onEditPreset,
        onDeletePreset = onDeletePreset,
        onNewChat = onNewChat,
        onOpenHistory = onOpenHistory,
        onOpenRoles = onOpenRoles,
        onOpenServers = onOpenServers,
        onOpenUserPrompt = onOpenUserPrompt,
    )

    fun createEditPresetState(
        preset: Preset,
        onSave: (Preset) -> Unit,
        onCancel: () -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenRoles: () -> Unit,
        onOpenServers: () -> Unit,
        onOpenUserPrompt: () -> Unit,
    ) = State.EditPresetState(
        preset = preset,
        onSave = onSave,
        onCancel = onCancel,
        onNewChat = onNewChat,
        onOpenHistory = onOpenHistory,
        onOpenRoles = onOpenRoles,
        onOpenServers = onOpenServers,
        onOpenUserPrompt = onOpenUserPrompt,
    )

    fun createServersState(
        servers: List<McpServerStatus>,
        onToggleServer: (String) -> Unit,
        onToggleTool: (String, String) -> Unit,
        onReload: () -> Unit,
        onEditConfig: () -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenRoles: () -> Unit,
        onOpenPresets: () -> Unit,
        onOpenUserPrompt: () -> Unit,
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
        onOpenUserPrompt = onOpenUserPrompt,
    )

    fun createUserPromptState(
        prompt: String,
        onSave: (String) -> Unit,
        onNewChat: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenRoles: () -> Unit,
        onOpenPresets: () -> Unit,
        onOpenServers: () -> Unit,
    ) = State.UserPromptState(
        prompt = prompt,
        onSave = onSave,
        onNewChat = onNewChat,
        onOpenHistory = onOpenHistory,
        onOpenRoles = onOpenRoles,
        onOpenPresets = onOpenPresets,
        onOpenServers = onOpenServers,
    )
}

private fun ChatSummary.toUi(): UiChatSummary =
    UiChatSummary(id, firstMessage, messages, createdAt)

