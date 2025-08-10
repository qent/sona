package io.qent.sona.core.chat

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.memory.ChatMemory
import kotlinx.coroutines.runBlocking

/**
 * ChatMemory backed by [ChatRepository]. It loads existing messages
 * synchronously and stores new ones only in memory. ChatController persists
 * messages explicitly so this memory primarily serves the AI service
 * during a request.
 */
class ChatRepositoryChatMemoryStore(
    private val chatRepository: ChatRepository,
    private val chatId: String,
) : ChatMemory {

    private val messages: MutableList<ChatMessage> = runBlocking {
        chatRepository.loadMessages(chatId).map { it.message }.toMutableList()
    }

    override fun id(): Any = chatId

    override fun add(message: ChatMessage) {
        messages += message
    }

    override fun messages(): MutableList<ChatMessage> = messages

    override fun clear() {
        messages.clear()
    }
}