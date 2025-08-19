package io.qent.sona.core.chat

import io.qent.sona.core.model.TokenUsageInfo

data class Chat(
    val chatId: String,
    val tokenUsage: TokenUsageInfo,
    val messages: List<ChatRepositoryMessage> = emptyList(),
    val requestInProgress: Boolean = false,
    val isStreaming: Boolean = false,
    val toolRequest: String? = null,
    val pendingPatch: String? = null,
    val autoApproveTools: Boolean = false,
)