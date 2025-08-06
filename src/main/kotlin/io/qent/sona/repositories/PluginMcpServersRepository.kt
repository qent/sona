package io.qent.sona.repositories

import com.intellij.openapi.project.Project
import io.qent.sona.config.SonaConfig
import io.qent.sona.core.McpServerConfig
import io.qent.sona.core.McpServersRepository

private val JETBRAINS_MCP_ARGS = listOf("-y", "@jetbrains/mcp-proxy")

class PluginMcpServersRepository(project: Project) : McpServersRepository {
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
}