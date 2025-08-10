package io.qent.sona.core.chat

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.service.AiServices
import io.qent.sona.core.model.SonaAiService
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.roles.RolesRepository

class ChatAgentFactory(
    private val modelFactory: (Preset) -> StreamingChatModel,
    private val systemMessages: List<SystemMessage> = emptyList(),
    private val toolsMapFactory: ToolsMapFactory,
    private val presetsRepository: PresetsRepository,
    private val rolesRepository: RolesRepository,
    private val chatRepository: ChatRepository,
) {
    suspend fun create(): SonaAiService {
        val preset = presetsRepository.load().let { it.presets[it.active] }
        val roles = rolesRepository.load()
        val roleText = roles.roles[roles.active].text
        val roleMessage = SystemMessage.from(roleText)
        val toolsMap = toolsMapFactory.create()

        return AiServices.builder(SonaAiService::class.java)
            .streamingChatModel(modelFactory(preset))
            .systemMessageProvider { (systemMessages + roleMessage).joinToString("\n") }
            .chatMemoryProvider { id -> ChatRepositoryChatMemoryStore(chatRepository, id.toString()) }
            .tools(toolsMap)
            .build()
    }
}