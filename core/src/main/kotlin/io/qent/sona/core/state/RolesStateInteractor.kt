package io.qent.sona.core.state

import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.DefaultRoles

class RolesStateInteractor(private val flow: RolesStateFlow) {
    var creatingRole: Boolean = false
        private set

    suspend fun load(): Roles {
        flow.load()
        return flow.value
    }

    suspend fun selectRole(idx: Int) {
        val roles = flow.value.copy(active = idx)
        flow.save(roles)
    }

    suspend fun selectRole(name: String) {
        val idx = flow.value.roles.indexOfFirst { it.name == name }
        if (idx >= 0) {
            selectRole(idx)
        }
    }

    suspend fun saveRole(short: String, text: String) {
        val current = flow.value
        val list = current.roles.toMutableList()
        list[current.active] = list[current.active].copy(short = short, text = text)
        flow.save(current.copy(roles = list))
    }

    fun startCreateRole() {
        creatingRole = true
    }

    fun finishCreateRole() {
        creatingRole = false
    }

    suspend fun addRole(name: String, short: String, text: String) {
        val current = flow.value
        val roles = Roles(active = current.roles.size, roles = current.roles + Role(name, short, text))
        creatingRole = false
        flow.save(roles)
    }

    suspend fun deleteRole() {
        val current = flow.value
        val currentName = current.roles[current.active].name
        if (currentName in DefaultRoles.NAMES) return
        val list = current.roles.toMutableList()
        list.removeAt(current.active)
        val newActive = current.active.coerceAtMost(list.lastIndex)
        flow.save(Roles(active = newActive, roles = list))
    }
}

