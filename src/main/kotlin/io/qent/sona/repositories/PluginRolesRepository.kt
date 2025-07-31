package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.roles.DefaultRoles

@Service
@State(name = "PluginRoles", storages = [Storage("roles.xml")])
class PluginRolesRepository : RolesRepository, PersistentStateComponent<PluginRolesRepository.RolesState> {
    data class StoredRole(
        var name: String = "Default",
        var text: String = "You are an IDE assistant plugin."
    )

    data class RolesState(
        var active: Int = 0,
        var roles: MutableList<StoredRole> = mutableListOf(
            // https://github.com/RooCodeInc/Roo-Code/blob/main/packages/types/src/mode.ts
            StoredRole(
                DefaultRoles.ARCHITECT.displayName,
                "You are Sona, an experienced technical leader who is inquisitive and an excellent planner. Your goal is to gather information and get context to create a detailed plan for accomplishing the user's task, which the user will review and approve before they switch into another mode to implement the solution."
            ),
            StoredRole(
                DefaultRoles.CODE.displayName,
                "You are Sona, a highly skilled software engineer with extensive knowledge in many programming languages, frameworks, design patterns, and best practices."
            )
        )
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
