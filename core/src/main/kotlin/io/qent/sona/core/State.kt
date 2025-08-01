package io.qent.sona.core

import dev.langchain4j.data.message.ChatMessage

sealed class State {
    abstract val onNewChat: () -> Unit
    abstract val onOpenHistory: () -> Unit
    abstract val onOpenRoles: () -> Unit

    data class ChatState(
        val messages: List<ChatMessage> = emptyList(),
        val outputTokens: Int = 0,
        val inputTokens: Int = 0,
        val isSending: Boolean = false,
        val onSendMessage: (String) -> Unit = {},
        override val onNewChat: () -> Unit = {},
        override val onOpenHistory: () -> Unit = {},
        override val onOpenRoles: () -> Unit = {},
    ) : State()

    data class ChatListState(
        val chats: List<ChatSummary> = emptyList(),
        val onOpenChat: (String) -> Unit = {},
        val onDeleteChat: (String) -> Unit = {},
        override val onNewChat: () -> Unit = {},
        override val onOpenRoles: () -> Unit = {},
    ) : State() {
        override val onOpenHistory = { }
    }

    data class RolesState(
        val text: String = "",
        val onSave: (String) -> Unit = {},
        override val onNewChat: () -> Unit = {},
    ) : State() {
        override val onOpenHistory = { }
        override val onOpenRoles = { }
    }
}
