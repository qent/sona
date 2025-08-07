package io.qent.sona.core.state

import io.qent.sona.core.mcp.McpServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeServersController : ServersController {
    val toggled = mutableListOf<String>()
    val toggledTools = mutableListOf<Pair<String, String>>()
    override val servers = MutableStateFlow<List<McpServerStatus>>(emptyList())
    override fun toggle(name: String) { toggled.add(name) }
    override fun toggleTool(server: String, tool: String) { toggledTools.add(server to tool) }
    override suspend fun reload() { }
    override fun stop() {}
}

class ServersStateInteractorTest {
    @Test
    fun toggleDelegatesToController() = runBlocking {
        val controller = FakeServersController()
        val interactor = ServersStateInteractor(controller)
        interactor.toggle("a")
        assertTrue(controller.toggled.contains("a"))
    }

    @Test
    fun toggleToolDelegatesToController() = runBlocking {
        val controller = FakeServersController()
        val interactor = ServersStateInteractor(controller)
        interactor.toggleTool("s", "t")
        assertTrue(controller.toggledTools.contains("s" to "t"))
    }
}

