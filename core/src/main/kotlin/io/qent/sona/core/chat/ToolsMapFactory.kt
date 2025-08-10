package io.qent.sona.core.chat

import com.google.gson.Gson
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.service.tool.ToolExecutor
import io.qent.sona.core.mcp.McpConnectionManager
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.tools.Tools
import kotlinx.coroutines.runBlocking

class ToolsMapFactory(
    private val chatStateFlow: ChatStateFlow,
    private val tools: Tools,
    private val mcpManager: McpConnectionManager,
    private val permissionedToolExecutor: PermissionedToolExecutor,
    private val rolesRepository: RolesRepository,
    private val presetsRepository: PresetsRepository,
) {

    private val gson = Gson()

    suspend fun create(): Map<ToolSpecification, ToolExecutor> {
        val preset = presetsRepository.load().let { it.presets[it.active] }
        val specifications = ToolSpecifications.toolSpecificationsFrom(tools).toMutableList().apply {
            add(createSwitchRolesToolSpecification())
        } + mcpManager.listTools()

        return specifications.associateWith { spec: ToolSpecification ->
            permissionedToolExecutor.create(chatStateFlow.currentState.chatId, preset.model, spec.name()) { req ->
                when (spec.name()) {
                    "getFocusedFileInfo" -> gson.toJson(tools.getFocusedFileInfo())
                    "getFileLines" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val path = args["arg0"]?.toString() ?: return@create "Empty file path"
                        val from = (args["arg1"] as? Number)?.toInt() ?: 0
                        val to = (args["arg2"] as? Number)?.toInt() ?: 0
                        tools.getFileLines(path, from, to)
                    }

                    "readFile" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val path = args["arg0"]?.toString() ?: return@create "Empty file path"
                        tools.readFile(path)
                    }

                    "switchRole" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val name = args["name"]?.toString() ?: return@create "Empty role name"
                        tools.switchRole(name)
                    }

                    "applyPatch" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val path = args["arg0"]?.toString() ?: return@create " Empty patch"
                        tools.applyPatch(path)
                    }

                    else -> runBlocking {
                        mcpManager.execute(req.id(), spec.name(), req.arguments())
                    }
                }
            }
        }
    }

    private suspend fun createSwitchRolesToolSpecification(): ToolSpecification {
        val roleDescriptions = rolesRepository.load().roles.joinToString("\n") { "name = '${it.name}' (${it.short})" }
        return ToolSpecification.builder()
            .name("switchRole")
            .description("Switch agent role by name. Available roles: \n$roleDescriptions")
            .parameters(
                JsonObjectSchema.builder()
                    .addStringProperty("name", "Role name")
                    .required("name")
                    .build()
            )
            .build()
    }
}
