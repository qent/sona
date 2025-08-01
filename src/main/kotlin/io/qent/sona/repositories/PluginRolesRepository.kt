package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.qent.sona.core.Role
import io.qent.sona.core.Roles
import io.qent.sona.core.RolesRepository

@Service
@State(name = "PluginRoles", storages = [Storage("roles.xml")])
class PluginRolesRepository : RolesRepository, PersistentStateComponent<PluginRolesRepository.RolesState> {
    data class StoredRole(
        var name: String = "Default",
        var text: String = "You are an IDE assistant plugin."
    )

    data class RolesState(
        var active: Int = 0,
        var roles: MutableList<StoredRole> = mutableListOf(StoredRole())
    )

    private var state = RolesState()

    override fun getState() = state

    override fun loadState(state: RolesState) {
        this.state = state
    }

    override suspend fun load(): Roles = Roles(
        active = state.active,
        roles = state.roles.map { Role(it.name, it.text) }
    )

    override suspend fun save(roles: Roles) {
        state = RolesState(
            active = roles.active,
            roles = roles.roles.map { StoredRole(it.name, it.text) }.toMutableList()
        )
    }
}
