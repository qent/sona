package io.qent.sona.core

import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.kotlin.model.chat.request.ChatRequestBuilder
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.output.TokenUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
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
): Flow<Chat> {

    private val tools = ToolsInfoDecorator(tools)

    private val innerStateFlow = MutableStateFlow(Chat("", TokenUsage(0, 0)))
    private val currentState get() = innerStateFlow.value

    suspend fun loadChat(chatId: String) {
        var outputTokens = 0
        var inputTokens = 0
        val messages = mutableListOf<ChatRepositoryMessage>()

        chatRepository.loadMessages(chatId).forEach { repositoryMessage ->
            outputTokens += repositoryMessage.outputTokens
            inputTokens += repositoryMessage.inputTokens
            messages.add(repositoryMessage)
        }

        innerStateFlow.value = Chat(
            chatId,
            TokenUsage(inputTokens, outputTokens),
            messages
        )
    }

    public suspend fun send(text: String) = try {
        val chatId = currentState.chatId
        val settings = settingsRepository.load()
        val userMessage = ChatRepositoryMessage(chatId, UserMessage.from(text), settings.model)

        val baseMessages = if (currentState.messages.isEmpty()) {
            val systemMessage = ChatRepositoryMessage(chatId, SystemMessage.from(rolesRepository.load()), settings.model)
            listOf(systemMessage, userMessage)
        } else {
            currentState.messages + userMessage
        }
        // add placeholder AI message which will be updated during streaming
        val placeholder = ChatRepositoryMessage(chatId, AiMessage.from(""), settings.model)
        innerStateFlow.value = innerStateFlow.value.copy(
            requestInProgress = true,
            messages = baseMessages + placeholder
        )

        val model = modelFactory(settings)

        var chatRequestBuilder = ChatRequestBuilder(baseMessages.map { it.message }.toMutableList())
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
                innerStateFlow.value = innerStateFlow.value.copy(
                    messages = innerStateFlow.value.messages + toolResponseMessage
                )
            }

            chatRequestBuilder = ChatRequestBuilder(innerStateFlow.value.messages.map { it.message }.toMutableList())
            chatRequestBuilder.parameters(configurer = {
                toolSpecifications = ToolSpecifications.toolSpecificationsFrom(tools)
            })

            response = streamChat(model, chatRequestBuilder.build(), chatId, settings)
            responseMessage = response.aiMessage()
        }

        innerStateFlow.value = innerStateFlow.value.copy(requestInProgress = false)

    } catch (e: Exception) {
        val errorMessage = ChatRepositoryMessage(
            innerStateFlow.value.chatId,
            AiMessage.from("Error: ${e.message}"),
            settingsRepository.load().model
        )
        innerStateFlow.value = innerStateFlow.value.copy(
            messages = innerStateFlow.value.messages + errorMessage,
            requestInProgress = false
        )
    }

    override suspend fun collect(collector: FlowCollector<Chat>) {
        innerStateFlow.collect(collector)
    }

    private fun calculateLastTokenUsage(tokenUsage: TokenUsage): TokenUsage {
        return TokenUsage(
            tokenUsage.inputTokenCount() - innerStateFlow.value.tokenUsage.inputTokenCount(),
            tokenUsage.outputTokenCount() - innerStateFlow.value.tokenUsage.outputTokenCount(),
        )
    }

    private suspend fun streamChat(
        model: StreamingChatModel,
        request: dev.langchain4j.model.chat.request.ChatRequest,
        chatId: String,
        settings: Settings
    ): dev.langchain4j.model.chat.response.ChatResponse {
        val builder = StringBuilder()
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            model.chat(request, object : dev.langchain4j.model.chat.response.StreamingChatResponseHandler {
                override fun onPartialResponse(partialResponse: String) {
                    builder.append(partialResponse)
                    val msgs = innerStateFlow.value.messages.toMutableList()
                    val lastIndex = msgs.lastIndex
                    if (lastIndex >= 0) {
                        msgs[lastIndex] = msgs[lastIndex].copy(message = AiMessage.from(builder.toString()))
                        innerStateFlow.value = innerStateFlow.value.copy(messages = msgs)
                    }
                }

                override fun onCompleteResponse(chatResponse: dev.langchain4j.model.chat.response.ChatResponse) {
                    val lastUsage = calculateLastTokenUsage(chatResponse.tokenUsage())
                    val msgs = innerStateFlow.value.messages.toMutableList()
                    val lastIndex = msgs.lastIndex
                    if (lastIndex >= 0) {
                        msgs[lastIndex] = ChatRepositoryMessage(
                            chatId,
                            chatResponse.aiMessage(),
                            settings.model,
                            inputTokens = lastUsage.inputTokenCount(),
                            outputTokens = lastUsage.outputTokenCount()
                        )
                        innerStateFlow.value = innerStateFlow.value.copy(
                            messages = msgs,
                            tokenUsage = chatResponse.tokenUsage()
                        )
                    }
                    cont.resume(chatResponse)
                }

                override fun onError(t: Throwable) {
                    cont.resumeWithException(t)
                }
            })
        }
    }
}
