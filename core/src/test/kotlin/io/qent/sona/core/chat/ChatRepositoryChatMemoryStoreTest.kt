package io.qent.sona.core.chat

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.model.TokenUsageInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRepositoryChatMemoryStoreTest {
    private class FakeChatRepository : ChatRepository {
        val messages = mutableMapOf<String, MutableList<ChatRepositoryMessage>>()
        override suspend fun createChat(): String = "1"
        override suspend fun addMessage(chatId: String, message: dev.langchain4j.data.message.ChatMessage, model: String, tokenUsage: TokenUsageInfo) {
            messages.getOrPut(chatId) { mutableListOf() }.add(ChatRepositoryMessage(chatId, message, model, tokenUsage))
        }
        override suspend fun loadMessages(chatId: String) = messages[chatId] ?: emptyList()
        override suspend fun loadTokenUsage(chatId: String) = TokenUsageInfo()
        override suspend fun isToolAllowed(chatId: String, toolName: String) = false
        override suspend fun addAllowedTool(chatId: String, toolName: String) {}
        override suspend fun listChats() = emptyList<ChatSummary>()
        override suspend fun deleteChat(chatId: String) {}
        override suspend fun deleteMessagesFrom(chatId: String, index: Int) {
            messages[chatId]?.apply { if (index < size) subList(index, size).clear() }
        }
    }

    @Test
    fun insertsPlaceholderBeforeNextUserMessage() = runBlocking {
        val repo = FakeChatRepository()
        val req = ToolExecutionRequest.builder().id("x").name("t").arguments("{}").build()
        val ai = AiMessage("", listOf(req))
        val user = UserMessage.from("hi")
        repo.messages["1"] = mutableListOf(
            ChatRepositoryMessage("1", ai, "m"),
            ChatRepositoryMessage("1", user, "m"),
        )
        val store = ChatRepositoryChatMemoryStore(repo, "1", "error")
        val msgs = store.messages()
        assertEquals(3, msgs.size)
        val result = msgs[1] as ToolExecutionResultMessage
        assertEquals("error", result.text())
        val stored = repo.messages["1"]!!
        assertEquals(3, stored.size)
        assertEquals(result, stored[1].message)
    }
}
