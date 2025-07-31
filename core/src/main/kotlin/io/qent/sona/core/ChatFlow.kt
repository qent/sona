package io.qent.sona.core

import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.kotlin.model.chat.request.ChatRequestBuilder
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.output.TokenUsage
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class Chat(
    val chatId: String,
    val tokenUsage: TokenUsage,
    val messages: List<ChatRepositoryMessage> = emptyList(),
    val requestInProgress: Boolean = false
)

class ChatFlow(
    private val settingsRepository: SettingsRepository,
    private val rolesRepository: RolesRepository,
    private val chatRepository: ChatRepository,
    private val modelFactory: suspend (Settings) -> StreamingChatModel,
    tools: Tools,
    scope: CoroutineScope,
): Flow<Chat> {

    private val scope = scope + Dispatchers.IO
    private val tools = ToolsInfoDecorator(tools)

    private val mutableSharedState = MutableSharedFlow<Chat>()
    private var currentState = Chat("", TokenUsage(0, 0))
    private var currentContinuation: CancellableContinuation<ChatResponse>? = null

    suspend fun loadChat(chatId: String) {
        var outputTokens = 0
        var inputTokens = 0
        val messages = mutableListOf<ChatRepositoryMessage>()

        chatRepository.loadMessages(chatId).forEach { repositoryMessage ->
            outputTokens += repositoryMessage.outputTokens
            inputTokens += repositoryMessage.inputTokens
            messages.add(repositoryMessage)
        }

        emit(Chat(chatId, TokenUsage(inputTokens, outputTokens), messages))
    }
    
    private fun emit(chat: Chat) {
        currentState = chat
        scope.launch {
            mutableSharedState.emit(chat)
        }
    }

    suspend fun send(text: String) = try {
        val chatId = currentState.chatId
        val settings = settingsRepository.load()
        val userMessage = ChatRepositoryMessage(chatId, UserMessage.from(text), settings.model)

        val baseMessages = currentState.messages + userMessage
        emit(currentState.copy(messages = baseMessages))

        val placeholder = ChatRepositoryMessage(chatId, AiMessage.from(""), settings.model)
        emit(currentState.copy(
            requestInProgress = true,
            messages = baseMessages + placeholder
        ))

        val model = modelFactory(settings)

        val roleText = rolesRepository.load().let { it.roles[it.active].text }
        val systemMessage = SystemMessage.from(roleText)

        var chatRequestBuilder = ChatRequestBuilder((listOf(systemMessage) + baseMessages.map { it.message }).toMutableList())
        chatRequestBuilder.parameters(configurer = {
            toolSpecifications = ToolSpecifications.toolSpecificationsFrom(tools)
        })

        var response = streamChat(model, chatRequestBuilder.build(), chatId, settings)
        var responseMessage = response.aiMessage()

        while (responseMessage.hasToolExecutionRequests()) {
            for (toolRequest in responseMessage.toolExecutionRequests()) {
                val toolName = toolRequest.name()
                val toolResponse = when (toolName) {
                    "getFocusedFileText" -> ToolExecutionResultMessage(toolRequest.id(), toolName, tools.getFocusedFileText())
                    else -> throw IllegalArgumentException()
                }
                val toolResponseMessage = ChatRepositoryMessage(chatId, toolResponse, settings.model)
                emit(currentState.copy(
                    messages = currentState.messages + toolResponseMessage
                ))
            }

            chatRequestBuilder = ChatRequestBuilder((listOf(systemMessage) + currentState.messages.map { it.message }).toMutableList())
            chatRequestBuilder.parameters(configurer = {
                toolSpecifications = ToolSpecifications.toolSpecificationsFrom(tools)
            })

            response = streamChat(model, chatRequestBuilder.build(), chatId, settings)
            responseMessage = response.aiMessage()
        }

        emit(currentState.copy(requestInProgress = false))
    } catch (_: CancellationException) {
        // request was cancelled, keep partial response
    } catch (e: Exception) {
        val errorMessage = ChatRepositoryMessage(
            currentState.chatId,
            AiMessage.from("Error: ${e.message}"),
            settingsRepository.load().model
        )
        emit(currentState.copy(
            messages = currentState.messages + errorMessage,
            requestInProgress = false
        ))
    } finally {
        currentContinuation = null
    }

    override suspend fun collect(collector: FlowCollector<Chat>) {
        mutableSharedState.collect(collector)
    }

    private fun calculateLastTokenUsage(tokenUsage: TokenUsage): TokenUsage {
        return TokenUsage(
            tokenUsage.inputTokenCount() - currentState.tokenUsage.inputTokenCount(),
            tokenUsage.outputTokenCount() - currentState.tokenUsage.outputTokenCount(),
        )
    }

    private suspend fun streamChat(
        model: StreamingChatModel,
        request: ChatRequest,
        chatId: String,
        settings: Settings
    ): ChatResponse {
        val builder = StringBuilder()
        return suspendCancellableCoroutine { cont ->
            currentContinuation = cont
            model.chat(request, object : StreamingChatResponseHandler {
                override fun onPartialResponse(partialResponse: String) {
                    if (!cont.isActive) return
                    builder.append(partialResponse)
                    val msgs = currentState.messages.toMutableList()
                    val lastIndex = msgs.lastIndex
                    if (lastIndex >= 0) {
                        msgs[lastIndex] = msgs[lastIndex].copy(message = AiMessage.from(builder.toString()))
                        emit(currentState.copy(messages = msgs))
                    }
                }

                override fun onCompleteResponse(chatResponse: ChatResponse) {
                    if (!cont.isActive) return
                    val lastUsage = calculateLastTokenUsage(chatResponse.tokenUsage())
                    val msgs = currentState.messages.toMutableList()
                    val lastIndex = msgs.lastIndex
                    if (lastIndex >= 0) {
                        msgs[lastIndex] = ChatRepositoryMessage(
                            chatId,
                            chatResponse.aiMessage(),
                            settings.model,
                            inputTokens = lastUsage.inputTokenCount(),
                            outputTokens = lastUsage.outputTokenCount()
                        )
                        emit(currentState.copy(
                            messages = msgs,
                            tokenUsage = chatResponse.tokenUsage()
                        ))
                    }
                    cont.resume(chatResponse)
                }

                override fun onError(t: Throwable) {
                    if (!cont.isActive) return
                    cont.resumeWithException(t)
                }
            })
        }
    }

    fun stop() {
        currentContinuation?.cancel()
        currentContinuation = null
        emit(currentState.copy(requestInProgress = false))
    }
}
