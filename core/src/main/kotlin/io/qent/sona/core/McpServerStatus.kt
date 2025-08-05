package io.qent.sona.core

import dev.langchain4j.agent.tool.ToolSpecification

/**
 * Represents the current connection state of an MCP server.
 */
data class McpServerStatus(
    val name: String,
    val status: Status,
    val tools: List<ToolSpecification>
) {
    sealed interface Status {
        data object CONNECTING : Status
        data object CONNECTED : Status
        data class FAILED(val e: Exception) : Status
    }
}
