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

    data class RolesState(
        val roles: List<String>,
        val currentIndex: Int,
        val creating: Boolean,
        val text: String,
        val onSelectRole: (Int) -> Unit,
        val onStartCreateRole: () -> Unit,
        val onAddRole: (String, String) -> Unit,
        val onDeleteRole: () -> Unit,
        val onSave: (String) -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenHistory: () -> Unit,
        override val onOpenRoles: () -> Unit,
        override val onOpenPresets: () -> Unit,
        override val onOpenServers: () -> Unit,
    ) : State()

    data class PresetsState(
        val presets: List<String>,
        val currentIndex: Int,
        val creating: Boolean,
        val preset: Preset,
        val onSelectPreset: (Int) -> Unit,
        val onStartCreatePreset: () -> Unit,
        val onAddPreset: (Preset) -> Unit,
        val onDeletePreset: () -> Unit,
        val onSave: (Preset) -> Unit,
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
        val onReload: () -> Unit,
        override val onNewChat: () -> Unit,
        override val onOpenHistory: () -> Unit,
        override val onOpenRoles: () -> Unit,
        override val onOpenPresets: () -> Unit,
    ) : State() {
        override val onOpenServers = { }
    }
}
