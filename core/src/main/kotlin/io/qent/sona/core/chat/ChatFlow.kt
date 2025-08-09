package io.qent.sona.core.chat

import com.google.gson.Gson
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.service.tool.ToolExecutor
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.tool.ToolExecution
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import io.qent.sona.core.mcp.McpConnectionManager
import io.qent.sona.core.model.TokenUsageInfo
import io.qent.sona.core.model.SonaAiService
import io.qent.sona.core.model.toInfo
import io.qent.sona.core.repositories.ChatRepositoryChatMemoryStore
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.tools.Tools
import io.qent.sona.core.settings.SettingsRepository


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
    private val settingsRepository: SettingsRepository,
) : Flow<Chat> {

    private val scope = scope + Dispatchers.IO

    private val mutableSharedState = MutableSharedFlow<Chat>()
    private var currentState = Chat("", TokenUsageInfo())
    private var currentStream: TokenStream? = null
    private var ignoreCallbacks = false
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
        val gson = Gson()
        try {
            val userMsg = UserMessage.from(text)
            chatRepository.addMessage(chatId, userMsg, preset.model)
            val userRepoMsg = ChatRepositoryMessage(chatId, userMsg, preset.model)
            val baseMessages = currentState.messages + userRepoMsg
            emit(currentState.copy(messages = baseMessages))

            val placeholder = ChatRepositoryMessage(chatId, AiMessage.from(""), preset.model)
            emit(
                currentState.copy(
                    requestInProgress = true,
                    messages = baseMessages + placeholder,
                    isStreaming = true
                )
            )

            val roles = rolesRepository.load()
            val roleText = roles.roles[roles.active].text
            val roleMessage = SystemMessage.from(roleText)
            val roleDescriptions = roles.roles.joinToString("\n") { "name = '${it.name}' (${it.short})" }

            val roleSpec = ToolSpecification.builder()
                .name("switchRole")
                .description("Switch agent role by name. Available roles: \n$roleDescriptions")
                .parameters(
                    JsonObjectSchema.builder()
                        .addStringProperty("name", "Role name")
                        .required("name")
                        .build()
                )
                .build()

            val toolSpecs = ToolSpecifications.toolSpecificationsFrom(tools).toMutableList().apply { add(roleSpec) } + mcpManager.listTools()
            val toolMap = toolSpecs.associateWith { spec: ToolSpecification ->
                PermissionedToolExecutor(chatId, preset.model, spec.name()) { req ->
                    when (spec.name()) {
                        "getFocusedFileText" -> tools.getFocusedFileText()
                        "readFile" -> {
                            val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                            val path = args["arg0"]?.toString() ?: ""
                            tools.readFile(path)
                        }
                        "switchRole" -> {
                            val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                            val name = args["name"]?.toString() ?: ""
                            tools.switchRole(name)
                        }
                        else -> runBlocking { mcpManager.execute(req.id(), spec.name(), req.arguments()) }
                    }
                }
            }

            val maxRetries = settingsRepository.load().apiRetries
            val builder = StringBuilder()
            var attempt = 0

            fun startStream() {
                if (ignoreCallbacks) return
                val aiService = AiServices.builder(SonaAiService::class.java)
                    .streamingChatModel(modelFactory(preset))
                    .systemMessageProvider { (systemMessages + roleMessage).joinToString("\n") }
                    .chatMemoryProvider { id -> ChatRepositoryChatMemoryStore(chatRepository, id.toString()) }
                    .tools(toolMap)
                    .build()

                builder.setLength(0)
                val msgs = currentState.messages.toMutableList()
                val lastIdx = msgs.lastIndex
                if (lastIdx >= 0) {
                    msgs[lastIdx] = msgs[lastIdx].copy(message = AiMessage.from(""))
                    emit(currentState.copy(messages = msgs, isStreaming = true, requestInProgress = true))
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
                            emit(currentState.copy(messages = msgs, isStreaming = true, requestInProgress = true))
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
                        emit(
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
                        emit(
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
                            emit(currentState.copy(requestInProgress = false, isStreaming = false))
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
                            emit(currentState.copy(requestInProgress = true, isStreaming = false))
                        } else {
                            val errMsg = ChatRepositoryMessage(chatId, AiMessage.from("Error: ${t.message}"), preset.model)
                            runBlocking { chatRepository.addMessage(chatId, errMsg.message, preset.model) }
                            val messages = currentState.messages.toMutableList()
                            if (messages.isNotEmpty()) {
                                messages[messages.lastIndex] = errMsg
                            } else {
                                messages += errMsg
                            }
                            emit(
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
            emit(currentState.copy(requestInProgress = false, isStreaming = false))
        } catch (e: Exception) {
            val errorMessage = ChatRepositoryMessage(
                chatId,
                AiMessage.from("Error: ${e.message}\n\n${e.stackTrace}"),
                preset.model
            )
            emit(
                currentState.copy(
                    messages = currentState.messages + errorMessage,
                    requestInProgress = false,
                    isStreaming = false
                )
            )
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
        emit(currentState.copy(toolRequest = null, requestInProgress = true))
    }

    fun toggleAutoApproveTools() {
        emit(currentState.copy(autoApproveTools = !currentState.autoApproveTools))
    }

    inner class PermissionedToolExecutor(
        private val chatId: String,
        private val model: String,
        private val name: String,
        private val run: (ToolExecutionRequest) -> String,
    ) : ToolExecutor {
        override fun execute(request: ToolExecutionRequest, memoryId: Any?): String {
            runBlocking {
                // fix empty lastAiMessage tools
                val messages = currentState.messages.toMutableList()
                messages.lastOrNull { it.message is AiMessage }?.let { lastAiMessage ->
                    val tools = (lastAiMessage.message as AiMessage).toolExecutionRequests().toMutableList()
                    tools.add(request)
                    val messageWithRequests = AiMessage(lastAiMessage.message.text(), tools)
                    chatRepository.addMessage(chatId, messageWithRequests, model, TokenUsageInfo())

                    messages[messages.indexOf(lastAiMessage)] = ChatRepositoryMessage(
                        lastAiMessage.chatId,
                        messageWithRequests,
                        lastAiMessage.model,
                        lastAiMessage.tokenUsage
                    )
                    emit(currentState.copy(messages = messages))
                }
            }

            val decision = runBlocking {
                if (currentState.autoApproveTools || chatRepository.isToolAllowed(chatId, name)) {
                    ToolDecision(true, false)
                } else {
                    requestToolPermission(name)
                }
            }
            if (decision.always) {
                runBlocking { chatRepository.addAllowedTool(chatId, name) }
            }
            return if (decision.allow) {
                emit(
                    currentState.copy(
                        messages = currentState.messages + ChatRepositoryMessage(
                            chatId,
                            ToolExecutionResultMessage(request.id(), name, "executing"),
                            model
                        ),
                        requestInProgress = true,
                        isStreaming = false
                    )
                )
                run(request)
            } else "Tool execution cancelled"
        }
    }

    suspend fun deleteFrom(index: Int) {
        stop()
        val chatId = currentState.chatId
        chatRepository.deleteMessagesFrom(chatId, index)
        loadChat(chatId)
    }

    fun stop() {
        ignoreCallbacks = true
        currentStream?.ignoreErrors()
        currentStream = null
        emit(currentState.copy(requestInProgress = false, isStreaming = false))
    }


}
