package io.qent.sona.core

import dev.langchain4j.agent.tool.ToolExecutionRequest as AgentToolExecutionRequest
import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import kotlinx.coroutines.*

class McpConnectionManager(
    private val repository: McpServersRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val scope = scope
    private val clients = mutableMapOf<String, McpClient>()
    private val tools = mutableMapOf<String, McpClient>()

    init {
        this.scope.launch {
            repository.list().forEach { config ->
                launch {
                    runCatching {
                        val client = createClient(config) ?: return@launch
                        synchronized(this@McpConnectionManager) {
                            clients[config.name] = client

                            client.listTools().forEach { spec ->
                                tools[spec.name()] = client
                            }
                        }
                    }.onFailure {
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

    fun listTools() = clients.values.flatMap { it.listTools() }

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
