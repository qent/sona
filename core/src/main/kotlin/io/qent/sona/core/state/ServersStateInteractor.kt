package io.qent.sona.core.state

import io.qent.sona.core.mcp.McpServerStatus
import kotlinx.coroutines.flow.StateFlow

interface ServersController {
    val servers: StateFlow<List<McpServerStatus>>
    fun toggle(name: String)
    fun toggleTool(server: String, tool: String)
    suspend fun reload()
    fun stop()
}

class ServersStateInteractor(private val controller: ServersController) {
    val servers: StateFlow<List<McpServerStatus>> = controller.servers

    fun toggle(name: String) = controller.toggle(name)
    fun toggleTool(server: String, tool: String) = controller.toggleTool(server, tool)

    suspend fun reload() = controller.reload()

    fun stop() = controller.stop()
}

