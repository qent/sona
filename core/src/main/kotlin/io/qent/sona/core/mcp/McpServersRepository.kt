package io.qent.sona.core.mcp

data class McpServerConfig(
    val name: String,
    val command: String? = null,
    val args: List<String>? = null,
    val env: Map<String, String>? = null,
    val transport: String,
    val url: String? = null,
    val cwd: String? = null,
    val headers: Map<String, String>? = null,
)

interface McpServersRepository {
    suspend fun list(): List<McpServerConfig>
    suspend fun loadEnabled(): Set<String>
    suspend fun saveEnabled(enabled: Set<String>)
}
