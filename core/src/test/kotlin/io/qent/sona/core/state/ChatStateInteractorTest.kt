package io.qent.sona.core.state

import dev.langchain4j.data.message.ChatMessage
import io.qent.sona.core.chat.ChatRepository
import io.qent.sona.core.chat.ChatRepositoryMessage
import io.qent.sona.core.chat.ChatSession
import io.qent.sona.core.chat.ChatStateFlow
import io.qent.sona.core.chat.ChatSummary
import io.qent.sona.core.model.TokenUsageInfo
import io.qent.sona.core.state.interactors.ChatStateInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

private class FakeChatRepository : ChatRepository {
    var chats = mutableListOf<ChatSummary>()
    var messages = mutableMapOf<String, MutableList<ChatRepositoryMessage>>()
    var counter = 0
    var deleted: String? = null
    var deletedFrom: Pair<String, Int>? = null
    override suspend fun createChat(): String {
        val id = (++counter).toString()
        chats.add(ChatSummary(id, "", 0, 0))
        messages[id] = mutableListOf()
        return id
    }
    override suspend fun addMessage(chatId: String, message: ChatMessage, model: String, tokenUsage: TokenUsageInfo) {}
    override suspend fun loadMessages(chatId: String): List<ChatRepositoryMessage> = messages[chatId] ?: emptyList()
    override suspend fun loadTokenUsage(chatId: String) = TokenUsageInfo()
    override suspend fun isToolAllowed(chatId: String, toolName: String) = false
    override suspend fun addAllowedTool(chatId: String, toolName: String) {}
    override suspend fun listChats(): List<ChatSummary> = chats
    override suspend fun deleteChat(chatId: String) { deleted = chatId; chats.removeIf { it.id == chatId }; messages.remove(chatId) }
    override suspend fun deleteMessagesFrom(chatId: String, index: Int) { deletedFrom = chatId to index }
}

private class FakeChatSession : ChatSession {
    var sent: String? = null
    var stopped = false
    var deletedFrom: Int? = null
    var toggled = false
    override suspend fun send(text: String) { sent = text }
    override fun stop() { stopped = true }
    override suspend fun deleteFrom(idx: Int) { deletedFrom = idx }
    override fun toggleAutoApproveTools() { toggled = true }
}

class ChatStateInteractorTest {
    @Test
    fun newChatCreatesWhenEmpty() = runBlocking {
        val repo = FakeChatRepository()
        val chatId = repo.createChat()
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        interactor.newChat()
        assertNotEquals(chatId, chatStateFlow.currentState.chatId)
    }

    @Test
    fun newChatCreatesWhenLastHasMessages() = runBlocking {
        val repo = FakeChatRepository()
        val existing = repo.createChat()
        repo.messages[existing]?.add(ChatRepositoryMessage(existing, dev.langchain4j.data.message.UserMessage.from("hi"), "m"))
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        interactor.newChat()
        assertNotEquals(existing, chatStateFlow.currentState.chatId)
    }

    @Test
    fun openChatDelegates() = runBlocking {
        val repo = FakeChatRepository()
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        interactor.openChat("123")
        assertEquals("123", chatStateFlow.currentState.chatId)
    }

    @Test
    fun listChatsReturnsRepositoryData() = runBlocking {
        val repo = FakeChatRepository()
        val chat1 = repo.createChat()
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        val chats = interactor.listChats()
        assertEquals(1, chats.size)
        assertEquals(chat1, chats.first().id)
    }

    @Test
    fun deleteChatDelegatesToRepository() = runBlocking {
        val repo = FakeChatRepository()
        val id = repo.createChat()
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        interactor.deleteChat(id)
        assertEquals(id, repo.deleted)
    }

    @Test
    fun sendDelegatesToFlow() = runBlocking {
        val repo = FakeChatRepository()
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        interactor.send("hello")
        assertEquals("hello", session.sent)
    }

    @Test
    fun stopDelegatesToFlow() = runBlocking {
        val repo = FakeChatRepository()
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        interactor.stop()
        assertEquals(true, session.stopped)
    }

    @Test
    fun deleteFromDelegatesToFlow() = runBlocking {
        val repo = FakeChatRepository()
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        interactor.deleteFrom(5)
        assertEquals(5, session.deletedFrom)
    }

    @Test
    fun toggleAutoApproveToolsDelegates() = runBlocking {
        val repo = FakeChatRepository()
        val chatStateFlow = ChatStateFlow(repo, CoroutineScope(Dispatchers.Unconfined))
        val session = FakeChatSession()
        val interactor = ChatStateInteractor(session, repo, chatStateFlow)
        interactor.toggleAutoApproveTools()
        assertEquals(true, session.toggled)
    }
}

