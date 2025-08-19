package io.qent.sona.core.tools

import io.qent.sona.core.permissions.DirectoryListing
import io.qent.sona.core.permissions.FileElement
import io.qent.sona.core.permissions.FileElementType
import io.qent.sona.core.permissions.FileInfo
import io.qent.sona.core.permissions.FilePermissionManager
import io.qent.sona.core.permissions.FilePermissionsRepository
import io.qent.sona.core.permissions.FileDependenciesInfo
import io.qent.sona.core.permissions.FileStructureInfo
import io.qent.sona.core.chat.Chat
import io.qent.sona.core.chat.ChatRepository
import io.qent.sona.core.chat.ChatRepositoryMessage
import io.qent.sona.core.chat.ChatSummary
import io.qent.sona.core.chat.ChatStateFlow
import io.qent.sona.core.model.TokenUsageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import dev.langchain4j.data.message.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

private class StubRepository(
    override val whitelist: List<String>,
    override val blacklist: List<String>,
    override val projectPath: String = ""
) : FilePermissionsRepository

private class FakeExternalTools(
    private val focused: FileStructureInfo?,
    private val files: Map<String, FileInfo?>,
    private val dirs: Map<String, DirectoryListing?> = emptyMap()
) : ExternalTools {
    override fun getFocusedFileInfo(): FileStructureInfo? = focused
    override fun getFileLines(path: String, fromLine: Int, toLine: Int): FileInfo? = files[path]
    override fun createPatch(chatId: String, patch: String) = 0
    override fun applyPatch(chatId: String, patchId: Int) = ""
    override fun listPath(path: String): DirectoryListing? = dirs[path]
    override fun sendTerminalCommand(command: String) = ""
    override fun readTerminalOutput() = ""
    override fun getFileDependencies(path: String): FileDependenciesInfo? = null
}

private class FakeInternalTools : InternalTools {
    var last: String? = null
    override fun switchRole(name: String): String { last = name; return name }
}

private fun testChatStateFlow(): ChatStateFlow {
    val repo = object : ChatRepository {
        override suspend fun createChat() = "1"
        override suspend fun addMessage(chatId: String, message: ChatMessage, model: String, tokenUsage: TokenUsageInfo) {}
        override suspend fun loadMessages(chatId: String) = emptyList<ChatRepositoryMessage>()
        override suspend fun loadTokenUsage(chatId: String) = TokenUsageInfo()
        override suspend fun isToolAllowed(chatId: String, toolName: String) = true
        override suspend fun addAllowedTool(chatId: String, toolName: String) {}
        override suspend fun listChats() = emptyList<ChatSummary>()
        override suspend fun deleteChat(chatId: String) {}
        override suspend fun deleteMessagesFrom(chatId: String, index: Int) {}
    }
    return ChatStateFlow(repo).apply {
        emit(Chat("1", TokenUsageInfo()))
    }
}

class ToolsInfoDecoratorTest {

    @Test
    fun `focused file info passes through permission check`() {
        val repo = StubRepository(listOf(".*"), emptyList())
        val manager = FilePermissionManager(repo)
        val elem = FileElement("C", FileElementType.CLASS, true, 1 to 2)
        val info = FileStructureInfo("/a", listOf(elem), 10)
        val decorator = ToolsInfoDecorator(testChatStateFlow(), FakeInternalTools(), FakeExternalTools(info, emptyMap()), manager)
        assertEquals(info, decorator.getFocusedFileInfo())
    }

    @Test
    fun `getFileLines denies access when not whitelisted`() {
        val repo = StubRepository(emptyList(), emptyList())
        val manager = FilePermissionManager(repo)
        val info = FileInfo("/secret", "pw")
        val decorator = ToolsInfoDecorator(testChatStateFlow(), FakeInternalTools(), FakeExternalTools(null, mapOf("/secret" to info)), manager)
        assertEquals("Access to /secret denied", decorator.getFileLines("/secret", 1, 2))
    }

    @Test
    fun `delegates role switching to internal tools`() {
        val internal = FakeInternalTools()
        val decorator = ToolsInfoDecorator(testChatStateFlow(), internal, FakeExternalTools(null, emptyMap()), FilePermissionManager(StubRepository(listOf(".*"), emptyList())))
        assertEquals("R", decorator.switchRole("R"))
        assertEquals("R", internal.last)
    }

    @Test
    fun `getFileLines returns message when file not found`() {
        val repo = StubRepository(listOf(".*"), emptyList())
        val manager = FilePermissionManager(repo)
        val decorator = ToolsInfoDecorator(testChatStateFlow(), FakeInternalTools(), FakeExternalTools(null, emptyMap()), manager)
        assertEquals("File not found", decorator.getFileLines("/missing", 1, 2))
    }

    @Test
    fun `listPath filters entries by permissions`() {
        val repo = StubRepository(listOf("/allowed.*"), listOf("/allowed/.*secret.*"))
        val manager = FilePermissionManager(repo)
        val listing = DirectoryListing(
            items = listOf("file.txt", "secret.txt", "dir/"),
            contents = mapOf("dir/" to listOf("inside.txt", "secret2.txt"))
        )
        val external = FakeExternalTools(null, emptyMap(), mapOf("/allowed" to listing))
        val decorator = ToolsInfoDecorator(testChatStateFlow(), FakeInternalTools(), external, manager)
        val result = decorator.listPath("/allowed")
        assertEquals(listOf("file.txt", "dir/"), result.items)
        assertEquals(mapOf("dir/" to listOf("inside.txt")), result.contents)
    }
}

