package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import io.qent.sona.config.SonaConfig
import io.qent.sona.core.mcp.McpServerConfig
import io.qent.sona.core.mcp.McpServersRepository

private val JETBRAINS_MCP_ARGS = listOf("-y", "@jetbrains/mcp-proxy")

@Service(Service.Level.PROJECT)
@State(name = "PluginMcpServersRepository", storages = [Storage("mcp_servers.xml")])
class PluginMcpServersRepository(private val project: Project) : McpServersRepository,
    PersistentStateComponent<PluginMcpServersRepository.State> {

    data class State(
        var enabled: MutableSet<String> = mutableSetOf(),
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    private val root = project.basePath ?: "/"
    override suspend fun list(): List<McpServerConfig> {
        val servers = SonaConfig.load(root)?.mcpServers ?: emptyList()
        val result = servers.mapNotNull { server ->
            val name = server.name ?: return@mapNotNull null
            McpServerConfig(
                name = name,
                command = server.command,
                args = server.args,
                env = server.env,
                transport = server.transport,
                url = server.url,
                cwd = server.cwd,
                headers = server.headers,
            )
        }.toMutableList()

        if (result.find { it.args?.joinToString() == JETBRAINS_MCP_ARGS.joinToString() } == null) {
            result.add(
                0, McpServerConfig(
                    name = "@jetbrains/mcp-proxy",
                    command = "npx",
                    args = JETBRAINS_MCP_ARGS,
                    transport = "stdio"
                )
            )
        }

        return result
    }

    override suspend fun loadEnabled(): Set<String> = state.enabled

    override suspend fun saveEnabled(enabled: Set<String>) {
        state.enabled = enabled.toMutableSet()
    }
}