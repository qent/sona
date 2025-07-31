package com.github.d0lfin.sona.logic

import dev.langchain4j.data.message.ChatMessage
import com.github.d0lfin.sona.logic.ChatSettings

data class PluginState(
    val messages: List<ChatMessage> = emptyList(),
    val outgoingTokens: Int = 0,
    val incomingTokens: Int = 0,
    val isSending: Boolean = false,
    val onSendMessage: (String) -> Unit = {},
    val onSaveSettings: (ChatSettings) -> Unit = {}
)
