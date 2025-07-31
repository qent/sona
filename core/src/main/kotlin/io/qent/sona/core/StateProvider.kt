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
    private val repository: SettingsRepository,
    private val modelFactory: suspend (Settings) -> ChatModel,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val messages = mutableListOf<ChatMessage>()
    private var outgoing = 0
    private var incoming = 0
    private var sending = false

    private val _state = MutableStateFlow(createState())
    val state: StateFlow<State> = _state.asStateFlow()

    private fun createState() = State(
        messages = messages.toList(),
        outgoingTokens = outgoing,
        incomingTokens = incoming,
        isSending = sending,
        onSendMessage = { text -> scope.launch { send(text) } },
    )

    private fun emit() {
        _state.value = createState()
    }

    private suspend fun send(text: String) {
        if (sending) return
        sending = true
        messages += UserMessage.from(text)
        emit()
        try {
            val settings = repository.load()
            val model = modelFactory(settings)
            val response: ChatResponse = model.chat(messages)
            val aiMessage = response.aiMessage()
            val usage: TokenUsage? = response.tokenUsage()
            outgoing += usage?.inputTokenCount() ?: 0
            incoming += usage?.outputTokenCount() ?: 0
            messages += aiMessage
        } catch (e: Exception) {
            messages += AiMessage.from("Error: ${e.message}")
        } finally {
            sending = false
            emit()
        }
    }
}
