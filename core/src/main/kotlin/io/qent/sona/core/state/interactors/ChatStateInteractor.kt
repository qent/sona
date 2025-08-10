package io.qent.sona.core.state.interactors

import io.qent.sona.core.chat.Chat
import io.qent.sona.core.chat.ChatRepository
import io.qent.sona.core.chat.ChatSession
import io.qent.sona.core.chat.ChatStateFlow
import io.qent.sona.core.chat.ChatSummary
import kotlinx.coroutines.flow.Flow

class ChatStateInteractor(
    private val chatSession: ChatSession,
    private val chatRepository: ChatRepository,
    private val chatStateFlow: ChatStateFlow,
) {
    val chat: Flow<Chat> = chatStateFlow

    suspend fun newChat() {
        val lastChat = chatRepository.listChats().firstOrNull()?.id ?: run {
            chatStateFlow.loadChat(chatRepository.createChat())
            return
        }
        if (chatRepository.loadMessages(lastChat).isEmpty()) {
            chatRepository.deleteChat(lastChat)
        }
        chatStateFlow.loadChat(chatRepository.createChat())
    }

    suspend fun openChat(id: String) = chatStateFlow.loadChat(id)

    suspend fun listChats(): List<ChatSummary> = chatRepository.listChats()

    suspend fun deleteChat(id: String) = chatRepository.deleteChat(id)

    suspend fun send(text: String) = chatSession.send(text)

    fun stop() = chatSession.stop()

    suspend fun deleteFrom(idx: Int) = chatSession.deleteFrom(idx)

    fun toggleAutoApproveTools() = chatSession.toggleAutoApproveTools()
}

