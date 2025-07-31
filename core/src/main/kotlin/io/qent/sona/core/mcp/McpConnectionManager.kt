package io.qent.sona.core.mcp

import dev.langchain4j.agent.tool.ToolExecutionRequest as AgentToolExecutionRequest
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class McpConnectionManager(
    private val repository: McpServersRepository,
    scope: CoroutineScope
) {
    private val scope = scope + SupervisorJob() + Dispatchers.IO
    private val clients = mutableMapOf<String, McpClient>()
    private val tools = mutableMapOf<String, McpClient>()
    private val statuses = mutableMapOf<String, McpServerStatus>()
    private val enabled = mutableSetOf<String>()

    private val _servers = MutableStateFlow<List<McpServerStatus>>(emptyList())
    val servers: StateFlow<List<McpServerStatus>> = _servers.asStateFlow()

    private fun updateStatus(status: McpServerStatus) {
        synchronized(this) {
            statuses[status.name] = status
            _servers.value = statuses.values.toList()
        }
    }

    init {
        scope.launch {
            val configs = repository.list()
            var enabledNames = repository.loadEnabled()
            enabled += enabledNames
            configs.forEach { config ->
                if (enabled.contains(config.name)) {
                    connect(config)
                } else {
                    updateStatus(
                        McpServerStatus(
                            name = config.name,
                            status = McpServerStatus.Status.DISABLED,
                            tools = emptyList(),
                        )
                    )
                }
            }
        }
    }

    private fun connect(config: McpServerConfig) {
        updateStatus(
            McpServerStatus(
                name = config.name,
                status = McpServerStatus.Status.CONNECTING,
                tools = emptyList()
            )
        )
        scope.launch {
            runCatching {
                val client = createClient(config) ?: throw Exception("Invalid config")
                val specs = client.listTools()
                synchronized(this@McpConnectionManager) {
                    clients[config.name] = client
                    specs.forEach { spec ->
                        tools[spec.name()] = client
                    }
                }
                updateStatus(
                    McpServerStatus(
                        name = config.name,
                        status = McpServerStatus.Status.CONNECTED,
                        tools = specs
                    )
                )
            }.onFailure {
                updateStatus(
                    McpServerStatus(
                        name = config.name,
                        status = McpServerStatus.Status.FAILED(it as? Exception ?: Exception(it)),
                        tools = emptyList()
                    )
                )
                println("Failed to connect to MCP server ${config.name}: ${it.message}")
            }
        }
    }

    private fun disconnect(name: String) {
        val client = clients.remove(name)
        client?.close()
        tools.entries.removeIf { it.value == client }
    }


    fun toggle(name: String) {
        if (enabled.remove(name)) {
            disconnect(name)
            updateStatus(
                McpServerStatus(
                    name = name,
                    status = McpServerStatus.Status.DISABLED,
                    tools = emptyList(),
                )
            )
            scope.launch { repository.saveEnabled(enabled) }
        } else {
            enabled += name
            scope.launch {
                val config = repository.list().find { it.name == name }
                if (config != null) {
                    connect(config)
                } else {
                    enabled.remove(name)
                    updateStatus(
                        McpServerStatus(
                            name = name,
                            status = McpServerStatus.Status.FAILED(Exception("Config not found")),
                            tools = emptyList(),
                        )
                    )
                }
                repository.saveEnabled(enabled)
            }
        }
    }



    suspend fun reload() {
        clients.values.forEach { runCatching { it.close() } }
        clients.clear()
        tools.clear()
        statuses.clear()
        _servers.value = emptyList()

        val configs = repository.list()
        val stored = repository.loadEnabled()
        val configNames = configs.map { it.name }.toSet()
        enabled.clear()
        enabled.addAll(stored.intersect(configNames))
        repository.saveEnabled(enabled)

        configs.forEach { config ->
            if (enabled.contains(config.name)) {
                connect(config)
            } else {
                updateStatus(
                    McpServerStatus(
                        name = config.name,
                        status = McpServerStatus.Status.DISABLED,
                        tools = emptyList(),
                    )
                )
            }
        }
    }


    private fun createClient(config: McpServerConfig): DefaultMcpClient? {
        val transport = when (config.transport.lowercase()) {
            "stdio" -> {
                val cmd = buildList {
                    resolveCommand(config.command)?.let { add(it) }
                    config.args?.let { addAll(it) }
                }
                if (cmd.isEmpty()) return null
                StdioMcpTransport.Builder()
                    .command(cmd)
                    .environment(config.env ?: emptyMap())
                    .build()
            }
            "http" -> {
                val url = config.url ?: return null
                HttpMcpTransport.Builder()
                    .sseUrl(url)
                    .build()
            }
            else -> {
                println("Unsupported MCP transport: ${config.transport}")
                return null
            }
        }
        return DefaultMcpClient.Builder()
            .key(config.name)
            .transport(transport)
            .build()
    }

    private fun resolveCommand(command: String?): String? {
        command ?: return null
        return if (command.equals("npx", ignoreCase = true)) {
            findNpxExecutable() ?: command
        } else {
            command
        }
    }

    private fun findNpxExecutable(): String? {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        val isWindows = os.contains("win")
        val names = if (isWindows) listOf("npx.cmd", "npx.exe") else listOf("npx")
        val candidates = mutableListOf<File>()

        System.getenv("PATH")?.split(File.pathSeparator)?.forEach { dir ->
            names.forEach { name -> candidates += File(dir, name) }
        }

        if (isWindows) {
            candidates += File(home, "AppData/Local/Programs/npm/npx.cmd")
            candidates += File(home, "AppData/Roaming/npm/npx.cmd")
            candidates += File("C:/Program Files/nodejs/npx.cmd")
            candidates += File("C:/Program Files (x86)/nodejs/npx.cmd")
        } else if (os.contains("mac") || os.contains("darwin")) {
            candidates += File("/opt/homebrew/bin/npx")
            candidates += File("/usr/local/bin/npx")
        } else {
            candidates += File("/usr/bin/npx")
            candidates += File("/usr/local/bin/npx")
        }

        val nvmDir = File(home, ".nvm/versions/node")
        if (nvmDir.isDirectory) {
            nvmDir.listFiles()?.forEach { version ->
                val bin = File(version, "bin/npx")
                if (bin.exists()) candidates += bin
            }
        }

        val asdfShim = File(home, ".asdf/shims/npx")
        if (asdfShim.exists()) candidates += asdfShim

        return candidates.firstOrNull { it.exists() && it.canExecute() }?.absolutePath
    }

    fun hasTool(name: String): Boolean = tools.containsKey(name)

    fun listTools() = try {
        clients.values.flatMap { it.listTools() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun execute(id: String, name: String, args: String): String {
        val client = tools[name] ?: return "Tool not found"
        return withContext(scope.coroutineContext) {
            runCatching {
                client.executeTool(
                    AgentToolExecutionRequest.builder()
                        .id(id)
                        .name(name)
                        .arguments(args)
                        .build()
                )
            }.getOrElse { e -> "Error: ${e.message}" }
        }
    }

    fun stop() {
        clients.values.forEach { runCatching { it.close() } }
        scope.cancel()
    }
}

