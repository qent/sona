package io.qent.sona.chat

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.qent.sona.core.ChatRepository
import io.qent.sona.core.ChatSummary
import io.qent.sona.core.StoredChatMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ChatMessageDeserializer
import dev.langchain4j.data.message.ChatMessageSerializer
import java.util.UUID

@Service
@State(name = "PluginChatRepository", storages = [Storage("chat_history.xml")])
class PluginChatRepository : ChatRepository, PersistentStateComponent<PluginChatRepository.ChatsState> {

    data class StoredMessage(
        var json: String = "",
        var model: String? = null,
        var inputTokens: Int = 0,
        var outputTokens: Int = 0,
        var timestamp: Long = 0L,
    )

    data class StoredChat(
        var id: String = UUID.randomUUID().toString(),
        var createdAt: Long = System.currentTimeMillis(),
        var messages: MutableList<StoredMessage> = mutableListOf(),
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

    private fun StoredMessage.toStoredChatMessage(): StoredChatMessage {
        val msg = ChatMessageDeserializer.messageFromJson(json)
        return StoredChatMessage(
            message = msg,
            model = model,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
        )
    }

    override suspend fun createChat(): String {
        val chat = StoredChat()
        state.chats.add(chat)
        return chat.id
    }

    override suspend fun addMessage(chatId: String, message: ChatMessage, model: String?, inputTokens: Int, outputTokens: Int) {
        val stored = StoredMessage(
            json = ChatMessageSerializer.messageToJson(message),
            model = model,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            timestamp = System.currentTimeMillis(),
        )
        findChat(chatId).messages.add(stored)
    }

    override suspend fun loadMessages(chatId: String): List<StoredChatMessage> {
        return findChat(chatId).messages.map { it.toStoredChatMessage() }
    }

    override suspend fun listChats(): List<ChatSummary> {
        return state.chats.sortedBy { it.createdAt }.map { chat ->
            val firstMsg = chat.messages.firstOrNull()?.let { ChatMessageDeserializer.messageFromJson(it.json) }
            val first = firstMsg?.toString() ?: ""
            ChatSummary(chat.id, first.take(100), chat.createdAt)
        }
    }

    override suspend fun deleteChat(chatId: String) {
        state.chats.removeIf { it.id == chatId }
    }
}
