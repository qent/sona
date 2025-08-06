package io.qent.sona.core

import dev.langchain4j.data.message.ChatMessage

/**
 * Repository for storing user chats and messages.
 */
interface ChatRepository {
    /** Create a new chat and return its id. */
    suspend fun createChat(): String

    /**
     * Append a message to a chat. The [model] name and token usage
     * are optional and normally provided for AI messages.
     */
    suspend fun addMessage(
        chatId: String,
        message: ChatMessage,
        model: String,
        tokenUsage: TokenUsageInfo = TokenUsageInfo(),
    )

    /** Load all messages for a chat. */
    suspend fun loadMessages(chatId: String): List<ChatRepositoryMessage>

    /** Load total token usage for a chat. */
    suspend fun loadTokenUsage(chatId: String): TokenUsageInfo

    /** Check if a [toolName] is allowed to execute in the chat. */
    suspend fun isToolAllowed(chatId: String, toolName: String): Boolean

    /** Persist that a [toolName] is allowed for this chat. */
    suspend fun addAllowedTool(chatId: String, toolName: String)

    /** List all chats with their first message snippet. */
    suspend fun listChats(): List<ChatSummary>

    /** Delete chat with all messages. */
    suspend fun deleteChat(chatId: String)

    /** Delete all messages in [chatId] starting from [index]. */
    suspend fun deleteMessagesFrom(chatId: String, index: Int)
}

data class ChatRepositoryMessage(
    val chatId: String,
    val message: ChatMessage,
    val model: String,
    val tokenUsage: TokenUsageInfo = TokenUsageInfo(),
)

data class ChatSummary(
    val id: String,
    val firstMessage: String,
    val messages: Int,
    val createdAt: Long,
)
