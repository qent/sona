package io.qent.sona.core

import dev.langchain4j.model.chat.ChatModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StateProvider(
    settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    modelFactory: suspend (Settings) -> ChatModel,
    tools: Tools,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val chatFlow = ChatFlow(settingsRepository, chatRepository, modelFactory, tools)

    private val _state = MutableStateFlow<State>(State.ChatState())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        scope.launch {
            val lastChatId = chatRepository.listChats().firstOrNull()?.id
            if (lastChatId == null) {
                newChat()
            } else {
                openChat(lastChatId)
            }
        }

        chatFlow.onEach { (chatId, tokenUsage, messages, requestInProgress) ->
            _state.value = State.ChatState(
                messages.map { it.message },
                tokenUsage.outputTokenCount(),
                tokenUsage.inputTokenCount(),
                isSending = requestInProgress,
                onSendMessage = { text -> scope.launch { send(text) } },
                onNewChat = { scope.launch { newChat() } },
                onOpenHistory = { scope.launch { showHistory() } },
            )
        }.launchIn(scope)

        chatFlow.map { it.messages.lastOrNull() }.filterNotNull().distinctUntilChanged().onEach { message ->
            chatRepository.addMessage(
                message.chatId,
                message.message,
                message.model,
                message.inputTokens,
                message.outputTokens
            )
        }.launchIn(scope)
    }

    private fun createListState(chats: List<ChatSummary>) = State.ChatListState(
        chats = chats,
        onOpenChat = { id -> scope.launch { openChat(id) } },
        onDeleteChat = { id -> scope.launch { deleteChat(id) } },
        onNewChat = { scope.launch { newChat() } },
    )

    private suspend fun showHistory() {
        val chats = chatRepository.listChats()
        _state.value = createListState(chats)
    }

    private suspend fun newChat() {
        chatFlow.loadChat(chatRepository.createChat())
    }

    private suspend fun openChat(id: String) {
        chatFlow.loadChat(id)
    }

    private suspend fun deleteChat(id: String) {
        chatRepository.deleteChat(id)
        if (chatRepository.listChats().isEmpty()) {
            newChat()
        } else {
            showHistory()
        }
    }

    private suspend fun send(text: String) {
        chatFlow.send(text)
    }
}
