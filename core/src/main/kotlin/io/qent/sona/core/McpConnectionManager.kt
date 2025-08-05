package io.qent.sona.core

import dev.langchain4j.agent.tool.ToolExecutionRequest as AgentToolExecutionRequest
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class McpConnectionManager(
    private val repository: McpServersRepository,
    scope: CoroutineScope
) {
    private val scope = scope + SupervisorJob() + Dispatchers.IO
    private val clients = mutableMapOf<String, McpClient>()
    private val tools = mutableMapOf<String, McpClient>()
    private val statuses = mutableMapOf<String, McpServerStatus>()

    private val _servers = MutableStateFlow<List<McpServerStatus>>(emptyList())
    val servers: StateFlow<List<McpServerStatus>> = _servers.asStateFlow()

    private fun updateStatus(status: McpServerStatus) {
        synchronized(this) {
            statuses[status.name] = status
            _servers.value = statuses.values.toList()
        }
    }

    init {
        this.scope.launch {
            repository.list().forEach { config ->
                updateStatus(
                    McpServerStatus(
                        name = config.name,
                        status = McpServerStatus.Status.CONNECTING,
                        tools = emptyList()
                    )
                )
                launch {
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
        }
    }

    private fun createClient(config: McpServerConfig): DefaultMcpClient? {
        val transport = when (config.transport?.lowercase()) {
            "stdio" -> {
                val cmd = buildList {
                    config.command?.let { add(it) }
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
