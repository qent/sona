package io.qent.sona.core.search

import com.google.gson.Gson
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.internal.JsonSchemaElementUtils
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequestParameters
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.chat.request.ResponseFormatType
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonSchema
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.tool.ToolExecutor
import io.qent.sona.core.data.SearchResult
import io.qent.sona.core.model.SearchAiService
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.tools.ExternalTools
import java.nio.file.Files

class SearchAgentFactory(
    private val modelFactory: (Preset) -> ChatModel,
    private val preset: Preset,
    private val externalTools: ExternalTools,
    private val onMessage: (ChatMessage) -> Unit = {},
) {

    private val gson = Gson()
    private val systemRolePrompt = loadResourceText("prompts/agents/search.md")
    private val toolsMap = mutableMapOf<ToolSpecification, ToolExecutor>()

    private fun loadResourceText(path: String): String {
        val cl = SearchAgentFactory::class.java.classLoader
        return try {
            cl.getResourceAsStream(path)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: ""
        } catch (_: Throwable) {
            // Fallback for local dev/tests where resources might not be on the classpath
            runCatching {
                Files.readString(java.nio.file.Paths.get(path))
            }.getOrDefault("")
        }
    }

    init {
        fun addTool(
            name: String,
            description: String,
            schema: JsonObjectSchema,
            executor: (Map<String, Any?>) -> Any?
        ) {
            val spec = ToolSpecification.builder()
                .name(name)
                .description(description)
                .parameters(schema)
                .build()
            toolsMap[spec] = ToolExecutor { request, _ ->
                onMessage(AiMessage.from(request))
                val args = gson.fromJson(request.arguments(), Map::class.java) as Map<String, Any?>
                val result = gson.toJson(executor(args))
                onMessage(ToolExecutionResultMessage.from(request, result))
                result
            }
        }

        val patternSchema = JsonObjectSchema.builder()
            .addStringProperty("pattern", "Search pattern")
            .addIntegerProperty("offset", "Result offset")
            .addIntegerProperty("limit", "Maximum number of results")
            .required("pattern")
            .build()

        addTool(
            name = "findFilesByNames",
            description = "Find file paths whose names match the pattern",
            schema = patternSchema
        ) { args ->
            val pattern = args["pattern"]?.toString() ?: ""
            val offset = (args["offset"] as? Number)?.toInt() ?: 0
            val limit = (args["limit"] as? Number)?.toInt() ?: 3
            externalTools.findFilesByNames(pattern, offset, limit)
        }

        addTool(
            name = "findClasses",
            description = "Find classes matching the pattern and return their structure",
            schema = patternSchema
        ) { args ->
            val pattern = args["pattern"]?.toString() ?: ""
            val offset = (args["offset"] as? Number)?.toInt() ?: 0
            val limit = (args["limit"] as? Number)?.toInt() ?: 3
            externalTools.findClasses(pattern, offset, limit)
        }

        addTool(
            name = "findText",
            description = "Find text occurrences matching the pattern",
            schema = patternSchema
        ) { args ->
            val pattern = args["pattern"]?.toString() ?: ""
            val offset = (args["offset"] as? Number)?.toInt() ?: 0
            val limit = (args["limit"] as? Number)?.toInt() ?: 3
            externalTools.findText(pattern, offset, limit)
        }

        val pathSchema = JsonObjectSchema.builder()
            .addStringProperty("path", "Absolute path")
            .required("path")
            .build()

        addTool(
            name = "listPath",
            description = "List files and directories for the given path",
            schema = pathSchema
        ) { args ->
            val path = args["path"]?.toString() ?: ""
            externalTools.listPath(path)
        }

        val linesSchema = JsonObjectSchema.builder()
            .addStringProperty("path", "File path")
            .addIntegerProperty("fromLine", "Start line (1-based)")
            .addIntegerProperty("toLine", "End line (1-based)")
            .required("path")
            .required("fromLine")
            .required("toLine")
            .build()

        addTool(
            name = "getFileLines",
            description = "Read lines from a file",
            schema = linesSchema
        ) { args ->
            val path = args["path"]?.toString() ?: ""
            val from = (args["fromLine"] as? Number)?.toInt() ?: 0
            val to = (args["toLine"] as? Number)?.toInt() ?: 0
            externalTools.getFileLines(path, from, to)
        }

        addTool(
            name = "getFileDependencies",
            description = "Return direct dependencies of the file",
            schema = pathSchema
        ) { args ->
            val path = args["path"]?.toString() ?: ""
            externalTools.getFileDependencies(path)
        }
    }

    suspend fun create(): SearchAiService {

        val model = modelFactory(preset)
        val responseFormat = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(
                JsonSchema.builder()
                    .name("SearchResults")
                    .rootElement(
                        JsonSchemaElementUtils.jsonSchemaElementFrom(
                            Array<SearchResult>::class.java
                        )
                    )
                    .build()
            )
            .build()
        val requestParams = DefaultChatRequestParameters.builder()
            .responseFormat(responseFormat)
            .build()
        val modelWithFormat = object : ChatModel by model {
            override fun defaultRequestParameters(): ChatRequestParameters {
                return model.defaultRequestParameters().overrideWith(requestParams)
            }
        }

        return AiServices.builder(SearchAiService::class.java)
            .chatModel(modelWithFormat)
            .systemMessageProvider { systemRolePrompt }
            .tools(toolsMap)
            .build()
    }
}