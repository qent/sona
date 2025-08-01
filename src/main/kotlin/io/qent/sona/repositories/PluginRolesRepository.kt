package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.qent.sona.core.RolesRepository

@Service
@State(name = "PluginRoles", storages = [Storage("roles.xml")])
class PluginRolesRepository : RolesRepository, PersistentStateComponent<PluginRolesRepository.RolesState> {
    data class RolesState(var text: String = "You are an IDE assistant plugin.")

    private var state = RolesState()

    override fun getState() = state

    override fun loadState(state: RolesState) {
        this.state = state
    }

    override suspend fun load(): String = state.text

    override suspend fun save(text: String) {
        state = state.copy(text = text)
    }
}
