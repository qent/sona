package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ChatMessageDeserializer
import dev.langchain4j.data.message.ChatMessageSerializer
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.ChatRepository
import io.qent.sona.core.ChatRepositoryMessage
import io.qent.sona.core.ChatSummary
import java.util.*

@Service
@State(name = "PluginChatRepository", storages = [Storage("chat_history.xml")])
class PluginChatRepository : ChatRepository, PersistentStateComponent<PluginChatRepository.ChatsState> {

    data class StoredMessage(
        var json: String = "",
        var model: String = "",
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

    private fun StoredMessage.toStoredChatMessage(chatId: String): ChatRepositoryMessage {
        val msg = ChatMessageDeserializer.messageFromJson(json)
        return ChatRepositoryMessage(
            chatId = chatId,
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

    override suspend fun addMessage(
        chatId: String,
        message: ChatMessage,
        model: String,
        inputTokens: Int,
        outputTokens: Int
    ) {
        val stored = StoredMessage(
            json = ChatMessageSerializer.messageToJson(message),
            model = model,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            timestamp = System.currentTimeMillis(),
        )
        findChat(chatId).messages.add(stored)
    }

    override suspend fun loadMessages(chatId: String): List<ChatRepositoryMessage> {
        return findChat(chatId).messages.map { it.toStoredChatMessage(chatId) }
    }

    override suspend fun listChats(): List<ChatSummary> {
        return state.chats.sortedByDescending { it.createdAt }.map { chat ->
            val firstMsg = chat.messages.asSequence().map {
                ChatMessageDeserializer.messageFromJson(it.json)
            }.firstOrNull { it is UserMessage }
            val first = (firstMsg as? UserMessage)?.singleText() ?: ""
            ChatSummary(chat.id, first.take(100), chat.createdAt)
        }
    }

    override suspend fun deleteChat(chatId: String) {
        state.chats.removeIf { it.id == chatId }
    }
}
