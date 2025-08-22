package io.qent.sona.repositories

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
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

    private fun role(): String = runBlocking {
        project.service<PluginRolesRepository>().load().let { it.roles[it.active].name }
    }

    override suspend fun list(): List<McpServerConfig> {
        val servers = SonaConfig.load(root, role())?.mcpServers ?: emptyMap()
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
        val servers = SonaConfig.load(root, role())?.mcpServers ?: emptyMap()
        return servers.filter { it.value.enabled != false }.keys.toSet()
    }

    override suspend fun saveEnabled(enabled: Set<String>) {
        val r = role()
        val config = SonaConfig.load(root, r) ?: SonaConfig()
        val servers = config.mcpServers?.toMutableMap() ?: mutableMapOf()
        val defaults = list().associateBy { it.name }

        (servers.keys + defaults.keys + enabled).forEach { name ->
            val server = servers[name] ?: SonaConfig.McpServer().apply {
                defaults[name]?.let { d ->
                    command = d.command
                    args = d.args
                    env = d.env
                    transport = d.transport
                    url = d.url
                    cwd = d.cwd
                    headers = d.headers
                }
            }
            server.enabled = enabled.contains(name)
            servers[name] = server
        }

        config.mcpServers = servers
        SonaConfig.save(root, config, r)
    }

    override suspend fun loadDisabledTools(): Map<String, Set<String>> {
        val servers = SonaConfig.load(root, role())?.mcpServers ?: emptyMap()
        return servers.mapValues { it.value.disabledTools?.toSet() ?: emptySet() }
    }

    override suspend fun saveDisabledTools(disabled: Map<String, Set<String>>) {
        val r = role()
        val config = SonaConfig.load(root, r) ?: SonaConfig()
        val servers = config.mcpServers?.toMutableMap() ?: mutableMapOf()
        val defaults = list().associateBy { it.name }

        (servers.keys + defaults.keys + disabled.keys).forEach { name ->
            val server = servers[name] ?: SonaConfig.McpServer().apply {
                defaults[name]?.let { d ->
                    command = d.command
                    args = d.args
                    env = d.env
                    transport = d.transport
                    url = d.url
                    cwd = d.cwd
                    headers = d.headers
                }
            }
            disabled[name]?.let { set -> server.disabledTools = set.toList() }
            servers[name] = server
        }

        config.mcpServers = servers
        SonaConfig.save(root, config, r)
    }

    fun openConfig() {
        val r = role()
        val file = SonaConfig.path(root, r)
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
            SonaConfig.save(root, config, r)
        }
        val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (vFile != null) {
            FileEditorManager.getInstance(project).openFile(vFile, true)
        }
    }
}