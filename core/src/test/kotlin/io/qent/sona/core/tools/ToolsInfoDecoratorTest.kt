package io.qent.sona.core.tools

import io.qent.sona.core.permissions.DirectoryListing
import io.qent.sona.core.permissions.FileElement
import io.qent.sona.core.permissions.FileElementType
import io.qent.sona.core.permissions.FileInfo
import io.qent.sona.core.permissions.FilePermissionManager
import io.qent.sona.core.permissions.FilePermissionsRepository
import io.qent.sona.core.permissions.FileStructureInfo
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
    override fun readFile(path: String): FileInfo? = files[path]
    override fun applyPatch(patch: String) = ""
    override fun listPath(path: String): DirectoryListing? = dirs[path]
}

private class FakeInternalTools : InternalTools {
    var last: String? = null
    override fun switchRole(name: String): String { last = name; return name }
}

class ToolsInfoDecoratorTest {

    @Test
    fun `focused file info passes through permission check`() {
        val repo = StubRepository(listOf(".*"), emptyList())
        val manager = FilePermissionManager(repo)
        val elem = FileElement("C", FileElementType.CLASS, true, 1 to 2)
        val info = FileStructureInfo("/a", listOf(elem))
        val decorator = ToolsInfoDecorator(FakeInternalTools(), FakeExternalTools(info, emptyMap()), manager)
        assertEquals(info, decorator.getFocusedFileInfo())
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
        assertEquals("R", decorator.switchRole("R"))
        assertEquals("R", internal.last)
    }

    @Test
    fun `readFile returns message when file not found`() {
        val repo = StubRepository(listOf(".*"), emptyList())
        val manager = FilePermissionManager(repo)
        val decorator = ToolsInfoDecorator(FakeInternalTools(), FakeExternalTools(null, emptyMap()), manager)
        assertEquals("File not found", decorator.readFile("/missing"))
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
        val decorator = ToolsInfoDecorator(FakeInternalTools(), external, manager)
        val result = decorator.listPath("/allowed")
        assertEquals(listOf("file.txt", "dir/"), result.items)
        assertEquals(mapOf("dir/" to listOf("inside.txt")), result.contents)
    }
}

