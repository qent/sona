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
        inputTokens: Int = 0,
        outputTokens: Int = 0,
    )

    /** Load all messages for a chat. */
    suspend fun loadMessages(chatId: String): List<ChatRepositoryMessage>

    /** List all chats with their first message snippet. */
    suspend fun listChats(): List<ChatSummary>

    /** Delete chat with all messages. */
    suspend fun deleteChat(chatId: String)
}

data class ChatRepositoryMessage(
    val chatId: String,
    val message: ChatMessage,
    val model: String,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
)

data class ChatSummary(
    val id: String,
    val firstMessage: String,
    val createdAt: Long,
)
