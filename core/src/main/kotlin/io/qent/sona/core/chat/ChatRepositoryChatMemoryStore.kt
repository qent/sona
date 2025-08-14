package io.qent.sona.core.chat

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
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
    private val connectionErrorText: String,
) : ChatMemory {

    private val messages: MutableList<ChatMessage> = runBlocking {
        val repoMessages = chatRepository.loadMessages(chatId).toMutableList()
        fixDanglingToolRequests(repoMessages).map { it.message }.toMutableList()
    }

    override fun id(): Any = chatId

    override fun add(message: ChatMessage) {
        messages += message
    }

    override fun messages(): MutableList<ChatMessage> = messages

    override fun clear() {
        messages.clear()
    }

    private suspend fun fixDanglingToolRequests(messages: MutableList<ChatRepositoryMessage>): List<ChatRepositoryMessage> {
        val fixed = mutableListOf<ChatRepositoryMessage>()
        val pending = mutableListOf<ToolExecutionRequest>()
        for (msg in messages) {
            when (val m = msg.message) {
                is AiMessage -> {
                    fixed += msg
                    pending += m.toolExecutionRequests()
                }
                is ToolExecutionResultMessage -> {
                    val removed = pending.removeAll { it.id() == m.id() }
                    if (removed) fixed += msg
                }
                else -> {
                    if (pending.isNotEmpty()) {
                        pending.forEach { req ->
                            val result = ToolExecutionResultMessage(req.id(), req.name(), connectionErrorText)
                            fixed += ChatRepositoryMessage(chatId, result, "")
                        }
                        pending.clear()
                    }
                    fixed += msg
                }
            }
        }
        if (pending.isNotEmpty()) {
            pending.forEach { req ->
                val result = ToolExecutionResultMessage(req.id(), req.name(), connectionErrorText)
                fixed += ChatRepositoryMessage(chatId, result, "")
            }
        }
        if (fixed.size != messages.size) {
            chatRepository.deleteMessagesFrom(chatId, 0)
            for (m in fixed) {
                chatRepository.addMessage(chatId, m.message, m.model, m.tokenUsage)
            }
        }
        return fixed
    }
}