package io.qent.sona.core.chat

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.tool.ToolExecution
import io.qent.sona.core.Logger
import io.qent.sona.core.model.toInfo
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking

/**
 * Handles chat operations such as sending messages and managing streaming state.
 *
 * The actual chat state is provided by [ChatStateFlow].
 */
class ChatController(
    private val presetsRepository: PresetsRepository,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val chatStateFlow: ChatStateFlow,
    private val chatAgentFactory: ChatAgentFactory,
    scope: CoroutineScope,
    private val log: Logger = Logger.NoOp,
) : ChatSession {

    private val scope = scope + Dispatchers.IO

    private val currentState get() = chatStateFlow.currentState

    private var currentStream: TokenStream? = null
    private var ignoreCallbacks = false

    override suspend fun send(text: String) {
        val chatId = currentState.chatId
        val preset = presetsRepository.load().let { it.presets[it.active] }
        log.log("send: chatId=$chatId preset=${preset.model} text=$text")

        try {
            val userMsg = UserMessage.from(text)
            log.log("save user message to repo")
            chatRepository.addMessage(chatId, userMsg, preset.model)
            val userRepoMsg = ChatRepositoryMessage(chatId, userMsg, preset.model)
            val baseMessages = currentState.messages + userRepoMsg
            log.log("emit: user message")
            chatStateFlow.emit(currentState.copy(messages = baseMessages))

            val placeholder = ChatRepositoryMessage(chatId, AiMessage.from(""), preset.model)
            log.log("emit: ai placeholder message")
            chatStateFlow.emit(
                currentState.copy(
                    requestInProgress = true,
                    messages = baseMessages + placeholder,
                    isStreaming = true
                )
            )

            val aiService = chatAgentFactory.create()
            val maxRetries = settingsRepository.load().apiRetries
            val builder = StringBuilder()
            var attempt = 0

            fun startStream() {
                if (ignoreCallbacks) {
                    log.log("ignore callbacks: startStream")
                    return
                }

                builder.setLength(0)

                ignoreCallbacks = false
                log.log("startStream: attempt=$attempt")
                currentStream = aiService.chat(chatId, text)
                    .onPartialResponse { token ->
                        log.log("ignore callbacks: onPartialResponse")
                        if (ignoreCallbacks) return@onPartialResponse
                        log.log("onPartialResponse: $token")
                        builder.append(token)
                        val msgs = currentState.messages.toMutableList()
                        val lastIndex = msgs.lastIndex
                        if (lastIndex >= 0) {
                            msgs[lastIndex] = msgs[lastIndex].copy(
                                message = AiMessage.from(builder.toString())
                            )
                            log.log("emit: partial message")
                            chatStateFlow.emit(currentState.copy(messages = msgs, isStreaming = true, requestInProgress = true))
                        }
                    }
                    .onToolExecuted { exec: ToolExecution ->
                        if (ignoreCallbacks) {
                            log.log("ignore callbacks: onToolExecuted")
                            return@onToolExecuted
                        }
                        log.log("onToolExecuted: ${exec.request().name()}")
                        val toolResultMessage = ToolExecutionResultMessage(
                            exec.request().id(),
                            exec.request().name(),
                            exec.result()
                        )

                        runBlocking {
                            log.log("add tool result to repo")
                            chatRepository.addMessage(chatId, toolResultMessage, preset.model)
                        }

                        val messages = currentState.messages.toMutableList()
                        val repositoryMessage = ChatRepositoryMessage(chatId, toolResultMessage, preset.model)
                        val idx = messages.indexOfLast {
                            val m = it.message
                            m is ToolExecutionResultMessage && m.id() == exec.request().id()
                        }
                        if (idx >= 0) {
                            log.log("replace tool placeholder")
                            messages[idx] = repositoryMessage
                        } else {
                            messages += repositoryMessage
                        }
                        val placeholderAfter = ChatRepositoryMessage(chatId, AiMessage.from(""), preset.model)
                        builder.setLength(0)
                        log.log("emit: placeholder after tools")
                        chatStateFlow.emit(
                            currentState.copy(
                                messages = messages + placeholderAfter,
                                requestInProgress = true,
                                isStreaming = false
                            )
                        )
                    }
                    .onCompleteResponse { response ->
                        if (ignoreCallbacks) {
                            log.log("ignore callbacks: onCompleteResponse")
                            return@onCompleteResponse
                        }
                        log.log("onCompleteResponse")
                        val totalUsage = response.metadata().tokenUsage().toInfo()
                        val msgs = currentState.messages.toMutableList()
                        val lastIndex = msgs.lastIndex
                        if (lastIndex >= 0) {
                            msgs[lastIndex] = ChatRepositoryMessage(
                                chatId,
                                response.aiMessage(),
                                preset.model,
                                totalUsage
                            )
                        }
                        runBlocking {
                            log.log("add full response message to repo")
                            chatRepository.addMessage(chatId, response.aiMessage(), preset.model, totalUsage)
                        }
                        log.log("emit: token usage")
                        chatStateFlow.emit(
                            currentState.copy(
                                messages = msgs,
                                tokenUsage = currentState.tokenUsage + totalUsage,
                                requestInProgress = false,
                                isStreaming = false
                            )
                        )
                        currentStream = null
                    }
                    .onError { t ->
                        if (ignoreCallbacks) {
                            log.log("ignore callbacks: onError")
                            return@onError
                        }
                        log.log("onError: ${t.message}")
                        if (t is CancellationException) {
                            log.log("CancellationException")
                            currentStream = null
                            chatStateFlow.emit(currentState.copy(requestInProgress = false, isStreaming = false))
                            return@onError
                        }
                        currentStream = null
                        if (attempt < maxRetries) {
                            val delayMs = 1000L * (1 shl attempt)
                            attempt++
                            log.log("retrying in ${delayMs}ms")
                            scope.launch {
                                delay(delayMs)
                                if (!ignoreCallbacks) startStream()
                            }
                            chatStateFlow.emit(currentState.copy(requestInProgress = true, isStreaming = false))
                        } else {
                            log.log("max retries reached")
                            val errMsg = ChatRepositoryMessage(
                                chatId,
                                AiMessage.from("Error: ${t.message}"),
                                preset.model
                            )
                            runBlocking {
                                log.log("add error to repo")
                                chatRepository.addMessage(chatId, errMsg.message, preset.model)
                            }
                            val messages = currentState.messages.toMutableList()
                            if (messages.isNotEmpty()) {
                                messages[messages.lastIndex] = errMsg
                            } else {
                                messages += errMsg
                            }
                            log.log("emit: error")
                            chatStateFlow.emit(
                                currentState.copy(
                                    messages = messages,
                                    requestInProgress = false,
                                    isStreaming = false
                                )
                            )
                        }
                    }
                currentStream?.start()
            }

            startStream()
        } catch (_: CancellationException) {
            log.log("send cancelled")
            currentStream = null
            chatStateFlow.emit(currentState.copy(requestInProgress = false, isStreaming = false))
        } catch (e: Exception) {
            log.log("send exception: ${e.message}")
            val errorMessage = ChatRepositoryMessage(
                chatId,
                AiMessage.from("Error: ${e.message}\n\n${e.stackTrace}"),
                preset.model
            )
            chatStateFlow.emit(
                currentState.copy(
                    messages = currentState.messages + errorMessage,
                    requestInProgress = false,
                    isStreaming = false
                )
            )
        }
    }

    override fun toggleAutoApproveTools() {
        val newValue = !currentState.autoApproveTools
        log.log("toggleAutoApproveTools: $newValue")
        chatStateFlow.emit(currentState.copy(autoApproveTools = newValue))
    }

    override suspend fun deleteFrom(index: Int) {
        log.log("deleteFrom: index=$index")
        stop()
        val chatId = currentState.chatId
        chatRepository.deleteMessagesFrom(chatId, index)
        chatStateFlow.loadChat(chatId)
    }

    override fun stop() {
        log.log("stop")
        ignoreCallbacks = true
        currentStream?.ignoreErrors()
        currentStream = null
        chatStateFlow.emit(currentState.copy(requestInProgress = false, isStreaming = false))
    }
}

