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
    override suspend fun deleteChat(chatId: String) { chats.removeIf { it.id == chatId }; messages.remove(chatId) }
    override suspend fun deleteMessagesFrom(chatId: String, index: Int) {}
}

private class FakeChatFlow : ChatSession {
    private val flow = MutableSharedFlow<Chat>()
    var lastLoaded: String? = null
    override suspend fun loadChat(id: String) { lastLoaded = id }
    override suspend fun send(text: String) {}
    override fun stop() {}
    override suspend fun deleteFrom(idx: Int) {}
    override fun toggleAutoApproveTools() {}
    override suspend fun resolveToolPermission(allow: Boolean, always: Boolean) {}
    override suspend fun collect(collector: FlowCollector<Chat>) { flow.collect(collector) }
}

class ChatStateInteractorTest {
    @Test
    fun newChatLoadsExistingWhenEmpty() = runBlocking {
        val repo = FakeChatRepository()
        val chatId = repo.createChat()
        val flow = FakeChatFlow()
        val interactor = ChatStateInteractor(flow, repo)
        interactor.newChat()
        assertEquals(chatId, flow.lastLoaded)
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
}

