package io.qent.sona.repositories

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import io.qent.sona.config.SonaConfig
import io.qent.sona.core.mcp.McpServerConfig
import io.qent.sona.core.mcp.McpServersRepository
import java.io.File
import kotlinx.coroutines.runBlocking

private val JETBRAINS_MCP_ARGS = listOf("-y", "@jetbrains/mcp-proxy")
private val MEMORY_MCP_ARGS = listOf("-y", "@modelcontextprotocol/server-memory")

@Service(Service.Level.PROJECT)
class PluginMcpServersRepository(private val project: Project) : McpServersRepository {

    private val root = project.basePath ?: "/"

    override suspend fun list(): List<McpServerConfig> {
        val servers = SonaConfig.load(root)?.mcpServers ?: emptyMap()
        val result = servers.map { (name, server) ->
            McpServerConfig(
                name = name,
                command = server.command,
                args = server.args,
                env = server.env,
                transport = server.transport ?: "stdio",
                url = server.url,
                cwd = server.cwd,
                headers = server.headers,
            )
        }.toMutableList()

        if (result.none { it.args?.joinToString() == JETBRAINS_MCP_ARGS.joinToString() }) {
            result.add(
                0, McpServerConfig(
                    name = "@jetbrains/mcp-proxy",
                    command = "npx",
                    args = JETBRAINS_MCP_ARGS,
                    transport = "stdio"
                )
            )
        }

        if (result.none { it.args?.joinToString() == MEMORY_MCP_ARGS.joinToString() }) {
            result.add(
                McpServerConfig(
                    name = "memory",
                    command = "npx",
                    args = MEMORY_MCP_ARGS,
                    env = mapOf("MEMORY_FILE_PATH" to File(root, ".sona/sona_memory.json").absolutePath),
                    transport = "stdio"
                )
            )
        }

        return result
    }

    override suspend fun loadEnabled(): Set<String> {
        val servers = SonaConfig.load(root)?.mcpServers ?: emptyMap()
        return servers.filter { it.value.enabled != false }.keys.toSet()
    }

    override suspend fun saveEnabled(enabled: Set<String>) {
        val file = File(root, ".sona/sona.json")
        if (!file.exists()) return
        val config = SonaConfig.load(root) ?: SonaConfig()
        val servers = config.mcpServers?.toMutableMap() ?: mutableMapOf()
        servers.forEach { (name, server) ->
            server.enabled = enabled.contains(name)
        }
        config.mcpServers = servers
        SonaConfig.save(root, config)
    }

    override suspend fun loadDisabledTools(): Map<String, Set<String>> {
        val servers = SonaConfig.load(root)?.mcpServers ?: emptyMap()
        return servers.mapValues { it.value.disabledTools?.toSet() ?: emptySet() }
    }

    override suspend fun saveDisabledTools(disabled: Map<String, Set<String>>) {
        val file = File(root, ".sona/sona.json")
        if (!file.exists()) return
        val config = SonaConfig.load(root) ?: SonaConfig()
        val servers = config.mcpServers?.toMutableMap() ?: mutableMapOf()
        disabled.forEach { (name, set) ->
            val server = servers[name] ?: SonaConfig.McpServer()
            server.disabledTools = set.toList()
            servers[name] = server
        }
        config.mcpServers = servers
        SonaConfig.save(root, config)
    }

    fun openConfig() {
        val file = File(root, ".sona/sona.json")
        if (!file.exists()) {
            val enabledSet = runBlocking { loadEnabled() }
            val servers = runBlocking { list() }
            val perms = PluginFilePermissionsRepository(project)
            val config = SonaConfig()
            config.permissions = SonaConfig.Permissions().apply {
                files = SonaConfig.Permissions.Files().apply {
                    whitelist = perms.whitelist
                    blacklist = perms.blacklist
                }
            }
            config.mcpServers = servers.associate { server ->
                server.name to SonaConfig.McpServer().apply {
                    enabled = enabledSet.contains(server.name)
                    command = server.command
                    args = server.args
                    env = server.env
                    transport = server.transport
                    url = server.url
                    cwd = server.cwd
                    headers = server.headers
                }
            }.toMutableMap()
            SonaConfig.save(root, config)
        }
        val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (vFile != null) {
            FileEditorManager.getInstance(project).openFile(vFile, true)
        }
    }
}