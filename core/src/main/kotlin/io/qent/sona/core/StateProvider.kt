package io.qent.sona.core

import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.kotlin.model.chat.request.ChatRequestBuilder
import dev.langchain4j.model.chat.ChatModel
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
    tools: Tools,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var currentChatId: String? = null
    private var messages = mutableListOf<StoredChatMessage>()
    private var outputTokens = 0
    private var inputTokens = 0
    private var sending = false

    private val tools = ToolsInfoDecorator(tools)

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
        outgoingTokens = outputTokens,
        incomingTokens = inputTokens,
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
        outputTokens = 0
        inputTokens = 0
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
        if (chatRepository.listChats().isEmpty()) {
            newChat()
        } else {
            showHistory()
        }
    }

    private fun recalcTokens() {
        outputTokens = messages.sumOf { it.outputTokens }
        inputTokens = messages.sumOf { it.inputTokens }
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
            val chatRequestBuilder = ChatRequestBuilder(messages.map { it.message }.toMutableList())

            if (messages.size == 1) {
                chatRequestBuilder.parameters(configurer = {
                    toolSpecifications = ToolSpecifications.toolSpecificationsFrom(tools)
                })
            }

            var llmResponse = model.chat(chatRequestBuilder.build())
            var llmResponseMessage = llmResponse.aiMessage()
            var currentResponseTokenUsage: TokenUsage? = llmResponse.tokenUsage()
            var currentResponseTokenInput = currentResponseTokenUsage?.inputTokenCount() ?: 0
            var currentResponseTokenOutput = currentResponseTokenUsage?.outputTokenCount() ?: 0

            messages += StoredChatMessage(
                llmResponseMessage,
                model = settings.model,
                inputTokens = currentResponseTokenInput - inputTokens,
                outputTokens = currentResponseTokenOutput - outputTokens
            )
            chatRepository.addMessage(
                chatId,
                llmResponseMessage,
                settings.model,
                inputTokens = currentResponseTokenInput - inputTokens,
                outputTokens = currentResponseTokenOutput - outputTokens
            )

            outputTokens = currentResponseTokenOutput
            inputTokens = currentResponseTokenInput

            emitChatState()

            while (llmResponseMessage.hasToolExecutionRequests()) {
                for (toolRequest in llmResponseMessage.toolExecutionRequests()) {
                    val toolName = toolRequest.name()
                    val toolResponse = when (toolName) {
                        "getFocusedFileText" -> {
                            ToolExecutionResultMessage(toolRequest.id(), toolName, tools.getFocusedFileText())
                        }
                        else -> throw IllegalArgumentException()
                    }

                    messages += StoredChatMessage(toolResponse)
                    chatRepository.addMessage(chatId, toolResponse, null, 0, 0)
                    emitChatState()
                }

                llmResponse = model.chat(messages.map { it.message }.toMutableList())
                llmResponseMessage = llmResponse.aiMessage()
                currentResponseTokenUsage = llmResponse.tokenUsage()
                currentResponseTokenInput += currentResponseTokenUsage?.inputTokenCount() ?: 0
                currentResponseTokenOutput += currentResponseTokenUsage?.outputTokenCount() ?: 0

                messages += StoredChatMessage(
                    llmResponseMessage,
                    model = settings.model,
                    inputTokens = currentResponseTokenInput - inputTokens,
                    outputTokens = currentResponseTokenOutput - outputTokens
                )
                chatRepository.addMessage(
                    chatId,
                    llmResponseMessage,
                    settings.model,
                    inputTokens = currentResponseTokenInput - inputTokens,
                    outputTokens = currentResponseTokenOutput - outputTokens
                )

                outputTokens = currentResponseTokenOutput
                inputTokens = currentResponseTokenInput

                emitChatState()
            }
        } catch (e: Exception) {
            val err = AiMessage.from("Error: ${e.message}")
            messages += StoredChatMessage(err)
            chatRepository.addMessage(chatId, err, null, 0, 0)
            emitChatState()
        } finally {
            sending = false
        }
    }
}
