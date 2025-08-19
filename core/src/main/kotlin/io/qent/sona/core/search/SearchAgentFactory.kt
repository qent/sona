package io.qent.sona.core.search

import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.tool.ToolExecutor
import io.qent.sona.core.model.SearchAiService
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.tools.ExternalTools

class SearchAgentFactory(
    private val modelFactory: (Preset) -> StreamingChatModel,
    private val preset: Preset,
    private val externalTools: ExternalTools,
) {

    private val systemRolePrompt = "" // TODO read prompt from /prompts/agents/search.md
    private val toolsMap = mutableMapOf<ToolSpecification, ToolExecutor>()

    init {
        // TODO fill tool map by external search tools
        // externalTools.findFilesByNames()
        // externalTools.findText()
        // externalTools.findClasses()
    }

    suspend fun create(): SearchAiService {

        return AiServices.builder(SearchAiService::class.java)
            .streamingChatModel(modelFactory(preset))
            .systemMessageProvider { systemRolePrompt }
            .tools(toolsMap)
            .build()
    }
}