package io.qent.sona.core.state

import dev.langchain4j.agent.tool.ToolExecutionRequest
import kotlinx.coroutines.flow.StateFlow
import io.qent.sona.core.model.TokenUsageInfo
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.mcp.McpServerStatus

sealed interface UiMessage {
    val text: String

    data class User(override val text: String) : UiMessage
    data class Ai(override val text: String, val toolRequests: List<ToolExecutionRequest>) : UiMessage
    data class Tool(override val text: String) : UiMessage
}

data class UiChatSummary(
    val id: String,
    val firstMessage: String,
    val messages: Int,
    val createdAt: Long,
)

sealed class State {
    abstract val onNewChat: () -> Unit
    abstract val onOpenHistory: () -> Unit
    abstract val onOpenRoles: () -> Unit
    abstract val onOpenPresets: () -> Unit
    abstract val onOpenServers: () -> Unit

    data class ChatState(
        val messages: List<UiMessage>,
        val totalTokenUsage: TokenUsageInfo,
        val lastTokenUsage: TokenUsageInfo,
        val isSending: Boolean,
        val roles: List<String>,
        val activeRole: Int,
        val onSelectRole: (Int) -> Unit,
        val presets: Presets,
        val onSelectPreset: (Int) -> Unit,
        val onSendMessage: (String) -> Unit,
        val onStop: () -> Unit,
        val onDeleteFrom: (Int) -> Unit,
        val toolRequest: Boolean,
        val autoApproveTools: Boolean,
        val onToggleAutoApprove: () -> Unit,
        val onAllowTool: () -> Unit,
        val onAlwaysAllowTool: () -> Unit,
        val onDenyTool: () -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenHistory: () -> Unit,
        override val onOpenRoles: () -> Unit,
        override val onOpenPresets: () -> Unit,
        override val onOpenServers: () -> Unit,
    ) : State()

    data class ChatListState(
        val chats: List<UiChatSummary>,
        val onOpenChat: (String) -> Unit,
        val onDeleteChat: (String) -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenRoles: () -> Unit,
        override val onOpenPresets: () -> Unit,
        override val onOpenServers: () -> Unit,
    ) : State() {
        override val onOpenHistory = { }
    }

    data class RolesListState(
        val roles: List<io.qent.sona.core.roles.Role>,
        val currentIndex: Int,
        val onSelectRole: (Int) -> Unit,
        val onAddRole: () -> Unit,
        val onEditRole: (Int) -> Unit,
        val onDeleteRole: (Int) -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenHistory: () -> Unit,
        override val onOpenPresets: () -> Unit,
        override val onOpenServers: () -> Unit,
    ) : State() {
        override val onOpenRoles = { }
    }

    data class EditRoleState(
        val role: io.qent.sona.core.roles.Role,
        val onSave: (io.qent.sona.core.roles.Role) -> Unit,
        val onCancel: () -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenHistory: () -> Unit,
        override val onOpenPresets: () -> Unit,
        override val onOpenServers: () -> Unit,
    ) : State() {
        override val onOpenRoles = { }
    }

    data class PresetsListState(
        val presets: List<Preset>,
        val currentIndex: Int,
        val onSelectPreset: (Int) -> Unit,
        val onAddPreset: () -> Unit,
        val onEditPreset: (Int) -> Unit,
        val onDeletePreset: (Int) -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenHistory: () -> Unit,
        override val onOpenRoles: () -> Unit,
        override val onOpenServers: () -> Unit,
    ) : State() {
        override val onOpenPresets = { }
    }

    data class EditPresetState(
        val preset: Preset,
        val onSave: (Preset) -> Unit,
        val onCancel: () -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenHistory: () -> Unit,
        override val onOpenRoles: () -> Unit,
        override val onOpenServers: () -> Unit,
    ) : State() {
        override val onOpenPresets = { }
    }

    data class ServersState(
        val servers: StateFlow<List<McpServerStatus>>,
        val onToggleServer: (String) -> Unit,
        val onToggleTool: (String, String) -> Unit,
        val onReload: () -> Unit,
        val onEditConfig: () -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenHistory: () -> Unit,
        override val onOpenRoles: () -> Unit,
        override val onOpenPresets: () -> Unit,
    ) : State() {
        override val onOpenServers = { }
    }
}
