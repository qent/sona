package io.qent.sona.core

import dev.langchain4j.data.message.ChatMessage

data class State(
    val messages: List<ChatMessage> = emptyList(),
    val outgoingTokens: Int = 0,
    val incomingTokens: Int = 0,
    val isSending: Boolean = false,
    val onSendMessage: (String) -> Unit = {},
)
