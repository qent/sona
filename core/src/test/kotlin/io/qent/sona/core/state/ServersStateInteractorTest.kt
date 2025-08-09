package io.qent.sona.core.state

import io.qent.sona.core.mcp.McpServerStatus
import io.qent.sona.core.state.interactors.ServersController
import io.qent.sona.core.state.interactors.ServersStateInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeServersController : ServersController {
    val toggled = mutableListOf<String>()
    val toggledTools = mutableListOf<Pair<String, String>>()
    var reloaded = false
    var stopped = false
    override val servers = MutableStateFlow<List<McpServerStatus>>(emptyList())
    override fun toggle(name: String) { toggled.add(name) }
    override fun toggleTool(server: String, tool: String) { toggledTools.add(server to tool) }
    override suspend fun reload() { reloaded = true }
    override fun stop() { stopped = true }
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

    @Test
    fun reloadDelegatesToController() = runBlocking {
        val controller = FakeServersController()
        val interactor = ServersStateInteractor(controller)
        interactor.reload()
        assertTrue(controller.reloaded)
    }

    @Test
    fun stopDelegatesToController() {
        val controller = FakeServersController()
        val interactor = ServersStateInteractor(controller)
        interactor.stop()
        assertTrue(controller.stopped)
    }
}

