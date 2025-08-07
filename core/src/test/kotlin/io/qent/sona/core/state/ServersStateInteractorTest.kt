package io.qent.sona.core.state

import io.qent.sona.core.mcp.McpServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeServersController : ServersController {
    val toggled = mutableListOf<String>()
    override val servers = MutableStateFlow<List<McpServerStatus>>(emptyList())
    override fun toggle(name: String) { toggled.add(name) }
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
}

