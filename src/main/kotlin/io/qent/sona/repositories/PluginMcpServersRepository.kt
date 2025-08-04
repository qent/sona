package io.qent.sona.repositories

import com.intellij.openapi.project.Project
import io.qent.sona.config.SonaConfig
import io.qent.sona.core.McpServerConfig
import io.qent.sona.core.McpServersRepository

class PluginMcpServersRepository(project: Project) : McpServersRepository {
    private val root = project.basePath ?: "/"
    private val config = SonaConfig.load(root)

    override suspend fun list(): List<McpServerConfig> {
        val servers = config?.mcpServers ?: return emptyList()
        return servers.mapNotNull { server ->
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
        }
    }
}
