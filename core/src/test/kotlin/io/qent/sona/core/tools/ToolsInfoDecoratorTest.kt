package io.qent.sona.core.tools

import io.qent.sona.core.permissions.FileInfo
import io.qent.sona.core.permissions.FilePermissionManager
import io.qent.sona.core.permissions.FilePermissionsRepository
import org.junit.Assert.assertEquals
import org.junit.Test

private class StubRepository(
    override val whitelist: List<String>,
    override val blacklist: List<String>
) : FilePermissionsRepository

private class FakeExternalTools(
    private val focused: FileInfo?,
    private val files: Map<String, FileInfo?>
) : ExternalTools {
    override fun getFocusedFileText(): FileInfo? = focused
    override fun readFile(path: String): FileInfo? = files[path]
}

private class FakeInternalTools : InternalTools {
    var architectCalls = 0
    var codeCalls = 0
    override fun switchToArchitect(): String { architectCalls++; return "A" }
    override fun switchToCode(): String { codeCalls++; return "C" }
}

class ToolsInfoDecoratorTest {

    @Test
    fun `focused file text passes through permission check`() {
        val repo = StubRepository(listOf(".*"), emptyList())
        val manager = FilePermissionManager(repo)
        val info = FileInfo("/a", "ok")
        val decorator = ToolsInfoDecorator(FakeInternalTools(), FakeExternalTools(info, emptyMap()), manager)
        assertEquals("ok", decorator.getFocusedFileText())
    }

    @Test
    fun `readFile denies access when not whitelisted`() {
        val repo = StubRepository(emptyList(), emptyList())
        val manager = FilePermissionManager(repo)
        val info = FileInfo("/secret", "pw")
        val decorator = ToolsInfoDecorator(FakeInternalTools(), FakeExternalTools(null, mapOf("/secret" to info)), manager)
        assertEquals("Access to /secret denied", decorator.readFile("/secret"))
    }

    @Test
    fun `delegates role switching to internal tools`() {
        val internal = FakeInternalTools()
        val decorator = ToolsInfoDecorator(internal, FakeExternalTools(null, emptyMap()), FilePermissionManager(StubRepository(listOf(".*"), emptyList())))
        assertEquals("A", decorator.switchToArchitect())
        assertEquals("C", decorator.switchToCode())
        assertEquals(1, internal.architectCalls)
        assertEquals(1, internal.codeCalls)
    }

    @Test
    fun `readFile returns message when file not found`() {
        val repo = StubRepository(listOf(".*"), emptyList())
        val manager = FilePermissionManager(repo)
        val decorator = ToolsInfoDecorator(FakeInternalTools(), FakeExternalTools(null, emptyMap()), manager)
        assertEquals("File not found", decorator.readFile("/missing"))
    }
}

