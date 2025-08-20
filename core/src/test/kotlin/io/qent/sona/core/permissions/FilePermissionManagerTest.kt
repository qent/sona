package io.qent.sona.core.permissions

import io.qent.sona.core.data.FileLines
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeRepository(
    override val whitelist: List<String>,
    override val blacklist: List<String>,
    override val projectPath: String = ""
) : FilePermissionsRepository

class FilePermissionManagerTest {

    @Test
    fun `returns content for whitelisted file`() {
        val repo = FakeRepository(whitelist = listOf(".*"), blacklist = emptyList())
        val manager = FilePermissionManager(repo)
        val info = FileLines("/path/file.txt", "content")
        assertEquals("content", manager.getFileContent(info))
    }

    @Test
    fun `denies access when path not whitelisted`() {
        val repo = FakeRepository(whitelist = listOf("/allowed/.*"), blacklist = emptyList())
        val manager = FilePermissionManager(repo)
        val info = FileLines("/other/file.txt", "content")
        assertEquals("Access to /other/file.txt denied", manager.getFileContent(info))
    }

    @Test
    fun `blacklist overrides whitelist`() {
        val repo = FakeRepository(
            whitelist = listOf(".*"),
            blacklist = listOf(".*secret.*")
        )
        val manager = FilePermissionManager(repo)
        val info = FileLines("/allowed/secret.txt", "content")
        assertEquals("Access to /allowed/secret.txt denied", manager.getFileContent(info))
    }

    @Test
    fun `partial match in whitelist allows access`() {
        val repo = FakeRepository(whitelist = listOf("src/allowed"), blacklist = emptyList())
        val manager = FilePermissionManager(repo)
        val info = FileLines("/project/src/allowed/My.kt", "code")
        assertEquals("code", manager.getFileContent(info))
    }
}

