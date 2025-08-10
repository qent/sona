package io.qent.sona.core.chat

import com.google.gson.Gson
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.tool.ToolExecution
import io.qent.sona.core.mcp.McpConnectionManager
import io.qent.sona.core.model.SonaAiService
import io.qent.sona.core.model.toInfo
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.settings.SettingsRepository
import io.qent.sona.core.tools.Tools
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector


class ChatFlow(
    private val presetsRepository: PresetsRepository,
    private val rolesRepository: RolesRepository,
    private val chatRepository: ChatRepository,
    private val modelFactory: (Preset) -> StreamingChatModel,
    private val tools: Tools,
    scope: CoroutineScope,
    private val systemMessages: List<SystemMessage> = emptyList(),
    private val mcpManager: McpConnectionManager,
    private val settingsRepository: SettingsRepository,
) : Flow<Chat> {

    private val scope = scope + Dispatchers.IO

    private val chatStateFlow = ChatStateFlow(chatRepository, scope)
    private val permissionedToolExecutor = PermissionedToolExecutor(chatStateFlow, chatRepository)
    private val toolsMapFactory = ToolsMapFactory(tools, mcpManager, permissionedToolExecutor, rolesRepository)

    private val currentState get() = chatStateFlow.currentState

    private var currentStream: TokenStream? = null
    private var ignoreCallbacks = false

    suspend fun loadChat(chatId: String) = chatStateFlow.loadChat(chatId)

    fun resolveToolPermission(allow: Boolean, always: Boolean) {
        permissionedToolExecutor.resolveToolPermission(allow, always)
    }

    suspend fun send(text: String) {
        val chatId = currentState.chatId
        val preset = presetsRepository.load().let { it.presets[it.active] }

        try {
            val userMsg = UserMessage.from(text)
            chatRepository.addMessage(chatId, userMsg, preset.model)
            val userRepoMsg = ChatRepositoryMessage(chatId, userMsg, preset.model)
            val baseMessages = currentState.messages + userRepoMsg
            chatStateFlow.emit(currentState.copy(messages = baseMessages))

            val placeholder = ChatRepositoryMessage(chatId, AiMessage.from(""), preset.model)
            chatStateFlow.emit(
                currentState.copy(
                    requestInProgress = true,
                    messages = baseMessages + placeholder,
                    isStreaming = true
                )
            )

            val roles = rolesRepository.load()
            val roleText = roles.roles[roles.active].text
            val roleMessage = SystemMessage.from(roleText)
            val toolsMap = toolsMapFactory.create(chatId, preset.model)

            val maxRetries = settingsRepository.load().apiRetries
            val builder = StringBuilder()
            var attempt = 0

            fun startStream() {
                if (ignoreCallbacks) return
                val aiService = AiServices.builder(SonaAiService::class.java)
                    .streamingChatModel(modelFactory(preset))
                    .systemMessageProvider { (systemMessages + roleMessage).joinToString("\n") }
                    .chatMemoryProvider { id -> ChatRepositoryChatMemoryStore(chatRepository, id.toString()) }
                    .tools(toolsMap)
                    .build()

                builder.setLength(0)
                val msgs = currentState.messages.toMutableList()
                val lastIdx = msgs.lastIndex
                if (lastIdx >= 0) {
                    msgs[lastIdx] = msgs[lastIdx].copy(message = AiMessage.from(""))
                    chatStateFlow.emit(currentState.copy(messages = msgs, isStreaming = true, requestInProgress = true))
                }

                ignoreCallbacks = false
                currentStream = aiService.chat(chatId, text)
                    .onPartialResponse { token ->
                        if (ignoreCallbacks) return@onPartialResponse
                        builder.append(token)
                        val msgs = currentState.messages.toMutableList()
                        val lastIndex = msgs.lastIndex
                        if (lastIndex >= 0) {
                            msgs[lastIndex] = msgs[lastIndex].copy(
                                message = AiMessage.from(builder.toString())
                            )
                            chatStateFlow.emit(currentState.copy(messages = msgs, isStreaming = true, requestInProgress = true))
                        }
                    }
                    .onToolExecuted { exec: ToolExecution ->
                        if (ignoreCallbacks) return@onToolExecuted
                        val toolResultMessage = ToolExecutionResultMessage(exec.request().id(), exec.request().name(), exec.result())
                        val repositoryMessage = ChatRepositoryMessage(chatId, toolResultMessage, preset.model)

                        runBlocking {
                            chatRepository.addMessage(chatId, toolResultMessage, preset.model)
                        }

                        val messages = currentState.messages.toMutableList()
                        val idx = messages.indexOfLast {
                            val m = it.message
                            m is ToolExecutionResultMessage && m.id() == exec.request().id()
                        }
                        if (idx >= 0) {
                            messages[idx] = repositoryMessage
                        } else {
                            messages += repositoryMessage
                        }
                        val placeholderAfter = ChatRepositoryMessage(chatId, AiMessage.from(""), preset.model)
                        builder.setLength(0)
                        chatStateFlow.emit(
                            currentState.copy(
                                messages = messages + placeholderAfter,
                                requestInProgress = true,
                                isStreaming = false
                            )
                        )
                    }
                    .onCompleteResponse { response ->
                        if (ignoreCallbacks) return@onCompleteResponse
                        val totalUsage = response.metadata().tokenUsage().toInfo()
                        val msgs = currentState.messages.toMutableList()
                        val lastIndex = msgs.lastIndex
                        if (lastIndex >= 0) {
                            msgs[lastIndex] = ChatRepositoryMessage(chatId, response.aiMessage(), preset.model, totalUsage)
                        }
                        runBlocking { chatRepository.addMessage(chatId, response.aiMessage(), preset.model, totalUsage) }
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
                        if (ignoreCallbacks) return@onError
                        if (t is CancellationException) {
                            currentStream = null
                            chatStateFlow.emit(currentState.copy(requestInProgress = false, isStreaming = false))
                            return@onError
                        }
                        currentStream = null
                        if (attempt < maxRetries) {
                            val delayMs = 1000L * (1 shl attempt)
                            attempt++
                            scope.launch {
                                delay(delayMs)
                                if (!ignoreCallbacks) startStream()
                            }
                            chatStateFlow.emit(currentState.copy(requestInProgress = true, isStreaming = false))
                        } else {
                            val errMsg = ChatRepositoryMessage(chatId, AiMessage.from("Error: ${t.message}"), preset.model)
                            runBlocking { chatRepository.addMessage(chatId, errMsg.message, preset.model) }
                            val messages = currentState.messages.toMutableList()
                            if (messages.isNotEmpty()) {
                                messages[messages.lastIndex] = errMsg
                            } else {
                                messages += errMsg
                            }
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
        } catch (e: CancellationException) {
            currentStream = null
            chatStateFlow.emit(currentState.copy(requestInProgress = false, isStreaming = false))
        } catch (e: Exception) {
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

    override suspend fun collect(collector: FlowCollector<Chat>) {
        chatStateFlow.collect(collector)
    }

    fun toggleAutoApproveTools() {
        chatStateFlow.emit(currentState.copy(autoApproveTools = !currentState.autoApproveTools))
    }

    suspend fun deleteFrom(index: Int) {
        stop()
        val chatId = currentState.chatId
        chatRepository.deleteMessagesFrom(chatId, index)
        chatStateFlow.loadChat(chatId)
    }

    fun stop() {
        ignoreCallbacks = true
        currentStream?.ignoreErrors()
        currentStream = null
        chatStateFlow.emit(currentState.copy(requestInProgress = false, isStreaming = false))
    }
}
