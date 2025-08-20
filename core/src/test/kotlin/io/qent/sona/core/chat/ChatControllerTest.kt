package io.qent.sona.core.chat

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.mcp.McpConnectionManager
import io.qent.sona.core.mcp.McpServerConfig
import io.qent.sona.core.mcp.McpServersRepository
import io.qent.sona.core.model.TokenUsageInfo
import io.qent.sona.core.presets.LlmProvider
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.settings.Settings
import io.qent.sona.core.settings.SettingsRepository
import io.qent.sona.core.data.DirectoryListing
import io.qent.sona.core.data.FileStructureInfo
import io.qent.sona.core.data.FileDependenciesInfo
import io.qent.sona.core.data.SearchResult
import io.qent.sona.core.tools.Tools
import io.qent.sona.core.tokens.TokenCounter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakePresetsRepository(private val preset: Preset) : PresetsRepository {
    override suspend fun load() = Presets(0, listOf(preset))
    override suspend fun save(presets: Presets) {}
}

private class FakeRolesRepository : RolesRepository {
    override suspend fun load() = Roles(0, listOf(Role("r", "s", "t")))
    override suspend fun save(roles: Roles) {}
}

private class FakeChatRepository : ChatRepository {
    val messages = mutableMapOf<String, MutableList<ChatRepositoryMessage>>()
    val usage = mutableMapOf<String, TokenUsageInfo>()
    val allowed = mutableSetOf<String>()
    override suspend fun createChat(): String = "1"
    override suspend fun addMessage(chatId: String, message: dev.langchain4j.data.message.ChatMessage, model: String, tokenUsage: TokenUsageInfo) {
        messages.getOrPut(chatId) { mutableListOf() }.add(ChatRepositoryMessage(chatId, message, model, tokenUsage))
        val current = usage[chatId] ?: TokenUsageInfo()
        usage[chatId] = current + tokenUsage
    }
    override suspend fun loadMessages(chatId: String) = messages[chatId] ?: emptyList()
    override suspend fun loadTokenUsage(chatId: String) = usage[chatId] ?: TokenUsageInfo()
    override suspend fun isToolAllowed(chatId: String, toolName: String) = allowed.contains(toolName)
    override suspend fun addAllowedTool(chatId: String, toolName: String) { allowed.add(toolName) }
    override suspend fun listChats() = emptyList<ChatSummary>()
    override suspend fun deleteChat(chatId: String) {}
    override suspend fun deleteMessagesFrom(chatId: String, index: Int) {
        messages[chatId]?.apply { if (index < size) subList(index, size).clear() }
    }
}

private class FakeTools : Tools {
    override fun getFocusedFileInfo() = FileStructureInfo("", emptyList(), 0)
        override fun getFileLines(path: String, fromLine: Int, toLine: Int) = ""
    override fun applyPatch(patch: String) = ""
    override fun switchRole(name: String) = ""
    override fun listPath(path: String) = DirectoryListing(emptyList(), emptyMap())
    override fun getFileDependencies(path: String) = FileDependenciesInfo(path, emptyList())
    override fun sendTerminalCommand(command: String) = ""
    override fun readTerminalOutput() = ""
    override fun search(searchRequest: String) = emptyList<SearchResult>()
}

private class FakeSettingsRepository : SettingsRepository {
    override suspend fun load() = Settings(false, false, false, 0, false, true)
}

private class EmptyMcpRepository : McpServersRepository {
    override suspend fun list() = emptyList<McpServerConfig>()
    override suspend fun loadEnabled() = emptySet<String>()
    override suspend fun saveEnabled(enabled: Set<String>) {}
    override suspend fun loadDisabledTools() = emptyMap<String, Set<String>>()
    override suspend fun saveDisabledTools(disabled: Map<String, Set<String>>) {}
}

private data class ChatDeps(
    val controller: ChatController,
    val stateFlow: ChatStateFlow,
    val permissionedToolExecutor: PermissionedToolExecutor,
    val scope: CoroutineScope,
    val mcp: McpConnectionManager,
)

private fun buildChatController(repo: FakeChatRepository): ChatDeps {
    val provider = LlmProvider("p", "e", emptyList())
    val preset = Preset("n", provider, "e", "m", "k")
    val presetsRepo = FakePresetsRepository(preset)
    val rolesRepo = FakeRolesRepository()
    val tools = FakeTools()
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val mcpManager = McpConnectionManager(EmptyMcpRepository(), scope)
    val settingsRepo = FakeSettingsRepository()
    val stateFlow = ChatStateFlow(repo)
    val permissioned = PermissionedToolExecutor(stateFlow, repo)
    val toolsMapFactory = ToolsMapFactory(stateFlow, tools, mcpManager, permissioned, rolesRepo, presetsRepo)
    val agentFactory = ChatAgentFactory({ throw UnsupportedOperationException() }, { emptyList() }, toolsMapFactory, presetsRepo, rolesRepo, repo, "error")
    val tokenCounter = object : TokenCounter { override suspend fun count(text: String, preset: Preset) = 0 }
    val controller = ChatController(presetsRepo, repo, settingsRepo, stateFlow, agentFactory, scope, tokenCounter, { "" })
    return ChatDeps(controller, stateFlow, permissioned, scope, mcpManager)
}

class ChatControllerTest {
    @Test
    fun loadChatEmitsState() = runBlocking {
        val repo = FakeChatRepository()
        repo.messages["1"] = mutableListOf(ChatRepositoryMessage("1", UserMessage.from("hi"), "m"))
        val deps = buildChatController(repo)
        val channel = Channel<Chat>(1)
        val job = launch { deps.stateFlow.collect { channel.send(it) } }
        yield()
        deps.stateFlow.loadChat("1")
        val chat = withTimeout(1000) { channel.receive() }
        job.cancel()
        deps.scope.cancel()
        deps.mcp.stop()
        assertEquals("1", chat.chatId)
        assertEquals(1, chat.messages.size)
    }

    @Test
    fun toggleAutoApproveToolsFlipsFlag() = runBlocking {
        val repo = FakeChatRepository()
        val deps = buildChatController(repo)
        val channel = Channel<Chat>(2)
        val job = launch { deps.stateFlow.collect { channel.send(it) } }
        yield()
        deps.stateFlow.loadChat("1")
        channel.receive() // initial state
        deps.controller.toggleAutoApproveTools()
        val toggled = withTimeout(1000) { channel.receive() }
        job.cancel()
        deps.scope.cancel()
        deps.mcp.stop()
        assertTrue(toggled.autoApproveTools)
    }

    @Test
    fun toolExecutionRequestsPermission() = runBlocking {
        val repo = FakeChatRepository()
        val deps = buildChatController(repo)
        val channel = Channel<Chat>(Channel.UNLIMITED)
        val job = launch { deps.stateFlow.collect { channel.send(it) } }
        yield()
        deps.stateFlow.loadChat("1")
        channel.receive() // initial state
        val executor = deps.permissionedToolExecutor.create("1", "m") { "done" }
        val req = ToolExecutionRequest.builder().id("x").name("tool").arguments("{}" ).build()
        val deferred = async { executor.execute(req, null) }
        val toolState = withTimeout(1000) { channel.receive() }
        assertEquals("tool", toolState.toolRequest)
        deps.permissionedToolExecutor.resolveToolPermission(true, true)
        assertEquals("done", deferred.await())
        assertTrue(repo.allowed.contains("tool"))
        job.cancel()
        deps.scope.cancel()
        deps.mcp.stop()
    }
}
