package io.qent.sona.core

import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.kotlin.model.chat.request.ChatRequestBuilder
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.output.TokenUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

data class Chat(
    val chatId: String,
    val tokenUsage: TokenUsage,
    val messages: List<ChatRepositoryMessage> = emptyList(),
    val requestInProgress: Boolean = false
)

class ChatFlow(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val modelFactory: suspend (Settings) -> ChatModel,
    tools: Tools,
): Flow<Chat> {

    private val tools = ToolsInfoDecorator(tools)

    private val innerStateFlow = MutableStateFlow(Chat("", TokenUsage(0, 0)))

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
        val chatId = innerStateFlow.value.chatId
        val settings = settingsRepository.load()
        val userMessage = ChatRepositoryMessage(chatId, UserMessage.from(text), settings.model)
        val messages = innerStateFlow.value.messages + userMessage
        innerStateFlow.value = innerStateFlow.value.copy(
            requestInProgress = true,
            messages = messages
        )

        val model = modelFactory(settings)
        val chatRequestBuilder = ChatRequestBuilder(messages.map { it.message }.toMutableList())
        chatRequestBuilder.parameters(configurer = {
            toolSpecifications = ToolSpecifications.toolSpecificationsFrom(tools)
        })

        var response = model.chat(chatRequestBuilder.build())
        val lastDialogTokenUsage = calculateLastTokenUsage(response.tokenUsage())
        val responseChatMessage = ChatRepositoryMessage(
            chatId,
            response.aiMessage(),
            settings.model,
            inputTokens = lastDialogTokenUsage.inputTokenCount(),
            outputTokens = lastDialogTokenUsage.outputTokenCount()
        )
        innerStateFlow.value = innerStateFlow.value.copy(
            messages = innerStateFlow.value.messages + responseChatMessage,
            tokenUsage = response.tokenUsage()
        )

        var responseMessage = response.aiMessage()
        while (responseMessage.hasToolExecutionRequests()) {
            for (toolRequest in responseMessage.toolExecutionRequests()) {
                val toolName = toolRequest.name()
                val toolResponse = when (toolName) {
                    "getFocusedFileText" -> {
                        ToolExecutionResultMessage(toolRequest.id(), toolName, tools.getFocusedFileText())
                    }

                    else -> throw IllegalArgumentException()
                }
                val toolResponseMessage = ChatRepositoryMessage(chatId, toolResponse, settings.model)
                innerStateFlow.value = innerStateFlow.value.copy(
                    messages = innerStateFlow.value.messages + toolResponseMessage
                )
            }

            response = model.chat(innerStateFlow.value.messages.map { it.message })
            responseMessage = response.aiMessage()

            val lastDialogTokenUsage = calculateLastTokenUsage(response.tokenUsage())
            val responseChatMessage = ChatRepositoryMessage(
                chatId,
                response.aiMessage(),
                settings.model,
                inputTokens = lastDialogTokenUsage.inputTokenCount(),
                outputTokens = lastDialogTokenUsage.outputTokenCount()
            )
            innerStateFlow.value = innerStateFlow.value.copy(
                messages = innerStateFlow.value.messages + responseChatMessage,
                tokenUsage = response.tokenUsage(),
                requestInProgress = false
            )
        }
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
            tokenUsage.outputTokenCount() - innerStateFlow.value.tokenUsage.outputTokenCount()
        )
    }
}
