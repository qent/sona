package io.qent.sona.core.state

import dev.langchain4j.data.message.ChatMessage
import io.qent.sona.core.chat.Chat
import io.qent.sona.core.chat.ChatRepository
import io.qent.sona.core.chat.ChatRepositoryMessage
import io.qent.sona.core.chat.ChatSummary
import io.qent.sona.core.model.TokenUsageInfo
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
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

private class FakeChatFlow : ChatSession {
    private val flow = MutableSharedFlow<Chat>()
    var lastLoaded: String? = null
    var sent: String? = null
    var stopped = false
    var deletedFrom: Int? = null
    var toggled = false
    var resolved: Pair<Boolean, Boolean>? = null
    override suspend fun loadChat(id: String) { lastLoaded = id }
    override suspend fun send(text: String) { sent = text }
    override fun stop() { stopped = true }
    override suspend fun deleteFrom(idx: Int) { deletedFrom = idx }
    override fun toggleAutoApproveTools() { toggled = true }
    override suspend fun resolveToolPermission(allow: Boolean, always: Boolean) { resolved = allow to always }
    override suspend fun collect(collector: FlowCollector<Chat>) { flow.collect(collector) }
}

class ChatStateInteractorTest {
    @Test
    fun newChatCreatesWhenEmpty() = runBlocking {
        val repo = FakeChatRepository()
        val chatId = repo.createChat()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.newChat()
        assertNotEquals(chatId, flow.lastLoaded)
    }

    @Test
    fun newChatCreatesWhenLastHasMessages() = runBlocking {
        val repo = FakeChatRepository()
        val existing = repo.createChat()
        repo.messages[existing]?.add(ChatRepositoryMessage(existing, dev.langchain4j.data.message.UserMessage.from("hi"), "m"))
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.newChat()
        assertNotEquals(existing, flow.lastLoaded)
    }

    @Test
    fun openChatDelegates() = runBlocking {
        val repo = FakeChatRepository()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.openChat("123")
        assertEquals("123", flow.lastLoaded)
    }

    @Test
    fun listChatsReturnsRepositoryData() = runBlocking {
        val repo = FakeChatRepository()
        val chat1 = repo.createChat()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        val chats = interactor.listChats()
        assertEquals(1, chats.size)
        assertEquals(chat1, chats.first().id)
    }

    @Test
    fun deleteChatDelegatesToRepository() = runBlocking {
        val repo = FakeChatRepository()
        val id = repo.createChat()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.deleteChat(id)
        assertEquals(id, repo.deleted)
    }

    @Test
    fun sendDelegatesToFlow() = runBlocking {
        val repo = FakeChatRepository()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.send("hello")
        assertEquals("hello", flow.sent)
    }

    @Test
    fun stopDelegatesToFlow() = runBlocking {
        val repo = FakeChatRepository()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.stop()
        assertEquals(true, flow.stopped)
    }

    @Test
    fun deleteFromDelegatesToFlow() = runBlocking {
        val repo = FakeChatRepository()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.deleteFrom(5)
        assertEquals(5, flow.deletedFrom)
    }

    @Test
    fun toggleAutoApproveToolsDelegates() = runBlocking {
        val repo = FakeChatRepository()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.toggleAutoApproveTools()
        assertEquals(true, flow.toggled)
    }

    @Test
    fun resolveToolPermissionDelegates() = runBlocking {
        val repo = FakeChatRepository()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.resolveToolPermission(true, false)
        assertEquals(true to false, flow.resolved)
    }
}

