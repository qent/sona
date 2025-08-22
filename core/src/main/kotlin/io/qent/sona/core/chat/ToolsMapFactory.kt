package io.qent.sona.core.chat

import com.google.gson.Gson
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.service.tool.ToolExecutor
import io.qent.sona.core.mcp.McpConnectionManager
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.roles.DefaultRoles
import io.qent.sona.core.tools.Tools
import io.qent.sona.core.settings.SettingsRepository
import kotlinx.coroutines.runBlocking

class ToolsMapFactory(
    private val chatStateFlow: ChatStateFlow,
    private val tools: Tools,
    private val mcpManager: McpConnectionManager,
    private val permissionedToolExecutor: PermissionedToolExecutor,
    private val rolesRepository: RolesRepository,
    private val presetsRepository: PresetsRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val gson = Gson()

    suspend fun create(): Map<ToolSpecification, ToolExecutor> {
        val preset = presetsRepository.load().let { it.presets[it.active] }
        val currentRole = rolesRepository.load().let { it.roles[it.active].name }
        val baseSpecs = ToolSpecifications.toolSpecificationsFrom(tools).toMutableList().apply {
            add(createSwitchRolesToolSpecification())
        }
        val roleFiltered = if (currentRole == DefaultRoles.MANAGER.displayName) {
            baseSpecs.filter { it.name() in setOf("getFocusedFileInfo", "switchRole") }
        } else {
            baseSpecs
        }
        val useSearchAgent = settingsRepository.load().useSearchAgent
        val filtered = roleFiltered.filter { spec ->
            if (useSearchAgent) {
                spec.name() !in setOf("findFilesByNames", "findClasses", "findText")
            } else {
                spec.name() != "search"
            }
        }
        val specifications = if (currentRole == DefaultRoles.MANAGER.displayName) {
            filtered
        } else {
            filtered + mcpManager.listTools()
        }

        return specifications.associateWith { spec: ToolSpecification ->
            permissionedToolExecutor.create(chatStateFlow.currentState.chatId, preset.model) { req ->
                when (spec.name()) {
                    "getFocusedFileInfo" -> gson.toJson(tools.getFocusedFileInfo())
                    "getFileLines" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val path = args["arg0"]?.toString() ?: return@create "Empty file path"
                        val from = (args["arg1"] as? Number)?.toInt() ?: 0
                        val to = (args["arg2"] as? Number)?.toInt() ?: 0
                        tools.getFileLines(path, from, to)
                    }

                    "listPath" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val path = args["arg0"]?.toString() ?: return@create "Empty path"
                        gson.toJson(tools.listPath(path))
                    }

                    "getFileDependencies" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val path = args["arg0"]?.toString() ?: return@create "Empty file path"
                        gson.toJson(tools.getFileDependencies(path))
                    }

                    "search" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val query = args["arg0"]?.toString() ?: return@create "Empty search request"
                        gson.toJson(tools.search(query))
                    }
                    "findFilesByNames" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val pattern = args["arg0"]?.toString() ?: return@create "Empty pattern"
                        val offset = (args["arg1"] as? Number)?.toInt() ?: 0
                        val limit = (args["arg2"] as? Number)?.toInt() ?: 3
                        gson.toJson(tools.findFilesByNames(pattern, offset, limit))
                    }
                    "findClasses" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val pattern = args["arg0"]?.toString() ?: return@create "Empty pattern"
                        val offset = (args["arg1"] as? Number)?.toInt() ?: 0
                        val limit = (args["arg2"] as? Number)?.toInt() ?: 3
                        gson.toJson(tools.findClasses(pattern, offset, limit))
                    }
                    "findText" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val pattern = args["arg0"]?.toString() ?: return@create "Empty pattern"
                        val offset = (args["arg1"] as? Number)?.toInt() ?: 0
                        val limit = (args["arg2"] as? Number)?.toInt() ?: 3
                        gson.toJson(tools.findText(pattern, offset, limit))
                    }

                    "sendTerminalCommand" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val command = args["arg0"]?.toString() ?: return@create "Empty terminal command"
                        gson.toJson(tools.sendTerminalCommand(command))
                    }

                    "readTerminalOutput" -> {
                        gson.toJson(tools.readTerminalOutput())
                    }

                    "switchRole" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val name = args["name"]?.toString() ?: return@create "Empty role name"
                        tools.switchRole(name)
                    }

                    "applyPatch" -> {
                        val args = gson.fromJson(req.arguments(), Map::class.java) as Map<*, *>
                        val patch = args["arg0"]?.toString() ?: return@create " Empty patch"
                        tools.applyPatch(patch)
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
