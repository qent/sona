package io.qent.sona.core.state.interactors

import io.qent.sona.core.chat.Chat
import io.qent.sona.core.chat.ChatRepository
import io.qent.sona.core.chat.ChatSummary
import kotlinx.coroutines.flow.Flow

interface ChatSession : Flow<Chat> {
    suspend fun loadChat(id: String)
    suspend fun send(text: String)
    fun stop()
    suspend fun deleteFrom(idx: Int)
    fun toggleAutoApproveTools()
    suspend fun resolveToolPermission(allow: Boolean, always: Boolean)
}

class ChatStateInteractor(
    private val chatFlow: ChatSession,
    private val chatRepository: ChatRepository,
) {
    val chat: Flow<Chat> = chatFlow

    suspend fun newChat() {
        val lastChat = chatRepository.listChats().firstOrNull()?.id ?: run {
            chatFlow.loadChat(chatRepository.createChat())
            return
        }
        if (chatRepository.loadMessages(lastChat).isEmpty()) {
            chatRepository.deleteChat(lastChat)
        }
        chatFlow.loadChat(chatRepository.createChat())
    }

    suspend fun openChat(id: String) = chatFlow.loadChat(id)

    suspend fun listChats(): List<ChatSummary> = chatRepository.listChats()

    suspend fun deleteChat(id: String) = chatRepository.deleteChat(id)

    suspend fun send(text: String) = chatFlow.send(text)

    fun stop() = chatFlow.stop()

    suspend fun deleteFrom(idx: Int) = chatFlow.deleteFrom(idx)

    fun toggleAutoApproveTools() = chatFlow.toggleAutoApproveTools()

    suspend fun resolveToolPermission(allow: Boolean, always: Boolean) =
        chatFlow.resolveToolPermission(allow, always)
}

