package io.qent.sona.core

import com.google.gson.Gson
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
import dev.langchain4j.service.tool.ToolProviderRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class Chat(
    val chatId: String,
    val tokenUsage: TokenUsageInfo,
    val messages: List<ChatRepositoryMessage> = emptyList(),
    val requestInProgress: Boolean = false,
    val isStreaming: Boolean = false,
    val toolRequest: String? = null,
    val autoApproveTools: Boolean = false,
)

class ChatFlow(
    private val presetsRepository: PresetsRepository,
    private val rolesRepository: RolesRepository,
    private val chatRepository: ChatRepository,
    private val modelFactory: (Preset) -> StreamingChatModel,
    private val tools: Tools,
    scope: CoroutineScope,
    private val systemMessages: List<SystemMessage> = emptyList(),
    private val mcpManager: McpConnectionManager,
) : Flow<Chat> {

    private val scope = scope + Dispatchers.IO

    private val mutableSharedState = MutableSharedFlow<Chat>()
    private var currentState = Chat("", TokenUsageInfo())
    private var currentContinuation: CancellableContinuation<ChatResponse>? = null
    private var toolContinuation: CancellableContinuation<ToolDecision>? = null

    suspend fun loadChat(chatId: String) {
        val messages = chatRepository.loadMessages(chatId)
        val usage = chatRepository.loadTokenUsage(chatId)
        emit(Chat(chatId, usage, messages))
    }

    private fun emit(chat: Chat) {
        currentState = chat // BEFORE SCOPE LAUNCH!
        scope.launch {
            mutableSharedState.emit(chat)
        }
    }

    suspend fun send(text: String) {
        val chatId = currentState.chatId
        val preset = presetsRepository.load().let { it.presets[it.active] }
        try {
            val userMessage = ChatRepositoryMessage(chatId, UserMessage.from(text), preset.model)

            val baseMessages = currentState.messages + userMessage
            emit(currentState.copy(messages = baseMessages))

            val placeholder = ChatRepositoryMessage(chatId, AiMessage.from(""), preset.model)
            emit(
                currentState.copy(
                    requestInProgress = true,
                    messages = baseMessages + placeholder,
                    isStreaming = true
                )
            )

            val model = modelFactory(preset)

            val roleText = rolesRepository.load().let { it.roles[it.active].text }
            val roleMessage = SystemMessage.from(roleText)

            var chatRequestBuilder =
                ChatRequestBuilder((systemMessages + roleMessage + baseMessages.map { it.message }).toMutableList())
            val baseToolSpecs = ToolSpecifications.toolSpecificationsFrom(tools).toMutableList() + mcpManager.listTools()

            chatRequestBuilder.parameters(configurer = {
                toolSpecifications = baseToolSpecs
            })

            var response = streamChat(model, chatRequestBuilder.build(), chatId, preset)
            var responseMessage = response.aiMessage()

            while (responseMessage.hasToolExecutionRequests()) {
                val messagesWithToolsResponse = currentState.messages.toMutableList()
                for (toolRequest in responseMessage.toolExecutionRequests()) {
                    val toolName = toolRequest.name()
                    val decision = if (currentState.autoApproveTools || chatRepository.isToolAllowed(chatId, toolName)) {
                        ToolDecision(true, false)
                    } else {
                        requestToolPermission(toolName)
                    }
                    if (decision.always) {
                        chatRepository.addAllowedTool(chatId, toolName)
                    }
                    val toolResponse = if (decision.allow) {
                        when (toolName) {
                            "getFocusedFileText" -> ToolExecutionResultMessage(
                                toolRequest.id(),
                                toolName,
                                tools.getFocusedFileText()
                            )

                            "readFile" -> {
                                val args = Gson().fromJson(toolRequest.arguments(), Map::class.java) as Map<*, *>
                                val path = args["arg0"]?.toString() ?: ""
                                ToolExecutionResultMessage(
                                    toolRequest.id(),
                                    toolName,
                                    tools.readFile(path)
                                )
                            }

                            "switchToArchitect" -> ToolExecutionResultMessage(
                                toolRequest.id(),
                                toolName,
                                tools.switchToArchitect()
                            )

                            "switchToCode" -> ToolExecutionResultMessage(
                                toolRequest.id(),
                                toolName,
                                tools.switchToCode()
                            )

                            else ->
                                if (mcpManager?.hasTool(toolName) == true) {
                                    ToolExecutionResultMessage(
                                        toolRequest.id(),
                                        toolName,
                                        mcpManager.execute(
                                            toolRequest.id(),
                                            toolName,
                                            toolRequest.arguments()
                                        )
                                    )
                                } else {
                                    ToolExecutionResultMessage(
                                        toolRequest.id(),
                                        toolName,
                                        "Tool not found"
                                    )
                                }
                        }
                    } else {
                        ToolExecutionResultMessage(
                            toolRequest.id(),
                            toolName,
                            "Tool execution cancelled"
                        )
                    }
                    val toolResponseMessage = ChatRepositoryMessage(chatId, toolResponse, preset.model)
                    messagesWithToolsResponse += toolResponseMessage
                    emit(currentState.copy(
                        messages = messagesWithToolsResponse,
                        isStreaming = false,
                        requestInProgress = true
                    ))
                }

                emit(currentState.copy(
                    messages = currentState.messages + placeholder,
                    isStreaming = true
                ))

                chatRequestBuilder =
                    ChatRequestBuilder((systemMessages + roleMessage + messagesWithToolsResponse.map { it.message }).toMutableList())
                val loopToolSpecs = ToolSpecifications.toolSpecificationsFrom(tools).toMutableList() + mcpManager.listTools()

                chatRequestBuilder.parameters(configurer = {
                    toolSpecifications = loopToolSpecs
                })

                response = streamChat(model, chatRequestBuilder.build(), chatId, preset)
                responseMessage = response.aiMessage()
            }
        } catch (_: CancellationException) {
            // request was cancelled, keep partial response
        } catch (e: Exception) {
            val errorMessage = ChatRepositoryMessage(
                currentState.chatId,
                AiMessage.from("Error: ${e.message}"),
                preset.model
            )
            emit(
                currentState.copy(
                    messages = currentState.messages + errorMessage,
                    requestInProgress = false,
                    isStreaming = false
                )
            )
        } finally {
            currentContinuation = null
        }
    }

    override suspend fun collect(collector: FlowCollector<Chat>) {
        mutableSharedState.collect(collector)
    }

    private data class ToolDecision(val allow: Boolean, val always: Boolean)

    private suspend fun requestToolPermission(toolName: String): ToolDecision {
        return suspendCancellableCoroutine { cont ->
            toolContinuation = cont
            emit(currentState.copy(
                toolRequest = toolName,
                requestInProgress = false
            ))
        }
    }

    fun resolveToolPermission(allow: Boolean, always: Boolean) {
        toolContinuation?.resume(ToolDecision(allow, always))
        toolContinuation = null
        emit(currentState.copy(toolRequest = null))
    }

    fun toggleAutoApproveTools() {
        emit(currentState.copy(autoApproveTools = !currentState.autoApproveTools))
    }

    private suspend fun streamChat(
        model: StreamingChatModel,
        request: ChatRequest,
        chatId: String,
        preset: Preset
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
                        emit(currentState.copy(
                            messages = msgs,
                            isStreaming = true,
                            requestInProgress = true
                        ))
                    }
                }

                override fun onCompleteResponse(chatResponse: ChatResponse) {
                    if (!cont.isActive) return
                    val lastUsage = chatResponse.tokenUsage().toInfo()
                    val msgs = currentState.messages.toMutableList()
                    val lastIndex = msgs.lastIndex
                    if (lastIndex >= 0) {
                        msgs[lastIndex] = ChatRepositoryMessage(
                            chatId,
                            chatResponse.aiMessage(),
                            preset.model,
                            tokenUsage = lastUsage,
                        )
                        emit(
                            currentState.copy(
                                messages = msgs,
                                tokenUsage = currentState.tokenUsage + lastUsage,
                                isStreaming = false,
                                requestInProgress = false
                            )
                        )
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

    suspend fun deleteFrom(index: Int) {
        stop()
        val chatId = currentState.chatId
        chatRepository.deleteMessagesFrom(chatId, index)
        loadChat(chatId)
    }

    fun stop() {
        currentContinuation?.cancel()
        currentContinuation = null
        emit(currentState.copy(requestInProgress = false))
    }
}
