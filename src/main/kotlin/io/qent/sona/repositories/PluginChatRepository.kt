package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ChatMessageDeserializer
import dev.langchain4j.data.message.ChatMessageSerializer
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.chat.ChatRepository
import io.qent.sona.core.chat.ChatRepositoryMessage
import io.qent.sona.core.chat.ChatSummary
import io.qent.sona.core.model.TokenUsageInfo
import com.intellij.openapi.components.service
import java.util.*

@Service
@State(name = "PluginChatRepository", storages = [Storage("chat_history.xml")])
class PluginChatRepository : ChatRepository, PersistentStateComponent<PluginChatRepository.ChatsState> {

    data class StoredMessage(
        var json: String = "",
        var model: String = "",
        var inputTokens: Int = 0,
        var outputTokens: Int = 0,
        var cachedInputTokens: Int = 0,
        var cachedOutputTokens: Int = 0,
        var timestamp: Long = 0L,
    )

    data class StoredChat(
        var id: String = UUID.randomUUID().toString(),
        var createdAt: Long = System.currentTimeMillis(),
        var messages: MutableList<StoredMessage> = mutableListOf(),
        var allowedTools: MutableSet<String> = mutableSetOf(),
        var inputTokens: Int = 0,
        var outputTokens: Int = 0,
        var cacheReadInputTokens: Int = 0,
        var cacheCreationInputTokens: Int = 0,
    )

    data class ChatsState(
        var chats: MutableList<StoredChat> = mutableListOf(),
    )

    private var state = ChatsState()

    override fun getState() = state

    override fun loadState(state: ChatsState) {
        this.state = state
    }

    private fun findChat(id: String) = state.chats.first { it.id == id }

    private fun StoredMessage.toStoredChatMessage(chatId: String): ChatRepositoryMessage {
        val msg = ChatMessageDeserializer.messageFromJson(json)
        return ChatRepositoryMessage(
            chatId = chatId,
            message = msg,
            model = model,
            tokenUsage = TokenUsageInfo(
                outputTokens = outputTokens,
                inputTokens = inputTokens,
                cacheCreationInputTokens = cachedOutputTokens,
                cacheReadInputTokens = cachedInputTokens,
            ),
        )
    }

    override suspend fun createChat(): String {
        val chat = StoredChat()
        state.chats.add(chat)
        return chat.id
    }

    override suspend fun addMessage(
        chatId: String,
        message: ChatMessage,
        model: String,
        tokenUsage: TokenUsageInfo
    ) {
        val stored = StoredMessage(
            json = ChatMessageSerializer.messageToJson(message),
            model = model,
            inputTokens = tokenUsage.inputTokens,
            outputTokens = tokenUsage.outputTokens,
            cachedInputTokens = tokenUsage.cacheReadInputTokens,
            cachedOutputTokens = tokenUsage.cacheCreationInputTokens,
            timestamp = System.currentTimeMillis(),
        )
        val chat = findChat(chatId)
        chat.messages.add(stored)
        chat.inputTokens += tokenUsage.inputTokens
        chat.outputTokens += tokenUsage.outputTokens
        chat.cacheReadInputTokens += tokenUsage.cacheReadInputTokens
        chat.cacheCreationInputTokens += tokenUsage.cacheCreationInputTokens
    }

    override suspend fun loadMessages(chatId: String): List<ChatRepositoryMessage> {
        return findChat(chatId).messages.map { it.toStoredChatMessage(chatId) }
    }

    override suspend fun loadTokenUsage(chatId: String): TokenUsageInfo {
        val chat = findChat(chatId)
        return TokenUsageInfo(
            outputTokens = chat.outputTokens,
            inputTokens = chat.inputTokens,
            cacheCreationInputTokens = chat.cacheCreationInputTokens,
            cacheReadInputTokens = chat.cacheReadInputTokens,
        )
    }

    override suspend fun isToolAllowed(chatId: String, toolName: String): Boolean {
        return findChat(chatId).allowedTools.contains(toolName)
    }

    override suspend fun addAllowedTool(chatId: String, toolName: String) {
        findChat(chatId).allowedTools.add(toolName)
    }

    override suspend fun listChats(): List<ChatSummary> {
        return state.chats.sortedByDescending { it.createdAt }.map { chat ->
            val firstMsg = chat.messages.asSequence().map {
                ChatMessageDeserializer.messageFromJson(it.json)
            }.firstOrNull { it is UserMessage }
            val first = (firstMsg as? UserMessage)?.singleText() ?: ""
            ChatSummary(chat.id, first.take(100), chat.messages.size, chat.createdAt)
        }
    }

    override suspend fun deleteChat(chatId: String) {
        state.chats.removeIf { it.id == chatId }
    }

    override suspend fun deleteMessagesFrom(chatId: String, index: Int) {
        val chat = findChat(chatId)
        if (index < chat.messages.size) {
            chat.messages.subList(index, chat.messages.size).clear()
        }
    }
}
