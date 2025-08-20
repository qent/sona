package io.qent.sona.core.search

import com.google.gson.Gson
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
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

    private val gson = Gson()
    private val systemRolePrompt = runCatching {
        java.nio.file.Files.readString(java.nio.file.Paths.get("prompts/agents/search.md"))
    }.getOrDefault("")
    private val toolsMap = mutableMapOf<ToolSpecification, ToolExecutor>()

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
                val args = gson.fromJson(request.arguments(), Map::class.java) as Map<String, Any?>
                gson.toJson(executor(args))
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

        return AiServices.builder(SearchAiService::class.java)
            .streamingChatModel(modelFactory(preset))
            .systemMessageProvider { systemRolePrompt }
            .tools(toolsMap)
            .build()
    }
}