package io.qent.sona.core

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.output.TokenUsage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StateProvider(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val modelFactory: suspend (Settings) -> ChatModel,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var currentChatId: String? = null
    private var messages = mutableListOf<StoredChatMessage>()
    private var outgoing = 0
    private var incoming = 0
    private var sending = false

    private val _state = MutableStateFlow<State>(State.ChatState())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        scope.launch { loadInitialChat() }
    }

    private suspend fun loadInitialChat() {
        val chats = chatRepository.listChats()
        currentChatId = chats.firstOrNull()?.id ?: chatRepository.createChat()
        messages = chatRepository.loadMessages(currentChatId!!).toMutableList()
        recalcTokens()
        emitChatState()
    }

    private fun createChatState() = State.ChatState(
        messages = messages.map { it.message },
        outgoingTokens = outgoing,
        incomingTokens = incoming,
        isSending = sending,
        onSendMessage = { text -> scope.launch { send(text) } },
        onNewChat = { scope.launch { newChat() } },
        onOpenHistory = { scope.launch { showHistory() } },
    )

    private fun createListState(chats: List<ChatSummary>) = State.ChatListState(
        chats = chats,
        onOpenChat = { id -> scope.launch { openChat(id) } },
        onDeleteChat = { id -> scope.launch { deleteChat(id) } },
        onNewChat = { scope.launch { newChat() } },
    )

    private fun emitChatState() {
        _state.value = createChatState()
    }

    private suspend fun showHistory() {
        val chats = chatRepository.listChats()
        _state.value = createListState(chats)
    }

    private suspend fun newChat() {
        currentChatId = chatRepository.createChat()
        messages.clear()
        outgoing = 0
        incoming = 0
        emitChatState()
    }

    private suspend fun openChat(id: String) {
        currentChatId = id
        messages = chatRepository.loadMessages(id).toMutableList()
        recalcTokens()
        emitChatState()
    }

    private suspend fun deleteChat(id: String) {
        chatRepository.deleteChat(id)
        showHistory()
    }

    private fun recalcTokens() {
        outgoing = messages.sumOf { it.inputTokens }
        incoming = messages.sumOf { it.outputTokens }
    }

    private suspend fun send(text: String) {
        val chatId = currentChatId ?: return
        if (sending) return
        sending = true
        val userMsg = UserMessage.from(text)
        messages += StoredChatMessage(userMsg)
        chatRepository.addMessage(chatId, userMsg, null, 0, 0)
        emitChatState()
        try {
            val settings = settingsRepository.load()
            val model = modelFactory(settings)
            val response: ChatResponse = model.chat(messages.map { it.message })
            val aiMessage = response.aiMessage()
            val usage: TokenUsage? = response.tokenUsage()
            val out = usage?.inputTokenCount() ?: 0
            val inn = usage?.outputTokenCount() ?: 0
            outgoing += out
            incoming += inn
            val stored = StoredChatMessage(
                aiMessage,
                model = settings.model,
                inputTokens = out,
                outputTokens = inn,
            )
            messages += stored
            chatRepository.addMessage(chatId, aiMessage, settings.model, out, inn)
        } catch (e: Exception) {
            val err = AiMessage.from("Error: ${e.message}")
            messages += StoredChatMessage(err)
            chatRepository.addMessage(chatId, err, null, 0, 0)
        } finally {
            sending = false
            emitChatState()
        }
    }
}
