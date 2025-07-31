package io.qent.sona.core

import dev.langchain4j.data.message.ChatMessage

sealed class State {
    abstract val onNewChat: () -> Unit
    abstract val onOpenHistory: () -> Unit

    data class ChatState(
        val messages: List<ChatMessage> = emptyList(),
        val outgoingTokens: Int = 0,
        val incomingTokens: Int = 0,
        val isSending: Boolean = false,
        val onSendMessage: (String) -> Unit = {},
        override val onNewChat: () -> Unit = {},
        override val onOpenHistory: () -> Unit = {},
    ) : State()

    data class ChatListState(
        val chats: List<ChatSummary> = emptyList(),
        val onOpenChat: (String) -> Unit = {},
        val onDeleteChat: (String) -> Unit = {},
        override val onNewChat: () -> Unit = {},
    ) : State() {
        override val onOpenHistory = { }
    }
}
