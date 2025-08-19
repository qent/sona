package io.qent.sona.core.chat

import com.google.gson.Gson
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.service.tool.ToolExecutor
import io.qent.sona.core.Logger
import io.qent.sona.core.model.TokenUsageInfo
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ToolDecision(val allow: Boolean, val always: Boolean)

class PermissionedToolExecutor(
    private val chatStateFlow: ChatStateFlow,
    private val chatRepository: ChatRepository,
    private val log: Logger = Logger.NoOp,
) {

    private val currentChatState get() = chatStateFlow.currentState

    private var toolContinuation: CancellableContinuation<ToolDecision>? = null

    fun resolveToolPermission(allow: Boolean, always: Boolean) {
        log.log("resolveToolPermission: allow=$allow always=$always")
        toolContinuation?.resume(ToolDecision(allow, always))
        toolContinuation = null
        log.log("emit: tool request = null")
        chatStateFlow.emit(currentChatState.copy(toolRequest = null, pendingPatch = null, requestInProgress = true))
    }

    fun create(
        chatId: String,
        model: String,
        run: (ToolExecutionRequest) -> String
    ) = ToolExecutor { request, memoryId ->
        val name = request.name()
        log.log("tool execute request: $name")
        runBlocking {
            // fix empty lastAiMessage tools
            val messages = currentChatState.messages.toMutableList()
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
                log.log("emit: fixed message with tools request")
                chatStateFlow.emit(currentChatState.copy(messages = messages))
            }
        }

        val decision = runBlocking {
            if (currentChatState.autoApproveTools || chatRepository.isToolAllowed(chatId, name)) {
                log.log("autoApproveTools = ${currentChatState.autoApproveTools} or tool allowed at chat")
                ToolDecision(true, false)
            } else {
                requestToolPermission(request)
            }
        }
        log.log("tool decision: allow=${decision.allow} always=${decision.always}")
        if (decision.always) {
            runBlocking {
                log.log("add allowed tool")
                chatRepository.addAllowedTool(chatId, name)
            }
        }
        if (decision.allow) {
            log.log("emit: tool placeholder")
            chatStateFlow.emit(
                currentChatState.copy(
                    messages = currentChatState.messages + ChatRepositoryMessage(
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

    private val gson = Gson()

    private suspend fun requestToolPermission(request: ToolExecutionRequest): ToolDecision {
        val toolName = request.name()
        log.log("requestToolPermission: $toolName")
        val patch = if (toolName == "applyPatch") {
            try {
                val args = gson.fromJson(request.arguments(), Map::class.java) as Map<*, *>
                args["arg0"]?.toString()
            } catch (_: Exception) {
                null
            }
        } else null
        return suspendCancellableCoroutine { cont ->
            toolContinuation = cont
            log.log("emit: tool request = $toolName")
            chatStateFlow.emit(
                currentChatState.copy(
                    toolRequest = toolName,
                    pendingPatch = patch,
                    requestInProgress = false
                )
            )
        }
    }
}
